package net.unfamily.iskautils.client.gui;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.DeepDrawersBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Menu for the Deep Drawers GUI
 * 
 * Features:
 * - 54 visible slots (9 columns x 6 rows)
 * - Scrollable to access all configured slots (default: 49995)
 * - Player inventory (27 + 9 hotbar slots)
 * - Shift-click support for quick item transfer
 */
public class DeepDrawersMenu extends AbstractContainerMenu {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DeepDrawersMenu.class);
    
    private final DeepDrawersBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final BlockPos blockPos;
    private final IItemHandler itemHandler;
    private final IItemHandler viewHandler; // Handler for the visible 54 slots (updated when scroll changes)
    private final OffsetItemHandler offsetItemHandler; // Wrapper that adds scrollOffset automatically
    
    // GUI Layout constants
    public static final int VISIBLE_ROWS = 6;  // 6 rows of storage slots visible
    public static final int COLUMNS = 9;       // 9 columns (standard chest width)
    public static final int VISIBLE_SLOTS = VISIBLE_ROWS * COLUMNS; // 54 slots visible at once
    
    // Slot positions (from texture specification, adjusted)
    // Storage slots: 9 columns x 6 rows - texture moved 7px right, +1px down
    public static final int STORAGE_SLOTS_X = 15;  // Original 8 + 7px right (texture change)
    public static final int STORAGE_SLOTS_Y = 31;  // 30 + 1px down
    
    // Player inventory: 9 columns x 3 rows + hotbar - texture moved 7px right, +2px down
    public static final int PLAYER_INVENTORY_X = 15;    // Original 8 + 7px right (texture change)
    public static final int PLAYER_INVENTORY_Y = 153;   // 151 + 2px down
    
    // Scrolling state
    private int scrollOffset = 0;
    private final int totalSlots;
    
    // Container data for syncing scroll offset between server and client
    private final net.minecraft.world.inventory.ContainerData containerData;
    
    // Flag to prevent client from reading stale containerData after local update
    private int ignoreContainerDataTicks = 0;
    
    // Server-side constructor (called when opening GUI from block)
    public DeepDrawersMenu(int containerId, Inventory playerInventory, DeepDrawersBlockEntity blockEntity) {
        super(ModMenuTypes.DEEP_DRAWERS_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.itemHandler = blockEntity.getItemHandler();
        this.totalSlots = Config.deepDrawersSlotCount;
        
        // Create view handler for visible slots
        this.viewHandler = new net.neoforged.neoforge.items.ItemStackHandler(VISIBLE_SLOTS);
        updateViewHandler(); // Populate with initial items
        
        // Create offset wrapper for itemHandler
        this.offsetItemHandler = new OffsetItemHandler(this.itemHandler);
        
        // Create container data for syncing scroll offset
        this.containerData = new net.minecraft.world.inventory.SimpleContainerData(1);
        this.addDataSlots(this.containerData);
        
        // Add Deep Drawers visible slots (9 columns x 6 rows = 54 slots)
        addDeepDrawersSlots(playerInventory);
        
        // Add player inventory slots
        addPlayerInventorySlots(playerInventory);
    }
    
    // Client-side constructor (NeoForge factory pattern) - simple version
    public DeepDrawersMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.DEEP_DRAWERS_MENU.get(), containerId);
        // Client-side: we don't have direct access to BlockEntity
        this.blockEntity = null;
        this.blockPos = BlockPos.ZERO;
        this.levelAccess = ContainerLevelAccess.NULL;
        this.itemHandler = new net.neoforged.neoforge.items.ItemStackHandler(Config.deepDrawersSlotCount);
        this.totalSlots = Config.deepDrawersSlotCount;
        
        // View handler for visible slots (synced by Minecraft)
        this.viewHandler = new net.neoforged.neoforge.items.ItemStackHandler(VISIBLE_SLOTS);
        
        // Create offset wrapper for itemHandler
        this.offsetItemHandler = new OffsetItemHandler(this.itemHandler);
        
        // Create container data for syncing scroll offset
        this.containerData = new net.minecraft.world.inventory.SimpleContainerData(1);
        this.addDataSlots(this.containerData);
        
        // Add Deep Drawers visible slots (9 columns x 6 rows = 54 slots)
        addDeepDrawersSlots(playerInventory);
        
        // Add player inventory slots
        addPlayerInventorySlots(playerInventory);
    }
    
    /**
     * Adds the visible slots for the Deep Drawers storage
     * These slots read from offsetItemHandler which automatically adds scrollOffset
     */
    private void addDeepDrawersSlots(Inventory playerInventory) {
        // Use offsetItemHandler which automatically adds scrollOffset to all accesses
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int slotIndex = col + row * COLUMNS; // 0-53
                int xPos = STORAGE_SLOTS_X + col * 18;
                int yPos = STORAGE_SLOTS_Y + row * 18;
                // Create slot that reads from offsetItemHandler (automatically adds scrollOffset)
                this.addSlot(new SlotItemHandler(offsetItemHandler, slotIndex, xPos, yPos));
            }
        }
    }
    
    /**
     * Adds player inventory and hotbar slots
     */
    private void addPlayerInventorySlots(Inventory playerInventory) {
        // Player inventory (3 rows x 9 slots)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9 + 9; // +9 to skip hotbar
                int xPos = PLAYER_INVENTORY_X + col * 18;
                int yPos = PLAYER_INVENTORY_Y + row * 18;
                this.addSlot(new Slot(playerInventory, slotIndex, xPos, yPos));
            }
        }
        
        // Player hotbar (1 row x 9 slots)
        for (int col = 0; col < 9; col++) {
            int slotIndex = col;
            int xPos = PLAYER_INVENTORY_X + col * 18;
            int yPos = PLAYER_INVENTORY_Y + 3 * 18 + 4; // Below inventory with spacing
            this.addSlot(new Slot(playerInventory, slotIndex, xPos, yPos));
        }
    }
    
    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.DEEP_DRAWERS.get());
    }
    
    @Override
    public void slotsChanged(net.minecraft.world.Container container) {
        super.slotsChanged(container);
        // Notify the block entity that slots have changed
        if (this.blockEntity != null) {
            this.blockEntity.setChanged();
        }
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();
            
            // VISIBLE_SLOTS = 54 (Deep Drawers visible slots)
            // slots 54-89 = player inventory + hotbar (36 slots)
            
            if (index < VISIBLE_SLOTS) {
                // Moving from Deep Drawers to player inventory
                // The ScrollableSlotItemHandler handles the offset automatically
                if (!this.moveItemStackTo(slotStack, VISIBLE_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to Deep Drawers
                // Try to find an empty slot or stack with the same item
                if (!this.moveItemStackTo(slotStack, 0, VISIBLE_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            }
            
            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            
            if (slotStack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            
            slot.onTake(player, slotStack);
        }
        
        return itemstack;
    }
    
    /**
     * Updates the view handler with items from the current scroll position
     * This is called when the scroll offset changes to populate the visible slots
     * Server-side only: reads from actual itemHandler
     */
    private void updateViewHandler() {
        if (this.itemHandler != null && this.blockEntity != null && 
            !this.blockEntity.getLevel().isClientSide()) {
            LOGGER.debug("Updating viewHandler for scroll offset: {}", this.scrollOffset);
            // Copy items from actual storage to the view handler
            if (this.viewHandler instanceof net.neoforged.neoforge.items.ItemStackHandler stackHandler) {
                for (int i = 0; i < VISIBLE_SLOTS; i++) {
                    int actualIndex = this.scrollOffset + i;
                    ItemStack stack = this.itemHandler.getStackInSlot(actualIndex);
                    
                    // Set in view handler
                    stackHandler.setStackInSlot(i, stack.copy());
                    LOGGER.debug("ViewHandler slot {} = item from actual slot {}: {}", 
                                i, actualIndex, stack.isEmpty() ? "EcMPTY" : stack.getItem());
                    
                    // Force update the slot to trigger sync
                    if (i < this.slots.size()) {
                        Slot slot = this.slots.get(i);
                        if (slot != null) {
                            // Trigger slot change to force sync
                            slot.setChanged();
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Updates the item handler and view handler from server-sent data (client-side only)
     * Called when receiving DeepDrawersSyncSlotsS2CPacket
     */
    public void updateViewHandlerFromServer(int offset, java.util.List<ItemStack> visibleStacks) {
        LOGGER.debug("Client: Updating itemHandler and viewHandler from server. Offset: {}, Stacks: {}", offset, visibleStacks.size());
        this.scrollOffset = offset;
        
        // Update itemHandler lato client con i nuovi item (solo per gli slot visibili)
        if (this.itemHandler instanceof net.neoforged.neoforge.items.ItemStackHandler itemStackHandler) {
            for (int i = 0; i < VISIBLE_SLOTS && i < visibleStacks.size(); i++) {
                int actualIndex = offset + i;
                ItemStack stack = visibleStacks.get(i);
                ItemStack stackCopy = stack.copy();
                
                // Update itemHandler at the actual index
                itemStackHandler.setStackInSlot(actualIndex, stackCopy);
                LOGGER.debug("Client: ItemHandler slot {} = {}", actualIndex, stackCopy.isEmpty() ? "EMPTY" : stackCopy.getItem());
            }
            
            // Fill remaining visible slots with empty stacks if needed
            for (int i = visibleStacks.size(); i < VISIBLE_SLOTS; i++) {
                int actualIndex = offset + i;
                itemStackHandler.setStackInSlot(actualIndex, ItemStack.EMPTY);
            }
        }
        
        // Also update viewHandler for backward compatibility
        if (this.viewHandler instanceof net.neoforged.neoforge.items.ItemStackHandler stackHandler) {
            for (int i = 0; i < VISIBLE_SLOTS && i < visibleStacks.size(); i++) {
                ItemStack stack = visibleStacks.get(i);
                ItemStack stackCopy = stack.copy();
                stackHandler.setStackInSlot(i, stackCopy);
            }
            
            // Fill remaining slots with empty stacks if needed
            for (int i = visibleStacks.size(); i < VISIBLE_SLOTS; i++) {
                stackHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        
        // Then, force all slots to refresh by calling setChanged() on each one
        // This ensures the GUI actually displays the new items
        for (int i = 0; i < VISIBLE_SLOTS && i < this.slots.size(); i++) {
            Slot slot = this.slots.get(i);
            if (slot != null) {
                // Force slot to recognize the change
                slot.setChanged();
            }
        }
        
        // Force full broadcast to ensure all slots are synced
        this.broadcastFullState();
        
        LOGGER.debug("Client: Finished updating itemHandler/viewHandler and forcing slot refresh");
    }
    
    /**
     * Updates the scroll offset and refreshes visible slots
     * Called from both client (immediate update) and server (packet response)
     */
    public void setScrollOffset(int offset) {
        int maxOffset = Math.max(0, totalSlots - VISIBLE_SLOTS);
        int oldOffset = this.scrollOffset;
        this.scrollOffset = Math.max(0, Math.min(offset, maxOffset));
        
        // Only update container data on server side
        // Container data automatically syncs to client
        if (this.blockEntity != null && this.blockEntity.getLevel() != null && !this.blockEntity.getLevel().isClientSide()) {
            this.containerData.set(0, this.scrollOffset);
            
            // Server-side: When scroll changes, update the view handler
            if (oldOffset != this.scrollOffset) {
                LOGGER.debug("Server: scroll offset changed from {} to {}", oldOffset, this.scrollOffset);
                updateViewHandler(); // Copy new items into view handler
                
                // Force full state broadcast to sync all slots
                this.broadcastFullState();
            }
        } else {
            // Client-side: set flag to ignore containerData for a few ticks
            // This prevents reading stale data before server response arrives
            this.ignoreContainerDataTicks = 5;
            
            // Client-side: When scroll changes, force all slots to refresh
            // This ensures slots read from the new scroll position immediately
            if (oldOffset != this.scrollOffset) {
                LOGGER.debug("Client: scroll offset changed from {} to {}, forcing slot refresh", oldOffset, this.scrollOffset);
                // Force all visible slots to refresh by calling setChanged() on each one
                // This makes them re-read from offsetItemHandler which uses the new scrollOffset
                for (int i = 0; i < VISIBLE_SLOTS && i < this.slots.size(); i++) {
                    Slot slot = this.slots.get(i);
                    if (slot != null) {
                        slot.setChanged();
                    }
                }
                // Also broadcast the state change to ensure GUI updates
                this.broadcastFullState();
            }
        }
    }
    
    
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        
        // Client-side: sync scroll offset FROM container data (server authoritative)
        if (this.blockEntity == null || this.blockEntity.getLevel() == null || this.blockEntity.getLevel().isClientSide()) {
            // Decrement ignore counter
            if (this.ignoreContainerDataTicks > 0) {
                this.ignoreContainerDataTicks--;
            }
            
            // Only sync from containerData if we're not ignoring it
            if (this.ignoreContainerDataTicks == 0 && this.containerData.get(0) != this.scrollOffset) {
                this.scrollOffset = this.containerData.get(0);
            }
        }
    }
    
    
    /**
     * ItemHandler wrapper that automatically adds scrollOffset to all slot accesses
     * Similar to LimitedContainer in Tom's Storage mod
     * This allows slots to always access indices 0-53, while the wrapper handles the offset
     */
    private class OffsetItemHandler implements IItemHandlerModifiable {
        private final IItemHandler delegate;
        
        public OffsetItemHandler(IItemHandler delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public int getSlots() {
            return VISIBLE_SLOTS; // Always return visible slots count
        }
        
        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= VISIBLE_SLOTS) {
                return ItemStack.EMPTY;
            }
            int actualIndex = scrollOffset + slot;
            if (actualIndex < 0 || actualIndex >= totalSlots) {
                return ItemStack.EMPTY;
            }
            return delegate.getStackInSlot(actualIndex);
        }
        
        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot < 0 || slot >= VISIBLE_SLOTS) {
                return stack;
            }
            int actualIndex = scrollOffset + slot;
            if (actualIndex < 0 || actualIndex >= totalSlots) {
                return stack;
            }
            return delegate.insertItem(actualIndex, stack, simulate);
        }
        
        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < 0 || slot >= VISIBLE_SLOTS) {
                return ItemStack.EMPTY;
            }
            int actualIndex = scrollOffset + slot;
            if (actualIndex < 0 || actualIndex >= totalSlots) {
                return ItemStack.EMPTY;
            }
            return delegate.extractItem(actualIndex, amount, simulate);
        }
        
        @Override
        public int getSlotLimit(int slot) {
            if (slot < 0 || slot >= VISIBLE_SLOTS) {
                return 64;
            }
            int actualIndex = scrollOffset + slot;
            if (actualIndex < 0 || actualIndex >= totalSlots) {
                return 64;
            }
            return delegate.getSlotLimit(actualIndex);
        }
        
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot < 0 || slot >= VISIBLE_SLOTS) {
                return false;
            }
            int actualIndex = scrollOffset + slot;
            if (actualIndex < 0 || actualIndex >= totalSlots) {
                return false;
            }
            return delegate.isItemValid(actualIndex, stack);
        }
        
        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            if (slot < 0 || slot >= VISIBLE_SLOTS) {
                return;
            }
            int actualIndex = scrollOffset + slot;
            if (actualIndex < 0 || actualIndex >= totalSlots) {
                return;
            }
            if (delegate instanceof IItemHandlerModifiable modifiable) {
                modifiable.setStackInSlot(actualIndex, stack);
            } else {
                // Fallback: extract old item and insert new one
                delegate.extractItem(actualIndex, Integer.MAX_VALUE, false);
                if (!stack.isEmpty()) {
                    delegate.insertItem(actualIndex, stack, false);
                }
            }
        }
    }
    
    /**
     * Gets the current scroll offset
     */
    public int getScrollOffset() {
        return scrollOffset;
    }
    
    /**
     * Gets the total number of slots
     */
    public int getTotalSlots() {
        return totalSlots;
    }
    
    /**
     * Gets the maximum scroll offset
     */
    public int getMaxScrollOffset() {
        return Math.max(0, totalSlots - VISIBLE_SLOTS);
    }
    
    /**
     * Gets the block entity (server-side only)
     */
    public DeepDrawersBlockEntity getBlockEntity() {
        return blockEntity;
    }
    
    /**
     * Gets the block position
     */
    public BlockPos getBlockPos() {
        return blockPos;
    }
    
    /**
     * Client-side ItemStackHandler that reads from the client item cache
     * This is used instead of a real handler since the client doesn't have access to the BlockEntity
     */
}

