package net.unfamily.iskautils.client.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
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
    
    // 27 slot display (3 righe x 9 slot)
    private static final int DISPLAY_SLOTS = 27;
    
    // Flags per tracciare quando i dati blueprint cambiano
    private boolean blueprintDataChanged = false;
    private net.minecraft.core.BlockPos lastVertex1 = null;
    private net.minecraft.core.BlockPos lastVertex2 = null;
    
    public StructureSaverMachineMenu(int containerId, Inventory playerInventory, StructureSaverMachineBlockEntity blockEntity) {
        super(ModMenuTypes.STRUCTURE_SAVER_MACHINE_MENU.get(), containerId);
        
        this.blockEntity = blockEntity;
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        
        addDisplaySlots();
    }
    
    // Costruttore client-side
    public StructureSaverMachineMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.STRUCTURE_SAVER_MACHINE_MENU.get(), containerId);
        
        this.blockEntity = null;
        this.levelAccess = ContainerLevelAccess.NULL;
        
        addDisplaySlots();
    }
    
    /**
     * Aggiunge i 27 slot display (3 righe x 9 slot) a partire da X=7 Y=145
     */
    private void addDisplaySlots() {
        IItemHandler itemHandler;
        
        if (blockEntity != null) {
            itemHandler = blockEntity.getItemHandler();
        } else {
            // Client-side fallback
            itemHandler = new ItemStackHandler(DISPLAY_SLOTS);
        }
        
        // 3 righe di 9 slot
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = row * 9 + col;
                int xPos = 8 + col * 18; // GUI coordinate 7 + 1 (spostato +1 a sinistra)
                int yPos = 142 + row * 18; // GUI coordinate 145 - 3 (spostato 1 pixel più in basso)
                
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
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, ModBlocks.STRUCTURE_SAVER_MACHINE.get());
    }
    
    /**
     * Controlla se i dati blueprint sono cambiati
     */
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        
        // Controlla se i dati blueprint sono cambiati (con null-safety)
        if (blockEntity != null) {
            var currentVertex1 = blockEntity.getBlueprintVertex1();
            var currentVertex2 = blockEntity.getBlueprintVertex2();
            
            boolean changed = false;
            if (!java.util.Objects.equals(lastVertex1, currentVertex1) || !java.util.Objects.equals(lastVertex2, currentVertex2)) {
                lastVertex1 = currentVertex1;
                lastVertex2 = currentVertex2;
                changed = true;
            }
            
            if (changed) {
                blueprintDataChanged = true;
            }
        }
    }
    
    /**
     * Verifica se i dati blueprint sono cambiati e resetta il flag
     */
    public boolean checkAndResetBlueprintDataChanged() {
        boolean changed = blueprintDataChanged;
        blueprintDataChanged = false;
        
        // Debug logging con null-safety
        if (changed && blockEntity != null) {
            System.out.println("DEBUG MENU: Blueprint data changed detected!");
            System.out.println("DEBUG MENU: vertex1 = " + blockEntity.getBlueprintVertex1());
            System.out.println("DEBUG MENU: vertex2 = " + blockEntity.getBlueprintVertex2());
            System.out.println("DEBUG MENU: hasValidArea = " + blockEntity.hasValidArea());
        } else if (changed && blockEntity == null) {
            System.out.println("DEBUG MENU: Blueprint data changed but blockEntity is null!");
        }
        
        return changed;
    }
    
    /**
     * Forza il refresh dei dati blueprint
     */
    public void forceRefreshBlueprintData() {
        blueprintDataChanged = true;
        
        // Aggiorna i dati di confronto per forzare il refresh
        if (blockEntity != null) {
            lastVertex1 = blockEntity.getBlueprintVertex1();
            lastVertex2 = blockEntity.getBlueprintVertex2();
        }
        
        System.out.println("DEBUG MENU: Forced refresh of blueprint data");
    }
    
    public StructureSaverMachineBlockEntity getBlockEntity() {
        return blockEntity;
    }
} 