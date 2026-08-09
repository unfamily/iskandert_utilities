package net.unfamily.iskautils.client.gui;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.util.LabelingNameStyle;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Portable Labeling Machine menu: one target slot + player inventory (shop layout).
 * Locks only the player-band slot of the tool that opened the GUI (Settings Copier pattern).
 */
public class LabelingMachineMenu extends AbstractContainerMenu {

    public static final int TARGET_SLOT_X = 20;
    public static final int TARGET_SLOT_Y = 28;
    public static final int INVENTORY_Y = 154;
    public static final int HOTBAR_Y = 212;

    /** Player band: main 0–26 then hotbar 27–35 (same order as slots below). */
    public static final int PLAYER_BAND_SIZE = 36;
    public static final int PLAYER_SLOT_START = 1;

    private final ItemStackHandler targetHandler = new ItemStackHandler(1);
    private final InteractionHand hand;
    /**
     * Index 0–35 within the player band; {@code -1} if the tool is not in that band (e.g. off-hand).
     */
    private final int openingToolBandSlot;
    private boolean colorPickerOpen;

    public static LabelingMachineMenu createClient(int containerId, Inventory playerInventory, FriendlyByteBuf extra) {
        int ho = extra.readByte() & 0xFF;
        InteractionHand hand = ho == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        int bandSlot = extra.readVarInt();
        return new LabelingMachineMenu(containerId, playerInventory, hand, bandSlot);
    }

    /** Server menu (resolves locked player-band slot locally). */
    public LabelingMachineMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        this(containerId, playerInventory, hand, Integer.MIN_VALUE);
    }

    private LabelingMachineMenu(
            int containerId, Inventory playerInventory, InteractionHand hand, int openingBandSlotFromSync) {
        super(ModMenuTypes.LABELING_MACHINE_MENU.get(), containerId);
        this.hand = hand;
        this.openingToolBandSlot = openingBandSlotFromSync != Integer.MIN_VALUE
                ? openingBandSlotFromSync
                : resolveOpeningToolMenuSlot(playerInventory, hand, playerInventory.player);

        this.addSlot(new SlotItemHandler(targetHandler, 0, TARGET_SLOT_X, TARGET_SLOT_Y) {
            @Override
            public boolean isActive() {
                return !colorPickerOpen;
            }
        });

        int bandSlot = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int invIndex = col + row * 9 + 9;
                int xPos = 20 + col * 18;
                int yPos = INVENTORY_Y + row * 18;
                this.addSlot(new LockedToolPlayerSlot(
                        playerInventory, invIndex, xPos, yPos, bandSlot == openingToolBandSlot));
                bandSlot++;
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new LockedToolPlayerSlot(
                    playerInventory, col, 20 + col * 18, HOTBAR_Y, bandSlot == openingToolBandSlot));
            bandSlot++;
        }
    }

    /**
     * Index 0–35 within the player band (main 0–26, hotbar 27–35).
     * {@code -1} if the tool is not in that band (e.g. off-hand).
     */
    public static int resolveOpeningToolMenuSlot(Inventory inv, InteractionHand openHand, Player player) {
        if (openHand != InteractionHand.MAIN_HAND) {
            return -1;
        }
        ItemStack held = player.getItemInHand(openHand);
        if (held.isEmpty() || !held.is(ModItems.LABELING_MACHINE.get())) {
            return -1;
        }
        int selected = inv.selected;
        if (selected >= 0
                && selected < inv.getContainerSize()
                && ItemStack.isSameItemSameComponents(held, inv.getItem(selected))) {
            return menuSlotIndexForPlayerInventoryIndex(selected);
        }
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (ItemStack.isSameItemSameComponents(held, inv.getItem(i))) {
                return menuSlotIndexForPlayerInventoryIndex(i);
            }
        }
        return menuSlotIndexForPlayerInventoryIndex(selected);
    }

    static int menuSlotIndexForPlayerInventoryIndex(int playerInvIndex) {
        if (playerInvIndex >= 9 && playerInvIndex < 36) {
            return playerInvIndex - 9;
        }
        if (playerInvIndex >= 0 && playerInvIndex < 9) {
            return 27 + playerInvIndex;
        }
        return -1;
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

    public boolean applyFormattedName(List<LabelingNameStyle.Segment> segments, Player player) {
        ItemStack stack = targetHandler.getStackInSlot(0);
        if (stack.isEmpty()) {
            return false;
        }
        List<LabelingNameStyle.Segment> toApply = segments;
        if (LabelingNameStyle.shouldForceItalicFor(player)) {
            List<LabelingNameStyle.Segment> forced = new java.util.ArrayList<>();
            if (segments != null) {
                for (LabelingNameStyle.Segment s : segments) {
                    if (s != null) {
                        forced.add(s.copy());
                    }
                }
            }
            LabelingNameStyle.forceItalicOnSegments(forced);
            toApply = forced;
        }
        Component name = LabelingNameStyle.buildName(toApply);
        if (name.getString().isEmpty()) {
            stack.remove(DataComponents.CUSTOM_NAME);
        } else {
            stack.set(DataComponents.CUSTOM_NAME, name);
        }
        targetHandler.setStackInSlot(0, stack);
        broadcastChanges();
        return true;
    }

    public boolean applyFormattedLore(List<List<LabelingNameStyle.Segment>> lines) {
        ItemStack stack = targetHandler.getStackInSlot(0);
        if (stack.isEmpty()) {
            return false;
        }
        List<Component> components = LabelingNameStyle.buildLoreComponents(lines);
        if (components.isEmpty()) {
            stack.remove(DataComponents.LORE);
        } else {
            stack.set(DataComponents.LORE, new ItemLore(components));
        }
        targetHandler.setStackInSlot(0, stack);
        broadcastChanges();
        return true;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        ItemStack stack = player.getItemInHand(hand);
        return player.isAlive() && stack.is(ModItems.LABELING_MACHINE.get());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        int lockedMenu = openingToolBandSlot >= 0 ? openingToolBandSlot + PLAYER_SLOT_START : -1;
        if (index == lockedMenu) {
            return ItemStack.EMPTY;
        }
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index == 0) {
            if (!this.moveItemStackTo(stack, PLAYER_SLOT_START, PLAYER_SLOT_START + PLAYER_BAND_SIZE, true)) {
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
        if (!player.level().isClientSide) {
            ItemStack leftover = targetHandler.getStackInSlot(0);
            if (!leftover.isEmpty()) {
                if (!player.getInventory().add(leftover.copy())) {
                    player.drop(leftover.copy(), false);
                }
                targetHandler.setStackInSlot(0, ItemStack.EMPTY);
            }
        }
    }

    /** Blocks pickup/place on the menu slot that held the opening Labeling Machine. */
    private final class LockedToolPlayerSlot extends Slot {
        private final boolean toolLocked;

        LockedToolPlayerSlot(Inventory inv, int index, int x, int y, boolean toolLocked) {
            super(inv, index, x, y);
            this.toolLocked = toolLocked;
        }

        @Override
        public boolean isActive() {
            return !colorPickerOpen;
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return !toolLocked && super.mayPickup(player);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return !toolLocked && super.mayPlace(stack);
        }
    }
}
