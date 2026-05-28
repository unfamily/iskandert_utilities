package net.unfamily.iskautils.integration.jei;

import com.mojang.blaze3d.systems.RenderSystem;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.iskautils.IskaUtils;

public final class AncientTabletJeiBackgroundDrawable implements IDrawable {

    public static final ResourceLocation SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/single_slot.png");

    public static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/jei_arrow.png");

    public static final int SLOT_SIZE = 18;
    public static final int COLS = 3;
    public static final int ROWS = 6;
    public static final int MAX_SLOTS = 18;
    public static final int INPUT_X = 6;
    public static final int ARROW_X = INPUT_X + COLS * SLOT_SIZE + 6;
    public static final int OUTPUT_X = ARROW_X + 16 + 6;
    public static final int GRID_Y = 6;
    public static final int ITEM_OFFSET = 1;
    public static final int WARN_Y = GRID_Y + ROWS * SLOT_SIZE + 4;
    private static final int ARROW_W = 24;
    private static final int ARROW_H = 17;

    private final int width;
    private final int height;

    public AncientTabletJeiBackgroundDrawable(int width, int height) {
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
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        RenderSystem.enableBlend();
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int xIn = INPUT_X + col * SLOT_SIZE;
                int y = GRID_Y + row * SLOT_SIZE;
                graphics.blit(SLOT_TEXTURE, xOffset + xIn, yOffset + y, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
                int xOut = OUTPUT_X + col * SLOT_SIZE;
                graphics.blit(SLOT_TEXTURE, xOffset + xOut, yOffset + y, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
            }
        }
        RenderSystem.disableBlend();

        // Arrow between grids.
        int ax = xOffset + ARROW_X - 4;
        int ay = yOffset + GRID_Y + (ROWS * SLOT_SIZE / 2) - (ARROW_H / 2);
        graphics.blit(ARROW_TEXTURE, ax, ay, 0, 0, ARROW_W, ARROW_H, ARROW_W, ARROW_H);
    }
}
