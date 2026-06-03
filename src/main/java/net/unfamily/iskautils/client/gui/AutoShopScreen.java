package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.unfamily.iskautils.IskaUtils;

public class AutoShopScreen extends AbstractContainerScreen<AutoShopMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/backgrounds/auto_shop.png");
    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 160;

    private Button closeButton;
    private ItemIconButton redstoneModeButton;
    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;
    private static final int REDSTONE_BUTTON_SIZE = 16;
    private static final int REDSTONE_BUTTON_X = CLOSE_BUTTON_X - REDSTONE_BUTTON_SIZE - 4;

    public AutoShopScreen(AutoShopMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        closeButton = Button.builder(Component.literal("✕"),
                        button -> {
                            playButtonSound();
                            this.onClose();
                        })
                .bounds(this.leftPos + CLOSE_BUTTON_X, this.topPos + CLOSE_BUTTON_Y,
                        CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
                .build();
        addRenderableWidget(closeButton);

        redstoneModeButton = addRenderableWidget(MachineGuiButtons.redstoneIconButton(
                this.leftPos + REDSTONE_BUTTON_X,
                this.topPos + CLOSE_BUTTON_Y,
                b -> onRedstoneModePressed(false),
                menu::getRedstoneMode,
                true));
    }

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
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        if (redstoneModeButton != null && redstoneModeButton.isMouseOver(mouseX, mouseY)) {
            MachineGuiButtons.renderTooltipLine(
                    guiGraphics, font, mouseX, mouseY,
                    MachineGuiButtons.redstoneTooltip(menu.getRedstoneMode(), true));
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 1 && redstoneModeButton != null && redstoneModeButton.isMouseOver(event.x(), event.y())) {
            onRedstoneModePressed(true);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void onRedstoneModePressed(boolean backward) {
        playButtonSound();
        BlockPos machinePos = menu.getSyncedBlockPos();
        if (!machinePos.equals(BlockPos.ZERO)) {
            net.unfamily.iskautils.network.ModMessages.sendAutoShopRedstoneModePacket(machinePos, backward);
        }
    }
}
