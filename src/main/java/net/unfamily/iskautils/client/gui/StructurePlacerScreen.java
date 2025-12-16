package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.structure.StructureLoader;
import net.minecraft.client.gui.components.Button;

/**
 * Screen per la GUI del Structure Placer
 */
public class StructurePlacerScreen extends AbstractContainerScreen<StructurePlacerMenu> {
    
    // Background texture
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/backgrounds/structure_selector.png");
    private static final ResourceLocation ENTRY_TEXTURE = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/entry_wide.png");
    private static final ResourceLocation SCROLLBAR_TEXTURE = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/scrollbar.png");
    private static final ResourceLocation TINY_BUTTONS_TEXTURE = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/tiny_buttons.png");
    private static final ResourceLocation SINGLE_SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/single_slot.png");
    
    // Dimensioni della texture (basate sul file PNG reale: 200x164)
    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 164;
    
    // Dimensioni degli elementi UI
    private static final int ENTRY_WIDTH = 140; // entry_wide.png è 140x24
    private static final int ENTRY_HEIGHT = 24;
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int HANDLE_SIZE = 8;
    
    // Posizioni degli elementi centrali
    private static final int ENTRIES_START_X = (GUI_WIDTH - ENTRY_WIDTH) / 2; // Centra le entry (ora 30)
    private static final int ENTRIES_START_Y = 30; // Inizio delle entry sotto il titolo
    private static final int ENTRY_SPACING = 0; // Nessuno spazio tra le entry
    
    // Costanti per i pulsanti di selezione (8x8 pixel)
    private static final int BUTTON_SIZE = 8;
    private static final int BUTTON_EMPTY_U = 8; // Seconda colonna (pulsante vuoto)
    private static final int BUTTON_FILLED_U = 16; // Terza colonna (pulsante pieno)
    private static final int BUTTON_NORMAL_V = 0; // Prima riga (normale)
    private static final int BUTTON_HOVERED_V = 8; // Seconda riga (illuminato)
    
    // Costanti per lo slot dell'icona (18x18 pixel)
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_ITEM_SIZE = 16; // Item dentro lo slot
    
    // Posizioni per Save/Cancel sotto la terza entry
    private static final int SAVE_CANCEL_Y = ENTRIES_START_Y + (3 * ENTRY_HEIGHT) + 8; // 8 pixel sotto l'ultima entry (3 entry fisse)
    private static final int SAVE_BUTTON_X = ENTRIES_START_X + 20; // A sinistra
    private static final int CANCEL_BUTTON_X = ENTRIES_START_X + ENTRY_WIDTH - 60; // A destra, più spazio per pulsante più largo
    private static final int SAVE_CANCEL_BUTTON_WIDTH = 40; // Pulsanti più larghi
    private static final int SAVE_CANCEL_BUTTON_HEIGHT = 20; // Altezza standard
    
    // Posizioni scrollbar (accanto alla prima entry) 
    private static final int SCROLLBAR_X = ENTRIES_START_X + ENTRY_WIDTH + 4; // 4 pixel di margine (ora 174)
    private static final int BUTTON_UP_Y = ENTRIES_START_Y; // Pulsante SU all'inizio
    private static final int SCROLLBAR_Y = ENTRIES_START_Y + HANDLE_SIZE; // Scrollbar sotto il pulsante SU
    private static final int SCROLLBAR_HEIGHT = 34; // Altezza completa della texture scrollbar.png
    private static final int BUTTON_DOWN_Y = SCROLLBAR_Y + SCROLLBAR_HEIGHT; // Pulsante GIÙ subito dopo
    
    // Variabili per lo scrolling e strutture
    private int scrollOffset = 0;
    private final int visibleEntries = 3;
    private java.util.List<net.unfamily.iskautils.structure.StructureDefinition> availableStructures;
    private int totalEntries;
    
    // Variabili per il drag dell'handle
    private boolean isDraggingHandle = false;
    private int dragStartY = 0;
    private int dragStartScrollOffset = 0;
    
