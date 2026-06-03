package net.unfamily.iskautils.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskalib.client.marker.MarkRenderer;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.network.ModMessages;
import net.unfamily.iskautils.network.packet.CollectingCratePreviewToggleC2SPacket;
import net.unfamily.iskautils.network.packet.CollectingCrateSizeC2SPacket;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.CollectingCrateMode;
import net.unfamily.iskautils.util.ExperienceFluidMath;
import org.lwjgl.glfw.GLFW;

public class CollectingCrateScreen extends AbstractContainerScreen<CollectingCrateMenu> {

    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/backgrounds/collecting_crate.png");
    private static final Identifier MEDIUM_BUTTONS = Identifier.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/medium_buttons.png");
    private static final Identifier REDSTONE_GUI = Identifier.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/redstone_gui.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 230;
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

    private int collectButtonX;
    private int collectButtonY;
    private int depositButtonX;
    private int depositButtonY;
    private int modeButtonX;
    private int modeButtonY;
    private int redstoneButtonX;
    private int redstoneButtonY;

    public CollectingCrateScreen(CollectingCrateMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GUI_WIDTH, GUI_HEIGHT);
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

        layoutActionButtons();
        updateButtonTooltips();
        previewButtonShowsHide = menu.isPreviewEnabled();
        updatePreviewButtonLabel();
        if (menu.isPreviewEnabled()) {
            BlockPos pos = menu.getSyncedBlockPos();
            if (!pos.equals(BlockPos.ZERO)) {
                ClientPacketDistributor.sendToServer(new CollectingCratePreviewToggleC2SPacket(pos, true));
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

    private void layoutActionButtons() {
        int buttonX = this.leftPos + CollectingCrateMenu.actionButtonsColumnX();
        int y = this.topPos + CollectingCrateMenu.actionButtonsColumnStartY()
                + CollectingCrateMenu.ACTION_BUTTONS_STACK_HEIGHT - BUTTON_SIZE;

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

    private int modifierStepAmount() {
        if (minecraft == null || minecraft.getWindow() == null) {
            return 1;
        }
        var window = minecraft.getWindow();
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
        ClientPacketDistributor.sendToServer(new CollectingCrateSizeC2SPacket(pos, direction, increment, modifierStepAmount()));
    }

    private void togglePreview() {
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos.equals(BlockPos.ZERO)) {
            return;
        }
        playButtonSound();
        boolean enabling = !menu.isPreviewEnabled();
        MarkRenderer.getInstance().clearBillboardMarkersForOwner(pos);
        ClientPacketDistributor.sendToServer(new CollectingCratePreviewToggleC2SPacket(pos, enabling));
        previewButtonShowsHide = enabling;
        updatePreviewButtonLabel();
    }

    private void drawCenteredText(GuiGraphicsExtractor guiGraphics, Component text, int centerX, int y, int color) {
        guiGraphics.text(this.font, text, centerX - this.font.width(text) / 2, y, color, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);
        renderXpBar(guiGraphics);
        renderActionButtons(guiGraphics, mouseX, mouseY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        renderGhostModule(guiGraphics);
        renderButtonTooltips(guiGraphics, mouseX, mouseY);
    }

    private void renderXpBar(GuiGraphicsExtractor guiGraphics) {
        long mb = menu.getStoredXpMb();
        int levels = ExperienceFluidMath.displayLevelsFromMb(mb);
        double progress = ExperienceFluidMath.displayProgressFromMb(mb);

        int panelX = this.leftPos + XP_PANEL_X;
        int barY = this.topPos + XP_BAR_Y;
        int barCenterX = panelX + XP_BAR_W / 2;

        Component levelText = Component.translatable("gui.iska_utils.collecting_crate.xp_levels", levels);
        drawCenteredText(guiGraphics, levelText, barCenterX, this.topPos + XP_TEXT_Y, GuiTextColors.TITLE);

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
        drawCenteredText(guiGraphics, sizeLabel, barCenterX, this.topPos + SIZE_LABEL_Y, GuiTextColors.TITLE);
    }

    private void renderActionButtons(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
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

    private void renderButton(GuiGraphicsExtractor guiGraphics, int x, int y, int mouseX, int mouseY, ItemStack icon) {
        boolean hovered = mouseX >= x && mouseX <= x + BUTTON_SIZE && mouseY >= y && mouseY <= y + BUTTON_SIZE;
        float textureY = hovered ? 16.0F : 0.0F;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, MEDIUM_BUTTONS, x, y, 0.0F, textureY,
                BUTTON_SIZE, BUTTON_SIZE, 96, 96);
        renderScaledItem(guiGraphics, icon, x + 2, y + 2, 12);
    }

    private void renderRedstoneButton(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
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

    private void renderScaledItem(GuiGraphicsExtractor guiGraphics, ItemStack itemStack, int x, int y, int size) {
        if (itemStack.isEmpty()) {
            return;
        }
        guiGraphics.pose().pushMatrix();
        float scale = (float) size / 16.0f;
        guiGraphics.pose().translate(x, y);
        guiGraphics.pose().scale(scale, scale);
        guiGraphics.item(itemStack, 0, 0);
        guiGraphics.pose().popMatrix();
    }

    private void renderScaledTexture(GuiGraphicsExtractor guiGraphics, Identifier texture, int x, int y, int size) {
        guiGraphics.pose().pushMatrix();
        float scale = (float) size / 16.0f;
        guiGraphics.pose().translate(x, y);
        guiGraphics.pose().scale(scale, scale);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
        guiGraphics.pose().popMatrix();
    }

    private void renderGhostModule(GuiGraphicsExtractor guiGraphics) {
        Slot slot = menu.getSlot(CollectingCrateMenu.MODULE_SLOT_INDEX);
        if (!slot.getItem().isEmpty()) {
            return;
        }
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(this.leftPos + slot.x, this.topPos + slot.y);
        guiGraphics.item(GHOST_RANGE_MODULE, 0, 0);
        guiGraphics.fill(0, 0, 16, 16, 0x80000000);
        guiGraphics.pose().popMatrix();
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractTooltip(guiGraphics, mouseX, mouseY);
        Slot moduleSlot = menu.getSlot(CollectingCrateMenu.MODULE_SLOT_INDEX);
        if (moduleSlot.getItem().isEmpty() && isMouseOverSlot(moduleSlot, mouseX, mouseY)) {
            guiGraphics.setTooltipForNextFrame(
                    this.font,
                    java.util.List.of(GHOST_RANGE_MODULE.getHoverName().getVisualOrderText()),
                    DefaultTooltipPositioner.INSTANCE,
                    mouseX,
                    mouseY,
                    true);
        }
    }

    private boolean isMouseOverSlot(Slot slot, int mouseX, int mouseY) {
        int x = this.leftPos + slot.x;
        int y = this.topPos + slot.y;
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    private void renderButtonTooltips(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        Component tooltip = null;
        if (isOver(collectButtonX, collectButtonY, mouseX, mouseY)) {
            tooltip = Component.translatable("gui.iska_utils.collecting_crate.collect_xp");
        } else if (isOver(depositButtonX, depositButtonY, mouseX, mouseY)) {
            tooltip = Component.translatable("gui.iska_utils.collecting_crate.deposit_xp");
        } else if (isOver(modeButtonX, modeButtonY, mouseX, mouseY)) {
            CollectingCrateMode mode = CollectingCrateMode.fromId(menu.getCollectMode());
            tooltip = Component.translatable("gui.iska_utils.collecting_crate.mode." + mode.name().toLowerCase());
        } else if (isOver(redstoneButtonX, redstoneButtonY, mouseX, mouseY)) {
            int redstoneMode = menu.getRedstoneMode();
            if (redstoneMode == 3) {
                redstoneMode = 4;
            }
            tooltip = switch (redstoneMode) {
                case 0 -> Component.translatable("gui.iska_utils.generic.redstone_mode.none");
                case 1 -> Component.translatable("gui.iska_utils.generic.redstone_mode.low");
                case 2 -> Component.translatable("gui.iska_utils.generic.redstone_mode.high");
                case 4 -> Component.translatable("gui.iska_utils.generic.redstone_mode.disabled");
                default -> Component.literal("Unknown mode");
            };
        }
        if (tooltip != null) {
            guiGraphics.setTooltipForNextFrame(
                    this.font,
                    java.util.List.of(tooltip.getVisualOrderText()),
                    DefaultTooltipPositioner.INSTANCE,
                    mouseX,
                    mouseY,
                    true);
        }
    }

    private boolean isOver(int x, int y, int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + BUTTON_SIZE && mouseY >= y && mouseY <= y + BUTTON_SIZE;
    }

    private static boolean isInArrowBounds(int x, int y, int left, int top) {
        return x >= left && x < left + BUTTON_W && y >= top && y < top + BUTTON_H;
    }

    private void playGuiClickSound() {
        if (minecraft != null && minecraft.getSoundManager() != null) {
            AbstractWidget.playButtonClickSound(minecraft.getSoundManager());
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() == 1) {
            int x = (int) event.x();
            int y = (int) event.y();
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
        if (handleMouseClicked(event.x(), event.y(), event.button())) {
            return true;
        }
        return super.mouseClicked(event, isDoubleClick);
    }

    private boolean handleMouseClicked(double mouseX, double mouseY, int button) {
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
        return false;
    }

    private void playButtonSound() {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int titleWidth = this.font.width(this.title);
        int titleX = (this.imageWidth - titleWidth) / 2;
        guiGraphics.text(this.font, this.title, titleX, 8, GuiTextColors.TITLE, false);
    }
}
