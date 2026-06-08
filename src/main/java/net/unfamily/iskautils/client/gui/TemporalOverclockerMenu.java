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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.TemporalOverclockerBlockEntity;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.util.AncientTableFuel;

/**
 * Menu for the Temporal Overclocker
 */
public class TemporalOverclockerMenu extends AbstractContainerMenu {
    private final TemporalOverclockerBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final ContainerData containerData;
    private final BlockPos blockPos;

    public static final int SLOT_SIZE = 18;
    public static final int SLOT_ITEM_RENDER_SIZE = 16;
    /** 16x16 item centered inside 18x18 single_slot frame. */
    public static final int MACHINE_SLOT_ITEM_INSET = (SLOT_SIZE - SLOT_ITEM_RENDER_SIZE) / 2;
    public static final int MACHINE_SLOT_HIGHLIGHT_PADDING = 4;
    public static final int MACHINE_SLOT_HIGHLIGHT_SIZE = 24;

    public static int machineSlotItemX(int frameX) {
        return frameX + MACHINE_SLOT_ITEM_INSET;
    }

    public static int machineSlotItemY(int frameY) {
        return frameY + MACHINE_SLOT_ITEM_INSET;
    }

    public static int machineSlotHighlightX(int frameX) {
        return machineSlotItemX(frameX) - MACHINE_SLOT_HIGHLIGHT_PADDING;
    }

    public static int machineSlotHighlightY(int frameY) {
        return machineSlotItemY(frameY) - MACHINE_SLOT_HIGHLIGHT_PADDING;
    }

    public static final int GUI_WIDTH = 200;
    public static final int GUI_HEIGHT = 260;

    public static final int UPGRADE_SLOT_X = 24;
    public static final int FUEL_SLOT_X = 158;

    public static final int ENERGY_BAR_HEIGHT = 32;
    public static final int SIDE_BUTTON_SIZE = 16;
    public static final int SIDE_BUTTONS_GAP = 2;
    public static final int SIDE_BUTTONS_STACK_HEIGHT =
            SIDE_BUTTON_SIZE + SIDE_BUTTONS_GAP + SIDE_BUTTON_SIZE;

    public static final int CONTENT_Y_OFFSET = -10;
    public static final int LINKED_ENTRIES_START_Y = 30 + CONTENT_Y_OFFSET;
    public static final int LINKED_ENTRIES_VISIBLE = 5;
    public static final int LINKED_ENTRY_HEIGHT = 24;
    public static final int ACCELERATION_BUTTON_Y =
            LINKED_ENTRIES_START_Y + LINKED_ENTRIES_VISIBLE * LINKED_ENTRY_HEIGHT + 5;
    public static final int ACCELERATION_BUTTON_HEIGHT = 20;
    public static final int ACCELERATION_BUTTON_WIDTH = 100;
    /** Vertically centered with the overclock button row. */
    public static final int ENTROPY_ROW_Y =
            ACCELERATION_BUTTON_Y + (ACCELERATION_BUTTON_HEIGHT - SLOT_SIZE) / 2;
    public static final int UPGRADE_SLOT_INDEX = TemporalOverclockerBlockEntity.UPGRADE_SLOT_INDEX;
    public static final int FUEL_SLOT_INDEX = TemporalOverclockerBlockEntity.FUEL_SLOT_INDEX;

    /** Horizontally center text under an 18x18 machine slot frame. */
    public static int machineSlotLabelX(int slotX, int textWidth) {
        return slotX + (SLOT_SIZE - textWidth) / 2;
    }

    public static int machineSlotLabelY(int slotY) {
        return slotY + SLOT_SIZE + 2;
    }

    /** Side widgets align with the bottom rows of the visible entry list (scrollbar at the top). */
    public static final int LINKED_ENTRIES_SIDEBAR_ALIGN_COUNT = 3;

    public static int linkedEntriesSidebarAlignStartY() {
        int skippedRows = LINKED_ENTRIES_VISIBLE - LINKED_ENTRIES_SIDEBAR_ALIGN_COUNT;
        return LINKED_ENTRIES_START_Y + skippedRows * LINKED_ENTRY_HEIGHT;
    }

    public static int linkedEntriesSidebarAlignBlockHeight() {
        return LINKED_ENTRIES_SIDEBAR_ALIGN_COUNT * LINKED_ENTRY_HEIGHT;
    }

    /** Vertically center a side-column widget within the bottom linked-entry rows. */
    public static int linkedEntriesSidebarAlignElementY(int elementHeight) {
        return linkedEntriesSidebarAlignStartY()
                + (linkedEntriesSidebarAlignBlockHeight() - elementHeight) / 2;
    }

    public static int energyBarY() {
        return linkedEntriesSidebarAlignElementY(ENERGY_BAR_HEIGHT);
    }

    public static int sideButtonsStartY() {
        return linkedEntriesSidebarAlignElementY(SIDE_BUTTONS_STACK_HEIGHT);
    }

