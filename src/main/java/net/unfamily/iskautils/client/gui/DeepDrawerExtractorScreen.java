package net.unfamily.iskautils.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.DeepDrawerExtractorBlockEntity;
import net.unfamily.iskautils.network.ModMessages;

/**
 * Screen for Deep Drawer Extractor GUI
 * Shows scrollable EditBoxes for infinite filter fields, allow/deny button, and help text
 */
public class DeepDrawerExtractorScreen extends AbstractContainerScreen<DeepDrawerExtractorMenu> {
    
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/backgrounds/deep_drawer_extractor.png");
    private static final ResourceLocation BACKGROUND_EMPTY = ResourceLocation.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/backgrounds/deep_drawer_extractor_empty.png");
    
    // Medium buttons texture (16x32 - normal and highlighted)
    private static final ResourceLocation MEDIUM_BUTTONS = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/medium_buttons.png");
    // Redstone GUI icon
    private static final ResourceLocation REDSTONE_GUI = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/redstone_gui.png");
    // Scrollbar texture (identica a DeepDrawersScreen)
    private static final ResourceLocation SCROLLBAR_TEXTURE = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/scrollbar.png");
    // Wide entry texture for filter entries
    private static final ResourceLocation ENTRY_TEXTURE = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/entry_wide.png");
    // Single slot texture for item display
    private static final ResourceLocation SINGLE_SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/single_slot.png");
    
    // GUI dimensions (based on image: 330x250)
    private static final int GUI_WIDTH = 330;
    private static final int GUI_HEIGHT = 250;  
    
    // Title position
    private static final int TITLE_Y = 8;
    
    // Close button
    private Button closeButton;
    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;
    
    // Screen mode: false = main screen, true = how to use screen
    private boolean isHowToUseMode = false;
    
    // Entry dimensions and positions (scrollable)
    // ENTRY_WIDE is 140x24
    private static final int ENTRY_WIDTH = 140; // Width of entry_wide.png texture
    private static final int ENTRY_HEIGHT = 24; // Height of entry_wide.png texture
    private static final int ENTRY_X = 8;
    private static final int FILTERS_LABEL_Y = 30; // Y position for "Filters" label
    private static final int FIRST_ROW_Y = FILTERS_LABEL_Y + 12; // Start entries after "Filters" label (12px for label height + spacing)
    private static final int ENTRY_SPACING = 0; // No spacing between entries (they touch each other)
    private static final int BUFFER_SLOTS_Y = 134; // Y position of buffer slots (where entries should stop)
    private static final int MAX_FILTER_SLOTS = net.unfamily.iskautils.Config.deepDrawerExtractorMaxFilters; // Total filter slots in BlockEntity (from config, default 50)
    private static final int VISIBLE_ENTRIES = 7; // Number of entries visible at once (scrollable) - reduced because wide entries are taller
    
    // Scrollbar constants (identical to DeepDrawersScreen)
    private static final int SCROLLBAR_WIDTH = 8;      // Width of each scrollbar element
    private static final int SCROLLBAR_HEIGHT = 34;    // Height of scrollbar background in texture
    private static final int HANDLE_SIZE = 8;          // Size of UP/DOWN buttons and handle
    private static final int SCROLLBAR_X = ENTRY_X + ENTRY_WIDTH + 4; // After entry + spacing (8 + 140 + 4 = 152)
    private static final int BUTTON_UP_Y = FIRST_ROW_Y; // Aligned with first entry (after "Filters" label)
    private static final int SCROLLBAR_Y = BUTTON_UP_Y + HANDLE_SIZE; // Scrollbar starts after UP button
    private static final int BUTTON_DOWN_Y = SCROLLBAR_Y + SCROLLBAR_HEIGHT; // DOWN button after scrollbar
    
    // Button dimensions and position (moved right, same spacing from right edge as entries from left edge)
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_X = GUI_WIDTH - BUTTON_WIDTH - 8; // 8px from right edge (same as entries from left)
    private static final int BUTTON_Y = FIRST_ROW_Y; // Aligned with first EditBox (after "Filters" label)
    
    // How to use button
    private Button howToUseButton;
    private static final int HOW_TO_USE_BUTTON_Y = BUTTON_Y; // Same Y as first EditBox
    
    // Redstone mode button
    private int redstoneModeButtonX, redstoneModeButtonY;
    private static final int REDSTONE_BUTTON_SIZE = 16;
    
    // Allow/Deny button (whitelist/blacklist toggle) - wider button, same height as redstone
    private Button modeButton;
    private static final int MODE_BUTTON_HEIGHT = 16;
    private static final int MODE_BUTTON_WIDTH = 60; // Wider button
    private static final int BUTTON_SPACING = 4; // Space between redstone and mode button
    
    // Track current mode locally (for immediate UI feedback on button click)
    private boolean isWhitelistMode = false;
    
    // Cached filter fields for rendering (synced from server) - dynamic list
    private java.util.List<String> cachedFilterFields = new java.util.ArrayList<>();
    
    // Scroll state for filter EditBoxes (identical to DeepDrawersScreen)
    private int filterScrollOffset = 0;
    private boolean isDraggingHandle = false;
    private int dragStartY = 0;
    private int dragStartScrollOffset = 0;
    
    // EditBox for editing filter (shown when clicking on an entry)
    private EditBox editingEditBox = null;
    private int editingFilterIndex = -1;
    
    // Edit mode: tracks which filter index is in edit mode (shows different view)
    private int editModeFilterIndex = -1; // -1 means no entry is in edit mode
    
    // Edit buttons for each visible entry (recreated on scroll)
    private final java.util.List<Button> editButtons = new java.util.ArrayList<>();
    
    // Edit mode UI elements
    private ItemStack ghostSlotItem = ItemStack.EMPTY; // Ghost slot item (copy, doesn't consume)
    private EditBox editModeTextBox = null; // Textbox that appears in edit mode
    private Button leftArrowButton = null; // Left arrow button
    private Button rightArrowButton = null; // Right arrow button
    private java.util.List<String> filterVariants = new java.util.ArrayList<>(); // All possible filter variants for current item
    private int currentFilterVariantIndex = 0; // Current index in filterVariants list
    
