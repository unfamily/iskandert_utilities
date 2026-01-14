package net.unfamily.iskautils.client.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.TemporalOverclockerBlockEntity;

/**
 * Menu for the Temporal Overclocker
 */
public class TemporalOverclockerMenu extends AbstractContainerMenu {
    private final TemporalOverclockerBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final ContainerData containerData;
    private final BlockPos blockPos;
    
    // ContainerData indices for syncing
    private static final int ENERGY_INDEX = 0;
    private static final int MAX_ENERGY_INDEX = 1;
    private static final int LINKED_BLOCKS_COUNT_INDEX = 2;
    private static final int REDSTONE_MODE_INDEX = 3;
    private static final int ACCELERATION_FACTOR_INDEX = 4;
    private static final int BLOCK_POS_X_INDEX = 5;
    private static final int BLOCK_POS_Y_INDEX = 6;
    private static final int BLOCK_POS_Z_INDEX = 7;
    private static final int LINKED_BLOCKS_HASH_INDEX = 8;
    private static final int PERSISTENT_MODE_INDEX = 9;
    private static final int DATA_COUNT = 10;
    
    // Constructor for server-side (with block entity)
    public TemporalOverclockerMenu(int containerId, Inventory playerInventory, TemporalOverclockerBlockEntity blockEntity) {
        super(ModMenuTypes.TEMPORAL_OVERCLOCKER_MENU.get(), containerId);
        
        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        
        // Create container data that syncs with the block entity
        this.containerData = new ContainerData() {
            @Override
            public int get(int index) {
                return switch(index) {
                    case ENERGY_INDEX -> blockEntity.getEnergyStorage().getEnergyStored();
                    case MAX_ENERGY_INDEX -> blockEntity.getEnergyStorage().getMaxEnergyStored();
                    case LINKED_BLOCKS_COUNT_INDEX -> blockEntity.getLinkedBlocks().size();
                    case REDSTONE_MODE_INDEX -> blockEntity.getRedstoneMode();
                    case ACCELERATION_FACTOR_INDEX -> blockEntity.getAccelerationFactor();
                    case BLOCK_POS_X_INDEX -> blockPos.getX();
                    case BLOCK_POS_Y_INDEX -> blockPos.getY();
                    case BLOCK_POS_Z_INDEX -> blockPos.getZ();
                    case LINKED_BLOCKS_HASH_INDEX -> calculateLinkedBlocksHash(blockEntity.getLinkedBlocks());
                    case PERSISTENT_MODE_INDEX -> blockEntity.isPersistentMode() ? 1 : 0;
                    default -> 0;
                };
            }
            
            @Override
            public void set(int index, int value) {
                // Values are read-only from client side
            }
            
            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
        
        this.addDataSlots(this.containerData);
    }
    
    // Constructor for client-side
    public TemporalOverclockerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, BlockPos.ZERO);
    }
    
    // Constructor for client-side with position
    public TemporalOverclockerMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModMenuTypes.TEMPORAL_OVERCLOCKER_MENU.get(), containerId);
        
        this.blockEntity = null;
        this.blockPos = pos;
        this.levelAccess = ContainerLevelAccess.NULL;
        
        // Create dummy container data for client
        this.containerData = new SimpleContainerData(DATA_COUNT);
        this.addDataSlots(this.containerData);
    }
    
    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.TEMPORAL_OVERCLOCKER.get());
    }
    
    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        // No slots, so quickMoveStack doesn't need to be implemented
        return net.minecraft.world.item.ItemStack.EMPTY;
    }
    
    public TemporalOverclockerBlockEntity getBlockEntity() {
        return blockEntity;
    }
    
    // Methods to access synced data
    public int getEnergyStored() {
        return this.containerData.get(ENERGY_INDEX);
    }
    
    public int getMaxEnergyStored() {
        return this.containerData.get(MAX_ENERGY_INDEX);
    }
    
    public int getLinkedBlocksCount() {
        return this.containerData.get(LINKED_BLOCKS_COUNT_INDEX);
    }
    
    public int getRedstoneMode() {
        return this.containerData.get(REDSTONE_MODE_INDEX);
    }
    
    public int getAccelerationFactor() {
        return this.containerData.get(ACCELERATION_FACTOR_INDEX);
    }
    
    public boolean isPersistentMode() {
        return this.containerData.get(PERSISTENT_MODE_INDEX) != 0;
    }
    
    public BlockPos getSyncedBlockPos() {
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
    
    public BlockPos getBlockPos() {
        return this.blockPos;
    }
    
    /**
     * Calculates a hash of the linked blocks list
     */
    private static int calculateLinkedBlocksHash(java.util.List<BlockPos> linkedBlocks) {
        int hash = 0;
        for (BlockPos pos : linkedBlocks) {
            hash = hash * 31 + pos.hashCode();
        }
        return hash;
    }
    
    public int getLinkedBlocksHash() {
        return this.containerData.get(LINKED_BLOCKS_HASH_INDEX);
    }
    
    // Cached linked blocks data (client-side)
    private java.util.List<BlockPos> cachedLinkedBlocks = new java.util.ArrayList<>();
    private int lastSyncedLinkedBlocksHash = 0;
    
    /**
     * Updates cached linked blocks data when hash changes (client-side)
     */
    public void updateCachedLinkedBlocks() {
        if (this.blockEntity != null) {
            // Server side - directly get the linked blocks
            this.cachedLinkedBlocks = new java.util.ArrayList<>(this.blockEntity.getLinkedBlocks());
            this.lastSyncedLinkedBlocksHash = calculateLinkedBlocksHash(this.cachedLinkedBlocks);
        } else {
            // Client side - check if hash changed and try to get linked blocks from block entity using synced position
            int currentHash = getLinkedBlocksHash();
            
            // Always try to load blocks if hash is 0 (first time) OR if hash changed
            // Also try to load if cachedLinkedBlocks is empty but count > 0
            boolean shouldUpdate = (currentHash != this.lastSyncedLinkedBlocksHash || this.lastSyncedLinkedBlocksHash == 0) ||
                                  (this.cachedLinkedBlocks.isEmpty() && getLinkedBlocksCount() > 0);
            
            if (shouldUpdate) {
                // Hash changed OR first time (lastSyncedLinkedBlocksHash == 0 means not initialized yet)
                this.lastSyncedLinkedBlocksHash = currentHash;
                
                // Try to find the block entity using synced position only
                if (net.minecraft.client.Minecraft.getInstance().level != null) {
                    TemporalOverclockerBlockEntity be = getBlockEntityFromLevel(net.minecraft.client.Minecraft.getInstance().level);
                    if (be != null) {
                        this.cachedLinkedBlocks = new java.util.ArrayList<>(be.getLinkedBlocks());
                    } else {
                        // If we can't find the block entity, keep existing linked blocks (don't clear them)
                        // This prevents clearing the list when the block entity is temporarily unavailable
                    }
                }
            }
        }
    }
    
    /**
     * Gets the cached linked blocks (works on both client and server)
     */
    public java.util.List<BlockPos> getCachedLinkedBlocks() {
        return new java.util.ArrayList<>(cachedLinkedBlocks); // Return copy
    }
    
    /**
     * Method to get block entity from level (for client-side actions)
     * Uses synced position only
     */
    public TemporalOverclockerBlockEntity getBlockEntityFromLevel(Level level) {
        if (this.blockEntity != null) {
            return this.blockEntity; // Server side
        } else {
            // Client side - use synced position
            BlockPos syncedPos = getSyncedBlockPos();
            if (!syncedPos.equals(BlockPos.ZERO)) {
                BlockEntity be = level.getBlockEntity(syncedPos);
                if (be instanceof TemporalOverclockerBlockEntity overclockerEntity) {
                    return overclockerEntity;
                }
            }
            return null;
        }
    }
}

