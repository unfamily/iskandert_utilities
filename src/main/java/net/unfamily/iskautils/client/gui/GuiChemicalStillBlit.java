package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.unfamily.iskautils.integration.mekanism.MekChemicalHelper;

/**
 * Draws a 16×16 Mekanism chemical preview in GUI slots (block atlas + tint).
 */
public final class GuiChemicalStillBlit {

    private GuiChemicalStillBlit() {}

    public static void blit16(GuiGraphics graphics, Object chemicalStack, int x, int y) {
        if (chemicalStack == null || MekChemicalHelper.isEmpty(chemicalStack)) {
            return;
        }
        TextureAtlasSprite sprite = FluidRenderHelper.resolveChemicalSprite(chemicalStack);
        int tint = MekChemicalHelper.getTint(chemicalStack);
        if (sprite == null) {
            blitTintQuadFallback(graphics, tint, x, y);
            return;
        }
        float a = ((tint >> 24) & 0xFF) / 255f;
        if (a <= 1e-3f) {
            a = 1f;
        }
        float r = ((tint >> 16) & 0xFF) / 255f;
        float g = ((tint >> 8) & 0xFF) / 255f;
        float b = (tint & 0xFF) / 255f;
        // POSITION_TEX_COLOR — setColor/setShaderColor do not tint plain blit(sprite) reliably
        graphics.blit(x, y, 0, 16, 16, sprite, r, g, b, a);
    }

    private static void blitTintQuadFallback(GuiGraphics graphics, int tint, int x, int y) {
        if (tint == 0) {
            tint = 0xFFB8B8B8;
        }
        int r = (tint >> 16) & 0xFF;
        int g = (tint >> 8) & 0xFF;
        int b = tint & 0xFF;
        graphics.fill(x, y, x + 16, y + 16, 0xFF000000 | (r << 16) | (g << 8) | b);
    }
}
