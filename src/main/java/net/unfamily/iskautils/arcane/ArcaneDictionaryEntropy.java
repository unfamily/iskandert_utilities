package net.unfamily.iskautils.arcane;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.util.EntropyCharges;

public final class ArcaneDictionaryEntropy {
    /** Entropy upkeep is charged once per interval (5 seconds at 20 TPS). */
    public static final int UPKEEP_PERIOD_TICKS = 100;

    private ArcaneDictionaryEntropy() {}

    public static int maxStored() {
        return Math.max(0, Config.arcaneDictionaryMaxStored);
    }

    /** Each fast tick: absorb drops into the single dictionary equipped in Curios (no waste past cap). */
    public static void tickInventoryRefill(ServerPlayer player) {
        ItemStack dictionary = ArcaneDictionaryActivation.singleCurioDictionary(player);
        if (dictionary == null) {
            return;
        }
        tryAbsorbDrops(dictionary, player);
    }

    public static boolean consume(ItemStack dictionary, int cost) {
        if (cost <= 0) {
            return true;
        }
        int stored = ArcaneDictionaryContents.getStoredEntropy(dictionary);
        if (stored < cost) {
            return false;
        }
        ArcaneDictionaryContents.setStoredEntropy(dictionary, EntropyCharges.consume(stored, cost));
        return true;
    }

    public static int computeTotalUpkeep(ItemStack dictionary) {
        return ArcaneDictionaryContents.computeTotalUpkeep(dictionary);
    }

    /** Consumes stored entropy on upkeep interval ticks; between payments, traits stay active while any charge remains. */
    public static boolean tickEffectUpkeep(ItemStack dictionary, int upkeep, long gameTime) {
        if (upkeep <= 0) {
            return true;
        }
        if (gameTime % UPKEEP_PERIOD_TICKS == 0) {
            return consume(dictionary, upkeep);
        }
        return ArcaneDictionaryContents.getStoredEntropy(dictionary) > 0;
    }

    public static boolean canAffordUpkeep(ItemStack dictionary, int upkeep, long gameTime) {
        if (upkeep <= 0) {
            return true;
        }
        if (gameTime % UPKEEP_PERIOD_TICKS == 0) {
            return hasStoredCharges(dictionary, upkeep);
        }
        return ArcaneDictionaryContents.getStoredEntropy(dictionary) > 0;
    }

    public static boolean hasStoredCharges(ItemStack dictionary, int minimum) {
        return ArcaneDictionaryContents.getStoredEntropy(dictionary) >= minimum;
    }

    private static void tryAbsorbDrops(ItemStack dictionary, ServerPlayer player) {
        int stored = ArcaneDictionaryContents.getStoredEntropy(dictionary);
        int max = maxStored();
        boolean changed = false;
        while (EntropyCharges.canAbsorbOneMore(stored, max)) {
            ItemStack drop = findEntropyDropInInventory(player);
            if (drop.isEmpty()) {
                break;
            }
            drop.shrink(1);
            stored = EntropyCharges.absorbOneDrop(stored, max);
            changed = true;
        }
        if (changed) {
            ArcaneDictionaryContents.setStoredEntropy(dictionary, stored);
        }
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
