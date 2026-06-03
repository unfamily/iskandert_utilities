package net.unfamily.iskautils.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.MarkRenderer;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.network.ModMessages;
import net.unfamily.iskautils.network.packet.CollectingCratePreviewToggleC2SPacket;
import net.unfamily.iskautils.network.packet.CollectingCrateSizeC2SPacket;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.CollectingCrateMode;
import net.unfamily.iskautils.util.ExperienceFluidMath;
import org.lwjgl.glfw.GLFW;

public class CollectingCrateScreen extends AbstractContainerScreen<CollectingCrateMenu> {

    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/backgrounds/collecting_crate.png");
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 230;
    private static final int TITLE_COLOR = 0x404040;
    private static final int SECONDARY_COLOR = 0x606060;
    private static final int BUTTON_SIZE = 16;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;
    private static final int CLOSE_BUTTON_Y = 5;

    /** XP bar + labels: right of module slot so text does not overlap it. */
    private static final int XP_PANEL_X = CollectingCrateMenu.MODULE_SLOT_X + 16 + 12;
    private static final int XP_TEXT_Y = 80;
    private static final int XP_BAR_Y = 92;
    private static final int XP_BAR_W = 100;
    private static final int XP_BAR_H = 5;
    private static final int XP_BAR_COLOR_NORMAL = 0xFF80FF20;
    private static final int XP_BAR_COLOR_FULL = 0xFFFF3030;
    private static final int SIZE_LABEL_Y = 101;

    /** Area size buttons — preview left-aligned with slots; arrows to the right on the same row. */
    private static final int BUTTON_W = 14;
    private static final int BUTTON_H = 12;
    private static final int GAP = 3;
    private static final int ARROW_GROUP_WIDTH = 3 * BUTTON_W + 2 * GAP;
    private static final int PREVIEW_BUTTON_X = CollectingCrateMenu.STORAGE_SLOTS_X;
    private static final int PREVIEW_BUTTON_W = 46;
    private static final int ARROW_GROUP_LEFT_X = PREVIEW_BUTTON_X + PREVIEW_BUTTON_W + GAP;
    private static final int ROW1_Y = 114;
    private static final int ROW2_Y = 130;
    private static final int PREVIEW_BUTTON_Y = ROW2_Y;

    private static final String TOOLTIP_LEFT_CLICK = "gui.iska_utils.collecting_crate.tooltip.left_click";
    private static final String TOOLTIP_RIGHT_CLICK = "gui.iska_utils.collecting_crate.tooltip.right_click";
    private static final String TOOLTIP_SHIFT_10 = "gui.iska_utils.collecting_crate.tooltip.shift_10";
    private static final String TOOLTIP_ALT_CTRL_5 = "gui.iska_utils.collecting_crate.tooltip.alt_ctrl_5";

    private static final int BUTTON_GAP = CollectingCrateMenu.ACTION_BUTTON_GAP;

    private static final ItemStack GHOST_RANGE_MODULE = new ItemStack(ModItems.RANGE_MODULE.get());

    private Button closeButton;
    private Button buttonUp;
    private Button buttonLeft;
    private Button buttonRight;
    private Button buttonDepth;
    private Button buttonPreview;
    private boolean previewButtonShowsHide;

    private ItemIconButton collectButton;
    private ItemIconButton depositButton;
    private ItemIconButton modeButton;
    private ItemIconButton redstoneButton;

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

