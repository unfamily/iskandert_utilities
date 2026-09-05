package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.network.ModMessages;
import net.unfamily.iskautils.shop.ShopCategory;
import net.unfamily.iskautils.shop.ShopCurrency;
import net.unfamily.iskautils.shop.ShopEntry;
import net.unfamily.iskautils.shop.ShopEntryHelper;
import net.unfamily.iskautils.shop.ShopOtherRegistry;
import net.unfamily.iskautils.shop.ShopLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Shop-catalog sub-view for Auto Shop: browse entries and apply item + currency + buy/sell mode atomically.
 */
public final class AutoShopItemPickerOverlay {

    private static final Identifier SHOP_TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/backgrounds/shop.png");
    private static final Identifier ENTRY_TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/enrty_wide_wide_wide.png");
    private static final Identifier SCROLLBAR_TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/scrollbar.png");
    private static final Identifier SINGLE_SLOT_TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/single_slot.png");

    private static final int GUI_WIDTH = ShopBrowsePanel.GUI_WIDTH;
    private static final int GUI_HEIGHT = 240;
    private static final int INVENTORY_Y = ShopBrowsePanel.INVENTORY_Y;
    private static final int PANEL_COVER_COLOR = 0xFFC6C6C6;

    private static final int ENTRY_HEIGHT = ShopBrowsePanel.ENTRY_HEIGHT;
    private static final int ENTRY_START_X = ShopBrowsePanel.ENTRY_START_X;
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int SCROLLBAR_HEIGHT = 34;
    private static final int HANDLE_SIZE = 8;
    /** Scrollbar anchored near the right edge of the shop background. */
    private static final int SCROLLBAR_X = GUI_WIDTH - 16 - SCROLLBAR_WIDTH;
    /** Extended entry rows: fill space from entry start to scrollbar gap. */
    private static final int PICKER_ENTRY_WIDTH = SCROLLBAR_X - ENTRY_START_X - 4;
    private static final int BACK_BUTTON_WIDTH = 30;
    private static final int BACK_BUTTON_HEIGHT = 15;
    private static final int BACK_BUTTON_X = GUI_WIDTH - 10 - BACK_BUTTON_WIDTH;
    private static final int BACK_BUTTON_Y = 20;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;
    private static final int SELECT_BUTTON_WIDTH = 58;
    private static final int BUTTON_HEIGHT = 12;
    private static final int BUTTONS_SPACING = 3;
    private static final int ENTRY_RIGHT_MARGIN = 3;
    private static final int SEARCH_DEBOUNCE_TICKS = 4;
    /** Auto Shop picker has no player inventory; use three extra rows over the covered area. */
    private static final int PICKER_VISIBLE_ENTRIES = ShopBrowsePanel.MAX_VISIBLE_ENTRIES + 3;

    private final Runnable onCloseHost;
    private final Runnable onBackToMain;
    private final Supplier<BlockPos> machinePosSupplier;
    private final IntSupplier leftPosSupplier;
    private final IntSupplier topPosSupplier;
    private final Runnable playButtonSound;
    private final Runnable rebuildSelectButtons;
    private final Supplier<Font> fontSupplier;

    private final ShopBrowsePanel browsePanel = new ShopBrowsePanel(true);
    private Map<String, ShopCurrency> availableCurrencies = Map.of();

    private EditBox searchBox;
    private SymbolIconButton currencyFilterButton;
    private SymbolIconButton scopeFilterButton;
    private SymbolIconButton availabilityFilterButton;
    private Button backButton;
    private Button closeButton;
    private final List<Button> selectBuyButtons = new ArrayList<>();
    private final List<Button> selectSellButtons = new ArrayList<>();

    private boolean showingCategories = true;
    private String currentCategoryId;
    private int scrollOffset;
    private int totalShopEntries;
    private boolean isDraggingHandle;
    private int dragStartY;
    private int dragStartScrollOffset;
    private int searchDebounceTicks;

    public AutoShopItemPickerOverlay(
            Runnable onCloseHost,
            Runnable onBackToMain,
            Supplier<BlockPos> machinePosSupplier,
            IntSupplier leftPosSupplier,
            IntSupplier topPosSupplier,
            Runnable playButtonSound,
            Runnable rebuildSelectButtons,
            Supplier<Font> fontSupplier) {
        this.onCloseHost = onCloseHost;
        this.onBackToMain = onBackToMain;
        this.machinePosSupplier = machinePosSupplier;
        this.leftPosSupplier = leftPosSupplier;
        this.topPosSupplier = topPosSupplier;
        this.playButtonSound = playButtonSound;
        this.rebuildSelectButtons = rebuildSelectButtons;
        this.fontSupplier = fontSupplier;
    }

    public int guiWidth() {
        return GUI_WIDTH;
    }

    public int guiHeight() {
        return GUI_HEIGHT;
    }

    public void loadData() {
        availableCurrencies = new HashMap<>(ShopLoader.getCurrencies());
        browsePanel.loadAllCategories();
        showingCategories = true;
        currentCategoryId = null;
        scrollOffset = 0;
        totalShopEntries = browsePanel.getFilteredCategories().size();
    }

