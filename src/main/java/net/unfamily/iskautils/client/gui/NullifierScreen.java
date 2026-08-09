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
import net.unfamily.iskalib.client.marker.AreaBorderRenderer;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.network.packet.NullifierRangeC2SPacket;
import net.unfamily.iskautils.network.packet.NullifierRedstoneModeC2SPacket;
import net.unfamily.iskautils.network.packet.NullifierShowAreaC2SPacket;
import org.lwjgl.glfw.GLFW;

public class NullifierScreen extends AbstractContainerScreen<NullifierMenu> {

    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/backgrounds/nullifier.png");
    private static final ResourceLocation REDSTONE_GUI = ResourceLocation.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/redstone_gui.png");
    private static final ResourceLocation SINGLE_SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/single_slot.png");

    private static final ItemStack GHOST_RANGE_MODULE = new ItemStack(ModItems.RANGE_MODULE.get());

    private static final int GUI_WIDTH = NullifierMenu.GUI_WIDTH;
    private static final int GUI_HEIGHT = NullifierMenu.GUI_HEIGHT;
    private static final int TEXTURE_WIDTH = NullifierMenu.TEXTURE_WIDTH;
    private static final int TEXTURE_HEIGHT = NullifierMenu.TEXTURE_HEIGHT;

    private static final int TITLE_COLOR = 0x404040;
    private static final int BTN = 16;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;
    private static final int CLOSE_BUTTON_Y = 5;

    private static final int REDSTONE_BTN_X = 8;
    private static final int SLOT_SIDE_BTN_Y = NullifierMenu.MODULE_SLOT_Y;

    private static final int ROW_BTN_GAP = 2;
    private static final int RANGE_ROW_Y = NullifierMenu.PLAYER_INV_Y - BTN - 2;
    private static final int RANGE_BTN_X = 8;
    private static final int SHOW_BTN_W = 40;
    private static final int SHOW_BTN_X = GUI_WIDTH - 8 - SHOW_BTN_W;
    private static final int RANGE_BTN_WIDTH = SHOW_BTN_X - RANGE_BTN_X - ROW_BTN_GAP;

    private ItemIconButton redstoneModeButton;
    private Button rangeButton;
    private Button showAreaButton;
    private Button closeButton;
    private boolean showingArea;

    public NullifierScreen(NullifierMenu menu, Inventory playerInventory, Component title) {
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

        closeButton = Button.builder(Component.literal("\u2715"), b -> {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.closeContainer();
            }
        }).bounds(leftPos + CLOSE_BUTTON_X, topPos + CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE).build();
        addRenderableWidget(closeButton);

        redstoneModeButton = addRenderableWidget(new ItemIconButton(
                leftPos + REDSTONE_BTN_X,
                topPos + SLOT_SIDE_BTN_Y,
                BTN,
                b -> cycleRedstoneMode(false),
                this::nullifierRedstoneIcon,
                this::nullifierRedstoneOverlay,
                Component.empty()));

        rangeButton = addRenderableWidget(Button.builder(Component.empty(), b -> {})
                .bounds(leftPos + RANGE_BTN_X, topPos + RANGE_ROW_Y, RANGE_BTN_WIDTH, BTN)
                .tooltip(Tooltip.create(Component.translatable("gui.iska_utils.nullifier.tooltip.range")))
                .build());

