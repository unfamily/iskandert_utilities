package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Button that renders a currency or text symbol centered on the widget.
 */
public class SymbolIconButton extends Button {
    private final Supplier<String> symbolSupplier;

    public SymbolIconButton(
            int x,
            int y,
            int size,
            Button.OnPress onPress,
            Supplier<String> symbolSupplier,
            Component tooltip) {
        this(x, y, size, size, onPress, symbolSupplier, tooltip);
    }

    public SymbolIconButton(
            int x,
            int y,
            int width,
            int height,
            Button.OnPress onPress,
            Supplier<String> symbolSupplier,
            Component tooltip) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.symbolSupplier = symbolSupplier;
        if (!tooltip.getString().isEmpty()) {
            setTooltip(Tooltip.create(tooltip));
        }
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractDefaultSprite(graphics);
        String symbol = symbolSupplier.get();
        if (symbol == null || symbol.isEmpty()) {
            return;
        }
        int textX = getX() + (getWidth() - MinecraftHolder.font().width(symbol)) / 2;
        int textY = getY() + (getHeight() - 8) / 2;
        graphics.text(MinecraftHolder.font(), Component.literal(symbol), textX, textY, GuiTextColors.BUTTON_SYMBOL, false);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    private static final class MinecraftHolder {
        private static net.minecraft.client.gui.Font font() {
            return net.minecraft.client.Minecraft.getInstance().font;
        }
    }
}
