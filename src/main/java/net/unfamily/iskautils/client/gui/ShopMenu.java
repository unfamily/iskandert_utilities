package net.unfamily.iskautils.client.gui;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.unfamily.iskautils.block.entity.ShopBlockEntity;
import net.unfamily.iskautils.block.ModBlocks;

public class ShopMenu extends AbstractContainerMenu {
    private final ShopBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final BlockPos blockPos;

    // Costruttore server-side
    public ShopMenu(int containerId, Inventory playerInventory, ShopBlockEntity blockEntity) {
        super(ModMenuTypes.SHOP_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
    }

    // Costruttore client-side (NeoForge factory)
    public ShopMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.SHOP_MENU.get(), containerId);
        // Client-side: non abbiamo accesso diretto alla BlockEntity
        // I dati verranno sincronizzati tramite packet se necessario
        this.blockEntity = null;
        this.blockPos = BlockPos.ZERO;
        this.levelAccess = ContainerLevelAccess.NULL;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.SHOP.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    public ShopBlockEntity getBlockEntity() {
        return blockEntity;
    }
    public BlockPos getBlockPos() {
        return blockPos;
    }
} 