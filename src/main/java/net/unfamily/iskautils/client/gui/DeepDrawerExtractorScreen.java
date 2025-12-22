package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.core.BlockPos;
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
    
    // EditBox dimensions and positions (scrollable, infinite)
    // ENTRY_WIDE is 140x24, EditBox uses full width now (no remove button)
    private static final int ENTRY_WIDE_WIDTH = 140; // Width of entry_wide.png texture
    private static final int EDIT_BOX_WIDTH = ENTRY_WIDE_WIDTH; // Full width (140px)
    private static final int EDIT_BOX_HEIGHT = 15; // Original height (not matching ENTRY_WIDE)
    private static final int EDIT_BOX_X = 8;
    private static final int FILTERS_LABEL_Y = 30; // Y position for "Filters" label
    private static final int FIRST_ROW_Y = FILTERS_LABEL_Y + 12; // Start EditBoxes after "Filters" label (12px for label height + spacing)
    private static final int BUFFER_SLOTS_Y = 134; // Y position of buffer slots (where EditBoxes should stop)
    private static final int MAX_FILTER_SLOTS = net.unfamily.iskautils.Config.deepDrawerExtractorMaxFilters; // Total filter slots in BlockEntity (from config, default 50)
    private static final int VISIBLE_EDIT_BOXES = 12; // Number of EditBoxes visible at once (scrollable)
    
    // Scrollbar constants (identical to DeepDrawersScreen)
    private static final int SCROLLBAR_WIDTH = 8;      // Width of each scrollbar element
    private static final int SCROLLBAR_HEIGHT = 34;    // Height of scrollbar background in texture
    private static final int HANDLE_SIZE = 8;          // Size of UP/DOWN buttons and handle
    private static final int SCROLLBAR_X = EDIT_BOX_X + ENTRY_WIDE_WIDTH + 4; // After EditBox + remove button + spacing (8 + 140 + 4 = 152)
    private static final int BUTTON_UP_Y = FIRST_ROW_Y; // Aligned with first EditBox (after "Filters" label)
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
    
    // Dynamic EditBoxes for filter fields (only visible ones are created)
    private final java.util.List<EditBox> filterEditBoxes = new java.util.ArrayList<>();
    
    // Buttons for removing filters
    
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
        
        // Create EditBoxes for visible filters (will be updated in containerTick)
        updateFilterEditBoxes();
        
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
        
        // Update EditBoxes visibility (handled in updateFilterEditBoxes)
        if (showMain) {
            updateFilterEditBoxes();
        } else {
            // Hide all EditBoxes in how to use mode
            for (EditBox editBox : filterEditBoxes) {
                if (editBox != null) {
                    editBox.visible = false;
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
        
        // Right click (button == 1) on EditBox clears its content and focuses it
        // Iterate in reverse order to check last EditBox first (in case of overlap)
        if (button == 1 && !isHowToUseMode) {
            for (int i = filterEditBoxes.size() - 1; i >= 0; i--) {
                EditBox editBox = filterEditBoxes.get(i);
                if (editBox != null && editBox.isMouseOver(mouseX, mouseY)) {
                    // Unfocus all other EditBoxes first
                    for (EditBox other : filterEditBoxes) {
                        if (other != null && other != editBox) {
                            other.setFocused(false);
                        }
                    }
                    // Clear and focus the clicked EditBox
                    editBox.setValue("");
                    editBox.setFocused(true);
                    saveFilterData(); // Save after clearing
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
            // Collect filter field values from cached filters (remove empty ones)
            java.util.List<String> filterFields = new java.util.ArrayList<>();
            for (String filter : cachedFilterFields) {
                if (filter != null && !filter.trim().isEmpty()) {
                    filterFields.add(filter.trim());
                }
            }
            
            // Always read whitelist mode from synced ContainerData (not local isWhitelistMode)
            // This ensures we use the server-authoritative value, not stale local state
            boolean currentWhitelistMode = menu.getWhitelistMode();
            
            // Send to server via packet
            ModMessages.sendDeepDrawerExtractorFilterUpdatePacket(machinePos, filterFields, currentWhitelistMode);
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
            // Center the label with the EditBoxes
            int labelX = this.leftPos + EDIT_BOX_X + (EDIT_BOX_WIDTH - labelWidth) / 2;
            guiGraphics.drawString(this.font, filtersLabel, labelX, this.topPos + FILTERS_LABEL_Y, 0x404040, false);
        }
        
        // Render redstone mode button (only in main mode, not in how to use)
        if (!isHowToUseMode) {
            renderRedstoneModeButton(guiGraphics, mouseX, mouseY);
            // Render scrollbar (only in main mode, not in how to use)
            renderScrollbar(guiGraphics, mouseX, mouseY);
        }
    }
    
    /**
     * Renders the scrollbar with UP/DOWN buttons and draggable handle
     * Identical to DeepDrawersScreen implementation
     */
    private void renderScrollbar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Only show scrollbar if there are more slots than can fit
        if (MAX_FILTER_SLOTS <= VISIBLE_EDIT_BOXES) return;
        
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
        int maxScrollOffset = Math.max(0, MAX_FILTER_SLOTS - VISIBLE_EDIT_BOXES);
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
     * Updates filter EditBoxes based on scroll offset and cached filters
     * Creates/removes EditBoxes as needed to show only visible ones
     */
    private void updateFilterEditBoxes() {
        // Save which EditBox was focused before removing them
        int focusedIndex = -1;
        for (int i = 0; i < filterEditBoxes.size(); i++) {
            EditBox editBox = filterEditBoxes.get(i);
            if (editBox != null && editBox.isFocused()) {
                // Calculate the actual filter index (scrollOffset + visible index)
                focusedIndex = filterScrollOffset + i;
                break;
            }
        }
        
        // Remove all existing EditBoxes
        for (EditBox editBox : filterEditBoxes) {
            if (editBox != null) {
                removeWidget(editBox);
            }
        }
        filterEditBoxes.clear();
        
        if (isHowToUseMode) {
            return; // Don't create EditBoxes in how to use mode
        }
        
        // Calculate scroll limits
        int maxScroll = Math.max(0, MAX_FILTER_SLOTS - VISIBLE_EDIT_BOXES);
        filterScrollOffset = Math.min(filterScrollOffset, maxScroll);
        
        // Create VISIBLE_EDIT_BOXES EditBoxes (12 visible, scrollable through all 50)
        for (int i = 0; i < VISIBLE_EDIT_BOXES; i++) {
            int filterIndex = filterScrollOffset + i;
            if (filterIndex >= MAX_FILTER_SLOTS) {
                break; // Don't create more than MAX_FILTER_SLOTS
            }
            
            int y = FIRST_ROW_Y + i * (EDIT_BOX_HEIGHT + 2); // 2px spacing between EditBoxes
            
            // Create EditBox
            EditBox editBox = new EditBox(this.font,
                    this.leftPos + EDIT_BOX_X,
                    this.topPos + y,
                    EDIT_BOX_WIDTH,
                    EDIT_BOX_HEIGHT,
                    Component.empty());
            editBox.setMaxLength(100);
            
            // Set value from cached filters (ensure we have all 50 slots)
            while (cachedFilterFields.size() < MAX_FILTER_SLOTS) {
                cachedFilterFields.add("");
            }
            editBox.setValue(cachedFilterFields.get(filterIndex) != null ? cachedFilterFields.get(filterIndex) : "");
            
            // All EditBoxes are editable
            editBox.setEditable(true);
            
            // Restore focus if this was the focused EditBox
            if (filterIndex == focusedIndex) {
                editBox.setFocused(true);
            }
            
            // Save data when EditBox value changes
            final int finalIndex = filterIndex;
            editBox.setResponder(value -> {
                // Ensure cachedFilterFields has all 50 slots
                while (cachedFilterFields.size() < MAX_FILTER_SLOTS) {
                    cachedFilterFields.add("");
                }
                
                String trimmedValue = value.trim();
                cachedFilterFields.set(finalIndex, trimmedValue);
                
                saveFilterData();
            });
            
            // Add EditBox
            filterEditBoxes.add(editBox);
            addRenderableWidget(editBox);
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
        
        // Update EditBoxes to reflect changes (only if not focused to avoid overwriting user input)
        boolean anyFocused = false;
        for (EditBox editBox : filterEditBoxes) {
            if (editBox != null && editBox.isFocused()) {
                anyFocused = true;
                break;
            }
        }
        if (!anyFocused) {
            updateFilterEditBoxes();
        }
        
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
        // Check if an EditBox is focused
        boolean isEditBoxFocused = false;
        for (EditBox editBox : filterEditBoxes) {
            if (editBox != null && editBox.isFocused()) {
                isEditBoxFocused = true;
                break;
            }
        }
        
        // Handle ESC key
        if (keyCode == 256) { // ESC key
            if (isEditBoxFocused) {
                // If EditBox is focused, just unfocus it
                for (EditBox editBox : filterEditBoxes) {
                    if (editBox != null) {
                        editBox.setFocused(false);
                    }
                }
                return true;
            }
        }
        
        if (isEditBoxFocused) {
            // Let the focused EditBox handle the key first
            for (EditBox editBox : filterEditBoxes) {
                if (editBox != null && editBox.isFocused()) {
                    if (editBox.keyPressed(keyCode, scanCode, modifiers)) {
                        return true;
                    }
                }
            }
            
            // If inventory key is pressed while EditBox is focused, prevent closing
            if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                return true; // Prevent closing
            }
        }
        
        // Handle ESC key based on current mode
        if (isHowToUseMode) {
            // In how to use mode, ESC or inventory key returns to main screen
            if (keyCode == 256 || // ESC key
                (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode))) {
                if (!isEditBoxFocused) {
                    playButtonSound();
                    switchToMainScreen();
                    return true;
                }
            }
        } else {
            // In main mode, ESC or inventory key closes GUI (unless EditBox is focused)
            if (keyCode == 256 || // ESC key
                (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode))) {
                if (!isEditBoxFocused) {
                    // Close GUI without sound
                    this.onClose();
                    return true;
                }
            }
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
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
        if (button == 0 && isDraggingHandle && MAX_FILTER_SLOTS > VISIBLE_EDIT_BOXES) {
            int deltaY = (int) mouseY - dragStartY;
            int maxScrollOffset = Math.max(0, MAX_FILTER_SLOTS - VISIBLE_EDIT_BOXES);
            
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
        if (MAX_FILTER_SLOTS <= VISIBLE_EDIT_BOXES) return false;
        
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
        if (MAX_FILTER_SLOTS <= VISIBLE_EDIT_BOXES) return false;
        
        int guiX = this.leftPos;
        int guiY = this.topPos;
        
        int maxScrollOffset = Math.max(0, MAX_FILTER_SLOTS - VISIBLE_EDIT_BOXES);
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
        if (MAX_FILTER_SLOTS <= VISIBLE_EDIT_BOXES) return false;
        
        int guiX = this.leftPos;
        int guiY = this.topPos;
        
        if (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH &&
            mouseY >= guiY + SCROLLBAR_Y && mouseY < guiY + SCROLLBAR_Y + SCROLLBAR_HEIGHT) {
            
            // Calculate new scroll position based on click
            float clickRatio = (float)(mouseY - (guiY + SCROLLBAR_Y)) / SCROLLBAR_HEIGHT;
            clickRatio = Math.max(0, Math.min(1, clickRatio));
            
            int maxScrollOffset = Math.max(0, MAX_FILTER_SLOTS - VISIBLE_EDIT_BOXES);
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
        if (MAX_FILTER_SLOTS > VISIBLE_EDIT_BOXES && filterScrollOffset > 0) {
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
        int maxScrollOffset = Math.max(0, MAX_FILTER_SLOTS - VISIBLE_EDIT_BOXES);
        
        if (MAX_FILTER_SLOTS > VISIBLE_EDIT_BOXES && filterScrollOffset < maxScrollOffset) {
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
        int maxScrollOffset = Math.max(0, MAX_FILTER_SLOTS - VISIBLE_EDIT_BOXES);
        this.filterScrollOffset = Math.max(0, Math.min(maxScrollOffset, offset));
        updateFilterEditBoxes();
    }
    
    @Override
    public void onClose() {
        // Save data when closing
        saveFilterData();
        super.onClose();
    }
}