        int upX = leftPos + ARROW_GROUP_LEFT_X + ARROW_GROUP_WIDTH / 2 - BUTTON_W / 2;
        buttonUp = Button.builder(Component.literal("\u2191"), b -> sendSize(0, true))
                .bounds(upX, topPos + ROW1_Y, BUTTON_W, BUTTON_H)
                .build();
        buttonLeft = Button.builder(Component.literal("\u2190"), b -> sendSize(1, true))
                .bounds(leftPos + ARROW_GROUP_LEFT_X, topPos + ROW2_Y, BUTTON_W, BUTTON_H)
                .build();
        buttonDepth = Button.builder(Component.literal("-"), b -> sendSize(3, true))
                .bounds(leftPos + ARROW_GROUP_LEFT_X + BUTTON_W + GAP, topPos + ROW2_Y, BUTTON_W, BUTTON_H)
                .build();
        buttonRight = Button.builder(Component.literal("\u2192"), b -> sendSize(2, true))
                .bounds(leftPos + ARROW_GROUP_LEFT_X + 2 * (BUTTON_W + GAP), topPos + ROW2_Y, BUTTON_W, BUTTON_H)
                .build();
        addRenderableWidget(buttonUp);
        addRenderableWidget(buttonLeft);
        addRenderableWidget(buttonDepth);
        addRenderableWidget(buttonRight);

        buttonPreview = Button.builder(Component.translatable("gui.iska_utils.collecting_crate.preview"), b -> togglePreview())
                .bounds(leftPos + PREVIEW_BUTTON_X, topPos + PREVIEW_BUTTON_Y, PREVIEW_BUTTON_W, BUTTON_H)
                .build();
        buttonPreview.setTooltip(Tooltip.create(Component.translatable("gui.iska_utils.collecting_crate.preview.tooltip")));
        addRenderableWidget(buttonPreview);

        createActionButtons();
        updateButtonTooltips();
        previewButtonShowsHide = menu.isPreviewEnabled();
        updatePreviewButtonLabel();
        if (menu.isPreviewEnabled()) {
            BlockPos pos = menu.getSyncedBlockPos();
            if (!pos.equals(BlockPos.ZERO)) {
                PacketDistributor.sendToServer(new CollectingCratePreviewToggleC2SPacket(pos, true));
            }
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtonTooltips();
        if (menu.isPreviewEnabled() != previewButtonShowsHide) {
            previewButtonShowsHide = menu.isPreviewEnabled();
            updatePreviewButtonLabel();
        }
    }

    private void updatePreviewButtonLabel() {
        if (buttonPreview != null) {
            buttonPreview.setMessage(Component.translatable(
                    previewButtonShowsHide ? "gui.iska_utils.generic.hide" : "gui.iska_utils.collecting_crate.preview"));
        }
    }

    private static Tooltip tooltipWithValue(String valueKey, int value) {
        Component full = Component.translatable(valueKey, value)
                .append(Component.literal("\n"))
                .append(Component.translatable(TOOLTIP_LEFT_CLICK))
                .append(Component.literal("\n"))
                .append(Component.translatable(TOOLTIP_RIGHT_CLICK))
                .append(Component.literal("\n"))
                .append(Component.translatable(TOOLTIP_SHIFT_10))
                .append(Component.literal("\n"))
                .append(Component.translatable(TOOLTIP_ALT_CTRL_5));
        return Tooltip.create(full);
    }

    private void updateButtonTooltips() {
        buttonUp.setTooltip(tooltipWithValue("gui.iska_utils.collecting_crate.tooltip.up_value", menu.getAreaHeight()));
        buttonLeft.setTooltip(tooltipWithValue("gui.iska_utils.collecting_crate.tooltip.left_value", menu.getSizeLeft()));
        buttonRight.setTooltip(tooltipWithValue("gui.iska_utils.collecting_crate.tooltip.right_value", menu.getSizeRight()));
        buttonDepth.setTooltip(tooltipWithValue("gui.iska_utils.collecting_crate.tooltip.behind_value", menu.getAreaDepth()));
    }

