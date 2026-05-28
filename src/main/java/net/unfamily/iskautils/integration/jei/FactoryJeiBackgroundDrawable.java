package net.unfamily.iskautils.integration.jei;

import com.mojang.blaze3d.systems.RenderSystem;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.iskautils.IskaUtils;

/**
 * JEI background: one input slot and a fixed 9×3 output grid (vanilla-style slot texture).
 * Vertical padding inside the recipe bounds (empty space above first / below last slot row).
 */
public final class FactoryJeiBackgroundDrawable implements IDrawable {

    public static final ResourceLocation SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/single_slot.png");

    public static final int SLOT_SIZE = IskaUtilsGridJeiLayout.SLOT_SIZE;
    public static final int BG_PADDING_V = IskaUtilsGridJeiLayout.BG_PADDING_V;
    public static final int INPUT_X = IskaUtilsGridJeiLayout.INPUT_X;
    public static final int GRID_X = IskaUtilsGridJeiLayout.GRID_X;
    public static final int GRID_Y = IskaUtilsGridJeiLayout.GRID_Y;
    public static final int INPUT_Y = IskaUtilsGridJeiLayout.INPUT_Y;
    public static final int GRID_COLS = IskaUtilsGridJeiLayout.GRID_COLS;
    public static final int GRID_ROWS = IskaUtilsGridJeiLayout.GRID_ROWS;
    public static final int ITEM_OFFSET = IskaUtilsGridJeiLayout.ITEM_OFFSET;

    private final int width;
    private final int height;

    public FactoryJeiBackgroundDrawable(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void draw(GuiGraphics guiGraphics, int xOffset, int yOffset) {
        RenderSystem.enableBlend();
        blitSlot(guiGraphics, xOffset + INPUT_X, yOffset + INPUT_Y);
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int x = xOffset + GRID_X + col * SLOT_SIZE;
                int y = yOffset + GRID_Y + row * SLOT_SIZE;
                blitSlot(guiGraphics, x, y);
            }
        }
        RenderSystem.disableBlend();
    }

    private static void blitSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(SLOT_TEXTURE, x, y, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
    }
}
