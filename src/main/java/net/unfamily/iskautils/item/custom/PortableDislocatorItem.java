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
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.util.ModUtils;
import net.unfamily.iskautils.block.ModBlocks;

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
        
        // Get the keybind name
        String keybindName = KeyBindings.PORTABLE_DISLOCATOR_KEY.getTranslatedKeyMessage().getString();
        
        // Add main tooltip
        tooltipComponents.add(Component.translatable("item.iska_utils.portable_dislocator.tooltip.main", keybindName));
        
        // Add compass info
        tooltipComponents.add(Component.translatable("item.iska_utils.portable_dislocator.tooltip.compasses"));
        
        // Energy information
        if (canStoreEnergy()) {
            int energy = getEnergyStored(stack);
            int maxEnergy = getMaxEnergyStored(stack);
            float percentage = (float) energy / Math.max(1, maxEnergy) * 100f;
            
            String energyString = String.format("%,d / %,d RF (%.1f%%)", energy, maxEnergy, percentage);
            Component energyText = Component.translatable("item.iska_utils.portable_dislocator.tooltip.energy")
                .withStyle(style -> style.withColor(ChatFormatting.RED))
                .append(Component.literal(energyString).withStyle(ChatFormatting.RED));
            
            tooltipComponents.add(energyText);
            
            // If energy is enabled but we also have XP consumption as backup
            if (Config.portableDislocatorXpConsume > 0) {
                Component xpBackupText = Component.translatable(
                    "item.iska_utils.portable_dislocator.tooltip.xp_backup", 
                    Config.portableDislocatorXpConsume)
                    .withStyle(style -> style.withColor(ChatFormatting.GREEN));
                tooltipComponents.add(xpBackupText);
            }
        } else if (Config.portableDislocatorXpConsume > 0) {
            // Energy is disabled but XP consumption is enabled
            Component xpText = Component.translatable(
                "item.iska_utils.portable_dislocator.tooltip.consumes_xp", 
                Config.portableDislocatorXpConsume)
                .withStyle(style -> style.withColor(ChatFormatting.GREEN));
            tooltipComponents.add(xpText);
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
                checkForTeleportRequest(player, stack, level);
            }
        }
    }
    
    /**
     * Tick method for the item in normal inventory - handles keybind when item is present
     */
    private void tickInInventory(ItemStack stack, net.minecraft.world.level.Level level, Player player, int slotId, boolean isSelected) {
        // Check if the portable dislocator keybind was pressed
        if (KeyBindings.consumeDislocatorKeyClick()) {
            LOGGER.info("Dislocator key pressed while in inventory for player {}", player.getName().getString());
            handleDislocatorActivation(player, stack, "inventory");
        }
    }
    
    /**
     * Tick method for Curios - called by the Curio handler
     */
    public static void tickInCurios(ItemStack stack, net.minecraft.world.level.Level level, Player player) {
        // Check if the portable dislocator keybind was pressed (client side)
        if (level.isClientSide && KeyBindings.PORTABLE_DISLOCATOR_KEY.consumeClick()) {
            handleDislocatorActivation(player, stack, "curios");
        }
        
        // Handle pending teleportation (server side)
        if (!level.isClientSide) {
            handlePendingTeleportation(player, level);
            checkForTeleportRequest(player, stack, level);
        }
    }
    
    /**
     * Checks for teleport requests from client side
     */
    public static void checkForTeleportRequest(Player player, ItemStack stack, Level level) {
        UUID playerId = player.getUUID();
        TeleportRequest request = pendingRequests.get(playerId);
        
        if (request != null && level instanceof ServerLevel) {
            LOGGER.info("Processing teleport request from client for player {} to {}, {}", 
                player.getName().getString(), request.targetX, request.targetZ);
            
            // Clear the request
            pendingRequests.remove(playerId);
            
            // Start the server-side teleportation using the provided stack
            startServerTeleportation(player, stack, request.targetX, request.targetZ);
        }
    }
    
    /**
     * Starts server-side teleportation
     */
    private static void startServerTeleportation(Player player, ItemStack dislocatorStack, int targetX, int targetZ) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        // Verify the provided stack is a Portable Dislocator
        if (!(dislocatorStack.getItem() instanceof PortableDislocatorItem)) {
            LOGGER.warn("Provided ItemStack is not a Portable Dislocator during teleportation");
            return;
        }
        
        PortableDislocatorItem dislocator = (PortableDislocatorItem) dislocatorStack.getItem();
        
        // Check resources based on priority settings
        boolean canTeleport = false;
        boolean energyEnabled = dislocator.canStoreEnergy() && dislocator.requiresEnergyToFunction();
        boolean xpEnabled = Config.portableDislocatorXpConsume > 0;
        boolean hasEnoughEnergy = energyEnabled && dislocator.hasEnoughEnergy(dislocatorStack);
        boolean hasEnoughXp = xpEnabled && dislocator.hasEnoughXp(player);
        
        // Get priority settings
        boolean prioritizeEnergy = Config.portableDislocatorPrioritizeEnergy;
        boolean prioritizeXp = Config.portableDislocatorPrioritizeXp;
        
        // Check if both priorities are enabled (consume both resources)
        if (prioritizeEnergy && prioritizeXp) {
            // Both resources must be available to teleport
            if (hasEnoughEnergy && hasEnoughXp) {
                boolean energyConsumed = dislocator.consumeEnergyForTeleportation(dislocatorStack);
                boolean xpConsumed = dislocator.consumeXpForTeleportation(player);
                
                LOGGER.info("Consumed energy (dual mode): {}", dislocator.getEffectiveEnergyConsumption());
                LOGGER.info("Consumed XP (dual mode): {}", Config.portableDislocatorXpConsume);
                
                if (energyConsumed && xpConsumed) {
                    player.displayClientMessage(
                        Component.translatable("item.iska_utils.portable_dislocator.message.used_both", 
                                             dislocator.getEffectiveEnergyConsumption(),
                                             Config.portableDislocatorXpConsume)
                                .withStyle(ChatFormatting.GOLD), 
                        true);
                }
                
                // Can teleport only if both resources were consumed
                canTeleport = energyConsumed && xpConsumed;
            } else {
                // Not enough of both resources, cannot teleport
                canTeleport = false;
            }
        }
        // Energy has priority
        else if (prioritizeEnergy) {
            if (hasEnoughEnergy) {
                canTeleport = dislocator.consumeEnergyForTeleportation(dislocatorStack);
                LOGGER.info("Used energy with priority: {}", dislocator.getEffectiveEnergyConsumption());
            } 
            // If energy priority but no energy, try XP
            else if (hasEnoughXp) {
                canTeleport = dislocator.consumeXpForTeleportation(player);
                LOGGER.info("Energy prioritized but not available, used XP: {}", Config.portableDislocatorXpConsume);
                
                if (canTeleport) {
                    player.displayClientMessage(
                        Component.translatable("item.iska_utils.portable_dislocator.message.used_xp", 
                                             Config.portableDislocatorXpConsume)
                                .withStyle(ChatFormatting.GOLD), 
                        true);
                }
            }
        }
        // XP has priority
        else if (prioritizeXp) {
            if (hasEnoughXp) {
                canTeleport = dislocator.consumeXpForTeleportation(player);
                LOGGER.info("Used XP with priority: {}", Config.portableDislocatorXpConsume);
                
                if (canTeleport) {
                    player.displayClientMessage(
                        Component.translatable("item.iska_utils.portable_dislocator.message.used_xp", 
                                             Config.portableDislocatorXpConsume)
                                .withStyle(ChatFormatting.GOLD), 
                        true);
                }
            } 
            // If XP priority but no XP, try energy
            else if (hasEnoughEnergy) {
                canTeleport = dislocator.consumeEnergyForTeleportation(dislocatorStack);
                LOGGER.info("XP prioritized but not available, used energy: {}", dislocator.getEffectiveEnergyConsumption());
            }
        }
        // No priority set (default to energy first, then XP)
        else {
            if (energyEnabled) {
                if (hasEnoughEnergy) {
                    canTeleport = dislocator.consumeEnergyForTeleportation(dislocatorStack);
                    LOGGER.info("Default priority - used energy: {}", dislocator.getEffectiveEnergyConsumption());
                } else if (xpEnabled && hasEnoughXp) {
                    canTeleport = dislocator.consumeXpForTeleportation(player);
                    LOGGER.info("Default priority - energy not available, used XP: {}", Config.portableDislocatorXpConsume);
                    
                    if (canTeleport) {
                        player.displayClientMessage(
                            Component.translatable("item.iska_utils.portable_dislocator.message.used_xp", 
                                                 Config.portableDislocatorXpConsume)
                                    .withStyle(ChatFormatting.GOLD), 
                            true);
                    }
                }
            } else if (xpEnabled && hasEnoughXp) {
                canTeleport = dislocator.consumeXpForTeleportation(player);
                LOGGER.info("Default priority - energy disabled, used XP: {}", Config.portableDislocatorXpConsume);
                
                if (canTeleport) {
                    player.displayClientMessage(
                        Component.translatable("item.iska_utils.portable_dislocator.message.used_xp", 
                                             Config.portableDislocatorXpConsume)
                                .withStyle(ChatFormatting.GOLD), 
                        true);
                }
            } else if (!energyEnabled && !xpEnabled) {
                // Both systems disabled, teleport is free
                canTeleport = true;
                LOGGER.info("Both energy and XP disabled, teleport is free");
            }
        }
        
        if (!canTeleport) {
            // Check which resource is depleted and notify player
            if (energyEnabled && !hasEnoughEnergy) {
                player.displayClientMessage(
                    Component.translatable("item.iska_utils.portable_dislocator.message.no_energy")
                            .withStyle(ChatFormatting.RED), 
                    true);
            } 
            if (xpEnabled && !hasEnoughXp) {
                player.displayClientMessage(
                    Component.translatable("item.iska_utils.portable_dislocator.message.no_xp")
                            .withStyle(ChatFormatting.RED), 
                    true);
            }
            if (!energyEnabled && !xpEnabled) {
                LOGGER.warn("Unexpected state: teleportation failed but no resources are required");
            }
            return;
        }
        
        // Generate random offset to teleport 100-150 blocks away from the target
        java.util.Random random = new java.util.Random();
        
        // Generate random offset for X: either [-150, -100] or [100, 150]
        int offsetX;
        if (random.nextBoolean()) {
            // Positive range: 100 to 150
            offsetX = random.nextInt(51) + 100; // 100-150
        } else {
            // Negative range: -150 to -100
            offsetX = -(random.nextInt(51) + 100); // -150 to -100
        }
        
        // Generate random offset for Z: either [-150, -100] or [100, 150]
        int offsetZ;
        if (random.nextBoolean()) {
            // Positive range: 100 to 150
            offsetZ = random.nextInt(51) + 100; // 100-150
        } else {
            // Negative range: -150 to -100
            offsetZ = -(random.nextInt(51) + 100); // -150 to -100
        }
        
        // Apply offset to the original coordinates
        int randomizedX = targetX + offsetX;
        int randomizedZ = targetZ + offsetZ;
        
        // Log the coordinates for debugging
        LOGGER.info("Teleporting from original coordinates {}, {} to randomized coordinates {}, {} (offset: {}, {})",
            targetX, targetZ, randomizedX, randomizedZ, offsetX, offsetZ);
        
        // Clear any existing teleportation for this player
        UUID playerId = player.getUUID();
        resetTeleportationState(playerId);
        
        // Store original position for alternative attempts if needed
        BlockPos playerPos = player.blockPosition();
        
        // Start teleportation process with randomized coordinates
        TeleportationData teleportData = new TeleportationData(player, randomizedX, randomizedZ);
        teleportData.originalX = targetX; // Keep original coordinates for reference
        teleportData.originalZ = targetZ;
        activeTeleportations.put(playerId, teleportData);
        
        // Notify player
        player.displayClientMessage(
            Component.translatable("item.iska_utils.portable_dislocator.message.teleporting"), 
            true);
    }
    
    /**
     * Handles pending teleportation process
     */
    public static void handlePendingTeleportation(Player player, Level level) {
        UUID playerId = player.getUUID();
        TeleportationData data = activeTeleportations.get(playerId);
        
        if (data == null) return;
        
        data.ticksWaiting++;
        
        // Log progress every second (20 ticks)
        if (data.ticksWaiting % 20 == 0) {
            LOGGER.info("Waiting for teleportation: player={}, target={},{}, attempt={}, waiting={}",
                player.getName().getString(), data.targetX, data.targetZ, data.attemptCount, data.ticksWaiting);
        }
        
        // Timeout after max wait time
        if (data.ticksWaiting > MAX_WAIT_TICKS) {
            LOGGER.info("Teleportation timed out after {} ticks, attempt {} of {}",
                data.ticksWaiting, data.attemptCount, MAX_ATTEMPTS);
                
            // Try another attempt if we haven't exceeded max attempts
            if (data.attemptCount < MAX_ATTEMPTS) {
                attemptNewTeleportation(player, data.originalX, data.originalZ, data.attemptCount + 1);
            } else {
                // All attempts failed, give up
                resetTeleportationState(playerId);
                
                // Notify player about the failure
                player.displayClientMessage(
                    Component.translatable("item.iska_utils.portable_dislocator.message.failed")
                        .withStyle(ChatFormatting.RED), 
                    true);
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
                            
                            // Verifichiamo se ci troviamo sopra all'acqua o se c'è una raft
                            BlockPos waterCheckPos = new BlockPos(data.targetX, safeY - 1, data.targetZ);
                            BlockPos raftCheckPos = new BlockPos(data.targetX, safeY, data.targetZ);
                            BlockState belowState = level.getBlockState(waterCheckPos);
                            BlockState currentState = level.getBlockState(raftCheckPos);
                            
                            boolean isWater = belowState.is(net.minecraft.world.level.block.Blocks.WATER) ||
                                             belowState.getFluidState().is(net.minecraft.world.level.material.Fluids.WATER) ||
                                             belowState.getFluidState().is(net.minecraft.world.level.material.Fluids.FLOWING_WATER);
                            boolean isRaft = currentState.is(ModBlocks.RAFT.get());
                            
                            // Offset Y per posizionare il giocatore correttamente
                            double yOffset = 0;
                            int teleportY = safeY;
                            
                            if (isRaft) {
                                // Se c'è una raft, posiziona il giocatore sopra di essa
                                yOffset = 0.6;
                                teleportY = safeY; // La raft è alla stessa Y del safeY
                            } else if (isWater) {
                                // Se c'è acqua, posiziona il giocatore alla Y originale
                                yOffset = 0;
                            }
                            
                            // Teleport to safe position
                            player.teleportTo(data.targetX + 0.5, teleportY + yOffset, data.targetZ + 0.5);
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
        
        // Find the dislocator item
        ItemStack dislocatorStack = findPortableDislocator(player);
        if (dislocatorStack == null) {
            LOGGER.warn("Portable Dislocator not found during retry attempt");
            return;
        }
        
        // Generate new coordinates with the same fixed distance range for all attempts
        java.util.Random random = new java.util.Random();
        
        // Fixed range for all attempts: 100-150 blocks
        int minRange = 100;
        int maxRange = 150;
        
        // Generate random offset for X: either [-150, -100] or [100, 150]
        int offsetX;
        if (random.nextBoolean()) {
            // Positive range: 100 to 150
            offsetX = random.nextInt(maxRange - minRange + 1) + minRange;
        } else {
            // Negative range: -150 to -100
            offsetX = -(random.nextInt(maxRange - minRange + 1) + minRange);
        }
        
        // Generate random offset for Z: either [-150, -100] or [100, 150]
        int offsetZ;
        if (random.nextBoolean()) {
            // Positive range: 100 to 150
            offsetZ = random.nextInt(maxRange - minRange + 1) + minRange;
        } else {
            // Negative range: -150 to -100
            offsetZ = -(random.nextInt(maxRange - minRange + 1) + minRange);
        }
        
        int newX = originalX + offsetX;
        int newZ = originalZ + offsetZ;
        
        LOGGER.info("Retry attempt {}: Teleporting to new coordinates {}, {} (offset {}, {} from original)",
            attemptNumber, newX, newZ, offsetX, offsetZ);
        
        // Create new teleportation data for this attempt
        TeleportationData newData = new TeleportationData(player, newX, newZ);
        newData.originalX = originalX;
        newData.originalZ = originalZ;
        newData.attemptCount = attemptNumber;
        
        // Store the new attempt and prepare for teleportation
        activeTeleportations.put(playerId, newData);
        
        // No need to check energy again for retry attempts - it was already consumed in the first attempt
        
        // Notify player about the retry
        player.displayClientMessage(
            Component.translatable("item.iska_utils.portable_dislocator.message.retry", attemptNumber)
                .withStyle(ChatFormatting.YELLOW), 
            true);
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
     * Prepares the ground for teleportation, placing raft if needed
     */
    private static void prepareGround(Level level, int x, int finalY, int z) {
        BlockPos groundPos = new BlockPos(x, finalY - 1, z);
        BlockState groundState = level.getBlockState(groundPos);
        
        // If ground is water, place a raft
        if (!groundState.isSolid() && !groundState.isAir()) {
            boolean isLiquid = !groundState.getFluidState().isEmpty();
            if (isLiquid) {
                // Only replace water with raft - other liquids should not reach this point
                boolean isWater = groundState.is(net.minecraft.world.level.block.Blocks.WATER) ||
                                 groundState.getFluidState().is(net.minecraft.world.level.material.Fluids.WATER) ||
                                 groundState.getFluidState().is(net.minecraft.world.level.material.Fluids.FLOWING_WATER);
                
                if (isWater) {
                    // Piazziamo la raft un blocco più in alto rispetto all'acqua
                    BlockPos raftPos = new BlockPos(x, finalY, z);
                    // Controlliamo che ci sia spazio per la raft
                    if (level.getBlockState(raftPos).isAir()) {
                        level.setBlock(raftPos, ModBlocks.RAFT.get().defaultBlockState(), 3);
                    }
                    
                    // Note: L'offset Y verrà gestito direttamente nel metodo handlePendingTeleportation
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
    public static void handleDislocatorActivation(Player player, ItemStack dislocatorStack, String source) {
        LOGGER.info("handleDislocatorActivation called from {} for player {}", source, player.getName().getString());
        
        // Prevent activation if already teleporting
        UUID playerId = player.getUUID();
        TeleportationData data = activeTeleportations.get(playerId);
        if (data != null) {
            LOGGER.info("Aborting activation: teleportation already in progress");
            return;
        }
        
        // Verify the provided ItemStack is a Portable Dislocator
        if (!(dislocatorStack.getItem() instanceof PortableDislocatorItem)) {
            LOGGER.info("Aborting activation: provided item is not a Portable Dislocator");
            return;
        }
        
        LOGGER.info("Using dislocator from {}: {}", source, dislocatorStack);
        
        // Check energy requirements
        PortableDislocatorItem dislocator = (PortableDislocatorItem) dislocatorStack.getItem();
        
        // We no longer check energy here as it will be checked in startServerTeleportation
        
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
            // Server side - start teleportation with the provided dislocator stack
            startServerTeleportation(player, dislocatorStack, coordinates.getLeft(), coordinates.getRight());
        }
    }
    
    /**
     * Starts the teleportation process (can be called from client or server)
     * Compatibility version that finds the dislocator in player's inventory
     * @deprecated Use version with explicit ItemStack instead
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
            // Server side - find the dislocator and start directly
            ItemStack dislocatorStack = findPortableDislocator(player);
            if (dislocatorStack != null) {
                startServerTeleportation(player, dislocatorStack, targetX, targetZ);
            } else {
                LOGGER.warn("Could not find Portable Dislocator for player {} in legacy teleportation", 
                    player.getName().getString());
            }
        }
    }
    
    /**
     * Starts the teleportation process (can be called from client or server)
     * @deprecated Use startServerTeleportation instead
     */
    @Deprecated
    public static void startTeleportation(Player player, ItemStack dislocatorStack, int targetX, int targetZ) {
        LOGGER.info("Legacy startTeleportation called for player {} to coordinates {}, {}", 
            player.getName().getString(), targetX, targetZ);
        
        if (player.level().isClientSide) {
            // Client side - create request
            TeleportRequest request = new TeleportRequest(player, targetX, targetZ);
            pendingRequests.put(player.getUUID(), request);
        } else {
            // Server side - start directly with the provided dislocator
            startServerTeleportation(player, dislocatorStack, targetX, targetZ);
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
        // If capacity is 0, disable energy system completely
        if (Config.portableDislocatorEnergyCapacity == 0) {
            return false;
        }
        
        return true;
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
     * Checks if player has enough XP for teleportation
     * @param player The player
     * @return true if player has enough XP or no XP is required
     */
    public boolean hasEnoughXp(Player player) {
        if (Config.portableDislocatorXpConsume <= 0) {
            return true; // No XP required
        }
        
        return player.totalExperience >= Config.portableDislocatorXpConsume || 
               player.experienceLevel > 0;
    }
    
    /**
     * Consumes XP from player for teleportation
     * @param player The player
     * @return true if XP was consumed or no XP is required, false if insufficient XP
     */
    public boolean consumeXpForTeleportation(Player player) {
        if (Config.portableDislocatorXpConsume <= 0) {
            return true; // No XP required
        }
        
        if (player.getAbilities().instabuild) {
            return true; // Creative mode - no XP consumption
        }
        
        int xpToConsume = Config.portableDislocatorXpConsume;
        
        // Verifichiamo se il giocatore ha abbastanza esperienza (punti totali o livelli convertibili)
        // L'XP totale è in punti, quindi controlliamo sia quello che i livelli
        if (player.totalExperience >= xpToConsume || player.experienceLevel > 0) {
            // Lasciamo che Minecraft gestisca la conversione tra livelli e punti
            player.giveExperiencePoints(-xpToConsume);
            return true;
        }
        
        return false; // Insufficient XP
    }
    
    /**
     * Finds the Portable Dislocator in player's inventory or curios
     * @param player The player to check
     * @return ItemStack of the Portable Dislocator if found, null otherwise
     */
    public static ItemStack findPortableDislocator(Player player) {
        // Check hands first (highest priority)
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof PortableDislocatorItem) {
            return mainHand;
        }
        
        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof PortableDislocatorItem) {
            return offHand;
        }
        
        // If Curios is loaded, check Curios slots (second priority)
        if (ModUtils.isCuriosLoaded()) {
            ItemStack curioDislocator = checkCuriosSlots(player);
            if (curioDislocator != null) {
                return curioDislocator;
            }
        }
        
        // Check player inventory (lowest priority)
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof PortableDislocatorItem) {
                return stack;
            }
        }
        
        return null;
    }
    
    /**
     * Uses reflection to check if the Portable Dislocator is equipped in a Curios slot
     */
    private static ItemStack checkCuriosSlots(Player player) {
        try {
            // Approccio alternativo che usa getCuriosHandler invece di getAllEquipped
            Class<?> curioApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            
            // Ottiene l'handler delle Curios per il player
            Method getCuriosHandlerMethod = curioApiClass.getMethod("getCuriosHelper");
            Object curiosHelper = getCuriosHandlerMethod.invoke(null);
            
            Method getEquippedCurios = curiosHelper.getClass().getMethod("getEquippedCurios", LivingEntity.class);
            Object equippedCurios = getEquippedCurios.invoke(curiosHelper, player);
            
            if (equippedCurios instanceof Iterable<?> items) {
                for (Object itemPair : items) {
                    // Extract stack from each pair using getRight method
                    Method getStackMethod = itemPair.getClass().getMethod("getRight");
                    ItemStack stack = (ItemStack) getStackMethod.invoke(itemPair);
                    
                    if (stack.getItem() instanceof PortableDislocatorItem) {
                        return stack;
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            // Se c'è un errore di reflection, log e continua
            LOGGER.warn("Error checking Curios slots: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
            return null;
        }
    }
} 