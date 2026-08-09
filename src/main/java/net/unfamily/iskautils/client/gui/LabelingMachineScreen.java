package net.unfamily.iskautils.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.neoforge.network.PacketDistributor;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.network.packet.LabelingMachineLoreC2SPacket;
import net.unfamily.iskautils.network.packet.LabelingMachineRenameC2SPacket;
import net.unfamily.iskautils.util.LabelingNameStyle;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Labeling Machine GUI: name segments, lore list/line editors, nested COLOR_PICKER (HSV).
 */
public class LabelingMachineScreen extends AbstractContainerScreen<LabelingMachineMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/backgrounds/shop.png");
    private static final ResourceLocation SINGLE_SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/single_slot.png");
    private static final ResourceLocation SCROLLBAR_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/scrollbar.png");

    private static final int GUI_WIDTH = 300;
    private static final int GUI_HEIGHT = 240;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;
    private static final int INVENTORY_Y = LabelingMachineMenu.INVENTORY_Y;

    private static final int RENAME_BUTTON_X = LabelingMachineMenu.TARGET_SLOT_X + 18 + 4;
    private static final int RENAME_BUTTON_Y = LabelingMachineMenu.TARGET_SLOT_Y - 1;
    private static final int RENAME_BUTTON_W = 60;
    private static final int RENAME_BUTTON_H = 18;
    private static final int COPY_BUTTON_X = RENAME_BUTTON_X + RENAME_BUTTON_W + 3;
    private static final int COPY_BUTTON_W = 50;
    private static final int RESET_BUTTON_X = COPY_BUTTON_X + COPY_BUTTON_W + 3;
    private static final int RESET_BUTTON_W = 50;

    private static final int LORE_NAV_X = 20;
    private static final int LORE_NAV_Y = INVENTORY_Y - 22;
    private static final int LORE_NAV_W = 50;
    private static final int LORE_PREVIEW_CHARS = 24;

    private static final int PREVIEW_Y = 52;
    /** Opaque dark strip on light shop background so colored preview text stays readable. */
    private static final int PREVIEW_BG = 0xFF1A1A1A;

    private static final int VISIBLE_SEGMENTS = 3;
    private static final int VISIBLE_LORE_BTNS = 3;
    private static final int LIST_START_Y = 72;
    private static final int SEGMENT_ROW_H = 20;
    private static final int STYLE_SIZE = 16;
    private static final int STYLE_GAP = 2;
    private static final int COLOR_SWATCH = 16;
    private static final int ROW_CONTROLS_X = 20;
    private static final int SEGMENT_EDIT_X = ROW_CONTROLS_X + 5 * (STYLE_SIZE + STYLE_GAP) + COLOR_SWATCH + 4;
    private static final int SEGMENT_EDIT_H = 16;
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int SCROLLBAR_HEIGHT = 34;
    private static final int HANDLE_SIZE = 8;
    private static final int SCROLLBAR_X = GUI_WIDTH - 12 - SCROLLBAR_WIDTH;
    private static final int SEGMENT_EDIT_W = SCROLLBAR_X - SEGMENT_EDIT_X - 4;
    private static final int LORE_LINE_BTN_X = ROW_CONTROLS_X;
    private static final int LORE_LINE_BTN_W = SCROLLBAR_X - LORE_LINE_BTN_X - 4;
    private static final int BUTTON_UP_Y = LIST_START_Y;
    private static final int SCROLLBAR_Y = LIST_START_Y + HANDLE_SIZE;
    private static final int BUTTON_DOWN_Y = SCROLLBAR_Y + SCROLLBAR_HEIGHT;

    /** Selected segment row highlight (behind widgets). */
    private static final int SELECTION_HIGHLIGHT = 0x60B8E8C0;
    private static final int PANEL_COVER_COLOR = 0xFFC6C6C6;

    private static final int SV_SIZE = 70;
    private static final int SV_X = 20;
    private static final int SV_Y = 24;
    private static final int HUE_BAR_W = 12;
    private static final int HUE_BAR_X = SV_X + SV_SIZE + 6;
    private static final int HUE_BAR_Y = SV_Y;
    private static final int SWATCH_SIZE = 18;
    private static final int HEX_EDIT_X = HUE_BAR_X + HUE_BAR_W + 10;
    private static final int HEX_EDIT_Y = SV_Y;
    private static final int HEX_EDIT_W = 70;
    private static final int HEX_EDIT_H = 16;
    private static final int PALETTE_START_Y = SV_Y + SV_SIZE + 6;
    private static final int PALETTE_COLS = 8;
    private static final int PALETTE_SIZE = 16;
    private static final int PALETTE_GAP = 3;

    private static final String[] STYLE_LABELS = {"B", "I", "U", "S", "O"};
    private static final String[] STYLE_TIPS = {
            "gui.iska_utils.labeling_machine.style.bold",
            "gui.iska_utils.labeling_machine.style.italic",
            "gui.iska_utils.labeling_machine.style.underline",
            "gui.iska_utils.labeling_machine.style.strikethrough",
            "gui.iska_utils.labeling_machine.style.obfuscated"
    };

    private enum SubView { MAIN, COLOR_PICKER, LORE, LORE_LINE }

    private enum ColorDrag { NONE, SV, HUE }

    private SubView subView = SubView.MAIN;
    private SubView colorPickerReturnView = SubView.MAIN;

    private Button applyButton;
    private Button copyButton;
    private Button resetButton;
    private Button loreNavButton;
    private final Button[] loreLineButtons = new Button[VISIBLE_LORE_BTNS];
    private final Button[][] styleButtons = new Button[VISIBLE_SEGMENTS][5];
    private final Button[] colorButtons = new Button[VISIBLE_SEGMENTS];
    private final EditBox[] segmentEdits = new EditBox[VISIBLE_SEGMENTS];

    private EditBox hexEdit;
    private Button applyColorButton;
    private Button cancelColorButton;

    /** Working segment editor (name on MAIN, lore line on LORE_LINE). */
    private final List<LabelingNameStyle.Segment> segments = new ArrayList<>();
    private final List<LabelingNameStyle.Segment> stashedNameSegments = new ArrayList<>();
    private final List<List<LabelingNameStyle.Segment>> loreLines = new ArrayList<>();
    private int editingLoreLine;
    private int loreScrollOffset;

    private int selectedIndex;
    private int scrollOffset;
    private boolean isDraggingHandle;
    private boolean isDraggingLoreHandle;
    private int dragStartY;
    private int dragStartScrollOffset;

    private float pickerHue = 0f;
    private float pickerSat = 0f;
    private float pickerVal = 1f;
    private int draftColorRgb = LabelingNameStyle.DEFAULT_COLOR_RGB;
    private ColorDrag colorDrag = ColorDrag.NONE;
    private boolean suppressHexResponder;
    private boolean suppressSegmentResponder;

    public LabelingMachineScreen(LabelingMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
        segments.add(LabelingNameStyle.Segment.blank());
        ensureLoreLinesCapacity();
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();
        ensureLoreLinesCapacity();

        addRenderableWidget(Button.builder(Component.literal("✕"), b -> handleCloseOrBack())
                .bounds(leftPos + CLOSE_BUTTON_X, topPos + CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
                .build());

        applyButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.iska_utils.labeling_machine.apply"),
                b -> onApplyPressed())
                .bounds(leftPos + RENAME_BUTTON_X, topPos + RENAME_BUTTON_Y, RENAME_BUTTON_W, RENAME_BUTTON_H)
                .build());
        copyButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.iska_utils.labeling_machine.copy"),
                b -> onCopyPressed())
                .bounds(leftPos + COPY_BUTTON_X, topPos + RENAME_BUTTON_Y, COPY_BUTTON_W, RENAME_BUTTON_H)
                .tooltip(Tooltip.create(Component.translatable("gui.iska_utils.labeling_machine.copy.tooltip")))
                .build());
        resetButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.iska_utils.labeling_machine.reset"),
                b -> onResetPressed())
                .bounds(leftPos + RESET_BUTTON_X, topPos + RENAME_BUTTON_Y, RESET_BUTTON_W, RENAME_BUTTON_H)
                .build());

        loreNavButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.iska_utils.labeling_machine.lore"),
                b -> onLoreNavPressed())
                .bounds(leftPos + LORE_NAV_X, topPos + LORE_NAV_Y, LORE_NAV_W, RENAME_BUTTON_H)
                .build());

        for (int i = 0; i < VISIBLE_LORE_BTNS; i++) {
            final int visibleSlot = i;
            loreLineButtons[i] = addRenderableWidget(Button.builder(Component.empty(),
                            b -> enterLoreLine(loreScrollOffset + visibleSlot))
                    .bounds(leftPos + LORE_LINE_BTN_X,
                            topPos + LIST_START_Y + visibleSlot * SEGMENT_ROW_H,
                            LORE_LINE_BTN_W,
                            SEGMENT_EDIT_H)
                    .build());
        }

        for (int row = 0; row < VISIBLE_SEGMENTS; row++) {
            final int visibleRow = row;
            int rowY = topPos + LIST_START_Y + row * SEGMENT_ROW_H;

            for (int s = 0; s < 5; s++) {
                final int styleIdx = s;
                int x = leftPos + ROW_CONTROLS_X + s * (STYLE_SIZE + STYLE_GAP);
                styleButtons[row][s] = addRenderableWidget(Button.builder(Component.literal(STYLE_LABELS[s]),
                                b -> toggleStyle(visibleRow, styleIdx))
                        .bounds(x, rowY, STYLE_SIZE, STYLE_SIZE)
                        .tooltip(Tooltip.create(Component.translatable(STYLE_TIPS[s])))
                        .build());
            }

            int colorX = leftPos + ROW_CONTROLS_X + 5 * (STYLE_SIZE + STYLE_GAP);
            colorButtons[row] = addRenderableWidget(Button.builder(Component.empty(),
                            b -> enterColorPicker(scrollOffset + visibleRow))
                    .bounds(colorX, rowY, COLOR_SWATCH, COLOR_SWATCH)
                    .tooltip(Tooltip.create(Component.translatable("gui.iska_utils.labeling_machine.color")))
                    .build());

            EditBox box = new EditBox(font,
                    leftPos + SEGMENT_EDIT_X,
                    rowY,
                    SEGMENT_EDIT_W,
                    SEGMENT_EDIT_H,
                    Component.translatable("gui.iska_utils.labeling_machine.segment"));
            box.setMaxLength(LabelingNameStyle.MAX_SEGMENT_LENGTH);
            box.setResponder(value -> onSegmentTyped(visibleRow, value));
            segmentEdits[row] = addRenderableWidget(box);
        }

        hexEdit = new EditBox(font, leftPos + HEX_EDIT_X + SWATCH_SIZE + 4, topPos + HEX_EDIT_Y,
                HEX_EDIT_W, HEX_EDIT_H,
                Component.translatable("gui.iska_utils.labeling_machine.hex"));
        hexEdit.setMaxLength(7);
        hexEdit.setValue(LabelingNameStyle.toHexString(draftColorRgb));
        hexEdit.setResponder(this::onHexTyped);
        addRenderableWidget(hexEdit);

        int actionY = topPos + HEX_EDIT_Y + HEX_EDIT_H + 4;
        applyColorButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.iska_utils.labeling_machine.apply_color"),
                b -> leaveColorPicker(true))
                .bounds(leftPos + HEX_EDIT_X, actionY, 56, 16)
                .build());
        cancelColorButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.iska_utils.labeling_machine.cancel"),
                b -> leaveColorPicker(false))
                .bounds(leftPos + HEX_EDIT_X + 60, actionY, 56, 16)
                .build());

        applySubViewVisibility();
        ensureTrailingEmpty();
        refreshVisibleRows();
        refreshLoreLineButtons();
    }

    private void ensureLoreLinesCapacity() {
        int max = LabelingNameStyle.maxLoreLines();
        while (loreLines.size() < max) {
            List<LabelingNameStyle.Segment> line = new ArrayList<>();
            line.add(LabelingNameStyle.Segment.blankLore());
            loreLines.add(line);
        }
        while (loreLines.size() > max) {
            loreLines.remove(loreLines.size() - 1);
        }
        loreScrollOffset = Mth.clamp(loreScrollOffset, 0, Math.max(0, loreLines.size() - VISIBLE_LORE_BTNS));
    }

    private boolean isSegmentEditorView() {
        return subView == SubView.MAIN || subView == SubView.LORE_LINE;
    }

    private boolean isEditingLoreSegments() {
        return subView == SubView.LORE_LINE
                || (subView == SubView.COLOR_PICKER && colorPickerReturnView == SubView.LORE_LINE);
    }

    private LabelingNameStyle.Segment newBlankSegment() {
        return isEditingLoreSegments()
                ? LabelingNameStyle.Segment.blankLore()
                : LabelingNameStyle.Segment.blank();
    }

    private void handleCloseOrBack() {
        if (subView == SubView.COLOR_PICKER) {
            leaveColorPicker(false);
        } else if (subView == SubView.LORE_LINE) {
            leaveLoreLine();
        } else if (subView == SubView.LORE) {
            leaveLoreToMain();
        } else {
            onClose();
        }
    }

    private boolean handleSubMenuBackKey() {
        if (subView == SubView.COLOR_PICKER) {
            leaveColorPicker(false);
            return true;
        }
        if (subView == SubView.LORE_LINE) {
            leaveLoreLine();
            return true;
        }
        if (subView == SubView.LORE) {
            leaveLoreToMain();
            return true;
        }
        return false;
    }

    private void onLoreNavPressed() {
        if (subView == SubView.LORE_LINE) {
            leaveLoreLine();
        } else if (subView == SubView.LORE) {
            leaveLoreToMain();
        } else if (subView == SubView.MAIN) {
            enterLore();
        }
    }

    private void enterLore() {
        stashedNameSegments.clear();
        for (LabelingNameStyle.Segment s : segments) {
            stashedNameSegments.add(s.copy());
        }
        ensureLoreLinesCapacity();
        subView = SubView.LORE;
        menu.setColorPickerOpen(false);
        applySubViewVisibility();
        refreshLoreLineButtons();
    }

    private void leaveLoreToMain() {
        segments.clear();
        if (stashedNameSegments.isEmpty()) {
            segments.add(LabelingNameStyle.Segment.blank());
        } else {
            for (LabelingNameStyle.Segment s : stashedNameSegments) {
                segments.add(s.copy());
            }
        }
        selectedIndex = 0;
        scrollOffset = 0;
        subView = SubView.MAIN;
        applySubViewVisibility();
        ensureTrailingEmpty();
        refreshVisibleRows();
    }

    private void enterLoreLine(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= loreLines.size()) {
            return;
        }
        editingLoreLine = lineIndex;
        segments.clear();
        for (LabelingNameStyle.Segment s : loreLines.get(lineIndex)) {
            segments.add(s.copy());
        }
        if (segments.isEmpty()) {
            segments.add(LabelingNameStyle.Segment.blankLore());
        }
        selectedIndex = 0;
        scrollOffset = 0;
        subView = SubView.LORE_LINE;
        applySubViewVisibility();
        ensureTrailingEmpty();
        refreshVisibleRows();
    }

    private void leaveLoreLine() {
        saveCurrentLoreLineFromEditor();
        subView = SubView.LORE;
        applySubViewVisibility();
        refreshLoreLineButtons();
    }

    private void saveCurrentLoreLineFromEditor() {
        ensureLoreLinesCapacity();
        if (editingLoreLine < 0 || editingLoreLine >= loreLines.size()) {
            return;
        }
        List<LabelingNameStyle.Segment> saved = new ArrayList<>();
        for (LabelingNameStyle.Segment s : segments) {
            saved.add(s.copy());
        }
        loreLines.set(editingLoreLine, saved);
    }

    private LabelingNameStyle.Segment segmentAt(int index) {
        if (segments.isEmpty()) {
            segments.add(newBlankSegment());
        }
        return segments.get(Mth.clamp(index, 0, segments.size() - 1));
    }

    private void toggleStyle(int visibleRow, int styleIdx) {
        int index = scrollOffset + visibleRow;
        if (index < 0 || index >= segments.size()) {
            return;
        }
        selectedIndex = index;
        LabelingNameStyle.Segment seg = segments.get(index);
        switch (styleIdx) {
            case 0 -> seg.bold = !seg.bold;
            case 1 -> {
                if (!isEditingLoreSegments() && forceNameItalicLocked()) {
                    seg.italic = true;
                } else {
                    seg.italic = !seg.italic;
                }
            }
            case 2 -> seg.underline = !seg.underline;
            case 3 -> seg.strikethrough = !seg.strikethrough;
            case 4 -> seg.obfuscated = !seg.obfuscated;
            default -> {}
        }
        updateRowStyleMessages(visibleRow);
    }

    private boolean forceNameItalicLocked() {
        return minecraft != null && LabelingNameStyle.shouldForceItalicFor(minecraft.player);
    }

    private void enforceNameItalicIfLocked() {
        if (!forceNameItalicLocked()) {
            return;
        }
        LabelingNameStyle.forceItalicOnSegments(segments);
        LabelingNameStyle.forceItalicOnSegments(stashedNameSegments);
    }

    private void updateRowStyleMessages(int visibleRow) {
        int index = scrollOffset + visibleRow;
        if (index < 0 || index >= segments.size()) {
            return;
        }
        LabelingNameStyle.Segment seg = segments.get(index);
        boolean[] flags = {seg.bold, seg.italic, seg.underline, seg.strikethrough, seg.obfuscated};
        for (int i = 0; i < 5; i++) {
            Button b = styleButtons[visibleRow][i];
            if (b != null) {
                b.setMessage(Component.literal(STYLE_LABELS[i])
                        .withStyle(flags[i] ? ChatFormatting.GREEN : ChatFormatting.RED));
            }
        }
    }

    private void onSegmentTyped(int visibleRow, String value) {
        if (suppressSegmentResponder) {
            return;
        }
        int index = scrollOffset + visibleRow;
        if (index < 0 || index >= segments.size()) {
            return;
        }
        String clamped = LabelingNameStyle.clampSegmentText(value);
        int otherLen = 0;
        for (int i = 0; i < segments.size(); i++) {
            if (i == index) {
                continue;
            }
            otherLen += segments.get(i).text.length();
        }
        int maxForThis = Math.max(0, LabelingNameStyle.maxLineLength() - otherLen);
        if (clamped.length() > maxForThis) {
            clamped = clamped.substring(0, maxForThis);
        }
        segments.get(index).text = clamped;
        selectedIndex = index;
        EditBox box = segmentEdits[visibleRow];
        if (box != null && !box.getValue().equals(clamped)) {
            suppressSegmentResponder = true;
            box.setValue(clamped);
            suppressSegmentResponder = false;
        }
        int sizeBefore = segments.size();
        ensureTrailingEmpty();
        if (segments.size() != sizeBefore) {
            ensureSelectedVisible();
            refreshVisibleRows(visibleRow);
        }
    }

    private void ensureTrailingEmpty() {
        if (segments.isEmpty()) {
            segments.add(newBlankSegment());
            return;
        }
        while (segments.size() >= 2
                && segments.get(segments.size() - 1).text.isEmpty()
                && segments.get(segments.size() - 2).text.isEmpty()) {
            segments.remove(segments.size() - 1);
        }
        LabelingNameStyle.Segment last = segments.get(segments.size() - 1);
        if (!last.text.isEmpty() && segments.size() < LabelingNameStyle.MAX_SEGMENTS) {
            segments.add(newBlankSegment());
        }
        selectedIndex = Mth.clamp(selectedIndex, 0, segments.size() - 1);
        clampScroll();
    }

    private void refreshVisibleRows() {
        refreshVisibleRows(-1);
    }

    /**
     * @param skipFocusedVisibleRow if >= 0, do not reset that EditBox value (preserves cursor while typing)
     */
    private void refreshVisibleRows(int skipFocusedVisibleRow) {
        clampScroll();
        if (!isEditingLoreSegments()) {
            enforceNameItalicIfLocked();
        }
        suppressSegmentResponder = true;
        boolean editor = isSegmentEditorView();
        for (int row = 0; row < VISIBLE_SEGMENTS; row++) {
            EditBox box = segmentEdits[row];
            Button colorBtn = colorButtons[row];
            int index = scrollOffset + row;
            boolean has = index < segments.size();

            for (int s = 0; s < 5; s++) {
                Button sb = styleButtons[row][s];
                if (sb != null) {
                    sb.visible = editor && has;
                    sb.active = editor && has;
                }
            }
            if (colorBtn != null) {
                colorBtn.visible = editor && has;
                colorBtn.active = editor && has;
            }
            if (box != null) {
                box.visible = editor && has;
                box.setEditable(editor && has);
                if (has) {
                    if (row != skipFocusedVisibleRow) {
                        box.setValue(segments.get(index).text);
                    }
                    updateRowStyleMessages(row);
                }
            }
        }
        suppressSegmentResponder = false;
    }

    private void refreshLoreLineButtons() {
        ensureLoreLinesCapacity();
        clampLoreScroll();
        boolean lore = subView == SubView.LORE;
        int max = loreLines.size();
        for (int visibleSlot = 0; visibleSlot < VISIBLE_LORE_BTNS; visibleSlot++) {
            Button btn = loreLineButtons[visibleSlot];
            if (btn == null) {
                continue;
            }
            int lineIndex = loreScrollOffset + visibleSlot;
            boolean show = lore && lineIndex < max;
            btn.visible = show;
            btn.active = show;
            if (show) {
                int y = topPos + LIST_START_Y + visibleSlot * SEGMENT_ROW_H;
                btn.setX(leftPos + LORE_LINE_BTN_X);
                btn.setY(y);
                btn.setWidth(LORE_LINE_BTN_W);
                btn.setMessage(loreLineButtonLabel(lineIndex));
            }
        }
    }

    private Component loreLineButtonLabel(int lineIndex) {
        String preview = LabelingNameStyle.plainText(loreLines.get(lineIndex));
        int displayNum = lineIndex + 1;
        if (preview.isEmpty()) {
            return Component.translatable("gui.iska_utils.labeling_machine.edit_lore_line", displayNum);
        }
        String shortPreview = preview.length() > LORE_PREVIEW_CHARS
                ? preview.substring(0, LORE_PREVIEW_CHARS) + "..."
                : preview;
        return Component.translatable("gui.iska_utils.labeling_machine.edit_lore_line_preview",
                displayNum, shortPreview);
    }

    private void clampScroll() {
        int max = Math.max(0, segments.size() - VISIBLE_SEGMENTS);
        scrollOffset = Mth.clamp(scrollOffset, 0, max);
    }

    private void clampLoreScroll() {
        int max = Math.max(0, loreLines.size() - VISIBLE_LORE_BTNS);
        loreScrollOffset = Mth.clamp(loreScrollOffset, 0, max);
    }

    private boolean canScroll() {
        return segments.size() > VISIBLE_SEGMENTS;
    }

    private boolean canScrollLore() {
        return loreLines.size() > VISIBLE_LORE_BTNS;
    }

    private void scrollUp() {
        if (scrollOffset > 0) {
            scrollOffset--;
            refreshVisibleRows();
        }
    }

    private void scrollDown() {
        int max = Math.max(0, segments.size() - VISIBLE_SEGMENTS);
        if (scrollOffset < max) {
            scrollOffset++;
            refreshVisibleRows();
        }
    }

    private void scrollLoreUp() {
        if (loreScrollOffset > 0) {
            loreScrollOffset--;
            refreshLoreLineButtons();
        }
    }

    private void scrollLoreDown() {
        int max = Math.max(0, loreLines.size() - VISIBLE_LORE_BTNS);
        if (loreScrollOffset < max) {
            loreScrollOffset++;
            refreshLoreLineButtons();
        }
    }

    private void ensureSelectedVisible() {
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + VISIBLE_SEGMENTS) {
            scrollOffset = selectedIndex - VISIBLE_SEGMENTS + 1;
        }
        clampScroll();
    }

    private void selectSegment(int index) {
        if (index < 0 || index >= segments.size()) {
            return;
        }
        selectedIndex = index;
        ensureSelectedVisible();
        refreshVisibleRows();
    }

    private void enterColorPicker(int segmentIndex) {
        if (segmentIndex < 0 || segmentIndex >= segments.size()) {
            return;
        }
        selectedIndex = segmentIndex;
        setDraftColor(segments.get(segmentIndex).colorRgb);
        colorPickerReturnView = subView == SubView.LORE_LINE ? SubView.LORE_LINE : SubView.MAIN;
        subView = SubView.COLOR_PICKER;
        menu.setColorPickerOpen(true);
        colorDrag = ColorDrag.NONE;
        applySubViewVisibility();
    }

    private void leaveColorPicker(boolean apply) {
        if (apply) {
            Integer parsed = LabelingNameStyle.parseHexColor(hexEdit.getValue());
            if (parsed != null) {
                setDraftColor(parsed);
            }
            segmentAt(selectedIndex).colorRgb = draftColorRgb & 0xFFFFFF;
        }
        subView = colorPickerReturnView;
        menu.setColorPickerOpen(false);
        colorDrag = ColorDrag.NONE;
        applySubViewVisibility();
        if (isSegmentEditorView()) {
            refreshVisibleRows();
        }
    }

    private void setDraftColor(int rgb) {
        draftColorRgb = rgb & 0xFFFFFF;
        LabelingNameStyle.Hsv hsv = LabelingNameStyle.rgbToHsv(draftColorRgb);
        pickerHue = hsv.h;
        pickerSat = hsv.s;
        pickerVal = hsv.v;
        suppressHexResponder = true;
        if (hexEdit != null) {
            hexEdit.setValue(LabelingNameStyle.toHexString(draftColorRgb));
        }
        suppressHexResponder = false;
    }

    private void syncDraftFromHsv() {
        draftColorRgb = LabelingNameStyle.hsvToRgb(pickerHue, pickerSat, pickerVal);
        suppressHexResponder = true;
        if (hexEdit != null) {
            hexEdit.setValue(LabelingNameStyle.toHexString(draftColorRgb));
        }
        suppressHexResponder = false;
    }

    private void onHexTyped(String value) {
        if (suppressHexResponder) {
            return;
        }
        Integer parsed = LabelingNameStyle.parseHexColor(value);
        if (parsed != null) {
            draftColorRgb = parsed;
            LabelingNameStyle.Hsv hsv = LabelingNameStyle.rgbToHsv(draftColorRgb);
            pickerHue = hsv.h;
            pickerSat = hsv.s;
            pickerVal = hsv.v;
        }
    }

    private void applySubViewVisibility() {
        boolean main = subView == SubView.MAIN;
        boolean lore = subView == SubView.LORE;
        boolean loreLine = subView == SubView.LORE_LINE;
        boolean color = subView == SubView.COLOR_PICKER;
        boolean showGlobalActions = !color;

        applyButton.visible = showGlobalActions;
        applyButton.active = showGlobalActions && !menu.getTargetStack().isEmpty();
        copyButton.visible = showGlobalActions;
        copyButton.active = showGlobalActions && !menu.getTargetStack().isEmpty();
        resetButton.visible = showGlobalActions;
        resetButton.active = showGlobalActions;

        if (copyButton != null) {
            copyButton.setTooltip(Tooltip.create(Component.translatable(
                    "gui.iska_utils.labeling_machine.copy.tooltip")));
        }

        boolean showNav = main || lore || loreLine;
        loreNavButton.visible = showNav;
        if (main) {
            loreNavButton.setMessage(Component.translatable("gui.iska_utils.labeling_machine.lore"));
        } else {
            loreNavButton.setMessage(Component.translatable("gui.iska_utils.labeling_machine.back"));
        }
        loreNavButton.active = showNav;

        hexEdit.visible = color;
        hexEdit.setEditable(color);
        applyColorButton.visible = color;
        applyColorButton.active = color;
        cancelColorButton.visible = color;
        cancelColorButton.active = color;

        if (isSegmentEditorView()) {
            refreshVisibleRows();
        } else {
            for (int row = 0; row < VISIBLE_SEGMENTS; row++) {
                for (int s = 0; s < 5; s++) {
                    if (styleButtons[row][s] != null) {
                        styleButtons[row][s].visible = false;
                        styleButtons[row][s].active = false;
                    }
                }
                if (colorButtons[row] != null) {
                    colorButtons[row].visible = false;
                    colorButtons[row].active = false;
                }
                if (segmentEdits[row] != null) {
                    segmentEdits[row].visible = false;
                    segmentEdits[row].setEditable(false);
                }
            }
        }
        refreshLoreLineButtons();
    }

    private void onApplyPressed() {
        if (menu.getTargetStack().isEmpty()) {
            return;
        }
        if (subView == SubView.LORE_LINE) {
            saveCurrentLoreLineFromEditor();
        }
        applyNameFrom(nameSegmentsSource());
        applyLore();
    }

    private void onCopyPressed() {
        if (menu.getTargetStack().isEmpty()) {
            return;
        }
        loadNameFromTargetIntoEditors();
        copyLoreFromTargetStack();
        if (subView == SubView.LORE_LINE) {
            loadLoreLineIntoEditor(editingLoreLine);
        } else if (subView == SubView.MAIN) {
            selectedIndex = 0;
            scrollOffset = 0;
            ensureTrailingEmpty();
            refreshVisibleRows();
        } else if (subView == SubView.LORE) {
            refreshLoreLineButtons();
        }
    }

    private void onResetPressed() {
        resetNameInEditors();
        resetAllLoreLines();
        if (subView == SubView.LORE_LINE) {
            segments.clear();
            segments.add(LabelingNameStyle.Segment.blankLore());
            selectedIndex = 0;
            scrollOffset = 0;
            ensureTrailingEmpty();
            refreshVisibleRows();
        } else if (subView == SubView.MAIN) {
            selectedIndex = 0;
            scrollOffset = 0;
            ensureTrailingEmpty();
            refreshVisibleRows();
        } else if (subView == SubView.LORE) {
            loreScrollOffset = 0;
            refreshLoreLineButtons();
        }
    }

    /** Name buffer currently being edited, or the stashed name while in lore views. */
    private List<LabelingNameStyle.Segment> nameSegmentsSource() {
        if (subView == SubView.MAIN
                || (subView == SubView.COLOR_PICKER && colorPickerReturnView == SubView.MAIN)) {
            return segments;
        }
        return stashedNameSegments;
    }

    private void applyNameFrom(List<LabelingNameStyle.Segment> source) {
        List<LabelingNameStyle.Segment> payload = new ArrayList<>();
        if (source != null) {
            for (LabelingNameStyle.Segment s : source) {
                payload.add(s.copy());
            }
        }
        if (forceNameItalicLocked()) {
            LabelingNameStyle.forceItalicOnSegments(payload);
        }
        PacketDistributor.sendToServer(new LabelingMachineRenameC2SPacket(payload));
    }

    private void applyLore() {
        ensureLoreLinesCapacity();
        List<List<LabelingNameStyle.Segment>> payload = new ArrayList<>();
        for (List<LabelingNameStyle.Segment> line : loreLines) {
            List<LabelingNameStyle.Segment> copy = new ArrayList<>();
            for (LabelingNameStyle.Segment s : line) {
                copy.add(s.copy());
            }
            payload.add(copy);
        }
        PacketDistributor.sendToServer(new LabelingMachineLoreC2SPacket(payload));
    }

    private void loadNameFromTargetIntoEditors() {
        ItemStack target = menu.getTargetStack();
        List<LabelingNameStyle.Segment> loaded = new ArrayList<>();
        Component custom = target.get(DataComponents.CUSTOM_NAME);
        if (custom != null) {
            loaded.addAll(LabelingNameStyle.fromComponent(custom));
        } else {
            loaded.add(LabelingNameStyle.Segment.blank());
        }

        if (subView == SubView.MAIN
                || (subView == SubView.COLOR_PICKER && colorPickerReturnView == SubView.MAIN)) {
            segments.clear();
            for (LabelingNameStyle.Segment s : loaded) {
                segments.add(s.copy());
            }
        } else {
            stashedNameSegments.clear();
            for (LabelingNameStyle.Segment s : loaded) {
                stashedNameSegments.add(s.copy());
            }
        }
        enforceNameItalicIfLocked();
    }

    private void resetNameInEditors() {
        if (subView == SubView.MAIN
                || (subView == SubView.COLOR_PICKER && colorPickerReturnView == SubView.MAIN)) {
            segments.clear();
            segments.add(LabelingNameStyle.Segment.blank());
        }
        stashedNameSegments.clear();
        stashedNameSegments.add(LabelingNameStyle.Segment.blank());
    }

    private void resetAllLoreLines() {
        ensureLoreLinesCapacity();
        for (List<LabelingNameStyle.Segment> line : loreLines) {
            line.clear();
            line.add(LabelingNameStyle.Segment.blankLore());
        }
    }

    private void loadLoreLineIntoEditor(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= loreLines.size()) {
            return;
        }
        segments.clear();
        for (LabelingNameStyle.Segment s : loreLines.get(lineIndex)) {
            segments.add(s.copy());
        }
        if (segments.isEmpty()) {
            segments.add(LabelingNameStyle.Segment.blankLore());
        }
        selectedIndex = 0;
        scrollOffset = 0;
        ensureTrailingEmpty();
        refreshVisibleRows();
    }

    private void copyLoreFromTargetStack() {
        ItemStack target = menu.getTargetStack();
        if (target.isEmpty()) {
            return;
        }
        ensureLoreLinesCapacity();
        for (List<LabelingNameStyle.Segment> line : loreLines) {
            line.clear();
            line.add(LabelingNameStyle.Segment.blankLore());
        }
        ItemLore lore = target.get(DataComponents.LORE);
        if (lore != null) {
            List<Component> lines = lore.lines();
            int count = Math.min(lines.size(), loreLines.size());
            for (int i = 0; i < count; i++) {
                List<LabelingNameStyle.Segment> parsed = LabelingNameStyle.fromComponent(lines.get(i), true);
                loreLines.set(i, new ArrayList<>(parsed));
            }
        }
        loreScrollOffset = 0;
        refreshLoreLineButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (subView != SubView.COLOR_PICKER && applyButton != null && copyButton != null) {
            boolean has = !menu.getTargetStack().isEmpty();
            applyButton.active = has;
            copyButton.active = has;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (isSegmentEditorView()) {
            renderPreview(graphics);
            renderRowColorSwatches(graphics);
            renderScrollbar(graphics, mouseX, mouseY, scrollOffset,
                    Math.max(0, segments.size() - VISIBLE_SEGMENTS), isDraggingHandle);
        } else if (subView == SubView.COLOR_PICKER) {
            renderColorPicker(graphics);
            graphics.fill(leftPos + 8, topPos + INVENTORY_Y - 4, leftPos + imageWidth - 8, topPos + imageHeight - 6, PANEL_COVER_COLOR);
        } else if (subView == SubView.LORE) {
            renderScrollbar(graphics, mouseX, mouseY, loreScrollOffset,
                    Math.max(0, loreLines.size() - VISIBLE_LORE_BTNS), isDraggingLoreHandle);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderPreview(GuiGraphics graphics) {
        List<LabelingNameStyle.Segment> pieces = new ArrayList<>();
        for (LabelingNameStyle.Segment seg : segments) {
            if (seg == null || seg.text == null || seg.text.isEmpty()) {
                continue;
            }
            pieces.add(seg);
        }
        if (pieces.isEmpty()) {
            return;
        }

        int gapWidth = Math.max(1, font.width(" ") / 2);
        int budget = LabelingNameStyle.maxLineLength();
        int used = 0;
        boolean truncated = false;
        List<Component> drawPieces = new ArrayList<>();
        List<Boolean> drawLight = new ArrayList<>();
        List<Boolean> gapBefore = new ArrayList<>();

        Boolean prevLight = null;
        for (LabelingNameStyle.Segment seg : pieces) {
            if (used >= budget) {
                truncated = true;
                break;
            }
            String text = seg.text;
            int remain = budget - used;
            if (text.length() > remain) {
                text = text.substring(0, remain);
                truncated = true;
            }
            if (text.isEmpty()) {
                break;
            }
            boolean light = isLightPreviewColor(seg.colorRgb);
            boolean needGap = prevLight != null && prevLight != light;
            drawPieces.add(Component.literal(text).withStyle(seg.toStyle()));
            drawLight.add(light);
            gapBefore.add(needGap);
            used += text.length();
            prevLight = light;
            if (truncated) {
                break;
            }
        }
        if (drawPieces.isEmpty()) {
            return;
        }
        if (truncated) {
            drawPieces.add(Component.literal("...").withStyle(Style.EMPTY.withColor(0xFFFFFF)));
            drawLight.add(false);
            gapBefore.add(false);
        }

        int totalWidth = 0;
        for (int i = 0; i < drawPieces.size(); i++) {
            if (Boolean.TRUE.equals(gapBefore.get(i))) {
                totalWidth += gapWidth;
            }
            totalWidth += font.width(drawPieces.get(i));
        }

        int x = leftPos + (imageWidth - totalWidth) / 2;
        int py = topPos + PREVIEW_Y;
        int vPad = 3;
        for (int i = 0; i < drawPieces.size(); i++) {
            if (Boolean.TRUE.equals(gapBefore.get(i))) {
                x += gapWidth;
            }
            Component piece = drawPieces.get(i);
            int w = font.width(piece);
            if (Boolean.TRUE.equals(drawLight.get(i))) {
                graphics.fill(x - 1, py - vPad, x + w + 1, py + font.lineHeight + 2, PREVIEW_BG);
            }
            graphics.drawString(font, piece, x, py, 0xFFFFFFFF, false);
            x += w;
        }
    }

    private static boolean isLightPreviewColor(int colorRgb) {
        int rgb = colorRgb & 0xFFFFFF;
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        return 0.2126f * r + 0.7152f * g + 0.0722f * b >= 0.55f;
    }

    private void renderRowColorSwatches(GuiGraphics graphics) {
        for (int row = 0; row < VISIBLE_SEGMENTS; row++) {
            int index = scrollOffset + row;
            if (index >= segments.size() || colorButtons[row] == null || !colorButtons[row].visible) {
                continue;
            }
            int rgb = segments.get(index).colorRgb & 0xFFFFFF;
            int x = colorButtons[row].getX();
            int y = colorButtons[row].getY();
            graphics.fill(x + 1, y + 1, x + COLOR_SWATCH - 1, y + COLOR_SWATCH - 1, 0xFF000000 | rgb);
            graphics.fill(x, y, x + COLOR_SWATCH, y + 1, 0xFF000000);
            graphics.fill(x, y + COLOR_SWATCH - 1, x + COLOR_SWATCH, y + COLOR_SWATCH, 0xFF000000);
            graphics.fill(x, y, x + 1, y + COLOR_SWATCH, 0xFF000000);
            graphics.fill(x + COLOR_SWATCH - 1, y, x + COLOR_SWATCH, y + COLOR_SWATCH, 0xFF000000);
        }
    }

    private void renderSegmentSelection(GuiGraphics graphics) {
        for (int row = 0; row < VISIBLE_SEGMENTS; row++) {
            int index = scrollOffset + row;
            if (index >= segments.size() || index != selectedIndex) {
                continue;
            }
            int y = topPos + LIST_START_Y + row * SEGMENT_ROW_H - 1;
            graphics.fill(leftPos + ROW_CONTROLS_X - 2, y,
                    leftPos + SEGMENT_EDIT_X + SEGMENT_EDIT_W + 2, y + SEGMENT_EDIT_H + 2, SELECTION_HIGHLIGHT);
        }
    }

    private void renderScrollbar(GuiGraphics graphics, int mouseX, int mouseY, int offset, int max, boolean dragging) {
        renderScrollChrome(graphics, mouseX, mouseY,
                leftPos + SCROLLBAR_X, topPos + SCROLLBAR_Y, topPos + BUTTON_UP_Y, topPos + BUTTON_DOWN_Y,
                offset, max, dragging);
    }

    private void renderScrollChrome(GuiGraphics graphics, int mouseX, int mouseY,
                                    int scrollbarX, int scrollbarY, int buttonUpY, int buttonDownY,
                                    int offset, int max, boolean dragging) {
        graphics.blit(SCROLLBAR_TEXTURE, scrollbarX, scrollbarY, 0, 0,
                SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT, 32, 34);

        boolean upHovered = mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE
                && mouseY >= buttonUpY && mouseY < buttonUpY + HANDLE_SIZE;
        int upTextureY = upHovered ? HANDLE_SIZE : 0;
        graphics.blit(SCROLLBAR_TEXTURE, scrollbarX, buttonUpY,
                SCROLLBAR_WIDTH * 2, upTextureY, HANDLE_SIZE, HANDLE_SIZE, 32, 34);

        boolean downHovered = mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE
                && mouseY >= buttonDownY && mouseY < buttonDownY + HANDLE_SIZE;
        int downTextureY = downHovered ? HANDLE_SIZE : 0;
        graphics.blit(SCROLLBAR_TEXTURE, scrollbarX, buttonDownY,
                SCROLLBAR_WIDTH * 3, downTextureY, HANDLE_SIZE, HANDLE_SIZE, 32, 34);

        float scrollRatio = max > 0 ? (float) offset / max : 0f;
        int handleY = scrollbarY + (int) (scrollRatio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
        boolean handleHovered = mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE
                && mouseY >= handleY && mouseY < handleY + HANDLE_SIZE;
        int handleTextureY = (handleHovered || dragging) ? HANDLE_SIZE : 0;
        graphics.blit(SCROLLBAR_TEXTURE, scrollbarX, handleY,
                SCROLLBAR_WIDTH, handleTextureY, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
    }

    private void renderColorPicker(GuiGraphics graphics) {
        int svLeft = leftPos + SV_X;
        int svTop = topPos + SV_Y;
        for (int y = 0; y < SV_SIZE; y++) {
            float v = 1f - (y / (float) (SV_SIZE - 1));
            for (int x = 0; x < SV_SIZE; x++) {
                float s = x / (float) (SV_SIZE - 1);
                int rgb = LabelingNameStyle.hsvToRgb(pickerHue, s, v);
                graphics.fill(svLeft + x, svTop + y, svLeft + x + 1, svTop + y + 1, 0xFF000000 | rgb);
            }
        }
        drawRectBorder(graphics, svLeft, svTop, SV_SIZE, SV_SIZE);

        int cursorX = svLeft + Math.round(pickerSat * (SV_SIZE - 1));
        int cursorY = svTop + Math.round((1f - pickerVal) * (SV_SIZE - 1));
        graphics.fill(cursorX - 2, cursorY - 2, cursorX + 3, cursorY + 3, 0xFFFFFFFF);
        graphics.fill(cursorX - 1, cursorY - 1, cursorX + 2, cursorY + 2, 0xFF000000);

        int hueLeft = leftPos + HUE_BAR_X;
        int hueTop = topPos + HUE_BAR_Y;
        for (int y = 0; y < SV_SIZE; y++) {
            float h = (y / (float) (SV_SIZE - 1)) * 360f;
            int rgb = LabelingNameStyle.hsvToRgb(h, 1f, 1f);
            graphics.fill(hueLeft, hueTop + y, hueLeft + HUE_BAR_W, hueTop + y + 1, 0xFF000000 | rgb);
        }
        drawRectBorder(graphics, hueLeft, hueTop, HUE_BAR_W, SV_SIZE);

        int hueCursorY = hueTop + Math.round((pickerHue / 360f) * (SV_SIZE - 1));
        graphics.fill(hueLeft - 2, hueCursorY - 1, hueLeft + HUE_BAR_W + 2, hueCursorY + 2, 0xFFFFFFFF);
        graphics.fill(hueLeft - 1, hueCursorY, hueLeft + HUE_BAR_W + 1, hueCursorY + 1, 0xFF000000);

        int swatchX = leftPos + HEX_EDIT_X;
        int swatchY = topPos + HEX_EDIT_Y;
        graphics.fill(swatchX, swatchY, swatchX + SWATCH_SIZE, swatchY + SWATCH_SIZE, 0xFF000000 | draftColorRgb);
        drawRectBorder(graphics, swatchX, swatchY, SWATCH_SIZE, SWATCH_SIZE);

        ChatFormatting[] palette = LabelingNameStyle.VANILLA_PALETTE;
        for (int i = 0; i < palette.length; i++) {
            Integer rgbObj = palette[i].getColor();
            int rgb = rgbObj == null ? 0xFFFFFF : rgbObj;
            int col = i % PALETTE_COLS;
            int row = i / PALETTE_COLS;
            int x = leftPos + SV_X + col * (PALETTE_SIZE + PALETTE_GAP);
            int y = topPos + PALETTE_START_Y + row * (PALETTE_SIZE + PALETTE_GAP);
            graphics.fill(x, y, x + PALETTE_SIZE, y + PALETTE_SIZE, 0xFF000000 | (rgb & 0xFFFFFF));
            drawRectBorder(graphics, x, y, PALETTE_SIZE, PALETTE_SIZE);
        }
    }

    private static void drawRectBorder(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x - 1, y - 1, x + w + 1, y, 0xFF000000);
        graphics.fill(x - 1, y + h, x + w + 1, y + h + 1, 0xFF000000);
        graphics.fill(x - 1, y - 1, x, y + h + 1, 0xFF000000);
        graphics.fill(x + w, y - 1, x + w + 1, y + h + 1, 0xFF000000);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, GUI_WIDTH, GUI_HEIGHT);
        if (subView == SubView.COLOR_PICKER) {
            graphics.fill(leftPos + 8, topPos + INVENTORY_Y - 4, leftPos + imageWidth - 8, topPos + imageHeight - 6, PANEL_COVER_COLOR);
        } else {
            graphics.blit(SINGLE_SLOT_TEXTURE,
                    leftPos + LabelingMachineMenu.TARGET_SLOT_X - 1,
                    topPos + LabelingMachineMenu.TARGET_SLOT_Y - 1,
                    0, 0, 18, 18, 18, 18);
            if (isSegmentEditorView()) {
                renderSegmentSelection(graphics);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        Component title = Component.translatable("gui.iska_utils.labeling_machine.title");
        int titleX = (imageWidth - font.width(title)) / 2;
        graphics.drawString(font, title, titleX, 9, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (subView == SubView.COLOR_PICKER && button == 0) {
            if (isInSv(mouseX, mouseY)) {
                colorDrag = ColorDrag.SV;
                updateSvFromMouse(mouseX, mouseY);
                return true;
            }
            if (isInHue(mouseX, mouseY)) {
                colorDrag = ColorDrag.HUE;
                updateHueFromMouse(mouseY);
                return true;
            }
            int paletteIndex = paletteIndexAt(mouseX, mouseY);
            if (paletteIndex >= 0) {
                Integer rgbObj = LabelingNameStyle.VANILLA_PALETTE[paletteIndex].getColor();
                setDraftColor(rgbObj == null ? 0xFFFFFF : rgbObj);
                return true;
            }
        }

        if (isSegmentEditorView() && button == 0) {
            if (handleSegmentScrollClick(mouseX, mouseY)) {
                return true;
            }
            for (int row = 0; row < VISIBLE_SEGMENTS; row++) {
                int index = scrollOffset + row;
                if (index >= segments.size()) {
                    continue;
                }
                int y = topPos + LIST_START_Y + row * SEGMENT_ROW_H;
                if (mouseX >= leftPos + SEGMENT_EDIT_X && mouseX < leftPos + SEGMENT_EDIT_X + SEGMENT_EDIT_W
                        && mouseY >= y && mouseY < y + SEGMENT_EDIT_H) {
                    selectSegment(index);
                    break;
                }
            }
        }

        if (subView == SubView.LORE && button == 0 && canScrollLore()) {
            if (handleLoreScrollClick(mouseX, mouseY)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleSegmentScrollClick(double mouseX, double mouseY) {
        int scrollbarX = leftPos + SCROLLBAR_X;
        int buttonUpY = topPos + BUTTON_UP_Y;
        int buttonDownY = topPos + BUTTON_DOWN_Y;
        int scrollbarY = topPos + SCROLLBAR_Y;

        if (mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE
                && mouseY >= buttonUpY && mouseY < buttonUpY + HANDLE_SIZE) {
            scrollUp();
            return true;
        }
        if (mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE
                && mouseY >= buttonDownY && mouseY < buttonDownY + HANDLE_SIZE) {
            scrollDown();
            return true;
        }

        float scrollRatio = 0f;
        int max = Math.max(0, segments.size() - VISIBLE_SEGMENTS);
        if (max > 0) {
            scrollRatio = (float) scrollOffset / max;
        }
        int handleY = scrollbarY + (int) (scrollRatio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
        if (canScroll() && mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE
                && mouseY >= handleY && mouseY < handleY + HANDLE_SIZE) {
            isDraggingHandle = true;
            dragStartY = (int) mouseY;
            dragStartScrollOffset = scrollOffset;
            return true;
        }
        return false;
    }

    private boolean handleLoreScrollClick(double mouseX, double mouseY) {
        int scrollbarX = leftPos + SCROLLBAR_X;
        int buttonUpY = topPos + BUTTON_UP_Y;
        int buttonDownY = topPos + BUTTON_DOWN_Y;
        int scrollbarY = topPos + SCROLLBAR_Y;

        if (mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE
                && mouseY >= buttonUpY && mouseY < buttonUpY + HANDLE_SIZE) {
            scrollLoreUp();
            return true;
        }
        if (mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE
                && mouseY >= buttonDownY && mouseY < buttonDownY + HANDLE_SIZE) {
            scrollLoreDown();
            return true;
        }

        int max = Math.max(0, loreLines.size() - VISIBLE_LORE_BTNS);
        float scrollRatio = max > 0 ? (float) loreScrollOffset / max : 0f;
        int handleY = scrollbarY + (int) (scrollRatio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
        if (canScrollLore() && mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE
                && mouseY >= handleY && mouseY < handleY + HANDLE_SIZE) {
            isDraggingLoreHandle = true;
            dragStartY = (int) mouseY;
            dragStartScrollOffset = loreScrollOffset;
            return true;
        }
        return false;
    }

    private int paletteIndexAt(double mouseX, double mouseY) {
        for (int i = 0; i < LabelingNameStyle.VANILLA_PALETTE.length; i++) {
            int col = i % PALETTE_COLS;
            int row = i / PALETTE_COLS;
            int x = leftPos + SV_X + col * (PALETTE_SIZE + PALETTE_GAP);
            int y = topPos + PALETTE_START_Y + row * (PALETTE_SIZE + PALETTE_GAP);
            if (mouseX >= x && mouseX < x + PALETTE_SIZE && mouseY >= y && mouseY < y + PALETTE_SIZE) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (subView == SubView.COLOR_PICKER && button == 0) {
            if (colorDrag == ColorDrag.SV) {
                updateSvFromMouse(mouseX, mouseY);
                return true;
            }
            if (colorDrag == ColorDrag.HUE) {
                updateHueFromMouse(mouseY);
                return true;
            }
        }
        if (isSegmentEditorView() && isDraggingHandle && canScroll()) {
            int max = segments.size() - VISIBLE_SEGMENTS;
            int track = SCROLLBAR_HEIGHT - HANDLE_SIZE;
            int delta = (int) mouseY - dragStartY;
            int newOffset = dragStartScrollOffset + Math.round((delta / (float) track) * max);
            scrollOffset = Mth.clamp(newOffset, 0, max);
            refreshVisibleRows();
            return true;
        }
        if (subView == SubView.LORE && isDraggingLoreHandle && canScrollLore()) {
            int max = loreLines.size() - VISIBLE_LORE_BTNS;
            int track = SCROLLBAR_HEIGHT - HANDLE_SIZE;
            int delta = (int) mouseY - dragStartY;
            int newOffset = dragStartScrollOffset + Math.round((delta / (float) track) * max);
            loreScrollOffset = Mth.clamp(newOffset, 0, max);
            refreshLoreLineButtons();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDraggingHandle = false;
            isDraggingLoreHandle = false;
            colorDrag = ColorDrag.NONE;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isSegmentEditorView() && canScroll()) {
            if (scrollY > 0) {
                scrollUp();
                return true;
            }
            if (scrollY < 0) {
                scrollDown();
                return true;
            }
        }
        if (subView == SubView.LORE && canScrollLore()) {
            if (scrollY > 0) {
                scrollLoreUp();
                return true;
            }
            if (scrollY < 0) {
                scrollLoreDown();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean isInSv(double mouseX, double mouseY) {
        int left = leftPos + SV_X;
        int top = topPos + SV_Y;
        return mouseX >= left && mouseX < left + SV_SIZE && mouseY >= top && mouseY < top + SV_SIZE;
    }

    private boolean isInHue(double mouseX, double mouseY) {
        int left = leftPos + HUE_BAR_X;
        int top = topPos + HUE_BAR_Y;
        return mouseX >= left && mouseX < left + HUE_BAR_W && mouseY >= top && mouseY < top + SV_SIZE;
    }

    private void updateSvFromMouse(double mouseX, double mouseY) {
        float s = (float) ((mouseX - (leftPos + SV_X)) / (SV_SIZE - 1));
        float v = 1f - (float) ((mouseY - (topPos + SV_Y)) / (SV_SIZE - 1));
        pickerSat = Mth.clamp(s, 0f, 1f);
        pickerVal = Mth.clamp(v, 0f, 1f);
        syncDraftFromHsv();
    }

    private void updateHueFromMouse(double mouseY) {
        float h = (float) ((mouseY - (topPos + HUE_BAR_Y)) / (SV_SIZE - 1)) * 360f;
        pickerHue = Mth.clamp(h, 0f, 360f);
        syncDraftFromHsv();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean editFocused = hexEdit != null && hexEdit.isFocused();
        for (EditBox box : segmentEdits) {
            if (box != null && box.isFocused()) {
                editFocused = true;
                break;
            }
        }

        boolean inventoryClose = this.minecraft != null
                && this.minecraft.options.keyInventory.matches(keyCode, scanCode);
        boolean escape = keyCode == GLFW.GLFW_KEY_ESCAPE;

        if ((escape || inventoryClose) && subView != SubView.MAIN) {
            if (editFocused && inventoryClose && !escape) {
                return true;
            }
            return handleSubMenuBackKey();
        }

        if (editFocused && inventoryClose) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
