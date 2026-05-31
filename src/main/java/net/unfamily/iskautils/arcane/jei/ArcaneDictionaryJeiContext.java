package net.unfamily.iskautils.arcane.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.iskautils.arcane.ArcaneDictionaryDefinition;
import net.unfamily.iskautils.arcane.ArcaneDictionaryPools;

import java.util.Locale;

public record ArcaneDictionaryJeiContext(
        ResourceLocation traitId,
        ArcaneDictionaryDefinition.Entry poolEntry,
        int resolvedConsume,
        int minLevel,
        int maxLevel,
        double poolChancePercent) {

    public static ArcaneDictionaryJeiContext of(ResourceLocation traitId, ArcaneDictionaryDefinition.Entry entry) {
        return of(traitId, entry, Minecraft.getInstance());
    }

    public static ArcaneDictionaryJeiContext of(
            ResourceLocation traitId,
            ArcaneDictionaryDefinition.Entry entry,
            Minecraft mc) {
        return ArcaneDictionaryPools.jeiContext(traitId, entry, mc);
    }

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
