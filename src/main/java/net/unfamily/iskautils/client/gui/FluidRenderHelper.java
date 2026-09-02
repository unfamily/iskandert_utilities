package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Draws fluid fill in AutoShop tank bars. Empty tanks leave the GUI texture visible.
 */
public final class FluidRenderHelper {

    private FluidRenderHelper() {}

    public static void renderTank(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                                  int fluidRegistryId, long amount, long capacity) {
        if (fluidRegistryId < 0 || amount <= 0 || capacity <= 0) {
            return;
        }
        Fluid fluid = BuiltInRegistries.FLUID.byId(fluidRegistryId);
        if (fluid == null || fluid == Fluids.EMPTY) {
            return;
        }
        int fillHeight = (int) Math.min(height, Math.max(1L, amount * height / capacity));
        int rgb = 0x4060A0 | (fluidRegistryId * 0x45D9F3B & 0x003F7F7F);
        graphics.fill(x, y + height - fillHeight, x + width, y + height, 0xFF000000 | rgb);
    }
}
