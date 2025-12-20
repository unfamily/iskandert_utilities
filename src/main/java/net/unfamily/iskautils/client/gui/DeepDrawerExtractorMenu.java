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
    private static final int DATA_SIZE = 4;
    
    // Server-side constructor
    public DeepDrawerExtractorMenu(int containerId, Inventory playerInventory, DeepDrawerExtractorBlockEntity blockEntity) {
        super(ModMenuTypes.DEEP_DRAWER_EXTRACTOR_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        
        // Create ContainerData that syncs redstone mode and block position
        this.containerData = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case REDSTONE_MODE_INDEX -> blockEntity.getRedstoneMode();
                    case BLOCK_POS_X_INDEX -> blockPos.getX();
                    case BLOCK_POS_Y_INDEX -> blockPos.getY();
                    case BLOCK_POS_Z_INDEX -> blockPos.getZ();
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
     * This searches for the nearest Deep Drawer Extractor in render distance
     */
    public DeepDrawerExtractorBlockEntity getBlockEntityFromLevel(net.minecraft.world.level.Level level) {
        if (this.blockEntity != null) {
            return this.blockEntity; // Server side
        } else {
            // Client side - find the block entity by searching near player
            if (level.isClientSide && net.minecraft.client.Minecraft.getInstance().player != null) {
                net.minecraft.core.BlockPos playerPos = net.minecraft.client.Minecraft.getInstance().player.blockPosition();
                
                // Search in a 16x16x16 area around player for the machine
                for (int x = -8; x <= 8; x++) {
                    for (int y = -8; y <= 8; y++) {
                        for (int z = -8; z <= 8; z++) {
                            net.minecraft.core.BlockPos searchPos = playerPos.offset(x, y, z);
                            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(searchPos);
                            if (be instanceof DeepDrawerExtractorBlockEntity extractorEntity) {
                                return extractorEntity;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
    
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        // ContainerData is automatically synced by Minecraft
        // The get() method reads directly from blockEntity, so changes should be reflected immediately
    }
}
