package net.unfamily.iskautils.client.gui;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/**
 * Menu per la GUI del Structure Placer
 * Ora senza slot dell'inventario del player
 */
public class StructurePlacerMenu extends AbstractContainerMenu {
    
    public StructurePlacerMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.STRUCTURE_PLACER_MENU.get(), containerId);
        
        // Non aggiungiamo più gli slot dell'inventario del player
        // La GUI ora serve solo per la selezione delle strutture
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Non ci sono slot, quindi non gestiamo il shift-click
        return ItemStack.EMPTY;
    }
    
    @Override
    public boolean stillValid(Player player) {
        // La GUI è sempre valida finché il player ha il Structure Placer in mano
        return true;
    }
} 