    public void initWidgets(AutoShopScreen screen) {
        int leftPos = leftPosSupplier.getAsInt();
        int topPos = topPosSupplier.getAsInt();

        searchBox = new EditBox(
                fontSupplier.get(),
                leftPos + ShopBrowsePanel.SEARCH_BAR_X,
                topPos + ShopBrowsePanel.SEARCH_BAR_Y,
                ShopBrowsePanel.SEARCH_BAR_WIDTH,
                ShopBrowsePanel.SEARCH_BAR_HEIGHT,
                Component.empty());
        searchBox.setMaxLength(256);
        searchBox.setBordered(true);
        searchBox.setHint(Component.translatable("gui.iska_utils.shop.search.placeholder"));
        searchBox.setResponder(text -> searchDebounceTicks = SEARCH_DEBOUNCE_TICKS);
        searchBox.visible = true;
        searchBox.active = true;
        screen.addPickerWidget(searchBox);

        scopeFilterButton = screen.addPickerWidget(new SymbolIconButton(
                leftPos + browsePanel.scopeButtonX(),
                topPos + ShopBrowsePanel.FILTER_ROW_Y,
                ShopBrowsePanel.SCOPE_BUTTON_WIDTH,
                ShopBrowsePanel.FILTER_BUTTON_HEIGHT,
                button -> onScopeFilterPressed(false),
                () -> browsePanel.scopeLetter(browsePanel.getSearchScope()),
                getScopeTooltip(browsePanel.getSearchScope())));

        currencyFilterButton = screen.addPickerWidget(new SymbolIconButton(
                leftPos + browsePanel.currencyButtonX(),
                topPos + ShopBrowsePanel.FILTER_ROW_Y,
                ShopBrowsePanel.CURRENCY_BUTTON_WIDTH,
                ShopBrowsePanel.FILTER_BUTTON_HEIGHT,
                button -> onCurrencyFilterPressed(false),
                this::getCurrencyFilterLabel,
                getCurrencyFilterTooltip()));

        availabilityFilterButton = screen.addPickerWidget(new SymbolIconButton(
                leftPos + browsePanel.availabilityButtonX(),
                topPos + ShopBrowsePanel.FILTER_ROW_Y,
                ShopBrowsePanel.AVAILABILITY_BUTTON_WIDTH,
                ShopBrowsePanel.FILTER_BUTTON_HEIGHT,
                button -> onAvailabilityFilterPressed(),
                browsePanel::tradeVisibilityLetter,
                getAvailabilityFilterTooltip()));

        backButton = screen.addPickerWidget(Button.builder(
                        Component.translatable("gui.iska_utils.shop.back"),
                        button -> onBackPressed())
                .bounds(leftPos + BACK_BUTTON_X, topPos + BACK_BUTTON_Y, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT)
                .build());
        updateBackButtonState();

        closeButton = screen.addPickerWidget(Button.builder(Component.literal("✕"), button -> {
            playButtonSound.run();
            onCloseHost.run();
        }).bounds(leftPos + CLOSE_BUTTON_X, topPos + CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE).build());

        layoutChromeWidgets();
        updateSelectButtons(screen);
        refreshFilteredLists();
    }

    /** Keep search/filter/back/close aligned after dual-layout leftPos/topPos changes. */
    public void layoutChromeWidgets() {
        int leftPos = leftPosSupplier.getAsInt();
        int topPos = topPosSupplier.getAsInt();
        if (searchBox != null) {
            searchBox.setPosition(leftPos + ShopBrowsePanel.SEARCH_BAR_X, topPos + ShopBrowsePanel.SEARCH_BAR_Y);
            searchBox.visible = true;
            searchBox.active = true;
        }
        if (scopeFilterButton != null) {
            scopeFilterButton.setPosition(leftPos + browsePanel.scopeButtonX(), topPos + ShopBrowsePanel.FILTER_ROW_Y);
            scopeFilterButton.visible = true;
            scopeFilterButton.active = true;
        }
        if (currencyFilterButton != null) {
            currencyFilterButton.setPosition(leftPos + browsePanel.currencyButtonX(), topPos + ShopBrowsePanel.FILTER_ROW_Y);
            currencyFilterButton.visible = true;
            currencyFilterButton.active = true;
        }
        if (availabilityFilterButton != null) {
            availabilityFilterButton.setPosition(
                    leftPos + browsePanel.availabilityButtonX(), topPos + ShopBrowsePanel.FILTER_ROW_Y);
            availabilityFilterButton.visible = true;
            availabilityFilterButton.active = true;
        }
        if (backButton != null) {
            backButton.setPosition(leftPos + BACK_BUTTON_X, topPos + BACK_BUTTON_Y);
            backButton.visible = true;
        }
        if (closeButton != null) {
            closeButton.setPosition(leftPos + CLOSE_BUTTON_X, topPos + CLOSE_BUTTON_Y);
            closeButton.visible = true;
            closeButton.active = true;
        }
    }

