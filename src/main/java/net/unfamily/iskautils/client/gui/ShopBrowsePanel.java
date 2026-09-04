package net.unfamily.iskautils.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.shop.ShopCategory;
import net.unfamily.iskautils.shop.ShopCurrency;
import net.unfamily.iskautils.shop.ShopEntry;
import net.unfamily.iskautils.shop.ShopEntryHelper;
import net.unfamily.iskautils.shop.ShopLoader;
import net.unfamily.iskautils.util.DeepDrawerItemFilter;
import net.unfamily.iskalib.item.ItemConverter;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Client-side shop browse state: search, scope/currency filters, and filtered category/item lists.
 */
public final class ShopBrowsePanel {

    public enum SearchScope {
        ALL,
        BUYABLE,
        SELLABLE,
        CATEGORY
    }

    /** Show all entries (S, default) vs hide entries you can neither buy nor sell (H). */
    public enum TradeVisibility {
        SHOW,
        HIDE_UNTRADEABLE
    }

    public static final int GUI_WIDTH = 300;
    public static final int INVENTORY_Y = 154;
    public static final int ENTRY_WIDTH = 220;
    public static final int ENTRY_HEIGHT = 24;
    public static final int ENTRY_START_X = 19;
    public static final int ENTRY_START_Y = 20;
    public static final int MAX_VISIBLE_ENTRIES = 4;

    public static final int FILTER_ROW_HEIGHT = 16;
    public static final int SEARCH_BAR_HEIGHT = FILTER_ROW_HEIGHT;
    public static final int FILTER_BUTTON_HEIGHT = FILTER_ROW_HEIGHT;
    public static final int SCOPE_BUTTON_WIDTH = 16;
    public static final int CURRENCY_BUTTON_WIDTH = 36;
    public static final int AVAILABILITY_BUTTON_WIDTH = 16;
    public static final int FILTER_BUTTON_GAP = 2;

    /** Search + filter row replaces the first entry slot (same Y as original entry list). */
    public static final int SEARCH_ROW_Y = ENTRY_START_Y;
    public static final int SEARCH_BAR_X = ENTRY_START_X;
    public static final int SEARCH_BAR_WIDTH = ENTRY_WIDTH - SCOPE_BUTTON_WIDTH - CURRENCY_BUTTON_WIDTH
            - AVAILABILITY_BUTTON_WIDTH - 3 * FILTER_BUTTON_GAP;
    public static final int SEARCH_BAR_Y = SEARCH_ROW_Y + (ENTRY_HEIGHT - SEARCH_BAR_HEIGHT) / 2;
    public static final int FILTER_ROW_Y = SEARCH_BAR_Y;
    public static final int SCOPE_BUTTON_X = SEARCH_BAR_X + SEARCH_BAR_WIDTH + FILTER_BUTTON_GAP;
    public static final int CURRENCY_BUTTON_X = SCOPE_BUTTON_X + SCOPE_BUTTON_WIDTH + FILTER_BUTTON_GAP;
    public static final int AVAILABILITY_BUTTON_X = CURRENCY_BUTTON_X + CURRENCY_BUTTON_WIDTH + FILTER_BUTTON_GAP;

    /** @deprecated use {@link #SCOPE_BUTTON_WIDTH} */
    @Deprecated
    public static final int FILTER_BUTTON_SIZE = SCOPE_BUTTON_WIDTH;

    private SearchScope searchScope = SearchScope.ALL;
    @Nullable
    private String currencyFilterId = null;
    private TradeVisibility tradeVisibility = TradeVisibility.SHOW;
    private String searchQuery = "";
    private final boolean autoShopMode;

    private List<ShopCategory> allCategories = List.of();
    private List<ShopEntry> categoryItems = List.of();
    private List<ShopCategory> filteredCategories = List.of();
    private List<ShopEntry> filteredItems = List.of();

    public ShopBrowsePanel() {
        this(false);
    }

    public ShopBrowsePanel(boolean autoShopMode) {
        this.autoShopMode = autoShopMode;
    }

