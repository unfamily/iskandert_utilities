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
    
    // 27 slot display (3 righe x 9 slot)
    private static final int DISPLAY_SLOTS = 27;
    
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
    private static final int DATA_COUNT = 12;
    
    public StructureSaverMachineMenu(int containerId, Inventory playerInventory, StructureSaverMachineBlockEntity blockEntity) {
        super(ModMenuTypes.STRUCTURE_SAVER_MACHINE_MENU.get(), containerId);
        
        this.blockEntity = blockEntity;
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
        super(ModMenuTypes.STRUCTURE_SAVER_MACHINE_MENU.get(), containerId);
        
        this.blockEntity = null;
        this.levelAccess = ContainerLevelAccess.NULL;
        
        // Create dummy container data for client
        this.containerData = new SimpleContainerData(DATA_COUNT);
        this.addDataSlots(this.containerData);
        
        addDisplaySlots();
    }
    
    /**
     * Aggiunge i 27 slot display (3 righe x 9 slot) a partire da X=8 Y=188 (spostato +46px per la nuova texture 176x246)
     */
    private void addDisplaySlots() {
        IItemHandler itemHandler;
        
        if (blockEntity != null) {
            itemHandler = blockEntity.getItemHandler();
        } else {
            // Client-side fallback
            itemHandler = new ItemStackHandler(DISPLAY_SLOTS);
        }
        
        // 3 righe di 9 slot con aggiustamenti verticali
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = row * 9 + col;
                int xPos = 8 + col * 18; // GUI coordinate 7 + 1 (spostato +1 a sinistra)
                
                // Calcola yPos con aggiustamenti per riga (spostato più in basso per la nuova texture):
                // Riga 0: normale (188) - spostato +46 pixel più in basso (142 + 46)
                // Riga 1: -1 pixel (205 invece di 206)
                // Riga 2: -2 pixel (222 invece di 224)
                int baseY = 188;
                int yPos = switch (row) {
                    case 0 -> baseY; // Prima riga: normale
                    case 1 -> baseY + 18 - 1; // Seconda riga: -1 pixel
                    case 2 -> baseY + 36 - 2; // Terza riga: -2 pixel
                    default -> baseY + row * 18;
                };
                
                addSlot(new SlotItemHandler(itemHandler, slotIndex, xPos, yPos));
            }
        }
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        
        if (slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();
            
            // Tutti gli slot sono display slots, non c'è movimento tra inventari
            slot.setChanged();
        }
        
        return newStack;
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
     * Forza il refresh dei dati blueprint (ora non più necessario con ContainerData)
     */
    public void forceRefreshBlueprintData() {
        // Con ContainerData, la sincronizzazione è automatica
        System.out.println("DEBUG MENU: ContainerData sync is automatic, no manual refresh needed");
    }
    
    public StructureSaverMachineBlockEntity getBlockEntity() {
        return blockEntity;
    }
} 