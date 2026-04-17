package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.unfamily.iskautils.IskaUtils;

public class AutoShopScreen extends AbstractContainerScreen<AutoShopMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/backgrounds/auto_shop.png");
    private static final Identifier MEDIUM_BUTTONS =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/medium_buttons.png");
    private static final Identifier REDSTONE_GUI =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/redstone_gui.png");
    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 160;
    
    // Close button
    private Button closeButton;
    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5; // 5px from right edge

    // Redstone mode button (left of close button)
    private static final int REDSTONE_BUTTON_SIZE = 16;
    private static final int REDSTONE_BUTTON_X = CLOSE_BUTTON_X - REDSTONE_BUTTON_SIZE - 4;
    private int redstoneModeButtonX, redstoneModeButtonY;

    public AutoShopScreen(AutoShopMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GUI_WIDTH, GUI_HEIGHT);
    }
    
    @Override
    protected void init() {
        super.init();
        this.redstoneModeButtonX = this.leftPos + REDSTONE_BUTTON_X;
        this.redstoneModeButtonY = this.topPos + CLOSE_BUTTON_Y;
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
    
    /**
     * Plays button click sound
     */
    private void playButtonSound() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);
    }
    
    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        Component title = Component.translatable("block.iska_utils.auto_shop");
        int titleWidth = this.font.width(title);
        guiGraphics.text(this.font, title, (this.imageWidth - titleWidth) / 2, 8, GuiTextColors.TITLE, false);

        Component selectText = Component.translatable("gui.iska_utils.auto_shop.select_item");
        guiGraphics.text(this.font, selectText, 75, 27, GuiTextColors.TITLE, false);

        Component encapsulatedText = Component.translatable("gui.iska_utils.auto_shop.encapsulated_item");
        guiGraphics.text(this.font, encapsulatedText, 75, 52, GuiTextColors.TITLE, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && event.x() >= redstoneModeButtonX && event.x() < redstoneModeButtonX + REDSTONE_BUTTON_SIZE
                && event.y() >= redstoneModeButtonY && event.y() < redstoneModeButtonY + REDSTONE_BUTTON_SIZE) {
            onRedstoneModePressed();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void onRedstoneModePressed() {
        playButtonSound();
        BlockPos machinePos = menu.getSyncedBlockPos();
        if (!machinePos.equals(BlockPos.ZERO)) {
            net.unfamily.iskautils.network.ModMessages.sendAutoShopRedstoneModePacket(machinePos);
        }
    }

    private void renderRedstoneModeButton(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        boolean isHovered = mouseX >= redstoneModeButtonX && mouseX < redstoneModeButtonX + REDSTONE_BUTTON_SIZE
                && mouseY >= redstoneModeButtonY && mouseY < redstoneModeButtonY + REDSTONE_BUTTON_SIZE;
        int textureY = isHovered ? 16 : 0;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, MEDIUM_BUTTONS, redstoneModeButtonX, redstoneModeButtonY, 0.0F, (float)textureY, REDSTONE_BUTTON_SIZE, REDSTONE_BUTTON_SIZE, 96, 96);
        int redstoneMode = menu.getRedstoneMode();
        int iconX = redstoneModeButtonX + 2;
        int iconY = redstoneModeButtonY + 2;
        int iconSize = 12;
        switch (redstoneMode) {
            case 0 -> renderScaledItem(guiGraphics, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GUNPOWDER), iconX, iconY, iconSize);
            case 1 -> renderScaledItem(guiGraphics, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.REDSTONE), iconX, iconY, iconSize);
            case 2 -> renderScaledTexture(guiGraphics, REDSTONE_GUI, iconX, iconY, iconSize);
            case 3 -> renderScaledItem(guiGraphics, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.REPEATER), iconX, iconY, iconSize);
            case 4 -> renderScaledItem(guiGraphics, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BARRIER), iconX, iconY, iconSize);
        }
    }

    private void renderRedstoneModeTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        boolean isHovered = mouseX >= redstoneModeButtonX && mouseX < redstoneModeButtonX + REDSTONE_BUTTON_SIZE
                && mouseY >= redstoneModeButtonY && mouseY < redstoneModeButtonY + REDSTONE_BUTTON_SIZE;
        if (isHovered) {
            Component tooltip = switch (menu.getRedstoneMode()) {
                case 0 -> Component.translatable("gui.iska_utils.generic.redstone_mode.none");
                case 1 -> Component.translatable("gui.iska_utils.generic.redstone_mode.low");
                case 2 -> Component.translatable("gui.iska_utils.generic.redstone_mode.high");
                case 3 -> Component.translatable("gui.iska_utils.generic.redstone_mode.pulse");
                case 4 -> Component.translatable("gui.iska_utils.generic.redstone_mode.disabled");
                default -> Component.literal("Unknown mode");
            };
            java.util.List<FormattedCharSequence> lines = java.util.List.of(tooltip.getVisualOrderText());
            guiGraphics.setTooltipForNextFrame(this.font, lines, DefaultTooltipPositioner.INSTANCE, mouseX, mouseY, true);
        }
    }

    private void renderScaledItem(GuiGraphicsExtractor guiGraphics, net.minecraft.world.item.ItemStack itemStack, int x, int y, int size) {
        float scale = (float) size / 16.0f;
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(x, y);
        guiGraphics.pose().scale(scale, scale);
        guiGraphics.item(itemStack, 0, 0);
        guiGraphics.pose().popMatrix();
    }

    private void renderScaledTexture(GuiGraphicsExtractor guiGraphics, Identifier texture, int x, int y, int size) {
        float scale = (float) size / 16.0f;
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(x, y);
        guiGraphics.pose().scale(scale, scale);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
        guiGraphics.pose().popMatrix();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        renderRedstoneModeButton(guiGraphics, mouseX, mouseY);
        renderRedstoneModeTooltip(guiGraphics, mouseX, mouseY);
    }
} 