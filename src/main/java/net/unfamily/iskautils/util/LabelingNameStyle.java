package net.unfamily.iskautils.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * Builds a styled display name for the Labeling Machine (custom name only).
 */
public final class LabelingNameStyle {

    public static final int MAX_NAME_LENGTH = 50;
    public static final int DEFAULT_COLOR_RGB = 0xFFFFFF;

    public static final ChatFormatting[] VANILLA_PALETTE = {
            ChatFormatting.BLACK,
            ChatFormatting.DARK_BLUE,
            ChatFormatting.DARK_GREEN,
            ChatFormatting.DARK_AQUA,
            ChatFormatting.DARK_RED,
            ChatFormatting.DARK_PURPLE,
            ChatFormatting.GOLD,
            ChatFormatting.GRAY,
            ChatFormatting.DARK_GRAY,
            ChatFormatting.BLUE,
            ChatFormatting.GREEN,
            ChatFormatting.AQUA,
            ChatFormatting.RED,
            ChatFormatting.LIGHT_PURPLE,
            ChatFormatting.YELLOW,
            ChatFormatting.WHITE
    };

    private LabelingNameStyle() {}

    public static Component buildName(String text, boolean bold, boolean italic, boolean underline,
                                      boolean strikethrough, boolean obfuscated, int colorRgb) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            trimmed = trimmed.substring(0, MAX_NAME_LENGTH);
        }
        if (trimmed.isEmpty()) {
            return Component.empty();
        }
        Style style = Style.EMPTY
                .withBold(bold)
                .withItalic(italic)
                .withUnderlined(underline)
                .withStrikethrough(strikethrough)
                .withObfuscated(obfuscated)
                .withColor(TextColor.fromRgb(colorRgb & 0xFFFFFF));
        return Component.literal(trimmed).withStyle(style);
    }

    public static MutableComponent preview(String text, boolean bold, boolean italic, boolean underline,
                                           boolean strikethrough, boolean obfuscated, int colorRgb) {
        Component built = buildName(text, bold, italic, underline, strikethrough, obfuscated, colorRgb);
        if (built.getString().isEmpty()) {
            return Component.empty();
        }
        return built.copy();
    }

    /** Parses {@code #RRGGBB} or {@code RRGGBB}; returns null if invalid. */
    public static Integer parseHexColor(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.length() != 6) {
            return null;
        }
        try {
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String toHexString(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }
}
