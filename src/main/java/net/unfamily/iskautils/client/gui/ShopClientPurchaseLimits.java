package net.unfamily.iskautils.client.gui;

import net.unfamily.iskautils.shop.ShopPurchaseLimitsData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Client cache of server-evaluated shop trade limits for the current player.
 * Stores used/max/resetEpochMs for every entry with a non-always rule (A9).
 */
public final class ShopClientPurchaseLimits {

    public record LimitEntry(int used, int max, long resetEpochMs) {
        public boolean isBlocked() { return used >= max; }
    }

    private static final Map<String, LimitEntry> LIMITS = new HashMap<>();

    private ShopClientPurchaseLimits() {}

    public static void replace(Map<String, long[]> data) {
        LIMITS.clear();
        data.forEach((key, arr) -> {
            // arr[0]=used, arr[1]=max, arr[2]=resetEpochMs
            LIMITS.put(key, new LimitEntry((int) arr[0], (int) arr[1], arr[2]));
        });
    }

    public static boolean isBlocked(String entryId, ShopPurchaseLimitsData.TradeSide side) {
        LimitEntry le = LIMITS.get(key(entryId, side));
        return le != null && le.isBlocked();
    }

    public static long nextResetEpochMs(String entryId, ShopPurchaseLimitsData.TradeSide side) {
        LimitEntry le = LIMITS.get(key(entryId, side));
        return le != null ? le.resetEpochMs() : -1L;
    }

    @Nullable
    public static LimitEntry getLimitEntry(String entryId, ShopPurchaseLimitsData.TradeSide side) {
        return LIMITS.get(key(entryId, side));
    }

    public static boolean hasLimit(String entryId, ShopPurchaseLimitsData.TradeSide side) {
        return LIMITS.containsKey(key(entryId, side));
    }

    public static String key(String entryId, ShopPurchaseLimitsData.TradeSide side) {
        return entryId + "|" + side.id();
    }
}
