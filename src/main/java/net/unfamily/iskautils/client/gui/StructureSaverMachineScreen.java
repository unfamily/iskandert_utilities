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
    
    // Scrollbar dimensions (identical to StructureSelectionScreen)
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int SCROLLBAR_HEIGHT = 34;
    private static final int HANDLE_SIZE = 8;
    
    // Button dimensions (first button of second set)
    private static final int BUTTON_SIZE = 8;
    private static final int BUTTON_U = 0; // First column
    private static final int BUTTON_NORMAL_V = 24; // Fourth row (X normal)
    private static final int BUTTON_HOVERED_V = 32; // Fifth row (X illuminated)
    
    // Selection buttons (8x8) - like in Structure Placer
    private static final int SELECTION_BUTTON_EMPTY_U = 8; // Second column (empty button)
    private static final int SELECTION_BUTTON_FILLED_U = 16; // Third column (full button)
    private static final int SELECTION_BUTTON_NORMAL_V = 0; // First row (normal)
    private static final int SELECTION_BUTTON_HOVERED_V = 8; // Second row (illuminated)
    
    // Slot dimensions
    private static final int SLOT_SIZE = 18;
    

    
    // Positions (adapted for 176x246 GUI, moved higher without energy bar)
    private static final int ENTRIES_START_X = 18;
    private static final int ENTRIES_START_Y = 20; // Moved 5 pixels higher (from 25 to 20)
    
    // Scrollbar positions (updated for new entry position)
    private static final int SCROLLBAR_X = ENTRIES_START_X + ENTRY_WIDTH + 4;
    private static final int BUTTON_UP_Y = ENTRIES_START_Y;
    private static final int SCROLLBAR_Y = ENTRIES_START_Y + HANDLE_SIZE;
    private static final int BUTTON_DOWN_Y = SCROLLBAR_Y + SCROLLBAR_HEIGHT;
    
    // Scrolling variables
    private int scrollOffset = 0;
    private java.util.List<net.unfamily.iskautils.structure.StructureDefinition> clientStructures;
    private int selectedEntryIndex = -1;
    private int selectedStructureIndex = -1; // To track selection (-1 = no selection)
    private boolean isDraggingHandle = false;
    private int dragStartY = 0;
    private int dragStartScrollOffset = 0;
    
    // New UI components
    private EditBox nameEditBox;
    private EditBox idEditBox;
    private Button saveButton;
    private Button modeButton;
    private Button closeButton;
    
    // Placement mode
    private boolean isPlayerMode = false; // false = Normal, true = Player
    
    // Close button position - top right
    private static final int CLOSE_BUTTON_Y = 5; // 5px from top edge
    private static final int CLOSE_BUTTON_SIZE = 12; // 12x12 pixels
    // CLOSE_BUTTON_X will be calculated in init() using imageWidth
    
    // New component positions - just below entries
    private static final int NAME_EDIT_BOX_Y = ENTRIES_START_Y + (VISIBLE_ENTRIES * ENTRY_HEIGHT) + 5; // 5px below last entry
    private static final int ID_EDIT_BOX_Y = NAME_EDIT_BOX_Y + 22; // 22px below first EditBox
    
    private static final int SAVE_BUTTON_X = ENTRIES_START_X;
    private static final int SAVE_BUTTON_Y = ID_EDIT_BOX_Y + 22; // 22px below second EditBox
    
    // Mode button position - aligned with EditBox end
    private static final int MODE_BUTTON_X = ENTRIES_START_X + ENTRY_WIDTH - 50; // Aligned with EditBox end
    private static final int MODE_BUTTON_Y = SAVE_BUTTON_Y;
    private static final int MODE_BUTTON_WIDTH = 50; // Width for "Normal"/"Player" text
    
    public StructureSaverMachineScreen(StructureSaverMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        
        // Actual texture dimensions (176 x 246 from new texture)
        this.imageWidth = 176;
        this.imageHeight = 246;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Ricarica le strutture all'apertura della GUI usando il flag del config
        net.unfamily.iskautils.structure.StructureLoader.reloadAllDefinitions(net.unfamily.iskautils.Config.acceptClientStructure);
        
        // Request data from server (area bounds, etc.)
        requestDataFromServer();
        
        // Initialize components (EditBoxes, buttons, etc.)
        initComponents();
        
        // Load client structures
        loadClientStructures();
    }
    
    /**
     * No longer necessary to manually request synchronization - ContainerData does it automatically
     */
    private void requestDataFromServer() {
        // ContainerData synchronizes data automatically from server
    }
    
    /**
     * Initialize the two EditBoxes and the Save button
     */
    private void initComponents() {
        // EditBox for name - length as entry
        nameEditBox = new EditBox(this.font, this.leftPos + ENTRIES_START_X, this.topPos + NAME_EDIT_BOX_Y, 
                                 ENTRY_WIDTH, 20, Component.translatable("gui.iska_utils.structure_name"));
        nameEditBox.setMaxLength(64);
        nameEditBox.setHint(Component.translatable("gui.iska_utils.structure_saver.name_hint")); // Hint that clears on click
        addRenderableWidget(nameEditBox);
        
        // EditBox for ID - length as entry
        idEditBox = new EditBox(this.font, this.leftPos + ENTRIES_START_X, this.topPos + ID_EDIT_BOX_Y, 
                               ENTRY_WIDTH, 20, Component.translatable("gui.iska_utils.structure_id"));
        idEditBox.setMaxLength(64);
        idEditBox.setHint(Component.translatable("gui.iska_utils.structure_saver.id_hint")); // Hint that clears on click
        addRenderableWidget(idEditBox);
        
        // Save/Update button - height 20px like before
        saveButton = Button.builder(Component.translatable("gui.iska_utils.save"), 
                                   button -> onSaveButtonClicked())
                          .bounds(this.leftPos + SAVE_BUTTON_X, this.topPos + SAVE_BUTTON_Y, 40, 20)
                          .build();
        addRenderableWidget(saveButton);
        
        // Mode button - height 20px like before
        modeButton = Button.builder(Component.translatable("gui.iska_utils.structure_saver.mode.normal"), 
                                   button -> onModeButtonClicked())
                          .bounds(this.leftPos + MODE_BUTTON_X, this.topPos + MODE_BUTTON_Y, MODE_BUTTON_WIDTH, 20)
                          .build();
        addRenderableWidget(modeButton);
        
        // Close button - top right with ✕ symbol
        int closeButtonX = this.imageWidth - CLOSE_BUTTON_SIZE - 5; // 5px from right edge
        closeButton = Button.builder(Component.literal("✕"), 
                                    button -> {
                                        playButtonSound();
                                        this.onClose();
                                    })
                           .bounds(this.leftPos + closeButtonX, this.topPos + CLOSE_BUTTON_Y, 
                                  CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
                           .build();
        addRenderableWidget(closeButton);
        
        // Slots are populated automatically by the server when the area is set
    }
    
    /**
     * Populates slots with blocks from the area from the blueprint
     */
    // RIMOSSO: populateAreaBlocks now executed by the server in the BlockEntity
    
    // RIMOSSO: scanAndPopulateBlocks - now handled by the server in the BlockEntity
    
    /**
     * Loads structures from player_structures.json (only those modifiable via this GUI)
     */
    private void loadClientStructures() {
        // Use the new dedicated method that loads only from player_structures.json
        var playerStructures = net.unfamily.iskautils.structure.StructureLoader.getPlayerStructuresForSaverMachine();
        this.clientStructures = new java.util.ArrayList<>(playerStructures.values());
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Render custom quantities DOPO tutti gli slot (sopra tutto)
        renderCustomStackCounts(guiGraphics);
        
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Center the GUI
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        // Draw the background specifying explicitly the texture dimensions
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 176, 246);
        
        // Render components
        renderEntries(guiGraphics, mouseX, mouseY);
        renderScrollbar(guiGraphics, mouseX, mouseY);
        renderNewComponents(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Only the translatable title
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }
    
    /**
     * Renders the 3 visible entries with client structures
     */
    private void renderEntries(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (int i = 0; i < VISIBLE_ENTRIES; i++) {
            int entryIndex = scrollOffset + i;
            
            int entryX = this.leftPos + ENTRIES_START_X;
            int entryY = this.topPos + ENTRIES_START_Y + (i * ENTRY_HEIGHT);
            
            // Draw entry background
            guiGraphics.blit(ENTRY_TEXTURE, entryX, entryY, 0, 0, ENTRY_WIDTH, ENTRY_HEIGHT, 140, 24);
            
            // If the entry has a client structure, show name, ID, slot and button
            if (entryIndex < clientStructures.size()) {
                net.unfamily.iskautils.structure.StructureDefinition structure = clientStructures.get(entryIndex);
                
                // Smaller text: scale to 0.7
                float textScale = 0.7f;
                guiGraphics.pose().pushPose();
                guiGraphics.pose().scale(textScale, textScale, 1.0f);
                
                // Calculate positions with scaling - slot and button space
                int scaledTextX = (int)((entryX + 4) / textScale);
                int scaledNameY = (int)((entryY + 2) / textScale);
                
                // Upper text: Structure name
                String structureName = structure.getName() != null ? structure.getName() : structure.getId();
                if (structureName.length() > 25) { // Increased from 20 to 25 characters
                    structureName = structureName.substring(0, 22) + "...";
                }
                guiGraphics.drawString(this.font, structureName, scaledTextX, scaledNameY, 0x404040, false);
                
                // Lower text: Structure ID
                String structureId = structure.getId();
                if (structureId.length() > 28) { // Increased from 23 to 28 characters
                    structureId = structureId.substring(0, 25) + "...";
                }
                int scaledIdY = (int)((entryY + ENTRY_HEIGHT - (this.font.lineHeight * textScale) - 2) / textScale);
                guiGraphics.drawString(this.font, structureId, scaledTextX, scaledIdY, 0x666666, false);
                
                guiGraphics.pose().popPose();
                
                // Render slot and button
                renderSlotAndButton(guiGraphics, entryX, entryY, entryIndex, mouseX, mouseY);
            }
        }
    }
    
    /**
     * Renders slot and buttons for an entry (selection + X, but buttons temporarily disabled via comment)
     */
    private void renderSlotAndButton(GuiGraphics guiGraphics, int entryX, int entryY, int entryIndex, int mouseX, int mouseY) {
        // Slot position: moved more to the left for space for two buttons
        int slotX = entryX + ENTRY_WIDTH - SLOT_SIZE - (BUTTON_SIZE * 2) - 8; // 8 pixel total margin for two buttons
        int slotY = entryY + (ENTRY_HEIGHT - SLOT_SIZE) / 2; // Centered vertically
        
        // --- SLOT SINGLE TEMPORARILY DISABILITATED ---
        /*
        // Draw the slot (18x18)
        guiGraphics.blit(SINGLE_SLOT_TEXTURE, slotX, slotY, 0, 0, 
                        SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
        
        // Draw the structure icon in the slot if available
        if (entryIndex < clientStructures.size()) {
            net.unfamily.iskautils.structure.StructureDefinition structure = clientStructures.get(entryIndex);
            renderStructureIcon(guiGraphics, structure, slotX + 1, slotY + 1); // +1 pixel to center in the slot
        }
        */
        
        // --- SELECTION BUTTONS TEMPORARILY DISABILITATED ---
        /*
        // Position of the first button (selection): right after the slot
        int selectionButtonX = slotX + SLOT_SIZE + 2; // 2 pixel space after the slot
        int selectionButtonY = entryY + (ENTRY_HEIGHT - BUTTON_SIZE) / 2; // Centered vertically
        
        // Check if the mouse is over the selection button
        boolean isSelectionHovered = mouseX >= selectionButtonX && mouseX < selectionButtonX + BUTTON_SIZE &&
                                    mouseY >= selectionButtonY && mouseY < selectionButtonY + BUTTON_SIZE;
        
        // Determine the type of selection button (empty or full)
        boolean isSelected = (entryIndex == selectedStructureIndex);
        int selectionButtonU = isSelected ? SELECTION_BUTTON_FILLED_U : SELECTION_BUTTON_EMPTY_U;
        int selectionButtonV = isSelectionHovered ? SELECTION_BUTTON_HOVERED_V : SELECTION_BUTTON_NORMAL_V;
        
        // Draw the selection button
        guiGraphics.blit(TINY_BUTTONS_TEXTURE, selectionButtonX, selectionButtonY, selectionButtonU, selectionButtonV, 
                        BUTTON_SIZE, BUTTON_SIZE, 64, 96);
        
        // Position of the second button (X): right after the selection button
        int buttonX = selectionButtonX + BUTTON_SIZE + 2; // 2 pixel space after the selection button
        int buttonY = entryY + (ENTRY_HEIGHT - BUTTON_SIZE) / 2; // Centered vertically
        
        // Check if the mouse is over the X button
        boolean isHovered = mouseX >= buttonX && mouseX < buttonX + BUTTON_SIZE &&
                           mouseY >= buttonY && mouseY < buttonY + BUTTON_SIZE;
        
        // Use the X button (second set): normal if not hover, illuminated if hover
        int buttonV = isHovered ? BUTTON_HOVERED_V : BUTTON_NORMAL_V;
        
        // Draw the X button
        guiGraphics.blit(TINY_BUTTONS_TEXTURE, buttonX, buttonY, BUTTON_U, buttonV, 
                        BUTTON_SIZE, BUTTON_SIZE, 64, 96);
        */
    }
    
    /**
     * Renders the icon of a structure in the slot (identical to StructureSelectionScreen)
     */
    private void renderStructureIcon(GuiGraphics guiGraphics, net.unfamily.iskautils.structure.StructureDefinition structure, int x, int y) {
        if (structure.getIcon() != null && structure.getIcon().getItem() != null) {
            // Try to get the item from the specified ID in the script
            try {
                net.minecraft.resources.ResourceLocation itemId = net.minecraft.resources.ResourceLocation.parse(structure.getIcon().getItem());
                net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId);
                
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    net.minecraft.world.item.ItemStack itemStack = new net.minecraft.world.item.ItemStack(item);
                    guiGraphics.renderItem(itemStack, x, y);
                    return;
                }
            } catch (Exception e) {
                // If fails, use default item
            }
        }
        
        // Default item if not specified or not found: stone block
        net.minecraft.world.item.ItemStack defaultItem = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE);
        guiGraphics.renderItem(defaultItem, x, y);
    }
    

    
    /**
     * Renders the scrollbar with handle and buttons (IDENTICAL to StructureSelectionScreen)
     */
    private void renderScrollbar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int scrollbarX = this.leftPos + SCROLLBAR_X;
        int scrollbarY = this.topPos + SCROLLBAR_Y;
        int buttonUpY = this.topPos + BUTTON_UP_Y;
        int buttonDownY = this.topPos + BUTTON_DOWN_Y;
        
        // Draw the complete scrollbar (8 pixel wide, 34 pixel height)
        guiGraphics.blit(SCROLLBAR_TEXTURE, scrollbarX, scrollbarY, 0, 0, 
                        SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT, 32, 34);
        
        // Up button (8x8 pixel) - above the scrollbar
        boolean upHovered = mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE &&
                           mouseY >= buttonUpY && mouseY < buttonUpY + HANDLE_SIZE;
        int upTextureY = upHovered ? HANDLE_SIZE : 0;
        guiGraphics.blit(SCROLLBAR_TEXTURE, scrollbarX, buttonUpY, 
                        SCROLLBAR_WIDTH * 2, upTextureY, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        
        // Down button (8x8 pixel) - below the scrollbar  
        boolean downHovered = mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE &&
                             mouseY >= buttonDownY && mouseY < buttonDownY + HANDLE_SIZE;
        int downTextureY = downHovered ? HANDLE_SIZE : 0;
        guiGraphics.blit(SCROLLBAR_TEXTURE, scrollbarX, buttonDownY, 
                        SCROLLBAR_WIDTH * 3, downTextureY, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        
        // Handle (8x8 pixel) - always visible, but mobile only if necessary
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
        
        // Tooltip for the mode button
        if (modeButton != null && x >= modeButton.getX() && x < modeButton.getX() + modeButton.getWidth() &&
            y >= modeButton.getY() && y < modeButton.getY() + modeButton.getHeight()) {
            
            java.util.List<Component> tooltipLines = new java.util.ArrayList<>();
            tooltipLines.add(Component.translatable("gui.iska_utils.structure_saver.mode_tooltip.line1"));
            
            if (isPlayerMode) {
                tooltipLines.add(Component.translatable("gui.iska_utils.structure_saver.mode_tooltip.line2.player"));
                tooltipLines.add(Component.translatable("gui.iska_utils.structure_saver.mode_tooltip.line3.player"));
            } else {
                tooltipLines.add(Component.translatable("gui.iska_utils.structure_saver.mode_tooltip.line2.normal"));
            }
            
            guiGraphics.renderComponentTooltip(this.font, tooltipLines, x, y);
        }
    }
    
    /**
     * Scrolls up (with sound for clicks)
     */
    private void scrollUp() {
        if (scrollUpSilent()) {
            playButtonSound();
        }
    }

    /**
     * Scrolls down (with sound for clicks)
     */
    private void scrollDown() {
        if (scrollDownSilent()) {
            playButtonSound();
        }
    }
    
    /**
     * Scrolls up silently (for mouse wheel)
     */
    private boolean scrollUpSilent() {
        if (clientStructures.size() > VISIBLE_ENTRIES && scrollOffset > 0) {
            scrollOffset--;
            return true;
        }
        return false;
    }
    
    /**
     * Scrolls down silently (for mouse wheel)
     */
    private boolean scrollDownSilent() {
        if (clientStructures.size() > VISIBLE_ENTRIES && scrollOffset < clientStructures.size() - VISIBLE_ENTRIES) {
            scrollOffset++;
            return true;
        }
        return false;
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Handles mouse scroll WITHOUT SOUND (like StructureSelectionScreen)
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
        if (button == 0 && isDraggingHandle && clientStructures.size() > VISIBLE_ENTRIES) {
            int scrollbarY = this.topPos + SCROLLBAR_Y;
            int deltaY = (int) mouseY - dragStartY;
            int maxScroll = clientStructures.size() - VISIBLE_ENTRIES;
            
            // Calculate new offset based on movement
            int newScrollOffset = dragStartScrollOffset + (deltaY * maxScroll) / (SCROLLBAR_HEIGHT - HANDLE_SIZE);
            scrollOffset = Math.max(0, Math.min(maxScroll, newScrollOffset));
            
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    /**
     * Renders new UI components (Area: info)
     */
    private void renderNewComponents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Text "Area:" - below buttons, aligned with the start of entries (moved 18px higher)
        int areaTextX = this.leftPos + ENTRIES_START_X;
        guiGraphics.drawString(this.font, Component.translatable("gui.iska_utils.area"), 
                               areaTextX, this.topPos + SAVE_BUTTON_Y + 25, 0x404040, false);
        
        // Area information from synchronized data
        boolean hasValidArea = this.menu.getSyncedHasValidArea();
        var vertex1 = this.menu.getSyncedVertex1();
        var vertex2 = this.menu.getSyncedVertex2();
        
        if (hasValidArea && vertex1 != null && vertex2 != null) {
                int[] dimensions = calculateDimensions(vertex1, vertex2);
                
                // Check validity (all dimensions ≤ 64)
                boolean isValid = dimensions[0] <= 64 && dimensions[1] <= 64 && dimensions[2] <= 64;
                
                // Text format: "32x45x60 XYZ"
                String areaText = String.format("%dx%dx%d XYZ", dimensions[0], dimensions[1], dimensions[2]);
                
                // Position: right after the "Area:" text on the same line
                int textX = areaTextX + this.font.width(Component.translatable("gui.iska_utils.area")) + 5; // 5px space
                int textY = this.topPos + SAVE_BUTTON_Y + 25; // Same height as "Area:" text
                
                // Neutral color (same as normal text color)
                int color = 0x404040;
                
                guiGraphics.drawString(this.font, areaText, textX, textY, color, false);
                
                // If not valid, shows error message below the EditBox (also moved higher)
                if (!isValid) {
                    String errorText = Component.translatable("gui.iska_utils.area_too_large").getString();
                    int errorX = this.leftPos + ENTRIES_START_X;
                    int errorY = this.topPos + ID_EDIT_BOX_Y + 25;
                    guiGraphics.drawString(this.font, errorText, errorX, errorY, 0xFF0000, false);
                }
        }
    }
    

    
    /**
     * Handles the click on the Save button
     */
    private void onSaveButtonClicked() {
        String structureName = nameEditBox.getValue().trim();
        String structureId = idEditBox.getValue().trim();
        
        // Validation: both fields are required
        if (structureName.isEmpty()) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                    Component.translatable("gui.iska_utils.save_error_empty_name"), 
                    true);
            }
            return;
        }
        
        if (structureId.isEmpty()) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                    Component.translatable("gui.iska_utils.save_error_empty_id"), 
                    true);
            }
            return;
        }
        
        // Check if there are blueprint data using synchronized data
        boolean hasValidArea = this.menu.getSyncedHasValidArea();
        var vertex1 = this.menu.getSyncedVertex1();
        var vertex2 = this.menu.getSyncedVertex2();
        
        if (!hasValidArea || vertex1 == null || vertex2 == null) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                    Component.translatable("gui.iska_utils.save_error_no_coordinates"), 
                    true);
            }
            return;
        }
        
        // Check if the area dimensions are valid (≤ 64x64x64)
        int[] dimensions = calculateDimensions(vertex1, vertex2);
        
        if (dimensions[0] > 64 || dimensions[1] > 64 || dimensions[2] > 64) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                    Component.translatable("gui.iska_utils.area_too_large"), 
                    true);
            }
            return;
        }
        
        // Determine if it's a modification or a new save
        boolean isModify = (selectedStructureIndex != -1);
        String oldStructureId = null;
        
        if (isModify && selectedStructureIndex < clientStructures.size()) {
            // Get the full ID of the selected structure (include prefix "client_playername_")
            oldStructureId = clientStructures.get(selectedStructureIndex).getId();
            
            // Construct the full new ID with prefix for the server
            if (this.minecraft != null && this.minecraft.player != null) {
                String playerName = this.minecraft.player.getName().getString();
                String newFullStructureId = "client_" + playerName + "_" + structureId;
            }
        }
        
        // Send the packet to the server to save/modify the structure using the synchronized position
        BlockPos machinePos = this.menu.getSyncedBlockPos();
        
        if (!machinePos.equals(BlockPos.ZERO)) {
            net.unfamily.iskautils.network.ModMessages.sendStructureSaverMachineSavePacket(structureName, structureId, machinePos, isPlayerMode, isPlayerMode, oldStructureId);
            
            // Reset EditBoxes after sending the packet (blueprint data will be cleared by the server)
            nameEditBox.setValue("");
            idEditBox.setValue("");
            
            // Reset selection after save/modify
            selectedStructureIndex = -1;
            
            // Reload client structures to show the newly saved/modified one in the list
            loadClientStructures();
        } else {
            // Fallback: try to get the position from the block entity
            var blockEntity = this.menu.getBlockEntity();
            if (blockEntity != null) {
                BlockPos fallbackPos = blockEntity.getBlockPos();
                net.unfamily.iskautils.network.ModMessages.sendStructureSaverMachineSavePacket(structureName, structureId, fallbackPos, isPlayerMode, isPlayerMode, oldStructureId);
                nameEditBox.setValue("");
                idEditBox.setValue("");
                selectedStructureIndex = -1;
                loadClientStructures();
            } else {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.player.displayClientMessage(
                        Component.translatable("gui.iska_utils.structure_saver.error.machine_not_found"), 
                        true);
                }
            }
        }
    }
    
    /**
     * Calculates the area dimensions
     */
    private int[] calculateDimensions(net.minecraft.core.BlockPos vertex1, net.minecraft.core.BlockPos vertex2) {
        int sizeX = Math.abs(vertex2.getX() - vertex1.getX()) + 1;
        int sizeY = Math.abs(vertex2.getY() - vertex1.getY()) + 1;
        int sizeZ = Math.abs(vertex2.getZ() - vertex1.getZ()) + 1;
        return new int[]{sizeX, sizeY, sizeZ};
    }
    
    /**
     * Formats a position for display
     */
    private String formatPosition(net.minecraft.core.BlockPos pos) {
        return String.format("(%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ());
    }
    
    @Override
    public void containerTick() {
        super.containerTick();
        
        // With ContainerData, data is synchronized automatically from server
        // Slots are populated automatically by the server when the area is set
        
        // Save button remains always active - validation is done in the onSaveButtonClicked() method
        if (saveButton != null) {
            saveButton.active = true; // Always active, validation in method
        }
        
        // Update button text based on selection
        updateSaveButtonText();
    }
    
    /**
     * Updates the text of the Save/Modify button based on selection
     */
    private void updateSaveButtonText() {
        if (saveButton != null) {
            if (selectedStructureIndex == -1) {
                saveButton.setMessage(Component.translatable("gui.iska_utils.save"));
            } else {
                saveButton.setMessage(Component.translatable("gui.iska_utils.modify"));
            }
        }
    }
    
    /**
     * Populates EditBoxes with data from the selected structure (or clears if no selection)
     */
    private void populateEditBoxesFromSelection() {
        if (selectedStructureIndex == -1 || selectedStructureIndex >= clientStructures.size()) {
            // No selection or invalid selection - clear EditBoxes
            if (nameEditBox != null) {
                nameEditBox.setValue("");
            }
            if (idEditBox != null) {
                idEditBox.setValue("");
            }
        } else {
            // Valid selection - populate with structure data
            var selectedStructure = clientStructures.get(selectedStructureIndex);
            
            if (nameEditBox != null) {
                nameEditBox.setValue(selectedStructure.getName());
            }
            if (idEditBox != null) {
                // Remove "client_playername_" prefix from ID to show only user ID
                String fullId = selectedStructure.getId();
                if (this.minecraft != null && this.minecraft.player != null) {
                    String playerName = this.minecraft.player.getName().getString();
                    String prefix = "client_" + playerName + "_";
                    if (fullId.startsWith(prefix)) {
                        String userFriendlyId = fullId.substring(prefix.length());
                        idEditBox.setValue(userFriendlyId);
                    } else {
                        // Fallback if format is not expected
                        idEditBox.setValue(fullId);
                    }
                } else {
                    // Fallback if no player
                    idEditBox.setValue(fullId);
                }
            }
        }
    }
    
    /**
     * Renders all custom quantities under items
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
     * Renders custom stack count for a single slot
     */
    private void renderCustomStackCount(GuiGraphics guiGraphics, Slot slot, ItemStack stack) {
        // Read actual count from NBT
        int actualCount = getActualCount(stack);
        if (actualCount <= 1) return; // Don't show count for 1 item
        
        // Stack count scale based on number
        float stackCountScale;
        if (actualCount > 9999) {
            stackCountScale = 0.5f; // 50% for very large numbers (5+ digits)
        } else {
            stackCountScale = 0.7f; // 70% for normal numbers (1-4 digits)
        }
        
        // Use absolute slot coordinates
        int slotX = this.leftPos + slot.x;
        int slotY = this.topPos + slot.y;
        
        // Use actual count from NBT
        String countText = String.valueOf(actualCount);
        
        // Calculate text size at desired scale
        int scaledTextWidth = (int)(this.font.width(countText) * stackCountScale);
        
        // Position text UNDER the slot, centered horizontally
        int finalX = slotX + (16 - scaledTextWidth) / 2;  // Centered horizontally in the slot
        int finalY = slotY + 20;  // 4 pixels below the slot (16 + 4), moved 2px lower
        
        // Apply scale only for text rendering
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(stackCountScale, stackCountScale, 1.0f);
        
        // Convert final position for scaled system
        int scaledX = (int)(finalX / stackCountScale);
        int scaledY = (int)(finalY / stackCountScale);
        
        // Draw dark text without shadow (dark gray)
        guiGraphics.drawString(this.font, countText, scaledX, scaledY, 0x404040, false);
        
        guiGraphics.pose().popPose();
    }
    
    /**
     * Reads actual count from item NBT
     */
    private int getActualCount(ItemStack stack) {
        var customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            var tag = customData.copyTag();
            return tag.getInt("ActualCount");
        }
        return stack.getCount(); // Fallback to normal count
    }
    
    /**
     * Handles click on Mode button
     */
         private void onModeButtonClicked() {
         isPlayerMode = !isPlayerMode;
         modeButton.setMessage(Component.translatable("gui.iska_utils.structure_saver.mode." + (isPlayerMode ? "player" : "normal")));
     }
     
     @Override
     public boolean mouseClicked(double mouseX, double mouseY, int button) {
         if (button == 0) { // Left click
             // Handle various clicks in priority order (like StructureSelectionScreen)
             if (handleScrollButtonClick(mouseX, mouseY) ||
                 handleSelectionButtonClick(mouseX, mouseY) ||
                 handleHandleClick(mouseX, mouseY) ||
                 handleScrollbarClick(mouseX, mouseY)) {
                 return true;
             }
         }
         
         return super.mouseClicked(mouseX, mouseY, button);
     }
     
     /**
      * Handles clicks on scroll buttons (up/down arrows)
      */
     private boolean handleScrollButtonClick(double mouseX, double mouseY) {
         int scrollbarX = this.leftPos + SCROLLBAR_X;
         int buttonUpY = this.topPos + BUTTON_UP_Y;
         int buttonDownY = this.topPos + BUTTON_DOWN_Y;
         
         // Up button (above scrollbar)
         if (mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE &&
             mouseY >= buttonUpY && mouseY < buttonUpY + HANDLE_SIZE) {
             scrollUp();
             return true;
         }
         
         // Down button (below scrollbar)
         if (mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE &&
             mouseY >= buttonDownY && mouseY < buttonDownY + HANDLE_SIZE) {
             scrollDown();
             return true;
         }
         
         return false;
     }
     
     /**
      * Handles clicks on selection buttons (circle)
      */
     private boolean handleSelectionButtonClick(double mouseX, double mouseY) {
         for (int i = 0; i < VISIBLE_ENTRIES; i++) {
             int entryIndex = scrollOffset + i;
             if (entryIndex >= clientStructures.size()) continue;
             
             int entryX = this.leftPos + ENTRIES_START_X;
             int entryY = this.topPos + ENTRIES_START_Y + i * ENTRY_HEIGHT;
             
             // Selection button position (must match EXACTLY renderSlotAndButton)
             int slotX = entryX + ENTRY_WIDTH - SLOT_SIZE - (BUTTON_SIZE * 2) - 8; // Same calculation as render
             int selectionButtonX = slotX + SLOT_SIZE + 2; // FIRST button (circle)
             int selectionButtonY = entryY + (ENTRY_HEIGHT - BUTTON_SIZE) / 2;
             
             if (mouseX >= selectionButtonX && mouseX < selectionButtonX + BUTTON_SIZE &&
                 mouseY >= selectionButtonY && mouseY < selectionButtonY + BUTTON_SIZE) {
                 
                 // Toggle selection with deselection
                 if (selectedStructureIndex == entryIndex) {
                     selectedStructureIndex = -1; // Deselect if already selected
                     // Clear EditBoxes when deselected
                     populateEditBoxesFromSelection();
                 } else {
                     selectedStructureIndex = entryIndex; // Select new
                     // Populate EditBoxes with selected structure data
                     populateEditBoxesFromSelection();
                 }
                 
                 playButtonSound();
                 return true;
             }
         }
         return false;
     }
     
     /**
      * Handles clicks on handle to start drag
      */
     private boolean handleHandleClick(double mouseX, double mouseY) {
         if (clientStructures.size() <= VISIBLE_ENTRIES) return false; // Not draggable if few structures
         
         int scrollbarX = this.leftPos + SCROLLBAR_X;
         int scrollbarY = this.topPos + SCROLLBAR_Y;
         
         float scrollRatio = (float) scrollOffset / (clientStructures.size() - VISIBLE_ENTRIES);
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
      * Handles clicks on scrollbar to jump to a position
      */
     private boolean handleScrollbarClick(double mouseX, double mouseY) {
         if (clientStructures.size() <= VISIBLE_ENTRIES) return false;
         
         int scrollbarX = this.leftPos + SCROLLBAR_X;
         int scrollbarY = this.topPos + SCROLLBAR_Y;
         
         if (mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_WIDTH &&
             mouseY >= scrollbarY && mouseY < scrollbarY + SCROLLBAR_HEIGHT) {
             
             // Calculate new scroll position based on click
             float clickRatio = (float)(mouseY - scrollbarY) / SCROLLBAR_HEIGHT;
             clickRatio = Math.max(0, Math.min(1, clickRatio)); // Clamp between 0 and 1
             
             int newScrollOffset = (int)(clickRatio * (clientStructures.size() - VISIBLE_ENTRIES));
             newScrollOffset = Math.max(0, Math.min(clientStructures.size() - VISIBLE_ENTRIES, newScrollOffset));
             
             if (newScrollOffset != scrollOffset) {
                 scrollOffset = newScrollOffset;
                 playButtonSound();
             }
             return true;
         }
         return false;
     }
     
     /**
      * Plays button click sound
      */
     private void playButtonSound() {
         if (this.minecraft != null) {
             this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
         }
     }
     
     /**
      * Prevents closing the GUI with inventory key when an EditBox is focused
      */
     @Override
     public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
         // Check if an EditBox is focused (either name or id)
         boolean isEditBoxFocused = (nameEditBox != null && nameEditBox.isFocused()) || 
                                    (idEditBox != null && idEditBox.isFocused());
         
         if (isEditBoxFocused) {
             // Let the focused EditBox handle the key first
             if (nameEditBox != null && nameEditBox.isFocused()) {
                 if (nameEditBox.keyPressed(keyCode, scanCode, modifiers)) {
                     return true;
                 }
             }
             if (idEditBox != null && idEditBox.isFocused()) {
                 if (idEditBox.keyPressed(keyCode, scanCode, modifiers)) {
                     return true;
                 }
             }
             
             // If inventory key is pressed while EditBox is focused, prevent closing
             // This works even if the user has changed the inventory key binding
             if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                 return true; // Prevent closing
             }
         }
         
         return super.keyPressed(keyCode, scanCode, modifiers);
     }
} 