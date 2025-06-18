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
import net.unfamily.iskautils.block.entity.StructurePlacerMachineBlockEntity;
import net.minecraft.client.gui.components.Button;

/**
 * Screen per la GUI di selezione strutture della Structure Placer Machine (basata sulla StructurePlacerScreen dell'item)
 */
public class StructureSelectionScreen extends AbstractContainerScreen<StructurePlacerMachineMenu> {
    
    // Background texture
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/structure_selector.png");
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
    
    // Reference alla macchina
    private final StructurePlacerMachineBlockEntity blockEntity;
    
    public StructureSelectionScreen(StructurePlacerMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        
        // Imposta le dimensioni della GUI
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        
        // Ottieni il block entity dalla menu
        this.blockEntity = menu.getBlockEntity();
        
        // Carica le strutture disponibili
        loadAvailableStructures();
    }
    
    /**
     * Carica le strutture disponibili dal StructureLoader (già ordinate)
     */
    private void loadAvailableStructures() {
        var structureMap = StructureLoader.getAllStructures();
        this.availableStructures = new java.util.ArrayList<>(structureMap.values());
        
        this.totalEntries = availableStructures.size();
        
        // Trova la struttura attualmente selezionata
        if (blockEntity != null && !blockEntity.getSelectedStructure().isEmpty()) {
            String currentSelection = blockEntity.getSelectedStructure();
            for (int i = 0; i < availableStructures.size(); i++) {
                if (availableStructures.get(i).getId().equals(currentSelection)) {
                    selectedStructureIndex = i;
                    // Scrolla per mostrare la selezione corrente
                    if (selectedStructureIndex >= visibleEntries) {
                        scrollOffset = Math.max(0, selectedStructureIndex - visibleEntries + 1);
                    }
                    break;
                }
            }
        }
    }
    
