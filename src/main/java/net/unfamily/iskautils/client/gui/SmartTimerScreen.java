package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.network.ModMessages;

public class SmartTimerScreen extends AbstractContainerScreen<SmartTimerMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/backgrounds/redstone_machine.png");
    private static final int GUI_WIDTH = 280;
    private static final int GUI_HEIGHT = 200;
    
    // Medium buttons texture (16x32 - normal and highlighted)
    private static final ResourceLocation MEDIUM_BUTTONS = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/medium_buttons.png");
    // Redstone GUI icon
    private static final ResourceLocation REDSTONE_GUI = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/redstone_gui.png");
    
    // Close button
    private Button closeButton;
    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;
    
    // Custom redstone mode button
    private int redstoneModeButtonX, redstoneModeButtonY;
    private static final int REDSTONE_BUTTON_SIZE = 16;
    
    // Configuration button (also serves as Back button in config mode)
    private int configButtonX, configButtonY;
    private static final int CONFIG_BUTTON_SIZE = 16;
    
    // Screen mode: false = main screen, true = configuration screen
    private boolean isConfigurationMode = false;
    
    // Text positions (spacing halved)
    private static final int COOLDOWN_LABEL_Y = 50;
    private static final int COOLDOWN_TICKS_Y = 58; // Line below for "total ticks" (8px instead of 15px)
    private static final int COOLDOWN_BUTTONS_Y = 70; // Buttons below total ticks
    private static final int SIGNAL_DURATION_LABEL_Y = 110;
    private static final int SIGNAL_DURATION_TICKS_Y = 118; // Line below for "total ticks" (8px instead of 15px)
    private static final int SIGNAL_DURATION_BUTTONS_Y = 130; // Buttons below total ticks
    // Calculate buttons total width: 5 normal (32px) + 2 tick (40px) + 6 spacings (1px) = 246px
    // GUI width is 280px, so to center better: (280 - 246) / 2 = 17px margin
    private static final int TEXT_START_X = 17;
    
    // Button dimensions
    private static final int BUTTON_WIDTH_NORMAL = 32; // Width for normal buttons
    private static final int BUTTON_WIDTH_TICK = 40; // Width for tick buttons
    private static final int BUTTON_HEIGHT = 14;
    private static final int BUTTON_SPACING_X = 1; // Reduced spacing
    private static final int BUTTON_SPACING_Y = 2;
    
    // Current values (in ticks)
    private int currentCooldownTicks = 100; // 5 seconds default
    private int currentSignalDurationTicks = 60; // 3 seconds default
    
    // Minimum values
    private static final int MIN_TICKS = 5;

    public SmartTimerScreen(SmartTimerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        
        // Initialize redstone mode button position (between title and cooldown label, right side with margin from close button)
        // Title is at Y=8, cooldown label is at Y=50, so center around Y=29
        this.redstoneModeButtonY = 29;
        // Position on right side but with good margin from close button (close button X=263, button size 16, margin ~30px)
        this.redstoneModeButtonX = GUI_WIDTH - REDSTONE_BUTTON_SIZE - 30;
        
        // Configuration button positioned to the left of redstone button with spacing
        this.configButtonY = 29;
        this.configButtonX = (GUI_WIDTH - REDSTONE_BUTTON_SIZE - 30) - CONFIG_BUTTON_SIZE - 5; // 5px spacing between buttons
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Load values from ContainerData (synced automatically)
        currentCooldownTicks = menu.getCooldownTicks();
        currentSignalDurationTicks = menu.getSignalDurationTicks();
        
        // Close button
        closeButton = Button.builder(Component.literal("✕"), 
                                    button -> {
                                        playButtonSound();
                                        this.onClose();
                                    })
                           .bounds(this.leftPos + CLOSE_BUTTON_X, this.topPos + CLOSE_BUTTON_Y, 
                                  CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
                           .build();
        addRenderableWidget(closeButton);
        
        // Buttons for cooldown (created always but visibility controlled)
        createButtonsForValue(TEXT_START_X, COOLDOWN_BUTTONS_Y, true);
        
        // Buttons for signal duration (created always but visibility controlled)
        createButtonsForValue(TEXT_START_X, SIGNAL_DURATION_BUTTONS_Y, false);
        
        // Update visibility based on current mode
        updateWidgetVisibility();
    }
    
    private void createButtonsForValue(int startX, int startY, boolean isCooldown) {
        int x = startX;
        int y = startY;
        
        // First row: all + buttons
        addButton(x, y, "+1h", BUTTON_WIDTH_NORMAL, () -> adjustValue(isCooldown, 72000)); // 1 hour
        x += BUTTON_WIDTH_NORMAL + BUTTON_SPACING_X;
        addButton(x, y, "+1m", BUTTON_WIDTH_NORMAL, () -> adjustValue(isCooldown, 1200)); // 1 minute
        x += BUTTON_WIDTH_NORMAL + BUTTON_SPACING_X;
        addButton(x, y, "+10s", BUTTON_WIDTH_NORMAL, () -> adjustValue(isCooldown, 200)); // 10 seconds
        x += BUTTON_WIDTH_NORMAL + BUTTON_SPACING_X;
        addButton(x, y, "+5s", BUTTON_WIDTH_NORMAL, () -> adjustValue(isCooldown, 100)); // 5 seconds
        x += BUTTON_WIDTH_NORMAL + BUTTON_SPACING_X;
        addButton(x, y, "+1s", BUTTON_WIDTH_NORMAL, () -> adjustValue(isCooldown, 20)); // 1 second
        x += BUTTON_WIDTH_NORMAL + BUTTON_SPACING_X;
        addButton(x, y, "+10t/0.5s", BUTTON_WIDTH_TICK, () -> adjustValue(isCooldown, 10)); // 10 ticks (0.5s)
        x += BUTTON_WIDTH_TICK + BUTTON_SPACING_X;
        addButton(x, y, "+5t/0.25s", BUTTON_WIDTH_TICK, () -> adjustValue(isCooldown, 5)); // 5 ticks (0.25s)
        
        // Second row: all - buttons
        x = startX;
        y += BUTTON_HEIGHT + BUTTON_SPACING_Y;
        addButton(x, y, "-1h", BUTTON_WIDTH_NORMAL, () -> adjustValue(isCooldown, -72000));
        x += BUTTON_WIDTH_NORMAL + BUTTON_SPACING_X;
        addButton(x, y, "-1m", BUTTON_WIDTH_NORMAL, () -> adjustValue(isCooldown, -1200));
        x += BUTTON_WIDTH_NORMAL + BUTTON_SPACING_X;
        addButton(x, y, "-10s", BUTTON_WIDTH_NORMAL, () -> adjustValue(isCooldown, -200));
        x += BUTTON_WIDTH_NORMAL + BUTTON_SPACING_X;
        addButton(x, y, "-5s", BUTTON_WIDTH_NORMAL, () -> adjustValue(isCooldown, -100));
        x += BUTTON_WIDTH_NORMAL + BUTTON_SPACING_X;
        addButton(x, y, "-1s", BUTTON_WIDTH_NORMAL, () -> adjustValue(isCooldown, -20)); // 1 second
        x += BUTTON_WIDTH_NORMAL + BUTTON_SPACING_X;
        addButton(x, y, "-10t/0.5s", BUTTON_WIDTH_TICK, () -> adjustValue(isCooldown, -10));
        x += BUTTON_WIDTH_TICK + BUTTON_SPACING_X;
        addButton(x, y, "-5t/0.25s", BUTTON_WIDTH_TICK, () -> adjustValue(isCooldown, -5));
    }
    
    private void addButton(int x, int y, String label, int width, Runnable action) {
        Button button = Button.builder(Component.literal(label), 
                buttonWidget -> {
                    playButtonSound();
                    action.run();
                })
                .bounds(this.leftPos + x, this.topPos + y, width, BUTTON_HEIGHT)
                .build();
        addRenderableWidget(button);
    }
    
    private void adjustValue(boolean isCooldown, int delta) {
        // Get position from menu (synced from server)
        BlockPos pos = menu.getSyncedBlockPos();
        if (!pos.equals(net.minecraft.core.BlockPos.ZERO)) {
            // Send delta to server, server will calculate new value from current BlockEntity value
            ModMessages.sendSmartTimerUpdatePacket(pos, isCooldown, delta);
        }
    }
    
    /**
     * Updates values from ContainerData periodically
     */
    @Override
    public void containerTick() {
        super.containerTick();
        
        // Always update values from ContainerData (synced automatically from server)
        // This ensures we always show the server's authoritative values
        currentCooldownTicks = menu.getCooldownTicks();
        currentSignalDurationTicks = menu.getSignalDurationTicks();
    }
    
    /**
     * Formats the time part: "1h 2m 10s + 2t"
     */
    private String formatTimePart(int ticks) {
        int hours = ticks / 72000; // 1 hour = 3600 seconds = 72000 ticks
        ticks %= 72000;
        int minutes = ticks / 1200; // 1 minute = 60 seconds = 1200 ticks
        ticks %= 1200;
        int seconds = ticks / 20; // 1 second = 20 ticks
        int remainingTicks = ticks % 20;
        
        StringBuilder sb = new StringBuilder();
        boolean hasValue = false;
        
        if (hours > 0) {
            sb.append(hours).append("h ");
            hasValue = true;
        }
        if (minutes > 0 || hasValue) {
            sb.append(minutes).append("m ");
            hasValue = true;
        }
        if (seconds > 0 || hasValue || remainingTicks > 0) {
            sb.append(seconds).append("s");
            hasValue = true;
        }
        if (remainingTicks > 0) {
            sb.append(" + ").append(remainingTicks).append("t");
        }
        
        return sb.toString();
    }
    
    /**
     * Formats the "total ticks: X" part
     */
    private String formatTicksPart(int ticks) {
        return "total ticks: " + ticks;
    }
    
    private void playButtonSound() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);
        
        // Always render redstone mode button and configuration/back button
        renderRedstoneModeButton(guiGraphics, mouseX, mouseY);
        renderConfigurationButton(guiGraphics, mouseX, mouseY);
        
        // Render I/O configuration buttons if in configuration mode
        if (isConfigurationMode) {
            renderIoConfigurationButtons(guiGraphics, mouseX, mouseY);
        }
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isConfigurationMode) {
            // Configuration screen title
            Component title = Component.translatable("gui.iska_utils.smart_timer.config.title");
            int titleWidth = this.font.width(title);
            guiGraphics.drawString(this.font, title, (this.imageWidth - titleWidth) / 2, 8, 0x404040, false);
        } else {
            // Main screen
            Component title = Component.translatable("block.iska_utils.smart_timer");
            int titleWidth = this.font.width(title);
            guiGraphics.drawString(this.font, title, (this.imageWidth - titleWidth) / 2, 8, 0x404040, false);
            
            // Label and value for cooldown (Redstone off for)
            Component cooldownLabel = Component.translatable("gui.iska_utils.smart_timer.cooldown");
            String cooldownTime = formatTimePart(currentCooldownTicks);
            String cooldownLabelText = cooldownLabel.getString() + " " + cooldownTime;
            guiGraphics.drawString(this.font, cooldownLabelText, TEXT_START_X, COOLDOWN_LABEL_Y, 0x404040, false);
            
            String cooldownTicks = formatTicksPart(currentCooldownTicks);
            guiGraphics.drawString(this.font, cooldownTicks, TEXT_START_X, COOLDOWN_TICKS_Y, 0x404040, false);
            
            // Label and value for signal duration (Redstone on for)
            Component signalDurationLabel = Component.translatable("gui.iska_utils.smart_timer.signal_duration");
            String signalDurationTime = formatTimePart(currentSignalDurationTicks);
            String signalDurationLabelText = signalDurationLabel.getString() + " " + signalDurationTime;
            guiGraphics.drawString(this.font, signalDurationLabelText, TEXT_START_X, SIGNAL_DURATION_LABEL_Y, 0x404040, false);
            
            String signalDurationTicks = formatTicksPart(currentSignalDurationTicks);
            guiGraphics.drawString(this.font, signalDurationTicks, TEXT_START_X, SIGNAL_DURATION_TICKS_Y, 0x404040, false);
        }
    }
    
    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        
        // Render tooltips for I/O buttons if in configuration mode
        if (isConfigurationMode) {
            renderIoTooltips(guiGraphics, mouseX, mouseY);
        }
    }

    private void renderConfigurationButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Calculate absolute position
        int buttonX = this.leftPos + this.configButtonX;
        int buttonY = this.topPos + this.configButtonY;
        
        // Check if mouse is over the button
        boolean isHovered = mouseX >= buttonX && mouseX <= buttonX + CONFIG_BUTTON_SIZE &&
                           mouseY >= buttonY && mouseY <= buttonY + CONFIG_BUTTON_SIZE;
        
        // Draw button background (normal or highlighted)
        int textureY = isHovered ? 16 : 0;
        guiGraphics.blit(MEDIUM_BUTTONS, buttonX, buttonY, 
                        0, textureY, CONFIG_BUTTON_SIZE, CONFIG_BUTTON_SIZE, 
                        96, 96);
        
        // Draw icon: Swiss Wrench for both Configuration and Back modes
        int iconX = buttonX + 2;
        int iconY = buttonY + 2;
        int iconSize = 12;
        
        // Always use Swiss Wrench icon
        net.minecraft.world.item.ItemStack swissWrench = new net.minecraft.world.item.ItemStack(ModItems.SWISS_WRENCH.get());
        renderScaledItem(guiGraphics, swissWrench, iconX, iconY, iconSize);
    }
    
    private void renderRedstoneModeButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Calculate absolute position
        int buttonX = this.leftPos + this.redstoneModeButtonX;
        int buttonY = this.topPos + this.redstoneModeButtonY;
        
        // Check if mouse is over the button
        boolean isHovered = mouseX >= buttonX && mouseX <= buttonX + REDSTONE_BUTTON_SIZE &&
                           mouseY >= buttonY && mouseY <= buttonY + REDSTONE_BUTTON_SIZE;
        
        // Draw button background (normal or highlighted)
        int textureY = isHovered ? 16 : 0; // Highlighted version is below the normal one
        guiGraphics.blit(MEDIUM_BUTTONS, buttonX, buttonY, 
                        0, textureY, REDSTONE_BUTTON_SIZE, REDSTONE_BUTTON_SIZE, 
                        96, 96); // Correct texture size: 96x96
        
        // Get current redstone mode from menu
        int redstoneMode = this.menu.getRedstoneMode();
        
        // Draw the appropriate icon (12x12 pixels, centered in the 16x16 button)
        int iconX = buttonX + 2; // Center: (16-12)/2 = 2
        int iconY = buttonY + 2; // Center: (16-12)/2 = 2
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
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Always render tooltips for redstone and config/back buttons
        renderRedstoneModeTooltip(guiGraphics, mouseX, mouseY);
        renderConfigurationTooltip(guiGraphics, mouseX, mouseY);
        
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    private void renderRedstoneModeTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Calculate absolute position
        int buttonX = this.leftPos + this.redstoneModeButtonX;
        int buttonY = this.topPos + this.redstoneModeButtonY;
        
        // Check if mouse is over the button
        boolean isHovered = mouseX >= buttonX && mouseX <= buttonX + REDSTONE_BUTTON_SIZE &&
                           mouseY >= buttonY && mouseY <= buttonY + REDSTONE_BUTTON_SIZE;
        
        if (isHovered) {
            // Get current redstone mode from menu
            int redstoneMode = this.menu.getRedstoneMode();
            
            // Draw the appropriate tooltip
            Component tooltip = switch (redstoneMode) {
                case 0 -> Component.translatable("gui.iska_utils.smart_timer.redstone_mode.none");
                case 1 -> Component.translatable("gui.iska_utils.smart_timer.redstone_mode.low");
                case 2 -> Component.translatable("gui.iska_utils.smart_timer.redstone_mode.high");
                case 3 -> Component.translatable("gui.iska_utils.smart_timer.redstone_mode.pulse");
                default -> Component.literal("Unknown mode");
            };
            
            // Use the standard tooltip rendering system (with background and border)
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            boolean isShiftDown = this.minecraft != null && this.minecraft.player != null && 
                                  this.minecraft.player.isShiftKeyDown();
            
            // Check if click is on redstone mode button (always visible)
            int buttonX = this.leftPos + this.redstoneModeButtonX;
            int buttonY = this.topPos + this.redstoneModeButtonY;
            if (mouseX >= buttonX && mouseX <= buttonX + REDSTONE_BUTTON_SIZE &&
                mouseY >= buttonY && mouseY <= buttonY + REDSTONE_BUTTON_SIZE) {
                
                onRedstoneModePressed();
                return true;
            }
            
            // Check if click is on configuration/back button (always visible)
            buttonX = this.leftPos + this.configButtonX;
            buttonY = this.topPos + this.configButtonY;
            if (mouseX >= buttonX && mouseX <= buttonX + CONFIG_BUTTON_SIZE &&
                mouseY >= buttonY && mouseY <= buttonY + CONFIG_BUTTON_SIZE) {
                
                onConfigurationButtonPressed();
                return true;
            }
            
            // In configuration mode, handle I/O button clicks
            if (isConfigurationMode) {
                if (handleIoButtonClick(mouseX, mouseY, isShiftDown)) {
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isConfigurationMode) {
            // In configuration mode, ESC or inventory key returns to main screen
            if (keyCode == 256 || // ESC key
                (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode))) {
                playButtonSound();
                switchToMainScreen();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    private void onRedstoneModePressed() {
        // Get position from menu (synced from server)
        BlockPos pos = menu.getSyncedBlockPos();
        if (!pos.equals(net.minecraft.core.BlockPos.ZERO)) {
            // Send redstone mode packet to cycle the mode
            ModMessages.sendSmartTimerRedstoneModePacket(pos);
            playButtonSound();
        }
    }
    
    private void onConfigurationButtonPressed() {
        playButtonSound();
        if (isConfigurationMode) {
            // If in config mode, button acts as Back - return to main screen
            switchToMainScreen();
        } else {
            // If in main mode, button acts as Configuration - go to config screen
            switchToConfigurationScreen();
        }
    }
    
    private void switchToConfigurationScreen() {
        isConfigurationMode = true;
        updateWidgetVisibility();
    }
    
    private void switchToMainScreen() {
        isConfigurationMode = false;
        updateWidgetVisibility();
    }
    
    private void updateWidgetVisibility() {
        // Hide/show widgets based on current mode
        for (var widget : this.renderables) {
            // Close button is always visible
            if (widget == closeButton) {
                continue;
            }
            // All timer buttons are only visible in main mode
            if (widget instanceof Button) {
                ((Button) widget).visible = !isConfigurationMode;
            }
        }
    }
    
    private void renderConfigurationTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Calculate absolute position
        int buttonX = this.leftPos + this.configButtonX;
        int buttonY = this.topPos + this.configButtonY;
        
        // Check if mouse is over the button
        boolean isHovered = mouseX >= buttonX && mouseX <= buttonX + CONFIG_BUTTON_SIZE &&
                           mouseY >= buttonY && mouseY <= buttonY + CONFIG_BUTTON_SIZE;
        
        if (isHovered) {
            Component tooltip = isConfigurationMode ? 
                Component.translatable("gui.iska_utils.smart_timer.config.back") :
                Component.translatable("gui.iska_utils.smart_timer.config.button");
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }
    
    /**
     * Renders the I/O configuration buttons in a 2D layout
     * Layout:
     *     [UP]
     * [WEST] [FRONT] [EAST]
     *     [DOWN]
     *              [BACK]
     */
    private void renderIoConfigurationButtons(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.minecraft == null || this.minecraft.level == null) {
            return;
        }
        
        // Get block state to determine facing direction
        net.minecraft.core.BlockPos blockPos = menu.getSyncedBlockPos();
        if (blockPos.equals(net.minecraft.core.BlockPos.ZERO)) {
            return;
        }
        
        net.minecraft.world.level.block.state.BlockState blockState = this.minecraft.level.getBlockState(blockPos);
        if (!(blockState.getBlock() instanceof net.unfamily.iskautils.block.SmartTimerBlock)) {
            return;
        }
        
        net.minecraft.core.Direction facing = blockState.getValue(net.unfamily.iskautils.block.SmartTimerBlock.FACING);
        net.minecraft.core.Direction back = facing.getOpposite();
        
        // Center position for the layout
        int centerX = GUI_WIDTH / 2;
        int centerY = GUI_HEIGHT / 2;
        int buttonSize = 20; // Size of each button
        int spacing = 5; // Spacing between buttons
        
        // Calculate positions relative to GUI (then add leftPos/topPos when rendering)
        int frontX = centerX - buttonSize / 2;
        int frontY = centerY - buttonSize / 2;
        
        // UP (above front)
        int upX = frontX;
        int upY = frontY - buttonSize - spacing;
        
        // DOWN (below front)
        int downX = frontX;
        int downY = frontY + buttonSize + spacing;
        
        // WEST (left of front)
        int westX = frontX - buttonSize - spacing;
        int westY = frontY;
        
        // EAST (right of front)
        int eastX = frontX + buttonSize + spacing;
        int eastY = frontY;
        
        // BACK (top-right corner)
        int backX = GUI_WIDTH - buttonSize - 30; // Margin from right edge
        int backY = 30; // Top area
        
        // Render front (non-clickable, displayed)
        renderIoButton(guiGraphics, mouseX, mouseY, facing, frontX, frontY, buttonSize, true);
        
        // Render other faces (clickable, excluding front)
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            if (dir == facing) continue; // Skip front, already rendered
            
            int x, y;
            if (dir == net.minecraft.core.Direction.UP) {
                x = upX;
                y = upY;
            } else if (dir == net.minecraft.core.Direction.DOWN) {
                x = downX;
                y = downY;
            } else if (dir == net.minecraft.core.Direction.WEST) {
                x = westX;
                y = westY;
            } else if (dir == net.minecraft.core.Direction.EAST) {
                x = eastX;
                y = eastY;
            } else if (dir == back) {
                x = backX;
                y = backY;
            } else {
                continue;
            }
            
            renderIoButton(guiGraphics, mouseX, mouseY, dir, x, y, buttonSize, false);
        }
    }
    
    /**
     * Renders a single I/O configuration button
     */
    private void renderIoButton(GuiGraphics guiGraphics, int mouseX, int mouseY, 
                                net.minecraft.core.Direction direction, int x, int y, int size, boolean isFront) {
        int absoluteX = this.leftPos + x;
        int absoluteY = this.topPos + y;
        
        // Check if mouse is over the button
        boolean isHovered = !isFront && mouseX >= absoluteX && mouseX <= absoluteX + size &&
                           mouseY >= absoluteY && mouseY <= absoluteY + size;
        
        // Get I/O config for this direction
        byte ioConfig = menu.getIoConfig(direction);
        
        // Determine colors based on I/O type
        int backgroundColor;
        int borderColor;
        if (isFront) {
            backgroundColor = 0xFF808080; // Gray for front (non-configurable)
            borderColor = 0xFF606060;
        } else {
            switch (ioConfig) {
                case 1 -> { // INPUT
                    backgroundColor = isHovered ? 0xFF4488FF : 0xFF3366CC;
                    borderColor = 0xFF2255AA;
                }
                case 2 -> { // OUTPUT
                    backgroundColor = isHovered ? 0xFFFF8844 : 0xFFCC6633;
                    borderColor = 0xFFAA5522;
                }
                default -> { // BLANK (0)
                    backgroundColor = isHovered ? 0xFFAAAAAA : 0xFF888888;
                    borderColor = 0xFF666666;
                }
            }
        }
        
        // Draw button background
        guiGraphics.fill(absoluteX, absoluteY, absoluteX + size, absoluteY + size, backgroundColor);
        // Draw border
        guiGraphics.fill(absoluteX, absoluteY, absoluteX + size, absoluteY + 1, borderColor); // Top
        guiGraphics.fill(absoluteX, absoluteY + size - 1, absoluteX + size, absoluteY + size, borderColor); // Bottom
        guiGraphics.fill(absoluteX, absoluteY, absoluteX + 1, absoluteY + size, borderColor); // Left
        guiGraphics.fill(absoluteX + size - 1, absoluteY, absoluteX + size, absoluteY + size, borderColor); // Right
        
        // Draw direction label
        String label = getDirectionLabel(direction);
        int labelWidth = this.font.width(label);
        int labelX = absoluteX + (size - labelWidth) / 2;
        int labelY = absoluteY + (size - this.font.lineHeight) / 2;
        int textColor = isFront ? 0xFF404040 : 0xFFFFFFFF;
        guiGraphics.drawString(this.font, label, labelX, labelY, textColor, false);
    }
    
    /**
     * Gets a short label for a direction
     */
    private String getDirectionLabel(net.minecraft.core.Direction direction) {
        return switch (direction) {
            case UP -> "↑";
            case DOWN -> "↓";
            case NORTH -> "N";
            case SOUTH -> "S";
            case WEST -> "W";
            case EAST -> "E";
        };
    }
    
    /**
     * Handles clicks on I/O configuration buttons
     */
    private boolean handleIoButtonClick(double mouseX, double mouseY, boolean isShiftDown) {
        if (this.minecraft == null || this.minecraft.level == null) {
            return false;
        }
        
        net.minecraft.core.BlockPos blockPos = menu.getSyncedBlockPos();
        if (blockPos.equals(net.minecraft.core.BlockPos.ZERO)) {
            return false;
        }
        
        net.minecraft.world.level.block.state.BlockState blockState = this.minecraft.level.getBlockState(blockPos);
        if (!(blockState.getBlock() instanceof net.unfamily.iskautils.block.SmartTimerBlock)) {
            return false;
        }
        
        net.minecraft.core.Direction facing = blockState.getValue(net.unfamily.iskautils.block.SmartTimerBlock.FACING);
        
        // Center position for the layout
        int centerX = GUI_WIDTH / 2;
        int centerY = GUI_HEIGHT / 2;
        int buttonSize = 20;
        int spacing = 5;
        
        int frontX = centerX - buttonSize / 2;
        int frontY = centerY - buttonSize / 2;
        
        // Check if clicking on front (shift+click = reset all)
        int absoluteFrontX = this.leftPos + frontX;
        int absoluteFrontY = this.topPos + frontY;
        if (mouseX >= absoluteFrontX && mouseX <= absoluteFrontX + buttonSize &&
            mouseY >= absoluteFrontY && mouseY <= absoluteFrontY + buttonSize) {
            if (isShiftDown) {
                // Shift+click on front = reset all faces
                ModMessages.sendSmartTimerIoConfigResetPacket(blockPos);
                playButtonSound();
                return true;
            }
            // Normal click on front does nothing (front is not configurable)
            return false;
        }
        
        // Check other faces
        int upX = frontX;
        int upY = frontY - buttonSize - spacing;
        int downX = frontX;
        int downY = frontY + buttonSize + spacing;
        int westX = frontX - buttonSize - spacing;
        int westY = frontY;
        int eastX = frontX + buttonSize + spacing;
        int eastY = frontY;
        int backX = GUI_WIDTH - buttonSize - 30;
        int backY = 30;
        net.minecraft.core.Direction back = facing.getOpposite();
        
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            if (dir == facing) continue;
            
            int x, y;
            if (dir == net.minecraft.core.Direction.UP) {
                x = upX;
                y = upY;
            } else if (dir == net.minecraft.core.Direction.DOWN) {
                x = downX;
                y = downY;
            } else if (dir == net.minecraft.core.Direction.WEST) {
                x = westX;
                y = westY;
            } else if (dir == net.minecraft.core.Direction.EAST) {
                x = eastX;
                y = eastY;
            } else if (dir == back) {
                x = backX;
                y = backY;
            } else {
                continue;
            }
            
            int absoluteX = this.leftPos + x;
            int absoluteY = this.topPos + y;
            if (mouseX >= absoluteX && mouseX <= absoluteX + buttonSize &&
                mouseY >= absoluteY && mouseY <= absoluteY + buttonSize) {
                // Cycle I/O config for this direction
                ModMessages.sendSmartTimerIoConfigCyclePacket(blockPos, dir);
                playButtonSound();
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Renders tooltips for I/O configuration buttons
     */
    private void renderIoTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.minecraft == null || this.minecraft.level == null) {
            return;
        }
        
        net.minecraft.core.BlockPos blockPos = menu.getSyncedBlockPos();
        if (blockPos.equals(net.minecraft.core.BlockPos.ZERO)) {
            return;
        }
        
        net.minecraft.world.level.block.state.BlockState blockState = this.minecraft.level.getBlockState(blockPos);
        if (!(blockState.getBlock() instanceof net.unfamily.iskautils.block.SmartTimerBlock)) {
            return;
        }
        
        net.minecraft.core.Direction facing = blockState.getValue(net.unfamily.iskautils.block.SmartTimerBlock.FACING);
        
        // Same layout calculations as renderIoConfigurationButtons
        int centerX = GUI_WIDTH / 2;
        int centerY = GUI_HEIGHT / 2;
        int buttonSize = 20;
        int spacing = 5;
        int frontX = centerX - buttonSize / 2;
        int frontY = centerY - buttonSize / 2;
        int upX = frontX;
        int upY = frontY - buttonSize - spacing;
        int downX = frontX;
        int downY = frontY + buttonSize + spacing;
        int westX = frontX - buttonSize - spacing;
        int westY = frontY;
        int eastX = frontX + buttonSize + spacing;
        int eastY = frontY;
        int backX = GUI_WIDTH - buttonSize - 30;
        int backY = 30;
        net.minecraft.core.Direction back = facing.getOpposite();
        
        // Check front (shift+click tooltip)
        int absoluteFrontX = this.leftPos + frontX;
        int absoluteFrontY = this.topPos + frontY;
        if (mouseX >= absoluteFrontX && mouseX <= absoluteFrontX + buttonSize &&
            mouseY >= absoluteFrontY && mouseY <= absoluteFrontY + buttonSize) {
            Component tooltip = Component.translatable("gui.iska_utils.smart_timer.config.reset_all");
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
            return;
        }
        
        // Check other faces
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            if (dir == facing) continue;
            
            int x, y;
            if (dir == net.minecraft.core.Direction.UP) {
                x = upX;
                y = upY;
            } else if (dir == net.minecraft.core.Direction.DOWN) {
                x = downX;
                y = downY;
            } else if (dir == net.minecraft.core.Direction.WEST) {
                x = westX;
                y = westY;
            } else if (dir == net.minecraft.core.Direction.EAST) {
                x = eastX;
                y = eastY;
            } else if (dir == back) {
                x = backX;
                y = backY;
            } else {
                continue;
            }
            
            int absoluteX = this.leftPos + x;
            int absoluteY = this.topPos + y;
            if (mouseX >= absoluteX && mouseX <= absoluteX + buttonSize &&
                mouseY >= absoluteY && mouseY <= absoluteY + buttonSize) {
                byte ioConfig = menu.getIoConfig(dir);
                Component tooltip = switch (ioConfig) {
                    case 1 -> Component.translatable("gui.iska_utils.smart_timer.config.io.input");
                    case 2 -> Component.translatable("gui.iska_utils.smart_timer.config.io.output");
                    default -> Component.translatable("gui.iska_utils.smart_timer.config.io.blank");
                };
                guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
                return;
            }
        }
    }
}
