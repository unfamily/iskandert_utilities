package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.SoundMufflerBlockEntity;
import net.unfamily.iskautils.network.ModMessages;

public class SoundMufflerScreen extends AbstractContainerScreen<SoundMufflerMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/backgrounds/sound_muffler.png");

    // Nuove dimensioni dello sfondo (texture allargata)
    private static final int GUI_WIDTH = 230;
    private static final int GUI_HEIGHT = 180;

    private static final int COLS = 3;
    private static final int MARGIN = 10;
    private static final int CELL_W = (GUI_WIDTH - 2 * MARGIN) / COLS;
    private static final int ROW_H = 32;
    private static final int TOP = 24;
    /** Extra Y offset for label text only (categories + Range), not for buttons. */
    private static final int LABEL_VERTICAL_OFFSET = 3;
    private static final int BUTTON_W = 14;
    private static final int BUTTON_H = 12;
    private static final int ROW_CONTENT_W = BUTTON_W + 4 + 22 + 4 + BUTTON_W;
    /** Same vertical gap from label (bottom) to button row (top) for all rows (categories + Range). */
    private static final int LABEL_TO_BUTTON_GAP = 3;
    private static final int FONT_LINE_HEIGHT = 9;
    private static final int ROW_Y_OFFSET = FONT_LINE_HEIGHT + LABEL_TO_BUTTON_GAP;

    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_MARGIN = 5;
    private static final int BOTTOM_BUTTONS_Y = TOP + 4 * ROW_H + 2;
    private static final int BOTTOM_BUTTON_W = 72;
    private static final int BOTTOM_BUTTON_H = 18;
    private static final int BOTTOM_BUTTON_GAP = 6;
    /** Range row: same structure as category rows (Blocks etc.): [-] label [+] then Filter button. */
    private Button closeButton;
    private Button filterButton;

    /** Display order: All first (alone), then Records..Voice, Other last. Maps display slot -> BE category index. */
    private static final int[] DISPLAY_TO_CATEGORY = { 0, 2, 3, 4, 5, 6, 7, 8, 9, 1 };
    private static final String[] CATEGORY_KEYS = {
            "gui.iska_utils.sound_muffler.cat.all",
            "gui.iska_utils.sound_muffler.cat.records",
            "gui.iska_utils.sound_muffler.cat.weather",
            "gui.iska_utils.sound_muffler.cat.blocks",
            "gui.iska_utils.sound_muffler.cat.hostile",
            "gui.iska_utils.sound_muffler.cat.neutral",
            "gui.iska_utils.sound_muffler.cat.players",
            "gui.iska_utils.sound_muffler.cat.ambient",
            "gui.iska_utils.sound_muffler.cat.voice",
            "gui.iska_utils.sound_muffler.cat.other"
    };

    public SoundMufflerScreen(SoundMufflerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GUI_WIDTH, GUI_HEIGHT);
        this.inventoryLabelY = -10000;
    }

    @Override
    protected void init() {
        super.init();

        // Pulsante X di chiusura in alto a destra
        int closeX = this.leftPos + GUI_WIDTH - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_MARGIN;
        int closeY = this.topPos + CLOSE_BUTTON_MARGIN;
        closeButton = Button.builder(Component.literal("✕"), btn -> this.onClose())
                .bounds(closeX, closeY, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
                .build();
        addRenderableWidget(closeButton);

        // All alone on row 0 (centered); then 9 categories in 3x3 grid. Use DISPLAY_TO_CATEGORY for BE index.
        for (int i = 0; i < SoundMufflerBlockEntity.CATEGORY_COUNT; i++) {
            final int categoryIndex = DISPLAY_TO_CATEGORY[i];
            int cellX, cellY;
            if (i == 0) {
                cellX = leftPos + (GUI_WIDTH - ROW_CONTENT_W) / 2;
                cellY = topPos + TOP;
            } else {
                int gridIndex = i - 1;
                int col = gridIndex % COLS;
                int row = gridIndex / COLS;
                cellX = leftPos + MARGIN + col * CELL_W + (CELL_W - ROW_CONTENT_W) / 2;
                cellY = topPos + TOP + (row + 1) * ROW_H;
            }
            int lineY = cellY + ROW_Y_OFFSET;
            int rowStartX = cellX;

            int minusX = rowStartX;
            int plusX = rowStartX + ROW_CONTENT_W - BUTTON_W;

            addRenderableWidget(
                    Button.builder(Component.literal("-"), btn -> adjustVolume(categoryIndex, -getStep()))
                            .bounds(minusX, lineY, BUTTON_W, BUTTON_H)
                            .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.iska_utils.sound_muffler.tooltip.step")))
                            .build());
            addRenderableWidget(
                    Button.builder(Component.literal("+"), btn -> adjustVolume(categoryIndex, getStep()))
                            .bounds(plusX, lineY, BUTTON_W, BUTTON_H)
                            .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.iska_utils.sound_muffler.tooltip.step")))
                            .build());
        }

        // Range row: same structure as category rows (Blocks etc.) — [-] "Range: 8" [+] then Filter
        int totalBottomW = ROW_CONTENT_W + BOTTOM_BUTTON_GAP + BOTTOM_BUTTON_W;
        int rangeRowX = leftPos + (GUI_WIDTH - totalBottomW) / 2;
        int lineY = topPos + BOTTOM_BUTTONS_Y + (BOTTOM_BUTTON_H - BUTTON_H) / 2;
        int minusX = rangeRowX;
        int plusX = rangeRowX + ROW_CONTENT_W - BUTTON_W;
        addRenderableWidget(
                Button.builder(Component.literal("-"), btn -> setRangeStep(-1))
                        .bounds(minusX, lineY, BUTTON_W, BUTTON_H)
                        .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.iska_utils.sound_muffler.tooltip.range_step")))
                        .build());
        addRenderableWidget(
                Button.builder(Component.literal("+"), btn -> setRangeStep(1))
                        .bounds(plusX, lineY, BUTTON_W, BUTTON_H)
                        .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.iska_utils.sound_muffler.tooltip.range_step")))
                        .build());
        int filterX = leftPos + (GUI_WIDTH - totalBottomW) / 2 + ROW_CONTENT_W + BOTTOM_BUTTON_GAP;
        filterButton = Button.builder(Component.translatable("gui.iska_utils.sound_muffler.filter"), btn -> onFilterClicked())
                .bounds(filterX, topPos + BOTTOM_BUTTONS_Y, BOTTOM_BUTTON_W, BOTTOM_BUTTON_H)
                .build();
        addRenderableWidget(filterButton);
    }

    /** Range step: click 1, ctrl/alt 5, shift 10 (like volume but for range value). */
    private int getRangeStep() {
        if (minecraft == null || minecraft.player == null) return 1;
        if (isShiftDownNow()) return 10;
        if (isCtrlDownNow() || isAltDownNow()) return 5;
        return 1;
    }

    private int getCurrentRangeForStep() {
        if (minecraft != null && minecraft.level != null) {
            var be = menu.getBlockEntityFromLevel(minecraft.level);
            if (be != null) return be.getRange();
        }
        return menu.getRange();
    }

    private void setRangeStep(int delta) {
        int step = getRangeStep();
        int current = getCurrentRangeForStep();
        int maxRange = Config.soundMufflerRangeMax;
        int newValue = Math.max(SoundMufflerBlockEntity.RANGE_MIN, Math.min(maxRange, current + delta * step));
        setRange(newValue);
    }

    private void setRange(int value) {
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos.equals(BlockPos.ZERO)) return;
        ModMessages.sendSoundMufflerRangePacket(pos, value);
    }

    private void onFilterClicked() {
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos.equals(BlockPos.ZERO)) return;
        if (minecraft == null || minecraft.player == null) return;
        SoundMufflerFilterMenu filterMenu = new SoundMufflerFilterMenu(0, minecraft.player.getInventory(), pos);
        minecraft.setScreen(new SoundMufflerFilterScreen(filterMenu, minecraft.player.getInventory(),
                Component.translatable("gui.iska_utils.sound_muffler.filter_title"), this));
    }

    /** Click 10%, Ctrl 5%, Shift 1% */
    private int getStep() {
        if (minecraft == null || minecraft.player == null) return 10;
        if (isCtrlDownNow() || isAltDownNow()) return 5;
        if (isShiftDownNow()) return 1;
        return 10;
    }

    private void adjustVolume(int categoryIndex, int delta) {
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos.equals(BlockPos.ZERO)) return;
        ModMessages.sendSoundMufflerVolumePacket(pos, categoryIndex, delta);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        // Titolo standard centrato in alto
        Component title = Component.translatable("block.iska_utils.sound_muffler");
        int titleWidth = this.font.width(title);
        guiGraphics.text(this.font, title, (this.imageWidth - titleWidth) / 2, 8, 0x404040, false);

        // All centered on row 0; then 9 categories in 3x3 grid. Volume by BE index (DISPLAY_TO_CATEGORY).
        for (int i = 0; i < SoundMufflerBlockEntity.CATEGORY_COUNT; i++) {
            int cellX, cellY;
            if (i == 0) {
                cellX = (imageWidth - ROW_CONTENT_W) / 2;
                cellY = TOP;
            } else {
                int gridIndex = i - 1;
                int col = gridIndex % COLS;
                int row = gridIndex / COLS;
                cellX = MARGIN + col * CELL_W + (CELL_W - ROW_CONTENT_W) / 2;
                cellY = TOP + (row + 1) * ROW_H;
            }
            Component label = Component.translatable(CATEGORY_KEYS[i]);
            String text = label.getString();
            int labelMaxWidth = ROW_CONTENT_W - 4;
            if (font.width(text) > labelMaxWidth) {
                text = font.plainSubstrByWidth(text, labelMaxWidth - 4) + "..";
            }
            int labelX = cellX + (ROW_CONTENT_W - font.width(text)) / 2;
            guiGraphics.text(this.font, Component.literal(text), labelX, cellY + LABEL_VERTICAL_OFFSET, 0x404040, false);

            int percent = menu.getVolume(DISPLAY_TO_CATEGORY[i]);
            int lineY = (i == 0) ? TOP + ROW_Y_OFFSET : TOP + ((i - 1) / COLS + 1) * ROW_H + ROW_Y_OFFSET;
            int percentX = cellX + BUTTON_W + (ROW_CONTENT_W - 2 * BUTTON_W - font.width(percent + "%")) / 2;
            int percentY = lineY + (BUTTON_H - this.font.lineHeight) / 2;
            guiGraphics.text(this.font, Component.literal(percent + "%"), percentX, percentY, 0x404040, false);
        }
        // Range row: same layout as category rows — label at (rangeRowCellY), buttons at (rangeRowCellY + ROW_Y_OFFSET)
        int totalBottomW = ROW_CONTENT_W + BOTTOM_BUTTON_GAP + BOTTOM_BUTTON_W;
        int rangeRowX = (imageWidth - totalBottomW) / 2;
        Component rangeLabel = Component.translatable("gui.iska_utils.sound_muffler.range_label");
        int rangeLabelX = rangeRowX + (ROW_CONTENT_W - font.width(rangeLabel)) / 2;
        // Range -/+ buttons top in init() is BOTTOM_BUTTONS_Y + (BOTTOM_BUTTON_H - BUTTON_H)/2; label uses same offset as categories
        int rangeLineY = BOTTOM_BUTTONS_Y + (BOTTOM_BUTTON_H - BUTTON_H) / 2;
        int rangeCellY = rangeLineY - ROW_Y_OFFSET;
        int rangeLabelY = rangeCellY + LABEL_VERTICAL_OFFSET;
        guiGraphics.text(this.font, rangeLabel, rangeLabelX, rangeLabelY, 0x404040, false);
        int currentRange = menu.getRange();
        if (minecraft != null && minecraft.level != null) {
            var be = menu.getBlockEntityFromLevel(minecraft.level);
            if (be != null) currentRange = be.getRange();
        }
        String valueStr = String.valueOf(currentRange);
        int valueX = rangeRowX + BUTTON_W + (ROW_CONTENT_W - 2 * BUTTON_W - font.width(valueStr)) / 2;
        int valueY = BOTTOM_BUTTONS_Y + (BOTTOM_BUTTON_H - font.lineHeight) / 2;
        guiGraphics.text(this.font, Component.literal(valueStr), valueX, valueY, 0x404040, false);
    }

    private boolean isShiftDownNow() {
        if (this.minecraft == null) return false;
        var window = this.minecraft.getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private boolean isCtrlDownNow() {
        if (this.minecraft == null) return false;
        var window = this.minecraft.getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    private boolean isAltDownNow() {
        if (this.minecraft == null) return false;
        var window = this.minecraft.getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT);
    }

    @Override
    public void containerTick() {
        super.containerTick();
    }
}
