package net.unfamily.iskautils.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.lwjgl.glfw.GLFW;
import net.unfamily.iskalib.item.ItemConverter;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.integration.jei.ghost.IIskaUtilsGhostTarget;
import net.unfamily.iskautils.integration.mekanism.MekChemicalHelper;
import net.unfamily.iskautils.network.ModMessages;
import net.unfamily.iskautils.shop.ShopCategory;
import net.unfamily.iskautils.shop.ShopCurrency;
import net.unfamily.iskautils.shop.ShopEntry;
import net.unfamily.iskautils.shop.ShopEntryHelper;
import net.unfamily.iskautils.shop.ShopOtherRegistry;
import net.unfamily.iskautils.shop.ShopStage;
import net.unfamily.iskautils.shop.edit.ShopEditResourceFormats;
import net.unfamily.iskautils.shop.edit.ShopEditSession;
import net.unfamily.iskautils.shop.edit.ShopEditWorkspace;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Single-screen shop JSON editor with sub-views (no nested screens).
 */
public class ShopEditScreen extends AbstractContainerScreen<ShopEditMenu> implements IIskaUtilsGhostTarget {

    private enum SubView {
        CATEGORIES, CATEGORY_EDIT, ENTRIES, ENTRY_EDIT, ENTRY_STAGES, CURRENCIES, CURRENCY_EDIT
    }

    private enum Dialog {
        NONE, DELETE_CONFIRM, RENAME_CONFIRM, CLOSE_HINT
    }

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/backgrounds/shop.png");
    private static final Identifier SCROLLBAR_TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/scrollbar.png");
    private static final Identifier SINGLE_SLOT_TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/single_slot.png");

    private static final int GUI_WIDTH = 300;
    private static final int GUI_HEIGHT = 240;
    private static final int ENTRY_HEIGHT = 24;
    private static final int ENTRY_START_X = 19;
    private static final int ENTRY_START_Y = 28;
    private static final int MAX_VISIBLE = 4;
    private static final int LIST_ICON_SIZE = 18;
    private static final int LIST_ICON_GAP = 4;
    private static final int RESOURCE_ARROW_SIZE = 12;
    private static final int RESOURCE_ARROW_GAP = 4;
    private static final int RESOURCE_SLOT_SIZE = 18;

    private static final int SCROLLBAR_WIDTH = 8;
    private static final int SCROLLBAR_HEIGHT = 34;
    private static final int HANDLE_SIZE = 8;

    /** Buttons beside the player inventory (inventory starts at x=20,y=154). */
    private static final int SIDE_BTN_X = 188;
    private static final int SIDE_BTN_Y = 154;
    private static final int SIDE_BTN_W = 72;
    private static final int SIDE_BTN_H = 14;

    /** Form content aligns to the right edge of the Done button. */
    private static final int FORM_LEFT = 20;
    private static final int FORM_RIGHT = SIDE_BTN_X + SIDE_BTN_W;
    private static final int FORM_WIDTH = FORM_RIGHT - FORM_LEFT;
    private static final int FORM_GAP = 4;
    private static final int SIDE_BTN_DONE_Y = SIDE_BTN_Y + SIDE_BTN_H + FORM_GAP;

    /**
     * Browse lists end flush with Done/Currencies ({@link #FORM_RIGHT}).
     * Scrollbar sits just after that edge. Edit forms are unchanged.
     */
    private static final int LIST_ACTION_W = 18;
    private static final int LIST_ACTION_GAP = 2;
    private static final int LIST_ACTION_RIGHT = FORM_RIGHT;
    private static final int ENTRY_WIDTH = LIST_ACTION_RIGHT - ENTRY_START_X;
    private static final int SCROLLBAR_X = LIST_ACTION_RIGHT + 4;

    private static final int STAGE_ROW_HEIGHT = 14;
    private static final int MAX_VISIBLE_STAGES = 6;
    private static final int STAGE_LIST_X = FORM_LEFT;
    private static final int STAGE_LIST_Y = 48;
    /** Scrollbar sits at Done's right edge so the stage delete button aligns with Done. */
    private static final int STAGE_SCROLLBAR_X = FORM_RIGHT;
    private static final int STAGE_LIST_WIDTH = FORM_RIGHT - STAGE_LIST_X;

    private record FormLabel(int x, int y, Component text) {}
    /** Left-side list preview: item icon slot, entry (item/fluid/gas), currency symbol, or empty. */
    private enum ListRowKind { ITEM_SLOT, ENTRY_SLOT, SYMBOL_CELL }
    private record ListRowVisual(
            int rowIndex,
            ListRowKind kind,
            @Nullable ItemStack icon,
            @Nullable String itemSelector,
            @Nullable String symbol,
            @Nullable ShopEntry entry) {}

    private SubView subView = SubView.CATEGORIES;
    /** Where Done from the currencies list returns (e.g. entry edit). */
    @Nullable private SubView currenciesReturnTo;
    private Dialog dialog = Dialog.NONE;
    private int scrollOffset;
    private int stageScrollOffset;
    private boolean isDraggingHandle;
    private boolean isDraggingStageHandle;

    @Nullable private String selectedCategoryId;

    private ShopCategory draftCategory;
    private String draftCategoryOldId;
    private boolean draftCategoryIsNew;
    private ShopCurrency draftCurrency;
    private String draftCurrencyOldId;
    private boolean draftCurrencyIsNew;
    private ShopEntry draftEntry;
    private String draftEntryOldId;
    private final List<ShopStage> draftStages = new ArrayList<>();
    private int editingStageIndex = -1;

    private String pendingDeleteKind;
    private String pendingDeleteId;
    private String renameKind;
    private String renameOldId;
    private String renameNewId;
    private boolean closingConfirmed;

    private final List<String> resourceVariants = new ArrayList<>();
    private int resourceVariantIndex;
    private int formDebounceTicks;
    private boolean formDirty;
    private final List<FormLabel> formLabels = new ArrayList<>();
    private final List<ListRowVisual> listRowVisuals = new ArrayList<>();

    private final List<Button> dynamicButtons = new ArrayList<>();
    private final List<EditBox> formBoxes = new ArrayList<>();
    private EditBox idBox;
    private EditBox nameBox;
    private EditBox descBox;
    private EditBox resourceBox;
    private EditBox symbolBox;
    private EditBox amountBox;
    private EditBox buyBox;
    private EditBox sellBox;
    private EditBox priorityBox;
    private EditBox stageNameBox;
    private Button freeButton;
    private Button typeButton;
    private Button currencyButton;
    private Button stageTypeButton;
    private Button stageIsButton;
    private Button stageAddButton;

    private String stageTypeDraft = "world";
    private boolean stageIsDraft = true;