        showingArea = menu.isShowAreaEnabled();
        showAreaButton = addRenderableWidget(Button.builder(showAreaLabel(), b -> toggleArea())
                .bounds(leftPos + SHOW_BTN_X, topPos + RANGE_ROW_Y, SHOW_BTN_W, BTN)
                .tooltip(Tooltip.create(Component.translatable(
                        showingArea
                                ? "gui.iska_utils.nullifier.tooltip.hide_area"
                                : "gui.iska_utils.nullifier.tooltip.show_area")))
                .build());
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (rangeButton != null) {
            rangeButton.setMessage(rangeButtonLabel());
        }
        boolean nowShowing = menu.isShowAreaEnabled();
        if (nowShowing != showingArea) {
            showingArea = nowShowing;
            if (showAreaButton != null) {
                showAreaButton.setMessage(showAreaLabel());
                showAreaButton.setTooltip(Tooltip.create(Component.translatable(
                        showingArea
                                ? "gui.iska_utils.nullifier.tooltip.hide_area"
                                : "gui.iska_utils.nullifier.tooltip.show_area")));
            }
            if (!showingArea) {
                AreaBorderRenderer.getInstance().clearArea(areaKey());
            }
        }
        if (showingArea) {
            refreshAreaPreview();
        }
        if (redstoneModeButton != null) {
            redstoneModeButton.setTooltip(Tooltip.create(nullifierRedstoneTooltip(menu.getRedstoneModeGui())));
        }
    }

    @Override
    public void removed() {
        super.removed();
        // Keep the area border after closing the GUI while Show is enabled (Fan-style persistence).
    }

    private Component rangeButtonLabel() {
        return Component.translatable(
                "gui.iska_utils.nullifier.range",
                menu.getRange() + " / " + menu.getMaxRange());
    }

    private boolean isShiftDownNow() {
        if (minecraft == null) {
            return false;
        }
        long window = minecraft.getWindow().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (rangeButton != null && rangeButton.isMouseOver(mouseX, mouseY)) {
            boolean shift = isShiftDownNow();
            int range = menu.getRange();
            int max = menu.getMaxRange();
            if (button == 0) {
                sendRange(shift ? (max - range) : 1);
                return true;
            }
            if (button == 1) {
                sendRange(shift ? (1 - range) : -1);
                return true;
            }
        }
        if (button == 1 && redstoneModeButton != null && redstoneModeButton.isMouseOver(mouseX, mouseY)) {
            cycleRedstoneMode(true);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void sendRange(int delta) {
        if (delta == 0) {
            return;
        }
        PacketDistributor.sendToServer(new NullifierRangeC2SPacket(menu.getSyncedBlockPos(), delta));
    }

    private void cycleRedstoneMode(boolean backward) {
        int next = backward ? cycleModeBackward(menu.getRedstoneModeGui()) : cycleModeForward(menu.getRedstoneModeGui());
        PacketDistributor.sendToServer(new NullifierRedstoneModeC2SPacket(menu.getSyncedBlockPos(), next));
    }

    private static int cycleModeForward(int mode) {
        return switch (mode) {
            case 0 -> 2;
            case 2 -> 3;
            case 3 -> 1;
            default -> 0;
        };
    }

    private static int cycleModeBackward(int mode) {
        return switch (mode) {
            case 0 -> 1;
            case 1 -> 3;
            case 3 -> 2;
            default -> 0;
        };
    }

    private void toggleArea() {
        boolean enabling = !menu.isShowAreaEnabled();
        showingArea = enabling;
        if (showAreaButton != null) {
            showAreaButton.setMessage(showAreaLabel());
        }
        PacketDistributor.sendToServer(new NullifierShowAreaC2SPacket(menu.getSyncedBlockPos(), enabling));
        if (enabling) {
            refreshAreaPreview();
        } else {
            AreaBorderRenderer.getInstance().clearArea(areaKey());
        }
        if (showAreaButton != null) {
            showAreaButton.setTooltip(Tooltip.create(Component.translatable(
                    enabling
                            ? "gui.iska_utils.nullifier.tooltip.hide_area"
                            : "gui.iska_utils.nullifier.tooltip.show_area")));
        }
    }

    private void refreshAreaPreview() {
        BlockPos center = menu.getSyncedBlockPos();
        if (center.equals(BlockPos.ZERO)) {
            return;
        }
        int r = menu.getRange();
        AreaBorderRenderer.getInstance().showArea(
                areaKey(),
                center.offset(-r, -r, -r),
                center.offset(r, r, r),
                AreaBorderRenderer.DEFAULT_MACHINE_COLOR,
                0);
    }

    private Component showAreaLabel() {
        return showingArea
                ? Component.translatable("gui.iska_utils.generic.hide")
                : Component.translatable("gui.iska_utils.generic.show");
    }

    private Object areaKey() {
        return "nullifier_area_" + menu.getSyncedBlockPos().toShortString();
    }

    /** GUI mode: 0=Ignore redstone (gunpowder), 1=Disabled, 2=Low, 3=High */
    private ItemStack nullifierRedstoneIcon() {
        return switch (menu.getRedstoneModeGui()) {
            case 0 -> new ItemStack(Items.GUNPOWDER);
            case 1 -> new ItemStack(Items.BARRIER);
            case 2 -> new ItemStack(Items.REDSTONE);
            case 3 -> ItemStack.EMPTY;
            default -> new ItemStack(Items.GUNPOWDER);
        };
    }

    private ResourceLocation nullifierRedstoneOverlay() {
        return menu.getRedstoneModeGui() == 3 ? REDSTONE_GUI : null;
    }

    private static Component nullifierRedstoneTooltip(int mode) {
        return switch (mode) {
            case 0 -> Component.translatable("gui.iska_utils.generic.redstone_mode.none");
            case 1 -> Component.translatable("gui.iska_utils.generic.redstone_mode.disabled");
            case 2 -> Component.translatable("gui.iska_utils.generic.redstone_mode.low");
            case 3 -> Component.translatable("gui.iska_utils.generic.redstone_mode.high");
            default -> Component.literal("Unknown mode");
        };
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        renderModuleGhostTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(
                BACKGROUND,
                leftPos,
                topPos,
                0,
                0,
                imageWidth,
                imageHeight,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT);
        graphics.blit(
                SINGLE_SLOT_TEXTURE,
                leftPos + NullifierMenu.MODULE_SLOT_FRAME_X,
                topPos + NullifierMenu.MODULE_SLOT_FRAME_Y,
                0,
                0,
                18,
                18,
                18,
                18);
        renderModuleGhost(graphics);
    }

    private void renderModuleGhost(GuiGraphics graphics) {
        Slot slot = menu.getSlot(NullifierMenu.MODULE_SLOT_INDEX);
        GuiGhostItem.render(graphics, leftPos, topPos, slot, GHOST_RANGE_MODULE);
    }

    private void renderModuleGhostTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        Slot moduleSlot = menu.getSlot(NullifierMenu.MODULE_SLOT_INDEX);
        if (!moduleSlot.getItem().isEmpty() || !isMouseOverSlot(moduleSlot, mouseX, mouseY)) {
            return;
        }
        graphics.renderTooltip(
                font,
                getTooltipFromContainerItem(GHOST_RANGE_MODULE),
                GHOST_RANGE_MODULE.getTooltipImage(),
                mouseX,
                mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        String titleKey = switch (menu.getTypeId()) {
            case 1 -> "gui.iska_utils.wander_nullifier.title";
            case 2 -> "gui.iska_utils.soul_nullifier.title";
            default -> "gui.iska_utils.ender_nullifier.title";
        };
        Component title = Component.translatable(titleKey);
        graphics.drawString(font, title, (imageWidth - font.width(title)) / 2, 8, TITLE_COLOR, false);
    }

    private boolean isMouseOverSlot(Slot slot, int mouseX, int mouseY) {
        return isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY);
    }
}
