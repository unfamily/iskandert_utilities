package net.unfamily.iskautils.client.gui;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.unfamily.iskautils.block.entity.AutoShopBlockEntity;
import net.unfamily.iskautils.block.ModBlocks;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.IItemHandler;

public class AutoShopMenu extends AbstractContainerMenu {
    private final AutoShopBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final BlockPos blockPos;
    
    // Indici degli slot
    private static final int ENCAPSULATED_SLOT_INDEX = 0;
    private static final int PLAYER_INV_START = 1;
    private static final int PLAYER_HOTBAR_START = PLAYER_INV_START + 27;
    private static final int PLAYER_HOTBAR_END = PLAYER_HOTBAR_START + 9;

    // Costruttore server-side
    public AutoShopMenu(int containerId, Inventory playerInventory, AutoShopBlockEntity blockEntity) {
        super(ModMenuTypes.AUTO_SHOP_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        
        // Aggiungi prima lo slot encapsulato
        if (blockEntity != null) {
            this.addSlot(new SlotItemHandler(blockEntity.getEncapsulatedSlot(), 0, 190 + 12, 154 + 24));
        }
        
        // Poi aggiungi l'inventario del player
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    // Costruttore client-side (NeoForge factory)
    public AutoShopMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.AUTO_SHOP_MENU.get(), containerId);
        this.blockEntity = null;
        this.blockPos = BlockPos.ZERO;
        this.levelAccess = ContainerLevelAccess.NULL;
        
        // Aggiungi uno slot dummy per il client
        this.addSlot(new Slot(playerInventory, 0, 190 + 12, 154 + 24));
        
        // Poi aggiungi l'inventario del player
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
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();
            
            // Se lo slot è quello encapsulato (index 0)
            if (index == ENCAPSULATED_SLOT_INDEX) {
                // Prova a spostare nel player inventory
                if (!this.moveItemStackTo(stackInSlot, PLAYER_INV_START, PLAYER_HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stackInSlot, itemstack);
            }
            // Se lo slot è nell'inventario del player
            else if (index >= PLAYER_INV_START) {
                // Prova a spostare nello slot encapsulato
                if (!this.moveItemStackTo(stackInSlot, ENCAPSULATED_SLOT_INDEX, ENCAPSULATED_SLOT_INDEX + 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
            
            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            
            if (stackInSlot.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            
            slot.onTake(player, stackInSlot);
        }
        
        return itemstack;
    }

    public AutoShopBlockEntity getBlockEntity() {
        return blockEntity;
    }
    
    public BlockPos getBlockPos() {
        return blockPos;
    }
    
    private void addPlayerInventory(Inventory playerInventory) {
        // Inventario del giocatore (3 righe x 9 slot)
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 20 + l * 18, 154 + i * 18));
            }
        }
    }
    
    private void addPlayerHotbar(Inventory playerInventory) {
        // Hotbar del giocatore (1 riga x 9 slot)
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 20 + i * 18, 212));
        }
    }
} 