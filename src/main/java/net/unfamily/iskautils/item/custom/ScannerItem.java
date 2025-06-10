package net.unfamily.iskautils.item.custom;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.Config;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import net.minecraft.world.item.UseAnim;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Item for scanning specific blocks in an area
 */
public class ScannerItem extends Item {
    private static final Logger LOGGER = LogUtils.getLogger();
 
    private static final String TARGET_BLOCK_TAG = "TargetBlock";
    private static final String TARGET_MOB_TAG = "TargetMob";
    private static final String SCANNER_ID_TAG = "ScannerId";
    private static final String CLEAR_MARKERS_TAG = "ClearMarkers";
    private static final String ENERGY_TAG = "Energy"; 
    private boolean clear_markers=false;
    
    // Map to track active markers by scanner ID
    private static final Map<UUID, List<BlockPos>> ACTIVE_MARKERS = new HashMap<>();
    
    // Map to track TTL for each marker
    private static final Map<BlockPos, Integer> MARKER_TTL = new HashMap<>();

    private static final int LOADING_BAR_LENGTH = 15; // Number of blocks █ in the loading bar
    private static final Map<UUID, LoadingData> LOADING_DATA = new HashMap<>(); // Class to track loading data
    
    // Class to track loading data
    private static class LoadingData {
        private final long startTime;
        private int ticksUsed;
        private boolean displayingFeedback;
        private long lastUpdateTime;
        
        public LoadingData() {
            this.startTime = System.currentTimeMillis();
            this.ticksUsed = 0;
            this.displayingFeedback = false;
            this.lastUpdateTime = 0;
        }
        
        public void update(int ticksUsed) {
            this.ticksUsed = ticksUsed;
            this.displayingFeedback = true;
            this.lastUpdateTime = System.currentTimeMillis();
        }
        
        public boolean shouldHideFeedback() {
            return System.currentTimeMillis() - lastUpdateTime > 500; // Hide after 500ms from the last update
        }
    }

