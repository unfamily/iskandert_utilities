package net.unfamily.iskautils.util;

import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.item.ModItems;

/**
 * Ancient Table internal fuel buffer (no item NBT). Entropy drops in the fuel slot are converted
 * one item at a time when {@code stored + chargePerDrop <= maxStored}.
 */
public final class AncientTableFuel {
    private AncientTableFuel() {}

    public static boolean isEntropyFuel(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.DROP_OF_ENTROPY.get());
    }

    public static int maxStored() {
        return Math.max(1, Config.entropyAncientTableMaxStored);
    }

    public static int fuelPerDrop() {
        return EntropyCharges.chargePerDrop();
    }

    public static boolean canAbsorbOneMore(int storedFuel) {
        return EntropyCharges.canAbsorbOneMore(storedFuel, maxStored());
    }

    public static int comparatorFromFuelSlot(ItemStack slotStack) {
        if (slotStack.isEmpty()) {
            return 0;
        }
        int max = slotStack.getMaxStackSize();
        if (max <= 0) {
            return 0;
        }
        return net.minecraft.util.Mth.clamp((int) Math.floor(15.0 * slotStack.getCount() / max), 0, 15);
    }
}
