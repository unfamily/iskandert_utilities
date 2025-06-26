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
import net.unfamily.iskautils.block.entity.ShopBlockEntity;
import net.unfamily.iskautils.block.ModBlocks;
import net.minecraft.network.chat.Component;
import net.unfamily.iskautils.block.entity.AutoShopBlockEntity;
import org.jetbrains.annotations.Nullable;

public class ShopMenu extends AbstractContainerMenu {
    private final ShopBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final BlockPos blockPos;

    // Server-side constructor
    public ShopMenu(int containerId, Inventory playerInventory, ShopBlockEntity blockEntity) {
        super(ModMenuTypes.SHOP_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        
        addPlayerInventorySlots(playerInventory);
    }

    // Client-side constructor (NeoForge factory)
    public ShopMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.SHOP_MENU.get(), containerId);
        // Client-side: we don't have direct access to BlockEntity
        // Data will be synchronized via packet if needed
        this.blockEntity = null;
        this.blockPos = BlockPos.ZERO;
        this.levelAccess = ContainerLevelAccess.NULL;
        
        addPlayerInventorySlots(playerInventory);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.SHOP.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            
            if (index < 36) {
                // Moving from player to shop
                if (!this.moveItemStackTo(itemstack1, 36, this.slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from shop to player
                if (!this.moveItemStackTo(itemstack1, 0, 36, false)) {
                    return ItemStack.EMPTY;
                }
            }
            
            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        
        return itemstack;
    }

    public ShopBlockEntity getBlockEntity() {
        return blockEntity;
    }
    public BlockPos getBlockPos() {
        return blockPos;
    }
    
    private void addPlayerInventorySlots(Inventory playerInventory) {
        // Player inventory (3 rows x 9 slots) - starting at 20, 154
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9 + 9; // +9 to skip hotbar
                int xPos = 20 + col * 18; // Starting at x=20
                int yPos = 154 + row * 18; // Starting at y=154
                this.addSlot(new Slot(playerInventory, slotIndex, xPos, yPos));
            }
        }
        
        // Player hotbar (1 row x 9 slots) - starting at 20, 212
        for (int col = 0; col < 9; col++) {
            int slotIndex = col;
            int xPos = 20 + col * 18; // Starting at x=20
            int yPos = 212; // Below inventory (154 + 3*18 = 208, +4 spacing)
            this.addSlot(new Slot(playerInventory, slotIndex, xPos, yPos));
        }
    }
} 