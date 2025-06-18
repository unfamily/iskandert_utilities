package net.unfamily.iskautils.client.gui;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu per la GUI del Structure Placer
 */
public class StructurePlacerMenu extends AbstractContainerMenu {
    
    // L'inventario del player inizia alle coordinate 20;154 nel background (+1 pixel)
    private static final int PLAYER_INVENTORY_X = 20;
    private static final int PLAYER_INVENTORY_Y = 154;
    
    public StructurePlacerMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.STRUCTURE_PLACER_MENU.get(), containerId);
        
        // Aggiungi gli slot dell'inventario del player
        addPlayerInventorySlots(playerInventory);
    }
    
    /**
     * Aggiunge gli slot dell'inventario del player alla GUI
     */
    private void addPlayerInventorySlots(Inventory playerInventory) {
        // Inventario principale (27 slot, 3 righe di 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = 9 + row * 9 + col; // Gli slot 0-8 sono la hotbar
                int x = PLAYER_INVENTORY_X + col * 18;
                int y = PLAYER_INVENTORY_Y + row * 18;
                addSlot(new Slot(playerInventory, slotIndex, x, y));
            }
        }
        
        // Hotbar (9 slot, 1 riga)
        for (int col = 0; col < 9; col++) {
            int slotIndex = col;
            int x = PLAYER_INVENTORY_X + col * 18;
            int y = PLAYER_INVENTORY_Y + 58; // 58 pixel sotto l'inventario principale
            addSlot(new Slot(playerInventory, slotIndex, x, y));
        }
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Per ora non gestiamo il shift-click
        return ItemStack.EMPTY;
    }
    
    @Override
    public boolean stillValid(Player player) {
        // La GUI è sempre valida finché il player ha il Structure Placer in mano
        return true;
    }
} 