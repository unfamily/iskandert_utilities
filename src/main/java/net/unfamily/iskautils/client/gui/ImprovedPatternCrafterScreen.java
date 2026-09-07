package net.unfamily.iskautils.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.integration.anotherdynamics.AnotherDynamicsCompat;
import net.unfamily.iskautils.integration.anotherdynamics.client.DeepDrawerSettingsCopierClient;
import net.unfamily.iskautils.network.packet.AutoclearVariablesC2SPacket;
import net.unfamily.iskautils.network.packet.CraftingModeSetC2SPacket;
import net.unfamily.iskautils.network.packet.CraftingModeSwitchC2SPacket;
import net.unfamily.iskautils.network.packet.FilterLetterUpdateC2SPacket;
import net.unfamily.iskautils.network.packet.FilterPageC2SPacket;
import net.unfamily.iskautils.network.packet.MarkInputC2SPacket;
import net.unfamily.iskautils.network.packet.MarkOutputC2SPacket;
import net.unfamily.iskautils.network.packet.MarkFilterSetC2SPacket;
import net.unfamily.iskautils.network.packet.OutputPageC2SPacket;
import net.unfamily.iskautils.network.packet.PatternCellUpdateC2SPacket;
import net.unfamily.iskautils.network.packet.PatternCellItemAssignC2SPacket;
import net.unfamily.iskautils.network.packet.PatternCrafterSettingsCopierC2SPacket;
import net.unfamily.iskautils.network.packet.PatternSwitchC2SPacket;
import net.unfamily.iskautils.network.packet.RecursiveOutputModeC2SPacket;
import net.unfamily.iskautils.network.packet.RemainderRoutingModeC2SPacket;
import net.unfamily.iskautils.network.packet.ToolSafeguardC2SPacket;
import net.unfamily.iskautils.network.packet.ForbiddenFiltersC2SPacket;
import net.unfamily.iskautils.network.packet.VariableFilterSetC2SPacket;
import net.unfamily.iskautils.pattern.PatternData;
import net.unfamily.iskautils.integration.jei.ghost.IIskaUtilsGhostTarget;
import net.unfamily.iskautils.util.DeepDrawerItemFilter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen for the Improved Pattern Crafter.
 * Renders the custom 340x270 GUI background, pattern navigation buttons,
 * a 3x3 pattern grid and Pattern-Crafter-style variable letter labels + 18x18 filter buttons.
 */
