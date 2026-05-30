package net.unfamily.iskautils.integration.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ArcaneDictionaryJeiBackgroundDrawable implements IDrawable {

    public static final int SLOT_SIZE = 18;
    public static final int CATALYST_GAP = 2;
    public static final int ITEM_OFFSET = 1;
    public static final int CATALYST_LABEL_GAP = 4;

    private final int width;
    private final int height;

    public ArcaneDictionaryJeiBackgroundDrawable(int width, int height) {
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
    public void draw(GuiGraphicsExtractor graphics, int xOffset, int yOffset) {
        // Text-only layout; catalyst slots are rendered by JEI.
    }
}
