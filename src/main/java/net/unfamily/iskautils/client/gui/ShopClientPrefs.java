package net.unfamily.iskautils.client.gui;

import org.jetbrains.annotations.Nullable;

/**
 * A4: Client-side storage for player shop UI preferences.
 * Preferences are preserved within a play session and synced to server via
 * {@link net.unfamily.iskautils.network.packet.ShopUiPrefsC2SPacket}.
 */
public final class ShopClientPrefs {

    private static ShopBrowsePanel.SearchScope scope = ShopBrowsePanel.SearchScope.ALL;
    @Nullable
    private static String currencyFilter = null;
    private static ShopBrowsePanel.TradeVisibility tradeVisibility = ShopBrowsePanel.TradeVisibility.SHOW;
    private static ShopBrowsePanel.SortMode sortMode = ShopBrowsePanel.SortMode.PRIORITY;

    private ShopClientPrefs() {}

    public static ShopBrowsePanel.SearchScope getScope() { return scope; }
    @Nullable
    public static String getCurrencyFilter() { return currencyFilter; }
    public static ShopBrowsePanel.TradeVisibility getTradeVisibility() { return tradeVisibility; }
    public static ShopBrowsePanel.SortMode getSortMode() { return sortMode; }

    public static void setScope(ShopBrowsePanel.SearchScope s) {
        scope = s != null ? s : ShopBrowsePanel.SearchScope.ALL;
        saveToServer();
    }

    public static void setCurrencyFilter(@Nullable String cf) {
        currencyFilter = cf;
        saveToServer();
    }

    public static void setTradeVisibility(ShopBrowsePanel.TradeVisibility tv) {
        tradeVisibility = tv != null ? tv : ShopBrowsePanel.TradeVisibility.SHOW;
        saveToServer();
    }

    public static void setSortMode(ShopBrowsePanel.SortMode sm) {
        sortMode = sm != null ? sm : ShopBrowsePanel.SortMode.PRIORITY;
        saveToServer();
    }

    /** Apply server-loaded prefs from NBT (called via S2C on shop open). */
    public static void applyFromServer(String scopeName, @Nullable String currencyId,
                                        String visibilityName, String sortName) {
        try { scope = ShopBrowsePanel.SearchScope.valueOf(scopeName); } catch (Exception ignored) {}
        currencyFilter = currencyId;
        try { tradeVisibility = ShopBrowsePanel.TradeVisibility.valueOf(visibilityName); } catch (Exception ignored) {}
        try { sortMode = ShopBrowsePanel.SortMode.valueOf(sortName); } catch (Exception ignored) {}
    }

    /** Restore preferences into a browse panel instance. */
    public static void restoreInto(ShopBrowsePanel panel) {
        panel.restorePrefs(scope, currencyFilter, tradeVisibility, sortMode);
    }

    private static void saveToServer() {
        net.unfamily.iskautils.network.ModMessages.sendShopUiPrefs(
                scope.name(), currencyFilter, tradeVisibility.name(), sortMode.name());
    }
}
