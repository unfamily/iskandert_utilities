package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.structure.StructureLoader;

public class StructureSaverMachineScreen extends AbstractContainerScreen<StructureSaverMachineMenu> {
    
    private static final ResourceLocation TEXTURE = 
        ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/backgrounds/block_structure_save.png");
    private static final ResourceLocation ENTRY_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/entry_wide.png");
    private static final ResourceLocation SCROLLBAR_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/scrollbar.png");
    private static final ResourceLocation TINY_BUTTONS_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/tiny_buttons.png");
    private static final ResourceLocation SINGLE_SLOT_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/single_slot.png");
    
    // Entry dimensions
    private static final int ENTRY_WIDTH = 140;
    private static final int ENTRY_HEIGHT = 24;
    private static final int VISIBLE_ENTRIES = 3;
    
    // Scrollbar dimensions (identica a StructureSelectionScreen)
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int SCROLLBAR_HEIGHT = 34;
    private static final int HANDLE_SIZE = 8;
    
    // Button dimensions (primo pulsante del secondo set)
    private static final int BUTTON_SIZE = 8;
    private static final int BUTTON_U = 0; // Prima colonna del secondo set
    private static final int BUTTON_NORMAL_V = 16; // Riga 3 (secondo set normale)
    private static final int BUTTON_HOVERED_V = 24; // Riga 4 (secondo set illuminato)
    
    // Slot dimensions
    private static final int SLOT_SIZE = 18;
    

    
    // Positions (adapted for 176x200 GUI, spostato più in alto senza energia)
    private static final int ENTRIES_START_X = 18;
    private static final int ENTRIES_START_Y = 20; // Spostato 5 pixel più in alto (da 25 a 20)
    
    // Scrollbar positions (aggiornate per la nuova posizione delle entry)
    private static final int SCROLLBAR_X = ENTRIES_START_X + ENTRY_WIDTH + 4;
    private static final int BUTTON_UP_Y = ENTRIES_START_Y;
    private static final int SCROLLBAR_Y = ENTRIES_START_Y + HANDLE_SIZE;
    private static final int BUTTON_DOWN_Y = SCROLLBAR_Y + SCROLLBAR_HEIGHT;
    
    // Scrolling variables
    private int scrollOffset = 0;
    private java.util.List<net.unfamily.iskautils.structure.StructureDefinition> clientStructures;
    private int selectedEntryIndex = -1;
    private boolean isDraggingHandle = false;
    private int dragStartY = 0;
    private int dragStartScrollOffset = 0;
    
    // Nuovi componenti UI
    private EditBox nameEditBox;
    private Button saveButton;
    
    // Posizioni nuovi componenti (spostati ancora più in alto di poco)
    private static final int EDIT_BOX_Y = ENTRIES_START_Y + (VISIBLE_ENTRIES * ENTRY_HEIGHT) + 2; // 2px sotto l'ultima entry (era 5px)
    private static final int BUTTONS_ROW_Y = EDIT_BOX_Y + 22; // 22px sotto l'EditBox (era 25px)
    
    public StructureSaverMachineScreen(StructureSaverMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        
        // Dimensioni reali della texture (176 x 246 dalla nuova texture)
        this.imageWidth = 176;
        this.imageHeight = 246;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Centro il titolo ora che this.font è inizializzato
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        
        // Carica le strutture client (solo quelle client)
        loadClientStructures();
        
        // Inizializza i componenti UI
        initComponents();
    }
    
    /**
     * Non più necessario richiedere sincronizzazione manuale - ContainerData lo fa automaticamente
     */
    private void requestDataFromServer() {
        // ContainerData sincronizza automaticamente i dati dal server
        System.out.println("DEBUG SCREEN: Using ContainerData for automatic sync");
    }
    
    /**
     * Inizializza EditBox, pulsante Save e pulsanti tiny
     */
    private void initComponents() {
        // EditBox per il nome - lungo tutta la entry
        nameEditBox = new EditBox(this.font, this.leftPos + ENTRIES_START_X, this.topPos + EDIT_BOX_Y, 
                                 ENTRY_WIDTH, 20, Component.translatable("gui.iska_utils.structure_name"));
        nameEditBox.setMaxLength(64);
        nameEditBox.setValue("");
        addRenderableWidget(nameEditBox);
        
        // Pulsante Save - allineato con l'inizio entry
        saveButton = Button.builder(Component.translatable("gui.iska_utils.save"), 
                                   button -> onSaveButtonClicked())
                          .bounds(this.leftPos + ENTRIES_START_X, this.topPos + BUTTONS_ROW_Y, 40, 20)
                          .build();
        addRenderableWidget(saveButton);
        
        // Gli slot vengono popolati automaticamente dal server quando l'area viene impostata
    }
    