    // Variabile per la selezione
    private int selectedStructureIndex = -1; // -1 significa nessuna selezione
    
    // Pulsanti vanilla
    private Button saveButton;
    private Button cancelButton;
    private Button closeButton;
    
    // Close button position - top right
    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5; // 5px from right edge
    
    public StructurePlacerScreen(StructurePlacerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        
        // Imposta le dimensioni della GUI
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        
        // Carica le strutture disponibili
        loadAvailableStructures();
    }
    
    /**
     * Carica le strutture disponibili dal StructureLoader (già ordinate e filtrate per machine GUI)
     */
    private void loadAvailableStructures() {
        // Use getMachineVisibleStructures to filter out machine=false structures
        var structureMap = StructureLoader.getMachineVisibleStructures();
        this.availableStructures = new java.util.ArrayList<>(structureMap.values());
        
        this.totalEntries = availableStructures.size();
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Ricarica le strutture all'apertura della GUI usando il flag del config
        net.unfamily.iskautils.structure.StructureLoader.reloadAllDefinitions(net.unfamily.iskautils.Config.acceptClientStructure);
        
        // Crea i pulsanti vanilla Save e Cancel
        int saveX = this.leftPos + SAVE_BUTTON_X;
        int cancelX = this.leftPos + CANCEL_BUTTON_X;
        int buttonY = this.topPos + SAVE_CANCEL_Y;
        
        this.saveButton = Button.builder(Component.translatable("gui.iska_utils.structure_placer.apply"), button -> handleSaveClick())
            .bounds(saveX, buttonY, SAVE_CANCEL_BUTTON_WIDTH, SAVE_CANCEL_BUTTON_HEIGHT)
            .build();
        
        this.cancelButton = Button.builder(Component.translatable("gui.iska_utils.structure_placer.cancel"), button -> handleCancelClick())
            .bounds(cancelX, buttonY, SAVE_CANCEL_BUTTON_WIDTH, SAVE_CANCEL_BUTTON_HEIGHT)
            .build();
        
        this.addRenderableWidget(saveButton);
        this.addRenderableWidget(cancelButton);
        
        // Centra la GUI sullo schermo
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        
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
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Disegna il background
        guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);
        
        // Disegna le 3 entry centrali
        renderEntries(guiGraphics, mouseX, mouseY);
        