public class ImprovedPatternCrafterScreen extends AbstractContainerScreen<ImprovedPatternCrafterMenu>
        implements IIskaUtilsGhostTarget {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID,
                    "textures/gui/backgrounds/pattern_crafter.png");

    private static final Identifier ENERGY_BAR =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID,
                    "textures/gui/energy_bar.png");
    private static final Identifier SINGLE_SLOT =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/single_slot.png");
    private static final Identifier ENTRY_TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/enrty_wide_wide_wide.png");
    private static final Identifier SCROLLBAR_TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/scrollbar.png");

    // Upgrade slot ghost previews (Fan / Structure Placer style via GhostItemRenderer)
    private static final ItemStack GHOST_LOGIC_MODULE = new ItemStack(ModItems.LOGIC_MODULE.get());
    private static final ItemStack GHOST_PRODUCTION_MODULE = new ItemStack(ModItems.PRODUCTION_MODULE.get());
    private static final ItemStack[] SPEED_MODULES = {
            new ItemStack(ModItems.SLOW_MODULE.get()),
            new ItemStack(ModItems.MODERATE_MODULE.get()),
            new ItemStack(ModItems.FAST_MODULE.get()),
            new ItemStack(ModItems.EXTREME_MODULE.get()),
            new ItemStack(ModItems.ULTRA_MODULE.get())
    };
    private int speedModuleCycleIndex = 0;
    private long lastSpeedModuleCycleTime = 0L;
    private static final long SPEED_MODULE_CYCLE_MS = 1000L;

    private static final int GUI_WIDTH = 340;
    private static final int GUI_HEIGHT = 270;

    // Energy bar dimensions (from energy_bar.png: 16x32, first 8px = charged, next 8px = empty)
    private static final int ENERGY_BAR_WIDTH = 8;
    private static final int ENERGY_BAR_HEIGHT = 32;

    // Filter letter labels above/below 18x18 variable edit buttons
    private static final int FILTER_LABEL_WIDTH = 16;
    private static final int FILTER_LABEL_HEIGHT = 14;
    private static final int FILTER_LABEL_GAP = 3;
    private static final int VARIABLE_SLOT_X = ImprovedPatternCrafterMenu.MACHINE_INPUT_X;
    private static final int VARIABLE_SLOT_Y = 47;
    private static final int NAV_BACK_WIDTH = 40;
    private static final int NAV_HELP_DEFAULT_WIDTH = 70;
    private static final int PLAYER_INV_WIDTH = 9 * 18;
    /** Wider than the 3×3 output grid; centered on the output column. */
    private static final int OUTPUT_SIDE_BTN_W = 72;
    private static final int OUTPUT_SIDE_BTN_X =
            ImprovedPatternCrafterMenu.OUTPUT_SLOT_X + (54 - OUTPUT_SIDE_BTN_W) / 2;

    // Close button (X) and redstone mode button
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;
    private static final int CLOSE_BUTTON_Y = 5;
    private Button closeButton;
    private Button redstoneButton;

    // Mark Input under Save in the pattern control column
    private Button markInputButton;
    private Button autoclearVariablesButton;
    private Button markOutputButton;
    private Button prevOutputPageButton;
    private Button nextOutputPageButton;
    private int currentOutputPage;

    // Crafting mode button (above pattern nav)
    private Button craftingModeButton;

    // Pattern navigation buttons
    private Button prevPatternButton;
    private Button patternLabelButton;
    private Button nextPatternButton;
    private Button toolSafeguardButton;
    private Button savePatternButton;
    private Button discardPatternButton;
    private Button forbiddenButton;
    private Button settingsCopierSaveButton;
    private Button settingsCopierLoadButton;
    /** Ticks to wait after settings-copier paste before reloading Forbidden draft from BE. */
    private int pendingForbiddenReloadTicks = 0;
    /** Pending pattern grid letter edits; flushed on Save. */
    private final boolean[] pendingGridDirty = new boolean[9];
    private final int[] pendingGridValues = new int[9];
    /** Immediate JEI ghost stacks for pending pattern cells (until Save). */
    private final ItemStack[] pendingGridDisplays = new ItemStack[9];
    private boolean pendingCraftingModeDirty = false;
    private int pendingCraftingMode = 0;

    /** Recursive output routing (1–3) and unconsumed-input / remainder routing (1–2). */
    private Button recursiveOutputButton;
    private Button remainderRoutingButton;

    /** Filter page: → at end of first row of filter slots, ← below it (when maxKeyInputs > 18). */
    private Button prevFilterPageButton;
    private Button nextFilterPageButton;

    // 3x3 pattern grid cells
    private final PatternCellWidget[][] gridCells = new PatternCellWidget[3][3];

    /** Letter widgets 16x10 (unlock/cycle). Paginated when capability > 18. */
    private PatternCellWidget[] filterLabels = new PatternCellWidget[0];
    /** Vanilla 18x18 buttons showing filter preview; click opens variable editor when unlocked. */
    private ItemIconButton[] variableButtons = new ItemIconButton[0];
    private ItemStack[] variablePreviews = new ItemStack[0];
    /** Absolute editor ghost slot origin (set by createEditModeUI / layout). */
    private int editorGhostAbsX;
    private int editorGhostAbsY;
    private int editorHelpWidth = NAV_HELP_DEFAULT_WIDTH;
    /** Current page of input filter slots (0-based); only used when slot count > 18. */
    private int currentFilterPage = 0;
    /** Cooldown after page change to avoid rapid clicks mixing data (ticks). */
    private int filterPageChangeCooldownTicks = 0;
    private static final int FILTER_PAGE_COOLDOWN_TICKS = 3;
    private int lastClickSlotIndex = -1;
    private long lastClickTime;

    /** MAIN / FORBIDDEN / FILTER_HELP. Variable edit is inline on MAIN (no VARIABLE_EDIT subview). */
    private enum SubView { MAIN, FORBIDDEN, FILTER_HELP }
    private SubView subView = SubView.MAIN;
    private SubView filterHelpReturnView = SubView.MAIN;
    private int editingVariableIndex = -1;
    /** Inline variable-filter editor on MAIN (not a SubView). */
    private boolean variableInlineEdit = false;

    // Forbidden list — wider entry (enrty_wide_wide_wide 220x24), centered with scrollbar.
    private static final int ENTRY_WIDTH = 220;
    private static final int ENTRY_HEIGHT = 24;
    private static final int VISIBLE_ENTRIES = 4;
    private static final int FIRST_ROW_Y = 28; // slightly higher so 4 rows fit above editor
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int HANDLE_SIZE = 8;
    private static final int SCROLLBAR_HEIGHT = 34;
    /** Entry + gap + scrollbar block centered in GUI. */
    private static final int ENTRY_X = (GUI_WIDTH - (ENTRY_WIDTH + 4 + SCROLLBAR_WIDTH)) / 2;
    private static final int SCROLLBAR_X = ENTRY_X + ENTRY_WIDTH + 4;
    private static final int BUTTON_UP_Y = FIRST_ROW_Y;
    private static final int SCROLLBAR_Y = BUTTON_UP_Y + HANDLE_SIZE;
    private static final int BUTTON_DOWN_Y = SCROLLBAR_Y + SCROLLBAR_HEIGHT;
    private static final int MAX_FORBIDDEN_SLOTS = 30;
    private int filterListScroll = 0;
    private boolean isDraggingHandle = false;
    private int dragStartY = 0;
    private int dragStartScrollOffset = 0;
    private EditBox filterEditBox;
    private Button filterApplyButton;
    private Button filterClearButton;
    private Button filterCancelButton;
    private Button filterBackButton;
    private Button filterHelpButton;
    private Button filterPrevVariantButton;
    private Button filterNextVariantButton;
    private final java.util.List<Button> filterEditButtons = new java.util.ArrayList<>();
    private final java.util.List<Button> filterDeleteButtons = new java.util.ArrayList<>();
    private final java.util.List<String> draftForbidden = new java.util.ArrayList<>();
    private String draftVariableFilter = "";
    private boolean nestedFilterEdit;
    private int nestedEditIndex = -1;
    private String nestedOriginalValue = "";
    private ItemStack editorGhostItem = ItemStack.EMPTY;
    private final java.util.List<String> filterVariants = new java.util.ArrayList<>();
    private int filterVariantIndex = 0;
    /** Cached Mark Input bounds (used to place inline variable ghost selector). */
    private int markInputBtnX;
    private int markInputBtnY;
    private int markInputBtnW;
    private int markInputBtnH;

    public ImprovedPatternCrafterScreen(ImprovedPatternCrafterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    public IGhostIngredientConsumer getGhostHandler() {
        return new IGhostItemConsumer() {
            @Override
            public void accept(Object ingredient) {
                if (ingredient instanceof ItemStack stack && isEditorGhostActive()) {
                    acceptEditorGhost(stack);
                }
            }
        };
    }

    @Override
    public Rect2i getGhostTargetArea() {
        if (isEditorGhostActive()) {
            int[] ghost = editorGhostBounds();
            return new Rect2i(ghost[0], ghost[1], ghost[2], ghost[3]);
        }
        return null;
    }

    @Override
    public List<GhostDropTarget> getGhostDropTargets() {
        // Only visible drop zones. Hidden UI must not be registered — JEI must not highlight them.
        if (menu.getBlockEntity() == null) return List.of();
        List<GhostDropTarget> targets = new ArrayList<>();
        var pos = menu.getBlockEntity().getBlockPos();
        if (isEditorGhostActive()) {
            int[] ghost = editorGhostBounds();
            // 18x18 covers the full SINGLE_SLOT texture (same as Extractor / AutoShop).
            targets.add(new GhostDropTarget(
                    new Rect2i(ghost[0], ghost[1], 18, 18),
                    this::acceptEditorGhost));
            return targets;
        }
        if (subView != SubView.MAIN || variableInlineEdit || !menu.areMachineSlotsActive()) {
            return targets;
        }
        for (int i = 0; i < ImprovedPatternCrafterMenu.INPUT_SLOTS; i++) {
            int targetIndex = i;
            Slot slot = menu.getSlot(menu.getInputStart() + targetIndex);
            if (!slot.isActive()) continue;
            // Slot item is 16x16 at (x,y); SINGLE_SLOT frame is 18x18 at (x-1,y-1).
            targets.add(new GhostDropTarget(
                    new Rect2i(leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18),
                    stack -> ClientPacketDistributor.sendToServer(
                            new MarkFilterSetC2SPacket(pos, false, targetIndex, stack.copyWithCount(1)))));
        }
        for (int i = 0; i < ImprovedPatternCrafterMenu.OUTPUT_SLOTS; i++) {
            int real = currentOutputPage * 9 + i;
            if (real >= menu.getOutputSlotCount()) continue;
            Slot slot = menu.getSlot(menu.getOutputStart() + i);
            if (!slot.isActive()) continue;
            targets.add(new GhostDropTarget(
                    new Rect2i(leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18),
                    stack -> ClientPacketDistributor.sendToServer(
                            new MarkFilterSetC2SPacket(pos, true, real, stack.copyWithCount(1)))));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                PatternCellWidget cell = gridCells[row][col];
                if (cell == null || !cell.visible) continue;
                int cellIndex = cell.getCellIndex();
                // Pattern cells are already 18x18 widgets aligned to the slot frame.
                targets.add(new GhostDropTarget(
                        new Rect2i(cell.getX(), cell.getY(), 18, 18),
                        stack -> assignPatternItem(cellIndex, stack)));
            }
        }
        return targets;
    }

    private boolean isEditorGhostActive() {
        return (nestedFilterEdit && subView == SubView.FORBIDDEN)
                || (variableInlineEdit && subView == SubView.MAIN);
    }

    /** Absolute screen bounds for the editor ghost slot: [x, y, w, h]. Always 18x18 texture. */
    private int[] editorGhostBounds() {
        return new int[]{editorGhostAbsX, editorGhostAbsY, 18, 18};
    }

    /**
     * Layout for ← · ghost · → · C · A · ✕ · Valid Keys (Extractor 12px chrome; no Back on this row).
     * Forbidden: centered on the entry list band (Valid Keys keeps default width when it fits).
     * Variable inline: Valid Keys is shortened so the chrome row never exceeds the editbox width below.
     */
    private EditChromeLayout computeEditChromeLayout(boolean variableInline) {
        int buttonSize = ImprovedPatternCrafterMenu.EDIT_BTN_SIZE;
        int slotSize = ImprovedPatternCrafterMenu.EDIT_SLOT_SIZE;
        // Width from left arrow through ✕ + gap before Valid Keys (exclusive of Valid Keys).
        int widthBeforeHelp =
                buttonSize + ImprovedPatternCrafterMenu.EDIT_ARROW_GAP
                        + slotSize + ImprovedPatternCrafterMenu.EDIT_ARROW_GAP
                        + buttonSize + ImprovedPatternCrafterMenu.EDIT_ACTION_GAP
                        + buttonSize + ImprovedPatternCrafterMenu.EDIT_BTN_SPACING
                        + buttonSize + ImprovedPatternCrafterMenu.EDIT_BTN_SPACING
                        + buttonSize
                        + ImprovedPatternCrafterMenu.EDIT_ROW_GAP;

        int helpW = NAV_HELP_DEFAULT_WIDTH;
        int zoneLeft;
        int zoneWidth;
        int ghostY;
        int textBoxX;
        int textBoxY;
        int textBoxW;

        if (variableInline) {
            zoneLeft = this.leftPos + ImprovedPatternCrafterMenu.PLAYER_INV_X;
            zoneWidth = PLAYER_INV_WIDTH;
            // Shrink Valid Keys to fit remaining space in the player-inv / editbox band.
            helpW = Math.max(buttonSize, zoneWidth - widthBeforeHelp);
            int chromeH = slotSize + ImprovedPatternCrafterMenu.EDIT_ROW_GAP
                    + ImprovedPatternCrafterMenu.EDIT_TEXTBOX_HEIGHT;
            int areaY = this.topPos + ImprovedPatternCrafterMenu.MACHINE_INPUT_Y;
            int areaH = 3 * 18;
            int chromeTop = areaY + Math.max(0, (areaH - chromeH) / 2);
            ghostY = chromeTop;
            textBoxY = ghostY + slotSize + ImprovedPatternCrafterMenu.EDIT_ROW_GAP;
            textBoxX = zoneLeft;
            textBoxW = zoneWidth;
        } else {
            zoneLeft = this.leftPos + ENTRY_X;
            zoneWidth = ENTRY_WIDTH + 4 + SCROLLBAR_WIDTH;
            helpW = Math.min(NAV_HELP_DEFAULT_WIDTH, Math.max(buttonSize, zoneWidth - widthBeforeHelp));
            ghostY = this.topPos + ImprovedPatternCrafterMenu.EDIT_MODE_PANEL_Y;
            textBoxX = this.leftPos + ImprovedPatternCrafterMenu.PLAYER_INV_X;
            textBoxY = this.topPos + ImprovedPatternCrafterMenu.EDIT_TEXTBOX_Y;
            textBoxW = PLAYER_INV_WIDTH;
        }

        int totalW = widthBeforeHelp + helpW;
        int leftArrowX = zoneLeft + Math.max(0, (zoneWidth - totalW) / 2);
        int ghostX = leftArrowX + buttonSize + ImprovedPatternCrafterMenu.EDIT_ARROW_GAP;
        int helpX = ghostX + slotSize + ImprovedPatternCrafterMenu.EDIT_ARROW_GAP
                + buttonSize + ImprovedPatternCrafterMenu.EDIT_ACTION_GAP
                + buttonSize + ImprovedPatternCrafterMenu.EDIT_BTN_SPACING
                + buttonSize + ImprovedPatternCrafterMenu.EDIT_BTN_SPACING
                + buttonSize + ImprovedPatternCrafterMenu.EDIT_ROW_GAP;
        int navY = ghostY + (slotSize - buttonSize) / 2;
        return new EditChromeLayout(ghostX, ghostY, textBoxX, textBoxY, textBoxW, helpW, helpX, navY);
    }

    private record EditChromeLayout(
            int ghostX, int ghostY,
            int textBoxX, int textBoxY, int textBoxW,
            int helpW, int helpX, int navY) {}

    private int getVisibleForbiddenEntries() {
        return VISIBLE_ENTRIES;
    }

    @Override
    protected void init() {
        super.init();

        // Close button (X) - top right, like Fan
        closeButton = Button.builder(Component.literal("✕"), btn -> onCloseButtonPressed())
                .bounds(this.leftPos + CLOSE_BUTTON_X, this.topPos + CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
                .build();
        addRenderableWidget(closeButton);

        // Output column: Forbidden → 3×3 → page → Mark Output (aligned to hotbar), with gaps
        int markOutputY = ImprovedPatternCrafterMenu.MARK_OUTPUT_Y;
        int outputPageY = ImprovedPatternCrafterMenu.OUTPUT_PAGE_Y;
        markOutputButton = Button.builder(Component.translatable("gui.iska_utils.mark_output"),
                        btn -> onMarkOutputPressed())
                .bounds(this.leftPos + OUTPUT_SIDE_BTN_X, this.topPos + markOutputY,
                        OUTPUT_SIDE_BTN_W, ImprovedPatternCrafterMenu.MARK_OUTPUT_H)
                .tooltip(Tooltip.create(
                        Component.translatable("gui.iska_utils.mark_output.tooltip.line1")
                                .append(Component.literal("\n"))
                                .append(Component.translatable("gui.iska_utils.mark_output.tooltip.line2"))
                                .append(Component.literal("\n"))
                                .append(Component.translatable("gui.iska_utils.mark_output.tooltip.line3"))))
                .build();
        addRenderableWidget(markOutputButton);

        // Upgrades stacked left; RF then RS to the right, vertically centered on modules
        int upgradeTop = ImprovedPatternCrafterMenu.UPGRADE_SLOT_Y0;
        int upgradeBottom = ImprovedPatternCrafterMenu.UPGRADE_SLOT_Y2 + 16;
        int upgradeCenterY = upgradeTop + (upgradeBottom - upgradeTop) / 2;
        int energyBarX = ImprovedPatternCrafterMenu.UPGRADE_SLOT_X + 18 + 4;
        int energyBarY = upgradeCenterY - ENERGY_BAR_HEIGHT / 2;
        int rsSize = 16;
        int rsX = energyBarX + ENERGY_BAR_WIDTH + 4;
        int rsY = upgradeCenterY - rsSize / 2;
        redstoneButton = addRenderableWidget(MachineGuiButtons.redstoneIconButton(
                this.leftPos + rsX, this.topPos + rsY,
                btn -> cycleRedstoneMode(), menu::getRedstoneMode, true));

        // ===== Pattern column: mode → recursive → remainder → tool → browser → grid → Save =====
        // Nav buttons are wider; Save + 3×3 grid stay centered under them.
        int patternColX = 12;
        int patternNavWidth = 70;
        int gridWidth = 3 * 18; // 54
        int gridPad = (patternNavWidth - gridWidth) / 2;
        int navStartX = this.leftPos + patternColX;
        int gridStartX = navStartX + gridPad;
        int gridStartY = this.topPos + ImprovedPatternCrafterMenu.PATTERN_GRID_Y;
        int btnH = 12;
        int btnGap = 1;
        int arrowWidth = 12;
        int labelWidth = patternNavWidth - arrowWidth * 2;

        // 4 control rows above browser, then browser immediately above grid
        int browserY = gridStartY - btnH - btnGap;
        int toolSafeguardY = browserY - btnH - btnGap;
        int remainderRowY = toolSafeguardY - btnH - btnGap;
        int recursiveRowY = remainderRowY - btnH - btnGap;
        int modeButtonY = recursiveRowY - btnH - btnGap;

        craftingModeButton = Button.builder(Component.translatable("gui.iska_utils.crafting_mode.both"),
                        btn -> cycleCraftingMode())
                .bounds(navStartX, modeButtonY, patternNavWidth, btnH)
                .tooltip(Tooltip.create(
                        Component.translatable("gui.iska_utils.cycle_hint.line1")
                                .append(Component.literal("\n"))
                                .append(Component.translatable("gui.iska_utils.cycle_hint.line2"))
                ))
                .build();
        addRenderableWidget(craftingModeButton);

        recursiveOutputButton = Button.builder(Component.translatable("gui.iska_utils.recursive_outputs.button.1"),
                        btn -> onRecursiveOutputPressed())
                .bounds(navStartX, recursiveRowY, patternNavWidth, btnH)
                .build();
        addRenderableWidget(recursiveOutputButton);

        remainderRoutingButton = Button.builder(Component.translatable("gui.iska_utils.unused_inputs.button.1"),
                        btn -> onRemainderRoutingPressed())
                .bounds(navStartX, remainderRowY, patternNavWidth, btnH)
                .build();
        addRenderableWidget(remainderRoutingButton);

        toolSafeguardButton = Button.builder(Component.translatable("gui.iska_utils.tool_safeguard.on"),
                        btn -> toggleToolSafeguard())
                .bounds(navStartX, toolSafeguardY, patternNavWidth, btnH)
                .build();
        addRenderableWidget(toolSafeguardButton);

        prevPatternButton = Button.builder(Component.literal("←"),
                        btn -> switchPattern(-1))
                .bounds(navStartX, browserY, arrowWidth, btnH)
                .tooltip(Tooltip.create(Component.translatable("gui.iska_utils.pattern_nav.previous")))
                .build();
        addRenderableWidget(prevPatternButton);

        patternLabelButton = Button.builder(Component.literal("1/4"),
                        btn -> resetCurrentPattern())
                .bounds(navStartX + arrowWidth, browserY, labelWidth, btnH)
                .tooltip(Tooltip.create(Component.translatable("gui.iska_utils.reset_pattern_tooltip")))
                .build();
        addRenderableWidget(patternLabelButton);

        nextPatternButton = Button.builder(Component.literal("→"),
                        btn -> switchPattern(1))
                .bounds(navStartX + arrowWidth + labelWidth, browserY, arrowWidth, btnH)
                .tooltip(Tooltip.create(Component.translatable("gui.iska_utils.pattern_nav.next")))
                .build();
        addRenderableWidget(nextPatternButton);

        // Save under the 3x3 grid (same width/X as grid — centered under nav)
        int saveY = gridStartY + 54 + btnGap;
        savePatternButton = Button.builder(Component.translatable("gui.iska_utils.save_pattern"),
                        btn -> savePendingPattern())
                .bounds(gridStartX, saveY, gridWidth, btnH)
                .tooltip(Tooltip.create(Component.translatable("gui.iska_utils.save_pattern.tooltip")))
                .build();
        addRenderableWidget(savePatternButton);

        discardPatternButton = Button.builder(Component.translatable("gui.iska_utils.discard_pattern"),
                        btn -> discardPendingPattern())
                .bounds(gridStartX, saveY + btnH + btnGap, gridWidth, btnH)
                .tooltip(Tooltip.create(Component.translatable("gui.iska_utils.discard_pattern.tooltip")))
                .build();
        addRenderableWidget(discardPatternButton);

        // Mark Input + Autoclear (equal width) just under the machine inventory
        int markGap = 2;
        int halfW = (PLAYER_INV_WIDTH - markGap) / 2;
        markInputBtnH = btnH;
        markInputBtnX = this.leftPos + ImprovedPatternCrafterMenu.PLAYER_INV_X - 1;
        markInputBtnY = this.topPos + ImprovedPatternCrafterMenu.MACHINE_INPUT_Y + 54;
        markInputBtnW = halfW;
        markInputButton = Button.builder(
                        Component.translatable("gui.iska_utils.mark_input"),
                        btn -> onMarkInputPressed())
                .bounds(markInputBtnX, markInputBtnY, markInputBtnW, markInputBtnH)
                .build();
        addRenderableWidget(markInputButton);

        autoclearVariablesButton = Button.builder(
                        Component.translatable("gui.iska_utils.autoclear.on"),
                        btn -> onAutoclearVariablesPressed())
                .bounds(markInputBtnX + markInputBtnW + markGap, markInputBtnY, halfW, markInputBtnH)
                .tooltip(Tooltip.create(Component.translatable("gui.iska_utils.autoclear.tooltip")))
                .build();
        addRenderableWidget(autoclearVariablesButton);

        // Forbidden above the output grid (extra gap — not packed against the slots)
        forbiddenButton = Button.builder(Component.translatable("gui.iska_utils.forbidden_outputs"),
                        btn -> onForbiddenPressed())
                .bounds(this.leftPos + OUTPUT_SIDE_BTN_X, this.topPos + ImprovedPatternCrafterMenu.FORBIDDEN_Y,
                        OUTPUT_SIDE_BTN_W, ImprovedPatternCrafterMenu.OUTPUT_CTRL_H)
                .tooltip(Tooltip.create(Component.translatable("gui.iska_utils.forbidden.tooltip")))
                .build();
        addRenderableWidget(forbiddenButton);

        // 3x3 pattern grid with tooltip
        int cellSize = 18;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int cellIndex = row * 3 + col;
                gridCells[row][col] = new PatternCellWidget(
                        gridStartX + col * cellSize,
                        gridStartY + row * cellSize,
                        cellSize, cellSize,
                        cellIndex,
                        this::onGridCellClick,
                        stack -> assignPatternItem(cellIndex, stack)
                );
                gridCells[row][col].setTooltip(Tooltip.create(
                        Component.translatable("gui.iska_utils.shift_click_clear")
                                .append(Component.literal("\n"))
                                .append(Component.translatable("gui.iska_utils.cycle_hint.line1"))
                                .append(Component.literal("\n"))
                                .append(Component.translatable("gui.iska_utils.cycle_hint.line2"))
                                .append(Component.literal("\n"))
                                .append(Component.translatable("gui.iska_utils.pattern_cell.pending_hint"))
                ));
                addRenderableWidget(gridCells[row][col]);
            }
        }

        // ===== Variable letter labels (16x12) + vanilla 18x18 filter buttons =====
        int labelCount = menu.hasFilterPaginationCapability() ? 18 : menu.getInputFilterSlotCount();
        filterLabels = new PatternCellWidget[labelCount];
        variableButtons = new ItemIconButton[labelCount];
        variablePreviews = new ItemStack[labelCount];
        for (int i = 0; i < labelCount; i++) {
            int row = i / 9;
            int col = i % 9;
            final int localIndex = i;
            variablePreviews[i] = ItemStack.EMPTY;
            int slotX = this.leftPos + VARIABLE_SLOT_X + col * 18;
            int slotY = this.topPos + VARIABLE_SLOT_Y + row * 18;
            int editX = slotX - 1;
            int editY = slotY - 1;
            int labelX = editX + (18 - FILTER_LABEL_WIDTH) / 2;
            int labelY = row == 0
                    ? editY - FILTER_LABEL_GAP - FILTER_LABEL_HEIGHT
                    : editY + 18 + FILTER_LABEL_GAP;
            filterLabels[i] = new PatternCellWidget(
                    labelX, labelY,
                    FILTER_LABEL_WIDTH, FILTER_LABEL_HEIGHT,
                    localIndex,
                    this::onFilterLabelClick);
            addRenderableWidget(filterLabels[i]);

            variableButtons[i] = new ItemIconButton(
                    editX, editY, 18,
                    btn -> openVariableInlineEdit(resolveFilterIndex(localIndex)),
                    () -> variablePreviews[localIndex],
                    Component.empty());
            addRenderableWidget(variableButtons[i]);
        }

        // Filter page arrows: always create when capability (maxKeyInputs) > 18
        if (menu.hasFilterPaginationCapability()) {
            int filterRow1X = this.leftPos + VARIABLE_SLOT_X - 1;
            int filterRow1Y = this.topPos + VARIABLE_SLOT_Y;
            int slotRowW = 9 * 18;
            int btnW = 12;
            int pageBtnH = 14;
            int btnX = filterRow1X + slotRowW + 2;
            nextFilterPageButton = Button.builder(Component.literal("→"), btn -> setFilterPageFromButton(currentFilterPage + 1))
                    .bounds(btnX, filterRow1Y, btnW, pageBtnH)
                    .build();
            addRenderableWidget(nextFilterPageButton);
            prevFilterPageButton = Button.builder(Component.literal("←"), btn -> setFilterPageFromButton(currentFilterPage - 1))
                    .bounds(btnX, filterRow1Y + 18, btnW, pageBtnH)
                    .build();
            addRenderableWidget(prevFilterPageButton);
        } else {
            prevFilterPageButton = null;
            nextFilterPageButton = null;
        }

        prevOutputPageButton = Button.builder(Component.literal("←"), btn -> setOutputPage(currentOutputPage - 1))
                .bounds(this.leftPos + OUTPUT_SIDE_BTN_X, this.topPos + outputPageY, 12,
                        ImprovedPatternCrafterMenu.OUTPUT_CTRL_H)
                .tooltip(Tooltip.create(Component.translatable("gui.iska_utils.output_page.previous")))
                .build();
        nextOutputPageButton = Button.builder(Component.literal("→"), btn -> setOutputPage(currentOutputPage + 1))
                .bounds(this.leftPos + OUTPUT_SIDE_BTN_X + OUTPUT_SIDE_BTN_W - 12, this.topPos + outputPageY, 12,
                        ImprovedPatternCrafterMenu.OUTPUT_CTRL_H)
                .tooltip(Tooltip.create(Component.translatable("gui.iska_utils.output_page.next")))
                .build();
        addRenderableWidget(prevOutputPageButton);
        addRenderableWidget(nextOutputPageButton);

        // Snapshot in-progress edit text before dropping stale widget refs (JEI re-init).
        if (filterEditBox != null) {
            if (variableInlineEdit) {
                draftVariableFilter = filterEditBox.getValue() != null ? filterEditBox.getValue() : "";
            } else if (nestedFilterEdit && nestedEditIndex >= 0) {
                while (draftForbidden.size() <= nestedEditIndex) {
                    draftForbidden.add("");
                }
                draftForbidden.set(nestedEditIndex,
                        filterEditBox.getValue() != null ? filterEditBox.getValue() : "");
            }
        }

        // Edit chrome widgets are created dynamically in createEditModeUI()
        filterBackButton = null;
        filterHelpButton = null;
        filterEditBox = null;
        filterClearButton = null;
        filterApplyButton = null;
        filterCancelButton = null;
        filterPrevVariantButton = null;
        filterNextVariantButton = null;

        applySubViewVisibility();

        // JEI (and some UI transitions) can cause a screen re-init that clears widgets.
        // If we are mid edit-mode, restore the edit widgets without grabbing keyboard focus.
        restoreEditChromeAfterReinit();
        initSettingsCopierButtons();
    }

    private void initSettingsCopierButtons() {
        if (!AnotherDynamicsCompat.isLoaded() || !menu.includesCopierSlot()) {
            return;
        }
        int colX = this.leftPos + ImprovedPatternCrafterMenu.COPIER_COLUMN_X;
        settingsCopierSaveButton = Button.builder(
                        Component.translatable("gui.iska_utils.deep_drawer_extractor.settings_copier.copy"),
                        b -> sendSettingsCopierAction(PatternCrafterSettingsCopierC2SPacket.ACTION_COPY))
                .tooltip(Tooltip.create(DeepDrawerExtractorGuiTooltips.grayLine(
                        "gui.iska_utils.deep_drawer_extractor.settings_copier.copy.tooltip")))
                .bounds(colX, this.topPos + ImprovedPatternCrafterMenu.COPIER_SAVE_BUTTON_Y,
                        ImprovedPatternCrafterMenu.COPIER_ACTION_BUTTON_W, ImprovedPatternCrafterMenu.COPIER_ACTION_BUTTON_H)
                .build();
        settingsCopierLoadButton = Button.builder(
                        Component.translatable("gui.iska_utils.deep_drawer_extractor.settings_copier.paste"),
                        b -> sendSettingsCopierAction(PatternCrafterSettingsCopierC2SPacket.ACTION_PASTE))
                .tooltip(Tooltip.create(DeepDrawerExtractorGuiTooltips.grayLine(
                        "gui.iska_utils.deep_drawer_extractor.settings_copier.paste.tooltip")))
                .bounds(colX, this.topPos + ImprovedPatternCrafterMenu.COPIER_LOAD_BUTTON_Y,
                        ImprovedPatternCrafterMenu.COPIER_ACTION_BUTTON_W, ImprovedPatternCrafterMenu.COPIER_ACTION_BUTTON_H)
                .build();
        addRenderableWidget(settingsCopierSaveButton);
        addRenderableWidget(settingsCopierLoadButton);
        refreshCopierPasteUi();
        applySubViewVisibility();
    }

    private void sendSettingsCopierAction(int action) {
        if (menu.getBlockEntity() == null) {
            return;
        }
        ClientPacketDistributor.sendToServer(new PatternCrafterSettingsCopierC2SPacket(
                menu.getBlockEntity().getBlockPos(), action));
        if (action == PatternCrafterSettingsCopierC2SPacket.ACTION_PASTE) {
            pendingForbiddenReloadTicks = 5;
        }
        refreshCopierPasteUi();
    }

    private void refreshCopierPasteUi() {
        if (settingsCopierLoadButton == null) {
            return;
        }
        settingsCopierLoadButton.active = showsSettingsCopierColumn();
    }

    private void layoutSettingsCopierButtons() {
        if (settingsCopierSaveButton == null || settingsCopierLoadButton == null) {
            return;
        }
        int colX = this.leftPos + ImprovedPatternCrafterMenu.COPIER_COLUMN_X;
        settingsCopierSaveButton.setX(colX);
        settingsCopierSaveButton.setY(this.topPos + ImprovedPatternCrafterMenu.COPIER_SAVE_BUTTON_Y);
        settingsCopierSaveButton.setWidth(ImprovedPatternCrafterMenu.COPIER_ACTION_BUTTON_W);
        settingsCopierSaveButton.setHeight(ImprovedPatternCrafterMenu.COPIER_ACTION_BUTTON_H);
        settingsCopierLoadButton.setX(colX);
        settingsCopierLoadButton.setY(this.topPos + ImprovedPatternCrafterMenu.COPIER_LOAD_BUTTON_Y);
        settingsCopierLoadButton.setWidth(ImprovedPatternCrafterMenu.COPIER_ACTION_BUTTON_W);
        settingsCopierLoadButton.setHeight(ImprovedPatternCrafterMenu.COPIER_ACTION_BUTTON_H);
    }

    private boolean showsSettingsCopierColumn() {
        return AnotherDynamicsCompat.isLoaded()
                && menu.includesCopierSlot()
                && subView == SubView.FORBIDDEN;
    }

    /**
     * Recreates filter edit chrome after {@link #init()} when variable/forbidden edit state survived
     * a JEI-driven rebuild (widgets were cleared but flags/drafts were not).
     */
    private void restoreEditChromeAfterReinit() {
        if (variableInlineEdit && subView == SubView.MAIN && editingVariableIndex >= 0) {
            createEditModeUI(true);
            if (filterEditBox != null) {
                filterEditBox.setValue(draftVariableFilter != null ? draftVariableFilter : "");
                filterEditBox.setFocused(false);
            }
            seedEditorGhostFromFilter(draftVariableFilter);
            applySubViewVisibility();
            return;
        }
        if (subView == SubView.FORBIDDEN) {
            ensureForbiddenNavButtons();
            if (nestedFilterEdit && nestedEditIndex >= 0) {
                createEditModeUI(false);
                String value = nestedEditIndex < draftForbidden.size()
                        ? draftForbidden.get(nestedEditIndex)
                        : nestedOriginalValue;
                if (value == null) {
                    value = "";
                }
                if (filterEditBox != null) {
                    filterEditBox.setValue(value);
                    filterEditBox.setFocused(false);
                }
                seedEditorGhostFromFilter(value);
            }
            applySubViewVisibility();
            updateForbiddenEditButtons();
        }
    }

    /** Number of filter pages based on effective count (18 slots per page). */
    private int getFilterPageCount() {
        int n = liveEffectiveKeyInputCount();
        if (n <= 18) return 1;
        return (n % 18 == 0) ? (n / 18) : (n / 18 + 1);
    }

    private int resolveFilterIndex(int localIndex) {
        return menu.hasFilterPaginationCapability()
                ? currentFilterPage * 18 + localIndex
                : localIndex;
    }

    private void setFilterPage(int page) {
        if (filterPageChangeCooldownTicks > 0) return;
        int maxPage = getFilterPageCount() - 1;
        int newPage = Math.max(0, Math.min(maxPage, page));
        if (newPage == currentFilterPage) return;
        // Block writes on the view so container sync (possibly from previous page) doesn't overwrite the new page's slots
        var viewHandler = menu.getInputFilterViewHandler();
        if (viewHandler != null) viewHandler.setAcceptWrites(false);
        currentFilterPage = newPage;
        filterPageChangeCooldownTicks = FILTER_PAGE_COOLDOWN_TICKS;
        menu.setInputFilterViewOffset(currentFilterPage * 18);
        if (menu.getBlockEntity() != null) {
            ClientPacketDistributor.sendToServer(new FilterPageC2SPacket(menu.getBlockEntity().getBlockPos(), currentFilterPage));
        }
    }

    /** Called by filter page prev/next buttons; updates page and plays sound. */
    private void setFilterPageFromButton(int page) {
        int maxPage = getFilterPageCount() - 1;
        int newPage = Math.max(0, Math.min(maxPage, page));
        if (newPage != currentFilterPage) {
            setFilterPage(newPage);
            playButtonSound();
        }
    }

    // ===== Pattern Actions =====

    private void switchPattern(int direction) {
        if (menu.getBlockEntity() == null) return;
        clearPendingGrid();
        ClientPacketDistributor.sendToServer(
                new PatternSwitchC2SPacket(menu.getBlockEntity().getBlockPos(), direction)
        );
    }

    private void resetCurrentPattern() {
        if (!isShiftDown()) return;
        if (menu.getBlockEntity() == null) return;
        clearPendingGrid();
        ClientPacketDistributor.sendToServer(
                new PatternSwitchC2SPacket(menu.getBlockEntity().getBlockPos(), 0)
        );
    }

    private void toggleToolSafeguard() {
        if (menu.getBlockEntity() != null) {
            ClientPacketDistributor.sendToServer(new ToolSafeguardC2SPacket(menu.getBlockEntity().getBlockPos()));
        }
    }

    private void clearPendingGrid() {
        for (int i = 0; i < pendingGridDirty.length; i++) {
            pendingGridDirty[i] = false;
            pendingGridDisplays[i] = ItemStack.EMPTY;
        }
        pendingCraftingModeDirty = false;
    }

    private boolean hasPendingPatternEdits() {
        if (pendingCraftingModeDirty) return true;
        for (boolean dirty : pendingGridDirty) {
            if (dirty) return true;
        }
        return false;
    }

    /** Discard unsaved pattern grid/mode edits (variables are already committed). */
    private void discardPendingPattern() {
        clearPendingGrid();
        playButtonSound();
    }

    /** Live effective variable slots (updates when Logic Module is inserted without reopening). */
    private int liveEffectiveKeyInputCount() {
        var be = menu.getBlockEntity();
        return be != null ? be.getEffectiveKeyInputCount() : menu.getEffectiveKeyInputCount();
    }

    private int liveFilterLetter(int index) {
        var be = menu.getBlockEntity();
        if (be != null) return be.getFilterLetter(index);
        return menu.getFilterLetter(index);
    }

    private String liveFilterString(int index) {
        var be = menu.getBlockEntity();
        return be != null ? be.getInputFilterString(index) : "";
    }

    /** Effective grid letter including local JEI/manual pending edits. */
    public int getEffectiveGridCell(int cell) {
        if (cell < 0 || cell >= 9) return PatternData.EMPTY;
        return pendingGridDirty[cell] ? pendingGridValues[cell] : menu.getGridCell(cell);
    }

    /**
     * Stage JEI pattern grid + crafting mode until Save/Discard.
     * Variables are already applied on the BE (client + server).
     */
    public void applyJeiPending(int[] letters, int craftingMode) {
        applyJeiPending(letters, craftingMode, null);
    }

    public void applyJeiPending(int[] letters, int craftingMode, @Nullable List<ItemStack> displays) {
        if (letters == null || letters.length < 9) return;
        for (int i = 0; i < 9; i++) {
            pendingGridValues[i] = letters[i];
            pendingGridDirty[i] = true;
            if (displays != null && i < displays.size() && displays.get(i) != null && !displays.get(i).isEmpty()) {
                pendingGridDisplays[i] = displays.get(i).copyWithCount(1);
            } else {
                pendingGridDisplays[i] = ItemStack.EMPTY;
            }
            if (gridCells[i / 3][i % 3] != null) {
                gridCells[i / 3][i % 3].setValue(letters[i]);
                if (!pendingGridDisplays[i].isEmpty()) {
                    gridCells[i / 3][i % 3].setDisplayItems(List.of(pendingGridDisplays[i]));
                }
            }
        }
        pendingCraftingMode = craftingMode;
        pendingCraftingModeDirty = true;
    }

    private void savePendingPattern() {
        if (menu.getBlockEntity() == null) return;
        var pos = menu.getBlockEntity().getBlockPos();
        int patternIndex = menu.getCurrentPatternIndex();
        boolean any = false;
        for (int i = 0; i < 9; i++) {
            if (!pendingGridDirty[i]) continue;
            ClientPacketDistributor.sendToServer(new PatternCellUpdateC2SPacket(
                    pos, patternIndex, i, pendingGridValues[i]));
            pendingGridDirty[i] = false;
            any = true;
        }
        if (pendingCraftingModeDirty) {
            ClientPacketDistributor.sendToServer(new CraftingModeSetC2SPacket(pos, pendingCraftingMode));
            pendingCraftingModeDirty = false;
            any = true;
        }
        if (any) {
            for (int i = 0; i < pendingGridDisplays.length; i++) {
                pendingGridDisplays[i] = ItemStack.EMPTY;
            }
            playButtonSound();
        }
    }

    private void onForbiddenPressed() {
        openForbiddenSubview();
    }

    private void cycleCraftingMode() {
        if (menu.getBlockEntity() != null) {
            ClientPacketDistributor.sendToServer(
                    new CraftingModeSwitchC2SPacket(menu.getBlockEntity().getBlockPos())
            );
        }
    }

    private void onRecursiveOutputPressed() {
        if (menu.getBlockEntity() != null) {
            ClientPacketDistributor.sendToServer(
                    new RecursiveOutputModeC2SPacket(menu.getBlockEntity().getBlockPos())
            );
        }
    }

    private void onRemainderRoutingPressed() {
        if (menu.getBlockEntity() != null) {
            ClientPacketDistributor.sendToServer(
                    new RemainderRoutingModeC2SPacket(menu.getBlockEntity().getBlockPos())
            );
        }
    }

    private void cycleRedstoneMode() {
        if (menu.getBlockEntity() != null) {
            ClientPacketDistributor.sendToServer(
                    new net.unfamily.iskautils.network.packet.RedstoneModeC2SPacket(menu.getBlockEntity().getBlockPos())
            );
        }
    }

    private void onMarkInputPressed() {
        if (menu.getBlockEntity() == null) return;
        int mode = MarkInputC2SPacket.MODE_NORMAL;
        if (isShiftDown()) {
            mode = MarkInputC2SPacket.MODE_SHIFT;
        } else if (isControlDown() || isAltDown()) {
            mode = MarkInputC2SPacket.MODE_CTRL;
        }
        ClientPacketDistributor.sendToServer(new MarkInputC2SPacket(menu.getBlockEntity().getBlockPos(), mode));
    }

    private void onAutoclearVariablesPressed() {
        if (menu.getBlockEntity() == null) return;
        ClientPacketDistributor.sendToServer(new AutoclearVariablesC2SPacket(menu.getBlockEntity().getBlockPos()));
    }

    private void onMarkOutputPressed() {
        if (menu.getBlockEntity() == null) return;
        int mode = isShiftDown() ? MarkInputC2SPacket.MODE_SHIFT
                : (isControlDown() || isAltDown()
                    ? MarkInputC2SPacket.MODE_CTRL : MarkInputC2SPacket.MODE_NORMAL);
        ClientPacketDistributor.sendToServer(new MarkOutputC2SPacket(menu.getBlockEntity().getBlockPos(), mode));
    }

    private void setOutputPage(int page) {
        if (menu.getBlockEntity() == null) return;
        int max = Math.max(0, (menu.getOutputSlotCount() - 1) / 9);
        int next = Math.max(0, Math.min(max, page));
        if (next == currentOutputPage) return;
        currentOutputPage = next;
        menu.setOutputViewOffset(next * 9);
        ClientPacketDistributor.sendToServer(new OutputPageC2SPacket(menu.getBlockEntity().getBlockPos(), next));
        playButtonSound();
    }

    private void onGridCellClick(PatternCellWidget widget) {
        if (menu.getBlockEntity() == null) return;
        int cell = widget.getCellIndex();
        pendingGridValues[cell] = widget.getValue();
        pendingGridDirty[cell] = true;
    }

    private void assignPatternItem(int cellIndex, ItemStack stack) {
        if (menu.getBlockEntity() == null || stack.isEmpty()) return;
        ClientPacketDistributor.sendToServer(new PatternCellItemAssignC2SPacket(
                menu.getBlockEntity().getBlockPos(), cellIndex, stack.copyWithCount(1)));
        int letter = menu.getBlockEntity().previewExactAssignLetter(stack);
        if (letter != PatternData.EMPTY) {
            pendingGridValues[cellIndex] = letter;
            pendingGridDirty[cellIndex] = true;
        }
    }

    private void onFilterLabelClick(PatternCellWidget widget) {
        if (menu.getBlockEntity() == null) return;
        int filterIndex = resolveFilterIndex(widget.getCellIndex());
        if (filterIndex < 0 || filterIndex >= liveEffectiveKeyInputCount()) return;
        ClientPacketDistributor.sendToServer(new FilterLetterUpdateC2SPacket(
                menu.getBlockEntity().getBlockPos(),
                filterIndex,
                widget.getValue()));
        playButtonSound();
    }

    // ===== Sync from ContainerData =====

    @Override
    protected void containerTick() {
        super.containerTick();

        // Update crafting mode button text
        int mode = pendingCraftingModeDirty ? pendingCraftingMode : menu.getCraftingMode();
        String modeKey = switch (mode) {
            case 1 -> "gui.iska_utils.crafting_mode.shaped_only";
            case 2 -> "gui.iska_utils.crafting_mode.shapeless_only";
            default -> "gui.iska_utils.crafting_mode.both";
        };
        craftingModeButton.setMessage(Component.translatable(modeKey));
        craftingModeButton.setTooltip(Tooltip.create(
                Component.translatable("gui.iska_utils.crafting_mode.tooltip." + mode)
                        .append(Component.literal("\n"))
                        .append(Component.translatable("gui.iska_utils.cycle_hint.line1"))
                        .append(Component.literal("\n"))
                        .append(Component.translatable("gui.iska_utils.cycle_hint.line2"))
        ));

        int recursiveMode = menu.getSyncedRecursiveOutputMode();
        recursiveOutputButton.setMessage(Component.translatable("gui.iska_utils.recursive_outputs.button." + recursiveMode));
        recursiveOutputButton.setTooltip(Tooltip.create(
                Component.translatable("gui.iska_utils.recursive_outputs.tooltip." + recursiveMode)
                        .append(Component.literal("\n"))
                        .append(Component.translatable("gui.iska_utils.cycle_hint.line1"))
                        .append(Component.literal("\n"))
                        .append(Component.translatable("gui.iska_utils.cycle_hint.line2"))));

        int remainderMode = menu.getSyncedRemainderRoutingMode();
        remainderRoutingButton.setMessage(Component.translatable("gui.iska_utils.unused_inputs.button." + remainderMode));
        remainderRoutingButton.setTooltip(Tooltip.create(
                Component.translatable("gui.iska_utils.unused_inputs.tooltip." + remainderMode)
                        .append(Component.literal("\n"))
                        .append(Component.translatable("gui.iska_utils.cycle_hint.line1"))
                        .append(Component.literal("\n"))
                        .append(Component.translatable("gui.iska_utils.cycle_hint.line2"))));

        // Update pattern label button text
        int idx = menu.getCurrentPatternIndex();
        int total = menu.getTotalPatterns();
        patternLabelButton.setMessage(Component.literal((idx + 1) + "/" + total));

        boolean safeguard = menu.isToolSafeguardEnabled();
        toolSafeguardButton.setMessage(Component.translatable(
                safeguard ? "gui.iska_utils.tool_safeguard.on" : "gui.iska_utils.tool_safeguard.off"));
        toolSafeguardButton.setTooltip(Tooltip.create(Component.translatable(
                safeguard ? "gui.iska_utils.tool_safeguard.tooltip.on" : "gui.iska_utils.tool_safeguard.tooltip.off")));

        if (autoclearVariablesButton != null) {
            boolean autoclear = menu.isAutoclearVariablesEnabled();
            autoclearVariablesButton.setMessage(Component.translatable(
                    autoclear ? "gui.iska_utils.autoclear.on" : "gui.iska_utils.autoclear.off"));
        }

        if (discardPatternButton != null && discardPatternButton.visible) {
            discardPatternButton.active = hasPendingPatternEdits();
        }

        if (pendingForbiddenReloadTicks > 0) {
            pendingForbiddenReloadTicks--;
            if (pendingForbiddenReloadTicks == 0
                    && subView == SubView.FORBIDDEN
                    && !nestedFilterEdit
                    && menu.getBlockEntity() != null) {
                draftForbidden.clear();
                draftForbidden.addAll(menu.getBlockEntity().getForbiddenFilters());
                ensureForbiddenRows();
                updateForbiddenEditButtons();
            }
        }

        // Update grid cells from synced pattern data (keep local pending edits)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int cellIndex = row * 3 + col;
                int value = pendingGridDirty[cellIndex] ? pendingGridValues[cellIndex] : menu.getGridCell(cellIndex);
                gridCells[row][col].setValue(value);
                List<ItemStack> candidates = new ArrayList<>();
                var blockEntity = menu.getBlockEntity();
                if (blockEntity != null && value > PatternData.EMPTY) {
                    var registries = blockEntity.getLevel() != null ? blockEntity.getLevel().registryAccess() : null;
                    for (int filter = 0; filter < liveEffectiveKeyInputCount(); filter++) {
                        if (liveFilterLetter(filter) != value) continue;
                        String filterStr = liveFilterString(filter);
                        if (filterStr.isEmpty()) continue;
                        ItemStack preview = previewFilterItem(filterStr, registries);
                        if (!preview.isEmpty()) candidates.add(preview);
                    }
                }
                if (candidates.isEmpty() && pendingGridDirty[cellIndex]
                        && pendingGridDisplays[cellIndex] != null && !pendingGridDisplays[cellIndex].isEmpty()) {
                    candidates.add(pendingGridDisplays[cellIndex]);
                }
                gridCells[row][col].setDisplayItems(candidates);
            }
        }

        // Letters A-Z only (26); no limit by slot count
        int maxLetter = PatternData.MAX_LETTER;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                gridCells[row][col].setMaxLetter(maxLetter);
            }
        }
        // When paginated, the menu’s 18 slots are a view over BE; set offset so they show the current page
        int filterCount = liveEffectiveKeyInputCount();
        boolean paginated = menu.hasFilterPaginationCapability();
        boolean pagesActive = filterCount > 18;
        if (filterPageChangeCooldownTicks > 0) {
            filterPageChangeCooldownTicks--;
            if (filterPageChangeCooldownTicks == 0) {
                var viewHandler = menu.getInputFilterViewHandler();
                if (viewHandler != null) viewHandler.setAcceptWrites(true);
            }
        }
        if (paginated) {
            int pageCount = getFilterPageCount();
            currentFilterPage = Math.max(0, Math.min(menu.getSyncedFilterPage(), pageCount - 1));
            menu.setInputFilterViewOffset(currentFilterPage * 18);
            if (prevFilterPageButton != null) {
                prevFilterPageButton.visible = subView == SubView.MAIN;
                prevFilterPageButton.active = pagesActive && currentFilterPage > 0;
            }
            if (nextFilterPageButton != null) {
                nextFilterPageButton.visible = subView == SubView.MAIN;
                nextFilterPageButton.active = pagesActive && currentFilterPage < pageCount - 1;
            }
        }
        currentOutputPage = Math.max(0, Math.min((menu.getOutputSlotCount() - 1) / 9, menu.getSyncedOutputPage()));
        menu.setOutputViewOffset(currentOutputPage * 9);
        prevOutputPageButton.active = currentOutputPage > 0;
        nextOutputPageButton.active = currentOutputPage < (menu.getOutputSlotCount() - 1) / 9;
        for (int i = 0; i < variableButtons.length; i++) {
            int slotIndex = paginated ? currentFilterPage * 18 + i : i;
            boolean showMain = subView == SubView.MAIN;
            if (slotIndex >= filterCount) {
                if (variableButtons[i] != null) {
                    variableButtons[i].visible = false;
                    variableButtons[i].active = false;
                }
                if (filterLabels[i] != null) {
                    filterLabels[i].visible = false;
                }
                variablePreviews[i] = ItemStack.EMPTY;
                continue;
            }
            ItemStack preview = ItemStack.EMPTY;
            int letterValue = liveFilterLetter(slotIndex);
            if (menu.getBlockEntity() != null) {
                String filterStr = liveFilterString(slotIndex);
                preview = previewFilterItem(filterStr,
                        menu.getBlockEntity().getLevel() != null
                                ? menu.getBlockEntity().getLevel().registryAccess() : null);
            }
            variablePreviews[i] = preview;
            if (variableButtons[i] != null) {
                variableButtons[i].visible = showMain;
                // Inactive letter: disabled look; Shift+click clear still handled in mouseClicked.
                boolean letterActive = letterValue > PatternData.EMPTY;
                variableButtons[i].active = showMain && letterActive;
                Component varTip;
                if (!letterActive) {
                    varTip = Component.translatable("gui.iska_utils.variable_locked_hint")
                            .append(Component.literal("\n"))
                            .append(Component.translatable("gui.iska_utils.variable_shift_clear_only"));
                } else {
                    varTip = Component.translatable("gui.iska_utils.variable_open_tooltip")
                            .append(Component.literal("\n"))
                            .append(Component.translatable("gui.iska_utils.variable_shift_clear_filter"));
                }
                variableButtons[i].setTooltip(Tooltip.create(varTip));
            }
            if (filterLabels[i] != null) {
                filterLabels[i].visible = showMain;
                filterLabels[i].setValue(letterValue);
                filterLabels[i].setMaxLetter(PatternData.MAX_LETTER);
                filterLabels[i].active = showMain;
                if (letterValue <= PatternData.EMPTY) {
                    filterLabels[i].setTooltip(Tooltip.create(
                            Component.translatable("gui.iska_utils.cycle_hint.line1")
                                    .append(Component.literal("\n"))
                                    .append(Component.translatable("gui.iska_utils.cycle_hint.line2"))
                    ));
                } else if (preview.isEmpty()) {
                    filterLabels[i].setTooltip(Tooltip.create(
                            Component.translatable("gui.iska_utils.shift_click_clear")
                                    .append(Component.literal("\n"))
                                    .append(Component.translatable("gui.iska_utils.filter_any_item"))
                                    .append(Component.literal("\n"))
                                    .append(Component.translatable("gui.iska_utils.filter_excluding_others"))
                                    .append(Component.literal("\n"))
                                    .append(Component.translatable("gui.iska_utils.cycle_hint.line1"))
                                    .append(Component.literal("\n"))
                                    .append(Component.translatable("gui.iska_utils.cycle_hint.line2"))
                    ));
                } else {
                    filterLabels[i].setTooltip(Tooltip.create(
                            Component.translatable("gui.iska_utils.shift_click_clear")
                                    .append(Component.literal("\n"))
                                    .append(Component.translatable("gui.iska_utils.filter_allowed_item",
                                            preview.getHoverName()))
                                    .append(Component.literal("\n"))
                                    .append(Component.translatable("gui.iska_utils.cycle_hint.line1"))
                                    .append(Component.literal("\n"))
                                    .append(Component.translatable("gui.iska_utils.cycle_hint.line2"))
                    ));
                }
            }
        }
    }

    private void onCloseButtonPressed() {
        if (subView == SubView.FORBIDDEN) {
            subView = SubView.MAIN;
            nestedFilterEdit = false;
            nestedEditIndex = -1;
            resetEditorGhost();
            removeEditModeUI();
            applySubViewVisibility();
            return;
        }
        if (subView == SubView.FILTER_HELP) {
            closeSubview();
            return;
        }
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.closeContainer();
        }
    }

    // ===== Rendering =====

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);

        if (subView != SubView.MAIN) {
            // Full cover over machine UI down to player inventory
            guiGraphics.fill(x + 4, y + 16, x + this.imageWidth - 4, y + ImprovedPatternCrafterMenu.PLAYER_INV_Y - 2, 0xFFC6C6C6);
            if (subView == SubView.FORBIDDEN) {
                renderForbiddenEntries(guiGraphics, mouseX, mouseY);
                renderForbiddenScrollbar(guiGraphics, mouseX, mouseY);
            }
            if (nestedFilterEdit) {
                renderEditorGhostFull(guiGraphics);
            }
            return;
        }

        if (variableInlineEdit) {
            // Cover only the machine input inventory (keep variables / upgrades / output / RF / redstone).
            int inX = x + ImprovedPatternCrafterMenu.MACHINE_INPUT_X - 1;
            int inY = y + ImprovedPatternCrafterMenu.MACHINE_INPUT_Y - 1;
            guiGraphics.fill(inX, inY, inX + 9 * 18 + 1, inY + 3 * 18 + 1, 0xFFC6C6C6);
            renderEditorGhostFull(guiGraphics);
            // Fall through to draw output/upgrade frames below.
        }

        // Variable filters: letter widgets + 18x18 buttons (no ghost-slot frames).
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SINGLE_SLOT,
                        x + ImprovedPatternCrafterMenu.OUTPUT_SLOT_X - 1 + col * 18,
                        y + ImprovedPatternCrafterMenu.OUTPUT_SLOT_Y - 1 + row * 18,
                        0, 0, 18, 18, 18, 18);
            }
        }
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SINGLE_SLOT, x + ImprovedPatternCrafterMenu.UPGRADE_SLOT_X - 1,
                y + ImprovedPatternCrafterMenu.UPGRADE_SLOT_Y0 - 1, 0, 0, 18, 18, 18, 18);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SINGLE_SLOT, x + ImprovedPatternCrafterMenu.UPGRADE_SLOT_X - 1,
                y + ImprovedPatternCrafterMenu.UPGRADE_SLOT_Y1 - 1, 0, 0, 18, 18, 18, 18);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SINGLE_SLOT, x + ImprovedPatternCrafterMenu.UPGRADE_SLOT_X - 1,
                y + ImprovedPatternCrafterMenu.UPGRADE_SLOT_Y2 - 1, 0, 0, 18, 18, 18, 18);

        // Ghosts / lock under slots + carried cursor item (must be before extractCarriedItem).
        if (menu.getBlockEntity() != null) {
            renderUpgradeSlotOverlays(guiGraphics);
        }
        if (!variableInlineEdit) {
            renderMarkInputGhosts(guiGraphics);
        }
        renderMarkOutputGhosts(guiGraphics);
        renderUpgradeLockOverlays(guiGraphics);
    }

    /** Input filter slots are visual-only letter+button widgets; skip menu ghost slot contents. */
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
    protected void renderSlotContents(GuiGraphicsExtractor guiGraphics, ItemStack stack, Slot slot, @org.jetbrains.annotations.Nullable String count) {
        if (menu.copySettingsSlotIndex() >= 0 && slot.index == menu.copySettingsSlotIndex()) {
            return;
        }
        if (subView != SubView.MAIN && slot.index < menu.getPlayerInvStart()) {
            return;
        }
        if (variableInlineEdit
                && slot.index >= menu.getInputStart() && slot.index < menu.getInputEnd()) {
            return;
        }
        if (slot.index >= ImprovedPatternCrafterMenu.INPUT_FILTER_START && slot.index < menu.getInputFilterEnd()) {
            return;
        }
        super.renderSlotContents(guiGraphics, stack, slot, count);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        if (subView != SubView.MAIN) {
            renderFilterListSubview(guiGraphics, mouseX, mouseY);
            renderSettingsCopierItem(guiGraphics, mouseX, mouseY);
        }

        if (subView == SubView.MAIN && this.menu.getMaxEnergyStored() > 0) {
            renderEnergyBar(guiGraphics);
            renderEnergyTooltip(guiGraphics, mouseX, mouseY);
        }

        if (subView == SubView.MAIN && redstoneButton != null && redstoneButton.isHovered()) {
            queueTooltip(guiGraphics,
                    List.of(MachineGuiButtons.redstoneTooltip(menu.getRedstoneMode(), true)), mouseX, mouseY);
        }

        // Mark Input button tooltip (same style as Structure Placer set_inventory)
        if (!variableInlineEdit) {
            renderMarkInputTooltip(guiGraphics, mouseX, mouseY);
        }

    }

    private boolean isCopierSlotHovered(double mouseX, double mouseY) {
        int idx = menu.copySettingsSlotIndex();
        if (idx < 0 || !showsSettingsCopierColumn()) {
            return false;
        }
        Slot slot = menu.getSlot(idx);
        return slot.isActive() && isHovering(slot.x, slot.y, ImprovedPatternCrafterMenu.COPIER_SLOT_SIZE,
                ImprovedPatternCrafterMenu.COPIER_SLOT_SIZE, mouseX, mouseY);
    }

    private void renderSettingsCopierItem(GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        if (!showsSettingsCopierColumn()) {
            return;
        }
        int idx = menu.copySettingsSlotIndex();
        if (idx < 0) {
            return;
        }
        int frameX = this.leftPos + ImprovedPatternCrafterMenu.COPIER_COLUMN_X;
        int frameY = this.topPos + ImprovedPatternCrafterMenu.COPIER_SLOT_BACKGROUND_Y;
        int iconX = this.leftPos + ImprovedPatternCrafterMenu.copierSlotItemX(ImprovedPatternCrafterMenu.COPIER_COLUMN_X);
        int iconY = this.topPos + ImprovedPatternCrafterMenu.copierSlotItemY(ImprovedPatternCrafterMenu.COPIER_SLOT_BACKGROUND_Y);
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
        int x = this.leftPos + ImprovedPatternCrafterMenu.copierSlotHighlightX(ImprovedPatternCrafterMenu.COPIER_COLUMN_X);
        int y = this.topPos + ImprovedPatternCrafterMenu.copierSlotHighlightY(ImprovedPatternCrafterMenu.COPIER_SLOT_BACKGROUND_Y);
        int size = ImprovedPatternCrafterMenu.COPIER_SLOT_HIGHLIGHT_SIZE;
        graphics.fillGradient(x, y, x + size, y + size, COPIER_SLOT_HOVER_COLOR, COPIER_SLOT_HOVER_COLOR);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractTooltip(guiGraphics, mouseX, mouseY);
        if (subView == SubView.MAIN) {
            renderEmptyUpgradeSlotTooltips(guiGraphics, mouseX, mouseY);
        }
    }

    /** Fan-style: show supported module name when hovering an empty upgrade slot. */
    private void renderEmptyUpgradeSlotTooltips(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        for (int i = 0; i < 3; i++) {
            Slot slot = menu.getSlot(menu.getUpgradeStart() + i);
            if (!isMouseOverSlot(slot, mouseX, mouseY)) {
                continue;
            }
            Component tip = GuiSlotLock.isLocked(slot)
                    ? GuiSlotLock.lockedTooltip()
                    : (slot.getItem().isEmpty() ? currentUpgradeGhost(i).getHoverName() : null);
            if (tip == null) {
                continue;
            }
            guiGraphics.setTooltipForNextFrame(
                    this.font,
                    List.of(tip.getVisualOrderText()),
                    DefaultTooltipPositioner.INSTANCE,
                    mouseX,
                    mouseY,
                    true);
            return;
        }
    }

    private boolean isMouseOverSlot(Slot slot, int mouseX, int mouseY) {
        int x = this.leftPos + slot.x;
        int y = this.topPos + slot.y;
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    private void renderEditorGhostSlot(GuiGraphicsExtractor guiGraphics) {
        int[] g = editorGhostBounds();
        // Same as Extractor: blit 18x18 texture at the JEI target origin (not offset -1).
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SINGLE_SLOT, g[0], g[1], 0, 0, 18, 18, 18, 18);
    }

    /** Slot frame + item in extractBackground so JEI drag highlight paints above them. */
    private void renderEditorGhostFull(GuiGraphicsExtractor guiGraphics) {
        renderEditorGhostSlot(guiGraphics);
        if (!editorGhostItem.isEmpty()) {
            int[] g = editorGhostBounds();
            guiGraphics.item(editorGhostItem, g[0] + 1, g[1] + 1);
            guiGraphics.itemDecorations(this.font, editorGhostItem, g[0] + 1, g[1] + 1);
        }
    }

    private void renderEditorGhostItem(GuiGraphicsExtractor guiGraphics) {
        renderEditorGhostFull(guiGraphics);
    }

    /**
     * Empty upgrade slots: Fan-style semi-transparent ItemStack ghosts.
     */
    private ItemStack currentUpgradeGhost(int slotIndex) {
        if (slotIndex == 0) return GHOST_LOGIC_MODULE;
        if (slotIndex == 2) return GHOST_PRODUCTION_MODULE;
        long now = System.currentTimeMillis();
        if (now - lastSpeedModuleCycleTime >= SPEED_MODULE_CYCLE_MS) {
            speedModuleCycleIndex = (speedModuleCycleIndex + 1) % SPEED_MODULES.length;
            lastSpeedModuleCycleTime = now;
        }
        return SPEED_MODULES[speedModuleCycleIndex];
    }

    private void renderUpgradeSlotOverlays(GuiGraphicsExtractor guiGraphics) {
        int slotX = ImprovedPatternCrafterMenu.UPGRADE_SLOT_X;
        int[] ys = {
                ImprovedPatternCrafterMenu.UPGRADE_SLOT_Y0,
                ImprovedPatternCrafterMenu.UPGRADE_SLOT_Y1,
                ImprovedPatternCrafterMenu.UPGRADE_SLOT_Y2
        };
        for (int i = 0; i < 3; i++) {
            Slot slot = menu.getSlot(menu.getUpgradeStart() + i);
            if (GuiSlotLock.isLocked(slot) || !slot.getItem().isEmpty()) {
                continue;
            }
            GhostItemRenderer.render(guiGraphics, currentUpgradeGhost(i),
                    this.leftPos + slotX, this.topPos + ys[i], GuiGhostItem.DEFAULT_ARGB);
        }
    }

    private void renderUpgradeLockOverlays(GuiGraphicsExtractor guiGraphics) {
        for (int i = 0; i < 3; i++) {
            GuiSlotLock.renderIfLocked(guiGraphics, this.leftPos, this.topPos,
                    menu.getSlot(menu.getUpgradeStart() + i));
        }
    }

    /**
     * Renders ghost items (semi-transparent) in the 27 input slots when the slot has a mark-input
     * filter but is empty. Same technique as Structure Placer Machine / Modular Fan.
     */
    private void renderMarkInputGhosts(GuiGraphicsExtractor guiGraphics) {
        for (int slot = 0; slot < 27; slot++) {
            if (menu.hasMarkInputFilter(slot)) {
                net.minecraft.world.inventory.Slot guiSlot = menu.getSlot(menu.getInputStart() + slot);
                if (guiSlot.getItem().isEmpty()) {
                    ItemStack ghostFilter = menu.getMarkInputFilter(slot);
                    if (!ghostFilter.isEmpty()) {
                        renderMarkGhostItem(guiGraphics, ghostFilter, guiSlot.x, guiSlot.y);
                    }
                }
            }
        }
    }

    private void renderMarkOutputGhosts(GuiGraphicsExtractor guiGraphics) {
        for (int local = 0; local < 9; local++) {
            int real = currentOutputPage * 9 + local;
            if (real >= menu.getOutputSlotCount() || !menu.hasMarkOutputFilter(real)) continue;
            Slot guiSlot = menu.getSlot(menu.getOutputStart() + local);
            if (guiSlot.getItem().isEmpty()) {
                renderMarkGhostItem(guiGraphics, menu.getMarkOutputFilter(real), guiSlot.x, guiSlot.y);
            }
        }
    }

    /** Fan / Structure Placer ghost style. */
    private void renderMarkGhostItem(GuiGraphicsExtractor guiGraphics, ItemStack itemStack, int x, int y) {
        GhostItemRenderer.render(guiGraphics, itemStack, leftPos + x, topPos + y, GuiGhostItem.DEFAULT_ARGB);
    }

    /**
     * Renders the energy bar to the left of the upgrade slots.
     * Texture layout: 16x32 total, left 8px = charged, right 8px = empty.
     * Fills from bottom to top based on current energy percentage.
     */
    private int energyBarX() {
        return this.leftPos + ImprovedPatternCrafterMenu.UPGRADE_SLOT_X + 18 + 4;
    }

    private int energyBarY() {
        int top = ImprovedPatternCrafterMenu.UPGRADE_SLOT_Y0;
        int bottom = ImprovedPatternCrafterMenu.UPGRADE_SLOT_Y2 + 16;
        return this.topPos + top + (bottom - top - ENERGY_BAR_HEIGHT) / 2;
    }

    private void renderEnergyBar(GuiGraphicsExtractor guiGraphics) {
        int energyBarX = energyBarX();
        int energyBarY = energyBarY();

        // Draw empty bar background (right half of texture: x=8)
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ENERGY_BAR, energyBarX, energyBarY,
                8, 0,
                ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT,
                16, 32);

        // Draw filled bar from bottom up (left half of texture: x=0)
        int energy = this.menu.getEnergyStored();
        int maxEnergy = this.menu.getMaxEnergyStored();

        if (energy > 0 && maxEnergy > 0) {
            int energyHeight = (energy * ENERGY_BAR_HEIGHT) / maxEnergy;
            int energyY = energyBarY + (ENERGY_BAR_HEIGHT - energyHeight);

            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ENERGY_BAR, energyBarX, energyY,
                    0, ENERGY_BAR_HEIGHT - energyHeight,
                    ENERGY_BAR_WIDTH, energyHeight,
                    16, 32);
        }
    }

    /**
     * Renders the energy tooltip when hovering over the energy bar.
     */
    private void renderEnergyTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int energyBarX = energyBarX();
        int energyBarY = energyBarY();

        if (mouseX >= energyBarX && mouseX < energyBarX + ENERGY_BAR_WIDTH
                && mouseY >= energyBarY && mouseY < energyBarY + ENERGY_BAR_HEIGHT) {
            int energy = this.menu.getEnergyStored();
            int maxEnergy = this.menu.getMaxEnergyStored();
            guiGraphics.setTooltipForNextFrame(this.font,
                    List.of(Component.literal(String.format("%,d / %,d RF", energy, maxEnergy))
                            .withStyle(ChatFormatting.RED).getVisualOrderText()),
                    DefaultTooltipPositioner.INSTANCE, mouseX, mouseY, true);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (MachineGuiInput.clearEditBoxOnRightClick(mouseX, mouseY, button, filterEditBox)) {
            clearFilterEdit();
            return true;
        }
        // Shift+LMB clears filter content even when the button looks inactive (no letter).
        if (subView == SubView.MAIN && button == 0 && isShiftDown() && menu.getBlockEntity() != null) {
            for (int i = 0; i < variableButtons.length; i++) {
                ItemIconButton btn = variableButtons[i];
                // isMouseOver requires active; check bounds so inactive (no-letter) buttons still clear.
                if (btn == null || !btn.visible
                        || mouseX < btn.getX() || mouseY < btn.getY()
                        || mouseX >= btn.getX() + btn.getWidth()
                        || mouseY >= btn.getY() + btn.getHeight()) {
                    continue;
                }
                int slotIndex = resolveFilterIndex(i);
                if (slotIndex < 0 || slotIndex >= liveEffectiveKeyInputCount()) continue;
                int letter = liveFilterLetter(slotIndex);
                ClientPacketDistributor.sendToServer(new VariableFilterSetC2SPacket(
                        menu.getBlockEntity().getBlockPos(), slotIndex, "", letter));
                variablePreviews[i] = ItemStack.EMPTY;
                playButtonSound();
                return true;
            }
        }
        if (subView == SubView.FORBIDDEN && button == 0) {
            if (handleScrollbarClick(mouseX, mouseY)) {
                return true;
            }
        }
        if (isEditorGhostActive() && button == 0) {
            int[] g = editorGhostBounds();
            if (mouseX >= g[0] && mouseX < g[0] + g[2]
                    && mouseY >= g[1] && mouseY < g[1] + g[3]) {
                ItemStack carried = minecraft != null && minecraft.player != null
                        ? minecraft.player.containerMenu.getCarried() : ItemStack.EMPTY;
                if (!carried.isEmpty()) {
                    acceptEditorGhost(carried);
                    playButtonSound();
                    return true;
                }
                if (!editorGhostItem.isEmpty()) {
                    resetEditorGhost();
                    if (filterEditBox != null) filterEditBox.setValue("");
                    playButtonSound();
                    return true;
                }
            }
        }
        if (subView != SubView.MAIN) {
            if (hoveredSlot != null && hoveredSlot.index < menu.getPlayerInvStart()) {
                return true;
            }
        } else if (variableInlineEdit) {
            // Block only machine INPUT inventory clicks; upgrades/outputs stay usable.
            if (hoveredSlot != null
                    && hoveredSlot.index >= menu.getInputStart()
                    && hoveredSlot.index < menu.getInputEnd()) {
                return true;
            }
        }
        if (subView == SubView.MAIN && button == 0 && !isShiftDown() && hoveredSlot != null) {
            int slotIndex = hoveredSlot.index;
            boolean machineInput = !variableInlineEdit
                    && slotIndex >= menu.getInputStart() && slotIndex < menu.getInputEnd();
            boolean machineOutput = slotIndex >= menu.getOutputStart() && slotIndex < menu.getOutputEnd();
            if (machineInput || machineOutput) {
                long now = System.currentTimeMillis();
                boolean isDoubleClick = slotIndex == lastClickSlotIndex && now - lastClickTime <= 250L;
                lastClickSlotIndex = slotIndex;
                lastClickTime = now;
                if (isDoubleClick && hoveredSlot.getItem().isEmpty() && menu.getBlockEntity() != null) {
                    int local = machineInput ? slotIndex - menu.getInputStart() : slotIndex - menu.getOutputStart();
                    int real = machineOutput ? currentOutputPage * 9 + local : local;
                    boolean hasMark = machineOutput
                            ? menu.hasMarkOutputFilter(real) : menu.hasMarkInputFilter(real);
                    if (hasMark) {
                        ClientPacketDistributor.sendToServer(new MarkFilterSetC2SPacket(
                                menu.getBlockEntity().getBlockPos(), machineOutput, real, ItemStack.EMPTY));
                        return true;
                    }
                }
            }
        }
        // Right-click support for cycle buttons: previous = two forward cycles (for 3-state cycles)
        if (button == 1 && subView == SubView.MAIN) {
            if (craftingModeButton != null && craftingModeButton.isHovered()) {
                playButtonSound();
                cycleCraftingMode();
                cycleCraftingMode();
                return true;
            }
            if (recursiveOutputButton != null && recursiveOutputButton.isHovered()) {
                playButtonSound();
                onRecursiveOutputPressed();
                onRecursiveOutputPressed();
                return true;
            }
            if (remainderRoutingButton != null && remainderRoutingButton.isHovered()) {
                playButtonSound();
                onRemainderRoutingPressed();
                return true;
            }
            if (redstoneButton != null && redstoneButton.isHovered()) {
                playButtonSound();
                cycleRedstoneMode();
                cycleRedstoneMode();
                cycleRedstoneMode();
                cycleRedstoneMode();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && isDraggingHandle) {
            isDraggingHandle = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        int visible = getVisibleForbiddenEntries();
        if (event.button() == 0 && isDraggingHandle && getForbiddenSlotCount() > visible) {
            int maxScrollOffset = Math.max(0, getForbiddenSlotCount() - visible);
            int deltaY = (int) event.y() - dragStartY;
            float scrollRatio = (float) deltaY / (SCROLLBAR_HEIGHT - HANDLE_SIZE);
            int newOffset = dragStartScrollOffset + Math.round(scrollRatio * maxScrollOffset);
            filterListScroll = Math.max(0, Math.min(maxScrollOffset, newOffset));
            updateForbiddenEditButtons();
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (subView == SubView.FORBIDDEN) {
            ensureForbiddenRows();
            int visible = getVisibleForbiddenEntries();
            int maxScroll = Math.max(0, getForbiddenSlotCount() - visible);
            if (maxScroll > 0) {
                filterListScroll = (int) Math.max(0, Math.min(maxScroll, filterListScroll - Math.signum(scrollY)));
                updateForbiddenEditButtons();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (MachineGuiInput.handleContainerKeyPressed(
                this, event, isDraggingHandle, filterEditBox)) {
            return true;
        }
        int keyCode = event.key();
        boolean inventoryKey = minecraft != null && minecraft.options.keyInventory.matches(event);
        boolean escape = keyCode == 256;
        if (variableInlineEdit && (escape || inventoryKey)) {
            // Esc / inventory close edit only when EditBox is not focused (handled above).
            exitVariableInlineEdit(true);
            return true;
        }
        if (subView != SubView.MAIN && (escape || inventoryKey)) {
            closeSubview();
            return true;
        }
        return super.keyPressed(event);
    }

    private static boolean isShiftDown() {
        var window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private static boolean isControlDown() {
        var window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    private static boolean isAltDown() {
        var window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT);
    }

    private void playButtonSound() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private void renderMarkInputTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (markInputButton != null && markInputButton.isHovered()) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.iska_utils.mark_input.tooltip.line1"));
            lines.add(Component.translatable("gui.iska_utils.mark_input.tooltip.line2"));
            lines.add(Component.translatable("gui.iska_utils.mark_input.tooltip.line3"));
            queueTooltip(guiGraphics, lines, mouseX, mouseY);
        }
    }

    private void queueTooltip(GuiGraphicsExtractor graphics, List<Component> lines, int mouseX, int mouseY) {
        graphics.setTooltipForNextFrame(this.font,
                lines.stream().map(Component::getVisualOrderText).toList(),
                DefaultTooltipPositioner.INSTANCE, mouseX, mouseY, true);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        // Title centered at top of GUI
        int titleW = this.font.width(this.title);
        int titleX = (this.imageWidth - titleW) / 2;
        guiGraphics.text(this.font, this.title, titleX, 6, GuiTextColors.TITLE, false);

        if (subView != SubView.MAIN) return;

        int maxPages = Math.max(1, (menu.getOutputSlotCount() + 8) / 9);
        String outputPage = (currentOutputPage + 1) + "/" + maxPages;
        int outputPageY = ImprovedPatternCrafterMenu.OUTPUT_PAGE_Y + 3;
        int outputPageX = OUTPUT_SIDE_BTN_X + (OUTPUT_SIDE_BTN_W - this.font.width(outputPage)) / 2;
        guiGraphics.text(this.font, outputPage, outputPageX, outputPageY, GuiTextColors.TITLE, false);
    }

    /**
     * Display item for Valid Keys filter strings (Extractor / Another-Dynamics parity):
     * {@code -id}, {@code #tag}, {@code @mod}, {@code ?nbt}, {@code &macro}, bare id.
     */
    private static ItemStack previewFilterItem(String filter, net.minecraft.core.HolderLookup.Provider registries) {
        if (filter == null || filter.trim().isEmpty()) {
            return ItemStack.EMPTY;
        }
        String trimmed = filter.trim();

        // ID filter: -minecraft:diamond
        if (trimmed.startsWith("-")) {
            try {
                var id = Identifier.tryParse(trimmed.substring(1));
                if (id == null) return ItemStack.EMPTY;
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id);
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    return new ItemStack(item);
                }
            } catch (Exception ignored) {}
            return ItemStack.EMPTY;
        }

        // Tag filter: #c:ingots (cycle samples)
        if (trimmed.startsWith("#")) {
            return getItemForTag(trimmed.substring(1));
        }

        // Mod ID filter: @iska_utils (cycle samples)
        if (trimmed.startsWith("@")) {
            return getItemForMod(trimmed.substring(1));
        }

        // NBT / component filter: ?...
        if (trimmed.startsWith("?")) {
            return new ItemStack(net.minecraft.world.item.Items.KNOWLEDGE_BOOK);
        }

        // Macro filter: &enchanted, &damaged, …
        if (trimmed.startsWith("&")) {
            String macro = trimmed.substring(1).trim().toLowerCase();
            if (macro.equals("enchanted") || macro.startsWith("enchanted")) {
                return new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
            }
            if (macro.equals("damaged") || macro.startsWith("damaged")) {
                ItemStack stack = new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
                stack.setDamageValue(stack.getMaxDamage() / 2);
                return stack;
            }
            if (macro.startsWith("temperature")) {
                return new ItemStack(net.minecraft.world.item.Items.BLAZE_POWDER);
            }
            if (macro.startsWith("light")) {
                return new ItemStack(net.minecraft.world.item.Items.LANTERN);
            }
            if (macro.startsWith("tint")) {
                return new ItemStack(net.minecraft.world.item.Items.RED_DYE);
            }
            // Unspecified macros: knowledge book (Valid Keys / Another-Dynamics)
            return new ItemStack(net.minecraft.world.item.Items.KNOWLEDGE_BOOK);
        }

        // Command-style bracket filters for display only
        if (trimmed.startsWith("minecraft:enchanted_book[")) {
            return new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
        }

        // Bare item id
        try {
            var id = Identifier.tryParse(trimmed);
            if (id != null) {
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id);
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    return new ItemStack(item);
                }
            }
        } catch (Exception ignored) {}

        // Last resort: scan registry with typed matcher (covers odd Valid Keys forms)
        if (registries != null && DeepDrawerItemFilter.usesTypedFilterSyntax(trimmed)) {
            int checked = 0;
            for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                ItemStack stack = new ItemStack(item);
                if (DeepDrawerItemFilter.matchesFilterEntry(stack, trimmed, registries)) {
                    return stack;
                }
                if (++checked > 512) break;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack getItemForTag(String tagId) {
        try {
            var tagLocation = Identifier.tryParse(tagId);
            if (tagLocation == null) return ItemStack.EMPTY;
            var itemTag = net.minecraft.tags.ItemTags.create(tagLocation);
            java.util.List<net.minecraft.world.item.Item> items = new java.util.ArrayList<>();
            for (var holder : net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(itemTag)) {
                items.add(holder.value());
            }
            if (!items.isEmpty()) {
                int index = (int) ((System.currentTimeMillis() / 3500L) % items.size());
                return new ItemStack(items.get(index));
            }
        } catch (Exception ignored) {}
        return ItemStack.EMPTY;
    }

    private static ItemStack getItemForMod(String modId) {
        java.util.List<net.minecraft.world.item.Item> modItems = new java.util.ArrayList<>();
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            var itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            if (itemId != null && itemId.getNamespace().startsWith(modId)) {
                modItems.add(item);
            }
        }
        if (!modItems.isEmpty()) {
            int index = (int) ((System.currentTimeMillis() / 3500L) % modItems.size());
            return new ItemStack(modItems.get(index));
        }
        return ItemStack.EMPTY;
    }


    private void openForbiddenSubview() {
        if (variableInlineEdit) {
            exitVariableInlineEdit(true);
        }
        draftForbidden.clear();
        if (menu.getBlockEntity() != null) {
            draftForbidden.addAll(menu.getBlockEntity().getForbiddenFilters());
        }
        ensureForbiddenRows();
        filterListScroll = 0;
        nestedFilterEdit = false;
        nestedEditIndex = -1;
        editingVariableIndex = -1;
        subView = SubView.FORBIDDEN;
        resetEditorGhost();
        removeEditModeUI();
        applySubViewVisibility();
        updateForbiddenEditButtons();
    }

    private void openVariableInlineEdit(int index) {
        if (index < 0 || index >= liveEffectiveKeyInputCount()) return;
        if (liveFilterLetter(index) <= PatternData.EMPTY) return;
        editingVariableIndex = index;
        var be = menu.getBlockEntity();
        draftVariableFilter = liveFilterString(index);
        nestedOriginalValue = draftVariableFilter;
        variableInlineEdit = true;
        nestedFilterEdit = false;
        nestedEditIndex = -1;
        resetEditorGhost();
        createEditModeUI(true);
        if (filterEditBox != null) filterEditBox.setValue(draftVariableFilter);
        seedEditorGhostFromFilter(draftVariableFilter);
        applySubViewVisibility();
        playButtonSound();
    }

    private void exitVariableInlineEdit(boolean discard) {
        if (discard && filterEditBox != null) {
            // discard local draft; server unchanged
        }
        variableInlineEdit = false;
        editingVariableIndex = -1;
        nestedOriginalValue = "";
        resetEditorGhost();
        removeEditModeUI();
        applySubViewVisibility();
    }

    private void openFilterHelp() {
        filterHelpReturnView = subView;
        boolean keepVariable = variableInlineEdit;
        removeEditModeUI();
        subView = SubView.FILTER_HELP;
        // preserve variableInlineEdit flag so Back restores it
        if (!keepVariable) variableInlineEdit = false;
        applySubViewVisibility();
    }

    private void closeSubview() {
        if (subView == SubView.FILTER_HELP) {
            subView = filterHelpReturnView;
            if (subView == SubView.FORBIDDEN) {
                createEditModeUI(false);
                if (nestedFilterEdit) {
                    // restore forbidden edit chrome
                    createEditModeUI(false);
                    if (filterEditBox != null && nestedEditIndex >= 0 && nestedEditIndex < draftForbidden.size()) {
                        filterEditBox.setValue(draftForbidden.get(nestedEditIndex));
                        seedEditorGhostFromFilter(draftForbidden.get(nestedEditIndex));
                    }
                }
            } else if (variableInlineEdit) {
                createEditModeUI(true);
                if (filterEditBox != null) filterEditBox.setValue(draftVariableFilter);
                seedEditorGhostFromFilter(draftVariableFilter);
            }
            applySubViewVisibility();
            if (subView == SubView.FORBIDDEN) updateForbiddenEditButtons();
            return;
        }
        if (nestedFilterEdit && subView == SubView.FORBIDDEN) {
            cancelNestedEdit();
            return;
        }
        subView = SubView.MAIN;
        nestedFilterEdit = false;
        nestedEditIndex = -1;
        resetEditorGhost();
        removeEditModeUI();
        applySubViewVisibility();
    }

    private void applySubViewVisibility() {
        boolean main = subView == SubView.MAIN;
        boolean help = subView == SubView.FILTER_HELP;
        boolean forbidden = subView == SubView.FORBIDDEN;
        boolean editing = (variableInlineEdit && main) || (nestedFilterEdit && forbidden);
        // During variable edit keep upgrades / RF / redstone / output / variables usable.
        boolean showMainSide = main; // includes variableInlineEdit
        boolean showPatternColumn = main;
        boolean normalMain = main && !variableInlineEdit;

        // Close (X) stays available on Forbidden / filter help as well as MAIN.
        if (closeButton != null) closeButton.visible = true;
        if (markInputButton != null) {
            markInputButton.visible = main;
            markInputButton.active = normalMain;
        }
        if (autoclearVariablesButton != null) {
            autoclearVariablesButton.visible = main;
            autoclearVariablesButton.active = normalMain;
        }
        if (markOutputButton != null) {
            markOutputButton.visible = showMainSide;
            markOutputButton.active = showMainSide;
        }
        if (redstoneButton != null) {
            redstoneButton.visible = showMainSide;
            redstoneButton.active = showMainSide;
        }
        if (craftingModeButton != null) craftingModeButton.visible = showPatternColumn;
        if (prevPatternButton != null) prevPatternButton.visible = showPatternColumn;
        if (patternLabelButton != null) patternLabelButton.visible = showPatternColumn;
        if (nextPatternButton != null) nextPatternButton.visible = showPatternColumn;
        if (recursiveOutputButton != null) recursiveOutputButton.visible = showPatternColumn;
        if (remainderRoutingButton != null) remainderRoutingButton.visible = showPatternColumn;
        if (toolSafeguardButton != null) toolSafeguardButton.visible = showPatternColumn;
        if (savePatternButton != null) savePatternButton.visible = showPatternColumn;
        if (discardPatternButton != null) {
            discardPatternButton.visible = showPatternColumn;
            discardPatternButton.active = showPatternColumn && hasPendingPatternEdits();
        }
        if (forbiddenButton != null) {
            forbiddenButton.visible = showMainSide;
            forbiddenButton.active = normalMain;
        }
        if (prevOutputPageButton != null) {
            prevOutputPageButton.visible = showMainSide;
            prevOutputPageButton.active = showMainSide && currentOutputPage > 0;
        }
        if (nextOutputPageButton != null) {
            nextOutputPageButton.visible = showMainSide;
            nextOutputPageButton.active = showMainSide
                    && currentOutputPage < (menu.getOutputSlotCount() - 1) / 9;
        }
        if (prevFilterPageButton != null) prevFilterPageButton.visible = showMainSide;
        if (nextFilterPageButton != null) nextFilterPageButton.visible = showMainSide;
        for (PatternCellWidget[] row : gridCells) {
            for (PatternCellWidget cell : row) {
                if (cell != null) cell.visible = showPatternColumn;
            }
        }
        for (PatternCellWidget label : filterLabels) {
            if (label != null) label.visible = showMainSide;
        }
        for (ItemIconButton btn : variableButtons) {
            if (btn != null) btn.visible = showMainSide;
        }

        // Forbidden SubView: hide all machine slots. Variable edit: only hide machine INPUT inventory.
        menu.setMachineSlotsActive(main);
        menu.setMachineInventoryActive(normalMain);

        boolean showEditChrome = editing && !help;
        if (filterEditBox != null) {
            filterEditBox.visible = showEditChrome;
            filterEditBox.active = showEditChrome;
        }
        if (filterApplyButton != null) filterApplyButton.visible = showEditChrome;
        if (filterClearButton != null) filterClearButton.visible = showEditChrome;
        if (filterCancelButton != null) filterCancelButton.visible = showEditChrome;
        if (filterPrevVariantButton != null) filterPrevVariantButton.visible = showEditChrome;
        if (filterNextVariantButton != null) filterNextVariantButton.visible = showEditChrome;

        // Forbidden idle / FILTER_HELP: Back sits where Valid Keys is when Valid Keys is hidden.
        // Forbidden/variable edit: Valid Keys on that slot, no Back beside it.
        if (forbidden && !help && !nestedFilterEdit) {
            ensureForbiddenNavButtons();
            if (filterHelpButton != null) filterHelpButton.visible = false;
            if (filterBackButton != null) filterBackButton.visible = true;
        } else if (showEditChrome) {
            if (filterHelpButton != null) filterHelpButton.visible = true;
            if (filterBackButton != null) filterBackButton.visible = false;
        } else if (help) {
            ensureFilterHelpBackButton();
            if (filterHelpButton != null) filterHelpButton.visible = false;
            if (filterBackButton != null) filterBackButton.visible = true;
        } else {
            if (filterHelpButton != null) filterHelpButton.visible = false;
            if (filterBackButton != null) filterBackButton.visible = false;
        }

        if (forbidden && !help) {
            updateForbiddenEditButtons();
        } else {
            clearForbiddenEditButtons();
        }

        boolean showCopier = showsSettingsCopierColumn();
        menu.setSettingsCopierActive(showCopier);
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
    }

    /**
     * Forbidden idle: Back at the Valid Keys slot (Valid Keys only appears during edit chrome).
     */
    private void ensureForbiddenNavButtons() {
        EditChromeLayout layout = computeEditChromeLayout(false);
        editorGhostAbsX = layout.ghostX();
        editorGhostAbsY = layout.ghostY();
        editorHelpWidth = layout.helpW();
        int btnH = ImprovedPatternCrafterMenu.EDIT_BTN_SIZE;
        int y = layout.navY();
        int slotX = layout.helpX();
        if (filterBackButton == null) {
            filterBackButton = Button.builder(Component.translatable("gui.iska_utils.back"), btn -> {
                        subView = SubView.MAIN;
                        removeEditModeUI();
                        applySubViewVisibility();
                    })
                    .bounds(slotX, y, layout.helpW(), btnH)
                    .build();
            addRenderableWidget(filterBackButton);
        } else {
            filterBackButton.setX(slotX);
            filterBackButton.setY(y);
            filterBackButton.setWidth(layout.helpW());
            filterBackButton.setHeight(btnH);
        }
        // Keep Valid Keys widget ready for edit mode at the same slot (hidden while idle).
        if (filterHelpButton == null) {
            filterHelpButton = Button.builder(Component.translatable("gui.iska_utils.valid_keys"), btn -> openFilterHelp())
                    .bounds(slotX, y, layout.helpW(), btnH)
                    .build();
            addRenderableWidget(filterHelpButton);
        } else {
            filterHelpButton.setX(slotX);
            filterHelpButton.setY(y);
            filterHelpButton.setWidth(layout.helpW());
            filterHelpButton.setHeight(btnH);
        }
        filterHelpButton.visible = false;
    }

    /** FILTER_HELP: Back at the same Valid Keys slot used by Forbidden chrome. */
    private void ensureFilterHelpBackButton() {
        EditChromeLayout layout = computeEditChromeLayout(filterHelpReturnView == SubView.MAIN && variableInlineEdit);
        int btnH = ImprovedPatternCrafterMenu.EDIT_BTN_SIZE;
        int y = layout.navY();
        int slotX = layout.helpX();
        if (filterBackButton != null) {
            removeWidget(filterBackButton);
            filterBackButton = null;
        }
        filterBackButton = Button.builder(Component.translatable("gui.iska_utils.back"), btn -> closeSubview())
                .bounds(slotX, y, layout.helpW(), btnH)
                .build();
        addRenderableWidget(filterBackButton);
    }

    private void enterNestedEdit(int index) {
        nestedFilterEdit = true;
        nestedEditIndex = index;
        while (draftForbidden.size() <= index) draftForbidden.add("");
        nestedOriginalValue = draftForbidden.get(index);
        resetEditorGhost();
        createEditModeUI(false);
        if (filterEditBox != null) filterEditBox.setValue(nestedOriginalValue);
        seedEditorGhostFromFilter(nestedOriginalValue);
        // Clamp scroll so visible window fits with fewer rows
        int maxScroll = Math.max(0, getForbiddenSlotCount() - getVisibleForbiddenEntries());
        filterListScroll = Math.min(filterListScroll, maxScroll);
        applySubViewVisibility();
    }

    private void cancelNestedEdit() {
        if (subView == SubView.FORBIDDEN && nestedEditIndex >= 0 && nestedEditIndex < draftForbidden.size()) {
            draftForbidden.set(nestedEditIndex, nestedOriginalValue);
        }
        nestedFilterEdit = false;
        nestedEditIndex = -1;
        resetEditorGhost();
        removeEditModeUI();
        applySubViewVisibility();
        updateForbiddenEditButtons();
    }

    private void applyFilterEdit() {
        if (menu.getBlockEntity() == null || filterEditBox == null) return;
        String value = filterEditBox.getValue() == null ? "" : filterEditBox.getValue().trim();
        if (subView == SubView.FORBIDDEN && nestedFilterEdit) {
            while (draftForbidden.size() <= nestedEditIndex) draftForbidden.add("");
            if (value.isEmpty()) {
                if (nestedEditIndex >= 0 && nestedEditIndex < draftForbidden.size()
                        && !draftForbidden.get(nestedEditIndex).isEmpty()) {
                    draftForbidden.remove(nestedEditIndex);
                }
            } else if (nestedEditIndex >= 0) {
                draftForbidden.set(nestedEditIndex, value);
            } else {
                draftForbidden.add(value);
            }
            if (draftForbidden.isEmpty() || !draftForbidden.get(draftForbidden.size() - 1).isEmpty()) {
                draftForbidden.add("");
            }
            sendForbiddenDraft();
            nestedFilterEdit = false;
            nestedEditIndex = -1;
            resetEditorGhost();
            removeEditModeUI();
            applySubViewVisibility();
            updateForbiddenEditButtons();
        } else if (variableInlineEdit && editingVariableIndex >= 0) {
            draftVariableFilter = value;
            int letter = menu.getFilterLetter(editingVariableIndex);
            if (letter <= 0) letter = 1;
            ClientPacketDistributor.sendToServer(new VariableFilterSetC2SPacket(
                    menu.getBlockEntity().getBlockPos(),
                    editingVariableIndex,
                    draftVariableFilter,
                    letter));
            exitVariableInlineEdit(false);
        }
    }

    private void clearFilterEdit() {
        MachineGuiInput.clearEditBox(filterEditBox);
        editorGhostItem = ItemStack.EMPTY;
        filterVariants.clear();
        filterVariantIndex = 0;
    }

    private void sendForbiddenDraft() {
        if (menu.getBlockEntity() == null) return;
        java.util.List<String> compact = new java.util.ArrayList<>();
        for (String s : draftForbidden) {
            if (s != null && !s.isBlank()) compact.add(s.trim());
        }
        ClientPacketDistributor.sendToServer(new ForbiddenFiltersC2SPacket(
                menu.getBlockEntity().getBlockPos(), compact));
    }

    private void resetEditorGhost() {
        editorGhostItem = ItemStack.EMPTY;
        filterVariants.clear();
        filterVariantIndex = 0;
    }

    /**
     * When opening an existing {@code -id} filter, load that item into the selector so the player
     * can cycle to tags / other variants. Leaves textbox unchanged; empty / non-id filters stay empty.
     */
    private void seedEditorGhostFromFilter(String filter) {
        ItemStack fromId = net.unfamily.iskautils.util.DeepDrawerFilterVariants.itemStackFromIdFilter(filter);
        if (fromId.isEmpty()) {
            return;
        }
        editorGhostItem = fromId.copyWithCount(1);
        var registries = minecraft != null && minecraft.level != null ? minecraft.level.registryAccess() : null;
        filterVariants.clear();
        filterVariants.addAll(net.unfamily.iskautils.util.DeepDrawerFilterVariants.generateAllFilterVariants(
                editorGhostItem, registries));
        filterVariantIndex = net.unfamily.iskautils.util.DeepDrawerFilterVariants.indexOfVariant(
                filterVariants, filter);
    }

    private void acceptEditorGhost(ItemStack stack) {
        if (!isEditorGhostActive() || stack == null || stack.isEmpty()) return;
        editorGhostItem = stack.copyWithCount(1);
        var registries = minecraft != null && minecraft.level != null ? minecraft.level.registryAccess() : null;
        filterVariants.clear();
        filterVariants.addAll(net.unfamily.iskautils.util.DeepDrawerFilterVariants.generateAllFilterVariants(
                editorGhostItem, registries));
        filterVariantIndex = 0;
        if (filterEditBox != null && !filterVariants.isEmpty()) {
            filterEditBox.setValue(filterVariants.get(0));
            filterEditBox.setCursorPosition(0);
            filterEditBox.setHighlightPos(0);
        }
    }

    private void cycleFilterVariant(int direction) {
        if (filterVariants.isEmpty()) return;
        filterVariantIndex = Math.floorMod(filterVariantIndex + direction, filterVariants.size());
        if (filterEditBox != null) {
            filterEditBox.setValue(filterVariants.get(filterVariantIndex));
            filterEditBox.setCursorPosition(0);
            filterEditBox.setHighlightPos(0);
        }
    }

    /**
     * Creates edit chrome (Extractor 12px proportions). Valid Keys ends the row (no Back beside it).
     * Forbidden Valid Keys X/Y match idle nav from {@link #ensureForbiddenNavButtons()}.
     */
    private void createEditModeUI(boolean variableInline) {
        removeEditModeUI();

        EditChromeLayout layout = computeEditChromeLayout(variableInline);
        editorGhostAbsX = layout.ghostX();
        editorGhostAbsY = layout.ghostY();
        editorHelpWidth = layout.helpW();

        int buttonSize = ImprovedPatternCrafterMenu.EDIT_BTN_SIZE;
        int buttonSpacing = ImprovedPatternCrafterMenu.EDIT_BTN_SPACING;
        int arrowSpacing = ImprovedPatternCrafterMenu.EDIT_ARROW_GAP;
        int editActionGap = ImprovedPatternCrafterMenu.EDIT_ACTION_GAP;
        int slotSize = ImprovedPatternCrafterMenu.EDIT_SLOT_SIZE;

        int slotX = layout.ghostX();
        int slotY = layout.ghostY();

        int leftButtonX = slotX - buttonSize - arrowSpacing;
        int leftButtonY = slotY + (slotSize - buttonSize) / 2;
        filterPrevVariantButton = Button.builder(Component.literal("←"), btn -> cycleFilterVariant(-1))
                .bounds(leftButtonX, leftButtonY, buttonSize, buttonSize)
                .build();
        addRenderableWidget(filterPrevVariantButton);

        int rightButtonX = slotX + slotSize + arrowSpacing;
        int rightButtonY = slotY + (slotSize - buttonSize) / 2;
        filterNextVariantButton = Button.builder(Component.literal("→"), btn -> cycleFilterVariant(1))
                .bounds(rightButtonX, rightButtonY, buttonSize, buttonSize)
                .build();
        addRenderableWidget(filterNextVariantButton);

        int buttonAfterSlotY = slotY + (slotSize - buttonSize) / 2;
        int clearButtonX = rightButtonX + buttonSize + editActionGap;
        int applyButtonX = clearButtonX + buttonSize + buttonSpacing;
        int closeButtonX = applyButtonX + buttonSize + buttonSpacing;

        filterEditBox = new EditBox(this.font, layout.textBoxX(), layout.textBoxY(),
                layout.textBoxW(), ImprovedPatternCrafterMenu.EDIT_TEXTBOX_HEIGHT, Component.empty());
        filterEditBox.setMaxLength(512);
        filterEditBox.setVisible(true);
        filterEditBox.setEditable(true);
        addRenderableWidget(filterEditBox);

        filterClearButton = Button.builder(Component.literal("C"), btn -> clearFilterEdit())
                .bounds(clearButtonX, buttonAfterSlotY, buttonSize, buttonSize)
                .tooltip(Tooltip.create(Component.translatable("gui.iska_utils.deep_drawer_extractor.clear")))
                .build();
        addRenderableWidget(filterClearButton);

        filterApplyButton = Button.builder(Component.literal("A"), btn -> applyFilterEdit())
                .bounds(applyButtonX, buttonAfterSlotY, buttonSize, buttonSize)
                .tooltip(Tooltip.create(Component.translatable("gui.iska_utils.deep_drawer_extractor.apply")))
                .build();
        addRenderableWidget(filterApplyButton);

        filterCancelButton = Button.builder(Component.literal("✕"),
                        btn -> {
                            if (variableInlineEdit) exitVariableInlineEdit(true);
                            else cancelNestedEdit();
                        })
                .bounds(closeButtonX, buttonAfterSlotY, buttonSize, buttonSize)
                .tooltip(Tooltip.create(Component.translatable("gui.iska_utils.deep_drawer_extractor.close_without_saving")))
                .build();
        addRenderableWidget(filterCancelButton);

        int navH = buttonSize;
        int navY = layout.navY();
        filterHelpButton = Button.builder(Component.translatable("gui.iska_utils.valid_keys"), btn -> openFilterHelp())
                .bounds(layout.helpX(), navY, layout.helpW(), navH)
                .build();
        addRenderableWidget(filterHelpButton);
        // No Back beside Valid Keys — ✕ cancels edit; Forbidden idle puts Back in this slot.
    }

    private void removeEditModeUI() {
        if (filterEditBox != null) { removeWidget(filterEditBox); filterEditBox = null; }
        if (filterPrevVariantButton != null) { removeWidget(filterPrevVariantButton); filterPrevVariantButton = null; }
        if (filterNextVariantButton != null) { removeWidget(filterNextVariantButton); filterNextVariantButton = null; }
        if (filterClearButton != null) { removeWidget(filterClearButton); filterClearButton = null; }
        if (filterApplyButton != null) { removeWidget(filterApplyButton); filterApplyButton = null; }
        if (filterCancelButton != null) { removeWidget(filterCancelButton); filterCancelButton = null; }
        if (filterHelpButton != null) { removeWidget(filterHelpButton); filterHelpButton = null; }
        if (filterBackButton != null) { removeWidget(filterBackButton); filterBackButton = null; }
        editorGhostItem = ItemStack.EMPTY;
        filterVariants.clear();
        filterVariantIndex = 0;
    }

    private void renderFilterListSubview(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (subView == SubView.FILTER_HELP) {
            String[] keys = {
                    "gui.iska_utils.general_filter_text.id",
                    "gui.iska_utils.general_filter_text.modid",
                    "gui.iska_utils.general_filter_text.tag",
                    "gui.iska_utils.general_filter_text.nbt",
                    "gui.iska_utils.general_filter_text.macro",
                    "gui.iska_utils.general_filter_text.usage"
            };
            int y = this.topPos + 40;
            for (String key : keys) {
                guiGraphics.text(this.font, Component.translatable(key), this.leftPos + 12, y, GuiTextColors.BODY, false);
                y += 12;
            }
            return;
        }

        if (nestedFilterEdit) {
            // Ghost already drawn in extractBackground (JEI highlight must stay above).
        }
    }

    private void renderForbiddenEntries(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        ensureForbiddenRows();
        int visible = getVisibleForbiddenEntries();
        var registries = minecraft != null && minecraft.level != null ? minecraft.level.registryAccess() : null;
        for (int i = 0; i < visible; i++) {
            int filterIndex = filterListScroll + i;
            if (filterIndex >= getForbiddenSlotCount()) break;
            int entryX = this.leftPos + ENTRY_X;
            int entryY = this.topPos + FIRST_ROW_Y + i * ENTRY_HEIGHT;
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ENTRY_TEXTURE, entryX, entryY, 0, 0, ENTRY_WIDTH, ENTRY_HEIGHT, ENTRY_WIDTH, ENTRY_HEIGHT);

            String filter = filterIndex < draftForbidden.size() ? draftForbidden.get(filterIndex) : "";
            if (filter == null) filter = "";

            int slotX = entryX + 3;
            int slotY = entryY + 3;
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SINGLE_SLOT, slotX, slotY, 0, 0, 18, 18, 18, 18);
            ItemStack displayItem = previewFilterItem(filter, registries);
            if (!displayItem.isEmpty()) {
                guiGraphics.item(displayItem, slotX + 1, slotY + 1);
                guiGraphics.itemDecorations(this.font, displayItem, slotX + 1, slotY + 1);
            }

            int textX = slotX + 18 + 6;
            int textY = entryY + (ENTRY_HEIGHT - this.font.lineHeight) / 2;
            int buttonSize = ImprovedPatternCrafterMenu.EDIT_BTN_SIZE;
            int buttonMargin = 5;
            int buttonSpacing = ImprovedPatternCrafterMenu.EDIT_BTN_SPACING;
            int editButtonX = entryX + ENTRY_WIDTH - buttonMargin - buttonSize;
            int deleteButtonX = editButtonX - buttonSize - buttonSpacing;
            int maxTextWidth = deleteButtonX - textX - 5;
            String displayText = filter;
            if (!displayText.isEmpty() && this.font.width(displayText) > maxTextWidth) {
                displayText = this.font.plainSubstrByWidth(displayText, maxTextWidth - this.font.width("...")) + "...";
            }
            if (!displayText.isEmpty()) {
                guiGraphics.text(this.font, Component.literal(displayText), textX, textY, GuiTextColors.BODY, false);
            }
        }
    }

    private void renderForbiddenScrollbar(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int visible = getVisibleForbiddenEntries();
        int guiX = this.leftPos;
        int guiY = this.topPos;

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, guiY + SCROLLBAR_Y, 0, 0,
                SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT, 32, 34);

        boolean upHovered = mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH
                && mouseY >= guiY + BUTTON_UP_Y && mouseY < guiY + BUTTON_UP_Y + HANDLE_SIZE;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, guiY + BUTTON_UP_Y,
                SCROLLBAR_WIDTH * 2, (float) (upHovered ? HANDLE_SIZE : 0), HANDLE_SIZE, HANDLE_SIZE, 32, 34);

        boolean downHovered = mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH
                && mouseY >= guiY + BUTTON_DOWN_Y && mouseY < guiY + BUTTON_DOWN_Y + HANDLE_SIZE;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, guiY + BUTTON_DOWN_Y,
                SCROLLBAR_WIDTH * 3, (float) (downHovered ? HANDLE_SIZE : 0), HANDLE_SIZE, HANDLE_SIZE, 32, 34);

        int maxScrollOffset = Math.max(0, getForbiddenSlotCount() - visible);
        if (maxScrollOffset > 0) {
            double scrollRatio = (double) filterListScroll / maxScrollOffset;
            int handleY = guiY + SCROLLBAR_Y + (int) (scrollRatio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
            boolean handleHovered = mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + HANDLE_SIZE
                    && mouseY >= handleY && mouseY < handleY + HANDLE_SIZE;
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, handleY,
                    (float) SCROLLBAR_WIDTH, (float) (handleHovered ? HANDLE_SIZE : 0),
                    HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        }
    }

    private boolean handleScrollbarClick(double mouseX, double mouseY) {
        int visible = getVisibleForbiddenEntries();
        int guiX = this.leftPos;
        int guiY = this.topPos;
        int maxScrollOffset = Math.max(0, getForbiddenSlotCount() - visible);

        if (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH
                && mouseY >= guiY + BUTTON_UP_Y && mouseY < guiY + BUTTON_UP_Y + HANDLE_SIZE) {
            filterListScroll = Math.max(0, filterListScroll - 1);
            updateForbiddenEditButtons();
            playButtonSound();
            return true;
        }
        if (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH
                && mouseY >= guiY + BUTTON_DOWN_Y && mouseY < guiY + BUTTON_DOWN_Y + HANDLE_SIZE) {
            filterListScroll = Math.min(maxScrollOffset, filterListScroll + 1);
            updateForbiddenEditButtons();
            playButtonSound();
            return true;
        }
        if (maxScrollOffset > 0) {
            double scrollRatio = (double) filterListScroll / maxScrollOffset;
            int handleY = guiY + SCROLLBAR_Y + (int) (scrollRatio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
            if (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + HANDLE_SIZE
                    && mouseY >= handleY && mouseY < handleY + HANDLE_SIZE) {
                isDraggingHandle = true;
                dragStartY = (int) mouseY;
                dragStartScrollOffset = filterListScroll;
                return true;
            }
            if (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH
                    && mouseY >= guiY + SCROLLBAR_Y && mouseY < guiY + SCROLLBAR_Y + SCROLLBAR_HEIGHT) {
                float clickRatio = (float) (mouseY - (guiY + SCROLLBAR_Y)) / SCROLLBAR_HEIGHT;
                filterListScroll = Math.max(0, Math.min(maxScrollOffset, Math.round(clickRatio * maxScrollOffset)));
                updateForbiddenEditButtons();
                return true;
            }
        }
        return false;
    }

    private void clearForbiddenEditButtons() {
        for (Button button : filterEditButtons) {
            removeWidget(button);
        }
        filterEditButtons.clear();
        for (Button button : filterDeleteButtons) {
            removeWidget(button);
        }
        filterDeleteButtons.clear();
    }

    private void updateForbiddenEditButtons() {
        clearForbiddenEditButtons();
        if (subView != SubView.FORBIDDEN || subView == SubView.FILTER_HELP) return;
        ensureForbiddenRows();

        int buttonSize = ImprovedPatternCrafterMenu.EDIT_BTN_SIZE;
        int buttonMargin = 5;
        int buttonSpacing = ImprovedPatternCrafterMenu.EDIT_BTN_SPACING;
        int visible = getVisibleForbiddenEntries();
        for (int i = 0; i < visible; i++) {
            int filterIndex = filterListScroll + i;
            if (filterIndex >= getForbiddenSlotCount()) break;
            int entryX = this.leftPos + ENTRY_X;
            int entryY = this.topPos + FIRST_ROW_Y + i * ENTRY_HEIGHT;
            int editButtonX = entryX + ENTRY_WIDTH - buttonMargin - buttonSize;
            int deleteButtonX = editButtonX - buttonSize - buttonSpacing;
            int buttonY = entryY + (ENTRY_HEIGHT - buttonSize) / 2;
            final int index = filterIndex;

            Button deleteButton = Button.builder(Component.literal("C"), btn -> {
                        if (index < draftForbidden.size() && !draftForbidden.get(index).isEmpty()) {
                            draftForbidden.remove(index);
                            sendForbiddenDraft();
                            ensureForbiddenRows();
                            int maxScroll = Math.max(0, getForbiddenSlotCount() - getVisibleForbiddenEntries());
                            filterListScroll = Math.min(filterListScroll, maxScroll);
                            updateForbiddenEditButtons();
                            playButtonSound();
                        }
                    })
                    .bounds(deleteButtonX, buttonY, buttonSize, buttonSize)
                    .tooltip(Tooltip.create(Component.translatable("gui.iska_utils.deep_drawer_extractor.clear")))
                    .build();
            filterDeleteButtons.add(deleteButton);
            addRenderableWidget(deleteButton);

            Button editButton = Button.builder(Component.literal("✎"), btn -> {
                        if (nestedFilterEdit && nestedEditIndex == index) {
                            cancelNestedEdit();
                        } else {
                            enterNestedEdit(index);
                        }
                        playButtonSound();
                    })
                    .bounds(editButtonX, buttonY, buttonSize, buttonSize)
                    .tooltip(Tooltip.create(Component.translatable("gui.iska_utils.shop_edit.edit")))
                    .build();
            filterEditButtons.add(editButton);
            addRenderableWidget(editButton);
        }
    }

    private int getForbiddenSlotCount() {
        ensureForbiddenRows();
        // Always expose full Forbidden capacity (30) so scrollbar stays meaningful.
        return MAX_FORBIDDEN_SLOTS;
    }

    private void ensureForbiddenRows() {
        int minRows = getVisibleForbiddenEntries();
        while (draftForbidden.size() < minRows) {
            draftForbidden.add("");
        }
        if (draftForbidden.size() > MAX_FORBIDDEN_SLOTS) {
            draftForbidden.subList(MAX_FORBIDDEN_SLOTS, draftForbidden.size()).clear();
        }
        if (draftForbidden.size() < MAX_FORBIDDEN_SLOTS
                && (draftForbidden.isEmpty() || !draftForbidden.get(draftForbidden.size() - 1).isEmpty())) {
            draftForbidden.add("");
        }
    }

}
