package net.unfamily.iskautils.integration.jei;

import com.mojang.blaze3d.systems.RenderSystem;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.iskautils.IskaUtils;

public final class AncientTabletJeiBackgroundDrawable implements IDrawable {

    public static final ResourceLocation SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/single_slot.png");

    public static final int SLOT_SIZE = 18;
    public static final int COLS = 6;
    public static final int ROWS = 3;
    public static final int MAX_SLOTS = 18;
    public static final int INPUT_X = 6;
    public static final int OUTPUT_X = 98;
    public static final int GRID_Y = 6;
    public static final int ITEM_OFFSET = 1;
    public static final int WARN_Y = GRID_Y + ROWS * SLOT_SIZE + 4;

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
    }
}
