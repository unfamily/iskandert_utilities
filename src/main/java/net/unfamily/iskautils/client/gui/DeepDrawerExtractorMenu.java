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

/**
 * Menu for Deep Drawer Extractor GUI
 */
public class DeepDrawerExtractorMenu extends AbstractContainerMenu {
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
    private static final int DATA_SIZE = 7;
    
    // Cached filter data (client-side) - now using dynamic list
    private java.util.List<String> cachedFilterFields = new java.util.ArrayList<>();
    private java.util.List<String> cachedInvertedFilterFields = new java.util.ArrayList<>();
    private boolean cachedWhitelistMode = false;
    private int lastSyncedFilterHash = 0;
    private int lastSyncedInvertedFilterHash = 0;
    
    // Server-side constructor
    public DeepDrawerExtractorMenu(int containerId, Inventory playerInventory, DeepDrawerExtractorBlockEntity blockEntity) {
        super(ModMenuTypes.DEEP_DRAWER_EXTRACTOR_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        
        // Initialize cached data on server side
        this.cachedFilterFields = new java.util.ArrayList<>(blockEntity.getFilterFields());
        this.cachedInvertedFilterFields = new java.util.ArrayList<>(blockEntity.getInvertedFilterFields());
        this.cachedWhitelistMode = blockEntity.isWhitelistMode();
        this.lastSyncedFilterHash = calculateFilterHash(this.cachedFilterFields);
        this.lastSyncedInvertedFilterHash = calculateFilterHash(this.cachedInvertedFilterFields);
        
        // Create ContainerData that syncs redstone mode, block position, filter hash, and whitelist mode
        this.containerData = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case REDSTONE_MODE_INDEX -> blockEntity.getRedstoneMode();
                    case BLOCK_POS_X_INDEX -> blockPos.getX();
                    case BLOCK_POS_Y_INDEX -> blockPos.getY();
                    case BLOCK_POS_Z_INDEX -> blockPos.getZ();
                    case FILTER_HASH_INDEX -> calculateFilterHash(blockEntity.getFilterFields());
                    case INVERTED_FILTER_HASH_INDEX -> calculateFilterHash(blockEntity.getInvertedFilterFields());
                    case WHITELIST_MODE_INDEX -> blockEntity.isWhitelistMode() ? 1 : 0;
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
        
        // Add buffer slots (5 slots in a row)
        addBufferSlots();
        
        // Add player inventory and hotbar
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    // Client-side constructor (NeoForge factory)
    public DeepDrawerExtractorMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.DEEP_DRAWER_EXTRACTOR_MENU.get(), containerId);
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
        
        // 5 slots in a row at position 195, 134 (2px right from previous)
        for (int i = 0; i < 5; i++) {
            int xPos = 195 + i * 18; // 18 pixels per slot
            int yPos = 134;
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
        // Basic quick move implementation
        net.minecraft.world.item.ItemStack itemstack = net.minecraft.world.item.ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            net.minecraft.world.item.ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            
            // Buffer slots are 0-4, player inventory starts at 5
            if (index < 5) {
                // Moving from buffer to player inventory
                if (!this.moveItemStackTo(stack, 5, this.slots.size(), true)) {
                    return net.minecraft.world.item.ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to buffer
                if (!this.moveItemStackTo(stack, 0, 5, false)) {
                    return net.minecraft.world.item.ItemStack.EMPTY;
                }
            }
            
            if (stack.isEmpty()) {
                slot.setByPlayer(net.minecraft.world.item.ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        
        return itemstack;
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
    private static int calculateFilterHash(java.util.List<String> filterFields) {
        if (filterFields == null) {
            return 0;
        }
        StringBuilder sb = new StringBuilder();
        for (String field : filterFields) {
            sb.append(field != null ? field : "");
            sb.append("|");
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
            this.cachedWhitelistMode = this.blockEntity.isWhitelistMode();
            this.lastSyncedFilterHash = calculateFilterHash(this.cachedFilterFields);
            this.lastSyncedInvertedFilterHash = calculateFilterHash(this.cachedInvertedFilterFields);
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
                if (net.minecraft.client.Minecraft.getInstance().level != null) {
                    DeepDrawerExtractorBlockEntity be = getBlockEntityFromLevel(net.minecraft.client.Minecraft.getInstance().level);
                    if (be != null) {
                        this.cachedFilterFields = new java.util.ArrayList<>(be.getFilterFields());
                        this.cachedInvertedFilterFields = new java.util.ArrayList<>(be.getInvertedFilterFields());
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
            int currentHash = calculateFilterHash(currentFilters);
            int currentInvertedHash = calculateFilterHash(currentInvertedFilters);
            
            // If filters changed, update cache
            if (currentHash != this.lastSyncedFilterHash) {
                this.cachedFilterFields = new java.util.ArrayList<>(currentFilters);
                this.lastSyncedFilterHash = currentHash;
            }
            if (currentInvertedHash != this.lastSyncedInvertedFilterHash) {
                this.cachedInvertedFilterFields = new java.util.ArrayList<>(currentInvertedFilters);
                this.lastSyncedInvertedFilterHash = currentInvertedHash;
            }
            this.cachedWhitelistMode = this.blockEntity.isWhitelistMode();
        }
    }
}
