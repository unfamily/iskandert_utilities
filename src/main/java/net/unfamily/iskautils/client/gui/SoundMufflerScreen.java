package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.SoundMufflerBlockEntity;
import net.unfamily.iskautils.network.ModMessages;

public class SoundMufflerScreen extends AbstractContainerScreen<SoundMufflerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/backgrounds/sound_muffler.png");

    // Nuove dimensioni dello sfondo (texture allargata)
    private static final int GUI_WIDTH = 230;
    private static final int GUI_HEIGHT = 180;

    private static final int COLS = 3;
    private static final int MARGIN = 10;
    private static final int CELL_W = (GUI_WIDTH - 2 * MARGIN) / COLS;
    private static final int ROW_H = 32;
    private static final int TOP = 24;
    private static final int BUTTON_W = 14;
    private static final int BUTTON_H = 12;
    private static final int ROW_CONTENT_W = BUTTON_W + 4 + 22 + 4 + BUTTON_W;
    private static final int LABEL_Y_OFFSET = 0;
    private static final int ROW_Y_OFFSET = 12;

    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_MARGIN = 5;
    private static final int BOTTOM_BUTTONS_Y = TOP + 4 * ROW_H + 2;
    private static final int BOTTOM_BUTTON_W = 72;
    private static final int BOTTOM_BUTTON_H = 18;
    private static final int BOTTOM_BUTTON_GAP = 6;
    /** Range row: label + small buttons (same structure as category rows). */
    private static final int RANGE_LABEL_WIDTH = 30;
    private static final int RANGE_LABEL_GAP = 4;
    private static final int RANGE_BUTTON_W = BUTTON_W;
    private static final int RANGE_BUTTON_H = BUTTON_H;
    private static final int RANGE_BUTTON_GAP = 4;
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
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = -10000;
    }

    @Override
    protected void init() {
        super.init();

        // Pulsante X di chiusura in alto a destra
        int closeX = this.leftPos + GUI_WIDTH - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_MARGIN;
        int closeY = this.topPos + CLOSE_BUTTON_MARGIN;
        closeButton = Button.builder(Component.literal("✕"), btn -> {
                    playButtonSound();
                    this.onClose();
                })
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

        // Range row: label "Range: X" + buttons 8, 16, 32, 64 (up to config max), then Filter
        int maxRange = Config.soundMufflerRangeMax;
        int[] rangeValues = { 8, 16, 32, 64 };
        int rangeCount = 0;
        for (int v : rangeValues) {
            if (v <= maxRange) rangeCount++;
        }
        int rangeRowW = RANGE_LABEL_WIDTH + RANGE_LABEL_GAP + rangeCount * RANGE_BUTTON_W + (rangeCount - 1) * RANGE_BUTTON_GAP;
        int totalBottomW = rangeRowW + BOTTOM_BUTTON_GAP + BOTTOM_BUTTON_W;
        int baseX = (GUI_WIDTH - totalBottomW) / 2;
        int rangeButtonsStartX = leftPos + baseX + RANGE_LABEL_WIDTH + RANGE_LABEL_GAP;
        int lineY = topPos + BOTTOM_BUTTONS_Y + (BOTTOM_BUTTON_H - RANGE_BUTTON_H) / 2;
        for (int i = 0; i < rangeCount; i++) {
            final int value = rangeValues[i];
            int bx = rangeButtonsStartX + i * (RANGE_BUTTON_W + RANGE_BUTTON_GAP);
            addRenderableWidget(
                    Button.builder(Component.literal(String.valueOf(value)), btn -> setRange(value))
                            .bounds(bx, lineY, RANGE_BUTTON_W, RANGE_BUTTON_H)
                            .build());
        }
        int filterX = leftPos + baseX + rangeRowW + BOTTOM_BUTTON_GAP;
        filterButton = Button.builder(Component.translatable("gui.iska_utils.sound_muffler.filter"), btn -> onFilterClicked())
                .bounds(filterX, topPos + BOTTOM_BUTTONS_Y, BOTTOM_BUTTON_W, BOTTOM_BUTTON_H)
                .build();
        addRenderableWidget(filterButton);
    }

    private void setRange(int value) {
        playButtonSound();
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos.equals(BlockPos.ZERO)) return;
        ModMessages.sendSoundMufflerRangePacket(pos, value);
    }

    private void onFilterClicked() {
        playButtonSound();
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos.equals(BlockPos.ZERO)) return;
        if (minecraft == null || minecraft.player == null) return;
        SoundMufflerFilterMenu filterMenu = new SoundMufflerFilterMenu(0, minecraft.player.getInventory(), pos);
        minecraft.setScreen(new SoundMufflerFilterScreen(filterMenu, minecraft.player.getInventory(),
                Component.translatable("gui.iska_utils.sound_muffler.filter_title")));
    }

    /** Click 10%, Ctrl 5%, Shift 1% */
    private int getStep() {
        if (minecraft == null || minecraft.player == null) return 10;
        if (net.minecraft.client.gui.screens.Screen.hasControlDown() || net.minecraft.client.gui.screens.Screen.hasAltDown()) return 5;
        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) return 1;
        return 10;
    }

    private void adjustVolume(int categoryIndex, int delta) {
        playButtonSound();
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos.equals(BlockPos.ZERO)) return;
        ModMessages.sendSoundMufflerVolumePacket(pos, categoryIndex, delta);
    }

    private void playButtonSound() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(
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
        // Titolo standard centrato in alto
        Component title = Component.translatable("block.iska_utils.sound_muffler");
        int titleWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title, (this.imageWidth - titleWidth) / 2, 8, 0x404040, false);

        // All centered on row 0; then 9 categories in 3x3 grid. Volume by BE index (DISPLAY_TO_CATEGORY).
        for (int i = 0; i < SoundMufflerBlockEntity.CATEGORY_COUNT; i++) {
            int cellX, cellY;
            if (i == 0) {
                cellX = (imageWidth - ROW_CONTENT_W) / 2;
                cellY = TOP + LABEL_Y_OFFSET;
            } else {
                int gridIndex = i - 1;
                int col = gridIndex % COLS;
                int row = gridIndex / COLS;
                cellX = MARGIN + col * CELL_W + (CELL_W - ROW_CONTENT_W) / 2;
                cellY = TOP + (row + 1) * ROW_H + LABEL_Y_OFFSET;
            }
            Component label = Component.translatable(CATEGORY_KEYS[i]);
            String text = label.getString();
            int labelMaxWidth = ROW_CONTENT_W - 4;
            if (font.width(text) > labelMaxWidth) {
                text = font.plainSubstrByWidth(text, labelMaxWidth - 4) + "..";
            }
            int labelX = cellX + (ROW_CONTENT_W - font.width(text)) / 2;
            guiGraphics.drawString(this.font, text, labelX, cellY, 0x404040, false);

            int percent = menu.getVolume(DISPLAY_TO_CATEGORY[i]);
            int lineY = (i == 0) ? TOP + ROW_Y_OFFSET : TOP + ((i - 1) / COLS + 1) * ROW_H + ROW_Y_OFFSET;
            int percentX = cellX + BUTTON_W + (ROW_CONTENT_W - 2 * BUTTON_W - font.width(percent + "%")) / 2;
            int percentY = lineY + (BUTTON_H - this.font.lineHeight) / 2;
            guiGraphics.drawString(this.font, percent + "%", percentX, percentY, 0x404040, false);
        }
        // Range label on bottom row (left of range value buttons); read from BE when available so it updates after packet
        int rangeLabelY = BOTTOM_BUTTONS_Y + (BOTTOM_BUTTON_H - font.lineHeight) / 2;
        int currentRange = menu.getRange();
        if (minecraft != null && minecraft.level != null) {
            var be = menu.getBlockEntityFromLevel(minecraft.level);
            if (be != null) currentRange = be.getRange();
        }
        Component rangeLabel = Component.translatable("gui.iska_utils.sound_muffler.range", currentRange);
        int maxRange = Config.soundMufflerRangeMax;
        int[] rangeValues = { 8, 16, 32, 64 };
        int rangeCount = 0;
        for (int v : rangeValues) {
            if (v <= maxRange) rangeCount++;
        }
        int rangeRowW = RANGE_LABEL_WIDTH + RANGE_LABEL_GAP + rangeCount * RANGE_BUTTON_W + (rangeCount - 1) * RANGE_BUTTON_GAP;
        int totalBottomW = rangeRowW + BOTTOM_BUTTON_GAP + BOTTOM_BUTTON_W;
        int baseX = (imageWidth - totalBottomW) / 2;
        int labelX = baseX;
        guiGraphics.drawString(this.font, rangeLabel, labelX, rangeLabelY, 0x404040, false);
    }

    @Override
    public void containerTick() {
        super.containerTick();
    }
}
