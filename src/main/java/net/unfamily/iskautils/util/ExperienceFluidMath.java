package net.unfamily.iskautils.util;

import net.unfamily.iskautils.Config;

/**
 * Converts between experience points, display levels, and fluid millibuckets.
 */
public final class ExperienceFluidMath {
    public static final int DEFAULT_MB_PER_XP_POINT = 20;

    private ExperienceFluidMath() {}

    public static int mbPerXpPoint() {
        return Config.collectingCrateXpMbPerPoint > 0 ? Config.collectingCrateXpMbPerPoint : DEFAULT_MB_PER_XP_POINT;
    }

    public static long levelsToXp(int levels) {
        if (levels <= 16) {
            return (long) (Math.pow(levels, 2) + 6L * levels);
        } else if (levels <= 31) {
            return (long) (2.5 * Math.pow(levels, 2) - 40.5 * levels + 360);
        } else {
            return (long) (4.5 * Math.pow(levels, 2) - 162.5 * levels + 2220);
        }
    }

    public static int xpToLevels(long xp) {
        if (xp < 394) {
            return (int) (Math.sqrt(xp + 9) - 3);
        } else if (xp < 1628) {
            return (int) ((Math.sqrt(40 * xp - 7839) + 81) * 0.1);
        } else {
            return (int) ((Math.sqrt(72 * xp - 54215) + 325) / 18);
        }
    }

    /** Total mb capacity for the configured level cap (may exceed {@link Integer#MAX_VALUE}). */
    public static long capacityMbFromLevels(int levels) {
        return levelsToXp(levels) * (long) mbPerXpPoint();
    }

    public static long xpPointsFromMb(long mb) {
        return mb / mbPerXpPoint();
    }

    public static int displayLevelsFromMb(long mb) {
        return xpToLevels(xpPointsFromMb(mb));
    }

    public static double displayProgressFromMb(long mb) {
        long points = xpPointsFromMb(mb);
        int levels = displayLevelsFromMb(mb);
        long levelBase = levelsToXp(levels);
        long nextLevel = levelsToXp(levels + 1);
        if (nextLevel <= levelBase) {
            return 0.0;
        }
        return (double) (points - levelBase) / (nextLevel - levelBase);
    }

    public static long mbFromXpPoints(long points) {
        return points * (long) mbPerXpPoint();
    }

    /** @deprecated Use {@link #xpPointsFromMb(long)} */
    @Deprecated
    public static int xpPointsFromMb(int mb) {
        return (int) Math.min(xpPointsFromMb((long) mb), Integer.MAX_VALUE);
    }

    /** @deprecated Use {@link #mbFromXpPoints(long)} */
    @Deprecated
    public static int mbFromXpPoints(int points) {
        return (int) Math.min(mbFromXpPoints((long) points), Integer.MAX_VALUE);
    }

    /** @deprecated Use {@link #displayLevelsFromMb(long)} */
    @Deprecated
    public static int displayLevelsFromMb(int mb) {
        return displayLevelsFromMb((long) mb);
    }

    /** @deprecated Use {@link #displayProgressFromMb(long)} */
    @Deprecated
    public static double displayProgressFromMb(int mb) {
        return displayProgressFromMb((long) mb);
    }
}
