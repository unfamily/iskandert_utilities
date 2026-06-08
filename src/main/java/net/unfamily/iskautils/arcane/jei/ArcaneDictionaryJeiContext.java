package net.unfamily.iskautils.arcane.jei;

import net.minecraft.resources.ResourceLocation;
import net.unfamily.iskautils.arcane.ArcaneDictionaryDefinition;

import java.util.Locale;

/** Server-safe JEI description context (no client class references). */
public record ArcaneDictionaryJeiContext(
        ResourceLocation traitId,
        ArcaneDictionaryDefinition.Entry poolEntry,
        int resolvedConsume,
        int minLevel,
        int maxLevel,
        double poolChancePercent) {

    public int level(int level) {
        return Math.max(minLevel, Math.min(maxLevel, level));
    }

    public double percentAtLevel(double fractionPerLevel, int level) {
        return fractionPerLevel * level(level) * 100.0D;
    }

    public String formatPoolChancePercent() {
        return String.format(Locale.ROOT, "%.1f%%", poolChancePercent);
    }

    public String formatPercent(double percent) {
        if (Math.rint(percent) == percent) {
            return String.format(Locale.ROOT, "%.0f%%", percent);
        }
        return String.format(Locale.ROOT, "%.1f%%", percent);
    }

    public String formatNumber(double value) {
        if (Math.rint(value) == value) {
            return String.format(Locale.ROOT, "%.0f", value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /** e.g. jei.iska_utils.arcane_trait.iska_utils.glass_skin.reflect */
    public String jeiKey(String suffix) {
        return "jei.iska_utils.arcane_trait." + traitId.getNamespace() + "." + traitId.getPath() + "." + suffix;
    }

    public String traitNameKey() {
        return "arcane_trait." + traitId.getNamespace() + "." + traitId.getPath();
    }
}
