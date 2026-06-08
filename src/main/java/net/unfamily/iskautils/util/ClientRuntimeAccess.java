package net.unfamily.iskautils.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Reflective access to client-only Minecraft runtime APIs from common code.
 */
public final class ClientRuntimeAccess {
    private static final String MINECRAFT_CLASS = "net.minecraft.client.Minecraft";

    private ClientRuntimeAccess() {}

    public static @Nullable MinecraftServer getSingleplayerServer() {
        if (!ClientPlayerAccess.isClientEnvironment()) {
            return null;
        }
        try {
            Object mc = Class.forName(MINECRAFT_CLASS).getMethod("getInstance").invoke(null);
            Object server = mc.getClass().getMethod("getSingleplayerServer").invoke(mc);
            return server instanceof MinecraftServer minecraftServer ? minecraftServer : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static @Nullable ServerPlayer getFirstSingleplayerPlayer() {
        MinecraftServer server = getSingleplayerServer();
        if (server == null || server.getPlayerList().getPlayers().isEmpty()) {
            return null;
        }
        return server.getPlayerList().getPlayers().get(0);
    }

    public static void runOnSingleplayerServer(Runnable action) {
        MinecraftServer server = getSingleplayerServer();
        if (server != null) {
            server.execute(action);
        }
    }

    public static void runOnFirstSingleplayerPlayer(Consumer<ServerPlayer> action) {
        runOnSingleplayerServer(() -> {
            ServerPlayer player = getFirstSingleplayerPlayer();
            if (player != null) {
                action.accept(player);
            }
        });
    }

    public static void runOnClientThread(Runnable action) {
        if (!ClientPlayerAccess.isClientEnvironment()) {
            return;
        }
        try {
            Object mc = Class.forName(MINECRAFT_CLASS).getMethod("getInstance").invoke(null);
            mc.getClass().getMethod("execute", Runnable.class).invoke(mc, action);
        } catch (Throwable ignored) {
        }
    }

    public static @Nullable Level getClientLevel() {
        if (!ClientPlayerAccess.isClientEnvironment()) {
            return null;
        }
        try {
            Object mc = Class.forName(MINECRAFT_CLASS).getMethod("getInstance").invoke(null);
            Object level;
            try {
                level = mc.getClass().getMethod("getLevel").invoke(mc);
            } catch (NoSuchMethodException ignored) {
                level = mc.getClass().getField("level").get(mc);
            }
            return level instanceof Level l ? l : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
