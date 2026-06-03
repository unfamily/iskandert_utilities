package net.unfamily.iskautils.util;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.List;
import java.util.function.Consumer;

/**
 * Appends {@code tooltip.iska_utils.<path>.desc0..descN} lines until a key is missing.
 * Lore lines use light gray; mechanical lines use lime green.
 * If a translation already contains legacy {@code §} color codes, styles are not applied so lang wins.
 */
public final class ArtifactTooltipUtil {
    private static final int MAX_DESC_LINES = 8;

    /** Light gray flavor / lore text ({@code #AAAAAA}). */
    public static final int LORE_COLOR = 0xAAAAAA;
    /** Lime green mechanical / stats text ({@code #55FF55}). */
    public static final int TECH_COLOR = 0x55FF55;

    private ArtifactTooltipUtil() {}

    public static Style loreStyle() {
        return Style.EMPTY.withColor(TextColor.fromRgb(LORE_COLOR));
    }

    public static Style techStyle() {
        return Style.EMPTY.withColor(TextColor.fromRgb(TECH_COLOR));
    }

    public static MutableComponent loreLine(String translationKey, Object... args) {
        return styledLine(translationKey, loreStyle(), args);
    }

    public static MutableComponent techLine(String translationKey, Object... args) {
        return styledLine(translationKey, techStyle(), args);
    }

    public static void addLoreLine(Consumer<Component> tooltip, String translationKey, Object... args) {
        tooltip.accept(loreLine(translationKey, args));
    }

    public static void addTechLine(Consumer<Component> tooltip, String translationKey, Object... args) {
        tooltip.accept(techLine(translationKey, args));
    }

    public static void appendDescLines(List<Component> tooltip, String path, int loreLineCount) {
        appendDescLines(tooltip::add, path, loreLineCount);
    }

    public static void appendDescLines(Consumer<Component> tooltip, String path, int loreLineCount) {
        appendDescLines(tooltip, path, loreLineCount, -1);
    }

    public static void appendDescLines(List<Component> tooltip, String path, int loreLineCount, int formattedDescIndex, Object... formatArgs) {
        appendDescLines(tooltip::add, path, loreLineCount, formattedDescIndex, formatArgs);
    }

    /**
     * @param loreLineCount first N {@code desc} lines use lore color
     * @param formattedDescIndex desc line that receives {@code formatArgs}, or -1 for none
     */
    public static void appendDescLines(
            Consumer<Component> tooltip,
            String path,
            int loreLineCount,
            int formattedDescIndex,
            Object... formatArgs) {
        appendDescLinesFrom(tooltip, path, 0, loreLineCount, formattedDescIndex, formatArgs);
    }

    public static void appendDescLinesFrom(
            Consumer<Component> tooltip,
            String path,
            int startIndex,
            int loreLineCount,
            int formattedDescIndex,
            Object... formatArgs) {
        for (int i = startIndex; i < MAX_DESC_LINES; i++) {
            String key = "tooltip.iska_utils." + path + ".desc" + i;
            if (!hasTranslation(key)) {
                break;
            }
            int relative = i - startIndex;
            Style style = relative < loreLineCount ? loreStyle() : techStyle();
            if (i == formattedDescIndex && formatArgs.length > 0) {
                tooltip.accept(withContextStyle(key, style, formatArgs));
            } else {
                tooltip.accept(withContextStyle(key, style));
            }
        }
    }

    private static MutableComponent styledLine(String translationKey, Style style, Object... args) {
        return withContextStyle(translationKey, style, args);
    }

    private static MutableComponent withContextStyle(String translationKey, Style style, Object... args) {
        MutableComponent line = args.length > 0
                ? Component.translatable(translationKey, args)
                : Component.translatable(translationKey);
        if (resolvedTextHasLegacyFormatting(translationKey, args)) {
            return line;
        }
        return line.withStyle(style);
    }

    private static boolean resolvedTextHasLegacyFormatting(String translationKey, Object... args) {
        MutableComponent line = args.length > 0
                ? Component.translatable(translationKey, args)
                : Component.translatable(translationKey);
        return line.getString().indexOf('\u00A7') >= 0;
    }

    private static boolean hasTranslation(String key) {
        return Language.getInstance().has(key);
    }
}
