package net.unfamily.iskautils.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
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
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation still = extensions.getStillTexture(fluid);
        if (still == null) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(still);
        int tint = extensions.getTintColor(fluid);
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
    }
}
