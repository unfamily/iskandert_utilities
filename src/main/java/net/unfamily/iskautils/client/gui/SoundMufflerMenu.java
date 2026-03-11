package net.unfamily.iskautils.client.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.SoundMufflerBlockEntity;

public class SoundMufflerMenu extends AbstractContainerMenu {

    private final SoundMufflerBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final BlockPos blockPos;
    private final ContainerData containerData;

    private static final int VOLUME_START = 0;
    private static final int VOLUME_COUNT = SoundMufflerBlockEntity.CATEGORY_COUNT;
    private static final int POS_X_INDEX = VOLUME_COUNT;
    private static final int POS_Y_INDEX = VOLUME_COUNT + 1;
    private static final int POS_Z_INDEX = VOLUME_COUNT + 2;
    private static final int ALLOW_LIST_INDEX = VOLUME_COUNT + 3;
    private static final int DATA_COUNT = VOLUME_COUNT + 4;

    public SoundMufflerMenu(int containerId, Inventory playerInventory, SoundMufflerBlockEntity blockEntity) {
        super(ModMenuTypes.SOUND_MUFFLER_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        this.containerData = new ContainerData() {
            @Override
            public int get(int index) {
                if (index >= 0 && index < VOLUME_COUNT) {
                    return blockEntity.getVolume(index);
                }
                return switch (index) {
                    case POS_X_INDEX -> blockPos.getX();
                    case POS_Y_INDEX -> blockPos.getY();
                    case POS_Z_INDEX -> blockPos.getZ();
                    case ALLOW_LIST_INDEX -> blockEntity.isAllowList() ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
        this.addDataSlots(this.containerData);
    }

    /** Client-side constructor (no block entity). */
    public SoundMufflerMenu(int containerId, Inventory playerInventory, net.minecraft.world.entity.player.Player player) {
        this(containerId, playerInventory);
    }

    public SoundMufflerMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.SOUND_MUFFLER_MENU.get(), containerId);
        this.blockEntity = null;
        this.blockPos = BlockPos.ZERO;
        this.levelAccess = ContainerLevelAccess.NULL;
        this.containerData = new SimpleContainerData(DATA_COUNT);
        this.addDataSlots(this.containerData);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.SOUND_MUFFLER.get());
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        return net.minecraft.world.item.ItemStack.EMPTY;
    }

    public int getVolume(int categoryIndex) {
        if (categoryIndex < 0 || categoryIndex >= VOLUME_COUNT) return SoundMufflerBlockEntity.DEFAULT_VOLUME;
        return containerData.get(VOLUME_START + categoryIndex);
    }

    public BlockPos getSyncedBlockPos() {
        if (blockEntity != null) {
            return blockPos;
        }
        int x = containerData.get(POS_X_INDEX);
        int y = containerData.get(POS_Y_INDEX);
        int z = containerData.get(POS_Z_INDEX);
        return new BlockPos(x, y, z);
    }

    public boolean isAllowList() {
        if (blockEntity != null) return blockEntity.isAllowList();
        return containerData.get(ALLOW_LIST_INDEX) != 0;
    }

    public SoundMufflerBlockEntity getBlockEntityFromLevel(Level level) {
        if (blockEntity != null) return blockEntity;
        if (level == null) return null;
        BlockPos pos = getSyncedBlockPos();
        if (pos.equals(BlockPos.ZERO)) return null;
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof SoundMufflerBlockEntity m ? m : null;
    }
}
