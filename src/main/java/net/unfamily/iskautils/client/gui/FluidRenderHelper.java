package net.unfamily.iskautils.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
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

    public static void renderTank(GuiGraphics graphics, int x, int y, int width, int height,
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

    public static void renderChemicalTank(GuiGraphics graphics, int x, int y, int width, int height,
                                          @Nullable String chemicalId, long amount, long capacity) {
        if (chemicalId == null || chemicalId.isBlank() || amount <= 0 || capacity <= 0
                || width <= 0 || height <= 0 || !MekChemicalHelper.isLoaded()) {
            return;
        }
        int fillHeight = (int) Math.min(height, Math.max(1L, amount * height / capacity));
        int fillTop = y + height - fillHeight;
        Object stack = MekChemicalHelper.createStackFromId(chemicalId.trim(), Math.max(1L, amount));
        if (stack == null || MekChemicalHelper.isEmpty(stack)) {
            return;
        }
        if (drawMekanismTiledGauge(graphics, stack, x, y, width, height, fillHeight)) {
            return;
        }
        TextureAtlasSprite sprite = resolveChemicalSprite(stack);
        int tint = MekChemicalHelper.getTint(stack);
        if (sprite == null) {
            int rgb = tint == 0 ? 0x66DDEE : (tint & 0x00FFFFFF);
            graphics.fill(x, fillTop, x + width, y + height, 0xFF000000 | rgb);
            return;
        }
        float r = ((tint >> 16) & 0xFF) / 255f;
        float g = ((tint >> 8) & 0xFF) / 255f;
        float b = (tint & 0xFF) / 255f;
        drawTiledSpriteBottomUp(graphics, sprite, x, fillTop, width, fillHeight, r, g, b);
    }

    public static void drawFluidInTank(GuiGraphics guiGraphics, FluidStack fluidStack, int x, int y, int width, int height) {
        if (fluidStack.isEmpty() || fluidStack.getFluid() == Fluids.EMPTY || width <= 0 || height <= 0) {
            return;
        }
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluidStack.getFluid());
        ResourceLocation stillTexture = ext.getStillTexture(fluidStack);
        if (stillTexture == null) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(stillTexture);
        int tint = ext.getTintColor(fluidStack);
        float r = ((tint >> 16) & 0xFF) / 255f;
        float g = ((tint >> 8) & 0xFF) / 255f;
        float b = (tint & 0xFF) / 255f;
        drawTiledSpriteBottomUp(guiGraphics, sprite, x, y, width, height, r, g, b);
    }

    private static void drawTiledSpriteBottomUp(GuiGraphics guiGraphics, TextureAtlasSprite sprite,
                                                int x, int y, int width, int height,
                                                float r, float g, float b) {
        int remainingHeight = height;
        int tileBottom = y + height;
        while (remainingHeight > 0) {
            int tileH = Math.min(TILE_SIZE, remainingHeight);
            tileBottom -= tileH;
            for (int dx = 0; dx < width; dx += TILE_SIZE) {
                int tileW = Math.min(TILE_SIZE, width - dx);
                int tileX = x + dx;
                guiGraphics.enableScissor(tileX, tileBottom, tileX + tileW, tileBottom + tileH);
                guiGraphics.blit(tileX, tileBottom, 0, TILE_SIZE, TILE_SIZE, sprite, r, g, b, 1f);
                guiGraphics.disableScissor();
            }
            remainingHeight -= tileH;
        }
    }

    @Nullable
    static TextureAtlasSprite resolveChemicalSprite(Object chemicalStack) {
        try {
            Class<?> renderer = Class.forName("mekanism.client.render.MekanismRenderer");
            Object sprite = renderer.getMethod("getChemicalTexture", chemicalStack.getClass())
                    .invoke(null, chemicalStack);
            if (sprite instanceof TextureAtlasSprite atlasSprite) {
                return atlasSprite;
            }
        } catch (Throwable ignored) {
        }
        try {
            Object chemical = chemicalStack.getClass().getMethod("getChemical").invoke(chemicalStack);
            if (chemical == null) {
                return null;
            }
            ResourceLocation icon = (ResourceLocation) chemical.getClass().getMethod("getIcon").invoke(chemical);
            if (icon == null) {
                return null;
            }
            return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(icon);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean drawMekanismTiledGauge(GuiGraphics graphics, Object stack,
                                                  int x, int y, int width, int height, int fillPx) {
        try {
            Class<?> renderer = Class.forName("mekanism.client.render.MekanismRenderer");
            TextureAtlasSprite sprite = resolveChemicalSprite(stack);
            if (sprite == null) {
                return false;
            }
            Class<?> guiUtils = Class.forName("mekanism.client.gui.GuiUtils");
            Class<?> tilingDir = Class.forName("mekanism.client.gui.GuiUtils$TilingDirection");
            Object upRight = Enum.valueOf(tilingDir.asSubclass(Enum.class), "UP_RIGHT");
            renderer.getMethod("color", GuiGraphics.class, stack.getClass()).invoke(null, graphics, stack);
            guiUtils.getMethod(
                    "drawTiledSprite",
                    GuiGraphics.class,
                    int.class, int.class, int.class, int.class, int.class,
                    TextureAtlasSprite.class,
                    int.class, int.class, int.class,
                    tilingDir, boolean.class
            ).invoke(null, graphics, x, y, height, width, Math.min(fillPx, height), sprite,
                    TILE_SIZE, TILE_SIZE, 100, upRight, true);
            renderer.getMethod("resetColor", GuiGraphics.class).invoke(null, graphics);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
