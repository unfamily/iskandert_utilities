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
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
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
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component title = Component.translatable("block.iska_utils.auto_shop");
        int titleWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title, (this.imageWidth - titleWidth) / 2, 8, 0x404040, false);

        Component selectText = Component.translatable("gui.iska_utils.auto_shop.select_item");
        guiGraphics.drawString(this.font, selectText, 75, 27, 0x404040, false);

        Component encapsulatedText = Component.translatable("gui.iska_utils.auto_shop.encapsulated_item");
        guiGraphics.drawString(this.font, encapsulatedText, 75, 52, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (redstoneModeButton.isHovered()) {
            guiGraphics.renderTooltip(font,
                    MachineGuiButtons.redstoneTooltip(menu.getRedstoneMode(), true), mouseX, mouseY);
        }
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && redstoneModeButton.isHovered()) {
            onRedstoneModePressed(true);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void onRedstoneModePressed(boolean backward) {
        playButtonSound();
        BlockPos machinePos = menu.getSyncedBlockPos();
        if (!machinePos.equals(BlockPos.ZERO)) {
            net.unfamily.iskautils.network.ModMessages.sendAutoShopRedstoneModePacket(machinePos, backward);
        }
    }
}