    private void createActionButtons() {
        int buttonX = this.leftPos + CollectingCrateMenu.actionButtonsColumnX();
        int y = this.topPos + CollectingCrateMenu.actionButtonsColumnStartY()
                + CollectingCrateMenu.ACTION_BUTTONS_STACK_HEIGHT - BUTTON_SIZE;

        int depositY = y;
        y -= BUTTON_SIZE + BUTTON_GAP;
        int redstoneY = y;
        y -= BUTTON_SIZE + BUTTON_GAP;
        int modeY = y;
        y -= BUTTON_SIZE + BUTTON_GAP;
        int collectY = y;

        collectButton = addRenderableWidget(new ItemIconButton(
                buttonX, collectY, BUTTON_SIZE, b -> sendXpCollect(), () -> new ItemStack(Items.EXPERIENCE_BOTTLE), Component.empty()));
        depositButton = addRenderableWidget(new ItemIconButton(
                buttonX, depositY, BUTTON_SIZE, b -> sendXpDeposit(), () -> new ItemStack(Items.GLASS_BOTTLE), Component.empty()));
        modeButton = addRenderableWidget(new ItemIconButton(
                buttonX, modeY, BUTTON_SIZE, b -> sendMode(false), this::modeIcon, Component.empty()));
        redstoneButton = addRenderableWidget(MachineGuiButtons.redstoneIconButton(
                buttonX, redstoneY, b -> sendRedstone(false), menu::getRedstoneMode, false));
    }

    private void sendXpCollect() {
        BlockPos pos = menu.getSyncedBlockPos();
        if (!pos.equals(BlockPos.ZERO)) {
            ModMessages.sendCollectingCrateXpCollectPacket(pos);
            playButtonSound();
        }
    }

    private void sendXpDeposit() {
        BlockPos pos = menu.getSyncedBlockPos();
        if (!pos.equals(BlockPos.ZERO)) {
            ModMessages.sendCollectingCrateXpDepositPacket(pos);
            playButtonSound();
        }
    }

    private void sendMode(boolean backward) {
        BlockPos pos = menu.getSyncedBlockPos();
        if (!pos.equals(BlockPos.ZERO)) {
            ModMessages.sendCollectingCrateModePacket(pos, backward);
            playButtonSound();
        }
    }

    private void sendRedstone(boolean backward) {
        BlockPos pos = menu.getSyncedBlockPos();
        if (!pos.equals(BlockPos.ZERO)) {
            ModMessages.sendCollectingCrateRedstoneModePacket(pos, backward);
            playButtonSound();
        }
    }

