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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
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
    
    // Timer for extraction (interval from config, default 1 tick = 0.05 seconds, maximum speed)
    private int extractionTimer = 0;
    
    // Cache of found Deep Drawer (for performance)
    private BlockPos cachedDrawerPos = null;
    private int cacheValidTicks = 0;
    private static final int CACHE_VALIDITY_TICKS = 100; // Cache valid for 5 seconds
    
    // Filter configuration (fixed array from config, default 50)
    private final String[] filterFields;
    private boolean isWhitelistMode = true; // false = blacklist, true = whitelist (default: true to prevent random extraction)
    
    // Redstone mode configuration
    private int redstoneMode = 0; // 0=NONE, 1=LOW, 2=HIGH, 3=PULSE
    private boolean previousRedstoneState = false; // For PULSE mode
    private int pulseIgnoreTimer = 0; // Timer to ignore redstone after pulse extraction
    private static final int PULSE_IGNORE_INTERVAL = 10; // Ignore pulses for 0.5 seconds after extraction
    
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
        // Initialize all filter slots to empty strings
        for (int i = 0; i < maxSlots; i++) {
            filterFields[i] = "";
        }
    }
    
    /**
     * Gets the maximum number of filter slots (from config)
     */
    private int getMaxFilterSlots() {
        return filterFields.length;
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
    
    /**
     * Main tick called by the block
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, DeepDrawerExtractorBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
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
        
        // Get all items in drawer to extract one at a time
        Map<Integer, ItemStack> allItems = drawer.getAllItems();
        if (allItems.isEmpty()) {
            return; // Drawer is empty
        }
        
        // Extract the first available item using the optimized API
        // Iterate over items to find one that matches the filter criteria
        
        for (Map.Entry<Integer, ItemStack> entry : allItems.entrySet()) {
            ItemStack drawerStack = entry.getValue();
            if (drawerStack == null || drawerStack.isEmpty()) {
                continue;
            }
            
            // Check if item matches filter criteria
            if (!matchesFilter(drawerStack)) {
                continue; // Item doesn't match filter, skip it
            }
            
            // Item matches filter, extract it
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
     * Checks if an ItemStack matches the filter criteria
     * Returns true if the item should be extracted based on whitelist/blacklist mode
     */
    private boolean matchesFilter(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        
        // If no valid filters, whitelist mode = extract nothing, blacklist mode = extract everything
        if (!hasValidFilters()) {
            return !isWhitelistMode; // Blacklist with no filters = extract all
        }
        
        // Check if item matches any filter
        boolean matchesAnyFilter = false;
        for (int i = 0; i < getMaxFilterSlots(); i++) {
            String filter = filterFields[i];
            if (filter != null && !filter.trim().isEmpty()) {
                if (matchesFilterEntry(stack, filter.trim())) {
                    matchesAnyFilter = true;
                    break;
                }
            }
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
     */
    private boolean matchesFilterEntry(ItemStack stack, String filter) {
        if (filter == null || filter.isEmpty()) {
            return false;
        }
        
        Item item = stack.getItem();
        net.minecraft.resources.ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        String itemIdStr = itemId.toString();
        
        // ID filter: -minecraft:diamond
        if (filter.startsWith("-")) {
            String idFilter = filter.substring(1);
            return itemIdStr.equals(idFilter);
        }
        
        // Tag filter: #c:ingots
        if (filter.startsWith("#")) {
            String tagFilter = filter.substring(1);
            try {
                net.minecraft.resources.ResourceLocation tagId = net.minecraft.resources.ResourceLocation.parse(tagFilter);
                net.minecraft.tags.TagKey<Item> itemTag = net.minecraft.tags.ItemTags.create(tagId);
                return item.builtInRegistryHolder().is(itemTag);
            } catch (Exception e) {
                // Invalid tag format, ignore
                return false;
            }
        }
        
        // Mod ID filter: @iska_utils
        if (filter.startsWith("@")) {
            String modIdFilter = filter.substring(1);
            String itemModId = itemId.getNamespace();
            return itemModId.equals(modIdFilter);
        }
        
        // NBT filter: ?"apotheosis:rarity":"apotheosis:mythic"
        // Uses the same method as the original hardcoded example
        if (filter.startsWith("?")) {
            String nbtFilter = filter.substring(1);
            try {
                if (level != null) {
                    var tag = stack.save(level.registryAccess());
                    if (tag instanceof CompoundTag compoundTag) {
                        // Serialize CompoundTag to string and search for exact string (like original example)
                        String nbtString = compoundTag.toString();
                        // Search for exact string with quotes (same as original: "apotheosis:rarity":"apotheosis:mythic")
                        return nbtString.contains(nbtFilter);
                    }
                }
            } catch (Exception e) {
                // Error reading NBT, consider not matching
                return false;
            }
        }
        
        // Macro filter: &enchanted, &damaged
        if (filter.startsWith("&")) {
            String macroFilter = filter.substring(1).toLowerCase();
            return switch (macroFilter) {
                case "enchanted" -> stack.isEnchanted();
                case "damaged" -> stack.isDamaged();
                default -> false;
            };
        }
        
        // Default: treat as direct ID match (without prefix)
        return itemIdStr.equals(filter);
    }
    
    // ===== NBT Save/Load =====
    
    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        
        // Save filter fields as index-value pairs (version 4)
        CompoundTag filterTag = new CompoundTag();
        net.minecraft.nbt.ListTag filterList = new net.minecraft.nbt.ListTag();
        int maxSlots = getMaxFilterSlots();
        for (int i = 0; i < maxSlots; i++) {
            String filter = filterFields[i];
            // Only save non-empty filters to reduce NBT size
            if (filter != null && !filter.trim().isEmpty()) {
                CompoundTag filterEntry = new CompoundTag();
                filterEntry.putInt("index", i);
                filterEntry.putString("value", filter);
                filterList.add(filterEntry);
            }
        }
        filterTag.put("filters", filterList);
        filterTag.putBoolean("whitelist_mode", isWhitelistMode);
        filterTag.putInt("filter_version", 4); // Version 4 = index-value pairs
        filterTag.putInt("filter_slot_count", maxSlots); // Save slot count for migration
        tag.put("filter_config", filterTag);
        
        // Save redstone mode
        tag.putInt("redstoneMode", redstoneMode);
        tag.putBoolean("previousRedstoneState", previousRedstoneState);
        tag.putInt("pulseIgnoreTimer", pulseIgnoreTimer);
    }
    
    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ContainerHelper.loadAllItems(tag, items, provider);
        
        // filterFields is already initialized in constructor with size from config
        int maxSlots = getMaxFilterSlots();
        
        // Load filter fields (with automatic conversion from old format)
        if (tag.contains("filter_config", CompoundTag.TAG_COMPOUND)) {
            CompoundTag filterTag = tag.getCompound("filter_config");
            
            // Check version: version 4 = index-value pairs, version 3 = fixed array, version 2 = dynamic list, version 1 or no version = old array format
            int version = filterTag.contains("filter_version") ? filterTag.getInt("filter_version") : 1;
            
            // Initialize all slots to empty first
            for (int i = 0; i < maxSlots; i++) {
                filterFields[i] = "";
            }
            
            if (version >= 4) {
                // New format: index-value pairs
                if (filterTag.contains("filters", CompoundTag.TAG_LIST)) {
                    net.minecraft.nbt.ListTag filterList = filterTag.getList("filters", CompoundTag.TAG_COMPOUND);
                    for (int i = 0; i < filterList.size(); i++) {
                        CompoundTag filterEntry = filterList.getCompound(i);
                        if (filterEntry.contains("index", CompoundTag.TAG_INT) && filterEntry.contains("value", CompoundTag.TAG_STRING)) {
                            int index = filterEntry.getInt("index");
                            String value = filterEntry.getString("value");
                            // Ignore indices outside valid range (0 to maxSlots-1)
                            if (index >= 0 && index < maxSlots) {
                                filterFields[index] = value != null ? value : "";
                            }
                            // If index is out of range, simply ignore it (as requested)
                        }
                    }
                }
            } else if (version >= 3) {
                // Old format: fixed array (size from config)
                if (filterTag.contains("filters", CompoundTag.TAG_LIST)) {
                    net.minecraft.nbt.ListTag filterList = filterTag.getList("filters", CompoundTag.TAG_STRING);
                    int savedSlotCount = filterTag.contains("filter_slot_count") ? filterTag.getInt("filter_slot_count") : maxSlots;
                    // Load up to the minimum of saved count and current config
                    int loadCount = Math.min(maxSlots, Math.min(savedSlotCount, filterList.size()));
                    for (int i = 0; i < loadCount; i++) {
                        String filter = filterList.getString(i);
                        filterFields[i] = filter != null ? filter : "";
                    }
                    // Remaining slots already initialized to empty above
                }
            } else if (version >= 2) {
                // Old format: dynamic list - convert to fixed array
                if (filterTag.contains("filters", CompoundTag.TAG_LIST)) {
                    net.minecraft.nbt.ListTag filterList = filterTag.getList("filters", CompoundTag.TAG_STRING);
                    int loadCount = Math.min(maxSlots, filterList.size());
                    for (int i = 0; i < loadCount; i++) {
                        String filter = filterList.getString(i);
                        if (filter != null && !filter.isEmpty()) {
                            filterFields[i] = filter;
                        } else {
                            filterFields[i] = "";
                        }
                    }
                    // Initialize remaining slots to empty
                    for (int i = loadCount; i < maxSlots; i++) {
                        filterFields[i] = "";
                    }
                }
            } else {
                // Old format: convert from array (field_0, field_1, ..., field_10)
                int loadCount = Math.min(11, maxSlots);
                for (int i = 0; i < loadCount; i++) {
                    String key = "field_" + i;
                    if (filterTag.contains(key, CompoundTag.TAG_STRING)) {
                        String value = filterTag.getString(key);
                        if (value != null && !value.isEmpty()) {
                            filterFields[i] = value;
                        } else {
                            filterFields[i] = "";
                        }
                    } else {
                        filterFields[i] = "";
                    }
                }
                // Initialize remaining slots to empty
                for (int i = loadCount; i < maxSlots; i++) {
                    filterFields[i] = "";
                }
            }
            
            isWhitelistMode = filterTag.getBoolean("whitelist_mode");
        } else {
            // No filter_config tag: initialize all to empty
            for (int i = 0; i < maxSlots; i++) {
                filterFields[i] = "";
            }
        }
        
        // Load redstone mode
        if (tag.contains("redstoneMode")) {
            redstoneMode = tag.getInt("redstoneMode");
        }
        if (tag.contains("previousRedstoneState")) {
            previousRedstoneState = tag.getBoolean("previousRedstoneState");
        }
        if (tag.contains("pulseIgnoreTimer")) {
            pulseIgnoreTimer = tag.getInt("pulseIgnoreTimer");
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
                        value = value.trim();
                        // Remove single quotes (') for KubeJS compatibility
                        value = value.replace("'", "");
                        filterFields[index] = value;
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
        saveAdditional(tag, provider);
        return tag;
    }
    
    @Override
    public void onDataPacket(net.minecraft.network.Connection net, 
                            net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt, 
                            @NotNull HolderLookup.Provider lookupProvider) {
        super.onDataPacket(net, pkt, lookupProvider);
        if (pkt.getTag() != null) {
            loadAdditional(pkt.getTag(), lookupProvider);
        }
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
