package net.unfamily.iskautils.client.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.SmartTimerBlockEntity;

public class SmartTimerMenu extends AbstractContainerMenu {
    private final SmartTimerBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final BlockPos blockPos;
    private final ContainerData containerData;
    
    // ContainerData indices for syncing
    private static final int COOLDOWN_TICKS_INDEX = 0;
    private static final int SIGNAL_DURATION_TICKS_INDEX = 1;
    private static final int BLOCK_POS_X_INDEX = 2;
    private static final int BLOCK_POS_Y_INDEX = 3;
    private static final int BLOCK_POS_Z_INDEX = 4;
    private static final int REDSTONE_MODE_INDEX = 5;
    // 5 facce relative (BACK, LEFT, RIGHT, UP, DOWN) invece di 6 direzioni assolute
    private static final int IO_CONFIG_BACK_INDEX = 6;
    private static final int IO_CONFIG_LEFT_INDEX = 7;
    private static final int IO_CONFIG_RIGHT_INDEX = 8;
    private static final int IO_CONFIG_UP_INDEX = 9;
    private static final int IO_CONFIG_DOWN_INDEX = 10;
    private static final int DATA_COUNT = 11;

    // Server-side constructor
    public SmartTimerMenu(int containerId, Inventory playerInventory, SmartTimerBlockEntity blockEntity) {
        super(ModMenuTypes.SMART_TIMER_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        
        // Create container data that syncs with the block entity
        this.containerData = new ContainerData() {
            @Override
            public int get(int index) {
                return switch(index) {
                    case COOLDOWN_TICKS_INDEX -> blockEntity.getCooldownTicks(); // Get directly in ticks
                    case SIGNAL_DURATION_TICKS_INDEX -> blockEntity.getSignalDurationTicks(); // Get directly in ticks
                    case BLOCK_POS_X_INDEX -> blockPos.getX();
                    case BLOCK_POS_Y_INDEX -> blockPos.getY();
                    case BLOCK_POS_Z_INDEX -> blockPos.getZ();
                    case REDSTONE_MODE_INDEX -> blockEntity.getRedstoneMode();
                    case IO_CONFIG_BACK_INDEX -> blockEntity.getIoConfig(SmartTimerBlockEntity.RelativeFace.BACK);
                    case IO_CONFIG_LEFT_INDEX -> blockEntity.getIoConfig(SmartTimerBlockEntity.RelativeFace.LEFT);
                    case IO_CONFIG_RIGHT_INDEX -> blockEntity.getIoConfig(SmartTimerBlockEntity.RelativeFace.RIGHT);
                    case IO_CONFIG_UP_INDEX -> blockEntity.getIoConfig(SmartTimerBlockEntity.RelativeFace.UP);
                    case IO_CONFIG_DOWN_INDEX -> blockEntity.getIoConfig(SmartTimerBlockEntity.RelativeFace.DOWN);
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
    }

    // Client-side constructor (NeoForge factory)
    public SmartTimerMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.SMART_TIMER_MENU.get(), containerId);
        // Client-side: we don't have direct access to the BlockEntity
        this.blockEntity = null;
        this.blockPos = BlockPos.ZERO;
        this.levelAccess = ContainerLevelAccess.NULL;
        
        // Create dummy container data for client
        this.containerData = new SimpleContainerData(DATA_COUNT);
        this.addDataSlots(this.containerData);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.SMART_TIMER.get());
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        // No slots, so quickMoveStack doesn't need to be implemented
        return net.minecraft.world.item.ItemStack.EMPTY;
    }

    public SmartTimerBlockEntity getBlockEntity() {
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
    
    public int getCooldownTicks() {
        return containerData.get(COOLDOWN_TICKS_INDEX);
    }
    
    public int getSignalDurationTicks() {
        return containerData.get(SIGNAL_DURATION_TICKS_INDEX);
    }
    
    public int getRedstoneMode() {
        return containerData.get(REDSTONE_MODE_INDEX);
    }
    
    /**
     * Ottiene il tipo I/O per una direzione assoluta del mondo (conversione automatica)
     * Converte la direzione assoluta in faccia relativa usando il facing del blocco
     */
    public byte getIoConfig(net.minecraft.core.Direction worldDirection) {
        // Se abbiamo accesso al BlockEntity, usiamo il metodo di conversione
        if (blockEntity != null) {
            return blockEntity.getIoConfig(worldDirection);
        }
        
        // Client-side: dobbiamo convertire usando il facing del blocco dal livello
        // Per semplicità, leggiamo direttamente dal BlockEntity se disponibile
        // Altrimenti, per ora restituiamo BLANK (la GUI dovrebbe accedere al livello)
        return 0; // BLANK - la GUI accederà direttamente al BlockEntity
    }
    
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        // ContainerData is automatically synced by Minecraft
    }
}
