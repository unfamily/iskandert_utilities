package net.unfamily.iskautils.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.DeepDrawerExtractorBlockEntity;
import net.unfamily.iskautils.integration.anotherdynamics.client.DeepDrawerSettingsCopierClient;
import net.unfamily.iskautils.integration.anotherdynamics.AnotherDynamicsCompat;
import net.unfamily.iskautils.network.ModMessages;
import net.unfamily.iskautils.network.packet.DeepDrawerExtractorSettingsCopierC2SPacket;
import net.unfamily.iskautils.integration.jei.ghost.IIskaUtilsGhostTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Screen for Deep Drawer Extractor GUI
 * Shows scrollable EditBoxes for infinite filter fields, allow/deny button, and help text
 */
public class DeepDrawerExtractorScreen extends AbstractContainerScreen<DeepDrawerExtractorMenu>
    implements IIskaUtilsGhostTarget {
    
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DeepDrawerExtractorScreen.class);
    
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/backgrounds/deep_drawer_extractor.png");
    private static final Identifier BACKGROUND_EMPTY = Identifier.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/backgrounds/deep_drawer_extractor_empty.png");
    
    // Scrollbar texture (identica a DeepDrawersScreen)
    private static final Identifier SCROLLBAR_TEXTURE = Identifier.fromNamespaceAndPath("iska_utils", "textures/gui/scrollbar.png");
    // Wide entry texture for filter entries
    private static final Identifier ENTRY_TEXTURE = Identifier.fromNamespaceAndPath("iska_utils", "textures/gui/entry_wide.png");
    // Single slot texture for item display
    private static final Identifier SINGLE_SLOT_TEXTURE = Identifier.fromNamespaceAndPath("iska_utils", "textures/gui/single_slot.png");
    
    // GUI dimensions (based on image: 400x250)
    private static final int GUI_WIDTH = 400;
    private static final int GUI_HEIGHT = 250;  
    
    // Title position
    private static final int TITLE_Y = 8;
    
    // Close button
    private Button closeButton;
    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;

    private enum SubView {
        MAIN,
        DENY_FILTERS,
        ALLOW_FILTERS,
        HOW_TO_USE
    }

    private SubView subView = SubView.MAIN;
    private SubView filterListBeforeHelp = SubView.MAIN;
    private boolean pendingFilterSubviewRestore = true;
    
    // Filter list on the left (original Deep Drawer layout)
    private static final int ENTRY_X = 8;
    private static final int ENTRY_WIDTH = 140;
    private static final int ENTRY_HEIGHT = 24;
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int FILTERS_LABEL_Y = 30;
    private static final int FIRST_ROW_Y = FILTERS_LABEL_Y + 12;
    private static final int ENTRY_SPACING = 0;
    /** Menu-relative Y of buffer row (must match {@link DeepDrawerExtractorMenu} addBufferSlots). */
    private static final int BUFFER_SLOTS_Y_MENU = 223;
    private static final int BUFFER_SLOTS_FIRST_X = 32;
    private static final int BUFFER_SLOT_COUNT = 5;
    private static final int BUFFER_SLOT_STEP = 18;
    private static final int MAX_FILTER_SLOTS = net.unfamily.iskautils.Config.deepDrawerExtractorMaxFilters;
    private static final int VISIBLE_ENTRIES = 7;
    
    // Scrollbar constants
    private static final int SCROLLBAR_HEIGHT = 34;
    private static final int HANDLE_SIZE = 8;
    private static final int SCROLLBAR_X = ENTRY_X + ENTRY_WIDTH + 4;
    private static final int BUTTON_UP_Y = FIRST_ROW_Y;
    private static final int SCROLLBAR_Y = BUTTON_UP_Y + HANDLE_SIZE;
    private static final int BUTTON_DOWN_Y = SCROLLBAR_Y + SCROLLBAR_HEIGHT;
    
    // Top-right row: Allow | <<<<>>>> | Deny | Redstone
    private static final int TOP_ROW_Y = FIRST_ROW_Y;
    private static final int TOP_ROW_SPACING = 4;
    private static final int NAV_BTN_HEIGHT = 16;
    private static final int REDSTONE_BUTTON_SIZE = 16;
    private static final int REDSTONE_GUI_X = GUI_WIDTH - 8 - REDSTONE_BUTTON_SIZE;
    /** First X for nav text buttons — right of filter scrollbar column. */
    private static final int NAV_ROW_LEFT = SCROLLBAR_X + SCROLLBAR_WIDTH + TOP_ROW_SPACING;
    private static final int NAV_ROW_RIGHT = REDSTONE_GUI_X - TOP_ROW_SPACING;
    private static final int NAV_TEXT_BTN_WIDTH = (NAV_ROW_RIGHT - NAV_ROW_LEFT - 2 * TOP_ROW_SPACING) / 3;
    // AD-style order: Deny | inverter | Allow
    private static final int DENY_NAV_X = NAV_ROW_LEFT;
    private static final int LIST_LOGIC_X = DENY_NAV_X + NAV_TEXT_BTN_WIDTH + TOP_ROW_SPACING;
    private static final int ALLOW_NAV_X = LIST_LOGIC_X + NAV_TEXT_BTN_WIDTH + TOP_ROW_SPACING;
    
    // How to use back button
    private static final int BACK_BUTTON_X = 8;
    private static final int BACK_BUTTON_Y = GUI_HEIGHT - 25;
    
    private Button validKeysButton;
    private Button denyNavButton;
    private Button allowNavButton;
    private Button listLogicButton;
    private Button backButton;

    private ItemIconButton redstoneModeButton;
    
    // Cached filter fields for rendering (synced from server) - dynamic list
    private java.util.List<String> cachedFilterFields = new java.util.ArrayList<>();
    // Cached inverted filter fields for rendering (synced from server) - dynamic list
    private java.util.List<String> cachedInvertedFilterFields = new java.util.ArrayList<>();
    private java.util.List<Integer> cachedConcatFields = new java.util.ArrayList<>();

    private Button settingsCopierSaveButton;
    private Button settingsCopierLoadButton;
    private final java.util.List<DeepDrawerConcatChannelButton> concatButtons = new java.util.ArrayList<>();

    // Scroll state for filter EditBoxes (identical to DeepDrawersScreen)
    private int filterScrollOffset = 0;
    private boolean isDraggingHandle = false;
    private int dragStartY = 0;
    private int dragStartScrollOffset = 0;
    
    // EditBox for editing filter (shown when clicking on an entry)
    private EditBox editingEditBox = null;
    private int editingFilterIndex = -1;
    
    // Edit mode: tracks which filter index is in edit mode (shows different view)
    private int editModeFilterIndex = -1; // -1 means no entry is in edit mode
    
    // Edit buttons for each visible entry (recreated on scroll)
    private final java.util.List<Button> editButtons = new java.util.ArrayList<>();
    // Delete buttons (X) for each visible entry (recreated on scroll)
    private final java.util.List<Button> deleteButtons = new java.util.ArrayList<>();
    
    // Edit mode UI elements
    private ItemStack ghostSlotItem = ItemStack.EMPTY; // Ghost slot item (copy, doesn't consume)
    private EditBox editModeTextBox = null; // Textbox that appears in edit mode
    private Button leftArrowButton = null; // Left arrow button
    private Button rightArrowButton = null; // Right arrow button
    private Button editModeCloseButton = null; // X button to close edit mode
    private Button editModeClearButton = null; // C button to clear textbox
    private Button editModeApplyButton = null; // A button to apply changes
    private String originalFilterValue = ""; // Original filter value when entering edit mode (to restore if cancelled)
    private java.util.List<String> filterVariants = new java.util.ArrayList<>(); // All possible filter variants for current item
    private int currentFilterVariantIndex = 0; // Current index in filterVariants list

    @Override
    public IGhostIngredientConsumer getGhostHandler() {
        return new IGhostItemConsumer() {
            @Override
            public void accept(Object ingredient) {
                if (ingredient instanceof ItemStack stack) {
                    acceptJeiGhostItem(stack);
                }
            }
        };
    }

    @Override
    public Rect2i getGhostTargetArea() {
        if (editModeFilterIndex < 0) {
            return null;
        }
        int slotX = this.leftPos + DeepDrawerExtractorMenu.EDIT_MODE_GHOST_SLOT_X;
        int slotY = this.topPos + DeepDrawerExtractorMenu.EDIT_MODE_PANEL_Y;
        return new Rect2i(slotX, slotY, DeepDrawerExtractorMenu.EDIT_SLOT_SIZE, DeepDrawerExtractorMenu.EDIT_SLOT_SIZE);
    }

    private void acceptJeiGhostItem(ItemStack stack) {
        if (editModeFilterIndex < 0 || stack == null || stack.isEmpty()) {
            return;
        }
        ghostSlotItem = stack.copy();
        filterVariants = generateAllFilterVariants(stack);
        currentFilterVariantIndex = 0;
        if (editModeTextBox != null && !filterVariants.isEmpty()) {
            editModeTextBox.setValue(filterVariants.get(0));
        }
    }
    
    public DeepDrawerExtractorScreen(DeepDrawerExtractorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GUI_WIDTH, GUI_HEIGHT);
        this.inventoryLabelY = 10000;
    }
    
    @Override
    protected void init() {
        super.init();

        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = TITLE_Y;

        cachedFilterFields.clear();
        cachedInvertedFilterFields.clear();

        closeButton = Button.builder(Component.literal("✕"), button -> onCloseButtonClicked())
                .bounds(this.leftPos + CLOSE_BUTTON_X, this.topPos + CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
                .build();
        addRenderableWidget(closeButton);

        allowNavButton = Button.builder(Component.translatable("gui.iska_utils.deep_drawer_extractor.filters.allow"),
                        button -> openFilterSubview(SubView.ALLOW_FILTERS))
                .bounds(this.leftPos + ALLOW_NAV_X, this.topPos + TOP_ROW_Y, NAV_TEXT_BTN_WIDTH, NAV_BTN_HEIGHT)
                .build();
        addRenderableWidget(allowNavButton);

        listLogicButton = Button.builder(listLogicButtonMessage(denyOverridesAllow()),
                        button -> onListLogicButtonClicked())
                .bounds(this.leftPos + LIST_LOGIC_X, this.topPos + TOP_ROW_Y, NAV_TEXT_BTN_WIDTH, NAV_BTN_HEIGHT)
                .build();
        listLogicButton.setTooltip(listLogicButtonTooltip());
        addRenderableWidget(listLogicButton);

        denyNavButton = Button.builder(Component.translatable("gui.iska_utils.deep_drawer_extractor.filters.deny"),
                        button -> openFilterSubview(SubView.DENY_FILTERS))
                .bounds(this.leftPos + DENY_NAV_X, this.topPos + TOP_ROW_Y, NAV_TEXT_BTN_WIDTH, NAV_BTN_HEIGHT)
                .build();
        addRenderableWidget(denyNavButton);

        validKeysButton = Button.builder(Component.translatable("gui.iska_utils.deep_drawer_extractor.how_to_use"),
                        button -> openHowToUse())
                .bounds(0, 0, NAV_TEXT_BTN_WIDTH, NAV_BTN_HEIGHT)
                .build();
        addRenderableWidget(validKeysButton);

        redstoneModeButton = addRenderableWidget(MachineGuiButtons.redstoneIconButton(
                this.leftPos + DeepDrawerExtractorMenu.REDSTONE_GUI_X,
                this.topPos + DeepDrawerExtractorMenu.FIRST_ROW_Y,
                b -> onRedstoneModePressed(false),
                menu::getRedstoneMode,
                true));

        backButton = Button.builder(Component.translatable("gui.iska_utils.deep_drawer_extractor.back"),
                        button -> handleCloseOrBack())
                .bounds(this.leftPos + BACK_BUTTON_X, this.topPos + BACK_BUTTON_Y, NAV_TEXT_BTN_WIDTH, NAV_BTN_HEIGHT)
                .build();
        addRenderableWidget(backButton);

        applySubViewVisibility();
        tryRestoreSavedFilterSubview();
        initSettingsCopierButtons();
    }

    private void initSettingsCopierButtons() {
        if (!AnotherDynamicsCompat.isLoaded() || !menu.includesCopierSlot()) {
            return;
        }
        int colX = this.leftPos + DeepDrawerExtractorMenu.COPIER_COLUMN_X;
        settingsCopierSaveButton = Button.builder(
                        Component.translatable("gui.iska_utils.deep_drawer_extractor.settings_copier.copy"),
                        b -> sendSettingsCopierAction(DeepDrawerExtractorSettingsCopierC2SPacket.ACTION_COPY))
                .tooltip(Tooltip.create(DeepDrawerExtractorGuiTooltips.grayLine(
                        "gui.iska_utils.deep_drawer_extractor.settings_copier.copy.tooltip")))
                .bounds(colX, this.topPos + DeepDrawerExtractorMenu.COPIER_SAVE_BUTTON_Y,
                        DeepDrawerExtractorMenu.COPIER_ACTION_BUTTON_W, DeepDrawerExtractorMenu.COPIER_ACTION_BUTTON_H)
                .build();
        settingsCopierLoadButton = Button.builder(
                        Component.translatable("gui.iska_utils.deep_drawer_extractor.settings_copier.paste"),
                        b -> sendSettingsCopierAction(DeepDrawerExtractorSettingsCopierC2SPacket.ACTION_PASTE))
                .tooltip(Tooltip.create(DeepDrawerExtractorGuiTooltips.grayLine(
                        "gui.iska_utils.deep_drawer_extractor.settings_copier.paste.tooltip")))
                .bounds(colX, this.topPos + DeepDrawerExtractorMenu.COPIER_LOAD_BUTTON_Y,
                        DeepDrawerExtractorMenu.COPIER_ACTION_BUTTON_W, DeepDrawerExtractorMenu.COPIER_ACTION_BUTTON_H)
                .build();
        addRenderableWidget(settingsCopierSaveButton);
        addRenderableWidget(settingsCopierLoadButton);
        refreshCopierPasteUi();
    }

    private void sendSettingsCopierAction(int action) {
        BlockPos blockPos = resolveMachinePos();
        if (blockPos.equals(BlockPos.ZERO)) {
            return;
        }
        int allowDeny = isDenyPanelActive()
                ? DeepDrawerExtractorSettingsCopierC2SPacket.LIST_DENY
                : DeepDrawerExtractorSettingsCopierC2SPacket.LIST_ALLOW;
        saveFilterData();
        ModMessages.sendDeepDrawerExtractorSettingsCopierPacket(blockPos, action, allowDeny);
        menu.updateCachedFilters();
        updateCachedFiltersForMode();
        updateEditButtons();
        refreshCopierPasteUi();
    }

    private void refreshCopierPasteUi() {
        if (settingsCopierLoadButton == null) {
            return;
        }
        boolean allowPaste = menu.copySettingsSlotIndex() >= 0;
        settingsCopierLoadButton.active = allowPaste;
    }

    private void layoutSettingsCopierButtons() {
        if (settingsCopierSaveButton == null || settingsCopierLoadButton == null) {
            return;
        }
        int colX = this.leftPos + DeepDrawerExtractorMenu.COPIER_COLUMN_X;
        settingsCopierSaveButton.setX(colX);
        settingsCopierSaveButton.setY(this.topPos + DeepDrawerExtractorMenu.COPIER_SAVE_BUTTON_Y);
        settingsCopierSaveButton.setWidth(DeepDrawerExtractorMenu.COPIER_ACTION_BUTTON_W);
        settingsCopierSaveButton.setHeight(DeepDrawerExtractorMenu.COPIER_ACTION_BUTTON_H);
        settingsCopierLoadButton.setX(colX);
        settingsCopierLoadButton.setY(this.topPos + DeepDrawerExtractorMenu.COPIER_LOAD_BUTTON_Y);
        settingsCopierLoadButton.setWidth(DeepDrawerExtractorMenu.COPIER_ACTION_BUTTON_W);
        settingsCopierLoadButton.setHeight(DeepDrawerExtractorMenu.COPIER_ACTION_BUTTON_H);
    }

    private boolean showsSettingsCopierColumn() {
        return AnotherDynamicsCompat.isLoaded() && menu.includesCopierSlot() && isFilterListOpen();
    }

    private void tryRestoreSavedFilterSubview() {
        if (!pendingFilterSubviewRestore || subView != SubView.MAIN) {
            return;
        }
        SubView saved = menu.getLastFilterPanel() == DeepDrawerExtractorBlockEntity.FILTER_PANEL_DENY
                ? SubView.DENY_FILTERS
                : SubView.ALLOW_FILTERS;
        subView = saved;
        filterListBeforeHelp = saved;
        filterScrollOffset = 0;
        updateCachedFiltersForMode();
        applySubViewVisibility();
        updateEditButtons();
        pendingFilterSubviewRestore = false;
    }

    private void persistFilterPanelPreference(SubView target) {
        if (target != SubView.ALLOW_FILTERS && target != SubView.DENY_FILTERS) {
            return;
        }
        BlockPos blockPos = resolveMachinePos();
        if (!blockPos.equals(BlockPos.ZERO)) {
            int panel = target == SubView.DENY_FILTERS
                    ? DeepDrawerExtractorBlockEntity.FILTER_PANEL_DENY
                    : DeepDrawerExtractorBlockEntity.FILTER_PANEL_ALLOW;
            ModMessages.sendDeepDrawerExtractorFilterPanelPacket(blockPos, panel);
        }
    }

    private boolean isFilterListOpen() {
        return subView == SubView.ALLOW_FILTERS || subView == SubView.DENY_FILTERS;
    }

    private boolean inEditMode() {
        return editModeFilterIndex >= 0;
    }

    /**
     * In AD, allow/deny lists are stable; inverter only changes precedence logic.
     * So we never swap which list a panel edits based on whitelist mode.
     */
    private boolean isDenyPanelActive() {
        return effectiveFilterLineSubview() == SubView.DENY_FILTERS;
    }

    /** Filter list subview owning allow/deny semantics (persists across Valid Keys overlay). */
    private SubView effectiveFilterLineSubview() {
        if (subView == SubView.HOW_TO_USE) {
            return filterListBeforeHelp;
        }
        return subView;
    }

    private int getFilterPanelScreenX() {
        return this.leftPos + ENTRY_X;
    }

    /** {@code true} when deny list wins over allow (AD {@code >>>>>} / denyOverridesAllow). */
    private boolean denyOverridesAllow() {
        return !menu.getWhitelistMode();
    }

    private static Component listLogicButtonMessage(boolean denyOverridesAllow) {
        return Component.translatable(
                denyOverridesAllow
                        ? "gui.iska_utils.deep_drawer_extractor.list_logic.label.deny_wins"
                        : "gui.iska_utils.deep_drawer_extractor.list_logic.label.allow_bypass");
    }

    private Tooltip listLogicButtonTooltip() {
        boolean denyOver = denyOverridesAllow();
        return Tooltip.create(Component.translatable(
                denyOver
                        ? "gui.iska_utils.deep_drawer_extractor.list_logic.tooltip.deny_wins"
                        : "gui.iska_utils.deep_drawer_extractor.list_logic.tooltip.allow_bypass"));
    }

    private void refreshListLogicButton() {
        if (listLogicButton == null) {
            return;
        }
        boolean denyOver = denyOverridesAllow();
        listLogicButton.setMessage(listLogicButtonMessage(denyOver));
        listLogicButton.setTooltip(listLogicButtonTooltip());
    }

    private BlockPos resolveMachinePos() {
        BlockPos machinePos = menu.getSyncedBlockPos();
        if (machinePos.equals(BlockPos.ZERO)) {
            machinePos = menu.getBlockPos();
        }
        if (machinePos.equals(BlockPos.ZERO) && this.minecraft != null && this.minecraft.level != null && this.minecraft.player != null) {
            BlockPos playerPos = this.minecraft.player.blockPosition();
            for (int x = -8; x <= 8; x++) {
                for (int y = -8; y <= 8; y++) {
                    for (int z = -8; z <= 8; z++) {
                        BlockPos searchPos = playerPos.offset(x, y, z);
                        if (this.minecraft.level.getBlockEntity(searchPos) instanceof DeepDrawerExtractorBlockEntity) {
                            return searchPos;
                        }
                    }
                }
            }
        }
        return machinePos;
    }

    private void openFilterSubview(SubView target) {
        if (inEditMode()) {
            return;
        }
        saveFilterData();
        subView = target;
        filterListBeforeHelp = target;
        filterScrollOffset = 0;
        updateCachedFiltersForMode();
        applySubViewVisibility();
        updateEditButtons();
        persistFilterPanelPreference(target);
    }

    private void closeFilterSubview() {
        saveFilterData();
        subView = SubView.MAIN;
        applySubViewVisibility();
        updateEditButtons();
    }

    private void openHowToUse() {
        if (editModeTextBox != null) {
            editModeTextBox.setFocused(false);
        }
        if (editingEditBox != null) {
            editingEditBox.setFocused(false);
        }
        filterListBeforeHelp = subView;
        subView = SubView.HOW_TO_USE;
        applySubViewVisibility();
    }

    private void closeHowToUse() {
        subView = filterListBeforeHelp;
        applySubViewVisibility();
        if (isFilterListOpen() || inEditMode()) {
            updateEditButtons();
        }
    }

    private void handleCloseOrBack() {
        if (subView == SubView.HOW_TO_USE) {
            closeHowToUse();
            return;
        }
        if (inEditMode()) {
            exitEditMode(true);
            return;
        }
        if (isFilterListOpen()) {
            closeFilterSubview();
            return;
        }
        onClose();
    }

    private void onCloseButtonClicked() {
        onClose();
    }

    private void onListLogicButtonClicked() {
        if (inEditMode()) {
            return;
        }
        BlockPos blockPos = resolveMachinePos();
        if (!blockPos.equals(BlockPos.ZERO)) {
            ModMessages.sendDeepDrawerExtractorModeTogglePacket(blockPos);
        }
    }

    private void applySubViewVisibility() {
        boolean howTo = subView == SubView.HOW_TO_USE;
        boolean filterList = isFilterListOpen();
        boolean edit = inEditMode();

        this.inventoryLabelY = 10000;

        if (denyNavButton != null) {
            denyNavButton.visible = !howTo;
            denyNavButton.active = !edit && subView != SubView.DENY_FILTERS;
        }
        if (allowNavButton != null) {
            allowNavButton.visible = !howTo;
            allowNavButton.active = !edit && subView != SubView.ALLOW_FILTERS;
        }
        if (listLogicButton != null) {
            listLogicButton.visible = !howTo;
            listLogicButton.active = !edit;
            refreshListLogicButton();
        }
        if (validKeysButton != null) {
            validKeysButton.visible = !howTo;
            validKeysButton.active = true;
            if (validKeysButton.visible) {
                layoutValidKeysButton();
            }
        }
        if (backButton != null) {
            backButton.visible = howTo;
        }
        if (redstoneModeButton != null) {
            redstoneModeButton.visible = !howTo;
        }

        boolean showEditChrome = edit && !howTo && isFilterListOpen();
        if (editModeTextBox != null) {
            editModeTextBox.visible = showEditChrome;
        }
        if (leftArrowButton != null) {
            leftArrowButton.visible = showEditChrome;
        }
        if (rightArrowButton != null) {
            rightArrowButton.visible = showEditChrome;
        }
        if (editModeClearButton != null) {
            editModeClearButton.visible = showEditChrome;
        }
        if (editModeApplyButton != null) {
            editModeApplyButton.visible = showEditChrome;
        }
        if (editModeCloseButton != null) {
            editModeCloseButton.visible = showEditChrome;
        }

        boolean showCopier = showsSettingsCopierColumn() && !howTo;
        if (settingsCopierSaveButton != null) {
            settingsCopierSaveButton.visible = showCopier;
        }
        if (settingsCopierLoadButton != null) {
            settingsCopierLoadButton.visible = showCopier;
            if (showCopier) {
                layoutSettingsCopierButtons();
                refreshCopierPasteUi();
            }
        }
        for (DeepDrawerConcatChannelButton btn : concatButtons) {
            if (btn != null) {
                btn.visible = filterList && !howTo;
            }
        }

        if (!filterList || howTo) {
            for (Button button : editButtons) {
                if (button != null) {
                    button.visible = false;
                }
            }
            for (Button button : deleteButtons) {
                if (button != null) {
                    button.visible = false;
                }
            }
        } else {
            updateEditButtons();
        }

        if (howTo && editingEditBox != null) {
            editingEditBox.visible = false;
        }
    }

    /** On the edit row; follows close button while editing. */
    private void layoutValidKeysButton() {
        if (validKeysButton == null || subView == SubView.HOW_TO_USE) {
            return;
        }
        validKeysButton.setWidth(NAV_TEXT_BTN_WIDTH);
        validKeysButton.setHeight(NAV_BTN_HEIGHT);
        if (inEditMode() && editModeCloseButton != null) {
            validKeysButton.setX(editModeCloseButton.getX() + editModeCloseButton.getWidth() + DeepDrawerExtractorMenu.EDIT_ROW_GAP);
            validKeysButton.setY(editModeCloseButton.getY() + (editModeCloseButton.getHeight() - NAV_BTN_HEIGHT) / 2);
        } else {
            validKeysButton.setX(this.leftPos + DeepDrawerExtractorMenu.validKeysButtonX());
            validKeysButton.setY(this.topPos + DeepDrawerExtractorMenu.validKeysButtonY(NAV_BTN_HEIGHT));
        }
    }

    private boolean isMouseOverConcatButton(double mouseX, double mouseY) {
        for (DeepDrawerConcatChannelButton btn : concatButtons) {
            if (!btn.visible || !btn.active) {
                continue;
            }
            if (mouseX >= btn.getX() && mouseX < btn.getX() + btn.getWidth()
                    && mouseY >= btn.getY() && mouseY < btn.getY() + btn.getHeight()) {
                return true;
            }
        }
        return false;
    }

    private boolean handleMouseClicked(double mouseX, double mouseY, int button) {
        // Check if clicking on an example in how to use mode
        if (button == 0 && subView == SubView.HOW_TO_USE) { // Left click in how to use mode
            for (ExampleData exampleData : exampleDataList) {
                int screenX = this.leftPos + exampleData.x;
                int screenY = this.topPos + exampleData.y;
                
                if (mouseX >= screenX && mouseX <= screenX + exampleData.width &&
                    mouseY >= screenY && mouseY <= screenY + HELP_TEXT_LINE_HEIGHT) {
                    
                    // Copy to clipboard
                    if (this.minecraft != null && this.minecraft.keyboardHandler != null) {
                        this.minecraft.keyboardHandler.setClipboard(exampleData.example);
                        playButtonSound();
                    }
                    return true;
                }
            }
        }
        
        if (button == 0 && isFilterListOpen()) {
            // Handle ghost slot click (if in edit mode) - prioritize this
            if (editModeFilterIndex >= 0) {
                int slotX = this.leftPos + DeepDrawerExtractorMenu.EDIT_MODE_GHOST_SLOT_X;
                int slotY = this.topPos + DeepDrawerExtractorMenu.EDIT_MODE_PANEL_Y;
                int slotSize = DeepDrawerExtractorMenu.EDIT_SLOT_SIZE;
                
                if (mouseX >= slotX && mouseX < slotX + slotSize &&
                    mouseY >= slotY && mouseY < slotY + slotSize) {
                    // Click is on ghost slot
                    handleGhostSlotClick();
                    return true;
                }
            }
            
            // Handle scrollbar clicks first (they have priority)
            if (handleScrollButtonClick(mouseX, mouseY)) {
                MachineGuiInput.markScrollbarPressed();
                return true;
            }
            
            // Handle handle drag start
            if (handleHandleClick(mouseX, mouseY)) {
                MachineGuiInput.markScrollbarPressed();
                return true;
            }
            
            // Handle scrollbar area clicks
            if (handleScrollbarClick(mouseX, mouseY)) {
                MachineGuiInput.markScrollbarPressed();
                return true;
            }
        }

        if (subView != SubView.HOW_TO_USE && button == 1
                && redstoneModeButton != null && redstoneModeButton.isMouseOver(mouseX, mouseY)) {
            onRedstoneModePressed(true);
            playButtonSound();
            return true;
        }
        
        // Handle clicks on filter entries — edit/delete widgets handle their own clicks
        if (subView != SubView.HOW_TO_USE && isFilterListOpen() && button == 0) {
            for (int i = 0; i < VISIBLE_ENTRIES; i++) {
                int filterIndex = filterScrollOffset + i;
                if (filterIndex >= MAX_FILTER_SLOTS) {
                    break;
                }
                
                int entryX = this.leftPos + ENTRY_X;
                int entryY = this.topPos + FIRST_ROW_Y + i * (ENTRY_HEIGHT + ENTRY_SPACING);
                
                if (mouseX >= entryX && mouseX < entryX + ENTRY_WIDTH &&
                    mouseY >= entryY && mouseY < entryY + ENTRY_HEIGHT) {
                    return false;
                }
            }
        }
        
        // Handle right click on entries (clear filter)
        if (subView != SubView.HOW_TO_USE && isFilterListOpen() && button == 1) {
            if (isMouseOverConcatButton(mouseX, mouseY)) {
                return false;
            }
            // First check if right click is on edit mode textbox
            if (editModeTextBox != null && editModeFilterIndex >= 0) {
                int textBoxX = editModeTextBox.getX();
                int textBoxY = editModeTextBox.getY();
                int textBoxWidth = editModeTextBox.getWidth();
                int textBoxHeight = editModeTextBox.getHeight();
                
                if (mouseX >= textBoxX && mouseX < textBoxX + textBoxWidth &&
                    mouseY >= textBoxY && mouseY < textBoxY + textBoxHeight) {
                    // Right click on edit mode textbox: clear it (don't save - user must click Apply)
                    editModeTextBox.setValue("");
                    editModeTextBox.setCursorPosition(0);
                    editModeTextBox.setHighlightPos(0);
                    // Clear ghost slot and variants
                    ghostSlotItem = ItemStack.EMPTY;
                    filterVariants.clear();
                    currentFilterVariantIndex = 0;
                    playButtonSound();
                    return true;
                }
            }
            
            // Then check entries
            for (int i = 0; i < VISIBLE_ENTRIES; i++) {
                int filterIndex = filterScrollOffset + i;
                if (filterIndex >= MAX_FILTER_SLOTS) {
                    break;
                }
                
                int entryX = this.leftPos + ENTRY_X;
                int entryY = this.topPos + FIRST_ROW_Y + i * (ENTRY_HEIGHT + ENTRY_SPACING);
                
                // Check if click is within entry bounds
                if (mouseX >= entryX && mouseX < entryX + ENTRY_WIDTH &&
                    mouseY >= entryY && mouseY < entryY + ENTRY_HEIGHT) {
                    
                    // Right click: clear filter
                    while (cachedFilterFields.size() < MAX_FILTER_SLOTS) {
                        cachedFilterFields.add("");
                    }
                    cachedFilterFields.set(filterIndex, "");
                    while (cachedConcatFields.size() <= filterIndex) {
                        cachedConcatFields.add(0);
                    }
                    cachedConcatFields.set(filterIndex, 0);
                    saveFilterData();
                    playButtonSound();
                    return true;
                }
            }
        }
        
        // Let the normal screen handler deal with widgets/slots.
        return false;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (handleMouseClicked(event.x(), event.y(), event.button())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
    
    private void playButtonSound() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private Component getFilterListHeaderLabel() {
        return Component.translatable(effectiveFilterLineSubview() == SubView.ALLOW_FILTERS
                ? "gui.iska_utils.deep_drawer_extractor.mode.allow"
                : "gui.iska_utils.deep_drawer_extractor.mode.deny");
    }

    private void updateCachedFiltersForMode() {
        cachedFilterFields = isDenyPanelActive()
                ? new java.util.ArrayList<>(menu.getCachedInvertedFilterFields())
                : new java.util.ArrayList<>(menu.getCachedFilterFields());
        cachedConcatFields = isDenyPanelActive()
                ? new java.util.ArrayList<>(menu.getCachedDenyConcatChannels())
                : new java.util.ArrayList<>(menu.getCachedAllowConcatChannels());
        while (cachedFilterFields.size() < MAX_FILTER_SLOTS) {
            cachedFilterFields.add("");
        }
        while (cachedFilterFields.size() > MAX_FILTER_SLOTS) {
            cachedFilterFields.remove(cachedFilterFields.size() - 1);
        }
        while (cachedConcatFields.size() < MAX_FILTER_SLOTS) {
            cachedConcatFields.add(0);
        }
        while (cachedConcatFields.size() > MAX_FILTER_SLOTS) {
            cachedConcatFields.remove(cachedConcatFields.size() - 1);
        }
    }
    
    private void onRedstoneModePressed(boolean backward) {
        BlockPos blockPos = resolveMachinePos();
        if (!blockPos.equals(BlockPos.ZERO)) {
            ModMessages.sendDeepDrawerExtractorRedstoneModePacket(blockPos, backward);
        }
        saveFilterData();
    }
    
    /**
     * Saves filter data to server
     * Always reads whitelist mode from synced ContainerData (not local state)
     */
    private void saveFilterData() {
        // Get the machine position from the menu (synced from server, like rotation)
        BlockPos machinePos = resolveMachinePos();
        if (!machinePos.equals(net.minecraft.core.BlockPos.ZERO)) {
            // Collect filter field values as index-value pairs (only non-empty filters)
            java.util.Map<Integer, String> filterMap = new java.util.HashMap<>();
            java.util.Map<Integer, Integer> concatMap = new java.util.HashMap<>();
            for (int i = 0; i < cachedFilterFields.size(); i++) {
                String filter = cachedFilterFields.get(i);
                if (filter != null && !filter.trim().isEmpty()) {
                    filterMap.put(i, filter.trim());
                    int ch = (i < cachedConcatFields.size() && cachedConcatFields.get(i) != null)
                            ? cachedConcatFields.get(i) : 0;
                    if (ch > 0) {
                        concatMap.put(i, ch);
                    }
                }
            }
            boolean currentWhitelistMode = menu.getWhitelistMode();
            if (isDenyPanelActive()) {
                ModMessages.sendDeepDrawerExtractorInvertedFilterUpdatePacket(machinePos, filterMap, concatMap);
            } else {
                ModMessages.sendDeepDrawerExtractorFilterUpdatePacket(machinePos, filterMap, concatMap, currentWhitelistMode);
            }
        }
    }
    
    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        // Align with leftPos/topPos so slots, widgets, and texture match (AbstractContainerScreen uses leftPos/topPos)
        Identifier backgroundTexture = subView == SubView.HOW_TO_USE ? BACKGROUND_EMPTY : BACKGROUND;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, backgroundTexture, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);
        
        if (subView != SubView.HOW_TO_USE) {
            Component bufferLabel = Component.translatable("gui.iska_utils.deep_drawer_extractor.buffer_label");
            int bufferLabelWidth = this.font.width(bufferLabel);
            int bufferRowPixelWidth = BUFFER_SLOT_COUNT * BUFFER_SLOT_STEP;
            int bufferCenterGuiX = BUFFER_SLOTS_FIRST_X + bufferRowPixelWidth / 2;
            int bufferLabelX = this.leftPos + bufferCenterGuiX - bufferLabelWidth / 2;
            int bufferLabelY = this.topPos + BUFFER_SLOTS_Y_MENU - this.font.lineHeight - 3;
            guiGraphics.text(this.font, bufferLabel, bufferLabelX, bufferLabelY, GuiTextColors.TITLE, false);
        }

        if (isFilterListOpen()) {
            Component filtersLabel = getFilterListHeaderLabel();
            int labelWidth = this.font.width(filtersLabel);
            int labelX = this.leftPos + ENTRY_X + (ENTRY_WIDTH - labelWidth) / 2;
            guiGraphics.text(this.font, filtersLabel, labelX, this.topPos + FILTERS_LABEL_Y, GuiTextColors.TITLE, false);
            renderFilterEntries(guiGraphics, mouseX, mouseY);
        }
        
        if (subView != SubView.HOW_TO_USE) {
            if (redstoneModeButton != null && redstoneModeButton.isMouseOver(mouseX, mouseY)) {
                MachineGuiButtons.renderTooltipLine(guiGraphics, font, mouseX, mouseY,
                        MachineGuiButtons.redstoneTooltip(menu.getRedstoneMode(), true));
            }
            if (isFilterListOpen()) {
                renderScrollbar(guiGraphics, mouseX, mouseY);
                renderSettingsCopierColumn(guiGraphics);
            }
            
            if (editModeFilterIndex >= 0) {
                renderEditModeUI(guiGraphics, mouseX, mouseY);
            }
        }
    }
    
    /**
     * Renders filter entries as wide entries with single slot
     */
    private void renderFilterEntries(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        // Ensure cachedFilterFields has all slots
        while (cachedFilterFields.size() < MAX_FILTER_SLOTS) {
            cachedFilterFields.add("");
        }
        
        // Render visible entries
        for (int i = 0; i < VISIBLE_ENTRIES; i++) {
            int filterIndex = filterScrollOffset + i;
            if (filterIndex >= MAX_FILTER_SLOTS) {
                break;
            }
            
            int entryX = this.leftPos + ENTRY_X;
            int entryY = this.topPos + FIRST_ROW_Y + i * (ENTRY_HEIGHT + ENTRY_SPACING);
            
            // Draw entry background
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ENTRY_TEXTURE, entryX, entryY, 0.0F, 0.0F, ENTRY_WIDTH, ENTRY_HEIGHT, ENTRY_WIDTH, ENTRY_HEIGHT);
            
            // Get filter value
            String filter = cachedFilterFields.get(filterIndex);
            if (filter == null) {
                filter = "";
            }
            
            // Render entry content (slot + text)
            renderFilterEntry(guiGraphics, entryX, entryY, filter, filterIndex, mouseX, mouseY);
        }
    }
    
    /**
     * Renders a single filter entry with slot and text
     */
    private void renderFilterEntry(GuiGraphicsExtractor guiGraphics, int entryX, int entryY, String filter, int filterIndex, int mouseX, int mouseY) {
        // Check if this entry is in edit mode
        boolean isEditMode = (editModeFilterIndex == filterIndex);
        
        // For now, render the same view regardless of edit mode
        // In future phases, this will render a different view when isEditMode is true
        
        // Slot position (3px from left edge, 3px from top)
        int slotX = entryX + 3;
        int slotY = entryY + 3;
        
        // Draw single slot
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SINGLE_SLOT_TEXTURE, slotX, slotY, 0.0F, 0.0F, 18, 18, 18, 18);
        
        // Get item to display based on filter type
        ItemStack displayItem = getDisplayItemForFilter(filter);
        if (!displayItem.isEmpty()) {
            guiGraphics.item(displayItem, slotX + 1, slotY + 1);
            guiGraphics.itemDecorations(this.font, displayItem, slotX + 1, slotY + 1);
        }
        
        // Text position (after slot + 6px margin)
        int textX = slotX + 18 + 6;
        int textY = entryY + (ENTRY_HEIGHT - this.font.lineHeight) / 2;
        
        // Button positions (right side, but not on the edge)
        int buttonSize = 12;
        int buttonMargin = 5;
        int buttonSpacing = 2;
        int editButtonX = entryX + ENTRY_WIDTH - buttonMargin - buttonSize;
        int deleteButtonX = editButtonX - buttonSize - buttonSpacing;
        int concatButtonX = deleteButtonX - buttonSize - 2;
        int maxTextWidth = concatButtonX - textX - 5;
        
        // Render filter text (truncate if too long)
        String displayText = filter.isEmpty() ? "" : filter;
        int textWidth = this.font.width(displayText);
        if (textWidth > maxTextWidth && !displayText.isEmpty()) {
            // Truncate with ellipsis
            displayText = this.font.plainSubstrByWidth(displayText, maxTextWidth - this.font.width("...")) + "...";
        }
        
        guiGraphics.text(this.font, Component.literal(displayText), textX, textY, GuiTextColors.TITLE, false);
        
        // Edit button is rendered as a widget (created in updateEditButtons)
    }
    
    /**
     * Renders the edit mode UI (ghost slot and textbox)
     */
    private void renderEditModeUI(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int slotX = this.leftPos + DeepDrawerExtractorMenu.EDIT_MODE_GHOST_SLOT_X;
        int slotY = this.topPos + DeepDrawerExtractorMenu.EDIT_MODE_PANEL_Y;
        int slotSize = DeepDrawerExtractorMenu.EDIT_SLOT_SIZE;
        
        // Draw slot background
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SINGLE_SLOT_TEXTURE, slotX, slotY, 0.0F, 0.0F, slotSize, slotSize, slotSize, slotSize);
        
        // Render ghost slot item if present
        if (!ghostSlotItem.isEmpty()) {
            guiGraphics.item(ghostSlotItem, slotX + 1, slotY + 1);
            guiGraphics.itemDecorations(this.font, ghostSlotItem, slotX + 1, slotY + 1);
        }
        
        // Buttons and textbox are rendered as widgets (created in createEditModeUI)
    }
    
    /**
     * Gets the display item for a filter based on its type
     * Returns appropriate item for ID, tag, mod, NBT, or macro filters
     */
    private ItemStack getDisplayItemForFilter(String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            return ItemStack.EMPTY;
        }
        
        filter = filter.trim();
        
        // ID filter: -minecraft:diamond
        if (filter.startsWith("-")) {
            String idFilter = filter.substring(1);
            try {
                Identifier itemId = Identifier.parse(idFilter);
                return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId)
                    .map(holder -> new ItemStack(holder.value()))
                    .orElse(ItemStack.EMPTY);
            } catch (Exception e) {
                return ItemStack.EMPTY;
            }
        }
        
        // Tag filter: #c:ingots
        if (filter.startsWith("#")) {
            String tagFilter = filter.substring(1);
            return getItemForTag(tagFilter);
        }
        
        // Mod ID filter: @iska_utils
        if (filter.startsWith("@")) {
            String modIdFilter = filter.substring(1);
            return getItemForMod(modIdFilter);
        }
        
        // NBT filter: ?"apotheosis:rarity":"apotheosis:mythic"
        if (filter.startsWith("?")) {
            // For NBT filters, show knowledge book (green recipe book)
            return new ItemStack(net.minecraft.world.item.Items.KNOWLEDGE_BOOK);
        }
        
        // Macro filter: &enchanted, &damaged
        if (filter.startsWith("&")) {
            String macroFilter = filter.substring(1).toLowerCase();
            return switch (macroFilter) {
                case "enchanted" -> {
                    // Rendering-only hint item for the UI.
                    yield new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
                }
                case "damaged" -> {
                    // Return a damaged item
                    ItemStack stack = new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
                    stack.setDamageValue(stack.getMaxDamage() / 2);
                    yield stack;
                }
                default -> ItemStack.EMPTY;
            };
        }
        
        // Default: treat as direct ID match (without prefix)
        try {
            Identifier itemId = Identifier.parse(filter);
            return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId)
                .map(holder -> new ItemStack(holder.value()))
                .orElse(ItemStack.EMPTY);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
    
    /**
     * Gets an item that has the specified tag (for cyclic display)
     */
    private ItemStack getItemForTag(String tagId) {
        try {
            Identifier tagLocation = Identifier.parse(tagId);
            net.minecraft.tags.TagKey<net.minecraft.world.item.Item> itemTag = 
                net.minecraft.tags.ItemTags.create(tagLocation);
            
            java.util.List<net.minecraft.world.item.Item> items = new java.util.ArrayList<>();
            for (var holder : net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(itemTag)) {
                items.add(holder.value());
            }
            if (!items.isEmpty()) {
                int index = (int)((System.currentTimeMillis() / 2000) % items.size());
                return new ItemStack(items.get(index));
            }
        } catch (Exception e) {
            // Invalid tag, ignore
        }
        return ItemStack.EMPTY;
    }
    
    /**
     * Gets an item from the specified mod (for cyclic display)
     */
    private ItemStack getItemForMod(String modId) {
        // Find all items from this mod
        java.util.List<net.minecraft.world.item.Item> modItems = new java.util.ArrayList<>();
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            Identifier itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            if (itemId != null && itemId.getNamespace().startsWith(modId)) {
                modItems.add(item);
            }
        }
        
        if (!modItems.isEmpty()) {
            // Use cyclic index based on tick time for rotation
            int index = (int)((System.currentTimeMillis() / 2000) % modItems.size()); // Change every 2 seconds
            return new ItemStack(modItems.get(index));
        }
        
        return ItemStack.EMPTY;
    }
    
    private void renderSettingsCopierColumn(GuiGraphicsExtractor guiGraphics) {
        if (!showsSettingsCopierColumn()) {
            return;
        }
        DeepDrawerSettingsCopierClient.blitSlotFrame(
                guiGraphics,
                this.leftPos + DeepDrawerExtractorMenu.COPIER_COLUMN_X,
                this.topPos + DeepDrawerExtractorMenu.COPIER_SLOT_BACKGROUND_Y);
    }

    @Override
    protected void extractSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int copierIdx = menu.copySettingsSlotIndex();
        Slot hovered = this.hoveredSlot;
        if (hovered != null && copierIdx >= 0 && hovered.index == copierIdx) {
            this.hoveredSlot = null;
            super.extractSlots(graphics, mouseX, mouseY);
            this.hoveredSlot = hovered;
            return;
        }
        super.extractSlots(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderSlotContents(GuiGraphicsExtractor guiGraphics, ItemStack stack, net.minecraft.world.inventory.Slot slot, @org.jetbrains.annotations.Nullable String count) {
        if (menu.copySettingsSlotIndex() >= 0 && slot.index == menu.copySettingsSlotIndex()) {
            return;
        }
        super.renderSlotContents(guiGraphics, stack, slot, count);
    }

    /**
     * Renders the scrollbar with UP/DOWN buttons and draggable handle
     * Identical to DeepDrawersScreen implementation
     */
    private void renderScrollbar(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        // Only show scrollbar if there are more slots than can fit
        if (MAX_FILTER_SLOTS <= VISIBLE_ENTRIES) return;
        
        int guiX = this.leftPos;
        int guiY = this.topPos;
        
        // Draw scrollbar background (8 pixels wide, height as defined)
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, guiY + SCROLLBAR_Y, 0.0F, 0.0F, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT, 32, 34);
        
        // UP button (8x8 pixels) - above scrollbar
        boolean upButtonHovered = (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH &&
                                  mouseY >= guiY + BUTTON_UP_Y && mouseY < guiY + BUTTON_UP_Y + HANDLE_SIZE);
        int upButtonV = upButtonHovered ? HANDLE_SIZE : 0;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, guiY + BUTTON_UP_Y, (float)(SCROLLBAR_WIDTH * 2), (float)upButtonV, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        
        // DOWN button (8x8 pixels) - below scrollbar
        boolean downButtonHovered = (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH &&
                                    mouseY >= guiY + BUTTON_DOWN_Y && mouseY < guiY + BUTTON_DOWN_Y + HANDLE_SIZE);
        int downButtonV = downButtonHovered ? HANDLE_SIZE : 0;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, guiY + BUTTON_DOWN_Y, (float)(SCROLLBAR_WIDTH * 3), (float)downButtonV, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        
        // Handle (8x8 pixels) - position based on scroll offset
        int maxScrollOffset = Math.max(0, MAX_FILTER_SLOTS - VISIBLE_ENTRIES);
        if (maxScrollOffset > 0) {
            double scrollRatio = (double) filterScrollOffset / maxScrollOffset;
            int handleY = guiY + SCROLLBAR_Y + (int)(scrollRatio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
            
            boolean handleHovered = (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + HANDLE_SIZE &&
                                    mouseY >= handleY && mouseY < handleY + HANDLE_SIZE);
            int handleTextureY = handleHovered ? HANDLE_SIZE : 0;
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, handleY, (float)SCROLLBAR_WIDTH, (float)handleTextureY, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        }
    }
    
    /**
     * Updates edit and delete buttons for visible entries
     */
    private void updateEditButtons() {
        // Remove all existing edit buttons
        for (Button button : editButtons) {
            if (button != null) {
                removeWidget(button);
            }
        }
        editButtons.clear();
        
        // Remove all existing delete buttons
        for (Button button : deleteButtons) {
            if (button != null) {
                removeWidget(button);
            }
        }
        deleteButtons.clear();

        for (DeepDrawerConcatChannelButton button : concatButtons) {
            if (button != null) {
                removeWidget(button);
            }
        }
        concatButtons.clear();
        
        if (subView == SubView.HOW_TO_USE) {
            return;
        }
        if (!isFilterListOpen()) {
            return;
        }
        
        // Create edit and delete buttons for visible entries
        int buttonSize = 12;
        int buttonMargin = 5;
        int buttonSpacing = 2;
        int concatSpacing = 2;
        
        for (int i = 0; i < VISIBLE_ENTRIES; i++) {
            int filterIndex = filterScrollOffset + i;
            if (filterIndex >= MAX_FILTER_SLOTS) {
                break;
            }
            
            int entryX = this.leftPos + ENTRY_X;
            int entryY = this.topPos + FIRST_ROW_Y + i * (ENTRY_HEIGHT + ENTRY_SPACING);
            int editButtonX = entryX + ENTRY_WIDTH - buttonMargin - buttonSize;
            int deleteButtonX = editButtonX - buttonSize - buttonSpacing;
            int concatButtonX = deleteButtonX - buttonSize - concatSpacing;
            int buttonY = entryY + (ENTRY_HEIGHT - buttonSize) / 2;
            
            final int finalFilterIndex = filterIndex;
            while (cachedConcatFields.size() <= filterIndex) {
                cachedConcatFields.add(0);
            }
            int concatVal = cachedConcatFields.get(filterIndex) != null ? cachedConcatFields.get(filterIndex) : 0;

            DeepDrawerConcatChannelButton concatButton = new DeepDrawerConcatChannelButton(
                    concatButtonX, buttonY, buttonSize, buttonSize,
                    ch -> {
                        while (cachedConcatFields.size() <= finalFilterIndex) {
                            cachedConcatFields.add(0);
                        }
                        cachedConcatFields.set(finalFilterIndex, ch);
                        saveFilterData();
                    });
            concatButton.setChannelOrdinal(concatVal);
            concatButton.setTooltip(DeepDrawerExtractorGuiTooltips.concatChannelTooltip());
            concatButtons.add(concatButton);
            addRenderableWidget(concatButton);
            
            // Create delete button (C) with "Clear" tooltip
            Button deleteButton = Button.builder(Component.literal("C"),
                button -> {
                        onDeleteButtonClicked(finalFilterIndex);
                    })
                .bounds(deleteButtonX, buttonY, buttonSize, buttonSize)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.translatable("gui.iska_utils.deep_drawer_extractor.clear")))
                .build();
            
            deleteButtons.add(deleteButton);
            addRenderableWidget(deleteButton);
            
            // Create edit button
            Button editButton = Button.builder(Component.literal("✎"), 
                button -> {
                        onEditButtonClicked(finalFilterIndex);
                    })
                .bounds(editButtonX, buttonY, buttonSize, buttonSize)
                .build();
            
            editButtons.add(editButton);
            addRenderableWidget(editButton);
        }
    }
    
    /**
     * Handles delete button click - clears the filter entry
     */
    private void onDeleteButtonClicked(int filterIndex) {
        // Ensure cachedFilterFields has all slots
        while (cachedFilterFields.size() < MAX_FILTER_SLOTS) {
            cachedFilterFields.add("");
        }
        
        // Clear the filter entry
        cachedFilterFields.set(filterIndex, "");
        while (cachedConcatFields.size() <= filterIndex) {
            cachedConcatFields.add(0);
        }
        cachedConcatFields.set(filterIndex, 0);
        saveFilterData();
    }
    
    /**
     * Handles edit button click - switches to edit mode for the filter
     */
    private void onEditButtonClicked(int filterIndex) {
        // If this entry is in quick edit mode, close it first
        if (editingFilterIndex == filterIndex && editingEditBox != null) {
            stopEditingFilter();
        }
        
        // Toggle edit mode for this filter
        if (editModeFilterIndex == filterIndex) {
            // Already in edit mode, exit edit mode
            exitEditMode();
        } else {
            // Enter edit mode for this filter
            enterEditMode(filterIndex);
        }
    }
    
    /**
     * Enters edit mode for the specified filter
     */
    private void enterEditMode(int filterIndex) {
        editModeFilterIndex = filterIndex;
        if (filterIndex >= 0 && filterIndex < cachedFilterFields.size()) {
            originalFilterValue = cachedFilterFields.get(filterIndex) != null ? cachedFilterFields.get(filterIndex) : "";
        } else {
            originalFilterValue = "";
        }
        createEditModeUI();
        applySubViewVisibility();
    }

    private void exitEditMode() {
        exitEditMode(true);
    }

    private void exitEditMode(boolean discardChanges) {
        if (discardChanges && editModeFilterIndex >= 0) {
            while (cachedFilterFields.size() <= editModeFilterIndex) {
                cachedFilterFields.add("");
            }
            cachedFilterFields.set(editModeFilterIndex, originalFilterValue);
        }
        editModeFilterIndex = -1;
        originalFilterValue = "";
        removeEditModeUI();
        applySubViewVisibility();
    }
    
    /**
     * Creates the edit mode UI (textbox and ghost slot)
     */
    private void createEditModeUI() {
        // Remove existing edit mode UI if any
        removeEditModeUI();
        
        int slotX = this.leftPos + DeepDrawerExtractorMenu.EDIT_MODE_GHOST_SLOT_X;
        int slotY = this.topPos + DeepDrawerExtractorMenu.EDIT_MODE_PANEL_Y;
        int slotSize = DeepDrawerExtractorMenu.EDIT_SLOT_SIZE;
        
        int buttonSize = DeepDrawerExtractorMenu.EDIT_BTN_SIZE;
        int buttonSpacing = DeepDrawerExtractorMenu.EDIT_BTN_SPACING;
        int arrowSpacing = DeepDrawerExtractorMenu.EDIT_ARROW_GAP;
        int editActionGap = DeepDrawerExtractorMenu.EDIT_ACTION_GAP;
        
        // Left arrow button (to the left of slot)
        int leftButtonX = slotX - buttonSize - arrowSpacing;
        int leftButtonY = slotY + (slotSize - buttonSize) / 2;
        
        leftArrowButton = Button.builder(Component.literal("←"), 
            button -> cycleFilterVariant(-1))
            .bounds(leftButtonX, leftButtonY, buttonSize, buttonSize)
            .build();
        addRenderableWidget(leftArrowButton);
        
        // Right arrow button (to the right of slot)
        int rightButtonX = slotX + slotSize + arrowSpacing;
        int rightButtonY = slotY + (slotSize - buttonSize) / 2;
        
        rightArrowButton = Button.builder(Component.literal("→"), 
            button -> cycleFilterVariant(1))
            .bounds(rightButtonX, rightButtonY, buttonSize, buttonSize)
            .build();
        addRenderableWidget(rightArrowButton);
        
        // C (clear), A (apply), X (close) — after the right arrow, with extra gap so C does not overlap →
        int buttonAfterSlotY = slotY + (slotSize - buttonSize) / 2;
        int clearButtonX = rightButtonX + buttonSize + editActionGap;
        int applyButtonX = clearButtonX + buttonSize + buttonSpacing;
        int closeButtonX = applyButtonX + buttonSize + buttonSpacing;
        
        // Textbox on the row directly above player inventory
        int textBoxX = this.leftPos + DeepDrawerExtractorMenu.PLAYER_INV_X;
        int textBoxY = this.topPos + DeepDrawerExtractorMenu.EDIT_TEXTBOX_Y;
        int textBoxHeight = DeepDrawerExtractorMenu.EDIT_TEXTBOX_HEIGHT;
        
        // Calculate textbox width - almost to the right edge
        int rightEdge = this.leftPos + GUI_WIDTH;
        int margin = 5; // Margin from right edge
        int textBoxWidth = rightEdge - textBoxX - margin; // Almost to the right edge
        
        // Create textbox
        editModeTextBox = new EditBox(this.font, textBoxX, textBoxY, textBoxWidth, textBoxHeight,
            Component.literal("Edit Filter"));
        
        // IMPORTANT: Set maxLength BEFORE setValue() to avoid truncation
        // NBT filters can be voluminous, so we allow up to 512 characters
        editModeTextBox.setMaxLength(512);
        
        editModeTextBox.setVisible(true);
        editModeTextBox.setEditable(true);
        
        // Set initial value from cached filter fields
        if (editModeFilterIndex >= 0 && editModeFilterIndex < cachedFilterFields.size()) {
            String currentFilter = cachedFilterFields.get(editModeFilterIndex);
            // Debug: log when setting value in EditBox from NBT/cache
            // if (currentFilter != null && currentFilter.length() > 30) {
            //     LOGGER.info("DEBUG: Setting EditBox value from cache: index={}, length={}, value={}", editModeFilterIndex, currentFilter.length(), currentFilter);
            // }
            editModeTextBox.setValue(currentFilter != null ? currentFilter : "");
            // Debug: log after setting to verify
            // String setValue = editModeTextBox.getValue();
            // if (setValue != null && setValue.length() > 30) {
            //     LOGGER.info("DEBUG: EditBox value after setValue: index={}, length={}, value={}", editModeFilterIndex, setValue.length(), setValue);
            // }
        } else {
            editModeTextBox.setValue("");
        }
        
        // Don't save automatically - user must click Apply button
        editModeTextBox.setResponder(value -> {
            // Just update the textbox value, don't save yet
        });
        
        addRenderableWidget(editModeTextBox);
        
        // Create clear button (C) - clears the textbox
        editModeClearButton = Button.builder(Component.literal("C"), 
            button -> {
                if (editModeTextBox != null) {
                    editModeTextBox.setValue("");
                    editModeTextBox.setCursorPosition(0);
                    editModeTextBox.setHighlightPos(0);
                }
            })
            .bounds(clearButtonX, buttonAfterSlotY, buttonSize, buttonSize)
            .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("gui.iska_utils.deep_drawer_extractor.clear")))
            .build();
        addRenderableWidget(editModeClearButton);
        
        // Create apply button (A) - saves changes and exits edit mode
        editModeApplyButton = Button.builder(Component.literal("A"), 
            button -> {
                // Save the current textbox value
                if (editModeTextBox != null && editModeFilterIndex >= 0) {
                    String value = editModeTextBox.getValue();
                    while (cachedFilterFields.size() <= editModeFilterIndex) {
                        cachedFilterFields.add("");
                    }
                    cachedFilterFields.set(editModeFilterIndex, value);
                    saveFilterData(); // Save to server
                }
                // Exit edit mode (but don't restore original since we're saving)
                editModeFilterIndex = -1;
                originalFilterValue = "";
                removeEditModeUI();
                applySubViewVisibility();
            })
            .bounds(applyButtonX, buttonAfterSlotY, buttonSize, buttonSize)
            .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("gui.iska_utils.deep_drawer_extractor.apply")))
            .build();
        addRenderableWidget(editModeApplyButton);
        
        // Create close button (X) - exits edit mode without saving
        editModeCloseButton = Button.builder(Component.literal("✕"), button -> exitEditMode())
                .bounds(closeButtonX, buttonAfterSlotY, buttonSize, buttonSize)
                .build();
        addRenderableWidget(editModeCloseButton);

        layoutValidKeysButton();

        // Initialize ghost slot as empty
        ghostSlotItem = ItemStack.EMPTY;
    }
    
    /**
     * Removes the edit mode UI
     */
    private void removeEditModeUI() {
        if (editModeTextBox != null) {
            removeWidget(editModeTextBox);
            editModeTextBox = null;
        }
        if (leftArrowButton != null) {
            removeWidget(leftArrowButton);
            leftArrowButton = null;
        }
        if (rightArrowButton != null) {
            removeWidget(rightArrowButton);
            rightArrowButton = null;
        }
        if (editModeCloseButton != null) {
            removeWidget(editModeCloseButton);
            editModeCloseButton = null;
        }
        if (editModeClearButton != null) {
            removeWidget(editModeClearButton);
            editModeClearButton = null;
        }
        if (editModeApplyButton != null) {
            removeWidget(editModeApplyButton);
            editModeApplyButton = null;
        }
        ghostSlotItem = ItemStack.EMPTY;
        filterVariants.clear();
        currentFilterVariantIndex = 0;
    }
    
    /**
     * Handles click on ghost slot (phantom slot that copies items without consuming)
     */
    private void handleGhostSlotClick() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        
        // Get the item the player is holding (cursor item)
        ItemStack cursorItem = this.menu.getCarried();
        
        if (cursorItem.isEmpty()) {
            // Cursor is empty: clear the ghost slot and textbox
            ghostSlotItem = ItemStack.EMPTY;
            filterVariants.clear();
            currentFilterVariantIndex = 0;
            if (editModeTextBox != null) {
                editModeTextBox.setValue("");
                // Position cursor at the beginning
                editModeTextBox.setCursorPosition(0);
                editModeTextBox.setHighlightPos(0);
                // Don't save - user must click Apply
            }
            playButtonSound();
        } else {
            // Cursor has item: copy it to ghost slot (don't consume the original)
            ghostSlotItem = cursorItem.copy();
            
            // Generate all possible filter variants
            filterVariants = generateAllFilterVariants(cursorItem);
            currentFilterVariantIndex = 0; // Start with first variant
            
            // Update textbox with first variant
            if (editModeTextBox != null && !filterVariants.isEmpty()) {
                String filterString = filterVariants.get(0);
                // Debug: log when setting value from variant
                // if (filterString != null && filterString.length() > 30) {
                //     LOGGER.info("DEBUG: Setting EditBox value from variant: length={}, value={}", filterString.length(), filterString);
                // }
                editModeTextBox.setValue(filterString);
                // Position cursor at the beginning and show from start
                editModeTextBox.setCursorPosition(0);
                editModeTextBox.setHighlightPos(0);
                // Debug: log after setting to verify
                // String setValue = editModeTextBox.getValue();
                // if (setValue != null && setValue.length() > 30) {
                //     LOGGER.info("DEBUG: EditBox value after setValue (variant): length={}, value={}", setValue.length(), setValue);
                // }
                // Don't save - user must click Apply
            }
            
            playButtonSound();
        }
    }
    
    /**
     * Cycles to the next/previous filter variant
     * @param direction 1 for next, -1 for previous
     */
    private void cycleFilterVariant(int direction) {
        if (filterVariants.isEmpty()) {
            return;
        }
        
        // Calculate new index (with wrapping)
        currentFilterVariantIndex += direction;
        if (currentFilterVariantIndex < 0) {
            currentFilterVariantIndex = filterVariants.size() - 1;
        } else if (currentFilterVariantIndex >= filterVariants.size()) {
            currentFilterVariantIndex = 0;
        }
        
        // Update textbox with new variant
        String filterString = filterVariants.get(currentFilterVariantIndex);
        if (editModeTextBox != null) {
            // Debug: log when setting value from variant cycle
            // if (filterString != null && filterString.length() > 30) {
            //     LOGGER.info("DEBUG: Setting EditBox value from variant cycle: length={}, value={}", filterString.length(), filterString);
            // }
            editModeTextBox.setValue(filterString);
            // Position cursor at the beginning and show from start
            editModeTextBox.setCursorPosition(0);
            editModeTextBox.setHighlightPos(0);
            // Debug: log after setting to verify
            // String setValue = editModeTextBox.getValue();
            // if (setValue != null && setValue.length() > 30) {
            //     LOGGER.info("DEBUG: EditBox value after setValue (variant cycle): length={}, value={}", setValue.length(), setValue);
            // }
            // Don't save - user must click Apply
        }
    }
    
    /**
     * Generates all possible filter variants from an ItemStack
     * Order: ID item, &enchanted (if present), &damaged (if present), mod ID, all tags
     */
    private java.util.List<String> generateAllFilterVariants(ItemStack stack) {
        java.util.List<String> variants = new java.util.ArrayList<>();
        
        if (stack.isEmpty()) {
            return variants;
        }
        
        Identifier itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return variants;
        }
        
        // 1. Always start with item ID
        variants.add("-" + itemId.toString());
        
        // 2. Add mod ID (if not minecraft)
        String namespace = itemId.getNamespace();
        if (!namespace.equals("minecraft")) {
            variants.add("@" + namespace);
        }
        
        // 3. If enchanted, add &enchanted after mod ID
        if (stack.isEnchanted()) {
            variants.add("&enchanted");
        }
        
        // 4. If damaged, add &damaged after mod ID (and after enchanted if present)
        if (stack.isDamaged()) {
            variants.add("&damaged");
        }
        
        // 5. Add all tags (sorted)
        var item = stack.getItem();
        var itemHolder = net.minecraft.core.registries.BuiltInRegistries.ITEM.wrapAsHolder(item);
        var itemTags = net.minecraft.core.registries.BuiltInRegistries.ITEM.getTags()
            .filter(named -> named.contains(itemHolder))
            .map(named -> named.key().location())
            .map(Identifier::toString)
            .sorted()
            .toList();
        
        // Add all tags with # prefix
        for (String tagId : itemTags) {
            variants.add("#" + tagId);
        }

        // NBT/SNBT filter: encode the full stack to NBT via codec (1.26 lacks ItemStack#save(registryAccess)).
        try {
            if (this.minecraft != null && this.minecraft.level != null) {
                var ops = this.minecraft.level.registryAccess()
                    .createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
                net.minecraft.nbt.Tag encoded = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
                String snbt = encoded.toString();
                if (!snbt.isEmpty()) {
                    variants.add("?" + snbt);
                }
            }
        } catch (Exception ignored) {}
        
        return variants;
    }
    
    /**
     * Starts editing a filter entry at the given index
     */
    private void startEditingFilter(int filterIndex) {
        // Don't allow quick edit if this entry is already in edit mode
        if (editModeFilterIndex == filterIndex) {
            return; // Entry is already in edit mode, don't open quick edit
        }
        
        // Remove existing editing EditBox if any
        if (editingEditBox != null) {
            removeWidget(editingEditBox);
            editingEditBox = null;
        }
        
        // Ensure cachedFilterFields has all slots
        while (cachedFilterFields.size() < MAX_FILTER_SLOTS) {
            cachedFilterFields.add("");
        }
        
        // Calculate entry position
        int visibleIndex = filterIndex - filterScrollOffset;
        if (visibleIndex < 0 || visibleIndex >= VISIBLE_ENTRIES) {
            return; // Not visible
        }
        
        int entryX = this.leftPos + ENTRY_X;
        int entryY = this.topPos + FIRST_ROW_Y + visibleIndex * (ENTRY_HEIGHT + ENTRY_SPACING);
        
        // Create EditBox positioned over the entry text area
        int textX = entryX + 3 + 18 + 6; // After slot
        int textY = entryY + (ENTRY_HEIGHT - 15) / 2; // Centered vertically (EditBox height is 15)
        
        // Calculate width leaving space for delete (X) and edit buttons
        // Layout: [text] ... [X button] [edit button] [margin]
        int buttonSize = 12;
        int buttonMargin = 5;
        int buttonSpacing = 2;
        int editButtonX = entryX + ENTRY_WIDTH - buttonMargin - buttonSize;
        int deleteButtonX = editButtonX - buttonSize - buttonSpacing;
        int textWidth = deleteButtonX - textX - 5; // 5px margin before delete button
        
        editingEditBox = new EditBox(this.font,
                textX,
                textY,
                textWidth,
                15,
                Component.empty());
        // NBT filters can be voluminous, so we allow up to 512 characters
        editingEditBox.setMaxLength(512);
        editingEditBox.setValue(cachedFilterFields.get(filterIndex) != null ? cachedFilterFields.get(filterIndex) : "");
        editingEditBox.setEditable(true);
        editingEditBox.setFocused(true);
        
        editingFilterIndex = filterIndex;
        
        // Save data when EditBox value changes
        editingEditBox.setResponder(value -> {
            // Ensure cachedFilterFields has all 50 slots
            while (cachedFilterFields.size() < MAX_FILTER_SLOTS) {
                cachedFilterFields.add("");
            }
            
            String trimmedValue = value.trim();
            cachedFilterFields.set(filterIndex, trimmedValue);
            
            saveFilterData();
        });
        
        addRenderableWidget(editingEditBox);
    }
    
    /**
     * Stops editing the current filter
     */
    private void stopEditingFilter() {
        if (editingEditBox != null) {
            removeWidget(editingEditBox);
            editingEditBox = null;
            editingFilterIndex = -1;
        }
    }
    
    /**
     * Renders an item scaled to the specified size
     */
    private void renderScaledItem(GuiGraphicsExtractor guiGraphics, net.minecraft.world.item.ItemStack itemStack, int x, int y, int size) {
        guiGraphics.pose().pushMatrix();
        
        // Calculate scale: original item size is 16x16, we want 12x12
        float scale = (float) size / 16.0f;
        
        // Translate to position and apply scale
        guiGraphics.pose().translate(x, y);
        guiGraphics.pose().scale(scale, scale);
        
        // Render the item
        guiGraphics.item(itemStack, 0, 0);
        
        // Restore matrix state
        guiGraphics.pose().popMatrix();
    }
    
    /**
     * Renders a texture scaled to the specified size (like an item)
     */
    private void renderScaledTexture(GuiGraphicsExtractor guiGraphics, Identifier texture, int x, int y, int size) {
        guiGraphics.pose().pushMatrix();
        
        // Calculate scale: original texture size is 16x16, we want 12x12
        float scale = (float) size / 16.0f;
        
        // Translate to position and apply scale
        guiGraphics.pose().translate(x, y);
        guiGraphics.pose().scale(scale, scale);
        
        // Render the texture (assuming it's 16x16)
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
        
        // Restore matrix state
        guiGraphics.pose().popMatrix();
    }
    
    // Help text positions (for how to use screen)
    private static final int HELP_TEXT_START_Y = 30; // Below title
    private static final int HELP_TEXT_X = 8;
    private static final int HELP_TEXT_LINE_HEIGHT = 12; // Normal line height
    
    // Example text positions and data for copy functionality
    private static class ExampleData {
        final String example;
        final int x;
        final int y;
        final int width;
        
        ExampleData(String example, int x, int y, int width) {
            this.example = example;
            this.x = x;
            this.y = y;
            this.width = width;
        }
    }
    
    private final java.util.List<ExampleData> exampleDataList = new java.util.ArrayList<>();
    
    private void extractHowToUseText(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        // Clear example data list for this frame
        exampleDataList.clear();

        Component titleComponent = Component.translatable("gui.iska_utils.deep_drawer_extractor.how_to_use");
        int titleWidth = this.font.width(titleComponent);
        int titleX = this.leftPos + (this.imageWidth - titleWidth) / 2;
        guiGraphics.text(this.font, titleComponent, titleX, this.topPos + TITLE_Y, GuiTextColors.TITLE, false);

        int helpY = HELP_TEXT_START_Y;
        renderHelpLineWithExample(guiGraphics, "gui.iska_utils.general_filter_text.id",
            "gui.iska_utils.general_filter_text.id.example",
            "gui.iska_utils.general_filter_text.id.after",
            HELP_TEXT_X, helpY, mouseX, mouseY);
        helpY += HELP_TEXT_LINE_HEIGHT;

        renderHelpLineWithExample(guiGraphics, "gui.iska_utils.general_filter_text.tag",
            "gui.iska_utils.general_filter_text.tag.example",
            "gui.iska_utils.general_filter_text.tag.after",
            HELP_TEXT_X, helpY, mouseX, mouseY);
        helpY += HELP_TEXT_LINE_HEIGHT;

        renderHelpLineWithExample(guiGraphics, "gui.iska_utils.general_filter_text.modid",
            "gui.iska_utils.general_filter_text.modid.example",
            "gui.iska_utils.general_filter_text.modid.after",
            HELP_TEXT_X, helpY, mouseX, mouseY);
        helpY += HELP_TEXT_LINE_HEIGHT;

        guiGraphics.text(this.font, Component.translatable("gui.iska_utils.general_filter_text.nbt"),
            this.leftPos + HELP_TEXT_X, this.topPos + helpY, GuiTextColors.TITLE, false);
        helpY += HELP_TEXT_LINE_HEIGHT;

        renderHelpLineWithExample(guiGraphics, "gui.iska_utils.general_filter_text.nbt.example",
            "gui.iska_utils.general_filter_text.nbt.example.text",
            "gui.iska_utils.general_filter_text.nbt.after",
            HELP_TEXT_X, helpY, mouseX, mouseY);
        helpY += HELP_TEXT_LINE_HEIGHT;

        renderHelpLineWithTwoExamples(guiGraphics, "gui.iska_utils.general_filter_text.macro",
            "gui.iska_utils.general_filter_text.macro.example1",
            "gui.iska_utils.general_filter_text.macro.middle",
            "gui.iska_utils.general_filter_text.macro.example2",
            "gui.iska_utils.general_filter_text.macro.after",
            HELP_TEXT_X, helpY, mouseX, mouseY);
        helpY += HELP_TEXT_LINE_HEIGHT;

        helpY += HELP_TEXT_LINE_HEIGHT;
        guiGraphics.text(this.font, Component.translatable("gui.iska_utils.general_filter_text.usage"),
            this.leftPos + HELP_TEXT_X, this.topPos + helpY, GuiTextColors.TITLE, false);
        helpY += HELP_TEXT_LINE_HEIGHT;
        guiGraphics.text(this.font, Component.translatable("gui.iska_utils.general_filter_text.usage.what"),
            this.leftPos + HELP_TEXT_X, this.topPos + helpY, GuiTextColors.TITLE, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (subView == SubView.HOW_TO_USE) {
            // Do not render container slots in how-to-use mode.
            for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
                renderable.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
            }
            extractHowToUseText(guiGraphics, mouseX, mouseY);
            renderExampleTooltip(guiGraphics, mouseX, mouseY);
            return;
        }

        this.extractContents(guiGraphics, mouseX, mouseY, partialTick);
        this.extractCarriedItem(guiGraphics, mouseX, mouseY);
        this.extractSnapbackItem(guiGraphics);
        renderSettingsCopierItem(guiGraphics, mouseX, mouseY);
        this.extractTooltip(guiGraphics, mouseX, mouseY);
    }

    private boolean isCopierSlotHovered(double mouseX, double mouseY) {
        int idx = menu.copySettingsSlotIndex();
        if (idx < 0 || !showsSettingsCopierColumn()) {
            return false;
        }
        Slot slot = menu.getSlot(idx);
        return slot.isActive() && isHovering(slot.x, slot.y, DeepDrawerExtractorMenu.COPIER_SLOT_SIZE, DeepDrawerExtractorMenu.COPIER_SLOT_SIZE, mouseX, mouseY);
    }

    private void renderSettingsCopierItem(GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        if (!showsSettingsCopierColumn()) {
            return;
        }
        int idx = menu.copySettingsSlotIndex();
        if (idx < 0) {
            return;
        }
        int frameX = this.leftPos + DeepDrawerExtractorMenu.COPIER_COLUMN_X;
        int frameY = this.topPos + DeepDrawerExtractorMenu.COPIER_SLOT_BACKGROUND_Y;
        int iconX = this.leftPos + DeepDrawerExtractorMenu.copierSlotItemX(DeepDrawerExtractorMenu.COPIER_COLUMN_X);
        int iconY = this.topPos + DeepDrawerExtractorMenu.copierSlotItemY(DeepDrawerExtractorMenu.COPIER_SLOT_BACKGROUND_Y);
        ItemStack copier = menu.getSlot(idx).getItem();

        DeepDrawerSettingsCopierClient.blitSlotFrame(guiGraphics, frameX, frameY);
        if (!copier.isEmpty()) {
            guiGraphics.item(copier, iconX, iconY);
            guiGraphics.itemDecorations(this.font, copier, iconX, iconY);
        }
        if (isCopierSlotHovered(mouseX, mouseY)) {
            guiGraphics.nextStratum();
            renderCopierSlotHighlight(guiGraphics);
        }
    }

    private static final int COPIER_SLOT_HOVER_COLOR = -2130706433;

    private void renderCopierSlotHighlight(GuiGraphicsExtractor graphics) {
        int x = this.leftPos + DeepDrawerExtractorMenu.copierSlotHighlightX(DeepDrawerExtractorMenu.COPIER_COLUMN_X);
        int y = this.topPos + DeepDrawerExtractorMenu.copierSlotHighlightY(DeepDrawerExtractorMenu.COPIER_SLOT_BACKGROUND_Y);
        int size = DeepDrawerExtractorMenu.COPIER_SLOT_HIGHLIGHT_SIZE;
        graphics.fillGradient(x, y, x + size, y + size, COPIER_SLOT_HOVER_COLOR, COPIER_SLOT_HOVER_COLOR);
    }
    
    /**
     * Renders a help line with an example that can be clicked to copy
     */
    private void renderHelpLineWithExample(GuiGraphicsExtractor guiGraphics, String beforeKey, String exampleKey, String afterKey,
                                          int x, int y, int mouseX, int mouseY) {
        // Get translated parts
        Component beforeComponent = Component.translatable(beforeKey);
        Component exampleComponent = Component.translatable(exampleKey);
        Component afterComponent = Component.translatable(afterKey);
        
        String beforeText = beforeComponent.getString();
        String exampleText = exampleComponent.getString();
        String afterText = afterComponent.getString();
        
        // Convert relative coordinates to absolute screen coordinates
        int absX = this.leftPos + x;
        int absY = this.topPos + y;
        
        // Render before text
        int beforeWidth = this.font.width(beforeText);
        guiGraphics.text(this.font, beforeComponent, absX, absY, GuiTextColors.TITLE, false);
        
        // Render example text (clickable, with blue color)
        int exampleX = absX + beforeWidth;
        int exampleWidth = this.font.width(exampleText);
        
        // Check if hovering over example
        boolean isHovered = mouseX >= exampleX && mouseX <= exampleX + exampleWidth &&
                           mouseY >= absY && mouseY <= absY + HELP_TEXT_LINE_HEIGHT;
        
        // Use blue color for clickable example, darker blue when hovered
        int exampleColor = isHovered ? GuiTextColors.LINK_HOVER : GuiTextColors.LINK;
        
        // Render example text in blue
        guiGraphics.text(this.font, Component.literal(exampleText), exampleX, absY, exampleColor, false);
        
        // Underline when hovered
        if (isHovered) {
            // Draw underline
            int underlineY = absY + this.font.lineHeight;
            guiGraphics.fill(exampleX, underlineY, exampleX + exampleWidth, underlineY + 1, exampleColor);
        }
        
        // Render after text (parentheses, commas, etc.)
        if (!afterText.isEmpty()) {
            int afterX = exampleX + exampleWidth;
            guiGraphics.text(this.font, afterComponent, afterX, absY, GuiTextColors.TITLE, false);
        }
        
        // Store example data for click handling (store relative coordinates for later use)
        exampleDataList.add(new ExampleData(exampleText, x + beforeWidth, y, exampleWidth));
    }
    
    /**
     * Renders a help line with two examples that can be clicked to copy
     */
    private void renderHelpLineWithTwoExamples(GuiGraphicsExtractor guiGraphics, String beforeKey,
                                              String example1Key, String middleKey, String example2Key, String afterKey,
                                              int x, int y, int mouseX, int mouseY) {
        // Get translated parts
        Component beforeComponent = Component.translatable(beforeKey);
        Component example1Component = Component.translatable(example1Key);
        Component middleComponent = Component.translatable(middleKey);
        Component example2Component = Component.translatable(example2Key);
        Component afterComponent = Component.translatable(afterKey);
        
        String beforeText = beforeComponent.getString();
        String example1Text = example1Component.getString();
        String middleText = middleComponent.getString();
        String example2Text = example2Component.getString();
        String afterText = afterComponent.getString();
        
        // Convert relative coordinates to absolute screen coordinates
        int absX = this.leftPos + x;
        int absY = this.topPos + y;
        
        // Render before text
        int beforeWidth = this.font.width(beforeText);
        guiGraphics.text(this.font, beforeComponent, absX, absY, GuiTextColors.TITLE, false);
        
        // Render first example
        int example1X = absX + beforeWidth;
        int example1Width = this.font.width(example1Text);
        boolean isHovered1 = mouseX >= example1X && mouseX <= example1X + example1Width &&
                            mouseY >= absY && mouseY <= absY + HELP_TEXT_LINE_HEIGHT;
        int example1Color = isHovered1 ? GuiTextColors.LINK_HOVER : GuiTextColors.LINK;
        guiGraphics.text(this.font, Component.literal(example1Text), example1X, absY, example1Color, false);
        if (isHovered1) {
            int underlineY = absY + this.font.lineHeight;
            guiGraphics.fill(example1X, underlineY, example1X + example1Width, underlineY + 1, example1Color);
        }
        exampleDataList.add(new ExampleData(example1Text, x + beforeWidth, y, example1Width));
        
        // Render middle text (comma and space)
        int middleX = example1X + example1Width;
        guiGraphics.text(this.font, middleComponent, middleX, absY, GuiTextColors.TITLE, false);
        
        // Render second example
        int middleWidth = this.font.width(middleText);
        int example2X = middleX + middleWidth;
        int example2Width = this.font.width(example2Text);
        boolean isHovered2 = mouseX >= example2X && mouseX <= example2X + example2Width &&
                            mouseY >= absY && mouseY <= absY + HELP_TEXT_LINE_HEIGHT;
        int example2Color = isHovered2 ? GuiTextColors.LINK_HOVER : GuiTextColors.LINK;
        guiGraphics.text(this.font, Component.literal(example2Text), example2X, absY, example2Color, false);
        if (isHovered2) {
            int underlineY = absY + this.font.lineHeight;
            guiGraphics.fill(example2X, underlineY, example2X + example2Width, underlineY + 1, example2Color);
        }
        exampleDataList.add(new ExampleData(example2Text, x + beforeWidth + example1Width + middleWidth, y, example2Width));
        
        // Render after text
        if (!afterText.isEmpty()) {
            int afterX = example2X + example2Width;
            guiGraphics.text(this.font, afterComponent, afterX, absY, GuiTextColors.TITLE, false);
        }
    }
    
    /**
     * Renders tooltip when hovering over an example
     */
    private void renderExampleTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        for (ExampleData exampleData : exampleDataList) {
            int screenX = this.leftPos + exampleData.x;
            int screenY = this.topPos + exampleData.y;
            
            if (mouseX >= screenX && mouseX <= screenX + exampleData.width &&
                mouseY >= screenY && mouseY <= screenY + HELP_TEXT_LINE_HEIGHT) {
                
                java.util.List<FormattedCharSequence> tooltip = java.util.List.of(
                    Component.translatable("gui.iska_utils.general_filter_text.click_to_copy").getVisualOrderText(),
                    Component.translatable("gui.iska_utils.general_filter_text.paste_hint").getVisualOrderText()
                );
                guiGraphics.setTooltipForNextFrame(this.font, tooltip, DefaultTooltipPositioner.INSTANCE, mouseX, mouseY, true);
                return;
            }
        }
    }
    
    @Override
    public void containerTick() {
        super.containerTick();
        tryRestoreSavedFilterSubview();
        
        // Update cached filters from server (like redstone mode and structure)
        menu.updateCachedFilters();
        
        // Get cached data for filter fields (both normal and inverted)
        java.util.List<String> filterFields = menu.getCachedFilterFields();
        java.util.List<String> invertedFilterFields = menu.getCachedInvertedFilterFields();
        
        // Update cached inverted filter fields - ensure we always have exactly MAX_FILTER_SLOTS entries
        cachedInvertedFilterFields = new java.util.ArrayList<>(invertedFilterFields);
        while (cachedInvertedFilterFields.size() < MAX_FILTER_SLOTS) {
            cachedInvertedFilterFields.add("");
        }
        while (cachedInvertedFilterFields.size() > MAX_FILTER_SLOTS) {
            cachedInvertedFilterFields.remove(cachedInvertedFilterFields.size() - 1);
        }
        
        // Update cached filter fields based on current mode
        updateCachedFiltersForMode();
        
        // Ensure we always have exactly MAX_FILTER_SLOTS entries
        while (cachedFilterFields.size() < MAX_FILTER_SLOTS) {
            cachedFilterFields.add("");
        }
        while (cachedFilterFields.size() > MAX_FILTER_SLOTS) {
            cachedFilterFields.remove(cachedFilterFields.size() - 1);
        }
        
        // No need to update entries - they are rendered directly from cachedFilterFields
        
        refreshListLogicButton();
    }
    
    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (MachineGuiInput.handleContainerKeyPressed(this, event, isDraggingHandle, editingEditBox, editModeTextBox)) {
            return true;
        }
        if (editingEditBox != null && editingEditBox.isFocused() && event.key() == 257) {
            stopEditingFilter();
            return true;
        }
        return super.keyPressed(event);
    }
    
    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        String s = event.codepointAsString();
        if (s.isEmpty()) return false;
        char codePoint = s.charAt(0);
        // Check if editing EditBox is focused
        if (editingEditBox != null && editingEditBox.isFocused()) {
            if (editingEditBox.charTyped(event)) {
                return true;
            }
        }
        
        // Check if edit mode textbox is focused
        if (editModeTextBox != null && editModeTextBox.isFocused()) {
            if (editModeTextBox.charTyped(event)) {
                return true;
            }
        }

        return super.charTyped(event);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (isFilterListOpen()) {
            if (deltaY > 0) {
                scrollUp();
                return true;
            } else if (deltaY < 0) {
                scrollDown();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }
    
    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        int button = event.button();
        if (button == 0) {
            MachineGuiInput.clearScrollbarPressed();
            if (isDraggingHandle) {
                isDraggingHandle = false;
                return true;
            }
        }
        return super.mouseReleased(event);
    }
    
    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 0 && isDraggingHandle && MAX_FILTER_SLOTS > VISIBLE_ENTRIES) {
            int deltaY = (int) mouseY - dragStartY;
            int maxScrollOffset = Math.max(0, MAX_FILTER_SLOTS - VISIBLE_ENTRIES);
            
            if (maxScrollOffset > 0) {
                float scrollRatio = (float) deltaY / (SCROLLBAR_HEIGHT - HANDLE_SIZE);
                
                int newScrollOffset = dragStartScrollOffset + (int)(scrollRatio * maxScrollOffset);
                newScrollOffset = Math.max(0, Math.min(maxScrollOffset, newScrollOffset));
                
                setFilterScrollOffset(newScrollOffset);
            }
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }
    
    /**
     * Handles clicks on UP/DOWN buttons
     */
    private boolean handleScrollButtonClick(double mouseX, double mouseY) {
        if (MAX_FILTER_SLOTS <= VISIBLE_ENTRIES) return false;
        
        int guiX = this.leftPos;
        int guiY = this.topPos;
        
        // UP button
        if (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH &&
            mouseY >= guiY + BUTTON_UP_Y && mouseY < guiY + BUTTON_UP_Y + HANDLE_SIZE) {
            scrollUp();
            return true;
        }
        
        // DOWN button
        if (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH &&
            mouseY >= guiY + BUTTON_DOWN_Y && mouseY < guiY + BUTTON_DOWN_Y + HANDLE_SIZE) {
            scrollDown();
            return true;
        }
        
        return false;
    }
    
    /**
     * Handles clicks on the draggable handle
     */
    private boolean handleHandleClick(double mouseX, double mouseY) {
        if (MAX_FILTER_SLOTS <= VISIBLE_ENTRIES) return false;
        
        int guiX = this.leftPos;
        int guiY = this.topPos;
        
        int maxScrollOffset = Math.max(0, MAX_FILTER_SLOTS - VISIBLE_ENTRIES);
        if (maxScrollOffset > 0) {
            double scrollRatio = (double) filterScrollOffset / maxScrollOffset;
            int handleY = guiY + SCROLLBAR_Y + (int)(scrollRatio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
            
            if (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + HANDLE_SIZE &&
                mouseY >= handleY && mouseY < handleY + HANDLE_SIZE) {
                
                isDraggingHandle = true;
                dragStartY = (int) mouseY;
                dragStartScrollOffset = filterScrollOffset;
                playButtonSound();
                return true;
            }
        }
        return false;
    }
    
    /**
     * Handles clicks on the scrollbar area (jump to position)
     */
    private boolean handleScrollbarClick(double mouseX, double mouseY) {
        if (MAX_FILTER_SLOTS <= VISIBLE_ENTRIES) return false;
        
        int guiX = this.leftPos;
        int guiY = this.topPos;
        
        if (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH &&
            mouseY >= guiY + SCROLLBAR_Y && mouseY < guiY + SCROLLBAR_Y + SCROLLBAR_HEIGHT) {
            
            // Calculate new scroll position based on click
            float clickRatio = (float)(mouseY - (guiY + SCROLLBAR_Y)) / SCROLLBAR_HEIGHT;
            clickRatio = Math.max(0, Math.min(1, clickRatio));
            
            int maxScrollOffset = Math.max(0, MAX_FILTER_SLOTS - VISIBLE_ENTRIES);
            int newScrollOffset = (int)(clickRatio * maxScrollOffset);
            newScrollOffset = Math.max(0, Math.min(maxScrollOffset, newScrollOffset));
            
            if (newScrollOffset != filterScrollOffset) {
                setFilterScrollOffset(newScrollOffset);
                playButtonSound();
            }
            return true;
        }
        return false;
    }
    
    /**
     * Scrolls up by one entry
     */
    private void scrollUp() {
        if (scrollUpSilent()) {
            playButtonSound();
        }
    }
    
    /**
     * Scrolls down by one entry
     */
    private void scrollDown() {
        if (scrollDownSilent()) {
            playButtonSound();
        }
    }
    
    /**
     * Scrolls up silently (without sound)
     */
    private boolean scrollUpSilent() {
        if (MAX_FILTER_SLOTS > VISIBLE_ENTRIES && filterScrollOffset > 0) {
            int newOffset = Math.max(0, filterScrollOffset - 1);
            setFilterScrollOffset(newOffset);
            return true;
        }
        return false;
    }
    
    /**
     * Scrolls down silently (without sound)
     */
    private boolean scrollDownSilent() {
        int maxScrollOffset = Math.max(0, MAX_FILTER_SLOTS - VISIBLE_ENTRIES);
        
        if (MAX_FILTER_SLOTS > VISIBLE_ENTRIES && filterScrollOffset < maxScrollOffset) {
            int newOffset = Math.min(maxScrollOffset, filterScrollOffset + 1);
            setFilterScrollOffset(newOffset);
            return true;
        }
        return false;
    }
    
    /**
     * Sets the filter scroll offset and updates EditBoxes
     */
    private void setFilterScrollOffset(int offset) {
        int maxScrollOffset = Math.max(0, MAX_FILTER_SLOTS - VISIBLE_ENTRIES);
        this.filterScrollOffset = Math.max(0, Math.min(maxScrollOffset, offset));
        // Stop editing if the edited entry is no longer visible
        if (editingFilterIndex >= 0) {
            int visibleIndex = editingFilterIndex - filterScrollOffset;
            if (visibleIndex < 0 || visibleIndex >= VISIBLE_ENTRIES) {
                stopEditingFilter();
            }
        }
        // Update edit buttons when scroll changes
        updateEditButtons();
    }
    
    @Override
    public void onClose() {
        // Save data when closing
        saveFilterData();
        super.onClose();
    }
}
