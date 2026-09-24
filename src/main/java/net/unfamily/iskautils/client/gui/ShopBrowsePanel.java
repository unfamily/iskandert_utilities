package net.unfamily.iskautils.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.shop.ShopCategory;
import net.unfamily.iskautils.shop.ShopCurrency;
import net.unfamily.iskautils.shop.ShopEntry;
import net.unfamily.iskautils.shop.ShopEntryHelper;
import net.unfamily.iskautils.shop.ShopEntryTypes;
import net.unfamily.iskautils.shop.ShopHierarchy;
import net.unfamily.iskautils.shop.ShopLoader;
import net.unfamily.iskautils.util.DeepDrawerItemFilter;
import net.unfamily.iskalib.item.ItemConverter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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

    /**
     * A5: Sort mode for the browse list.
     * Applied when building the mixed row list.
     */
    public enum SortMode {
        PRIORITY("P"),
        BUY_ASC("B↑"),
        BUY_DESC("B↓"),
        SELL_ASC("S↑"),
        SELL_DESC("S↓");

        private final String label;
        SortMode(String label) { this.label = label; }
        public String label() { return label; }
    }

    /**
     * One visible browse row: either a child category or an entry at the current level.
     * Categories are listed before entries.
     */
    public static final class MixedRow {
        @Nullable
        public final ShopCategory category;
        @Nullable
        public final ShopEntry entry;

        private MixedRow(@Nullable ShopCategory category, @Nullable ShopEntry entry) {
            this.category = category;
            this.entry = entry;
        }

        public static MixedRow ofCategory(ShopCategory category) {
            return new MixedRow(category, null);
        }

        public static MixedRow ofEntry(ShopEntry entry) {
            return new MixedRow(null, entry);
        }

        public boolean isCategory() {
            return category != null;
        }

        public boolean isEntry() {
            return entry != null;
        }
    }

    public static final int GUI_WIDTH = 300;
    public static final int INVENTORY_Y = 154;
    public static final int ENTRY_WIDTH = 220;
    public static final int ENTRY_HEIGHT = 24;
    /** Content column shifted left so currency balances fit beside the Dynaimics scrollbar. */
    public static final int ENTRY_START_X = 7;
    public static final int ENTRY_START_Y = 20;
    public static final int MAX_VISIBLE_ENTRIES = 4;

    public static final int FILTER_ROW_HEIGHT = 16;
    public static final int SEARCH_BAR_HEIGHT = FILTER_ROW_HEIGHT;
    public static final int FILTER_BUTTON_HEIGHT = FILTER_ROW_HEIGHT;
    public static final int SCOPE_BUTTON_WIDTH = 16;
    public static final int CURRENCY_BUTTON_WIDTH = 36;
    public static final int AVAILABILITY_BUTTON_WIDTH = 16;
    public static final int SORT_BUTTON_WIDTH = 22;
    public static final int FILTER_BUTTON_GAP = 2;

    /** Search + filter row replaces the first entry slot (same Y as original entry list). */
    public static final int SEARCH_ROW_Y = ENTRY_START_Y;
    public static final int SEARCH_BAR_X = ENTRY_START_X;
    public static final int SEARCH_BAR_WIDTH = ENTRY_WIDTH - SCOPE_BUTTON_WIDTH - CURRENCY_BUTTON_WIDTH
            - AVAILABILITY_BUTTON_WIDTH - SORT_BUTTON_WIDTH - 4 * FILTER_BUTTON_GAP;
    public static final int SEARCH_BAR_Y = SEARCH_ROW_Y + (ENTRY_HEIGHT - SEARCH_BAR_HEIGHT) / 2;
    public static final int FILTER_ROW_Y = SEARCH_BAR_Y;
    public static final int SCOPE_BUTTON_X = SEARCH_BAR_X + SEARCH_BAR_WIDTH + FILTER_BUTTON_GAP;
    public static final int CURRENCY_BUTTON_X = SCOPE_BUTTON_X + SCOPE_BUTTON_WIDTH + FILTER_BUTTON_GAP;
    public static final int AVAILABILITY_BUTTON_X = CURRENCY_BUTTON_X + CURRENCY_BUTTON_WIDTH + FILTER_BUTTON_GAP;
    public static final int SORT_BUTTON_X = AVAILABILITY_BUTTON_X + AVAILABILITY_BUTTON_WIDTH + FILTER_BUTTON_GAP;

    /** @deprecated use {@link #SCOPE_BUTTON_WIDTH} */
    @Deprecated
    public static final int FILTER_BUTTON_SIZE = SCOPE_BUTTON_WIDTH;

    private SearchScope searchScope = SearchScope.ALL;
    @Nullable
    private String currencyFilterId = null;
    private TradeVisibility tradeVisibility = TradeVisibility.SHOW;
    /** A5: current sort mode. */
    private SortMode sortMode = SortMode.PRIORITY;
    private String searchQuery = "";
    private final boolean autoShopMode;

    private List<ShopCategory> allCategories = List.of();
    private List<ShopEntry> categoryItems = List.of();
    private List<ShopCategory> filteredCategories = List.of();
    private List<ShopEntry> filteredItems = List.of();
    /** A1: unified sorted mixed row list (categories + entries sorted by priority+id). */
    private List<MixedRow> mixedRows = List.of();

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

    // ── A5: Sort mode ────────────────────────────────────────────────────────

    public SortMode getSortMode() { return sortMode; }

    public void setSortMode(SortMode mode) { this.sortMode = mode != null ? mode : SortMode.PRIORITY; }

    public void cycleSortMode(boolean backward) {
        SortMode[] values = SortMode.values();
        int idx = sortMode.ordinal();
        if (backward) {
            idx = (idx - 1 + values.length) % values.length;
        } else {
            idx = (idx + 1) % values.length;
        }
        sortMode = values[idx];
    }

    public String sortModeLabel() {
        return sortMode.label();
    }

    // ── A4: Player prefs restore support ─────────────────────────────────────

    public void restorePrefs(SearchScope scope, @Nullable String currencyFilter,
                              TradeVisibility visibility, SortMode sort) {
        if (scope != null) searchScope = scope;
        currencyFilterId = currencyFilter;
        if (visibility != null) tradeVisibility = visibility;
        if (sort != null) sortMode = sort;
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

    /**
     * A1: Unified priority mix — categories and entries in one list sorted by priority DESC then id.
     */
    public int getMixedRowCount() {
        return mixedRows.size();
    }

    @Nullable
    public MixedRow getMixedRow(int index) {
        if (index < 0 || index >= mixedRows.size()) return null;
        return mixedRows.get(index);
    }

    /** Rebuild the unified mixed-row list from filteredCategories + filteredItems (A1 / A5). */
    private void rebuildMixedRows() {
        List<MixedRow> rows = new ArrayList<>(filteredCategories.size() + filteredItems.size());
        if (sortMode == SortMode.PRIORITY) {
            for (ShopCategory cat : filteredCategories) {
                rows.add(MixedRow.ofCategory(cat));
            }
            for (ShopEntry entry : filteredItems) {
                rows.add(MixedRow.ofEntry(entry));
            }
            // A1: categories and entries interleaved by priority DESC then id ASC
            rows.sort((a, b) -> {
                int pa = a.isCategory() ? a.category.priority : a.entry.priority;
                int pb = b.isCategory() ? b.category.priority : b.entry.priority;
                if (pb != pa) {
                    return Integer.compare(pb, pa);
                }
                String ia = a.isCategory() ? a.category.id : a.entry.id;
                String ib = b.isCategory() ? b.category.id : b.entry.id;
                return ia.compareToIgnoreCase(ib);
            });
        } else {
            // Price sorts: entries first (already sorted by entryComparator), categories last (no price)
            for (ShopEntry entry : filteredItems) {
                rows.add(MixedRow.ofEntry(entry));
            }
            for (ShopCategory cat : filteredCategories) {
                rows.add(MixedRow.ofCategory(cat));
            }
        }
        mixedRows = rows;
    }

    public void loadAllCategories() {
        Map<String, ShopCategory> categories = ShopLoader.getCategories();
        allCategories = ShopHierarchy.childCategories(categories.values(), null).stream()
                .sorted(Comparator.comparingInt((ShopCategory cat) -> cat.priority).reversed()
                        .thenComparing(cat -> cat.id))
                .collect(Collectors.toList());
        categoryItems = ShopHierarchy.childEntries(ShopLoader.getEntries().values(), null).stream()
                .filter(this::isBrowsable)
                .sorted(entryComparator())
                .collect(Collectors.toList());
        applyFilters(true);
    }

    public void loadCategoryItems(String categoryId) {
        Map<String, ShopCategory> categories = ShopLoader.getCategories();
        allCategories = ShopHierarchy.childCategories(categories.values(), categoryId).stream()
                .sorted(Comparator.comparingInt((ShopCategory cat) -> cat.priority).reversed()
                        .thenComparing(cat -> cat.id))
                .collect(Collectors.toList());
        categoryItems = ShopHierarchy.childEntries(ShopLoader.getEntries().values(), categoryId).stream()
                .filter(this::isBrowsable)
                .sorted(entryComparator())
                .collect(Collectors.toList());
        applyFilters(false);
    }

    public int sortButtonX() { return SORT_BUTTON_X; }

    private Comparator<ShopEntry> entryComparator() {
        Comparator<ShopEntry> byPriorityDesc = Comparator.comparingInt((ShopEntry e) -> e.priority).reversed();
        Comparator<ShopEntry> bySelector = Comparator.comparing(e -> {
            String s = ShopEntryHelper.resourceSelector(e);
            return s != null ? s : "";
        });
        return switch (sortMode) {
            case BUY_ASC -> Comparator.comparingDouble((ShopEntry e) -> e.buy)
                    .thenComparing(byPriorityDesc)
                    .thenComparing(bySelector);
            case BUY_DESC -> Comparator.comparingDouble((ShopEntry e) -> e.buy).reversed()
                    .thenComparing(byPriorityDesc)
                    .thenComparing(bySelector);
            case SELL_ASC -> Comparator.comparingDouble((ShopEntry e) -> e.sell)
                    .thenComparing(byPriorityDesc)
                    .thenComparing(bySelector);
            case SELL_DESC -> Comparator.comparingDouble((ShopEntry e) -> e.sell).reversed()
                    .thenComparing(byPriorityDesc)
                    .thenComparing(bySelector);
            default -> byPriorityDesc.thenComparing(bySelector);
        };
    }

    public void applyFilters(boolean categoryView) {
        if (isFlatItemSearch(categoryView)) {
            filteredItems = ShopLoader.getEntries().values().stream()
                    .filter(this::isBrowsable)
                    .filter(this::matchesItemFilters)
                    .sorted(entryComparator())
                    .collect(Collectors.toList());
            filteredCategories = List.of();
        } else {
            // Root or nested level: mixed child categories + child entries (A6: hide stage-locked)
            filteredCategories = allCategories.stream()
                    .filter(this::matchesCategoryFilters)
                    .collect(Collectors.toList());
            filteredItems = categoryItems.stream()
                    .filter(this::isBrowsable)
                    .filter(this::matchesItemFilters)
                    .sorted(entryComparator())
                    .collect(Collectors.toList());
        }
        // A1: rebuild unified mixed rows after filtering
        rebuildMixedRows();
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
        return ShopEntryTypes.isItem(entry) && ShopEntryHelper.isTagEntry(entry);
    }

    public static boolean isConcreteShopEntry(ShopEntry entry) {
        if (!ShopEntryTypes.isItem(entry) || isTagItemEntry(entry)) {
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
        // A6: hide stage-locked categories in HIDE_UNTRADEABLE mode
        if (tradeVisibility == TradeVisibility.HIDE_UNTRADEABLE
                && ShopClientStages.isCategoryBlocked(category)) {
            return false;
        }
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

    private boolean entryBelongsUnderCategory(ShopEntry entry, String categoryId) {
        if (entry == null || categoryId == null) {
            return false;
        }
        String entryParent = ShopHierarchy.normalizeParent(entry.inCategory);
        if (categoryId.equals(entryParent)) {
            return true;
        }
        return ShopHierarchy.isDescendantOf(ShopLoader.getCategories(), entryParent, categoryId);
    }

    private boolean categoryHasMatchingItem(String categoryId) {
        for (ShopEntry entry : ShopLoader.getEntries().values()) {
            if (!entryBelongsUnderCategory(entry, categoryId) || !isBrowsable(entry)) {
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
            if (entryBelongsUnderCategory(entry, categoryId) && currencyId.equals(entryCurrency(entry))) {
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
        // A6: hide stage-locked entries
        if (ShopClientStages.isEntryBlocked(entry)) {
            return false;
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
        if (!ShopEntryTypes.isItem(entry)) {
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
