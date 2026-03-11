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
import net.unfamily.iskautils.block.entity.SoundMufflerBlockEntity;

public class SoundMufflerFilterMenu extends AbstractContainerMenu {

    private final BlockPos blockPos;

    public SoundMufflerFilterMenu(int containerId, Inventory playerInventory, BlockPos blockPos) {
        super(ModMenuTypes.SOUND_MUFFLER_FILTER_MENU.get(), containerId);
        this.blockPos = blockPos == null ? BlockPos.ZERO : blockPos;
    }

    public SoundMufflerFilterMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, BlockPos.ZERO);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(player.level(), blockPos), player, ModBlocks.SOUND_MUFFLER.get());
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public SoundMufflerBlockEntity getBlockEntityFromLevel(Level level) {
        if (level == null || blockPos.equals(BlockPos.ZERO)) return null;
        BlockEntity be = level.getBlockEntity(blockPos);
        return be instanceof SoundMufflerBlockEntity m ? m : null;
    }
}
