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
            
            // Gli slot 0-35 sono l'inventario del player (0-26 = inventario, 27-35 = hotbar)
            int inventoryEnd = 36;
            
            if (index < inventoryEnd) {
                // Dallo slot dell'inventario del player: non fare nulla per ora
                // In futuro qui si potrebbero gestire vendite automatiche al shop
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
    
    private void addPlayerInventory(Inventory playerInventory) {
        // Inventario del giocatore (3 righe x 9 slot) - spostato +1 pixel a destra e +1 pixel in basso
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 20 + l * 18, 154 + i * 18));
            }
        }
    }
    
    private void addPlayerHotbar(Inventory playerInventory) {
        // Hotbar del giocatore (1 riga x 9 slot) - spostato +1 pixel a destra e +1 pixel in basso
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 20 + i * 18, 212));
        }
    }
} 