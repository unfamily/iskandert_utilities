package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.unfamily.iskalib.transfer.LegacyItemHandlerResourceHandler;
import net.unfamily.iskautils.block.DeepDrawerExtractorBlock;
import net.unfamily.iskautils.util.DeepDrawerConnectorHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * BlockEntity for Deep Drawer Extractor
 * Extracts items from adjacent Deep Drawer using the optimized API
 * Has 5 output slots
 */
public class DeepDrawerExtractorBlockEntity extends BlockEntity implements WorldlyContainer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DeepDrawerExtractorBlockEntity.class);
    
    // Inventory with 5 slots
    private static final int INVENTORY_SIZE = 5;
    private final NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    
    // ItemHandler for capability (exposes the 5 slots)
    private final IItemHandler itemHandler = new ExtractorItemHandler();
    private final ResourceHandler<ItemResource> itemTransferHandler = LegacyItemHandlerResourceHandler.wrap(itemHandler);

    // Timer for extraction (interval from config, default 1 tick = 0.05 seconds, maximum speed)
    private int extractionTimer = 0;
    
    // Cache of found Deep Drawer (for performance)
    private BlockPos cachedDrawerPos = null;
    private int cacheValidTicks = 0;
    private static final int CACHE_VALIDITY_TICKS = 100; // Cache valid for 5 seconds
    
    // Filter configuration (fixed array from config, default 50)
    private final String[] filterFields;
    // Inverted filter configuration (same size as filterFields)
    private final String[] invertedFilterFields;
    // Blacklist + empty filters = extract everything (usable out of the box). Whitelist + empty = no extraction.
    private boolean isWhitelistMode = false;
    
    // GUI state flags for filter optimization
    private boolean reloadFilters = false; // Set to true when GUI closes to trigger filter reordering
    private boolean openGui = false; // Set to true when GUI is open
    
    // Redstone mode configuration
    private int redstoneMode = 0; // 0=NONE, 1=LOW, 2=HIGH, 3=PULSE
    private boolean previousRedstoneState = false; // For PULSE mode
    private int pulseIgnoreTimer = 0; // Timer to ignore redstone after pulse extraction
    private static final int PULSE_IGNORE_INTERVAL = 10; // Ignore pulses for 0.5 seconds after extraction
    
    // Data version for migration/reset logic ("V2" = current). Must always persist to NBT;
    // null on disk used to mean "wipe filters" which broke blocks that saved filters but never wrote this key.
    private String dataVersion = "V2";
    
    /**
     * Enum for redstone modes
     */
    public enum RedstoneMode {
        NONE(0),    // Gunpowder icon
        LOW(1),     // Redstone dust icon  
        HIGH(2),    // Redstone gui icon
        PULSE(3),   // Repeater icon
        DISABLED(4); // Barrier icon
        
        private final int value;
        
        RedstoneMode(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
        
        public static RedstoneMode fromValue(int value) {
            for (RedstoneMode mode : values()) {
                if (mode.value == value) return mode;
            }
            return NONE;
        }
        
        public RedstoneMode next() {
            return switch (this) {
                case NONE -> LOW;
                case LOW -> HIGH;
                case HIGH -> PULSE;
                case PULSE -> DISABLED;
                case DISABLED -> NONE;
            };
        }
    }
    
    public DeepDrawerExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEEP_DRAWER_EXTRACTOR.get(), pos, state);
        // Initialize filter array with size from config (default 50)
        int maxSlots = net.unfamily.iskautils.Config.deepDrawerExtractorMaxFilters;
        filterFields = new String[maxSlots];
        invertedFilterFields = new String[maxSlots];
        // Initialize all filter slots to empty strings
        for (int i = 0; i < maxSlots; i++) {
            filterFields[i] = "";
            invertedFilterFields[i] = "";
        }
    }
    
    /**
     * Gets the maximum number of filter slots (from config)
     */
    private int getMaxFilterSlots() {
        return filterFields.length;
    }
    
    /**
     * Called when GUI is opened
     */
    public void onGuiOpened() {
        openGui = true;
        // Don't reorder filters on open - user might modify them
    }
    
    /**
     * Called when GUI is closed
     */
    public void onGuiClosed() {
        openGui = false;
        reloadFilters = true; // Trigger filter reordering when GUI closes (only once)
    }
    
    /**
     * Reorders filters by computational cost (cheapest first)
     * Order: - (ID), @ (mod ID), & (macro), # (tag), ? (NBT)
     * This optimizes filter checking by trying cheaper filters first
     */
    private void reorderFilters() {
        // Helper method to get filter priority (lower = cheaper)
        java.util.function.Function<String, Integer> getPriority = (filter) -> {
            if (filter == null || filter.trim().isEmpty()) {
                return 999; // Empty filters go to the end
            }
            String trimmed = filter.trim();
            if (trimmed.startsWith("-")) return 1; // ID filter - cheapest
            if (trimmed.startsWith("@")) return 2; // Mod ID filter
            if (trimmed.startsWith("&")) return 3; // Macro filter
            if (trimmed.startsWith("#")) return 4; // Tag filter
            if (trimmed.startsWith("?")) return 5; // NBT filter - most expensive
            return 6; // Unknown format - goes last
        };
        
        // Collect non-empty filters with their indices
        java.util.List<java.util.Map.Entry<Integer, String>> normalFilters = new java.util.ArrayList<>();
        java.util.List<java.util.Map.Entry<Integer, String>> invertedFilters = new java.util.ArrayList<>();
        
        for (int i = 0; i < getMaxFilterSlots(); i++) {
            String filter = filterFields[i];
            if (filter != null && !filter.trim().isEmpty()) {
                normalFilters.add(new java.util.AbstractMap.SimpleEntry<>(i, filter));
            }
            String invertedFilter = invertedFilterFields[i];
            if (invertedFilter != null && !invertedFilter.trim().isEmpty()) {
                invertedFilters.add(new java.util.AbstractMap.SimpleEntry<>(i, invertedFilter));
            }
        }
        
        // Sort by priority (cheapest first)
        normalFilters.sort((a, b) -> Integer.compare(getPriority.apply(a.getValue()), getPriority.apply(b.getValue())));
        invertedFilters.sort((a, b) -> Integer.compare(getPriority.apply(a.getValue()), getPriority.apply(b.getValue())));
        
        // Clear arrays
        for (int i = 0; i < getMaxFilterSlots(); i++) {
            filterFields[i] = "";
            invertedFilterFields[i] = "";
        }
        
        // Re-insert sorted filters
        for (int i = 0; i < normalFilters.size() && i < getMaxFilterSlots(); i++) {
            filterFields[i] = normalFilters.get(i).getValue();
        }
        for (int i = 0; i < invertedFilters.size() && i < getMaxFilterSlots(); i++) {
            invertedFilterFields[i] = invertedFilters.get(i).getValue();
        }
    }
    
    @Override
    public void setChanged() {
        super.setChanged();
        // Force sync to client (like StructurePlacerMachineBlockEntity)
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
    
    /**
     * Gets the ItemHandler for capability registration
     */
    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public ResourceHandler<ItemResource> getItemTransferHandler() {
        return itemTransferHandler;
    }

    /**
     * Main tick called by the block
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, DeepDrawerExtractorBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }
        
        // Reorder filters if GUI was closed (reloadFilters flag is set)
        if (blockEntity.reloadFilters) {
            blockEntity.reorderFilters();
            blockEntity.reloadFilters = false;
            blockEntity.setChanged();
        }
        
        // Invalidate cache after a while
        blockEntity.cacheValidTicks++;
        if (blockEntity.cacheValidTicks >= CACHE_VALIDITY_TICKS) {
            blockEntity.cachedDrawerPos = null;
            blockEntity.cacheValidTicks = 0;
        }
        
        // Check redstone conditions before extraction
        int redstonePower = level.getBestNeighborSignal(pos);
        boolean hasRedstoneSignal = redstonePower > 0;
        boolean shouldExtract = false;
        
        RedstoneMode mode = RedstoneMode.fromValue(blockEntity.redstoneMode);
        switch (mode) {
            case NONE -> {
                // Mode 0: Always active, ignore redstone
                shouldExtract = true;
            }
            case LOW -> {
                // Mode 1: Only when redstone is OFF (low signal)
                shouldExtract = !hasRedstoneSignal;
            }
            case HIGH -> {
                // Mode 2: Only when redstone is ON (high signal)
                shouldExtract = hasRedstoneSignal;
            }
            case DISABLED -> {
                // Mode 4: Always disabled
                shouldExtract = false;
            }
            case PULSE -> {
                // Mode 3: Only on redstone pulse (low to high transition)
                // Decrement ignore timer if active
                if (blockEntity.pulseIgnoreTimer > 0) {
                    blockEntity.pulseIgnoreTimer--;
                }
                
                // Check for low-to-high transition only if not ignoring pulses
                if (blockEntity.pulseIgnoreTimer == 0) {
                    if (hasRedstoneSignal && !blockEntity.previousRedstoneState) {
                        // Detected rising edge (pulse) - extract immediately
                        shouldExtract = true;
                        // Start ignore timer
                        blockEntity.pulseIgnoreTimer = PULSE_IGNORE_INTERVAL;
                    }
                }
                
                // Update previous state
                blockEntity.previousRedstoneState = hasRedstoneSignal;
            }
        }
        
        // Check if we should skip extraction (whitelist mode with no valid filters = no extraction)
        boolean hasValidFilters = blockEntity.hasValidFilters();
        if (blockEntity.isWhitelistMode && !hasValidFilters) {
            // Whitelist mode with empty filters = don't extract anything, skip tick
            return;
        }
        
        // Early exit if buffer is full - no need to search drawer if we can't insert items
        if (blockEntity.isFull()) {
            return;
        }
        
        // Handle extraction based on mode
        if (mode == RedstoneMode.PULSE) {
            // PULSE mode: extract immediately when pulse is detected (already set shouldExtract above)
            if (shouldExtract) {
                blockEntity.tryExtractFromDrawer();
            }
            // Reset extraction timer in pulse mode (we don't use it)
            blockEntity.extractionTimer = 0;
        } else {
            // Other modes: use extraction interval timer
            blockEntity.extractionTimer++;
            int extractionInterval = net.unfamily.iskautils.Config.deepDrawerExtractorInterval;
            if (blockEntity.extractionTimer >= extractionInterval && shouldExtract) {
                blockEntity.extractionTimer = 0;
                blockEntity.tryExtractFromDrawer();
            }
        }
    }
    
    /**
     * Finds an adjacent Deep Drawer and extracts an item
     */
    private void tryExtractFromDrawer() {
        if (level == null) {
            return;
        }
        
        // If inventory is full, don't extract
        if (isFull()) {
            return;
        }
        
        // Find adjacent Deep Drawer
        DeepDrawersBlockEntity drawer = findAdjacentDrawer();
        if (drawer == null) {
            return;
        }
        
        // Get storage entries for direct iteration without creating a copy
        // This avoids O(n) memory allocation and copying overhead
        Set<Map.Entry<Integer, ItemStack>> storageEntries = drawer.getStorageEntries();
        if (storageEntries.isEmpty()) {
            return; // Drawer is empty
        }
        
        // Extract the first available item using the optimized API
        // Iterate over items directly without creating a copy of the HashMap
        
        for (Map.Entry<Integer, ItemStack> entry : storageEntries) {
            ItemStack drawerStack = entry.getValue();
            if (drawerStack == null || drawerStack.isEmpty()) {
                continue;
            }
            
            // Pre-compute item metadata once per item (used by multiple filter checks)
            Item item = drawerStack.getItem();
            net.minecraft.resources.Identifier itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            String itemIdStr = itemId.toString();
            String itemModId = itemId.getNamespace();
            
            // FIRST: Check inverted filter (applied before normal filter)
            // The inverted filter has opposite logic:
            // - If extractor is in allow (whitelist): inverted filter is in deny (blacklist)
            //   If inverted filter matches, item is NOT extracted and does NOT pass to normal filter
            // - If extractor is in deny (blacklist): inverted filter is in allow (whitelist)
            //   If inverted filter matches, item is extracted IMMEDIATELY, otherwise passes to normal filter
            boolean invertedFilterResult = matchesInvertedFilter(drawerStack, item, itemId, itemIdStr, itemModId);
            
            if (isWhitelistMode) {
                // Extractor in allow mode: inverted filter is in deny mode
                // If inverted filter matches, block extraction and skip to next item
                if (invertedFilterResult) {
                    continue; // Item matches inverted filter (deny), skip it
                }
                // Item doesn't match inverted filter, continue to normal filter
            } else {
                // Extractor in deny mode: inverted filter is in allow mode
                // If inverted filter matches, extract immediately
                if (invertedFilterResult) {
                    // Extract immediately without checking normal filter
                    ItemStack extracted = drawer.extractItemByStack(drawerStack, 1, false);
                    if (!extracted.isEmpty()) {
                        if (insertItem(extracted)) {
                            setChanged();
                            return; // Extraction successful, exit
                        } else {
                            // No space, put item back in drawer
                            drawer.insertItemIntoPhysicalSlotDirect(entry.getKey(), extracted, false);
                        }
                    }
                    continue; // Move to next item
                }
                // Item doesn't match inverted filter, continue to normal filter
            }
            
            // SECOND: Check normal filter (only if inverted filter didn't block/extract)
            if (!matchesFilter(drawerStack, item, itemId, itemIdStr, itemModId)) {
                continue; // Item doesn't match normal filter, skip it
            }
            
            // Item matches normal filter, extract it
            // Use extractItemByStack to extract the specific item we verified (with correct NBT)
            ItemStack extracted = drawer.extractItemByStack(drawerStack, 1, false);
            
            if (!extracted.isEmpty()) {
                // Try to insert into available slot
                if (insertItem(extracted)) {
                    setChanged();
                    return; // Extraction successful, exit
                } else {
                    // No space, put item back in drawer
                    drawer.insertItemIntoPhysicalSlotDirect(entry.getKey(), extracted, false);
                }
            }
        }
    }
    
    /**
     * Finds an adjacent Deep Drawer (in all 6 directions)
     */
    /**
     * Finds a connected Deep Drawer through connector network (extender, interface, extractor)
     * All connectors can connect to each other to find the drawer
     */
    @Nullable
    private DeepDrawersBlockEntity findAdjacentDrawer() {
        if (level == null) {
            return null;
        }
        
        // Use cache if valid
        if (cachedDrawerPos != null && cacheValidTicks < CACHE_VALIDITY_TICKS) {
            BlockEntity be = level.getBlockEntity(cachedDrawerPos);
            if (be instanceof DeepDrawersBlockEntity drawer) {
                return drawer;
            } else {
                // Cache invalid, reset
                cachedDrawerPos = null;
            }
        }
        
        // Search through connector network
        DeepDrawersBlockEntity drawer = DeepDrawerConnectorHelper.findConnectedDrawer(level, worldPosition);
        if (drawer != null && drawer.getBlockPos() != null) {
            // Cache the found position
            cachedDrawerPos = drawer.getBlockPos();
            cacheValidTicks = 0;
        }
        
        return drawer;
    }
    
    /**
     * Inserts an item into the first available slot
     */
    private boolean insertItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        
        // First try to merge with existing slots
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack existing = items.get(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
                int spaceLeft = existing.getMaxStackSize() - existing.getCount();
                if (spaceLeft > 0) {
                    int toAdd = Math.min(spaceLeft, stack.getCount());
                    existing.grow(toAdd);
                    items.set(i, existing);
                    setChanged();
                    return true;
                }
            }
        }
        
        // If merge is not possible, find empty slot
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, stack);
                setChanged();
                return true;
            }
        }
        
        return false; // No slot available
    }
    
    /**
     * Checks if the inventory is full
     */
    private boolean isFull() {
        for (ItemStack stack : items) {
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Gets the number of occupied slots in the buffer
     */
    public int getOccupiedSlots() {
        int count = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Gets the total number of slots in the buffer
     */
    public int getTotalSlots() {
        return INVENTORY_SIZE;
    }
    
    /**
     * Checks if there are any valid (non-empty) filter entries
     */
    private boolean hasValidFilters() {
        for (int i = 0; i < getMaxFilterSlots(); i++) {
            String filter = filterFields[i];
            if (filter != null && !filter.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if there are any valid (non-empty) inverted filter entries
     */
    private boolean hasValidInvertedFilters() {
        for (int i = 0; i < getMaxFilterSlots(); i++) {
            String filter = invertedFilterFields[i];
            if (filter != null && !filter.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if an ItemStack matches the inverted filter criteria
     * The inverted filter has opposite logic to the normal filter:
     * - If extractor is in allow (whitelist): inverted filter acts as deny (blacklist)
     * - If extractor is in deny (blacklist): inverted filter acts as allow (whitelist)
     * 
     * @param stack The ItemStack to check
     * @param item Pre-computed Item (to avoid repeated getItem() calls)
     * @param itemId Pre-computed ResourceLocation (to avoid repeated registry lookups)
     * @param itemIdStr Pre-computed item ID string (to avoid repeated toString() calls)
     * @param itemModId Pre-computed mod ID (to avoid repeated getNamespace() calls)
     */
    private boolean matchesInvertedFilter(ItemStack stack, Item item, net.minecraft.resources.Identifier itemId,
                                         String itemIdStr, String itemModId) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        
        // Check if item matches any inverted filter
        // After reorderFilters(), empty filters are at the end, so we can early exit
        boolean matchesAnyFilter = false;
        for (int i = 0; i < getMaxFilterSlots(); i++) {
            String filter = invertedFilterFields[i];
            // Early exit: after reorderFilters(), empty filters are at the end
            if (filter == null || filter.isEmpty()) {
                break; // No more valid filters
            }
            if (matchesFilterEntry(stack, item, itemId, itemIdStr, itemModId, filter)) {
                matchesAnyFilter = true;
                break;
            }
        }
        
        // The inverted filter always returns true if it matches (regardless of extractor mode)
        // The logic of what to do with the match is handled in tryExtractFromDrawer()
        return matchesAnyFilter;
    }
    
    /**
     * Checks if an ItemStack matches the filter criteria
     * Returns true if the item should be extracted based on whitelist/blacklist mode
     * 
     * @param stack The ItemStack to check
     * @param item Pre-computed Item (to avoid repeated getItem() calls)
     * @param itemId Pre-computed ResourceLocation (to avoid repeated registry lookups)
     * @param itemIdStr Pre-computed item ID string (to avoid repeated toString() calls)
     * @param itemModId Pre-computed mod ID (to avoid repeated getNamespace() calls)
     */
    private boolean matchesFilter(ItemStack stack, Item item, net.minecraft.resources.Identifier itemId,
                                  String itemIdStr, String itemModId) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        
        // Check if item matches any filter
        // After reorderFilters(), empty filters are at the end, so we can early exit
        boolean matchesAnyFilter = false;
        boolean hasAnyFilter = false;
        for (int i = 0; i < getMaxFilterSlots(); i++) {
            String filter = filterFields[i];
            // Early exit: after reorderFilters(), empty filters are at the end
            if (filter == null || filter.isEmpty()) {
                break; // No more valid filters
            }
            hasAnyFilter = true;
            if (matchesFilterEntry(stack, item, itemId, itemIdStr, itemModId, filter)) {
                matchesAnyFilter = true;
                break;
            }
        }
        
        // If no valid filters, whitelist mode = extract nothing, blacklist mode = extract everything
        if (!hasAnyFilter) {
            return !isWhitelistMode; // Blacklist with no filters = extract all
        }
        
        // Whitelist: extract only if matches a filter
        // Blacklist: extract only if doesn't match any filter
        return isWhitelistMode ? matchesAnyFilter : !matchesAnyFilter;
    }
    
    /**
     * Checks if an ItemStack matches a single filter entry
     * Supports:
     * - ID filter: -minecraft:diamond
     * - Tag filter: #c:ingots
     * - Mod ID filter: @iska_utils
     * - NBT filter: ?"apotheosis:rarity":"apotheosis:mythic"
     * - Macro filter: &enchanted, &damaged
     * 
     * @param stack The ItemStack to check
     * @param item Pre-computed Item (to avoid repeated getItem() calls)
     * @param itemId Pre-computed ResourceLocation (to avoid repeated registry lookups)
     * @param itemIdStr Pre-computed item ID string (to avoid repeated toString() calls)
     * @param itemModId Pre-computed mod ID (to avoid repeated getNamespace() calls)
     * @param filter The filter string to match against (already trimmed)
     */
    private boolean matchesFilterEntry(ItemStack stack, Item item, net.minecraft.resources.Identifier itemId,
                                      String itemIdStr, String itemModId, String filter) {
        if (filter == null || filter.isEmpty()) {
            return false;
        }
        
        // ID filter: -minecraft:diamond (cheapest - check first)
        if (filter.startsWith("-")) {
            String idFilter = filter.substring(1);
            return itemIdStr.equals(idFilter);
        }
        
        // Mod ID filter: @iska_utils (cheap - check second)
        // Supports abbreviations, e.g., @meka matches mekanism, mekanismtools, etc.
        if (filter.startsWith("@")) {
            String modIdFilter = filter.substring(1);
            // Check if mod ID starts with the filter (supports abbreviations)
            return itemModId.startsWith(modIdFilter);
        }
        
        // Macro filter: &enchanted, &damaged (cheap - check third)
        if (filter.startsWith("&")) {
            String macroFilter = filter.substring(1).toLowerCase();
            return switch (macroFilter) {
                case "enchanted" -> stack.isEnchanted();
                case "damaged" -> stack.isDamaged();
                default -> false;
            };
        }
        
        // Tag filter: #c:ingots (more expensive - check fourth)
        if (filter.startsWith("#")) {
            String tagFilter = filter.substring(1);
            try {
                net.minecraft.resources.Identifier tagId = net.minecraft.resources.Identifier.parse(tagFilter);
                net.minecraft.tags.TagKey<Item> itemTag = net.minecraft.tags.ItemTags.create(tagId);
                return item.builtInRegistryHolder().is(itemTag);
            } catch (Exception e) {
                // Invalid tag format, ignore
                return false;
            }
        }
        
        // NBT filter: ?"apotheosis:rarity":"apotheosis:mythic" (most expensive - check last)
        if (filter.startsWith("?")) {
            String nbtFilter = filter.substring(1);
            try {
                if (level != null) {
                    String nbtString = stack.getComponentsPatch().toString();
                    return nbtString.contains(nbtFilter);
                }
            } catch (Exception e) {
                // Error reading NBT, consider not matching
                return false;
            }
        }
        
        // Default: treat as direct ID match (without prefix)
        return itemIdStr.equals(filter);
    }
    
    // ===== NBT Save/Load =====
    
    @Override
    protected void saveAdditional(@NotNull ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        
        // Save filter fields as index-value pairs (version 5 - added inverted filters)
        ValueOutput filterTag = output.child("filter_config");
        ValueOutput.ValueOutputList filterList = filterTag.childrenList("filters");
        ValueOutput.ValueOutputList invertedFilterList = filterTag.childrenList("inverted_filters");
        int maxSlots = getMaxFilterSlots();
        for (int i = 0; i < maxSlots; i++) {
            String filter = filterFields[i];
            // Only save non-empty filters to reduce NBT size
            if (filter != null && !filter.trim().isEmpty()) {
                ValueOutput filterEntry = filterList.addChild();
                filterEntry.putInt("index", i);
                filterEntry.putString("value", filter);
            }
            // Save inverted filters
            String invertedFilter = invertedFilterFields[i];
            if (invertedFilter != null && !invertedFilter.trim().isEmpty()) {
                ValueOutput invertedFilterEntry = invertedFilterList.addChild();
                invertedFilterEntry.putInt("index", i);
                invertedFilterEntry.putString("value", invertedFilter);
            }
        }
        if (filterList.isEmpty()) filterTag.discard("filters");
        if (invertedFilterList.isEmpty()) filterTag.discard("inverted_filters");
        filterTag.putBoolean("whitelist_mode", isWhitelistMode);
        filterTag.putInt("filter_version", 5); // Version 5 = index-value pairs + inverted filters
        filterTag.putInt("filter_slot_count", maxSlots); // Save slot count for migration
        if (filterTag.isEmpty()) output.discard("filter_config");
        
        // Save redstone mode
        output.putInt("redstoneMode", redstoneMode);
        output.putBoolean("previousRedstoneState", previousRedstoneState);
        output.putInt("pulseIgnoreTimer", pulseIgnoreTimer);
        
        // Always persist so chunk reload / sync packets do not trigger a false "legacy" wipe in loadAdditional
        output.putString("dataVersion", dataVersion != null ? dataVersion : "V2");
    }
    
    @Override
    protected void loadAdditional(@NotNull ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, items);
        
        // filterFields is already initialized in constructor with size from config
        int maxSlots = getMaxFilterSlots();
        
        // Load filter fields (with automatic conversion from old format)
        java.util.Optional<ValueInput> filterTagOpt = input.child("filter_config");
        if (filterTagOpt.isPresent()) {
            ValueInput filterTag = filterTagOpt.get();
            
            // Check version: version 4 = index-value pairs, version 3 = fixed array, version 2 = dynamic list, version 1 or no version = old array format
            int version = filterTag.getIntOr("filter_version", 1);
            
            // Initialize all slots to empty first
            for (int i = 0; i < maxSlots; i++) {
                filterFields[i] = "";
                invertedFilterFields[i] = "";
            }
            
            // Load inverted filters (version 5+)
            if (version >= 5) {
                for (ValueInput invertedFilterEntry : filterTag.childrenListOrEmpty("inverted_filters")) {
                    int index = invertedFilterEntry.getIntOr("index", -1);
                    String value = invertedFilterEntry.getStringOr("value", "");
                    if (index >= 0 && index < maxSlots) {
                        invertedFilterFields[index] = value;
                    }
                }
            }
            
            if (version >= 4) {
                for (ValueInput filterEntry : filterTag.childrenListOrEmpty("filters")) {
                    int index = filterEntry.getIntOr("index", -1);
                    String value = filterEntry.getStringOr("value", "");
                    if (index >= 0 && index < maxSlots) {
                        filterFields[index] = value;
                    }
                }
            }
            
            isWhitelistMode = filterTag.getBooleanOr("whitelist_mode", false);
        } else {
            // No filter_config tag: initialize all to empty
            for (int i = 0; i < maxSlots; i++) {
                filterFields[i] = "";
                invertedFilterFields[i] = "";
            }
            isWhitelistMode = false;
        }
        
        // Load redstone mode
        redstoneMode = input.getIntOr("redstoneMode", redstoneMode);
        previousRedstoneState = input.getBooleanOr("previousRedstoneState", previousRedstoneState);
        pulseIgnoreTimer = input.getIntOr("pulseIgnoreTimer", pulseIgnoreTimer);
        
        // Load data version; missing key means "never had migration marker" — do not wipe loaded filters (see saveAdditional)
        dataVersion = input.getStringOr("dataVersion", "V2");
        
        // Explicit non-V2 tag: legacy reset (blocks that stored an older marker)
        if (!dataVersion.equals("V2")) {
            for (int i = 0; i < maxSlots; i++) {
                filterFields[i] = "";
                invertedFilterFields[i] = "";
            }
            isWhitelistMode = false;
            dataVersion = "V2";
            setChanged();
        }
    }
    
    // ===== Filter Configuration Getters/Setters =====
    
    public java.util.List<String> getFilterFields() {
        java.util.List<String> list = new java.util.ArrayList<>();
        int maxSlots = getMaxFilterSlots();
        for (int i = 0; i < maxSlots; i++) {
            list.add(filterFields[i] != null ? filterFields[i] : "");
        }
        return list; // Return copy as list for compatibility
    }
    
    public void setFilterFields(java.util.List<String> fields) {
        int maxSlots = getMaxFilterSlots();
        // Initialize all to empty
        for (int i = 0; i < maxSlots; i++) {
            filterFields[i] = "";
        }
        // Fill from provided list (backward compatibility - assumes sequential indices)
        if (fields != null) {
            for (int i = 0; i < Math.min(maxSlots, fields.size()); i++) {
                String field = fields.get(i);
                if (field != null) {
                    field = field.trim();
                    // Remove single quotes (') for KubeJS compatibility
                    field = field.replace("'", "");
                    filterFields[i] = field;
                }
            }
        }
        setChanged();
        // Force sync to client (like SmartTimerBlockEntity)
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
    
    /**
     * Gets the inverted filter fields as a list
     */
    public java.util.List<String> getInvertedFilterFields() {
        java.util.List<String> list = new java.util.ArrayList<>();
        int maxSlots = getMaxFilterSlots();
        for (int i = 0; i < maxSlots; i++) {
            list.add(invertedFilterFields[i] != null ? invertedFilterFields[i] : "");
        }
        return list; // Return copy as list for compatibility
    }
    
    /**
     * Sets inverted filter fields from a list
     */
    public void setInvertedFilterFields(java.util.List<String> fields) {
        int maxSlots = getMaxFilterSlots();
        // Initialize all to empty
        for (int i = 0; i < maxSlots; i++) {
            invertedFilterFields[i] = "";
        }
        // Fill from provided list (backward compatibility - assumes sequential indices)
        if (fields != null) {
            for (int i = 0; i < Math.min(maxSlots, fields.size()); i++) {
                String field = fields.get(i);
                if (field != null) {
                    field = field.trim();
                    // Remove single quotes (') for KubeJS compatibility
                    field = field.replace("'", "");
                    invertedFilterFields[i] = field;
                }
            }
        }
        setChanged();
        // Force sync to client
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
    
    /**
     * Sets inverted filter fields from a map of index-value pairs.
     * Indices outside the valid range (0 to maxSlots-1) are ignored.
     */
    public void setInvertedFilterFieldsFromMap(java.util.Map<Integer, String> filterMap) {
        int maxSlots = getMaxFilterSlots();
        // Initialize all to empty first
        for (int i = 0; i < maxSlots; i++) {
            invertedFilterFields[i] = "";
        }
        // Fill from provided map, ignoring out-of-range indices
        if (filterMap != null) {
            for (java.util.Map.Entry<Integer, String> entry : filterMap.entrySet()) {
                int index = entry.getKey();
                String value = entry.getValue();
                if (index >= 0 && index < maxSlots && value != null) {
                    value = value.trim();
                    // Remove single quotes (') for KubeJS compatibility
                    value = value.replace("'", "");
                    invertedFilterFields[index] = value;
                }
            }
        }
        setChanged();
        // Force sync to client
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
    
    /**
     * Sets filter fields from a map of index-value pairs.
     * Indices outside the valid range (0 to maxSlots-1) are ignored.
     */
    public void setFilterFieldsFromMap(java.util.Map<Integer, String> filterMap) {
        int maxSlots = getMaxFilterSlots();
        // Initialize all to empty first
        for (int i = 0; i < maxSlots; i++) {
            filterFields[i] = "";
        }
        // Fill from provided map, ignoring out-of-range indices
        if (filterMap != null) {
            for (java.util.Map.Entry<Integer, String> entry : filterMap.entrySet()) {
                int index = entry.getKey();
                String value = entry.getValue();
                // Ignore indices outside valid range
                if (index >= 0 && index < maxSlots) {
                    if (value != null) {
                        // Debug: log value before processing
                        // LOGGER.debug("setFilterFieldsFromMap: index={}, original length={}, value={}", index, value.length(), value);
                        value = value.trim();
                        // Remove single quotes (') for KubeJS compatibility
                        value = value.replace("'", "");
                        // Debug: log value after processing
                        // LOGGER.debug("setFilterFieldsFromMap: index={}, processed length={}, value={}", index, value.length(), value);
                        filterFields[index] = value;
                        // Debug: log value after setting
                        // LOGGER.debug("setFilterFieldsFromMap: index={}, stored length={}, value={}", index, filterFields[index] != null ? filterFields[index].length() : 0, filterFields[index]);
                    } else {
                        filterFields[index] = "";
                    }
                }
                // If index is out of range, simply ignore it (as requested)
            }
        }
        setChanged();
        // Force sync to client
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
    
    /**
     * Gets a filter field at the specified index
     */
    public String getFilterField(int index) {
        int maxSlots = getMaxFilterSlots();
        if (index >= 0 && index < maxSlots) {
            return filterFields[index] != null ? filterFields[index] : "";
        }
        return "";
    }
    
    /**
     * Sets a filter field at the specified index
     */
    public void setFilterField(int index, String filter) {
        int maxSlots = getMaxFilterSlots();
        if (index >= 0 && index < maxSlots) {
            if (filter != null) {
                filter = filter.trim();
                // Remove single quotes (') for KubeJS compatibility
                filter = filter.replace("'", "");
            }
            filterFields[index] = filter != null ? filter : "";
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    }
    
    /**
     * Gets the maximum number of filter slots (from config)
     */
    public int getFilterFieldCount() {
        return getMaxFilterSlots();
    }
    
    public boolean isWhitelistMode() {
        return isWhitelistMode;
    }
    
    public void setWhitelistMode(boolean whitelistMode) {
        if (this.isWhitelistMode != whitelistMode) {
            this.isWhitelistMode = whitelistMode;
            setChanged(); // setChanged() override already calls sendBlockUpdated()
        }
    }
    
    // ===== Redstone Mode Getters/Setters =====
    
    public int getRedstoneMode() {
        return redstoneMode;
    }
    
    public void setRedstoneMode(int redstoneMode) {
        int oldMode = this.redstoneMode;
        this.redstoneMode = redstoneMode % 5; // Ensure mode is always 0-4
        setChanged();
        // Force sync to client
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            // Log for debugging
            if (oldMode != this.redstoneMode) {
                LOGGER.debug("DeepDrawerExtractor: Redstone mode changed from {} to {}", oldMode, this.redstoneMode);
                // Reset pulse timer when switching modes
                if (RedstoneMode.fromValue(this.redstoneMode) != RedstoneMode.PULSE) {
                    pulseIgnoreTimer = 0;
                }
            }
        }
    }
    
    // ===== Network Synchronization =====
    
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    
    @Override
    public CompoundTag getUpdateTag(@NotNull HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        tag.merge(this.saveCustomOnly(provider));
        return tag;
    }
    
    // ===== WorldlyContainer Implementation =====
    
    private static final int[] SLOTS_FOR_ALL_SIDES = new int[]{0, 1, 2, 3, 4};
    
    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
    }
    
    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= INVENTORY_SIZE) {
            return ItemStack.EMPTY;
        }
        return items.get(slot);
    }
    
    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }
    
    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }
    
    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }
    
    @Override
    public boolean stillValid(Player player) {
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(this.worldPosition.getX() + 0.5, 
                                    this.worldPosition.getY() + 0.5, 
                                    this.worldPosition.getZ() + 0.5) <= 64.0 &&
               level.getBlockState(worldPosition).getBlock() instanceof DeepDrawerExtractorBlock;
    }
    
    @Override
    public void clearContent() {
        items.clear();
    }
    
    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS_FOR_ALL_SIDES; // Tutti gli slot sono accessibili da tutte le direzioni
    }
    
    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false; // Non accetta inserimenti dall'esterno, solo estrazioni
    }
    
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true; // Permette estrazione da tutte le direzioni
    }
    
    // ===== IItemHandler Implementation =====
    
    /**
     * ItemHandler che espone i 5 slot dell'estrattore
     * Permette estrazione ma non inserimento (solo output)
     */
    private class ExtractorItemHandler implements IItemHandlerModifiable {
        
        @Override
        public int getSlots() {
            return INVENTORY_SIZE;
        }
        
        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= INVENTORY_SIZE) {
                return ItemStack.EMPTY;
            }
            return items.get(slot).copy();
        }
        
        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            // Non accetta inserimenti dall'esterno
            return stack;
        }
        
        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < 0 || slot >= INVENTORY_SIZE || amount <= 0) {
                return ItemStack.EMPTY;
            }
            
            ItemStack existing = items.get(slot);
            if (existing.isEmpty()) {
                return ItemStack.EMPTY;
            }
            
            int toExtract = Math.min(amount, existing.getCount());
            ItemStack extracted = existing.copy();
            extracted.setCount(toExtract);
            
            if (!simulate) {
                if (toExtract >= existing.getCount()) {
                    items.set(slot, ItemStack.EMPTY);
                } else {
                    ItemStack newStack = existing.copy();
                    newStack.shrink(toExtract);
                    items.set(slot, newStack);
                }
                setChanged();
            }
            
            return extracted;
        }
        
        @Override
        public int getSlotLimit(int slot) {
            return 64; // Stack size standard
        }
        
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return false; // Non accetta inserimenti
        }
        
        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            if (slot < 0 || slot >= INVENTORY_SIZE) {
                return;
            }
            items.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            setChanged();
        }
    }
}
