package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.shop.ShopLoader;
import net.unfamily.iskautils.shop.ShopCategory;
import net.unfamily.iskautils.shop.ShopEntry;
import net.unfamily.iskautils.shop.ShopValute;

import java.util.*;
import java.util.stream.Collectors;

public class ShopScreen extends AbstractContainerScreen<ShopMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/backgrounds/shop.png");
    private static final ResourceLocation ENTRY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/entry_wide.png");
    private static final ResourceLocation SCROLLBAR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/scrollbar.png");
    private static final ResourceLocation SINGLE_SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/single_slot.png");

    private static final int GUI_WIDTH = 240;
    private static final int GUI_HEIGHT = 240;
    private static final int ENTRY_WIDTH = 140;
    private static final int ENTRY_HEIGHT = 24;
    private static final int ENTRIES = 5;
    private static final int ENTRY_START_X = 30; // (200-140)/2
    private static final int ENTRY_START_Y = 20;
    
    // Scrollbar constants (dalla StructurePlacerScreen)
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int SCROLLBAR_HEIGHT = 34;
    private static final int HANDLE_SIZE = 8;
    
    // Posizioni scrollbar (accanto alla prima entry)
    private static final int SCROLLBAR_X = ENTRY_START_X + ENTRY_WIDTH + 4; // 4 pixel di margine
    private static final int BUTTON_UP_Y = ENTRY_START_Y; // Pulsante SU all'inizio
    private static final int SCROLLBAR_Y = ENTRY_START_Y + HANDLE_SIZE; // Scrollbar sotto il pulsante SU
    private static final int BUTTON_DOWN_Y = SCROLLBAR_Y + SCROLLBAR_HEIGHT; // Pulsante GIÙ subito dopo
    
    // Costanti per i pulsanti Buy/Sell
    private static final int BUTTON_WIDTH = 30; // Allargati (era 25)
    private static final int BUTTON_HEIGHT = 12; // Più alti (era 10) 
    private static final int BUTTONS_SPACING = 3; // Spazio tra Buy e Sell (era 2)
    
    // Costanti per l'area informazioni a destra
    private static final int INFO_AREA_X = 195; // Spostato più a sinistra (era 205)
    private static final int INFO_AREA_WIDTH = 35; // Larghezza dell'area informazioni
    private static final int BACK_BUTTON_WIDTH = 30; // Ridotto per stare nell'area
    private static final int BACK_BUTTON_HEIGHT = 15;
    private static final int BACK_BUTTON_X = INFO_AREA_X + 2; // Spostato più a sinistra (era centrato)
    private static final int BACK_BUTTON_Y = 20; // Stesso livello delle entry
    private static final int CURRENCIES_START_Y = BACK_BUTTON_Y + BACK_BUTTON_HEIGHT + 10; // 10px sotto il pulsante
    
    // Variabili per lo scrolling
    private int scrollOffset = 0;
    private int totalShopEntries = 0;
    private boolean isDraggingHandle = false;
    private int dragStartY = 0;
    private int dragStartScrollOffset = 0;
    
    // Modalità della GUI
    private boolean showingCategories = true; // true = mostra categorie, false = mostra item
    private String currentCategoryId = null;
    private String currentCategoryName = "Shop";
    
    // Dati del shop
    private List<ShopCategory> availableCategories = new ArrayList<>();
    private List<ShopEntry> availableItems = new ArrayList<>();
    private Map<String, ShopValute> availableValutes = new HashMap<>();
    
    // Pulsanti vanilla
    private Button backButton;
    private List<Button> buyButtons = new ArrayList<>();
    private List<Button> sellButtons = new ArrayList<>();
    
    // Dati del team del giocatore
    private String playerTeamName = null;
    private Map<String, Double> playerTeamBalances = new HashMap<>();
    private static ShopScreen currentInstance = null; // Per il callback statico

    public ShopScreen(ShopMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        
        // Carica i dati del shop
        loadShopData();
        
        // Registra questa istanza per il callback
        currentInstance = this;
        
        // Richiedi i dati del team dal server
        net.unfamily.iskautils.network.ModMessages.sendShopTeamDataRequest();
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Crea il pulsante Back vanilla
        int buttonX = this.leftPos + BACK_BUTTON_X;
        int buttonY = this.topPos + BACK_BUTTON_Y;
        
        this.backButton = Button.builder(Component.translatable("gui.iska_utils.shop.back"), button -> {
            if (!showingCategories) {
                navigateBackToCategories();
            }
        }).bounds(buttonX, buttonY, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT).build();
        
        this.addRenderableWidget(backButton);
        
        // Aggiorna lo stato del pulsante e crea i pulsanti Buy/Sell
        updateBackButtonState();
        updateBuySellButtons();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        // Sfondo principale
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 240, 240);
        
        // Entry del shop
        for (int i = 0; i < ENTRIES; i++) {
            int entryY = y + ENTRY_START_Y + i * ENTRY_HEIGHT;
            guiGraphics.blit(ENTRY_TEXTURE, x + ENTRY_START_X, entryY, 0, 0, ENTRY_WIDTH, ENTRY_HEIGHT, ENTRY_WIDTH, ENTRY_HEIGHT);
            
            // Renderizza il contenuto della entry (slot + testo)
            renderEntryContent(guiGraphics, x + ENTRY_START_X, entryY, i);
        }
        
        // Scrollbar (solo accanto alla prima entry)
        renderScrollbar(guiGraphics, mouseX, mouseY);
        
        // Area informazioni a destra
        renderInfoArea(guiGraphics, mouseX, mouseY);
    }
    

    

    
    private void renderScrollbar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        int scrollbarX = x + SCROLLBAR_X;
        int scrollbarY = y + SCROLLBAR_Y;
        int buttonUpY = y + BUTTON_UP_Y;
        int buttonDownY = y + BUTTON_DOWN_Y;
        
        // Disegna la scrollbar completa (8 pixel larghe, altezza 34)
        guiGraphics.blit(SCROLLBAR_TEXTURE, scrollbarX, scrollbarY, 0, 0, 
                        SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT, 32, 34);
        
        // Pulsante SU (8x8 pixel) - sopra la scrollbar
        boolean upHovered = mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE &&
                           mouseY >= buttonUpY && mouseY < buttonUpY + HANDLE_SIZE;
        int upTextureY = upHovered ? HANDLE_SIZE : 0;
        guiGraphics.blit(SCROLLBAR_TEXTURE, scrollbarX, buttonUpY, 
                        SCROLLBAR_WIDTH * 2, upTextureY, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        
        // Pulsante GIÙ (8x8 pixel) - sotto la scrollbar  
        boolean downHovered = mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE &&
                             mouseY >= buttonDownY && mouseY < buttonDownY + HANDLE_SIZE;
        int downTextureY = downHovered ? HANDLE_SIZE : 0;
        guiGraphics.blit(SCROLLBAR_TEXTURE, scrollbarX, buttonDownY, 
                        SCROLLBAR_WIDTH * 3, downTextureY, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        
        // Handle (8x8 pixel) - sempre visibile, ma mobile solo se necessario
        float scrollRatio = 0;
        if (totalShopEntries > ENTRIES) {
            scrollRatio = (float) scrollOffset / (totalShopEntries - ENTRIES);
        }
        int handleY = scrollbarY + (int)(scrollRatio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
        
        boolean handleHovered = mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE &&
                               mouseY >= handleY && mouseY < handleY + HANDLE_SIZE;
        int handleTextureY = handleHovered ? HANDLE_SIZE : 0;
        guiGraphics.blit(SCROLLBAR_TEXTURE, scrollbarX, handleY, 
                        SCROLLBAR_WIDTH, handleTextureY, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Click sinistro
            if (handleScrollButtonClick(mouseX, mouseY) ||
                handleHandleClick(mouseX, mouseY) ||
                handleScrollbarClick(mouseX, mouseY) ||
                handleEntryClick(mouseX, mouseY)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private boolean handleScrollButtonClick(double mouseX, double mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int scrollbarX = x + SCROLLBAR_X;
        int buttonUpY = y + BUTTON_UP_Y;
        int buttonDownY = y + BUTTON_DOWN_Y;
        
        // Pulsante SU
        if (mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE &&
            mouseY >= buttonUpY && mouseY < buttonUpY + HANDLE_SIZE) {
            scrollUp();
            return true;
        }
        
        // Pulsante GIÙ
        if (mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE &&
            mouseY >= buttonDownY && mouseY < buttonDownY + HANDLE_SIZE) {
            scrollDown();
            return true;
        }
        
        return false;
    }
    
    private boolean handleHandleClick(double mouseX, double mouseY) {
        if (totalShopEntries <= ENTRIES) return false; // Non draggabile se poche entry
        
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int scrollbarX = x + SCROLLBAR_X;
        int scrollbarY = y + SCROLLBAR_Y;
        
        float scrollRatio = (float) scrollOffset / (totalShopEntries - ENTRIES);
        int handleY = scrollbarY + (int)(scrollRatio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
        
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
        if (totalShopEntries <= ENTRIES) return false;
        
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int scrollbarX = x + SCROLLBAR_X;
        int scrollbarY = y + SCROLLBAR_Y;
        
        if (mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_WIDTH &&
            mouseY >= scrollbarY && mouseY < scrollbarY + SCROLLBAR_HEIGHT) {
            
            // Calcola la nuova posizione del scroll in base al click
            float clickRatio = (float)(mouseY - scrollbarY) / SCROLLBAR_HEIGHT;
            clickRatio = Math.max(0, Math.min(1, clickRatio)); // Clamp tra 0 e 1
            
            int newScrollOffset = (int)(clickRatio * (totalShopEntries - ENTRIES));
            newScrollOffset = Math.max(0, Math.min(totalShopEntries - ENTRIES, newScrollOffset));
            
            if (newScrollOffset != scrollOffset) {
                scrollOffset = newScrollOffset;
                updateBuySellButtons();
                playButtonSound();
            }
            return true;
        }
        return false;
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isDraggingHandle) {
            isDraggingHandle = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && isDraggingHandle && totalShopEntries > ENTRIES) {
            int deltaY = (int) mouseY - dragStartY;
            float scrollRatio = (float) deltaY / (SCROLLBAR_HEIGHT - HANDLE_SIZE);
            
            int newScrollOffset = dragStartScrollOffset + (int)(scrollRatio * (totalShopEntries - ENTRIES));
            newScrollOffset = Math.max(0, Math.min(totalShopEntries - ENTRIES, newScrollOffset));
            
            scrollOffset = newScrollOffset;
            updateBuySellButtons();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
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
        if (totalShopEntries > ENTRIES && scrollOffset > 0) {
            scrollOffset--;
            updateBuySellButtons();
            return true;
        }
        return false;
    }
    
    private boolean scrollDownSilent() {
        if (totalShopEntries > ENTRIES && scrollOffset < totalShopEntries - ENTRIES) {
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
        // Assicurati che scrollOffset sia valido
        if (totalShopEntries > ENTRIES) {
            scrollOffset = Math.max(0, Math.min(scrollOffset, totalShopEntries - ENTRIES));
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
     * Carica i dati del shop dal sistema
     */
    private void loadShopData() {
        Map<String, ShopCategory> categories = ShopLoader.getCategories();
        availableValutes = ShopLoader.getValutes();
        
        // Converte in lista e ordina per ID
        availableCategories = categories.values().stream()
                .sorted(Comparator.comparing(cat -> cat.id))
                .collect(Collectors.toList());
        
        // Inizializza modalità categorie
        showingCategories = true;
        currentCategoryId = null;
        currentCategoryName = Component.translatable("gui.iska_utils.shop.title").getString();
        totalShopEntries = availableCategories.size();
        
        // Reset scroll
        scrollOffset = 0;
    }
    
    /**
     * Renderizza il contenuto di una entry (slot + testo)
     */
    private void renderEntryContent(GuiGraphics guiGraphics, int entryX, int entryY, int entryIndex) {
        int actualIndex = scrollOffset + entryIndex;
        
        if (showingCategories) {
            // Modalità categorie
            if (actualIndex < availableCategories.size()) {
                ShopCategory category = availableCategories.get(actualIndex);
                renderCategoryEntry(guiGraphics, entryX, entryY, category);
            }
        } else {
            // Modalità item
            if (actualIndex < availableItems.size()) {
                ShopEntry item = availableItems.get(actualIndex);
                renderItemEntry(guiGraphics, entryX, entryY, item);
            }
        }
    }
    
    /**
     * Renderizza una entry di categoria
     */
    private void renderCategoryEntry(GuiGraphics guiGraphics, int entryX, int entryY, ShopCategory category) {
        // Posizioni
        int slotX = entryX + 3; // 3 pixel dal bordo sinistro
        int slotY = entryY + 3; // 3 pixel dal bordo superiore
        int textX = slotX + 18 + 6; // Dopo lo slot + 6 pixel di margine (spostato più al centro)
        int textY = entryY + (ENTRY_HEIGHT - 8) / 2; // Centrato verticalmente
        
        // Disegna lo slot (18x18)
        guiGraphics.blit(SINGLE_SLOT_TEXTURE, slotX, slotY, 0, 0, 18, 18, 18, 18);
        
        // Ottieni l'ItemStack per l'icona della categoria
        ItemStack categoryIcon = getItemStackFromString(category.item);
        if (!categoryIcon.isEmpty()) {
            guiGraphics.renderItem(categoryIcon, slotX + 1, slotY + 1);
        }
        
        // Disegna il nome della categoria
        Component categoryName = Component.literal(category.name);
        guiGraphics.drawString(this.font, categoryName, textX, textY, 0x404040, false);
    }
    
    /**
     * Renderizza una entry di item
     */
    private void renderItemEntry(GuiGraphics guiGraphics, int entryX, int entryY, ShopEntry item) {
        // Posizioni
        int slotX = entryX + 3; // 3 pixel dal bordo sinistro
        int slotY = entryY + 3; // 3 pixel dal bordo superiore
        int textX = slotX + 18 + 6; // Dopo lo slot + 6 pixel di margine (spostato più al centro)
        int textY = entryY + (ENTRY_HEIGHT - 8) / 2; // Centrato verticalmente
        
        // Disegna lo slot (18x18)
        guiGraphics.blit(SINGLE_SLOT_TEXTURE, slotX, slotY, 0, 0, 18, 18, 18, 18);
        
        // Ottieni l'ItemStack per l'item
        ItemStack itemStack = getItemStackFromString(item.item);
        if (!itemStack.isEmpty()) {
            itemStack.setCount(item.itemCount);
            guiGraphics.renderItem(itemStack, slotX + 1, slotY + 1);
            guiGraphics.renderItemDecorations(this.font, itemStack, slotX + 1, slotY + 1);
        }
        
        // Disegna il nome dell'item (nome display)
        String displayName = getItemDisplayName(item.item);
        Component itemName = Component.literal(displayName);
        guiGraphics.drawString(this.font, itemName, textX, textY, 0x404040, false);
        
        // Calcola posizioni per i pulsanti Buy/Sell (alla fine dell'entry e centrati verticalmente)
        int buyButtonX = entryX + ENTRY_WIDTH - BUTTON_WIDTH - BUTTONS_SPACING - BUTTON_WIDTH - 3; // 3 pixel dal bordo destro
        int sellButtonX = entryX + ENTRY_WIDTH - BUTTON_WIDTH - 3; // 3 pixel dal bordo destro
        int buttonsY = entryY + (ENTRY_HEIGHT - BUTTON_HEIGHT) / 2; // Centrati verticalmente
        
        // I pulsanti Buy/Sell sono ora gestiti come widget vanilla nel metodo init()
    }
    
    /**
     * Converte una stringa item ID in ItemStack
     */
    private ItemStack getItemStackFromString(String itemId) {
        try {
            ResourceLocation itemResource = ResourceLocation.parse(itemId);
            var item = BuiltInRegistries.ITEM.get(itemResource);
            if (item != Items.AIR) {
                return new ItemStack(item);
            }
        } catch (Exception e) {
            // Fallback su stone se l'item non è valido
        }
        return new ItemStack(Items.STONE);
    }
    
    /**
     * Ottiene il nome display di un item
     */
    private String getItemDisplayName(String itemId) {
        ItemStack stack = getItemStackFromString(itemId);
        return stack.getHoverName().getString();
    }
    
    /**
     * Gestisce il click su una entry
     */
    private boolean handleEntryClick(double mouseX, double mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        for (int i = 0; i < ENTRIES; i++) {
            int entryX = x + ENTRY_START_X;
            int entryY = y + ENTRY_START_Y + i * ENTRY_HEIGHT;
            
            if (mouseX >= entryX && mouseX < entryX + ENTRY_WIDTH &&
                mouseY >= entryY && mouseY < entryY + ENTRY_HEIGHT) {
                
                int actualIndex = scrollOffset + i;
                
                if (showingCategories) {
                    // Click su categoria - naviga agli item
                    if (actualIndex < availableCategories.size()) {
                        ShopCategory category = availableCategories.get(actualIndex);
                        navigateToCategory(category);
                        playButtonSound();
                        return true;
                    }
                } else {
                    // Click su item - non fare nulla per ora (nessun acquisto/vendita)
                    if (actualIndex < availableItems.size()) {
                        playButtonSound();
                        return true;
                    }
                }
                break;
            }
        }
        return false;
    }
    
    /**
     * Naviga a una categoria specifica
     */
    private void navigateToCategory(ShopCategory category) {
        showingCategories = false;
        currentCategoryId = category.id;
        currentCategoryName = category.name;
        
        // Carica gli item di questa categoria
        Map<String, ShopEntry> allEntries = ShopLoader.getEntries();
        availableItems = allEntries.values().stream()
                .filter(entry -> category.id.equals(entry.inCategory))
                .sorted(Comparator.comparing(entry -> entry.item))
                .collect(Collectors.toList());
        
        totalShopEntries = availableItems.size();
        scrollOffset = 0; // Reset scroll
        
        // Aggiorna lo stato del pulsante Back
        updateBackButtonState();
        updateBuySellButtons();
    }
    
    /**
     * Torna alla vista categorie
     */
    public void navigateBackToCategories() {
        showingCategories = true;
        currentCategoryId = null;
        currentCategoryName = Component.translatable("gui.iska_utils.shop.title").getString();
        availableItems.clear();
        totalShopEntries = availableCategories.size();
        scrollOffset = 0; // Reset scroll
        
        // Aggiorna lo stato del pulsante Back
        updateBackButtonState();
        updateBuySellButtons();
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Titolo centrato - usa il nome della categoria corrente
        Component titleComponent = Component.literal(currentCategoryName);
        int titleWidth = this.font.width(titleComponent);
        int titleX = (this.imageWidth - titleWidth) / 2;
        guiGraphics.drawString(this.font, titleComponent, titleX, 7, 0x404040, false);
        
        // Non renderizzare "Inventory" - rimosso
    }
    

    

    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        
        // Renderizza i tooltip per i pulsanti Buy/Sell
        renderButtonTooltips(guiGraphics, mouseX, mouseY);
    }
    
    /**
     * Renderizza i tooltip per i pulsanti Buy/Sell
     */
    private void renderButtonTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!showingCategories) { // Solo nella modalità item
            int x = (this.width - this.imageWidth) / 2;
            int y = (this.height - this.imageHeight) / 2;
            
            for (int i = 0; i < ENTRIES; i++) {
                int actualIndex = scrollOffset + i;
                if (actualIndex >= availableItems.size()) continue;
                
                ShopEntry item = availableItems.get(actualIndex);
                int entryY = y + ENTRY_START_Y + i * ENTRY_HEIGHT;
                
                // Posizioni dei pulsanti
                int buyButtonX = x + ENTRY_START_X + ENTRY_WIDTH - BUTTON_WIDTH - BUTTONS_SPACING - BUTTON_WIDTH - 3;
                int sellButtonX = x + ENTRY_START_X + ENTRY_WIDTH - BUTTON_WIDTH - 3;
                int buttonsY = entryY + (ENTRY_HEIGHT - BUTTON_HEIGHT) / 2;
                
                // Calcola posizioni corrette per i tooltip (stesse del rendering)
                int entryX = x + ENTRY_START_X; // Posizione X dell'entry
                int correctBuyButtonX = entryX + ENTRY_WIDTH - BUTTON_WIDTH - BUTTONS_SPACING - BUTTON_WIDTH - 3;
                int correctSellButtonX = entryX + ENTRY_WIDTH - BUTTON_WIDTH - 3;
                int correctButtonsY = entryY + (ENTRY_HEIGHT - BUTTON_HEIGHT) / 2; // Centrati verticalmente
                
                // Tooltip per pulsante Buy
                if (item.buy > 0 && mouseX >= correctBuyButtonX && mouseX < correctBuyButtonX + BUTTON_WIDTH &&
                    mouseY >= correctButtonsY && mouseY < correctButtonsY + BUTTON_HEIGHT) {
                    
                    List<Component> buyTooltip = createBuyTooltip(item);
                    guiGraphics.renderComponentTooltip(this.font, buyTooltip, mouseX, mouseY);
                    return;
                }
                
                // Tooltip per pulsante Sell
                if (item.sell > 0 && mouseX >= correctSellButtonX && mouseX < correctSellButtonX + BUTTON_WIDTH &&
                    mouseY >= correctButtonsY && mouseY < correctButtonsY + BUTTON_HEIGHT) {
                    
                    List<Component> sellTooltip = createSellTooltip(item);
                    guiGraphics.renderComponentTooltip(this.font, sellTooltip, mouseX, mouseY);
                    return;
                }
            }
        }
    }
    
    /**
     * Crea il tooltip per il pulsante Buy
     */
    private List<Component> createBuyTooltip(ShopEntry item) {
        List<Component> tooltip = new ArrayList<>();
        
        // Costo con simbolo della valuta
        String currencySymbol = getCurrencySymbol(item.valute);
        tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.buy.cost", item.buy, currencySymbol));
        
        // Istruzioni
        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.buy.click"));
        
        // Calcola quantità per Ctrl/Alt
        ItemStack stack = getItemStackFromString(item.item);
        int maxStackSize = stack.getMaxStackSize();
        int quarterStack = Math.max(1, maxStackSize / 4);
        
        tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.buy.ctrl", quarterStack));
        tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.buy.shift", maxStackSize));
        
        return tooltip;
    }
    
    /**
     * Crea il tooltip per il pulsante Sell
     */
    private List<Component> createSellTooltip(ShopEntry item) {
        List<Component> tooltip = new ArrayList<>();
        
        // Prezzo con simbolo della valuta
        String currencySymbol = getCurrencySymbol(item.valute);
        tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.sell.price", item.sell, currencySymbol));
        
        // Istruzioni
        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.sell.click"));
        
        // Calcola quantità per Ctrl/Alt
        ItemStack stack = getItemStackFromString(item.item);
        int maxStackSize = stack.getMaxStackSize();
        int quarterStack = Math.max(1, maxStackSize / 4);
        
        tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.sell.ctrl", quarterStack));
        tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.sell.shift", maxStackSize));
        
        return tooltip;
    }
    
    /**
     * Renderizza l'area informazioni a destra (solo valute, il pulsante Back è vanilla)
     */
    private void renderInfoArea(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        // Renderizza le valute disponibili
        renderAvailableCurrencies(guiGraphics, x, y);
    }
    

    
    /**
     * Renderizza le valute disponibili con i balance reali del team
     */
    private void renderAvailableCurrencies(GuiGraphics guiGraphics, int guiX, int guiY) {
        int startY = guiY + CURRENCIES_START_Y;
        int textX = guiX + INFO_AREA_X + 2; // 2px di margine dal bordo sinistro dell'area (era 3)
        
        // Se il giocatore non è in un team
        if (playerTeamName == null) {
            Component noTeamText = Component.translatable("gui.iska_utils.shop.no_team");
            guiGraphics.drawString(this.font, noTeamText, textX, startY, 0x808080, false);
            return;
        }
        
        int lineIndex = 0;
        for (ShopValute valute : availableValutes.values()) {
            int textY = startY + lineIndex * 10; // 10px di spaziatura tra le righe
            
            // Ottieni il balance reale del team per questa valuta
            double balance = playerTeamBalances.getOrDefault(valute.id, 0.0);
            
            // Formatta il balance (mostra come intero se è un numero intero, altrimenti con decimali)
            String balanceStr;
            if (balance == Math.floor(balance)) {
                balanceStr = String.valueOf((int)balance);
            } else {
                balanceStr = String.format("%.1f", balance);
            }
            
            String balanceText = balanceStr + " " + (valute.charSymbol != null ? valute.charSymbol : valute.id);
            Component currencyText = Component.literal(balanceText);
            
            // Colore: rosso se balance è 0, normale altrimenti
            int color = balance > 0 ? 0x404040 : 0x804040;
            guiGraphics.drawString(this.font, currencyText, textX, textY, color, false);
            
            lineIndex++;
        }
        
        // Se non ci sono valute configurate, mostra un messaggio
        if (availableValutes.isEmpty()) {
            Component noValutesText = Component.translatable("gui.iska_utils.shop.no_valutes");
            guiGraphics.drawString(this.font, noValutesText, textX, startY, 0x808080, false);
        }
    }
    
    /**
     * Aggiorna lo stato del pulsante Back
     */
    private void updateBackButtonState() {
        if (backButton != null) {
            backButton.active = !showingCategories;
        }
    }
    
    /**
     * Ottiene il simbolo della valuta
     */
    private String getCurrencySymbol(String valuteId) {
        if (valuteId == null) return "?";
        ShopValute valute = availableValutes.get(valuteId);
        if (valute != null && valute.charSymbol != null) {
            return valute.charSymbol;
        }
        return valuteId; // Fallback sull'ID
    }
    
    /**
     * Aggiorna i pulsanti Buy/Sell dinamici basandosi sulle entry visibili
     */
    private void updateBuySellButtons() {
        // Rimuovi tutti i pulsanti esistenti
        buyButtons.forEach(this::removeWidget);
        sellButtons.forEach(this::removeWidget);
        buyButtons.clear();
        sellButtons.clear();
        
        // Se stiamo mostrando le categorie, non servono pulsanti Buy/Sell
        if (showingCategories) {
            return;
        }
        
        // Crea pulsanti per ogni entry visibile
        int visibleEntries = Math.min(ENTRIES, totalShopEntries - scrollOffset);
        
        for (int i = 0; i < visibleEntries; i++) {
            int entryIndex = scrollOffset + i;
            if (entryIndex >= availableItems.size()) break;
            
            ShopEntry item = availableItems.get(entryIndex);
            int entryY = this.topPos + ENTRY_START_Y + i * ENTRY_HEIGHT;
            
            // Pulsante Buy
            if (item.buy > 0) {
                int buyButtonX = this.leftPos + ENTRY_START_X + ENTRY_WIDTH - BUTTON_WIDTH - BUTTONS_SPACING - BUTTON_WIDTH - 3;
                int buttonY = entryY + (ENTRY_HEIGHT - BUTTON_HEIGHT) / 2; // Centrati verticalmente
                
                Component buyText = Component.translatable("gui.iska_utils.shop.buy");
                
                Button buyButton = Button.builder(buyText, button -> {
                    // TODO: Implementare logica di acquisto
                    String currencySymbol = getCurrencySymbol(item.valute);
                    System.out.println("Buy " + item.item + " for " + item.buy + " " + currencySymbol);
                }).bounds(buyButtonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
                
                buyButtons.add(buyButton);
                this.addRenderableWidget(buyButton);
            }
            
            // Pulsante Sell
            if (item.sell > 0) {
                int sellButtonX = this.leftPos + ENTRY_START_X + ENTRY_WIDTH - BUTTON_WIDTH - 3;
                int buttonY = entryY + (ENTRY_HEIGHT - BUTTON_HEIGHT) / 2; // Centrati verticalmente
                
                Component sellText = Component.translatable("gui.iska_utils.shop.sell");
                
                Button sellButton = Button.builder(sellText, button -> {
                    // TODO: Implementare logica di vendita
                    String currencySymbol = getCurrencySymbol(item.valute);
                    System.out.println("Sell " + item.item + " for " + item.sell + " " + currencySymbol);
                }).bounds(sellButtonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
                
                sellButtons.add(sellButton);
                this.addRenderableWidget(sellButton);
            }
        }
    }
    
    /**
     * Metodo statico per gestire l'aggiornamento dei dati del team dal server
     */
    public static void handleTeamDataUpdate(String teamName, Map<String, Double> teamBalances) {
        if (currentInstance != null) {
            currentInstance.updateTeamData(teamName, teamBalances);
        }
    }
    
    /**
     * Aggiorna i dati del team ricevuti dal server
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
        // Cleanup del riferimento statico quando la GUI viene chiusa
        if (currentInstance == this) {
            currentInstance = null;
        }
    }


} 