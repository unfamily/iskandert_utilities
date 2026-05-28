package net.unfamily.iskautils.obtaining;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

public final class SuspiciousDeliveryLoot {

    private SuspiciousDeliveryLoot() {}

    public static List<SuspiciousDeliveryDefinition.Entry> eligiblePool(
            ServerPlayer player,
            SuspiciousDeliveryDefinition definition) {
        List<SuspiciousDeliveryDefinition.Entry> pool = new ArrayList<>();
        for (SuspiciousDeliveryDefinition.Entry entry : definition.entries()) {
            if (entry.isEligible(player)) {
                pool.add(entry);
            }
        }
        return pool;
    }

    public static SuspiciousDeliveryDefinition.Entry pick(ServerPlayer player, RandomSource random) {
        SuspiciousDeliveryDefinition definition = SuspiciousDeliveryLoader.get();
        List<SuspiciousDeliveryDefinition.Entry> pool = eligiblePool(player, definition);
        if (pool.isEmpty()) {
            return null;
        }
        SuspiciousDeliveryDefinition.Entry picked = weightedPick(pool, random);
        return SuspiciousDeliveryLuckReroll.apply(player, random, pool, picked);
    }

    public static SuspiciousDeliveryDefinition.Entry weightedPick(
            List<SuspiciousDeliveryDefinition.Entry> entries,
            RandomSource random) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        int total = 0;
        for (var e : entries) {
            total += Math.max(0, e.weight());
        }
        if (total <= 0) {
            return entries.get(0);
        }
        int r = random.nextInt(total);
        int acc = 0;
        for (var e : entries) {
            acc += Math.max(0, e.weight());
            if (r < acc) {
                return e;
            }
        }
        return entries.get(entries.size() - 1);
    }

    public static int totalWeight(SuspiciousDeliveryDefinition definition) {
        int total = 0;
        for (SuspiciousDeliveryDefinition.Entry entry : definition.entries()) {
            total += Math.max(0, entry.weight());
        }
        return total;
    }
}
