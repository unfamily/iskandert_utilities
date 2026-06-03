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
import net.minecraft.core.HolderLookup;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.DeepDrawersBlockEntity;
import net.unfamily.iskautils.util.DeepDrawerItemFilter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Menu for the Deep Drawers GUI
 * 
 * Features:
 * - 45 visible slots (9 columns x 5 rows) plus client search bar above the grid
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
    private final IItemHandler viewHandler; // Handler for the visible slots (updated when scroll changes)
    private final OffsetItemHandler offsetItemHandler; // Wrapper that adds scrollOffset automatically
    
    // GUI Layout constants
    public static final int VISIBLE_ROWS = 5;
    public static final int COLUMNS = 9;
    public static final int VISIBLE_SLOTS = VISIBLE_ROWS * COLUMNS;
    public static final int SEARCH_BAR_X = 14;
    public static final int SEARCH_BAR_Y = 28;
    public static final int SEARCH_BAR_HEIGHT = 18;
    public static final int SEARCH_BAR_WIDTH = COLUMNS * 18;
    
    // Storage slots: 9 columns x 5 rows; texture row 0 removed (search bar). Anchor Y unchanged.
    public static final int STORAGE_SLOTS_X = 15;
    public static final int STORAGE_SLOTS_Y = 31;
    /** First visible row index in the texture grid (row 0 is gone). */
    public static final int STORAGE_FIRST_VISIBLE_ROW = 1;
    public static final int STORAGE_SLOTS_VISIBLE_Y = STORAGE_SLOTS_Y + STORAGE_FIRST_VISIBLE_ROW * 18;
    /** Between last storage row and player inventory (menu-relative). */
    public static final int CAPACITY_LABEL_Y = STORAGE_SLOTS_VISIBLE_Y + VISIBLE_ROWS * 18 + 4;
    
    // Player inventory: 9 columns x 3 rows + hotbar - texture moved 7px right, +2px down
    public static final int PLAYER_INVENTORY_X = 15;    // Original 8 + 7px right (texture change)
    public static final int PLAYER_INVENTORY_Y = 153;   // 151 + 2px down
    
    // Scrolling state
    private int scrollOffset = 0;
    private boolean searchFilterActive = false;
    private int filterScrollOffset = 0;
    private String lastSearchQuery = "";
    private final java.util.List<Integer> filteredOccupiedIndices = new java.util.ArrayList<>();
    private final int totalSlots;
    
    // Container data for syncing scroll offset and block position between server and client
    private final net.minecraft.world.inventory.ContainerData containerData;
    
    // ContainerData indices
    private static final int SCROLL_OFFSET_INDEX = 0;
    private static final int BLOCK_POS_X_INDEX = 1;
    private static final int BLOCK_POS_Y_INDEX = 2;
    private static final int BLOCK_POS_Z_INDEX = 3;
    private static final int OCCUPIED_COUNT_INDEX = 4;
    private static final int MAX_SLOTS_INDEX = 5;
    private static final int DATA_COUNT = 6;
    
    // Flag to prevent client from reading stale containerData after local update
    private int ignoreContainerDataTicks = 0;
    
    // Flag to prevent recursive calls to updateViewHandler
    private boolean updatingViewHandler = false;
    
    // Server-side constructor (called when opening GUI from block)
    public DeepDrawersMenu(int containerId, Inventory playerInventory, DeepDrawersBlockEntity blockEntity) {
        super(ModMenuTypes.DEEP_DRAWERS_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.itemHandler = blockEntity.getItemHandler();
        this.totalSlots = DeepDrawersBlockEntity.DEFAULT_MAX_SLOTS;
        
        // Always start with scroll offset 0 when opening GUI
        this.scrollOffset = 0;
        
        // Create view handler for visible slots
        this.viewHandler = new net.neoforged.neoforge.items.ItemStackHandler(VISIBLE_SLOTS);
        updateViewHandler(); // Populate with initial items
        
        // Create offset wrapper for itemHandler
        this.offsetItemHandler = new OffsetItemHandler(this.itemHandler);
        
        // Create container data for syncing scroll offset and block position
        this.containerData = new net.minecraft.world.inventory.ContainerData() {
            @Override
            public int get(int index) {
                return switch(index) {
                    case SCROLL_OFFSET_INDEX -> scrollOffset;
                    case BLOCK_POS_X_INDEX -> blockPos.getX();
                    case BLOCK_POS_Y_INDEX -> blockPos.getY();
                    case BLOCK_POS_Z_INDEX -> blockPos.getZ();
                    case OCCUPIED_COUNT_INDEX -> blockEntity.getOccupiedSlotsCount();
                    case MAX_SLOTS_INDEX -> blockEntity.getMaxSlots();
                    default -> 0;
                };
            }
            
            @Override
            public void set(int index, int value) {
                // Values are read-only from client side, handled by server
            }
            
            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
        this.addDataSlots(this.containerData);
        
        // Add Deep Drawers visible slots (9 columns x 5 rows)
        addDeepDrawersSlots(playerInventory);
        
        // Add player inventory slots
        addPlayerInventorySlots(playerInventory);
    }
    
    /**
     * Sends all slots to the client (server-side only)
     * Called when the menu is opened to sync all slot contents
     */
    public void sendAllSlotsToClient(net.minecraft.server.level.ServerPlayer player) {
        if (this.blockEntity == null || this.itemHandler == null) return;
        
        // Get all slots from the item handler
        java.util.Map<Integer, net.minecraft.world.item.ItemStack> allSlots = new java.util.HashMap<>();
        for (int i = 0; i < totalSlots; i++) {
            net.minecraft.world.item.ItemStack stack = this.itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                allSlots.put(i, stack.copy());
            }
        }
        
        // Send all slots to client
        net.unfamily.iskautils.network.ModMessages.sendToPlayer(
            new net.unfamily.iskautils.network.packet.DeepDrawersSyncSlotsS2CPacket(allSlots),
            player
        );
        
        LOGGER.debug("Server: Sent all {} slots to client", allSlots.size());
    }
    
    // Client-side constructor (NeoForge factory pattern) - simple version
    public DeepDrawersMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.DEEP_DRAWERS_MENU.get(), containerId);
        // Client-side: we don't have direct access to BlockEntity
        this.blockEntity = null;
        this.blockPos = BlockPos.ZERO;
        this.levelAccess = ContainerLevelAccess.NULL;
        this.itemHandler = new net.neoforged.neoforge.items.ItemStackHandler(DeepDrawersBlockEntity.DEFAULT_MAX_SLOTS);
        this.totalSlots = DeepDrawersBlockEntity.DEFAULT_MAX_SLOTS;
        
        // View handler for visible slots (synced by Minecraft)
        this.viewHandler = new net.neoforged.neoforge.items.ItemStackHandler(VISIBLE_SLOTS);
        
        // Create offset wrapper for itemHandler
        this.offsetItemHandler = new OffsetItemHandler(this.itemHandler);
        
        // Create container data for syncing scroll offset and block position (client-side)
        this.containerData = new net.minecraft.world.inventory.SimpleContainerData(DATA_COUNT);
        this.addDataSlots(this.containerData);
        
        // Initialize scrollOffset from ContainerData (will be synced from server)
        // Note: ContainerData might not be populated yet, but it will be synced soon
        // We'll also sync it in broadcastChanges() when data arrives
        
        // Add Deep Drawers visible slots (9 columns x 5 rows)
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
                int yPos = STORAGE_SLOTS_Y + (STORAGE_FIRST_VISIBLE_ROW + row) * 18;
                // Create slot that reads from offsetItemHandler (automatically adds scrollOffset)
                // Block insertion from GUI by overriding mayPlace
                // Limit extraction to 1 item at a time by overriding remove
                this.addSlot(new SlotItemHandler(offsetItemHandler, slotIndex, xPos, yPos) {
                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        // Do not use vanilla slot placement; we intercept clicks in clicked() for safe insertion.
                        return false;
                    }
                    
                    @Override
                    public @NotNull ItemStack remove(int amount) {
                        // Always extract only 1 item at a time
                        return super.remove(1);
                    }
                });
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
        boolean valid = stillValid(levelAccess, player, ModBlocks.DEEP_DRAWERS.get());
        if (!valid && blockEntity != null) {
            // GUI is closing, notify block entity
            blockEntity.onGuiClosed();
        }
        return valid;
    }
    
    @Override
    public void slotsChanged(net.minecraft.world.Container container) {
        super.slotsChanged(container);
        // Notify the block entity that slots have changed
        if (this.blockEntity != null) {
            this.blockEntity.setChanged();
            // Update viewHandler to reflect changes in physical storage
            // This ensures GUI shows correct items even when logical slots change
            // Only update if not already updating to prevent recursion
            if (!updatingViewHandler && this.blockEntity.getLevel() != null && !this.blockEntity.getLevel().isClientSide()) {
                updateViewHandler();
            }
        }
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot clicked = this.slots.get(index);
        if (clicked == null || !clicked.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stackInSlot = clicked.getItem();
        ItemStack original = stackInSlot.copy();

        // Storage grid (0..53): extract 1 directly into player inventory (no cursor).
        if (this.blockEntity != null
                && this.blockEntity.getLevel() != null
                && !this.blockEntity.getLevel().isClientSide()
                && index >= 0
                && index < VISIBLE_SLOTS) {
            if (searchFilterActive && offsetItemHandler.getStackInSlot(index).isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack extracted = this.offsetItemHandler.extractItem(index, 1, false);
            if (extracted.isEmpty()) {
                return ItemStack.EMPTY;
            }
            boolean inserted = player.getInventory().add(extracted);
            if (!inserted) {
                // Give back to drawer if inventory full.
                int virtualInsertSlot = Math.max(0, this.blockEntity.getOccupiedSlotsCount());
                this.blockEntity.getItemHandler().insertItem(virtualInsertSlot, extracted, false);
                return ItemStack.EMPTY;
            }
            this.blockEntity.setChanged();
            updateViewHandler();
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                sendAllSlotsToClient(serverPlayer);
            }
            refreshSearchFilterIfActive();
            return original;
        }

        // Player inventory / hotbar: shift-insert into drawer buffer (virtual insertion slot).
        if (this.blockEntity != null
                && this.blockEntity.getLevel() != null
                && !this.blockEntity.getLevel().isClientSide()) {
            if (this.blockEntity.isFull()) {
                return ItemStack.EMPTY;
            }
            int virtualInsertSlot = Math.max(0, this.blockEntity.getOccupiedSlotsCount());
            ItemStack remainder = this.blockEntity.getItemHandler().insertItem(virtualInsertSlot, stackInSlot.copy(), false);
            int insertedCount = stackInSlot.getCount() - remainder.getCount();
            if (insertedCount <= 0) {
                return ItemStack.EMPTY;
            }
            stackInSlot.shrink(insertedCount);
            clicked.set(stackInSlot.isEmpty() ? ItemStack.EMPTY : stackInSlot);
            clicked.setChanged();
            this.blockEntity.setChanged();
            updateViewHandler();
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                sendAllSlotsToClient(serverPlayer);
            }
            return original;
        }

        return ItemStack.EMPTY;
    }

    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ContainerInput containerInput, Player player) {
        // Safe GUI insertion: do not rely on SlotItemHandler default placement logic (can desync and "eat" items).
        // When the player is carrying an item and clicks the storage grid, insert via the block entity handler.
        if (
                containerInput == net.minecraft.world.inventory.ContainerInput.PICKUP
                        && this.blockEntity != null
                        && this.blockEntity.getLevel() != null
                        && !this.blockEntity.getLevel().isClientSide()
                        && slotId >= 0
                        && slotId < VISIBLE_SLOTS
        ) {
            ItemStack carried = this.getCarried();
            if (!carried.isEmpty()) {
                if (this.blockEntity.isFull()) {
                    return; // Do not consume items when full.
                }
                // Insert using the virtual insertion slot (last slot) of the sparse handler.
                int virtualInsertSlot = Math.max(0, this.blockEntity.getOccupiedSlotsCount());
                ItemStack remainder = this.blockEntity.getItemHandler().insertItem(virtualInsertSlot, carried, false);
                this.setCarried(remainder);
                this.blockEntity.setChanged();
                updateViewHandler();
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    sendAllSlotsToClient(serverPlayer);
                }
                return;
            }
        }
        if (searchFilterActive
                && slotId >= 0
                && slotId < VISIBLE_SLOTS
                && offsetItemHandler.getStackInSlot(slotId).isEmpty()
                && containerInput == net.minecraft.world.inventory.ContainerInput.PICKUP) {
            ItemStack carried = this.getCarried();
            if (carried.isEmpty()) {
                return;
            }
        }
        super.clicked(slotId, button, containerInput, player);
    }
    
    /**
     * Updates the view handler with items from the current scroll position
     * This is called when the scroll offset changes to populate the visible slots
     * Server-side only: reads from actual itemHandler
     */
    private void updateViewHandler() {
        // Prevent recursive calls
        if (updatingViewHandler) {
            return;
        }
        
        if (this.blockEntity != null && 
            !this.blockEntity.getLevel().isClientSide()) {
            updatingViewHandler = true;
            try {
                LOGGER.debug("Updating viewHandler for scroll offset: {}", this.scrollOffset);
                // Copy items from actual storage to the view handler
                if (this.viewHandler instanceof net.neoforged.neoforge.items.ItemStackHandler stackHandler) {
                    for (int i = 0; i < VISIBLE_SLOTS; i++) {
                        int occupiedIndex = this.scrollOffset + i;
                        int physicalSlot = this.blockEntity.getPhysicalSlotAtOccupiedIndex(occupiedIndex);
                        ItemStack stack = physicalSlot >= 0
                                ? this.blockEntity.getStackInPhysicalSlot(physicalSlot)
                                : ItemStack.EMPTY;
                        stackHandler.setStackInSlot(i, stack.copy());
                    }
                    // Force full state broadcast to sync to client (this will update slots without recursion)
                    this.broadcastFullState();
                }
            } finally {
                updatingViewHandler = false;
            }
        }
    }
    
    /**
     * Updates ALL slots from server-sent data (client-side only)
     * Called when receiving DeepDrawersSyncSlotsS2CPacket with full sync
     * This loads all slots into client memory so scrolling works instantly
     */
    public void updateAllSlotsFromServer(java.util.Map<Integer, ItemStack> allSlots) {
        LOGGER.debug("Client: Updating ALL slots from server. Slots: {}", allSlots.size());
        
        // Update itemHandler lato client con TUTTI gli slot
        if (this.itemHandler instanceof net.neoforged.neoforge.items.ItemStackHandler itemStackHandler) {
            // First, clear all slots (set to empty)
            for (int i = 0; i < totalSlots; i++) {
                itemStackHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
            
            // Then, update with all non-empty slots from server
            for (java.util.Map.Entry<Integer, ItemStack> entry : allSlots.entrySet()) {
                int slotIndex = entry.getKey();
                ItemStack stack = entry.getValue();
                if (slotIndex >= 0 && slotIndex < totalSlots && !stack.isEmpty()) {
                    itemStackHandler.setStackInSlot(slotIndex, stack.copy());
                    LOGGER.debug("Client: ItemHandler slot {} = {}", slotIndex, stack.getItem());
                }
            }
        }
        
        // Also update viewHandler with visible slots based on current scroll offset
        if (this.viewHandler instanceof net.neoforged.neoforge.items.ItemStackHandler stackHandler) {
            for (int i = 0; i < VISIBLE_SLOTS; i++) {
                int actualIndex = this.scrollOffset + i;
                if (actualIndex >= 0 && actualIndex < totalSlots) {
                    ItemStack stack = allSlots.getOrDefault(actualIndex, ItemStack.EMPTY);
                    stackHandler.setStackInSlot(i, stack.copy());
                } else {
                    stackHandler.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }
        
        // Force all visible slots to refresh
        for (int i = 0; i < VISIBLE_SLOTS && i < this.slots.size(); i++) {
            Slot slot = this.slots.get(i);
            if (slot != null) {
                // Force slot to re-read from offsetItemHandler (which now has all data)
                ItemStack currentItem = offsetItemHandler.getStackInSlot(i);
                slot.set(currentItem);
                slot.setChanged();
            }
        }
        
        // Force full broadcast to ensure all slots are synced
        this.broadcastFullState();
        
        LOGGER.debug("Client: Finished updating ALL slots from server");
    }
    
    /**
     * Updates the item handler and view handler from server-sent data (client-side only)
     * Called when receiving DeepDrawersSyncSlotsS2CPacket (backward compatibility)
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
        
        // Then, force all slots to refresh by re-reading from offsetItemHandler
        // This ensures the GUI actually displays the new items
        for (int i = 0; i < VISIBLE_SLOTS && i < this.slots.size(); i++) {
            Slot slot = this.slots.get(i);
            if (slot != null) {
                // Force slot to re-read by getting the item from offsetItemHandler
                // offsetItemHandler will use the updated scrollOffset and read from itemHandler
                ItemStack currentItem = offsetItemHandler.getStackInSlot(i);
                // Set the item to force the slot to refresh
                slot.set(currentItem);
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
        if (searchFilterActive) {
            return;
        }
        int maxOffset = getEffectiveMaxScrollOffset();
        int oldOffset = this.scrollOffset;
        this.scrollOffset = Math.max(0, Math.min(offset, maxOffset));
        
        // Only update container data on server side
        // Container data automatically syncs to client
        if (this.blockEntity != null && this.blockEntity.getLevel() != null && !this.blockEntity.getLevel().isClientSide()) {
            // Note: ContainerData is read-only on server, values are read from get() method
            // We don't save scrollOffset to BlockEntity - it resets to 0 when GUI opens
            
            // Server-side: When scroll changes, update the view handler
            if (oldOffset != this.scrollOffset) {
                LOGGER.debug("Server: scroll offset changed from {} to {} (saved to BlockEntity)", oldOffset, this.scrollOffset);
                updateViewHandler(); // Copy new items into view handler
                
                // Force full state broadcast to sync all slots
                this.broadcastFullState();
            }
        } else {
            // Client-side: set flag to ignore containerData for a few ticks
            // This prevents reading stale data before server response arrives
            // Increased to 20 ticks (1 second) to give server time to process scroll packet
            this.ignoreContainerDataTicks = 20;
            
            // Client-side: When scroll changes, force all slots to refresh
            // This ensures slots read from the new scroll position immediately
            if (oldOffset != this.scrollOffset) {
                LOGGER.debug("Client: scroll offset changed from {} to {}, forcing slot refresh", oldOffset, this.scrollOffset);
                
                // Force all visible slots to refresh by re-reading from offsetItemHandler
                // Since all slots are now in memory, we just need to refresh the display
                // offsetItemHandler uses the updated scrollOffset and reads from itemHandler
                // which now has all slots in memory
                for (int i = 0; i < VISIBLE_SLOTS && i < this.slots.size(); i++) {
                    Slot slot = this.slots.get(i);
                    if (slot != null) {
                        // Force slot to re-read from offsetItemHandler (which has all data in memory)
                        ItemStack currentItem = offsetItemHandler.getStackInSlot(i);
                        slot.set(currentItem);
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
        // BUT: Don't sync if we just scrolled locally (to prevent resetting to 0)
        if (this.blockEntity == null || this.blockEntity.getLevel() == null || this.blockEntity.getLevel().isClientSide()) {
            // Decrement ignore counter
            if (this.ignoreContainerDataTicks > 0) {
                this.ignoreContainerDataTicks--;
            }
            
            // Only sync from containerData if we're not ignoring it AND if the server value is different
            // IMPORTANT: Never sync from server=0 to client non-zero value, as this would reset the scroll
            // The server should always have the correct value after the packet is processed
            if (this.ignoreContainerDataTicks == 0 && !searchFilterActive) {
                int serverOffset = this.containerData.get(SCROLL_OFFSET_INDEX);
                // Only sync if server offset is different AND not a reset to 0 when we have a non-zero value
                if (serverOffset != this.scrollOffset) {
                    // NEVER sync from server=0 to client non-zero - this would reset the scroll
                    // The server should have the correct value after the packet is processed
                    if (serverOffset == 0 && this.scrollOffset != 0) {
                        LOGGER.warn("Client: Ignoring containerData sync (server=0, local={}) - server hasn't updated yet or packet didn't arrive", this.scrollOffset);
                        // Don't sync - keep our local value
                        return;
                    }
                    // Otherwise, sync from server (server has the authoritative value)
                    LOGGER.debug("Client: Syncing scrollOffset from containerData: {} -> {}", this.scrollOffset, serverOffset);
                    this.scrollOffset = serverOffset;
                }
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
        
        /**
         * Gets the current scroll offset, ensuring it's always up-to-date
         * On server, uses the local scrollOffset field (which is updated when scroll packet arrives)
         * On client, uses the local scrollOffset field (which is updated immediately when scrolling)
         * 
         * IMPORTANT: This method is called during slot interactions, so it must always return
         * the correct scrollOffset. On server, the scrollOffset is updated when the scroll packet
         * arrives, so it should be correct. But we add a safety check to ensure it's in sync.
         */
        private int getCurrentScrollOffset() {
            return getEffectiveScrollOffset();
        }

        private int resolveLogicalIndexForVisibleSlot(int visibleSlot) {
            if (searchFilterActive) {
                int listIndex = filterScrollOffset + visibleSlot;
                if (listIndex < 0 || listIndex >= filteredOccupiedIndices.size()) {
                    return -1;
                }
                return filteredOccupiedIndices.get(listIndex);
            }
            return scrollOffset + visibleSlot;
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
            int logicalIndex = resolveLogicalIndexForVisibleSlot(slot);
            if (logicalIndex < 0) {
                return ItemStack.EMPTY;
            }
            if (blockEntity != null) {
                int physicalSlot = blockEntity.getPhysicalSlotAtOccupiedIndex(logicalIndex);
                if (physicalSlot < 0) {
                    return ItemStack.EMPTY;
                }
                return blockEntity.getStackInPhysicalSlot(physicalSlot);
            }
            return delegate.getStackInSlot(logicalIndex);
        }
        
        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot < 0 || slot >= VISIBLE_SLOTS || stack.isEmpty()) {
                return stack;
            }
            int logicalIndex = resolveLogicalIndexForVisibleSlot(slot);
            if (logicalIndex < 0) {
                return stack;
            }

            // Delegate is the sparse logical handler: it exposes (occupiedSlots + 1) where the last slot is virtual insertion.
            int delegateSlots = delegate.getSlots();
            if (delegateSlots <= 0) {
                return stack;
            }
            int logicalSlot = Math.min(logicalIndex, delegateSlots - 1);
            ItemStack remainder = delegate.insertItem(logicalSlot, stack, simulate);
            if (!simulate && blockEntity != null && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide()) {
                // Force refresh + sync so the inserted item is immediately visible client-side.
                blockEntity.setChanged();
                updateViewHandler();
            }
            return remainder;
        }
        
        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < 0 || slot >= VISIBLE_SLOTS) {
                return ItemStack.EMPTY;
            }
            int logicalIndex = resolveLogicalIndexForVisibleSlot(slot);
            if (logicalIndex < 0) {
                return ItemStack.EMPTY;
            }
            // Always extract only 1 item at a time (even if more is requested)
            // This ensures shift-click and normal click both extract only 1 item
            try {
                DeepDrawersBlockEntity.ITEM_HANDLER_ALLOW_EXTRACT.set(true);
                return delegate.extractItem(logicalIndex, 1, simulate);
            } finally {
                DeepDrawersBlockEntity.ITEM_HANDLER_ALLOW_EXTRACT.set(false);
            }
        }
        
        @Override
        public int getSlotLimit(int slot) {
            if (slot < 0 || slot >= VISIBLE_SLOTS) {
                return 64;
            }
            int logicalIndex = resolveLogicalIndexForVisibleSlot(slot);
            if (logicalIndex < 0) {
                return 64;
            }
            return delegate.getSlotLimit(logicalIndex);
        }
        
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            // Block all insertions from GUI
            return false;
        }
        
        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            // Block all insertions from GUI - only allow clearing slots
            if (slot < 0 || slot >= VISIBLE_SLOTS) {
                return;
            }
            int logicalIndex = resolveLogicalIndexForVisibleSlot(slot);
            if (logicalIndex < 0) {
                return;
            }
            // Only allow clearing slots (extracting), not inserting
            if (stack.isEmpty()) {
                // Allow extraction by clearing the slot
                if (delegate instanceof IItemHandlerModifiable modifiable) {
                    modifiable.setStackInSlot(logicalIndex, ItemStack.EMPTY);
                } else {
                    try {
                        DeepDrawersBlockEntity.ITEM_HANDLER_ALLOW_EXTRACT.set(true);
                        delegate.extractItem(logicalIndex, Integer.MAX_VALUE, false);
                    } finally {
                        DeepDrawersBlockEntity.ITEM_HANDLER_ALLOW_EXTRACT.set(false);
                    }
                }
            }
            // If stack is not empty, block the insertion
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

    /** Occupied slot count for capacity label (same values as shift+click status). */
    public int getDisplayedOccupiedCount() {
        if (blockEntity != null) {
            return blockEntity.getOccupiedSlotsCount();
        }
        return Math.max(0, containerData.get(OCCUPIED_COUNT_INDEX));
    }

    /** Max slot count for capacity label (same values as shift+click status). */
    public int getDisplayedMaxSlots() {
        if (blockEntity != null) {
            return blockEntity.getMaxSlots();
        }
        int synced = containerData.get(MAX_SLOTS_INDEX);
        return synced > 0 ? synced : DeepDrawersBlockEntity.DEFAULT_MAX_SLOTS;
    }
    
    /**
     * Gets the maximum scroll offset
     */
    public int getMaxScrollOffset() {
        return getEffectiveMaxScrollOffset();
    }

    public boolean isSearchFilterActive() {
        return searchFilterActive;
    }

    public int getEffectiveScrollOffset() {
        return searchFilterActive ? filterScrollOffset : scrollOffset;
    }

    public void setEffectiveScrollOffset(int offset) {
        if (searchFilterActive) {
            setFilterScrollOffset(offset);
        } else {
            setScrollOffset(offset);
        }
    }

    public void setFilterScrollOffset(int offset) {
        if (!searchFilterActive) {
            return;
        }
        int max = getEffectiveMaxScrollOffset();
        filterScrollOffset = Math.max(0, Math.min(offset, max));
        refreshVisibleSlots();
    }

    public int getScrollContentSize() {
        if (searchFilterActive) {
            return filteredOccupiedIndices.size();
        }
        if (blockEntity != null) {
            return blockEntity.getOccupiedSlotsCount();
        }
        return totalSlots;
    }

    public int getEffectiveMaxScrollOffset() {
        return Math.max(0, getScrollContentSize() - VISIBLE_SLOTS);
    }

    /**
     * Client-side name filter (case-insensitive): visible grid shows only matching occupied slots.
     */
    private void refreshSearchFilterIfActive() {
        if (!searchFilterActive || blockEntity == null || blockEntity.getLevel() == null) {
            return;
        }
        int preserved = filterScrollOffset;
        applySearchFilter(lastSearchQuery, blockEntity.getLevel().registryAccess());
        setFilterScrollOffset(Math.min(preserved, getEffectiveMaxScrollOffset()));
    }

    public void applySearchFilter(@NotNull String query, @NotNull HolderLookup.Provider registryAccess) {
        String trimmed = query == null ? "" : query.trim();
        lastSearchQuery = trimmed;
        if (trimmed.isEmpty()) {
            searchFilterActive = false;
            filteredOccupiedIndices.clear();
            filterScrollOffset = 0;
            refreshVisibleSlots();
            if (blockEntity != null && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide()) {
                updateViewHandler();
            }
            return;
        }
        searchFilterActive = true;
        filteredOccupiedIndices.clear();
        filterScrollOffset = 0;
        if (itemHandler != null) {
            int slotCount = blockEntity != null
                    ? blockEntity.getOccupiedSlotsCount()
                    : totalSlots;
            for (int i = 0; i < slotCount; i++) {
                ItemStack stack = itemHandler.getStackInSlot(i);
                if (!stack.isEmpty() && DeepDrawerItemFilter.matchesSearch(stack, trimmed, registryAccess)) {
                    filteredOccupiedIndices.add(i);
                }
            }
        }
        refreshVisibleSlots();
    }

    public void refreshVisibleSlots() {
        for (int i = 0; i < VISIBLE_SLOTS && i < slots.size(); i++) {
            Slot slot = slots.get(i);
            if (slot != null) {
                slot.set(offsetItemHandler.getStackInSlot(i));
                slot.setChanged();
            }
        }
        broadcastFullState();
    }

    /**
     * Gets the block entity (server-side only)
     */
    public DeepDrawersBlockEntity getBlockEntity() {
        return blockEntity;
    }
    
    /**
     * Gets the block position (synced from server on client)
     */
    public BlockPos getBlockPos() {
        if (this.blockEntity != null) {
            return blockPos; // Server side
        } else {
            // Client side - get from synced data
            int x = this.containerData.get(BLOCK_POS_X_INDEX);
            int y = this.containerData.get(BLOCK_POS_Y_INDEX);
            int z = this.containerData.get(BLOCK_POS_Z_INDEX);
            if (x == 0 && y == 0 && z == 0) {
                return blockPos; // Fallback to stored position
            }
            return new BlockPos(x, y, z);
        }
    }
    
    /**
     * Gets the synced block position (same as getBlockPos, but more explicit)
     */
    public BlockPos getSyncedBlockPos() {
        return getBlockPos();
    }
    
    /**
     * Method to get block entity from level (for client-side actions)
     * Uses synced position
     */
    public DeepDrawersBlockEntity getBlockEntityFromLevel(net.minecraft.world.level.Level level) {
        if (this.blockEntity != null) {
            return this.blockEntity; // Server side
        } else {
            // Client side - use synced position
            BlockPos syncedPos = getSyncedBlockPos();
            if (!syncedPos.equals(BlockPos.ZERO)) {
                net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(syncedPos);
                if (be instanceof DeepDrawersBlockEntity drawerEntity) {
                    return drawerEntity;
                }
            }
            return null;
        }
    }
    
    /**
     * Client-side ItemStackHandler that reads from the client item cache
     * This is used instead of a real handler since the client doesn't have access to the BlockEntity
     */
}
