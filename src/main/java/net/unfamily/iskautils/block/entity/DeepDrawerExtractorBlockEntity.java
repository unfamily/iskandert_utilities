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
    
    // Timer for extraction (every 1 tick = 0.05 seconds, maximum speed)
    private int extractionTimer = 0;
    private static final int EXTRACTION_INTERVAL = 1;
    
    // Cache of found Deep Drawer (for performance)
    private BlockPos cachedDrawerPos = null;
    private int cacheValidTicks = 0;
    private static final int CACHE_VALIDITY_TICKS = 100; // Cache valid for 5 seconds
    
    // Filter configuration
    private static final int FILTER_FIELD_COUNT = 11;
    private final String[] filterFields = new String[FILTER_FIELD_COUNT];
    private boolean isWhitelistMode = false; // false = blacklist, true = whitelist
    
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
        
        // Extract every EXTRACTION_INTERVAL ticks, but only if redstone conditions are met
        if (blockEntity.extractionTimer >= EXTRACTION_INTERVAL && shouldExtract) {
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
        // Iterate over items to find one we can insert
        // BLACKLIST FILTER: skip items with NBT apotheosis:rarity = apotheosis:mythic
        // NOTE: epic items are extracted normally, only mythic items are excluded
        
        for (Map.Entry<Integer, ItemStack> entry : allItems.entrySet()) {
            ItemStack drawerStack = entry.getValue();
            if (drawerStack == null || drawerStack.isEmpty()) {
                continue;
            }
            
            Item item = drawerStack.getItem();
            
            // BLACKLIST: Check if item has NBT apotheosis:rarity = apotheosis:mythic
            // Use save() to get the complete CompoundTag and search the string directly
            // NOTE: epic items are extracted, only mythic items are blacklisted
            boolean isMythic = false;
            try {
                if (level != null) {
                    var tag = drawerStack.save(level.registryAccess());
                    if (tag instanceof CompoundTag compoundTag) {
                        // Serialize CompoundTag to string and search for exact string
                        String nbtString = compoundTag.toString();
                        
                        // Search for "apotheosis:rarity":"apotheosis:mythic" in serialized string
                        // Format in CompoundTag is: "apotheosis:rarity":"apotheosis:mythic"
                        if (nbtString.contains("\"apotheosis:rarity\":\"apotheosis:mythic\"")) {
                            isMythic = true;
                        }
                    }
                }
            } catch (Exception e) {
                // Error reading NBT, consider not present
                // Item doesn't have required NBT or there's an error reading it
            }
            
            // BLACKLIST: exclude mythic items
            if (isMythic) {
                continue; // Skip blacklisted item (has apotheosis:rarity = apotheosis:mythic)
            }
            
            // Item is not blacklisted, extract it
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
        
        // Search in all 6 directions
        for (Direction direction : Direction.values()) {
            BlockPos checkPos = worldPosition.relative(direction);
            BlockEntity be = level.getBlockEntity(checkPos);
            
            if (be instanceof DeepDrawersBlockEntity drawer) {
                // Cache the found position
                cachedDrawerPos = checkPos;
                cacheValidTicks = 0;
                return drawer;
            }
        }
        
        return null;
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
            System.arraycopy(fields, 0, filterFields, 0, FILTER_FIELD_COUNT);
            setChanged();
            // Force sync to client (like SmartTimerBlockEntity)
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        } else if (fields != null && fields.length < FILTER_FIELD_COUNT) {
            // Copy available fields and set rest to null
            System.arraycopy(fields, 0, filterFields, 0, fields.length);
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
            setChanged();
            // Force sync to client (like SmartTimerBlockEntity)
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
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
