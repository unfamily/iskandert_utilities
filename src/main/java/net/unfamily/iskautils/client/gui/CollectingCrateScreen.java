package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.network.ModMessages;
import net.unfamily.iskautils.util.CollectingCrateMode;
import net.unfamily.iskautils.util.ExperienceFluidMath;

public class CollectingCrateScreen extends AbstractContainerScreen<CollectingCrateMenu> {

    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/backgrounds/collecting_crate.png");
    private static final ResourceLocation MEDIUM_BUTTONS = ResourceLocation.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/medium_buttons.png");
    private static final ResourceLocation REDSTONE_GUI = ResourceLocation.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/redstone_gui.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 230;
    private static final int TITLE_COLOR = 0x404040;
    private static final int BUTTON_SIZE = 16;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;
    private static final int CLOSE_BUTTON_Y = 5;

    /** Free panel between storage grid and player inventory (GUI-relative). */
    private static final int PANEL_LEFT = 28;
    private static final int PANEL_RIGHT = 168;
    private static final int PANEL_BOTTOM = 138;

    private static final int XP_TEXT_Y = 80;
    private static final int XP_BAR_Y = 92;
    private static final int XP_BAR_W = 100;
    private static final int XP_BAR_H = 5;
    private static final int RANGE_TEXT_Y = 101;

    private static final int BUTTON_GAP = 2;
    /** Right column: four buttons stacked above the deposit (empty bottle) button. */
    private static final int BUTTONS_X = PANEL_RIGHT - BUTTON_SIZE - 6;
    private static final int BUTTONS_ANCHOR_BOTTOM = PANEL_BOTTOM - 4;

    private static final ItemStack GHOST_RANGE_MODULE = new ItemStack(ModItems.RANGE_MODULE.get());

    private Button closeButton;
    private int collectButtonX;
    private int collectButtonY;
    private int depositButtonX;
    private int depositButtonY;
    private int modeButtonX;
    private int modeButtonY;
    private int redstoneButtonX;
    private int redstoneButtonY;

    public CollectingCrateScreen(CollectingCrateMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        closeButton = Button.builder(Component.literal("✕"), button -> {
            if (this.minecraft != null) {
                this.minecraft.player.closeContainer();
            }
        }).bounds(this.leftPos + CLOSE_BUTTON_X, this.topPos + CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE).build();
        addRenderableWidget(closeButton);
        layoutActionButtons();
    }

    private void layoutActionButtons() {
        int buttonX = this.leftPos + BUTTONS_X;
        int y = this.topPos + BUTTONS_ANCHOR_BOTTOM - BUTTON_SIZE;

        depositButtonX = buttonX;
        depositButtonY = y;
        y -= BUTTON_SIZE + BUTTON_GAP;
        redstoneButtonX = buttonX;
        redstoneButtonY = y;
        y -= BUTTON_SIZE + BUTTON_GAP;
        modeButtonX = buttonX;
        modeButtonY = y;
        y -= BUTTON_SIZE + BUTTON_GAP;
        collectButtonX = buttonX;
        collectButtonY = y;
    }

    private void drawCenteredText(GuiGraphics guiGraphics, Component text, int centerX, int y, int color) {
        guiGraphics.drawString(this.font, text, centerX - this.font.width(text) / 2, y, color, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);
        renderXpBar(guiGraphics);
        renderActionButtons(guiGraphics, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderGhostModule(guiGraphics);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderButtonTooltips(guiGraphics, mouseX, mouseY);
    }

    private void renderXpBar(GuiGraphics guiGraphics) {
        int mb = menu.getStoredXpMb();
        int levels = ExperienceFluidMath.displayLevelsFromMb(mb);
        double progress = ExperienceFluidMath.displayProgressFromMb(mb);

        int barX = this.leftPos + PANEL_LEFT;
        int barY = this.topPos + XP_BAR_Y;
        int barCenterX = barX + XP_BAR_W / 2;

        Component levelText = Component.translatable("gui.iska_utils.collecting_crate.xp_levels", levels);
        drawCenteredText(guiGraphics, levelText, barCenterX, this.topPos + XP_TEXT_Y, TITLE_COLOR);

        guiGraphics.fill(barX, barY, barX + XP_BAR_W, barY + XP_BAR_H, 0xFF000000);
        int fill = (int) (XP_BAR_W * progress);
        if (fill > 0) {
            guiGraphics.fill(barX, barY, barX + fill, barY + XP_BAR_H, 0xFF80FF20);
        }

        Component rangeText = Component.translatable("gui.iska_utils.collecting_crate.range", menu.getEffectiveRange());
        drawCenteredText(guiGraphics, rangeText, barCenterX, this.topPos + RANGE_TEXT_Y, 0x606060);
    }

    private void renderActionButtons(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderButton(guiGraphics, collectButtonX, collectButtonY, mouseX, mouseY,
                new ItemStack(Items.EXPERIENCE_BOTTLE));
        renderButton(guiGraphics, depositButtonX, depositButtonY, mouseX, mouseY,
                new ItemStack(Items.GLASS_BOTTLE));
        renderButton(guiGraphics, modeButtonX, modeButtonY, mouseX, mouseY, modeIcon());
        renderRedstoneButton(guiGraphics, mouseX, mouseY);
    }

    private ItemStack modeIcon() {
        return switch (CollectingCrateMode.fromId(menu.getCollectMode())) {
            case BOTH -> new ItemStack(Items.CHEST);
            case EXPERIENCE_ONLY -> new ItemStack(Items.EXPERIENCE_BOTTLE);
            case ITEMS_ONLY -> new ItemStack(Items.HOPPER);
        };
    }

    private void renderButton(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY, ItemStack icon) {
        boolean hovered = mouseX >= x && mouseX <= x + BUTTON_SIZE && mouseY >= y && mouseY <= y + BUTTON_SIZE;
        int textureY = hovered ? 16 : 0;
        guiGraphics.blit(MEDIUM_BUTTONS, x, y, 0, textureY, BUTTON_SIZE, BUTTON_SIZE, 96, 96);
        renderScaledItem(guiGraphics, icon, x + 2, y + 2, 12);
    }

    private void renderRedstoneButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderButton(guiGraphics, redstoneButtonX, redstoneButtonY, mouseX, mouseY, ItemStack.EMPTY);
        int iconX = redstoneButtonX + 2;
        int iconY = redstoneButtonY + 2;
        int iconSize = 12;
        int redstoneMode = menu.getRedstoneMode();
        if (redstoneMode == 3) {
            redstoneMode = 4;
        }
        switch (redstoneMode) {
            case 0 -> renderScaledItem(guiGraphics, new ItemStack(Items.GUNPOWDER), iconX, iconY, iconSize);
            case 1 -> renderScaledItem(guiGraphics, new ItemStack(Items.REDSTONE), iconX, iconY, iconSize);
            case 2 -> renderScaledTexture(guiGraphics, REDSTONE_GUI, iconX, iconY, iconSize);
            case 4 -> renderScaledItem(guiGraphics, new ItemStack(Items.BARRIER), iconX, iconY, iconSize);
            default -> {}
        }
    }