    public static int sideButtonsX(int entriesStartX, int entryWidth) {
        int entriesEndX = entriesStartX + entryWidth;
        int rightSpace = GUI_WIDTH - entriesEndX;
        return entriesEndX + (rightSpace - SIDE_BUTTON_SIZE) / 2;
    }

    private static final int PLAYER_INV_X = 20;
    public static final int PLAYER_INV_Y = 178;
    private static final int SLOT_PLAYER_START = 2;
    private static final int SLOT_PLAYER_END = 38;

    // ContainerData indices for syncing
    private static final int ENERGY_INDEX = 0;
    private static final int MAX_ENERGY_INDEX = 1;
    private static final int LINKED_BLOCKS_COUNT_INDEX = 2;
    private static final int REDSTONE_MODE_INDEX = 3;
    private static final int ACCELERATION_FACTOR_INDEX = 4;
    private static final int BLOCK_POS_X_INDEX = 5;
    private static final int BLOCK_POS_Y_INDEX = 6;
    private static final int BLOCK_POS_Z_INDEX = 7;
    private static final int LINKED_BLOCKS_HASH_INDEX = 8;
    private static final int PERSISTENT_MODE_INDEX = 9;
    private static final int STORED_ENTROPY_INDEX = 10;
    private static final int MAX_STORED_ENTROPY_INDEX = 11;
    private static final int DATA_COUNT = 12;

    // Constructor for server-side (with block entity)
    public TemporalOverclockerMenu(int containerId, Inventory playerInventory, TemporalOverclockerBlockEntity blockEntity) {
        super(ModMenuTypes.TEMPORAL_OVERCLOCKER_MENU.get(), containerId);

        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        this.containerData = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case ENERGY_INDEX -> blockEntity.getEnergyStorage().getEnergyStored();
                    case MAX_ENERGY_INDEX -> blockEntity.getEnergyStorage().getMaxEnergyStored();
                    case LINKED_BLOCKS_COUNT_INDEX -> blockEntity.getLinkedBlocks().size();
                    case REDSTONE_MODE_INDEX -> blockEntity.getRedstoneMode();
                    case ACCELERATION_FACTOR_INDEX -> blockEntity.getAccelerationFactor();
                    case BLOCK_POS_X_INDEX -> blockPos.getX();
                    case BLOCK_POS_Y_INDEX -> blockPos.getY();
                    case BLOCK_POS_Z_INDEX -> blockPos.getZ();
                    case LINKED_BLOCKS_HASH_INDEX -> calculateLinkedBlocksHash(blockEntity.getLinkedBlocks());
                    case PERSISTENT_MODE_INDEX -> blockEntity.isPersistentMode() ? 1 : 0;
                    case STORED_ENTROPY_INDEX -> blockEntity.getStoredEntropy();
                    case MAX_STORED_ENTROPY_INDEX -> blockEntity.getMaxStoredEntropy();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // Values are read-only from client side
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };

        this.addDataSlots(this.containerData);

