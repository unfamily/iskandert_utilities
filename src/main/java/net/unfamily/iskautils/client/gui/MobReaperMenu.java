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
import net.unfamily.iskautils.block.entity.MobReaperBlockEntity;

public class MobReaperMenu extends AbstractContainerMenu {
    private final MobReaperBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final BlockPos blockPos;
    private final ContainerData containerData;

    private static final int EFFECTIVE_DAMAGE_INDEX = 0;
    private static final int TARGET_TYPE_INDEX = 1;
    private static final int REDSTONE_MODE_INDEX = 2;
    private static final int BLOCK_POS_X_INDEX = 3;
    private static final int BLOCK_POS_Y_INDEX = 4;
    private static final int BLOCK_POS_Z_INDEX = 5;
    private static final int BEHEADING_CHANCE_INDEX = 6;
    private static final int LETHAL_ACTIVE_INDEX = 7;
    private static final int LUCK_LEVEL_INDEX = 8;
    private static final int EXPERIENCE_MULT_INDEX = 9;
    private static final int AGE_FILTER_INDEX = 10;
    private static final int DATA_COUNT = 11;

    public static final int MODULE_SLOTS_X = 7;
    public static final int[] MODULE_SLOT_Y = {19, 37, 55, 73, 91};
    public static final int PLAYER_INVENTORY_X = 7;
    public static final int PLAYER_INVENTORY_Y = 117;

    public MobReaperMenu(int containerId, Inventory playerInventory, MobReaperBlockEntity blockEntity) {
        super(ModMenuTypes.MOB_REAPER_MENU.get(), containerId);

        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        this.containerData = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case EFFECTIVE_DAMAGE_INDEX -> (int) (blockEntity.getEffectiveDamage() * 1000);
                    case TARGET_TYPE_INDEX -> blockEntity.getTargetType().getId();
                    case REDSTONE_MODE_INDEX -> blockEntity.getRedstoneMode();
                    case BLOCK_POS_X_INDEX -> blockPos.getX();
                    case BLOCK_POS_Y_INDEX -> blockPos.getY();
                    case BLOCK_POS_Z_INDEX -> blockPos.getZ();
                    case BEHEADING_CHANCE_INDEX -> (int) (blockEntity.getBeheadingChance() * 1000);
                    case LETHAL_ACTIVE_INDEX -> blockEntity.isLethalActive() ? 1 : 0;
                    case LUCK_LEVEL_INDEX -> blockEntity.getEffectiveLuckLevel();
                    case EXPERIENCE_MULT_INDEX -> (int) (blockEntity.getExperienceMultiplier() * 1000);
                    case AGE_FILTER_INDEX -> blockEntity.getAgeFilter().getId();
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
        addModuleSlots();
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    public MobReaperMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.MOB_REAPER_MENU.get(), containerId);

        this.blockEntity = null;
        this.blockPos = BlockPos.ZERO;
        this.levelAccess = ContainerLevelAccess.NULL;
        this.containerData = new SimpleContainerData(DATA_COUNT);
        this.addDataSlots(this.containerData);
        addModuleSlots();
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private void addModuleSlots() {
        IItemHandler itemHandler = blockEntity != null ? blockEntity.getModuleHandler() : new ItemStackHandler(5);
        for (int i = 0; i < 5; i++) {
            final int slotIndex = i;
            addSlot(new SlotItemHandler(itemHandler, slotIndex, MODULE_SLOTS_X + 1, MODULE_SLOT_Y[i] + 1) {
                @Override
                public int getMaxStackSize() {
                    return itemHandler.getSlotLimit(slotIndex);
                }
            });
        }
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9 + 9;
                addSlot(new Slot(playerInventory, slotIndex, PLAYER_INVENTORY_X + 1 + col * 18, PLAYER_INVENTORY_Y + 1 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, PLAYER_INVENTORY_X + 1 + col * 18, PLAYER_INVENTORY_Y + 1 + 3 * 18 + 4));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.MOB_REAPER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            if (index < 5) {
                if (!this.moveItemStackTo(slotStack, 5, 41, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(slotStack, 0, 5, false)) {
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

    public MobReaperBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public double getEffectiveDamage() {
        return containerData.get(EFFECTIVE_DAMAGE_INDEX) / 1000.0;
    }

    public int getTargetType() {
        return containerData.get(TARGET_TYPE_INDEX);
    }

    public int getRedstoneMode() {
        return containerData.get(REDSTONE_MODE_INDEX);
    }

    public float getBeheadingChance() {
        return containerData.get(BEHEADING_CHANCE_INDEX) / 1000.0f;
    }

    public boolean isLethalActive() {
        return containerData.get(LETHAL_ACTIVE_INDEX) == 1;
    }

    public int getLuckLevel() {
        return containerData.get(LUCK_LEVEL_INDEX);
    }

    public float getExperienceMultiplier() {
        return containerData.get(EXPERIENCE_MULT_INDEX) / 1000.0f;
    }

    public int getAgeFilter() {
        return containerData.get(AGE_FILTER_INDEX);
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