    @Override
    protected void init() {
        super.init();
        
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
        
        // Renderizza gli item tooltips (se necessario)
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Titolo della GUI (centrato) - usa la traduzione per selezione strutture
        Component titleComponent = Component.translatable("gui.iska_utils.structure_selection.title");
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
                
                // Renderizza il pulsante di selezione
                renderSelectionButton(guiGraphics, entryX, entryY, entryIndex, mouseX, mouseY);
                
                // Renderizza l'icona della struttura
                renderStructureIcon(guiGraphics, structure, entryX + ENTRY_WIDTH - SLOT_SIZE - 2, entryY + 3);
            }
        }
    }
    
    /**
     * Renderizza la scrollbar
     */
    private void renderScrollbar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (totalEntries <= visibleEntries) return; // Non mostrare scrollbar se non serve
        
        int scrollbarX = this.leftPos + SCROLLBAR_X;
        
        // Pulsante SU (freccia su) - prima riga di tiny_buttons.png (0,0)
        int upButtonY = this.topPos + BUTTON_UP_Y;
        boolean upHovered = mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_WIDTH && 
                           mouseY >= upButtonY && mouseY < upButtonY + HANDLE_SIZE;
        int upV = upHovered ? 8 : 0; // Seconda riga se hovered
        guiGraphics.blit(TINY_BUTTONS_TEXTURE, scrollbarX, upButtonY, 0, upV, HANDLE_SIZE, HANDLE_SIZE, 32, 16);
        
        // Scrollbar track (background)
        int trackY = this.topPos + SCROLLBAR_Y;
        guiGraphics.blit(SCROLLBAR_TEXTURE, scrollbarX, trackY, 0, 0, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT);
        
        // Handle (slider)
        if (totalEntries > visibleEntries) {
            int maxScroll = totalEntries - visibleEntries;
            int handleY = trackY + (scrollOffset * (SCROLLBAR_HEIGHT - HANDLE_SIZE)) / maxScroll;
            // Handle è nella seconda parte della texture scrollbar.png (offset X = SCROLLBAR_WIDTH)
            guiGraphics.blit(SCROLLBAR_TEXTURE, scrollbarX, handleY, SCROLLBAR_WIDTH, 0, HANDLE_SIZE, HANDLE_SIZE, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT);
        }
        
        // Pulsante GIÙ (freccia giù) - ultima colonna di tiny_buttons.png (24,0)
        int downButtonY = this.topPos + BUTTON_DOWN_Y;
        boolean downHovered = mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_WIDTH && 
                             mouseY >= downButtonY && mouseY < downButtonY + HANDLE_SIZE;
        int downV = downHovered ? 8 : 0; // Seconda riga se hovered
        guiGraphics.blit(TINY_BUTTONS_TEXTURE, scrollbarX, downButtonY, 24, downV, HANDLE_SIZE, HANDLE_SIZE, 32, 16);
    }
    
    /**
     * Renderizza il pulsante di selezione per ogni entry
     */
    private void renderSelectionButton(GuiGraphics guiGraphics, int entryX, int entryY, int entryIndex, int mouseX, int mouseY) {
        // Posizione del pulsante: a destra dell'entry, prima dello slot dell'icona
        int buttonX = entryX + ENTRY_WIDTH - SLOT_SIZE - BUTTON_SIZE - 4; // 4 pixel di margine
        int buttonY = entryY + (ENTRY_HEIGHT - BUTTON_SIZE) / 2; // Centrato verticalmente
        
        // Determina se è selezionato e se è hovered
        boolean isSelected = (entryIndex == selectedStructureIndex);
        boolean isHovered = mouseX >= buttonX && mouseX < buttonX + BUTTON_SIZE && 
                           mouseY >= buttonY && mouseY < buttonY + BUTTON_SIZE;
        
        // Seleziona texture in base allo stato
        int u = isSelected ? BUTTON_FILLED_U : BUTTON_EMPTY_U; // Colonna: vuoto(8) o pieno(16)
        int v = isHovered ? BUTTON_HOVERED_V : BUTTON_NORMAL_V; // Riga: normale(0) o illuminato(8)
        
        // Renderizza il pulsante dalla texture tiny_buttons.png
        guiGraphics.blit(TINY_BUTTONS_TEXTURE, buttonX, buttonY, u, v, BUTTON_SIZE, BUTTON_SIZE, 32, 16);
    }
    
    /**
     * Renderizza l'icona della struttura (per ora slot vuoto)
     */
    private void renderStructureIcon(GuiGraphics guiGraphics, net.unfamily.iskautils.structure.StructureDefinition structure, int x, int y) {
        // Disegna lo slot usando la texture single_slot.png (18x18)
        guiGraphics.blit(SINGLE_SLOT_TEXTURE, x, y, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
        
        // L'icona della struttura potrebbe essere implementata in futuro
        // usando structure.getIcon() quando il sistema sarà completo
    }
    
    /**
     * Gestisce il click sul pulsante Save/Apply
     */
    private void handleSaveClick() {
        // Salva la struttura selezionata nel block entity
        if (selectedStructureIndex >= 0 && selectedStructureIndex < availableStructures.size() && blockEntity != null) {
            String selectedId = availableStructures.get(selectedStructureIndex).getId();
            blockEntity.setSelectedStructure(selectedId);
            
            // Messaggio di conferma al player
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(
                    Component.literal("Selected structure: " + selectedId)
                );
            }
        }
        
        playButtonSound();
        this.onClose();
    }
    
    /**
     * Gestisce il click sul pulsante Cancel
     */
    private void handleCancelClick() {
        playButtonSound();
        this.onClose();
    }
    
    /**
     * Suona il suono del click del pulsante
     */
    private void playButtonSound() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            // Gestisci i vari click nell'ordine di priorità
            if (handleScrollButtonClick(mouseX, mouseY) ||
                handleSelectionButtonClick(mouseX, mouseY) ||
                handleHandleClick(mouseX, mouseY) ||
                handleScrollbarClick(mouseX, mouseY) ||
                handleEntryClick(mouseX, mouseY)) {
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    /**
     * Gestisce i click sui pulsanti di scroll (frecce su/giù)
     */
    private boolean handleScrollButtonClick(double mouseX, double mouseY) {
        if (totalEntries <= visibleEntries) return false;
        
        int scrollbarX = this.leftPos + SCROLLBAR_X;
        
        // Pulsante SU
        int upButtonY = this.topPos + BUTTON_UP_Y;
        if (mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_WIDTH && 
            mouseY >= upButtonY && mouseY < upButtonY + HANDLE_SIZE) {
            scrollUp();
            return true;
        }
        
        // Pulsante GIÙ
        int downButtonY = this.topPos + BUTTON_DOWN_Y;
        if (mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_WIDTH && 
            mouseY >= downButtonY && mouseY < downButtonY + HANDLE_SIZE) {
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
            
            // Calcola posizione del pulsante
            int buttonX = entryX + ENTRY_WIDTH - SLOT_SIZE - BUTTON_SIZE - 4;
            int buttonY = entryY + (ENTRY_HEIGHT - BUTTON_SIZE) / 2;
            
            // Controlla se il click è sul pulsante
            if (mouseX >= buttonX && mouseX < buttonX + BUTTON_SIZE && 
                mouseY >= buttonY && mouseY < buttonY + BUTTON_SIZE) {
                selectedStructureIndex = entryIndex;
                playButtonSound();
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gestisce il click sull'handle per iniziare il drag
     */
    private boolean handleHandleClick(double mouseX, double mouseY) {
        if (totalEntries <= visibleEntries) return false;
        
        int scrollbarX = this.leftPos + SCROLLBAR_X;
        int trackY = this.topPos + SCROLLBAR_Y;
        
        // Calcola la posizione dell'handle
        int maxScroll = totalEntries - visibleEntries;
        int handleY = trackY + (scrollOffset * (SCROLLBAR_HEIGHT - HANDLE_SIZE)) / maxScroll;
        
        // Controlla se il click è sull'handle
        if (mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE && 
            mouseY >= handleY && mouseY < handleY + HANDLE_SIZE) {
            isDraggingHandle = true;
            dragStartY = (int)mouseY;
            dragStartScrollOffset = scrollOffset;
            return true;
        }
        
        return false;
    }
    
    /**
     * Gestisce i click sulla track della scrollbar
     */
    private boolean handleScrollbarClick(double mouseX, double mouseY) {
        if (totalEntries <= visibleEntries) return false;
        
        int scrollbarX = this.leftPos + SCROLLBAR_X;
        int trackY = this.topPos + SCROLLBAR_Y;
        
        // Controlla se il click è sulla track
        if (mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_WIDTH && 
            mouseY >= trackY && mouseY < trackY + SCROLLBAR_HEIGHT) {
            
            // Calcola dove dovrebbe andare l'handle
            int maxScroll = totalEntries - visibleEntries;
            int handleY = trackY + (scrollOffset * (SCROLLBAR_HEIGHT - HANDLE_SIZE)) / maxScroll;
            
            // Scrolla verso l'alto se si clicca sopra l'handle, giù se sotto
            if (mouseY < handleY) {
                scrollUpSilent();
            } else if (mouseY > handleY + HANDLE_SIZE) {
                scrollDownSilent();
            }
            return true;
        }
        
        return false;
    }
    
    /**
     * Gestisce i click direttamente sulle entry per selezione rapida
     */
    private boolean handleEntryClick(double mouseX, double mouseY) {
        for (int i = 0; i < visibleEntries; i++) {
            int entryIndex = scrollOffset + i;
            if (entryIndex >= availableStructures.size()) continue;
            
            int entryX = this.leftPos + ENTRIES_START_X;
            int entryY = this.topPos + ENTRIES_START_Y + i * (ENTRY_HEIGHT + ENTRY_SPACING);
            
            // Controlla se il click è sull'entry (ma non sui pulsanti/slot)
            if (mouseX >= entryX && mouseX < entryX + ENTRY_WIDTH && 
                mouseY >= entryY && mouseY < entryY + ENTRY_HEIGHT) {
                selectedStructureIndex = entryIndex;
                return true;
            }
        }
        return false;
    }
    
    /**
     * Scrolla su con suono
     */
    private void scrollUp() {
        if (scrollUpSilent()) {
            playButtonSound();
        }
    }
    
    /**
     * Scrolla giù con suono
     */
    private void scrollDown() {
        if (scrollDownSilent()) {
            playButtonSound();
        }
    }
    
    /**
     * Scrolla su senza suono (ritorna true se c'è stato movimento)
     */
    private boolean scrollUpSilent() {
        if (scrollOffset > 0) {
            scrollOffset--;
            return true;
        }
        return false;
    }
    
    /**
     * Scrolla giù senza suono (ritorna true se c'è stato movimento)
     */
    private boolean scrollDownSilent() {
        int maxScroll = Math.max(0, totalEntries - visibleEntries);
        if (scrollOffset < maxScroll) {
            scrollOffset++;
            return true;
        }
        return false;
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Scroll con la rotella del mouse
        if (totalEntries > visibleEntries) {
            if (scrollY > 0) {
                scrollUpSilent();
            } else if (scrollY < 0) {
                scrollDownSilent();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
        // Gestisce il drag dell'handle della scrollbar
        if (isDraggingHandle && totalEntries > visibleEntries) {
            int deltaY = (int)mouseY - dragStartY;
            int maxScroll = totalEntries - visibleEntries;
            
            // Calcola il nuovo offset basato sul movimento
            int newOffset = dragStartScrollOffset + (deltaY * maxScroll) / (SCROLLBAR_HEIGHT - HANDLE_SIZE);
            scrollOffset = Math.max(0, Math.min(maxScroll, newOffset));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
}