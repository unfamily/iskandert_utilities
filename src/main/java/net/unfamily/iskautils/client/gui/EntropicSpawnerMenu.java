package net.unfamily.iskautils.client.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.EntropicSpawnerBlockEntity;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.util.AncientTableFuel;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Menu for the Entropic Spawner GUI (Fan layout: 176×200).
 */
public class EntropicSpawnerMenu extends AbstractContainerMenu {
    public static final int GUI_WIDTH = 176;
    public static final int GUI_HEIGHT = 200;

    public static final int MODULE_SLOTS_X = 7;
    public static final int MODULE_SLOT_1_Y = 23;
    public static final int MODULE_SLOT_2_Y = 41;
    public static final int MODULE_SLOT_3_Y = 59;
    public static final int FUEL_SLOT_X = (GUI_WIDTH - 18) / 2;
    public static final int FUEL_SLOT_Y = MODULE_SLOT_2_Y;
    public static final int PLAYER_INVENTORY_X = 7;
    public static final int PLAYER_INVENTORY_Y = 117;

    private static final int SLOT_PLAYER_START = EntropicSpawnerBlockEntity.MACHINE_SLOT_COUNT;
    private static final int SLOT_PLAYER_END = SLOT_PLAYER_START + 36;

    private static final int DATA_SPAWN_DELAY = 0;
    private static final int DATA_REDSTONE_MODE = 1;
    private static final int DATA_ENTITY_TYPE_ID = 2;
    private static final int DATA_POS_X = 3;
    private static final int DATA_POS_Y = 4;
    private static final int DATA_POS_Z = 5;
    private static final int DATA_STORED_ENTROPY = 6;
    private static final int DATA_STORED_ENTROPY_MAX = 7;
    private static final int DATA_LIFETIME_SPAWN_COUNT = 8;
    private static final int DATA_LIFETIME_SPAWN_MAX = 9;
    private static final int DATA_COUNT = 10;

    @Nullable
    private final EntropicSpawnerBlockEntity blockEntity;
    private final BlockPos blockPos;
    private final ContainerLevelAccess levelAccess;
    private final ContainerData containerData;

