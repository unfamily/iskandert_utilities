package net.unfamily.iskautils.util;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * Appends {@code tooltip.iska_utils.<path>.desc0..descN} lines until a key is missing.
 */
public final class RelicTooltipUtil {
    private static final int MAX_DESC_LINES = 8;

    private RelicTooltipUtil() {}

    public static void appendDescLines(List<Component> tooltip, String path) {
        appendDescLines(tooltip::add, path, -1);
    }

    public static void appendDescLines(Consumer<Component> tooltip, String path) {
        appendDescLines(tooltip, path, -1);
    }

    /**
     * @param formattedDescIndex desc line that receives {@code formatArgs}, or -1 for none
     */
    public static void appendDescLines(List<Component> tooltip, String path, int formattedDescIndex, Object... formatArgs) {
        appendDescLines(tooltip::add, path, formattedDescIndex, formatArgs);
    }

    public static void appendDescLines(
            Consumer<Component> tooltip,
            String path,
            int formattedDescIndex,
            Object... formatArgs) {
        for (int i = 0; i < MAX_DESC_LINES; i++) {
            String key = "tooltip.iska_utils." + path + ".desc" + i;
            if (!hasTranslation(key)) {
                break;
            }
            if (i == formattedDescIndex && formatArgs.length > 0) {
                tooltip.accept(Component.translatable(key, formatArgs));
            } else {
                tooltip.accept(Component.translatable(key));
            }
        }
    }

    private static boolean hasTranslation(String key) {
        return Language.getInstance().has(key);
    }
}