    private void renderScaledItem(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y, int size) {
        if (itemStack.isEmpty()) {
            return;
        }
        guiGraphics.pose().pushPose();
        float scale = (float) size / 16.0f;
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        guiGraphics.renderItem(itemStack, 0, 0);
        guiGraphics.pose().popPose();
    }

    private void renderScaledTexture(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int size) {
        guiGraphics.pose().pushPose();
        float scale = (float) size / 16.0f;
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        guiGraphics.blit(texture, 0, 0, 0, 0, 16, 16, 16, 16);
        guiGraphics.pose().popPose();
    }

    private void renderGhostModule(GuiGraphics guiGraphics) {
        Slot slot = menu.getSlot(CollectingCrateMenu.MODULE_SLOT_INDEX);
        if (!slot.getItem().isEmpty()) {
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.leftPos + slot.x, this.topPos + slot.y, 0);
        guiGraphics.renderItem(GHOST_RANGE_MODULE, 0, 0);
        guiGraphics.fill(0, 0, 16, 16, 0x80000000);
        guiGraphics.pose().popPose();
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        Slot moduleSlot = menu.getSlot(CollectingCrateMenu.MODULE_SLOT_INDEX);
        if (moduleSlot.getItem().isEmpty() && isMouseOverSlot(moduleSlot, mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, GHOST_RANGE_MODULE.getHoverName(), mouseX, mouseY);
        }
    }

    private boolean isMouseOverSlot(Slot slot, int mouseX, int mouseY) {
        int x = this.leftPos + slot.x;
        int y = this.topPos + slot.y;
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    private void renderButtonTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isOver(collectButtonX, collectButtonY, mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font,
                    Component.translatable("gui.iska_utils.collecting_crate.collect_xp"), mouseX, mouseY);
        } else if (isOver(depositButtonX, depositButtonY, mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font,
                    Component.translatable("gui.iska_utils.collecting_crate.deposit_xp"), mouseX, mouseY);
        } else if (isOver(modeButtonX, modeButtonY, mouseX, mouseY)) {
            CollectingCrateMode mode = CollectingCrateMode.fromId(menu.getCollectMode());
            guiGraphics.renderTooltip(this.font,
                    Component.translatable("gui.iska_utils.collecting_crate.mode." + mode.name().toLowerCase()), mouseX, mouseY);
        } else if (isOver(redstoneButtonX, redstoneButtonY, mouseX, mouseY)) {
            int redstoneMode = menu.getRedstoneMode();
            if (redstoneMode == 3) {
                redstoneMode = 4;
            }
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

    private boolean isOver(int x, int y, int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + BUTTON_SIZE && mouseY >= y && mouseY <= y + BUTTON_SIZE;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 || button == 1) {
            boolean backward = button == 1;
            BlockPos pos = menu.getSyncedBlockPos();
            if (!pos.equals(BlockPos.ZERO)) {
                if (isOver(collectButtonX, collectButtonY, (int) mouseX, (int) mouseY) && !backward) {
                    ModMessages.sendCollectingCrateXpCollectPacket(pos);
                    playButtonSound();
                    return true;
                }
                if (isOver(depositButtonX, depositButtonY, (int) mouseX, (int) mouseY) && !backward) {
                    ModMessages.sendCollectingCrateXpDepositPacket(pos);
                    playButtonSound();
                    return true;
                }
                if (isOver(modeButtonX, modeButtonY, (int) mouseX, (int) mouseY)) {
                    ModMessages.sendCollectingCrateModePacket(pos, backward);
                    playButtonSound();
                    return true;
                }
                if (isOver(redstoneButtonX, redstoneButtonY, (int) mouseX, (int) mouseY)) {
                    ModMessages.sendCollectingCrateRedstoneModePacket(pos, backward);
                    playButtonSound();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void playButtonSound() {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int titleWidth = this.font.width(this.title);
        int titleX = (this.imageWidth - titleWidth) / 2;
        guiGraphics.drawString(this.font, this.title, titleX, 8, TITLE_COLOR, false);
    }
}
