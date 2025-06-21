package net.unfamily.iskautils.client.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.StructureSaverMachineBlockEntity;

public class StructureSaverMachineMenu extends AbstractContainerMenu {
    private final StructureSaverMachineBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final ContainerData containerData;
    private final BlockPos blockPos;
    
    // 18 slot display (2 righe x 9 slot) - rimossa la seconda riga
    private static final int DISPLAY_SLOTS = 18;
    
    // ContainerData indices for syncing (seguendo il pattern del Structure Placer Machine)
    private static final int VERTEX1_X_INDEX = 0;
    private static final int VERTEX1_Y_INDEX = 1;
    private static final int VERTEX1_Z_INDEX = 2;
    private static final int VERTEX2_X_INDEX = 3;
    private static final int VERTEX2_Y_INDEX = 4;
    private static final int VERTEX2_Z_INDEX = 5;
    private static final int CENTER_X_INDEX = 6;
    private static final int CENTER_Y_INDEX = 7;
    private static final int CENTER_Z_INDEX = 8;
    private static final int HAS_VALID_AREA_INDEX = 9;
    private static final int IS_WORKING_INDEX = 10;
    private static final int WORK_PROGRESS_INDEX = 11;
    private static final int MACHINE_POS_X_INDEX = 12;
    private static final int MACHINE_POS_Y_INDEX = 13;
    private static final int MACHINE_POS_Z_INDEX = 14;
    private static final int DATA_COUNT = 15;
    
    public StructureSaverMachineMenu(int containerId, Inventory playerInventory, StructureSaverMachineBlockEntity blockEntity) {
        super(ModMenuTypes.STRUCTURE_SAVER_MACHINE_MENU.get(), containerId);
        
        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        
        // Create container data that syncs with the block entity
        this.containerData = new ContainerData() {
            @Override
            public int get(int index) {
                return switch(index) {
                    case VERTEX1_X_INDEX -> blockEntity.getBlueprintVertex1() != null ? blockEntity.getBlueprintVertex1().getX() : 0;
                    case VERTEX1_Y_INDEX -> blockEntity.getBlueprintVertex1() != null ? blockEntity.getBlueprintVertex1().getY() : 0;
                    case VERTEX1_Z_INDEX -> blockEntity.getBlueprintVertex1() != null ? blockEntity.getBlueprintVertex1().getZ() : 0;
                    case VERTEX2_X_INDEX -> blockEntity.getBlueprintVertex2() != null ? blockEntity.getBlueprintVertex2().getX() : 0;
                    case VERTEX2_Y_INDEX -> blockEntity.getBlueprintVertex2() != null ? blockEntity.getBlueprintVertex2().getY() : 0;
                    case VERTEX2_Z_INDEX -> blockEntity.getBlueprintVertex2() != null ? blockEntity.getBlueprintVertex2().getZ() : 0;
                    case CENTER_X_INDEX -> blockEntity.getBlueprintCenter() != null ? blockEntity.getBlueprintCenter().getX() : 0;
                    case CENTER_Y_INDEX -> blockEntity.getBlueprintCenter() != null ? blockEntity.getBlueprintCenter().getY() : 0;
                    case CENTER_Z_INDEX -> blockEntity.getBlueprintCenter() != null ? blockEntity.getBlueprintCenter().getZ() : 0;
                    case HAS_VALID_AREA_INDEX -> blockEntity.hasValidArea() ? 1 : 0;
                    case IS_WORKING_INDEX -> blockEntity.isWorking() ? 1 : 0;
                    case WORK_PROGRESS_INDEX -> blockEntity.getWorkProgress();
                    case MACHINE_POS_X_INDEX -> blockPos.getX();
                    case MACHINE_POS_Y_INDEX -> blockPos.getY();
                    case MACHINE_POS_Z_INDEX -> blockPos.getZ();
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
        
        addDisplaySlots();
    }
    
    // Costruttore client-side
    public StructureSaverMachineMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, BlockPos.ZERO);
    }
    
    // Aggiunto: Costruttore client-side con posizione (per gestione packet)
    public StructureSaverMachineMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModMenuTypes.STRUCTURE_SAVER_MACHINE_MENU.get(), containerId);
        
        this.blockEntity = null;
        this.blockPos = pos;
        this.levelAccess = ContainerLevelAccess.NULL;
        
        // Create dummy container data for client
        this.containerData = new SimpleContainerData(DATA_COUNT);
        this.addDataSlots(this.containerData);
        
