package net.unfamily.iskautils.client.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.StructurePlacerMachineBlockEntity;

/**
 * Menu per la GUI di selezione strutture (senza slot, solo per accesso al BlockEntity)
 */
public class StructureSelectionMenu extends AbstractContainerMenu {
    private final StructurePlacerMachineBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final BlockPos blockPos;
    
    // Constructor for server-side (with block entity)
    public StructureSelectionMenu(int containerId, Inventory playerInventory, StructurePlacerMachineBlockEntity blockEntity) {
        super(ModMenuTypes.STRUCTURE_SELECTION_MENU.get(), containerId);
        
        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
    }
    
    // Constructor for client-side (simplified)
    public StructureSelectionMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.STRUCTURE_SELECTION_MENU.get(), containerId);
        
        this.blockEntity = null;
        this.blockPos = BlockPos.ZERO;
        this.levelAccess = ContainerLevelAccess.NULL;
    }
    
    // Constructor for client-side with block position
    public StructureSelectionMenu(int containerId, Inventory playerInventory, BlockPos machinePos) {
        super(ModMenuTypes.STRUCTURE_SELECTION_MENU.get(), containerId);
        
        this.blockEntity = null;
        this.blockPos = machinePos; // Store the machine position for client-side lookup
        this.levelAccess = ContainerLevelAccess.NULL;
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // No slots, so no shift-click behavior
        return ItemStack.EMPTY;
    }
    
    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.STRUCTURE_PLACER_MACHINE.get());
    }
    
    public StructurePlacerMachineBlockEntity getBlockEntity() {
        return blockEntity;
    }
    
    public BlockPos getBlockPos() {
        return this.blockPos;
    }
    
    // Method to get block entity from level (for client-side actions)
    public StructurePlacerMachineBlockEntity getBlockEntityFromLevel(Level level) {
        if (this.blockEntity != null) {
            return this.blockEntity; // Server side
        } else {
            // Client side
            if (level.isClientSide) {
                // First try the saved block position
                if (!this.blockPos.equals(BlockPos.ZERO)) {
                    BlockEntity be = level.getBlockEntity(this.blockPos);
                    if (be instanceof StructurePlacerMachineBlockEntity machineEntity) {
                        return machineEntity;
                    }
                }
                
                // Fallback: search near player if saved position doesn't work
                if (net.minecraft.client.Minecraft.getInstance().player != null) {
                    BlockPos playerPos = net.minecraft.client.Minecraft.getInstance().player.blockPosition();
                    
                    // Search in a 16x16x16 area around player for the machine
                    for (int x = -8; x <= 8; x++) {
                        for (int y = -8; y <= 8; y++) {
                            for (int z = -8; z <= 8; z++) {
                                BlockPos searchPos = playerPos.offset(x, y, z);
                                BlockEntity be = level.getBlockEntity(searchPos);
                                if (be instanceof StructurePlacerMachineBlockEntity machineEntity) {
                                    return machineEntity;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
} 