    public EntropicSpawnerMenu(int containerId, Inventory playerInventory, EntropicSpawnerBlockEntity blockEntity) {
        super(ModMenuTypes.ENTROPIC_SPAWNER_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.containerData = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_SPAWN_DELAY -> blockEntity.getSpawnDelayTicks();
                    case DATA_REDSTONE_MODE -> blockEntity.getRedstoneMode();
                    case DATA_ENTITY_TYPE_ID -> blockEntity.getSpawnEntityTypeSyncId();
                    case DATA_POS_X -> blockPos.getX();
                    case DATA_POS_Y -> blockPos.getY();
                    case DATA_POS_Z -> blockPos.getZ();
                    case DATA_STORED_ENTROPY -> blockEntity.getStoredEntropy();
                    case DATA_STORED_ENTROPY_MAX -> blockEntity.getMaxStoredEntropy();
                    case DATA_LIFETIME_SPAWN_COUNT -> blockEntity.getLifetimeSpawnCount();
                    case DATA_LIFETIME_SPAWN_MAX -> blockEntity.getLifetimeSpawnMax();
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
        addMachineSlots(blockEntity);
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    public EntropicSpawnerMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.ENTROPIC_SPAWNER_MENU.get(), containerId);
        this.blockEntity = null;
        this.blockPos = BlockPos.ZERO;
        this.levelAccess = ContainerLevelAccess.NULL;
        this.containerData = new SimpleContainerData(DATA_COUNT);
        addDataSlots(containerData);

        var dummy = new net.neoforged.neoforge.items.ItemStackHandler(EntropicSpawnerBlockEntity.MACHINE_SLOT_COUNT);
        addMachineSlotsHandler(dummy);
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private void addMachineSlots(EntropicSpawnerBlockEntity be) {
        addMachineSlotsHandler(be.getMachineItems());
    }

    private void addMachineSlotsHandler(net.neoforged.neoforge.items.ItemStackHandler handler) {
        addSlot(new SlotItemHandler(handler, EntropicSpawnerBlockEntity.CLOCK_SLOT_INDEX,
                MODULE_SLOTS_X + 1, MODULE_SLOT_1_Y + 1));

        addSlot(new SlotItemHandler(handler, EntropicSpawnerBlockEntity.PRODUCTION_SLOT_INDEX,
                MODULE_SLOTS_X + 1, MODULE_SLOT_2_Y + 1));

        addSlot(new SlotItemHandler(handler, EntropicSpawnerBlockEntity.PLACEHOLDER_SLOT_INDEX,
                MODULE_SLOTS_X + 1, MODULE_SLOT_3_Y + 1) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        });

        addSlot(new SlotItemHandler(handler, EntropicSpawnerBlockEntity.FUEL_SLOT_INDEX,
                FUEL_SLOT_X + 1, FUEL_SLOT_Y + 1) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AncientTableFuel.isEntropyFuel(stack);
            }
        });
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9 + 9;
                addSlot(new Slot(playerInventory, slotIndex,
                        PLAYER_INVENTORY_X + 1 + col * 18,
                        PLAYER_INVENTORY_Y + 1 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col,
                    PLAYER_INVENTORY_X + 1 + col * 18,
                    PLAYER_INVENTORY_Y + 1 + 3 * 18 + 4));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.ENTROPIC_SPAWNER.get());
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
                if (!moveItemStackTo(slotStack, EntropicSpawnerBlockEntity.CLOCK_SLOT_INDEX,
                        EntropicSpawnerBlockEntity.CLOCK_SLOT_INDEX + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotStack.is(ModItems.PRODUCTION_MODULE.get())) {
                if (!moveItemStackTo(slotStack, EntropicSpawnerBlockEntity.PRODUCTION_SLOT_INDEX,
                        EntropicSpawnerBlockEntity.PRODUCTION_SLOT_INDEX + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (AncientTableFuel.isEntropyFuel(slotStack)) {
                if (!moveItemStackTo(slotStack, EntropicSpawnerBlockEntity.FUEL_SLOT_INDEX,
                        EntropicSpawnerBlockEntity.FUEL_SLOT_INDEX + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(slotStack, EntropicSpawnerBlockEntity.CLOCK_SLOT_INDEX,
                    EntropicSpawnerBlockEntity.FUEL_SLOT_INDEX, false)) {
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

    public BlockPos getSyncedBlockPos() {
        int x = containerData.get(DATA_POS_X);
        int y = containerData.get(DATA_POS_Y);
        int z = containerData.get(DATA_POS_Z);
        if (x == 0 && y == 0 && z == 0) {
            return blockPos;
        }
        return new BlockPos(x, y, z);
    }

    public int getSpawnDelayTicks() {
        return containerData.get(DATA_SPAWN_DELAY);
    }

    public int getRedstoneMode() {
        return containerData.get(DATA_REDSTONE_MODE);
    }

    public int getFuelCharges() {
        return containerData.get(DATA_STORED_ENTROPY);
    }

    public int getFuelChargesMax() {
        return containerData.get(DATA_STORED_ENTROPY_MAX);
    }

    public int getLifetimeSpawnCount() {
        return containerData.get(DATA_LIFETIME_SPAWN_COUNT);
    }

    public int getLifetimeSpawnMax() {
        return containerData.get(DATA_LIFETIME_SPAWN_MAX);
    }

    public boolean isLifetimeSpawnCapReached() {
        int max = getLifetimeSpawnMax();
        return max > 0 && getLifetimeSpawnCount() >= max;
    }

    @Nullable
    public EntityType<?> getSyncedEntityType() {
        int id = containerData.get(DATA_ENTITY_TYPE_ID);
        if (id < 0) {
            return null;
        }
        return BuiltInRegistries.ENTITY_TYPE.byId(id);
    }

    @Nullable
    public EntropicSpawnerBlockEntity getBlockEntity() {
        return blockEntity;
    }
}
