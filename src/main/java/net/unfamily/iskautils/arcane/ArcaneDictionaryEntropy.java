package net.unfamily.iskautils.arcane;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.arcane.effects.EntropicCapacityEffect;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.util.EntropyCharges;

public final class ArcaneDictionaryEntropy {
    /** Entropy consume is charged once per interval (5 seconds at 20 TPS). */
    public static final int CONSUME_PERIOD_TICKS = 100;

    private ArcaneDictionaryEntropy() {}

    public static int maxStored() {
        return Math.max(0, Config.arcaneDictionaryMaxStored);
    }

    public static int maxStored(ItemStack dictionary, ServerPlayer player) {
        return maxStored() + EntropicCapacityEffect.bonusStored(dictionary, player);
    }

    /** Each fast tick: absorb drops into the single dictionary equipped in Curios (no waste past cap). */
    public static void tickInventoryRefill(ServerPlayer player) {
        ItemStack dictionary = ArcaneDictionaryActivation.singleCurioDictionary(player);
        if (dictionary == null) {
            return;
        }
        tryAbsorbDrops(dictionary, player);
    }

    public static boolean consume(ItemStack dictionary, int cost, ServerPlayer player) {
        if (cost <= 0) {
            return true;
        }
        int stored = ArcaneDictionaryContents.getStoredEntropy(dictionary);
        if (stored < cost) {
            return false;
        }
        ArcaneDictionaryContents.setStoredEntropy(
                dictionary, EntropyCharges.consume(stored, cost), maxStored(dictionary, player));
        return true;
    }

    public static boolean consume(ItemStack dictionary, int cost) {
        return consume(dictionary, cost, null);
    }

    public static int computeTotalConsume(ItemStack dictionary) {
        return ArcaneDictionaryContents.computeTotalConsume(dictionary);
    }

    /** Consumes stored entropy on consume interval ticks; between payments, traits stay active while any charge remains. */
    public static boolean tickEffectConsume(
            ItemStack dictionary, int periodConsume, long gameTime, ServerPlayer player) {
        if (periodConsume <= 0) {
            return true;
        }
        if (gameTime % CONSUME_PERIOD_TICKS == 0) {
            return consume(dictionary, periodConsume, player);
        }
        return ArcaneDictionaryContents.getStoredEntropy(dictionary) > 0;
    }

    public static boolean tickEffectConsume(ItemStack dictionary, int periodConsume, long gameTime) {
        return tickEffectConsume(dictionary, periodConsume, gameTime, null);
    }

    public static boolean canAffordConsume(ItemStack dictionary, int periodConsume, long gameTime) {
        if (periodConsume <= 0) {
            return true;
        }
        if (gameTime % CONSUME_PERIOD_TICKS == 0) {
            return hasStoredCharges(dictionary, periodConsume);
        }
        return ArcaneDictionaryContents.getStoredEntropy(dictionary) > 0;
    }

    public static boolean hasStoredCharges(ItemStack dictionary, int minimum) {
        return ArcaneDictionaryContents.getStoredEntropy(dictionary) >= minimum;
    }

    private static void tryAbsorbDrops(ItemStack dictionary, ServerPlayer player) {
        int stored = ArcaneDictionaryContents.getStoredEntropy(dictionary);
        int max = maxStored(dictionary, player);
        int funnelLevel = entropyFunnelLevel(dictionary, player);
        boolean changed = false;
        while (EntropyCharges.canAbsorbOneMore(stored, max)) {
            ItemStack drop = findEntropyDropInInventory(player);
            if (drop.isEmpty()) {
                break;
            }
            drop.shrink(1);
            stored = EntropyCharges.absorbOneDrop(stored, max);
            if (funnelLevel > 0) {
                int bonus = (int) Math.floor(Config.arcaneEntropyFunnelBonusChargesPerLevel * funnelLevel);
                stored = Math.min(max, stored + bonus);
            }
            changed = true;
        }
        if (changed) {
            ArcaneDictionaryContents.setStoredEntropy(dictionary, stored, max);
        }
    }

    private static int entropyFunnelLevel(ItemStack dictionary, ServerPlayer player) {
        for (ArcaneDictionaryContents.TraitSlot trait : ArcaneDictionaryContents.getTraits(dictionary)) {
            if (!ArcaneDictionaryTraitIds.ENTROPY_FUNNEL.equals(trait.id())) {
                continue;
            }
            if (!ArcaneDictionaryEntryGate.traitActive(player, trait.id())) {
                return 0;
            }
            return trait.level();
        }
        return 0;
    }

    private static ItemStack findEntropyDropInInventory(ServerPlayer player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.is(ModItems.DROP_OF_ENTROPY.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