    public ScannerItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON)
                .fireResistant());
    }

    /**
     * Called when creating a new ItemStack of this item
     * Initializes the item with energy
     */
    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        if (canStoreEnergy()) {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            tag.putInt(ENERGY_TAG, 0);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return stack;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack itemStack = context.getItemInHand();
        BlockPos blockPos = context.getClickedPos();
        
        if (level.isClientSide || player == null) {
            return InteractionResult.SUCCESS;
        }
        
        // If player is crouching (Shift), register the target block
        if (player.isCrouching()) {
            BlockState state = level.getBlockState(blockPos);
            Block block = state.getBlock();
            
            if (block != Blocks.AIR) {
                // Register the target block in the scanner's NBT
                setTargetBlock(itemStack, block);
                
                player.displayClientMessage(Component.translatable("item.iska_utils.scanner.target_set", block.getName()), true);
                return InteractionResult.SUCCESS;
            }
        }
        
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        
        if (level.isClientSide) {
            return InteractionResultHolder.success(itemStack);
        }
        
        // Check if we have a target block or mob
        Block targetBlock = getTargetBlock(itemStack);
        String targetMob = getTargetMob(itemStack);
        
        if (targetBlock == null && targetMob == null) {
            player.displayClientMessage(Component.translatable("item.iska_utils.scanner.no_target"), true);
            return InteractionResultHolder.fail(itemStack);
        }
        
        // Check if we have enough energy for scanning (energy check only for scanning, not for clearing)
        if (requiresEnergyToFunction() && !hasEnoughEnergy(itemStack)) {
            player.displayClientMessage(Component.translatable("item.iska_utils.scanner.no_energy"), true);
            return InteractionResultHolder.fail(itemStack);
        }
        
        // Remove existing markers if scanning blocks
        if (targetBlock != null) {
            clearMarkers(player, itemStack);
        }
        
        // Set use duration
        player.startUsingItem(hand);
        
        return InteractionResultHolder.consume(itemStack);
    }
    
    public InteractionResultHolder<ItemStack> resetTarget(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        
        if (level.isClientSide) {
            return InteractionResultHolder.success(itemStack);
        }
        
        // Check for target block or mob
        Block targetBlock = getTargetBlock(itemStack);
        String targetMob = getTargetMob(itemStack);
        
        if (targetBlock == null && targetMob == null) {
            player.displayClientMessage(Component.translatable("item.iska_utils.scanner.no_target"), true);
            return InteractionResultHolder.fail(itemStack);
        }
        
        // Remove existing markers and reset targets
        clearMarkersAndResetTarget(player instanceof ServerPlayer ? (ServerPlayer) player : null, itemStack);
        
        player.displayClientMessage(Component.translatable("item.iska_utils.scanner.target_reset"), true);
        
        return InteractionResultHolder.consume(itemStack);
    }
    

    @Override
	public UseAnim getUseAnimation(ItemStack itemstack) {
		return UseAnim.BOW;
	}

	@Override
	public int getUseDuration(ItemStack itemstack, LivingEntity livingEntity) {
		return 300;
	}

    @Override
    public void releaseUsing(ItemStack itemstack, Level world, LivingEntity entity, int time) {
        // Rimuovi i dati di caricamento
        if (entity instanceof Player player) {
            LOADING_DATA.remove(player.getUUID());
        }
        
        if (world.isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }
        
        // Check if item was held for enough time
        Block targetBlock = getTargetBlock(itemstack);
        String targetMob = getTargetMob(itemstack);
        
        if (targetBlock == null && targetMob == null) {
            player.displayClientMessage(Component.translatable("item.iska_utils.scanner.no_target"), true);
            return;
        }
        
        // The time parameter represents remaining time, so we need to check if it's LESS than a certain value
        // to determine if user has held long enough
        int totalDuration = getUseDuration(itemstack, entity);
        int timeUsed = totalDuration - time;
        int requiredDuration = Config.scannerScanDuration;
        
        if (timeUsed >= requiredDuration) {
            // Double-check energy before scanning
            if (requiresEnergyToFunction() && !hasEnoughEnergy(itemstack)) {
                player.displayClientMessage(Component.translatable("item.iska_utils.scanner.no_energy"), true);
                return;
            }
            
            boolean scanSuccess = false;
            
            if (targetBlock != null) {
                // Scan for blocks
                player.displayClientMessage(Component.translatable("item.iska_utils.scanner.scan_started", targetBlock.getName()), true);
                scanArea(player, itemstack);
                clear_markers = true;
                scanSuccess = true;
            } else if (targetMob != null) {
                // Scan for mobs
                String mobName = "entity.minecraft." + targetMob.substring(targetMob.indexOf(":") + 1);
                player.displayClientMessage(Component.translatable("item.iska_utils.scanner.scan_started_mob", Component.translatable(mobName)), true);
                scanForMobs(player, itemstack);
                clear_markers = true;
                scanSuccess = true;
            }
            
            // Consume energy if scan was successful
            if (scanSuccess) {
                consumeEnergyForOperation(itemstack);
                
                // Mostra barra completata
                if (!world.isClientSide) {
                    displayLoadingBar(player, requiredDuration, requiredDuration);
                }
            }
        } else if (clear_markers) {
            player.displayClientMessage(Component.translatable("item.iska_utils.scanner.interrupted"), true);
            resetTarget(player.level(), player, InteractionHand.MAIN_HAND);
            clear_markers = false;
        }
    }
    
    /**
     * Sets the target block in the scanner
     */
    private void setTargetBlock(ItemStack itemStack, Block block) {
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        
        // Remove any target mob
        tag.remove(TARGET_MOB_TAG);
        
        // Set target block
        tag.putString(TARGET_BLOCK_TAG, BuiltInRegistries.BLOCK.getKey(block).toString());
        
        // Make sure the scanner has a unique ID
        if (!tag.contains(SCANNER_ID_TAG)) {
            tag.putUUID(SCANNER_ID_TAG, UUID.randomUUID());
        }
        
        // Set clear_markers state
        tag.putBoolean(CLEAR_MARKERS_TAG, clear_markers);
        
        // Save data to ItemStack
        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * Gets the target block from the scanner
     */
    private Block getTargetBlock(ItemStack itemStack) {
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(TARGET_BLOCK_TAG)) {
            return null;
        }
        
        // Read clear_markers state
        if (tag.contains(CLEAR_MARKERS_TAG)) {
            clear_markers = tag.getBoolean(CLEAR_MARKERS_TAG);
        } else {
            clear_markers = false;
        }
        
        String blockId = tag.getString(TARGET_BLOCK_TAG);
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockId));
    }
    
    /**
     * Removes all existing markers for this scanner
     */
    private void clearMarkers(Player player, ItemStack itemStack) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(SCANNER_ID_TAG)) {
            UUID scannerId = tag.getUUID(SCANNER_ID_TAG);
            String scannerIdStr = scannerId.toString();
            String sessionId = net.unfamily.iskautils.util.SessionVariables.getScannerSessionId().toString();
            String scannerTag = "scanner_" + scannerIdStr;
            String sessionTag = "session_" + sessionId;
            
            try {
                // Prima identifica tutti i marker di questo scanner in questa sessione
                String tagCommand = String.format("execute as @e[type=block_display,tag=temp_scan,tag=%s,tag=%s] run tag @s add scanner_cleanup", 
                        scannerTag, sessionTag);
                serverPlayer.getServer().getCommands().performPrefixedCommand(
                    serverPlayer.getServer().createCommandSourceStack(),
                    tagCommand
                );
                
                // Rimuovi dal team
                String teamLeaveCommand = "team leave @e[type=block_display,tag=scanner_cleanup]";
                serverPlayer.getServer().getCommands().performPrefixedCommand(
                    serverPlayer.getServer().createCommandSourceStack(),
                    teamLeaveCommand
                );
                
                // Poi uccidi i marker
                String killCommand = "kill @e[type=block_display,tag=scanner_cleanup]";
                serverPlayer.getServer().getCommands().performPrefixedCommand(
                    serverPlayer.getServer().createCommandSourceStack(),
                    killCommand
                );
            } catch (Exception e) {
                LOGGER.error("Error clearing markers for scanner {}: {}", scannerIdStr, e.getMessage());
            }
            
            // Clear from our tracking maps
            if (ACTIVE_MARKERS.containsKey(scannerId)) {
                List<BlockPos> markers = ACTIVE_MARKERS.get(scannerId);
                for (BlockPos pos : markers) {
                    MARKER_TTL.remove(pos);
                }
                
                markers.clear();
            }
        }
    }
    
    /**
     * Gets the target mob from the scanner
     */
    private String getTargetMob(ItemStack itemStack) {
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        
        // Read clear_markers state if present
        if (tag.contains(CLEAR_MARKERS_TAG)) {
            clear_markers = tag.getBoolean(CLEAR_MARKERS_TAG);
        }
        
        if (!tag.contains(TARGET_MOB_TAG)) {
            LOGGER.debug("No target mob found in item");
            return null;
        }
        
        String mobId = tag.getString(TARGET_MOB_TAG);
        LOGGER.debug("Found target mob in item: {}", mobId);
        
        return mobId;
    }
    
    /**
     * Clears all markers and resets both block and mob targets
     */
    private void clearMarkersAndResetTarget(ServerPlayer player, ItemStack itemStack) {
        // Clear existing markers
        clearMarkers(player, itemStack);
        
        // Reset targets
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.remove(TARGET_BLOCK_TAG);
        tag.remove(TARGET_MOB_TAG);
        
        // Set clear_markers from NBT
        if (tag.contains(CLEAR_MARKERS_TAG)) {
            clear_markers = tag.getBoolean(CLEAR_MARKERS_TAG);
        } else {
            clear_markers = false;
        }
        
        // Save data to ItemStack
        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    
    /**
     * Scans the area for target blocks
     */
    private void scanArea(ServerPlayer player, ItemStack itemStack) {
        Block targetBlock = getTargetBlock(itemStack);
        if (targetBlock == null || player.level() == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        UUID scannerId = tag.getUUID(SCANNER_ID_TAG);
        
        // Remove any existing markers
        clearMarkers(player, itemStack);
        
        // Create a list for this scanner's markers
        List<BlockPos> scannerMarkers = new ArrayList<>();
        ACTIVE_MARKERS.put(scannerId, scannerMarkers);
        
        // Get player position
        BlockPos playerPos = player.blockPosition();
        ChunkPos playerChunkPos = new ChunkPos(playerPos);
        
        // Get config values
        int scanRange = Config.scannerScanRange;
        int maxBlocksScan = Config.scannerMaxBlocks;
        int maxTTL = 600;
        
        // Determina il numero di chunk da scansionare in base al raggio configurato
        // Ogni chunk è 16x16 blocchi, quindi calcoliamo quanti chunk dobbiamo controllare
        int chunkRadius = Math.max(1, scanRange / 16);
        
        // Scan in a radius based on the configured scan range
        int markersFound = 0;
        boolean limitReached = false;
        
        scanLoop:
        for (int chunkX = playerChunkPos.x - chunkRadius; chunkX <= playerChunkPos.x + chunkRadius; chunkX++) {
            for (int chunkZ = playerChunkPos.z - chunkRadius; chunkZ <= playerChunkPos.z + chunkRadius; chunkZ++) {
                ChunkPos currentChunkPos = new ChunkPos(chunkX, chunkZ);
                
                // Check if chunk is loaded
                if (!level.isLoaded(BlockPos.containing(currentChunkPos.getMiddleBlockX(), 0, currentChunkPos.getMiddleBlockZ()))) {
                    continue;
                }
                
                // Scan the chunk
                for (int x = currentChunkPos.getMinBlockX(); x <= currentChunkPos.getMaxBlockX(); x++) {
                    for (int z = currentChunkPos.getMinBlockZ(); z <= currentChunkPos.getMaxBlockZ(); z++) {
                        // Prioritize blocks closer to the player's Y level
                        int playerY = playerPos.getY();
                        
                        // Scan from player Y outward in both directions
                        for (int yOffset = 0; yOffset < level.getMaxBuildHeight() - level.getMinBuildHeight(); yOffset++) {
                            // Try above player first, then below
                            for (int yDir = 0; yDir <= 1; yDir++) {
                                int y = playerY + (yDir == 0 ? yOffset : -yOffset);
                                
                                // Skip if out of world bounds
                                if (y < level.getMinBuildHeight() || y > level.getMaxBuildHeight()) {
                                    continue;
                                }
                                
                            BlockPos pos = new BlockPos(x, y, z);
                            
                                // Check if it's too far from the player
                                if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > scanRange * scanRange) {
                                continue;
                            }
                            
                                // Check the block
                            if (level.getBlockState(pos).getBlock() == targetBlock) {
                                    // Create a marker
                                createMarker(player, pos);
                                scannerMarkers.add(pos);
                                markersFound++;
                                
                                    // Add TTL for this marker
                                    MARKER_TTL.put(pos, maxTTL);
                                    
                                    // Check if we've reached the limit
                                    if (markersFound >= maxBlocksScan) {
                                        limitReached = true;
                                        break scanLoop;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Component message;
        if (limitReached) {
            message = Component.translatable("item.iska_utils.scanner.found_blocks_limit", 
                    markersFound, targetBlock.getName(), maxBlocksScan);
        } else {
            message = Component.translatable("item.iska_utils.scanner.found_blocks", 
                    markersFound, targetBlock.getName());
        }
        
        player.displayClientMessage(message, true);
    }
    
    /**
     * Creates a marker at the specified position
     */
    private void createMarker(ServerPlayer player, BlockPos pos) {
        try {
            // Ensure the scan marker team exists
            ensureScanMarkerTeam(player.serverLevel());
            
            // Get scanner ID from the itemstack
            CompoundTag tag = player.getItemInHand(InteractionHand.MAIN_HAND)
                .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            String scannerId = tag.contains(SCANNER_ID_TAG) ? tag.getUUID(SCANNER_ID_TAG).toString() : "unknown";
            
            // Get the current session ID
            String sessionId = net.unfamily.iskautils.util.SessionVariables.getScannerSessionId().toString();
            
            // First create a marker for exact positioning
            player.getServer().getCommands().performPrefixedCommand(
                player.getServer().createCommandSourceStack(),
                String.format("summon minecraft:marker %d %d %d {Tags:[\"temp_scan_marker\"]}", pos.getX(), pos.getY(), pos.getZ())
            );
            
            // Use UUID for marker identification to ensure uniqueness
            String markerId = UUID.randomUUID().toString();
            
            // Then create a block_display at the block center with glowing effect, including scanner ID and session ID tags
            player.getServer().getCommands().performPrefixedCommand(
                player.getServer().createCommandSourceStack(),
                String.format("execute at @e[type=marker,tag=temp_scan_marker,limit=1] run summon block_display ~0.0 ~0.0 ~0.0 " +
                "{Tags:[\"temp_scan\",\"scanner_%s\",\"session_%s\",\"marker_%s\"],Glowing:1b,block_state:{Name:\"iska_utils:scan_block\"}," +
                "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.55f,-0.05f,-0.55f],scale:[1.1f,1.1f,1.1f]}}", 
                scannerId, sessionId, markerId)
            );
            
            // Add the just spawned block_display to the iska_utils_scan team using the unique marker ID
            player.getServer().getCommands().performPrefixedCommand(
                player.getServer().createCommandSourceStack(),
                String.format("team join iska_utils_scan @e[type=block_display,tag=temp_scan,tag=marker_%s]", markerId)
            );
            
            // Remove the marker
            player.getServer().getCommands().performPrefixedCommand(
                player.getServer().createCommandSourceStack(),
                "kill @e[type=marker,tag=temp_scan_marker]"
            );
        } catch (Exception e) {
            LOGGER.error("Error creating a marker: {}", e.getMessage());
        }
    }
    
    /**
     * Handles item tick (called every game tick)
     * Used to update markers and their TTL
     */
    public static void tick(ServerLevel level) {
        Iterator<Map.Entry<BlockPos, Integer>> markerIterator = MARKER_TTL.entrySet().iterator();
        
        while (markerIterator.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = markerIterator.next();
            BlockPos pos = entry.getKey();
            int ttl = entry.getValue() - 1;
            
            if (ttl <= 0) {
                // Remove expired marker
                try {
                    String command = String.format("kill @e[type=block_display,tag=temp_scan,x=%d,y=%d,z=%d,distance=..1]", 
                            pos.getX(), pos.getY(), pos.getZ());
                    level.getServer().getCommands().performPrefixedCommand(
                        level.getServer().createCommandSourceStack().withSuppressedOutput(),
                        command
                    );
                } catch (Exception e) {
                    LOGGER.error("Error removing an expired marker: {}", e.getMessage());
                }
                
                // Remove marker from TTL map
                markerIterator.remove();
                
                // Remove marker from all active scanners
                for (UUID scannerId : ACTIVE_MARKERS.keySet()) {
                    List<BlockPos> markers = ACTIVE_MARKERS.get(scannerId);
                    markers.remove(pos);
                }
            } else {
                // Update TTL
                entry.setValue(ttl);
            }
        }
    }
    
    /**
     * Ensures the scan marker team exists with the correct color
     */
    private static void ensureScanMarkerTeam(ServerLevel level) {
        try {
            // Perform team creation command
            String teamCommand = "team add iska_utils_scan \"Iska Utils Scan\"";
            level.getServer().getCommands().performPrefixedCommand(
                level.getServer().createCommandSourceStack().withSuppressedOutput(),
                teamCommand
            );

            // Set team color to aqua
            String colorCommand = "team modify iska_utils_scan color aqua";
            level.getServer().getCommands().performPrefixedCommand(
                level.getServer().createCommandSourceStack().withSuppressedOutput(),
                colorCommand
            );

            // Make team visible to all players
            String visibilityCommand = "team modify iska_utils_scan seeFriendlyInvisibles true";
            level.getServer().getCommands().performPrefixedCommand(
                level.getServer().createCommandSourceStack().withSuppressedOutput(),
                visibilityCommand
            );
            
            // Allow friendly fire
            String friendlyFireCommand = "team modify iska_utils_scan friendlyFire true";
            level.getServer().getCommands().performPrefixedCommand(
                level.getServer().createCommandSourceStack().withSuppressedOutput(),
                friendlyFireCommand
            );
            
            // Don't prevent members from hitting each other
            String collisionCommand = "team modify iska_utils_scan collisionRule never";
            level.getServer().getCommands().performPrefixedCommand(
                level.getServer().createCommandSourceStack().withSuppressedOutput(),
                collisionCommand
            );
        } catch (Exception e) {
            LOGGER.error("Error creating or updating scan marker team: {}", e.getMessage());
        }
    }



	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity entity, LivingEntity sourceentity) {
        if (sourceentity instanceof ServerPlayer player && !(entity instanceof Player)) {
            // Seleziona il mob come target e pulisci i blocchi
            String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
            
            // Clear any existing markers
            clearMarkersAndResetTarget(player, itemstack);
            
            // Set the target mob
            setTargetMob(itemstack, entityId);
            
            player.displayClientMessage(Component.translatable("item.iska_utils.scanner.mob_target_set", entity.getName()), true);
            
            return true; // Non danneggiare il mob
        }
        return false;
	}

    /**
     * Sets the target mob in the scanner and remove any target block
     */
    private void setTargetMob(ItemStack itemStack, String mobId) {
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        
        // Debug log
        LOGGER.info("Setting mob target in NBT: {}", mobId);
        
        // Remove target block if any
        tag.remove(TARGET_BLOCK_TAG);
        
        // Set target mob
        tag.putString(TARGET_MOB_TAG, mobId);
        
        // Make sure the scanner has a unique ID
        if (!tag.contains(SCANNER_ID_TAG)) {
            tag.putUUID(SCANNER_ID_TAG, UUID.randomUUID());
        }
        
        // Set clear_markers value in NBT
        tag.putBoolean(CLEAR_MARKERS_TAG, clear_markers);
        
        // Save data to ItemStack
        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        
        // Verify target was set
        CompoundTag newTag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (newTag.contains(TARGET_MOB_TAG)) {
            LOGGER.info("Target mob successfully set: {}", newTag.getString(TARGET_MOB_TAG));
        } else {
            LOGGER.error("Failed to set target mob!");
        }
    }
    
    /**
     * Scans the area for target mobs
     */
    private void scanForMobs(ServerPlayer player, ItemStack itemStack) {
        String targetMobId = getTargetMob(itemStack);
        if (targetMobId == null || player.level() == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        UUID scannerId = tag.getUUID(SCANNER_ID_TAG);
        
        // Ensure the scan marker team exists
        ensureScanMarkerTeam(level);
        
        // Get player position
        BlockPos playerPos = player.blockPosition();
        
        // Get config values
        int scanRange = Config.scannerScanRange;
        int maxTTL = 600;
        int mobEffectDuration = maxTTL / 20; // Convert ticks to seconds for potion effect
        
        // Scan in a radius
        int markersFound = 0;
        
        double scanRangeSquared = scanRange * scanRange;
        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(
            LivingEntity.class, 
            player.getBoundingBox().inflate(scanRange),
            entity -> {
                double distanceSq = player.distanceToSqr(entity.getX(), entity.getY(), entity.getZ());
                return distanceSq <= scanRangeSquared && 
                       BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().equals(targetMobId);
            }
        );
        
        for (LivingEntity entity : nearbyEntities) {
            // Skip entities that already have a team (except our team)
            String entityUUID = entity.getStringUUID();
            if (level.getScoreboard().getPlayersTeam(entityUUID) != null && 
                !level.getScoreboard().getPlayersTeam(entityUUID).getName().equals("iska_utils_scan")) {
                continue;
            }
            
            // Check if the entity has already active effects and determine whether to hide particles
            boolean hideParticles = true; // Default: particles hidden if no effects
            
            // If the entity has effects, maintain the particle visibility of the first effect found
            if (!entity.getActiveEffects().isEmpty()) {
                hideParticles = !entity.getActiveEffects().iterator().next().isVisible();
            }
            
            // Apply glowing effect to the mob with the right particle visibility
            String effectCommand = String.format(
                "effect give %s minecraft:glowing %d 0 %b", 
                entityUUID, mobEffectDuration, hideParticles
            );
            player.getServer().getCommands().performPrefixedCommand(
                player.getServer().createCommandSourceStack().withSuppressedOutput(),
                effectCommand
            );
            
            // Add to the team for coloring
            String teamJoinCommand = String.format("team join iska_utils_scan %s", entityUUID);
            player.getServer().getCommands().performPrefixedCommand(
                player.getServer().createCommandSourceStack().withSuppressedOutput(),
                teamJoinCommand
            );
            
            markersFound++;
        }
        
        player.displayClientMessage(Component.translatable("item.iska_utils.scanner.found_mobs", 
                markersFound, Component.translatable("entity.minecraft." + targetMobId.substring(targetMobId.indexOf(":") + 1))), true);
    }

    // ===== ENERGY MANAGEMENT METHODS =====
    
    /**
     * Check if the item can store energy
     */
    public boolean canStoreEnergy() {
        // If buffer is 0, disable energy system completely
        return Config.scannerEnergyBuffer > 0;
    }
    
    /**
     * Check if the item requires energy to function
     */
    public boolean requiresEnergyToFunction() {
        if (Config.scannerEnergyConsume <= 0) {
            return false;
        }
        
        return canStoreEnergy();
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
        int maxCapacity = Config.scannerEnergyBuffer;
        tag.putInt(ENERGY_TAG, Math.max(0, Math.min(energy, maxCapacity)));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    
    
    /**
     * Ensures that the energy tag exists in the ItemStack's NBT
     * If not, it creates it with the maximum energy value
     */
    private CompoundTag ensureEnergyTag(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        
        if (canStoreEnergy() && !tag.contains(ENERGY_TAG)) {
            tag.putInt(ENERGY_TAG, 0);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        
        return tag;
    }
    
    /**
     * Gets the maximum energy that can be stored
     */
    public int getMaxEnergyStored(ItemStack stack) {
        if (!canStoreEnergy()) {
            return 0;
        }
        return Config.scannerEnergyBuffer;
    }
    
    /**
     * Checks if the scanner has enough energy for operation
     */
    public boolean hasEnoughEnergy(ItemStack stack) {
        if (!requiresEnergyToFunction()) {
            return true; // No energy required
        }
        
        int currentEnergy = getEnergyStored(stack);
        int requiredEnergy = Math.min(Config.scannerEnergyConsume, getMaxEnergyStored(stack));
        return currentEnergy >= requiredEnergy;
    }
    
    /**
     * Consumes energy for operation
     * @param stack The ItemStack
     * @return true if energy was consumed or no energy is required, false if insufficient energy
     */
    public boolean consumeEnergyForOperation(ItemStack stack) {
        if (!requiresEnergyToFunction()) {
            return true; // No energy required
        }
        
        int consumption = Math.min(Config.scannerEnergyConsume, getMaxEnergyStored(stack));
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
     * Adds tooltip information to the item
     */
    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
        // Energy information - only show if energy is enabled
        if (canStoreEnergy() && requiresEnergyToFunction()) {
            int energy = getEnergyStored(stack);
            int maxEnergy = getMaxEnergyStored(stack);
            float percentage = (float) energy / Math.max(1, maxEnergy) * 100f;
            
            String energyString = String.format("%,d / %,d RF (%.1f%%)", energy, maxEnergy, percentage);
            Component energyText = Component.translatable("item.iska_utils.scanner.tooltip.energy")
                .withStyle(style -> style.withColor(ChatFormatting.RED))
                .append(Component.literal(energyString).withStyle(ChatFormatting.RED));
            
            tooltipComponents.add(energyText);
        }
    }
    
    /**
     * Recharges energy for the item - can be called from charging stations or other energy sources
     * 
     * @param stack The ItemStack to charge
     * @param amount The amount of energy to add
     * @return The amount of energy that was actually added
     */
    public int rechargeEnergy(ItemStack stack, int amount) {
        if (!canStoreEnergy() || amount <= 0) {
            return 0;
        }
        
        int currentEnergy = getEnergyStored(stack);
        int maxEnergy = getMaxEnergyStored(stack);
        
        if (currentEnergy >= maxEnergy) {
            return 0; // Already full
        }
        
        int energyToAdd = Math.min(amount, maxEnergy - currentEnergy);
        setEnergyStored(stack, currentEnergy + energyToAdd);
        
        return energyToAdd;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide && entity instanceof Player player) {
            // Calculate the completion percentage
            int totalDuration = getUseDuration(stack, entity);
            int ticksUsed = totalDuration - remainingUseDuration;
            int requiredDuration = Config.scannerScanDuration;
            
            // Update loading data
            UUID playerId = player.getUUID();
            LOADING_DATA.computeIfAbsent(playerId, k -> new LoadingData()).update(ticksUsed);
            
            // Show a loading bar to the player
            if (ticksUsed % 5 == 0) { // Aggiorna ogni 5 tick per non spammare
                displayLoadingBar(player, ticksUsed, requiredDuration);
            }
        }
    }
    
    /**
     * Show a loading bar to the player
     */
    private void displayLoadingBar(Player player, int ticksUsed, int requiredDuration) {
        float percentage = Math.min(1.0f, (float) ticksUsed / requiredDuration);
        int filledBlocks = Math.round(percentage * LOADING_BAR_LENGTH);
        
        MutableComponent message = Component.literal("");
        
        // Aggiungi percentuale
        String percentText = String.format(" %.0f%% ", percentage * 100);
        message.append(Component.literal(percentText).withStyle(percentage >= 1.0f ? 
                ChatFormatting.GREEN : ChatFormatting.RED));
        
        
        // Aggiungi blocchi pieni (verdi se completamente carico, altrimenti rossi)
        for (int i = 0; i < filledBlocks; i++) {
            message.append(Component.literal("█").withStyle(percentage >= 1.0f ? 
                    ChatFormatting.GREEN : ChatFormatting.RED));
        }
        
        // Aggiungi blocchi vuoti (grigi)
        for (int i = filledBlocks; i < LOADING_BAR_LENGTH; i++) {
            message.append(Component.literal("█").withStyle(ChatFormatting.DARK_GRAY));
        }
        
        message.append(Component.literal(percentText).withStyle(percentage >= 1.0f ? 
            ChatFormatting.GREEN : ChatFormatting.RED));
        
        // Show the message to the player
        player.displayClientMessage(message, true);
    }
} 


