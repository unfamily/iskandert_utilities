package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ARGB;
import net.unfamily.iskautils.integration.mekanism.MekChemicalHelper;

/**
 * Draws a 16×16 Mekanism chemical preview in GUI slots (block atlas + tint).
 */
public final class GuiChemicalStillBlit {

    private GuiChemicalStillBlit() {}

    public static void blit16(GuiGraphicsExtractor graphics, Object chemicalStack, int x, int y) {
        if (chemicalStack == null || MekChemicalHelper.isEmpty(chemicalStack)) {
            return;
        }
        TextureAtlasSprite sprite = FluidRenderHelper.resolveChemicalSprite(chemicalStack);
        int tint = MekChemicalHelper.getTint(chemicalStack);
        if (sprite == null) {
            blitTintQuadFallback(graphics, tint, x, y);
            return;
        }
        int alpha = (tint >> 24) & 0xFF;
        if (alpha == 0) {
            alpha = 0xFF;
        }
        int color = ARGB.color(alpha, (tint >> 16) & 0xFF, (tint >> 8) & 0xFF, tint & 0xFF);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 16, 16, color);
    }

    private static void blitTintQuadFallback(GuiGraphicsExtractor graphics, int tint, int x, int y) {
        if (tint == 0) {
            tint = 0xFFB8B8B8;
        }
        int r = (tint >> 16) & 0xFF;
        int g = (tint >> 8) & 0xFF;
        int b = tint & 0xFF;
        graphics.fill(x, y, x + 16, y + 16, 0xFF000000 | (r << 16) | (g << 8) | b);
    }
}
