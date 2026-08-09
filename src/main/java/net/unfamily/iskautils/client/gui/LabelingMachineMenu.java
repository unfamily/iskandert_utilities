package net.unfamily.iskautils.client.gui;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.util.LabelingNameStyle;
import org.jetbrains.annotations.NotNull;

/**
 * Portable Labeling Machine menu: one target slot + player inventory (shop layout).
 */
public class LabelingMachineMenu extends AbstractContainerMenu {

    public static final int TARGET_SLOT_X = 20;
    public static final int TARGET_SLOT_Y = 28;
    public static final int INVENTORY_Y = 154;
    public static final int HOTBAR_Y = 212;

    private final ItemStackHandler targetHandler = new ItemStackHandler(1);
    private boolean colorPickerOpen;

    public LabelingMachineMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.LABELING_MACHINE_MENU.get(), containerId);

        this.addSlot(new SlotItemHandler(targetHandler, 0, TARGET_SLOT_X, TARGET_SLOT_Y) {
            @Override
            public boolean isActive() {
                return !colorPickerOpen;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9 + 9;
                int xPos = 20 + col * 18;
                int yPos = INVENTORY_Y + row * 18;
                this.addSlot(new Slot(playerInventory, slotIndex, xPos, yPos) {
                    @Override
                    public boolean isActive() {
                        return !colorPickerOpen;
                    }
                });
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 20 + col * 18, HOTBAR_Y) {
                @Override
                public boolean isActive() {
                    return !colorPickerOpen;
                }
            });
        }
    }

    public ItemStack getTargetStack() {
        return targetHandler.getStackInSlot(0);
    }

    public void setColorPickerOpen(boolean open) {
        this.colorPickerOpen = open;
    }

    public boolean isColorPickerOpen() {
        return colorPickerOpen;
    }

    public boolean applyFormattedName(String text, boolean bold, boolean italic, boolean underline,
                                      boolean strikethrough, boolean obfuscated, int colorRgb) {
        ItemStack stack = targetHandler.getStackInSlot(0);
        if (stack.isEmpty()) {
            return false;
        }
        Component name = LabelingNameStyle.buildName(
                text, bold, italic, underline, strikethrough, obfuscated, colorRgb);
        if (name.getString().isEmpty()) {
            stack.remove(DataComponents.CUSTOM_NAME);
        } else {
            stack.set(DataComponents.CUSTOM_NAME, name);
        }
        targetHandler.setStackInSlot(0, stack);
        broadcastChanges();
        return true;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return hasLabelingMachine(player);
    }

    private static boolean hasLabelingMachine(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(ModItems.LABELING_MACHINE.get())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index == 0) {
            if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            ItemStack leftover = targetHandler.getStackInSlot(0);
            if (!leftover.isEmpty()) {
                if (!player.getInventory().add(leftover.copy())) {
                    player.drop(leftover.copy(), false);
                }
                targetHandler.setStackInSlot(0, ItemStack.EMPTY);
            }
        }
    }
}
