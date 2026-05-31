package net.unfamily.iskautils.arcane;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.integration.apotheosis.ApotheosisCompat;

public final class ArcaneDictionaryConsume {
    private ArcaneDictionaryConsume() {}

    public static int computeTotalConsume(ItemStack dictionary, long gameTime, boolean curioFull, ServerPlayer player) {
        int total = 0;
        int overflowLevel = 0;
        int shiftingLevel = 0;
        int tierResonanceLevel = 0;

        for (ArcaneDictionaryContents.TraitSlot trait : ArcaneDictionaryContents.getTraits(dictionary)) {
            if (player != null && !ArcaneDictionaryEntryGate.traitActive(player, trait.id())) {
                continue;
            }
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
            if (ArcaneDictionaryTraitIds.TIER_RESONANCE.equals(trait.id())) {
                tierResonanceLevel = trait.level();
                continue;
            }
            total += ArcaneDictionaryEffectRegistry.resolveConsumePerLevel(trait.id()) * trait.level();
        }

        if (curioFull && shiftingLevel > 0 && ArcaneDictionaryShiftingState.isActive(dictionary, gameTime)) {
            ArcaneDictionaryShiftingState.MimicState mimic = ArcaneDictionaryShiftingState.read(dictionary);
            if (mimic != null
                    && (player == null || ArcaneDictionaryEntryGate.traitActive(player, mimic.traitId()))) {
                total += ArcaneDictionaryEffectRegistry.resolveConsumePerLevel(mimic.traitId()) * shiftingLevel;
            }
        }

        if (overflowLevel > 0) {
            double multiplier = 1.0D - Config.arcaneEntropyOverflowConsumeReductionPerLevel * overflowLevel;
            total = (int) Math.floor(total * Math.max(0.0D, multiplier));
        }

        if (tierResonanceLevel > 0 && player != null && ApotheosisCompat.isLoaded()) {
            int tierIndex = ApotheosisCompat.getWorldTierIndex(player);
            double reduction = tierIndex
                    * Config.arcaneTierResonanceConsumeReductionPerTierPerLevel
                    * tierResonanceLevel;
            total = (int) Math.floor(total * Math.max(0.0D, 1.0D - reduction));
        }

        if (tierResonanceLevel > 0) {
            total += ArcaneDictionaryEffectRegistry.resolveConsumePerLevel(ArcaneDictionaryTraitIds.TIER_RESONANCE)
                    * tierResonanceLevel;
        }

        return Math.max(0, total);
    }

    public static int computeTotalConsume(ItemStack dictionary, long gameTime, boolean curioFull) {
        return computeTotalConsume(dictionary, gameTime, curioFull, null);
    }

    public static int computeTotalConsume(ItemStack dictionary, long gameTime) {
        return computeTotalConsume(dictionary, gameTime, true, null);
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