    private int modifierStepAmount() {
        if (minecraft == null || minecraft.getWindow() == null) {
            return 1;
        }
        long window = minecraft.getWindow().getWindow();
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT)) {
            return 10;
        }
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT)) {
            return 5;
        }
        return 1;
    }

    private void sendSize(int direction, boolean increment) {
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos.equals(BlockPos.ZERO)) {
            return;
        }
        PacketDistributor.sendToServer(new CollectingCrateSizeC2SPacket(pos, direction, increment, modifierStepAmount()));
    }

    private void togglePreview() {
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos.equals(BlockPos.ZERO)) {
            return;
        }
        playButtonSound();
        boolean enabling = !menu.isPreviewEnabled();
        MarkRenderer.getInstance().clearBillboardMarkersForOwner(pos);
        PacketDistributor.sendToServer(new CollectingCratePreviewToggleC2SPacket(pos, enabling));
        previewButtonShowsHide = enabling;
        updatePreviewButtonLabel();
    }

    private void drawCenteredText(GuiGraphics guiGraphics, Component text, int centerX, int y, int color) {
        guiGraphics.drawString(this.font, text, centerX - this.font.width(text) / 2, y, color, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);
        renderXpBar(guiGraphics);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderGhostModule(guiGraphics);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderButtonTooltips(guiGraphics, mouseX, mouseY);
    }

    private void renderXpBar(GuiGraphics guiGraphics) {
        long mb = menu.getStoredXpMb();
        int levels = ExperienceFluidMath.displayLevelsFromMb(mb);
        double progress = ExperienceFluidMath.displayProgressFromMb(mb);

        int panelX = this.leftPos + XP_PANEL_X;
        int barY = this.topPos + XP_BAR_Y;
        int barCenterX = panelX + XP_BAR_W / 2;

        Component levelText = Component.translatable("gui.iska_utils.collecting_crate.xp_levels", levels);
        drawCenteredText(guiGraphics, levelText, barCenterX, this.topPos + XP_TEXT_Y, TITLE_COLOR);

        guiGraphics.fill(panelX, barY, panelX + XP_BAR_W, barY + XP_BAR_H, 0xFF000000);
        long capacityMb = ExperienceFluidMath.capacityMbFromLevels(Config.collectingCrateXpCapacityLevels);
        boolean atMax = capacityMb > 0 && mb >= capacityMb;
        int fill = atMax ? XP_BAR_W : (int) (XP_BAR_W * progress);
        if (fill > 0) {
            guiGraphics.fill(panelX, barY, panelX + fill, barY + XP_BAR_H,
                    atMax ? XP_BAR_COLOR_FULL : XP_BAR_COLOR_NORMAL);
        }

        Component sizeLabel = Component.translatable(
                "gui.iska_utils.collecting_crate.size",
                menu.getSizeLeft(),
                menu.getSizeRight(),
                menu.getAreaWidth(),
                menu.getAreaHeight(),
                menu.getAreaDepth());
        drawCenteredText(guiGraphics, sizeLabel, barCenterX, this.topPos + SIZE_LABEL_Y, TITLE_COLOR);
    }

    private ItemStack modeIcon() {
        return switch (CollectingCrateMode.fromId(menu.getCollectMode())) {
            case BOTH -> new ItemStack(Items.CHEST);
            case EXPERIENCE_ONLY -> new ItemStack(Items.EXPERIENCE_BOTTLE);
            case ITEMS_ONLY -> new ItemStack(Items.HOPPER);
        };
    }

    private void renderGhostModule(GuiGraphics guiGraphics) {
        GuiGhostItem.render(guiGraphics, leftPos, topPos, menu.getSlot(CollectingCrateMenu.MODULE_SLOT_INDEX), GHOST_RANGE_MODULE);
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
        if (collectButton.isHovered()) {
            guiGraphics.renderTooltip(this.font,
                    Component.translatable("gui.iska_utils.collecting_crate.collect_xp"), mouseX, mouseY);
        } else if (depositButton.isHovered()) {
            guiGraphics.renderTooltip(this.font,
                    Component.translatable("gui.iska_utils.collecting_crate.deposit_xp"), mouseX, mouseY);
        } else if (modeButton.isHovered()) {
            CollectingCrateMode mode = CollectingCrateMode.fromId(menu.getCollectMode());
            guiGraphics.renderTooltip(this.font,
                    Component.translatable("gui.iska_utils.collecting_crate.mode." + mode.name().toLowerCase()), mouseX, mouseY);
        } else if (redstoneButton.isHovered()) {
            guiGraphics.renderTooltip(this.font,
                    MachineGuiButtons.redstoneTooltip(menu.getRedstoneMode(), false), mouseX, mouseY);
        }
    }

    private static boolean isInArrowBounds(int x, int y, int left, int top) {
        return x >= left && x < left + BUTTON_W && y >= top && y < top + BUTTON_H;
    }

    private void playGuiClickSound() {
        playButtonSound();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            int x = (int) mouseX;
            int y = (int) mouseY;
            int upX = leftPos + ARROW_GROUP_LEFT_X + ARROW_GROUP_WIDTH / 2 - BUTTON_W / 2;
            if (isInArrowBounds(x, y, upX, topPos + ROW1_Y)) {
                playGuiClickSound();
                sendSize(0, false);
                return true;
            }
            if (isInArrowBounds(x, y, leftPos + ARROW_GROUP_LEFT_X, topPos + ROW2_Y)) {
                playGuiClickSound();
                sendSize(1, false);
                return true;
            }
            if (isInArrowBounds(x, y, leftPos + ARROW_GROUP_LEFT_X + BUTTON_W + GAP, topPos + ROW2_Y)) {
                playGuiClickSound();
                sendSize(3, false);
                return true;
            }
            if (isInArrowBounds(x, y, leftPos + ARROW_GROUP_LEFT_X + 2 * (BUTTON_W + GAP), topPos + ROW2_Y)) {
                playGuiClickSound();
                sendSize(2, false);
                return true;
            }
        }
        if (button == 1) {
            if (modeButton.isHovered()) {
                sendMode(true);
                return true;
            }
            if (redstoneButton.isHovered()) {
                sendRedstone(true);
                return true;
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