        addDisplaySlots();
    }
    
    /**
     * Aggiunge i 18 slot display (2 righe x 9 slot) a partire da X=8 Y=188 (spostato +46px per la nuova texture 176x246)
     */
    private void addDisplaySlots() {
        IItemHandler itemHandler;
        
        if (blockEntity != null) {
            itemHandler = blockEntity.getItemHandler();
        } else {
            // Client-side fallback
            itemHandler = new ItemStackHandler(DISPLAY_SLOTS);
        }
        
        // 2 rows of 9 slots with the second row moved 7 slots higher
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = row * 9 + col;
                int xPos = 8 + col * 18; // GUI coordinate 7 + 1 (moved +1 to the left)
                
                // Calculate yPos:
                // Row 0: 186 (first row)
                // Row 1: 215 (second row, specific position requested)
                int yPos = switch (row) {
                    case 0 -> 186; // First row: 186
                    case 1 -> 215; // Second row: 215 (specific position requested)
                    default -> 188 + row * 18;
                };
                
                // Display-only slots (non-interactive)
                addSlot(new SlotItemHandler(itemHandler, slotIndex, xPos, yPos) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false; // Don't allow manual insertion
                    }
                    
                    @Override
                    public boolean mayPickup(Player player) {
                        return false; // Don't allow extraction
                    }
                    
                    @Override
                    public ItemStack remove(int amount) {
                        return ItemStack.EMPTY; // Don't allow removal
                    }
                });
            }
        }
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Tutti gli slot sono display-only, non permettere shift-click
        return ItemStack.EMPTY;
    }
    
    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.STRUCTURE_SAVER_MACHINE.get());
    }
    
    // Methods to access synced data (works on both client and server)
    public BlockPos getSyncedVertex1() {
        int x = this.containerData.get(VERTEX1_X_INDEX);
        int y = this.containerData.get(VERTEX1_Y_INDEX);
        int z = this.containerData.get(VERTEX1_Z_INDEX);
        if (x == 0 && y == 0 && z == 0) {
            return null; // No vertex1 set
        }
        return new BlockPos(x, y, z);
    }
    
    public BlockPos getSyncedVertex2() {
        int x = this.containerData.get(VERTEX2_X_INDEX);
        int y = this.containerData.get(VERTEX2_Y_INDEX);
        int z = this.containerData.get(VERTEX2_Z_INDEX);
        if (x == 0 && y == 0 && z == 0) {
            return null; // No vertex2 set
        }
        return new BlockPos(x, y, z);
    }
    
    public BlockPos getSyncedCenter() {
        int x = this.containerData.get(CENTER_X_INDEX);
        int y = this.containerData.get(CENTER_Y_INDEX);
        int z = this.containerData.get(CENTER_Z_INDEX);
        if (x == 0 && y == 0 && z == 0) {
            return null; // No center set
        }
        return new BlockPos(x, y, z);
    }
    
    public boolean getSyncedHasValidArea() {
        return this.containerData.get(HAS_VALID_AREA_INDEX) != 0;
    }
    
    public boolean getSyncedIsWorking() {
        return this.containerData.get(IS_WORKING_INDEX) != 0;
    }
    
    public int getSyncedWorkProgress() {
        return this.containerData.get(WORK_PROGRESS_INDEX);
    }
    
    /**
     * Forces blueprint data refresh (no longer necessary with ContainerData)
     */
    public void forceRefreshBlueprintData() {
        // With ContainerData, synchronization is automatic
    }
    
    public StructureSaverMachineBlockEntity getBlockEntity() {
        return blockEntity;
    }
    
    // Aggiunto: Metodi per ottenere la posizione della macchina (seguendo il pattern Structure Placer Machine)
    public BlockPos getSyncedBlockPos() {
        // Get position from synced data if available, otherwise use stored position
        if (this.blockEntity != null) {
            return this.blockPos; // Server side
        } else {
            // Client side - get from synced data
            int x = this.containerData.get(MACHINE_POS_X_INDEX);
            int y = this.containerData.get(MACHINE_POS_Y_INDEX);
            int z = this.containerData.get(MACHINE_POS_Z_INDEX);
            if (x == 0 && y == 0 && z == 0) {
                return this.blockPos; // Fallback to stored position
            }
            return new BlockPos(x, y, z);
        }
    }
    
    public BlockPos getBlockPos() {
        return this.blockPos;
    }
} 