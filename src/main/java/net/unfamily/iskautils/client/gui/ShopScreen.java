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
import net.unfamily.iskautils.shop.ItemConverter;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.*;
import java.util.stream.Collectors;

public class ShopScreen extends AbstractContainerScreen<AbstractContainerMenu> {
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
    private static final int INFO_AREA_X = 185; // Spostato ulteriormente a sinistra (era 195, scrollbar finisce a 182)
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
    protected String currentCategoryName = "Shop";
    
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
    
    /**
     * Notifica tutte le istanze aperte di ShopScreen di ricaricare i dati
     */
    public static void notifyReload() {
        if (currentInstance != null) {
            currentInstance.reloadShopData();
        }
    }
    
    // Area di feedback per messaggi di errore/successo
    private String feedbackMessage = null;
    private int feedbackColor = 0xFFFFFF;
    private long feedbackClearTime = 0;
    private static final long FEEDBACK_DISPLAY_TIME = 3000; // 3 secondi
    // Calcola la posizione del feedback al centro tra quinta entry e inventario (Y=154)
    private static final int INVENTORY_Y = 154; // Y dell'inventario principale (dal ShopMenu)
    private static final int FIFTH_ENTRY_END = ENTRY_START_Y + (ENTRIES * ENTRY_HEIGHT); // Fine quinta entry (Y=140)
    private static final int FEEDBACK_Y_OFFSET = FIFTH_ENTRY_END + ((INVENTORY_Y - FIFTH_ENTRY_END) / 2) - 4; // Centrato (Y=147-4=143)

    public ShopScreen(AbstractContainerMenu menu, Inventory playerInventory, Component title) {
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
        
        // Renderizza l'area di feedback
        updateAndRenderFeedback(guiGraphics, x, y);
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
            if (handleScrollButtonClick(mouseX, mouseY)) {
                return true;
            }
            if (handleHandleClick(mouseX, mouseY)) {
                return true;
            }
            if (handleScrollbarClick(mouseX, mouseY)) {
                return true;
            }
            if (handleEntryClick(mouseX, mouseY)) {
                return true;
            }
        }
        
        // Gestisci i pulsanti vanilla (inclusi Buy/Sell) dopo i nostri handler
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
     * Ricarica i dati del shop (per reload del sistema)
     */
    public void reloadShopData() {
        loadShopData();
        
        // Aggiorna i pulsanti
        updateBackButtonState();
        updateBuySellButtons();
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
        
        // Disegna il nome della categoria (scalato)
        // Per le categorie, il testo può andare fino alla fine dell'entry (no pulsanti)
        int maxTextWidth = entryX + ENTRY_WIDTH - textX - 5; // 5px di margine dal bordo destro
        
        renderScaledText(guiGraphics, category.name, textX, textY, maxTextWidth, 0x404040);
    }
    
    /**
     * Renderizza una entry di item
     */
    private void renderItemEntry(GuiGraphics guiGraphics, int entryX, int entryY, ShopEntry item) {
        // Controlla se l'item è bloccato
        boolean isBlocked = isItemBlocked(item);
        
        // Se bloccato, applica overlay rosso
        if (isBlocked) {
            // Overlay rosso semi-trasparente
            guiGraphics.fill(entryX, entryY, entryX + ENTRY_WIDTH, entryY + ENTRY_HEIGHT, 
                            0x80FF0000); // Rosso con alpha
        }
        
        // Posizioni
        int slotX = entryX + 3; // 3 pixel dal bordo sinistro
        int slotY = entryY + 3; // 3 pixel dal bordo superiore
        int textX = slotX + 18 + 6; // Dopo lo slot + 6 pixel di margine (spostato più al centro)
        int textY = entryY + (ENTRY_HEIGHT - 8) / 2; // Centrato verticalmente
        
        // Disegna lo slot (18x18)
        guiGraphics.blit(SINGLE_SLOT_TEXTURE, slotX, slotY, 0, 0, 18, 18, 18, 18);
        
        // Ottieni l'ItemStack per l'item (supporta data components inline)
        ItemStack itemStack = getItemStackFromString(item.item);
        if (!itemStack.isEmpty()) {
            itemStack.setCount(item.itemCount);
            guiGraphics.renderItem(itemStack, slotX + 1, slotY + 1);
            guiGraphics.renderItemDecorations(this.font, itemStack, slotX + 1, slotY + 1);
        }
        
        // Disegna il nome dell'item (nome display scalato)
        String displayName = getItemDisplayName(item.item);
        
        // Calcola la larghezza massima disponibile per il testo
        // Dal textX fino ai pulsanti (con un po' di margine)
        int buyButtonStartX = entryX + ENTRY_WIDTH - BUTTON_WIDTH - BUTTONS_SPACING - BUTTON_WIDTH - 3;
        int maxTextWidth = buyButtonStartX - textX - 5; // 5px di margine dai pulsanti
        
        // Renderizza il testo scalato
        renderScaledText(guiGraphics, displayName, textX, textY, maxTextWidth, 0x404040);
        
        // Calcola posizioni per i pulsanti Buy/Sell (alla fine dell'entry e centrati verticalmente)
        int buyButtonX = entryX + ENTRY_WIDTH - BUTTON_WIDTH - BUTTONS_SPACING - BUTTON_WIDTH - 3; // 3 pixel dal bordo destro
        int sellButtonX = entryX + ENTRY_WIDTH - BUTTON_WIDTH - 3; // 3 pixel dal bordo destro
        int buttonsY = entryY + (ENTRY_HEIGHT - BUTTON_HEIGHT) / 2; // Centrati verticalmente
        
        // I pulsanti Buy/Sell sono ora gestiti come widget vanilla nel metodo init()
    }
    