    public DeepDrawerExtractorScreen(DeepDrawerExtractorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        // Hide inventory label in how to use mode (will be set dynamically)
        this.inventoryLabelY = 10000; // Hidden by default, will be adjusted in updateWidgetVisibility
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Load initial mode from ContainerData (like rotation in StructurePlacerMachineScreen)
        isWhitelistMode = menu.getWhitelistMode();
        
        // Initialize cached filter fields (will be updated in containerTick)
        cachedFilterFields.clear();
        
        // Create edit buttons for visible entries
        updateEditButtons();
        
        // Close button
        closeButton = Button.builder(Component.literal("✕"), 
                                    button -> onCloseButtonClicked())
                           .bounds(this.leftPos + CLOSE_BUTTON_X, this.topPos + CLOSE_BUTTON_Y, 
                                  CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
                           .build();
        addRenderableWidget(closeButton);
        
        // How to use button (moved left, same spacing from edge as entries)
        howToUseButton = Button.builder(Component.translatable("gui.iska_utils.deep_drawer_extractor.how_to_use"), 
                                       button -> onHowToUseButtonClicked())
                              .bounds(this.leftPos + BUTTON_X, this.topPos + HOW_TO_USE_BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
                              .build();
        addRenderableWidget(howToUseButton);
        
        // Redstone mode button (below how to use)
        this.redstoneModeButtonX = this.leftPos + BUTTON_X;
        this.redstoneModeButtonY = this.topPos + BUTTON_Y + BUTTON_HEIGHT + 4; // 4px below how to use
        
        // Allow/Deny button (wider, to the left of redstone button, same Y and height)
        int modeButtonX = this.leftPos + BUTTON_X; // Start from same X as how to use
        int modeButtonY = this.topPos + BUTTON_Y + BUTTON_HEIGHT + 4; // Same Y as redstone button
        // Initialize with current mode from ContainerData (will be updated in containerTick)
        Component buttonText = isWhitelistMode
                ? Component.translatable("gui.iska_utils.deep_drawer_extractor.mode.allow")
                : Component.translatable("gui.iska_utils.deep_drawer_extractor.mode.deny");
        
        modeButton = Button.builder(buttonText, button -> onModeButtonClicked())
                .bounds(modeButtonX, modeButtonY, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT)
                .build();
        addRenderableWidget(modeButton);
        
        // Update redstone button X position to be after mode button with spacing
        this.redstoneModeButtonX = modeButtonX + MODE_BUTTON_WIDTH + BUTTON_SPACING;
        
        // Back button (for how to use screen)
        backButton = Button.builder(Component.translatable("gui.iska_utils.deep_drawer_extractor.back"), 
                                   button -> onBackButtonClicked())
                          .bounds(this.leftPos + BACK_BUTTON_X, this.topPos + BACK_BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
                          .build();
        addRenderableWidget(backButton);
        
        // Update visibility based on current mode
        updateWidgetVisibility();
    }
    
    private void onCloseButtonClicked() {
        // If in edit mode, exit edit mode instead of closing
        if (editModeFilterIndex >= 0) {
            playButtonSound();
            exitEditMode();
            return;
        }
        
        if (isHowToUseMode) {
            // In how to use mode, close button acts as back
            playButtonSound();
            switchToMainScreen();
        } else {
            // In main mode, close button closes GUI (no sound)
            this.onClose();
        }
    }
    
    private void onHowToUseButtonClicked() {
        playButtonSound();
        switchToHowToUseScreen();
    }
    
    private void onBackButtonClicked() {
        playButtonSound();
        switchToMainScreen();
    }
    
    private void switchToHowToUseScreen() {
        isHowToUseMode = true;
        updateWidgetVisibility();
    }
    
    private void switchToMainScreen() {
        isHowToUseMode = false;
        updateWidgetVisibility();
    }
    
    private void updateWidgetVisibility() {
        // Update visibility of widgets based on current mode
        boolean showMain = !isHowToUseMode;
        
        // Hide inventory label in how to use mode
        this.inventoryLabelY = isHowToUseMode ? 10000 : 165 - this.font.lineHeight - 2;
        
        // Update buttons
        if (howToUseButton != null) {
            howToUseButton.visible = showMain;
        }
        if (modeButton != null) {
            modeButton.visible = showMain;
        }
        if (backButton != null) {
            backButton.visible = isHowToUseMode;
        }
        // Redstone button is always visible in main mode (rendered in renderBg)
        
        // Hide editing EditBox in how to use mode
        if (!showMain && editingEditBox != null) {
            editingEditBox.visible = false;
        }
        
        // Update edit buttons visibility
        if (showMain) {
            updateEditButtons();
        } else {
            // Hide all edit buttons in how to use mode
            for (Button button : editButtons) {
                if (button != null) {
                    button.visible = false;
                }
            }
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check if clicking on an example in how to use mode
        if (button == 0 && isHowToUseMode) { // Left click in how to use mode
            for (ExampleData exampleData : exampleDataList) {
                int screenX = this.leftPos + exampleData.x;
                int screenY = this.topPos + exampleData.y;
                
                if (mouseX >= screenX && mouseX <= screenX + exampleData.width &&
                    mouseY >= screenY && mouseY <= screenY + HELP_TEXT_LINE_HEIGHT) {
                    
                    // Copy to clipboard
                    if (this.minecraft != null && this.minecraft.keyboardHandler != null) {
                        this.minecraft.keyboardHandler.setClipboard(exampleData.example);
                        playButtonSound();
                    }
                    return true;
                }
            }
        }
        
        if (button == 0 && !isHowToUseMode) { // Left click, only in main mode
            // Handle ghost slot click (if in edit mode) - prioritize this
            if (editModeFilterIndex >= 0) {
                // Position: align horizontally with second column of player inventory (x = 159 + 18 = 177)
                int inventoryStartX = 159; // First column of inventory
                int slotColumn = 1; // Second column (0-indexed: 0=first, 1=second)
                int slotSize = 18;
                int slotX = this.leftPos + inventoryStartX + slotColumn * 18 - 1; // Second column, -1px left
                int slotY = this.topPos + 100 - 1; // Positioned higher, not aligned vertically with inventory, -1px up
                
                if (mouseX >= slotX && mouseX < slotX + slotSize &&
                    mouseY >= slotY && mouseY < slotY + slotSize) {
                    // Click is on ghost slot
                    handleGhostSlotClick();
                    return true;
                }
            }
            
            // Handle scrollbar clicks first (they have priority)
            if (handleScrollButtonClick(mouseX, mouseY)) {
                return true;
            }
            
            // Handle handle drag start
            if (handleHandleClick(mouseX, mouseY)) {
                return true;
            }
            
            // Handle scrollbar area clicks
            if (handleScrollbarClick(mouseX, mouseY)) {
                return true;
            }
            
            // Check if click is on redstone mode button
            if (mouseX >= this.redstoneModeButtonX && mouseX <= this.redstoneModeButtonX + REDSTONE_BUTTON_SIZE &&
                mouseY >= this.redstoneModeButtonY && mouseY <= this.redstoneModeButtonY + REDSTONE_BUTTON_SIZE) {
                
                onRedstoneModePressed();
                return true;
            }
        }
        
        // Handle clicks on filter entries (left click to edit, right click to clear)
        // But prioritize edit button clicks - if click is on edit button area, let the button handle it
        if (!isHowToUseMode && button == 0) {
            // Check if click is on a filter entry
            for (int i = 0; i < VISIBLE_ENTRIES; i++) {
                int filterIndex = filterScrollOffset + i;
                if (filterIndex >= MAX_FILTER_SLOTS) {
                    break;
                }
                
                int entryX = this.leftPos + ENTRY_X;
                int entryY = this.topPos + FIRST_ROW_Y + i * (ENTRY_HEIGHT + ENTRY_SPACING);
                
                // Check if click is within entry bounds
                if (mouseX >= entryX && mouseX < entryX + ENTRY_WIDTH &&
                    mouseY >= entryY && mouseY < entryY + ENTRY_HEIGHT) {
                    
                    // Check if click is on edit button area (prioritize button)
                    int editButtonSize = 12;
                    int editButtonMargin = 5;
                    int editButtonX = entryX + ENTRY_WIDTH - editButtonMargin - editButtonSize;
                    int editButtonY = entryY + (ENTRY_HEIGHT - editButtonSize) / 2;
                    
                    if (mouseX >= editButtonX && mouseX < editButtonX + editButtonSize &&
                        mouseY >= editButtonY && mouseY < editButtonY + editButtonSize) {
                        // Click is on edit button - handle it directly
                        playButtonSound();
                        onEditButtonClicked(filterIndex);
                        return true; // Consume the click
                    }
                    
                    // Click is on entry but not on edit button - start editing
                    startEditingFilter(filterIndex);
                    return true;
                }
            }
        }
        
        // Handle right click on entries (clear filter)
        if (!isHowToUseMode && button == 1) {
            // First check if right click is on edit mode textbox
            if (editModeTextBox != null && editModeFilterIndex >= 0) {
                int textBoxX = editModeTextBox.getX();
                int textBoxY = editModeTextBox.getY();
                int textBoxWidth = editModeTextBox.getWidth();
                int textBoxHeight = editModeTextBox.getHeight();
                
                if (mouseX >= textBoxX && mouseX < textBoxX + textBoxWidth &&
                    mouseY >= textBoxY && mouseY < textBoxY + textBoxHeight) {
                    // Right click on edit mode textbox: clear it
                    editModeTextBox.setValue("");
                    editModeTextBox.setCursorPosition(0);
                    editModeTextBox.setHighlightPos(0);
                    // Save empty filter
                    if (editModeFilterIndex >= 0) {
                        while (cachedFilterFields.size() <= editModeFilterIndex) {
                            cachedFilterFields.add("");
                        }
                        cachedFilterFields.set(editModeFilterIndex, "");
                        saveFilterData();
                    }
                    // Clear ghost slot and variants
                    ghostSlotItem = ItemStack.EMPTY;
                    filterVariants.clear();
                    currentFilterVariantIndex = 0;
                    return true;
                }
            }
            
            // Then check entries
            for (int i = 0; i < VISIBLE_ENTRIES; i++) {
                int filterIndex = filterScrollOffset + i;
                if (filterIndex >= MAX_FILTER_SLOTS) {
                    break;
                }
                
                int entryX = this.leftPos + ENTRY_X;
                int entryY = this.topPos + FIRST_ROW_Y + i * (ENTRY_HEIGHT + ENTRY_SPACING);
                
                // Check if click is within entry bounds
                if (mouseX >= entryX && mouseX < entryX + ENTRY_WIDTH &&
                    mouseY >= entryY && mouseY < entryY + ENTRY_HEIGHT) {
                    
                    // Right click: clear filter
                    while (cachedFilterFields.size() < MAX_FILTER_SLOTS) {
                        cachedFilterFields.add("");
                    }
                    cachedFilterFields.set(filterIndex, "");
                    saveFilterData();
                    return true;
                }
            }
        }
        
        // Let super handle other clicks (buttons, etc.)
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private void playButtonSound() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
    
    private void onModeButtonClicked() {
        // Get the machine position from the menu (synced from server, like rotation)
        BlockPos machinePos = menu.getSyncedBlockPos();
        
        // If synced position is ZERO, try fallback methods (like StructurePlacerMachineScreen.onShowPressed)
        if (machinePos.equals(net.minecraft.core.BlockPos.ZERO)) {
            // Try to get position from block entity as fallback
            if (this.minecraft != null && this.minecraft.level != null) {
                DeepDrawerExtractorBlockEntity blockEntity = menu.getBlockEntityFromLevel(this.minecraft.level);
                if (blockEntity != null) {
                    machinePos = blockEntity.getBlockPos();
                }
            }
        }
        
        if (!machinePos.equals(net.minecraft.core.BlockPos.ZERO)) {
            // Send mode toggle packet to toggle whitelist/blacklist mode (like rotation)
            // Do NOT call saveFilterData() here - it would overwrite the mode change with old local state
            ModMessages.sendDeepDrawerExtractorModeTogglePacket(machinePos);
        }
    }
    
    private void onRedstoneModePressed() {
        // Try multiple methods to get the machine position (like StructurePlacerMachineScreen.onRedstoneModePressed)
        BlockPos blockPos = menu.getSyncedBlockPos();
        
        // If getSyncedBlockPos returns ZERO, try getBlockPos
        if (blockPos.equals(net.minecraft.core.BlockPos.ZERO)) {
            blockPos = menu.getBlockPos();
        }
        
        // If still ZERO, try to find the machine by searching nearby
        if (blockPos.equals(net.minecraft.core.BlockPos.ZERO) && this.minecraft != null && this.minecraft.level != null && this.minecraft.player != null) {
            BlockPos playerPos = this.minecraft.player.blockPosition();
            
            // Search in a 16x16x16 area around player for the machine
            for (int x = -8; x <= 8; x++) {
                for (int y = -8; y <= 8; y++) {
                    for (int z = -8; z <= 8; z++) {
                        BlockPos searchPos = playerPos.offset(x, y, z);
                        if (this.minecraft.level.getBlockEntity(searchPos) instanceof DeepDrawerExtractorBlockEntity) {
                            blockPos = searchPos;
                            break;
                        }
                    }
                    if (!blockPos.equals(net.minecraft.core.BlockPos.ZERO)) break;
                }
                if (!blockPos.equals(net.minecraft.core.BlockPos.ZERO)) break;
            }
        }
        
        if (!blockPos.equals(net.minecraft.core.BlockPos.ZERO)) {
            // Send redstone mode packet to cycle the mode
            ModMessages.sendDeepDrawerExtractorRedstoneModePacket(blockPos);
            playButtonSound();
        }
        
        // Save filter data to ensure all changes are persisted
        saveFilterData();
    }
    
    /**
     * Saves filter data to server
     * Uses the same pattern as onModeButtonClicked() - get position with fallback
     * Always reads whitelist mode from synced ContainerData (not local state)
     */
    private void saveFilterData() {
        // Get the machine position from the menu (synced from server, like rotation)
        BlockPos machinePos = menu.getSyncedBlockPos();
        
        // If synced position is ZERO, try fallback methods (like StructurePlacerMachineScreen.onShowPressed)
        if (machinePos.equals(net.minecraft.core.BlockPos.ZERO)) {
            // Try to get position from block entity as fallback
            if (this.minecraft != null && this.minecraft.level != null) {
                DeepDrawerExtractorBlockEntity blockEntity = menu.getBlockEntityFromLevel(this.minecraft.level);
                if (blockEntity != null) {
                    machinePos = blockEntity.getBlockPos();
                }
            }
        }
        
        if (!machinePos.equals(net.minecraft.core.BlockPos.ZERO)) {
            // Collect filter field values as index-value pairs (only non-empty filters)
            java.util.Map<Integer, String> filterMap = new java.util.HashMap<>();
            for (int i = 0; i < cachedFilterFields.size(); i++) {
                String filter = cachedFilterFields.get(i);
                if (filter != null && !filter.trim().isEmpty()) {
                    filterMap.put(i, filter.trim());
                }
            }
            
            // Always read whitelist mode from synced ContainerData (not local isWhitelistMode)
            // This ensures we use the server-authoritative value, not stale local state
            boolean currentWhitelistMode = menu.getWhitelistMode();
            
            // Send to server via packet
            ModMessages.sendDeepDrawerExtractorFilterUpdatePacket(machinePos, filterMap, currentWhitelistMode);
        }
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        // Use different background for "how to use" mode
        ResourceLocation backgroundTexture = isHowToUseMode ? BACKGROUND_EMPTY : BACKGROUND;
        guiGraphics.blit(backgroundTexture, x, y, 0, 0, this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);
        
        // Render "Filters" label (only in main mode, not in how to use)
        if (!isHowToUseMode) {
            Component filtersLabel = Component.translatable("gui.iska_utils.deep_drawer_extractor.filters");
            int labelWidth = this.font.width(filtersLabel);
            // Center the label with the entries
            int labelX = this.leftPos + ENTRY_X + (ENTRY_WIDTH - labelWidth) / 2;
            guiGraphics.drawString(this.font, filtersLabel, labelX, this.topPos + FILTERS_LABEL_Y, 0x404040, false);
            
            // Render filter entries (wide entries with single slot)
            renderFilterEntries(guiGraphics, mouseX, mouseY);
        }
        
        // Render redstone mode button (only in main mode, not in how to use)
        if (!isHowToUseMode) {
            renderRedstoneModeButton(guiGraphics, mouseX, mouseY);
            // Render scrollbar (only in main mode, not in how to use)
            renderScrollbar(guiGraphics, mouseX, mouseY);
            
            // Render edit mode UI (ghost slot and textbox) if in edit mode
            if (editModeFilterIndex >= 0) {
                renderEditModeUI(guiGraphics, mouseX, mouseY);
            }
        }
    }
    
    /**
     * Renders filter entries as wide entries with single slot
     */
    private void renderFilterEntries(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Ensure cachedFilterFields has all slots
        while (cachedFilterFields.size() < MAX_FILTER_SLOTS) {
            cachedFilterFields.add("");
        }
        
        // Render visible entries
        for (int i = 0; i < VISIBLE_ENTRIES; i++) {
            int filterIndex = filterScrollOffset + i;
            if (filterIndex >= MAX_FILTER_SLOTS) {
                break;
            }
            
            int entryX = this.leftPos + ENTRY_X;
            int entryY = this.topPos + FIRST_ROW_Y + i * (ENTRY_HEIGHT + ENTRY_SPACING);
            
            // Draw entry background
            guiGraphics.blit(ENTRY_TEXTURE, entryX, entryY, 0, 0, ENTRY_WIDTH, ENTRY_HEIGHT, ENTRY_WIDTH, ENTRY_HEIGHT);
            
            // Get filter value
            String filter = cachedFilterFields.get(filterIndex);
            if (filter == null) {
                filter = "";
            }
            
            // Render entry content (slot + text)
            renderFilterEntry(guiGraphics, entryX, entryY, filter, filterIndex, mouseX, mouseY);
        }
    }
    
    /**
     * Renders a single filter entry with slot and text
     */
    private void renderFilterEntry(GuiGraphics guiGraphics, int entryX, int entryY, String filter, int filterIndex, int mouseX, int mouseY) {
        // Check if this entry is in edit mode
        boolean isEditMode = (editModeFilterIndex == filterIndex);
        
        // For now, render the same view regardless of edit mode
        // In future phases, this will render a different view when isEditMode is true
        
        // Slot position (3px from left edge, 3px from top)
        int slotX = entryX + 3;
        int slotY = entryY + 3;
        
        // Draw single slot
        guiGraphics.blit(SINGLE_SLOT_TEXTURE, slotX, slotY, 0, 0, 18, 18, 18, 18);
        
        // Get item to display based on filter type
        ItemStack displayItem = getDisplayItemForFilter(filter);
        if (!displayItem.isEmpty()) {
            guiGraphics.renderItem(displayItem, slotX + 1, slotY + 1);
            guiGraphics.renderItemDecorations(this.font, displayItem, slotX + 1, slotY + 1);
        }
        
        // Text position (after slot + 6px margin)
        int textX = slotX + 18 + 6;
        int textY = entryY + (ENTRY_HEIGHT - this.font.lineHeight) / 2;
        
        // Edit button position (right side, but not on the edge)
        int editButtonSize = 12; // Small button
        int editButtonMargin = 5; // Margin from right edge (not on border)
        int editButtonX = entryX + ENTRY_WIDTH - editButtonMargin - editButtonSize;
        
        // Calculate available width for text (up to edit button)
        int maxTextWidth = editButtonX - textX - 5; // 5px margin before edit button
        
        // Render filter text (truncate if too long)
        String displayText = filter.isEmpty() ? "" : filter;
        int textWidth = this.font.width(displayText);
        if (textWidth > maxTextWidth && !displayText.isEmpty()) {
            // Truncate with ellipsis
            displayText = this.font.plainSubstrByWidth(displayText, maxTextWidth - this.font.width("...")) + "...";
        }
        
        guiGraphics.drawString(this.font, displayText, textX, textY, 0x404040, false);
        
        // Edit button is rendered as a widget (created in updateEditButtons)
    }
    
    /**
     * Renders the edit mode UI (ghost slot and textbox)
     */
    private void renderEditModeUI(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Position: align horizontally with second column of player inventory (x = 159 + 18 = 177)
        // Player inventory starts at x=159 (from DeepDrawerExtractorMenu)
        int inventoryStartX = 159; // First column of inventory
        int slotColumn = 1; // Second column (0-indexed: 0=first, 1=second)
        int slotSize = 18;
        int slotX = this.leftPos + inventoryStartX + slotColumn * 18 - 1; // Second column, -1px left
        int slotY = this.topPos + 100 - 1; // Positioned higher, not aligned vertically with inventory, -1px up
        
        // Draw slot background
        guiGraphics.blit(SINGLE_SLOT_TEXTURE, slotX, slotY, 0, 0, slotSize, slotSize, slotSize, slotSize);
        
        // Render ghost slot item if present
        if (!ghostSlotItem.isEmpty()) {
            guiGraphics.renderItem(ghostSlotItem, slotX + 1, slotY + 1);
            guiGraphics.renderItemDecorations(this.font, ghostSlotItem, slotX + 1, slotY + 1);
        }
        
        // Buttons and textbox are rendered as widgets (created in createEditModeUI)
    }
    
    /**
     * Gets the display item for a filter based on its type
     * Returns appropriate item for ID, tag, mod, NBT, or macro filters
     */
    private ItemStack getDisplayItemForFilter(String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            return ItemStack.EMPTY;
        }
        
        filter = filter.trim();
        
        // ID filter: -minecraft:diamond
        if (filter.startsWith("-")) {
            String idFilter = filter.substring(1);
            try {
                net.minecraft.resources.ResourceLocation itemId = net.minecraft.resources.ResourceLocation.parse(idFilter);
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId);
                return new ItemStack(item);
            } catch (Exception e) {
                return ItemStack.EMPTY;
            }
        }
        
        // Tag filter: #c:ingots
        if (filter.startsWith("#")) {
            String tagFilter = filter.substring(1);
            return getItemForTag(tagFilter);
        }
        
        // Mod ID filter: @iska_utils
        if (filter.startsWith("@")) {
            String modIdFilter = filter.substring(1);
            return getItemForMod(modIdFilter);
        }
        
        // NBT filter: ?"apotheosis:rarity":"apotheosis:mythic"
        if (filter.startsWith("?")) {
            // For NBT filters, show knowledge book (green recipe book)
            return new ItemStack(net.minecraft.world.item.Items.KNOWLEDGE_BOOK);
        }
        
        // Macro filter: &enchanted, &damaged
        if (filter.startsWith("&")) {
            String macroFilter = filter.substring(1).toLowerCase();
            return switch (macroFilter) {
                case "enchanted" -> {
                    // Parse item from raw SNBT string
                    String snbtString = "{components:{\"minecraft:enchantments\":{levels:{\"minecraft:aqua_affinity\":1}},\"minecraft:repair_cost\":1},count:1,id:\"minecraft:iron_pickaxe\"}";
                    ItemStack stack = parseItemStackFromSNBT(snbtString);
                    if (stack.isEmpty()) {
                        // Fallback to diamond pickaxe with efficiency if parsing fails
                        stack = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
                    }
                    yield stack;
                }
                case "damaged" -> {
                    // Return a damaged item
                    ItemStack stack = new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
                    stack.setDamageValue(stack.getMaxDamage() / 2);
                    yield stack;
                }
                default -> ItemStack.EMPTY;
            };
        }
        
        // Default: treat as direct ID match (without prefix)
        try {
            net.minecraft.resources.ResourceLocation itemId = net.minecraft.resources.ResourceLocation.parse(filter);
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId);
            return new ItemStack(item);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
    
    /**
     * Parses an ItemStack from a raw SNBT (String NBT) string
     * @param snbtString The SNBT string representing the item
     * @return ItemStack parsed from the SNBT, or ItemStack.EMPTY if parsing fails
     */
    private ItemStack parseItemStackFromSNBT(String snbtString) {
        try {
            // Parse SNBT string to CompoundTag
            CompoundTag nbtTag = TagParser.parseTag(snbtString);
            
            // Get registry access from Minecraft level (required for ItemStack.parse)
            if (Minecraft.getInstance().level == null) {
                return ItemStack.EMPTY; // Cannot parse without level/registry access
            }
            
            net.minecraft.core.HolderLookup.Provider registryAccess = Minecraft.getInstance().level.registryAccess();
            
            // Parse ItemStack from CompoundTag
            return ItemStack.parse(registryAccess, nbtTag).orElse(ItemStack.EMPTY);
        } catch (Exception e) {
            // If parsing fails, return empty stack
            return ItemStack.EMPTY;
        }
    }
    
    /**
     * Gets an item that has the specified tag (for cyclic display)
     */
    private ItemStack getItemForTag(String tagId) {
        try {
            net.minecraft.resources.ResourceLocation tagLocation = net.minecraft.resources.ResourceLocation.parse(tagId);
            net.minecraft.tags.TagKey<net.minecraft.world.item.Item> itemTag = 
                net.minecraft.tags.ItemTags.create(tagLocation);
            
            // Get tag contents
            var tagContents = net.minecraft.core.registries.BuiltInRegistries.ITEM.getTag(itemTag);
            if (tagContents.isPresent()) {
                var items = tagContents.get();
                if (items.size() > 0) {
                    // Use cyclic index based on tick time for rotation
                    int index = (int)((System.currentTimeMillis() / 2000) % items.size()); // Change every 2 seconds
                    var itemHolder = items.get(index);
                    return new ItemStack(itemHolder.value());
                }
            }
        } catch (Exception e) {
            // Invalid tag, ignore
        }
        return ItemStack.EMPTY;
    }
    
    /**
     * Gets an item from the specified mod (for cyclic display)
     */
    private ItemStack getItemForMod(String modId) {
        // Find all items from this mod
        java.util.List<net.minecraft.world.item.Item> modItems = new java.util.ArrayList<>();
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            net.minecraft.resources.ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            if (itemId != null && itemId.getNamespace().startsWith(modId)) {
                modItems.add(item);
            }
        }
        
        if (!modItems.isEmpty()) {
            // Use cyclic index based on tick time for rotation
            int index = (int)((System.currentTimeMillis() / 2000) % modItems.size()); // Change every 2 seconds
            return new ItemStack(modItems.get(index));
        }
        
        return ItemStack.EMPTY;
    }
    
    /**
     * Renders the scrollbar with UP/DOWN buttons and draggable handle
     * Identical to DeepDrawersScreen implementation
     */
    private void renderScrollbar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Only show scrollbar if there are more slots than can fit
        if (MAX_FILTER_SLOTS <= VISIBLE_ENTRIES) return;
        
        int guiX = this.leftPos;
        int guiY = this.topPos;
        
        // Draw scrollbar background (8 pixels wide, height as defined)
        guiGraphics.blit(SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, guiY + SCROLLBAR_Y, 0, 0, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT, 32, 34);
        
        // UP button (8x8 pixels) - above scrollbar
        boolean upButtonHovered = (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH &&
                                  mouseY >= guiY + BUTTON_UP_Y && mouseY < guiY + BUTTON_UP_Y + HANDLE_SIZE);
        int upButtonV = upButtonHovered ? HANDLE_SIZE : 0;
        guiGraphics.blit(SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, guiY + BUTTON_UP_Y, SCROLLBAR_WIDTH * 2, upButtonV, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        
        // DOWN button (8x8 pixels) - below scrollbar
        boolean downButtonHovered = (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH &&
                                    mouseY >= guiY + BUTTON_DOWN_Y && mouseY < guiY + BUTTON_DOWN_Y + HANDLE_SIZE);
        int downButtonV = downButtonHovered ? HANDLE_SIZE : 0;
        guiGraphics.blit(SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, guiY + BUTTON_DOWN_Y, SCROLLBAR_WIDTH * 3, downButtonV, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        
        // Handle (8x8 pixels) - position based on scroll offset
        int maxScrollOffset = Math.max(0, MAX_FILTER_SLOTS - VISIBLE_ENTRIES);
        if (maxScrollOffset > 0) {
            double scrollRatio = (double) filterScrollOffset / maxScrollOffset;
            int handleY = guiY + SCROLLBAR_Y + (int)(scrollRatio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
            
            boolean handleHovered = (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + HANDLE_SIZE &&
                                    mouseY >= handleY && mouseY < handleY + HANDLE_SIZE);
            int handleTextureY = handleHovered ? HANDLE_SIZE : 0;
            guiGraphics.blit(SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, handleY, SCROLLBAR_WIDTH, handleTextureY, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        }
    }
    
    /**
     * Updates edit buttons for visible entries
     */
    private void updateEditButtons() {
        // Remove all existing edit buttons
        for (Button button : editButtons) {
            if (button != null) {
                removeWidget(button);
            }
        }
        editButtons.clear();
        
        if (isHowToUseMode) {
            return; // Don't create buttons in how to use mode
        }
        
        // Create edit buttons for visible entries
        int editButtonSize = 12;
        int editButtonMargin = 5; // Margin from right edge
        
        for (int i = 0; i < VISIBLE_ENTRIES; i++) {
            int filterIndex = filterScrollOffset + i;
            if (filterIndex >= MAX_FILTER_SLOTS) {
                break;
            }
            
            int entryX = this.leftPos + ENTRY_X;
            int entryY = this.topPos + FIRST_ROW_Y + i * (ENTRY_HEIGHT + ENTRY_SPACING);
            int editButtonX = entryX + ENTRY_WIDTH - editButtonMargin - editButtonSize;
            int editButtonY = entryY + (ENTRY_HEIGHT - editButtonSize) / 2;
            
            final int finalFilterIndex = filterIndex;
            Button editButton = Button.builder(Component.literal("✎"), 
                button -> {
                    playButtonSound();
                    onEditButtonClicked(finalFilterIndex);
                })
                .bounds(editButtonX, editButtonY, editButtonSize, editButtonSize)
                .build();
            
            editButtons.add(editButton);
            addRenderableWidget(editButton);
        }
    }
    
    /**
     * Handles edit button click - switches to edit mode for the filter
     */
    private void onEditButtonClicked(int filterIndex) {
        // Toggle edit mode for this filter
        if (editModeFilterIndex == filterIndex) {
            // Already in edit mode, exit edit mode
            exitEditMode();
        } else {
            // Enter edit mode for this filter
            enterEditMode(filterIndex);
        }
    }
    
    /**
     * Enters edit mode for the specified filter
     */
    private void enterEditMode(int filterIndex) {
        editModeFilterIndex = filterIndex;
        createEditModeUI();
    }
    
    /**
     * Exits edit mode
     */
    private void exitEditMode() {
        editModeFilterIndex = -1;
        removeEditModeUI();
    }
    
    /**
     * Creates the edit mode UI (textbox and ghost slot)
     */
    private void createEditModeUI() {
        // Remove existing edit mode UI if any
        removeEditModeUI();
        
        // Position: align horizontally with second column of player inventory (x = 159 + 18 = 177)
        // Player inventory starts at x=159 (from DeepDrawerExtractorMenu)
        int inventoryStartX = 159; // First column of inventory
        int slotColumn = 1; // Second column (0-indexed: 0=first, 1=second)
        int slotSize = 18;
        int slotX = this.leftPos + inventoryStartX + slotColumn * 18 - 1; // Second column, -1px left
        int slotY = this.topPos + 100 - 1; // Positioned higher, not aligned vertically with inventory, -1px up
        
        // Button size and spacing (small buttons)
        int buttonSize = 12; // Small buttons
        int buttonSpacing = 2; // Space between button and slot
        
        // Left arrow button (to the left of slot)
        int leftButtonX = slotX - buttonSize - buttonSpacing;
        int leftButtonY = slotY + (slotSize - buttonSize) / 2; // Center vertically with slot
        
        leftArrowButton = Button.builder(Component.literal("←"), 
            button -> {
                playButtonSound();
                cycleFilterVariant(-1); // Previous variant
            })
            .bounds(leftButtonX, leftButtonY, buttonSize, buttonSize)
            .build();
        addRenderableWidget(leftArrowButton);
        
        // Right arrow button (to the right of slot)
        int rightButtonX = slotX + slotSize + buttonSpacing;
        int rightButtonY = slotY + (slotSize - buttonSize) / 2; // Center vertically with slot
        
        rightArrowButton = Button.builder(Component.literal("→"), 
            button -> {
                playButtonSound();
                cycleFilterVariant(1); // Next variant
            })
            .bounds(rightButtonX, rightButtonY, buttonSize, buttonSize)
            .build();
        addRenderableWidget(rightArrowButton);
        
        // Textbox position (to the right of right arrow button)
        int textBoxSpacing = 2; // Space between button and textbox
        int textBoxX = rightButtonX + buttonSize + textBoxSpacing;
        int textBoxY = slotY + (slotSize - 15) / 2; // Center vertically with slot (15 is textbox height)
        int textBoxHeight = 15;
        
        // Calculate max width for textbox
        // Don't exceed the end of inventory slots (9 columns * 18px = 162px from inventory start)
        int inventoryEndX = this.leftPos + inventoryStartX + (9 * 18); // End of inventory slots
        int rightEdge = this.leftPos + GUI_WIDTH;
        int margin = 5; // Margin from right edge
        // Maximum width is the minimum between: end of inventory slots, right edge margin, and default 120px
        int maxTextWidthFromInventory = inventoryEndX - textBoxX;
        int maxTextWidthFromEdge = rightEdge - textBoxX - margin;
        int textBoxWidth = Math.min(120, Math.min(maxTextWidthFromInventory, maxTextWidthFromEdge));
        
        // Create textbox
        editModeTextBox = new EditBox(this.font, textBoxX, textBoxY, textBoxWidth, textBoxHeight,
            Component.literal("Edit Filter"));
        
        // Set initial value from cached filter fields
        if (editModeFilterIndex >= 0 && editModeFilterIndex < cachedFilterFields.size()) {
            String currentFilter = cachedFilterFields.get(editModeFilterIndex);
            editModeTextBox.setValue(currentFilter != null ? currentFilter : "");
        } else {
            editModeTextBox.setValue("");
        }
        
        editModeTextBox.setVisible(true);
        editModeTextBox.setEditable(true);
        editModeTextBox.setMaxLength(100);
        
        // Set responder to save on change
        editModeTextBox.setResponder(value -> {
            // Save filter value when text changes
            while (cachedFilterFields.size() <= editModeFilterIndex) {
                cachedFilterFields.add("");
            }
            cachedFilterFields.set(editModeFilterIndex, value);
            saveFilterData();
        });
        
        addRenderableWidget(editModeTextBox);
        
        // Initialize ghost slot as empty
        ghostSlotItem = ItemStack.EMPTY;
    }
    
    /**
     * Removes the edit mode UI
     */
    private void removeEditModeUI() {
        if (editModeTextBox != null) {
            removeWidget(editModeTextBox);
            editModeTextBox = null;
        }
        if (leftArrowButton != null) {
            removeWidget(leftArrowButton);
            leftArrowButton = null;
        }
        if (rightArrowButton != null) {
            removeWidget(rightArrowButton);
            rightArrowButton = null;
        }
        ghostSlotItem = ItemStack.EMPTY;
        filterVariants.clear();
        currentFilterVariantIndex = 0;
    }
    
    /**
     * Handles click on ghost slot (phantom slot that copies items without consuming)
     */
    private void handleGhostSlotClick() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        
        // Get the item the player is holding (cursor item)
        ItemStack cursorItem = this.menu.getCarried();
        
        if (cursorItem.isEmpty()) {
            // Cursor is empty: clear the ghost slot and textbox
            ghostSlotItem = ItemStack.EMPTY;
            filterVariants.clear();
            currentFilterVariantIndex = 0;
            if (editModeTextBox != null) {
                editModeTextBox.setValue("");
                // Save empty filter
                if (editModeFilterIndex >= 0) {
                    while (cachedFilterFields.size() <= editModeFilterIndex) {
                        cachedFilterFields.add("");
                    }
                    cachedFilterFields.set(editModeFilterIndex, "");
                    saveFilterData();
                }
            }
            playButtonSound();
        } else {
            // Cursor has item: copy it to ghost slot (don't consume the original)
            ghostSlotItem = cursorItem.copy();
            
            // Generate all possible filter variants
            filterVariants = generateAllFilterVariants(cursorItem);
            currentFilterVariantIndex = 0; // Start with first variant
            
            // Update textbox with first variant
            if (editModeTextBox != null && !filterVariants.isEmpty()) {
                String filterString = filterVariants.get(0);
                editModeTextBox.setValue(filterString);
                // Position cursor at the beginning and show from start
                editModeTextBox.setCursorPosition(0);
                editModeTextBox.setHighlightPos(0);
                // Save filter immediately
                if (editModeFilterIndex >= 0) {
                    while (cachedFilterFields.size() <= editModeFilterIndex) {
                        cachedFilterFields.add("");
                    }
                    cachedFilterFields.set(editModeFilterIndex, filterString);
                    saveFilterData();
                }
            }
            
            playButtonSound();
        }
    }
    
    /**
     * Cycles to the next/previous filter variant
     * @param direction 1 for next, -1 for previous
     */
    private void cycleFilterVariant(int direction) {
        if (filterVariants.isEmpty()) {
            return;
        }
        
        // Calculate new index (with wrapping)
        currentFilterVariantIndex += direction;
        if (currentFilterVariantIndex < 0) {
            currentFilterVariantIndex = filterVariants.size() - 1;
        } else if (currentFilterVariantIndex >= filterVariants.size()) {
            currentFilterVariantIndex = 0;
        }
        
        // Update textbox with new variant
        String filterString = filterVariants.get(currentFilterVariantIndex);
        if (editModeTextBox != null) {
            editModeTextBox.setValue(filterString);
            // Position cursor at the beginning and show from start
            editModeTextBox.setCursorPosition(0);
            editModeTextBox.setHighlightPos(0);
            // Save filter immediately
            if (editModeFilterIndex >= 0) {
                while (cachedFilterFields.size() <= editModeFilterIndex) {
                    cachedFilterFields.add("");
                }
                cachedFilterFields.set(editModeFilterIndex, filterString);
                saveFilterData();
            }
        }
    }
    
    /**
     * Generates all possible filter variants from an ItemStack
     * Order: ID item, &enchanted (if present), &damaged (if present), mod ID, all tags
     */
    private java.util.List<String> generateAllFilterVariants(ItemStack stack) {
        java.util.List<String> variants = new java.util.ArrayList<>();
        
        if (stack.isEmpty()) {
            return variants;
        }
        
        net.minecraft.resources.ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return variants;
        }
        
        // 1. Always start with item ID
        variants.add("-" + itemId.toString());
        
        // 2. Add mod ID (if not minecraft)
        String namespace = itemId.getNamespace();
        if (!namespace.equals("minecraft")) {
            variants.add("@" + namespace);
        }
        
        // 3. If enchanted, add &enchanted after mod ID
        if (stack.isEnchanted()) {
            variants.add("&enchanted");
        }
        
        // 4. If damaged, add &damaged after mod ID (and after enchanted if present)
        if (stack.isDamaged()) {
            variants.add("&damaged");
        }
        
        // 5. Add all tags (sorted)
        var item = stack.getItem();
        var itemHolder = net.minecraft.core.registries.BuiltInRegistries.ITEM.wrapAsHolder(item);
        var itemTags = net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagNames()
                .filter(tagKey -> {
                    var tag = net.minecraft.core.registries.BuiltInRegistries.ITEM.getTag(tagKey);
                    return tag.isPresent() && tag.get().contains(itemHolder);
                })
                .map(net.minecraft.tags.TagKey::location)
                .map(net.minecraft.resources.ResourceLocation::toString)
                .sorted()
                .toList();
        
        // Add all tags with # prefix
        for (String tagId : itemTags) {
            variants.add("#" + tagId);
        }
        
        return variants;
    }
    
    /**
     * Starts editing a filter entry at the given index
     */
    private void startEditingFilter(int filterIndex) {
        // Remove existing editing EditBox if any
        if (editingEditBox != null) {
            removeWidget(editingEditBox);
            editingEditBox = null;
        }
        
        // Ensure cachedFilterFields has all slots
        while (cachedFilterFields.size() < MAX_FILTER_SLOTS) {
            cachedFilterFields.add("");
        }
        
        // Calculate entry position
        int visibleIndex = filterIndex - filterScrollOffset;
        if (visibleIndex < 0 || visibleIndex >= VISIBLE_ENTRIES) {
            return; // Not visible
        }
        
        int entryX = this.leftPos + ENTRY_X;
        int entryY = this.topPos + FIRST_ROW_Y + visibleIndex * (ENTRY_HEIGHT + ENTRY_SPACING);
        
        // Create EditBox positioned over the entry text area
        int textX = entryX + 3 + 18 + 6; // After slot
        int textY = entryY + (ENTRY_HEIGHT - 15) / 2; // Centered vertically (EditBox height is 15)
        
        // Calculate width leaving space for edit button (12px button + 5px margin + 5px spacing)
        int editButtonSize = 12;
        int editButtonMargin = 5;
        int editButtonX = entryX + ENTRY_WIDTH - editButtonMargin - editButtonSize;
        int textWidth = editButtonX - textX - 5; // 5px margin before edit button
        
        editingEditBox = new EditBox(this.font,
                textX,
                textY,
                textWidth,
                15,
                Component.empty());
        editingEditBox.setMaxLength(100);
        editingEditBox.setValue(cachedFilterFields.get(filterIndex) != null ? cachedFilterFields.get(filterIndex) : "");
        editingEditBox.setEditable(true);
        editingEditBox.setFocused(true);
        
        editingFilterIndex = filterIndex;
        
        // Save data when EditBox value changes
        editingEditBox.setResponder(value -> {
            // Ensure cachedFilterFields has all 50 slots
            while (cachedFilterFields.size() < MAX_FILTER_SLOTS) {
                cachedFilterFields.add("");
            }
            
            String trimmedValue = value.trim();
            cachedFilterFields.set(filterIndex, trimmedValue);
            
            saveFilterData();
        });
        
        addRenderableWidget(editingEditBox);
    }
    
    /**
     * Stops editing the current filter
     */
    private void stopEditingFilter() {
        if (editingEditBox != null) {
            removeWidget(editingEditBox);
            editingEditBox = null;
            editingFilterIndex = -1;
        }
    }
    
    private void renderRedstoneModeButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Check if mouse is over the button
        boolean isHovered = mouseX >= this.redstoneModeButtonX && mouseX <= this.redstoneModeButtonX + REDSTONE_BUTTON_SIZE &&
                           mouseY >= this.redstoneModeButtonY && mouseY <= this.redstoneModeButtonY + REDSTONE_BUTTON_SIZE;
        
        // Draw button background (normal or highlighted)
        int textureY = isHovered ? 16 : 0; // Highlighted version is below the normal one
        guiGraphics.blit(MEDIUM_BUTTONS, this.redstoneModeButtonX, this.redstoneModeButtonY, 
                        0, textureY, REDSTONE_BUTTON_SIZE, REDSTONE_BUTTON_SIZE, 
                        96, 96); // Correct texture size: 96x96
        
        // Get current redstone mode from ContainerData (synced automatically)
        int redstoneMode = menu.getRedstoneMode();
        
        // Draw the appropriate icon (12x12 pixels, centered in the 16x16 button)
        int iconX = this.redstoneModeButtonX + 2; // Center: (16-12)/2 = 2
        int iconY = this.redstoneModeButtonY + 2; // Center: (16-12)/2 = 2
        int iconSize = 12;
        
        switch (redstoneMode) {
            case 0 -> {
                // NONE mode: Gunpowder icon
                net.minecraft.world.item.ItemStack gunpowder = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GUNPOWDER);
                renderScaledItem(guiGraphics, gunpowder, iconX, iconY, iconSize);
            }
            case 1 -> {
                // LOW mode: Redstone dust icon
                net.minecraft.world.item.ItemStack redstone = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.REDSTONE);
                renderScaledItem(guiGraphics, redstone, iconX, iconY, iconSize);
            }
            case 2 -> {
                // HIGH mode: Redstone GUI texture rendered as item-like (12x12)
                renderScaledTexture(guiGraphics, REDSTONE_GUI, iconX, iconY, iconSize);
            }
            case 3 -> {
                // PULSE mode: Repeater icon
                net.minecraft.world.item.ItemStack repeater = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.REPEATER);
                renderScaledItem(guiGraphics, repeater, iconX, iconY, iconSize);
            }
            case 4 -> {
                // DISABLED mode: Barrier icon
                net.minecraft.world.item.ItemStack barrier = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BARRIER);
                renderScaledItem(guiGraphics, barrier, iconX, iconY, iconSize);
            }
        }
    }
    
    /**
     * Renders an item scaled to the specified size
     */
    private void renderScaledItem(GuiGraphics guiGraphics, net.minecraft.world.item.ItemStack itemStack, int x, int y, int size) {
        // Save current matrix state
        guiGraphics.pose().pushPose();
        
        // Calculate scale: original item size is 16x16, we want 12x12
        float scale = (float) size / 16.0f;
        
        // Translate to position and apply scale
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        
        // Render the item
        guiGraphics.renderItem(itemStack, 0, 0);
        
        // Restore matrix state
        guiGraphics.pose().popPose();
    }
    
    /**
     * Renders a texture scaled to the specified size (like an item)
     */
    private void renderScaledTexture(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int size) {
        // Save current matrix state
        guiGraphics.pose().pushPose();
        
        // Calculate scale: original texture size is 16x16, we want 12x12
        float scale = (float) size / 16.0f;
        
        // Translate to position and apply scale
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        
        // Render the texture (assuming it's 16x16)
        guiGraphics.blit(texture, 0, 0, 0, 0, 16, 16, 16, 16);
        
        // Restore matrix state
        guiGraphics.pose().popPose();
    }
    
    // Help text positions (for how to use screen)
    private static final int HELP_TEXT_START_Y = 30; // Below title
    private static final int HELP_TEXT_X = 8;
    private static final int HELP_TEXT_LINE_HEIGHT = 12; // Normal line height
    
    // Back button (for how to use screen)
    private Button backButton;
    private static final int BACK_BUTTON_X = 8; // Left side, same as EditBoxes
    private static final int BACK_BUTTON_Y = GUI_HEIGHT - 25; // Near bottom
    
    // Example text positions and data for copy functionality
    private static class ExampleData {
        final String example;
        final int x;
        final int y;
        final int width;
        
        ExampleData(String example, int x, int y, int width) {
            this.example = example;
            this.x = x;
            this.y = y;
            this.width = width;
        }
    }
    
    private final java.util.List<ExampleData> exampleDataList = new java.util.ArrayList<>();
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isHowToUseMode) {
            // Clear example data list for this render
            exampleDataList.clear();
            
            // How to use screen title
            Component titleComponent = Component.translatable("gui.iska_utils.deep_drawer_extractor.how_to_use");
            int titleWidth = this.font.width(titleComponent);
            int titleX = this.leftPos + (this.imageWidth - titleWidth) / 2;
            guiGraphics.drawString(this.font, titleComponent, titleX, this.topPos + TITLE_Y, 0x404040, false);
            
            // Render help text with default font size and extract examples
            int helpY = HELP_TEXT_START_Y;
            
            // ID example: -minecraft:diamond
            renderHelpLineWithExample(guiGraphics, "gui.iska_utils.general_filter_text.id", 
                                     "gui.iska_utils.general_filter_text.id.example",
                                     "gui.iska_utils.general_filter_text.id.after",
                                     HELP_TEXT_X, helpY, mouseX, mouseY);
            helpY += HELP_TEXT_LINE_HEIGHT;
            
            // Tag example: #c:ingots
            renderHelpLineWithExample(guiGraphics, "gui.iska_utils.general_filter_text.tag", 
                                     "gui.iska_utils.general_filter_text.tag.example",
                                     "gui.iska_utils.general_filter_text.tag.after",
                                     HELP_TEXT_X, helpY, mouseX, mouseY);
            helpY += HELP_TEXT_LINE_HEIGHT;
            
            // Mod ID example: @iska_utils
            renderHelpLineWithExample(guiGraphics, "gui.iska_utils.general_filter_text.modid", 
                                     "gui.iska_utils.general_filter_text.modid.example",
                                     "gui.iska_utils.general_filter_text.modid.after",
                                     HELP_TEXT_X, helpY, mouseX, mouseY);
            helpY += HELP_TEXT_LINE_HEIGHT;
            
            // NBT: description on first line, example on second line
            guiGraphics.drawString(this.font, Component.translatable("gui.iska_utils.general_filter_text.nbt"), 
                                  this.leftPos + HELP_TEXT_X, this.topPos + helpY, 0x404040, false);
            helpY += HELP_TEXT_LINE_HEIGHT;
            // NBT example: ?"apotheosis:rarity":"apotheosis:mythic"
            renderHelpLineWithExample(guiGraphics, "gui.iska_utils.general_filter_text.nbt.example", 
                                     "gui.iska_utils.general_filter_text.nbt.example.text",
                                     "gui.iska_utils.general_filter_text.nbt.after",
                                     HELP_TEXT_X, helpY, mouseX, mouseY);
            helpY += HELP_TEXT_LINE_HEIGHT;
            
            // Macro examples: &enchanted, &damaged (both clickable)
            renderHelpLineWithTwoExamples(guiGraphics, "gui.iska_utils.general_filter_text.macro", 
                                         "gui.iska_utils.general_filter_text.macro.example1",
                                         "gui.iska_utils.general_filter_text.macro.middle",
                                         "gui.iska_utils.general_filter_text.macro.example2",
                                         "gui.iska_utils.general_filter_text.macro.after",
                                         HELP_TEXT_X, helpY, mouseX, mouseY);
            helpY += HELP_TEXT_LINE_HEIGHT;
            
            // Usage: add extra spacing before the command
            helpY += HELP_TEXT_LINE_HEIGHT; // Extra spacing
            guiGraphics.drawString(this.font, Component.translatable("gui.iska_utils.general_filter_text.usage"), 
                                  this.leftPos + HELP_TEXT_X, this.topPos + helpY, 0x404040, false);
            helpY += HELP_TEXT_LINE_HEIGHT;
            guiGraphics.drawString(this.font, Component.translatable("gui.iska_utils.general_filter_text.usage.what"), 
                                  this.leftPos + HELP_TEXT_X, this.topPos + helpY, 0x404040, false);
            
        } else {
            // Main screen title
            Component titleComponent = this.title;
            int titleWidth = this.font.width(titleComponent);
            int titleX = (this.imageWidth - titleWidth) / 2;
            guiGraphics.drawString(this.font, titleComponent, titleX, TITLE_Y, 0x404040, false);
            
            // Render "Deep Drawer Buffer" text centered above the 5 buffer slots
            // Buffer slots are at X=195, Y=134, 5 slots in a row (5 * 18 = 90 pixels wide)
            Component bufferLabel = Component.translatable("gui.iska_utils.deep_drawer_extractor.buffer_label");
            int bufferLabelWidth = this.font.width(bufferLabel);
            int bufferLabelX = 195 + (5 * 18) / 2 - bufferLabelWidth / 2; // Center of 5 slots minus half text width
            int bufferLabelY = 134 - this.font.lineHeight - 2; // Above slots with 2px spacing
            guiGraphics.drawString(this.font, bufferLabel, bufferLabelX, bufferLabelY, 0x404040, false);
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (isHowToUseMode) {
            // In how to use mode, render only background and labels (no slots)
            this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
            this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
            this.renderLabels(guiGraphics, mouseX, mouseY);
            // Render widgets (buttons, etc.)
            for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }
            // Render tooltip for examples
            renderExampleTooltip(guiGraphics, mouseX, mouseY);
        } else {
            // In main mode, render everything normally (including slots)
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            // Render default item tooltips for slots
            this.renderTooltip(guiGraphics, mouseX, mouseY);
        }
    }
    
    /**
     * Renders a help line with an example that can be clicked to copy
     */
    private void renderHelpLineWithExample(GuiGraphics guiGraphics, String beforeKey, String exampleKey, String afterKey,
                                          int x, int y, int mouseX, int mouseY) {
        // Get translated parts
        Component beforeComponent = Component.translatable(beforeKey);
        Component exampleComponent = Component.translatable(exampleKey);
        Component afterComponent = Component.translatable(afterKey);
        
        String beforeText = beforeComponent.getString();
        String exampleText = exampleComponent.getString();
        String afterText = afterComponent.getString();
        
        // Convert relative coordinates to absolute screen coordinates
        int absX = this.leftPos + x;
        int absY = this.topPos + y;
        
        // Render before text
        int beforeWidth = this.font.width(beforeText);
        guiGraphics.drawString(this.font, beforeComponent, absX, absY, 0x404040, false);
        
        // Render example text (clickable, with blue color)
        int exampleX = absX + beforeWidth;
        int exampleWidth = this.font.width(exampleText);
        
        // Check if hovering over example
        boolean isHovered = mouseX >= exampleX && mouseX <= exampleX + exampleWidth &&
                           mouseY >= absY && mouseY <= absY + HELP_TEXT_LINE_HEIGHT;
        
        // Use blue color for clickable example, darker blue when hovered
        int exampleColor = isHovered ? 0x0066FF : 0x0066CC;
        
        // Render example text in blue
        guiGraphics.drawString(this.font, exampleText, exampleX, absY, exampleColor, false);
        
        // Underline when hovered
        if (isHovered) {
            // Draw underline
            int underlineY = absY + this.font.lineHeight;
            guiGraphics.fill(exampleX, underlineY, exampleX + exampleWidth, underlineY + 1, exampleColor);
        }
        
        // Render after text (parentheses, commas, etc.)
        if (!afterText.isEmpty()) {
            int afterX = exampleX + exampleWidth;
            guiGraphics.drawString(this.font, afterComponent, afterX, absY, 0x404040, false);
        }
        
        // Store example data for click handling (store relative coordinates for later use)
        exampleDataList.add(new ExampleData(exampleText, x + beforeWidth, y, exampleWidth));
    }
    
    /**
     * Renders a help line with two examples that can be clicked to copy
     */
    private void renderHelpLineWithTwoExamples(GuiGraphics guiGraphics, String beforeKey, 
                                              String example1Key, String middleKey, String example2Key, String afterKey,
                                              int x, int y, int mouseX, int mouseY) {
        // Get translated parts
        Component beforeComponent = Component.translatable(beforeKey);
        Component example1Component = Component.translatable(example1Key);
        Component middleComponent = Component.translatable(middleKey);
        Component example2Component = Component.translatable(example2Key);
        Component afterComponent = Component.translatable(afterKey);
        
        String beforeText = beforeComponent.getString();
        String example1Text = example1Component.getString();
        String middleText = middleComponent.getString();
        String example2Text = example2Component.getString();
        String afterText = afterComponent.getString();
        
        // Convert relative coordinates to absolute screen coordinates
        int absX = this.leftPos + x;
        int absY = this.topPos + y;
        
        // Render before text
        int beforeWidth = this.font.width(beforeText);
        guiGraphics.drawString(this.font, beforeComponent, absX, absY, 0x404040, false);
        
        // Render first example
        int example1X = absX + beforeWidth;
        int example1Width = this.font.width(example1Text);
        boolean isHovered1 = mouseX >= example1X && mouseX <= example1X + example1Width &&
                            mouseY >= absY && mouseY <= absY + HELP_TEXT_LINE_HEIGHT;
        int example1Color = isHovered1 ? 0x0066FF : 0x0066CC;
        guiGraphics.drawString(this.font, example1Text, example1X, absY, example1Color, false);
        if (isHovered1) {
            int underlineY = absY + this.font.lineHeight;
            guiGraphics.fill(example1X, underlineY, example1X + example1Width, underlineY + 1, example1Color);
        }
        exampleDataList.add(new ExampleData(example1Text, x + beforeWidth, y, example1Width));
        
        // Render middle text (comma and space)
        int middleX = example1X + example1Width;
        guiGraphics.drawString(this.font, middleComponent, middleX, absY, 0x404040, false);
        
        // Render second example
        int middleWidth = this.font.width(middleText);
        int example2X = middleX + middleWidth;
        int example2Width = this.font.width(example2Text);
        boolean isHovered2 = mouseX >= example2X && mouseX <= example2X + example2Width &&
                            mouseY >= absY && mouseY <= absY + HELP_TEXT_LINE_HEIGHT;
        int example2Color = isHovered2 ? 0x0066FF : 0x0066CC;
        guiGraphics.drawString(this.font, example2Text, example2X, absY, example2Color, false);
        if (isHovered2) {
            int underlineY = absY + this.font.lineHeight;
            guiGraphics.fill(example2X, underlineY, example2X + example2Width, underlineY + 1, example2Color);
        }
        exampleDataList.add(new ExampleData(example2Text, x + beforeWidth + example1Width + middleWidth, y, example2Width));
        
        // Render after text
        if (!afterText.isEmpty()) {
            int afterX = example2X + example2Width;
            guiGraphics.drawString(this.font, afterComponent, afterX, absY, 0x404040, false);
        }
    }
    
    /**
     * Renders tooltip when hovering over an example
     */
    private void renderExampleTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (ExampleData exampleData : exampleDataList) {
            int screenX = this.leftPos + exampleData.x;
            int screenY = this.topPos + exampleData.y;
            
            if (mouseX >= screenX && mouseX <= screenX + exampleData.width &&
                mouseY >= screenY && mouseY <= screenY + HELP_TEXT_LINE_HEIGHT) {
                
                java.util.List<Component> tooltip = new java.util.ArrayList<>();
                tooltip.add(Component.translatable("gui.iska_utils.general_filter_text.click_to_copy"));
                tooltip.add(Component.translatable("gui.iska_utils.general_filter_text.paste_hint"));
                
                // renderComponentTooltip automatically positions the tooltip near the cursor
                // It handles screen bounds checking internally
                guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                return;
            }
        }
    }
    
    @Override
    public void containerTick() {
        super.containerTick();
        
        // Update cached filters from server (like redstone mode and structure)
        menu.updateCachedFilters();
        
        // Get cached data for filter fields
        java.util.List<String> filterFields = menu.getCachedFilterFields();
        
        // Update cached filter fields - ensure we always have exactly MAX_FILTER_SLOTS entries
        cachedFilterFields = new java.util.ArrayList<>(filterFields);
        while (cachedFilterFields.size() < MAX_FILTER_SLOTS) {
            cachedFilterFields.add("");
        }
        // Trim to MAX_FILTER_SLOTS if somehow we got more
        while (cachedFilterFields.size() > MAX_FILTER_SLOTS) {
            cachedFilterFields.remove(cachedFilterFields.size() - 1);
        }
        
        // No need to update entries - they are rendered directly from cachedFilterFields
        
        // Update mode button from synced ContainerData (like rotation in StructurePlacerMachineScreen)
        // Always read from ContainerData and update button - ContainerData is automatically synced
        if (modeButton != null) {
            boolean syncedWhitelistMode = menu.getWhitelistMode();
            Component buttonText = syncedWhitelistMode
                    ? Component.translatable("gui.iska_utils.deep_drawer_extractor.mode.allow")
                    : Component.translatable("gui.iska_utils.deep_drawer_extractor.mode.deny");
            modeButton.setMessage(buttonText);
            // Update local state to match
            isWhitelistMode = syncedWhitelistMode;
        }
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Check if editing EditBox is focused
        boolean isEditBoxFocused = editingEditBox != null && editingEditBox.isFocused();
        
        // Check if edit mode textbox is focused
        boolean isEditModeTextBoxFocused = editModeTextBox != null && editModeTextBox.isFocused();
        
        // Check if in edit mode (not editing text, but in edit mode for a filter)
        boolean isInEditMode = editModeFilterIndex >= 0;
        
        // Handle ESC key
        if (keyCode == 256) { // ESC key
            if (isEditBoxFocused) {
                // If EditBox is focused, stop editing
                stopEditingFilter();
                return true;
            }
            // If edit mode textbox is focused, exit edit mode
            if (isEditModeTextBoxFocused) {
                playButtonSound();
                exitEditMode();
                return true;
            }
            // If in edit mode (but not editing text), exit edit mode
            if (isInEditMode) {
                playButtonSound();
                exitEditMode();
                return true;
            }
        }
        
        if (isEditBoxFocused && editingEditBox != null) {
            // Let the focused EditBox handle the key first
            if (editingEditBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            
            // If Enter is pressed, stop editing
            if (keyCode == 257) { // Enter key
                stopEditingFilter();
                return true;
            }
            
            // If inventory key is pressed while EditBox is focused, prevent closing
            // This works even if the user has changed the inventory key binding
            if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                return true; // Prevent closing
            }
        }
        
        // Handle edit mode textbox key presses
        if (isEditModeTextBoxFocused && editModeTextBox != null) {
            // Let the focused edit mode textbox handle the key first
            if (editModeTextBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            
            // If inventory key is pressed while edit mode textbox is focused, block it (prevent closing)
            // This works even if the user has changed the inventory key binding
            if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                return true; // Block the key, don't exit edit mode
            }
        }
        
        // Handle ESC and inventory key based on current mode
        if (isHowToUseMode) {
            // In how to use mode, ESC or inventory key returns to main screen
            if (keyCode == 256 || // ESC key
                (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode))) {
                if (!isEditBoxFocused && !isEditModeTextBoxFocused) {
                    // If in edit mode, exit edit mode first
                    if (isInEditMode) {
                        playButtonSound();
                        exitEditMode();
                        return true;
                    }
                    playButtonSound();
                    switchToMainScreen();
                    return true;
                }
            }
        } else {
            // In main mode, ESC or inventory key closes GUI (unless EditBox is focused or in edit mode)
            if (keyCode == 256 || // ESC key
                (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode))) {
                if (!isEditBoxFocused && !isEditModeTextBoxFocused) {
                    // If in edit mode, exit edit mode instead of closing
                    if (isInEditMode) {
                        playButtonSound();
                        exitEditMode();
                        return true;
                    }
                    // Close GUI without sound
                    this.onClose();
                    return true;
                }
            }
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // Check if editing EditBox is focused
        if (editingEditBox != null && editingEditBox.isFocused()) {
            if (editingEditBox.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        
        // Check if edit mode textbox is focused
        if (editModeTextBox != null && editModeTextBox.isFocused()) {
            if (editModeTextBox.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        
        return super.charTyped(codePoint, modifiers);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (!isHowToUseMode) {
            if (deltaY > 0) {
                return scrollUpSilent();
            } else if (deltaY < 0) {
                return scrollDownSilent();
            }
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
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
        if (button == 0 && isDraggingHandle && MAX_FILTER_SLOTS > VISIBLE_ENTRIES) {
            int deltaY = (int) mouseY - dragStartY;
            int maxScrollOffset = Math.max(0, MAX_FILTER_SLOTS - VISIBLE_ENTRIES);
            
            if (maxScrollOffset > 0) {
                float scrollRatio = (float) deltaY / (SCROLLBAR_HEIGHT - HANDLE_SIZE);
                
                int newScrollOffset = dragStartScrollOffset + (int)(scrollRatio * maxScrollOffset);
                newScrollOffset = Math.max(0, Math.min(maxScrollOffset, newScrollOffset));
                
                setFilterScrollOffset(newScrollOffset);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    /**
     * Handles clicks on UP/DOWN buttons
     */
    private boolean handleScrollButtonClick(double mouseX, double mouseY) {
        if (MAX_FILTER_SLOTS <= VISIBLE_ENTRIES) return false;
        
        int guiX = this.leftPos;
        int guiY = this.topPos;
        
        // UP button
        if (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH &&
            mouseY >= guiY + BUTTON_UP_Y && mouseY < guiY + BUTTON_UP_Y + HANDLE_SIZE) {
            scrollUp();
            return true;
        }
        
        // DOWN button
        if (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH &&
            mouseY >= guiY + BUTTON_DOWN_Y && mouseY < guiY + BUTTON_DOWN_Y + HANDLE_SIZE) {
            scrollDown();
            return true;
        }
        
        return false;
    }
    
    /**
     * Handles clicks on the draggable handle
     */
    private boolean handleHandleClick(double mouseX, double mouseY) {
        if (MAX_FILTER_SLOTS <= VISIBLE_ENTRIES) return false;
        
        int guiX = this.leftPos;
        int guiY = this.topPos;
        
        int maxScrollOffset = Math.max(0, MAX_FILTER_SLOTS - VISIBLE_ENTRIES);
        if (maxScrollOffset > 0) {
            double scrollRatio = (double) filterScrollOffset / maxScrollOffset;
            int handleY = guiY + SCROLLBAR_Y + (int)(scrollRatio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
            
            if (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + HANDLE_SIZE &&
                mouseY >= handleY && mouseY < handleY + HANDLE_SIZE) {
                
                isDraggingHandle = true;
                dragStartY = (int) mouseY;
                dragStartScrollOffset = filterScrollOffset;
                playButtonSound();
                return true;
            }
        }
        return false;
    }
    
    /**
     * Handles clicks on the scrollbar area (jump to position)
     */
    private boolean handleScrollbarClick(double mouseX, double mouseY) {
        if (MAX_FILTER_SLOTS <= VISIBLE_ENTRIES) return false;
        
        int guiX = this.leftPos;
        int guiY = this.topPos;
        
        if (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH &&
            mouseY >= guiY + SCROLLBAR_Y && mouseY < guiY + SCROLLBAR_Y + SCROLLBAR_HEIGHT) {
            
            // Calculate new scroll position based on click
            float clickRatio = (float)(mouseY - (guiY + SCROLLBAR_Y)) / SCROLLBAR_HEIGHT;
            clickRatio = Math.max(0, Math.min(1, clickRatio));
            
            int maxScrollOffset = Math.max(0, MAX_FILTER_SLOTS - VISIBLE_ENTRIES);
            int newScrollOffset = (int)(clickRatio * maxScrollOffset);
            newScrollOffset = Math.max(0, Math.min(maxScrollOffset, newScrollOffset));
            
            if (newScrollOffset != filterScrollOffset) {
                setFilterScrollOffset(newScrollOffset);
                playButtonSound();
            }
            return true;
        }
        return false;
    }
    
    /**
     * Scrolls up by one entry
     */
    private void scrollUp() {
        if (scrollUpSilent()) {
            playButtonSound();
        }
    }
    
    /**
     * Scrolls down by one entry
     */
    private void scrollDown() {
        if (scrollDownSilent()) {
            playButtonSound();
        }
    }
    
    /**
     * Scrolls up silently (without sound)
     */
    private boolean scrollUpSilent() {
        if (MAX_FILTER_SLOTS > VISIBLE_ENTRIES && filterScrollOffset > 0) {
            int newOffset = Math.max(0, filterScrollOffset - 1);
            setFilterScrollOffset(newOffset);
            return true;
        }
        return false;
    }
    
    /**
     * Scrolls down silently (without sound)
     */
    private boolean scrollDownSilent() {
        int maxScrollOffset = Math.max(0, MAX_FILTER_SLOTS - VISIBLE_ENTRIES);
        
        if (MAX_FILTER_SLOTS > VISIBLE_ENTRIES && filterScrollOffset < maxScrollOffset) {
            int newOffset = Math.min(maxScrollOffset, filterScrollOffset + 1);
            setFilterScrollOffset(newOffset);
            return true;
        }
        return false;
    }
    
    /**
     * Sets the filter scroll offset and updates EditBoxes
     */
    private void setFilterScrollOffset(int offset) {
        int maxScrollOffset = Math.max(0, MAX_FILTER_SLOTS - VISIBLE_ENTRIES);
        this.filterScrollOffset = Math.max(0, Math.min(maxScrollOffset, offset));
        // Stop editing if the edited entry is no longer visible
        if (editingFilterIndex >= 0) {
            int visibleIndex = editingFilterIndex - filterScrollOffset;
            if (visibleIndex < 0 || visibleIndex >= VISIBLE_ENTRIES) {
                stopEditingFilter();
            }
        }
        // Update edit buttons when scroll changes
        updateEditButtons();
    }
    
    @Override
    public void onClose() {
        // Save data when closing
        saveFilterData();
        super.onClose();
    }
}
