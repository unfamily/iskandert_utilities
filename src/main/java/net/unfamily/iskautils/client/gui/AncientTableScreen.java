package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.AncientTableBlockEntity;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.network.ModMessages;
import net.unfamily.iskautils.network.packet.AncientTableScrollC2SPacket;

import java.util.List;

public class AncientTableScreen extends AbstractContainerScreen<AncientTableMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/backgrounds/ancient_table.png");
    private static final Identifier SCROLLBAR_TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/scrollbar.png");
    private static final Identifier MEDIUM_BUTTONS =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/medium_buttons.png");
    private static final Identifier REDSTONE_GUI =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/redstone_gui.png");

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
        super(menu, playerInventory, title, GUI_WIDTH, GUI_HEIGHT);
        inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
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
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                leftPos,
                topPos,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                GUI_WIDTH,
                GUI_HEIGHT);
        renderScrollbarColumn(
                graphics, mouseX, mouseY, INPUT_SCROLL_X, INPUT_BUTTON_UP_Y, inputScroll, maxInputScroll());
        renderScrollbarColumn(
                graphics, mouseX, mouseY, OUTPUT_SCROLL_X, OUTPUT_BUTTON_UP_Y, outputScroll, maxOutputScroll());

        if (menu.getSlot(AncientTableMenu.FUEL_SLOT_INDEX).getItem().isEmpty()) {
            graphics.item(new ItemStack(ModItems.DROP_OF_ENTROPY.get()), leftPos + AncientTableMenu.FUEL_X, topPos + AncientTableMenu.FUEL_Y);
            graphics.fill(
                    leftPos + AncientTableMenu.FUEL_X,
                    topPos + AncientTableMenu.FUEL_Y,
                    leftPos + AncientTableMenu.FUEL_X + 16,
                    topPos + AncientTableMenu.FUEL_Y + 16,
                    0x80FFFFFF);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int titleWidth = font.width(MACHINE_TITLE);
        graphics.text(font, MACHINE_TITLE, (imageWidth - titleWidth) / 2, TITLE_Y, GuiTextColors.TITLE, false);

        int max = menu.getFuelChargesMax();
        int cur = menu.getFuelCharges();
        if (max > 0) {
            int pct = (int) Math.floor(100.0 * cur / max);
            Component text = Component.literal(pct + "%");
            int tx = AncientTableMenu.FUEL_X + 8 - font.width(text) / 2;
            graphics.text(font, text, tx, AncientTableMenu.FUEL_Y + 20, GuiTextColors.TITLE, false);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        renderRedstoneModeButton(graphics, mouseX, mouseY);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (tryRedstoneTooltip(graphics, mouseX, mouseY)) {
            return;
        }
        super.extractTooltip(graphics, mouseX, mouseY);
    }

    private void renderScrollbarColumn(
            GuiGraphicsExtractor graphics,
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
                RenderPipelines.GUI_TEXTURED,
                SCROLLBAR_TEXTURE,
                guiX + scrollX,
                guiY + trackY,
                0.0F,
                0.0F,
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
                RenderPipelines.GUI_TEXTURED,
                SCROLLBAR_TEXTURE,
                guiX + scrollX,
                guiY + buttonUpY,
                (float) (SCROLLBAR_WIDTH * 2),
                (float) upV,
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
                RenderPipelines.GUI_TEXTURED,
                SCROLLBAR_TEXTURE,
                guiX + scrollX,
                guiY + buttonDownY,
                (float) (SCROLLBAR_WIDTH * 3),
                (float) downV,
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
                RenderPipelines.GUI_TEXTURED,
                SCROLLBAR_TEXTURE,
                guiX + scrollX,
                handleY,
                (float) SCROLLBAR_WIDTH,
                (float) handleV,
                HANDLE_SIZE,
                HANDLE_SIZE,
                32,
                34);
    }

    private void renderRedstoneModeButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int buttonX = leftPos + REDSTONE_BUTTON_X;
        int buttonY = topPos + REDSTONE_BUTTON_Y;
        boolean hovered = mouseX >= buttonX
                && mouseX <= buttonX + REDSTONE_BUTTON_SIZE
                && mouseY >= buttonY
                && mouseY <= buttonY + REDSTONE_BUTTON_SIZE;
        int textureY = hovered ? 16 : 0;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                MEDIUM_BUTTONS,
                buttonX,
                buttonY,
                0.0F,
                (float) textureY,
                REDSTONE_BUTTON_SIZE,
                REDSTONE_BUTTON_SIZE,
                96,
                96);
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

    private void renderScaledItem(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int size) {
        graphics.pose().pushMatrix();
        float scale = size / 16.0f;
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.item(stack, 0, 0);
        graphics.pose().popMatrix();
    }

    private void renderScaledTexture(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int size) {
        graphics.pose().pushMatrix();
        float scale = size / 16.0f;
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
        graphics.pose().popMatrix();
    }

    private boolean tryRedstoneTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
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
        graphics.setTooltipForNextFrame(
                font,
                List.of(tooltip.getVisualOrderText()),
                DefaultTooltipPositioner.INSTANCE,
                mouseX,
                mouseY,
                true);
        return true;
    }

    private int maxInputScroll() {
        return Math.max(0, 63 - AncientTableBlockEntity.VISIBLE_GRID_SLOTS);
    }

    private int maxOutputScroll() {
        return Math.max(0, 63 - AncientTableBlockEntity.VISIBLE_GRID_SLOTS);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
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
        return super.mouseClicked(event, doubleClick);
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
            int old = input ? inputScroll : outputScroll;
            setScroll(input, (int) Math.round(ratio * max));
            if ((input ? inputScroll : outputScroll) != old) {
                playClick();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && dragSide >= 0) {
            dragSide = -1;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0 && dragSide >= 0) {
            boolean input = dragSide == 0;
            int max = input ? maxInputScroll() : maxOutputScroll();
            if (max > 0) {
                int deltaY = (int) event.y() - dragStartY;
                float ratio = deltaY / (float) (SCROLLBAR_HEIGHT - HANDLE_SIZE);
                int newOffset = dragStartScroll + (int) (ratio * max);
                setScroll(input, newOffset);
            }
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
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
            minecraft.getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private void playClick() {
        playButtonSound();
    }
}
