package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Draws a 16×16 still-fluid icon in GUI slots (block atlas + fluid tint).
 */
public final class GuiFluidStillBlit {

    private GuiFluidStillBlit() {}

    public static void blit16(GuiGraphics graphics, FluidStack fluid, int x, int y) {
        if (fluid == null || fluid.isEmpty()) {
            return;
        }
        FluidRenderHelper.drawFluidInTank(graphics, fluid, x, y, 16, 16);
    }
}
