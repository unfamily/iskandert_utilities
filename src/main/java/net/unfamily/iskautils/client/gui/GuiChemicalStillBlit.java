package net.unfamily.iskautils.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
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
        if (blitChemicalIconSprite(graphics, chemicalStack, x, y)) {
            return;
        }
        blitTintQuadFallback(graphics, chemicalStack, x, y);
    }

    private static boolean blitChemicalIconSprite(GuiGraphics graphics, Object chemicalStack, int x, int y) {
        try {
            Object chemical = chemicalStack.getClass().getMethod("getChemical").invoke(chemicalStack);
            if (chemical == null) {
                return false;
            }
            ResourceLocation icon = (ResourceLocation) chemical.getClass().getMethod("getIcon").invoke(chemical);
            if (icon == null) {
                return false;
            }
            int tint = MekChemicalHelper.getTint(chemicalStack);
            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(icon);
            float a = ((tint >> 24) & 0xFF) / 255.0f;
            if (a <= 0.0f) {
                a = 1.0f;
            }
            float r = ((tint >> 16) & 0xFF) / 255.0f;
            float g = ((tint >> 8) & 0xFF) / 255.0f;
            float b = (tint & 0xFF) / 255.0f;
            RenderSystem.setShaderColor(r, g, b, a);
            graphics.blit(x, y, 0, 16, 16, sprite);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void blitTintQuadFallback(GuiGraphics graphics, Object chemicalStack, int x, int y) {
        int tint = MekChemicalHelper.getTint(chemicalStack);
        if (tint == 0) {
            tint = 0xFFB8B8B8;
        }
        int r = (tint >> 16) & 0xFF;
        int g = (tint >> 8) & 0xFF;
        int b = tint & 0xFF;
        graphics.fill(x, y, x + 16, y + 16, 0xFF000000 | (r << 16) | (g << 8) | b);
    }
}
