package net.unfamily.iskautils.client.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.unfamily.iskautils.block.entity.INullifierBE;

public class NullifierMenu extends AbstractContainerMenu {

    public static final int TEXTURE_WIDTH = 176;
    public static final int TEXTURE_HEIGHT = 166;
    public static final int GUI_WIDTH = TEXTURE_WIDTH;
    public static final int GUI_HEIGHT = TEXTURE_HEIGHT;

    public static final int MODULE_SLOT_X = 27;
    public static final int MODULE_SLOT_Y = 33;
    public static final int MODULE_SLOT_FRAME_X = MODULE_SLOT_X - 1;
    public static final int MODULE_SLOT_FRAME_Y = MODULE_SLOT_Y - 1;
    public static final int MODULE_SLOT_INDEX = 0;

    public static final int PLAYER_INV_X = 8;
    public static final int PLAYER_INV_Y = 84;
    public static final int PLAYER_HOTBAR_Y = 142;

    public static final int RANGE_INDEX = 0;
    public static final int MAX_RANGE_INDEX = 1;
    public static final int REDSTONE_MODE_INDEX = 2;
    public static final int POS_X_INDEX = 3;
    public static final int POS_Y_INDEX = 4;
    public static final int POS_Z_INDEX = 5;
    public static final int SHOW_AREA_INDEX = 6;
    public static final int TYPE_ID_INDEX = 7;
    public static final int DATA_COUNT = 8;

    private final INullifierBE nullifierBE;
    private final ContainerLevelAccess levelAccess;
    private final BlockPos blockPos;
    private final ContainerData containerData;

    public NullifierMenu(int containerId, Inventory playerInventory, INullifierBE nullifierBE) {
        super(ModMenuTypes.NULLIFIER_MENU.get(), containerId);
        this.nullifierBE = nullifierBE;
        this.blockPos = nullifierBE.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(nullifierBE.getLevel(), this.blockPos);

        this.containerData = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case RANGE_INDEX -> nullifierBE.getRange();
                    case MAX_RANGE_INDEX -> nullifierBE.getMaxRange();
                    case REDSTONE_MODE_INDEX -> nullifierBE.getRedstoneModeGui();
                    case POS_X_INDEX -> blockPos.getX();
                    case POS_Y_INDEX -> blockPos.getY();
                    case POS_Z_INDEX -> blockPos.getZ();
                    case SHOW_AREA_INDEX -> nullifierBE.isShowAreaEnabled() ? 1 : 0;
                    case TYPE_ID_INDEX -> nullifierBE.getNullifierType().getId();
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
        addDataSlots(this.containerData);
        addModuleSlot();
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    public NullifierMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.NULLIFIER_MENU.get(), containerId);
        this.nullifierBE = null;
        this.blockPos = BlockPos.ZERO;
        this.levelAccess = ContainerLevelAccess.NULL;
        this.containerData = new SimpleContainerData(DATA_COUNT);
        addDataSlots(this.containerData);
        addModuleSlot();
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private void addModuleSlot() {
        IItemHandler handler = nullifierBE != null
                ? nullifierBE.getModuleHandler()
                : new ItemStackHandler(1);
        addSlot(new SlotItemHandler(handler, 0, MODULE_SLOT_X, MODULE_SLOT_Y) {
            @Override
            public int getMaxStackSize() {
                return handler.getSlotLimit(0);
            }
        });
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col,
                    PLAYER_INV_X + col * 18, PLAYER_HOTBAR_Y));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (nullifierBE == null) {
            return false;
        }
        var level = nullifierBE.getLevel();
        if (level == null) {
            return false;
        }
        return level.getBlockEntity(blockPos) == nullifierBE
                && player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) < 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();
            if (index == 0) {
                if (!this.moveItemStackTo(slotStack, 1, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(slotStack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    public INullifierBE getNullifierBE() {
        return nullifierBE;
    }

    public int getRange() {
        return containerData.get(RANGE_INDEX);
    }

    public int getMaxRange() {
        return containerData.get(MAX_RANGE_INDEX);
    }

    public int getRedstoneModeGui() {
        return containerData.get(REDSTONE_MODE_INDEX);
    }

    public boolean isShowAreaEnabled() {
        return containerData.get(SHOW_AREA_INDEX) != 0;
    }

    public int getTypeId() {
        return containerData.get(TYPE_ID_INDEX);
    }

    public BlockPos getSyncedBlockPos() {
        if (nullifierBE != null) {
            return blockPos;
        }
        int x = containerData.get(POS_X_INDEX);
        int y = containerData.get(POS_Y_INDEX);
        int z = containerData.get(POS_Z_INDEX);
        return (x == 0 && y == 0 && z == 0) ? blockPos : new BlockPos(x, y, z);
    }
}
