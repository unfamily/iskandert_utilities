package net.unfamily.iskautils.util;

import net.minecraft.server.level.ServerPlayer;

/**
 * After login (or respawn), Curios and equip stages may not be ready for a few ticks.
 * Defer stripping transient max-health modifiers until then to avoid losing bonus hearts briefly.
 */
public final class AttributeSyncGrace {
    /** Two seconds at 20 TPS — matches {@link ArtifactTickIntervals#FAST_TICKS} cadence. */
    public static final int GRACE_TICKS = ArtifactTickIntervals.FAST_TICKS * 2;

    private AttributeSyncGrace() {}

    public static boolean shouldDeferRemoval(ServerPlayer player) {
        return player != null && player.tickCount < GRACE_TICKS;
    }
}
