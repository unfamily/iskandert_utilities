package net.unfamily.iskautils.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

/**
 * Transient max-health bonuses are not saved. On rejoin, max health drops and current health is clamped;
 * when bonuses are re-applied, empty heart containers remain unless we restore health from the saved ratio.
 */
public final class BonusHealthResync {
    private static final String HEALTH_RATIO_KEY = "iska_health_ratio";
    private static final String RESYNC_ACTIVE_KEY = "iska_health_resync";

    private BonusHealthResync() {}

    public static void snapshotHealthRatio(ServerPlayer player) {
        float max = player.getMaxHealth();
        if (max <= 0.0F) {
            return;
        }
        float ratio = Mth.clamp(player.getHealth() / max, 0.0F, 1.0F);
        player.getPersistentData().putFloat(HEALTH_RATIO_KEY, ratio);
    }

    public static void beginResync(ServerPlayer player) {
        AttributeSyncGrace.armGracePeriod(player);
        player.getPersistentData().putBoolean(RESYNC_ACTIVE_KEY, true);
    }

    public static void tick(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean(RESYNC_ACTIVE_KEY).orElse(false)) {
            return;
        }
        if (AttributeSyncGrace.inGracePeriod(player)) {
            applySavedRatio(player);
            return;
        }
        applySavedRatio(player);
        data.remove(RESYNC_ACTIVE_KEY);
    }

    private static void applySavedRatio(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(HEALTH_RATIO_KEY)) {
            return;
        }
        float max = player.getMaxHealth();
        if (max <= 0.0F) {
            return;
        }
        float target = data.getFloat(HEALTH_RATIO_KEY).orElse(1.0F) * max;
        if (player.getHealth() < target - 0.01F) {
            player.setHealth(Math.min(target, max));
        }
    }
}
