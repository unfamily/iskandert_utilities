package net.unfamily.iskautils.client.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
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
        
        // Slot display per selectedItem (non interagibile)
        this.addSlot(new net.neoforged.neoforge.items.SlotItemHandler(new net.neoforged.neoforge.items.ItemStackHandler(1) {
            @Override
            public ItemStack getStackInSlot(int slot) {
                return blockEntity.getSelectedItem();
            }
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return false;
            }
            @Override
            public void setStackInSlot(int slot, ItemStack stack) {
                // no-op
            }
        }, 0, 56, 23) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
            @Override
            public boolean mayPickup(Player player) { return false; }
            @Override
            public ItemStack remove(int amount) { return ItemStack.EMPTY; }
        });

        // Slot encapsulated (interagibile)
        this.addSlot(new net.neoforged.neoforge.items.SlotItemHandler(blockEntity.getEncapsulatedSlot(), 0, 56, 48));

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    // Costruttore client-side (NeoForge factory)
    public AutoShopMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.AUTO_SHOP_MENU.get(), containerId);
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
        return ItemStack.EMPTY;
    }

    public AutoShopBlockEntity getBlockEntity() {
        return blockEntity;
    }
    public BlockPos getBlockPos() {
        return blockPos;
    }
    
    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 20 + l * 18, 74 + i * 18));
            }
        }
    }
    
    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 20 + i * 18, 74 + 58));
        }
    }
} 