    /**
     * Popola gli slot con i blocchi dell'area dalla blueprint
     */
    // RIMOSSO: populateAreaBlocks ora viene eseguito dal server nel BlockEntity
    
    // RIMOSSO: scanAndPopulateBlocks - ora gestito dal server nel BlockEntity
    
    /**
     * Carica le strutture client dal StructureLoader
     */
    private void loadClientStructures() {
        var clientStructureMap = StructureLoader.getClientStructures();
        this.clientStructures = new java.util.ArrayList<>(clientStructureMap.values());
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Centra la GUI
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        // Disegna il background specificando esplicitamente le dimensioni della texture
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 176, 246);
        
        // Renderizza i componenti
        renderEntries(guiGraphics, mouseX, mouseY);
        renderScrollbar(guiGraphics, mouseX, mouseY);
        renderNewComponents(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Solo il titolo traducibile
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }
    
    /**
     * Renderizza le 3 entry visibili con strutture client
     */
    private void renderEntries(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (int i = 0; i < VISIBLE_ENTRIES; i++) {
            int entryIndex = scrollOffset + i;
            
            int entryX = this.leftPos + ENTRIES_START_X;
            int entryY = this.topPos + ENTRIES_START_Y + (i * ENTRY_HEIGHT);
            
            // Disegna background entry
            guiGraphics.blit(ENTRY_TEXTURE, entryX, entryY, 0, 0, ENTRY_WIDTH, ENTRY_HEIGHT, 140, 24);
            
            // Se l'entry ha una struttura client, mostra nome, ID, slot e pulsante
            if (entryIndex < clientStructures.size()) {
                net.unfamily.iskautils.structure.StructureDefinition structure = clientStructures.get(entryIndex);
                
                // Testo più piccolo: scala a 0.7
                float textScale = 0.7f;
                guiGraphics.pose().pushPose();
                guiGraphics.pose().scale(textScale, textScale, 1.0f);
                
                // Calcola posizioni con scaling - spazio per slot e pulsante
                int scaledTextX = (int)((entryX + 4) / textScale);
                int scaledNameY = (int)((entryY + 2) / textScale);
                
                // Testo superiore: Nome della struttura
                String structureName = structure.getName() != null ? structure.getName() : structure.getId();
                if (structureName.length() > 20) { // Ridotto per far spazio a slot e pulsante
                    structureName = structureName.substring(0, 17) + "...";
                }
                guiGraphics.drawString(this.font, structureName, scaledTextX, scaledNameY, 0x404040, false);
                
                // Testo inferiore: ID della struttura
                String structureId = structure.getId();
                if (structureId.length() > 23) { // Ridotto per far spazio a slot e pulsante
                    structureId = structureId.substring(0, 20) + "...";
                }
                int scaledIdY = (int)((entryY + ENTRY_HEIGHT - (this.font.lineHeight * textScale) - 2) / textScale);
                guiGraphics.drawString(this.font, structureId, scaledTextX, scaledIdY, 0x666666, false);
                
                guiGraphics.pose().popPose();
                
                // Renderizza slot e pulsante
                renderSlotAndButton(guiGraphics, entryX, entryY, entryIndex, mouseX, mouseY);
            }
        }
    }
    
