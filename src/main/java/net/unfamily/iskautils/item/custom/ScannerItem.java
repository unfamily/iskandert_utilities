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
    private static final int TTL_MULTIPLIER = 5;
    
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
        
        // Cancella sempre tutti i marker esistenti ad ogni click
        clearMarkers(player, itemStack);
        
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
        // Remove loading data
        if (world.isClientSide || !(entity instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (entity instanceof Player player) {
            if(player.isCrouching()) {
                LOADING_DATA.remove(player.getUUID());
                
                // Check if there's a chip in the offhand and transfer the target
                if (!world.isClientSide) {
                    checkAndTransferFromChip(player);
                }
            }
            else {
                // Check if item was held for enough time
                Block targetBlock = getTargetBlock(itemstack);
                String targetMob = getTargetMob(itemstack);
                
                if (targetBlock == null && targetMob == null) {
                    serverPlayer.displayClientMessage(Component.translatable("item.iska_utils.scanner.no_target"), true);
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
                        serverPlayer.displayClientMessage(Component.translatable("item.iska_utils.scanner.no_energy"), true);
                        return;
                    }
                    
                    boolean scanSuccess = false;
                    
                    if (targetBlock != null) {
                        // Scan for blocks
                        serverPlayer.displayClientMessage(Component.translatable("item.iska_utils.scanner.scan_started", targetBlock.getName()), true);
                        scanArea(serverPlayer, itemstack);
                        scanSuccess = true;
                    } else if (targetMob != null) {
                        // Scan for mobs
                        serverPlayer.displayClientMessage(Component.translatable("item.iska_utils.scanner.scan_started_mob", 
                                getLocalizedMobName(targetMob)), true);
                        scanForMobs(serverPlayer, itemstack);
                        scanSuccess = true;
                    }
                    
                    // Consume energy if scan was successful
                    if (scanSuccess) {
                        consumeEnergyForOperation(itemstack);
                        
                        // Show completed loading bar
                        if (!world.isClientSide) {
                            displayLoadingBar(serverPlayer, requiredDuration, requiredDuration);
                        }
                    }
                } else {
                    // If player released before required time, clear existing markers
                    clearMarkers(player, itemstack);
                    serverPlayer.displayClientMessage(Component.translatable("item.iska_utils.scanner.markers_cleared"), true);
                    
                    // Remove loading data
                    LOADING_DATA.remove(player.getUUID());
                }
            }
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
            
            // Clear from our tracking maps
            if (ACTIVE_MARKERS.containsKey(scannerId)) {
                List<BlockPos> markers = ACTIVE_MARKERS.get(scannerId);
                
                // Send clear message to client for each marker
                for (BlockPos pos : markers) {
                    // Remove the marker from client side
                    if (MARKER_TTL.containsKey(pos)) {
                        // Rimuovi i marker usando handleRemoveHighlight che rimuove sia i blocchi che i billboard
                        net.unfamily.iskautils.network.ModMessages.sendRemoveHighlightPacket(serverPlayer, pos);
                        
                        MARKER_TTL.remove(pos);
                    }
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
        
        // Get or create a list for this scanner's markers
        List<BlockPos> scannerMarkers = ACTIVE_MARKERS.computeIfAbsent(scannerId, k -> new ArrayList<>());
        
        // Get player position
        BlockPos playerPos = player.blockPosition();
        ChunkPos playerChunkPos = new ChunkPos(playerPos);
        
        // Get config values
        int scanRange = Config.scannerScanRange;
        int maxBlocksScan = Config.scannerMaxBlocks;
        int baseTTL = Config.scannerMarkerTTL;
        
        // Determine the number of chunks to scan based on the configured radius
        int chunkRadius = Math.max(1, scanRange / 16);
        
        // Create a set of currently existing marker positions
        Set<BlockPos> existingMarkerPositions = new HashSet<>(scannerMarkers);
        
        // Count how many new markers we can create (remaining capacity)
        // If maxBlocksScan is -1, it means infinite blocks
        boolean infiniteBlocks = maxBlocksScan == -1;
        int remainingCapacity = infiniteBlocks ? Integer.MAX_VALUE : maxBlocksScan - existingMarkerPositions.size();
        
        if (!infiniteBlocks && remainingCapacity <= 0) {
            // Already at maximum capacity
            player.displayClientMessage(Component.translatable("item.iska_utils.scanner.max_markers_reached", maxBlocksScan), true);
            return;
        }
        
        // Calculate the total number of chunks to scan 
        int totalChunksToScan = (2 * chunkRadius + 1) * (2 * chunkRadius + 1);
        int currentChunksScanned = 0;
        
        // Scan in a radius based on the configured scan range
        int newMarkersFound = 0;
        boolean limitReached = false;
        
        scanLoop:
        for (int chunkX = playerChunkPos.x - chunkRadius; chunkX <= playerChunkPos.x + chunkRadius; chunkX++) {
            for (int chunkZ = playerChunkPos.z - chunkRadius; chunkZ <= playerChunkPos.z + chunkRadius; chunkZ++) {
                ChunkPos currentChunkPos = new ChunkPos(chunkX, chunkZ);
                
                // Check if chunk is loaded
                if (!level.isLoaded(BlockPos.containing(currentChunkPos.getMiddleBlockX(), 0, currentChunkPos.getMiddleBlockZ()))) {
                    currentChunksScanned++;
                    continue;
                }
                
                // Update the loading bar for each chunk
                float percentage = (float) currentChunksScanned / totalChunksToScan;
                displayLoadingBar(player, (int)(percentage * Config.scannerScanDuration), Config.scannerScanDuration);
                
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
                                    // Skip if we already have a marker at this position
                                    if (existingMarkerPositions.contains(pos)) {
                                        // Refresh TTL for existing marker
                                        MARKER_TTL.put(pos, baseTTL);
                                        continue;
                                    }
                                    
                                    // Add the block position to the scanner's markers
                                    scannerMarkers.add(pos);
                                    newMarkersFound++;
                                    
                                    // Calculate TTL multiplier based on the number of scanned blocks
                                    int finalTTL = baseTTL * TTL_MULTIPLIER;
                                    
                                    // Add TTL for this marker
                                    MARKER_TTL.put(pos, finalTTL);
                                    
                                    // Use MarkRenderer to add the highlighted block on the client side
                                    // Light blue color (ARGB format): 0x8000BFFF (alpha, red, green, blue)
                                    int lightBlueColor = 0x8000BFFF;
                                    net.unfamily.iskautils.network.ModMessages.sendAddHighlightPacket(player, pos, lightBlueColor, finalTTL);
                                    
                                    // Check if we've reached the limit (skip if infinite blocks)
                                    if (!infiniteBlocks && newMarkersFound >= remainingCapacity) {
                                        limitReached = true;
                                        break scanLoop;
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Increment scanned chunks
                currentChunksScanned++;
            }
        }
        
        Component message;
        if (limitReached) {
            message = Component.translatable("item.iska_utils.scanner.found_blocks_limit", 
                    newMarkersFound, targetBlock.getName(), maxBlocksScan);
        } else {
            message = Component.translatable("item.iska_utils.scanner.found_blocks", 
                    newMarkersFound, targetBlock.getName());
        }
        
        player.displayClientMessage(message, true);
        
        // Make sure to show the completed bar at the end
        displayLoadingBar(player, Config.scannerScanDuration, Config.scannerScanDuration);
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
                // Send message to all players in the dimension to remove the marker
                for (ServerPlayer player : level.players()) {
                    net.unfamily.iskautils.network.ModMessages.sendRemoveHighlightPacket(player, pos);
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
     * Scans the area for target mobs
     */
    private void scanForMobs(ServerPlayer player, ItemStack itemStack) {
        String targetMobId = getTargetMob(itemStack);
        if (targetMobId == null || player.level() == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        UUID scannerId = tag.getUUID(SCANNER_ID_TAG);
        
        // Get player position
        BlockPos playerPos = player.blockPosition();
        
        // Get config values
        int scanRange = Config.scannerScanRange;
        int maxTTL = Config.scannerMarkerTTL;
        
        // Calculate the total number of chunks to scan
        int chunkRadius = Math.max(1, scanRange / 16);
        int totalChunksToScan = (2 * chunkRadius + 1) * (2 * chunkRadius + 1);
        int currentChunksScanned = 0;
        
        // Scan in a radius
        int markersFound = 0;
        
        double scanRangeSquared = scanRange * scanRange;
        
        // Create a set of currently existing marker positions
        Set<BlockPos> existingMarkerPositions = new HashSet<>();
        if (ACTIVE_MARKERS.containsKey(scannerId)) {
            existingMarkerPositions.addAll(ACTIVE_MARKERS.get(scannerId));
        }
        
        // Get or create a list for this scanner's markers
        List<BlockPos> scannerMarkers = ACTIVE_MARKERS.computeIfAbsent(scannerId, k -> new ArrayList<>());
        
        // Scan the chunks in the area
        for (int chunkX = playerPos.getX() / 16 - chunkRadius; chunkX <= playerPos.getX() / 16 + chunkRadius; chunkX++) {
            for (int chunkZ = playerPos.getZ() / 16 - chunkRadius; chunkZ <= playerPos.getZ() / 16 + chunkRadius; chunkZ++) {
                ChunkPos currentChunkPos = new ChunkPos(chunkX, chunkZ);
                
                // Check if the chunk is loaded
                if (!level.isLoaded(BlockPos.containing(currentChunkPos.getMiddleBlockX(), 0, currentChunkPos.getMiddleBlockZ()))) {
                    currentChunksScanned++;
                    continue;
                }
                
                // Update the loading bar for each chunk
                float percentage = (float) currentChunksScanned / totalChunksToScan;
                displayLoadingBar(player, (int)(percentage * Config.scannerScanDuration), Config.scannerScanDuration);
                
                // Search for entities in the current chunk
                List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(
                    LivingEntity.class, 
                    new net.minecraft.world.phys.AABB(
                        currentChunkPos.getMinBlockX(), level.getMinBuildHeight(), currentChunkPos.getMinBlockZ(),
                        currentChunkPos.getMaxBlockX(), level.getMaxBuildHeight(), currentChunkPos.getMaxBlockZ()
                    ),
                    entity -> {
                        double distanceSq = player.distanceToSqr(entity.getX(), entity.getY(), entity.getZ());
                        return distanceSq <= scanRangeSquared && 
                               BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().equals(targetMobId);
                    }
                );
                
                // Process the found entities
                for (LivingEntity entity : nearbyEntities) {
                    // Get the entity's position
                    BlockPos entityPos = entity.blockPosition();
                    
                    // Skip if we already have a marker at this position
                    if (existingMarkerPositions.contains(entityPos)) {
                        // Refresh TTL for existing marker
                        MARKER_TTL.put(entityPos, maxTTL);
                        continue;
                    }
                    
                    // Add the entity position to the scanner's markers
                    scannerMarkers.add(entityPos);
                    markersFound++;
                    
                    // Calculate the final TTL
                    int finalTTL = maxTTL * TTL_MULTIPLIER;
                    
                    // Add TTL for this marker
                    MARKER_TTL.put(entityPos, finalTTL);
                    
                    // Use MarkRenderer to add a billboard marker on the client side
                    // Magenta/Purple color (ARGB format): 0x80EB3480 (alpha, red, green, blue)
                    int color = 0x80EB3480;
                    net.unfamily.iskautils.network.ModMessages.sendAddBillboardPacket(player, entityPos, color, finalTTL);
                }
                
                // Increment scanned chunks
                currentChunksScanned++;
            }
        }
        
        player.displayClientMessage(Component.translatable("item.iska_utils.scanner.found_mobs", 
                markersFound, getLocalizedMobName(targetMobId)), true);
        
        // Make sure to show the completed bar at the end
        displayLoadingBar(player, Config.scannerScanDuration, Config.scannerScanDuration);
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
     * Creates a localized name for a mob from its ID
     */
    private Component getLocalizedMobName(String mobId) {
        if (mobId == null) return Component.literal("Unknown");
        
        // Extract namespace and path from the ID
        String namespace = "minecraft";
        String path = mobId;
        
        if (mobId.contains(":")) {
            String[] parts = mobId.split(":", 2);
            namespace = parts[0];
            path = parts[1];
        }
        
        // Try to use the specific translation key for the namespace
        String translationKey = "entity." + namespace + "." + path;
        Component translated = Component.translatable(translationKey);
        
    
        // If the namespace is not minecraft, add the namespace to the name if the translation fails
        if (!namespace.equals("minecraft")) {
            // Check if the translation was successful
            String translatedText = translated.getString();
            if (translatedText.equals(translationKey)) {
                // Translation failed, use an alternative format
                return Component.literal(namespace + ":" + path);
            }
        }
        
        return translated;
    }
    
    /**
     * Aggiunge informazioni tooltip all'item
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
        
        // Target information
        Block targetBlock = getTargetBlock(stack);
        String targetMob = getTargetMob(stack);
        
        if (targetBlock != null) {
            Component targetText = Component.translatable("item.iska_utils.scanner.tooltip.target_block")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA))
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(targetBlock.getName().copy().withStyle(ChatFormatting.WHITE));
            
            tooltipComponents.add(targetText);
        } else if (targetMob != null) {
            Component targetText = Component.translatable("item.iska_utils.scanner.tooltip.target_mob")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA))
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(getLocalizedMobName(targetMob).copy().withStyle(ChatFormatting.WHITE));
            
            tooltipComponents.add(targetText);
        } else {
            Component noTargetText = Component.translatable("item.iska_utils.scanner.tooltip.no_target")
                .withStyle(style -> style.withColor(ChatFormatting.GRAY));
            
            tooltipComponents.add(noTargetText);
        }
        
        // Instructions
        Component instruction0Text = Component.translatable("item.iska_utils.scanner.tooltip.instruction0")
            .withStyle(style -> style.withColor(ChatFormatting.YELLOW));
        tooltipComponents.add(instruction0Text);

        Component instruction1Text = Component.translatable("item.iska_utils.scanner.tooltip.instruction1")
            .withStyle(style -> style.withColor(ChatFormatting.YELLOW));
        tooltipComponents.add(instruction1Text);

        Component instruction2Text = Component.translatable("item.iska_utils.scanner.tooltip.instruction2")
            .withStyle(style -> style.withColor(ChatFormatting.YELLOW));
        tooltipComponents.add(instruction2Text);

        // Chip integration info
        Component chipInfoText = Component.translatable("item.iska_utils.scanner.tooltip.chip_info0")
            .withStyle(style -> style.withColor(ChatFormatting.AQUA));

        if(Config.scannerEnergyConsume > 0) {
            Component chipInfoText1 = Component.translatable("item.iska_utils.scanner.tooltip.chip_info1")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA));
            tooltipComponents.add(chipInfoText1);
        } else {
            Component chipInfoText2 = Component.translatable("item.iska_utils.scanner.tooltip.chip_info2")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA));
            tooltipComponents.add(chipInfoText2);
        }

        tooltipComponents.add(chipInfoText);
        
        tooltipComponents.add(chipInfoText);
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
        
        // Add percentage
        String percentText = String.format(" %.0f%% ", percentage * 100);
        message.append(Component.literal(percentText).withStyle(percentage >= 1.0f ? 
                ChatFormatting.GREEN : ChatFormatting.RED));
        
        
        // Add filled blocks (green if fully loaded, otherwise red)
        for (int i = 0; i < filledBlocks; i++) {
            message.append(Component.literal("█").withStyle(percentage >= 1.0f ? 
                    ChatFormatting.GREEN : ChatFormatting.RED));
        }
        
        // Add empty blocks (gray)
        for (int i = filledBlocks; i < LOADING_BAR_LENGTH; i++) {
            message.append(Component.literal("█").withStyle(ChatFormatting.DARK_GRAY));
        }
        
        message.append(Component.literal(percentText).withStyle(percentage >= 1.0f ? 
            ChatFormatting.GREEN : ChatFormatting.RED));
        
        // Show the message to the player
        player.displayClientMessage(message, true);
    }

    /**
     * Checks if there's a chip in the offhand and transfers the target
     */
    private void checkAndTransferFromChip(Player player) {
        ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHandItem = player.getItemInHand(InteractionHand.OFF_HAND);
        
        // Verify that the item in the main hand is this scanner
        if (mainHandItem.getItem() != this) {
            return;
        }
        
        // Verify that the item in the offhand is a ScannerChip
        if (!(offHandItem.getItem() instanceof ScannerChipItem)) {
            return;
        }
        
        // Get the tags of both items
        CompoundTag scannerTag = mainHandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag chipTag = offHandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        
        // Check if the chip has a block target
        if (chipTag.contains("TargetBlock")) {
            // Remove mob target if present
            scannerTag.remove("TargetMob");
            
            // Copy the block target
            String blockId = chipTag.getString("TargetBlock");
            scannerTag.putString("TargetBlock", blockId);
            
            // Make sure the scanner has a unique ID
            if (!scannerTag.contains("ScannerId")) {
                scannerTag.putUUID("ScannerId", UUID.randomUUID());
            }
            
            // Save the data to the scanner
            mainHandItem.set(DataComponents.CUSTOM_DATA, CustomData.of(scannerTag));
            
            // Notify the player
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockId));
            player.displayClientMessage(Component.translatable("item.iska_utils.scanner_chip.transfer_success", block.getName()), true);
        } 
        // Check if the chip has a mob target
        else if (chipTag.contains("TargetMob")) {
            // Remove block target if present
            scannerTag.remove("TargetBlock");
            
            // Copy the mob target
            String mobId = chipTag.getString("TargetMob");
            scannerTag.putString("TargetMob", mobId);
            
            // Make sure the scanner has a unique ID
            if (!scannerTag.contains("ScannerId")) {
                scannerTag.putUUID("ScannerId", UUID.randomUUID());
            }
            
            // Save the data to the scanner
            mainHandItem.set(DataComponents.CUSTOM_DATA, CustomData.of(scannerTag));
            
            // Notify the player
            player.displayClientMessage(Component.translatable("item.iska_utils.scanner_chip.transfer_success_mob", 
                    getLocalizedMobName(mobId)), true);
        }
    }
} 


