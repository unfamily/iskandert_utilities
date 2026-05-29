package net.unfamily.iskautils.client.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.AncientTableBlockEntity;
import net.unfamily.iskautils.util.AncientTableFuel;

public class AncientTableMenu extends AbstractContainerMenu {
    private static final int DATA_INPUT_SCROLL = 0;
    private static final int DATA_OUTPUT_SCROLL = 1;
    private static final int DATA_POS_X = 2;
    private static final int DATA_POS_Y = 3;
    private static final int DATA_POS_Z = 4;
    private static final int DATA_REDSTONE_MODE = 5;
    private static final int DATA_CRAFT_PROGRESS = 6;
    private static final int DATA_CRAFT_MAX = 7;
    private static final int DATA_FUEL_CHARGES = 8;
    private static final int DATA_FUEL_MAX = 9;
    private static final int DATA_COUNT = 10;

    public static final int INPUT_GRID_X = 20;
    public static final int INPUT_GRID_Y = 29;
    public static final int OUTPUT_GRID_X = 128;
    public static final int OUTPUT_GRID_Y = 29;
    public static final int FUEL_X = 92;
    public static final int FUEL_Y = 65;
    public static final int PLAYER_INV_X = 20;
    public static final int PLAYER_INV_Y = 108;
    public static final int SLOT_SIZE = 18;
    public static final int VISIBLE_GRID_ROWS = 3;
    public static final int GRID_COLS = 3;
    public static final int GRID_WIDTH = GRID_COLS * SLOT_SIZE;

    public static final int SCROLLBAR_WIDTH = 8;
    /**
     * Empty space between a 3×3 grid edge and the adjacent scrollbar (same on input and output).
     * Output at {@link #OUTPUT_GRID_X} + {@link #GRID_WIDTH} + gap = 128 + 54 + 2 = 184.
     */
    public static final int SCROLLBAR_GAP_X = 2;
    public static final int SCROLLBAR_HANDLE_SIZE = 8;
    public static final int SCROLLBAR_TRACK_HEIGHT = 34;
    public static final int SCROLLBAR_COLUMN_HEIGHT =
            SCROLLBAR_HANDLE_SIZE + SCROLLBAR_TRACK_HEIGHT + SCROLLBAR_HANDLE_SIZE;

    /** Left of input grid: 2px further left than the default gap (20 − 2 − 8 − 2 = 8). */
    public static final int INPUT_SCROLL_X = INPUT_GRID_X - SCROLLBAR_GAP_X - SCROLLBAR_WIDTH - 2;
    /** Right of output grid: grid right plus gap (182 + 2 = 184). */
    public static final int OUTPUT_SCROLL_X = OUTPUT_GRID_X + GRID_WIDTH + SCROLLBAR_GAP_X;

    /** Top Y for a widget vertically centered in a slot row (same as {@link FactoryMenu}). */
    public static int yCenteredInSlotRow(int slotTopY, int widgetHeight) {
        return slotTopY + (SLOT_SIZE - widgetHeight) / 2;
    }

    public static int inputGridRightX() {
        return INPUT_GRID_X + GRID_WIDTH;
    }

    public static int outputGridRightX() {
        return OUTPUT_GRID_X + GRID_WIDTH;
    }

    /** Nudge so the drawn column lines up with the visible slot block (items render at +1 in each slot). */
    private static final int SCROLL_VISUAL_Y_OFFSET = -1;

    /** Vertically centers the scrollbar column on the middle row of the 3×3 slot grid. */
    public static int scrollButtonUpY(int gridTopY) {
        int middleRowCenterY = gridTopY + SLOT_SIZE + SLOT_SIZE / 2;
        return middleRowCenterY - SCROLLBAR_COLUMN_HEIGHT / 2 + SCROLL_VISUAL_Y_OFFSET;
    }

    public static final int INPUT_SCROLL_UP_Y = scrollButtonUpY(INPUT_GRID_Y);
    public static final int OUTPUT_SCROLL_UP_Y = scrollButtonUpY(OUTPUT_GRID_Y);

