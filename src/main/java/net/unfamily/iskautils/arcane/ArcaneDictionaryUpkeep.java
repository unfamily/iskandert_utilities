package net.unfamily.iskautils.arcane;

import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.Config;

public final class ArcaneDictionaryUpkeep {
    private ArcaneDictionaryUpkeep() {}

    public static int computeTotalUpkeep(ItemStack dictionary, long gameTime, boolean curioFull) {
        int total = 0;
        int overflowLevel = 0;
        int shiftingLevel = 0;

        for (ArcaneDictionaryContents.TraitSlot trait : ArcaneDictionaryContents.getTraits(dictionary)) {
            if (!curioFull && !ArcaneDictionaryActivation.traitApplies(trait.id(), ArcaneDictionaryActivation.Scope.OFF_CURIO)) {
                continue;
            }
            if (ArcaneDictionaryTraitIds.ENTROPY_OVERFLOW.equals(trait.id())) {
                overflowLevel = trait.level();
                continue;
            }
            if (ArcaneDictionaryTraitIds.SHIFTING_POWER.equals(trait.id())) {
                shiftingLevel = trait.level();
                continue;
            }
            total += ArcaneDictionaryEffectRegistry.resolveUpkeepPerLevel(trait.id()) * trait.level();
        }

        if (curioFull && shiftingLevel > 0 && ArcaneDictionaryShiftingState.isActive(dictionary, gameTime)) {
            ArcaneDictionaryShiftingState.MimicState mimic = ArcaneDictionaryShiftingState.read(dictionary);
            if (mimic != null) {
                total += ArcaneDictionaryEffectRegistry.resolveUpkeepPerLevel(mimic.traitId()) * shiftingLevel;
            }
        }

        if (overflowLevel > 0) {
            double multiplier = 1.0D - Config.arcaneEntropyOverflowUpkeepReductionPerLevel * overflowLevel;
            total = (int) Math.floor(total * Math.max(0.0D, multiplier));
        }

        return Math.max(0, total);
    }

    public static int computeTotalUpkeep(ItemStack dictionary, long gameTime) {
        return computeTotalUpkeep(dictionary, gameTime, true);
    }

    public static int getOverflowLevel(ItemStack dictionary) {
        for (ArcaneDictionaryContents.TraitSlot trait : ArcaneDictionaryContents.getTraits(dictionary)) {
            if (ArcaneDictionaryTraitIds.ENTROPY_OVERFLOW.equals(trait.id())) {
                return trait.level();
            }
        }
        return 0;
    }
}
