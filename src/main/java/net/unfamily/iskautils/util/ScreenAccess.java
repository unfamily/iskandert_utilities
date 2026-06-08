package net.unfamily.iskautils.util;

public final class ScreenAccess {
    private ScreenAccess() {}

    public static boolean hasShiftDown() {
        if (!ClientPlayerAccess.isClientEnvironment()) {
            return false;
        }
        try {
            Class<?> screen = Class.forName("net.minecraft.client.gui.screens.Screen");
            return (boolean) screen.getMethod("hasShiftDown").invoke(null);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