    public void tick() {
        if (searchDebounceTicks > 0) {
            searchDebounceTicks--;
            if (searchDebounceTicks == 0) {
                refreshFilteredLists();
            }
        }
    }

    public void renderBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int leftPos = leftPosSupplier.getAsInt();
        int topPos = topPosSupplier.getAsInt();

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SHOP_TEXTURE,
                leftPos, topPos, 0.0F, 0.0F, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);

        guiGraphics.fill(leftPos + 8, topPos + INVENTORY_Y - 4,
                leftPos + GUI_WIDTH - 8, topPos + GUI_HEIGHT - 6, PANEL_COVER_COLOR);

        renderEntryRows(guiGraphics, leftPos, topPos);
        renderScrollbar(guiGraphics, mouseX, mouseY);
    }

    private void renderEntryRows(GuiGraphicsExtractor guiGraphics, int leftPos, int topPos) {
        int entries = visibleEntries();
        int startY = entryStartY();
        for (int i = 0; i < entries; i++) {
            int entryIndex = scrollOffset + i;
            int entryX = leftPos + ENTRY_START_X;
            int entryY = topPos + startY + i * ENTRY_HEIGHT;
            ShopScreenHelper.renderExtendedEntryBackground(
                    guiGraphics, ENTRY_TEXTURE, entryX, entryY, PICKER_ENTRY_WIDTH, ENTRY_HEIGHT);

            if (displayingItems()) {
                if (entryIndex < browsePanel.getFilteredItems().size()) {
                    renderItemEntry(guiGraphics, entryX, entryY, browsePanel.getFilteredItems().get(entryIndex));
                }
            } else if (entryIndex < browsePanel.getFilteredCategories().size()) {
                renderCategoryEntry(guiGraphics, entryX, entryY,
                        browsePanel.getFilteredCategories().get(entryIndex));
            }
        }
    }

    public boolean extractTooltips(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY,
                                   Function<ItemStack, List<Component>> itemTooltipProvider) {
        int entryIndex = getEntryUnderMouse(mouseX, mouseY);
        if (entryIndex < 0) {
            return false;
        }

        int leftPos = leftPosSupplier.getAsInt();
        int topPos = topPosSupplier.getAsInt();
        int startY = entryStartY();
        int row = entryIndex - scrollOffset;
        if (row < 0 || row >= visibleEntries()) {
            return false;
        }
        int entryX = leftPos + ENTRY_START_X;
        int entryY = topPos + startY + row * ENTRY_HEIGHT;
        Font font = fontSupplier.get();

        if (displayingItems()) {
            if (entryIndex >= browsePanel.getFilteredItems().size()) {
                return false;
            }
            ShopEntry item = browsePanel.getFilteredItems().get(entryIndex);
            int buyButtonX = entryX + PICKER_ENTRY_WIDTH - SELECT_BUTTON_WIDTH - BUTTONS_SPACING - SELECT_BUTTON_WIDTH - ENTRY_RIGHT_MARGIN;
            int sellButtonX = entryX + PICKER_ENTRY_WIDTH - SELECT_BUTTON_WIDTH - ENTRY_RIGHT_MARGIN;
            int buttonY = entryY + (ENTRY_HEIGHT - BUTTON_HEIGHT) / 2;

            if (ShopEntryHelper.isTagEntry(item)
                    && mouseX >= buyButtonX && mouseX < buyButtonX + SELECT_BUTTON_WIDTH
                    && mouseY >= buttonY && mouseY < buttonY + BUTTON_HEIGHT) {
                guiGraphics.setTooltipForNextFrame(font,
                        List.of(Component.translatable("gui.iska_utils.shop.tag_sell_only")
                                .getVisualOrderText()),
                        mouseX, mouseY);
                return true;
            }
            if ((item.buy > 0 || item.free)
                    && mouseX >= buyButtonX && mouseX < buyButtonX + SELECT_BUTTON_WIDTH
                    && mouseY >= buttonY && mouseY < buttonY + BUTTON_HEIGHT) {
                guiGraphics.setTooltipForNextFrame(font,
                        createBuyTooltip(item).stream().map(Component::getVisualOrderText).toList(),
                        mouseX, mouseY);
                return true;
            }
            if (item.sell > 0
                    && mouseX >= sellButtonX && mouseX < sellButtonX + SELECT_BUTTON_WIDTH
                    && mouseY >= buttonY && mouseY < buttonY + BUTTON_HEIGHT) {
                guiGraphics.setTooltipForNextFrame(font,
                        createSellTooltip(item).stream().map(Component::getVisualOrderText).toList(),
                        mouseX, mouseY);
                return true;
            }

            if (ShopScreenHelper.isMouseOverEntryIcon(mouseX, mouseY, entryX, entryY)) {
                if (ShopEntryHelper.isTagEntry(item) || item.type != ShopEntry.EntryType.ITEM) {
                    guiGraphics.setTooltipForNextFrame(font,
                            List.of(ShopEntryHelper.displayTooltipForEntry(item).getVisualOrderText()),
                            mouseX, mouseY);
                    return true;
                }
                ItemStack stack = ShopEntryHelper.displayStackForEntry(item);
                if (!stack.isEmpty()) {
                    stack.setCount(Math.max(1, item.amount));
                    guiGraphics.setTooltipForNextFrame(
                            font,
                            itemTooltipProvider.apply(stack),
                            stack.getTooltipImage(),
                            stack,
                            mouseX,
                            mouseY,
                            stack.get(DataComponents.TOOLTIP_STYLE));
                    return true;
                }
            }
            return false;
        }

        if (entryIndex >= browsePanel.getFilteredCategories().size()) {
            return false;
        }
        ShopCategory category = browsePanel.getFilteredCategories().get(entryIndex);

        if (ShopScreenHelper.isMouseOverEntryIcon(mouseX, mouseY, entryX, entryY)) {
            if (ShopEntryHelper.isTagSelector(category.item)) {
                guiGraphics.setTooltipForNextFrame(font,
                        List.of(Component.literal(category.item.trim()).getVisualOrderText()),
                        mouseX, mouseY);
                return true;
            }
            ItemStack stack = ShopEntryHelper.displayStackForItemSelector(category.item, 1);
            if (!stack.isEmpty()) {
                guiGraphics.setTooltipForNextFrame(
                        font,
                        itemTooltipProvider.apply(stack),
                        stack.getTooltipImage(),
                        stack,
                        mouseX,
                        mouseY,
                        stack.get(DataComponents.TOOLTIP_STYLE));
                return true;
            }
        }

        if (category.description != null && !category.description.trim().isEmpty()) {
            guiGraphics.setTooltipForNextFrame(font,
                    List.of(Component.translatable(category.description).getVisualOrderText()),
                    mouseX, mouseY);
            return true;
        }
        return false;
    }

    private int getEntryUnderMouse(int mouseX, int mouseY) {
        int leftPos = leftPosSupplier.getAsInt();
        int topPos = topPosSupplier.getAsInt();
        int startY = entryStartY();
        int entries = visibleEntries();

        for (int i = 0; i < entries; i++) {
            int entryX = leftPos + ENTRY_START_X;
            int entryY = topPos + startY + i * ENTRY_HEIGHT;
            if (mouseX >= entryX && mouseX < entryX + PICKER_ENTRY_WIDTH
                    && mouseY >= entryY && mouseY < entryY + ENTRY_HEIGHT) {
                return scrollOffset + i;
            }
        }
        return -1;
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 1) {
            if (scopeFilterButton != null && scopeFilterButton.isMouseOver(event.x(), event.y())) {
                onScopeFilterPressed(true);
                return true;
            }
            if (currencyFilterButton != null && currencyFilterButton.isMouseOver(event.x(), event.y())) {
                onCurrencyFilterPressed(true);
                return true;
            }
            if (availabilityFilterButton != null && availabilityFilterButton.isMouseOver(event.x(), event.y())) {
                onAvailabilityFilterPressed();
                return true;
            }
            if (MachineGuiInput.clearEditBoxOnRightClick(event.x(), event.y(), event.button(), searchBox)) {
                return true;
            }
        }
        if (event.button() == 0) {
            if (handleScrollButtonClick(event.x(), event.y())) {
                MachineGuiInput.markScrollbarPressed();
                return true;
            }
            if (handleHandleClick(event.x(), event.y())) {
                MachineGuiInput.markScrollbarPressed();
                return true;
            }
            if (handleScrollbarClick(event.x(), event.y())) {
                MachineGuiInput.markScrollbarPressed();
                return true;
            }
            if (handleEntryClick(event.x(), event.y())) {
                return true;
            }
        }
        return false;
    }

    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            MachineGuiInput.clearScrollbarPressed();
            if (isDraggingHandle) {
                isDraggingHandle = false;
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (isDraggingHandle && event.button() == 0) {
            int max = maxScrollOffset();
            if (max <= 0) {
                return true;
            }
            int track = SCROLLBAR_HEIGHT - HANDLE_SIZE;
            int delta = (int) event.y() - dragStartY;
            int newOffset = dragStartScrollOffset + (int) ((double) delta / track * max);
            scrollOffset = Math.max(0, Math.min(max, newOffset));
            rebuildSelectButtons.run();
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double deltaY) {
        if (deltaY > 0) {
            return scrollUpSilent();
        }
        if (deltaY < 0) {
            return scrollDownSilent();
        }
        return false;
    }

    public boolean keyPressed(AutoShopScreen screen, KeyEvent event) {
        return MachineGuiInput.handleContainerKeyPressed(screen, event, isDraggingHandle, searchBox);
    }

    public boolean charTyped(CharacterEvent event) {
        return searchBox != null && searchBox.isFocused() && searchBox.charTyped(event);
    }

    public boolean handleEscape() {
        onBackPressed();
        return true;
    }

    private void onBackPressed() {
        playButtonSound.run();
        if (!showingCategories) {
            navigateBackToCategories();
            return;
        }
        onBackToMain.run();
    }

    private boolean displayingItems() {
        return browsePanel.isDisplayingItems(showingCategories);
    }

    private void refreshFilteredLists() {
        if (searchBox != null) {
            browsePanel.setSearchQuery(searchBox.getValue() == null ? "" : searchBox.getValue());
        }
        browsePanel.applyFilters(showingCategories);
        totalShopEntries = displayingItems()
                ? browsePanel.getFilteredItems().size()
                : browsePanel.getFilteredCategories().size();
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset()));
        updateBackButtonState();
        rebuildSelectButtons.run();
    }

    private void updateSelectButtons(AutoShopScreen screen) {
        selectBuyButtons.forEach(screen::removePickerWidget);
        selectSellButtons.forEach(screen::removePickerWidget);
        selectBuyButtons.clear();
        selectSellButtons.clear();

        if (!displayingItems()) {
            return;
        }

        int leftPos = leftPosSupplier.getAsInt();
        int topPos = topPosSupplier.getAsInt();
        int entries = visibleEntries();
        int startY = entryStartY();
        int visibleCount = Math.min(entries, totalShopEntries - scrollOffset);

        for (int i = 0; i < visibleCount; i++) {
            int entryIndex = scrollOffset + i;
            if (entryIndex >= browsePanel.getFilteredItems().size()) {
                break;
            }
            ShopEntry item = browsePanel.getFilteredItems().get(entryIndex);
            int entryY = topPos + startY + i * ENTRY_HEIGHT;
            int entryX = leftPos + ENTRY_START_X;
            int buyButtonX = entryX + PICKER_ENTRY_WIDTH - SELECT_BUTTON_WIDTH - BUTTONS_SPACING - SELECT_BUTTON_WIDTH - ENTRY_RIGHT_MARGIN;
            int sellButtonX = entryX + PICKER_ENTRY_WIDTH - SELECT_BUTTON_WIDTH - ENTRY_RIGHT_MARGIN;
            int buttonY = entryY + (ENTRY_HEIGHT - BUTTON_HEIGHT) / 2;
            boolean tagEntry = ShopEntryHelper.isTagEntry(item);

            if (item.buy > 0 || item.free) {
                Button buyButton = Button.builder(
                                Component.translatable("gui.iska_utils.auto_shop.picker.select_buy"),
                                button -> applySelection(item, true))
                        .bounds(buyButtonX, buttonY, SELECT_BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build();
                buyButton.active = ShopBrowsePanel.isSelectableAutoShopEntry(item, true);
                if (tagEntry) {
                    buyButton.setTooltip(Tooltip.create(
                            Component.translatable("gui.iska_utils.shop.tag_sell_only")));
                }
                selectBuyButtons.add(buyButton);
                screen.addPickerWidget(buyButton);
            }

            if (item.sell > 0) {
                Button sellButton = Button.builder(
                                Component.translatable("gui.iska_utils.auto_shop.picker.select_sell"),
                                button -> applySelection(item, false))
                        .bounds(sellButtonX, buttonY, SELECT_BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build();
                sellButton.active = ShopBrowsePanel.isSelectableAutoShopEntry(item, false);
                selectSellButtons.add(sellButton);
                screen.addPickerWidget(sellButton);
            }
        }
    }

    private void applySelection(ShopEntry entry, boolean buyMode) {
        BlockPos pos = machinePosSupplier.get();
        if (pos.equals(BlockPos.ZERO)) {
            return;
        }
        ModMessages.sendAutoShopApplyPickerSelectionPacket(pos, entry.id, buyMode);
        playButtonSound.run();
        onBackToMain.run();
    }

    private void navigateBackToCategories() {
        showingCategories = true;
        currentCategoryId = null;
        resetSearchOnNavigation(false);
        browsePanel.loadAllCategories();
        scrollOffset = 0;
        refreshFilteredLists();
    }

    private void navigateToCategory(ShopCategory category) {
        showingCategories = false;
        currentCategoryId = category.id;
        resetSearchOnNavigation(true);
        browsePanel.loadCategoryItems(category.id);
        scrollOffset = 0;
        refreshFilteredLists();
    }

    private void resetSearchOnNavigation(boolean enteringCategory) {
        browsePanel.resetSearchAndScope(enteringCategory);
        if (searchBox != null) {
            searchBox.setValue("");
        }
        updateScopeFilterTooltip();
    }

    private void updateBackButtonState() {
        if (backButton != null) {
            backButton.active = true;
            backButton.visible = true;
        }
    }

    private int entryStartY() {
        return browsePanel.getEntryStartY();
    }

    private int visibleEntries() {
        return PICKER_VISIBLE_ENTRIES;
    }

    private int maxScrollOffset() {
        return Math.max(0, totalShopEntries - visibleEntries());
    }

    private int buttonUpY() {
        return browsePanel.getBrowseAreaStartY();
    }

    private int scrollbarY() {
        return browsePanel.getBrowseAreaStartY() + HANDLE_SIZE;
    }

    private int buttonDownY() {
        return scrollbarY() + SCROLLBAR_HEIGHT;
    }

    private void onScopeFilterPressed(boolean backward) {
        browsePanel.cycleSearchScope(backward, showingCategories);
        updateScopeFilterTooltip();
        refreshFilteredLists();
        playButtonSound.run();
    }

    private void onCurrencyFilterPressed(boolean backward) {
        browsePanel.cycleCurrencyFilter(backward);
        updateCurrencyFilterTooltip();
        refreshFilteredLists();
        playButtonSound.run();
    }

    private void onAvailabilityFilterPressed() {
        browsePanel.cycleTradeVisibility();
        updateAvailabilityFilterTooltip();
        refreshFilteredLists();
        playButtonSound.run();
    }

    private void updateScopeFilterTooltip() {
        if (scopeFilterButton != null) {
            scopeFilterButton.setTooltip(Tooltip.create(getScopeTooltip(browsePanel.getSearchScope())));
        }
    }

    private Component getScopeTooltip(ShopBrowsePanel.SearchScope scope) {
        return switch (scope) {
            case ALL -> Component.translatable("gui.iska_utils.shop.search_scope.all");
            case BUYABLE -> Component.translatable("gui.iska_utils.shop.search_scope.buyable");
            case SELLABLE -> Component.translatable("gui.iska_utils.shop.search_scope.sellable");
            case CATEGORY -> Component.translatable("gui.iska_utils.shop.search_scope.category");
        };
    }

    private String getCurrencyFilterLabel() {
        String currencyId = browsePanel.getCurrencyFilterId();
        if (currencyId == null) {
            return Component.translatable("gui.iska_utils.shop.currency.any.button").getString();
        }
        ShopCurrency currency = availableCurrencies.get(currencyId);
        if (currency != null && currency.charSymbol != null && !currency.charSymbol.isEmpty()) {
            return currency.charSymbol;
        }
        return currencyId.length() > 4 ? currencyId.substring(0, 4) : currencyId;
    }

    private Component getCurrencyFilterTooltip() {
        String currencyId = browsePanel.getCurrencyFilterId();
        if (currencyId == null) {
            return Component.translatable("gui.iska_utils.shop.currency.any");
        }
        ShopCurrency currency = availableCurrencies.get(currencyId);
        if (currency != null && currency.name != null && !currency.name.trim().isEmpty()) {
            return Component.translatable(currency.name);
        }
        return Component.literal(currencyId);
    }

    private void updateCurrencyFilterTooltip() {
        if (currencyFilterButton != null) {
            currencyFilterButton.setTooltip(Tooltip.create(getCurrencyFilterTooltip()));
        }
    }

    private Component getAvailabilityFilterTooltip() {
        return browsePanel.getTradeVisibility() == ShopBrowsePanel.TradeVisibility.HIDE_UNTRADEABLE
                ? Component.translatable("gui.iska_utils.shop.visibility.hide")
                : Component.translatable("gui.iska_utils.shop.visibility.show");
    }

    private void updateAvailabilityFilterTooltip() {
        if (availabilityFilterButton != null) {
            availabilityFilterButton.setTooltip(Tooltip.create(getAvailabilityFilterTooltip()));
        }
    }

    private void renderCategoryEntry(GuiGraphicsExtractor guiGraphics, int entryX, int entryY, ShopCategory category) {
        int slotX = entryX + 3;
        int slotY = entryY + 3;
        int textX = slotX + 24;
        int textY = entryY + (ENTRY_HEIGHT - 8) / 2;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SINGLE_SLOT_TEXTURE, slotX, slotY, 0.0F, 0.0F, 18, 18, 18, 18);
        ItemStack icon = ShopEntryHelper.displayStackForItemSelector(category.item, 1);
        if (!icon.isEmpty()) {
            guiGraphics.item(icon, slotX + 1, slotY + 1);
        }
        int maxTextWidth = entryX + PICKER_ENTRY_WIDTH - textX - 5;
        ShopScreenHelper.renderScaledText(guiGraphics, fontSupplier.get(),
                Component.translatable(category.name).getString(), textX, textY, maxTextWidth, GuiTextColors.TITLE);
    }

    private void renderItemEntry(GuiGraphicsExtractor guiGraphics, int entryX, int entryY, ShopEntry item) {
        int slotX = entryX + 3;
        int slotY = entryY + 3;
        int textX = slotX + 24;
        int textY = entryY + (ENTRY_HEIGHT - 8) / 2;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SINGLE_SLOT_TEXTURE, slotX, slotY, 0.0F, 0.0F, 18, 18, 18, 18);
        switch (item.type) {
            case ITEM -> {
                ItemStack stack = ShopEntryHelper.displayStackForEntry(item);
                if (!stack.isEmpty()) {
                    stack.setCount(Math.max(1, item.amount));
                    guiGraphics.item(stack, slotX + 1, slotY + 1);
                    guiGraphics.itemDecorations(fontSupplier.get(), stack, slotX + 1, slotY + 1);
                }
            }
            case FLUID -> {
                var fluid = ShopEntryHelper.displayFluidForEntry(item);
                if (!fluid.isEmpty()) {
                    GuiFluidStillBlit.blit16(guiGraphics, fluid, slotX + 1, slotY + 1);
                }
            }
            case GAS -> {
                Object gas = ShopEntryHelper.displayGasForEntry(item);
                if (gas != null) {
                    GuiChemicalStillBlit.blit16(guiGraphics, gas, slotX + 1, slotY + 1);
                }
            }
            case OTHER -> {
                ShopOtherRegistry.Definition definition = ShopOtherRegistry.get(item.other);
                if (definition != null) {
                    guiGraphics.blit(RenderPipelines.GUI_TEXTURED, definition.icon(),
                            slotX + 1, slotY + 1, 0.0F, 0.0F, 16, 16, 16, 16);
                }
            }
        }
        int buyButtonX = entryX + PICKER_ENTRY_WIDTH - SELECT_BUTTON_WIDTH - BUTTONS_SPACING - SELECT_BUTTON_WIDTH - ENTRY_RIGHT_MARGIN;
        int maxTextWidth = buyButtonX - textX - 5;
        ShopScreenHelper.renderScaledText(guiGraphics, fontSupplier.get(),
                ShopEntryHelper.displayLabelForEntry(item), textX, textY, maxTextWidth, GuiTextColors.TITLE);
    }

    private void renderScrollbar(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int entries = visibleEntries();
        if (totalShopEntries <= entries) {
            return;
        }
        int leftPos = leftPosSupplier.getAsInt();
        int topPos = topPosSupplier.getAsInt();
        int upY = buttonUpY();
        int barY = scrollbarY();
        int downY = buttonDownY();

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE,
                leftPos + SCROLLBAR_X, topPos + barY, 0.0F, 0.0F, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT, 32, 34);

        boolean upHovered = mouseX >= leftPos + SCROLLBAR_X && mouseX < leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH
                && mouseY >= topPos + upY && mouseY < topPos + upY + HANDLE_SIZE;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, leftPos + SCROLLBAR_X, topPos + upY,
                (float) (SCROLLBAR_WIDTH * 2), (float) (upHovered ? HANDLE_SIZE : 0), HANDLE_SIZE, HANDLE_SIZE, 32, 34);

        boolean downHovered = mouseX >= leftPos + SCROLLBAR_X && mouseX < leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH
                && mouseY >= topPos + downY && mouseY < topPos + downY + HANDLE_SIZE;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, leftPos + SCROLLBAR_X, topPos + downY,
                (float) (SCROLLBAR_WIDTH * 3), (float) (downHovered ? HANDLE_SIZE : 0), HANDLE_SIZE, HANDLE_SIZE, 32, 34);

        double scrollRatio = (double) scrollOffset / maxScrollOffset();
        int handleY = topPos + barY + (int) (scrollRatio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
        boolean handleHovered = mouseX >= leftPos + SCROLLBAR_X && mouseX < leftPos + SCROLLBAR_X + HANDLE_SIZE
                && mouseY >= handleY && mouseY < handleY + HANDLE_SIZE;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, leftPos + SCROLLBAR_X, handleY,
                (float) SCROLLBAR_WIDTH, (float) (handleHovered ? HANDLE_SIZE : 0), HANDLE_SIZE, HANDLE_SIZE, 32, 34);
    }

    private boolean handleEntryClick(double mouseX, double mouseY) {
        int leftPos = leftPosSupplier.getAsInt();
        int topPos = topPosSupplier.getAsInt();
        int startY = entryStartY();
        int entries = visibleEntries();

        for (int i = 0; i < entries; i++) {
            int entryX = leftPos + ENTRY_START_X;
            int entryY = topPos + startY + i * ENTRY_HEIGHT;
            if (mouseX < entryX || mouseX >= entryX + PICKER_ENTRY_WIDTH || mouseY < entryY || mouseY >= entryY + ENTRY_HEIGHT) {
                continue;
            }
            int actualIndex = scrollOffset + i;
            if (!displayingItems() && actualIndex < browsePanel.getFilteredCategories().size()) {
                navigateToCategory(browsePanel.getFilteredCategories().get(actualIndex));
                playButtonSound.run();
                return true;
            }
            if (displayingItems() && actualIndex < browsePanel.getFilteredItems().size()) {
                ShopEntry item = browsePanel.getFilteredItems().get(actualIndex);
                int buyButtonX = entryX + PICKER_ENTRY_WIDTH - SELECT_BUTTON_WIDTH - BUTTONS_SPACING - SELECT_BUTTON_WIDTH - ENTRY_RIGHT_MARGIN;
                int sellButtonX = entryX + PICKER_ENTRY_WIDTH - SELECT_BUTTON_WIDTH - ENTRY_RIGHT_MARGIN;
                int buttonY = entryY + (ENTRY_HEIGHT - BUTTON_HEIGHT) / 2;

                boolean clickOnBuyButton = (item.buy > 0 || item.free)
                        && mouseX >= buyButtonX && mouseX < buyButtonX + SELECT_BUTTON_WIDTH
                        && mouseY >= buttonY && mouseY < buttonY + BUTTON_HEIGHT;
                boolean clickOnSellButton = item.sell > 0
                        && mouseX >= sellButtonX && mouseX < sellButtonX + SELECT_BUTTON_WIDTH
                        && mouseY >= buttonY && mouseY < buttonY + BUTTON_HEIGHT;

                if (clickOnBuyButton || clickOnSellButton) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    private boolean handleScrollButtonClick(double mouseX, double mouseY) {
        if (totalShopEntries <= visibleEntries()) {
            return false;
        }
        int leftPos = leftPosSupplier.getAsInt();
        int topPos = topPosSupplier.getAsInt();
        if (mouseX >= leftPos + SCROLLBAR_X && mouseX < leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH
                && mouseY >= topPos + buttonUpY() && mouseY < topPos + buttonUpY() + HANDLE_SIZE) {
            scrollUp();
            return true;
        }
        if (mouseX >= leftPos + SCROLLBAR_X && mouseX < leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH
                && mouseY >= topPos + buttonDownY() && mouseY < topPos + buttonDownY() + HANDLE_SIZE) {
            scrollDown();
            return true;
        }
        return false;
    }

    private boolean handleHandleClick(double mouseX, double mouseY) {
        if (totalShopEntries <= visibleEntries()) {
            return false;
        }
        int leftPos = leftPosSupplier.getAsInt();
        int topPos = topPosSupplier.getAsInt();
        int barY = topPos + scrollbarY();
        double scrollRatio = (double) scrollOffset / maxScrollOffset();
        int handleY = barY + (int) (scrollRatio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
        if (mouseX >= leftPos + SCROLLBAR_X && mouseX < leftPos + SCROLLBAR_X + HANDLE_SIZE
                && mouseY >= handleY && mouseY < handleY + HANDLE_SIZE) {
            isDraggingHandle = true;
            dragStartY = (int) mouseY;
            dragStartScrollOffset = scrollOffset;
            playButtonSound.run();
            return true;
        }
        return false;
    }

    private boolean handleScrollbarClick(double mouseX, double mouseY) {
        if (totalShopEntries <= visibleEntries()) {
            return false;
        }
        int leftPos = leftPosSupplier.getAsInt();
        int topPos = topPosSupplier.getAsInt();
        int barY = topPos + scrollbarY();
        if (mouseX < leftPos + SCROLLBAR_X || mouseX >= leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH
                || mouseY < barY || mouseY >= barY + SCROLLBAR_HEIGHT) {
            return false;
        }
        float clickRatio = (float) (mouseY - barY) / SCROLLBAR_HEIGHT;
        scrollOffset = Math.max(0, Math.min(maxScrollOffset(), (int) (clickRatio * maxScrollOffset())));
        rebuildSelectButtons.run();
        playButtonSound.run();
        return true;
    }

    private void scrollUp() {
        if (scrollUpSilent()) {
            playButtonSound.run();
        }
    }

    private void scrollDown() {
        if (scrollDownSilent()) {
            playButtonSound.run();
        }
    }

    private boolean scrollUpSilent() {
        if (totalShopEntries > visibleEntries() && scrollOffset > 0) {
            scrollOffset--;
            rebuildSelectButtons.run();
            return true;
        }
        return false;
    }

    private boolean scrollDownSilent() {
        if (totalShopEntries > visibleEntries() && scrollOffset < maxScrollOffset()) {
            scrollOffset++;
            rebuildSelectButtons.run();
            return true;
        }
        return false;
    }

    private List<Component> createBuyTooltip(ShopEntry item) {
        List<Component> tooltip = new ArrayList<>();
        String currencyId = item.valute != null ? item.valute : "null_coin";
        if (item.free) {
            tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.buy.free"));
        } else {
            tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.buy.cost",
                    item.buy, getCurrencySymbol(currencyId)));
        }
        tooltip.add(ShopScreenHelper.amountLine(item));
        return tooltip;
    }

    private List<Component> createSellTooltip(ShopEntry item) {
        List<Component> tooltip = new ArrayList<>();
        String currencyId = item.valute != null ? item.valute : "null_coin";
        tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.sell.price",
                item.sell, getCurrencySymbol(currencyId)));
        tooltip.add(ShopScreenHelper.amountLine(item));
        return tooltip;
    }

    private String getCurrencySymbol(String valuteId) {
        if (valuteId == null) {
            return "?";
        }
        ShopCurrency currency = availableCurrencies.get(valuteId);
        if (currency != null && currency.charSymbol != null) {
            return currency.charSymbol;
        }
        return valuteId;
    }

    void rebuildSelectButtons(AutoShopScreen screen) {
        updateSelectButtons(screen);
    }
}
