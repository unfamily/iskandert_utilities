package net.unfamily.iskautils.util;

import net.minecraft.core.BlockPos;

/**
 * Reflective bridge to {@code ClientEvents} highlight handlers.
 */
public final class ClientEventsAccess {
    private static final String CLIENT_EVENTS_CLASS = "net.unfamily.iskautils.client.ClientEvents";

    private ClientEventsAccess() {}

    public static void handleAddHighlight(BlockPos pos, int color, int durationTicks) {
        invoke("handleAddHighlight", new Class<?>[] {BlockPos.class, int.class, int.class}, pos, color, durationTicks);
    }

    public static void handleAddHighlightWithName(BlockPos pos, int color, int durationTicks, String name) {
        invoke("handleAddHighlightWithName", new Class<?>[] {BlockPos.class, int.class, int.class, String.class},
                pos, color, durationTicks, name);
    }

    public static void handleAddBillboard(BlockPos pos, int color, int durationTicks) {
        invoke("handleAddBillboard", new Class<?>[] {BlockPos.class, int.class, int.class}, pos, color, durationTicks);
    }

    public static void handleAddBillboardWithName(BlockPos pos, int color, int durationTicks, String name) {
        invoke("handleAddBillboardWithName", new Class<?>[] {BlockPos.class, int.class, int.class, String.class},
                pos, color, durationTicks, name);
    }

    public static void handleRemoveHighlight(BlockPos pos) {
        invoke("handleRemoveHighlight", new Class<?>[] {BlockPos.class}, pos);
    }

    public static void handleClearHighlights() {
        invoke("handleClearHighlights", new Class<?>[0]);
    }

    private static void invoke(String method, Class<?>[] paramTypes, Object... args) {
        if (!ClientPlayerAccess.isClientEnvironment()) {
            return;
        }
        try {
            Class<?> clz = Class.forName(CLIENT_EVENTS_CLASS);
            clz.getMethod(method, paramTypes).invoke(null, args);
        } catch (Throwable ignored) {
        }
    }
}
