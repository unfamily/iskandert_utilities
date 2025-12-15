package net.unfamily.iskautils.client.gui;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.items.IItemHandler;
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
     * These slots read from the viewHandler which is updated when scrolling
     */
    private void addDeepDrawersSlots(Inventory playerInventory) {
        // Use viewHandler for display, but write to actual itemHandler
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int slotIndex = col + row * COLUMNS; // 0-53
                int xPos = STORAGE_SLOTS_X + col * 18;
                int yPos = STORAGE_SLOTS_Y + row * 18;
                // Create view slot that reads from viewHandler but writes to actual handler
                this.addSlot(new ViewSlot(slotIndex, xPos, yPos));
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
                                i, actualIndex, stack.isEmpty() ? "EMPTY" : stack.getItem());
                    
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
     * Updates the view handler from server-sent data (client-side only)
     * Called when receiving DeepDrawersSyncSlotsS2CPacket
     */
    public void updateViewHandlerFromServer(int offset, java.util.List<ItemStack> visibleStacks) {
        LOGGER.debug("Client: Updating viewHandler from server. Offset: {}, Stacks: {}", offset, visibleStacks.size());
        this.scrollOffset = offset;
        
        if (this.viewHandler instanceof net.neoforged.neoforge.items.ItemStackHandler stackHandler) {
            // First, update all items in viewHandler
            for (int i = 0; i < VISIBLE_SLOTS && i < visibleStacks.size(); i++) {
                ItemStack stack = visibleStacks.get(i);
                ItemStack stackCopy = stack.copy();
                
                // Get current item in viewHandler before update
                ItemStack current = stackHandler.getStackInSlot(i);
                
                // Update viewHandler
                stackHandler.setStackInSlot(i, stackCopy);
                LOGGER.debug("Client: ViewHandler slot {} = {} (was {})", 
                            i, stackCopy.isEmpty() ? "EMPTY" : stackCopy.getItem(),
                            current.isEmpty() ? "EMPTY" : current.getItem());
            }
            
            // Then, force all slots to refresh by calling setChanged() on each one
            // This ensures the GUI actually displays the new items
            for (int i = 0; i < VISIBLE_SLOTS && i < this.slots.size(); i++) {
                Slot slot = this.slots.get(i);
                if (slot != null) {
                    slot.setChanged();
                }
            }
        }
        
        // Force full broadcast to ensure all slots are synced
        this.broadcastFullState();
        
        LOGGER.debug("Client: Finished updating viewHandler and forcing slot refresh");
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
     * View slot that displays items from viewHandler but writes to actual itemHandler
     * This allows the GUI to show scrolled items while persisting changes to storage
     */
    private class ViewSlot extends SlotItemHandler {
        private final int viewIndex; // 0-53 (position in visible window)
        
        public ViewSlot(int viewIndex, int xPosition, int yPosition) {
            super(viewHandler, viewIndex, xPosition, yPosition);
            this.viewIndex = viewIndex;
        }
        
        @Override
        public @NotNull ItemStack getItem() {
            // Read from viewHandler for display
            ItemStack result = viewHandler.getStackInSlot(viewIndex);
            // Debug log for slots beyond 53
            if (scrollOffset + viewIndex >= 54 && scrollOffset + viewIndex < 108) {
                LOGGER.debug("ViewSlot.getItem(): viewIndex={}, scrollOffset={}, actualIndex={}, item={}", 
                            viewIndex, scrollOffset, scrollOffset + viewIndex, 
                            result.isEmpty() ? "EMPTY" : result.getItem());
            }
            return result;
        }
        
        @Override
        public void set(@NotNull ItemStack stack) {
            // Write to actual itemHandler at scrolled position
            int actualIndex = scrollOffset + viewIndex;
            if (itemHandler != null) {
                itemHandler.extractItem(actualIndex, Integer.MAX_VALUE, false);
                if (!stack.isEmpty()) {
                    itemHandler.insertItem(actualIndex, stack, false);
                }
                // Update viewHandler to reflect the change
                if (viewHandler instanceof net.neoforged.neoforge.items.ItemStackHandler stackHandler) {
                    stackHandler.setStackInSlot(viewIndex, stack.copy());
                }
                this.setChanged();
                LOGGER.debug("ViewSlot.set(): view={}, actual={}, item={}", viewIndex, actualIndex, 
                            stack.isEmpty() ? "EMPTY" : stack.getItem());
            }
        }
        
        @Override
        public @NotNull ItemStack remove(int amount) {
            // Remove from actual itemHandler at scrolled position
            int actualIndex = scrollOffset + viewIndex;
            ItemStack result = ItemStack.EMPTY;
            if (itemHandler != null) {
                result = itemHandler.extractItem(actualIndex, amount, false);
                if (!result.isEmpty()) {
                    // Update viewHandler to reflect the change
                    ItemStack remaining = itemHandler.getStackInSlot(actualIndex);
                    if (viewHandler instanceof net.neoforged.neoforge.items.ItemStackHandler stackHandler) {
                        stackHandler.setStackInSlot(viewIndex, remaining.copy());
                    }
                    this.setChanged();
                    LOGGER.debug("ViewSlot.remove(): view={}, actual={}, removed={}", viewIndex, actualIndex, result.getCount());
                }
            }
            return result;
        }
        
        @Override
        public int getMaxStackSize() {
            int actualIndex = scrollOffset + viewIndex;
            return itemHandler != null ? itemHandler.getSlotLimit(actualIndex) : 64;
        }
        
        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            int actualIndex = scrollOffset + viewIndex;
            return itemHandler != null && itemHandler.isItemValid(actualIndex, stack);
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

