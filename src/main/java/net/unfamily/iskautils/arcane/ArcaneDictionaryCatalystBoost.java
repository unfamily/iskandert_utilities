package net.unfamily.iskautils.arcane;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/** Offhand catalyst weight bonus during reroll — boost comes from {@code cat} suffix {@code :weight}. */
public final class ArcaneDictionaryCatalystBoost {
    private ArcaneDictionaryCatalystBoost() {}

    public static double boostedChancePercent(
            ArcaneDictionaryDefinition.Entry entry,
            int weightBoost,
            int poolTotalWeight) {
        if (entry == null || poolTotalWeight <= 0) {
            return 0.0D;
        }
        int boost = Math.max(0, weightBoost);
        int weight = Math.max(0, entry.weight()) + boost;
        int total = poolTotalWeight + boost;
        return 100.0D * weight / total;
    }

    public static double maxBoostedChancePercent(
            ArcaneDictionaryDefinition.Entry entry,
            int poolTotalWeight) {
        if (entry == null || entry.catalysts() == null || entry.catalysts().isEmpty()) {
            return boostedChancePercent(entry, 0, poolTotalWeight);
        }
        double max = boostedChancePercent(entry, 0, poolTotalWeight);
        for (ArcaneDictionaryCatalystSpec spec : ArcaneDictionaryCatalystSpec.parseAll(entry.catalysts())) {
            max = Math.max(max, boostedChancePercent(entry, spec.weightBoost(), poolTotalWeight));
        }
        return max;
    }

    public static boolean matchesAny(ItemStack catalyst, List<ArcaneDictionaryDefinition.Entry> pool, Level level) {
        if (catalyst.isEmpty() || pool == null || pool.isEmpty()) {
            return false;
        }
        for (ArcaneDictionaryDefinition.Entry entry : pool) {
            if (ArcaneDictionaryCatalysts.matches(catalyst, entry.catalysts(), level)) {
                return true;
            }
        }
        return false;
    }

    public static ArcaneDictionaryDefinition.Entry pickWithLuckAndCatalyst(
            RandomSource random,
            List<ArcaneDictionaryDefinition.Entry> pool,
            int netLuck,
            ItemStack catalystForMatching,
            Level level) {
        if (pool.isEmpty()) {
            return null;
        }
        if (catalystForMatching.isEmpty() || !matchesAny(catalystForMatching, pool, level)) {
            return ArcaneDictionaryLoot.pickWithLuck(random, pool, netLuck);
        }

        int baseTotal = pool.stream().mapToInt(e -> Math.max(0, e.weight())).sum();
        ArcaneDictionaryDefinition.Entry picked =
                weightedPickWithCatalyst(random, pool, catalystForMatching, level, baseTotal);
        return ArcaneDictionaryLoot.applyLuckReroll(random, pool, picked, netLuck);
    }

    private static ArcaneDictionaryDefinition.Entry weightedPickWithCatalyst(
            RandomSource random,
            List<ArcaneDictionaryDefinition.Entry> pool,
            ItemStack catalyst,
            Level level,
            int baseTotal) {
        int total = 0;
        int[] weights = new int[pool.size()];
        for (int i = 0; i < pool.size(); i++) {
            ArcaneDictionaryDefinition.Entry entry = pool.get(i);
            int weight = Math.max(0, entry.weight());
            weight += ArcaneDictionaryCatalysts.weightBoostFor(catalyst, entry.catalysts());
            weights[i] = weight;
            total += weight;
        }
        if (total <= 0) {
            return pool.get(random.nextInt(pool.size()));
        }
        int roll = random.nextInt(total);
        int acc = 0;
        for (int i = 0; i < pool.size(); i++) {
            acc += weights[i];
            if (roll < acc) {
                return pool.get(i);
            }
        }
        return pool.get(pool.size() - 1);
    }
}
