package net.unfamily.iskautils.item.custom;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.Tags;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.KeybindTooltipUtil;
import net.unfamily.iskautils.util.ScannerMobCategories;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDate;

/**
 * Item for scanning specific blocks in an area
 */
public class ScannerItem extends Item implements net.unfamily.iskautils.item.RfStoringItem {
    private static final Logger LOGGER = LogUtils.getLogger();
 
    private static final String TARGET_BLOCK_TAG = "TargetBlock";
    private static final String TARGET_MOB_TAG = "TargetMob";
    private static final String TARGET_GEN_TAG = "TargetGeneric";
    private static final String SCANNER_ID_TAG = "ScannerId";
    private static final String ENERGY_TAG = "Energy";
    private static final String SCAN_RANGE_TAG = "ScanRange"; 
    private static final int TTL_MULTIPLIER = 1;
    
    // Map to track active markers by scanner ID
    private static final Map<UUID, List<BlockPos>> ACTIVE_MARKERS = new HashMap<>();
    
    // Map to track TTL for each marker
    private static final Map<BlockPos, Integer> MARKER_TTL = new HashMap<>();

    private static final int LOADING_BAR_LENGTH = 15; // Number of blocks █ in the loading bar
    /** Extra hold ticks after scan requirement before the loading bar shows 100%. */
    private static final int HOLD_LOADING_BAR_MARGIN_TICKS = 10;
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

    public ScannerItem(Item.Properties properties) {
        super(properties);
    }

    /**
     * Called when creating a new ItemStack of this item
     * Initializes the item with energy
     */
    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        
        if (canStoreEnergy()) {
            tag.putInt(ENERGY_TAG, 0);
        }
        
        // Initialize scan range to default value
        if (!tag.contains(SCAN_RANGE_TAG)) {
            tag.putInt(SCAN_RANGE_TAG, Config.scannerDefaultRange);
        }
        
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack itemStack = context.getItemInHand();
        BlockPos blockPos = context.getClickedPos();
        
        if (level.isClientSide() || player == null) {
            return InteractionResult.SUCCESS;
        }
        
        // If player is crouching (shift), register the target block or mob
        if (player.isCrouching()) {
            BlockState state = level.getBlockState(blockPos);
            Block block = state.getBlock();
            
            if (block != Blocks.AIR) {
                // Register the target block
                setTargetBlock(itemStack, block);
                player.sendOverlayMessage(Component.translatable("item.iska_utils.scanner.target_set", block.getName()));
                return InteractionResult.SUCCESS;
            }
        }
        
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        
        // If player is crouching (shift), allow using for chip transfer
        if (player.isCrouching()) {
            // Check if there is a chip in the offhand
            ItemStack offHandItem = player.getItemInHand(InteractionHand.OFF_HAND);
            if (offHandItem.getItem() instanceof ScannerChipItem) {
                // Check if the chip has a valid target
                ScannerChipItem chipItem = (ScannerChipItem) offHandItem.getItem();
                boolean hasTarget = chipItem.getTargetBlock(offHandItem) != null || 
                                   chipItem.getTargetMob(offHandItem) != null || 
                                   chipItem.getGenericTarget(offHandItem) != null;
                
                if (hasTarget) {
                    // Start using the item for the transfer
                    player.sendOverlayMessage(Component.translatable("item.iska_utils.scanner.transferring_from_chip"));
                    player.startUsingItem(hand);
                    return InteractionResult.CONSUME;
                } else {
                    player.sendOverlayMessage(Component.translatable("item.iska_utils.scanner_chip.no_target"));
                    return InteractionResult.FAIL;
                }
            }
        }
        
        // Check if we have a target block, mob or generic target
        Block targetBlock = getTargetBlock(itemStack);
        String targetMob = getTargetMob(itemStack);
        String genericTarget = getGenericTarget(itemStack);
        
        if (targetBlock == null && targetMob == null && genericTarget == null) {
            // Manual clear: no target still removes all scanner-related client markers and resets server TTL maps
            if (player instanceof ServerPlayer sp) {
                clearAllVisualMarkersFor(sp, itemStack);
            }
            player.sendOverlayMessage(Component.translatable("item.iska_utils.scanner.no_target"));
            return InteractionResult.FAIL;
        }
        
        // Always clear existing markers before a new scan attempt (even if energy is too low to scan)
        clearMarkers(player, itemStack);
        
        // Energy is required only for scanning, not for the clear above
        if (requiresEnergyToFunction() && !hasEnoughEnergy(itemStack)) {
            player.sendOverlayMessage(Component.translatable("item.iska_utils.scanner.no_energy"));
            return InteractionResult.FAIL;
        }
        
        // Set use duration
        player.startUsingItem(hand);
        