    private static final int VISIBLE_SLOTS = 9;
    /** Container slot index for fuel in {@link AncientTableMenu}. */
    public static final int FUEL_SLOT_INDEX = 9;
    private static final int SLOT_FUEL = FUEL_SLOT_INDEX;
    private static final int SLOT_VISIBLE_OUTPUT_START = 10;
    private static final int SLOT_PLAYER_START = 19;
    private static final int SLOT_PLAYER_END = 55;

    private final AncientTableBlockEntity blockEntityOrNull;
    private final BlockPos blockPos;
    private final ContainerLevelAccess levelAccess;
    private final ContainerData data;

    public AncientTableMenu(int containerId, Inventory playerInventory, AncientTableBlockEntity blockEntity) {
        super(ModMenuTypes.ANCIENT_TABLE_MENU.get(), containerId);
        this.blockEntityOrNull = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_INPUT_SCROLL -> blockEntity.getInputScrollOffset();
                    case DATA_OUTPUT_SCROLL -> blockEntity.getOutputScrollOffset();
                    case DATA_POS_X -> blockPos.getX();
                    case DATA_POS_Y -> blockPos.getY();
                    case DATA_POS_Z -> blockPos.getZ();
                    case DATA_REDSTONE_MODE -> blockEntity.getRedstoneMode();
                    case DATA_CRAFT_PROGRESS -> blockEntity.getCraftProgress();
                    case DATA_CRAFT_MAX -> blockEntity.getCraftProgressMax();
                    case DATA_FUEL_CHARGES -> blockEntity.getFuelCharges();
                    case DATA_FUEL_MAX -> blockEntity.getFuelChargesMax();
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

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int guiIndex = col + row * 3;
                addSlot(new ScrolledInputSlot(
                        blockEntity,
                        guiIndex,
                        INPUT_GRID_X + col * SLOT_SIZE,
                        INPUT_GRID_Y + row * SLOT_SIZE));
            }
        }
        addSlot(new Slot(blockEntity.getItems(), blockEntity.fuelSlotIndex, FUEL_X, FUEL_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AncientTableFuel.isEntropyFuel(stack);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int guiIndex = col + row * 3;
                addSlot(new ScrolledOutputSlot(
                        blockEntity,
                        guiIndex,
                        OUTPUT_GRID_X + col * SLOT_SIZE,
                        OUTPUT_GRID_Y + row * SLOT_SIZE));
            }
        }
        addPlayerInventory(playerInventory);
    }

    public AncientTableMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.ANCIENT_TABLE_MENU.get(), containerId);
        this.blockEntityOrNull = null;
        this.blockPos = BlockPos.ZERO;
        this.levelAccess = ContainerLevelAccess.NULL;
        this.data = new net.minecraft.world.inventory.SimpleContainerData(DATA_COUNT);
        addDataSlots(this.data);
        var dummy = new net.minecraft.world.SimpleContainer(127);
        for (int i = 0; i < VISIBLE_SLOTS; i++) {
            addSlot(new Slot(dummy, i, INPUT_GRID_X + (i % 3) * SLOT_SIZE, INPUT_GRID_Y + (i / 3) * SLOT_SIZE));
        }
        addSlot(new Slot(dummy, SLOT_FUEL, FUEL_X, FUEL_Y));
        for (int i = 0; i < VISIBLE_SLOTS; i++) {
            addSlot(new Slot(dummy, SLOT_VISIBLE_OUTPUT_START + i, OUTPUT_GRID_X + (i % 3) * SLOT_SIZE, OUTPUT_GRID_Y + (i / 3) * SLOT_SIZE));
        }
        addPlayerInventory(playerInventory);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int idx = col + row * 9 + 9;
                addSlot(new Slot(playerInventory, idx, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, PLAYER_INV_X + col * 18, PLAYER_INV_Y + 58));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.ANCIENT_TABLE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack slotStack = slot.getItem();
        result = slotStack.copy();
        if (index < SLOT_PLAYER_START) {
            if (!moveItemStackTo(slotStack, SLOT_PLAYER_START, SLOT_PLAYER_END, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (AncientTableFuel.isEntropyFuel(slotStack)) {
                if (!moveItemStackTo(slotStack, SLOT_FUEL, SLOT_FUEL + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(slotStack, 0, SLOT_FUEL, false)) {
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
        if (x == 0 && y == 0 && z == 0) {
            return blockPos;
        }
        return new BlockPos(x, y, z);
    }

    public int getInputScrollOffset() {
        return data.get(DATA_INPUT_SCROLL);
    }

    public int getOutputScrollOffset() {
        return data.get(DATA_OUTPUT_SCROLL);
    }

    public int getRedstoneMode() {
        return data.get(DATA_REDSTONE_MODE);
    }

    public int getCraftProgress() {
        return data.get(DATA_CRAFT_PROGRESS);
    }

    public int getCraftProgressMax() {
        return data.get(DATA_CRAFT_MAX);
    }

    public int getFuelCharges() {
        return data.get(DATA_FUEL_CHARGES);
    }

    public int getFuelChargesMax() {
        return data.get(DATA_FUEL_MAX);
    }

    public AncientTableBlockEntity getBlockEntityOrNull() {
        return blockEntityOrNull;
    }

    private final class ScrolledInputSlot extends Slot {
        private final int guiIndex;

        ScrolledInputSlot(AncientTableBlockEntity be, int guiIndex, int x, int y) {
            super(be.getItems(), 0, x, y);
            this.guiIndex = guiIndex;
        }

        private int backingIndexOrNeg() {
            if (blockEntityOrNull == null) {
                return -1;
            }
            int backing = blockEntityOrNull.getInputScrollOffset() + guiIndex;
            if (backing < 0 || backing >= blockEntityOrNull.getInputCount()) {
                return -1;
            }
            return backing;
        }

        @Override
        public ItemStack getItem() {
            int backing = backingIndexOrNeg();
            return backing < 0 ? ItemStack.EMPTY : blockEntityOrNull.getItems().getItem(backing);
        }

        @Override
        public void set(ItemStack stack) {
            int backing = backingIndexOrNeg();
            if (backing >= 0) {
                blockEntityOrNull.setItem(backing, stack);
            }
        }

        @Override
        public ItemStack remove(int amount) {
            int backing = backingIndexOrNeg();
            if (backing < 0) {
                return ItemStack.EMPTY;
            }
            ItemStack removed = blockEntityOrNull.removeItem(backing, amount);
            if (!removed.isEmpty()) {
                blockEntityOrNull.setChanged();
            }
            return removed;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !AncientTableFuel.isEntropyFuel(stack);
        }
    }

    private final class ScrolledOutputSlot extends Slot {
        private final int guiIndex;

        ScrolledOutputSlot(AncientTableBlockEntity be, int guiIndex, int x, int y) {
            super(be.getItems(), be.outputStartIndex, x, y);
            this.guiIndex = guiIndex;
        }

        private int backingOutputSlotOrNeg() {
            if (blockEntityOrNull == null) {
                return -1;
            }
            int backing = blockEntityOrNull.getOutputScrollOffset() + guiIndex;
            if (backing < 0 || backing >= blockEntityOrNull.getInputCount()) {
                return -1;
            }
            return blockEntityOrNull.outputStartIndex + backing;
        }

        @Override
        public ItemStack getItem() {
            int slot = backingOutputSlotOrNeg();
            return slot < 0 ? ItemStack.EMPTY : blockEntityOrNull.getItems().getItem(slot);
        }

        @Override
        public void set(ItemStack stack) {
            int slot = backingOutputSlotOrNeg();
            if (slot >= 0) {
                blockEntityOrNull.setItem(slot, stack);
            }
        }

        @Override
        public ItemStack remove(int amount) {
            int slot = backingOutputSlotOrNeg();
            if (slot < 0) {
                return ItemStack.EMPTY;
            }
            ItemStack removed = blockEntityOrNull.removeItem(slot, amount);
            if (!removed.isEmpty()) {
                blockEntityOrNull.setChanged();
            }
            return removed;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
