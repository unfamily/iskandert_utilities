package net.unfamily.iskautils.client.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.DeepDrawerExtractorBlockEntity;

/**
 * Menu for Deep Drawer Extractor GUI
 */
public class DeepDrawerExtractorMenu extends AbstractContainerMenu {
    private final DeepDrawerExtractorBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final BlockPos blockPos;
    
    // Server-side constructor
    public DeepDrawerExtractorMenu(int containerId, Inventory playerInventory, DeepDrawerExtractorBlockEntity blockEntity) {
        super(ModMenuTypes.DEEP_DRAWER_EXTRACTOR_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
    }

    // Client-side constructor (NeoForge factory)
    public DeepDrawerExtractorMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.DEEP_DRAWER_EXTRACTOR_MENU.get(), containerId);
        // Client-side: we don't have direct access to the BlockEntity
        this.blockEntity = null;
        this.blockPos = BlockPos.ZERO;
        this.levelAccess = ContainerLevelAccess.NULL;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.DEEP_DRAWER_EXTRACTOR.get());
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        // No slots, so quickMoveStack doesn't need to be implemented
        return net.minecraft.world.item.ItemStack.EMPTY;
    }
    
    public DeepDrawerExtractorBlockEntity getBlockEntity() {
        return blockEntity;
    }
    
    public BlockPos getBlockPos() {
        return blockPos;
    }
}