    /**
     * Renderizza slot e pulsante per una entry (simile a StructureSelectionScreen)
     */
    private void renderSlotAndButton(GuiGraphics guiGraphics, int entryX, int entryY, int entryIndex, int mouseX, int mouseY) {
        // Posizione dello slot: più a sinistra, centrato verticalmente
        int slotX = entryX + ENTRY_WIDTH - SLOT_SIZE - BUTTON_SIZE - 6; // 6 pixel di margine totale
        int slotY = entryY + (ENTRY_HEIGHT - SLOT_SIZE) / 2; // Centrato verticalmente
        
        // Disegna lo slot (18x18)
        guiGraphics.blit(SINGLE_SLOT_TEXTURE, slotX, slotY, 0, 0, 
                        SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
        
        // Disegna l'icona della struttura nello slot se disponibile
        if (entryIndex < clientStructures.size()) {
            net.unfamily.iskautils.structure.StructureDefinition structure = clientStructures.get(entryIndex);
            renderStructureIcon(guiGraphics, structure, slotX + 1, slotY + 1); // +1 pixel per centrare nell'slot
        }
        
        // Posizione del pulsante: subito dopo lo slot, centrato verticalmente
        int buttonX = slotX + SLOT_SIZE + 2; // 2 pixel di spazio dopo lo slot
        int buttonY = entryY + (ENTRY_HEIGHT - BUTTON_SIZE) / 2; // Centrato verticalmente
        
        // Verifica se il mouse è sopra il pulsante
        boolean isHovered = mouseX >= buttonX && mouseX < buttonX + BUTTON_SIZE &&
                           mouseY >= buttonY && mouseY < buttonY + BUTTON_SIZE;
        
        // Usa sempre il primo pulsante del secondo set (primo tipo)
        int buttonV = isHovered ? BUTTON_HOVERED_V : BUTTON_NORMAL_V;
        
        // Disegna il pulsante (primo del secondo set)
        guiGraphics.blit(TINY_BUTTONS_TEXTURE, buttonX, buttonY, BUTTON_U, buttonV, 
                        BUTTON_SIZE, BUTTON_SIZE, 64, 96);
    }
    
    /**
     * Renderizza l'icona di una struttura nello slot (identica a StructureSelectionScreen)
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
     * Renderizza la scrollbar con handle e pulsanti (IDENTICA a StructureSelectionScreen)
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
        if (clientStructures.size() > VISIBLE_ENTRIES) {
            scrollRatio = (float) scrollOffset / (clientStructures.size() - VISIBLE_ENTRIES);
        }
        int handleY = scrollbarY + (int)(scrollRatio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
        
        boolean handleHovered = mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE &&
                               mouseY >= handleY && mouseY < handleY + HANDLE_SIZE;
        int handleTextureY = handleHovered ? HANDLE_SIZE : 0;
        guiGraphics.blit(SCROLLBAR_TEXTURE, scrollbarX, handleY, 
                        SCROLLBAR_WIDTH, handleTextureY, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
    }
    
    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        super.renderTooltip(guiGraphics, x, y);
        // Energia rimossa - nessun tooltip energia
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Gestisci i click sui pulsanti della scrollbar
        if (handleScrollButtonClick(mouseX, mouseY)) {
            return true;
        }
        
        // Gestisci i click sui tiny button nelle entry
        if (handleButtonClick(mouseX, mouseY)) {
            return true;
        }
        
        // Gestisci il click sulla maniglia della scrollbar
        if (handleHandleClick(mouseX, mouseY)) {
            return true;
        }
        
        // Gestisci i click sulla scrollbar
        if (handleScrollbarClick(mouseX, mouseY)) {
            return true;
        }
        
        // Gestisci i click sulle entry
        if (handleEntryClick(mouseX, mouseY)) {
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    /**
     * Gestisce i click sui pulsanti di scroll (frecce su/giù)
     */
    private boolean handleScrollButtonClick(double mouseX, double mouseY) {
        if (clientStructures.size() <= VISIBLE_ENTRIES) return false;
        
        int scrollbarX = this.leftPos + SCROLLBAR_X;
        
        // Pulsante SU
        int upButtonY = this.topPos + BUTTON_UP_Y;
        if (mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE && 
            mouseY >= upButtonY && mouseY < upButtonY + HANDLE_SIZE) {
            scrollUp();
            return true;
        }
        
        // Pulsante GIÙ
        int downButtonY = this.topPos + BUTTON_DOWN_Y;
        if (mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE && 
            mouseY >= downButtonY && mouseY < downButtonY + HANDLE_SIZE) {
            scrollDown();
            return true;
        }
        
        return false;
    }
    
    /**
     * Gestisce i click sui pulsanti delle entry
     */
    private boolean handleButtonClick(double mouseX, double mouseY) {
        for (int i = 0; i < VISIBLE_ENTRIES; i++) {
            int entryIndex = scrollOffset + i;
            if (entryIndex >= clientStructures.size()) continue;
            
            int entryX = this.leftPos + ENTRIES_START_X;
            int entryY = this.topPos + ENTRIES_START_Y + (i * ENTRY_HEIGHT);
            
            // Posizione del pulsante (deve corrispondere a renderSlotAndButton)
            int slotX = entryX + ENTRY_WIDTH - SLOT_SIZE - BUTTON_SIZE - 6;
            int buttonX = slotX + SLOT_SIZE + 2;
            int buttonY = entryY + (ENTRY_HEIGHT - BUTTON_SIZE) / 2;
            
            if (mouseX >= buttonX && mouseX < buttonX + BUTTON_SIZE &&
                mouseY >= buttonY && mouseY < buttonY + BUTTON_SIZE) {
                
                selectedEntryIndex = entryIndex;
                
                // TODO: Implementare azione per salvare la struttura
                net.unfamily.iskautils.structure.StructureDefinition structure = clientStructures.get(entryIndex);
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.player.displayClientMessage(
                        Component.literal("§aSaving structure: " + structure.getId()), 
                        true);
                }
                
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gestisce click sull'handle della scrollbar per il dragging
     */
    private boolean handleHandleClick(double mouseX, double mouseY) {
        if (clientStructures.size() <= VISIBLE_ENTRIES) return false;
        
        int scrollbarX = this.leftPos + SCROLLBAR_X;
        int scrollbarY = this.topPos + SCROLLBAR_Y;
        
        // Calcola posizione handle
        float scrollRatio = (float) scrollOffset / (clientStructures.size() - VISIBLE_ENTRIES);
        int handleY = scrollbarY + (int)(scrollRatio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
        
        if (mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE &&
            mouseY >= handleY && mouseY < handleY + HANDLE_SIZE) {
            isDraggingHandle = true;
            dragStartY = (int) mouseY;
            dragStartScrollOffset = scrollOffset;
            return true;
        }
        
        return false;
    }
    
    /**
     * Gestisce click sulla scrollbar (non sui pulsanti o handle)
     */
    private boolean handleScrollbarClick(double mouseX, double mouseY) {
        if (clientStructures.size() <= VISIBLE_ENTRIES) return false;
        
        int scrollbarX = this.leftPos + SCROLLBAR_X;
        int scrollbarY = this.topPos + SCROLLBAR_Y;
        
        if (mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH &&
            mouseY >= scrollbarY && mouseY <= scrollbarY + SCROLLBAR_HEIGHT) {
            
            // Click su scrollbar - sposta scroll alla posizione del click
            int clickY = (int) mouseY - scrollbarY;
            int maxScroll = Math.max(0, clientStructures.size() - VISIBLE_ENTRIES);
            scrollOffset = Math.min(maxScroll, (clickY * maxScroll) / SCROLLBAR_HEIGHT);
            return true;
        }
        return false;
    }
    
    /**
     * Gestisce click sulle entry
     */
    private boolean handleEntryClick(double mouseX, double mouseY) {
        for (int i = 0; i < VISIBLE_ENTRIES; i++) {
            int entryIndex = scrollOffset + i;
            if (entryIndex >= clientStructures.size()) break;
            
            int entryX = this.leftPos + ENTRIES_START_X;
            int entryY = this.topPos + ENTRIES_START_Y + (i * ENTRY_HEIGHT);
            
            if (mouseX >= entryX && mouseX <= entryX + ENTRY_WIDTH &&
                mouseY >= entryY && mouseY <= entryY + ENTRY_HEIGHT) {
                
                selectedEntryIndex = entryIndex;
                return true;
            }
        }
        return false;
    }
    
    /**
     * Scroll up methods
     */
    private void scrollUp() {
        scrollOffset = Math.max(0, scrollOffset - 1);
    }
    
    private void scrollDown() {
        int maxScroll = Math.max(0, clientStructures.size() - VISIBLE_ENTRIES);
        scrollOffset = Math.min(maxScroll, scrollOffset + 1);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Scroll wheel per le entry
        if (scrollY > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        } else if (scrollY < 0) {
            int maxScroll = Math.max(0, clientStructures.size() - VISIBLE_ENTRIES);
            scrollOffset = Math.min(maxScroll, scrollOffset + 1);
        }
        return true;
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
        if (button == 0 && isDraggingHandle && clientStructures.size() > VISIBLE_ENTRIES) {
            int scrollbarY = this.topPos + SCROLLBAR_Y;
            int deltaY = (int) mouseY - dragStartY;
            int maxScroll = clientStructures.size() - VISIBLE_ENTRIES;
            
            // Calcola nuovo offset basato sul movimento
            int newScrollOffset = dragStartScrollOffset + (deltaY * maxScroll) / (SCROLLBAR_HEIGHT - HANDLE_SIZE);
            scrollOffset = Math.max(0, Math.min(maxScroll, newScrollOffset));
            
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    /**
     * Renderizza i nuovi componenti UI (EditBox, Save button e "Area:")
     */
    private void renderNewComponents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Testo "Area:" - posizionato dopo il Save button
        int areaTextX = this.leftPos + ENTRIES_START_X + 50; // 10px dopo il Save button (larghezza 40px)
        guiGraphics.drawString(this.font, Component.translatable("gui.iska_utils.area"), 
                               areaTextX, this.topPos + BUTTONS_ROW_Y + 6, 0x404040, false);
        
        // Informazioni area dai dati sincronizzati
        boolean hasValidArea = this.menu.getSyncedHasValidArea();
        var vertex1 = this.menu.getSyncedVertex1();
        var vertex2 = this.menu.getSyncedVertex2();
        
        // Debug per area display
        System.out.println("DEBUG RENDER: Synced hasValidArea = " + hasValidArea);
        System.out.println("DEBUG RENDER: Synced vertex1 = " + vertex1);
        System.out.println("DEBUG RENDER: Synced vertex2 = " + vertex2);
        
        if (hasValidArea && vertex1 != null && vertex2 != null) {
                int[] dimensions = calculateDimensions(vertex1, vertex2);
                
                // Verifica validità (tutte le dimensioni ≤ 64)
                boolean isValid = dimensions[0] <= 64 && dimensions[1] <= 64 && dimensions[2] <= 64;
                
                // Formato testo: "32x45x60 XYZ"
                String areaText = String.format("%dx%dx%d XYZ", dimensions[0], dimensions[1], dimensions[2]);
                
                // Posizione: subito dopo il testo "Area:" sulla stessa riga
                int textX = areaTextX + this.font.width(Component.translatable("gui.iska_utils.area")) + 5; // 5px di spazio
                int textY = this.topPos + BUTTONS_ROW_Y + 6; // Stessa altezza del testo "Area:"
                
                // Colore neutro (stesso del testo normale)
                int color = 0x404040;
                
                guiGraphics.drawString(this.font, areaText, textX, textY, color, false);
                
                // Se non valido, mostra messaggio di errore sotto l'EditBox
                if (!isValid) {
                    String errorText = Component.translatable("gui.iska_utils.area_too_large").getString();
                    int errorX = this.leftPos + ENTRIES_START_X;
                    int errorY = this.topPos + EDIT_BOX_Y + 25;
                    guiGraphics.drawString(this.font, errorText, errorX, errorY, 0xFF0000, false);
                }
        }
    }
    
    /**
     * Gestisce il click sul pulsante Save
     */
    private void onSaveButtonClicked() {
        String structureName = nameEditBox.getValue().trim();
        if (structureName.isEmpty()) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                    Component.translatable("gui.iska_utils.save_error_empty_name"), 
                    true);
            }
            return;
        }
        
        // Verifica che il BlockEntity abbia dati blueprint
        var blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null || !blockEntity.hasBlueprintData()) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                    Component.translatable("gui.iska_utils.save_error_no_coordinates"), 
                    true);
            }
            return;
        }
        
        // Salva la struttura con le coordinate dal BlockEntity
        var vertex1 = blockEntity.getBlueprintVertex1();
        var vertex2 = blockEntity.getBlueprintVertex2();
        var center = blockEntity.getBlueprintCenter();
        
        if (this.minecraft != null && this.minecraft.player != null) {
            // TODO: Implementare il salvataggio effettivo della struttura
            // Per ora mostra un messaggio di debug con tutti i dati
            this.minecraft.player.displayClientMessage(
                Component.translatable("gui.iska_utils.save_success", structureName,
                    formatPosition(vertex1), formatPosition(vertex2), formatPosition(center)), 
                true);
            
            // Reset dei dati dopo il salvataggio
            blockEntity.clearBlueprintData();
            nameEditBox.setValue("");
        }
    }
    
    /**
     * Calcola le dimensioni dell'area
     */
    private int[] calculateDimensions(net.minecraft.core.BlockPos vertex1, net.minecraft.core.BlockPos vertex2) {
        int sizeX = Math.abs(vertex2.getX() - vertex1.getX()) + 1;
        int sizeY = Math.abs(vertex2.getY() - vertex1.getY()) + 1;
        int sizeZ = Math.abs(vertex2.getZ() - vertex1.getZ()) + 1;
        return new int[]{sizeX, sizeY, sizeZ};
    }
    
    /**
     * Formatta una posizione per la visualizzazione
     */
    private String formatPosition(net.minecraft.core.BlockPos pos) {
        return String.format("(%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ());
    }
    
    @Override
    public void containerTick() {
        super.containerTick();
        
        // Con ContainerData, i dati sono sincronizzati automaticamente dal server
        // Gli slot vengono popolati automaticamente dal server quando l'area viene impostata
        
        // Aggiorna lo stato del pulsante Save basandosi sulla validità dell'area
        if (saveButton != null) {
            boolean hasValidArea = this.menu.getSyncedHasValidArea();
            saveButton.active = hasValidArea; // Abilita solo se c'è un'area valida
        }
    }
} 