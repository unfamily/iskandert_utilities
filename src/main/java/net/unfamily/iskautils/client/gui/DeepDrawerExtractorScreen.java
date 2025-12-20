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
 * Shows 11 filter text fields, allow/deny button, and help text
 */
public class DeepDrawerExtractorScreen extends AbstractContainerScreen<DeepDrawerExtractorMenu> {
    
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/backgrounds/deep_drawer_extractor.png");
    
    // Medium buttons texture (16x32 - normal and highlighted)
    private static final ResourceLocation MEDIUM_BUTTONS = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/medium_buttons.png");
    // Redstone GUI icon
    private static final ResourceLocation REDSTONE_GUI = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/redstone_gui.png");
    
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
    
    // EditBox dimensions and positions (single column)
    private static final int EDIT_BOX_HEIGHT = 15;
    private static final int EDIT_BOX_SPACING = 2;
    private static final int EDIT_BOX_WIDTH = 184; // Full width minus margins (200 - 8*2 = 184)
    private static final int EDIT_BOX_X = 8;
    private static final int FIRST_ROW_Y = 55; // Below button with spacing
    
    // Button dimensions and position (on the right side, after EditBoxes)
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_X = EDIT_BOX_X + EDIT_BOX_WIDTH + 8; // Right side, after EditBoxes + spacing
    private static final int BUTTON_Y = FIRST_ROW_Y; // Start at same Y as first EditBox
    
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
    
    // EditBoxes for filter fields (11 total, one per row)
    private final EditBox[] filterEditBoxes = new EditBox[11];
    
    // Track current mode locally (for immediate UI feedback on button click)
    private boolean isWhitelistMode = false;
    
