package net.unfamily.iskautils.util;

import net.minecraft.util.RandomSource;

/** Stateless random helpers with no mod block/entity dependencies. */
public final class RandomRollUtil {
    private RandomRollUtil() {}

    public static int rollInclusive(int min, int max, RandomSource random) {
        int lo = Math.min(min, max);
        int hi = Math.max(min, max);
        if (lo == hi) {
            return lo;
        }
        return lo + random.nextInt(hi - lo + 1);
    }
}
