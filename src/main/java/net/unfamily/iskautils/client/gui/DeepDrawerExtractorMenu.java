package net.unfamily.iskautils.client.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
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
    private static final int WHITELIST_MODE_INDEX = 5; // 0 = blacklist, 1 = whitelist
    private static final int DATA_SIZE = 6;
    
    // Cached filter data (client-side)
    private String[] cachedFilterFields = new String[11];
    private boolean cachedWhitelistMode = false;
    private int lastSyncedFilterHash = 0;
    
    // Server-side constructor
    public DeepDrawerExtractorMenu(int containerId, Inventory playerInventory, DeepDrawerExtractorBlockEntity blockEntity) {
        super(ModMenuTypes.DEEP_DRAWER_EXTRACTOR_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        
        // Initialize cached data on server side
        this.cachedFilterFields = blockEntity.getFilterFields();
        this.cachedWhitelistMode = blockEntity.isWhitelistMode();
        this.lastSyncedFilterHash = calculateFilterHash(this.cachedFilterFields);
        
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
    }

    // Client-side constructor (NeoForge factory)
    public DeepDrawerExtractorMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.DEEP_DRAWER_EXTRACTOR_MENU.get(), containerId);
        // Client-side: we don't have direct access to the BlockEntity
        this.blockEntity = null;
        this.blockPos = BlockPos.ZERO;
        this.levelAccess = ContainerLevelAccess.NULL;
        this.containerData = new SimpleContainerData(DATA_SIZE);
        this.cachedFilterFields = new String[11];
        // Initialize cache from ContainerData if available (will be updated in updateCachedFilters)
        this.cachedWhitelistMode = false; // Will be updated when ContainerData syncs
        this.lastSyncedFilterHash = 0;
        addDataSlots(this.containerData);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.DEEP_DRAWER_EXTRACTOR.get());
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        // No slots, so quickMoveStack doesn't need to be implemented
        return net.minecraft.world.item.ItemStack.EMPTY;
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
     * Calculates a hash of the filter fields array
     */
    private static int calculateFilterHash(String[] filterFields) {
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
     * Updates cached filter data when hash changes (client-side)
     */
    public void updateCachedFilters() {
        if (this.blockEntity != null) {
            // Server side - directly get the filters
            this.cachedFilterFields = this.blockEntity.getFilterFields();
            this.cachedWhitelistMode = this.blockEntity.isWhitelistMode();
            this.lastSyncedFilterHash = calculateFilterHash(this.cachedFilterFields);
        } else {
            // Client side - check if hash changed and try to get filters from block entity using synced position
            int currentHash = getFilterHash();
            if (currentHash != this.lastSyncedFilterHash || this.lastSyncedFilterHash == 0) {
                // Hash changed OR first time (lastSyncedFilterHash == 0 means not initialized yet)
                this.lastSyncedFilterHash = currentHash;
                
                // Try to find the block entity using synced position only (search disabled)
                if (net.minecraft.client.Minecraft.getInstance().level != null) {
                    DeepDrawerExtractorBlockEntity be = getBlockEntityFromLevel(net.minecraft.client.Minecraft.getInstance().level);
                    if (be != null) {
                        this.cachedFilterFields = be.getFilterFields();
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
    public String[] getCachedFilterFields() {
        return cachedFilterFields;
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
            String[] currentFilters = this.blockEntity.getFilterFields();
            int currentHash = calculateFilterHash(currentFilters);
            
            // If filters changed, update cache
            if (currentHash != this.lastSyncedFilterHash) {
                this.cachedFilterFields = currentFilters;
                this.cachedWhitelistMode = this.blockEntity.isWhitelistMode();
                this.lastSyncedFilterHash = currentHash;
            }
        }
    }
}
