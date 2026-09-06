package net.unfamily.iskautils.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.pattern.PatternColors;
import net.unfamily.iskautils.pattern.PatternData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Custom widget for a pattern grid cell.
 * Displays a colored square with a letter (A-R) or empty.
 * Left-click cycles forward, right-click cycles backward.
 */
public class PatternCellWidget extends AbstractWidget {
    private int value = PatternData.EMPTY;
    private int maxLetter = PatternData.MAX_LETTER; // from BE getMaxKeyInputs(); cycle uses this
    private final int cellIndex;
    private final Consumer<PatternCellWidget> onPress;
    private final Consumer<ItemStack> onItemAssign;
    private List<ItemStack> displayCandidates = List.of();

    public PatternCellWidget(int x, int y, int width, int height, int cellIndex, Consumer<PatternCellWidget> onPress) {
        this(x, y, width, height, cellIndex, onPress, null);
    }

    public PatternCellWidget(int x, int y, int width, int height, int cellIndex,
                             Consumer<PatternCellWidget> onPress, Consumer<ItemStack> onItemAssign) {
        super(x, y, width, height, Component.empty());
        this.cellIndex = cellIndex;
        this.onPress = onPress;
        this.onItemAssign = onItemAssign;
    }

    public int getValue() {
        return value;
    }

    public int getCellIndex() {
        return cellIndex;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public void setMaxLetter(int maxLetter) {
        this.maxLetter = Math.max(1, Math.min(PatternData.MAX_LETTER, maxLetter));
    }

    public void setDisplayItems(List<ItemStack> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            displayCandidates = List.of();
            return;
        }
        List<ItemStack> copies = new ArrayList<>(candidates.size());
        for (ItemStack candidate : candidates) {
            if (!candidate.isEmpty()) copies.add(candidate.copyWithCount(1));
        }
        displayCandidates = List.copyOf(copies);
    }

    /**
     * Cycles the value forward: empty -> 1 -> ... -> maxLetter -> empty
     */
    public int cycleForward() {
        value = value >= maxLetter ? PatternData.EMPTY : value + 1;
        return value;
    }

    /**
     * When true, clicks only notify {@code onPress} (no cycle / clear / item assign).
     * Used for Pattern Crafter variable buttons that open a SubView editor.
     */
    private boolean openOnly;

    public void setOpenOnly(boolean openOnly) {
        this.openOnly = openOnly;
    }

    /**
     * Cycles the value backward: empty -> maxLetter -> ... -> 1 -> empty
     */
    public int cycleBackward() {
        value = value <= PatternData.EMPTY ? maxLetter : value - 1;
        return value;
    }

    @Override
    protected boolean isValidClickButton(int button) {
        return button == 0 || button == 1; // Accept left and right click
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.active && this.visible && this.isValidClickButton(button)) {
            if (this.clicked(mouseX, mouseY)) {
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                if (openOnly) {
                    onPress.accept(this);
                    return true;
                }
                ItemStack carried = Minecraft.getInstance().player != null
                        ? Minecraft.getInstance().player.containerMenu.getCarried() : ItemStack.EMPTY;
                if (button == 0 && !Screen.hasShiftDown() && !carried.isEmpty() && onItemAssign != null) {
                    onItemAssign.accept(carried.copyWithCount(1));
                    return true;
                } else if (Screen.hasShiftDown()) {
                    // Shift+click: reset to empty
                    value = PatternData.EMPTY;
                } else if (button == 0) {
                    cycleForward();
                } else if (button == 1) {
                    cycleBackward();
                }
                onPress.accept(this);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        boolean showItem = value > PatternData.EMPTY && !displayCandidates.isEmpty();
        int bgColor = showItem ? 0xFF8B8B8B : PatternColors.getColor(value);

        // Draw filled background (inside border)
        guiGraphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, bgColor);

        // Draw border
        int borderColor = isHovered ? 0xFFFFFFFF : 0xFF303030;
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + 1, borderColor);                // top
        guiGraphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, borderColor); // bottom
        guiGraphics.fill(getX(), getY(), getX() + 1, getY() + height, borderColor);                // left
        guiGraphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, borderColor); // right

        if (showItem) {
            int index = (int) ((System.currentTimeMillis() / 1000L) % displayCandidates.size());
            guiGraphics.renderItem(displayCandidates.get(index), getX() + (width - 16) / 2, getY() + (height - 16) / 2);

            // Badge above item; letter above badge (item blit uses elevated z).
            int badgeSize = 7;
            int badgeX = getX() + width - badgeSize - 1;
            int badgeY = getY() + height - badgeSize - 1;
            int border = 0xFF202020;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 200);
            guiGraphics.fill(badgeX, badgeY, badgeX + badgeSize, badgeY + badgeSize, border);
            guiGraphics.fill(badgeX + 1, badgeY + 1, badgeX + badgeSize - 1, badgeY + badgeSize - 1,
                    PatternColors.getColor(value));
            guiGraphics.pose().translate(0, 0, 10);
            String label = PatternData.letterValueToDisplayString(value);
            var font = Minecraft.getInstance().font;
            float scale = 0.5f;
            int labelW = font.width(label);
            int labelH = font.lineHeight;
            float inner = badgeSize - 2;
            float textX = badgeX + 1 + (inner - labelW * scale) / 2f;
            float textY = badgeY + 1 + (inner - labelH * scale) / 2f;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(textX, textY, 0);
            guiGraphics.pose().scale(scale, scale, 1f);
            guiGraphics.drawString(font, label, 0, 0, PatternColors.getTextColor(value), false);
            guiGraphics.pose().popPose();
            guiGraphics.pose().popPose();
        } else if (value > PatternData.EMPTY) {
            String label = PatternData.letterValueToDisplayString(value);
            int textColor = PatternColors.getTextColor(value);
            int labelW = Minecraft.getInstance().font.width(label);
            guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    label,
                    getX() + (width - labelW) / 2,
                    getY() + (height - Minecraft.getInstance().font.lineHeight) / 2,
                    textColor,
                    false
            );
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}
