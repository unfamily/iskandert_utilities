package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.client.gui.DeepDrawersMenu;
import net.unfamily.iskautils.util.DeepDrawerStackSizeContext;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * BlockEntity for Deep Drawers
 * Manages massive storage for non-stackable items
 * Uses a HashMap for efficient sparse storage (only stores non-empty slots)
 * Uses custom IItemHandler for hopper interaction with item validation
 */
public class DeepDrawersBlockEntity extends BlockEntity {
    
    // Sparse storage: only stores slots that contain items
    // Key = physical slot index, Value = ItemStack
    private final Map<Integer, ItemStack> storage = new HashMap<>();
    
    // Performance optimization: Index of items by type for O(1) extraction lookup
    // Key = Item, Value = Set of physical slot indices containing this item
    private final Map<Item, Set<Integer>> itemIndex = new HashMap<>();
    
    // Performance optimization: Queue of empty slots for O(1) insertion
    private final Queue<Integer> emptySlotsQueue = new ArrayDeque<>();
    
    // Performance optimization: Cursor for circular search when queue is empty
    private int insertionCursor = 0;
    
    // Performance optimization: Logical slot mapping
    // Maps logical slot index (exposed to pipes) to physical slot index (internal storage)
    private final List<Integer> occupiedSlots = new ArrayList<>(); // Ordered list of physical slots with items
    private final Map<Integer, Integer> logicalToPhysical = new HashMap<>(); // logicalSlot -> physicalSlot
    private final Map<Integer, Integer> physicalToLogical = new HashMap<>(); // physicalSlot -> logicalSlot
    
    // Cached slot count from config
    private int maxSlots;
    
    // Scroll offset for GUI (persisted in NBT)
    private int scrollOffset = 0;
    
    // Track if GUI is open to prevent input/output conflicts
    private int guiOpenCount = 0;
    
    // Track if compacting is in progress (blocks input/output)
    private boolean isCompacting = false;
    
    // Timer for periodic compaction (every 10 ticks)
    private int compactTimer = 0;
    private static final int COMPACT_INTERVAL = 10; // 10 ticks
    
    // Custom item handler for hopper/pipe interaction
    private final IItemHandler itemHandler = new DeepDrawersItemHandler();
    
