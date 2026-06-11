package net.unfamily.iskautils.client.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.DeepDrawerExtractorBlockEntity;
import net.unfamily.iskautils.integration.anotherdynamics.AnotherDynamicsCompat;
import net.unfamily.iskautils.integration.anotherdynamics.DeepDrawerSettingsCopierLogic;

/**
 * Menu for Deep Drawer Extractor GUI
 */
public class DeepDrawerExtractorMenu extends AbstractContainerMenu {
    /** Scrollbar column layout (menu-relative; shared with screen). */
    public static final int SCROLLBAR_X = 8 + 140 + 4;
    public static final int SCROLLBAR_WIDTH = 8;
    public static final int FIRST_ROW_Y = 30 + 12;
    public static final int SCROLLBAR_Y = FIRST_ROW_Y + 8;
    public static final int SCROLLBAR_HEIGHT = 34;
    public static final int BUTTON_DOWN_Y = SCROLLBAR_Y + SCROLLBAR_HEIGHT;

    public static final int GUI_WIDTH = 400;
    public static final int REDSTONE_BUTTON_SIZE = 16;
    public static final int REDSTONE_GUI_X = GUI_WIDTH - 8 - REDSTONE_BUTTON_SIZE;
    public static final int COPY_COLUMN_GAP = 2;
    public static final int COPIER_ACTION_BUTTON_W = 18;
    public static final int COPIER_ACTION_BUTTON_H = 12;
    /** Settings copier column: centered under the redstone button. */
    public static final int COPIER_COLUMN_X = REDSTONE_GUI_X + (REDSTONE_BUTTON_SIZE - COPIER_ACTION_BUTTON_W) / 2;
    public static final int COPIER_SLOT_BACKGROUND_Y = FIRST_ROW_Y + REDSTONE_BUTTON_SIZE + COPY_COLUMN_GAP;
    public static final int COPIER_SLOT_X = COPIER_COLUMN_X;
    public static final int COPIER_SLOT_Y = COPIER_SLOT_BACKGROUND_Y;
    public static final int COPIER_SLOT_SIZE = 18;
    public static final int COPIER_SLOT_HIGHLIGHT_SIZE = 16;
    public static final int COPIER_SLOT_HIGHLIGHT_INSET = (COPIER_SLOT_SIZE - COPIER_SLOT_HIGHLIGHT_SIZE) / 2;
    public static final int COPIER_ITEM_INSET = COPIER_SLOT_HIGHLIGHT_INSET;
    public static final int COPIER_SAVE_BUTTON_Y = COPIER_SLOT_BACKGROUND_Y + 18 + COPY_COLUMN_GAP;
    public static final int COPIER_LOAD_BUTTON_Y = COPIER_SAVE_BUTTON_Y + COPIER_ACTION_BUTTON_H + COPY_COLUMN_GAP;

    /** Player inventory grid (menu-local). */
    public static final int PLAYER_INV_X = 159;
    public static final int PLAYER_INV_Y = 165;

    /** Filter edit block: anchored directly above player inventory. */
    public static final int EDIT_SLOT_SIZE = 18;
    public static final int EDIT_TEXTBOX_HEIGHT = 15;
    public static final int EDIT_ROW_GAP = 2;
    public static final int EDIT_BTN_SIZE = 12;
    public static final int EDIT_BTN_SPACING = 2;
    public static final int EDIT_ARROW_GAP = 4;
    public static final int EDIT_ACTION_GAP = 8;
    public static final int EDIT_TEXTBOX_Y = PLAYER_INV_Y - EDIT_ROW_GAP - EDIT_TEXTBOX_HEIGHT;
    public static final int EDIT_MODE_PANEL_Y = EDIT_TEXTBOX_Y - EDIT_ROW_GAP - EDIT_SLOT_SIZE;
    public static final int EDIT_MODE_GHOST_SLOT_X = PLAYER_INV_X + EDIT_SLOT_SIZE - 1;

    /** Valid Keys sits on the edit row, after the close button. */
    public static int editModeCloseButtonX() {
        int slotX = EDIT_MODE_GHOST_SLOT_X;
        int rightButtonX = slotX + EDIT_SLOT_SIZE + EDIT_ARROW_GAP;
        int clearButtonX = rightButtonX + EDIT_BTN_SIZE + EDIT_ACTION_GAP;
        int applyButtonX = clearButtonX + EDIT_BTN_SIZE + EDIT_BTN_SPACING;
        return applyButtonX + EDIT_BTN_SIZE + EDIT_BTN_SPACING;
    }

