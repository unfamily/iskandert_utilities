package net.unfamily.iskautils.item.custom;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.unfamily.iskautils.client.KeyBindings;
import net.unfamily.iskautils.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

/**
 * Portable Dislocator - A special item that can be worn as a Curio when available.
 * When worn or held in hand, provides portable dislocation functionality.
 */
public class PortableDislocatorItem extends Item {
    private static final Logger LOGGER = LoggerFactory.getLogger(PortableDislocatorItem.class);
    
    // Energy storage tag
    private static final String ENERGY_TAG = "Energy";
    
    // Custom ticket type for chunk loading
    private static final TicketType<Unit> DISLOCATOR_TICKET = TicketType.create("portable_dislocator", (a, b) -> 0, 100);
    
    // Static variables to track teleportation state per player
    private static final Map<UUID, TeleportationData> activeTeleportations = new HashMap<>();
    private static final Map<UUID, TeleportRequest> pendingRequests = new HashMap<>();
    
    // Data classes for teleportation management
    private static class TeleportationData {
        Player player;
        int targetX;
        int targetZ;
        int originalX;
        int originalZ;
        int ticksWaiting;
        ChunkPos loadedChunk;
        ServerLevel chunkLevel;
        int attemptCount;
        
        TeleportationData(Player player, int targetX, int targetZ) {
            this.player = player;
            this.targetX = targetX;
            this.targetZ = targetZ;
            this.originalX = targetX;
            this.originalZ = targetZ;
            this.ticksWaiting = 0;
            this.loadedChunk = null;
            this.chunkLevel = null;
            this.attemptCount = 1;
        }
    }
    
    private static class TeleportRequest {
        Player player;
        int targetX;
        int targetZ;
        
        TeleportRequest(Player player, int targetX, int targetZ) {
            this.player = player;
            this.targetX = targetX;
            this.targetZ = targetZ;
        }
    }
    
    private static final int MAX_WAIT_TICKS = 200; // 10 seconds max wait
    private static final int MAX_ATTEMPTS = 5; // Maximum number of teleportation attempts

    public PortableDislocatorItem(Properties properties) {
        super(properties);
    }
    
    /**
     * Adds tooltip information to the item
     */
    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
        // Get the current keybind from the player's settings
        String keybindName = KeyBindings.PORTABLE_DISLOCATOR_KEY.getTranslatedKeyMessage().getString();
        
        // Add the main tooltip with the current keybind
        tooltipComponents.add(Component.translatable("item.iska_utils.portable_dislocator.tooltip.main", keybindName));
        
        // Add additional info about supported compasses
        tooltipComponents.add(Component.translatable("item.iska_utils.portable_dislocator.tooltip.compasses"));
        