        // Disegna la scrollbar
        renderScrollbar(guiGraphics, mouseX, mouseY);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Renderizza il background della GUI
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Renderizza gli item tooltips
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Titolo della GUI (centrato) - ora usa la traduzione
        Component titleComponent = Component.translatable("gui.iska_utils.structure_placer.title");
        String title = titleComponent.getString();
        int titleX = (this.imageWidth - this.font.width(title)) / 2;
        guiGraphics.drawString(this.font, title, titleX, 8, 0x404040, false);
    }
    
    /**
     * Renderizza le 3 entry centrali delle strutture
     */
    private void renderEntries(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (int i = 0; i < visibleEntries; i++) {
            int entryIndex = scrollOffset + i;
            
            int entryX = this.leftPos + ENTRIES_START_X;
            int entryY = this.topPos + ENTRIES_START_Y + i * (ENTRY_HEIGHT + ENTRY_SPACING);
            
            // Disegna l'entry usando l'intera texture entry.png (140x24)
            guiGraphics.blit(ENTRY_TEXTURE, entryX, entryY, 0, 0, 
                           ENTRY_WIDTH, ENTRY_HEIGHT, ENTRY_WIDTH, ENTRY_HEIGHT);
            
            // Se l'entry ha una struttura, mostra nome, ID e pulsante
            if (entryIndex < availableStructures.size()) {
                net.unfamily.iskautils.structure.StructureDefinition structure = availableStructures.get(entryIndex);
                
                // Testo più piccolo: scala a 0.7
                float textScale = 0.7f;
                guiGraphics.pose().pushPose();
                guiGraphics.pose().scale(textScale, textScale, 1.0f);
                
                // Calcola posizioni con scaling - SPAZIO PIÙ AMPIO PER IL TESTO
                int scaledTextX = (int)((entryX + 4) / textScale);
                int scaledNameY = (int)((entryY + 2) / textScale);
                
                // Testo superiore: Nome della struttura - LIMITI PIÙ GENEROSI
                String structureName = structure.getName() != null ? structure.getName() : structure.getId();
                if (structureName.length() > 35) { // Incrementato da 25 a 35 caratteri
                    structureName = structureName.substring(0, 32) + "...";
                }
                guiGraphics.drawString(this.font, structureName, scaledTextX, scaledNameY, 0x404040, false);
                
                // Testo inferiore: ID della struttura - LIMITI PIÙ GENEROSI
                String structureId = structure.getId();
                if (structureId.length() > 38) { // Incrementato da 28 a 38 caratteri
                    structureId = structureId.substring(0, 35) + "...";
                }
                int scaledIdY = (int)((entryY + ENTRY_HEIGHT - (this.font.lineHeight * textScale) - 2) / textScale);
                guiGraphics.drawString(this.font, structureId, scaledTextX, scaledIdY, 0x666666, false);
                
                guiGraphics.pose().popPose();
                
                // Pulsante di selezione nel fondo a destra dell'entry
                renderSelectionButton(guiGraphics, entryX, entryY, entryIndex, mouseX, mouseY);
            }
            // Se non c'è struttura, l'entry rimane vuota (solo la texture)
        }
    }
    
    /**
     * Renderizza la scrollbar con handle e pulsanti
     */
    private void renderScrollbar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int scrollbarX = this.leftPos + SCROLLBAR_X;
        int scrollbarY = this.topPos + SCROLLBAR_Y;
        int buttonUpY = this.topPos + BUTTON_UP_Y;
        int buttonDownY = this.topPos + BUTTON_DOWN_Y;
        
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
        if (availableStructures.size() > visibleEntries) {
            scrollRatio = (float) scrollOffset / (availableStructures.size() - visibleEntries);
        }
        int handleY = scrollbarY + (int)(scrollRatio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
        
        boolean handleHovered = mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE &&
                               mouseY >= handleY && mouseY < handleY + HANDLE_SIZE;
        int handleTextureY = handleHovered ? HANDLE_SIZE : 0;
        guiGraphics.blit(SCROLLBAR_TEXTURE, scrollbarX, handleY, 
                        SCROLLBAR_WIDTH, handleTextureY, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
    }
    
    /**
     * Renderizza lo slot dell'icona e il pulsante di selezione per un'entry
     */
    private void renderSelectionButton(GuiGraphics guiGraphics, int entryX, int entryY, int entryIndex, int mouseX, int mouseY) {
        // Posizione dello slot: più a sinistra, centrato verticalmente
        int slotX = entryX + ENTRY_WIDTH - SLOT_SIZE - BUTTON_SIZE - 6; // 6 pixel di margine totale
        int slotY = entryY + (ENTRY_HEIGHT - SLOT_SIZE) / 2; // Centrato verticalmente
        
        // Disegna lo slot (18x18)
        guiGraphics.blit(SINGLE_SLOT_TEXTURE, slotX, slotY, 0, 0, 
                        SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
        
        // Disegna l'icona della struttura nello slot se disponibile
        if (entryIndex < availableStructures.size()) {
            net.unfamily.iskautils.structure.StructureDefinition structure = availableStructures.get(entryIndex);
            renderStructureIcon(guiGraphics, structure, slotX + 1, slotY + 1); // +1 pixel per centrare nell'slot
        }
        
        // Posizione del pulsante: subito dopo lo slot, centrato verticalmente
        int buttonX = slotX + SLOT_SIZE + 2; // 2 pixel di spazio dopo lo slot
        int buttonY = entryY + (ENTRY_HEIGHT - BUTTON_SIZE) / 2; // Centrato verticalmente
        
        // Verifica se il mouse è sopra il pulsante
        boolean isHovered = mouseX >= buttonX && mouseX < buttonX + BUTTON_SIZE &&
                           mouseY >= buttonY && mouseY < buttonY + BUTTON_SIZE;
        
        // Determina il tipo di pulsante (vuoto o pieno)
        boolean isSelected = (entryIndex == selectedStructureIndex);
        int buttonU = isSelected ? BUTTON_FILLED_U : BUTTON_EMPTY_U;
        int buttonV = isHovered ? BUTTON_HOVERED_V : BUTTON_NORMAL_V;
        
        // Disegna il pulsante
        guiGraphics.blit(TINY_BUTTONS_TEXTURE, buttonX, buttonY, buttonU, buttonV, 
                        BUTTON_SIZE, BUTTON_SIZE, 64, 96);
    }
    
    /**
     * Renderizza l'icona di una struttura nello slot
     */
    private void renderStructureIcon(GuiGraphics guiGraphics, net.unfamily.iskautils.structure.StructureDefinition structure, int x, int y) {
        if (structure.getIcon() != null && structure.getIcon().getItem() != null) {
            // Cerca di ottenere l'item dall'ID specificato nello script
            try {
                net.minecraft.resources.ResourceLocation itemId = net.minecraft.resources.ResourceLocation.parse(structure.getIcon().getItem());
                net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId);
                
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    net.minecraft.world.item.ItemStack itemStack = new net.minecraft.world.item.ItemStack(item);
                    guiGraphics.renderItem(itemStack, x, y);
                    return;
                }
            } catch (Exception e) {
                // Se fallisce, usa l'item di default
            }
        }
        
        // Item di default se non specificato o non trovato: blocco di pietra
        net.minecraft.world.item.ItemStack defaultItem = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE);
        guiGraphics.renderItem(defaultItem, x, y);
    }
    
    /**
     * Gestisce i click sui pulsanti Save e Cancel
     */
    private void handleSaveClick() {
        // DEBUG: Stampa informazioni di debug
        System.out.println("=== SAVE CLICK DEBUG ===");
        System.out.println("selectedStructureIndex: " + selectedStructureIndex);
        System.out.println("availableStructures.size(): " + availableStructures.size());
        System.out.println("scrollOffset: " + scrollOffset);
        
        if (selectedStructureIndex >= 0 && selectedStructureIndex < availableStructures.size()) {
            net.unfamily.iskautils.structure.StructureDefinition structure = availableStructures.get(selectedStructureIndex);
            
            // DEBUG: Stampa informazioni sulla struttura selezionata
            System.out.println("Selected structure ID: " + structure.getId());
            System.out.println("Selected structure Name: " + structure.getName());
            
            // Invia il packet al server per salvare la struttura
            net.unfamily.iskautils.network.ModMessages.sendStructurePlacerSavePacket(structure.getId());
            
            playButtonSound();
            this.minecraft.setScreen(null); // Chiudi la GUI
        } else {
            // Nessuna struttura selezionata
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§cNo structure selected!"), 
                    true);
            }
        }
    }
    
    /**
     * Gestisce il click sul pulsante Cancel
     */
    private void handleCancelClick() {
        playButtonSound();
        if (this.minecraft != null) {
            this.minecraft.setScreen(null); // Chiudi la GUI senza salvare
        }
    }
    
    /**
     * Riproduce il suono dei pulsanti vanilla
     */
    private void playButtonSound() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Click sinistro
            // Verifica click sui pulsanti Save/Cancel
            if (handleSaveCancelClick(mouseX, mouseY)) {
                return true;
            }
            
            // Verifica click sui pulsanti di selezione
            if (handleSelectionButtonClick(mouseX, mouseY)) {
                return true;
            }
            
            // Verifica click sui pulsanti di scroll
            if (handleScrollButtonClick(mouseX, mouseY)) {
                return true;
            }
            
            // Verifica click sull'handle per il drag
            if (handleHandleClick(mouseX, mouseY)) {
                return true;
            }
            
            // Verifica click sulla scrollbar per il salto
            if (handleScrollbarClick(mouseX, mouseY)) {
                return true;
            }
            
            // Verifica click sulle entry
            if (handleEntryClick(mouseX, mouseY)) {
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    /**
     * Gestisce i click sui pulsanti di scroll
     */
    private boolean handleScrollButtonClick(double mouseX, double mouseY) {
        int scrollbarX = this.leftPos + SCROLLBAR_X;
        int buttonUpY = this.topPos + BUTTON_UP_Y;
        int buttonDownY = this.topPos + BUTTON_DOWN_Y;
        
        // Pulsante SU (sopra la scrollbar)
        if (mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE &&
            mouseY >= buttonUpY && mouseY < buttonUpY + HANDLE_SIZE) {
            scrollUp();
            return true;
        }
        
        // Pulsante GIÙ (sotto la scrollbar)
        if (mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE &&
            mouseY >= buttonDownY && mouseY < buttonDownY + HANDLE_SIZE) {
            scrollDown();
            return true;
        }
        
        return false;
    }
    
    /**
     * Gestisce i click sui pulsanti di selezione
     */
    private boolean handleSelectionButtonClick(double mouseX, double mouseY) {
        for (int i = 0; i < visibleEntries; i++) {
            int entryIndex = scrollOffset + i;
            if (entryIndex >= availableStructures.size()) continue;
            
            int entryX = this.leftPos + ENTRIES_START_X;
            int entryY = this.topPos + ENTRIES_START_Y + i * (ENTRY_HEIGHT + ENTRY_SPACING);
            
            // Posizione del pulsante (deve corrispondere a renderSelectionButton)
            int slotX = entryX + ENTRY_WIDTH - SLOT_SIZE - BUTTON_SIZE - 6;
            int buttonX = slotX + SLOT_SIZE + 2;
            int buttonY = entryY + (ENTRY_HEIGHT - BUTTON_SIZE) / 2;
            
            if (mouseX >= buttonX && mouseX < buttonX + BUTTON_SIZE &&
                mouseY >= buttonY && mouseY < buttonY + BUTTON_SIZE) {
                
                // DEBUG: Stampa informazioni di selezione
                System.out.println("=== SELECTION BUTTON CLICK DEBUG ===");
                System.out.println("Clicked entryIndex: " + entryIndex);
                System.out.println("Current selectedStructureIndex: " + selectedStructureIndex);
                System.out.println("scrollOffset: " + scrollOffset);
                System.out.println("Entry visual position i: " + i);
                
                // Seleziona/deseleziona la struttura
                if (selectedStructureIndex == entryIndex) {
                    selectedStructureIndex = -1; // Deseleziona
                    System.out.println("Deselected -> selectedStructureIndex: " + selectedStructureIndex);
                } else {
                    selectedStructureIndex = entryIndex; // Seleziona questa
                    System.out.println("Selected -> selectedStructureIndex: " + selectedStructureIndex);
                    
                    // DEBUG: Stampa info sulla struttura selezionata
                    if (entryIndex < availableStructures.size()) {
                        var structure = availableStructures.get(entryIndex);
                        System.out.println("Selected structure ID: " + structure.getId());
                        System.out.println("Selected structure Name: " + structure.getName());
                    }
                }
                playButtonSound();
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gestisce i click sull'handle per iniziare il drag
     */
    private boolean handleHandleClick(double mouseX, double mouseY) {
        if (availableStructures.size() <= visibleEntries) return false; // Non draggabile se poche strutture
        
        int scrollbarX = this.leftPos + SCROLLBAR_X;
        int scrollbarY = this.topPos + SCROLLBAR_Y;
        
        float scrollRatio = (float) scrollOffset / (availableStructures.size() - visibleEntries);
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
    
    /**
     * Gestisce i click sulla scrollbar per saltare a una posizione
     */
    private boolean handleScrollbarClick(double mouseX, double mouseY) {
        if (availableStructures.size() <= visibleEntries) return false;
        
        int scrollbarX = this.leftPos + SCROLLBAR_X;
        int scrollbarY = this.topPos + SCROLLBAR_Y;
        
        if (mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_WIDTH &&
            mouseY >= scrollbarY && mouseY < scrollbarY + SCROLLBAR_HEIGHT) {
            
            // Calcola la nuova posizione del scroll in base al click
            float clickRatio = (float)(mouseY - scrollbarY) / SCROLLBAR_HEIGHT;
            clickRatio = Math.max(0, Math.min(1, clickRatio)); // Clamp tra 0 e 1
            
            int newScrollOffset = (int)(clickRatio * (availableStructures.size() - visibleEntries));
            newScrollOffset = Math.max(0, Math.min(availableStructures.size() - visibleEntries, newScrollOffset));
            
            if (newScrollOffset != scrollOffset) {
                scrollOffset = newScrollOffset;
                playButtonSound();
            }
            return true;
        }
        return false;
    }
    
    /**
     * Gestisce i click sui pulsanti Save e Cancel
     */
    private boolean handleSaveCancelClick(double mouseX, double mouseY) {
        int saveX = this.leftPos + SAVE_BUTTON_X;
        int cancelX = this.leftPos + CANCEL_BUTTON_X;
        
        int buttonWidth = SAVE_CANCEL_BUTTON_WIDTH;
        int buttonHeight = SAVE_CANCEL_BUTTON_HEIGHT;
        
        // Click su Save
        if (mouseX >= saveX && mouseX < saveX + buttonWidth &&
            mouseY >= this.topPos + SAVE_CANCEL_Y && mouseY < this.topPos + SAVE_CANCEL_Y + buttonHeight) {
            handleSaveClick();
            return true;
        }
        
        // Click su Cancel
        if (mouseX >= cancelX && mouseX < cancelX + buttonWidth &&
            mouseY >= this.topPos + SAVE_CANCEL_Y && mouseY < this.topPos + SAVE_CANCEL_Y + buttonHeight) {
            handleCancelClick();
            return true;
        }
        
        return false;
    }
    
    /**
     * Gestisce i click sulle entry delle strutture - RIMOSSO: Non più selezione diretta
     */
    private boolean handleEntryClick(double mouseX, double mouseY) {
        // Le entry non sono più cliccabili per selezione
        // Solo i pulsanti di selezione funzionano
        return false;
    }
    
    /**
     * Scrolla verso l'alto (con suono per i click)
     */
    private void scrollUp() {
        if (scrollUpSilent()) {
            playButtonSound();
        }
    }
    
    /**
     * Scrolla verso il basso (con suono per i click)
     */
    private void scrollDown() {
        if (scrollDownSilent()) {
            playButtonSound();
        }
    }
    
    /**
     * Scrolla verso l'alto senza suono (per rotella mouse)
     */
    private boolean scrollUpSilent() {
        if (availableStructures.size() > visibleEntries && scrollOffset > 0) {
            scrollOffset--;
            return true;
        }
        return false;
    }
    
    /**
     * Scrolla verso il basso senza suono (per rotella mouse)
     */
    private boolean scrollDownSilent() {
        if (availableStructures.size() > visibleEntries && scrollOffset < availableStructures.size() - visibleEntries) {
            scrollOffset++;
            return true;
        }
        return false;
    }
    

    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Gestisce lo scroll del mouse SENZA SUONO
        if (scrollY > 0) {
            return scrollUpSilent();
        } else if (scrollY < 0) {
            return scrollDownSilent();
        }
        
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
        if (button == 0 && isDraggingHandle && availableStructures.size() > visibleEntries) {
            // Calcola il nuovo scroll offset basato sul movimento del mouse
            int deltaY = (int) mouseY - dragStartY;
            float scrollRatio = (float) deltaY / (SCROLLBAR_HEIGHT - HANDLE_SIZE);
            
            int newScrollOffset = dragStartScrollOffset + (int)(scrollRatio * (availableStructures.size() - visibleEntries));
            newScrollOffset = Math.max(0, Math.min(availableStructures.size() - visibleEntries, newScrollOffset));
            
            if (newScrollOffset != scrollOffset) {
                scrollOffset = newScrollOffset;
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
} 