    public static int validKeysButtonX() {
        return editModeCloseButtonX() + EDIT_BTN_SIZE + EDIT_ROW_GAP;
    }

    public static int validKeysButtonY(int navButtonHeight) {
        return EDIT_MODE_PANEL_Y + (EDIT_SLOT_SIZE - navButtonHeight) / 2;
    }

    public static int copierSlotHighlightX(int frameX) {
        return frameX + COPIER_SLOT_HIGHLIGHT_INSET;
    }

    public static int copierSlotHighlightY(int frameY) {
        return frameY + COPIER_SLOT_HIGHLIGHT_INSET;
    }

    public static int copierSlotItemX(int frameX) {
        return frameX + COPIER_ITEM_INSET;
    }

    public static int copierSlotItemY(int frameY) {
        return frameY + COPIER_ITEM_INSET;
    }

    private final DeepDrawerExtractorBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final BlockPos blockPos;
    private final ContainerData containerData;
    
    // ContainerData indices
    private static final int REDSTONE_MODE_INDEX = 0;
    private static final int BLOCK_POS_X_INDEX = 1;
    private static final int BLOCK_POS_Y_INDEX = 2;
    private static final int BLOCK_POS_Z_INDEX = 3;
    private static final int FILTER_HASH_INDEX = 4; // Hash of filter fields
    private static final int INVERTED_FILTER_HASH_INDEX = 5; // Hash of inverted filter fields
    private static final int WHITELIST_MODE_INDEX = 6; // 0 = blacklist, 1 = whitelist
    private static final int LAST_FILTER_PANEL_INDEX = 7; // 0 = allow, 1 = deny
    private static final int DATA_SIZE = 8;
    
    // Cached filter data (client-side) - now using dynamic list
    private java.util.List<String> cachedFilterFields = new java.util.ArrayList<>();
    private java.util.List<String> cachedInvertedFilterFields = new java.util.ArrayList<>();
    private java.util.List<Integer> cachedAllowConcatChannels = new java.util.ArrayList<>();
    private java.util.List<Integer> cachedDenyConcatChannels = new java.util.ArrayList<>();
    private boolean cachedWhitelistMode = false;
    private int lastSyncedFilterHash = 0;
    private int lastSyncedInvertedFilterHash = 0;
    private final net.minecraft.world.SimpleContainer copierContainer = new net.minecraft.world.SimpleContainer(1);
    private final boolean includeCopierSlot;
    
