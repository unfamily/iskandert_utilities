package net.unfamily.iskautils.util;

/**
 * Shared server tick cadence for artifact / arcane passive effects (20 TPS).
 */
public final class ArtifactTickIntervals {
    /** 1 second — attribute sync, stage refresh, trait ticks, entropy absorb. */
    public static final int FAST_TICKS = 20;
    /** 3 seconds — low-priority passive maintenance. */
    public static final int SLOW_TICKS = 60;
    /** 5 seconds — duration applied when refreshing trait mob effects. */
    public static final int POTION_DURATION_TICKS = 100;

    private ArtifactTickIntervals() {}

    public static boolean isDue(long gameTime, int periodTicks) {
        return periodTicks > 0 && gameTime % periodTicks == 0L;
    }
}
