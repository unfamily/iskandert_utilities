package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.network.ModMessages;

public class SmartTimerScreen extends AbstractContainerScreen<SmartTimerMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/backgrounds/redstone_machine.png");
    private static final int GUI_WIDTH = 280;
    private static final int GUI_HEIGHT = 200;
    
    // Close button
    private Button closeButton;
    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;
    
    // Redstone mode button
    private static final ResourceLocation MEDIUM_BUTTONS = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/medium_buttons.png");
    private static final ResourceLocation REDSTONE_GUI = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/redstone_gui.png");
    private static final int REDSTONE_BUTTON_SIZE = 16;
    private static final int REDSTONE_BUTTON_X = CLOSE_BUTTON_X - REDSTONE_BUTTON_SIZE - 5; // Left of close button
    private static final int REDSTONE_BUTTON_Y = 40; // Above "redstone off for" label (which is at 50)
    
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
        
        // Buttons for signal duration
        createButtonsForValue(TEXT_START_X, SIGNAL_DURATION_BUTTONS_Y, false);
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
        
        // Render redstone mode button
        renderRedstoneModeButton(guiGraphics, mouseX, mouseY);
    }
    
    private void renderRedstoneModeButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int buttonX = this.leftPos + REDSTONE_BUTTON_X;
        int buttonY = this.topPos + REDSTONE_BUTTON_Y;
        
        // Check if mouse is over the button
        boolean isHovered = mouseX >= buttonX && mouseX <= buttonX + REDSTONE_BUTTON_SIZE &&
                           mouseY >= buttonY && mouseY <= buttonY + REDSTONE_BUTTON_SIZE;
        
        // Draw button background (normal or highlighted)
        int textureY = isHovered ? 16 : 0;
        guiGraphics.blit(MEDIUM_BUTTONS, buttonX, buttonY, 
                        0, textureY, REDSTONE_BUTTON_SIZE, REDSTONE_BUTTON_SIZE, 
                        96, 96);
        
        // Get current redstone mode from menu
        int redstoneMode = menu.getRedstoneMode();
        
        // Draw the appropriate icon (12x12 pixels, centered in the 16x16 button)
        int iconX = buttonX + 2;
        int iconY = buttonY + 2;
        int iconSize = 12;
        
        switch (redstoneMode) {
            case 0 -> {
                // NONE mode: Gunpowder icon
                ItemStack gunpowder = new ItemStack(net.minecraft.world.item.Items.GUNPOWDER);
                renderScaledItem(guiGraphics, gunpowder, iconX, iconY, iconSize);
            }
            case 1 -> {
                // LOW mode: Redstone dust icon
                ItemStack redstone = new ItemStack(net.minecraft.world.item.Items.REDSTONE);
                renderScaledItem(guiGraphics, redstone, iconX, iconY, iconSize);
            }
            case 2 -> {
                // HIGH mode: Redstone GUI texture
                renderScaledTexture(guiGraphics, REDSTONE_GUI, iconX, iconY, iconSize);
            }
            case 4 -> {
                // DISABLED mode: Barrier icon
                ItemStack barrier = new ItemStack(net.minecraft.world.item.Items.BARRIER);
                renderScaledItem(guiGraphics, barrier, iconX, iconY, iconSize);
            }
            default -> {
                // Fallback (should never happen)
                ItemStack redstone = new ItemStack(net.minecraft.world.item.Items.REDSTONE);
                renderScaledItem(guiGraphics, redstone, iconX, iconY, iconSize);
            }
        }
    }
    
    /**
     * Renders an item scaled to the specified size
     */
    private void renderScaledItem(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y, int size) {
        guiGraphics.pose().pushPose();
        float scale = (float) size / 16.0f;
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        guiGraphics.renderItem(itemStack, 0, 0);
        guiGraphics.pose().popPose();
    }
    
    /**
     * Renders a texture scaled to the specified size (like an item)
     */
    private void renderScaledTexture(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int size) {
        guiGraphics.pose().pushPose();
        float scale = (float) size / 16.0f;
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        guiGraphics.blit(texture, 0, 0, 0, 0, 16, 16, 16, 16);
        guiGraphics.pose().popPose();
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
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
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Render tooltips
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        
        // Render redstone mode button tooltip
        renderRedstoneModeTooltip(guiGraphics, mouseX, mouseY);
    }
    
    private void renderRedstoneModeTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int buttonX = this.leftPos + REDSTONE_BUTTON_X;
        int buttonY = this.topPos + REDSTONE_BUTTON_Y;
        if (mouseX >= buttonX && mouseX <= buttonX + REDSTONE_BUTTON_SIZE &&
            mouseY >= buttonY && mouseY <= buttonY + REDSTONE_BUTTON_SIZE) {
            // Get current redstone mode from menu
            int redstoneMode = menu.getRedstoneMode();
            
            // Draw the appropriate tooltip
            Component tooltip = switch (redstoneMode) {
                case 0 -> Component.translatable("gui.iska_utils.generic.redstone_mode.none");
                case 1 -> Component.translatable("gui.iska_utils.generic.redstone_mode.low");
                case 2 -> Component.translatable("gui.iska_utils.generic.redstone_mode.high");
                case 4 -> Component.translatable("gui.iska_utils.generic.redstone_mode.disabled");
                default -> Component.literal("Unknown mode");
            };
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            int buttonX = this.leftPos + REDSTONE_BUTTON_X;
            int buttonY = this.topPos + REDSTONE_BUTTON_Y;
            
            // Check redstone mode button
            if (mouseX >= buttonX && mouseX <= buttonX + REDSTONE_BUTTON_SIZE &&
                mouseY >= buttonY && mouseY <= buttonY + REDSTONE_BUTTON_SIZE) {
                onRedstoneModePressed();
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private void onRedstoneModePressed() {
        playButtonSound();
        BlockPos pos = menu.getSyncedBlockPos();
        if (!pos.equals(net.minecraft.core.BlockPos.ZERO)) {
            ModMessages.sendSmartTimerRedstoneModePacket(pos);
        }
    }
    
    
}