    public DeepDrawerExtractorScreen(DeepDrawerExtractorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Load initial mode from ContainerData (like rotation in StructurePlacerMachineScreen)
        isWhitelistMode = menu.getWhitelistMode();
        
        // Initialize EditBoxes with empty values - data will be loaded in containerTick()
        // Create 11 EditBoxes (one per row, single column)
        for (int i = 0; i < 11; i++) {
            int y = FIRST_ROW_Y + i * (EDIT_BOX_HEIGHT + EDIT_BOX_SPACING);
            
            EditBox editBox = new EditBox(this.font, 
                    this.leftPos + EDIT_BOX_X, 
                    this.topPos + y,
                    EDIT_BOX_WIDTH, 
                    EDIT_BOX_HEIGHT, 
                    Component.empty());
            editBox.setMaxLength(100);
            editBox.setValue(""); // Will be updated in containerTick()
            // Save data when EditBox value changes
            editBox.setResponder(value -> saveFilterData());
            filterEditBoxes[i] = editBox;
            addRenderableWidget(editBox);
        }
        
        // Close button
        closeButton = Button.builder(Component.literal("✕"), 
                                    button -> onCloseButtonClicked())
                           .bounds(this.leftPos + CLOSE_BUTTON_X, this.topPos + CLOSE_BUTTON_Y, 
                                  CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
                           .build();
        addRenderableWidget(closeButton);
        
        // How to use button (on the right side)
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
        playButtonSound();
        if (isHowToUseMode) {
            // In how to use mode, close button acts as back
            switchToMainScreen();
        } else {
            // In main mode, close button closes GUI
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
        
        // Update EditBoxes
        for (EditBox editBox : filterEditBoxes) {
            if (editBox != null) {
                editBox.visible = showMain;
            }
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !isHowToUseMode) { // Left click, only in main mode
            // Check if click is on redstone mode button
            if (mouseX >= this.redstoneModeButtonX && mouseX <= this.redstoneModeButtonX + REDSTONE_BUTTON_SIZE &&
                mouseY >= this.redstoneModeButtonY && mouseY <= this.redstoneModeButtonY + REDSTONE_BUTTON_SIZE) {
                
                onRedstoneModePressed();
                return true;
            }
        }
        
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
            // Collect filter field values
            String[] filterFields = new String[11];
            for (int i = 0; i < 11; i++) {
                filterFields[i] = filterEditBoxes[i].getValue().trim();
                if (filterFields[i].isEmpty()) {
                    filterFields[i] = null;
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
        guiGraphics.blit(BACKGROUND, x, y, 0, 0, this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);
        
        // Render redstone mode button (only in main mode, not in how to use)
        if (!isHowToUseMode) {
            renderRedstoneModeButton(guiGraphics, mouseX, mouseY);
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
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isHowToUseMode) {
            // How to use screen title
            Component titleComponent = Component.translatable("gui.iska_utils.deep_drawer_extractor.how_to_use");
            int titleWidth = this.font.width(titleComponent);
            int titleX = (this.imageWidth - titleWidth) / 2;
            guiGraphics.drawString(this.font, titleComponent, titleX, TITLE_Y, 0x404040, false);
            
            // Render help text with default font size
            int helpY = HELP_TEXT_START_Y;
            guiGraphics.drawString(this.font, Component.translatable("gui.iska_utils.general_filter_text.id"), HELP_TEXT_X, helpY, 0x404040, false);
            helpY += HELP_TEXT_LINE_HEIGHT;
            
            guiGraphics.drawString(this.font, Component.translatable("gui.iska_utils.general_filter_text.tag"), HELP_TEXT_X, helpY, 0x404040, false);
            helpY += HELP_TEXT_LINE_HEIGHT;
            
            guiGraphics.drawString(this.font, Component.translatable("gui.iska_utils.general_filter_text.modid"), HELP_TEXT_X, helpY, 0x404040, false);
            helpY += HELP_TEXT_LINE_HEIGHT;
            
            // NBT: description on first line, example on second line
            guiGraphics.drawString(this.font, Component.translatable("gui.iska_utils.general_filter_text.nbt"), HELP_TEXT_X, helpY, 0x404040, false);
            helpY += HELP_TEXT_LINE_HEIGHT;
            guiGraphics.drawString(this.font, Component.translatable("gui.iska_utils.general_filter_text.nbt.example"), HELP_TEXT_X, helpY, 0x404040, false);
            helpY += HELP_TEXT_LINE_HEIGHT;
            
            guiGraphics.drawString(this.font, Component.translatable("gui.iska_utils.general_filter_text.macro"), HELP_TEXT_X, helpY, 0x404040, false);
            helpY += HELP_TEXT_LINE_HEIGHT;
            
            // Usage: add extra spacing before the command
            helpY += HELP_TEXT_LINE_HEIGHT; // Extra spacing
            guiGraphics.drawString(this.font, Component.translatable("gui.iska_utils.general_filter_text.usage"), HELP_TEXT_X, helpY, 0x404040, false);
            helpY += HELP_TEXT_LINE_HEIGHT;
            guiGraphics.drawString(this.font, Component.translatable("gui.iska_utils.general_filter_text.usage.what"), HELP_TEXT_X, helpY, 0x404040, false);
        } else {
            // Main screen title
            Component titleComponent = this.title;
            int titleWidth = this.font.width(titleComponent);
            int titleX = (this.imageWidth - titleWidth) / 2;
            guiGraphics.drawString(this.font, titleComponent, titleX, TITLE_Y, 0x404040, false);
        }
    }
    
    @Override
    public void containerTick() {
        super.containerTick();
        
        // Update cached filters from server (like redstone mode and structure)
        menu.updateCachedFilters();
        
        // Get cached data for filter fields
        String[] filterFields = menu.getCachedFilterFields();
        
        // Update EditBoxes with cached data (only if different to avoid triggering responder)
        for (int i = 0; i < 11 && i < filterEditBoxes.length && i < filterFields.length; i++) {
            if (filterEditBoxes[i] != null) {
                String value = filterFields[i] != null ? filterFields[i] : "";
                // Only update if different and EditBox is not focused to avoid overwriting user input
                if (!filterEditBoxes[i].getValue().equals(value) && !filterEditBoxes[i].isFocused()) {
                    filterEditBoxes[i].setValue(value);
                }
            }
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
            // This works even if the user has changed the inventory key binding
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
                    playButtonSound();
                    this.onClose();
                    return true;
                }
            }
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public void onClose() {
        // Save data when closing
        saveFilterData();
        super.onClose();
    }
}
