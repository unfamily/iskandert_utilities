package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.pattern.PatternColors;
import net.unfamily.iskautils.pattern.PatternData;

import java.util.function.Consumer;

/**
 * 16x16 variable filter control: no label text, renders configured item preview.
 * Left-click opens the editor. Right-click cycles the associated pattern letter (color border only).
 */
public class VariableFilterButton extends AbstractWidget {
    private ItemStack preview = ItemStack.EMPTY;
    private int letter = PatternData.EMPTY;
    private final int localIndex;
    private final Consumer<VariableFilterButton> onOpen;
    private final Consumer<VariableFilterButton> onCycleLetter;

    public VariableFilterButton(int x, int y, int localIndex,
                                Consumer<VariableFilterButton> onOpen,
                                Consumer<VariableFilterButton> onCycleLetter) {
        super(x, y, 16, 16, Component.empty());
        this.localIndex = localIndex;
        this.onOpen = onOpen;
        this.onCycleLetter = onCycleLetter;
    }

    public int getLocalIndex() {
        return localIndex;
    }

    public void setPreview(ItemStack stack) {
        this.preview = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    public void setLetter(int letter) {
        this.letter = letter;
    }

    public int getLetter() {
        return letter;
    }

    @Override
    protected boolean isValidClickButton(MouseButtonInfo buttonInfo) {
        return buttonInfo.button() == 0 || buttonInfo.button() == 1;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (event.buttonInfo().button() == 0) {
            if (onOpen != null) onOpen.accept(this);
        } else if (event.buttonInfo().button() == 1) {
            if (onCycleLetter != null) onCycleLetter.accept(this);
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        int fill = letter > PatternData.EMPTY ? PatternColors.getColor(letter) : 0xFF8B8B8B;
        int border = isHovered() ? 0xFFFFFFFF : 0xFF555555;
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, fill);
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + 1, border);
        guiGraphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, border);
        guiGraphics.fill(getX(), getY(), getX() + 1, getY() + height, border);
        guiGraphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, border);
        if (!preview.isEmpty()) {
            guiGraphics.item(preview, getX(), getY());
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}
