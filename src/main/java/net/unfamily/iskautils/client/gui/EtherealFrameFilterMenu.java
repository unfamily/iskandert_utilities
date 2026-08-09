package net.unfamily.iskautils.client.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.EtherealFrameBlockEntity;

public class EtherealFrameFilterMenu extends AbstractContainerMenu {

    private final BlockPos blockPos;

    public EtherealFrameFilterMenu(int containerId, Inventory playerInventory, BlockPos blockPos) {
        super(ModMenuTypes.ETHEREAL_FRAME_FILTER_MENU.get(), containerId);
        this.blockPos = blockPos == null ? BlockPos.ZERO : blockPos;
    }

    public EtherealFrameFilterMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, BlockPos.ZERO);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(player.level(), blockPos), player,
                ModBlocks.ETHEREAL_FRAME.get());
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public EtherealFrameBlockEntity getBlockEntityFromLevel(Level level) {
        if (level == null || blockPos.equals(BlockPos.ZERO)) return null;
        BlockEntity be = level.getBlockEntity(blockPos);
        return be instanceof EtherealFrameBlockEntity f ? f : null;
    }
}
