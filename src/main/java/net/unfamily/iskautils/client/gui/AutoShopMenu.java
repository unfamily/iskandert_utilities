package net.unfamily.iskautils.client.gui;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.unfamily.iskautils.block.entity.AutoShopBlockEntity;
import net.unfamily.iskautils.block.ModBlocks;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class AutoShopMenu extends AbstractContainerMenu {
    private final AutoShopBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final BlockPos blockPos;

    // Costruttore server-side
    public AutoShopMenu(int containerId, Inventory playerInventory, AutoShopBlockEntity blockEntity) {
        super(ModMenuTypes.AUTO_SHOP_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        
        // Aggiungi le slot speciali dell'Auto Shop
        addAutoShopSlots();
        
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    // Costruttore client-side (NeoForge factory)
    public AutoShopMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.AUTO_SHOP_MENU.get(), containerId);
        // Client-side: non abbiamo accesso diretto alla BlockEntity
        // I dati verranno sincronizzati tramite packet se necessario
        this.blockEntity = null;
        this.blockPos = BlockPos.ZERO;
        this.levelAccess = ContainerLevelAccess.NULL;
        
        // Aggiungi le slot speciali dell'Auto Shop anche nel client-side
        addAutoShopSlots();
        
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.AUTO_SHOP.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            
            // Slot 0-1: slot speciali dell'Auto Shop (0 = selected, 1 = encapsulated)
            // Slot 2-37: inventario del player (2-28 = inventario, 29-37 = hotbar)
            int autoShopSlots = 2;
            int inventoryEnd = 38;
            
            if (index < autoShopSlots) {
                // Dalle slot dell'Auto Shop all'inventario del player
                if (!this.moveItemStackTo(itemstack1, autoShopSlots, inventoryEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < inventoryEnd) {
                // Dallo slot dell'inventario del player: non fare nulla per ora
                // In futuro qui si potrebbero gestire transazioni automatiche con l'Auto Shop
                return ItemStack.EMPTY;
            }
        }
        
        return itemstack;
    }

    public AutoShopBlockEntity getBlockEntity() {
        return blockEntity;
    }
    
    public BlockPos getBlockPos() {
        return blockPos;
    }
    
    private void addAutoShopSlots() {
        IItemHandler selectedHandler;
        IItemHandler encapsulatedHandler;
        
        if (blockEntity != null) {
            selectedHandler = blockEntity.getSelectedSlot();
            encapsulatedHandler = blockEntity.getEncapsulatedSlot();
        } else {
            // Client-side fallback
            selectedHandler = new ItemStackHandler(1);
            encapsulatedHandler = new ItemStackHandler(1);
        }
        
        // Slot per l'item selezionato (per auto compra/vendi)
        this.addSlot(new SlotItemHandler(selectedHandler, 0, 56, 23));
        
        // Slot per l'item encapsulated (auto comprato/venduto)
        this.addSlot(new SlotItemHandler(encapsulatedHandler, 0, 56, 48));
    }
    
    private void addPlayerInventory(Inventory playerInventory) {
        // Inventario del giocatore (3 righe x 9 slot)
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 20 + l * 18, 74 + i * 18));
            }
        }
    }
    
    private void addPlayerHotbar(Inventory playerInventory) {
        // Hotbar del giocatore (1 riga x 9 slot) - spostata per allinearsi con l'inventario
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 20 + i * 18, 132));
        }
    }
} 