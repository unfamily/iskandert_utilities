package net.unfamily.iskautils.client.gui;

import net.unfamily.iskautils.shop.ShopPurchaseLimitsData;

import java.util.HashMap;
import java.util.Map;

/** Client cache of server-evaluated shop trade limits for the current player. */
public final class ShopClientPurchaseLimits {
    private static final Map<String, Long> BLOCKED = new HashMap<>();

    private ShopClientPurchaseLimits() {}

    public static void replace(Map<String, Long> blocked) {
        BLOCKED.clear();
        BLOCKED.putAll(blocked);
    }

    public static boolean isBlocked(String entryId, ShopPurchaseLimitsData.TradeSide side) {
        return BLOCKED.containsKey(key(entryId, side));
    }

    public static long nextResetEpochMs(String entryId, ShopPurchaseLimitsData.TradeSide side) {
        return BLOCKED.getOrDefault(key(entryId, side), -1L);
    }

    public static String key(String entryId, ShopPurchaseLimitsData.TradeSide side) {
        return entryId + "|" + side.id();
    }
}