        // Add energy information
        if (canStoreEnergy()) {
            int energy = getEnergyStored(stack);
            int maxEnergy = getMaxEnergyStored(stack);
            
            tooltipComponents.add(Component.literal(String.format("Energy: %,d / %,d RF", energy, maxEnergy)));
            
            if (requiresEnergyToFunction()) {
                tooltipComponents.add(Component.literal("§7Energy per teleportation: " + getEffectiveEnergyConsumption() + " RF"));
            }
        } else {
            tooltipComponents.add(Component.literal("§7No energy required"));
        }
    }
    
    /**
     * Called every tick for every item in the inventory
     */
    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        
        // Execute only for players
        if (entity instanceof Player player) {
            // Handle keybind on client side
            if (level.isClientSide) {
                tickInInventory(stack, level, player, slotId, isSelected);
            }
            
            // Handle pending teleportation on server side
            if (!level.isClientSide) {
                handlePendingTeleportation(player, level);
                checkForTeleportRequest(player, level);
            }
        }
    }
    
    /**
     * Tick method for the item in normal inventory - handles keybind when item is present
     */
    private void tickInInventory(ItemStack stack, net.minecraft.world.level.Level level, Player player, int slotId, boolean isSelected) {
        // Check if the portable dislocator keybind was pressed
        if (KeyBindings.PORTABLE_DISLOCATOR_KEY.consumeClick()) {
            handleDislocatorActivation(player, "inventory");
        }
    }
    
    /**
     * Tick method for Curios - called by the Curio handler
     */
    public static void tickInCurios(ItemStack stack, net.minecraft.world.level.Level level, Player player) {
        // Check if the portable dislocator keybind was pressed (client side)
        if (level.isClientSide && KeyBindings.PORTABLE_DISLOCATOR_KEY.consumeClick()) {
            handleDislocatorActivation(player, "curios");
        }
        
        // Handle pending teleportation (server side)
        if (!level.isClientSide) {
            handlePendingTeleportation(player, level);
            checkForTeleportRequest(player, level);
        }
    }
    
    /**
     * Checks for teleport requests from client side
     */
    private static void checkForTeleportRequest(Player player, Level level) {
        UUID playerId = player.getUUID();
        TeleportRequest request = pendingRequests.get(playerId);
        
        if (request != null && level instanceof ServerLevel) {
            LOGGER.info("Processing teleport request from client for player {} to {}, {}", 
                player.getName().getString(), request.targetX, request.targetZ);
            
            // Clear the request
            pendingRequests.remove(playerId);
            
            // Find the Portable Dislocator and check/consume energy
            ItemStack dislocatorStack = findPortableDislocator(player);
            if (dislocatorStack != null) {
                PortableDislocatorItem dislocator = (PortableDislocatorItem) dislocatorStack.getItem();
                if (dislocator.consumeEnergyForTeleportation(dislocatorStack)) {
                    // Start the server-side teleportation
                    startServerTeleportation(player, request.targetX, request.targetZ);
                }
            }
        }
    }
    
    /**
     * Starts server-side teleportation
     */
    private static void startServerTeleportation(Player player, int targetX, int targetZ) {
        UUID playerId = player.getUUID();
        
        // Silent operation - no feedback
        
        // Always randomize coordinates to be 100-300 blocks away from the original target
        java.util.Random random = new java.util.Random();
        
        // Generate random offset for X: either [-300, -100] or [100, 300]
        int offsetX;
        if (random.nextBoolean()) {
            // Positive range: 100 to 300
            offsetX = random.nextInt(201) + 100; // 100-300
        } else {
            // Negative range: -300 to -100
            offsetX = -(random.nextInt(201) + 100); // -300 to -100
        }
        
        // Generate random offset for Z: either [-300, -100] or [100, 300]
        int offsetZ;
        if (random.nextBoolean()) {
            // Positive range: 100 to 300
            offsetZ = random.nextInt(201) + 100; // 100-300
        } else {
            // Negative range: -300 to -100
            offsetZ = -(random.nextInt(201) + 100); // -300 to -100
        }
        
        // Calculate actual distance for logging
        int distance = (int) Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);
        
        // Apply offset to original coordinates
        int randomizedX = targetX + offsetX;
        int randomizedZ = targetZ + offsetZ;
        
        // Create teleportation data with randomized coordinates
        TeleportationData data = new TeleportationData(player, randomizedX, randomizedZ);
        data.originalX = targetX; // Keep original coordinates for reference
        data.originalZ = targetZ;
        activeTeleportations.put(playerId, data);
    }
    
    /**
     * Handles pending teleportation process
     */
    private static void handlePendingTeleportation(Player player, Level level) {
        UUID playerId = player.getUUID();
        TeleportationData data = activeTeleportations.get(playerId);
        
        if (data == null) return;
        
        data.ticksWaiting++;
        
        // Silent waiting - no logs
        
        // Timeout after max wait time
        if (data.ticksWaiting > MAX_WAIT_TICKS) {
            // Try another attempt if we haven't exceeded max attempts
            if (data.attemptCount < MAX_ATTEMPTS) {
                attemptNewTeleportation(player, data.originalX, data.originalZ, data.attemptCount + 1);
            } else {
                // All attempts failed, give up silently
                resetTeleportationState(playerId);
            }
            return;
        }
        
        // Force chunk loading and generation
        if (level instanceof ServerLevel serverLevel) {
            int chunkX = data.targetX >> 4;
            int chunkZ = data.targetZ >> 4;
            ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
            
            // Add ticket to force chunk loading with higher priority
            serverLevel.getChunkSource().addRegionTicket(DISLOCATOR_TICKET, chunkPos, 2, Unit.INSTANCE);
            
            // Force chunk generation synchronously
            try {
                // Try to get chunk with force generation
                ChunkAccess chunk = serverLevel.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
                
                if (chunk == null) {
                    // Force load using different approach
                    serverLevel.getChunkSource().getChunkNow(chunkX, chunkZ);
                    chunk = serverLevel.getChunk(chunkX, chunkZ);
                }
                
                if (chunk == null) {
                    // Force generation using server methods
                    var chunkManager = serverLevel.getChunkSource().chunkMap;
                    chunk = serverLevel.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
                }
                
                // Check if chunk is actually loaded and accessible
                if (chunk != null) {
                    // Verify chunk is fully loaded by checking if we can access blocks
                    boolean canAccessBlocks = true;
                    try {
                        BlockPos testPos = new BlockPos(data.targetX, 64, data.targetZ);
                        BlockState testState = level.getBlockState(testPos);
                    } catch (Exception e) {
                        canAccessBlocks = false;
                    }
                    
                    if (canAccessBlocks) {
                        // Chunk is now loaded, find safe Y position
                        int safeY = findSafeY(level, data.targetX, data.targetZ);
                        
                        if (safeY != -1) {
                            // Store chunk info for later cleanup
                            data.loadedChunk = chunkPos;
                            data.chunkLevel = serverLevel;
                            
                            // Teleport to safe position
                            player.teleportTo(data.targetX + 0.5, safeY, data.targetZ + 0.5);
                            player.setDeltaMovement(0, 0, 0);
                            
                            // Silent success - no feedback
                            
                            // Schedule chunk unloading after 5 seconds (100 ticks)
                            scheduleChunkUnload(serverLevel, chunkPos, 100);
                            
                            resetTeleportationState(playerId);
                            return;
                        } else {
                            // Remove the ticket since we're not using the chunk
                            serverLevel.getChunkSource().removeRegionTicket(DISLOCATOR_TICKET, chunkPos, 2, Unit.INSTANCE);
                            
                            // Try another attempt if we haven't exceeded max attempts
                            if (data.attemptCount < MAX_ATTEMPTS) {
                                attemptNewTeleportation(player, data.originalX, data.originalZ, data.attemptCount + 1);
                            } else {
                                // All attempts failed, give up silently
                                resetTeleportationState(playerId);
                            }
                            return;
                        }
                    }
                } else {
                    // If we've been waiting a while, try a more aggressive approach
                    if (data.ticksWaiting > 60) { // After 3 seconds
                        // Force the chunk to be generated by accessing it directly
                        try {
                            BlockPos forcePos = new BlockPos(data.targetX, 64, data.targetZ);
                            level.getBlockState(forcePos); // This should force chunk generation
                        } catch (Exception e) {
                            // Silent failure
                        }
                    }
                }
                
            } catch (Exception e) {
                // Silent exception handling
            }
        }
    }
    
    /**
     * Schedules chunk unloading after specified ticks
     */
    private static void scheduleChunkUnload(ServerLevel serverLevel, ChunkPos chunkPos, int delayTicks) {
        LOGGER.info("Scheduling chunk unload for {}, {} after {} ticks", chunkPos.x, chunkPos.z, delayTicks);
        // Use the server's scheduler to remove the ticket after delay
        serverLevel.getServer().execute(() -> {
            // Schedule the ticket removal
            new Thread(() -> {
                try {
                    Thread.sleep(delayTicks * 50); // Convert ticks to milliseconds
                    serverLevel.getServer().execute(() -> {
                        serverLevel.getChunkSource().removeRegionTicket(DISLOCATOR_TICKET, chunkPos, 2, Unit.INSTANCE);
                        LOGGER.info("Removed chunk loading ticket for chunk at {}, {}", chunkPos.x, chunkPos.z);
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.error("Chunk unload scheduling interrupted", e);
                }
            }).start();
        });
    }
    
    /**
     * Resets teleportation state for a specific player
     */
    private static void resetTeleportationState(UUID playerId) {
        LOGGER.info("Resetting teleportation state for player {}", playerId);
        activeTeleportations.remove(playerId);
        pendingRequests.remove(playerId);
    }
    
    /**
     * Attempts new teleportation with randomized coordinates around the original target
     */
    private static void attemptNewTeleportation(Player player, int originalX, int originalZ, int attemptNumber) {
        UUID playerId = player.getUUID();
        
        // Generate new coordinates with varying distance based on attempt number
        java.util.Random random = new java.util.Random();
        
        // Adjust range based on attempt number for better success chance
        int minRange, maxRange;
        switch (attemptNumber) {
            case 2:
                minRange = 50; maxRange = 200; // Smaller range for second attempt
                break;
            case 3:
                minRange = 25; maxRange = 150; // Even smaller for third attempt
                break;
            case 4:
                minRange = 10; maxRange = 100; // Very small for fourth attempt
                break;
            case 5:
                minRange = 5; maxRange = 50; // Minimal range for final attempt
                break;
            default:
                minRange = 100; maxRange = 300; // Default range
                break;
        }
        
        // Generate random offset for X: either [-maxRange, -minRange] or [minRange, maxRange]
        int offsetX;
        if (random.nextBoolean()) {
            // Positive range
            offsetX = random.nextInt(maxRange - minRange + 1) + minRange;
        } else {
            // Negative range
            offsetX = -(random.nextInt(maxRange - minRange + 1) + minRange);
        }
        
        // Generate random offset for Z: either [-maxRange, -minRange] or [minRange, maxRange]
        int offsetZ;
        if (random.nextBoolean()) {
            // Positive range
            offsetZ = random.nextInt(maxRange - minRange + 1) + minRange;
        } else {
            // Negative range
            offsetZ = -(random.nextInt(maxRange - minRange + 1) + minRange);
        }
        
        int newX = originalX + offsetX;
        int newZ = originalZ + offsetZ;
        
        // Create new teleportation data for this attempt
        TeleportationData newData = new TeleportationData(player, newX, newZ);
        newData.originalX = originalX;
        newData.originalZ = originalZ;
        newData.attemptCount = attemptNumber;
        
        // Store the new attempt
        activeTeleportations.put(playerId, newData);
        
        // Silent retry - no notification
    }
    
    /**
     * Finds a safe Y coordinate using an intelligent algorithm, returns -1 if no safe position found
     */
    private static int findSafeY(Level level, int x, int z) {
        
        // Get dimension limits with safety margins
        int absoluteMinY = level.getMinBuildHeight();
        int absoluteMaxY = level.getMaxBuildHeight();
        
        // Apply safety margins and dimension-specific limits
        int minY = Math.max(absoluteMinY + 5, getSafeDimensionMinY(level));
        int maxY = Math.min(absoluteMaxY - 5, getSafeDimensionMaxY(level));
        
        // Ensure we don't exceed bedrock boundaries
        minY = Math.max(minY, getBedrockFloorY(level) + 1);
        maxY = Math.min(maxY, getBedrockCeilingY(level) - 3); // -3 for player height + safety
        
        int startY = Math.max(50, minY);
        
        // Phase 1: Search upward from Y=50, looking for spaces and prioritizing sky access
        int firstValidPosition = -1;
        int skyAccessPosition = -1;
        
        for (int y = startY; y <= maxY - 2; y++) {
            if (isSpaceFree(level, x, y, z)) {
                // Found a free space, check if we have solid ground
                if (hasValidGround(level, x, y, z)) {
                    if (firstValidPosition == -1) {
                        firstValidPosition = y;
                    }
                    
                    // Check if this position has sky access
                    if (level.canSeeSky(new BlockPos(x, y + 1, z))) {
                        skyAccessPosition = y;
                        break; // Found the best position, stop searching
                    }
                }
            }
        }
        
        // Phase 2: Decide which upward position to use
        int chosenUpwardPosition = -1;
        if (skyAccessPosition != -1) {
            chosenUpwardPosition = skyAccessPosition;
        } else if (firstValidPosition != -1) {
            chosenUpwardPosition = firstValidPosition;
        }
        
        // Phase 3: If no upward position found, search downward from Y=50
        int downwardPosition = -1;
        if (chosenUpwardPosition == -1) {
            for (int y = startY - 1; y >= minY; y--) {
                if (isSpaceFree(level, x, y, z)) {
                    if (hasValidGround(level, x, y, z)) {
                        downwardPosition = y;
                        break;
                    }
                }
            }
        }
        
        // Phase 4: Choose final position
        int finalY = chosenUpwardPosition != -1 ? chosenUpwardPosition : downwardPosition;
        
        if (finalY != -1) {
            // Phase 5: Final safety check - ensure we're within dimension limits
            if (!isWithinDimensionLimits(level, finalY)) {
                return -1;
            }
            
            // Phase 6: Handle water placement if needed
            prepareGround(level, x, finalY, z);
            
            return finalY;
        }
        
        return -1;
    }
    
    /**
     * Gets the safe minimum Y for a specific dimension
     */
    private static int getSafeDimensionMinY(Level level) {
        String dimensionKey = level.dimension().location().toString();
        
        switch (dimensionKey) {
            case "minecraft:the_nether":
                // Nether: avoid bottom bedrock layer (Y=0-4)
                return 5;
            case "minecraft:the_end":
                // End: avoid void areas, start from a safe height
                return 50;
            case "minecraft:overworld":
            default:
                // Overworld and other dimensions: use standard minimum
                return level.getMinBuildHeight() + 5;
        }
    }
    
    /**
     * Gets the safe maximum Y for a specific dimension
     */
    private static int getSafeDimensionMaxY(Level level) {
        String dimensionKey = level.dimension().location().toString();
        
        switch (dimensionKey) {
            case "minecraft:the_nether":
                // Nether: avoid top bedrock layer (Y=123-127)
                return 122;
            case "minecraft:the_end":
                // End: no height limit issues, use standard
                return level.getMaxBuildHeight() - 5;
            case "minecraft:overworld":
            default:
                // Overworld and other dimensions: use standard maximum
                return level.getMaxBuildHeight() - 5;
        }
    }
    
    /**
     * Gets the Y level of the bedrock floor for the dimension
     */
    private static int getBedrockFloorY(Level level) {
        String dimensionKey = level.dimension().location().toString();
        
        switch (dimensionKey) {
            case "minecraft:the_nether":
                return 0; // Nether bedrock floor at Y=0
            case "minecraft:overworld":
                return level.getMinBuildHeight(); // Overworld bedrock at bottom
            case "minecraft:the_end":
                return 0; // End has void below, treat as Y=0
            default:
                return level.getMinBuildHeight();
        }
    }
    
    /**
     * Gets the Y level of the bedrock ceiling for the dimension
     */
    private static int getBedrockCeilingY(Level level) {
        String dimensionKey = level.dimension().location().toString();
        
        switch (dimensionKey) {
            case "minecraft:the_nether":
                return 127; // Nether bedrock ceiling at Y=127
            case "minecraft:overworld":
                return level.getMaxBuildHeight(); // Overworld has no bedrock ceiling
            case "minecraft:the_end":
                return level.getMaxBuildHeight(); // End has no bedrock ceiling
            default:
                return level.getMaxBuildHeight();
        }
    }
    
    /**
     * Checks if a Y coordinate is within safe dimension limits
     */
    private static boolean isWithinDimensionLimits(Level level, int y) {
        int safeMinY = Math.max(level.getMinBuildHeight() + 5, getSafeDimensionMinY(level));
        int safeMaxY = Math.min(level.getMaxBuildHeight() - 5, getSafeDimensionMaxY(level));
        
        // Additional bedrock checks
        int bedrockFloor = getBedrockFloorY(level) + 1;
        int bedrockCeiling = getBedrockCeilingY(level) - 3; // -3 for player height + safety
        
        safeMinY = Math.max(safeMinY, bedrockFloor);
        safeMaxY = Math.min(safeMaxY, bedrockCeiling);
        
        boolean withinLimits = y >= safeMinY && y <= safeMaxY;
        
        // Silent limit checking
        
        return withinLimits;
    }
    
    /**
     * Checks if the space at the given position is free (2 blocks high for player)
     */
    private static boolean isSpaceFree(Level level, int x, int y, int z) {
        BlockPos feetPos = new BlockPos(x, y, z);
        BlockPos headPos = new BlockPos(x, y + 1, z);
        
        BlockState feetState = level.getBlockState(feetPos);
        BlockState headState = level.getBlockState(headPos);
        
        // CRITICAL: Never allow teleportation into bedrock
        if (feetState.is(net.minecraft.world.level.block.Blocks.BEDROCK) || 
            headState.is(net.minecraft.world.level.block.Blocks.BEDROCK)) {
            return false;
        }
        
        // Check if blocks are liquids (water, lava, etc.)
        boolean feetIsLiquid = !feetState.getFluidState().isEmpty();
        boolean headIsLiquid = !headState.getFluidState().isEmpty();
        
        // Allow air, replaceable blocks (like grass, flowers), and flowers - BUT NOT LIQUIDS
        boolean feetFree = feetState.isAir() || 
                          (!feetIsLiquid && feetState.is(net.minecraft.tags.BlockTags.REPLACEABLE)) ||
                          (!feetIsLiquid && feetState.is(net.minecraft.tags.BlockTags.FLOWERS));
        boolean headFree = headState.isAir() || 
                          (!headIsLiquid && headState.is(net.minecraft.tags.BlockTags.REPLACEABLE)) ||
                          (!headIsLiquid && headState.is(net.minecraft.tags.BlockTags.FLOWERS));
        
        return feetFree && headFree;
    }
    
    /**
     * Checks if there's valid ground below the position
     */
    private static boolean hasValidGround(Level level, int x, int y, int z) {
        BlockPos groundPos = new BlockPos(x, y - 1, z);
        BlockState groundState = level.getBlockState(groundPos);
        
        // CRITICAL: Never place player on bedrock in dangerous positions
        if (groundState.is(net.minecraft.world.level.block.Blocks.BEDROCK)) {
            // Check if this is safe bedrock (like Nether floor) or dangerous (like Nether ceiling)
            String dimensionKey = level.dimension().location().toString();
            if ("minecraft:the_nether".equals(dimensionKey)) {
                // In Nether, bedrock at Y=0-4 is floor (safe), bedrock at Y=123-127 is ceiling (unsafe)
                if (groundPos.getY() >= 123) {
                    return false;
                }
            }
        }
        
        // Check if it's a liquid
        boolean isLiquid = !groundState.getFluidState().isEmpty();
        if (isLiquid) {
            // Only water is acceptable as ground (can be replaced with cobblestone)
            // Lava and other liquids make the position invalid
            boolean isWater = groundState.is(net.minecraft.world.level.block.Blocks.WATER) ||
                             groundState.getFluidState().is(net.minecraft.world.level.material.Fluids.WATER) ||
                             groundState.getFluidState().is(net.minecraft.world.level.material.Fluids.FLOWING_WATER);
            
            if (!isWater) {
                // This is lava or another liquid - position is not valid
                return false;
            }
        }
        
        // Valid ground: solid block, or water (which we can replace with cobblestone)
        boolean isSolid = groundState.isSolid();
        boolean isWater = isLiquid && (groundState.is(net.minecraft.world.level.block.Blocks.WATER) ||
                                      groundState.getFluidState().is(net.minecraft.world.level.material.Fluids.WATER) ||
                                      groundState.getFluidState().is(net.minecraft.world.level.material.Fluids.FLOWING_WATER));
        boolean isAir = groundState.isAir();
        
        boolean hasGround = isSolid || isWater;
        
        // Silent ground checking
        
        return hasGround;
    }
    
    /**
     * Prepares the ground for teleportation, placing cobblestone if needed
     */
    private static void prepareGround(Level level, int x, int finalY, int z) {
        BlockPos groundPos = new BlockPos(x, finalY - 1, z);
        BlockState groundState = level.getBlockState(groundPos);
        
        // If ground is water, replace it with cobblestone
        if (!groundState.isSolid() && !groundState.isAir()) {
            boolean isLiquid = !groundState.getFluidState().isEmpty();
            if (isLiquid) {
                // Only replace water with cobblestone - other liquids should not reach this point
                boolean isWater = groundState.is(net.minecraft.world.level.block.Blocks.WATER) ||
                                 groundState.getFluidState().is(net.minecraft.world.level.material.Fluids.WATER) ||
                                 groundState.getFluidState().is(net.minecraft.world.level.material.Fluids.FLOWING_WATER);
                
                if (isWater) {
                    // Replace water with cobblestone
                    level.setBlock(groundPos, net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState(), 3);
                }
                // Note: Other liquids (lava, etc.) should not reach this point due to hasValidGround check
            }
        }
        
        // Note: Flowers and grass are left as-is, player can spawn through them
    }
    
    /**
     * Checks if a position is safe for teleportation (legacy method for compatibility)
     * @deprecated Use the new intelligent algorithm instead
     */
    @Deprecated
    private static boolean isSafePosition(Level level, int x, int y, int z) {
        return isSpaceFree(level, x, y, z) && hasValidGround(level, x, y, z);
    }
    
    /**
     * Handles the activation of the Portable Dislocator
     */
    private static void handleDislocatorActivation(Player player, String source) {
        
        // Prevent activation if already teleporting
        UUID playerId = player.getUUID();
        TeleportationData data = activeTeleportations.get(playerId);
        if (data != null) {
            return;
        }
        
        // Find the Portable Dislocator
        ItemStack dislocatorStack = findPortableDislocator(player);
        if (dislocatorStack == null) {
            return; // No dislocator found
        }
        
        // Check energy requirements
        PortableDislocatorItem dislocator = (PortableDislocatorItem) dislocatorStack.getItem();
        if (!dislocator.hasEnoughEnergy(dislocatorStack)) {
            // Silent failure - insufficient energy
            return;
        }
        
        // Check if the player has a compass in hand
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        
        LOGGER.info("Checking hands - Main: {}, Off: {}", 
            mainHand.isEmpty() ? "empty" : mainHand.getItem().toString(),
            offHand.isEmpty() ? "empty" : offHand.getItem().toString());
        
        ItemStack compassStack = null;
        String compassType = null;
        
        // Check main hand
        if (isValidCompass(mainHand)) {
            compassStack = mainHand;
            compassType = getCompassType(mainHand);
            LOGGER.info("Found valid compass in main hand: {}", compassType);
        }
        // If not found in main hand, check off hand
        else if (isValidCompass(offHand)) {
            compassStack = offHand;
            compassType = getCompassType(offHand);
            LOGGER.info("Found valid compass in off hand: {}", compassType);
        }
        
        if (compassStack == null || compassType == null) {
            // Silent failure - no feedback
            return;
        }

        // Extract coordinates from compass using a robust approach
        Pair<Integer, Integer> coordinates = extractCoordinates(compassStack, player);
        
        if (coordinates == null) {
            // Silent failure - no feedback
            return;
        }

        // If we're on the client side, create a request for the server
        if (player.level().isClientSide) {
            TeleportRequest request = new TeleportRequest(player, coordinates.getLeft(), coordinates.getRight());
            pendingRequests.put(player.getUUID(), request);
            // Silent request - no feedback
        } else {
            // Server side - consume energy and start teleportation
            if (dislocator.consumeEnergyForTeleportation(dislocatorStack)) {
                startServerTeleportation(player, coordinates.getLeft(), coordinates.getRight());
            }
        }
    }
    
    /**
     * Starts the teleportation process (can be called from client or server)
     * @deprecated Use startServerTeleportation instead
     */
    @Deprecated
    public static void startTeleportation(Player player, int targetX, int targetZ) {
        LOGGER.info("Legacy startTeleportation called for player {} to coordinates {}, {}", 
            player.getName().getString(), targetX, targetZ);
        
        if (player.level().isClientSide) {
            // Client side - create request
            TeleportRequest request = new TeleportRequest(player, targetX, targetZ);
            pendingRequests.put(player.getUUID(), request);
        } else {
            // Server side - start directly
            startServerTeleportation(player, targetX, targetZ);
        }
    }
    
    /**
     * Extracts coordinates using reflection on DataComponents
     */
    private static Pair<Integer, Integer> extractCoordinates(ItemStack compass, Player player) {
        String compassType = compass.getItem().toString();
        
        var components = compass.getComponents();
        
        try {
            // Method 1: Try to read from CustomData (traditional NBT)
            if (compass.has(DataComponents.CUSTOM_DATA)) {
                var customData = compass.get(DataComponents.CUSTOM_DATA);
                var nbt = customData.copyTag();
                
                if (compassType.contains("naturescompass")) {
                    if (nbt.contains("naturescompass:found_x") && nbt.contains("naturescompass:found_z")) {
                        int x = nbt.getInt("naturescompass:found_x");
                        int z = nbt.getInt("naturescompass:found_z");
                        return Pair.of(x, z);
                    }
                } else if (compassType.contains("explorerscompass")) {
                    if (nbt.contains("explorerscompass:found_x") && nbt.contains("explorerscompass:found_z")) {
                        int x = nbt.getInt("explorerscompass:found_x");
                        int z = nbt.getInt("explorerscompass:found_z");
                        return Pair.of(x, z);
                    }
                }
            }
            
            // Method 2: Read directly from DataComponents
            Integer foundX = null;
            Integer foundZ = null;
            
            // Try to extract values by converting from Object to Integer
            for (var entry : components) {
                String componentKey = entry.type().toString();
                Object componentValue = entry.value();
                
                if (compassType.contains("naturescompass")) {
                    if (componentKey.contains("found_x") && componentValue instanceof Integer) {
                        foundX = (Integer) componentValue;
                    }
                    if (componentKey.contains("found_z") && componentValue instanceof Integer) {
                        foundZ = (Integer) componentValue;
                    }
                } else if (compassType.contains("explorerscompass")) {
                    if (componentKey.contains("found_x") && componentValue instanceof Integer) {
                        foundX = (Integer) componentValue;
                    }
                    if (componentKey.contains("found_z") && componentValue instanceof Integer) {
                        foundZ = (Integer) componentValue;
                    }
                }
            }
            
            // If we found both coordinates, return them
            if (foundX != null && foundZ != null) {
                return Pair.of(foundX, foundZ);
            }
            
            // Method 3: toString() parsing as fallback
            String compassString = compass.toString();
            
            // More flexible patterns that handle both possible orders
            Pattern patternXZ, patternZX;
            if (compassType.contains("naturescompass")) {
                // Pattern for X before Z
                patternXZ = Pattern.compile("naturescompass:found_x=(-?\\d+).*?naturescompass:found_z=(-?\\d+)");
                // Pattern for Z before X
                patternZX = Pattern.compile("naturescompass:found_z=(-?\\d+).*?naturescompass:found_x=(-?\\d+)");
            } else if (compassType.contains("explorerscompass")) {
                // Pattern for X before Z
                patternXZ = Pattern.compile("explorerscompass:found_x=(-?\\d+).*?explorerscompass:found_z=(-?\\d+)");
                // Pattern for Z before X
                patternZX = Pattern.compile("explorerscompass:found_z=(-?\\d+).*?explorerscompass:found_x=(-?\\d+)");
            } else {
                return null;
            }
            
            // Try X-Z pattern first
            Matcher matcherXZ = patternXZ.matcher(compassString);
            if (matcherXZ.find()) {
                int x = Integer.parseInt(matcherXZ.group(1));
                int z = Integer.parseInt(matcherXZ.group(2));
                return Pair.of(x, z);
            }
            
            // If not found, try Z-X pattern
            Matcher matcherZX = patternZX.matcher(compassString);
            if (matcherZX.find()) {
                int z = Integer.parseInt(matcherZX.group(1));  // First group is Z
                int x = Integer.parseInt(matcherZX.group(2));  // Second group is X
                return Pair.of(x, z);
            }
            
        } catch (Exception e) {
            // Silent failure
        }
        
        return null;
    }

    /**
     * Checks if the ItemStack is a valid compass
     */
    private static boolean isValidCompass(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemId.toString().equals("naturescompass:naturescompass") || 
               itemId.toString().equals("explorerscompass:explorerscompass");
    }

    /**
     * Gets the compass type from the ItemStack
     */
    private static String getCompassType(ItemStack stack) {
        if (stack.isEmpty()) return null;
        
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String itemIdString = itemId.toString();
        
        if (itemIdString.equals("naturescompass:naturescompass")) {
            return "naturescompass";
        } else if (itemIdString.equals("explorerscompass:explorerscompass")) {
            return "explorerscompass";
        }
        return null;
    }
    
    // ===== ENERGY MANAGEMENT METHODS =====
    
    /**
     * Check if the item can store energy
     * If consumption is exactly 0, disable energy system completely
     */
    public boolean canStoreEnergy() {
        // If consumption is exactly 0, disable energy system completely
        if (Config.portableDislocatorEnergyConsume == 0) {
            return false;
        }
        
        return Config.portableDislocatorEnergyCapacity > 0;
    }
    
    /**
     * Gets the effective energy consumption, limited by capacity
     */
    public int getEffectiveEnergyConsumption() {
        if (!canStoreEnergy()) {
            return 0;
        }
        
        int configuredConsumption = Config.portableDislocatorEnergyConsume;
        int maxCapacity = Config.portableDislocatorEnergyCapacity;
        
        // If consumption is greater than capacity, limit it to capacity
        if (configuredConsumption > maxCapacity) {
            return maxCapacity;
        }
        
        return configuredConsumption;
    }
    
    /**
     * Check if the item requires energy to function
     */
    public boolean requiresEnergyToFunction() {
        return getEffectiveEnergyConsumption() > 0;
    }
    
    /**
     * Gets the energy stored in the ItemStack
     */
    public int getEnergyStored(ItemStack stack) {
        if (!canStoreEnergy()) {
            return 0;
        }
        
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getInt(ENERGY_TAG);
    }
    
    /**
     * Sets the energy stored in the ItemStack
     */
    public void setEnergyStored(ItemStack stack, int energy) {
        if (!canStoreEnergy()) {
            return;
        }
        
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int maxCapacity = Config.portableDislocatorEnergyCapacity;
        tag.putInt(ENERGY_TAG, Math.max(0, Math.min(energy, maxCapacity)));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    
    /**
     * Gets the maximum energy that can be stored
     */
    public int getMaxEnergyStored(ItemStack stack) {
        if (!canStoreEnergy()) {
            return 0;
        }
        return Config.portableDislocatorEnergyCapacity;
    }
    
    /**
     * Checks if the dislocator has enough energy for teleportation
     */
    public boolean hasEnoughEnergy(ItemStack stack) {
        if (!requiresEnergyToFunction()) {
            return true; // No energy required
        }
        
        int currentEnergy = getEnergyStored(stack);
        return currentEnergy >= getEffectiveEnergyConsumption();
    }
    
    /**
     * Consumes energy for teleportation
     * @param stack The ItemStack
     * @return true if energy was consumed or no energy is required, false if insufficient energy
     */
    public boolean consumeEnergyForTeleportation(ItemStack stack) {
        if (!requiresEnergyToFunction()) {
            return true; // No energy required
        }
        
        int consumption = getEffectiveEnergyConsumption();
        if (consumption <= 0) {
            return true; // No consumption
        }
        
        int currentEnergy = getEnergyStored(stack);
        if (currentEnergy >= consumption) {
            setEnergyStored(stack, currentEnergy - consumption);
            return true;
        }
        
        return false; // Insufficient energy
    }
    
    /**
     * Finds the Portable Dislocator in player's inventory or curios
     * @param player The player to check
     * @return ItemStack of the Portable Dislocator if found, null otherwise
     */
    public static ItemStack findPortableDislocator(Player player) {
        // Check hands first
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof PortableDislocatorItem) {
            return mainHand;
        }
        
        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof PortableDislocatorItem) {
            return offHand;
        }
        
        // Check inventory
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof PortableDislocatorItem) {
                return stack;
            }
        }
        
        // TODO: Check Curios slots if needed
        
        return null;
    }
} 