    private boolean isBrowsable(ShopEntry entry) {
        return autoShopMode ? ShopEntryHelper.isAutoShopSelectable(entry) : ShopEntryHelper.isPlayerShopBrowsable(entry);
    }

    public int getEntryStartY() {
        return ENTRY_START_Y + ENTRY_HEIGHT;
    }

    /** Top of the entry list (first entry row). Scrollbar aligns here, below the search row. */
    public int getBrowseAreaStartY() {
        return getEntryStartY();
    }

    public int getVisibleEntryCount() {
        int max = (INVENTORY_Y - 4 - getEntryStartY()) / ENTRY_HEIGHT;
        return Math.max(1, Math.min(MAX_VISIBLE_ENTRIES, max));
    }

    public SearchScope getSearchScope() {
        return searchScope;
    }

    public void cycleSearchScope(boolean backward) {
        cycleSearchScope(backward, true);
    }

    public void cycleSearchScope(boolean backward, boolean categoryView) {
        SearchScope[] values = categoryView
                ? SearchScope.values()
                : new SearchScope[]{SearchScope.ALL, SearchScope.BUYABLE, SearchScope.SELLABLE};
        int idx = indexOfScope(values, searchScope);
        if (idx < 0) {
            searchScope = SearchScope.ALL;
            return;
        }
        if (backward) {
            idx = (idx - 1 + values.length) % values.length;
        } else {
            idx = (idx + 1) % values.length;
        }
        searchScope = values[idx];
    }

    public void resetSearchAndScope(boolean enteringCategory) {
        searchQuery = "";
        if (enteringCategory) {
            searchScope = SearchScope.ALL;
        }
    }

