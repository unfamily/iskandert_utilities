package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Standard NeoForge/vanilla {@link Button} (widget/button sprites) with a centered item or texture icon.
 */
public class ItemIconButton extends Button {
    private final Supplier<ItemStack> iconStack;
    @Nullable
    private final Supplier<ResourceLocation> overlayTexture;
    /** When false, LMB does not play the vanilla UI click (e.g. server already plays a custom sound). */
    private boolean playClickSound = true;

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
            @Nullable Supplier<ResourceLocation> overlayTexture,
            Component tooltip) {
        super(x, y, size, size, Component.empty(), onPress, DEFAULT_NARRATION);
        this.iconStack = iconStack;
        this.overlayTexture = overlayTexture;
        if (!tooltip.getString().isEmpty()) {
            setTooltip(Tooltip.create(tooltip));
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        int iconSize = Math.min(16, Math.max(8, getWidth() - 2));
        int ix = getX() + (getWidth() - iconSize) / 2;
        int iy = getY() + (getHeight() - iconSize) / 2;
        ResourceLocation texture = overlayTexture != null ? overlayTexture.get() : null;
        if (texture != null) {
            graphics.blit(texture, ix, iy, 0, 0, iconSize, iconSize, iconSize, iconSize);
            return;
        }
        ItemStack stack = iconStack.get();
        if (!stack.isEmpty()) {
            graphics.pose().pushPose();
            float scale = iconSize / 16.0f;
            graphics.pose().translate(ix, iy, 0);
            graphics.pose().scale(scale, scale, 1);
            if (!this.active) {
                graphics.setColor(1.0F, 1.0F, 1.0F, 0.4F);
            }
            graphics.renderItem(stack, 0, 0);
            if (!this.active) {
                graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
            graphics.pose().popPose();
        }
    }

    /** Disable vanilla click so a custom/server sound can be the only feedback. */
    public ItemIconButton muteClickSound() {
        this.playClickSound = false;
        return this;
    }

    @Override
    public void playDownSound(SoundManager handler) {
        if (playClickSound) {
            super.playDownSound(handler);
        }
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
