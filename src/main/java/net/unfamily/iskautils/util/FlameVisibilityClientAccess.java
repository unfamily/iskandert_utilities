package net.unfamily.iskautils.util;

import net.minecraft.world.entity.player.Player;

/**
 * Reflective bridge to {@code FlameVisibilityClient} so common code stays server-safe.
 */
public final class FlameVisibilityClientAccess {
    private static final String CLIENT_CLASS = "net.unfamily.iskautils.client.FlameVisibilityClient";

    private FlameVisibilityClientAccess() {}

    public static void applyGlobalFlameVision(boolean enabled) {
        if (!ClientPlayerAccess.isClientEnvironment()) {
            return;
        }
        try {
            Class<?> clz = Class.forName(CLIENT_CLASS);
            clz.getMethod("applyGlobalFlameVision", boolean.class).invoke(null, enabled);
        } catch (Throwable ignored) {
        }
    }

    public static boolean shouldShowFlames(Player player) {
        if (!ClientPlayerAccess.isClientEnvironment()) {
            return true;
        }
        try {
            Class<?> clz = Class.forName(CLIENT_CLASS);
            return (boolean) clz.getMethod("shouldShowFlames", Player.class).invoke(null, player);
        } catch (Throwable ignored) {
            return true;
        }
    }
}
