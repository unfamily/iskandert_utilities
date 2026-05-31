package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.integration.jei.IskaUtilsJeiDynamicRefresh;
import net.unfamily.iskautils.block.entity.AncientTableBlockEntity;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.network.ModMessages;
import net.unfamily.iskautils.network.packet.AncientTableScrollC2SPacket;

public class AncientTableScreen extends AbstractContainerScreen<AncientTableMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/backgrounds/ancient_table.png");
    private static final ResourceLocation SCROLLBAR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/scrollbar.png");
    private static final ResourceLocation MEDIUM_BUTTONS =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/medium_buttons.png");
    private static final ResourceLocation REDSTONE_GUI =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/redstone_gui.png");

    public static final int GUI_WIDTH = 200;
    public static final int GUI_HEIGHT = 190;

    private static final int GRID_COLS = 3;
    private static final int GRID_ROWS_VISIBLE = AncientTableMenu.VISIBLE_GRID_ROWS;
    private static final int CELL_STEP = AncientTableMenu.SLOT_SIZE;
    private static final int GRID_PIXEL_W = GRID_COLS * CELL_STEP;
    private static final int GRID_PIXEL_H = GRID_ROWS_VISIBLE * CELL_STEP;

    private static final int SCROLLBAR_WIDTH = AncientTableMenu.SCROLLBAR_WIDTH;
    private static final int HANDLE_SIZE = AncientTableMenu.SCROLLBAR_HANDLE_SIZE;
    private static final int SCROLLBAR_HEIGHT = AncientTableMenu.SCROLLBAR_TRACK_HEIGHT;

    private static final int INPUT_SCROLL_X = AncientTableMenu.INPUT_SCROLL_X;
    private static final int OUTPUT_SCROLL_X = AncientTableMenu.OUTPUT_SCROLL_X;

    private static final int INPUT_BUTTON_UP_Y = AncientTableMenu.INPUT_SCROLL_UP_Y;
    private static final int INPUT_SCROLLBAR_Y = INPUT_BUTTON_UP_Y + HANDLE_SIZE;
    private static final int INPUT_BUTTON_DOWN_Y = INPUT_SCROLLBAR_Y + SCROLLBAR_HEIGHT;

    private static final int OUTPUT_BUTTON_UP_Y = AncientTableMenu.OUTPUT_SCROLL_UP_Y;
    private static final int OUTPUT_SCROLLBAR_Y = OUTPUT_BUTTON_UP_Y + HANDLE_SIZE;
    private static final int OUTPUT_BUTTON_DOWN_Y = OUTPUT_SCROLLBAR_Y + SCROLLBAR_HEIGHT;

    private static final int TITLE_Y = 8;
    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;

    private static final int REDSTONE_BUTTON_SIZE = 16;
    private static final int REDSTONE_BUTTON_X =
            AncientTableMenu.FUEL_X + (AncientTableMenu.SLOT_SIZE - REDSTONE_BUTTON_SIZE) / 2 - 1;
    private static final int REDSTONE_BUTTON_Y = AncientTableMenu.INPUT_GRID_Y;

    private static final Component MACHINE_TITLE =
            Component.translatable("container.iska_utils.ancient_table");

    private Button closeButton;

    private int inputScroll;
    private int outputScroll;
    private int dragSide = -1;
    private int dragStartY;
    private int dragStartScroll;

    public AncientTableScreen(AncientTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
        inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        if (minecraft != null) {
            IskaUtilsJeiDynamicRefresh.scheduleRefresh(minecraft);
        }
        int titleWidth = font.width(MACHINE_TITLE);
        titleLabelX = (imageWidth - titleWidth) / 2;
        titleLabelY = TITLE_Y;
        closeButton = Button.builder(Component.literal("✕"), button -> {
                    playButtonSound();
                    onClose();
                })
                .bounds(leftPos + CLOSE_BUTTON_X, topPos + CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
                .build();
        addRenderableWidget(closeButton);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        inputScroll = menu.getInputScrollOffset();
        outputScroll = menu.getOutputScrollOffset();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, GUI_WIDTH, GUI_HEIGHT);
        renderScrollbarColumn(
                graphics, mouseX, mouseY, INPUT_SCROLL_X, INPUT_BUTTON_UP_Y, inputScroll, maxInputScroll());
        renderScrollbarColumn(
                graphics, mouseX, mouseY, OUTPUT_SCROLL_X, OUTPUT_BUTTON_UP_Y, outputScroll, maxOutputScroll());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderFuelGhostItem(graphics);
        renderRedstoneModeButton(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, MACHINE_TITLE, titleLabelX, titleLabelY, 0x404040, false);

        int max = menu.getFuelChargesMax();
        int cur = menu.getFuelCharges();
        if (max > 0) {
            int pct = (int) Math.floor(100.0 * cur / max);
            String text = pct + "%";
            int tx = AncientTableMenu.FUEL_X + 8 - font.width(text) / 2;
            graphics.drawString(font, text, tx, AncientTableMenu.FUEL_Y + 20, 0x404040, false);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (tryRedstoneTooltip(graphics, mouseX, mouseY)) {
            return;
        }
        if (tryFuelGhostTooltip(graphics, mouseX, mouseY)) {
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderFuelGhostItem(GuiGraphics graphics) {
        if (!menu.getSlot(AncientTableMenu.FUEL_SLOT_INDEX).getItem().isEmpty()) {
            return;
        }
        renderGhostItem(graphics, new ItemStack(ModItems.DROP_OF_ENTROPY.get()), AncientTableMenu.FUEL_X, AncientTableMenu.FUEL_Y);
    }

    /** Semi-transparent placeholder (same pattern as {@link FanScreen}). */
    private void renderGhostItem(GuiGraphics graphics, ItemStack stack, int slotX, int slotY) {
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos + slotX, topPos + slotY, 0.0f);
        graphics.renderItem(stack, 0, 0);
        graphics.fill(0, 0, 16, 16, 0x80000000);
        graphics.pose().popPose();
    }

    private boolean tryFuelGhostTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        Slot fuelSlot = menu.getSlot(AncientTableMenu.FUEL_SLOT_INDEX);
        if (!fuelSlot.getItem().isEmpty() || hoveredSlot != fuelSlot) {
            return false;
        }
        ItemStack ghost = new ItemStack(ModItems.DROP_OF_ENTROPY.get());
        graphics.renderComponentTooltip(font, getTooltipFromContainerItem(ghost), mouseX, mouseY);
        return true;
    }

    private void renderScrollbarColumn(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            int scrollX,
            int buttonUpY,
            int offset,
            int maxScroll) {
        if (maxScroll <= 0) {
            return;
        }

        int guiX = leftPos;
        int guiY = topPos;
        int trackY = buttonUpY + HANDLE_SIZE;
        int buttonDownY = trackY + SCROLLBAR_HEIGHT;

        graphics.blit(
                SCROLLBAR_TEXTURE,
                guiX + scrollX,
                guiY + trackY,
                0,
                0,
                SCROLLBAR_WIDTH,
                SCROLLBAR_HEIGHT,
                32,
                34);

        boolean upHovered = mouseX >= guiX + scrollX
                && mouseX < guiX + scrollX + SCROLLBAR_WIDTH
                && mouseY >= guiY + buttonUpY
                && mouseY < guiY + buttonUpY + HANDLE_SIZE;
        int upV = upHovered ? HANDLE_SIZE : 0;
        graphics.blit(
                SCROLLBAR_TEXTURE,
                guiX + scrollX,
                guiY + buttonUpY,
                SCROLLBAR_WIDTH * 2,
                upV,
                HANDLE_SIZE,
                HANDLE_SIZE,
                32,
                34);

        boolean downHovered = mouseX >= guiX + scrollX
                && mouseX < guiX + scrollX + SCROLLBAR_WIDTH
                && mouseY >= guiY + buttonDownY
                && mouseY < guiY + buttonDownY + HANDLE_SIZE;
        int downV = downHovered ? HANDLE_SIZE : 0;
        graphics.blit(
                SCROLLBAR_TEXTURE,
                guiX + scrollX,
                guiY + buttonDownY,
                SCROLLBAR_WIDTH * 3,
                downV,
                HANDLE_SIZE,
                HANDLE_SIZE,
                32,
                34);

        double ratio = (double) offset / maxScroll;
        int handleY = guiY + trackY + (int) (ratio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
        boolean handleHovered = mouseX >= guiX + scrollX
                && mouseX < guiX + scrollX + HANDLE_SIZE
                && mouseY >= handleY
                && mouseY < handleY + HANDLE_SIZE;
        int handleV = handleHovered ? HANDLE_SIZE : 0;
        graphics.blit(
                SCROLLBAR_TEXTURE,
                guiX + scrollX,
                handleY,
                SCROLLBAR_WIDTH,
                handleV,
                HANDLE_SIZE,
                HANDLE_SIZE,
                32,
                34);
    }

    private void renderRedstoneModeButton(GuiGraphics graphics, int mouseX, int mouseY) {
        int buttonX = leftPos + REDSTONE_BUTTON_X;
        int buttonY = topPos + REDSTONE_BUTTON_Y;
        boolean hovered = mouseX >= buttonX
                && mouseX <= buttonX + REDSTONE_BUTTON_SIZE
                && mouseY >= buttonY
                && mouseY <= buttonY + REDSTONE_BUTTON_SIZE;
        int textureY = hovered ? 16 : 0;
        graphics.blit(
                MEDIUM_BUTTONS, buttonX, buttonY, 0, textureY, REDSTONE_BUTTON_SIZE, REDSTONE_BUTTON_SIZE, 96, 96);
        int iconX = buttonX + 2;
        int iconY = buttonY + 2;
        int iconSize = 12;
        switch (menu.getRedstoneMode()) {
            case 0 -> renderScaledItem(graphics, new ItemStack(Items.GUNPOWDER), iconX, iconY, iconSize);
            case 1 -> renderScaledItem(graphics, new ItemStack(Items.REDSTONE), iconX, iconY, iconSize);
            case 2 -> renderScaledTexture(graphics, REDSTONE_GUI, iconX, iconY, iconSize);
            case 3 -> renderScaledItem(graphics, new ItemStack(Items.REPEATER), iconX, iconY, iconSize);
            case 4 -> renderScaledItem(graphics, new ItemStack(Items.BARRIER), iconX, iconY, iconSize);
            default -> renderScaledItem(graphics, new ItemStack(Items.REDSTONE), iconX, iconY, iconSize);
        }
    }

    private void renderScaledItem(GuiGraphics graphics, ItemStack stack, int x, int y, int size) {
        graphics.pose().pushPose();
        float scale = size / 16.0f;
        graphics.pose().translate(x, y, 0.0f);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();
    }

    private void renderScaledTexture(GuiGraphics graphics, ResourceLocation texture, int x, int y, int size) {
        graphics.pose().pushPose();
        float scale = size / 16.0f;
        graphics.pose().translate(x, y, 0.0f);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.blit(texture, 0, 0, 0, 0, 16, 16, 16, 16);
        graphics.pose().popPose();
    }

    private boolean tryRedstoneTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int bx = leftPos + REDSTONE_BUTTON_X;
        int by = topPos + REDSTONE_BUTTON_Y;
        if (mouseX < bx || mouseX > bx + REDSTONE_BUTTON_SIZE || mouseY < by || mouseY > by + REDSTONE_BUTTON_SIZE) {
            return false;
        }
        int mode = menu.getRedstoneMode();
        Component tooltip =
                switch (mode) {
                    case 0 -> Component.translatable("gui.iska_utils.generic.redstone_mode.none");
                    case 1 -> Component.translatable("gui.iska_utils.generic.redstone_mode.low");
                    case 2 -> Component.translatable("gui.iska_utils.generic.redstone_mode.high");
                    case 3 -> Component.translatable("gui.iska_utils.generic.redstone_mode.pulse");
                    case 4 -> Component.translatable("gui.iska_utils.generic.redstone_mode.disabled");
                    default -> Component.translatable("gui.iska_utils.generic.redstone_mode.disabled");
                };
        graphics.renderTooltip(font, tooltip, mouseX, mouseY);
        return true;
    }

    private int maxInputScroll() {
        return Math.max(0, 63 - AncientTableBlockEntity.VISIBLE_GRID_SLOTS);
    }

    private int maxOutputScroll() {
        return Math.max(0, 63 - AncientTableBlockEntity.VISIBLE_GRID_SLOTS);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 || button == 1) {
            if (handleRedstoneButtonClick(mouseX, mouseY, button)) {
                return true;
            }
        }
        if (button == 0) {
            if (handleScrollButtonClick(mouseX, mouseY, true)
                    || handleScrollButtonClick(mouseX, mouseY, false)
                    || handleHandleClick(mouseX, mouseY, true)
                    || handleHandleClick(mouseX, mouseY, false)
                    || handleScrollbarTrackClick(mouseX, mouseY, true)
                    || handleScrollbarTrackClick(mouseX, mouseY, false)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleRedstoneButtonClick(double mouseX, double mouseY, int button) {
        int bx = leftPos + REDSTONE_BUTTON_X;
        int by = topPos + REDSTONE_BUTTON_Y;
        if (mouseX >= bx && mouseX < bx + REDSTONE_BUTTON_SIZE && mouseY >= by && mouseY < by + REDSTONE_BUTTON_SIZE) {
            ModMessages.sendAncientTableRedstoneMode(menu.getSyncedBlockPos(), button == 1);
            playClick();
            return true;
        }
        return false;
    }

    private boolean handleScrollButtonClick(double mouseX, double mouseY, boolean input) {
        int max = input ? maxInputScroll() : maxOutputScroll();
        if (max <= 0) {
            return false;
        }
        int scrollX = leftPos + (input ? INPUT_SCROLL_X : OUTPUT_SCROLL_X);
        int buttonUpY = topPos + (input ? INPUT_BUTTON_UP_Y : OUTPUT_BUTTON_UP_Y);
        int buttonDownY = buttonUpY + HANDLE_SIZE + SCROLLBAR_HEIGHT;
        if (mouseX >= scrollX
                && mouseX < scrollX + SCROLLBAR_WIDTH
                && mouseY >= buttonUpY
                && mouseY < buttonUpY + HANDLE_SIZE) {
            int old = input ? inputScroll : outputScroll;
            scrollBy(input, -GRID_COLS);
            if ((input ? inputScroll : outputScroll) != old) {
                playClick();
            }
            return true;
        }
        if (mouseX >= scrollX
                && mouseX < scrollX + SCROLLBAR_WIDTH
                && mouseY >= buttonDownY
                && mouseY < buttonDownY + HANDLE_SIZE) {
            int old = input ? inputScroll : outputScroll;
            scrollBy(input, GRID_COLS);
            if ((input ? inputScroll : outputScroll) != old) {
                playClick();
            }
            return true;
        }
        return false;
    }

    private boolean handleHandleClick(double mouseX, double mouseY, boolean input) {
        int max = input ? maxInputScroll() : maxOutputScroll();
        if (max <= 0) {
            return false;
        }
        int offset = input ? inputScroll : outputScroll;
        int scrollX = leftPos + (input ? INPUT_SCROLL_X : OUTPUT_SCROLL_X);
        int trackY = topPos + (input ? INPUT_SCROLLBAR_Y : OUTPUT_SCROLLBAR_Y);
        double ratio = (double) offset / max;
        int handleY = trackY + (int) (ratio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
        if (mouseX >= scrollX
                && mouseX < scrollX + HANDLE_SIZE
                && mouseY >= handleY
                && mouseY < handleY + HANDLE_SIZE) {
            dragSide = input ? 0 : 1;
            dragStartY = (int) mouseY;
            dragStartScroll = offset;
            return true;
        }
        return false;
    }

    private boolean handleScrollbarTrackClick(double mouseX, double mouseY, boolean input) {
        int max = input ? maxInputScroll() : maxOutputScroll();
        if (max <= 0) {
            return false;
        }
        int scrollX = leftPos + (input ? INPUT_SCROLL_X : OUTPUT_SCROLL_X);
        int trackY = topPos + (input ? INPUT_SCROLLBAR_Y : OUTPUT_SCROLLBAR_Y);
        if (mouseX >= scrollX
                && mouseX < scrollX + SCROLLBAR_WIDTH
                && mouseY >= trackY
                && mouseY < trackY + SCROLLBAR_HEIGHT) {
            double clickTrack = (mouseY - trackY) - (HANDLE_SIZE / 2.0);
            double denom = Math.max(1.0, SCROLLBAR_HEIGHT - HANDLE_SIZE);
            double ratio = Mth.clamp(clickTrack / denom, 0.0, 1.0);
            int newOffset = (int) Math.round(ratio * max);
            int old = input ? inputScroll : outputScroll;
            setScroll(input, newOffset);
            if ((input ? inputScroll : outputScroll) != old) {
                playClick();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragSide >= 0) {
            dragSide = -1;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && dragSide >= 0) {
            boolean input = dragSide == 0;
            int max = input ? maxInputScroll() : maxOutputScroll();
            if (max > 0) {
                int deltaY = (int) mouseY - dragStartY;
                float ratio = deltaY / (float) (SCROLLBAR_HEIGHT - HANDLE_SIZE);
                int newOffset = dragStartScroll + (int) (ratio * max);
                setScroll(input, newOffset);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        boolean overInput = mouseX >= leftPos + AncientTableMenu.INPUT_GRID_X
                && mouseX < leftPos + AncientTableMenu.INPUT_GRID_X + GRID_PIXEL_W
                && mouseY >= topPos + AncientTableMenu.INPUT_GRID_Y
                && mouseY < topPos + AncientTableMenu.INPUT_GRID_Y + GRID_PIXEL_H;
        boolean overOutput = mouseX >= leftPos + AncientTableMenu.OUTPUT_GRID_X
                && mouseX < leftPos + AncientTableMenu.OUTPUT_GRID_X + GRID_PIXEL_W
                && mouseY >= topPos + AncientTableMenu.OUTPUT_GRID_Y
                && mouseY < topPos + AncientTableMenu.OUTPUT_GRID_Y + GRID_PIXEL_H;
        if (overInput && deltaY != 0) {
            scrollBy(true, deltaY > 0 ? -GRID_COLS : GRID_COLS);
            return true;
        }
        if (overOutput && deltaY != 0) {
            scrollBy(false, deltaY > 0 ? -GRID_COLS : GRID_COLS);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    private void scrollBy(boolean input, int delta) {
        int max = input ? maxInputScroll() : maxOutputScroll();
        int cur = input ? inputScroll : outputScroll;
        setScroll(input, Mth.clamp(cur + delta, 0, max));
    }

    private void setScroll(boolean input, int offset) {
        offset = (offset / GRID_COLS) * GRID_COLS;
        int max = input ? maxInputScroll() : maxOutputScroll();
        offset = Mth.clamp(offset, 0, max);
        if (input) {
            if (inputScroll != offset) {
                inputScroll = offset;
                ModMessages.sendAncientTableScroll(
                        menu.getSyncedBlockPos(), AncientTableScrollC2SPacket.SIDE_INPUT, offset);
            }
        } else if (outputScroll != offset) {
            outputScroll = offset;
            ModMessages.sendAncientTableScroll(
                    menu.getSyncedBlockPos(), AncientTableScrollC2SPacket.SIDE_OUTPUT, offset);
        }
    }

    private void playButtonSound() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
        }
    }

    private void playClick() {
        playButtonSound();
    }
}