    /**
     * Converte una stringa item ID in ItemStack utilizzando il nuovo sistema 1.21.1
     */
    private ItemStack getItemStackFromString(String itemId) {
        return ItemConverter.parseItemString(itemId, 1);
    }
    
    /**
     * Ottiene il nome display di un item
     */
    private String getItemDisplayName(String itemId) {
        return ItemConverter.getItemDisplayName(itemId);
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
                    // In modalità item, controlla se il click è sui pulsanti Buy/Sell
                    if (actualIndex < availableItems.size()) {
                        ShopEntry item = availableItems.get(actualIndex);
                        
                        // Calcola le posizioni dei pulsanti Buy/Sell
                        int buyButtonX = entryX + ENTRY_WIDTH - BUTTON_WIDTH - BUTTONS_SPACING - BUTTON_WIDTH - 3;
                        int sellButtonX = entryX + ENTRY_WIDTH - BUTTON_WIDTH - 3;
                        int buttonY = entryY + (ENTRY_HEIGHT - BUTTON_HEIGHT) / 2;
                        
                        // Se il click è nell'area dei pulsanti, non gestirlo qui
                        boolean clickOnBuyButton = item.buy > 0 && 
                            mouseX >= buyButtonX && mouseX < buyButtonX + BUTTON_WIDTH &&
                            mouseY >= buttonY && mouseY < buttonY + BUTTON_HEIGHT;
                            
                        boolean clickOnSellButton = item.sell > 0 && 
                            mouseX >= sellButtonX && mouseX < sellButtonX + BUTTON_WIDTH &&
                            mouseY >= buttonY && mouseY < buttonY + BUTTON_HEIGHT;
                        
                        if (clickOnBuyButton || clickOnSellButton) {
                            return false; // Lascia che super.mouseClicked gestisca i pulsanti
                        }
                        
                        // Click su item ma non sui pulsanti - per ora non fare nulla
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
        // Centra il titolo nell'area delle entry (da ENTRY_START_X a ENTRY_START_X + ENTRY_WIDTH)
        int entryAreaStart = ENTRY_START_X; // 30
        int entryAreaWidth = ENTRY_WIDTH; // 140
        int titleX = entryAreaStart + (entryAreaWidth - titleWidth) / 2;
        guiGraphics.drawString(this.font, titleComponent, titleX, 7, 0x404040, false);
        
        // Non renderizzare "Inventory" - rimosso
    }
    

    

    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        
        // Controlla se il mouse è su un'entry bloccata
        if (!showingCategories) {
            int entryIndex = getEntryUnderMouse(mouseX, mouseY);
            if (entryIndex >= 0 && entryIndex < availableItems.size()) {
                ShopEntry item = availableItems.get(entryIndex);
                if (isItemBlocked(item)) {
                    List<Component> tooltip = createMissingStagesTooltip(item);
                    guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                    return;
                }
            }
        }
        
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
        tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.buy.ctrl"));
        tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.buy.shift"));
        
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
        tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.sell.ctrl"));
        tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.sell.shift"));
        
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
        int textX = guiX + BACK_BUTTON_X; // Allineato con il pulsante Back
        
        // Se il giocatore non è in un team
        if (playerTeamName == null) {
            Component noTeamText = Component.translatable("gui.iska_utils.shop.no_team");
            
            // Applica scaling 0.77 per rimpicciolire il testo
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(textX, startY, 0);
            guiGraphics.pose().scale(0.77f, 0.77f, 1.0f);
            
            guiGraphics.drawString(this.font, noTeamText, 0, 0, 0x808080, false);
            
            guiGraphics.pose().popPose();
            return;
        }
        
        
        int lineIndex = 0;
        for (ShopValute valute : availableValutes.values()) {
            int textY = startY + lineIndex * 10; // 10px di spaziatura tra le righe
            
            // Ottieni il balance reale del team per questa valuta
            double balance = playerTeamBalances.getOrDefault(valute.id, 0.0);
            
            // Formatta il balance con abbreviazioni per numeri grandi
            String balanceStr = formatLargeNumber(balance);
            
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
     * Formatta un valore numerico con abbreviazioni per numeri grandi
     * @param value Il valore da formattare
     * @return Stringa formattata (es: 10K, 1.5M, 2.3B)
     */
    private String formatLargeNumber(double value) {
        if (value < 10000) {
            // Sotto i 10.000, mostra il numero normale
            if (value == Math.floor(value)) {
                return String.valueOf((int)value);
            } else {
                return String.format("%.1f", value);
            }
        }
        
        String[] suffixes = {"", "K", "M", "B", "T", "P", "E"}; // K=migliaia, M=milioni, B=miliardi, T=trilioni
        int suffixIndex = 0;
        double formattedValue = value;
        
        // Trova il suffisso appropriato
        while (formattedValue >= 1000 && suffixIndex < suffixes.length - 1) {
            formattedValue /= 1000;
            suffixIndex++;
        }
        
        // Formatta il numero con 1 decimale se necessario
        if (formattedValue == Math.floor(formattedValue)) {
            return String.format("%.0f%s", formattedValue, suffixes[suffixIndex]);
        } else {
            return String.format("%.1f%s", formattedValue, suffixes[suffixIndex]);
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
     * Ottiene il nome della valuta invece dell'ID
     */
    private String getCurrencyName(String valuteId) {
        if (valuteId == null) return "?";
        ShopValute valute = availableValutes.get(valuteId);
        if (valute != null && valute.name != null && !valute.name.trim().isEmpty()) {
            return valute.name;
        }
        return valuteId; // Fallback sull'ID
    }
    
    /**
     * Mostra un messaggio di feedback nell'area sottostante
     */
    private void showFeedback(String message, int color) {
        this.feedbackMessage = message;
        this.feedbackColor = color;
        this.feedbackClearTime = System.currentTimeMillis() + FEEDBACK_DISPLAY_TIME;
    }
    
    /**
     * Mostra un messaggio di errore di fondi insufficienti
     */
    private void showInsufficientFundsError(String currencyName) {
        Component message = Component.translatable("gui.iska_utils.shop.feedback.insufficient_funds", currencyName);
        showFeedback(message.getString(), 0xFF4444); // Rosso
    }
    
    /**
     * Mostra un messaggio di errore di oggetti insufficienti
     */
    private void showInsufficientItemsError() {
        Component message = Component.translatable("gui.iska_utils.shop.feedback.insufficient_items");
        showFeedback(message.getString(), 0xFF4444); // Rosso
    }
    
    /**
     * Nasconde il messaggio di feedback (successo)
     */
    private void hideFeedback() {
        this.feedbackMessage = null;
        this.feedbackClearTime = 0;
    }
    
    /**
     * Aggiorna e renderizza l'area di feedback
     */
    private void updateAndRenderFeedback(GuiGraphics guiGraphics, int guiX, int guiY) {
        // Controlla se è tempo di nascondere il messaggio
        if (feedbackMessage != null && System.currentTimeMillis() >= feedbackClearTime) {
            hideFeedback();
        }
        
        // Renderizza il messaggio se presente
        if (feedbackMessage != null) {
            int textX = guiX + ENTRY_START_X + 5; // 5px di margine
            int textY = guiY + FEEDBACK_Y_OFFSET;
            guiGraphics.drawString(this.font, feedbackMessage, textX, textY, feedbackColor, false);
        }
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
            if (entryIndex >= availableItems.size()) {
                break;
            }
            
            ShopEntry item = availableItems.get(entryIndex);
            int entryY = this.topPos + ENTRY_START_Y + i * ENTRY_HEIGHT;
            
            // Pulsante Buy
            if (item.buy > 0) {
                int buyButtonX = this.leftPos + ENTRY_START_X + ENTRY_WIDTH - BUTTON_WIDTH - BUTTONS_SPACING - BUTTON_WIDTH - 3;
                int buttonY = entryY + (ENTRY_HEIGHT - BUTTON_HEIGHT) / 2; // Centrati verticalmente
                
                Component buyText = Component.translatable("gui.iska_utils.shop.buy");
                
                Button buyButton = Button.builder(buyText, button -> {
                    int multiplier = calculateMultiplier();
                    handleBuyButtonClick(item, multiplier);
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
                    int multiplier = calculateMultiplier();
                    handleSellButtonClick(item, multiplier);
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
    
    /**
     * Gestisce il click di acquisto
     */
    private void handleBuyButtonClick(ShopEntry item, int multiplier) {
        // Controlla se il giocatore è in un team
        if (playerTeamName == null) {
            Component message = Component.translatable("gui.iska_utils.shop.feedback.no_team");
            showFeedback(message.getString(), 0xFF4444);
            return;
        }
        
        // Controlla i fondi prima di inviare al server
        String valuteId = item.valute != null ? item.valute : "null_coin";
        double currentBalance = playerTeamBalances.getOrDefault(valuteId, 0.0);
        double totalCost = item.buy * multiplier;
        
        if (currentBalance < totalCost) {
            String currencyName = getCurrencyName(valuteId);
            showInsufficientFundsError(currencyName);
            playButtonSound(); // Suono anche per il fallimento
            return;
        }
        
        // Nascondi il feedback se presente (successo)
        hideFeedback();
        
        // Invia il packet al server - usa l'ID univoco della entry
        net.unfamily.iskautils.network.ModMessages.sendShopBuyItemPacket(item.id, multiplier);
        
        playButtonSound();
    }
    
    /**
     * Gestisce il click di vendita
     */
    private void handleSellButtonClick(ShopEntry item, int multiplier) {
        // Controlla se il giocatore è in un team
        if (playerTeamName == null) {
            Component message = Component.translatable("gui.iska_utils.shop.feedback.no_team");
            showFeedback(message.getString(), 0xFF4444);
            return;
        }
        
        // Per la vendita non possiamo facilmente controllare l'inventario dal client
        // quindi mostriamo solo l'errore se il server ci informa del fallimento
        // Per ora nascondiamo il feedback e inviamo al server
        hideFeedback();
        
        // Invia il packet al server - usa l'ID univoco della entry
        net.unfamily.iskautils.network.ModMessages.sendShopSellItemPacket(item.id, multiplier);
        
        playButtonSound();
    }

    /**
     * Metodo statico per gestire errori di transazione dal server
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
            } else {
                Component message = Component.translatable("gui.iska_utils.shop.feedback.transaction_error");
                currentInstance.showFeedback(message.getString(), 0xFF4444);
            }
        }
    }
    
    /**
     * Metodo statico per gestire il successo delle transazioni dal server
     */
    public static void handleTransactionSuccess() {
        if (currentInstance != null) {
            currentInstance.hideFeedback();
        }
    }

    /**
     * Renderizza testo scalato per adattarsi alla larghezza disponibile
     */
    private void renderScaledText(GuiGraphics guiGraphics, String text, int x, int y, int maxWidth, int color) {
        Component textComponent = Component.literal(text);
        int textWidth = this.font.width(textComponent);
        
        if (textWidth <= maxWidth) {
            // Il testo sta già nella larghezza disponibile
            guiGraphics.drawString(this.font, textComponent, x, y, color, false);
        } else {
            // Il testo è troppo lungo, dobbiamo scalarlo
            float scale = (float) maxWidth / textWidth;
            
            // Applica la trasformazione di scaling
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x, y, 0);
            guiGraphics.pose().scale(scale, scale, 1.0f);
            
            // Renderizza il testo scalato alla posizione (0,0) nella matrice trasformata
            guiGraphics.drawString(this.font, textComponent, 0, 0, color, false);
            
            // Ripristina la matrice
            guiGraphics.pose().popPose();
        }
    }

    /**
     * Calcola il moltiplicatore basandosi sui modificatori premuti
     * Come specificato: click normale = 1, ctrl/alt = 4, shift = 16
     */
    private int calculateMultiplier() {
        if (Screen.hasShiftDown()) {
            return 16;
        } else if (Screen.hasControlDown() || Screen.hasAltDown()) {
            return 4;
        } else {
            return 1;
        }
    }

    /**
     * Controlla se un item ha stage mancanti
     */
    private List<String> getMissingStages(ShopEntry item) {
        List<String> missingStages = new ArrayList<>();
        
        if (item.stages != null && this.minecraft != null && this.minecraft.player != null) {
            try {
                // Ottieni il server player per i controlli stage
                net.minecraft.server.MinecraftServer server = this.minecraft.getSingleplayerServer();
                if (server != null) {
                    // Trova il server player corrispondente al client player
                    net.minecraft.server.level.ServerPlayer serverPlayer = null;
                    for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
                        if (player.getName().getString().equals(this.minecraft.player.getName().getString())) {
                            serverPlayer = player;
                            break;
                        }
                    }
                    
                    if (serverPlayer != null) {
                        // Ottieni il registry degli stage
                        net.unfamily.iskautils.stage.StageRegistry registry = 
                            net.unfamily.iskautils.stage.StageRegistry.getInstance(server);
                        
                        // Controlla ogni stage richiesto
                        for (net.unfamily.iskautils.shop.ShopStage stage : item.stages) {
                            boolean hasStage = false;
                            
                            switch (stage.stageType.toLowerCase()) {
                                case "player":
                                    hasStage = registry.hasPlayerStage(serverPlayer, stage.stage);
                                    break;
                                case "world":
                                    hasStage = registry.hasWorldStage(stage.stage);
                                    break;
                                case "team":
                                    hasStage = registry.hasPlayerTeamStage(serverPlayer, stage.stage);
                                    break;
                            }
                            
                            // Se lo stage non è presente quando dovrebbe essere, o viceversa
                            if (hasStage != stage.is) {
                                missingStages.add(stage.stageType + ":" + stage.stage);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Ignora errori - in caso di problemi, non mostrare stage mancanti
            }
        }
        
        return missingStages;
    }
    
    /**
     * Controlla se un item è bloccato da stage mancanti
     */
    private boolean isItemBlocked(ShopEntry item) {
        return !getMissingStages(item).isEmpty();
    }
    
    /**
     * Crea il tooltip per gli stage mancanti
     */
    private List<Component> createMissingStagesTooltip(ShopEntry item) {
        List<Component> tooltip = new ArrayList<>();
        List<String> missingStages = getMissingStages(item);
        
        if (!missingStages.isEmpty()) {
            // Raggruppa stage mancanti per tipo
            Map<String, List<String>> missingByType = new HashMap<>();
            
            for (String missingStage : missingStages) {
                String[] parts = missingStage.split(":", 2);
                if (parts.length == 2) {
                    String type = parts[0];
                    String stage = parts[1];
                    missingByType.computeIfAbsent(type, k -> new ArrayList<>()).add(stage);
                }
            }
            
            // Crea tooltip strutturato
            tooltip.add(Component.translatable("gui.iska_utils.shop.tooltip.missing_stages"));
            tooltip.add(Component.literal(""));
            
            for (Map.Entry<String, List<String>> entry : missingByType.entrySet()) {
                String type = entry.getKey();
                List<String> stages = entry.getValue();
                
                String typeLabel = switch (type.toLowerCase()) {
                    case "world" -> "World:";
                    case "player" -> "Player:";
                    case "team" -> "Team:";
                    default -> type + ":";
                };
                
                tooltip.add(Component.literal(typeLabel));
                for (String stage : stages) {
                    tooltip.add(Component.literal("  -" + stage));
                }
            }
        }
        
        return tooltip;
    }
    
    /**
     * Ottiene l'indice dell'entry sotto il mouse
     */
    private int getEntryUnderMouse(int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        for (int i = 0; i < ENTRIES; i++) {
            int entryX = x + ENTRY_START_X;
            int entryY = y + ENTRY_START_Y + i * ENTRY_HEIGHT;
            
            if (mouseX >= entryX && mouseX < entryX + ENTRY_WIDTH &&
                mouseY >= entryY && mouseY < entryY + ENTRY_HEIGHT) {
                return scrollOffset + i;
            }
        }
        return -1;
    }
} 