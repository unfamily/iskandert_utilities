package net.unfamily.iskautils.util;

import net.unfamily.iskautils.network.packet.DeepDrawersSyncSlotsS2CPacket;

public final class DeepDrawersSyncClientAccess {
    private static final String CLIENT_HANDLER = "net.unfamily.iskautils.client.gui.DeepDrawersSyncClient";

    private DeepDrawersSyncClientAccess() {}

    public static void handle(DeepDrawersSyncSlotsS2CPacket packet) {
        if (!ClientPlayerAccess.isClientEnvironment()) {
            return;
        }
        try {
            Class<?> clz = Class.forName(CLIENT_HANDLER);
            clz.getMethod("handle", DeepDrawersSyncSlotsS2CPacket.class).invoke(null, packet);
        } catch (Throwable ignored) {
        }
    }
}
