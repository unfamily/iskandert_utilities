package net.unfamily.iskautils.arcane;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryLuckReroll;

import java.util.List;
import java.util.function.Predicate;

public final class ArcaneDictionaryLoot {

    private ArcaneDictionaryLoot() {}

    public static ArcaneDictionaryDefinition.Entry weightedPick(
            List<ArcaneDictionaryDefinition.Entry> entries,
            RandomSource random) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        int total = 0;
        for (ArcaneDictionaryDefinition.Entry e : entries) {
            total += Math.max(0, e.weight());
        }
        if (total <= 0) {
            return entries.get(random.nextInt(entries.size()));
        }
        int r = random.nextInt(total);
        int acc = 0;
        for (ArcaneDictionaryDefinition.Entry e : entries) {
            acc += Math.max(0, e.weight());
            if (r < acc) {
                return e;
            }
        }
        return entries.get(entries.size() - 1);
    }

    public static ArcaneDictionaryDefinition.Entry pickWithLuck(
            RandomSource random,
            List<ArcaneDictionaryDefinition.Entry> pool,
            int netLuck) {
        if (pool.isEmpty()) {
            return null;
        }
        ArcaneDictionaryDefinition.Entry picked = weightedPick(pool, random);
        return applyLuckReroll(random, pool, picked, netLuck);
    }

    static ArcaneDictionaryDefinition.Entry applyLuckReroll(
            RandomSource random,
            List<ArcaneDictionaryDefinition.Entry> pool,
            ArcaneDictionaryDefinition.Entry picked,
            int netLuck) {
        if (picked == null || pool.isEmpty()) {
            return picked;
        }
        if (netLuck >= 0) {
            return applyFortune(random, pool, picked, netLuck);
        }
        return applyUnluck(random, pool, picked, -netLuck);
    }

    private static ArcaneDictionaryDefinition.Entry applyFortune(
            RandomSource random,
            List<ArcaneDictionaryDefinition.Entry> pool,
            ArcaneDictionaryDefinition.Entry entry,
            int net) {
        int tier1 = Math.min(50, 30 + net * 5);
        while (entry.luck() < 0 && random.nextInt(100) < tier1) {
            entry = weightedPick(pool, random);
        }
        if (net >= 5) {
            int tier2 = Math.min(100, 50 + (net - 4) * 10);
            while (entry.luck() < 0 && random.nextInt(100) < tier2) {
                entry = weightedPick(pool, random);
            }
        }
        if (net >= 10) {
            entry = rerollUntil(pool, random, e -> e.luck() >= 1000, entry);
            entry = rerollUntil(pool, random, e -> e.luck() >= 10000, entry);
        }
        return entry;
    }

    private static ArcaneDictionaryDefinition.Entry applyUnluck(
            RandomSource random,
            List<ArcaneDictionaryDefinition.Entry> pool,
            ArcaneDictionaryDefinition.Entry entry,
            int effective) {
        int tier1 = Math.min(50, 30 + effective * 5);
        while (entry.luck() > 0 && random.nextInt(100) < tier1) {
            entry = weightedPick(pool, random);
        }
        if (effective >= 5) {
            int tier2 = Math.min(100, 50 + (effective - 4) * 10);
            while (entry.luck() > 0 && random.nextInt(100) < tier2) {
                entry = weightedPick(pool, random);
            }
        }
        if (effective >= 10) {
            entry = rerollUntil(pool, random, e -> e.luck() <= -1000, entry);
            entry = rerollUntil(pool, random, e -> e.luck() <= -10000, entry);
        }
        return entry;
    }

    private static ArcaneDictionaryDefinition.Entry rerollUntil(
            List<ArcaneDictionaryDefinition.Entry> pool,
            RandomSource random,
            Predicate<ArcaneDictionaryDefinition.Entry> predicate,
            ArcaneDictionaryDefinition.Entry fallback) {
        if (pool.stream().anyMatch(predicate)) {
            for (int i = 0; i < 64; i++) {
                ArcaneDictionaryDefinition.Entry candidate = weightedPick(pool, random);
                if (predicate.test(candidate)) {
                    return candidate;
                }
            }
        }
        return fallback;
    }

    public static int computeNetLuck(net.minecraft.server.level.ServerPlayer player) {
        return SuspiciousDeliveryLuckReroll.computeNetLuck(player);
    }

    public static boolean matchesCatalyst(ItemStack stack, List<String> catalysts, Level level) {
        return ArcaneDictionaryCatalysts.matches(stack, catalysts, level);
    }
}
