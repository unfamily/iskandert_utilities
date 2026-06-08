package net.unfamily.iskautils.util;

import java.util.Map;

/**
 * Reflective bridge to client GUI callbacks invoked from common networking code.
 */
public final class ClientGuiAccess {
    private static final String SHOP_SCREEN = "net.unfamily.iskautils.client.gui.ShopScreen";

    private ClientGuiAccess() {}

    public static void handleShopTeamDataUpdate(String teamName, Map<String, Double> teamBalances) {
        invokeStatic(SHOP_SCREEN, "handleTeamDataUpdate", new Class<?>[] {String.class, Map.class}, teamName, teamBalances);
    }

    public static void notifyShopReload() {
        invokeStaticNoArgs(SHOP_SCREEN, "notifyReload");
    }

    public static void handleShopTransactionError(String errorType, String itemId, String valuteId) {
        invokeStatic(SHOP_SCREEN, "handleTransactionError", new Class<?>[] {String.class, String.class, String.class},
                errorType, itemId, valuteId);
    }

    public static void handleShopTransactionSuccess() {
        invokeStaticNoArgs(SHOP_SCREEN, "handleTransactionSuccess");
    }

    private static void invokeStaticNoArgs(String className, String method) {
        if (!ClientPlayerAccess.isClientEnvironment()) {
            return;
        }
        try {
            Class<?> clz = Class.forName(className);
            clz.getMethod(method).invoke(null);
        } catch (Throwable ignored) {
        }
    }

    private static void invokeStatic(String className, String method, Class<?>[] paramTypes, Object... args) {
        if (!ClientPlayerAccess.isClientEnvironment()) {
            return;
        }
        try {
            Class<?> clz = Class.forName(className);
            clz.getMethod(method, paramTypes).invoke(null, args);
        } catch (Throwable ignored) {
        }
    }

}
