package net.unfamily.iskautils.obtaining;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.List;
import java.util.function.Predicate;

/**
 * Applies fortune / unluck potion rerolls to a weighted loot pick.
 */
public final class SuspiciousDeliveryLuckReroll {
    private static final int MAX_FORCED_ATTEMPTS = 64;

    private SuspiciousDeliveryLuckReroll() {}

    public static int computeNetLuck(ServerPlayer player) {
        int fortune = 0;
        int unluck = 0;
        MobEffectInstance luck = player.getEffect(MobEffects.LUCK);
        if (luck != null) {
            fortune = luck.getAmplifier() + 1;
        }
        MobEffectInstance bad = player.getEffect(MobEffects.UNLUCK);
        if (bad != null) {
            unluck = bad.getAmplifier() + 1;
        }
        return fortune - unluck;
    }

    public static SuspiciousDeliveryDefinition.Entry apply(
            ServerPlayer player,
            RandomSource random,
            List<SuspiciousDeliveryDefinition.Entry> pool,
            SuspiciousDeliveryDefinition.Entry picked) {
        if (pool.isEmpty() || picked == null) {
            return picked;
        }
        int net = computeNetLuck(player);
        if (net >= 0) {
            return applyFortune(random, pool, picked, net);
        }
        return applyUnluck(random, pool, picked, -net);
    }

    private static SuspiciousDeliveryDefinition.Entry applyFortune(
            RandomSource random,
            List<SuspiciousDeliveryDefinition.Entry> pool,
            SuspiciousDeliveryDefinition.Entry entry,
            int net) {
        int tier1 = Math.min(50, 30 + net * 5);
        while (entry.luck() < 0 && random.nextInt(100) < tier1) {
            entry = SuspiciousDeliveryLoot.weightedPick(pool, random);
        }
        if (net >= 5) {
            int tier2 = Math.min(100, 50 + (net - 4) * 10);
            while (entry.luck() < 0 && random.nextInt(100) < tier2) {
                entry = SuspiciousDeliveryLoot.weightedPick(pool, random);
            }
        }
        if (net >= 10) {
            entry = rerollUntil(pool, random, e -> e.luck() >= 1000, entry);
            entry = rerollUntil(pool, random, e -> e.luck() >= 10000, entry);
        }
        return entry;
    }

    private static SuspiciousDeliveryDefinition.Entry applyUnluck(
            RandomSource random,
            List<SuspiciousDeliveryDefinition.Entry> pool,
            SuspiciousDeliveryDefinition.Entry entry,
            int effective) {
        int tier1 = Math.min(50, 30 + effective * 5);
        while (entry.luck() > 0 && random.nextInt(100) < tier1) {
            entry = SuspiciousDeliveryLoot.weightedPick(pool, random);
        }
        if (effective >= 5) {
            int tier2 = Math.min(100, 50 + (effective - 4) * 10);
            while (entry.luck() > 0 && random.nextInt(100) < tier2) {
                entry = SuspiciousDeliveryLoot.weightedPick(pool, random);
            }
        }
        if (effective >= 10) {
            entry = rerollUntil(pool, random, e -> e.luck() <= -1000, entry);
            entry = rerollUntil(pool, random, e -> e.luck() <= -10000, entry);
        }
        return entry;
    }

    private static SuspiciousDeliveryDefinition.Entry rerollUntil(
            List<SuspiciousDeliveryDefinition.Entry> pool,
            RandomSource random,
            Predicate<SuspiciousDeliveryDefinition.Entry> predicate,
            SuspiciousDeliveryDefinition.Entry fallback) {
        if (pool.stream().anyMatch(predicate)) {
            for (int i = 0; i < MAX_FORCED_ATTEMPTS; i++) {
                SuspiciousDeliveryDefinition.Entry candidate = SuspiciousDeliveryLoot.weightedPick(pool, random);
                if (predicate.test(candidate)) {
                    return candidate;
                }
            }
        }
        return fallback;
    }
}
