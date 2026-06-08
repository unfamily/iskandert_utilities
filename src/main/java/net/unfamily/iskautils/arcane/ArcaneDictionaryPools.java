package net.unfamily.iskautils.arcane;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ArcaneDictionaryPools {
    private ArcaneDictionaryPools() {}

    public static List<ArcaneDictionaryDefinition.Entry> eligibleForPlayer(
            ServerPlayer player,
            List<ArcaneDictionaryDefinition.Entry> pool) {
        List<ArcaneDictionaryDefinition.Entry> out = new ArrayList<>();
        for (ArcaneDictionaryDefinition.Entry entry : pool) {
            if (entry.isFullyEligible(player)) {
                out.add(entry);
            }
        }
        return out;
    }

    public static List<ArcaneDictionaryDefinition.Entry> visibleForJei(List<ArcaneDictionaryDefinition.Entry> all) {
        List<ArcaneDictionaryDefinition.Entry> out = new ArrayList<>();
        for (ArcaneDictionaryDefinition.Entry entry : uniqueEntries(all).values()) {
            if (entry.checkAllMods()) {
                out.add(entry);
            }
        }
        return out;
    }

    public static Map<ResourceLocation, ArcaneDictionaryDefinition.Entry> uniqueEntries(
            List<ArcaneDictionaryDefinition.Entry> entries) {
        Map<ResourceLocation, ArcaneDictionaryDefinition.Entry> unique = new LinkedHashMap<>();
        for (ArcaneDictionaryDefinition.Entry entry : entries) {
            unique.putIfAbsent(entry.enchant(), entry);
        }
        return unique;
    }

    public static int poolTotalWeight(List<ArcaneDictionaryDefinition.Entry> pool) {
        int total = 0;
        for (ArcaneDictionaryDefinition.Entry entry : pool) {
            total += Math.max(0, entry.weight());
        }
        return total;
    }

    public static double chancePercent(ArcaneDictionaryDefinition.Entry entry, List<ArcaneDictionaryDefinition.Entry> pool) {
        int total = poolTotalWeight(pool);
        if (total <= 0) {
            return 0.0D;
        }
        return 100.0D * Math.max(0, entry.weight()) / total;
    }
}