        return InteractionResult.CONSUME;
    }
    
    public InteractionResult resetTarget(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }
        
        Block targetBlock = getTargetBlock(itemStack);
        String targetMob = getTargetMob(itemStack);
        String genericTarget = getGenericTarget(itemStack);
        
        if (targetBlock == null && targetMob == null && genericTarget == null) {
            clearAllVisualMarkersFor(serverPlayer, itemStack);
            player.sendOverlayMessage(Component.translatable("item.iska_utils.scanner.markers_cleared"));
            return InteractionResult.CONSUME;
        }
        
        clearMarkersAndResetTarget(serverPlayer, itemStack);
        player.sendOverlayMessage(Component.translatable("item.iska_utils.scanner.target_reset"));
        return InteractionResult.CONSUME;
    }
    

    @Override
	public ItemUseAnimation getUseAnimation(ItemStack itemstack) {
		return ItemUseAnimation.BOW;
	}

	@Override
	public int getUseDuration(ItemStack itemstack, LivingEntity livingEntity) {
		return 300;
	}

    @Override
    public boolean releaseUsing(ItemStack itemstack, Level world, LivingEntity entity, int time) {
        // Remove loading data
        if (world.isClientSide() || !(entity instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        if (entity instanceof Player player) {
            if(player.isCrouching()) {
                LOADING_DATA.remove(player.getUUID());
                
                // Check if there's a chip in the offhand and transfer the target
                if (!world.isClientSide()) {
                    checkAndTransferFromChip(player);
                }
                return true;
            }
            else {
                // Check if item was held for enough time
                Block targetBlock = getTargetBlock(itemstack);
                String targetMob = getTargetMob(itemstack);
                String genericTarget = getGenericTarget(itemstack);
                
                if (targetBlock == null && targetMob == null && genericTarget == null) {
                    clearAllVisualMarkersFor(serverPlayer, itemstack);
                    serverPlayer.sendOverlayMessage(Component.translatable("item.iska_utils.scanner.no_target"));
                    return false;
                }
                
                // The time parameter represents remaining time, so we need to check if it's LESS than a certain value
                // to determine if user has held long enough
                int totalDuration = getUseDuration(itemstack, entity);
                int timeUsed = totalDuration - time;
                int requiredDuration = Config.scannerScanDuration;
                
                if (timeUsed >= requiredDuration) {
                    // Double-check energy before scanning
                    if (requiresEnergyToFunction() && !hasEnoughEnergy(itemstack)) {
                        serverPlayer.sendOverlayMessage(Component.translatable("item.iska_utils.scanner.no_energy"));
                        return false;
                    }
                    
                    boolean scanSuccess = false;
                    
                    if (targetBlock != null) {
                        // Scan for blocks
                        serverPlayer.sendOverlayMessage(Component.translatable("item.iska_utils.scanner.scan_started", targetBlock.getName()));
                        scanArea(serverPlayer, itemstack);
                        scanSuccess = true;
                    } else if (targetMob != null) {
                        // Scan for mobs
                        serverPlayer.sendOverlayMessage(Component.translatable("item.iska_utils.scanner.scan_started_mob", 
                                getLocalizedMobName(targetMob)));
                        scanForMobs(serverPlayer, itemstack);
                        scanSuccess = true;
                    } else if (genericTarget != null) {
                        // Scan based on generic target
                        if (genericTarget.startsWith("ores")) {
                            serverPlayer.sendOverlayMessage(Component.translatable("item.iska_utils.scanner.scan_started_ores"));
                            scanForAllOres(serverPlayer, itemstack);
                            scanSuccess = true;
                        } else if (ScannerMobCategories.isMobScanTarget(genericTarget)) {
                            String mode = ScannerMobCategories.normalizedMode(genericTarget);
                            serverPlayer.sendOverlayMessage(Component.translatable("item.iska_utils.scanner.scan_started_mobs." + mode));
                            scanForAllMobs(serverPlayer, itemstack, genericTarget);
                            scanSuccess = true;
                        }
                    }
                    
                    // Consume energy if scan was successful
                    if (scanSuccess) {
                        consumeEnergyForOperation(itemstack);
                    }
                    return scanSuccess;
                } else {
                    // If player released before required time, clear existing markers
                    clearMarkers(player, itemstack);
                    serverPlayer.sendOverlayMessage(Component.translatable("item.iska_utils.scanner.markers_cleared"));
                    
                    // Remove loading data
                    LOADING_DATA.remove(player.getUUID());
                    return true;
                }
            }
        }
        return false;
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
            tag.putString(SCANNER_ID_TAG, UUID.randomUUID().toString());
        }
        
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

        String blockId = tag.getString(TARGET_BLOCK_TAG).orElse("");
        Identifier id = Identifier.tryParse(blockId);
        return id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
    }
    
    /**
     * Gets the target mob from the scanner
     */
    private String getTargetMob(ItemStack itemStack) {
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        
        if (!tag.contains(TARGET_MOB_TAG)) {
            LOGGER.debug("No target mob found in item");
            return null;
        }
        
        String mobId = tag.getString(TARGET_MOB_TAG).orElse("");
        LOGGER.debug("Found target mob in item: {}", mobId);
        
        return mobId.isEmpty() ? null : mobId;
    }
    
    /**
     * Gets the generic target type from the scanner
     * @return "ores" or "mobs" or null if not set
     */
    private String getGenericTarget(ItemStack itemStack) {
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        
        if (!tag.contains(TARGET_GEN_TAG)) {
            return null;
        }
        
        String genericTarget = tag.getString(TARGET_GEN_TAG).orElse("");
        LOGGER.debug("Found generic target in item: {}", genericTarget);
        
        return genericTarget.isEmpty() ? null : genericTarget;
    }
    
    /**
     * Sets the generic target in the scanner
     */
    private void setGenericTarget(ItemStack itemStack, String targetType) {
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        
        // Remove any specific targets
        tag.remove(TARGET_BLOCK_TAG);
        tag.remove(TARGET_MOB_TAG);
        
        // Set generic target
        tag.putString(TARGET_GEN_TAG, targetType);
        
        // Make sure the scanner has a unique ID
        if (!tag.contains(SCANNER_ID_TAG)) {
            tag.putString(SCANNER_ID_TAG, UUID.randomUUID().toString());
        }
        
        // Initialize scan range if not set
        if (!tag.contains(SCAN_RANGE_TAG)) {
            tag.putInt(SCAN_RANGE_TAG, Config.scannerDefaultRange);
        }
        
        // Save data to ItemStack
        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    
    /**
     * Gets the current scan range from the scanner
     */
    private int getScanRange(ItemStack itemStack) {
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(SCAN_RANGE_TAG)) {
            // Initialize with default value
            int defaultRange = Config.scannerDefaultRange;
            tag.putInt(SCAN_RANGE_TAG, defaultRange);
            itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            return defaultRange;
        }
        int range = tag.getInt(SCAN_RANGE_TAG).orElse(Config.scannerDefaultRange);
        // Ensure range is valid (exists in options array)
        java.util.List<Integer> options = Config.scannerRangeOptions;
        if (options != null && !options.isEmpty()) {
            // Check if range exists in options using proper int comparison
            boolean found = false;
            for (Integer option : options) {
                if (option != null && option.intValue() == range) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                // If current range is not in options, reset to default
                range = Config.scannerDefaultRange;
                tag.putInt(SCAN_RANGE_TAG, range);
                itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
        }
        return range;
    }
    
    /**
     * Sets the scan range in the scanner
     */
    private void setScanRange(ItemStack itemStack, int range) {
        // Range is validated by the options array, no need to check against deprecated max
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(SCAN_RANGE_TAG, range);
        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    
    /**
     * Cycles through the available scan range options
     */
    public void cycleScanRange(ServerPlayer player, ItemStack itemStack) {
        int currentRange = getScanRange(itemStack);
        java.util.List<Integer> options = Config.scannerRangeOptions;
        
        if (options == null || options.isEmpty()) {
            return;
        }
        
        // Find current range in options (use intValue() to ensure proper comparison)
        int currentIndex = -1;
        for (int i = 0; i < options.size(); i++) {
            Integer optionValue = options.get(i);
            if (optionValue != null && optionValue.intValue() == currentRange) {
                currentIndex = i;
                break;
            }
        }
        
        // If current range not found or is last, cycle to first
        int nextIndex;
        if (currentIndex == -1 || currentIndex == options.size() - 1) {
            nextIndex = 0;
        } else {
            nextIndex = currentIndex + 1;
        }
        
        Integer nextRangeObj = options.get(nextIndex);
        int nextRange = nextRangeObj != null ? nextRangeObj.intValue() : Config.scannerDefaultRange;
        // Range is already validated by being in the options array
        
        setScanRange(itemStack, nextRange);
        
        // Display message to player
        player.sendOverlayMessage(Component.translatable("item.iska_utils.scanner.range_set", nextRange));
    }

    /**
     * Returns sorted scan range steps from config up to maxRange (inclusive).
     * If maxRange is not in the configured options, it is appended as the final step.
     */
    private static List<Integer> getRangeStepsUpTo(int maxRange) {
        java.util.TreeSet<Integer> steps = new java.util.TreeSet<>();
        if (Config.scannerRangeOptions != null) {
            for (Integer option : Config.scannerRangeOptions) {
                if (option != null && option > 0 && option <= maxRange) {
                    steps.add(option);
                }
            }
        }
        if (steps.isEmpty() || steps.last() < maxRange) {
            steps.add(maxRange);
        }
        return new ArrayList<>(steps);
    }

    private static int countChunksForRangeSteps(List<Integer> rangeSteps) {
        int total = 0;
        for (int ringRange : rangeSteps) {
            int chunkRadius = Math.max(1, ringRange / 16);
            total += (2 * chunkRadius + 1) * (2 * chunkRadius + 1);
        }
        return total;
    }

    /** Horizontal (XZ) distance only; vertical offset does not affect scan range. */
    private static boolean isWithinHorizontalRange(Player player, BlockPos pos, long ringRangeSq) {
        double dx = (pos.getX() + 0.5) - player.getX();
        double dz = (pos.getZ() + 0.5) - player.getZ();
        return dx * dx + dz * dz <= ringRangeSq;
    }

    private static boolean isWithinHorizontalRange(Player player, double x, double z, double ringRangeSquared) {
        double dx = x - player.getX();
        double dz = z - player.getZ();
        return dx * dx + dz * dz <= ringRangeSquared;
    }

    private static void sendScanLimitReachedMessage(ServerPlayer player, int maxLimit) {
        player.sendOverlayMessage(Component.translatable("item.iska_utils.scanner.scan_limit_reached", maxLimit));
    }

    private static int getHoldLoadingBarTicks() {
        return Config.scannerScanDuration + HOLD_LOADING_BAR_MARGIN_TICKS;
    }
    
    /**
     * Removes all existing markers for this scanner
     */
    private void clearMarkers(Player player, ItemStack itemStack) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        clearAllVisualMarkersFor(serverPlayer, itemStack);
    }

    /**
     * Full client reset ({@link net.unfamily.iskautils.network.ModMessages#sendClearHighlightsPacket}) plus server TTL
     * maps, so markers disappear even when the stack has no {@link #SCANNER_ID_TAG} or maps are out of sync after
     * swapping the item in/out of the hotbar.
     */
    private static void clearAllVisualMarkersFor(ServerPlayer player, ItemStack scannerStack) {
        net.unfamily.iskautils.network.ModMessages.sendClearHighlightsPacket(player);

        UUID scannerId = null;
        if (scannerStack != null && !scannerStack.isEmpty()) {
            CompoundTag tag = scannerStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (tag.contains(SCANNER_ID_TAG)) {
                scannerId = tag.getString(SCANNER_ID_TAG).map(UUID::fromString).orElse(null);
            }
        }

        if (scannerId != null && ACTIVE_MARKERS.containsKey(scannerId)) {
            for (BlockPos pos : new ArrayList<>(ACTIVE_MARKERS.get(scannerId))) {
                MARKER_TTL.remove(pos);
            }
            ACTIVE_MARKERS.get(scannerId).clear();
        } else {
            MARKER_TTL.clear();
            for (List<BlockPos> markers : ACTIVE_MARKERS.values()) {
                markers.clear();
            }
        }
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
        tag.remove(TARGET_GEN_TAG);
        
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
        UUID scannerId = tag.getString(SCANNER_ID_TAG).map(UUID::fromString).orElse(null);
        if (scannerId == null) {
            return;
        }
        
        // Get or create a list for this scanner's markers
        List<BlockPos> scannerMarkers = ACTIVE_MARKERS.computeIfAbsent(scannerId, k -> new ArrayList<>());
        
        // Get player position
        BlockPos playerPos = player.blockPosition();
        ChunkPos playerChunkPos = new ChunkPos(playerPos.getX() >> 4, playerPos.getZ() >> 4);
        
        // Get scan range from scanner (or use default)
        int scanRange = getScanRange(itemStack);
        int maxBlocksScan = Config.scannerMaxBlocks;
        int baseTTL = Config.scannerMarkerTTL;
        
        // Scan from smallest configured range outward so nearby matches are prioritized
        List<Integer> rangeSteps = getRangeStepsUpTo(scanRange);
        
        // Create a set of currently existing marker positions
        Set<BlockPos> existingMarkerPositions = new HashSet<>(scannerMarkers);
        
        // Count how many new markers we can create (remaining capacity)
        // If maxBlocksScan is -1, it means infinite blocks
        boolean infiniteBlocks = maxBlocksScan == -1;
        int remainingCapacity = infiniteBlocks ? Integer.MAX_VALUE : maxBlocksScan - existingMarkerPositions.size();
        
        if (!infiniteBlocks && remainingCapacity <= 0) {
            sendScanLimitReachedMessage(player, maxBlocksScan);
            return;
        }
        
        // Scan in expanding radius rings
        int newMarkersFound = 0;
        boolean limitReached = false;
        
        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;
        scanLoop:
        for (int ringRange : rangeSteps) {
            int chunkRadius = Math.max(1, ringRange / 16);
            long ringRangeSq = (long) ringRange * ringRange;
            for (int chunkX = playerChunkX - chunkRadius; chunkX <= playerChunkX + chunkRadius; chunkX++) {
                for (int chunkZ = playerChunkZ - chunkRadius; chunkZ <= playerChunkZ + chunkRadius; chunkZ++) {
                ChunkPos currentChunkPos = new ChunkPos(chunkX, chunkZ);
                
                // Check if chunk is loaded
                if (!level.isLoaded(BlockPos.containing(currentChunkPos.getMiddleBlockX(), 0, currentChunkPos.getMiddleBlockZ()))) {
                    continue;
                }
                
                // Scan the chunk
                for (int x = currentChunkPos.getMinBlockX(); x <= currentChunkPos.getMaxBlockX(); x++) {
                    for (int z = currentChunkPos.getMinBlockZ(); z <= currentChunkPos.getMaxBlockZ(); z++) {
                        for (int y = level.getMinY(); y <= level.getMaxY(); y++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            
                            if (!isWithinHorizontalRange(player, pos, ringRangeSq)) {
                                continue;
                            }
                            
                            // Check the block
                            if (level.getBlockState(pos).getBlock() == targetBlock) {
                                    // Skip positions already marked (do not re-scan)
                                    if (existingMarkerPositions.contains(pos)) {
                                        continue;
                                    }
                                    
                                    // Add the block position to the scanner's markers
                                    scannerMarkers.add(pos);
                                    existingMarkerPositions.add(pos);
                                    newMarkersFound++;
                                    
                                    // Calculate TTL multiplier based on the number of scanned blocks
                                    int finalTTL = baseTTL * TTL_MULTIPLIER;
                                    
                                    // Add TTL for this marker
                                    MARKER_TTL.put(pos, finalTTL);
                                    
                                    // Get the block name for display
                                    BlockState blockState = level.getBlockState(pos);
                                    Block block = blockState.getBlock();
                                    Component blockName = block.getName();
                                    String blockNameString = blockName.getString();
                                    String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
                                    
                                    // Get color from config based on block type
                                    int color = Config.scannerDefaultOreColor; // Default color from config
                                    
                                    // Check for exact match in config
                                    boolean colorFound = false;
                                    for (String entry : Config.scannerOreEntries) {
                                        String[] parts = entry.split(";");
                                        if (parts.length == 2) {
                                            String oreName = parts[0];
                                            // Exact match
                                            if (oreName.equals(blockId)) {
                                                try {
                                                    color = Integer.parseInt(parts[1], 16);
                                                    colorFound = true;
                                                    break;
                                                } catch (NumberFormatException e) {
                                                    // Use default color
                                                }
                                            }
                                            // Pattern match (with $ prefix)
                                            else if (oreName.startsWith("$")) {
                                                String searchTerm = oreName.substring(1).toLowerCase();
                                                if (blockId.toLowerCase().contains(searchTerm)) {
                                                    try {
                                                        color = Integer.parseInt(parts[1], 16);
                                                        colorFound = true;
                                                        break;
                                                    } catch (NumberFormatException e) {
                                                        // Use default color
                                                    }
                                                }
                                            }
                                            // Simple contains match (for entries like "gold", "iron", etc.)
                                            else if (blockId.toLowerCase().contains(oreName.toLowerCase())) {
                                                try {
                                                    color = Integer.parseInt(parts[1], 16);
                                                    colorFound = true;
                                                    break;
                                                } catch (NumberFormatException e) {
                                                    // Use default color
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Log debug info if needed
                                    if (colorFound) {
                                        LOGGER.debug("Applied color {} to block {}", Integer.toHexString(color), blockId);
                                    }
                                    
                                    // Add alpha channel to the color
                                    int colorWithAlpha = (0x80 << 24) | color;
                                    
                                    net.unfamily.iskautils.network.ModMessages.sendAddHighlightWithNamePacket(player, pos, colorWithAlpha, finalTTL, blockNameString);
                                    
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
        }
        }
        
        if (limitReached) {
            sendScanLimitReachedMessage(player, maxBlocksScan);
        } else {
            player.sendOverlayMessage(Component.translatable("item.iska_utils.scanner.found_blocks", 
                    newMarkersFound, targetBlock.getName()));
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
        UUID scannerId = tag.getString(SCANNER_ID_TAG).map(UUID::fromString).orElse(null);
        if (scannerId == null) {
            return;
        }
        
        // Get player position
        BlockPos playerPos = player.blockPosition();
        
        // Get scan range from scanner (or use default)
        int scanRange = getScanRange(itemStack);
        int maxTTL = Config.scannerMarkerTTL;
        int maxMarkersScan = Config.scannerMaxBlocks;
        
        // Scan from smallest configured range outward
        List<Integer> rangeSteps = getRangeStepsUpTo(scanRange);
        
        int markersFound = 0;
        boolean limitReached = false;
        
        // Create a set of currently existing marker positions
        Set<BlockPos> existingMarkerPositions = new HashSet<>();
        if (ACTIVE_MARKERS.containsKey(scannerId)) {
            existingMarkerPositions.addAll(ACTIVE_MARKERS.get(scannerId));
        }
        
        // Get or create a list for this scanner's markers
        List<BlockPos> scannerMarkers = ACTIVE_MARKERS.computeIfAbsent(scannerId, k -> new ArrayList<>());
        
        boolean infiniteMarkers = maxMarkersScan == -1;
        int remainingCapacity = infiniteMarkers ? Integer.MAX_VALUE : maxMarkersScan - existingMarkerPositions.size();
        if (!infiniteMarkers && remainingCapacity <= 0) {
            sendScanLimitReachedMessage(player, maxMarkersScan);
            return;
        }
        
        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;
        
        scanLoop:
        for (int ringRange : rangeSteps) {
            int chunkRadius = Math.max(1, ringRange / 16);
            double ringRangeSquared = (double) ringRange * ringRange;
            for (int chunkX = playerChunkX - chunkRadius; chunkX <= playerChunkX + chunkRadius; chunkX++) {
                for (int chunkZ = playerChunkZ - chunkRadius; chunkZ <= playerChunkZ + chunkRadius; chunkZ++) {
                ChunkPos currentChunkPos = new ChunkPos(chunkX, chunkZ);
                
                // Check if the chunk is loaded
                if (!level.isLoaded(BlockPos.containing(currentChunkPos.getMiddleBlockX(), 0, currentChunkPos.getMiddleBlockZ()))) {
                    continue;
                }
                
                // Search for entities in the current chunk
                net.minecraft.world.phys.AABB chunkBox = new net.minecraft.world.phys.AABB(
                        currentChunkPos.getMinBlockX(), level.getMinY(), currentChunkPos.getMinBlockZ(),
                        currentChunkPos.getMaxBlockX(), level.getMaxY(), currentChunkPos.getMaxBlockZ()
                );
                List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, chunkBox)
                        .stream()
                        .filter(entity -> isWithinHorizontalRange(player, entity.getX(), entity.getZ(), ringRangeSquared)
                                   && BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().equals(targetMobId))
                        .toList();
                
                // Process the found entities
                for (LivingEntity entity : nearbyEntities) {
                    // Get the entity's position
                    BlockPos entityPos = entity.blockPosition();
                    
                    // Skip positions already marked (do not re-scan)
                    if (existingMarkerPositions.contains(entityPos)) {
                        continue;
                    }
                    
                    // Add the entity position to the scanner's markers
                    scannerMarkers.add(entityPos);
                    existingMarkerPositions.add(entityPos);
                    markersFound++;
                    
                    // Calculate the final TTL
                    int finalTTL = maxTTL * TTL_MULTIPLIER;
                    
                    // Add TTL for this marker
                    MARKER_TTL.put(entityPos, finalTTL);
                    
                    // Use MarkRenderer to add a billboard marker on the client side with the entity name
                    // Get color from config and apply alpha
                    int color = (Config.scannerDefaultAlpha << 24) | Config.scannerDefaultMobColor;
                    net.unfamily.iskautils.network.ModMessages.sendAddBillboardWithNamePacket(player, entityPos, color, finalTTL, entity.getName().getString());
                    
                    if (!infiniteMarkers && markersFound >= remainingCapacity) {
                        limitReached = true;
                        break scanLoop;
                    }
                }
                
            }
        }
        }
        
        if (limitReached) {
            sendScanLimitReachedMessage(player, maxMarkersScan);
        } else {
            player.sendOverlayMessage(Component.translatable("item.iska_utils.scanner.found_mobs",
                    markersFound, getLocalizedMobName(targetMobId)));
        }
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
        return tag.getInt(ENERGY_TAG).orElse(0);
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
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, display, tooltipAdder, tooltipFlag);

        if (canStoreEnergy() && requiresEnergyToFunction()) {
            int energy = getEnergyStored(stack);
            int maxEnergy = getMaxEnergyStored(stack);
            float percentage = (float) energy / Math.max(1, maxEnergy) * 100f;
            String energyString = String.format("%,d / %,d RF (%.1f%%)", energy, maxEnergy, percentage);
            Component energyText = Component.translatable("item.iska_utils.scanner.tooltip.energy")
                .withStyle(style -> style.withColor(ChatFormatting.RED))
                .append(Component.literal(energyString).withStyle(ChatFormatting.RED));
            tooltipAdder.accept(energyText);
        }

        int scanRange = getScanRange(stack);
        Component keybindName = KeybindTooltipUtil.keybindOrTranslation("key.iska_utils.scanner_range", "SCANNER_RANGE_KEY");
        tooltipAdder.accept(
            Component.translatable("item.iska_utils.scanner.tooltip.range_key_line", scanRange, keybindName)
                .withStyle(ChatFormatting.GRAY));

        Block targetBlock = getTargetBlock(stack);
        String targetMob = getTargetMob(stack);
        String genericTarget = getGenericTarget(stack);

        if (targetBlock != null) {
            tooltipAdder.accept(
                Component.translatable("item.iska_utils.scanner.tooltip.target_block")
                    .withStyle(style -> style.withColor(ChatFormatting.AQUA))
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(targetBlock.getName().copy().withStyle(ChatFormatting.WHITE)));
        } else if (targetMob != null) {
            tooltipAdder.accept(
                Component.translatable("item.iska_utils.scanner.tooltip.target_mob")
                    .withStyle(style -> style.withColor(ChatFormatting.AQUA))
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(getLocalizedMobName(targetMob).copy().withStyle(ChatFormatting.WHITE)));
        } else if (genericTarget != null) {
            if (genericTarget.startsWith("ores")) {
                Component targetText = Component.translatable("item.iska_utils.scanner.tooltip.target_ores_prefix")
                    .withStyle(style -> style.withColor(ChatFormatting.AQUA));
                int miningLevel = 0;
                if (genericTarget.length() > 4) {
                    String levelStr = genericTarget.substring(4);
                    if (!levelStr.isEmpty()) {
                        try {
                            miningLevel = Integer.parseInt(levelStr);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                if (miningLevel == 0) {
                    targetText = targetText.copy().append(
                        Component.translatable("item.iska_utils.scanner.tooltip.target_ores_value").withStyle(ChatFormatting.WHITE));
                } else {
                    String levelText = switch (miningLevel) {
                        case 1 -> "item.iska_utils.scanner.tooltip.mining_level.wood";
                        case 2 -> "item.iska_utils.scanner.tooltip.mining_level.stone";
                        case 3 -> "item.iska_utils.scanner.tooltip.mining_level.iron";
                        case 4 -> "item.iska_utils.scanner.tooltip.mining_level.diamond";
                        case 5 -> "item.iska_utils.scanner.tooltip.mining_level.netherite";
                        case 100 -> "item.iska_utils.scanner.tooltip.mining_level.other";
                        default -> "item.iska_utils.scanner.tooltip.target_ores_value";
                    };
                    targetText = targetText.copy()
                        .append(Component.translatable("item.iska_utils.scanner.tooltip.target_ores_value").withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                        .append(Component.translatable(levelText).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(")").withStyle(ChatFormatting.GRAY));
                }
                tooltipAdder.accept(targetText);
            } else if (ScannerMobCategories.isMobScanTarget(genericTarget)) {
                String mode = ScannerMobCategories.normalizedMode(genericTarget);
                tooltipAdder.accept(
                    Component.translatable("item.iska_utils.scanner.tooltip.target_mobs_prefix")
                        .withStyle(style -> style.withColor(ChatFormatting.AQUA))
                        .append(Component.translatable("item.iska_utils.scanner.tooltip.target_mobs." + mode)
                            .withStyle(ChatFormatting.WHITE)));
            }
        } else {
            tooltipAdder.accept(
                Component.translatable("item.iska_utils.scanner.tooltip.no_target").withStyle(style -> style.withColor(ChatFormatting.GRAY)));
        }

        tooltipAdder.accept(
            Component.translatable("item.iska_utils.scanner.tooltip.brief_controls").withStyle(ChatFormatting.DARK_GRAY));
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
        if (level.isClientSide() && entity instanceof Player player) {
            // Calculate the completion percentage
            int totalDuration = getUseDuration(stack, entity);
            int ticksUsed = totalDuration - remainingUseDuration;
            
            // Update loading data
            UUID playerId = player.getUUID();
            LOADING_DATA.computeIfAbsent(playerId, k -> new LoadingData()).update(ticksUsed);
            
            // Show a loading bar to the player (fixed scale, independent from scan execution time)
            if (ticksUsed % 2 == 0) {
                displayLoadingBar(player, ticksUsed, getHoldLoadingBarTicks());
            }
        }
    }
    
    /**
     * Show a loading bar to the player
     */
    private void displayLoadingBar(Player player, int ticksUsed, int requiredDuration) {
        float percentage = Math.min(1.0f, (float) ticksUsed / requiredDuration);
        int filledBlocks = Math.round(percentage * LOADING_BAR_LENGTH);
        
        // Check if it's April 1st for the fish emoji easter egg
        LocalDate today = LocalDate.now();
        boolean isAprilFools = today.getMonthValue() == 4 && today.getDayOfMonth() == 1;
        String barChar = isAprilFools ? "🐟" : "█";
        
        MutableComponent message = Component.literal("");
        
        // Add April Fools message if it's April 1st
        if (isAprilFools) {
            message.append(Component.translatable("item.iska_utils.scanner.fish_powered").withStyle(ChatFormatting.AQUA));
        }
        
        // Add percentage
        String percentText = String.format(" %.0f%% ", percentage * 100);
        message.append(Component.literal(percentText).withStyle(percentage >= 1.0f ? 
                ChatFormatting.GREEN : ChatFormatting.RED));
        
        
        // Add filled blocks (green if fully loaded, otherwise red)
        for (int i = 0; i < filledBlocks; i++) {
            message.append(Component.literal(barChar).withStyle(percentage >= 1.0f ? 
                    ChatFormatting.GREEN : ChatFormatting.RED));
        }
        
        // Add empty blocks (gray)
        for (int i = filledBlocks; i < LOADING_BAR_LENGTH; i++) {
            message.append(Component.literal(barChar).withStyle(ChatFormatting.DARK_GRAY));
        }
        
        message.append(Component.literal(percentText).withStyle(percentage >= 1.0f ? 
            ChatFormatting.GREEN : ChatFormatting.RED));
        
        // Show the message to the player
        player.sendOverlayMessage(message);
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
            // Remove mob target and generic target if present
            scannerTag.remove("TargetMob");
            scannerTag.remove("TargetGeneric");
            
            // Copy the block target
            String blockId = chipTag.getString("TargetBlock").orElse("");
            scannerTag.putString("TargetBlock", blockId);
            
            // Make sure the scanner has a unique ID
            if (!scannerTag.contains("ScannerId")) {
                scannerTag.putString("ScannerId", UUID.randomUUID().toString());
            }
            
            // Save the data to the scanner
            mainHandItem.set(DataComponents.CUSTOM_DATA, CustomData.of(scannerTag));
            
            // Notify the player
            Identifier id = Identifier.tryParse(blockId);
            Block block = id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
            player.sendOverlayMessage(Component.translatable("item.iska_utils.scanner_chip.transfer_success", block == null ? Component.literal(blockId) : block.getName()));
        } 
        // Check if the chip has a mob target
        else if (chipTag.contains("TargetMob")) {
            // Remove block target and generic target if present
            scannerTag.remove("TargetBlock");
            scannerTag.remove("TargetGeneric");
            
            // Copy the mob target
            String mobId = chipTag.getString("TargetMob").orElse("");
            scannerTag.putString("TargetMob", mobId);
            
            // Make sure the scanner has a unique ID
            if (!scannerTag.contains("ScannerId")) {
                scannerTag.putString("ScannerId", UUID.randomUUID().toString());
            }
            
            // Save the data to the scanner
            mainHandItem.set(DataComponents.CUSTOM_DATA, CustomData.of(scannerTag));
            
            // Notify the player
            player.sendOverlayMessage(Component.translatable("item.iska_utils.scanner_chip.transfer_success_mob", 
                    getLocalizedMobName(mobId)));
        }
        // Check if the chip has a generic target
        else if (chipTag.contains("TargetGeneric")) {
            // Remove block target and mob target if present
            scannerTag.remove("TargetBlock");
            scannerTag.remove("TargetMob");
            
            // Copy the generic target
            String genericTarget = chipTag.getString("TargetGeneric").orElse("");
            
            // If it's ores, append mining level (ores -> ores0, ores1, ores100, etc.)
            String targetToSet = genericTarget;
            if ("ores".equals(genericTarget) && offHandItem.getItem() instanceof ScannerChipItem chipItem) {
                int miningLevel = chipItem.getMiningLevel(offHandItem);
                targetToSet = "ores" + miningLevel;
            }
            
            scannerTag.putString("TargetGeneric", targetToSet);
            
            // Make sure the scanner has a unique ID
            if (!scannerTag.contains("ScannerId")) {
                scannerTag.putString("ScannerId", UUID.randomUUID().toString());
            }
            
            // Save the data to the scanner
            mainHandItem.set(DataComponents.CUSTOM_DATA, CustomData.of(scannerTag));
            
            // Notify the player
            if (genericTarget != null && genericTarget.startsWith("ores")) {
                player.sendOverlayMessage(Component.translatable("item.iska_utils.scanner_chip.transfer_success_ores"));
            } else if (ScannerMobCategories.isMobScanTarget(genericTarget)) {
                String mode = ScannerMobCategories.normalizedMode(genericTarget);
                player.sendOverlayMessage(Component.translatable("item.iska_utils.scanner_chip.transfer_success_mobs." + mode));
            }
        }
    }

    /**
     * Mining tier for ore filtering: 0 = any / none, 1–5 = wood through netherite,
     * 100 = other / unknown (e.g. tagged ores without a recognized {@code incorrect_for_*_tool} tier).
     * Uses {@link BlockTags#INCORRECT_FOR_*_TOOLS} (weaker tool is wrong → need stronger tier).
     * Netherite requirement also matches {@link Tags.Blocks#NEEDS_NETHERITE_TOOL} as an alias.
     */
    private int getBlockMiningLevel(BlockState blockState) {
        Holder<Block> holder = blockState.getBlock().builtInRegistryHolder();

        if (holder.is(BlockTags.INCORRECT_FOR_NETHERITE_TOOL)) {
            return 100;
        }
        if (holder.is(BlockTags.INCORRECT_FOR_DIAMOND_TOOL)) {
            return 5;
        }
        if (holder.is(Tags.Blocks.NEEDS_NETHERITE_TOOL)) {
            return 5;
        }
        if (holder.is(BlockTags.INCORRECT_FOR_IRON_TOOL)) {
            return 4;
        }
        if (holder.is(BlockTags.INCORRECT_FOR_STONE_TOOL)) {
            return 3;
        }
        if (holder.is(BlockTags.INCORRECT_FOR_WOODEN_TOOL)) {
            return 2;
        }

        int tier = 0;
        if (blockState.requiresCorrectToolForDrops()) {
            if (holder.is(BlockTags.MINEABLE_WITH_PICKAXE)
                    || holder.is(BlockTags.MINEABLE_WITH_AXE)
                    || holder.is(BlockTags.MINEABLE_WITH_SHOVEL)
                    || holder.is(BlockTags.MINEABLE_WITH_HOE)) {
                tier = 1;
            }
        }

        if (holder.is(Tags.Blocks.ORES) && (tier == 0 || tier == 1)) {
            return 100;
        }

        return tier;
    }
    
    /**
     * Scans the area for all ore blocks based on common tags
     */
    private void scanForAllOres(ServerPlayer player, ItemStack itemStack) {
        if (player.level() == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        UUID scannerId = tag.getString(SCANNER_ID_TAG).map(UUID::fromString).orElse(null);
        if (scannerId == null) {
            return;
        }
        
        // Extract mining level from target (ores0, ores1, ores100, etc.)
        String genericTarget = tag.getString(TARGET_GEN_TAG).orElse(null);
        int requiredMiningLevel = 0; // Default: no filter
        if (genericTarget != null && genericTarget.startsWith("ores")) {
            String levelStr = genericTarget.substring(4); // Get everything after "ores"
            if (!levelStr.isEmpty()) {
                try {
                    requiredMiningLevel = Integer.parseInt(levelStr);
                } catch (NumberFormatException e) {
                    // Invalid format, use default
                    requiredMiningLevel = 0;
                }
            }
        }
        
        // Get or create a list for this scanner's markers
        List<BlockPos> scannerMarkers = ACTIVE_MARKERS.computeIfAbsent(scannerId, k -> new ArrayList<>());
        
        // Get player position
        BlockPos playerPos = player.blockPosition();
        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;
        
        // Get scan range from scanner (or use default)
        int scanRange = getScanRange(itemStack);
        int maxBlocksScan = Config.scannerMaxBlocks;
        int baseTTL = Config.scannerMarkerTTL;
        int defaultAlpha = Config.scannerDefaultAlpha;
        
        // Force alpha to be 80 for consistency
        defaultAlpha = 0x80;
        
        // Scan from smallest configured range outward so nearby matches are prioritized
        List<Integer> rangeSteps = getRangeStepsUpTo(scanRange);
        
        // Create a set of currently existing marker positions
        Set<BlockPos> existingMarkerPositions = new HashSet<>(scannerMarkers);
        
        // Count how many new markers we can create (remaining capacity)
        // If maxBlocksScan is -1, it means infinite blocks
        boolean infiniteBlocks = maxBlocksScan == -1;
        int remainingCapacity = infiniteBlocks ? Integer.MAX_VALUE : maxBlocksScan - existingMarkerPositions.size();
        
        if (!infiniteBlocks && remainingCapacity <= 0) {
            sendScanLimitReachedMessage(player, maxBlocksScan);
            return;
        }
        
        // Scan in expanding radius rings
        int newMarkersFound = 0;
        boolean limitReached = false;
        
        // Load ore color mappings from config
        Map<String, Integer> oreColorMap = new HashMap<>();
        for (String entry : Config.scannerOreEntries) {
            String[] parts = entry.split(";");
            if (parts.length == 2) {
                String oreName = parts[0];
                try {
                    int color = Integer.parseInt(parts[1], 16);
                    oreColorMap.put(oreName, color);
                } catch (NumberFormatException e) {
                    LOGGER.error("Invalid color format in ore entry: {}", entry);
                }
            }
        }
        
        // Default color for ores not in the config from Config class
        int defaultOreColor = Config.scannerDefaultOreColor; // Default ore color without alpha
        
        scanLoop:
        for (int ringRange : rangeSteps) {
            int chunkRadius = Math.max(1, ringRange / 16);
            long ringRangeSq = (long) ringRange * ringRange;
            for (int chunkX = playerChunkX - chunkRadius; chunkX <= playerChunkX + chunkRadius; chunkX++) {
                for (int chunkZ = playerChunkZ - chunkRadius; chunkZ <= playerChunkZ + chunkRadius; chunkZ++) {
                ChunkPos currentChunkPos = new ChunkPos(chunkX, chunkZ);
                
                // Check if chunk is loaded
                if (!level.isLoaded(BlockPos.containing(currentChunkPos.getMiddleBlockX(), 0, currentChunkPos.getMiddleBlockZ()))) {
                    continue;
                }
                
                // Scan the chunk
                for (int x = currentChunkPos.getMinBlockX(); x <= currentChunkPos.getMaxBlockX(); x++) {
                    for (int z = currentChunkPos.getMinBlockZ(); z <= currentChunkPos.getMaxBlockZ(); z++) {
                        for (int y = level.getMinY(); y <= level.getMaxY(); y++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            
                            if (!isWithinHorizontalRange(player, pos, ringRangeSq)) {
                                continue;
                            }
                            
                            // Check if the block is an ore
                            BlockState blockState = level.getBlockState(pos);
                                Block block = blockState.getBlock();
                                String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
                                
                                // Check if it's an ore using the configured tags
                                boolean isOre = false;
                                
                                // Check if the block matches any of the configured ore tags
                                for (String tagName : Config.scannerOreTags) {
                                    // For tag format like "c:ores", we need to check if the block has this tag
                                    if (tagName.startsWith("#")) {
                                        // Remove the # prefix
                                        tagName = tagName.substring(1);
                                    }
                                    
                                    // Check if it's a wildcard tag (ends with *)
                                    if (tagName.endsWith("*")) {
                                        // Remove the * and use as a prefix
                                        String prefix = tagName.substring(0, tagName.length() - 1);
                                        
                                        // Check all tags that start with this prefix
                                        for (net.minecraft.tags.TagKey<Block> blockTag : block.builtInRegistryHolder().tags().toList()) {
                                            String tagId = blockTag.location().toString();
                                            if (tagId.startsWith(prefix)) {
                                                isOre = true;
                                                break;
                                            }
                                        }
                                    } else {
                                        // Split namespace and path
                                        String[] parts = tagName.split(":", 2);
                                        String namespace = parts.length > 1 ? parts[0] : "minecraft";
                                        String path = parts.length > 1 ? parts[1] : tagName;
                                        
                                        // Check if the block has this tag
                                        Identifier tagId = Identifier.tryParse(namespace + ":" + path);
                                        if (tagId != null && block.builtInRegistryHolder().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, tagId))) {
                                            isOre = true;
                                            break;
                                        }
                                    }
                                }
                                
                                // Fallback check based on block name if no tag matches
                                if (!isOre) {
                                    isOre = blockId.contains("ore") || 
                                           blockId.contains("raw_block") || 
                                           (blockId.contains("deepslate") && blockId.contains("ore"));
                                }
                                
                                // If mining level filter is active (not 0), check the block's mining level
                                if (isOre && requiredMiningLevel > 0) {
                                    int blockMiningLevel = getBlockMiningLevel(blockState);
                                    
                                    // Filter based on required level
                                    if (requiredMiningLevel == 100) {
                                        // Level 100: only modded/unknown mining levels
                                        if (blockMiningLevel != 100) {
                                            isOre = false; // Skip vanilla levels
                                        }
                                    } else {
                                        // Level 1-5: exact match required
                                        if (blockMiningLevel != requiredMiningLevel) {
                                            isOre = false; // Skip if doesn't match
                                        }
                                    }
                                }
                                
                                if (isOre) {
                                    // Skip positions already marked (do not re-scan)
                                    if (existingMarkerPositions.contains(pos)) {
                                        continue;
                                    }
                                    
                                    // Add the block position to the scanner's markers
                                    scannerMarkers.add(pos);
                                    existingMarkerPositions.add(pos);
                                    newMarkersFound++;
                                    
                                    // Calculate TTL multiplier based on the number of scanned blocks
                                    int finalTTL = baseTTL * TTL_MULTIPLIER;
                                    
                                    // Add TTL for this marker
                                    MARKER_TTL.put(pos, finalTTL);
                                    
                                    // Determine color based on ore type
                                    int color = defaultOreColor;
                                    
                                    // Check for exact match in config
                                    boolean colorFound = false;
                                    for (String entry : Config.scannerOreEntries) {
                                        String[] parts = entry.split(";");
                                        if (parts.length == 2) {
                                            String oreName = parts[0];
                                            // Exact match
                                            if (oreName.equals(blockId)) {
                                                try {
                                                    color = Integer.parseInt(parts[1], 16);
                                                    colorFound = true;
                                                    break;
                                                } catch (NumberFormatException e) {
                                                    // Use default color
                                                }
                                            }
                                            // Pattern match (with $ prefix)
                                            else if (oreName.startsWith("$")) {
                                                String searchTerm = oreName.substring(1).toLowerCase();
                                                if (blockId.toLowerCase().contains(searchTerm)) {
                                                    try {
                                                        color = Integer.parseInt(parts[1], 16);
                                                        colorFound = true;
                                                        break;
                                                    } catch (NumberFormatException e) {
                                                        // Use default color
                                                    }
                                                }
                                            }
                                            // Simple contains match (for entries like "gold", "iron", etc.)
                                            else if (blockId.toLowerCase().contains(oreName.toLowerCase())) {
                                                try {
                                                    color = Integer.parseInt(parts[1], 16);
                                                    colorFound = true;
                                                    break;
                                                } catch (NumberFormatException e) {
                                                    // Use default color
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Log debug info if needed
                                    if (colorFound) {
                                        LOGGER.debug("Applied color {} to block {}", Integer.toHexString(color), blockId);
                                    }
                                    
                                    // Add alpha channel to the color
                                    int colorWithAlpha = (0x80 << 24) | color;
                                    
                                    // Get the block name for display
                                    Component blockName = block.getName();
                                    String blockNameString = blockName.getString();
                                    
                                    // Use MarkRenderer to add the highlighted block on the client side with the block name
                                    net.unfamily.iskautils.network.ModMessages.sendAddHighlightWithNamePacket(player, pos, colorWithAlpha, finalTTL, blockNameString);
                                    
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
        }
        }
        
        if (limitReached) {
            sendScanLimitReachedMessage(player, maxBlocksScan);
        } else {
            player.sendOverlayMessage(Component.translatable("item.iska_utils.scanner.found_ores", 
                    newMarkersFound));
        }
    }

    /**
     * Scans the area for all mobs
     */
    private void scanForAllMobs(ServerPlayer player, ItemStack itemStack, String genericTarget) {
        if (player.level() == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        UUID scannerId = tag.getString(SCANNER_ID_TAG).map(UUID::fromString).orElse(null);
        if (scannerId == null) {
            return;
        }
        
        // Get player position
        BlockPos playerPos = player.blockPosition();
        
        // Get scan range from scanner (or use default)
        int scanRange = getScanRange(itemStack);
        int maxTTL = Config.scannerMarkerTTL;
        int maxMarkersScan = Config.scannerMaxBlocks;
        int defaultAlpha = Config.scannerDefaultAlpha;
        
        // Force alpha to be 80 for consistency
        defaultAlpha = 0x80;
        
        // Scan from smallest configured range outward
        List<Integer> rangeSteps = getRangeStepsUpTo(scanRange);
        
        int markersFound = 0;
        boolean limitReached = false;
        
        // Create a set of currently existing marker positions
        Set<BlockPos> existingMarkerPositions = new HashSet<>();
        if (ACTIVE_MARKERS.containsKey(scannerId)) {
            existingMarkerPositions.addAll(ACTIVE_MARKERS.get(scannerId));
        }
        
        // Get or create a list for this scanner's markers
        List<BlockPos> scannerMarkers = ACTIVE_MARKERS.computeIfAbsent(scannerId, k -> new ArrayList<>());
        
        boolean infiniteMarkers = maxMarkersScan == -1;
        int remainingCapacity = infiniteMarkers ? Integer.MAX_VALUE : maxMarkersScan - existingMarkerPositions.size();
        if (!infiniteMarkers && remainingCapacity <= 0) {
            sendScanLimitReachedMessage(player, maxMarkersScan);
            return;
        }
        
        // Load mob color mappings from config
        Map<String, Integer> mobColorMap = new HashMap<>();
        for (String entry : Config.scannerMobEntries) {
            String[] parts = entry.split(";");
            if (parts.length == 2) {
                String mobPattern = parts[0];
                try {
                    int color = Integer.parseInt(parts[1], 16);
                    mobColorMap.put(mobPattern, color);
                } catch (NumberFormatException e) {
                    LOGGER.error("Invalid color format in mob entry: {}", entry);
                }
            }
        }
        
        // Default color for mobs not in the config (magenta/purple)
        int defaultMobColor = Config.scannerDefaultMobColor; // Without alpha
        
        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;
        
        scanLoop:
        for (int ringRange : rangeSteps) {
            int chunkRadius = Math.max(1, ringRange / 16);
            double ringRangeSquared = (double) ringRange * ringRange;
            for (int chunkX = playerChunkX - chunkRadius; chunkX <= playerChunkX + chunkRadius; chunkX++) {
                for (int chunkZ = playerChunkZ - chunkRadius; chunkZ <= playerChunkZ + chunkRadius; chunkZ++) {
                ChunkPos currentChunkPos = new ChunkPos(chunkX, chunkZ);
                
                // Check if the chunk is loaded
                if (!level.isLoaded(BlockPos.containing(currentChunkPos.getMiddleBlockX(), 0, currentChunkPos.getMiddleBlockZ()))) {
                    continue;
                }
                
                // Search for all entities in the current chunk
                net.minecraft.world.phys.AABB chunkBox = new net.minecraft.world.phys.AABB(
                        currentChunkPos.getMinBlockX(), level.getMinY(), currentChunkPos.getMinBlockZ(),
                        currentChunkPos.getMaxBlockX(), level.getMaxY(), currentChunkPos.getMaxBlockZ()
                );
                List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, chunkBox)
                        .stream()
                        .filter(entity -> isWithinHorizontalRange(player, entity.getX(), entity.getZ(), ringRangeSquared)
                                && !(entity instanceof Player)
                                && ScannerMobCategories.matches(entity, genericTarget))
                        .toList();
                
                // Process the found entities
                for (LivingEntity entity : nearbyEntities) {
                    // Get the entity's position
                    BlockPos entityPos = entity.blockPosition();
                    
                    // Skip positions already marked (do not re-scan)
                    if (existingMarkerPositions.contains(entityPos)) {
                        continue;
                    }
                    
                    // Add the entity position to the scanner's markers
                    scannerMarkers.add(entityPos);
                    existingMarkerPositions.add(entityPos);
                    markersFound++;
                    
                    // Calculate the final TTL
                    int finalTTL = maxTTL * TTL_MULTIPLIER;
                    
                    // Add TTL for this marker
                    MARKER_TTL.put(entityPos, finalTTL);
                    
                    // Determine color based on mob type
                    String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
                    int color = defaultMobColor;
                    
                    // Check for exact match first
                    if (mobColorMap.containsKey(entityId)) {
                        color = mobColorMap.get(entityId);
                    } else {
                        // Check for pattern matches (with $ prefix)
                        for (Map.Entry<String, Integer> entry : mobColorMap.entrySet()) {
                            String pattern = entry.getKey();
                            if (pattern.startsWith("$")) {
                                String searchTerm = pattern.substring(1).toLowerCase();
                                if (entityId.toLowerCase().contains(searchTerm)) {
                                    color = entry.getValue();
                                    break;
                                }
                            }
                        }
                    }
                    
                    // Add alpha channel to the color (force to 80 for consistency)
                    int colorWithAlpha = (0x80 << 24) | color;
                    
                    // Get the entity name for display
                    String entityName = entity.getName().getString();
                    
                    // Use MarkRenderer to add a billboard marker on the client side with the entity name
                    net.unfamily.iskautils.network.ModMessages.sendAddBillboardWithNamePacket(player, entityPos, colorWithAlpha, finalTTL, entityName);
                    
                    if (!infiniteMarkers && markersFound >= remainingCapacity) {
                        limitReached = true;
                        break scanLoop;
                    }
                }
                
            }
        }
        }
        
        if (limitReached) {
            sendScanLimitReachedMessage(player, maxMarkersScan);
        } else {
            player.sendOverlayMessage(Component.translatable("item.iska_utils.scanner.found_all_mobs", markersFound));
        }
    }
} 

