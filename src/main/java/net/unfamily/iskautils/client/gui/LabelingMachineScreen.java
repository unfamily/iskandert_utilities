package net.unfamily.iskautils.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.network.packet.LabelingMachineRenameC2SPacket;
import net.unfamily.iskautils.util.LabelingNameStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Labeling Machine GUI: MAIN rename controls + nested COLOR_PICKER sub-view (same Screen).
 */
public class LabelingMachineScreen extends AbstractContainerScreen<LabelingMachineMenu> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/backgrounds/shop.png");
    private static final Identifier SINGLE_SLOT_TEXTURE = Identifier.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/single_slot.png");

    private static final int GUI_WIDTH = 300;
    private static final int GUI_HEIGHT = 240;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;
    private static final int INVENTORY_Y = LabelingMachineMenu.INVENTORY_Y;

    private static final int RENAME_BUTTON_X = 44;
    private static final int RENAME_BUTTON_Y = LabelingMachineMenu.TARGET_SLOT_Y;
    private static final int RENAME_BUTTON_W = 72;
    private static final int RENAME_BUTTON_H = 18;

    private static final int PREVIEW_Y = 52;
    private static final int NAME_EDIT_X = 20;
    private static final int NAME_EDIT_Y = 68;
    private static final int NAME_EDIT_W = 260;
    private static final int NAME_EDIT_H = 16;

    private static final int STYLE_Y = 90;
    private static final int STYLE_SIZE = 18;
    private static final int STYLE_GAP = 3;
    private static final int COLOR_BUTTON_W = 50;

    private static final int HEX_EDIT_Y = 28;
    private static final int HEX_EDIT_W = 80;
    private static final int SWATCH_SIZE = 18;
    private static final int PALETTE_START_Y = 70;
    private static final int PALETTE_COLS = 8;
    private static final int PALETTE_SIZE = 18;
    private static final int PALETTE_GAP = 4;

    private enum SubView { MAIN, COLOR_PICKER }

    private SubView subView = SubView.MAIN;

    private Button closeButton;
    private Button renameButton;
    private Button colorButton;
    private Button[] styleButtons = new Button[5];
    private EditBox nameEdit;

    private EditBox hexEdit;
    private Button applyColorButton;
    private Button cancelColorButton;
    private final List<Button> paletteButtons = new ArrayList<>();

    private boolean bold;
    private boolean italic;
    private boolean underline;
    private boolean strikethrough;
    private boolean obfuscated;
    private int colorRgb = LabelingNameStyle.DEFAULT_COLOR_RGB;

    private ItemStack lastSyncedTarget = ItemStack.EMPTY;
    private boolean suppressNameResponder;

    public LabelingMachineScreen(LabelingMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GUI_WIDTH, GUI_HEIGHT);
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        clearWidgets();
        paletteButtons.clear();

        closeButton = addRenderableWidget(Button.builder(Component.literal("\u2715"), b -> {
            if (subView == SubView.COLOR_PICKER) {
                leaveColorPicker(false);
            } else {
                onClose();
            }
        }).bounds(leftPos + CLOSE_BUTTON_X, topPos + CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE).build());

        renameButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.iska_utils.labeling_machine.rename"),
                b -> applyRename())
                .bounds(leftPos + RENAME_BUTTON_X, topPos + RENAME_BUTTON_Y, RENAME_BUTTON_W, RENAME_BUTTON_H)
                .build());

        nameEdit = new EditBox(font, leftPos + NAME_EDIT_X, topPos + NAME_EDIT_Y, NAME_EDIT_W, NAME_EDIT_H,
                Component.translatable("gui.iska_utils.labeling_machine.name"));
        nameEdit.setMaxLength(LabelingNameStyle.MAX_NAME_LENGTH);
        nameEdit.setResponder(s -> {});
        addRenderableWidget(nameEdit);

        String[] styleLabels = {"B", "I", "U", "S", "O"};
        String[] styleTips = {
                "gui.iska_utils.labeling_machine.style.bold",
                "gui.iska_utils.labeling_machine.style.italic",
                "gui.iska_utils.labeling_machine.style.underline",
                "gui.iska_utils.labeling_machine.style.strikethrough",
                "gui.iska_utils.labeling_machine.style.obfuscated"
        };
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            int x = leftPos + NAME_EDIT_X + i * (STYLE_SIZE + STYLE_GAP);
            styleButtons[i] = addRenderableWidget(Button.builder(Component.literal(styleLabels[i]), b -> toggleStyle(idx))
                    .bounds(x, topPos + STYLE_Y, STYLE_SIZE, STYLE_SIZE)
                    .tooltip(Tooltip.create(Component.translatable(styleTips[i])))
                    .build());
        }

        int colorX = leftPos + NAME_EDIT_X + 5 * (STYLE_SIZE + STYLE_GAP) + 4;
        colorButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.iska_utils.labeling_machine.color"),
                b -> enterColorPicker())
                .bounds(colorX, topPos + STYLE_Y, COLOR_BUTTON_W, STYLE_SIZE)
                .build());

        hexEdit = new EditBox(font, leftPos + NAME_EDIT_X + SWATCH_SIZE + 6, topPos + HEX_EDIT_Y, HEX_EDIT_W, NAME_EDIT_H,
                Component.translatable("gui.iska_utils.labeling_machine.hex"));
        hexEdit.setMaxLength(7);
        hexEdit.setValue(LabelingNameStyle.toHexString(colorRgb));
        hexEdit.setResponder(this::onHexTyped);
        addRenderableWidget(hexEdit);

        applyColorButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.iska_utils.labeling_machine.apply_color"),
                b -> leaveColorPicker(true))
                .bounds(leftPos + NAME_EDIT_X + SWATCH_SIZE + 6 + HEX_EDIT_W + 6, topPos + HEX_EDIT_Y, 60, NAME_EDIT_H)
                .build());

        cancelColorButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.iska_utils.labeling_machine.cancel"),
                b -> leaveColorPicker(false))
                .bounds(leftPos + NAME_EDIT_X + SWATCH_SIZE + 6 + HEX_EDIT_W + 70, topPos + HEX_EDIT_Y, 60, NAME_EDIT_H)
                .build());

        ChatFormatting[] palette = LabelingNameStyle.VANILLA_PALETTE;
        for (int i = 0; i < palette.length; i++) {
            ChatFormatting fmt = palette[i];
            Integer rgbObj = fmt.getColor();
            int rgb = rgbObj == null ? 0xFFFFFF : rgbObj;
            int col = i % PALETTE_COLS;
            int row = i / PALETTE_COLS;
            int x = leftPos + NAME_EDIT_X + col * (PALETTE_SIZE + PALETTE_GAP);
            int y = topPos + PALETTE_START_Y + row * (PALETTE_SIZE + PALETTE_GAP);
            Button pb = addRenderableWidget(Button.builder(Component.empty(), b -> {
                colorRgb = rgb;
                suppressNameResponder = true;
                hexEdit.setValue(LabelingNameStyle.toHexString(colorRgb));
                suppressNameResponder = false;
            }).bounds(x, y, PALETTE_SIZE, PALETTE_SIZE).build());
            paletteButtons.add(pb);
        }

        applySubViewVisibility();
        syncFromTargetStack(true);
        updateStyleButtonMessages();
    }

    private void toggleStyle(int idx) {
        switch (idx) {
            case 0 -> bold = !bold;
            case 1 -> italic = !italic;
            case 2 -> underline = !underline;
            case 3 -> strikethrough = !strikethrough;
            case 4 -> obfuscated = !obfuscated;
            default -> {}
        }
        updateStyleButtonMessages();
    }

    private void updateStyleButtonMessages() {
        boolean[] flags = {bold, italic, underline, strikethrough, obfuscated};
        String[] labels = {"B", "I", "U", "S", "O"};
        for (int i = 0; i < 5; i++) {
            if (styleButtons[i] != null) {
                styleButtons[i].setMessage(Component.literal(labels[i])
                        .withStyle(flags[i] ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            }
        }
    }

    private void enterColorPicker() {
        subView = SubView.COLOR_PICKER;
        menu.setColorPickerOpen(true);
        suppressNameResponder = true;
        hexEdit.setValue(LabelingNameStyle.toHexString(colorRgb));
        suppressNameResponder = false;
        applySubViewVisibility();
    }

    private void leaveColorPicker(boolean applyHex) {
        if (applyHex) {
            Integer parsed = LabelingNameStyle.parseHexColor(hexEdit.getValue());
            if (parsed != null) {
                colorRgb = parsed;
            }
        }
        subView = SubView.MAIN;
        menu.setColorPickerOpen(false);
        applySubViewVisibility();
    }

    private void onHexTyped(String value) {
        if (suppressNameResponder) {
            return;
        }
        Integer parsed = LabelingNameStyle.parseHexColor(value);
        if (parsed != null) {
            colorRgb = parsed;
        }
    }

    private void applySubViewVisibility() {
        boolean main = subView == SubView.MAIN;
        renameButton.visible = main;
        renameButton.active = main;
        nameEdit.visible = main;
        nameEdit.setEditable(main);
        colorButton.visible = main;
        colorButton.active = main;
        for (Button b : styleButtons) {
            if (b != null) {
                b.visible = main;
                b.active = main;
            }
        }

        boolean color = subView == SubView.COLOR_PICKER;
        hexEdit.visible = color;
        hexEdit.setEditable(color);
        applyColorButton.visible = color;
        applyColorButton.active = color;
        cancelColorButton.visible = color;
        cancelColorButton.active = color;
        for (Button b : paletteButtons) {
            b.visible = color;
            b.active = color;
        }
    }

    private void applyRename() {
        if (menu.getTargetStack().isEmpty()) {
            return;
        }
        ClientPacketDistributor.sendToServer(new LabelingMachineRenameC2SPacket(
                nameEdit.getValue(),
                bold, italic, underline, strikethrough, obfuscated,
                colorRgb));
    }

    private void syncFromTargetStack(boolean force) {
        ItemStack target = menu.getTargetStack();
        if (!force && ItemStack.isSameItemSameComponents(target, lastSyncedTarget)) {
            return;
        }
        lastSyncedTarget = target.copy();
        if (target.isEmpty()) {
            if (!nameEdit.isFocused()) {
                suppressNameResponder = true;
                nameEdit.setValue("");
                suppressNameResponder = false;
            }
            return;
        }
        Component custom = target.get(DataComponents.CUSTOM_NAME);
        if (custom != null && !nameEdit.isFocused()) {
            suppressNameResponder = true;
            nameEdit.setValue(custom.getString());
            suppressNameResponder = false;
            Style style = custom.getStyle();
            bold = Boolean.TRUE.equals(style.isBold());
            italic = Boolean.TRUE.equals(style.isItalic());
            underline = Boolean.TRUE.equals(style.isUnderlined());
            strikethrough = Boolean.TRUE.equals(style.isStrikethrough());
            obfuscated = Boolean.TRUE.equals(style.isObfuscated());
            TextColor tc = style.getColor();
            colorRgb = tc != null ? tc.getValue() : LabelingNameStyle.DEFAULT_COLOR_RGB;
            updateStyleButtonMessages();
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        syncFromTargetStack(false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0.0F, 0.0F,
                imageWidth, imageHeight, GUI_WIDTH, GUI_HEIGHT);
        if (subView == SubView.MAIN) {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SINGLE_SLOT_TEXTURE,
                    leftPos + LabelingMachineMenu.TARGET_SLOT_X - 1,
                    topPos + LabelingMachineMenu.TARGET_SLOT_Y - 1,
                    0.0F, 0.0F, 18, 18, 18, 18);
        }
        if (subView == SubView.COLOR_PICKER) {
            renderColorExtras(guiGraphics);
        }
    }

    private void renderColorExtras(GuiGraphicsExtractor guiGraphics) {
        int swatchX = leftPos + NAME_EDIT_X;
        int swatchY = topPos + HEX_EDIT_Y;
        guiGraphics.fill(swatchX, swatchY, swatchX + SWATCH_SIZE, swatchY + SWATCH_SIZE, 0xFF000000 | (colorRgb & 0xFFFFFF));
        guiGraphics.fill(swatchX, swatchY, swatchX + SWATCH_SIZE, swatchY + 1, 0xFFFFFFFF);
        guiGraphics.fill(swatchX, swatchY + SWATCH_SIZE - 1, swatchX + SWATCH_SIZE, swatchY + SWATCH_SIZE, 0xFFFFFFFF);
        guiGraphics.fill(swatchX, swatchY, swatchX + 1, swatchY + SWATCH_SIZE, 0xFFFFFFFF);
        guiGraphics.fill(swatchX + SWATCH_SIZE - 1, swatchY, swatchX + SWATCH_SIZE, swatchY + SWATCH_SIZE, 0xFFFFFFFF);

        ChatFormatting[] palette = LabelingNameStyle.VANILLA_PALETTE;
        for (int i = 0; i < palette.length; i++) {
            Integer rgbObj = palette[i].getColor();
            int rgb = rgbObj == null ? 0xFFFFFF : rgbObj;
            int col = i % PALETTE_COLS;
            int row = i / PALETTE_COLS;
            int x = leftPos + NAME_EDIT_X + col * (PALETTE_SIZE + PALETTE_GAP);
            int y = topPos + PALETTE_START_Y + row * (PALETTE_SIZE + PALETTE_GAP);
            guiGraphics.fill(x + 1, y + 1, x + PALETTE_SIZE - 1, y + PALETTE_SIZE - 1, 0xFF000000 | (rgb & 0xFFFFFF));
        }

        guiGraphics.fill(leftPos + 8, topPos + INVENTORY_Y - 4, leftPos + imageWidth - 8, topPos + imageHeight - 6, 0xC0101010);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        Component title = Component.translatable("gui.iska_utils.labeling_machine.title");
        int titleX = (imageWidth - font.width(title)) / 2;
        guiGraphics.text(font, title, titleX, 9, GuiTextColors.TITLE, false);

        if (subView == SubView.MAIN) {
            Component preview = LabelingNameStyle.preview(
                    nameEdit.getValue(), bold, italic, underline, strikethrough, obfuscated, colorRgb);
            if (!preview.getString().isEmpty()) {
                int px = (imageWidth - font.width(preview)) / 2;
                guiGraphics.text(font, preview.getVisualOrderText(), px, PREVIEW_Y,
                        0xFF000000 | (colorRgb & 0xFFFFFF), false);
            } else {
                Component empty = Component.translatable("gui.iska_utils.labeling_machine.preview_empty")
                        .withStyle(ChatFormatting.DARK_GRAY);
                int px = (imageWidth - font.width(empty)) / 2;
                guiGraphics.text(font, empty, px, PREVIEW_Y, GuiTextColors.MUTED, false);
            }
        } else {
            Component hexLabel = Component.translatable("gui.iska_utils.labeling_machine.hex");
            guiGraphics.text(font, hexLabel, NAME_EDIT_X, HEX_EDIT_Y - 12, GuiTextColors.TITLE, false);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (MachineGuiInput.handleContainerKeyPressed(this, event, false, nameEdit, hexEdit)) {
            return true;
        }
        return super.keyPressed(event);
    }
}
