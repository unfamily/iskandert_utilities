package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Standard NeoForge/vanilla {@link Button} (widget/button sprites) with a centered item or texture icon.
 */
public class ItemIconButton extends Button {
    private final Supplier<ItemStack> iconStack;
    @Nullable
    private final Supplier<Identifier> overlayTexture;

    public ItemIconButton(
            int x,
            int y,
            int size,
            Button.OnPress onPress,
            Supplier<ItemStack> iconStack,
            Component tooltip) {
        this(x, y, size, onPress, iconStack, null, tooltip);
    }

    public ItemIconButton(
            int x,
            int y,
            int size,
            Button.OnPress onPress,
            Supplier<ItemStack> iconStack,
            @Nullable Supplier<Identifier> overlayTexture,
            Component tooltip) {
        super(x, y, size, size, Component.empty(), onPress, DEFAULT_NARRATION);
        this.iconStack = iconStack;
        this.overlayTexture = overlayTexture;
        if (!tooltip.getString().isEmpty()) {
            setTooltip(Tooltip.create(tooltip));
        }
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractDefaultSprite(graphics);
        int iconSize = 12;
        int ix = getX() + (getWidth() - iconSize) / 2;
        int iy = getY() + (getHeight() - iconSize) / 2;
        Identifier texture = overlayTexture != null ? overlayTexture.get() : null;
        if (texture != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, ix, iy, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
            return;
        }
        ItemStack stack = iconStack.get();
        if (!stack.isEmpty()) {
            graphics.pose().pushMatrix();
            float scale = iconSize / 16.0f;
            graphics.pose().translate(ix, iy);
            graphics.pose().scale(scale, scale);
            graphics.item(stack, 0, 0);
            graphics.pose().popMatrix();
        }
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