    // Server-side constructor
    public DeepDrawerExtractorMenu(int containerId, Inventory playerInventory, DeepDrawerExtractorBlockEntity blockEntity) {
        super(ModMenuTypes.DEEP_DRAWER_EXTRACTOR_MENU.get(), containerId);
        this.includeCopierSlot = AnotherDynamicsCompat.isLoaded();
        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        
        // Initialize cached data on server side
        this.cachedFilterFields = new java.util.ArrayList<>(blockEntity.getFilterFields());
        this.cachedInvertedFilterFields = new java.util.ArrayList<>(blockEntity.getInvertedFilterFields());
        this.cachedAllowConcatChannels = new java.util.ArrayList<>(blockEntity.getAllowConcatChannels());
        this.cachedDenyConcatChannels = new java.util.ArrayList<>(blockEntity.getDenyConcatChannels());
        this.cachedWhitelistMode = blockEntity.isWhitelistMode();
        this.lastSyncedFilterHash = calculateFilterHash(this.cachedFilterFields, this.cachedAllowConcatChannels);
        this.lastSyncedInvertedFilterHash = calculateFilterHash(this.cachedInvertedFilterFields, this.cachedDenyConcatChannels);
        
        // Create ContainerData that syncs redstone mode, block position, filter hash, and whitelist mode
        this.containerData = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case REDSTONE_MODE_INDEX -> blockEntity.getRedstoneMode();
                    case BLOCK_POS_X_INDEX -> blockPos.getX();
                    case BLOCK_POS_Y_INDEX -> blockPos.getY();
                    case BLOCK_POS_Z_INDEX -> blockPos.getZ();
                    case FILTER_HASH_INDEX -> calculateFilterHash(blockEntity.getFilterFields(), blockEntity.getAllowConcatChannels());
                    case INVERTED_FILTER_HASH_INDEX -> calculateFilterHash(blockEntity.getInvertedFilterFields(), blockEntity.getDenyConcatChannels());
                    case WHITELIST_MODE_INDEX -> blockEntity.isWhitelistMode() ? 1 : 0;
                    case LAST_FILTER_PANEL_INDEX -> blockEntity.getLastFilterPanel();
                    default -> 0;
                };
            }
            
            @Override
            public void set(int index, int value) {
                // Values are read-only from client side, handled by server
                // The set() method is called by Minecraft's sync system, but we don't want to modify the server-side value here
            }
            
            @Override
            public int getCount() {
                return DATA_SIZE;
            }
        };
        
        // Add data slots for synchronization
        addDataSlots(this.containerData);
        
        // Notify block entity that GUI is opened
        if (!blockEntity.getLevel().isClientSide()) {
            blockEntity.onGuiOpened();
        }
        
        // Add buffer slots (5 slots in a row)
        addBufferSlots();
        
        // Add player inventory and hotbar
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addCopierSlot();
    }

    // Client-side constructor (NeoForge factory)
    public DeepDrawerExtractorMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.DEEP_DRAWER_EXTRACTOR_MENU.get(), containerId);
        this.includeCopierSlot = AnotherDynamicsCompat.isLoaded();
        // Client-side: we don't have direct access to the BlockEntity
        this.blockEntity = null;
        this.blockPos = BlockPos.ZERO;
        this.levelAccess = ContainerLevelAccess.NULL;
        this.containerData = new SimpleContainerData(DATA_SIZE);
        this.cachedFilterFields = new java.util.ArrayList<>();
        this.cachedInvertedFilterFields = new java.util.ArrayList<>();
        // Initialize cache from ContainerData if available (will be updated in updateCachedFilters)
        this.cachedWhitelistMode = false; // Will be updated when ContainerData syncs
        this.lastSyncedFilterHash = 0;
        this.lastSyncedInvertedFilterHash = 0;
        addDataSlots(this.containerData);
        
        // Add buffer slots (5 slots in a row) - client-side fallback
        addBufferSlots();
        
        // Add player inventory and hotbar
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addCopierSlot();
    }

    private void addCopierSlot() {
        if (!includeCopierSlot) {
            return;
        }
        addSlot(new Slot(copierContainer, 0, COPIER_SLOT_X, COPIER_SLOT_Y) {
            @Override
            public boolean isHighlightable() {
                return false;
            }

            @Override
            public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                return DeepDrawerSettingsCopierLogic.isSettingsCopier(stack);
            }
        });
    }

    public int copySettingsSlotIndex() {
        if (!includeCopierSlot) {
            return -1;
        }
        return slots.size() - 1;
    }

    public boolean includesCopierSlot() {
        return includeCopierSlot;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.DEEP_DRAWER_EXTRACTOR.get());
    }

    /**
     * Adds buffer slots (5 slots in a row)
     */
    private void addBufferSlots() {
        net.neoforged.neoforge.items.IItemHandler itemHandler;
        
        if (blockEntity != null) {
            itemHandler = blockEntity.getItemHandler();
        } else {
            // Client-side fallback
            itemHandler = new net.neoforged.neoforge.items.ItemStackHandler(5);
        }
        
        // 5 slots in a row at position 32, 223
        for (int i = 0; i < 5; i++) {
            int xPos = 32 + i * 18; // 18 pixels per slot
            int yPos = 223;
            addSlot(new SlotItemHandler(itemHandler, i, xPos, yPos));
        }
    }
    
    /**
     * Adds player inventory slots (3 rows x 9 slots)
     */
    private void addPlayerInventory(Inventory playerInventory) {
        // Player inventory at position 159, 165 (2px right from previous)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9 + 9; // +9 to skip hotbar
                int xPos = 159 + col * 18;
                int yPos = 165 + row * 18;
                addSlot(new Slot(playerInventory, slotIndex, xPos, yPos));
            }
        }
    }
    
    /**
     * Adds player hotbar slots (1 row x 9 slots)
     */
    private void addPlayerHotbar(Inventory playerInventory) {
        // Hotbar below inventory
        for (int col = 0; col < 9; col++) {
            int xPos = 159 + col * 18;
            int yPos = 165 + 3 * 18 + 4; // Below inventory with spacing
            addSlot(new Slot(playerInventory, col, xPos, yPos));
        }
    }
    
    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        net.minecraft.world.item.ItemStack itemstack = net.minecraft.world.item.ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return itemstack;
        }
        net.minecraft.world.item.ItemStack stack = slot.getItem();
        itemstack = stack.copy();
        int copierIdx = copySettingsSlotIndex();
        int playerStart = 5;
        int playerEnd = copierIdx >= 0 ? copierIdx : this.slots.size();

        if (index < 5) {
            if (!this.moveItemStackTo(stack, playerStart, playerEnd, true)) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }
        } else if (copierIdx >= 0 && index == copierIdx) {
            if (!this.moveItemStackTo(stack, playerStart, playerEnd, true)) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }
        } else if (DeepDrawerSettingsCopierLogic.isSettingsCopier(stack) && copierIdx >= 0) {
            if (!this.moveItemStackTo(stack, copierIdx, copierIdx + 1, false)) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(stack, 0, 5, false)) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(net.minecraft.world.item.ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return itemstack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (includeCopierSlot && player instanceof net.minecraft.server.level.ServerPlayer) {
            clearContainer(player, copierContainer);
        }
    }
    
    public DeepDrawerExtractorBlockEntity getBlockEntity() {
        return blockEntity;
    }
    
    public BlockPos getBlockPos() {
        return blockPos;
    }
    
    public BlockPos getSyncedBlockPos() {
        // Get position from synced data if available, otherwise use stored position
        if (this.blockEntity != null) {
            return this.blockPos; // Server side
        } else {
            // Client side - get from synced data
            int x = this.containerData.get(BLOCK_POS_X_INDEX);
            int y = this.containerData.get(BLOCK_POS_Y_INDEX);
            int z = this.containerData.get(BLOCK_POS_Z_INDEX);
            if (x == 0 && y == 0 && z == 0) {
                return this.blockPos; // Fallback to stored position
            }
            return new BlockPos(x, y, z);
        }
    }
    
    public int getRedstoneMode() {
        return this.containerData.get(REDSTONE_MODE_INDEX);
    }
    
    /**
     * Method to get block entity from level (for client-side actions)
     * Uses synced position only (search disabled)
     */
    public DeepDrawerExtractorBlockEntity getBlockEntityFromLevel(net.minecraft.world.level.Level level) {
        if (this.blockEntity != null) {
            return this.blockEntity; // Server side
        } else {
            // Client side - use synced position only
            BlockPos syncedPos = getSyncedBlockPos();
            if (!syncedPos.equals(BlockPos.ZERO)) {
                net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(syncedPos);
                if (be instanceof DeepDrawerExtractorBlockEntity extractorEntity) {
                    return extractorEntity;
                }
            }
            
            // Search disabled - return null if synced position doesn't work
            return null;
        }
    }
    
    /**
     * Calculates a hash of the filter fields list
     */
    private static int calculateFilterHash(java.util.List<String> filterFields, java.util.List<Integer> concatChannels) {
        if (filterFields == null) {
            return 0;
        }
        StringBuilder sb = new StringBuilder();
        for (String field : filterFields) {
            sb.append(field != null ? field : "");
            sb.append("|");
        }
        if (concatChannels != null) {
            for (Integer ch : concatChannels) {
                sb.append(ch != null ? ch : 0);
                sb.append(",");
            }
        }
        return sb.toString().hashCode();
    }
    
    /**
     * Gets the filter hash from synced data
     */
    public int getFilterHash() {
        return this.containerData.get(FILTER_HASH_INDEX);
    }
    
    /**
     * Gets the whitelist mode from synced data
     */
    public boolean getWhitelistMode() {
        return this.containerData.get(WHITELIST_MODE_INDEX) != 0;
    }
    
    /** 0 = allow list panel, 1 = deny list panel. */
    public int getLastFilterPanel() {
        return this.containerData.get(LAST_FILTER_PANEL_INDEX);
    }
    
    /**
     * Gets the inverted filter hash from synced data
     */
    public int getInvertedFilterHash() {
        return this.containerData.get(INVERTED_FILTER_HASH_INDEX);
    }
    
    /**
     * Updates cached filter data when hash changes (client-side)
     */
    public void updateCachedFilters() {
        if (this.blockEntity != null) {
            // Server side - directly get the filters
            this.cachedFilterFields = new java.util.ArrayList<>(this.blockEntity.getFilterFields());
            this.cachedInvertedFilterFields = new java.util.ArrayList<>(this.blockEntity.getInvertedFilterFields());
            this.cachedAllowConcatChannels = new java.util.ArrayList<>(this.blockEntity.getAllowConcatChannels());
            this.cachedDenyConcatChannels = new java.util.ArrayList<>(this.blockEntity.getDenyConcatChannels());
            this.cachedWhitelistMode = this.blockEntity.isWhitelistMode();
            this.lastSyncedFilterHash = calculateFilterHash(this.cachedFilterFields, this.cachedAllowConcatChannels);
            this.lastSyncedInvertedFilterHash = calculateFilterHash(this.cachedInvertedFilterFields, this.cachedDenyConcatChannels);
        } else {
            // Client side - check if hash changed and try to get filters from block entity using synced position
            int currentHash = getFilterHash();
            int currentInvertedHash = getInvertedFilterHash();
            boolean hashChanged = (currentHash != this.lastSyncedFilterHash || this.lastSyncedFilterHash == 0);
            boolean invertedHashChanged = (currentInvertedHash != this.lastSyncedInvertedFilterHash || this.lastSyncedInvertedFilterHash == 0);
            
            if (hashChanged || invertedHashChanged) {
                // Hash changed OR first time
                if (hashChanged) {
                    this.lastSyncedFilterHash = currentHash;
                }
                if (invertedHashChanged) {
                    this.lastSyncedInvertedFilterHash = currentInvertedHash;
                }
                
                // Try to find the block entity using synced position only (search disabled)
                net.minecraft.world.level.Level clientLevel = net.unfamily.iskautils.util.ClientRuntimeAccess.getClientLevel();
                if (clientLevel != null) {
                    DeepDrawerExtractorBlockEntity be = getBlockEntityFromLevel(clientLevel);
                    if (be != null) {
                        this.cachedFilterFields = new java.util.ArrayList<>(be.getFilterFields());
                        this.cachedInvertedFilterFields = new java.util.ArrayList<>(be.getInvertedFilterFields());
                        this.cachedAllowConcatChannels = new java.util.ArrayList<>(be.getAllowConcatChannels());
                        this.cachedDenyConcatChannels = new java.util.ArrayList<>(be.getDenyConcatChannels());
                        this.cachedWhitelistMode = be.isWhitelistMode();
                    } else {
                        // If we can't find the block entity, try ContainerData if position is synced
                        BlockPos syncedPos = getSyncedBlockPos();
                        if (!syncedPos.equals(BlockPos.ZERO)) {
                            // Position is synced, ContainerData should be valid
                            this.cachedWhitelistMode = getWhitelistMode();
                            // Keep existing filter fields (don't clear them)
                        }
                        // If position not synced, keep existing cache (don't change it)
                    }
                }
            }
        }
    }
    
    /**
     * Gets the cached filter fields (works on both client and server)
     */
    public java.util.List<String> getCachedFilterFields() {
        return new java.util.ArrayList<>(cachedFilterFields); // Return copy
    }
    
    /**
     * Gets the cached inverted filter fields (works on both client and server)
     */
    public java.util.List<String> getCachedInvertedFilterFields() {
        return new java.util.ArrayList<>(cachedInvertedFilterFields); // Return copy
    }
    
    public java.util.List<Integer> getCachedAllowConcatChannels() {
        return new java.util.ArrayList<>(cachedAllowConcatChannels);
    }

    public java.util.List<Integer> getCachedDenyConcatChannels() {
        return new java.util.ArrayList<>(cachedDenyConcatChannels);
    }

    /**
     * Gets the cached whitelist mode (works on both client and server)
     */
    public boolean getCachedWhitelistMode() {
        return cachedWhitelistMode;
    }
    
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        
        // Server side: update cached filters and sync to client
        if (this.blockEntity != null) {
            java.util.List<String> currentFilters = this.blockEntity.getFilterFields();
            java.util.List<String> currentInvertedFilters = this.blockEntity.getInvertedFilterFields();
            java.util.List<Integer> currentAllowConcat = this.blockEntity.getAllowConcatChannels();
            java.util.List<Integer> currentDenyConcat = this.blockEntity.getDenyConcatChannels();
            int currentHash = calculateFilterHash(currentFilters, currentAllowConcat);
            int currentInvertedHash = calculateFilterHash(currentInvertedFilters, currentDenyConcat);

            if (currentHash != this.lastSyncedFilterHash) {
                this.cachedFilterFields = new java.util.ArrayList<>(currentFilters);
                this.cachedAllowConcatChannels = new java.util.ArrayList<>(currentAllowConcat);
                this.lastSyncedFilterHash = currentHash;
            }
            if (currentInvertedHash != this.lastSyncedInvertedFilterHash) {
                this.cachedInvertedFilterFields = new java.util.ArrayList<>(currentInvertedFilters);
                this.cachedDenyConcatChannels = new java.util.ArrayList<>(currentDenyConcat);
                this.lastSyncedInvertedFilterHash = currentInvertedHash;
            }
            this.cachedWhitelistMode = this.blockEntity.isWhitelistMode();
        }
    }
}
