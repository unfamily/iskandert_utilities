package net.unfamily.iskautils.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Tracks Lasting Candy invulnerability and cooldown windows using server game time.
 */
public final class LastingCandyPlayerState {
    private static final String INVULN_UNTIL = "lasting_candy_invuln_until";
    private static final String COOLDOWN_UNTIL = "lasting_candy_cooldown_until";

    private LastingCandyPlayerState() {}

    public static boolean isInvulnerable(Player player, long gameTime) {
        return gameTime < getLong(player, INVULN_UNTIL);
    }

    public static boolean isOnCooldown(Player player, long gameTime) {
        return gameTime < getLong(player, COOLDOWN_UNTIL);
    }

    public static void beginInvulnerability(Player player, long gameTime, int invulnSeconds, int cooldownSeconds) {
        long invulnUntil = gameTime + invulnSeconds * 20L;
        long cooldownUntil = invulnUntil + cooldownSeconds * 20L;
        setLong(player, INVULN_UNTIL, invulnUntil);
        setLong(player, COOLDOWN_UNTIL, cooldownUntil);
    }

    private static long getLong(Player player, String key) {
        try {
            CompoundTag persistentData = player.getPersistentData();
            if (!persistentData.contains("iskautils")) {
                return 0L;
            }
            CompoundTag iskaData = persistentData.getCompound("iskautils").orElse(null);
            if (iskaData == null || !iskaData.contains("longValues")) {
                return 0L;
            }
            CompoundTag longValues = iskaData.getCompound("longValues").orElse(null);
            if (longValues == null) {
                return 0L;
            }
            return longValues.getLong(key).orElse(0L);
        } catch (Exception e) {
            return 0L;
        }
    }

    private static void setLong(Player player, String key, long value) {
        try {
            CompoundTag persistentData = player.getPersistentData();
            CompoundTag iskaData = persistentData.contains("iskautils")
                    ? persistentData.getCompound("iskautils").orElse(new CompoundTag())
                    : new CompoundTag();
            CompoundTag longValues = iskaData.contains("longValues")
                    ? iskaData.getCompound("longValues").orElse(new CompoundTag())
                    : new CompoundTag();
            longValues.putLong(key, value);
            iskaData.put("longValues", longValues);
            persistentData.put("iskautils", iskaData);
        } catch (Exception ignored) {
            // Silently fail
        }
    }
}
