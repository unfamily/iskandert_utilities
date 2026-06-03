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
import net.neoforged.neoforge.items.SlotItemHandler;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.BlazingAltarBlockEntity;
import net.unfamily.iskautils.item.ModItems;

public class BlazingAltarMenu extends AbstractContainerMenu {
    /** Must match pixel size of {@code textures/gui/backgrounds/blazing_altar.png}. */
    public static final int TEXTURE_WIDTH = 176;
    public static final int TEXTURE_HEIGHT = 166;
    public static final int GUI_WIDTH = TEXTURE_WIDTH;
    public static final int GUI_HEIGHT = TEXTURE_HEIGHT;

    public static final int PLACER_X = 80;
    public static final int PLACER_Y = 34;
    /** Range module: {@code single_slot} at frame; item slot +1 inset (1px left/up vs prior 26,34 placement). */
    public static final int MODULE_SLOT_X = 27;
    public static final int MODULE_SLOT_Y = PLACER_Y - 1;
    public static final int MODULE_SLOT_FRAME_X = MODULE_SLOT_X - 1;
    public static final int MODULE_SLOT_FRAME_Y = MODULE_SLOT_Y - 1;
    public static final int PLAYER_INV_X = 8;
    public static final int PLAYER_INV_Y = 84;
    public static final int PLAYER_HOTBAR_Y = 142;

    public static final int PLACER_SLOT_INDEX = 0;
    public static final int MODULE_SLOT_INDEX = 1;
    private static final int PLAYER_SLOT_START = 2;

    private static final int CHUNK_RADIUS_INDEX = 0;
    private static final int MAX_CHUNK_RADIUS_INDEX = 1;
    private static final int SPAWN_MODE_INDEX = 2;
    private static final int GROUND_ONLY_INDEX = 3;
    private static final int REDSTONE_MODE_INDEX = 4;
    private static final int BLOCK_POS_X_INDEX = 5;
    private static final int BLOCK_POS_Y_INDEX = 6;
    private static final int BLOCK_POS_Z_INDEX = 7;
    private static final int PLACEMENT_CHUNK_PROGRESS_INDEX = 8;
    private static final int PLACEMENT_CHUNK_TOTAL_INDEX = 9;
    private static final int EXTINGUISH_CHUNK_PROGRESS_INDEX = 10;
    private static final int EXTINGUISH_CHUNK_TOTAL_INDEX = 11;
    private static final int EXTINGUISHING_INDEX = 12;
    private static final int DATA_COUNT = 13;

    private final BlazingAltarBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final BlockPos blockPos;
    private final ContainerData containerData;

    public BlazingAltarMenu(int containerId, Inventory playerInventory, BlazingAltarBlockEntity blockEntity) {
        super(ModMenuTypes.BLAZING_ALTAR_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        this.containerData = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case CHUNK_RADIUS_INDEX -> blockEntity.getChunkRadius();
                    case MAX_CHUNK_RADIUS_INDEX -> blockEntity.getMaxChunkRadius();
                    case SPAWN_MODE_INDEX -> blockEntity.getSpawnMode().getId();
                    case GROUND_ONLY_INDEX -> blockEntity.isGroundOnly() ? 1 : 0;
                    case REDSTONE_MODE_INDEX -> blockEntity.getRedstoneMode();
                    case BLOCK_POS_X_INDEX -> blockPos.getX();
                    case BLOCK_POS_Y_INDEX -> blockPos.getY();
                    case BLOCK_POS_Z_INDEX -> blockPos.getZ();
                    case PLACEMENT_CHUNK_PROGRESS_INDEX -> blockEntity.getPlacementChunkProgress();
                    case PLACEMENT_CHUNK_TOTAL_INDEX -> blockEntity.getPlacementChunkTotal();
                    case EXTINGUISH_CHUNK_PROGRESS_INDEX -> blockEntity.getExtinguishChunkProgress();
                    case EXTINGUISH_CHUNK_TOTAL_INDEX -> blockEntity.getExtinguishChunkTotal();
                    case EXTINGUISHING_INDEX -> blockEntity.isExtinguishing() ? 1 : 0;
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
        addDataSlots(containerData);

        addSlot(new SlotItemHandler(blockEntity.getPlacerHandler(), BlazingAltarBlockEntity.PLACER_SLOT, PLACER_X, PLACER_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.BURNING_BRAZIER.get()) || stack.is(ModItems.CURSED_CANDLE.get());
            }
        });

