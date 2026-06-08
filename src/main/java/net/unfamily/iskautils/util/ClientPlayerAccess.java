package net.unfamily.iskautils.util;

import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jspecify.annotations.Nullable;

/**
 * Reflective access to the local client player so common code never links {@code LocalPlayer} on dedicated servers.
 */
public final class ClientPlayerAccess {
    private ClientPlayerAccess() {}

    public static boolean isClientEnvironment() {
        return FMLEnvironment.getDist() == Dist.CLIENT;
    }

    public static @Nullable Player getLocalPlayer() {
        if (!isClientEnvironment()) {
            return null;
        }
        try {
            Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
            Object mc = mcClass.getMethod("getInstance").invoke(null);
            Object player;
            try {
                player = mcClass.getMethod("getPlayer").invoke(mc);
            } catch (NoSuchMethodException ignored) {
                player = mcClass.getField("player").get(mc);
            }
            return player instanceof Player p ? p : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
