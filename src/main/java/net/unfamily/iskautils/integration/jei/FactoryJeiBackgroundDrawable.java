package net.unfamily.iskautils.integration.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.unfamily.iskautils.IskaUtils;

/** JEI background: input slot (left) and fixed output grid (same slot texture as in-game GUIs). */
public final class FactoryJeiBackgroundDrawable implements IDrawable {

    public static final Identifier SLOT_TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/single_slot.png");

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
    public void draw(GuiGraphicsExtractor guiGraphics, int xOffset, int yOffset) {
        blitSlot(guiGraphics, xOffset + INPUT_X, yOffset + INPUT_Y);
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int x = xOffset + GRID_X + col * SLOT_SIZE;
                int y = yOffset + GRID_Y + row * SLOT_SIZE;
                blitSlot(guiGraphics, x, y);
            }
        }
    }

    private static void blitSlot(GuiGraphicsExtractor guiGraphics, int x, int y) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, x, y, 0.0F, 0.0F, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
    }
}
