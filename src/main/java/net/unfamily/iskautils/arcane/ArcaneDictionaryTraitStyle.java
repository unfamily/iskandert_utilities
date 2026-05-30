package net.unfamily.iskautils.arcane;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.iskautils.util.RomanNumerals;
import org.slf4j.Logger;

public final class ArcaneDictionaryTraitStyle {
    /** Default aqua when JSON omits {@code color}. */
    public static final int DEFAULT_RGB = 0x55FFFF;

    private ArcaneDictionaryTraitStyle() {}

    public static int resolveRgb(ResourceLocation traitId) {
        ArcaneDictionaryDefinition.Entry entry = ArcaneDictionaryLoader.findEntry(traitId);
        return resolveRgb(entry);
    }

    public static int resolveRgb(ArcaneDictionaryDefinition.Entry entry) {
        if (entry == null || entry.traitColorRgb() < 0) {
            return DEFAULT_RGB;
        }
        return entry.traitColorRgb();
    }

    public static Style style(int rgb) {
        return Style.EMPTY.withColor(rgb & 0xFFFFFF).withItalic(false);
    }

    public static Component traitName(ResourceLocation traitId) {
        return traitName(traitId, resolveRgb(traitId));
    }

    public static Component traitName(ResourceLocation traitId, int rgb) {
        return Component.translatable(traitNameKey(traitId)).withStyle(style(rgb));
    }

    public static Component traitNameAndLevel(ResourceLocation traitId, int level) {
        int rgb = resolveRgb(traitId);
        Style traitStyle = style(rgb);
        return Component.translatable(traitNameKey(traitId))
                .withStyle(traitStyle)
                .append(" ")
                .append(Component.literal(RomanNumerals.toRoman(level)).withStyle(traitStyle));
    }

    public static String traitNameKey(ResourceLocation traitId) {
        return "arcane_trait." + traitId.getNamespace() + "." + traitId.getPath();
    }

    public static int parseHexRgb(String hex, Logger logger, String contextId) {
        if (hex == null || hex.isBlank()) {
            return -1;
        }
        String s = hex.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.startsWith("0x") || s.startsWith("0X")) {
            s = s.substring(2);
        }
        if (s.length() != 6) {
            logger.warn("Invalid trait color '{}' in {}, using default", hex, contextId);
            return -1;
        }
        try {
            return Integer.parseUnsignedInt(s, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            logger.warn("Invalid trait color '{}' in {}, using default", hex, contextId);
            return -1;
        }
    }
}
