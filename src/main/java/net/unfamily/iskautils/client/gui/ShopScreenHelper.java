package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Shared shop GUI rendering helpers. */
public final class ShopScreenHelper {

    public static final int ENTRY_ICON_OFFSET = 3;
    public static final int ENTRY_ICON_SIZE = 18;
    public static final int SHOP_ENTRY_TEXTURE_WIDTH = 220;
    public static final int SHOP_ENTRY_TEXTURE_HEIGHT = 24;

    private ShopScreenHelper() {
    }

    /**
     * Draws a shop entry row background wider than the source art:
     * base pass uses texture columns {@code [0, width - 2]} (standard render minus the last pixel column),
     * then when {@code renderWidth} exceeds that, the tail {@code [X, last]} is drawn 1:1 where
     * {@code X = textureWidth - extensionWidth}.
     */
    public static void renderExtendedEntryBackground(
            GuiGraphics guiGraphics,
            ResourceLocation texture,
            int x,
            int y,
            int renderWidth,
            int height) {
        int texW = SHOP_ENTRY_TEXTURE_WIDTH;
        int texH = SHOP_ENTRY_TEXTURE_HEIGHT;
        int baseWidth = texW - 1;
        if (renderWidth <= baseWidth) {
            guiGraphics.blit(texture, x, y, renderWidth, height, 0, 0, renderWidth, height, texW, texH);
            return;
        }
        guiGraphics.blit(texture, x, y, baseWidth, height, 0, 0, baseWidth, height, texW, texH);
        int extensionWidth = renderWidth - baseWidth;
        int tailStart = texW - extensionWidth;
        guiGraphics.blit(texture, x + baseWidth, y, extensionWidth, height,
                tailStart, 0, extensionWidth, height, texW, texH);
    }

    public static boolean isMouseOverEntryIcon(int mouseX, int mouseY, int entryX, int entryY) {
        int slotX = entryX + ENTRY_ICON_OFFSET;
        int slotY = entryY + ENTRY_ICON_OFFSET;
        return mouseX >= slotX && mouseX < slotX + ENTRY_ICON_SIZE
                && mouseY >= slotY && mouseY < slotY + ENTRY_ICON_SIZE;
    }

    public static void renderScaledText(
            GuiGraphics guiGraphics,
            Font font,
            String text,
            int x,
            int y,
            int maxWidth,
            int color) {
        Component textComponent = Component.literal(text);
        int textWidth = font.width(textComponent);

        if (textWidth <= maxWidth) {
            guiGraphics.drawString(font, textComponent, x, y, color, false);
            return;
        }

        float scale = (float) maxWidth / textWidth;
        float minScale = 0.85f;
        if (scale < minScale) {
            scale = minScale;
        }

        if (textWidth * scale > maxWidth && text.length() > 3) {
            String truncated = text;
            String ellipsis = "...";
            while (truncated.length() > 3) {
                String candidate = truncated + ellipsis;
                int candidateWidth = font.width(candidate);
                if (candidateWidth * scale <= maxWidth) {
                    textComponent = Component.literal(candidate);
                    break;
                }
                truncated = truncated.substring(0, truncated.length() - 1);
            }
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        guiGraphics.drawString(font, textComponent, 0, 0, color, false);
        guiGraphics.pose().popPose();
    }
}
