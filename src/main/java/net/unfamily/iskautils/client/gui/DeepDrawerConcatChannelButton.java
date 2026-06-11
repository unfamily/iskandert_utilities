package net.unfamily.iskautils.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.unfamily.iskautils.util.DeepDrawerFilterConcatChannel;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntConsumer;

/** Per filter row: left-click cycles forward, right-click backward (None through A–Z). */
public final class DeepDrawerConcatChannelButton extends AbstractWidget {
    private int value;
    private final @Nullable IntConsumer onChanged;

    public DeepDrawerConcatChannelButton(int x, int y, int width, int height, @Nullable IntConsumer onChanged) {
        super(x, y, width, height, Component.empty());
        this.onChanged = onChanged;
    }

    public int getChannelOrdinal() {
        return value;
    }

    public void setChannelOrdinal(int ordinal) {
        value = Math.clamp(ordinal, 0, DeepDrawerFilterConcatChannel.MAX_LETTER);
    }

    @Override
    protected boolean isValidClickButton(MouseButtonInfo buttonInfo) {
        return buttonInfo.button() == 0 || buttonInfo.button() == 1;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (event.buttonInfo().button() == 0) {
            setChannelOrdinal(DeepDrawerFilterConcatChannel.fromOrdinal(value).next().ordinal());
        } else {
            setChannelOrdinal(DeepDrawerFilterConcatChannel.fromOrdinal(value).previous().ordinal());
        }
        if (onChanged != null) {
            onChanged.accept(value);
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int bg = value == 0 ? DeepDrawerFilterLetterPalette.backgroundArgb(0) : DeepDrawerFilterLetterPalette.backgroundArgb(value);
        graphics.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, bg);

        int border = isHovered() ? 0xFFFFFFFF : 0xFF303030;
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + 1, border);
        graphics.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), border);
        graphics.fill(getX(), getY(), getX() + 1, getY() + getHeight(), border);
        graphics.fill(getX() + getWidth() - 1, getY(), getX() + getWidth(), getY() + getHeight(), border);

        if (value > 0) {
            String label = String.valueOf((char) ('A' + value - 1));
            int textColor = DeepDrawerFilterLetterPalette.textArgb(value);
            int lw = Minecraft.getInstance().font.width(label);
            graphics.text(Minecraft.getInstance().font, label,
                    getX() + (getWidth() - lw) / 2,
                    getY() + (getHeight() - 8) / 2,
                    textColor,
                    false);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