    public ShopEditScreen(ShopEditMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, GUI_WIDTH, GUI_HEIGHT);
        this.inventoryLabelY = 142;
        this.titleLabelY = 8;
    }

    public void applySync(ShopEditWorkspace.ShopEditData data) {
        menu.applySync(data);
        if (dialog == Dialog.NONE && isListView()) {
            rebuild();
        }
    }

    private boolean isListView() {
        return subView == SubView.CATEGORIES || subView == SubView.ENTRIES || subView == SubView.CURRENCIES;
    }

    @Override
    protected void init() {
        super.init();
        rebuild();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (formDirty && formDebounceTicks > 0) {
            formDebounceTicks--;
            if (formDebounceTicks == 0) {
                formDirty = false;
                flushFormToDraft();
                autosaveCurrentForm(false);
            }
        }
    }

    private void clearDynamic() {
        for (Button b : dynamicButtons) {
            removeWidget(b);
        }
        dynamicButtons.clear();
        for (EditBox box : formBoxes) {
            removeWidget(box);
        }
        formBoxes.clear();
        formLabels.clear();
        listRowVisuals.clear();
        idBox = nameBox = descBox = resourceBox = symbolBox = null;
        amountBox = buyBox = sellBox = priorityBox = stageNameBox = null;
        freeButton = typeButton = currencyButton = stageTypeButton = stageIsButton = stageAddButton = null;
        if (subView != SubView.CATEGORY_EDIT && subView != SubView.ENTRY_EDIT) {
            menu.clearGhostStack();
        }
    }

    private <T extends Button> T addDyn(T button) {
        dynamicButtons.add(button);
        return addRenderableWidget(button);
    }

    private void addLabel(int x, int y, String langKey) {
        formLabels.add(new FormLabel(x, y, Component.translatable(langKey)));
    }

    private EditBox addBox(int x, int y, int w, int h, String value, int maxLen) {
        EditBox box = new EditBox(font, leftPos + x, topPos + y, w, h, Component.empty());
        box.setMaxLength(maxLen);
        box.setValue(value != null ? value : "");
        box.setResponder(v -> markFormDirty());
        formBoxes.add(box);
        addRenderableWidget(box);
        return box;
    }

    /** Label above, edit box below. Returns the edit box. */
    private EditBox addLabeledField(int x, int labelY, int boxY, int w, int h, String langKey, String value, int maxLen) {
        addLabel(x, labelY, langKey);
        return addBox(x, boxY, w, h, value, maxLen);
    }

    private void addSideNavButtons() {
        boolean inCurrencyMenus = subView == SubView.CURRENCIES || subView == SubView.CURRENCY_EDIT;
        Button currencies = addDyn(Button.builder(Component.translatable("gui.iska_utils.shop_edit.currencies"), b -> openCurrencies())
                .bounds(leftPos + SIDE_BTN_X, topPos + SIDE_BTN_Y, SIDE_BTN_W, SIDE_BTN_H)
                .build());
        currencies.active = !inCurrencyMenus;

        addDyn(Button.builder(Component.translatable("gui.iska_utils.shop_edit.done"), b -> navigateBack())
                .bounds(leftPos + SIDE_BTN_X, topPos + SIDE_BTN_DONE_Y, SIDE_BTN_W, SIDE_BTN_H)
                .build());
    }

    private void openCurrencies() {
        if (subView != SubView.CURRENCIES && subView != SubView.CURRENCY_EDIT) {
            flushFormToDraft();
            autosaveCurrentForm(true);
            currenciesReturnTo = subView;
        }
        subView = SubView.CURRENCIES;
        scrollOffset = 0;
        rebuild();
    }

    /** Equal-width column X for a row spanning {@link #FORM_LEFT}..{@link #FORM_RIGHT}. */
    private static int formColX(int index, int count) {
        int w = (FORM_WIDTH - FORM_GAP * Math.max(0, count - 1)) / count;
        return FORM_LEFT + index * (w + FORM_GAP);
    }

    /** Equal-width column width; last column absorbs remainder so the row ends at {@link #FORM_RIGHT}. */
    private static int formColW(int index, int count) {
        int w = (FORM_WIDTH - FORM_GAP * Math.max(0, count - 1)) / count;
        if (index >= count - 1) {
            return FORM_RIGHT - formColX(index, count);
        }
        return w;
    }

    private int resourceSlotX() {
        return FORM_LEFT + RESOURCE_ARROW_SIZE + RESOURCE_ARROW_GAP;
    }

    private int resourceRightArrowX() {
        return resourceSlotX() + RESOURCE_SLOT_SIZE + RESOURCE_ARROW_GAP;
    }

    private int resourceEditBoxX() {
        return resourceRightArrowX() + RESOURCE_ARROW_SIZE + RESOURCE_ARROW_GAP;
    }

    /**
     * Deep Drawer style: {@code ← SLOT → EDITBOX} (+ optional Convert), ending at {@link #FORM_RIGHT}.
     */
    private int addResourceSelectorRow(int rowY, String currentValue, boolean withConvert) {
        int arrowY = rowY + (RESOURCE_SLOT_SIZE - RESOURCE_ARROW_SIZE) / 2;
        int boxY = rowY + (RESOURCE_SLOT_SIZE - 12) / 2;
        syncGhostFromResource(currentValue);
        setupResourceVariants(currentValue);
        addDyn(Button.builder(Component.literal("←"), b -> cycleResource(-1))
                .bounds(leftPos + FORM_LEFT, topPos + arrowY, RESOURCE_ARROW_SIZE, RESOURCE_ARROW_SIZE)
                .build());
        addDyn(Button.builder(Component.literal("→"), b -> cycleResource(1))
                .bounds(leftPos + resourceRightArrowX(), topPos + arrowY, RESOURCE_ARROW_SIZE, RESOURCE_ARROW_SIZE)
                .build());
        int boxX = resourceEditBoxX();
        final int convertW = 52;
        int boxW;
        if (withConvert) {
            int convertX = FORM_RIGHT - convertW;
            boxW = Math.max(40, convertX - FORM_GAP - boxX);
            resourceBox = addBox(boxX, boxY, boxW, 12, currentValue, 512);
            addDyn(Button.builder(Component.translatable("gui.iska_utils.shop_edit.convert"), b -> convertResource())
                    .bounds(leftPos + convertX, topPos + boxY, convertW, 12)
                    .tooltip(Tooltip.create(Component.translatable("gui.iska_utils.shop_edit.convert.tooltip")))
                    .build());
            return FORM_RIGHT;
        }
        boxW = Math.max(40, FORM_RIGHT - boxX);
        resourceBox = addBox(boxX, boxY, boxW, 12, currentValue, 512);
        return FORM_RIGHT;
    }

    private void markFormDirty() {
        formDirty = true;
        formDebounceTicks = 8;
    }

    private void rebuild() {
        clearDynamic();
        scrollOffset = Math.max(0, scrollOffset);
        addDyn(Button.builder(Component.literal("✕"), b -> navigateBack())
                .bounds(leftPos + GUI_WIDTH - 17, topPos + 5, 12, 12)
                .tooltip(Tooltip.create(Component.translatable(
                        isMainView() ? "gui.iska_utils.shop_edit.close" : "gui.iska_utils.shop.back")))
                .build());

        if (dialog == Dialog.DELETE_CONFIRM) {
            buildDeleteDialog();
            return;
        }
        if (dialog == Dialog.RENAME_CONFIRM) {
            buildRenameDialog();
            return;
        }
        if (dialog == Dialog.CLOSE_HINT) {
            buildCloseHintDialog();
            return;
        }

        addSideNavButtons();

        switch (subView) {
            case CATEGORIES -> buildCategories();
            case CATEGORY_EDIT -> buildCategoryEdit();
            case ENTRIES -> buildEntries();
            case ENTRY_EDIT -> buildEntryEdit();
            case ENTRY_STAGES -> buildEntryStages();
            case CURRENCIES -> buildCurrencies();
            case CURRENCY_EDIT -> buildCurrencyEdit();
        }
    }

    private void buildDeleteDialog() {
        final int btnW = 90;
        final int btnH = 20;
        final int gap = 12;
        final int pairW = btnW * 2 + gap;
        final int startX = leftPos + (GUI_WIDTH - pairW) / 2;
        final int y = topPos + 100;
        addDyn(Button.builder(Component.translatable("gui.iska_utils.shop_edit.confirm_delete"), b -> {
            sendAction("delete_" + pendingDeleteKind, obj -> obj.addProperty("id", pendingDeleteId));
            dialog = Dialog.NONE;
            pendingDeleteKind = null;
            pendingDeleteId = null;
            rebuild();
        }).bounds(startX, y, btnW, btnH).build());
        addDyn(Button.builder(Component.translatable("gui.iska_utils.shop_edit.cancel"), b -> {
            dialog = Dialog.NONE;
            rebuild();
        }).bounds(startX + btnW + gap, y, btnW, btnH).build());
    }

    private void buildRenameDialog() {
        addLabel(FORM_LEFT, 48, "gui.iska_utils.shop_edit.rename.prompt");
        addDyn(Button.builder(Component.translatable("gui.iska_utils.shop_edit.rename.propagate"), b -> {
            finishRename("propagate");
        }).bounds(leftPos + FORM_LEFT, topPos + 80, FORM_WIDTH, 18).build());
        addDyn(Button.builder(Component.translatable("gui.iska_utils.shop_edit.rename.delete"), b -> {
            finishRename("delete");
        }).bounds(leftPos + FORM_LEFT, topPos + 102, FORM_WIDTH, 18).build());
        addDyn(Button.builder(Component.translatable("gui.iska_utils.shop_edit.rename.ignore"), b -> {
            finishRename("ignore");
        }).bounds(leftPos + FORM_LEFT, topPos + 124, FORM_WIDTH, 18).build());
    }

    private void buildCloseHintDialog() {
        Button currencies = addDyn(Button.builder(Component.translatable("gui.iska_utils.shop_edit.currencies"), b -> {})
                .bounds(leftPos + SIDE_BTN_X, topPos + SIDE_BTN_Y, SIDE_BTN_W, SIDE_BTN_H)
                .build());
        currencies.active = false;
        addDyn(Button.builder(Component.translatable("gui.iska_utils.shop_edit.done"), b -> confirmClose())
                .bounds(leftPos + SIDE_BTN_X, topPos + SIDE_BTN_DONE_Y, SIDE_BTN_W, SIDE_BTN_H)
                .build());
    }

    private boolean isMainView() {
        return dialog == Dialog.NONE && subView == SubView.CATEGORIES;
    }

    /**
     * One layer up (same as Done). On the main categories list, shows the close hint instead.
     */
    private void navigateBack() {
        if (dialog == Dialog.CLOSE_HINT) {
            confirmClose();
            return;
        }
        if (dialog == Dialog.DELETE_CONFIRM) {
            dialog = Dialog.NONE;
            pendingDeleteKind = null;
            pendingDeleteId = null;
            rebuild();
            return;
        }
        if (dialog == Dialog.RENAME_CONFIRM) {
            dialog = Dialog.NONE;
            renameKind = renameOldId = renameNewId = null;
            rebuild();
            return;
        }

        switch (subView) {
            case CATEGORIES -> {
                dialog = Dialog.CLOSE_HINT;
                rebuild();
            }
            case CURRENCIES -> {
                SubView back = currenciesReturnTo != null ? currenciesReturnTo : SubView.CATEGORIES;
                currenciesReturnTo = null;
                subView = back;
                scrollOffset = 0;
                rebuild();
            }
            case ENTRIES -> {
                subView = SubView.CATEGORIES;
                scrollOffset = 0;
                rebuild();
            }
            case CATEGORY_EDIT -> tryLeaveCategoryEdit();
            case CURRENCY_EDIT -> tryLeaveCurrencyEdit();
            case ENTRY_EDIT -> {
                flushFormToDraft();
                autosaveCurrentForm(true);
                editingStageIndex = -1;
                subView = SubView.ENTRIES;
                scrollOffset = 0;
                rebuild();
            }
            case ENTRY_STAGES -> {
                flushFormToDraft();
                autosaveCurrentForm(true);
                editingStageIndex = -1;
                subView = SubView.ENTRY_EDIT;
                rebuild();
            }
        }
    }

    private void confirmClose() {
        closingConfirmed = true;
        onClose();
    }

    @Override
    public void onClose() {
        if (!closingConfirmed) {
            navigateBack();
            return;
        }
        super.onClose();
    }

    private void finishRename(String mode) {
        if ("category".equals(renameKind) && draftCategory != null) {
            flushFormToDraft();
            sendUpsertCategory(mode);
            selectedCategoryId = draftCategory.id;
            draftCategoryIsNew = false;
            subView = SubView.CATEGORIES;
        } else if ("currency".equals(renameKind) && draftCurrency != null) {
            flushFormToDraft();
            sendUpsertCurrency(mode);
            draftCurrencyIsNew = false;
            subView = SubView.CURRENCIES;
        }
        dialog = Dialog.NONE;
        renameKind = renameOldId = renameNewId = null;
        rebuild();
    }

    private void buildCategories() {
        List<ShopCategory> list = sortedCategories();
        int visible = Math.min(MAX_VISIBLE, list.size() + 1);
        ensureScroll(list.size() + 1);
        int contentX = listContentX();
        int mainW = listMainButtonWidth();
        int addW = listAddButtonWidth();
        for (int i = 0; i < visible; i++) {
            int idx = scrollOffset + i;
            int y = ENTRY_START_Y + i * ENTRY_HEIGHT;
            if (idx < list.size()) {
                ShopCategory cat = list.get(idx);
                final String catId = cat.id;
                listRowVisuals.add(new ListRowVisual(i, ListRowKind.ITEM_SLOT, null, cat.item, null, null));
                addDyn(Button.builder(Component.literal(truncate(displayName(cat.name), 28)), b -> {
                    selectedCategoryId = catId;
                    subView = SubView.ENTRIES;
                    scrollOffset = 0;
                    rebuild();
                }).bounds(leftPos + contentX, topPos + y, mainW, ENTRY_HEIGHT - 2).build());
                addDyn(Button.builder(Component.literal("✎"), b -> openCategoryEdit(catId))
                        .bounds(leftPos + LIST_ACTION_RIGHT - LIST_ACTION_W * 2 - LIST_ACTION_GAP, topPos + y, LIST_ACTION_W, ENTRY_HEIGHT - 2)
                        .tooltip(Tooltip.create(Component.translatable("gui.iska_utils.shop_edit.edit")))
                        .build());
                addDyn(Button.builder(Component.literal("D"), b -> confirmDelete("category", catId))
                        .bounds(leftPos + LIST_ACTION_RIGHT - LIST_ACTION_W, topPos + y, LIST_ACTION_W, ENTRY_HEIGHT - 2)
                        .tooltip(deleteButtonTooltip())
                        .build());
            } else if (idx == list.size()) {
                listRowVisuals.add(new ListRowVisual(i, ListRowKind.ITEM_SLOT, ItemStack.EMPTY, null, null, null));
                addDyn(Button.builder(Component.translatable("gui.iska_utils.shop_edit.add_category"), b -> openCategoryEdit(null))
                        .bounds(leftPos + contentX, topPos + y, addW, ENTRY_HEIGHT - 2).build());
            }
        }
    }

    private void buildCurrencies() {
        List<ShopCurrency> list = sortedCurrencies();
        int visible = Math.min(MAX_VISIBLE, list.size() + 1);
        ensureScroll(list.size() + 1);
        int contentX = listContentX();
        int mainW = listMainButtonWidth();
        int addW = listAddButtonWidth();
        for (int i = 0; i < visible; i++) {
            int idx = scrollOffset + i;
            int y = ENTRY_START_Y + i * ENTRY_HEIGHT;
            if (idx < list.size()) {
                ShopCurrency cur = list.get(idx);
                final String curId = cur.id;
                listRowVisuals.add(new ListRowVisual(i, ListRowKind.SYMBOL_CELL, null, null, nullSafe(cur.charSymbol), null));
                addDyn(Button.builder(Component.literal(truncate(displayName(cur.name), 30)), b -> openCurrencyEdit(curId))
                        .bounds(leftPos + contentX, topPos + y, mainW, ENTRY_HEIGHT - 2).build());
                addDyn(Button.builder(Component.literal("✎"), b -> openCurrencyEdit(curId))
                        .bounds(leftPos + LIST_ACTION_RIGHT - LIST_ACTION_W * 2 - LIST_ACTION_GAP, topPos + y, LIST_ACTION_W, ENTRY_HEIGHT - 2).build());
                addDyn(Button.builder(Component.literal("D"), b -> confirmDelete("currency", curId))
                        .bounds(leftPos + LIST_ACTION_RIGHT - LIST_ACTION_W, topPos + y, LIST_ACTION_W, ENTRY_HEIGHT - 2)
                        .tooltip(deleteButtonTooltip())
                        .build());
            } else if (idx == list.size()) {
                listRowVisuals.add(new ListRowVisual(i, ListRowKind.SYMBOL_CELL, null, null, "", null));
                addDyn(Button.builder(Component.translatable("gui.iska_utils.shop_edit.add_currency"), b -> openCurrencyEdit(null))
                        .bounds(leftPos + contentX, topPos + y, addW, ENTRY_HEIGHT - 2).build());
            }
        }
    }

    private void buildEntries() {
        List<ShopEntry> list = entriesInCategory(selectedCategoryId);
        int visible = Math.min(MAX_VISIBLE, list.size() + 1);
        ensureScroll(list.size() + 1);
        int contentX = listContentX();
        int mainW = listMainButtonWidth();
        int addW = listAddButtonWidth();
        for (int i = 0; i < visible; i++) {
            int idx = scrollOffset + i;
            int y = ENTRY_START_Y + i * ENTRY_HEIGHT;
            if (idx < list.size()) {
                ShopEntry e = list.get(idx);
                final String entryId = e.id;
                listRowVisuals.add(new ListRowVisual(i, ListRowKind.ENTRY_SLOT, null, null, null, e));
                addDyn(Button.builder(Component.literal(truncate(entryContentLabel(e), 30)), b -> openEntryEdit(entryId))
                        .bounds(leftPos + contentX, topPos + y, mainW, ENTRY_HEIGHT - 2).build());
                addDyn(Button.builder(Component.literal("✎"), b -> openEntryEdit(entryId))
                        .bounds(leftPos + LIST_ACTION_RIGHT - LIST_ACTION_W * 2 - LIST_ACTION_GAP, topPos + y, LIST_ACTION_W, ENTRY_HEIGHT - 2).build());
                addDyn(Button.builder(Component.literal("D"), b -> confirmDelete("entry", entryId))
                        .bounds(leftPos + LIST_ACTION_RIGHT - LIST_ACTION_W, topPos + y, LIST_ACTION_W, ENTRY_HEIGHT - 2)
                        .tooltip(deleteButtonTooltip())
                        .build());
            } else if (idx == list.size()) {
                listRowVisuals.add(new ListRowVisual(i, ListRowKind.ITEM_SLOT, ItemStack.EMPTY, null, null, null));
                addDyn(Button.builder(Component.translatable("gui.iska_utils.shop_edit.add_entry"), b -> openEntryEdit(null))
                        .bounds(leftPos + contentX, topPos + y, addW, ENTRY_HEIGHT - 2).build());
            }
        }
    }

    private int listContentX() {
        return ENTRY_START_X + LIST_ICON_SIZE + LIST_ICON_GAP;
    }

    private int listMainButtonWidth() {
        int editX = LIST_ACTION_RIGHT - LIST_ACTION_W * 2 - LIST_ACTION_GAP;
        return editX - LIST_ACTION_GAP - listContentX();
    }

    private int listAddButtonWidth() {
        return LIST_ACTION_RIGHT - listContentX();
    }

    private void buildCategoryEdit() {
        if (draftCategory == null) {
            draftCategory = new ShopCategory();
            draftCategory.id = "new_category";
            draftCategory.name = "New Category";
            draftCategory.description = "";
            draftCategory.item = "minecraft:stone";
            draftCategory.priority = 0;
            draftCategoryOldId = draftCategory.id;
            draftCategoryIsNew = true;
        }
        idBox = addLabeledField(FORM_LEFT, 22, 32, FORM_WIDTH, 12, "gui.iska_utils.shop_edit.field.id", draftCategory.id, 128);
        nameBox = addLabeledField(FORM_LEFT, 48, 58, FORM_WIDTH, 12, "gui.iska_utils.shop_edit.field.name", draftCategory.name, 256);
        descBox = addLabeledField(FORM_LEFT, 74, 84, FORM_WIDTH, 12, "gui.iska_utils.shop_edit.field.description", draftCategory.description, 512);
        priorityBox = addLabeledField(FORM_LEFT, 100, 110, formColW(0, 4), 12, "gui.iska_utils.shop_edit.field.priority",
                String.valueOf(draftCategory.priority), 16);
        addLabel(FORM_LEFT, 124, "gui.iska_utils.shop_edit.field.icon");
        addResourceSelectorRow(134, draftCategory.item, false);
    }

    private void buildCurrencyEdit() {
        if (draftCurrency == null) {
            draftCurrency = new ShopCurrency();
            draftCurrency.id = "new_currency";
            draftCurrency.name = "New Currency";
            draftCurrency.charSymbol = ShopCurrency.DEFAULT_SYMBOL;
            draftCurrency.priority = 0;
            draftCurrencyOldId = draftCurrency.id;
            draftCurrencyIsNew = true;
        }
        idBox = addLabeledField(FORM_LEFT, 28, 38, FORM_WIDTH, 12, "gui.iska_utils.shop_edit.field.id", draftCurrency.id, 128);
        nameBox = addLabeledField(FORM_LEFT, 56, 66, FORM_WIDTH, 12, "gui.iska_utils.shop_edit.field.name", draftCurrency.name, 256);
        symbolBox = addLabeledField(formColX(0, 2), 84, 94, formColW(0, 2), 12, "gui.iska_utils.shop_edit.field.symbol",
                draftCurrency.charSymbol, 8);
        priorityBox = addLabeledField(formColX(1, 2), 84, 94, formColW(1, 2), 12, "gui.iska_utils.shop_edit.field.priority",
                String.valueOf(draftCurrency.priority), 16);
    }

    private void buildEntryEdit() {
        if (draftEntry == null) {
            draftEntry = new ShopEntry();
            draftEntry.id = "new_entry";
            draftEntry.inCategory = selectedCategoryId != null ? selectedCategoryId : "000_default";
            draftEntry.type = ShopEntry.EntryType.ITEM;
            draftEntry.item = "minecraft:stone";
            draftEntry.amount = 1;
            draftEntry.currency = firstCurrencyId();
            draftEntry.buy = 0;
            draftEntry.sell = 0;
            draftEntry.priority = 0;
            draftEntry.free = false;
            draftEntryOldId = draftEntry.id;
            draftStages.clear();
        }
        addLabel(formColX(0, 2), 20, "gui.iska_utils.shop_edit.field.id");
        idBox = addBox(formColX(0, 2), 30, formColW(0, 2), 12, draftEntry.id, 128);
        addLabel(formColX(1, 2), 20, "gui.iska_utils.shop_edit.field.type");
        typeButton = addDyn(Button.builder(Component.literal(typeLabel()), b -> cycleType())
                .bounds(leftPos + formColX(1, 2), topPos + 30, formColW(1, 2), 12).build());

        addLabel(FORM_LEFT, 44, "gui.iska_utils.shop_edit.field.resource");
        addResourceSelectorRow(54, resourceString(draftEntry), true);

        addLabel(formColX(0, 4), 76, "gui.iska_utils.shop_edit.field.amount");
        amountBox = addBox(formColX(0, 4), 86, formColW(0, 4), 12, String.valueOf(draftEntry.amount), 16);
        addLabel(formColX(1, 4), 76, "gui.iska_utils.shop_edit.field.buy");
        buyBox = addBox(formColX(1, 4), 86, formColW(1, 4), 12, formatNum(draftEntry.buy), 24);
        addLabel(formColX(2, 4), 76, "gui.iska_utils.shop_edit.field.sell");
        sellBox = addBox(formColX(2, 4), 86, formColW(2, 4), 12, formatNum(draftEntry.sell), 24);
        addLabel(formColX(3, 4), 76, "gui.iska_utils.shop_edit.field.priority");
        priorityBox = addBox(formColX(3, 4), 86, formColW(3, 4), 12, String.valueOf(draftEntry.priority), 16);

        addLabel(formColX(0, 3), 102, "gui.iska_utils.shop_edit.field.currency");
        currencyButton = addDyn(Button.builder(Component.literal(nullSafe(draftEntry.currency)), b -> cycleCurrency())
                .bounds(leftPos + formColX(0, 3), topPos + 112, formColW(0, 3), 12).build());
        freeButton = addDyn(Button.builder(Component.translatable(
                draftEntry.free ? "gui.iska_utils.shop_edit.field.free_on" : "gui.iska_utils.shop_edit.field.free_off"), b -> {
            flushFormToDraft();
            draftEntry.free = !draftEntry.free;
            freeButton.setMessage(Component.translatable(
                    draftEntry.free ? "gui.iska_utils.shop_edit.field.free_on" : "gui.iska_utils.shop_edit.field.free_off"));
            autosaveCurrentForm(true);
        }).bounds(leftPos + formColX(1, 3), topPos + 112, formColW(1, 3), 12).build());
        addDyn(Button.builder(Component.translatable("gui.iska_utils.shop_edit.stages_button", draftStages.size()), b -> {
            flushFormToDraft();
            editingStageIndex = -1;
            stageScrollOffset = 0;
            subView = SubView.ENTRY_STAGES;
            rebuild();
        }).bounds(leftPos + formColX(2, 3), topPos + 112, formColW(2, 3), 12)
                .tooltip(Tooltip.create(Component.translatable("gui.iska_utils.shop_edit.stages")))
                .build());
    }

    /** Live warning under currency/free/stages row while editing an entry. */
    @Nullable
    private Component entryEditWarning() {
        if (subView != SubView.ENTRY_EDIT || draftEntry == null || dialog != Dialog.NONE) {
            return null;
        }
        String resource = resourceBox != null ? resourceBox.getValue().trim() : resourceString(draftEntry).trim();
        double buy = buyBox != null ? parseDouble(buyBox.getValue(), draftEntry.buy) : draftEntry.buy;
        double sell = sellBox != null ? parseDouble(sellBox.getValue(), draftEntry.sell) : draftEntry.sell;
        boolean free = draftEntry.free;
        boolean tag = resource.startsWith("#");
        if (tag && (buy > 0 || free)) {
            return Component.translatable("gui.iska_utils.shop_edit.warn.tag_buy");
        }
        if (!free && buy <= 0 && sell <= 0) {
            return Component.translatable("gui.iska_utils.shop_edit.warn.no_trade");
        }
        return null;
    }

    private void buildEntryStages() {
        addLabel(FORM_LEFT, STAGE_LIST_Y - 24, "gui.iska_utils.shop_edit.stages");
        int toolbarY = STAGE_LIST_Y - 14;
        stageNameBox = addBox(formColX(0, 4), toolbarY, formColW(0, 4), 12,
                editingStageIndex >= 0 && editingStageIndex < draftStages.size()
                        ? nullSafe(draftStages.get(editingStageIndex).stage) : "", 128);
        stageTypeButton = addDyn(Button.builder(Component.literal(stageTypeDraft), b -> {
            stageTypeDraft = nextStageType(stageTypeDraft);
            stageTypeButton.setMessage(Component.literal(stageTypeDraft));
        }).bounds(leftPos + formColX(1, 4), topPos + toolbarY, formColW(1, 4), 12).build());
        stageIsButton = addDyn(Button.builder(Component.literal(stageIsDraft ? "is" : "!is"), b -> {
            stageIsDraft = !stageIsDraft;
            stageIsButton.setMessage(Component.literal(stageIsDraft ? "is" : "!is"));
        }).bounds(leftPos + formColX(2, 4), topPos + toolbarY, formColW(2, 4), 12).build());
        stageAddButton = addDyn(Button.builder(Component.literal(editingStageIndex >= 0 ? "A" : "+"), b -> applyStageDraft())
                .bounds(leftPos + formColX(3, 4), topPos + toolbarY, formColW(3, 4), 12)
                .tooltip(Tooltip.create(Component.translatable(editingStageIndex >= 0
                        ? "gui.iska_utils.shop_edit.stage.apply" : "gui.iska_utils.shop_edit.stage.add")))
                .build());

        ensureStageScroll();
        int visible = Math.min(MAX_VISIBLE_STAGES, draftStages.size());
        int actionW = 16;
        int labelW = STAGE_LIST_WIDTH - actionW * 2 - FORM_GAP * 2;
        for (int i = 0; i < visible; i++) {
            int idx = stageScrollOffset + i;
            if (idx >= draftStages.size()) {
                break;
            }
            ShopStage st = draftStages.get(idx);
            final int stageIndex = idx;
            int stageY = STAGE_LIST_Y + i * STAGE_ROW_HEIGHT;
            String label = (st.is ? "" : "!") + nullSafe(st.stageType) + ":" + nullSafe(st.stage);
            addDyn(Button.builder(Component.literal(truncate(label, 36)), b -> {})
                    .bounds(leftPos + STAGE_LIST_X, topPos + stageY, labelW, STAGE_ROW_HEIGHT - 2).build());
            addDyn(Button.builder(Component.literal("✎"), b -> beginEditStage(stageIndex))
                    .bounds(leftPos + STAGE_LIST_X + labelW + FORM_GAP, topPos + stageY, actionW, STAGE_ROW_HEIGHT - 2)
                    .tooltip(Tooltip.create(Component.translatable("gui.iska_utils.shop_edit.edit")))
                    .build());
            addDyn(Button.builder(Component.literal("D"), b -> {
                draftStages.remove(stageIndex);
                if (editingStageIndex == stageIndex) {
                    editingStageIndex = -1;
                } else if (editingStageIndex > stageIndex) {
                    editingStageIndex--;
                }
                autosaveCurrentForm(true);
                rebuild();
            }).bounds(leftPos + STAGE_LIST_X + labelW + FORM_GAP + actionW + FORM_GAP, topPos + stageY, actionW, STAGE_ROW_HEIGHT - 2)
                    .tooltip(deleteButtonTooltip())
                    .build());
        }
    }

    private void beginEditStage(int index) {
        if (index < 0 || index >= draftStages.size()) {
            return;
        }
        ShopStage st = draftStages.get(index);
        editingStageIndex = index;
        stageTypeDraft = st.stageType != null ? st.stageType : "world";
        stageIsDraft = st.is;
        rebuild();
    }

    private void applyStageDraft() {
        if (stageNameBox == null) {
            return;
        }
        String stage = stageNameBox.getValue().trim();
        if (stage.isEmpty()) {
            return;
        }
        if (editingStageIndex >= 0 && editingStageIndex < draftStages.size()) {
            ShopStage st = draftStages.get(editingStageIndex);
            st.stage = stage;
            st.stageType = stageTypeDraft;
            st.is = stageIsDraft;
            editingStageIndex = -1;
        } else {
            ShopStage st = new ShopStage();
            st.stage = stage;
            st.stageType = stageTypeDraft;
            st.is = stageIsDraft;
            draftStages.add(st);
        }
        autosaveCurrentForm(true);
        rebuild();
    }

    private void ensureScroll(int total) {
        int max = Math.max(0, total - MAX_VISIBLE);
        if (scrollOffset > max) {
            scrollOffset = max;
        }
    }

    private void ensureStageScroll() {
        int max = Math.max(0, draftStages.size() - MAX_VISIBLE_STAGES);
        if (stageScrollOffset > max) {
            stageScrollOffset = max;
        }
        if (stageScrollOffset < 0) {
            stageScrollOffset = 0;
        }
    }

    private int maxListScroll() {
        return Math.max(0, listTotalCount() - MAX_VISIBLE);
    }

    private int maxStageScroll() {
        return Math.max(0, draftStages.size() - MAX_VISIBLE_STAGES);
    }

    private void openCategoryEdit(@Nullable String id) {
        if (id == null) {
            draftCategory = new ShopCategory();
            draftCategory.id = uniqueId("category");
            draftCategory.name = friendlyNameFromId(draftCategory.id);
            draftCategory.description = "";
            draftCategory.item = "minecraft:stone";
            draftCategory.priority = 0;
            draftCategoryOldId = draftCategory.id;
            draftCategoryIsNew = true;
        } else {
            ShopCategory src = menu.getData().categories.get(id);
            draftCategory = src != null ? ShopEditSession.copyCategory(src) : new ShopCategory();
            draftCategoryOldId = draftCategory.id;
            draftCategoryIsNew = false;
        }
        subView = SubView.CATEGORY_EDIT;
        scrollOffset = 0;
        rebuild();
    }

    private void openCurrencyEdit(@Nullable String id) {
        if (id == null) {
            draftCurrency = new ShopCurrency();
            draftCurrency.id = uniqueId("currency");
            draftCurrency.name = friendlyNameFromId(draftCurrency.id);
            draftCurrency.charSymbol = ShopCurrency.DEFAULT_SYMBOL;
            draftCurrency.priority = 0;
            draftCurrencyOldId = draftCurrency.id;
            draftCurrencyIsNew = true;
        } else {
            ShopCurrency src = menu.getData().currencies.get(id);
            draftCurrency = src != null ? ShopEditSession.copyCurrency(src) : new ShopCurrency();
            draftCurrencyOldId = draftCurrency.id;
            draftCurrencyIsNew = false;
            if (src == null) {
                draftCurrency.priority = 0;
                if (draftCurrency.charSymbol == null || draftCurrency.charSymbol.isBlank()) {
                    draftCurrency.charSymbol = ShopCurrency.DEFAULT_SYMBOL;
                }
            }
        }
        subView = SubView.CURRENCY_EDIT;
        scrollOffset = 0;
        rebuild();
    }

    private void openEntryEdit(@Nullable String id) {
        draftStages.clear();
        if (id == null) {
            draftEntry = new ShopEntry();
            draftEntry.id = uniqueId("entry");
            draftEntry.inCategory = selectedCategoryId != null ? selectedCategoryId : "000_default";
            draftEntry.type = ShopEntry.EntryType.ITEM;
            draftEntry.item = "minecraft:stone";
            draftEntry.amount = 1;
            draftEntry.currency = firstCurrencyId();
            draftEntry.valute = draftEntry.currency;
            draftEntry.buy = 0;
            draftEntry.sell = 0;
            draftEntryOldId = draftEntry.id;
        } else {
            ShopEntry src = menu.getData().entries.get(id);
            draftEntry = src != null ? ShopEditSession.copyEntry(src) : new ShopEntry();
            draftEntryOldId = draftEntry.id;
            if (draftEntry.stages != null) {
                for (ShopStage st : draftEntry.stages) {
                    if (st != null) {
                        ShopStage copy = new ShopStage();
                        copy.stage = st.stage;
                        copy.stageType = st.stageType;
                        copy.is = st.is;
                        draftStages.add(copy);
                    }
                }
            }
        }
        subView = SubView.ENTRY_EDIT;
        scrollOffset = 0;
        stageScrollOffset = 0;
        editingStageIndex = -1;
        rebuild();
    }

    private void confirmDelete(String kind, String id) {
        if (isCtrlOrAltDownNow()) {
            sendAction("delete_" + kind, obj -> obj.addProperty("id", id));
            rebuild();
            return;
        }
        pendingDeleteKind = kind;
        pendingDeleteId = id;
        dialog = Dialog.DELETE_CONFIRM;
        rebuild();
    }

    private boolean isCtrlOrAltDownNow() {
        if (this.minecraft == null) {
            return false;
        }
        var window = this.minecraft.getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT);
    }

    private static Tooltip deleteButtonTooltip() {
        return Tooltip.create(net.minecraft.network.chat.CommonComponents.joinLines(
                Component.translatable("gui.iska_utils.shop_edit.delete"),
                Component.translatable("gui.iska_utils.shop_edit.delete.skip_confirm")));
    }

    private void tryLeaveCategoryEdit() {
        flushFormToDraft();
        if (draftCategory == null) {
            subView = SubView.CATEGORIES;
            rebuild();
            return;
        }
        if (!draftCategoryOldId.equals(draftCategory.id)
                && draftCategory.id != null
                && !draftCategory.id.isBlank()) {
            if (draftCategoryIsNew) {
                sendUpsertCategory("propagate");
                draftCategoryIsNew = false;
                subView = SubView.CATEGORIES;
                scrollOffset = 0;
                rebuild();
                return;
            }
            renameKind = "category";
            renameOldId = draftCategoryOldId;
            renameNewId = draftCategory.id;
            dialog = Dialog.RENAME_CONFIRM;
            rebuild();
            return;
        }
        sendUpsertCategory(null);
        draftCategoryIsNew = false;
        subView = SubView.CATEGORIES;
        scrollOffset = 0;
        rebuild();
    }

    private void tryLeaveCurrencyEdit() {
        flushFormToDraft();
        if (draftCurrency == null) {
            subView = SubView.CURRENCIES;
            rebuild();
            return;
        }
        if (!draftCurrencyOldId.equals(draftCurrency.id)
                && draftCurrency.id != null
                && !draftCurrency.id.isBlank()) {
            if (draftCurrencyIsNew) {
                sendUpsertCurrency("propagate");
                draftCurrencyIsNew = false;
                subView = SubView.CURRENCIES;
                scrollOffset = 0;
                rebuild();
                return;
            }
            renameKind = "currency";
            renameOldId = draftCurrencyOldId;
            renameNewId = draftCurrency.id;
            dialog = Dialog.RENAME_CONFIRM;
            rebuild();
            return;
        }
        sendUpsertCurrency(null);
        draftCurrencyIsNew = false;
        subView = SubView.CURRENCIES;
        scrollOffset = 0;
        rebuild();
    }

    private void flushFormToDraft() {
        if (subView == SubView.CATEGORY_EDIT && draftCategory != null) {
            if (idBox != null) draftCategory.id = idBox.getValue().trim();
            if (nameBox != null) draftCategory.name = nameBox.getValue();
            if (descBox != null) draftCategory.description = descBox.getValue();
            if (priorityBox != null) draftCategory.priority = parseInt(priorityBox.getValue(), 0);
            if (resourceBox != null) draftCategory.item = resourceBox.getValue().trim();
        } else if (subView == SubView.CURRENCY_EDIT && draftCurrency != null) {
            if (idBox != null) draftCurrency.id = idBox.getValue().trim();
            if (nameBox != null) draftCurrency.name = nameBox.getValue();
            if (symbolBox != null) draftCurrency.charSymbol = symbolBox.getValue();
            if (priorityBox != null) draftCurrency.priority = parseInt(priorityBox.getValue(), 0);
        } else if (subView == SubView.ENTRY_EDIT && draftEntry != null) {
            if (idBox != null) draftEntry.id = idBox.getValue().trim();
            if (amountBox != null) {
                draftEntry.amount = Math.max(1, parseInt(amountBox.getValue(), 1));
                draftEntry.itemCount = draftEntry.amount;
            }
            if (buyBox != null) draftEntry.buy = parseDouble(buyBox.getValue(), 0);
            if (sellBox != null) draftEntry.sell = parseDouble(sellBox.getValue(), 0);
            if (priorityBox != null) draftEntry.priority = parseInt(priorityBox.getValue(), 0);
            if (resourceBox != null) applyResourceString(resourceBox.getValue().trim());
            draftEntry.stages = draftStages.toArray(new ShopStage[0]);
        } else if (subView == SubView.ENTRY_STAGES && draftEntry != null) {
            draftEntry.stages = draftStages.toArray(new ShopStage[0]);
        }
    }

    private void autosaveCurrentForm(boolean immediate) {
        if (!immediate && formDirty) {
            return;
        }
        if (subView == SubView.CATEGORY_EDIT) {
            sendUpsertCategory(null);
        } else if (subView == SubView.CURRENCY_EDIT) {
            sendUpsertCurrency(null);
        } else if (subView == SubView.ENTRY_EDIT || subView == SubView.ENTRY_STAGES) {
            sendUpsertEntry();
        }
    }

    private void sendUpsertCategory(@Nullable String renameMode) {
        if (draftCategory == null) {
            return;
        }
        boolean pendingRename = renameMode == null
                && draftCategoryOldId != null
                && draftCategory.id != null
                && !draftCategoryOldId.equals(draftCategory.id);
        String saveId = pendingRename ? draftCategoryOldId : draftCategory.id;
        if (saveId == null || saveId.isBlank()) {
            return;
        }
        if (renameMode != null && (draftCategory.id == null || draftCategory.id.isBlank())) {
            return;
        }
        sendAction("upsert_category", o -> {
            o.addProperty("old_id", draftCategoryOldId);
            o.addProperty("id", renameMode != null ? draftCategory.id : saveId);
            o.addProperty("name", nullSafe(draftCategory.name));
            o.addProperty("description", nullSafe(draftCategory.description));
            o.addProperty("item", nullSafe(draftCategory.item));
            o.addProperty("priority", draftCategory.priority);
            if (renameMode != null) {
                o.addProperty("rename_mode", renameMode);
            }
        });
        if (renameMode != null || !pendingRename) {
            draftCategoryOldId = draftCategory.id;
        }
    }

    private void sendUpsertCurrency(@Nullable String renameMode) {
        if (draftCurrency == null) {
            return;
        }
        boolean pendingRename = renameMode == null
                && draftCurrencyOldId != null
                && draftCurrency.id != null
                && !draftCurrencyOldId.equals(draftCurrency.id);
        String saveId = pendingRename ? draftCurrencyOldId : draftCurrency.id;
        if (saveId == null || saveId.isBlank()) {
            return;
        }
        if (renameMode != null && (draftCurrency.id == null || draftCurrency.id.isBlank())) {
            return;
        }
        sendAction("upsert_currency", o -> {
            o.addProperty("old_id", draftCurrencyOldId);
            o.addProperty("id", renameMode != null ? draftCurrency.id : saveId);
            o.addProperty("name", nullSafe(draftCurrency.name));
            o.addProperty("char_symbol", nullSafe(draftCurrency.charSymbol));
            o.addProperty("priority", draftCurrency.priority);
            if (renameMode != null) {
                o.addProperty("rename_mode", renameMode);
            }
        });
        if (renameMode != null || !pendingRename) {
            draftCurrencyOldId = draftCurrency.id;
        }
    }

    private void sendUpsertEntry() {
        if (draftEntry == null || draftEntry.id == null || draftEntry.id.isBlank()) {
            return;
        }
        sendAction("upsert_entry", o -> {
            o.addProperty("old_id", draftEntryOldId);
            o.addProperty("id", draftEntry.id);
            o.addProperty("in_category", nullSafe(draftEntry.inCategory));
            o.addProperty("type", (draftEntry.type != null ? draftEntry.type : ShopEntry.EntryType.ITEM)
                    .name().toLowerCase(Locale.ROOT));
            if (draftEntry.item != null) o.addProperty("item", draftEntry.item);
            if (draftEntry.fluid != null) o.addProperty("fluid", draftEntry.fluid);
            if (draftEntry.gas != null) o.addProperty("gas", draftEntry.gas);
            if (draftEntry.other != null) o.addProperty("other", draftEntry.other);
            o.addProperty("amount", Math.max(1, draftEntry.amount));
            o.addProperty("currency", nullSafe(draftEntry.currency));
            o.addProperty("buy", draftEntry.buy);
            o.addProperty("sell", draftEntry.sell);
            o.addProperty("priority", draftEntry.priority);
            o.addProperty("free", draftEntry.free);
            JsonArray stages = new JsonArray();
            for (ShopStage st : draftStages) {
                JsonObject so = new JsonObject();
                so.addProperty("stage", nullSafe(st.stage));
                so.addProperty("stage_type", nullSafe(st.stageType));
                so.addProperty("is", st.is);
                stages.add(so);
            }
            o.add("stages", stages);
        });
        draftEntryOldId = draftEntry.id;
    }

    private void sendAction(String action, java.util.function.Consumer<JsonObject> filler) {
        JsonObject o = new JsonObject();
        filler.accept(o);
        ModMessages.sendShopEditActionPacket(action, o.toString());
    }

    private void setupResourceVariants(@Nullable String current) {
        resourceVariants.clear();
        if (subView == SubView.ENTRY_EDIT && draftEntry != null) {
            ShopEntry.EntryType type = draftEntry.type != null ? draftEntry.type : ShopEntry.EntryType.ITEM;
            if (type == ShopEntry.EntryType.FLUID) {
                resourceVariants.addAll(ShopEditResourceFormats.variantsFromFluid(current));
            } else if (type == ShopEntry.EntryType.GAS) {
                resourceVariants.addAll(ShopEditResourceFormats.variantsFromGas(current));
            } else if (type == ShopEntry.EntryType.OTHER) {
                resourceVariants.addAll(ShopOtherRegistry.all().stream()
                        .map(ShopOtherRegistry.Definition::id)
                        .toList());
            } else {
                addItemResourceVariants(current);
            }
        } else {
            addItemResourceVariants(current);
        }
        if (current != null && !current.isBlank() && !resourceVariants.contains(current)) {
            resourceVariants.add(0, current);
        }
        if (resourceVariants.isEmpty() && current != null) {
            resourceVariants.add(current);
        }
        resourceVariantIndex = ShopEditResourceFormats.indexOfOrZero(resourceVariants, current);
    }

    private void addItemResourceVariants(@Nullable String current) {
        ItemStack stack = menu.getGhostStack();
        if (stack.isEmpty() && current != null && !current.startsWith("#")) {
            stack = ItemConverter.parseItemString(current, 1);
        }
        if (!stack.isEmpty()) {
            resourceVariants.addAll(ShopEditResourceFormats.variantsFromStack(stack));
        }
    }

    private void cycleResource(int delta) {
        if (resourceVariants.isEmpty()) {
            return;
        }
        resourceVariantIndex = Math.floorMod(resourceVariantIndex + delta, resourceVariants.size());
        String value = resourceVariants.get(resourceVariantIndex);
        if (resourceBox != null) {
            resourceBox.setValue(value);
        }
        applyResourceString(value);
        autosaveCurrentForm(true);
    }

    private void applyResourceString(String value) {
        if (subView == SubView.CATEGORY_EDIT && draftCategory != null) {
            draftCategory.item = value;
        } else if (subView == SubView.ENTRY_EDIT && draftEntry != null) {
            ShopEntry.EntryType type = draftEntry.type != null ? draftEntry.type : ShopEntry.EntryType.ITEM;
            switch (type) {
                case FLUID -> {
                    draftEntry.fluid = value;
                    draftEntry.item = null;
                    draftEntry.gas = null;
                    draftEntry.other = null;
                }
                case GAS -> {
                    draftEntry.gas = value;
                    draftEntry.item = null;
                    draftEntry.fluid = null;
                    draftEntry.other = null;
                }
                case OTHER -> {
                    draftEntry.other = value;
                    draftEntry.item = null;
                    draftEntry.fluid = null;
                    draftEntry.gas = null;
                }
                case ITEM -> {
                    draftEntry.item = value;
                    draftEntry.fluid = null;
                    draftEntry.gas = null;
                    draftEntry.other = null;
                }
            }
        }
    }

    private String resourceString(ShopEntry e) {
        if (e.type == ShopEntry.EntryType.FLUID) {
            return nullSafe(e.fluid);
        }
        if (e.type == ShopEntry.EntryType.GAS) {
            return nullSafe(e.gas);
        }
        if (e.type == ShopEntry.EntryType.OTHER) {
            return nullSafe(e.other);
        }
        return nullSafe(e.item);
    }

    private void syncGhostFromResource(@Nullable String resource) {
        if (resource == null || resource.isBlank() || resource.startsWith("#")) {
            menu.setGhostStack(ItemStack.EMPTY);
            return;
        }
        if (subView == SubView.ENTRY_EDIT && draftEntry != null && draftEntry.type == ShopEntry.EntryType.FLUID) {
            menu.setGhostStack(ItemStack.EMPTY);
            return;
        }
        if (subView == SubView.ENTRY_EDIT && draftEntry != null
                && (draftEntry.type == ShopEntry.EntryType.GAS || draftEntry.type == ShopEntry.EntryType.OTHER)) {
            menu.setGhostStack(ItemStack.EMPTY);
            return;
        }
        ItemStack stack = ItemConverter.parseItemString(resource, 1);
        menu.setGhostStack(stack);
    }

    private void convertResource() {
        if (draftEntry == null || subView != SubView.ENTRY_EDIT) {
            return;
        }
        flushFormToDraft();
        ItemStack stack = menu.getGhostStack();
        if (stack.isEmpty() && draftEntry.item != null) {
            stack = ItemConverter.parseItemString(draftEntry.item, 1);
        }
        FluidStack fluid = ShopEntryHelper.normalizeFluidIngredient(ShopEntryHelper.fluidContainedInItem(stack));
        if (!fluid.isEmpty() && fluid.getFluid() != Fluids.EMPTY) {
            Identifier id = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
            draftEntry.type = ShopEntry.EntryType.FLUID;
            draftEntry.fluid = id != null ? id.toString() : "minecraft:water";
            draftEntry.item = null;
            draftEntry.gas = null;
            draftEntry.other = null;
            if (draftEntry.amount < 1000) {
                draftEntry.amount = 1000;
            }
            autosaveCurrentForm(true);
            rebuild();
            return;
        }
        if (MekChemicalHelper.isLoaded() && MekChemicalHelper.isGasSupportEnabled()) {
            Object gas = MekChemicalHelper.sampleFromItemStack(stack);
            String gasId = MekChemicalHelper.getRegistryName(gas);
            if (gasId != null && !gasId.isBlank()) {
                draftEntry.type = ShopEntry.EntryType.GAS;
                draftEntry.gas = gasId;
                draftEntry.item = null;
                draftEntry.fluid = null;
                draftEntry.other = null;
                if (draftEntry.amount < 1000) {
                    draftEntry.amount = 1000;
                }
                autosaveCurrentForm(true);
                rebuild();
            }
        }
    }

    private void cycleType() {
        if (draftEntry == null) {
            return;
        }
        flushFormToDraft();
        ShopEntry.EntryType cur = draftEntry.type != null ? draftEntry.type : ShopEntry.EntryType.ITEM;
        draftEntry.type = switch (cur) {
            case ITEM -> ShopEntry.EntryType.FLUID;
            case FLUID -> MekChemicalHelper.isGasSupportEnabled() ? ShopEntry.EntryType.GAS : ShopEntry.EntryType.OTHER;
            case GAS -> ShopEntry.EntryType.OTHER;
            case OTHER -> ShopEntry.EntryType.ITEM;
        };
        if (draftEntry.type == ShopEntry.EntryType.OTHER
                && (draftEntry.other == null || draftEntry.other.isBlank())) {
            draftEntry.other = ShopOtherRegistry.RF_ID;
        }
        applyResourceString(resourceString(draftEntry));
        if (typeButton != null) {
            typeButton.setMessage(Component.literal(typeLabel()));
        }
        autosaveCurrentForm(true);
        rebuild();
    }

    private String typeLabel() {
        ShopEntry.EntryType t = draftEntry != null && draftEntry.type != null ? draftEntry.type : ShopEntry.EntryType.ITEM;
        return t.name();
    }

    private void cycleCurrency() {
        List<ShopCurrency> list = sortedCurrencies();
        if (list.isEmpty() || draftEntry == null) {
            return;
        }
        flushFormToDraft();
        int idx = 0;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(draftEntry.currency)) {
                idx = i;
                break;
            }
        }
        idx = (idx + 1) % list.size();
        draftEntry.currency = list.get(idx).id;
        draftEntry.valute = draftEntry.currency;
        if (currencyButton != null) {
            currencyButton.setMessage(Component.literal(draftEntry.currency));
        }
        autosaveCurrentForm(true);
    }

    private void addStageFromDraft() {
        applyStageDraft();
    }

    private static String nextStageType(String current) {
        return switch (current == null ? "world" : current) {
            case "world" -> "player";
            case "player" -> "team";
            default -> "world";
        };
    }

    private List<ShopCategory> sortedCategories() {
        return menu.getData().categories.values().stream()
                .sorted(Comparator.comparingInt((ShopCategory c) -> -c.priority).thenComparing(c -> c.id))
                .toList();
    }

    private List<ShopCurrency> sortedCurrencies() {
        return ShopCurrency.sorted(menu.getData().currencies.values());
    }

    private List<ShopEntry> entriesInCategory(@Nullable String categoryId) {
        String cat = categoryId != null ? categoryId : "";
        return menu.getData().entries.values().stream()
                .filter(e -> cat.equals(e.inCategory))
                .sorted(Comparator.comparingInt((ShopEntry e) -> -e.priority).thenComparing(e -> e.id))
                .toList();
    }

    private String firstCurrencyId() {
        return sortedCurrencies().stream().map(c -> c.id).findFirst().orElse("null_coin");
    }

    private String uniqueId(String prefix) {
        String base = prefix + "_" + System.currentTimeMillis() % 100000;
        int n = 0;
        String id = base;
        while (menu.getData().categories.containsKey(id)
                || menu.getData().currencies.containsKey(id)
                || menu.getData().entries.containsKey(id)) {
            id = base + "_" + (++n);
        }
        return id;
    }

    /** Turns {@code category_6545} into {@code Category 6545} for default display names. */
    private static String friendlyNameFromId(String id) {
        if (id == null || id.isBlank()) {
            return "";
        }
        String[] parts = id.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            if (i == 0) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1));
                }
            } else {
                sb.append(part);
            }
        }
        return sb.toString();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0.0F, 0.0F, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
        if (dialog == Dialog.NONE && isListView()) {
            renderListScrollbar(graphics, mouseX, mouseY);
            renderListRowVisuals(graphics);
        }
        if ((subView == SubView.CATEGORY_EDIT || subView == SubView.ENTRY_EDIT) && dialog == Dialog.NONE) {
            int slotY = subView == SubView.ENTRY_EDIT ? 53 : 133;
            int slotX = leftPos + resourceSlotX();
            int iconY = topPos + slotY + 1;
            graphics.blit(RenderPipelines.GUI_TEXTURED, SINGLE_SLOT_TEXTURE, slotX - 1, topPos + slotY, 0.0F, 0.0F, 18, 18, 18, 18);
            if (subView == SubView.ENTRY_EDIT && draftEntry != null
                    && draftEntry.type == ShopEntry.EntryType.FLUID) {
                FluidStack fluid = ShopEntryHelper.displayFluidForEntry(draftEntry);
                if (!fluid.isEmpty()) {
                    GuiFluidStillBlit.blit16(graphics, fluid, slotX, iconY);
                }
            } else if (subView == SubView.ENTRY_EDIT && draftEntry != null
                    && draftEntry.type == ShopEntry.EntryType.GAS) {
                Object gas = ShopEntryHelper.displayGasForEntry(draftEntry);
                if (gas != null) {
                    GuiChemicalStillBlit.blit16(graphics, gas, slotX, iconY);
                }
            } else if (subView == SubView.ENTRY_EDIT && draftEntry != null
                    && draftEntry.type == ShopEntry.EntryType.OTHER) {
                ShopOtherRegistry.Definition definition = ShopOtherRegistry.get(draftEntry.other);
                if (definition != null) {
                    graphics.blit(RenderPipelines.GUI_TEXTURED, definition.icon(),
                            slotX, iconY, 0.0F, 0.0F, 16, 16, 16, 16);
                }
            } else {
                ItemStack ghost = menu.getGhostStack();
                if (!ghost.isEmpty()) {
                    graphics.item(ghost, slotX, iconY);
                    graphics.itemDecorations(this.font, ghost, slotX, iconY);
                }
            }
        }
        if (subView == SubView.ENTRY_STAGES && dialog == Dialog.NONE) {
            renderStageScrollbar(graphics, mouseX, mouseY);
        }
    }

    private void renderListRowVisuals(GuiGraphicsExtractor graphics) {
        for (ListRowVisual visual : listRowVisuals) {
            int rowY = ENTRY_START_Y + visual.rowIndex() * ENTRY_HEIGHT;
            int x = leftPos + ENTRY_START_X;
            int y = topPos + rowY + (ENTRY_HEIGHT - 2 - LIST_ICON_SIZE) / 2;
            if (visual.kind() == ListRowKind.SYMBOL_CELL) {
                renderSymbolCell(graphics, x, y, visual.symbol());
                continue;
            }
            graphics.blit(RenderPipelines.GUI_TEXTURED, SINGLE_SLOT_TEXTURE, x, y, 0.0F, 0.0F, LIST_ICON_SIZE, LIST_ICON_SIZE, LIST_ICON_SIZE, LIST_ICON_SIZE);
            int iconX = x + 1;
            int iconY = y + 1;
            if (visual.kind() == ListRowKind.ENTRY_SLOT && visual.entry() != null) {
                renderEntryListIcon(graphics, visual.entry(), iconX, iconY);
            } else if (visual.itemSelector() != null) {
                ItemStack icon = ShopEntryHelper.displayStackForItemSelector(visual.itemSelector(), 1);
                if (!icon.isEmpty()) {
                    graphics.item(icon, iconX, iconY);
                    graphics.itemDecorations(this.font, icon, iconX, iconY);
                }
            } else {
                ItemStack icon = visual.icon();
                if (icon != null && !icon.isEmpty()) {
                    graphics.item(icon, iconX, iconY);
                    graphics.itemDecorations(this.font, icon, iconX, iconY);
                }
            }
        }
    }

    private void renderEntryListIcon(GuiGraphicsExtractor graphics, ShopEntry entry, int iconX, int iconY) {
        switch (entry.type != null ? entry.type : ShopEntry.EntryType.ITEM) {
            case ITEM -> {
                ItemStack stack = ShopEntryHelper.displayStackForEntry(entry);
                if (!stack.isEmpty()) {
                    graphics.item(stack, iconX, iconY);
                    graphics.itemDecorations(this.font, stack, iconX, iconY);
                }
            }
            case FLUID -> {
                FluidStack fluid = ShopEntryHelper.displayFluidForEntry(entry);
                if (!fluid.isEmpty()) {
                    GuiFluidStillBlit.blit16(graphics, fluid, iconX, iconY);
                }
            }
            case GAS -> {
                Object gas = ShopEntryHelper.displayGasForEntry(entry);
                if (gas != null) {
                    GuiChemicalStillBlit.blit16(graphics, gas, iconX, iconY);
                }
            }
            case OTHER -> {
                ShopOtherRegistry.Definition definition = ShopOtherRegistry.get(entry.other);
                if (definition != null) {
                    graphics.blit(RenderPipelines.GUI_TEXTURED, definition.icon(),
                            iconX, iconY, 0.0F, 0.0F, 16, 16, 16, 16);
                }
            }
        }
    }

    /** Neutral channel-style cell (Another Dynamics letter box look, no palette colors). */
    private void renderSymbolCell(GuiGraphicsExtractor graphics, int x, int y, @Nullable String symbol) {
        int bg = 0xFF404040;
        int border = 0xFF303030;
        graphics.fill(x + 1, y + 1, x + LIST_ICON_SIZE - 1, y + LIST_ICON_SIZE - 1, bg);
        graphics.fill(x, y, x + LIST_ICON_SIZE, y + 1, border);
        graphics.fill(x, y + LIST_ICON_SIZE - 1, x + LIST_ICON_SIZE, y + LIST_ICON_SIZE, border);
        graphics.fill(x, y, x + 1, y + LIST_ICON_SIZE, border);
        graphics.fill(x + LIST_ICON_SIZE - 1, y, x + LIST_ICON_SIZE, y + LIST_ICON_SIZE, border);
        if (symbol == null || symbol.isBlank()) {
            return;
        }
        Component label = Component.literal(shortSymbolLabel(symbol));
        int lw = font.width(label);
        graphics.text(this.font, label, x + (LIST_ICON_SIZE - lw) / 2, y + (LIST_ICON_SIZE - 8) / 2, 0xFFFFFFFF, false);
    }

    private static String shortSymbolLabel(String symbol) {
        int[] cps = symbol.codePoints().limit(2).toArray();
        return new String(cps, 0, cps.length);
    }

    private void renderListScrollbar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (listTotalCount() <= MAX_VISIBLE) {
            return;
        }
        int upY = ENTRY_START_Y;
        int barY = ENTRY_START_Y + HANDLE_SIZE;
        int downY = barY + SCROLLBAR_HEIGHT;
        int sx = leftPos + SCROLLBAR_X;
        graphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, sx, topPos + barY, 0.0F, 0.0F, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT, 32, 34);
        boolean upHover = mouseX >= sx && mouseX < sx + SCROLLBAR_WIDTH
                && mouseY >= topPos + upY && mouseY < topPos + upY + HANDLE_SIZE;
        graphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, sx, topPos + upY, (float)(SCROLLBAR_WIDTH * 2), (float)(upHover ? HANDLE_SIZE : 0), HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        boolean downHover = mouseX >= sx && mouseX < sx + SCROLLBAR_WIDTH
                && mouseY >= topPos + downY && mouseY < topPos + downY + HANDLE_SIZE;
        graphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, sx, topPos + downY, (float)(SCROLLBAR_WIDTH * 3), (float)(downHover ? HANDLE_SIZE : 0), HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        double ratio = maxListScroll() == 0 ? 0 : (double) scrollOffset / maxListScroll();
        int handleY = topPos + barY + (int) (ratio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
        boolean handleHover = mouseX >= sx && mouseX < sx + HANDLE_SIZE
                && mouseY >= handleY && mouseY < handleY + HANDLE_SIZE;
        graphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, sx, handleY, (float)SCROLLBAR_WIDTH, (float)(handleHover ? HANDLE_SIZE : 0), HANDLE_SIZE, HANDLE_SIZE, 32, 34);
    }

    private void renderStageScrollbar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (draftStages.size() <= MAX_VISIBLE_STAGES) {
            return;
        }
        int stageBarH = Math.max(HANDLE_SIZE, MAX_VISIBLE_STAGES * STAGE_ROW_HEIGHT - HANDLE_SIZE * 2);
        int upY = STAGE_LIST_Y;
        int barY = STAGE_LIST_Y + HANDLE_SIZE;
        int downY = barY + stageBarH;
        int sx = leftPos + STAGE_SCROLLBAR_X;
        graphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, sx, topPos + barY, 0.0F, 0.0F, SCROLLBAR_WIDTH, Math.min(SCROLLBAR_HEIGHT, stageBarH), 32, 34);
        boolean upHover = mouseX >= sx && mouseX < sx + SCROLLBAR_WIDTH
                && mouseY >= topPos + upY && mouseY < topPos + upY + HANDLE_SIZE;
        graphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, sx, topPos + upY, (float)(SCROLLBAR_WIDTH * 2), (float)(upHover ? HANDLE_SIZE : 0), HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        boolean downHover = mouseX >= sx && mouseX < sx + SCROLLBAR_WIDTH
                && mouseY >= topPos + downY && mouseY < topPos + downY + HANDLE_SIZE;
        graphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, sx, topPos + downY, (float)(SCROLLBAR_WIDTH * 3), (float)(downHover ? HANDLE_SIZE : 0), HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        double ratio = maxStageScroll() == 0 ? 0 : (double) stageScrollOffset / maxStageScroll();
        int handleY = topPos + barY + (int) (ratio * Math.max(1, stageBarH - HANDLE_SIZE));
        boolean handleHover = mouseX >= sx && mouseX < sx + HANDLE_SIZE
                && mouseY >= handleY && mouseY < handleY + HANDLE_SIZE;
        graphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, sx, handleY, (float)SCROLLBAR_WIDTH, (float)(handleHover ? HANDLE_SIZE : 0), HANDLE_SIZE, HANDLE_SIZE, 32, 34);
    }

    private int listTotalCount() {
        return switch (subView) {
            case CATEGORIES -> sortedCategories().size() + 1;
            case CURRENCIES -> sortedCurrencies().size() + 1;
            case ENTRIES -> entriesInCategory(selectedCategoryId).size() + 1;
            default -> 0;
        };
    }

    private Component currentTitle() {
        return switch (dialog) {
            case DELETE_CONFIRM -> Component.translatable("gui.iska_utils.shop_edit.confirm_delete_title");
            case RENAME_CONFIRM -> Component.translatable("gui.iska_utils.shop_edit.rename.title",
                    nullSafe(renameOldId), nullSafe(renameNewId));
            case CLOSE_HINT -> Component.translatable("gui.iska_utils.shop_edit.close_hint_title");
            default -> switch (subView) {
                case CATEGORIES -> Component.translatable("gui.iska_utils.shop_edit.categories");
                case CATEGORY_EDIT -> Component.translatable("gui.iska_utils.shop_edit.category_edit");
                case ENTRIES -> Component.translatable("gui.iska_utils.shop_edit.entries",
                        selectedCategoryId != null ? selectedCategoryId : "");
                case ENTRY_EDIT -> Component.translatable("gui.iska_utils.shop_edit.entry_edit");
                case ENTRY_STAGES -> Component.translatable("gui.iska_utils.shop_edit.stages_edit");
                case CURRENCIES -> Component.translatable("gui.iska_utils.shop_edit.currencies_title");
                case CURRENCY_EDIT -> Component.translatable("gui.iska_utils.shop_edit.currency_edit");
            };
        };
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Component title = currentTitle();
        int titleX = (GUI_WIDTH - font.width(title)) / 2;
        graphics.text(this.font, title, titleX, 6, GuiTextColors.TITLE, false);
        if (dialog == Dialog.CLOSE_HINT) {
            Component hint = Component.translatable("gui.iska_utils.shop_edit.close_reload_hint");
            int y = 50;
            for (var line : font.split(hint, GUI_WIDTH - 40)) {
                graphics.text(this.font, line, 20, y, GuiTextColors.BODY, false);
                y += 12;
            }
            return;
        }
        for (FormLabel label : formLabels) {
            graphics.text(this.font, label.text(), label.x(), label.y(), GuiTextColors.BODY, false);
        }
        Component warning = entryEditWarning();
        if (warning != null) {
            int warnY = 128;
            for (String part : warning.getString().split("\n", -1)) {
                if (part.isEmpty()) {
                    warnY += 10;
                    continue;
                }
                for (var line : font.split(Component.literal(part), FORM_WIDTH)) {
                    graphics.text(this.font, line, FORM_LEFT, warnY, GuiTextColors.ERROR, false);
                    warnY += 10;
                }
            }
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (dialog == Dialog.NONE && scrollY != 0) {
            if (isListView()) {
                int max = maxListScroll();
                scrollOffset = (int) Math.max(0, Math.min(max, scrollOffset - Math.signum(scrollY)));
                rebuild();
                return true;
            }
            if (subView == SubView.ENTRY_STAGES
                    && mouseX >= leftPos + STAGE_LIST_X
                    && mouseX < leftPos + STAGE_SCROLLBAR_X + SCROLLBAR_WIDTH
                    && mouseY >= topPos + STAGE_LIST_Y - 14
                    && mouseY < topPos + STAGE_LIST_Y + MAX_VISIBLE_STAGES * STAGE_ROW_HEIGHT + 16) {
                int max = maxStageScroll();
                stageScrollOffset = (int) Math.max(0, Math.min(max, stageScrollOffset - Math.signum(scrollY)));
                rebuild();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (MachineGuiInput.handleContainerKeyPressed(this, event,
                isDraggingHandle || isDraggingStageHandle, formBoxes.toArray(EditBox[]::new))) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (MachineGuiInput.clearEditBoxOnRightClick(mouseX, mouseY, button, formBoxes.toArray(EditBox[]::new))) {
            return true;
        }
        if (button == 0 && dialog == Dialog.NONE) {
            if (isListView() && handleListScrollbarClick(mouseX, mouseY)) {
                MachineGuiInput.markScrollbarPressed();
                return true;
            }
            if (subView == SubView.ENTRY_STAGES && handleStageScrollbarClick(mouseX, mouseY)) {
                MachineGuiInput.markScrollbarPressed();
                return true;
            }
            if ((subView == SubView.CATEGORY_EDIT || subView == SubView.ENTRY_EDIT)) {
                int slotY = subView == SubView.ENTRY_EDIT ? 54 : 134;
                int sx = leftPos + resourceSlotX();
                int sy = topPos + slotY;
                if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                    ItemStack carried = this.menu.getCarried();
                    if (!carried.isEmpty()) {
                        acceptGhostItem(carried.copyWithCount(1));
                        return true;
                    }
                    if (!menu.getGhostStack().isEmpty()) {
                        menu.setGhostStack(ItemStack.EMPTY);
                        if (resourceBox != null) {
                            resourceBox.setValue("");
                        }
                        applyResourceString("");
                        autosaveCurrentForm(true);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            isDraggingHandle = false;
            isDraggingStageHandle = false;
            MachineGuiInput.clearScrollbarPressed();
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseY = event.y();
        int button = event.button();
        if (button == 0 && isDraggingHandle && maxListScroll() > 0) {
            int barY = topPos + ENTRY_START_Y + HANDLE_SIZE;
            float ratio = (float) (mouseY - barY) / (SCROLLBAR_HEIGHT - HANDLE_SIZE);
            scrollOffset = Math.max(0, Math.min(maxListScroll(), Math.round(ratio * maxListScroll())));
            rebuild();
            return true;
        }
        if (button == 0 && isDraggingStageHandle && maxStageScroll() > 0) {
            int stageBarH = Math.max(HANDLE_SIZE, MAX_VISIBLE_STAGES * STAGE_ROW_HEIGHT - HANDLE_SIZE * 2);
            int barY = topPos + STAGE_LIST_Y + HANDLE_SIZE;
            float ratio = (float) (mouseY - barY) / Math.max(1, stageBarH - HANDLE_SIZE);
            stageScrollOffset = Math.max(0, Math.min(maxStageScroll(), Math.round(ratio * maxStageScroll())));
            rebuild();
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    private boolean handleListScrollbarClick(double mouseX, double mouseY) {
        if (listTotalCount() <= MAX_VISIBLE) {
            return false;
        }
        int sx = leftPos + SCROLLBAR_X;
        int upY = topPos + ENTRY_START_Y;
        int barY = topPos + ENTRY_START_Y + HANDLE_SIZE;
        int downY = barY + SCROLLBAR_HEIGHT;
        if (mouseX < sx || mouseX >= sx + SCROLLBAR_WIDTH) {
            return false;
        }
        if (mouseY >= upY && mouseY < upY + HANDLE_SIZE) {
            scrollOffset = Math.max(0, scrollOffset - 1);
            rebuild();
            return true;
        }
        if (mouseY >= downY && mouseY < downY + HANDLE_SIZE) {
            scrollOffset = Math.min(maxListScroll(), scrollOffset + 1);
            rebuild();
            return true;
        }
        if (mouseY >= barY && mouseY < barY + SCROLLBAR_HEIGHT) {
            isDraggingHandle = true;
            float ratio = (float) (mouseY - barY) / SCROLLBAR_HEIGHT;
            scrollOffset = Math.max(0, Math.min(maxListScroll(), Math.round(ratio * maxListScroll())));
            rebuild();
            return true;
        }
        return false;
    }

    private boolean handleStageScrollbarClick(double mouseX, double mouseY) {
        if (draftStages.size() <= MAX_VISIBLE_STAGES) {
            return false;
        }
        int sx = leftPos + STAGE_SCROLLBAR_X;
        int stageBarH = Math.max(HANDLE_SIZE, MAX_VISIBLE_STAGES * STAGE_ROW_HEIGHT - HANDLE_SIZE * 2);
        int upY = topPos + STAGE_LIST_Y;
        int barY = topPos + STAGE_LIST_Y + HANDLE_SIZE;
        int downY = barY + stageBarH;
        if (mouseX < sx || mouseX >= sx + SCROLLBAR_WIDTH) {
            return false;
        }
        if (mouseY >= upY && mouseY < upY + HANDLE_SIZE) {
            stageScrollOffset = Math.max(0, stageScrollOffset - 1);
            rebuild();
            return true;
        }
        if (mouseY >= downY && mouseY < downY + HANDLE_SIZE) {
            stageScrollOffset = Math.min(maxStageScroll(), stageScrollOffset + 1);
            rebuild();
            return true;
        }
        if (mouseY >= barY && mouseY < barY + stageBarH) {
            isDraggingStageHandle = true;
            float ratio = (float) (mouseY - barY) / stageBarH;
            stageScrollOffset = Math.max(0, Math.min(maxStageScroll(), Math.round(ratio * maxStageScroll())));
            rebuild();
            return true;
        }
        return false;
    }

    private void acceptGhostItem(ItemStack stack) {
        menu.setGhostStack(stack);
        String preferred = ShopEditResourceFormats.preferredFromStack(stack);
        setupResourceVariants(preferred);
        if (resourceBox != null) {
            resourceBox.setValue(preferred);
        }
        if (subView == SubView.ENTRY_EDIT && draftEntry != null
                && draftEntry.type != ShopEntry.EntryType.ITEM) {
            draftEntry.type = ShopEntry.EntryType.ITEM;
        }
        applyResourceString(preferred);
        autosaveCurrentForm(true);
        rebuild();
    }

    @Override
    public IGhostIngredientConsumer getGhostHandler() {
        if (dialog != Dialog.NONE) {
            return null;
        }
        if (subView != SubView.CATEGORY_EDIT && subView != SubView.ENTRY_EDIT) {
            return null;
        }
        return new IGhostIngredientConsumer() {
            @Override
            public Object supportedTarget(Object ingredient) {
                if (ingredient instanceof ItemStack stack && !stack.isEmpty()) {
                    return stack;
                }
                if (subView == SubView.ENTRY_EDIT && ShopEntryHelper.isFluidIngredient(ingredient)) {
                    return ShopEntryHelper.normalizeFluidIngredient((FluidStack) ingredient);
                }
                if (subView == SubView.ENTRY_EDIT
                        && MekChemicalHelper.isLoaded()
                        && MekChemicalHelper.isGasSupportEnabled()
                        && MekChemicalHelper.isChemicalStackObject(ingredient)
                        && !MekChemicalHelper.isEmpty(ingredient)) {
                    return ingredient;
                }
                return null;
            }

            @Override
            public void accept(Object ingredient) {
                if (ingredient instanceof ItemStack stack) {
                    acceptGhostItem(stack.copyWithCount(1));
                } else if (ingredient instanceof FluidStack fluid && draftEntry != null) {
                    FluidStack norm = ShopEntryHelper.normalizeFluidIngredient(fluid);
                    Identifier id = BuiltInRegistries.FLUID.getKey(norm.getFluid());
                    draftEntry.type = ShopEntry.EntryType.FLUID;
                    draftEntry.fluid = id != null ? id.toString() : "minecraft:water";
                    draftEntry.item = null;
                    draftEntry.gas = null;
                    autosaveCurrentForm(true);
                    rebuild();
                } else if (MekChemicalHelper.isLoaded() && MekChemicalHelper.isChemicalStackObject(ingredient)
                        && draftEntry != null) {
                    String gasId = MekChemicalHelper.getRegistryName(ingredient);
                    if (gasId != null) {
                        draftEntry.type = ShopEntry.EntryType.GAS;
                        draftEntry.gas = gasId;
                        draftEntry.item = null;
                        draftEntry.fluid = null;
                        autosaveCurrentForm(true);
                        rebuild();
                    }
                }
            }
        };
    }

    @Override
    public Rect2i getGhostTargetArea() {
        if (dialog != Dialog.NONE) {
            return null;
        }
        if (subView == SubView.ENTRY_EDIT) {
            return new Rect2i(leftPos + resourceSlotX() - 1, topPos + 53, 18, 18);
        }
        if (subView == SubView.CATEGORY_EDIT) {
            return new Rect2i(leftPos + resourceSlotX() - 1, topPos + 133, 18, 18);
        }
        return null;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static String nullSafe(@Nullable String s) {
        return s != null ? s : "";
    }

    /** Localized name from lang key when available; otherwise the raw key/literal. */
    private static String displayName(@Nullable String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        return Component.translatable(name).getString();
    }

    /** Item/fluid/gas id or tag shown in the entries list (not the shop entry id). */
    private static String entryContentLabel(ShopEntry entry) {
        String selector = ShopEntryHelper.resourceSelector(entry);
        return selector != null ? selector.trim() : "";
    }

    private static String formatNum(double v) {
        if (Math.rint(v) == v) {
            return String.valueOf((long) v);
        }
        return String.valueOf(v);
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static double parseDouble(String s, double def) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return def;
        }
    }
}
