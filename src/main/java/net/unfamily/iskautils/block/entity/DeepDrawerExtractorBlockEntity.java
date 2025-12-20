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
    
    // Filter configuration
    private static final int FILTER_FIELD_COUNT = 11;
    private final String[] filterFields = new String[FILTER_FIELD_COUNT];
    private boolean isWhitelistMode = true; // false = blacklist, true = whitelist (default: true to prevent random extraction)
    
    // Redstone mode configuration
    private int redstoneMode = 0; // 0=NONE, 1=LOW, 2=HIGH, 3=PULSE
    private boolean previousRedstoneState = false; // For PULSE mode
    
    /**
     * Enum for redstone modes
     */
    public enum RedstoneMode {
        NONE(0),    // Gunpowder icon
        LOW(1),     // Redstone dust icon  
        HIGH(2),    // Redstone gui icon
        PULSE(3);   // Repeater icon
        
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
                case PULSE -> NONE;
            };
        }
    }
    
    public DeepDrawerExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEEP_DRAWER_EXTRACTOR.get(), pos, state);
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
            case PULSE -> {
                // Mode 3: Only on redstone pulse (low to high transition)
                if (hasRedstoneSignal && !blockEntity.previousRedstoneState) {
                    // Detected rising edge (pulse)
                    shouldExtract = true;
                }
                blockEntity.previousRedstoneState = hasRedstoneSignal;
            }
        }
        
        blockEntity.extractionTimer++;
        
        // Check if we should skip extraction (whitelist mode with no valid filters = no extraction)
        boolean hasValidFilters = blockEntity.hasValidFilters();
        if (blockEntity.isWhitelistMode && !hasValidFilters) {
            // Whitelist mode with empty filters = don't extract anything, skip tick
            return;
        }
        
        // Extract every N ticks (from config), but only if redstone conditions are met
        int extractionInterval = net.unfamily.iskautils.Config.deepDrawerExtractorInterval;
        if (blockEntity.extractionTimer >= extractionInterval && shouldExtract) {
            blockEntity.extractionTimer = 0;
            blockEntity.tryExtractFromDrawer();
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
        for (String filter : filterFields) {
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
        for (String filter : filterFields) {
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
        
        // Save filter fields
        CompoundTag filterTag = new CompoundTag();
        for (int i = 0; i < FILTER_FIELD_COUNT; i++) {
            String value = filterFields[i];
            if (value != null && !value.isEmpty()) {
                filterTag.putString("field_" + i, value);
            }
        }
        filterTag.putBoolean("whitelist_mode", isWhitelistMode);
        tag.put("filter_config", filterTag);
        
        // Save redstone mode
        tag.putInt("redstoneMode", redstoneMode);
        tag.putBoolean("previousRedstoneState", previousRedstoneState);
    }
    
    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ContainerHelper.loadAllItems(tag, items, provider);
        
        // Load filter fields
        if (tag.contains("filter_config", CompoundTag.TAG_COMPOUND)) {
            CompoundTag filterTag = tag.getCompound("filter_config");
            for (int i = 0; i < FILTER_FIELD_COUNT; i++) {
                String key = "field_" + i;
                if (filterTag.contains(key, CompoundTag.TAG_STRING)) {
                    filterFields[i] = filterTag.getString(key);
                } else {
                    filterFields[i] = null;
                }
            }
            isWhitelistMode = filterTag.getBoolean("whitelist_mode");
        }
        
        // Load redstone mode
        if (tag.contains("redstoneMode")) {
            redstoneMode = tag.getInt("redstoneMode");
        }
        if (tag.contains("previousRedstoneState")) {
            previousRedstoneState = tag.getBoolean("previousRedstoneState");
        }
    }
    
    // ===== Filter Configuration Getters/Setters =====
    
    public String[] getFilterFields() {
        return filterFields.clone();
    }
    
    public void setFilterFields(String[] fields) {
        if (fields != null && fields.length >= FILTER_FIELD_COUNT) {
            // Process each field: trim spaces and remove single quotes (KubeJS friendly)
            for (int i = 0; i < FILTER_FIELD_COUNT; i++) {
                String field = fields[i];
                if (field != null) {
                    field = field.trim();
                    // Remove single quotes (') for KubeJS compatibility
                    field = field.replace("'", "");
                    // Set to null if empty after processing
                    filterFields[i] = field.isEmpty() ? null : field;
                } else {
                    filterFields[i] = null;
                }
            }
            setChanged();
            // Force sync to client (like SmartTimerBlockEntity)
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        } else if (fields != null && fields.length < FILTER_FIELD_COUNT) {
            // Process available fields: trim spaces and remove single quotes (KubeJS friendly)
            for (int i = 0; i < fields.length; i++) {
                String field = fields[i];
                if (field != null) {
                    field = field.trim();
                    // Remove single quotes (') for KubeJS compatibility
                    field = field.replace("'", "");
                    // Set to null if empty after processing
                    filterFields[i] = field.isEmpty() ? null : field;
                } else {
                    filterFields[i] = null;
                }
            }
            // Set rest to null
            for (int i = fields.length; i < FILTER_FIELD_COUNT; i++) {
                filterFields[i] = null;
            }
            setChanged();
            // Force sync to client (like SmartTimerBlockEntity)
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
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
        this.redstoneMode = redstoneMode % 4; // Ensure mode is always 0-3
        setChanged();
        // Force sync to client
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            // Log for debugging
            if (oldMode != this.redstoneMode) {
                LOGGER.debug("DeepDrawerExtractor: Redstone mode changed from {} to {}", oldMode, this.redstoneMode);
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
