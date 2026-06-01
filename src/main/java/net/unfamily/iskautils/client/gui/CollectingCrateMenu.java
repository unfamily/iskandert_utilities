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
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.CollectingCrateBlockEntity;
import net.unfamily.iskautils.item.ModItems;

public class CollectingCrateMenu extends AbstractContainerMenu {
    /** Top-left of each slot group in {@code textures/gui/backgrounds/collecting_crate.png} (176x230). */
    public static final int STORAGE_SLOTS_X = 8;
    public static final int STORAGE_SLOTS_Y = 22;
    public static final int MODULE_SLOT_X = 6;
    public static final int MODULE_SLOT_Y = 86;
    public static final int PLAYER_INVENTORY_X = 8;
    public static final int PLAYER_INVENTORY_Y = 148;
    public static final int PLAYER_HOTBAR_Y = PLAYER_INVENTORY_Y + 58;

    private static final int STORAGE_SLOT_COUNT = 27;
    public static final int MODULE_SLOT_INDEX = 27;
    private static final int PLAYER_SLOT_START = 28;

    private static final int COLLECT_MODE_INDEX = 0;
    private static final int REDSTONE_MODE_INDEX = 1;
    private static final int STORED_XP_MB_INDEX = 2;
    private static final int EFFECTIVE_RANGE_INDEX = 3;
    private static final int BLOCK_POS_X_INDEX = 4;
    private static final int BLOCK_POS_Y_INDEX = 5;
    private static final int BLOCK_POS_Z_INDEX = 6;
    private static final int DATA_COUNT = 7;

    private final CollectingCrateBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final BlockPos blockPos;
    private final ContainerData containerData;

    public CollectingCrateMenu(int containerId, Inventory playerInventory, CollectingCrateBlockEntity blockEntity) {
        super(ModMenuTypes.COLLECTING_CRATE_MENU.get(), containerId);

        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        this.containerData = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case COLLECT_MODE_INDEX -> blockEntity.getCollectMode().getId();
                    case REDSTONE_MODE_INDEX -> blockEntity.getRedstoneMode();
                    case STORED_XP_MB_INDEX -> blockEntity.getStoredXpMb();
                    case EFFECTIVE_RANGE_INDEX -> blockEntity.getEffectiveRange();
                    case BLOCK_POS_X_INDEX -> blockPos.getX();
                    case BLOCK_POS_Y_INDEX -> blockPos.getY();
                    case BLOCK_POS_Z_INDEX -> blockPos.getZ();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // read-only from client
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };

        this.addDataSlots(this.containerData);
        addStorageSlots(blockEntity.getStorageHandler());
        addModuleSlot(blockEntity.getModuleHandler());
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    public CollectingCrateMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.COLLECTING_CRATE_MENU.get(), containerId);

        this.blockEntity = null;
        this.blockPos = BlockPos.ZERO;
        this.levelAccess = ContainerLevelAccess.NULL;
        this.containerData = new SimpleContainerData(DATA_COUNT);
        this.addDataSlots(this.containerData);

        ItemStackHandler storage = new ItemStackHandler(STORAGE_SLOT_COUNT);
        ItemStackHandler module = new ItemStackHandler(1);
        addStorageSlots(storage);
        addModuleSlot(module);
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private void addStorageSlots(IItemHandler storageHandler) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9;
                addSlot(new SlotItemHandler(storageHandler, slotIndex,
                        STORAGE_SLOTS_X + col * 18,
                        STORAGE_SLOTS_Y + row * 18));
            }
        }
    }

    private void addModuleSlot(IItemHandler moduleHandler) {
        addSlot(new SlotItemHandler(moduleHandler, 0, MODULE_SLOT_X, MODULE_SLOT_Y) {
            @Override
            public int getMaxStackSize() {
                return moduleHandler.getSlotLimit(0);
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.RANGE_MODULE.get());
            }
        });
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9 + 9;
                addSlot(new Slot(playerInventory, slotIndex,
                        PLAYER_INVENTORY_X + col * 18,
                        PLAYER_INVENTORY_Y + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col,
                    PLAYER_INVENTORY_X + col * 18,
                    PLAYER_HOTBAR_Y));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.COLLECTING_CRATE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            if (index < PLAYER_SLOT_START) {
                if (!this.moveItemStackTo(slotStack, PLAYER_SLOT_START, 64, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotStack.is(ModItems.RANGE_MODULE.get())) {
                if (!this.moveItemStackTo(slotStack, MODULE_SLOT_INDEX, MODULE_SLOT_INDEX + 1, false)
                        && !this.moveItemStackTo(slotStack, 0, MODULE_SLOT_INDEX, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(slotStack, 0, MODULE_SLOT_INDEX, false)) {
                return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    public int getCollectMode() {
        return containerData.get(COLLECT_MODE_INDEX);
    }

    public int getRedstoneMode() {
        return containerData.get(REDSTONE_MODE_INDEX);
    }

    public int getStoredXpMb() {
        return containerData.get(STORED_XP_MB_INDEX);
    }

    public int getEffectiveRange() {
        return containerData.get(EFFECTIVE_RANGE_INDEX);
    }

    public BlockPos getSyncedBlockPos() {
        if (this.blockEntity != null) {
            return this.blockPos;
        }
        int x = this.containerData.get(BLOCK_POS_X_INDEX);
        int y = this.containerData.get(BLOCK_POS_Y_INDEX);
        int z = this.containerData.get(BLOCK_POS_Z_INDEX);
        if (x == 0 && y == 0 && z == 0) {
            return this.blockPos;
        }
        return new BlockPos(x, y, z);
    }
}