        addSlot(new Slot(blockEntity.getMachineItems(), UPGRADE_SLOT_INDEX, UPGRADE_SLOT_X, ENTROPY_ROW_Y) {
            @Override
            public boolean isHighlightable() {
                return false;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.ENTROPIC_CLOCK.get());
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return 1;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                blockEntity.onMachineSlotChanged(UPGRADE_SLOT_INDEX);
            }
        });
        addSlot(new Slot(blockEntity.getMachineItems(), FUEL_SLOT_INDEX, FUEL_SLOT_X, ENTROPY_ROW_Y) {
            @Override
            public boolean isHighlightable() {
                return false;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return AncientTableFuel.isEntropyFuel(stack);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                blockEntity.onMachineSlotChanged(FUEL_SLOT_INDEX);
            }
        });
        addPlayerInventory(playerInventory);
    }

    // Constructor for client-side
    public TemporalOverclockerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, BlockPos.ZERO);
    }

    // Constructor for client-side with position
    public TemporalOverclockerMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModMenuTypes.TEMPORAL_OVERCLOCKER_MENU.get(), containerId);

        this.blockEntity = null;
        this.blockPos = pos;
        this.levelAccess = ContainerLevelAccess.NULL;

        this.containerData = new SimpleContainerData(DATA_COUNT);
        this.addDataSlots(this.containerData);

        var dummy = new net.minecraft.world.SimpleContainer(2);
        addSlot(new Slot(dummy, UPGRADE_SLOT_INDEX, UPGRADE_SLOT_X, ENTROPY_ROW_Y) {
            @Override
            public boolean isHighlightable() {
                return false;
            }
        });
        addSlot(new Slot(dummy, FUEL_SLOT_INDEX, FUEL_SLOT_X, ENTROPY_ROW_Y) {
            @Override
            public boolean isHighlightable() {
                return false;
            }
        });
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
        return stillValid(levelAccess, player, ModBlocks.TEMPORAL_OVERCLOCKER.get());
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
            if (slotStack.is(ModItems.ENTROPIC_CLOCK.get())) {
                if (!moveItemStackTo(slotStack, UPGRADE_SLOT_INDEX, UPGRADE_SLOT_INDEX + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (AncientTableFuel.isEntropyFuel(slotStack)) {
                if (!moveItemStackTo(slotStack, FUEL_SLOT_INDEX, FUEL_SLOT_INDEX + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(slotStack, UPGRADE_SLOT_INDEX, FUEL_SLOT_INDEX, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (slotStack.getCount() == result.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, slotStack);
        return result;
    }

    public TemporalOverclockerBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getEnergyStored() {
        return this.containerData.get(ENERGY_INDEX);
    }

    public int getMaxEnergyStored() {
        return this.containerData.get(MAX_ENERGY_INDEX);
    }

    public int getLinkedBlocksCount() {
        return this.containerData.get(LINKED_BLOCKS_COUNT_INDEX);
    }

    public int getRedstoneMode() {
        return this.containerData.get(REDSTONE_MODE_INDEX);
    }

    public int getAccelerationFactor() {
        return this.containerData.get(ACCELERATION_FACTOR_INDEX);
    }

    public boolean isAccelerationDepowered() {
        if (blockEntity != null) {
            return blockEntity.isAccelerationDepowered();
        }
        int factor = getAccelerationFactor();
        if (factor <= Config.temporalOverclockerAccelerationFactorMax) {
            return false;
        }
        if (!getSlot(UPGRADE_SLOT_INDEX).getItem().is(ModItems.ENTROPIC_CLOCK.get())) {
            return true;
        }
        if (Config.entropicClockEntropyPerTick <= 0) {
            return false;
        }
        if (getStoredEntropy() >= Config.entropicClockEntropyPerTick) {
            return false;
        }
        return !AncientTableFuel.isEntropyFuel(getSlot(FUEL_SLOT_INDEX).getItem());
    }

    public int getStoredEntropy() {
        return this.containerData.get(STORED_ENTROPY_INDEX);
    }

    public int getMaxStoredEntropy() {
        return this.containerData.get(MAX_STORED_ENTROPY_INDEX);
    }

    public boolean isPersistentMode() {
        return this.containerData.get(PERSISTENT_MODE_INDEX) != 0;
    }

    public BlockPos getSyncedBlockPos() {
        if (this.blockEntity != null) {
            return this.blockPos;
        } else {
            int x = this.containerData.get(BLOCK_POS_X_INDEX);
            int y = this.containerData.get(BLOCK_POS_Y_INDEX);
            int z = this.containerData.get(BLOCK_POS_Z_INDEX);
            if (x == 0 && y == 0 && z == 0) {
                return this.blockPos;
            }
            return new BlockPos(x, y, z);
        }
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    private static int calculateLinkedBlocksHash(java.util.List<BlockPos> linkedBlocks) {
        int hash = 0;
        for (BlockPos pos : linkedBlocks) {
            hash = hash * 31 + pos.hashCode();
        }
        return hash;
    }

    public int getLinkedBlocksHash() {
        return this.containerData.get(LINKED_BLOCKS_HASH_INDEX);
    }

    private java.util.List<BlockPos> cachedLinkedBlocks = new java.util.ArrayList<>();
    private int lastSyncedLinkedBlocksHash = 0;

    public void updateCachedLinkedBlocks() {
        if (this.blockEntity != null) {
            this.cachedLinkedBlocks = new java.util.ArrayList<>(this.blockEntity.getLinkedBlocks());
            this.lastSyncedLinkedBlocksHash = calculateLinkedBlocksHash(this.cachedLinkedBlocks);
        } else {
            int currentHash = getLinkedBlocksHash();
            boolean shouldUpdate = (currentHash != this.lastSyncedLinkedBlocksHash || this.lastSyncedLinkedBlocksHash == 0)
                    || (this.cachedLinkedBlocks.isEmpty() && getLinkedBlocksCount() > 0);

            if (shouldUpdate) {
                this.lastSyncedLinkedBlocksHash = currentHash;

                net.minecraft.world.level.Level clientLevel = net.unfamily.iskautils.util.ClientRuntimeAccess.getClientLevel();
                if (clientLevel != null) {
                    TemporalOverclockerBlockEntity be = getBlockEntityFromLevel(clientLevel);
                    if (be != null) {
                        this.cachedLinkedBlocks = new java.util.ArrayList<>(be.getLinkedBlocks());
                    }
                }
            }
        }
    }

    public java.util.List<BlockPos> getCachedLinkedBlocks() {
        return new java.util.ArrayList<>(cachedLinkedBlocks);
    }

    public TemporalOverclockerBlockEntity getBlockEntityFromLevel(Level level) {
        if (this.blockEntity != null) {
            return this.blockEntity;
        } else {
            BlockPos syncedPos = getSyncedBlockPos();
            if (!syncedPos.equals(BlockPos.ZERO)) {
                BlockEntity be = level.getBlockEntity(syncedPos);
                if (be instanceof TemporalOverclockerBlockEntity overclockerEntity) {
                    return overclockerEntity;
                }
            }
            return null;
        }
    }
}