    private static int indexOfScope(SearchScope[] values, SearchScope scope) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == scope) {
                return i;
            }
        }
        return -1;
    }

    @Nullable
    public String getCurrencyFilterId() {
        return currencyFilterId;
    }

    public TradeVisibility getTradeVisibility() {
        return tradeVisibility;
    }

    public void cycleTradeVisibility() {
        tradeVisibility = tradeVisibility == TradeVisibility.SHOW
                ? TradeVisibility.HIDE_UNTRADEABLE
                : TradeVisibility.SHOW;
    }

    public String tradeVisibilityLetter() {
        return tradeVisibility == TradeVisibility.HIDE_UNTRADEABLE ? "H" : "S";
    }

    public void cycleCurrencyFilter(boolean backward) {
        List<String> ids = getSortedCurrencyIds();
        if (ids.isEmpty()) {
            currencyFilterId = null;
            return;
        }
        if (currencyFilterId == null) {
            currencyFilterId = backward ? ids.get(ids.size() - 1) : ids.get(0);
            return;
        }
        int idx = ids.indexOf(currencyFilterId);
        if (idx < 0) {
            currencyFilterId = backward ? ids.get(ids.size() - 1) : null;
            return;
        }
        if (backward) {
            currencyFilterId = idx == 0 ? null : ids.get(idx - 1);
        } else if (idx >= ids.size() - 1) {
            currencyFilterId = null;
        } else {
            currencyFilterId = ids.get(idx + 1);
        }
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query != null ? query : "";
    }

    public List<ShopCategory> getFilteredCategories() {
        return filteredCategories;
    }

    public List<ShopEntry> getFilteredItems() {
        return filteredItems;
    }

    public void loadAllCategories() {
        Map<String, ShopCategory> categories = ShopLoader.getCategories();
        allCategories = categories.values().stream()
                .sorted(Comparator.comparingInt((ShopCategory cat) -> cat.priority).reversed()
                        .thenComparing(cat -> cat.id))
                .collect(Collectors.toList());
        categoryItems = List.of();
        applyFilters(true);
    }

    public void loadCategoryItems(String categoryId) {
        Map<String, ShopEntry> allEntries = ShopLoader.getEntries();
        categoryItems = allEntries.values().stream()
                .filter(entry -> categoryId.equals(entry.inCategory))
                .filter(this::isBrowsable)
                .sorted(Comparator.comparingInt((ShopEntry e) -> e.priority).reversed()
                        .thenComparing(entry -> {
                            String selector = ShopEntryHelper.resourceSelector(entry);
                            return selector != null ? selector : "";
                        }))
                .collect(Collectors.toList());
        applyFilters(false);
    }

    public void applyFilters(boolean categoryView) {
        if (isFlatItemSearch(categoryView)) {
            filteredItems = ShopLoader.getEntries().values().stream()
                    .filter(this::isBrowsable)
                    .filter(this::matchesItemFilters)
                    .sorted(Comparator.comparingInt((ShopEntry e) -> e.priority).reversed()
                            .thenComparing(entry -> {
                                String selector = ShopEntryHelper.resourceSelector(entry);
                                return selector != null ? selector : "";
                            }))
                    .collect(Collectors.toList());
            filteredCategories = List.of();
        } else if (categoryView) {
            filteredCategories = allCategories.stream()
                    .filter(this::matchesCategoryFilters)
                    .collect(Collectors.toList());
            filteredItems = List.of();
        } else {
            filteredItems = categoryItems.stream()
                    .filter(this::isBrowsable)
                    .filter(this::matchesItemFilters)
                    .collect(Collectors.toList());
            filteredCategories = List.of();
        }
    }

    /** Category list with item query (All/Buyable/Sellable): show matching items, not parent categories. */
    public boolean isFlatItemSearch(boolean categoryView) {
        return categoryView
                && searchScope != SearchScope.CATEGORY
                && !searchQuery.trim().isEmpty();
    }

    public boolean isDisplayingItems(boolean categoryView) {
        return !categoryView || isFlatItemSearch(categoryView);
    }

    public static boolean isTagItemEntry(ShopEntry entry) {
        return entry != null && entry.type == ShopEntry.EntryType.ITEM && ShopEntryHelper.isTagEntry(entry);
    }

    public static boolean isConcreteShopEntry(ShopEntry entry) {
        if (entry == null || entry.type != ShopEntry.EntryType.ITEM || isTagItemEntry(entry)) {
            return false;
        }
        ItemStack stack = ItemConverter.parseItemString(entry.item, 1);
        return !stack.isEmpty() && stack.getItem() != net.minecraft.world.item.Items.STONE;
    }

    public static boolean isSelectableAutoShopEntry(ShopEntry entry, boolean buyMode) {
        if (entry == null || !ShopEntryHelper.isAutoShopSelectable(entry)) {
            return false;
        }
        return buyMode ? ShopEntryHelper.isBuyAllowed(entry) : ShopEntryHelper.isSellAllowed(entry);
    }

    private boolean matchesCategoryFilters(ShopCategory category) {
        if (currencyFilterId != null && !categoryHasCurrency(category.id, currencyFilterId)) {
            return false;
        }
        if (searchScope == SearchScope.CATEGORY) {
            String query = searchQuery.trim();
            if (!query.isEmpty()) {
                String name = Component.translatable(category.name).getString();
                if (!name.toLowerCase().contains(query.toLowerCase())) {
                    return false;
                }
            }
        } else if (searchScope == SearchScope.BUYABLE || searchScope == SearchScope.SELLABLE) {
            return categoryHasMatchingItem(category.id);
        } else {
            String query = searchQuery.trim();
            if (!query.isEmpty()) {
                return categoryHasMatchingItem(category.id);
            }
        }
        if (tradeVisibility == TradeVisibility.HIDE_UNTRADEABLE) {
            return categoryHasMatchingItem(category.id);
        }
        return true;
    }

    private boolean categoryHasMatchingItem(String categoryId) {
        for (ShopEntry entry : ShopLoader.getEntries().values()) {
            if (!categoryId.equals(entry.inCategory) || !isBrowsable(entry)) {
                continue;
            }
            if (!passesTradeVisibility(entry)) {
                continue;
            }
            if (searchScope == SearchScope.BUYABLE && !ShopEntryHelper.isBuyAllowed(entry)) {
                continue;
            }
            if (searchScope == SearchScope.SELLABLE && !ShopEntryHelper.isSellAllowed(entry)) {
                continue;
            }
            if (currencyFilterId != null) {
                String cur = entryCurrency(entry);
                if (!currencyFilterId.equals(cur)) {
                    continue;
                }
            }
            String query = searchQuery.trim();
            if (!query.isEmpty() && !matchesItemSearch(entry, query)) {
                continue;
            }
            return true;
        }
        return false;
    }

    private boolean categoryHasCurrency(String categoryId, String currencyId) {
        for (ShopEntry entry : ShopLoader.getEntries().values()) {
            if (categoryId.equals(entry.inCategory) && currencyId.equals(entryCurrency(entry))) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesItemFilters(ShopEntry entry) {
        if (!isBrowsable(entry)) {
            return false;
        }
        if (!passesTradeVisibility(entry)) {
            return false;
        }
        if (currencyFilterId != null && !currencyFilterId.equals(entryCurrency(entry))) {
            return false;
        }
        if (searchScope == SearchScope.BUYABLE && !ShopEntryHelper.isBuyAllowed(entry)) {
            return false;
        }
        if (searchScope == SearchScope.SELLABLE && !ShopEntryHelper.isSellAllowed(entry)) {
            return false;
        }
        String query = searchQuery.trim();
        if (query.isEmpty()) {
            return true;
        }
        if (searchScope == SearchScope.CATEGORY) {
            return false;
        }
        return matchesItemSearch(entry, query);
    }

    /**
     * Whether the player can buy or sell this entry in the current shop context
     * (stage + player-shop / AutoShop trade rules).
     */
    private boolean passesTradeVisibility(ShopEntry entry) {
        if (tradeVisibility != TradeVisibility.HIDE_UNTRADEABLE) {
            return true;
        }
        return canBuyOrSell(entry);
    }

    private boolean canBuyOrSell(ShopEntry entry) {
        if (ShopClientStages.isEntryBlocked(entry)) {
            return false;
        }
        if (autoShopMode) {
            return isSelectableAutoShopEntry(entry, true) || isSelectableAutoShopEntry(entry, false);
        }
        if (!ShopEntryHelper.isPlayerShopTradable(entry)) {
            return false;
        }
        return ShopEntryHelper.isBuyAllowed(entry) || ShopEntryHelper.isSellAllowed(entry);
    }

    private boolean matchesItemSearch(ShopEntry entry, String query) {
        String lowerQuery = query.toLowerCase();
        if (ShopEntryHelper.isTagEntry(entry)) {
            return ShopEntryHelper.tagEntryMatchesSearch(entry, lowerQuery);
        }
        String label = ShopEntryHelper.displayLabelForEntry(entry);
        if (label != null && label.toLowerCase().contains(lowerQuery)) {
            return true;
        }
        if (entry.type != ShopEntry.EntryType.ITEM) {
            String selector = ShopEntryHelper.resourceSelector(entry);
            return selector != null && selector.toLowerCase().contains(lowerQuery);
        }
        ItemStack stack = ShopEntryHelper.displayStackForEntry(entry);
        if (stack.isEmpty()) {
            return false;
        }
        HolderLookup.Provider registryAccess = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.level != null) {
            registryAccess = mc.level.registryAccess();
        }
        return DeepDrawerItemFilter.matchesSearch(stack, query, registryAccess);
    }

    private static String entryCurrency(ShopEntry entry) {
        if (entry.currency != null && !entry.currency.isEmpty()) {
            return entry.currency;
        }
        if (entry.valute != null && !entry.valute.isEmpty()) {
            return entry.valute;
        }
        return "null_coin";
    }

    public static List<String> getSortedCurrencyIds() {
        return ShopCurrency.sortedIds(ShopLoader.getCurrencies().values());
    }

    public String scopeLetter(SearchScope scope) {
        return switch (scope) {
            case ALL -> "A";
            case BUYABLE -> "B";
            case SELLABLE -> "S";
            case CATEGORY -> "C";
        };
    }

    public int scopeButtonX() {
        return SCOPE_BUTTON_X;
    }

    public int currencyButtonX() {
        return CURRENCY_BUTTON_X;
    }

    public int availabilityButtonX() {
        return AVAILABILITY_BUTTON_X;
    }
}