    public DeepDrawersBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEEP_DRAWERS_BE.get(), pos, state);
        this.maxSlots = Config.deepDrawersSlotCount;
    }
    
    /**
     * Gets the item handler for capability registration
     */
    public IItemHandler getItemHandler() {
        return itemHandler;
    }
    
    /**
     * Converts a physical slot to logical slot
     * Returns -1 if physical slot is not occupied
     */
    public int physicalToLogicalSlot(int physicalSlot) {
        Integer logicalSlot = physicalToLogical.get(physicalSlot);
        return logicalSlot != null ? logicalSlot : -1;
    }
    
    /**
     * Gets item stack directly from physical slot (for GUI use)
     * Returns empty stack if slot is empty
     */
    public ItemStack getStackInPhysicalSlot(int physicalSlot) {
        if (physicalSlot < 0 || physicalSlot >= maxSlots) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = storage.get(physicalSlot);
        return stack != null ? stack.copy() : ItemStack.EMPTY;
    }
    
    /**
     * Gets the list of occupied slots in order (for GUI display)
     * @return ordered list of physical slot indices that contain items
     */
    public List<Integer> getOccupiedSlots() {
        return new ArrayList<>(occupiedSlots);
    }
    
    /**
     * Gets the physical slot at the given occupied slot index
     * @param occupiedIndex the index in the occupied slots list (0-based)
     * @return physical slot index, or -1 if index is out of bounds
     */
    public int getPhysicalSlotAtOccupiedIndex(int occupiedIndex) {
        if (occupiedIndex < 0 || occupiedIndex >= occupiedSlots.size()) {
            return -1;
        }
        return occupiedSlots.get(occupiedIndex);
    }
    
    /**
     * Inserts item directly into a physical slot (for GUI use)
     * This bypasses the logical slot system to ensure items go to the correct physical slot
     */
    public ItemStack insertItemIntoPhysicalSlotDirect(int physicalSlot, ItemStack stack, boolean simulate) {
        if (itemHandler instanceof DeepDrawersItemHandler handler) {
            return handler.insertItemIntoPhysicalSlot(physicalSlot, stack, simulate);
        }
        return stack;
    }
    
    /**
     * Called when GUI is opened
     */
    public void onGuiOpened() {
        guiOpenCount++;
    }
    
    /**
     * Called when GUI is closed
     */
    public void onGuiClosed() {
        if (guiOpenCount > 0) {
            guiOpenCount--;
        }
    }
    
    /**
     * Checks if GUI is currently open
     */
    public boolean isGuiOpen() {
        return guiOpenCount > 0;
    }
    
    /**
     * Checks if compacting is in progress (blocks input/output)
     */
    public boolean isCompacting() {
        return isCompacting;
    }
    
    /**
     * Static tick method called by the block
     */
    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, DeepDrawersBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return; // Server-side only
        }
        
        // Increment compact timer
        blockEntity.compactTimer++;
        
        // Every 3 seconds, perform compaction
        if (blockEntity.compactTimer >= COMPACT_INTERVAL) {
            blockEntity.compactTimer = 0;
            blockEntity.performCompaction();
        }
    }
    
    /**
     * Performs compaction: merges stackable items and removes gaps
     */
    private void performCompaction() {
        if (isCompacting || storage.isEmpty()) {
            return; // Already compacting or nothing to compact
        }
        
        isCompacting = true;
        try {
            // Step 1: Merge stackable items of the same type
            Map<Item, List<Integer>> itemsByType = new HashMap<>();
            for (Map.Entry<Integer, ItemStack> entry : storage.entrySet()) {
                Item item = entry.getValue().getItem();
                itemsByType.computeIfAbsent(item, k -> new ArrayList<>()).add(entry.getKey());
            }
            
            // Merge items of the same type that can stack
            for (Map.Entry<Item, List<Integer>> typeEntry : itemsByType.entrySet()) {
                List<Integer> slots = typeEntry.getValue();
                if (slots.size() <= 1) {
                    continue; // Only one slot, nothing to merge
                }
                
                // Get max stack size for this item type (use Deep Drawer limit)
                int maxStackSize = 4096; // Deep Drawer limit
                
                // Try to merge stacks
                for (int i = 0; i < slots.size(); i++) {
                    int sourceSlot = slots.get(i);
                    ItemStack sourceStack = storage.get(sourceSlot);
                    if (sourceStack == null || sourceStack.isEmpty()) {
                        continue;
                    }
                    
                    int sourceCount = sourceStack.getCount();
                    if (sourceCount >= maxStackSize) {
                        continue; // Already full
                    }
                    
                    // Try to merge with other slots of the same type
                    for (int j = i + 1; j < slots.size(); j++) {
                        int targetSlot = slots.get(j);
                        ItemStack targetStack = storage.get(targetSlot);
                        if (targetStack == null || targetStack.isEmpty()) {
                            continue;
                        }
                        
                        // Check if stacks can merge (same item, same NBT)
                        if (ItemStack.isSameItemSameComponents(sourceStack, targetStack)) {
                            int targetCount = targetStack.getCount();
                            int spaceLeft = maxStackSize - sourceCount;
                            
                            if (spaceLeft > 0 && targetCount > 0) {
                                int toTransfer = Math.min(spaceLeft, targetCount);
                                sourceStack.grow(toTransfer);
                                storage.put(sourceSlot, sourceStack);
                                
                                if (toTransfer >= targetCount) {
                                    // Target slot is now empty, remove it
                                    removeItemFromStorage(targetSlot);
                                    slots.remove(j);
                                    j--; // Adjust index after removal
                                } else {
                                    targetStack.shrink(toTransfer);
                                    storage.put(targetSlot, targetStack);
                                }
                                
                                sourceCount = sourceStack.getCount();
                                if (sourceCount >= maxStackSize) {
                                    break; // Source is full, move to next
                                }
                            }
                        }
                    }
                }
            }
            
            // Step 2: Compact slots (remove gaps by moving items to lower slots)
            // Sort occupied slots
            Collections.sort(occupiedSlots);
            
            // Find first empty slot
            int nextEmptySlot = 0;
            for (int i = 0; i < occupiedSlots.size(); i++) {
                int currentSlot = occupiedSlots.get(i);
                
                // If there's a gap, move item to fill it
                if (currentSlot > nextEmptySlot) {
                    ItemStack stack = storage.remove(currentSlot);
                    if (stack != null) {
                        // Move to the empty slot
                        storage.put(nextEmptySlot, stack);
                        
                        // Update item index
                        Item item = stack.getItem();
                        Set<Integer> itemSlots = itemIndex.get(item);
                        if (itemSlots != null) {
                            itemSlots.remove(currentSlot);
                            itemSlots.add(nextEmptySlot);
                        }
                        
                        // Update occupied slots list
                        occupiedSlots.set(i, nextEmptySlot);
                        
                        // Update empty slots queue
                        emptySlotsQueue.offer(currentSlot);
                    }
                }
                
                nextEmptySlot = Math.max(nextEmptySlot, currentSlot) + 1;
            }
            
            // Rebuild logical mapping after compaction
            rebuildLogicalMapping();
            
            setChanged();
        } finally {
            isCompacting = false;
        }
    }
    
    /**
     * Checks if the storage contains any items
     * @return true if at least one item is stored
     */
    public boolean hasItems() {
        return !storage.isEmpty();
    }
    
    /**
     * Gets the number of items currently stored (not the number of slots used)
     * @return total count of all items
     */
    public int getTotalItemCount() {
        return storage.values().stream()
                .mapToInt(ItemStack::getCount)
                .sum();
    }
    
    /**
     * Gets the number of used slots
     * @return number of slots containing items
     */
    public int getUsedSlotCount() {
        return storage.size();
    }
    
    /**
     * Gets the maximum number of slots
     * @return max slots from config
     */
    public int getMaxSlots() {
        return maxSlots;
    }
    
    /**
     * Clears all items from storage
     * WARNING: This does NOT drop items - use only when items should be destroyed
     */
    public void clearStorage() {
        storage.clear();
        itemIndex.clear();
        emptySlotsQueue.clear();
        occupiedSlots.clear();
        logicalToPhysical.clear();
        physicalToLogical.clear();
        insertionCursor = 0;
        setChanged();
    }
    
    /**
     * Gets all stored items (for iteration)
     * @return map of slot index to ItemStack
     */
    public Map<Integer, ItemStack> getAllItems() {
        return new HashMap<>(storage);
    }
    
    /**
     * Gets the current scroll offset for the GUI
     * @return the scroll offset
     */
    public int getScrollOffset() {
        return scrollOffset;
    }
    
    /**
     * Sets the scroll offset for the GUI and marks the block entity as changed
     * @param offset the new scroll offset
     */
    public void setScrollOffset(int offset) {
        if (offset < 0) {
            offset = 0;
        }
        int maxOffset = Math.max(0, maxSlots - DeepDrawersMenu.VISIBLE_SLOTS);
        if (offset > maxOffset) {
            offset = maxOffset;
        }
        
        if (this.scrollOffset != offset) {
            this.scrollOffset = offset;
            setChanged();
        }
    }
    
    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        
        // Save max slots (in case config changes)
        tag.putInt("MaxSlots", this.maxSlots);
        
        // Save scroll offset
        tag.putInt("ScrollOffset", this.scrollOffset);
        
        // Save compact timer
        tag.putInt("CompactTimer", this.compactTimer);
        
        // Save storage using ListTag format (like ItemStackHandler does internally)
        ListTag itemsList = new ListTag();
        for (Map.Entry<Integer, ItemStack> entry : storage.entrySet()) {
            ItemStack stack = entry.getValue();
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", entry.getKey());
                itemTag.put("Item", stack.saveOptional(provider));
                itemsList.add(itemTag);
            }
        }
        
        tag.put("Items", itemsList);
    }
    
    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        
        // Load max slots
        this.maxSlots = tag.getInt("MaxSlots");
        if (this.maxSlots <= 0) {
            this.maxSlots = Config.deepDrawersSlotCount;
        }
        
        // Load scroll offset
        this.scrollOffset = tag.getInt("ScrollOffset");
        if (this.scrollOffset < 0) {
            this.scrollOffset = 0;
        }
        
        // Load compact timer
        this.compactTimer = tag.getInt("CompactTimer");
        
        // Clear and load storage
        storage.clear();
        itemIndex.clear();
        emptySlotsQueue.clear();
        occupiedSlots.clear();
        logicalToPhysical.clear();
        physicalToLogical.clear();
        insertionCursor = 0;
        
        // Load storage using ListTag format (like ItemStackHandler does internally)
        if (tag.contains("Items", Tag.TAG_LIST)) {
            ListTag itemsList = tag.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < itemsList.size(); i++) {
                CompoundTag itemTag = itemsList.getCompound(i);
                int physicalSlot = itemTag.getInt("Slot");
                
                if (physicalSlot >= 0 && physicalSlot < maxSlots) {
                    ItemStack stack = ItemStack.parseOptional(provider, itemTag.getCompound("Item"));
                    if (!stack.isEmpty()) {
                        // Rebuild all indices
                        addItemToStorage(physicalSlot, stack);
                    }
                }
            }
        }
    }
    
    @Override
    public CompoundTag getUpdateTag(@NotNull HolderLookup.Provider provider) {
        // Only send minimal metadata, NOT all items
        // Sending all items would exceed the 2MB NBT limit for network packets
        // Items are synced separately when the GUI is opened
        CompoundTag tag = new CompoundTag();
        tag.putInt("MaxSlots", this.maxSlots);
        tag.putInt("ScrollOffset", this.scrollOffset);
        // Note: We intentionally do NOT include the Items list here
        // to avoid exceeding the 2MB NBT packet limit
        return tag;
    }
    
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        // Create update packet for network synchronization
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    
    @Override
    public void onDataPacket(@NotNull net.minecraft.network.Connection net, 
                            @NotNull net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt, 
                            @NotNull HolderLookup.Provider lookupProvider) {
        super.onDataPacket(net, pkt, lookupProvider);
        // Handle data packet from server on client
        // Only load metadata, not items (items are synced via GUI packets)
        if (pkt.getTag() != null) {
            CompoundTag tag = pkt.getTag();
            // Only update metadata, not storage
            if (tag.contains("MaxSlots", Tag.TAG_INT)) {
                this.maxSlots = tag.getInt("MaxSlots");
                if (this.maxSlots <= 0) {
                    this.maxSlots = Config.deepDrawersSlotCount;
                }
            }
            if (tag.contains("ScrollOffset", Tag.TAG_INT)) {
                this.scrollOffset = tag.getInt("ScrollOffset");
                if (this.scrollOffset < 0) {
                    this.scrollOffset = 0;
                }
            }
        }
    }
    
    @Override
    public void onLoad() {
        super.onLoad();
        // Sync data to client when chunk is loaded (server-side only)
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    @Override
    public void setChanged() {
        super.setChanged();
        // Sync changes to client immediately, but only metadata (not items)
        // Items are synced separately when GUI is opened to avoid exceeding 2MB NBT limit
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
    
    /**
     * Gets a display name for the block entity
     * @return the translated display name
     */
    public Component getDisplayName() {
        return Component.translatable("block.iska_utils.deep_drawers");
    }
    
    // ===== Item Validation =====
    
    /**
     * Validates if an item can be stored in the Deep Drawers
     * Checks against allowed tags/IDs and blacklist
     */
    private boolean isItemValid(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        
        // Check blacklist first (has priority)
        for (String blacklisted : Config.deepDrawersBlacklist) {
            if (matchesTagOrId(stack, blacklisted)) {
                return false; // Item is blacklisted
            }
        }
        
        // If no allowed tags configured, accept all (except blacklisted)
        if (Config.deepDrawersAllowedTags.isEmpty()) {
            return true;
        }
        
        // Check if item matches any allowed tag/ID
        for (String allowed : Config.deepDrawersAllowedTags) {
            if (matchesTagOrId(stack, allowed)) {
                return true;
            }
        }
        
        return false; // Item doesn't match any allowed tag/ID
    }
    
    /**
     * Checks if an ItemStack matches a tag or item ID
     * @param stack the ItemStack to check
     * @param tagOrId the tag (starting with #) or item ID
     * @return true if it matches
     */
    private boolean matchesTagOrId(ItemStack stack, String tagOrId) {
        if (tagOrId.startsWith("#")) {
            // It's a tag
            String tagName = tagOrId.substring(1); // Remove #
            try {
                ResourceLocation tagLocation = ResourceLocation.parse(tagName);
                TagKey<Item> itemTag = TagKey.create(BuiltInRegistries.ITEM.key(), tagLocation);
                return stack.is(itemTag);
            } catch (Exception e) {
                // Invalid tag format
                return false;
            }
        } else {
            // It's an item ID
            try {
                ResourceLocation itemId = ResourceLocation.parse(tagOrId);
                ResourceLocation stackId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                return itemId.equals(stackId);
            } catch (Exception e) {
                // Invalid item ID format
                return false;
            }
        }
    }
    
    // ===== Index Management =====
    
    /**
     * Adds an item to storage and updates all indices
     */
    private void addItemToStorage(int physicalSlot, ItemStack stack) {
        storage.put(physicalSlot, stack);
        
        // Update item index
        Item item = stack.getItem();
        itemIndex.computeIfAbsent(item, k -> new HashSet<>()).add(physicalSlot);
        
        // Update logical slot mapping
        int logicalSlot = occupiedSlots.size();
        occupiedSlots.add(physicalSlot);
        logicalToPhysical.put(logicalSlot, physicalSlot);
        physicalToLogical.put(physicalSlot, logicalSlot);
        
        // Update insertion cursor
        if (physicalSlot >= insertionCursor) {
            insertionCursor = physicalSlot + 1;
        }
    }
    
    /**
     * Removes an item from storage and updates all indices
     */
    private void removeItemFromStorage(int physicalSlot) {
        ItemStack removed = storage.remove(physicalSlot);
        if (removed != null) {
            // Remove from item index
            Item item = removed.getItem();
            Set<Integer> slots = itemIndex.get(item);
            if (slots != null) {
                slots.remove(physicalSlot);
                if (slots.isEmpty()) {
                    itemIndex.remove(item);
                }
            }
            
            // Remove from logical slot mapping
            Integer logicalSlot = physicalToLogical.remove(physicalSlot);
            if (logicalSlot != null) {
                logicalToPhysical.remove(logicalSlot);
                // Remove by value, not by index
                occupiedSlots.remove(Integer.valueOf(physicalSlot));
                
                // Rebuild logical mapping to keep it contiguous
                rebuildLogicalMapping();
            }
            
            // Add to empty slots queue
            emptySlotsQueue.offer(physicalSlot);
        }
    }
    
    /**
     * Rebuilds the logical to physical slot mapping after removal
     * Ensures logical slots remain contiguous (0, 1, 2, ...)
     */
    private void rebuildLogicalMapping() {
        logicalToPhysical.clear();
        physicalToLogical.clear();
        
        // Sort occupied slots for consistent ordering
        Collections.sort(occupiedSlots);
        
        // Rebuild mappings
        for (int i = 0; i < occupiedSlots.size(); i++) {
            int physicalSlot = occupiedSlots.get(i);
            logicalToPhysical.put(i, physicalSlot);
            physicalToLogical.put(physicalSlot, i);
        }
    }
    
    /**
     * Finds the next empty slot starting from cursor (circular search)
     */
    private int findNextEmptySlot(int startFrom) {
        // Search forward from cursor
        for (int i = 0; i < maxSlots; i++) {
            int slot = (startFrom + i) % maxSlots;
            if (!storage.containsKey(slot)) {
                return slot;
            }
        }
        return -1; // Storage is full
    }
    
    // ===== Custom IItemHandler Implementation =====
    
    /**
     * Optimized IItemHandler using logical slots
     * Exposes only occupied slots + 1 virtual slot for insertion
     * This dramatically reduces iteration overhead for pipes/hoppers
     */
    private class DeepDrawersItemHandler implements IItemHandlerModifiable {
        
        @Override
        public int getSlots() {
            // Return only occupied slots + 1 virtual slot for automatic insertion
            // This prevents pipes from iterating through 49,995 empty slots
            return occupiedSlots.size() + 1;
        }
        
        @Override
        public @NotNull ItemStack getStackInSlot(int logicalSlot) {
            if (logicalSlot < 0) {
                return ItemStack.EMPTY;
            }
            
            // Last slot is the virtual insertion slot (always empty)
            if (logicalSlot == occupiedSlots.size()) {
                return ItemStack.EMPTY;
            }
            
            // Map logical slot to physical slot
            if (logicalSlot >= occupiedSlots.size()) {
                return ItemStack.EMPTY;
            }
            
            Integer physicalSlot = logicalToPhysical.get(logicalSlot);
            if (physicalSlot == null) {
                return ItemStack.EMPTY;
            }
            
            ItemStack stack = storage.get(physicalSlot);
            return stack == null ? ItemStack.EMPTY : stack.copy();
        }
        
        @Override
        public @NotNull ItemStack insertItem(int logicalSlot, @NotNull ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || logicalSlot < 0) {
                return stack;
            }
            
            // Block input/output when compacting
            if (DeepDrawersBlockEntity.this.isCompacting) {
                return stack; // Reject when compacting
            }
            
            // Validate item
            if (!DeepDrawersBlockEntity.this.isItemValid(stack)) {
                return stack; // Reject invalid items
            }
            
            // Use Deep Drawer context to enable stack size modification via mixin
            // This allows non-stackable items to become stackable inside the Deep Drawer
            return DeepDrawerStackSizeContext.withDeepDrawerContext(() -> {
                // Virtual slot (last slot) = automatic insertion
                if (logicalSlot == occupiedSlots.size()) {
                    return insertItemAuto(stack, simulate);
                }
                
                // Regular logical slot - map to physical slot
                if (logicalSlot >= occupiedSlots.size()) {
                    return stack;
                }
                
                Integer physicalSlot = logicalToPhysical.get(logicalSlot);
                if (physicalSlot == null) {
                    return stack;
                }
                
                ItemStack existing = storage.get(physicalSlot);
                
                // If slot is empty, we can insert
                if (existing == null || existing.isEmpty()) {
                    if (!simulate) {
                        addItemToStorage(physicalSlot, stack.copy());
                        setChanged();
                    }
                    return ItemStack.EMPTY;
                }
                
                // If slot has different item, can't insert
                if (!ItemStack.isSameItemSameComponents(existing, stack)) {
                    return stack;
                }
                
                // Try to merge stacks
                // Use Deep Drawer slot limit instead of item's maxStackSize to allow stacking non-stackable items
                int maxStackSize = getSlotLimit(logicalSlot);
                int spaceLeft = maxStackSize - existing.getCount();
                
                if (spaceLeft <= 0) {
                    return stack; // Slot is full
                }
                
                int toInsert = Math.min(spaceLeft, stack.getCount());
                
                if (!simulate) {
                    ItemStack newStack = existing.copy();
                    newStack.grow(toInsert);
                    storage.put(physicalSlot, newStack);
                    setChanged();
                }
                
                if (toInsert >= stack.getCount()) {
                    return ItemStack.EMPTY; // All inserted
                }
                
                ItemStack remainder = stack.copy();
                remainder.shrink(toInsert);
                return remainder;
            });
        }
        
        /**
         * Inserts item into a specific physical slot (for GUI use)
         * Package-private for access from menu
         */
        @NotNull ItemStack insertItemIntoPhysicalSlot(int physicalSlot, @NotNull ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || physicalSlot < 0 || physicalSlot >= maxSlots) {
                return stack;
            }
            
            // Use Deep Drawer context to enable stack size modification via mixin
            return DeepDrawerStackSizeContext.withDeepDrawerContext(() -> {
                ItemStack existing = storage.get(physicalSlot);
                
                // If slot is empty, we can insert
                if (existing == null || existing.isEmpty()) {
                    if (!simulate) {
                        addItemToStorage(physicalSlot, stack.copy());
                        setChanged();
                    }
                    return ItemStack.EMPTY;
                }
                
                // If slot has different item, can't insert
                if (!ItemStack.isSameItemSameComponents(existing, stack)) {
                    return stack;
                }
                
                // Try to merge stacks (use Deep Drawer limit, not item's maxStackSize)
                int maxStackSize = getSlotLimit(0); // All slots have same limit
                int spaceLeft = maxStackSize - existing.getCount();
                
                if (spaceLeft <= 0) {
                    return stack; // Slot is full
                }
                
                int toInsert = Math.min(spaceLeft, stack.getCount());
                
                if (!simulate) {
                    ItemStack newStack = existing.copy();
                    newStack.grow(toInsert);
                    storage.put(physicalSlot, newStack);
                    setChanged();
                }
                
                if (toInsert >= stack.getCount()) {
                    return ItemStack.EMPTY; // All inserted
                }
                
                ItemStack remainder = stack.copy();
                remainder.shrink(toInsert);
                return remainder;
            });
        }
        
        /**
         * Automatic insertion that finds the best slot without iteration
         * Uses item index and empty slots queue for O(1) performance
         */
        private @NotNull ItemStack insertItemAuto(@NotNull ItemStack stack, boolean simulate) {
            Item item = stack.getItem();
            
            // First, try to merge into existing slot with same item
            Set<Integer> slotsWithItem = itemIndex.get(item);
            if (slotsWithItem != null) {
                for (int physicalSlot : slotsWithItem) {
                    ItemStack existing = storage.get(physicalSlot);
                    if (existing != null && ItemStack.isSameItemSameComponents(existing, stack)) {
                        // Use Deep Drawer slot limit instead of item's maxStackSize
                        int maxStackSize = getSlotLimit(0); // Use slot 0 limit (all slots have same limit)
                        int spaceLeft = maxStackSize - existing.getCount();
                        
                        if (spaceLeft > 0) {
                            // Can merge into this slot
                            int toInsert = Math.min(spaceLeft, stack.getCount());
                            
                            if (!simulate) {
                                ItemStack newStack = existing.copy();
                                newStack.grow(toInsert);
                                storage.put(physicalSlot, newStack);
                                setChanged();
                            }
                            
                            if (toInsert >= stack.getCount()) {
                                return ItemStack.EMPTY; // All inserted
                            }
                            
                            // Recursively insert remainder
                            ItemStack remainder = stack.copy();
                            remainder.shrink(toInsert);
                            return insertItemAuto(remainder, simulate);
                        }
                    }
                }
            }
            
            // No existing slot with space, find empty slot
            Integer emptySlot = emptySlotsQueue.poll();
            if (emptySlot == null) {
                // Queue empty, use circular search
                emptySlot = findNextEmptySlot(insertionCursor);
                if (emptySlot >= 0) {
                    insertionCursor = emptySlot + 1;
                }
            }
            
            if (emptySlot != null && emptySlot >= 0 && emptySlot < maxSlots) {
                if (!simulate) {
                    addItemToStorage(emptySlot, stack.copy());
                    setChanged();
                }
                return ItemStack.EMPTY;
            }
            
            // No space available
            return stack;
        }
        
        @Override
        public @NotNull ItemStack extractItem(int logicalSlot, int amount, boolean simulate) {
            if (amount <= 0 || logicalSlot < 0) {
                return ItemStack.EMPTY;
            }
            
            // Block input/output when compacting
            if (DeepDrawersBlockEntity.this.isCompacting) {
                return ItemStack.EMPTY; // Reject when compacting
            }
            
            // Virtual slot cannot be extracted from
            if (logicalSlot >= occupiedSlots.size()) {
                return ItemStack.EMPTY;
            }
            
            // Map logical slot to physical slot
            Integer physicalSlot = logicalToPhysical.get(logicalSlot);
            if (physicalSlot == null) {
                return ItemStack.EMPTY;
            }
            
            ItemStack existing = storage.get(physicalSlot);
            if (existing == null || existing.isEmpty()) {
                return ItemStack.EMPTY;
            }
            
            // Always extract only 1 item at a time to prevent duplications
            int toExtract = Math.min(1, existing.getCount());
            
            // Create the extracted stack BEFORE modifying
            ItemStack extracted = existing.copy();
            extracted.setCount(toExtract);
            
            if (!simulate) {
                if (toExtract >= existing.getCount()) {
                    // Slot is now empty
                    removeItemFromStorage(physicalSlot);
                } else {
                    // Partial extraction - update stack but keep in storage
                    ItemStack newStack = existing.copy();
                    newStack.shrink(toExtract);
                    storage.put(physicalSlot, newStack);
                }
                setChanged();
            }
            
            return extracted;
        }
        
        @Override
        public int getSlotLimit(int logicalSlot) {
            // Allow stacking in Deep Drawer to make non-stackable items stackable
            // Similar to Stackcraft behavior, but only for Deep Drawer
            return 4096; // Stack limit for Deep Drawer
        }
        
        @Override
        public boolean isItemValid(int logicalSlot, @NotNull ItemStack stack) {
            // Validation doesn't depend on slot, only on item
            return DeepDrawersBlockEntity.this.isItemValid(stack);
        }
        
        @Override
        public void setStackInSlot(int logicalSlot, @NotNull ItemStack stack) {
            if (logicalSlot < 0) {
                return;
            }
            
            // Virtual slot cannot be set
            if (logicalSlot >= occupiedSlots.size()) {
                return;
            }
            
            // Always limit to 1 item to prevent issues with aggregated storage
            ItemStack stackToSet = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
            if (!stackToSet.isEmpty() && stackToSet.getCount() > 1) {
                stackToSet.setCount(1);
            }
            
            // Map logical slot to physical slot
            Integer physicalSlot = logicalToPhysical.get(logicalSlot);
            if (physicalSlot == null) {
                return;
            }
            
            // Extract old item first (always only 1 item)
            ItemStack existing = storage.get(physicalSlot);
            if (existing != null && !existing.isEmpty()) {
                // Extract only 1 item
                if (existing.getCount() > 1) {
                    existing.shrink(1);
                    storage.put(physicalSlot, existing);
                } else {
                    removeItemFromStorage(physicalSlot);
                }
                DeepDrawersBlockEntity.this.setChanged();
            }
            
            // Insert new stack (always 1 item)
            if (!stackToSet.isEmpty()) {
                // If slot was removed, add it back; otherwise just update storage
                if (!storage.containsKey(physicalSlot)) {
                    addItemToStorage(physicalSlot, stackToSet);
                } else {
                    // Slot already exists, just update storage and index
                    storage.put(physicalSlot, stackToSet);
                    Item item = stackToSet.getItem();
                    itemIndex.computeIfAbsent(item, k -> new HashSet<>()).add(physicalSlot);
                }
                DeepDrawersBlockEntity.this.setChanged();
            }
        }
    }
}

