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
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.FactoryBlockEntity;
import net.unfamily.iskautils.data.load.FactoryLoader;

public class FactoryMenu extends AbstractContainerMenu {
    private static final int DATA_SELECTED_COLOR_INDEX = 0;
    private static final int DATA_SCROLL_OFFSET = 1;
    private static final int DATA_POS_X = 2;
    private static final int DATA_POS_Y = 3;
    private static final int DATA_POS_Z = 4;
    private static final int DATA_ENERGY_STORED = 5;
    private static final int DATA_MAX_ENERGY = 6;
    private static final int DATA_REDSTONE_MODE = 7;
    private static final int DATA_COUNT = 8;

    private static final int SLOT_MACHINE_END = 2;
    private static final int SLOT_PLAYER_START = 2;
    private static final int SLOT_PLAYER_END = 38;

    private final FactoryBlockEntity blockEntityOrNull;
    private final BlockPos blockPos;
    private final ContainerLevelAccess levelAccess;
    private final ContainerData data;

    public static final int SLOT_INPUT_X = 44;
    public static final int SLOT_INPUT_Y = 25;
    public static final int SLOT_OUTPUT_X = 116;
    public static final int SLOT_OUTPUT_Y = 25;
    /** Vanilla container slot width/height in GUI pixels (matches {@link Slot} placement). */
    public static final int SLOT_SIZE = 18;

    public static final int PLAYER_INV_X = 8;
    public static final int PLAYER_INV_Y = 108;

    /** Top Y for a widget so it is vertically centered within a slot row starting at {@code slotTopY}. */
    public static int yCenteredInSlotRow(int slotTopY, int widgetHeight) {
        return slotTopY + (SLOT_SIZE - widgetHeight) / 2;
    }

    public FactoryMenu(int containerId, Inventory playerInventory, FactoryBlockEntity blockEntity) {
        super(ModMenuTypes.FACTORY_MENU.get(), containerId);
        this.blockEntityOrNull = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_SELECTED_COLOR_INDEX -> blockEntity.getSelectedColorIndex();
                    case DATA_SCROLL_OFFSET -> blockEntity.getScrollOffset();
                    case DATA_POS_X -> blockPos.getX();
                    case DATA_POS_Y -> blockPos.getY();
                    case DATA_POS_Z -> blockPos.getZ();
                    case DATA_ENERGY_STORED -> blockEntity.getEnergyStoredDisplay();
                    case DATA_MAX_ENERGY -> blockEntity.getMaxEnergyDisplay();
                    case DATA_REDSTONE_MODE -> blockEntity.getRedstoneMode();
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
        addDataSlots(this.data);

        addSlot(new Slot(blockEntity.getItems(), FactoryBlockEntity.SLOT_INPUT, SLOT_INPUT_X, SLOT_INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.isEmpty() || FactoryLoader.findSource(stack).isPresent();
            }
        });
        addSlot(new Slot(blockEntity.getItems(), FactoryBlockEntity.SLOT_OUTPUT, SLOT_OUTPUT_X, SLOT_OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        addPlayerInventory(playerInventory);
    }

    public FactoryMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.FACTORY_MENU.get(), containerId);
        this.blockEntityOrNull = null;
        this.blockPos = BlockPos.ZERO;
        this.levelAccess = ContainerLevelAccess.NULL;
        this.data = new SimpleContainerData(DATA_COUNT);
        addDataSlots(this.data);

        var dummy = new net.minecraft.world.SimpleContainer(FactoryBlockEntity.SLOT_COUNT);
        addSlot(new Slot(dummy, FactoryBlockEntity.SLOT_INPUT, SLOT_INPUT_X, SLOT_INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.isEmpty() || FactoryLoader.findSource(stack).isPresent();
            }
        });
        addSlot(new Slot(dummy, FactoryBlockEntity.SLOT_OUTPUT, SLOT_OUTPUT_X, SLOT_OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        addPlayerInventory(playerInventory);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int idx = col + row * 9 + 9;
                int x = PLAYER_INV_X + col * 18;
                int y = PLAYER_INV_Y + row * 18;
                addSlot(new Slot(playerInventory, idx, x, y));
            }
        }
        for (int col = 0; col < 9; col++) {
            int x = PLAYER_INV_X + col * 18;
            int y = PLAYER_INV_Y + 58;
            addSlot(new Slot(playerInventory, col, x, y));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.FACTORY.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack slotStack = slot.getItem();
        result = slotStack.copy();

        if (index < SLOT_MACHINE_END) {
            if (!this.moveItemStackTo(slotStack, SLOT_PLAYER_START, SLOT_PLAYER_END, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (FactoryLoader.findSource(slotStack).isEmpty()) {
                return ItemStack.EMPTY;
            }
            if (!this.moveItemStackTo(slotStack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (slotStack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    public BlockPos getSyncedBlockPos() {
        int x = data.get(DATA_POS_X);
        int y = data.get(DATA_POS_Y);
        int z = data.get(DATA_POS_Z);
        if (x == 0 && y == 0 && z == 0) return blockPos;
        return new BlockPos(x, y, z);
    }

    public int getSelectedColorIndex() {
        return data.get(DATA_SELECTED_COLOR_INDEX);
    }

    public int getScrollOffset() {
        return data.get(DATA_SCROLL_OFFSET);
    }

    public int getEnergyStored() {
        return data.get(DATA_ENERGY_STORED);
    }

    public int getMaxEnergyStored() {
        return data.get(DATA_MAX_ENERGY);
    }

    public int getRedstoneMode() {
        return data.get(DATA_REDSTONE_MODE);
    }

    public FactoryBlockEntity getBlockEntityOrNull() {
        return blockEntityOrNull;
    }
}
