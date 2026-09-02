package net.unfamily.iskautils.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.fluids.FluidStack;
import net.unfamily.iskautils.integration.mekanism.MekChemicalHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Draws fluid/chemical fill in AutoShop tank bars using still textures tiled from the bottom
 * (Colossal-Reactors style). Empty tanks leave the GUI texture visible.
 */
public final class FluidRenderHelper {

    private static final int TILE_SIZE = 16;

    private FluidRenderHelper() {}

    public static void renderTank(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                                  int fluidRegistryId, long amount, long capacity) {
        if (fluidRegistryId < 0 || amount <= 0 || capacity <= 0 || width <= 0 || height <= 0) {
            return;
        }
        Fluid fluid = BuiltInRegistries.FLUID.byId(fluidRegistryId);
        if (fluid == null || fluid == Fluids.EMPTY) {
            return;
        }
        int fillHeight = (int) Math.min(height, Math.max(1L, amount * height / capacity));
        int fillTop = y + height - fillHeight;
        drawFluidInTank(graphics, new FluidStack(fluid, (int) Math.min(Integer.MAX_VALUE, Math.max(1L, amount))),
                x, fillTop, width, fillHeight);
    }

    public static void renderChemicalTank(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                                          @Nullable String chemicalId, long amount, long capacity) {
        if (chemicalId == null || chemicalId.isBlank() || amount <= 0 || capacity <= 0
                || width <= 0 || height <= 0 || !MekChemicalHelper.isLoaded()) {
            return;
        }
        int fillHeight = (int) Math.min(height, Math.max(1L, amount * height / capacity));
        int fillTop = y + height - fillHeight;
        Object stack = MekChemicalHelper.createStackFromId(chemicalId.trim(), Math.max(1L, amount));
        TextureAtlasSprite sprite = resolveChemicalSprite(stack);
        int tint = MekChemicalHelper.getTint(stack);
        if (sprite == null) {
            int rgb = tint == 0 ? 0x66DDEE : (tint & 0x00FFFFFF);
            graphics.fill(x, fillTop, x + width, y + height, 0xFF000000 | rgb);
            return;
        }
        int alpha = (tint >> 24) & 0xFF;
        if (alpha == 0) {
            alpha = 0xFF;
        }
        int color = ARGB.color(alpha, (tint >> 16) & 0xFF, (tint >> 8) & 0xFF, tint & 0xFF);
        drawTiledSpriteBottomUp(graphics, sprite, x, fillTop, width, fillHeight, color);
    }

    public static void drawFluidInTank(GuiGraphicsExtractor guiGraphics, FluidStack fluidStack,
                                       int x, int y, int width, int height) {
        if (fluidStack.isEmpty() || fluidStack.getFluid() == Fluids.EMPTY || width <= 0 || height <= 0) {
            return;
        }
        Fluid fluid = fluidStack.getFluid();
        FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());
        TextureAtlasSprite sprite = model.stillMaterial().sprite();
        FluidTintSource tintSource = model.fluidTintSource();
        int tint = tintSource != null ? tintSource.colorAsStack(fluidStack) : 0xFFFFFFFF;
        int alpha = (tint >> 24) & 0xFF;
        if (alpha == 0) {
            alpha = 0xFF;
        }
        int color = ARGB.color(alpha, (tint >> 16) & 0xFF, (tint >> 8) & 0xFF, tint & 0xFF);
        drawTiledSpriteBottomUp(guiGraphics, sprite, x, y, width, height, color);
    }

    private static void drawTiledSpriteBottomUp(GuiGraphicsExtractor guiGraphics, TextureAtlasSprite sprite,
                                                int x, int y, int width, int height, int color) {
        int remainingHeight = height;
        int tileBottom = y + height;
        while (remainingHeight > 0) {
            int tileH = Math.min(TILE_SIZE, remainingHeight);
            tileBottom -= tileH;
            for (int dx = 0; dx < width; dx += TILE_SIZE) {
                int tileW = Math.min(TILE_SIZE, width - dx);
                int tileX = x + dx;
                guiGraphics.enableScissor(tileX, tileBottom, tileX + tileW, tileBottom + tileH);
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, tileX, tileBottom, TILE_SIZE, TILE_SIZE, color);
                guiGraphics.disableScissor();
            }
            remainingHeight -= tileH;
        }
    }

    @Nullable
    static TextureAtlasSprite resolveChemicalSprite(@Nullable Object chemicalStack) {
        if (chemicalStack == null || MekChemicalHelper.isEmpty(chemicalStack)) {
            return null;
        }
        try {
            Object chemical = chemicalStack.getClass().getMethod("getChemical").invoke(chemicalStack);
            if (chemical == null) {
                return null;
            }
            Identifier icon = (Identifier) chemical.getClass().getMethod("getIcon").invoke(chemical);
            if (icon == null) {
                return null;
            }
            return Minecraft.getInstance().getAtlasManager().get(new SpriteId(TextureAtlas.LOCATION_BLOCKS, icon));
        } catch (Throwable ignored) {
            return null;
        }
    }
}
