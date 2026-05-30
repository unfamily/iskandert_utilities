package net.unfamily.iskautils.arcane.jei;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.arcane.ArcaneDictionaryCatalystBoost;
import net.unfamily.iskautils.arcane.ArcaneDictionaryCatalystSpec;
import net.unfamily.iskautils.arcane.ArcaneDictionaryDefinition;
import net.unfamily.iskautils.arcane.ArcaneDictionaryLoader;
import net.unfamily.iskautils.arcane.ArcaneDictionaryTraitStyle;
import net.unfamily.iskautils.util.RomanNumerals;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

public final class ArcaneDictionaryJeiLines {
    private ArcaneDictionaryJeiLines() {}

    public static void appendLevelHeader(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        Component traitName = ArcaneDictionaryTraitStyle.traitName(
                ctx.traitId(),
                ArcaneDictionaryTraitStyle.resolveRgb(ctx.poolEntry()));
        lines.add(Component.translatable(
                "jei.iska_utils.arcane_trait.meta.title",
                traitName,
                RomanNumerals.toRoman(ctx.minLevel()),
                RomanNumerals.toRoman(ctx.maxLevel())));
    }

    public static void appendPoolMeta(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        lines.add(Component.translatable(
                "jei.iska_utils.arcane_trait.meta.chance",
                ctx.formatPoolChancePercent()));
        lines.add(Component.translatable(
                "jei.iska_utils.arcane_trait.meta.luck",
                ctx.poolEntry().luck()));
        if (ctx.poolEntry().catalysts() != null && !ctx.poolEntry().catalysts().isEmpty()) {
            int total = poolTotalWeight();
            lines.add(Component.translatable(
                    "jei.iska_utils.arcane_trait.meta.catalyst_chance",
                    ctx.formatPercent(ArcaneDictionaryCatalystBoost.maxBoostedChancePercent(
                            ctx.poolEntry(), total))));
        }
    }

    public static int poolTotalWeight() {
        Map<Identifier, ArcaneDictionaryDefinition.Entry> unique = new LinkedHashMap<>();
        for (ArcaneDictionaryDefinition.Entry entry : ArcaneDictionaryLoader.getEntries()) {
            unique.putIfAbsent(entry.enchant(), entry);
        }
        return unique.values().stream().mapToInt(e -> Math.max(0, e.weight())).sum();
    }

    public static double chancePercent(ArcaneDictionaryDefinition.Entry entry) {
        int total = poolTotalWeight();
        if (total <= 0) {
            return 0.0D;
        }
        return 100.0D * Math.max(0, entry.weight()) / total;
    }

    public static void appendLine(List<Component> lines, String translationKey, Object... args) {
        lines.add(Component.translatable(translationKey, args));
    }

    public static void appendTraitLine(ArcaneDictionaryJeiContext ctx, List<Component> lines, String suffix, Object... args) {
        lines.add(Component.translatable(ctx.jeiKey(suffix), args));
    }

    public static void appendScaledPercent(
            ArcaneDictionaryJeiContext ctx,
            List<Component> lines,
            String suffix,
            double fractionPerLevel) {
        appendTraitLine(
                ctx,
                lines,
                suffix,
                ctx.formatPercent(ctx.percentAtLevel(fractionPerLevel, ctx.minLevel())),
                ctx.formatPercent(ctx.percentAtLevel(fractionPerLevel, ctx.maxLevel())));
    }

    public static void appendScaled(
            ArcaneDictionaryJeiContext ctx,
            List<Component> lines,
            String suffix,
            DoubleUnaryOperator perLevel) {
        appendTraitLine(
                ctx,
                lines,
                suffix,
                ctx.formatNumber(perLevel.applyAsDouble(ctx.minLevel())),
                ctx.formatNumber(perLevel.applyAsDouble(ctx.maxLevel())));
    }

    public record ResolvedCatalyst(ArcaneDictionaryCatalystSpec spec, List<ItemStack> stacks) {}

    public static List<ResolvedCatalyst> resolveCatalysts(List<String> catalysts, int max) {
        if (catalysts == null || catalysts.isEmpty()) {
            return List.of();
        }
        List<ResolvedCatalyst> out = new ArrayList<>();
        for (ArcaneDictionaryCatalystSpec spec : ArcaneDictionaryCatalystSpec.parseAll(catalysts)) {
            if (out.size() >= max) {
                break;
            }
            List<ItemStack> stacks = spec.exampleStacks();
            if (!stacks.isEmpty()) {
                out.add(new ResolvedCatalyst(spec, stacks));
            }
        }
        return List.copyOf(out);
    }

    public static List<Component> catalystTooltipLines(
            ArcaneDictionaryDefinition.Entry entry,
            ResolvedCatalyst catalyst) {
        List<Component> lines = new ArrayList<>();
        ArcaneDictionaryCatalystSpec spec = catalyst.spec();
        if (spec.matchSpec().startsWith("#")) {
            Identifier tagId = Identifier.tryParse(spec.matchSpec().substring(1));
            if (tagId != null) {
                lines.add(Component.translatable(
                        "jei.iska_utils.arcane_trait.meta.catalyst_tag", tagId.toString()));
            }
        }
        for (ItemStack stack : catalyst.stacks()) {
            lines.add(stack.getHoverName());
        }
        if (entry != null) {
            int total = poolTotalWeight();
            lines.add(Component.translatable(
                    "jei.iska_utils.arcane_trait.meta.catalyst_weight",
                    spec.weightBoost()));
            lines.add(Component.translatable(
                    "jei.iska_utils.arcane_trait.meta.catalyst_chance",
                    formatChancePercent(ArcaneDictionaryCatalystBoost.boostedChancePercent(
                            entry, spec.weightBoost(), total))));
        }
        return List.copyOf(lines);
    }

    private static String formatChancePercent(double percent) {
        if (Math.rint(percent) == percent) {
            return String.format(java.util.Locale.ROOT, "%.0f%%", percent);
        }
        return String.format(java.util.Locale.ROOT, "%.1f%%", percent);
    }
}
