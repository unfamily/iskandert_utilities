package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Helper for semi-transparent ghost items in machine GUIs.
 */
public final class GuiGhostItem {

    public static final int DEFAULT_ARGB = 0x80FFFFFF;

    private GuiGhostItem() {}

    public static void render(GuiGraphicsExtractor graphics, int leftPos, int topPos, Slot slot, ItemStack ghostStack) {
        render(graphics, leftPos, topPos, slot, ghostStack, DEFAULT_ARGB);
    }

    public static void render(GuiGraphicsExtractor graphics, int leftPos, int topPos, Slot slot, ItemStack ghostStack, int argbColor) {
        if (slot == null || !slot.getItem().isEmpty() || ghostStack.isEmpty()) {
            return;
        }
        GhostItemRenderer.render(graphics, ghostStack, leftPos + slot.x, topPos + slot.y, argbColor);
    }

    public static void renderCycling(
            GuiGraphicsExtractor graphics,
            int leftPos,
            int topPos,
            Slot slot,
            List<ItemStack> cycleStacks,
            GuiCycleTimer timer,
            int argbColor) {
        if (slot == null || !slot.getItem().isEmpty() || cycleStacks.isEmpty()) {
            return;
        }
        timer.onDraw();
        ItemStack stack = timer.getOrDefault(cycleStacks, cycleStacks.get(0));
        GhostItemRenderer.render(graphics, stack, leftPos + slot.x, topPos + slot.y, argbColor);
    }
}
