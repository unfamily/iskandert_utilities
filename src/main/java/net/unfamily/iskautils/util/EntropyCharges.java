package net.unfamily.iskautils.util;

import net.unfamily.iskautils.Config;

/**
 * Shared entropy charge math for Ancient Table and Arcane Dictionary.
 */
public final class EntropyCharges {
    private EntropyCharges() {}

    public static int chargePerDrop() {
        return Math.max(1, Config.entropyChargePerDrop);
    }

    public static boolean canAbsorbOneMore(int stored, int maxStored) {
        return stored + chargePerDrop() <= maxStored;
    }

    public static int absorbOneDrop(int stored, int maxStored) {
        if (!canAbsorbOneMore(stored, maxStored)) {
            return stored;
        }
        return stored + chargePerDrop();
    }

    public static int consume(int stored, int cost) {
        if (cost <= 0) {
            return stored;
        }
        return Math.max(0, stored - cost);
    }

    /** Drops needed to cover {@code deficit} without exceeding {@code maxStored}. */
    public static int dropsNeededForDeficit(int stored, int maxStored, int deficit) {
        if (deficit <= 0) {
            return 0;
        }
        int need = deficit;
        int drops = 0;
        int sim = stored;
        while (sim < stored + deficit && drops < 10000) {
            if (!canAbsorbOneMore(sim, maxStored)) {
                break;
            }
            sim += chargePerDrop();
            drops++;
            if (sim - stored >= need) {
                break;
            }
        }
        return drops;
    }
}
