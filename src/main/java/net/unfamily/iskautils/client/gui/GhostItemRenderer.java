package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;

/**
 * Renders GUI item previews with configurable ARGB overlay (26.x: deferred via {@link GuiGraphicsExtractor}).
 */
public final class GhostItemRenderer {

    private GhostItemRenderer() {}

    public static void render(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int argbColor) {
        if (stack.isEmpty()) {
            return;
        }

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.item(stack, 0, 0);
        if (ARGB.alpha(argbColor) != 0) {
            graphics.fill(0, 0, 16, 16, argbColor);
        }
        graphics.pose().popMatrix();
    }
}