        addSlot(new SlotItemHandler(blockEntity.getModuleHandler(), BlazingAltarBlockEntity.MODULE_SLOT, MODULE_SLOT_X, MODULE_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.RANGE_MODULE.get());
            }
        });

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    public BlazingAltarMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.BLAZING_ALTAR_MENU.get(), containerId);
        this.blockEntity = null;
        this.blockPos = BlockPos.ZERO;
        this.levelAccess = ContainerLevelAccess.NULL;
        this.containerData = new SimpleContainerData(DATA_COUNT);
        addDataSlots(containerData);

        addSlot(new Slot(new net.minecraft.world.SimpleContainer(1), 0, PLACER_X, PLACER_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.BURNING_BRAZIER.get()) || stack.is(ModItems.CURSED_CANDLE.get());
            }
        });

        addSlot(new Slot(new net.minecraft.world.SimpleContainer(1), 0, MODULE_SLOT_X, MODULE_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.RANGE_MODULE.get());
            }
        });

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, PLAYER_INV_X + col * 18, PLAYER_HOTBAR_Y));
        }
    }

    public BlazingAltarBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public BlockPos getSyncedBlockPos() {
        if (blockEntity != null) {
            return blockPos;
        }
        int x = containerData.get(BLOCK_POS_X_INDEX);
        int y = containerData.get(BLOCK_POS_Y_INDEX);
        int z = containerData.get(BLOCK_POS_Z_INDEX);
        if (x == 0 && y == 0 && z == 0) {
            return blockPos;
        }
        return new BlockPos(x, y, z);
    }

    public int getChunkRadius() {
        return containerData.get(CHUNK_RADIUS_INDEX);
    }

    public int getMaxChunkRadius() {
        return containerData.get(MAX_CHUNK_RADIUS_INDEX);
    }

    public int getSpawnModeId() {
        return containerData.get(SPAWN_MODE_INDEX);
    }

    public boolean isGroundOnly() {
        return containerData.get(GROUND_ONLY_INDEX) != 0;
    }

    public int getRedstoneMode() {
        return containerData.get(REDSTONE_MODE_INDEX);
    }

    public int getPlacementChunkProgress() {
        return containerData.get(PLACEMENT_CHUNK_PROGRESS_INDEX);
    }

    public int getPlacementChunkTotal() {
        return containerData.get(PLACEMENT_CHUNK_TOTAL_INDEX);
    }

    public int getExtinguishChunkProgress() {
        return containerData.get(EXTINGUISH_CHUNK_PROGRESS_INDEX);
    }

    public int getExtinguishChunkTotal() {
        return containerData.get(EXTINGUISH_CHUNK_TOTAL_INDEX);
    }

    public boolean isExtinguishing() {
        return containerData.get(EXTINGUISHING_INDEX) != 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index == PLACER_SLOT_INDEX) {
            if (!moveItemStackTo(stack, PLAYER_SLOT_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (index == MODULE_SLOT_INDEX) {
            if (!moveItemStackTo(stack, PLAYER_SLOT_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(ModItems.RANGE_MODULE.get())) {
            if (!moveItemStackTo(stack, MODULE_SLOT_INDEX, MODULE_SLOT_INDEX + 1, false)
                    && !moveItemStackTo(stack, PLAYER_SLOT_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(ModItems.BURNING_BRAZIER.get()) || stack.is(ModItems.CURSED_CANDLE.get())) {
            if (!moveItemStackTo(stack, PLACER_SLOT_INDEX, PLACER_SLOT_INDEX + 1, false)
                    && !moveItemStackTo(stack, PLAYER_SLOT_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, PLAYER_SLOT_START, slots.size(), true)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.BLAZING_ALTAR.get());
    }
}
