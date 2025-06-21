package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.structure.StructureLoader;
import net.unfamily.iskautils.network.ModMessages;

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
    private static final int BUTTON_U = 0; // Prima colonna
    private static final int BUTTON_NORMAL_V = 24; // Quarta riga (X normale)
    private static final int BUTTON_HOVERED_V = 32; // Quinta riga (X illuminato)
    
    // Pulsanti di selezione a pallino (8x8) - come nel Structure Placer
    private static final int SELECTION_BUTTON_EMPTY_U = 8; // Seconda colonna (pulsante vuoto)
    private static final int SELECTION_BUTTON_FILLED_U = 16; // Terza colonna (pulsante pieno)
    private static final int SELECTION_BUTTON_NORMAL_V = 0; // Prima riga (normale)
    private static final int SELECTION_BUTTON_HOVERED_V = 8; // Seconda riga (illuminato)
    
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
    private int selectedStructureIndex = -1; // Per tracciare la selezione a pallino (-1 = nessuna selezione)
    private boolean isDraggingHandle = false;
    private int dragStartY = 0;
    private int dragStartScrollOffset = 0;
    
    // Nuovi componenti UI
    private EditBox nameEditBox;
    private EditBox idEditBox;
    private Button saveButton;
    
    // Posizioni nuovi componenti - appena sotto le entry
    private static final int NAME_EDIT_BOX_Y = ENTRIES_START_Y + (VISIBLE_ENTRIES * ENTRY_HEIGHT) + 5; // 5px sotto l'ultima entry
    private static final int ID_EDIT_BOX_Y = NAME_EDIT_BOX_Y + 22; // 22px sotto la prima EditBox
    
    private static final int SAVE_BUTTON_X = ENTRIES_START_X;
    private static final int SAVE_BUTTON_Y = ID_EDIT_BOX_Y + 22; // 22px sotto la seconda EditBox
    
    public StructureSaverMachineScreen(StructureSaverMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        
        System.err.println("=== STRUCTURE SAVER MACHINE SCREEN CONSTRUCTOR ===");
        System.err.println("Menu: " + (menu != null ? "OK" : "NULL"));
        System.err.println("Player inventory: " + (playerInventory != null ? "OK" : "NULL"));
        System.err.println("Title: " + (title != null ? title.getString() : "NULL"));
        
        // Dimensioni reali della texture (176 x 246 dalla nuova texture)
        this.imageWidth = 176;
        this.imageHeight = 246;
        
        System.err.println("Set GUI dimensions: " + this.imageWidth + "x" + this.imageHeight);
    }
    
    @Override
    protected void init() {
        System.err.println("=== STRUCTURE SAVER MACHINE SCREEN INIT ===");
        System.err.println("Screen size: " + this.width + "x" + this.height);
        System.err.println("GUI size: " + this.imageWidth + "x" + this.imageHeight);
        
        super.init();
        
        // Centro il titolo ora che this.font è inizializzato
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        
        // Carica le strutture client (solo quelle client)
        loadClientStructures();
        
        // Inizializza i componenti UI
        initComponents();
        
        System.err.println("=== INIT COMPLETED ===");
        System.err.println("Save button: " + (saveButton != null ? "CREATED" : "NULL"));
        System.err.println("Name EditBox: " + (nameEditBox != null ? "CREATED" : "NULL"));
        System.err.println("ID EditBox: " + (idEditBox != null ? "CREATED" : "NULL"));
    }
    
    /**
     * Non più necessario richiedere sincronizzazione manuale - ContainerData lo fa automaticamente
     */
    private void requestDataFromServer() {
        // ContainerData sincronizza automaticamente i dati dal server
        System.out.println("DEBUG SCREEN: Using ContainerData for automatic sync");
    }
    
    /**
     * Inizializza le due EditBox e il pulsante Save
     */
    private void initComponents() {
        // EditBox per il nome - lunghe come le entry
        nameEditBox = new EditBox(this.font, this.leftPos + ENTRIES_START_X, this.topPos + NAME_EDIT_BOX_Y, 
                                 ENTRY_WIDTH, 20, Component.translatable("gui.iska_utils.structure_name"));
        nameEditBox.setMaxLength(64);
        nameEditBox.setHint(Component.literal("name")); // Hint che si cancella al click
        addRenderableWidget(nameEditBox);
        
        // EditBox per l'ID - lunghe come le entry
        idEditBox = new EditBox(this.font, this.leftPos + ENTRIES_START_X, this.topPos + ID_EDIT_BOX_Y, 
                               ENTRY_WIDTH, 20, Component.translatable("gui.iska_utils.structure_id"));
        idEditBox.setMaxLength(64);
        idEditBox.setHint(Component.literal("id")); // Hint che si cancella al click
        addRenderableWidget(idEditBox);
        
        // Pulsante Save - altezza 20px come prima
        saveButton = Button.builder(Component.translatable("gui.iska_utils.save"), 
                                   button -> onSaveButtonClicked())
                          .bounds(this.leftPos + SAVE_BUTTON_X, this.topPos + SAVE_BUTTON_Y, 40, 20)
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
        
        // Renderizza le quantità personalizzate DOPO tutti gli slot (sopra tutto)
        renderCustomStackCounts(guiGraphics);
        
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
                if (structureName.length() > 25) { // Aumentato da 20 a 25 caratteri
                    structureName = structureName.substring(0, 22) + "...";
                }
                guiGraphics.drawString(this.font, structureName, scaledTextX, scaledNameY, 0x404040, false);
                
                // Testo inferiore: ID della struttura
                String structureId = structure.getId();
                if (structureId.length() > 28) { // Aumentato da 23 a 28 caratteri
                    structureId = structureId.substring(0, 25) + "...";
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
     * Renderizza slot e pulsanti per una entry (selezione + X)
     */
    private void renderSlotAndButton(GuiGraphics guiGraphics, int entryX, int entryY, int entryIndex, int mouseX, int mouseY) {
        // Posizione dello slot: spostato più a sinistra per fare spazio ai due pulsanti
        int slotX = entryX + ENTRY_WIDTH - SLOT_SIZE - (BUTTON_SIZE * 2) - 8; // 8 pixel di margine totale per due pulsanti
        int slotY = entryY + (ENTRY_HEIGHT - SLOT_SIZE) / 2; // Centrato verticalmente
        
        // Disegna lo slot (18x18)
        guiGraphics.blit(SINGLE_SLOT_TEXTURE, slotX, slotY, 0, 0, 
                        SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
        
        // Disegna l'icona della struttura nello slot se disponibile
        if (entryIndex < clientStructures.size()) {
            net.unfamily.iskautils.structure.StructureDefinition structure = clientStructures.get(entryIndex);
            renderStructureIcon(guiGraphics, structure, slotX + 1, slotY + 1); // +1 pixel per centrare nell'slot
        }
        
        // Posizione del primo pulsante (selezione): subito dopo lo slot
        int selectionButtonX = slotX + SLOT_SIZE + 2; // 2 pixel di spazio dopo lo slot
        int selectionButtonY = entryY + (ENTRY_HEIGHT - BUTTON_SIZE) / 2; // Centrato verticalmente
        
        // Verifica se il mouse è sopra il pulsante di selezione
        boolean isSelectionHovered = mouseX >= selectionButtonX && mouseX < selectionButtonX + BUTTON_SIZE &&
                                    mouseY >= selectionButtonY && mouseY < selectionButtonY + BUTTON_SIZE;
        
        // Determina il tipo di pulsante di selezione (vuoto o pieno)
        boolean isSelected = (entryIndex == selectedStructureIndex);
        int selectionButtonU = isSelected ? SELECTION_BUTTON_FILLED_U : SELECTION_BUTTON_EMPTY_U;
        int selectionButtonV = isSelectionHovered ? SELECTION_BUTTON_HOVERED_V : SELECTION_BUTTON_NORMAL_V;
        
        // Disegna il pulsante di selezione
        guiGraphics.blit(TINY_BUTTONS_TEXTURE, selectionButtonX, selectionButtonY, selectionButtonU, selectionButtonV, 
                        BUTTON_SIZE, BUTTON_SIZE, 64, 96);
        
        // Posizione del secondo pulsante (X): subito dopo il pulsante di selezione
        int buttonX = selectionButtonX + BUTTON_SIZE + 2; // 2 pixel di spazio dopo il pulsante di selezione
        int buttonY = entryY + (ENTRY_HEIGHT - BUTTON_SIZE) / 2; // Centrato verticalmente
        
        // Verifica se il mouse è sopra il pulsante X
        boolean isHovered = mouseX >= buttonX && mouseX < buttonX + BUTTON_SIZE &&
                           mouseY >= buttonY && mouseY < buttonY + BUTTON_SIZE;
        
        // Usa il pulsante con X (secondo set): normale se non hover, illuminato se hover
        int buttonV = isHovered ? BUTTON_HOVERED_V : BUTTON_NORMAL_V;
        
        // Disegna il pulsante con X
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
     * Renderizza i nuovi componenti UI (Area: info)
     */
    private void renderNewComponents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Testo "Area:" - sotto i pulsanti, allineato con l'inizio delle entry (spostato 18px più in alto)
        int areaTextX = this.leftPos + ENTRIES_START_X;
        guiGraphics.drawString(this.font, Component.translatable("gui.iska_utils.area"), 
                               areaTextX, this.topPos + SAVE_BUTTON_Y + 25, 0x404040, false);
        
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
                int textY = this.topPos + SAVE_BUTTON_Y + 25; // Stessa altezza del testo "Area:"
                
                // Colore neutro (stesso del testo normale)
                int color = 0x404040;
                
                guiGraphics.drawString(this.font, areaText, textX, textY, color, false);
                
                // Se non valido, mostra messaggio di errore sotto le EditBox (anche questo spostato più in alto)
                if (!isValid) {
                    String errorText = Component.translatable("gui.iska_utils.area_too_large").getString();
                    int errorX = this.leftPos + ENTRIES_START_X;
                    int errorY = this.topPos + ID_EDIT_BOX_Y + 25;
                    guiGraphics.drawString(this.font, errorText, errorX, errorY, 0xFF0000, false);
                }
        }
    }
    

    
    /**
     * Gestisce il click sul pulsante Save
     */
    private void onSaveButtonClicked() {
        System.err.println("=== SAVE BUTTON CLICKED ===");
        
        // Mostra messaggio nel gioco per conferma
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(
                Component.literal("§e[DEBUG] Save button clicked!"), 
                true);
        }
        
        String structureName = nameEditBox.getValue().trim();
        String structureId = idEditBox.getValue().trim();
        System.err.println("Structure name: '" + structureName + "'");
        System.err.println("Structure ID: '" + structureId + "'");
        
        // Validazione: entrambi i campi sono obbligatori
        if (structureName.isEmpty()) {
            System.out.println("ERROR: Structure name is empty");
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                    Component.translatable("gui.iska_utils.save_error_empty_name"), 
                    true);
            }
            return;
        }
        
        if (structureId.isEmpty()) {
            System.out.println("ERROR: Structure ID is empty");
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                    Component.translatable("gui.iska_utils.save_error_empty_id"), 
                    true);
            }
            return;
        }
        
        // Verifica che ci siano dati blueprint usando i dati sincronizzati
        boolean hasValidArea = this.menu.getSyncedHasValidArea();
        var vertex1 = this.menu.getSyncedVertex1();
        var vertex2 = this.menu.getSyncedVertex2();
        
        System.out.println("Has valid area: " + hasValidArea);
        System.out.println("Vertex1: " + vertex1);
        System.out.println("Vertex2: " + vertex2);
        
        if (!hasValidArea || vertex1 == null || vertex2 == null) {
            System.out.println("ERROR: No valid blueprint data");
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                    Component.translatable("gui.iska_utils.save_error_no_coordinates"), 
                    true);
            }
            return;
        }
        
        // Verifica che le dimensioni dell'area siano valide (≤ 64x64x64)
        int[] dimensions = calculateDimensions(vertex1, vertex2);
        System.out.println("Area dimensions: " + dimensions[0] + "x" + dimensions[1] + "x" + dimensions[2]);
        
        if (dimensions[0] > 64 || dimensions[1] > 64 || dimensions[2] > 64) {
            System.out.println("ERROR: Area too large");
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                    Component.translatable("gui.iska_utils.area_too_large"), 
                    true);
            }
            return;
        }
        
        // Invia il packet al server per salvare la struttura usando la posizione sincronizzata
        BlockPos machinePos = this.menu.getSyncedBlockPos();
        System.out.println("Synced machine pos: " + machinePos);
        
        if (!machinePos.equals(BlockPos.ZERO)) {
            System.out.println("=== CALLING PACKET SEND METHOD ===");
            net.unfamily.iskautils.network.ModMessages.sendStructureSaverMachineSavePacket(structureName, structureId, machinePos);
            
            // Reset delle editbox dopo l'invio del packet (i dati blueprint verranno puliti dal server)
            nameEditBox.setValue("");
            idEditBox.setValue("");
            
            // Ricarica le strutture client per mostrare quella appena salvata nella lista
            loadClientStructures();
        } else {
            System.out.println("ERROR: Machine position is ZERO, trying fallback...");
            
            // Fallback: prova a ottenere la posizione dal block entity
            var blockEntity = this.menu.getBlockEntity();
            if (blockEntity != null) {
                BlockPos fallbackPos = blockEntity.getBlockPos();
                System.out.println("Fallback machine pos: " + fallbackPos);
                net.unfamily.iskautils.network.ModMessages.sendStructureSaverMachineSavePacket(structureName, structureId, fallbackPos);
                nameEditBox.setValue("");
                idEditBox.setValue("");
                loadClientStructures();
            } else {
                System.out.println("ERROR: No block entity available, cannot save");
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.player.displayClientMessage(
                        Component.literal("§cErrore: Impossibile trovare la macchina"), 
                        true);
                }
            }
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
            boolean wasActive = saveButton.active;
            saveButton.active = hasValidArea; // Abilita solo se c'è un'area valida
            
            // Log solo quando lo stato cambia
            if (wasActive != hasValidArea) {
                System.err.println("Save button active changed: " + hasValidArea);
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.player.displayClientMessage(
                        Component.literal("§6[DEBUG] Save button active: " + hasValidArea), 
                        true);
                }
            }
        }
    }
    
    /**
     * Renderizza tutte le quantità personalizzate sotto gli item
     */
    private void renderCustomStackCounts(GuiGraphics guiGraphics) {
        for (var slot : this.menu.slots) {
            if (slot instanceof net.neoforged.neoforge.items.SlotItemHandler) {
                var stack = slot.getItem();
                if (!stack.isEmpty()) {
                    int actualCount = getActualCount(stack);
                    if (actualCount > 1) {
                        renderCustomStackCount(guiGraphics, slot, stack);
                    }
                }
            }
        }
    }
    
    /**
     * Renderizza lo stack count personalizzato per un singolo slot
     */
    private void renderCustomStackCount(GuiGraphics guiGraphics, Slot slot, ItemStack stack) {
        // Leggi il count reale dall'NBT
        int actualCount = getActualCount(stack);
        if (actualCount <= 1) return; // Non mostrare count per 1 item
        
        // Scala del testo progressiva in base al numero
        float stackCountScale;
        if (actualCount > 9999) {
            stackCountScale = 0.5f; // 50% per numeri molto grandi (5+ cifre)
        } else {
            stackCountScale = 0.7f; // 70% per numeri normali (1-4 cifre)
        }
        
        // Usa le coordinate assolute dello slot
        int slotX = this.leftPos + slot.x;
        int slotY = this.topPos + slot.y;
        
        // Usa il count reale dall'NBT
        String countText = String.valueOf(actualCount);
        
        // Calcola la dimensione del testo alla scala desiderata
        int scaledTextWidth = (int)(this.font.width(countText) * stackCountScale);
        
        // Posiziona il testo SOTTO lo slot, centrato orizzontalmente
        int finalX = slotX + (16 - scaledTextWidth) / 2;  // Centrato orizzontalmente nello slot
        int finalY = slotY + 20;  // 4 pixel sotto lo slot (16 + 4), spostato 2px più in basso
        
        // Applica la scala solo per il rendering del testo
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(stackCountScale, stackCountScale, 1.0f);
        
        // Converti la posizione finale per il sistema scalato
        int scaledX = (int)(finalX / stackCountScale);
        int scaledY = (int)(finalY / stackCountScale);
        
        // Disegna il testo scuro senza ombra (grigio scuro)
        guiGraphics.drawString(this.font, countText, scaledX, scaledY, 0x404040, false);
        
        guiGraphics.pose().popPose();
    }
    
    /**
     * Legge il count reale dall'NBT dell'item
     */
    private int getActualCount(ItemStack stack) {
        var customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            var tag = customData.copyTag();
            return tag.getInt("ActualCount");
        }
        return stack.getCount(); // Fallback al count normale
    }
} 