package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.shop.ShopLoader;
import net.unfamily.iskautils.shop.ShopCategory;
import net.unfamily.iskautils.shop.ShopEntry;
import net.unfamily.iskautils.shop.ShopEntryHelper;
import net.unfamily.iskautils.shop.ShopHierarchy;
import net.unfamily.iskautils.shop.ShopEntryTypeHandler;
import net.unfamily.iskautils.shop.ShopEntryTypeRegistry;
import net.unfamily.iskautils.shop.ShopEntryTypes;
import net.unfamily.iskautils.shop.ShopCurrency;
import net.unfamily.iskautils.shop.ShopPurchaseLimitsData;
import net.unfamily.iskautils.shop.ShopTransactionManager;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.*;

public class ShopScreen extends AbstractContainerScreen<AbstractContainerMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/backgrounds/shop.png");
    private static final Identifier ENTRY_TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/enrty_wide_wide_wide.png");
    private static final Identifier SCROLLBAR_TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/scrollbar.png");
    private static final Identifier SINGLE_SLOT_TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/single_slot.png");

    // Background widened only to the right (shop.png 300x240)
    private static final int GUI_WIDTH = 300;
    private static final int GUI_HEIGHT = 240;
    // Entry texture: enrty_wide_wide_wide.png = 220x24, aligned with inventory start (x=20)
    private static final int ENTRY_WIDTH = 220;
    private static final int ENTRY_HEIGHT = 24;
    private static final int ENTRY_START_X = 19;
    private static final int SEARCH_DEBOUNCE_TICKS = 4;
    
    // Margin from right edge (don't go below this)
    private static final int RIGHT_EDGE_MARGIN = 10;
    
    // Scrollbar constants (from StructurePlacerScreen)
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int SCROLLBAR_HEIGHT = 34;
    private static final int HANDLE_SIZE = 8;
    
    // Scrollbar: right next to entries (Y positions derived from entryStartY())
    private static final int SCROLLBAR_X = ENTRY_START_X + ENTRY_WIDTH + 4;
    
    // Buy/Sell button constants
    private static final int BUTTON_WIDTH = 30;
    private static final int BUTTON_HEIGHT = 12;
    private static final int BUTTONS_SPACING = 3;
    
    // Right info area: as left as possible after scrollbar, but back button must end at least RIGHT_EDGE_MARGIN from right
    private static final int BACK_BUTTON_WIDTH = 30;
    private static final int BACK_BUTTON_HEIGHT = 15;
    private static final int BACK_BUTTON_X = GUI_WIDTH - RIGHT_EDGE_MARGIN - BACK_BUTTON_WIDTH; // 260
    private static final int CURRENCIES_AREA_LEFT = SCROLLBAR_X + SCROLLBAR_WIDTH + 2;
    private static final int BACK_BUTTON_Y = 20; // Same level as search row
    /** Aligned with the first list entry row (search occupies the slot above entries). */
    private static final int CURRENCIES_START_Y = ShopBrowsePanel.ENTRY_START_Y + ShopBrowsePanel.ENTRY_HEIGHT;
    
    // Scrolling variables
    private int scrollOffset = 0;
    private int totalShopEntries = 0;
    private boolean isDraggingHandle = false;
    private int dragStartY = 0;
    private int dragStartScrollOffset = 0;

    // GUI mode
    private boolean showingCategories = true; // true = show categories, false = show items
    private String currentCategoryId = null;
    protected String currentCategoryName = "Shop";
    
    // Shop data
    private final ShopBrowsePanel browsePanel = new ShopBrowsePanel();
    private Map<String, ShopCurrency> availableCurrencies = new HashMap<>();
    private EditBox searchBox;
    private int searchDebounceTicks = 0;
    private SymbolIconButton currencyFilterButton;
    private SymbolIconButton scopeFilterButton;
    private SymbolIconButton availabilityFilterButton;
    
    // Vanilla buttons
    private Button backButton;
    private Button closeButton;
    private List<Button> buyButtons = new ArrayList<>();
    private List<Button> sellButtons = new ArrayList<>();
    
    // Close button position - top right
    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5; // 5px from right edge
    
    // Player team data
    private String playerTeamName = null;
    private Map<String, Double> playerTeamBalances = new HashMap<>();
    private int teamBalanceRefreshTicks;
    private static ShopScreen currentInstance = null; // For static callback
    
    // Feedback area for error/success messages
    private String feedbackMessage = null;
    private int feedbackColor = GuiTextColors.FEEDBACK_DEFAULT;
    private long feedbackClearTime = 0;
    private static final long FEEDBACK_DISPLAY_TIME = 3000; // 3 seconds
    
    public ShopScreen(AbstractContainerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GUI_WIDTH, GUI_HEIGHT);
        
        // Carica i dati del shop
        loadShopData();
        
        // Keep instance for reload callbacks
        currentInstance = this;
        
        // Request team data from server
        net.unfamily.iskautils.network.ModMessages.sendShopTeamDataRequest();
        net.unfamily.iskautils.network.ModMessages.requestShopPurchaseLimits();
    }

    private int entryStartY() {
        return browsePanel.getEntryStartY();
    }

    private int visibleEntries() {
        return browsePanel.getVisibleEntryCount();
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

    private int maxScrollOffset() {
        return Math.max(0, totalShopEntries - visibleEntries());
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Load shop data
        loadShopData();
        
        // Register this instance for callback
        currentInstance = this;
        net.unfamily.iskautils.network.ModMessages.requestShopPurchaseLimits();

        searchBox = new EditBox(
                font,
                leftPos + ShopBrowsePanel.SEARCH_BAR_X,
                topPos + ShopBrowsePanel.SEARCH_BAR_Y,
                ShopBrowsePanel.SEARCH_BAR_WIDTH,
                ShopBrowsePanel.SEARCH_BAR_HEIGHT,
                Component.empty());
        searchBox.setMaxLength(256);
        searchBox.setBordered(true);
        searchBox.setHint(Component.translatable("gui.iska_utils.shop.search.placeholder"));
        searchBox.setResponder(text -> searchDebounceTicks = SEARCH_DEBOUNCE_TICKS);
        addRenderableWidget(searchBox);

        scopeFilterButton = addRenderableWidget(new SymbolIconButton(
                leftPos + browsePanel.scopeButtonX(),
                topPos + ShopBrowsePanel.FILTER_ROW_Y,
                ShopBrowsePanel.SCOPE_BUTTON_WIDTH,
                ShopBrowsePanel.FILTER_BUTTON_HEIGHT,
                button -> onScopeFilterPressed(false),
                () -> browsePanel.scopeLetter(browsePanel.getSearchScope()),
                getScopeTooltip(browsePanel.getSearchScope())));

        currencyFilterButton = addRenderableWidget(new SymbolIconButton(
                leftPos + browsePanel.currencyButtonX(),
                topPos + ShopBrowsePanel.FILTER_ROW_Y,
                ShopBrowsePanel.CURRENCY_BUTTON_WIDTH,
                ShopBrowsePanel.FILTER_BUTTON_HEIGHT,
                button -> onCurrencyFilterPressed(false),
                this::getCurrencyFilterLabel,
                getCurrencyFilterTooltip()));

        availabilityFilterButton = addRenderableWidget(new SymbolIconButton(
                leftPos + browsePanel.availabilityButtonX(),
                topPos + ShopBrowsePanel.FILTER_ROW_Y,
                ShopBrowsePanel.AVAILABILITY_BUTTON_WIDTH,
                ShopBrowsePanel.FILTER_BUTTON_HEIGHT,
                button -> onAvailabilityFilterPressed(),
                browsePanel::tradeVisibilityLetter,
                getAvailabilityFilterTooltip()));
        
        // Create vanilla Back button
        backButton = Button.builder(Component.translatable("gui.iska_utils.shop.back"), button -> {
            if (currentCategoryId != null) {
                navigateBackOneLevel();
            }
        }).bounds(this.leftPos + BACK_BUTTON_X, this.topPos + BACK_BUTTON_Y, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT).build();
        
        this.addRenderableWidget(backButton);
        
        // Close button - top right with ✕ symbol
        closeButton = Button.builder(Component.literal("✕"), 
                                    button -> {
                                        playButtonSound();
                                        this.onClose();
                                    })
                           .bounds(this.leftPos + CLOSE_BUTTON_X, this.topPos + CLOSE_BUTTON_Y, 
                                  CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
                           .build();
        addRenderableWidget(closeButton);
        
        // Update button state and create Buy/Sell buttons
        updateBackButtonState();
        updateBuySellButtons();
        refreshFilteredLists();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (searchDebounceTicks > 0) {
            searchDebounceTicks--;
            if (searchDebounceTicks == 0) {
                refreshFilteredLists();
            }
        }
        // Keep balances live while AutoShop (or other players) change team currencies.
        if (++teamBalanceRefreshTicks >= 20) {
            teamBalanceRefreshTicks = 0;
            net.unfamily.iskautils.network.ModMessages.sendShopTeamDataRequest();
        }
    }

    private void refreshFilteredLists() {
        if (searchBox != null) {
            browsePanel.setSearchQuery(searchBox.getValue() == null ? "" : searchBox.getValue());
        }
        browsePanel.applyFilters(showingCategories);
        totalShopEntries = browsePanel.getMixedRowCount();
        int maxScroll = maxScrollOffset();
        if (totalShopEntries > visibleEntries()) {
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        } else {
            scrollOffset = 0;
        }
        updateBuySellButtons();
    }

    private void onScopeFilterPressed(boolean backward) {
        browsePanel.cycleSearchScope(backward, showingCategories);
        updateScopeFilterTooltip();
        refreshFilteredLists();
        playButtonSound();
    }

    private void onCurrencyFilterPressed(boolean backward) {
        browsePanel.cycleCurrencyFilter(backward);
        updateCurrencyFilterTooltip();
        refreshFilteredLists();
        playButtonSound();
    }

    private void onAvailabilityFilterPressed() {
        browsePanel.cycleTradeVisibility();
        updateAvailabilityFilterTooltip();
        refreshFilteredLists();
        playButtonSound();
    }

    private void updateScopeFilterTooltip() {
        if (scopeFilterButton != null) {
            scopeFilterButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    getScopeTooltip(browsePanel.getSearchScope())));
        }
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

    private Component getScopeTooltip(ShopBrowsePanel.SearchScope scope) {
        return switch (scope) {
            case ALL -> Component.translatable("gui.iska_utils.shop.search_scope.all");
            case BUYABLE -> Component.translatable("gui.iska_utils.shop.search_scope.buyable");
            case SELLABLE -> Component.translatable("gui.iska_utils.shop.search_scope.sellable");
            case CATEGORY -> Component.translatable("gui.iska_utils.shop.search_scope.category");
        };
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
            currencyFilterButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(getCurrencyFilterTooltip()));
        }
    }

    private Component getAvailabilityFilterTooltip() {
        return browsePanel.getTradeVisibility() == ShopBrowsePanel.TradeVisibility.HIDE_UNTRADEABLE
                ? Component.translatable("gui.iska_utils.shop.visibility.hide")
                : Component.translatable("gui.iska_utils.shop.visibility.show");
    }

    private void updateAvailabilityFilterTooltip() {
        if (availabilityFilterButton != null) {
            availabilityFilterButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    getAvailabilityFilterTooltip()));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        int guiX = this.leftPos;
        int guiY = this.topPos;
        
        // Main background
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, guiX, guiY, 0, 0, this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);
        
        // Shop entries - render visible rows, even if some are empty
        int entries = visibleEntries();
        int startY = entryStartY();
        for (int i = 0; i < entries; i++) {
            int entryIndex = scrollOffset + i;
            int entryX = guiX + ENTRY_START_X;
            int entryY = guiY + startY + i * ENTRY_HEIGHT;
            
            // Render entry background (enrty_wide_wide_wide.png 220x24)
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ENTRY_TEXTURE, entryX, entryY, 0, 0, ENTRY_WIDTH, ENTRY_HEIGHT, 220, 24);
            
            ShopBrowsePanel.MixedRow row = browsePanel.getMixedRow(entryIndex);
            if (row != null && row.isCategory()) {
                renderCategoryEntry(guiGraphics, entryX, entryY, row.category);
            } else if (row != null && row.isEntry()) {
                renderItemEntry(guiGraphics, entryX, entryY, row.entry);
            }
        }
        
        // Scrollbar (only next to first entry)
        renderScrollbar(guiGraphics, mouseX, mouseY);
        
        // Right info area
        renderInfoArea(guiGraphics, mouseX, mouseY);
        
        // Render feedback area
        updateAndRenderFeedback(guiGraphics, guiX, guiY);
    }
    

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        renderShopTooltips(guiGraphics, mouseX, mouseY);
        renderCurrencyTooltips(guiGraphics, mouseX, mouseY);
    }

    private void renderScrollbar(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int entries = visibleEntries();
        // Only show scrollbar if there are more entries than can fit
        if (totalShopEntries <= entries) return;
        
        int guiX = this.leftPos;
        int guiY = this.topPos;
        int upY = buttonUpY();
        int barY = scrollbarY();
        int downY = buttonDownY();
        
        // Draw scrollbar background (8 pixels wide, height 34)
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, guiY + barY, 0, 0, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT, 32, 34);
        
        // UP button (8x8 pixels) - above scrollbar
        boolean upButtonHovered = (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH &&
                                  mouseY >= guiY + upY && mouseY < guiY + upY + HANDLE_SIZE);
        int upButtonV = upButtonHovered ? HANDLE_SIZE : 0;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, guiY + upY, SCROLLBAR_WIDTH * 2, upButtonV, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        
        // DOWN button (8x8 pixels) - below scrollbar
        boolean downButtonHovered = (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH &&
                                    mouseY >= guiY + downY && mouseY < guiY + downY + HANDLE_SIZE);
        int downButtonV = downButtonHovered ? HANDLE_SIZE : 0;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, guiY + downY, SCROLLBAR_WIDTH * 3, downButtonV, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        
        // Handle (8x8 pixels) - position based on scroll offset
        if (totalShopEntries > entries) {
            double scrollRatio = (double) scrollOffset / maxScrollOffset();
            int handleY = guiY + barY + (int)(scrollRatio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
            
            boolean handleHovered = (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + HANDLE_SIZE &&
                                    mouseY >= handleY && mouseY < handleY + HANDLE_SIZE);
            int handleTextureY = handleHovered ? HANDLE_SIZE : 0;
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, handleY, SCROLLBAR_WIDTH, handleTextureY, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        }
    }
    
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 1) {
            if (scopeFilterButton != null && scopeFilterButton.isMouseOver(mouseX, mouseY)) {
                onScopeFilterPressed(true);
                return true;
            }
            if (currencyFilterButton != null && currencyFilterButton.isMouseOver(mouseX, mouseY)) {
                onCurrencyFilterPressed(true);
                return true;
            }
            if (availabilityFilterButton != null && availabilityFilterButton.isMouseOver(mouseX, mouseY)) {
                onAvailabilityFilterPressed();
                return true;
            }
            if (MachineGuiInput.clearEditBoxOnRightClick(mouseX, mouseY, button, searchBox)) {
                return true;
            }
        }
        if (button == 0) {
            if (handleScrollButtonClick(mouseX, mouseY)) {
                MachineGuiInput.markScrollbarPressed();
                return true;
            }
            if (handleHandleClick(mouseX, mouseY)) {
                MachineGuiInput.markScrollbarPressed();
                return true;
            }
            if (handleScrollbarClick(mouseX, mouseY)) {
                MachineGuiInput.markScrollbarPressed();
                return true;
            }
            if (handleEntryClick(mouseX, mouseY)) {
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        int button = event.button();
        if (button == 0) {
            MachineGuiInput.clearScrollbarPressed();
            if (isDraggingHandle) {
                isDraggingHandle = false;
                return true;
            }
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (MachineGuiInput.handleContainerKeyPressed(this, event, isDraggingHandle, searchBox)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchBox != null && searchBox.isFocused() && searchBox.charTyped(event)) {
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseY = event.y();
        int button = event.button();
        if (button == 0 && isDraggingHandle && totalShopEntries > visibleEntries()) {
            int deltaY = (int) mouseY - dragStartY;
            float scrollRatio = (float) deltaY / (SCROLLBAR_HEIGHT - HANDLE_SIZE);
            int newScrollOffset = dragStartScrollOffset + (int) (scrollRatio * maxScrollOffset());
            scrollOffset = Math.max(0, Math.min(maxScrollOffset(), newScrollOffset));
            updateBuySellButtons();
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }
    
    private boolean handleScrollButtonClick(double mouseX, double mouseY) {
        int entries = visibleEntries();
        if (totalShopEntries <= entries) return false;
        
        int guiX = this.leftPos;
        int guiY = this.topPos;
        int upY = buttonUpY();
        int downY = buttonDownY();
        
        // UP button
        if (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH &&
            mouseY >= guiY + upY && mouseY < guiY + upY + HANDLE_SIZE) {
            scrollUp();
            return true;
        }
        
        // DOWN button
        if (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH &&
            mouseY >= guiY + downY && mouseY < guiY + downY + HANDLE_SIZE) {
            scrollDown();
            return true;
        }
        
        return false;
    }
    
    private boolean handleHandleClick(double mouseX, double mouseY) {
        int entries = visibleEntries();
        if (totalShopEntries <= entries) return false;
        
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int scrollbarX = x + SCROLLBAR_X;
        int scrollbarYPos = y + scrollbarY();
        
        float scrollRatio = maxScrollOffset() > 0 ? (float) scrollOffset / maxScrollOffset() : 0.0f;
        int handleY = scrollbarYPos + (int)(scrollRatio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
        
        if (mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE &&
            mouseY >= handleY && mouseY < handleY + HANDLE_SIZE) {
            
            isDraggingHandle = true;
            dragStartY = (int) mouseY;
            dragStartScrollOffset = scrollOffset;
            playButtonSound();
            return true;
        }
        return false;
    }
    
    private boolean handleScrollbarClick(double mouseX, double mouseY) {
        int entries = visibleEntries();
        if (totalShopEntries <= entries) return false;
        
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int scrollbarX = x + SCROLLBAR_X;
        int scrollbarYPos = y + scrollbarY();
        
        if (mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_WIDTH &&
            mouseY >= scrollbarYPos && mouseY < scrollbarYPos + SCROLLBAR_HEIGHT) {
            
            float clickRatio = (float)(mouseY - scrollbarYPos) / SCROLLBAR_HEIGHT;
            clickRatio = Math.max(0, Math.min(1, clickRatio));
            
            int newScrollOffset = (int)(clickRatio * maxScrollOffset());
            newScrollOffset = Math.max(0, Math.min(maxScrollOffset(), newScrollOffset));
            
            if (newScrollOffset != scrollOffset) {
                scrollOffset = newScrollOffset;
                updateBuySellButtons();
                playButtonSound();
            }
            return true;
        }
        return false;
    }
    
    // mouseReleased/mouseDragged are handled via MouseButtonEvent overrides above
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (deltaY > 0) {
            return scrollUpSilent();
        } else if (deltaY < 0) {
            return scrollDownSilent();
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }
    
    private void scrollUp() {
        if (scrollUpSilent()) {
            playButtonSound();
        }
    }
    
    private void scrollDown() {
        if (scrollDownSilent()) {
            playButtonSound();
        }
    }
    
    private boolean scrollUpSilent() {
        if (totalShopEntries > visibleEntries() && scrollOffset > 0) {
            scrollOffset--;
            updateBuySellButtons();
            return true;
        }
        return false;
    }
    
    private boolean scrollDownSilent() {
        if (totalShopEntries > visibleEntries() && scrollOffset < maxScrollOffset()) {
            scrollOffset++;
            updateBuySellButtons();
            return true;
        }
        return false;
    }
    
    private void playButtonSound() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
    
    /**
     * Aggiorna il numero totale di entry del shop (chiamato quando vengono caricati i dati)
     */
    public void setTotalShopEntries(int totalEntries) {
        this.totalShopEntries = totalEntries;
        if (totalShopEntries > visibleEntries()) {
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset()));
        } else {
            scrollOffset = 0;
        }
    }
    
    /**
     * Ritorna l'offset di scroll corrente
     */
    public int getScrollOffset() {
        return scrollOffset;
    }
    
    /**
     * Ricarica i dati del shop (per reload del sistema)
     */
    public void reloadShopData() {
        loadShopData();
        updateBackButtonState();
        refreshFilteredLists();
    }
    
    /**
     * Carica i dati del shop dal sistema
     */
    private void loadShopData() {
        availableCurrencies = ShopLoader.getCurrencies();
        browsePanel.loadAllCategories();
        showingCategories = true;
        currentCategoryId = null;
        currentCategoryName = Component.translatable("gui.iska_utils.shop.title").getString();
        scrollOffset = 0;
        totalShopEntries = browsePanel.getMixedRowCount();
    }
    
    /**
     * Render entry content (slot + text)
     */
    private void renderEntryContent(GuiGraphicsExtractor guiGraphics, int entryX, int entryY, int entryIndex) {
        int actualIndex = scrollOffset + entryIndex;
        ShopBrowsePanel.MixedRow row = browsePanel.getMixedRow(actualIndex);
        if (row != null && row.isCategory()) {
            renderCategoryEntry(guiGraphics, entryX, entryY, row.category);
        } else if (row != null && row.isEntry()) {
            renderItemEntry(guiGraphics, entryX, entryY, row.entry);
        }
    }
    
    /**
     * Render a category row
     */
    private void renderCategoryEntry(GuiGraphicsExtractor guiGraphics, int entryX, int entryY, ShopCategory category) {
        int slotX = entryX + 3;
        int slotY = entryY + 3;
        int textX = slotX + 18 + 6;
        int textY = entryY + (ENTRY_HEIGHT - 8) / 2;
        
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SINGLE_SLOT_TEXTURE, slotX, slotY, 0, 0, 18, 18, 18, 18);
        
        ItemStack categoryIcon = ShopEntryHelper.displayStackForItemSelector(category.item, 1);
        if (!categoryIcon.isEmpty()) {
            guiGraphics.item(categoryIcon, slotX + 1, slotY + 1);
        }
        
        int maxTextWidth = entryX + ENTRY_WIDTH - textX - 5;
        
        renderScaledText(guiGraphics, Component.translatable(category.name).getString(), textX, textY, maxTextWidth, GuiTextColors.TITLE);
    }
    
    /**
     * Render an item row
     */
    private void renderItemEntry(GuiGraphicsExtractor guiGraphics, int entryX, int entryY, ShopEntry item) {
        boolean isBlocked = isItemBlocked(item);
        
        if (isBlocked) {
            guiGraphics.fill(entryX, entryY, entryX + ENTRY_WIDTH, entryY + ENTRY_HEIGHT,
                            0x80FF0000);
        }
        
        int slotX = entryX + 3;
        int slotY = entryY + 3;
        int textX = slotX + 18 + 6;
        int textY = entryY + (ENTRY_HEIGHT - 8) / 2;
        
        // Draw slot (18x18)
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SINGLE_SLOT_TEXTURE, slotX, slotY, 0, 0, 18, 18, 18, 18);
        
        if (ShopEntryTypes.isItem(item)) {
            ItemStack itemStack = ShopEntryHelper.displayStackForEntry(item);
            if (!itemStack.isEmpty()) {
                itemStack.setCount(Math.max(1, item.amount));
                guiGraphics.item(itemStack, slotX + 1, slotY + 1);
                guiGraphics.itemDecorations(this.font, itemStack, slotX + 1, slotY + 1);
            }
        } else if (ShopEntryTypes.isFluid(item)) {
            var fluid = ShopEntryHelper.displayFluidForEntry(item);
            if (!fluid.isEmpty()) {
                GuiFluidStillBlit.blit16(guiGraphics, fluid, slotX + 1, slotY + 1);
            }
        } else if (ShopEntryTypes.isGas(item)) {
            Object gas = ShopEntryHelper.displayGasForEntry(item);
            if (gas != null) {
                GuiChemicalStillBlit.blit16(guiGraphics, gas, slotX + 1, slotY + 1);
            }
        } else {
            Identifier icon = ShopEntryTypeRegistry.require(item).guiIcon(item);
            if (icon != null) {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, icon, slotX + 1, slotY + 1, 0.0F, 0.0F, 16, 16, 16, 16);
            }
        }
        
        String displayName = ShopEntryHelper.displayLabelForEntry(item);
        
        int buyButtonStartX = entryX + ENTRY_WIDTH - BUTTON_WIDTH - BUTTONS_SPACING - BUTTON_WIDTH - 3;
        int maxTextWidth = buyButtonStartX - textX - 5;
        
        renderScaledText(guiGraphics, displayName, textX, textY, maxTextWidth, GuiTextColors.TITLE);
        
        // Buy/Sell buttons are vanilla widgets created in updateBuySellButtons()
    }
    
    /**
     * Handle entry click
     */
    private boolean handleEntryClick(double mouseX, double mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int startY = entryStartY();
        int entries = visibleEntries();
        
        for (int i = 0; i < entries; i++) {
            int entryX = x + ENTRY_START_X;
            int entryY = y + startY + i * ENTRY_HEIGHT;
            
            if (mouseX >= entryX && mouseX < entryX + ENTRY_WIDTH &&
                mouseY >= entryY && mouseY < entryY + ENTRY_HEIGHT) {
                
                int actualIndex = scrollOffset + i;
                ShopBrowsePanel.MixedRow row = browsePanel.getMixedRow(actualIndex);
                if (row == null) {
                    break;
                }

                if (row.isEntry()) {
                    ShopEntry item = row.entry;

                    int buyButtonX = entryX + ENTRY_WIDTH - BUTTON_WIDTH - BUTTONS_SPACING - BUTTON_WIDTH - 3;
                    int sellButtonX = entryX + ENTRY_WIDTH - BUTTON_WIDTH - 3;
                    int buttonY = entryY + (ENTRY_HEIGHT - BUTTON_HEIGHT) / 2;

                    // If click is on buttons, let super.mouseClicked handle them
                    boolean clickOnBuyButton = (item.buy > 0 || item.free) &&
                        mouseX >= buyButtonX && mouseX < buyButtonX + BUTTON_WIDTH &&
                        mouseY >= buttonY && mouseY < buttonY + BUTTON_HEIGHT;

                    boolean clickOnSellButton = item.sell > 0 &&
                        mouseX >= sellButtonX && mouseX < sellButtonX + BUTTON_WIDTH &&
                        mouseY >= buttonY && mouseY < buttonY + BUTTON_HEIGHT;

                    if (clickOnBuyButton || clickOnSellButton) {
                        return false; // Let super.mouseClicked handle buttons
                    }

                    // Click on row (not buttons): no-op for now
                    playButtonSound();
                    return true;
                }

                if (row.isCategory()) {
                    navigateToCategory(row.category);
                    playButtonSound();
                    return true;
                }
                break;
            }
        }
        return false;
    }
    
    /**
     * Navigate into a category
     */
    private void navigateToCategory(ShopCategory category) {
        openCategoryLevel(category, true);
    }

    private void openCategoryLevel(ShopCategory category, boolean enteringCategory) {
        showingCategories = false;
        currentCategoryId = category.id;
        currentCategoryName = Component.translatable(category.name).getString();
        resetSearchOnNavigation(enteringCategory);
        browsePanel.loadCategoryItems(category.id);
        scrollOffset = 0;
        updateBackButtonState();
        refreshFilteredLists();
    }

    /**
     * Go up one level: parent category, or root if parent is root.
     */
    private void navigateBackOneLevel() {
        if (currentCategoryId == null) {
            return;
        }
        ShopCategory current = ShopLoader.getCategories().get(currentCategoryId);
        String parentId = current != null ? ShopHierarchy.normalizeParent(current.inCategory) : null;
        if (parentId == null) {
            navigateBackToCategories();
            return;
        }
        ShopCategory parent = ShopLoader.getCategories().get(parentId);
        if (parent == null) {
            navigateBackToCategories();
            return;
        }
        openCategoryLevel(parent, false);
    }
    
    /**
     * Return to root category view
     */
    public void navigateBackToCategories() {
        showingCategories = true;
        currentCategoryId = null;
        currentCategoryName = Component.translatable("gui.iska_utils.shop.title").getString();
        resetSearchOnNavigation(false);
        browsePanel.loadAllCategories();
        scrollOffset = 0;
        updateBackButtonState();
        refreshFilteredLists();
    }

    private void resetSearchOnNavigation(boolean enteringCategory) {
        browsePanel.resetSearchAndScope(enteringCategory);
        if (searchBox != null) {
            searchBox.setValue("");
        }
        updateScopeFilterTooltip();
    }
    
    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        // Keep base behavior (inventory label) but override title.
        Component titleComponent = Component.literal(currentCategoryName);
        int titleWidth = this.font.width(titleComponent);
        int titleX = ENTRY_START_X + (ENTRY_WIDTH - titleWidth) / 2;
        guiGraphics.text(this.font, titleComponent, titleX, 9, GuiTextColors.TITLE, false);
        // Intentionally do not draw the "Inventory" label (vanilla would).
    }

    private void renderShopTooltips(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int entryIndex = getEntryUnderMouse(mouseX, mouseY);
        if (entryIndex < 0) {
            return;
        }

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int startY = entryStartY();
        int rowOffset = entryIndex - scrollOffset;
        if (rowOffset < 0 || rowOffset >= visibleEntries()) {
            return;
        }
        int entryX = x + ENTRY_START_X;
        int entryY = y + startY + rowOffset * ENTRY_HEIGHT;

        ShopBrowsePanel.MixedRow mixed = browsePanel.getMixedRow(entryIndex);
        if (mixed == null) {
            return;
        }

        if (mixed.isEntry()) {
            ShopEntry item = mixed.entry;

            List<Component> buttonTooltip = getButtonTooltip(mouseX, mouseY);
            if (buttonTooltip != null && !buttonTooltip.isEmpty()) {
                setTooltip(guiGraphics, this.font, buttonTooltip, mouseX, mouseY);
                return;
            }

            if (isItemBlocked(item)) {
                setTooltip(guiGraphics, this.font, createBlockedTooltip(item), mouseX, mouseY);
                return;
            }

            if (ShopScreenHelper.isMouseOverEntryIcon(mouseX, mouseY, entryX, entryY)) {
                if (ShopEntryHelper.isTagEntry(item) || !ShopEntryTypes.isItem(item)) {
                    setTooltip(guiGraphics, this.font,
                            List.of(ShopEntryHelper.displayTooltipForEntry(item)), mouseX, mouseY);
                    return;
                }
                ItemStack stack = ShopEntryHelper.displayStackForEntry(item);
                if (!stack.isEmpty()) {
                    stack.setCount(Math.max(1, item.amount));
                    setItemTooltip(guiGraphics, this.font, stack, mouseX, mouseY);
                }
            }
            return;
        }

        ShopCategory category = mixed.category;

        if (ShopScreenHelper.isMouseOverEntryIcon(mouseX, mouseY, entryX, entryY)) {
            if (ShopEntryHelper.isTagSelector(category.item)) {
                setTooltip(guiGraphics, this.font,
                        List.of(Component.literal(category.item.trim())), mouseX, mouseY);
                return;
            }
            ItemStack stack = ShopEntryHelper.displayStackForItemSelector(category.item, 1);
            if (!stack.isEmpty()) {
                setItemTooltip(guiGraphics, this.font, stack, mouseX, mouseY);
                return;
            }
        }

        if (category.description != null && !category.description.trim().isEmpty()) {
            setTooltip(guiGraphics, this.font,
                    List.of(Component.translatable(category.description)), mouseX, mouseY);
        }
    }

    /**
     * Buy/Sell button tooltips
     */
    private List<Component> getButtonTooltip(int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int startY = entryStartY();
        int entries = visibleEntries();

        for (int i = 0; i < entries; i++) {
            int actualIndex = scrollOffset + i;
            ShopBrowsePanel.MixedRow row = browsePanel.getMixedRow(actualIndex);
            if (row == null || !row.isEntry()) {
                continue;
            }

            ShopEntry item = row.entry;
            int entryY = y + startY + i * ENTRY_HEIGHT;
            int entryX = x + ENTRY_START_X;
            int buyButtonX = entryX + ENTRY_WIDTH - BUTTON_WIDTH - BUTTONS_SPACING - BUTTON_WIDTH - 3;
            int sellButtonX = entryX + ENTRY_WIDTH - BUTTON_WIDTH - 3;
            int buttonsY = entryY + (ENTRY_HEIGHT - BUTTON_HEIGHT) / 2;

            if ((item.buy > 0 || item.free) && mouseX >= buyButtonX && mouseX < buyButtonX + BUTTON_WIDTH
                && mouseY >= buttonsY && mouseY < buttonsY + BUTTON_HEIGHT) {
                if (isLimitBlocked(item, ShopPurchaseLimitsData.TradeSide.BUY)) {
                    return createPurchaseLimitTooltip(item, ShopPurchaseLimitsData.TradeSide.BUY);
                }
                if (!ShopEntryHelper.isPlayerShopTradable(item)) {
                    return ShopScreenHelper.playerShopFluidGasHintTooltip(item, true, getCurrencySymbol(item.valute));
                }
                return createBuyTooltip(item);
            }

            if (item.sell > 0 && mouseX >= sellButtonX && mouseX < sellButtonX + BUTTON_WIDTH
                && mouseY >= buttonsY && mouseY < buttonsY + BUTTON_HEIGHT) {
                if (isLimitBlocked(item, ShopPurchaseLimitsData.TradeSide.SELL)) {
                    return createPurchaseLimitTooltip(item, ShopPurchaseLimitsData.TradeSide.SELL);
                }
                if (!ShopEntryHelper.isPlayerShopTradable(item)) {
                    return ShopScreenHelper.playerShopFluidGasHintTooltip(item, false, getCurrencySymbol(item.valute));
                }
                return createSellTooltip(item);
            }
        }

        return null;
    }
    
    /**
     * Creates the tooltip for the Buy button
     */
    private List<Component> createBuyTooltip(ShopEntry item) {
        List<Component> tooltip = new ArrayList<>();
        
        if (item.free) {
            tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.buy.free"));
        } else {
            String currencySymbol = getCurrencySymbol(item.valute);
            tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.buy.cost", item.buy, currencySymbol));
        }
        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.buy.click"));
        tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.buy.ctrl"));
        tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.buy.shift"));
        
        return tooltip;
    }
    
    /**
     * Creates the tooltip for the Sell button
     */
    private List<Component> createSellTooltip(ShopEntry item) {
        List<Component> tooltip = new ArrayList<>();
        
        // Price with currency symbol
        String currencySymbol = getCurrencySymbol(item.valute);
        tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.sell.price", item.sell, currencySymbol));
        
        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.sell.click"));
        tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.sell.ctrl"));
        tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.sell.shift"));
        
        return tooltip;
    }
    
    /**
     * Render right info area (currencies only; Back is a vanilla button)
     */
    private void renderInfoArea(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        // Render available currencies
        renderAvailableCurrencies(guiGraphics, x, y);
    }
    
    /**
     * Render available currencies with real team balances
     */
    private void renderAvailableCurrencies(GuiGraphicsExtractor guiGraphics, int guiX, int guiY) {
        int startY = guiY + CURRENCIES_START_Y;

        if (playerTeamName == null) {
            Component noTeamText = Component.translatable("gui.iska_utils.shop.no_team");
            int textX = guiX + CURRENCIES_AREA_LEFT;

            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(textX, startY);
            guiGraphics.pose().scale(0.77f, 0.77f);
            guiGraphics.text(this.font, noTeamText, 0, 0, GuiTextColors.MUTED, false);
            guiGraphics.pose().popMatrix();
            return;
        }

        int lineIndex = 0;
        for (ShopCurrency currency : availableCurrencies.values()) {
            int textY = startY + lineIndex * 10;

            double balance = playerTeamBalances.getOrDefault(currency.id, 0.0);
            String balanceStr = ShopTransactionManager.formatCompactNumber(balance);
            String balanceText = balanceStr + " " + (currency.charSymbol != null ? currency.charSymbol : currency.id);
            Component currencyText = Component.literal(balanceText);

            int color = balance > 0 ? GuiTextColors.TITLE : GuiTextColors.NEGATIVE;
            int textX = guiX + CURRENCIES_AREA_LEFT;
            guiGraphics.text(this.font, currencyText, textX, textY, color, false);

            lineIndex++;
        }

        if (availableCurrencies.isEmpty()) {
            Component noValutesText = Component.translatable("gui.iska_utils.shop.no_valutes");
            int textX = guiX + CURRENCIES_AREA_LEFT;
            guiGraphics.text(this.font, noValutesText, textX, startY, GuiTextColors.MUTED, false);
        }
    }

    private void renderCurrencyTooltips(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (playerTeamName == null || availableCurrencies.isEmpty()) {
            return;
        }
        int guiX = this.leftPos;
        int startY = this.topPos + CURRENCIES_START_Y;
        int lineIndex = 0;
        for (ShopCurrency currency : availableCurrencies.values()) {
            int textY = startY + lineIndex * 10;
            int textX = guiX + CURRENCIES_AREA_LEFT;
            double balance = playerTeamBalances.getOrDefault(currency.id, 0.0);
            String balanceStr = ShopTransactionManager.formatCompactNumber(balance);
            String balanceText = balanceStr + " " + (currency.charSymbol != null ? currency.charSymbol : currency.id);
            int width = this.font.width(balanceText);
            if (mouseX >= textX && mouseX < textX + Math.max(width, 40)
                    && mouseY >= textY && mouseY < textY + 10) {
                String full = ShopTransactionManager.formatFullNumber(balance);
                String name = Component.translatable(currency.name).getString();
                String symbol = currency.charSymbol != null ? currency.charSymbol : currency.id;
                setTooltip(guiGraphics, this.font,
                        List.of(Component.literal(name + " " + symbol + ": " + full)),
                        mouseX, mouseY);
                return;
            }
            lineIndex++;
        }
    }


    /**
     * Update Back button enabled state
     */
    private void updateBackButtonState() {
        if (backButton != null) {
            backButton.active = currentCategoryId != null;
        }
    }
    
    /**
     * Resolve currency symbol
     */
    private String getCurrencySymbol(String valuteId) {
        if (valuteId == null) return "?";
        ShopCurrency currency = availableCurrencies.get(valuteId);
        if (currency != null && currency.charSymbol != null) {
            return currency.charSymbol;
        }
        return valuteId; // Fallback sull'ID
    }
    
    /**
     * Resolve currency display name instead of id
     */
    private String getCurrencyName(String valuteId) {
        if (valuteId == null) return "?";
        ShopCurrency currency = availableCurrencies.get(valuteId);
        if (currency != null && currency.name != null && !currency.name.trim().isEmpty()) {
            return Component.translatable(currency.name).getString();
        }
        return valuteId; // Fallback sull'ID
    }
    
    /**
     * Show feedback message in the area below
     */
    private void showFeedback(String message, int color) {
        this.feedbackMessage = message;
        this.feedbackColor = color;
        this.feedbackClearTime = System.currentTimeMillis() + FEEDBACK_DISPLAY_TIME;
    }
    
    /**
     * Show insufficient-funds error feedback
     */
    private void showInsufficientFundsError(String currencyName) {
        Component message = Component.translatable("gui.iska_utils.shop.feedback.insufficient_funds", currencyName);
        showFeedback(message.getString(), 0xFF4444); // Red
    }
    
    /**
     * Show insufficient-items error feedback
     */
    private void showInsufficientItemsError() {
        Component message = Component.translatable("gui.iska_utils.shop.feedback.insufficient_items");
        showFeedback(message.getString(), 0xFF4444); // Red
    }
    
    /**
     * Hide feedback message (success path)
     */
    private void hideFeedback() {
        this.feedbackMessage = null;
        this.feedbackClearTime = 0;
    }
    
    /**
     * Update and render the feedback area
     */
    private void updateAndRenderFeedback(GuiGraphicsExtractor guiGraphics, int guiX, int guiY) {
        // Check if it's time to hide the message
        if (feedbackMessage != null && System.currentTimeMillis() >= feedbackClearTime) {
            hideFeedback();
        }
        
        // Render the message if present
        if (feedbackMessage != null) {
            
            int textX = guiX + 20;
            int textY = guiY + 143;
            guiGraphics.text(this.font, Component.literal(feedbackMessage), textX, textY, feedbackColor, false);
        }
    }
    
    /**
     * Rebuild dynamic Buy/Sell buttons for visible entries
     */
    private void updateBuySellButtons() {
        // Remove existing buy/sell buttons
        buyButtons.forEach(this::removeWidget);
        sellButtons.forEach(this::removeWidget);
        buyButtons.clear();
        sellButtons.clear();
        
        // Create buttons only for visible entry rows (not categories)
        int entries = visibleEntries();
        int startY = entryStartY();
        int visibleCount = Math.min(entries, totalShopEntries - scrollOffset);
        
        for (int i = 0; i < visibleCount; i++) {
            int entryIndex = scrollOffset + i;
            ShopBrowsePanel.MixedRow row = browsePanel.getMixedRow(entryIndex);
            if (row == null || !row.isEntry()) {
                continue;
            }
            
            ShopEntry item = row.entry;
            int entryY = this.topPos + startY + i * ENTRY_HEIGHT;
            
            // Buy button — fluids/gases shown disabled (catalog only; trade via AutoShop)
            if (ShopEntryHelper.isBuyAllowed(item) && !ShopEntryHelper.isTagEntry(item)) {
                int buyButtonX = this.leftPos + ENTRY_START_X + ENTRY_WIDTH - BUTTON_WIDTH - BUTTONS_SPACING - BUTTON_WIDTH - 3;
                int buttonY = entryY + (ENTRY_HEIGHT - BUTTON_HEIGHT) / 2;
                
                Component buyText = Component.translatable("gui.iska_utils.shop.buy");
                boolean tradable = ShopEntryHelper.isPlayerShopTradable(item) && ShopEntryHelper.isBuyAllowed(item);
                
                Button buyButton = Button.builder(buyText, button -> {
                    int multiplier = calculateMultiplier();
                    handleBuyButtonClick(item, multiplier);
                }).bounds(buyButtonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
                buyButton.active = tradable;
                
                buyButtons.add(buyButton);
                this.addRenderableWidget(buyButton);
            }
            
            if (item.sell > 0) {
                int sellButtonX = this.leftPos + ENTRY_START_X + ENTRY_WIDTH - BUTTON_WIDTH - 3;
                int buttonY = entryY + (ENTRY_HEIGHT - BUTTON_HEIGHT) / 2;
                
                Component sellText = Component.translatable("gui.iska_utils.shop.sell");
                boolean tradable = ShopEntryHelper.isPlayerShopTradable(item) && ShopEntryHelper.isSellAllowed(item);
                
                Button sellButton = Button.builder(sellText, button -> {
                    int multiplier = calculateMultiplier();
                    handleSellButtonClick(item, multiplier);
                }).bounds(sellButtonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
                sellButton.active = tradable;
                
                sellButtons.add(sellButton);
                this.addRenderableWidget(sellButton);
            }
        }
    }
    
    /**
     * Static hook for team data updates from the server
     */
    public static void handleTeamDataUpdate(String teamName, Map<String, Double> teamBalances) {
        if (currentInstance != null) {
            currentInstance.updateTeamData(teamName, teamBalances);
        }
    }
    
    /**
     * Apply team data received from the server
     */
    private void updateTeamData(String teamName, Map<String, Double> teamBalances) {
        this.playerTeamName = teamName;
        this.playerTeamBalances.clear();
        if (teamBalances != null) {
            this.playerTeamBalances.putAll(teamBalances);
        }
    }
    
    @Override
    public void removed() {
        super.removed();
        // Clear static instance when GUI closes
        if (currentInstance == this) {
            currentInstance = null;
        }
    }
    
    /**
     * Handle buy button click
     */
    private void handleBuyButtonClick(ShopEntry item, int multiplier) {
        if (!ShopEntryHelper.isPlayerShopTradable(item) || !ShopEntryHelper.isBuyAllowed(item)) {
            return;
        }
        if (isLimitBlocked(item, ShopPurchaseLimitsData.TradeSide.BUY)) {
            showFeedback(Component.translatable("gui.iska_utils.shop.tooltip.cannot_repeat").getString(), 0xFF4444);
            return;
        }
        // Require player to be in a team
        if (playerTeamName == null) {
            Component message = Component.translatable("gui.iska_utils.shop.feedback.no_team");
            showFeedback(message.getString(), 0xFF4444);
            return;
        }
        
        // Check balance before sending to server (free entries cost 0)
        String valuteId = item.valute != null ? item.valute : "null_coin";
        double currentBalance = playerTeamBalances.getOrDefault(valuteId, 0.0);
        int effectiveMultiplier = multiplier;
        ShopEntryTypeHandler buyHandler = ShopEntryTypeRegistry.get(item);
        if (buyHandler != null && !buyHandler.usesBuy()) {
            effectiveMultiplier = 1;
        }
        double totalCost = item.free || (buyHandler != null && !buyHandler.usesBuy())
                ? 0
                : (item.buy * effectiveMultiplier);
        
        if (currentBalance < totalCost) {
            String currencyName = getCurrencyName(valuteId);
            showInsufficientFundsError(currencyName);
            playButtonSound();
            return;
        }
        
        // Clear feedback on success path
        hideFeedback();
        
        // Send packet to server using entry id
        net.unfamily.iskautils.network.ModMessages.sendShopBuyItemPacket(item.id, effectiveMultiplier);
        
        playButtonSound();
    }
    
    /**
     * Handle sell button click
     */
    private void handleSellButtonClick(ShopEntry item, int multiplier) {
        if (!ShopEntryHelper.isPlayerShopTradable(item) || !ShopEntryHelper.isSellAllowed(item)) {
            return;
        }
        if (isLimitBlocked(item, ShopPurchaseLimitsData.TradeSide.SELL)) {
            showFeedback(Component.translatable("gui.iska_utils.shop.tooltip.cannot_repeat").getString(), 0xFF4444);
            return;
        }
        // Require player to be in a team
        if (playerTeamName == null) {
            Component message = Component.translatable("gui.iska_utils.shop.feedback.no_team");
            showFeedback(message.getString(), 0xFF4444);
            return;
        }
        
        // Client cannot reliably validate inventory for sell
        // show errors when server reports failure
        // Clear feedback and send to server
        hideFeedback();
        
        // Send packet to server using entry id
        net.unfamily.iskautils.network.ModMessages.sendShopSellItemPacket(item.id, multiplier);
        
        playButtonSound();
    }

    /**
     * Static hook for transaction errors from the server
     */
    public static void handleTransactionError(String errorType, String itemId, String valuteId) {
        if (currentInstance != null) {
            if ("insufficient_funds".equals(errorType)) {
                String currencyName = currentInstance.getCurrencyName(valuteId);
                currentInstance.showInsufficientFundsError(currencyName);
            } else if ("insufficient_items".equals(errorType)) {
                currentInstance.showInsufficientItemsError();
            } else if ("no_team".equals(errorType)) {
                Component message = Component.translatable("gui.iska_utils.shop.feedback.no_team");
                currentInstance.showFeedback(message.getString(), 0xFF4444);
            } else if ("stage_requirements".equals(errorType)) {
                Component message = Component.translatable("gui.iska_utils.shop.feedback.stage_requirements");
                currentInstance.showFeedback(message.getString(), 0xFF4444);
            } else if ("purchase_limit".equals(errorType)) {
                Component message = Component.translatable("gui.iska_utils.shop.feedback.purchase_limit");
                currentInstance.showFeedback(message.getString(), 0xFF4444);
            } else {
                Component message = Component.translatable("gui.iska_utils.shop.feedback.transaction_error");
                currentInstance.showFeedback(message.getString(), 0xFF4444);
            }
        }
    }
    
    /**
     * Static hook for transaction success from the server
     */
    public static void handleTransactionSuccess() {
        if (currentInstance != null) {
            currentInstance.hideFeedback();
        }
    }

    /**
     * Draw text scaled to fit the available width.
     * Se anche alla scala minima sfora, tronca e aggiunge "..."
     */
    private void renderScaledText(GuiGraphicsExtractor guiGraphics, String text, int x, int y, int maxWidth, int color) {
        Component textComponent = Component.literal(text);
        int textWidth = this.font.width(textComponent);
        
        if (textWidth <= maxWidth) {
            guiGraphics.text(this.font, textComponent, x, y, color, false);
        } else {
            // Scale down long text (min scale to reduce narrowing)
            float scale = (float) maxWidth / textWidth;
            float minScale = 0.85f;
            if (scale < minScale) {
                scale = minScale;
            }
            
            // If still too wide at min scale, truncate with ellipsis
            if (textWidth * scale > maxWidth && text.length() > 3) {
                String base = text;
                String ellipsis = "...";
                String truncated = base;
                while (truncated.length() > 3) {
                    String candidate = truncated + ellipsis;
                    int candidateWidth = this.font.width(candidate);
                    if (candidateWidth * scale <= maxWidth) {
                        textComponent = Component.literal(candidate);
                        break;
                    }
                    truncated = truncated.substring(0, truncated.length() - 1);
                }
                if (this.font.width(textComponent) * scale > maxWidth) {
                    textComponent = Component.literal(ellipsis);
                }
            }
            
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(x, y);
            guiGraphics.pose().scale(scale, scale);
            
            guiGraphics.text(this.font, textComponent, 0, 0, color, false);
            
            guiGraphics.pose().popMatrix();
        }
    }

    /**
     * Compute quantity multiplier from pressed modifiers
     * Come specificato: click normale = 1, ctrl/alt = 4, shift = 16
     */
    private int calculateMultiplier() {
        if (isKeyDown(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)
                || isKeyDown(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT)) {
            return 16;
        } else if (isKeyDown(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL)
                || isKeyDown(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL)
                || isKeyDown(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT)
                || isKeyDown(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_ALT)) {
            return 4;
        }
        return 1;
    }

    private boolean isKeyDown(int key) {
        return minecraft != null && com.mojang.blaze3d.platform.InputConstants.isKeyDown(minecraft.getWindow(), key);
    }

    private static void setTooltip(GuiGraphicsExtractor graphics, net.minecraft.client.gui.Font font,
                                   List<? extends Component> lines, int mouseX, int mouseY) {
        graphics.setTooltipForNextFrame(font,
                lines.stream().map(Component::getVisualOrderText).toList(), mouseX, mouseY);
    }

    private static void setItemTooltip(GuiGraphicsExtractor graphics, net.minecraft.client.gui.Font font,
                                       ItemStack stack, int mouseX, int mouseY) {
        graphics.setTooltipForNextFrame(font,
                List.of(stack.getHoverName().getVisualOrderText()), mouseX, mouseY);
    }


    /**
     * Returns stage requirements that are not satisfied: required but missing, or forbidden but present.
     */
    private List<ShopClientStages.StageFailure> getMissingStages(ShopEntry item) {
        return ShopClientStages.getFailures(item);
    }
    
    /**
     * Whether an entry is locked by missing stages
     */
    private boolean isItemBlocked(ShopEntry item) {
        return ShopClientStages.isEntryBlocked(item)
                || isLimitBlocked(item, ShopPurchaseLimitsData.TradeSide.BUY)
                || isLimitBlocked(item, ShopPurchaseLimitsData.TradeSide.SELL);
    }

    private boolean isLimitBlocked(ShopEntry item, ShopPurchaseLimitsData.TradeSide side) {
        return ShopClientPurchaseLimits.isBlocked(item.id, side);
    }

    private List<Component> createBlockedTooltip(ShopEntry item) {
        List<Component> tooltip = createMissingStagesTooltip(item);
        for (ShopPurchaseLimitsData.TradeSide side : ShopPurchaseLimitsData.TradeSide.values()) {
            if (!isLimitBlocked(item, side)) {
                continue;
            }
            if (!tooltip.isEmpty()) {
                tooltip.add(Component.empty());
            }
            tooltip.addAll(createPurchaseLimitTooltip(item, side));
        }
        return tooltip;
    }

    private List<Component> createPurchaseLimitTooltip(ShopEntry item, ShopPurchaseLimitsData.TradeSide side) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.purchase_limit",
                side.id().toUpperCase(Locale.ROOT)));
        long reset = ShopClientPurchaseLimits.nextResetEpochMs(item.id, side);
        if (reset > 0) {
            String formatted = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(java.time.ZoneId.systemDefault())
                    .format(java.time.Instant.ofEpochMilli(reset));
            tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.resets_at", formatted));
        } else {
            tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.cannot_repeat"));
        }
        return tooltip;
    }
    
    /**
     * Build tooltip for unmet stages: missing required and "must not have".
     */
    private List<Component> createMissingStagesTooltip(ShopEntry item) {
        List<Component> tooltip = new ArrayList<>();
        List<ShopClientStages.StageFailure> failures = getMissingStages(item);
        
        if (failures.isEmpty()) {
            return tooltip;
        }
        
        List<ShopClientStages.StageFailure> requiredMissing = failures.stream().filter(f -> f.required()).toList();
        List<ShopClientStages.StageFailure> mustNotHave = failures.stream().filter(f -> !f.required()).toList();
        
        if (!requiredMissing.isEmpty()) {
            tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.missing_stages"));
            tooltip.add(Component.literal(""));
            groupByTypeAndAppend(requiredMissing, tooltip);
        }
        
        if (!mustNotHave.isEmpty()) {
            if (!requiredMissing.isEmpty()) {
                tooltip.add(Component.literal(""));
            }
            tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.must_not_have_stages"));
            tooltip.add(Component.literal(""));
            groupByTypeAndAppend(mustNotHave, tooltip);
        }
        
        return tooltip;
    }
    
    private void groupByTypeAndAppend(List<ShopClientStages.StageFailure> failures, List<Component> tooltip) {
        Map<String, List<String>> byType = new HashMap<>();
        for (ShopClientStages.StageFailure f : failures) {
            byType.computeIfAbsent(f.stageType(), k -> new ArrayList<>()).add(f.stageId());
        }
        for (Map.Entry<String, List<String>> entry : byType.entrySet()) {
            String type = entry.getKey();
            String typeLabel = switch (type.toLowerCase()) {
                case "world" -> "World:";
                case "player" -> "Player:";
                case "team" -> "Team:";
                default -> type + ":";
            };
            tooltip.add(Component.literal(typeLabel));
            for (String stage : entry.getValue()) {
                tooltip.add(Component.literal("  - " + stage));
            }
        }
    }
    
    /**
     * Entry index under mouse
     */
    private int getEntryUnderMouse(int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int startY = entryStartY();
        int entries = visibleEntries();
        
        for (int i = 0; i < entries; i++) {
            int entryX = x + ENTRY_START_X;
            int entryY = y + startY + i * ENTRY_HEIGHT;
            
            if (mouseX >= entryX && mouseX < entryX + ENTRY_WIDTH &&
                mouseY >= entryY && mouseY < entryY + ENTRY_HEIGHT) {
                return scrollOffset + i;
            }
        }
        return -1;
    }

    /**
     * Notifies all open ShopScreen instances to reload data
     */
    public static void notifyReload() {
        if (currentInstance != null) {
            currentInstance.reloadShopData();
        }
    }
} 