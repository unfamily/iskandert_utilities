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

public class AutoShopScreen extends AbstractContainerScreen<AutoShopMenu> {
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
    private String currentCategoryName = "Negozio Automatico";
    
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
    private static AutoShopScreen currentInstance = null; // Per il callback statico
    
    // Area di feedback per messaggi di errore/successo
    private String feedbackMessage = null;
    private int feedbackColor = 0xFFFFFF;
    private long feedbackClearTime = 0;
    private static final long FEEDBACK_DISPLAY_TIME = 3000; // 3 secondi
    // Calcola la posizione del feedback al centro tra quinta entry e inventario (Y=154)
    private static final int INVENTORY_Y = 154; // Y dell'inventario principale (dal ShopMenu)
    private static final int FIFTH_ENTRY_END = ENTRY_START_Y + (ENTRIES * ENTRY_HEIGHT); // Fine quinta entry (Y=140)
    private static final int FEEDBACK_Y_OFFSET = FIFTH_ENTRY_END + ((INVENTORY_Y - FIFTH_ENTRY_END) / 2) - 4; // Centrato (Y=147-4=143)

    public AutoShopScreen(AutoShopMenu menu, Inventory playerInventory, Component title) {
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
    
    /**
     * Renderizza la scrollbar
     */
    private void renderScrollbar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (totalShopEntries <= ENTRIES) {
            return; // Non serve scrollbar
        }
        
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        // Pulsante SU
        boolean upHovered = mouseX >= x + SCROLLBAR_X && mouseX <= x + SCROLLBAR_X + SCROLLBAR_WIDTH &&
                           mouseY >= y + BUTTON_UP_Y && mouseY <= y + BUTTON_UP_Y + HANDLE_SIZE;
        
        int upV = upHovered ? 8 : 0; // 8 pixel di offset per l'hover
        guiGraphics.blit(SCROLLBAR_TEXTURE, x + SCROLLBAR_X, y + BUTTON_UP_Y, 
                        0, upV, SCROLLBAR_WIDTH, HANDLE_SIZE, SCROLLBAR_WIDTH, 16);
        
        // Pulsante GIÙ
        boolean downHovered = mouseX >= x + SCROLLBAR_X && mouseX <= x + SCROLLBAR_X + SCROLLBAR_WIDTH &&
                             mouseY >= y + BUTTON_DOWN_Y && mouseY <= y + BUTTON_DOWN_Y + HANDLE_SIZE;
        
        int downV = downHovered ? 24 : 16; // 16 pixel di offset per il pulsante giù, +8 per hover
        guiGraphics.blit(SCROLLBAR_TEXTURE, x + SCROLLBAR_X, y + BUTTON_DOWN_Y, 
                        0, downV, SCROLLBAR_WIDTH, HANDLE_SIZE, SCROLLBAR_WIDTH, 32);
        
        // Handle della scrollbar
        float scrollRatio = (float) scrollOffset / (totalShopEntries - ENTRIES);
        int handleY = y + SCROLLBAR_Y + (int)(scrollRatio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
        
        boolean handleHovered = mouseX >= x + SCROLLBAR_X && mouseX <= x + SCROLLBAR_X + SCROLLBAR_WIDTH &&
                               mouseY >= handleY && mouseY <= handleY + HANDLE_SIZE;
        
        int handleV = handleHovered ? 40 : 32; // 32 pixel di offset per l'handle, +8 per hover
        guiGraphics.blit(SCROLLBAR_TEXTURE, x + SCROLLBAR_X, handleY, 
                        0, handleV, SCROLLBAR_WIDTH, HANDLE_SIZE, SCROLLBAR_WIDTH, 48);
    }
    
    /**
     * Renderizza l'area informazioni a destra
     */
    private void renderInfoArea(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Per ora vuota, in futuro qui si potrebbero mostrare informazioni aggiuntive
    }
    
    /**
     * Aggiorna e renderizza il feedback
     */
    private void updateAndRenderFeedback(GuiGraphics guiGraphics, int x, int y) {
        if (!feedbackMessage.isEmpty()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - feedbackClearTime > FEEDBACK_DISPLAY_TIME) {
                feedbackMessage = "";
                return;
            }
            
            // Renderizza il messaggio di feedback
            int textWidth = this.font.width(feedbackMessage);
            int textX = x + (GUI_WIDTH - textWidth) / 2;
            int textY = y + FEEDBACK_Y_OFFSET;
            
            // Sfondo semi-trasparente
            guiGraphics.fill(textX - 2, textY - 2, textX + textWidth + 2, textY + 10, 0x80000000);
            
            // Testo
            guiGraphics.drawString(this.font, feedbackMessage, textX, textY, 0xFFFFFF, false);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            int x = (this.width - this.imageWidth) / 2;
            int y = (this.height - this.imageHeight) / 2;
            
            // Controlla click sui pulsanti scrollbar
            if (totalShopEntries > ENTRIES) {
                // Pulsante SU
                if (mouseX >= x + SCROLLBAR_X && mouseX <= x + SCROLLBAR_X + SCROLLBAR_WIDTH &&
                    mouseY >= y + BUTTON_UP_Y && mouseY <= y + BUTTON_UP_Y + HANDLE_SIZE) {
                    scrollUp();
                    return true;
                }
                
                // Pulsante GIÙ
                if (mouseX >= x + SCROLLBAR_X && mouseX <= x + SCROLLBAR_X + SCROLLBAR_WIDTH &&
                    mouseY >= y + BUTTON_DOWN_Y && mouseY <= y + BUTTON_DOWN_Y + HANDLE_SIZE) {
                    scrollDown();
                    return true;
                }
                
                // Handle della scrollbar
                float scrollRatio = (float) scrollOffset / (totalShopEntries - ENTRIES);
                int handleY = y + SCROLLBAR_Y + (int)(scrollRatio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
                
                if (mouseX >= x + SCROLLBAR_X && mouseX <= x + SCROLLBAR_X + SCROLLBAR_WIDTH &&
                    mouseY >= handleY && mouseY <= handleY + HANDLE_SIZE) {
                    isDraggingHandle = true;
                    dragStartY = (int) mouseY;
                    dragStartScrollOffset = scrollOffset;
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDraggingHandle = false;
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
        currentCategoryName = Component.translatable("gui.iska_utils.auto_shop.title").getString();
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
        
        // Ottieni l'ItemStack per l'item
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
    private void handleEntryClick(int entryIndex) {
        int actualIndex = scrollOffset + entryIndex;
        
        if (showingCategories) {
            // Click su categoria
            if (actualIndex < availableCategories.size()) {
                ShopCategory category = availableCategories.get(actualIndex);
                navigateToCategory(category);
            }
        } else {
            // Click su item - per ora non fa nulla, i pulsanti Buy/Sell gestiscono le transazioni
        }
    }
    
    /**
     * Naviga verso una categoria
     */
    private void navigateToCategory(ShopCategory category) {
        currentCategoryId = category.id;
        currentCategoryName = category.name;
        showingCategories = false;
        
        // Carica gli item della categoria (fix: filtra manualmente)
        Map<String, ShopEntry> allEntries = ShopLoader.getEntries();
        availableItems = allEntries.values().stream()
            .filter(entry -> category.id.equals(entry.inCategory))
            .sorted(Comparator.comparing(entry -> entry.item))
            .collect(Collectors.toList());
        totalShopEntries = availableItems.size();
        scrollOffset = 0;
        
        // Aggiorna i pulsanti
        updateBackButtonState();
        updateBuySellButtons();
    }
    
    /**
     * Torna alle categorie
     */
    private void navigateBackToCategories() {
        showingCategories = true;
        currentCategoryId = null;
        currentCategoryName = Component.translatable("gui.iska_utils.auto_shop.title").getString();
        totalShopEntries = availableCategories.size();
        scrollOffset = 0;
        
        // Aggiorna i pulsanti
        updateBackButtonState();
        updateBuySellButtons();
    }
    
    /**
     * Aggiorna lo stato del pulsante Back
     */
    private void updateBackButtonState() {
        if (backButton != null) {
            backButton.visible = !showingCategories;
        }
    }
    
    /**
     * Aggiorna i pulsanti Buy/Sell
     */
    private void updateBuySellButtons() {
        // Rimuovi i pulsanti esistenti
        for (Button button : buyButtons) {
            this.removeWidget(button);
        }
        for (Button button : sellButtons) {
            this.removeWidget(button);
        }
        buyButtons.clear();
        sellButtons.clear();
        
        if (showingCategories) {
            return; // Non mostrare pulsanti nelle categorie
        }
        
        int visibleEntries = Math.min(ENTRIES, availableItems.size() - scrollOffset);
        
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
     * Calcola il moltiplicatore basato sui tasti premuti
     */
    private int calculateMultiplier() {
        int multiplier = 1;
        if (Screen.hasShiftDown()) {
            multiplier = 10;
        }
        if (Screen.hasControlDown()) {
            multiplier = 64;
        }
        return multiplier;
    }
    
    /**
     * Gestisce il click sul pulsante Buy
     */
    private void handleBuyButtonClick(ShopEntry item, int multiplier) {
        // TODO: Implementare logica di acquisto automatico
        showFeedback("Acquisto automatico non ancora implementato");
    }
    
    /**
     * Gestisce il click sul pulsante Sell
     */
    private void handleSellButtonClick(ShopEntry item, int multiplier) {
        // TODO: Implementare logica di vendita automatica
        showFeedback("Vendita automatica non ancora implementata");
    }
    
    /**
     * Mostra un messaggio di feedback
     */
    private void showFeedback(String message) {
        feedbackMessage = message;
        feedbackClearTime = System.currentTimeMillis();
    }
    
    /**
     * Controlla se un item è bloccato (non disponibile)
     */
    private boolean isItemBlocked(ShopEntry item) {
        // TODO: Implementare logica di controllo stage
        return false;
    }
    
    /**
     * Crea il tooltip per item bloccati
     */
    private List<Component> createMissingStagesTooltip(ShopEntry item) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal("Item bloccato"));
        tooltip.add(Component.literal("Completa gli stage richiesti"));
        return tooltip;
    }
    
    /**
     * Renderizza testo scalato
     */
    private void renderScaledText(GuiGraphics guiGraphics, String text, int x, int y, int maxWidth, int color) {
        String displayText = text;
        int textWidth = this.font.width(displayText);
        
        if (textWidth > maxWidth) {
            // Tronca il testo e aggiungi "..."
            while (textWidth > maxWidth && displayText.length() > 3) {
                displayText = displayText.substring(0, displayText.length() - 1);
                textWidth = this.font.width(displayText + "...");
            }
            displayText += "...";
        }
        
        guiGraphics.drawString(this.font, displayText, x, y, color, false);
    }
    
    /**
     * Ottiene l'index dell'entry sotto il mouse
     */
    private int getEntryUnderMouse(int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        for (int i = 0; i < ENTRIES; i++) {
            int entryY = y + ENTRY_START_Y + i * ENTRY_HEIGHT;
            if (mouseX >= x + ENTRY_START_X && mouseX <= x + ENTRY_START_X + ENTRY_WIDTH &&
                mouseY >= entryY && mouseY <= entryY + ENTRY_HEIGHT) {
                return i;
            }
        }
        return -1;
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
        if (showingCategories) {
            return;
        }
        
        int visibleEntries = Math.min(ENTRIES, availableItems.size() - scrollOffset);
        
        for (int i = 0; i < visibleEntries; i++) {
            int entryIndex = scrollOffset + i;
            if (entryIndex >= availableItems.size()) {
                break;
            }
            
            ShopEntry item = availableItems.get(entryIndex);
            int entryY = this.topPos + ENTRY_START_Y + i * ENTRY_HEIGHT;
            
            // Tooltip per pulsante Buy
            if (item.buy > 0) {
                int buyButtonX = this.leftPos + ENTRY_START_X + ENTRY_WIDTH - BUTTON_WIDTH - BUTTONS_SPACING - BUTTON_WIDTH - 3;
                int buttonY = entryY + (ENTRY_HEIGHT - BUTTON_HEIGHT) / 2;
                
                if (mouseX >= buyButtonX && mouseX <= buyButtonX + BUTTON_WIDTH &&
                    mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT) {
                    
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(Component.literal("Acquista " + item.itemCount + "x " + getItemDisplayName(item.item)));
                    
                    // Mostra prezzo
                    ShopValute valute = availableValutes.get(item.valute);
                    if (valute != null) {
                        tooltip.add(Component.literal("Prezzo: " + item.buy + " " + valute.name));
                    }
                    
                    tooltip.add(Component.literal("Shift: x10, Ctrl: x64"));
                    
                    guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                    return;
                }
            }
            
            // Tooltip per pulsante Sell
            if (item.sell > 0) {
                int sellButtonX = this.leftPos + ENTRY_START_X + ENTRY_WIDTH - BUTTON_WIDTH - 3;
                int buttonY = entryY + (ENTRY_HEIGHT - BUTTON_HEIGHT) / 2;
                
                if (mouseX >= sellButtonX && mouseX <= sellButtonX + BUTTON_WIDTH &&
                    mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT) {
                    
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(Component.literal("Vendi " + item.itemCount + "x " + getItemDisplayName(item.item)));
                    
                    // Mostra prezzo
                    ShopValute valute = availableValutes.get(item.valute);
                    if (valute != null) {
                        tooltip.add(Component.literal("Prezzo: " + item.sell + " " + valute.name));
                    }
                    
                    tooltip.add(Component.literal("Shift: x10, Ctrl: x64"));
                    
                    guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                    return;
                }
            }
        }
    }
    
    /**
     * Callback per aggiornare i dati del team (chiamato dal packet handler)
     */
    public static void updateTeamData(Map<String, Integer> teamData) {
        if (currentInstance != null) {
            // TODO: Implementare logica per aggiornare i dati del team
        }
    }
} 