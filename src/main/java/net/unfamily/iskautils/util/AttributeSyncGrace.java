package net.unfamily.iskautils.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * After login, respawn, or dimension change, Curios and equip stages may not be ready immediately.
 * Transient max-health modifiers are not saved — defer stripping and re-apply aggressively during grace.
 */
public final class AttributeSyncGrace {
    /** Five seconds at 20 TPS — enough for Curios to sync after world rejoin. */
    public static final int GRACE_TICKS = ArtifactTickIntervals.FAST_TICKS * 5;
    /** Require this many consecutive ticks without equipment before stripping a bonus. */
    public static final int CONFIRM_ABSENT_TICKS = ArtifactTickIntervals.FAST_TICKS * 2;

    private static final Map<String, Integer> ABSENT_STREAKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> GRACE_END_TICK = new ConcurrentHashMap<>();

    private AttributeSyncGrace() {}

    /** Extends attribute/health resync window after login or dimension change. */
    public static void armGracePeriod(ServerPlayer player) {
        if (player != null) {
            GRACE_END_TICK.put(player.getUUID(), player.tickCount + GRACE_TICKS);
        }
    }

    public static boolean inGracePeriod(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        Integer endTick = GRACE_END_TICK.get(player.getUUID());
        if (endTick != null) {
            if (player.tickCount >= endTick) {
                GRACE_END_TICK.remove(player.getUUID());
                return false;
            }
            return true;
        }
        return player.tickCount < GRACE_TICKS;
    }

    public static boolean shouldDeferRemoval(ServerPlayer player) {
        return inGracePeriod(player);
    }

    /** Run attribute apply logic every tick during grace, otherwise on the normal cadence. */
    public static boolean shouldRunAttributePass(ServerPlayer player, long gameTime, int periodTicks) {
        return inGracePeriod(player) || ArtifactTickIntervals.isDue(gameTime, periodTicks);
    }

    /**
     * Returns true only after {@code bonusId} equipment has been absent for {@link #CONFIRM_ABSENT_TICKS}
     * ticks (never during grace). Each bonus tracks its own absent streak.
     */
    public static boolean shouldRemoveEquippedBonus(ServerPlayer player, ResourceLocation bonusId, boolean equipped) {
        if (player == null || bonusId == null) {
            return false;
        }
        String key = streakKey(player.getUUID(), bonusId);
        if (equipped) {
            ABSENT_STREAKS.remove(key);
            return false;
        }
        if (inGracePeriod(player)) {
            return false;
        }
        int streak = ABSENT_STREAKS.merge(key, 1, Integer::sum);
        return streak >= CONFIRM_ABSENT_TICKS;
    }

    public static void clearPlayer(UUID playerId) {
        if (playerId == null) {
            return;
        }
        String prefix = playerId + "|";
        ABSENT_STREAKS.keySet().removeIf(key -> key.startsWith(prefix));
        GRACE_END_TICK.remove(playerId);
    }

    private static String streakKey(UUID playerId, ResourceLocation bonusId) {
        return playerId + "|" + bonusId;
    }
}
