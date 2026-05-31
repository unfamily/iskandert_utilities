package net.unfamily.iskautils.arcane;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ArcaneDictionaryReroll {
    private ArcaneDictionaryReroll() {}

    public static boolean tryReroll(ServerPlayer player, ItemStack dictionary) {
        if (ArcaneDictionaryXpRoll.getTotalExperience(player) <= 0) {
            player.sendSystemMessage(Component.translatable("message.iska_utils.arcane_dictionary.no_xp"), true);
            return false;
        }

        ArcaneDictionaryXpRoll.RollContext ctx = ArcaneDictionaryXpRoll.rollContextFromPlayer(player);
        if (ctx.consumedXp() <= 0) {
            player.sendSystemMessage(Component.translatable("message.iska_utils.arcane_dictionary.no_xp"), true);
            return false;
        }

        return applyRoll(player, dictionary, ctx.quality(), true);
    }

    /** Rolls traits on a traitless dictionary as a free level-1 reroll when first obtained. */
    public static boolean rollInitialTraits(ServerPlayer player, ItemStack dictionary) {
        if (!ArcaneDictionaryContents.needsInitialTraitRoll(dictionary)) {
            return false;
        }
        return applyRoll(player, dictionary, ArcaneDictionaryXpRoll.qualityForRollLevels(1), false);
    }

    private static boolean applyRoll(ServerPlayer player, ItemStack dictionary, double quality, boolean notify) {
        ArcaneDictionaryXpRoll.TraitRoll roll = ArcaneDictionaryXpRoll.resolveTraitRoll(quality, player.getRandom());
        int traitCount = roll.count();
        int traitLevel = roll.levelPerTrait();

        List<ArcaneDictionaryDefinition.Entry> pool = ArcaneDictionaryLoader.getEntries();
        if (pool.isEmpty()) {
            if (notify) {
                player.sendSystemMessage(Component.translatable("message.iska_utils.arcane_dictionary.no_entries"), true);
            }
            return false;
        }

        List<ArcaneDictionaryContents.TraitSlot> traits = rollTraitSlots(
                player,
                player.getRandom(),
                pool,
                traitCount,
                traitLevel,
                player.getOffhandItem());

        if (traits.isEmpty()) {
            if (notify) {
                player.sendSystemMessage(Component.translatable("message.iska_utils.arcane_dictionary.reroll_failed"), true);
            }
            return false;
        }

        ArcaneDictionaryContents.setTraits(dictionary, traits);
        if (notify) {
            player.sendSystemMessage(Component.translatable("message.iska_utils.arcane_dictionary.rerolled"), true);
        }
        return true;
    }

    static List<ArcaneDictionaryContents.TraitSlot> rollTraitSlots(
            ServerPlayer player,
            RandomSource random,
            List<ArcaneDictionaryDefinition.Entry> pool,
            int traitCount,
            int traitLevel,
            ItemStack catalyst) {
        List<ArcaneDictionaryDefinition.Entry> validPool = new ArrayList<>();
        for (ArcaneDictionaryDefinition.Entry entry : pool) {
            if (ArcaneDictionaryEffectRegistry.get(entry.enchant()) != null
                    && entry.isFullyEligible(player)) {
                validPool.add(entry);
            }
        }
        if (validPool.isEmpty()) {
            return List.of();
        }

        int netLuck = ArcaneDictionaryLoot.computeNetLuck(player);
        Set<Identifier> used = new HashSet<>();
        List<ArcaneDictionaryContents.TraitSlot> traits = new ArrayList<>();

        boolean catalystBoost = !catalyst.isEmpty()
                && ArcaneDictionaryCatalystBoost.matchesAny(catalyst, validPool, player.level());
        ItemStack catalystForMatching = catalystBoost ? catalyst.copy() : ItemStack.EMPTY;
        if (catalystBoost) {
            catalyst.shrink(1);
        }

        for (int slot = 0; slot < traitCount; slot++) {
            int remaining = traitCount - slot;
            List<ArcaneDictionaryDefinition.Entry> pickPool = poolForSlot(validPool, used, remaining);

            ArcaneDictionaryDefinition.Entry entry;
            if (catalystBoost) {
                entry = ArcaneDictionaryCatalystBoost.pickWithLuckAndCatalyst(
                        random, pickPool, netLuck, catalystForMatching, player.level());
            } else {
                entry = ArcaneDictionaryLoot.pickWithLuck(random, pickPool, netLuck);
            }

            if (entry == null) {
                continue;
            }
            if (ArcaneDictionaryEffectRegistry.get(entry.enchant()) == null) {
                ArcaneDictionaryEffectRegistry.warnUnknown(entry.enchant());
                continue;
            }
            traits.add(new ArcaneDictionaryContents.TraitSlot(entry.enchant(), traitLevel));
            used.add(entry.enchant());
        }

        return traits;
    }

    /** Unique traits when enough remain; otherwise the full valid pool (repeats allowed). */
    private static List<ArcaneDictionaryDefinition.Entry> poolForSlot(
            List<ArcaneDictionaryDefinition.Entry> validPool,
            Set<Identifier> used,
            int remainingSlots) {
        long unusedUnique = validPool.stream().filter(e -> !used.contains(e.enchant())).count();
        if (unusedUnique >= remainingSlots) {
            return validPool.stream().filter(e -> !used.contains(e.enchant())).toList();
        }
        return validPool;
    }
}
