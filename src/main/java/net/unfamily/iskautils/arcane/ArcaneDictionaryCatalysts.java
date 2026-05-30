package net.unfamily.iskautils.arcane;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public final class ArcaneDictionaryCatalysts {
    private ArcaneDictionaryCatalysts() {}

    public static boolean matches(ItemStack stack, List<String> catalysts, Level level) {
        if (stack.isEmpty() || catalysts == null || catalysts.isEmpty()) {
            return false;
        }
        for (ArcaneDictionaryCatalystSpec spec : ArcaneDictionaryCatalystSpec.parseAll(catalysts)) {
            if (spec.matches(stack)) {
                return true;
            }
        }
        return false;
    }

    public static int weightBoostFor(ItemStack stack, List<String> catalysts) {
        if (stack.isEmpty() || catalysts == null || catalysts.isEmpty()) {
            return 0;
        }
        for (ArcaneDictionaryCatalystSpec spec : ArcaneDictionaryCatalystSpec.parseAll(catalysts)) {
            if (spec.matches(stack)) {
                return Math.max(0, spec.weightBoost());
            }
        }
        return 0;
    }

    public static int maxWeightBoost(List<String> catalysts) {
        int max = 0;
        for (ArcaneDictionaryCatalystSpec spec : ArcaneDictionaryCatalystSpec.parseAll(catalysts)) {
            max = Math.max(max, Math.max(0, spec.weightBoost()));
        }
        return max;
    }

    public static ArcaneDictionaryDefinition.Entry findCatalyzedEntry(
            ItemStack catalyst,
            List<ArcaneDictionaryDefinition.Entry> pool,
            Level level) {
        if (catalyst.isEmpty() || pool.isEmpty()) {
            return null;
        }
        for (ArcaneDictionaryDefinition.Entry entry : pool) {
            if (matches(catalyst, entry.catalysts(), level)) {
                return entry;
            }
        }
        return null;
    }
}
