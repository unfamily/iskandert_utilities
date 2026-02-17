package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.unfamily.iskautils.IskaUtils;

public class AutoShopScreen extends AbstractContainerScreen<AutoShopMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/backgrounds/auto_shop.png");
    private static final ResourceLocation MEDIUM_BUTTONS =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/medium_buttons.png");
    private static final ResourceLocation REDSTONE_GUI =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/redstone_gui.png");
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
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
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
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Non chiamiamo super.renderLabels() per evitare titolo duplicato e scritta inventory
        
        // Titolo centrato in alto
        Component title = Component.translatable("block.iska_utils.auto_shop");
        int titleWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title, (this.imageWidth - titleWidth) / 2, 8, 0x404040, false);
        
        // Testo accanto alla slot selezionata (slot 0) - spostato un po' più in alto
        Component selectText = Component.translatable("gui.iska_utils.auto_shop.select_item");
        guiGraphics.drawString(this.font, selectText, 75, 27, 0x404040, false);
        
        // Testo accanto alla slot encapsulated (slot 1) - spostato un po' più in alto
        Component encapsulatedText = Component.translatable("gui.iska_utils.auto_shop.encapsulated_item");
        guiGraphics.drawString(this.font, encapsulatedText, 75, 52, 0x404040, false);
    }
    
    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        renderRedstoneModeTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderRedstoneModeButton(guiGraphics, mouseX, mouseY);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= redstoneModeButtonX && mouseX < redstoneModeButtonX + REDSTONE_BUTTON_SIZE
                && mouseY >= redstoneModeButtonY && mouseY < redstoneModeButtonY + REDSTONE_BUTTON_SIZE) {
            onRedstoneModePressed();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void onRedstoneModePressed() {
        playButtonSound();
        BlockPos machinePos = menu.getSyncedBlockPos();
        if (!machinePos.equals(BlockPos.ZERO)) {
            net.unfamily.iskautils.network.ModMessages.sendAutoShopRedstoneModePacket(machinePos);
        }
    }

    private void renderRedstoneModeButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        boolean isHovered = mouseX >= redstoneModeButtonX && mouseX < redstoneModeButtonX + REDSTONE_BUTTON_SIZE
                && mouseY >= redstoneModeButtonY && mouseY < redstoneModeButtonY + REDSTONE_BUTTON_SIZE;
        int textureY = isHovered ? 16 : 0;
        guiGraphics.blit(MEDIUM_BUTTONS, redstoneModeButtonX, redstoneModeButtonY, 0, textureY, REDSTONE_BUTTON_SIZE, REDSTONE_BUTTON_SIZE, 96, 96);
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

    private void renderRedstoneModeTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
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
            guiGraphics.renderTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    private void renderScaledItem(GuiGraphics guiGraphics, net.minecraft.world.item.ItemStack itemStack, int x, int y, int size) {
        float scale = (float) size / 16.0f;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        guiGraphics.renderItem(itemStack, 0, 0);
        guiGraphics.pose().popPose();
    }

    private void renderScaledTexture(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int size) {
        float scale = (float) size / 16.0f;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        guiGraphics.blit(texture, 0, 0, 0, 0, 16, 16, 16, 16);
        guiGraphics.pose().popPose();
    }
} 