package net.unfamily.iskautils.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.unfamily.iskautils.pattern.PatternColors;

/**
 * Pattern-letter style tag for shop browse rows: colored cell with {@code C} / {@code E}.
 * Uses PatternColors A (red) for categories and B (green) for entries.
 */
public class ShopKindTagButton extends AbstractWidget {
    private static final int TEXT_COLOR = 0xFF000000;

    /** PatternColors index for letter A (red) — category tag. */
    public static final int LETTER_CATEGORY = 1;
    /** PatternColors index for letter B (green) — entry tag. */
    public static final int LETTER_ENTRY = 2;

    private final int letter;
    private final Runnable onPress;

    public ShopKindTagButton(int x, int y, int width, int height, int letter, Runnable onPress) {
        super(x, y, width, height, Component.literal(labelFor(letter)));
        this.letter = letter;
        this.onPress = onPress;
        setTooltip(kindTooltip(letter));
    }

    public static String labelFor(int letter) {
        return switch (letter) {
            case LETTER_CATEGORY -> "C";
            case LETTER_ENTRY -> "E";
            default -> "?";
        };
    }

    private static Tooltip kindTooltip(int letter) {
        if (letter == LETTER_CATEGORY) {
            return Tooltip.create(CommonComponents.joinLines(
                    Component.translatable("gui.iska_utils.shop_edit.kind.category"),
                    Component.translatable("gui.iska_utils.shop_edit.kind.category.desc")));
        }
        return Tooltip.create(CommonComponents.joinLines(
                Component.translatable("gui.iska_utils.shop_edit.kind.entry"),
                Component.translatable("gui.iska_utils.shop_edit.kind.entry.desc")));
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (event.buttonInfo().button() == 0 && onPress != null) {
            onPress.run();
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int fill = PatternColors.getColor(letter);
        int border = isHovered() ? 0xFFFFFFFF : 0xFF555555;
        graphics.fill(getX(), getY(), getX() + width, getY() + height, fill);
        graphics.fill(getX(), getY(), getX() + width, getY() + 1, border);
        graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, border);
        graphics.fill(getX(), getY(), getX() + 1, getY() + height, border);
        graphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, border);
        var font = Minecraft.getInstance().font;
        Component label = getMessage();
        int lw = font.width(label);
        graphics.text(font, label,
                getX() + (width - lw) / 2,
                getY() + (height - 8) / 2,
                TEXT_COLOR,
                false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}
