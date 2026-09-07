package net.unfamily.iskautils.client.gui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.unfamily.iskautils.block.entity.ImprovedPatternCrafterBlockEntity;
import net.unfamily.iskautils.pattern.PatternData;

/**
 * Menu/Container for the Improved Pattern Crafter.
 *
 * Slot layout (item render positions) for 340×270 GUI — coords match baked slot interiors:
 *   - Input filter / variables:  9 x 2 at (90, 47) [paginated]
 *   - Upgrade slots:             stacked at (12, 207/225/243) [manual only; SINGLE_SLOT blit]
 *   - Output slots:              3x3 at (266, 119) [extract only, paginated; SINGLE_SLOT blit]
 *   - Machine input inventory:   9x3 at (90, 114)
 *   - Player inventory:          9x3 at (90, 185)
 *   - Player hotbar:             9x1 at (90, 243)
 */
public class ImprovedPatternCrafterMenu extends AbstractContainerMenu {

    // Slot counts (input filter count is dynamic from BE)
    public static final int OUTPUT_FILTER_SLOTS = 0;    // Forbidden moved to SubView list
    public static final int UPGRADE_SLOTS = 3;
    public static final int OUTPUT_SLOTS = 9;           // 3x3
    public static final int INPUT_SLOTS = 27;           // 9x3 machine input
    public static final int PLAYER_INV_SLOTS = 27;      // 9x3
    public static final int PLAYER_HOTBAR_SLOTS = 9;    // 9x1

    public static final int UPGRADE_SLOT_X = 12;
    public static final int UPGRADE_SLOT_Y0 = 207;
    public static final int UPGRADE_SLOT_Y1 = 225;
    public static final int UPGRADE_SLOT_Y2 = 243;
    /** First baked inventory slot interior (inside the dark border) on the 340×270 texture. */
    public static final int MACHINE_INPUT_X = 90;
    public static final int MACHINE_INPUT_Y = 114;
    public static final int PLAYER_INV_X = MACHINE_INPUT_X;
    public static final int PLAYER_INV_Y = 185;
    public static final int PLAYER_HOTBAR_Y = 243;
    /** Centered in the right gutter after the 9-wide inventory band. */
    public static final int OUTPUT_SLOT_X = 266;
    /** Aligned with the 3 player-inventory rows (not the hotbar). */
    public static final int OUTPUT_SLOT_Y = PLAYER_INV_Y;

    /** Filter edit chrome — exact DeepDrawer Extractor proportions. */
    public static final int EDIT_SLOT_SIZE = 18;
    public static final int EDIT_TEXTBOX_HEIGHT = 15;
    public static final int EDIT_ROW_GAP = 2;
    public static final int EDIT_BTN_SIZE = 12;
    public static final int EDIT_BTN_SPACING = 2;
    public static final int EDIT_ARROW_GAP = 4;
    public static final int EDIT_ACTION_GAP = 8;
    public static final int EDIT_TEXTBOX_Y = PLAYER_INV_Y - EDIT_ROW_GAP - EDIT_TEXTBOX_HEIGHT;
    public static final int EDIT_MODE_PANEL_Y = EDIT_TEXTBOX_Y - EDIT_ROW_GAP - EDIT_SLOT_SIZE;
    public static final int EDIT_MODE_GHOST_SLOT_X = PLAYER_INV_X + EDIT_SLOT_SIZE - 1;

    public static int editModeCloseButtonX() {
        int slotX = EDIT_MODE_GHOST_SLOT_X;
        int rightButtonX = slotX + EDIT_SLOT_SIZE + EDIT_ARROW_GAP;
        int clearButtonX = rightButtonX + EDIT_BTN_SIZE + EDIT_ACTION_GAP;
        int applyButtonX = clearButtonX + EDIT_BTN_SIZE + EDIT_BTN_SPACING;
        return applyButtonX + EDIT_BTN_SIZE + EDIT_BTN_SPACING;
    }

    public static int validKeysButtonX() {
        return editModeCloseButtonX() + EDIT_BTN_SIZE + EDIT_ROW_GAP;
    }

    public static int validKeysButtonY(int navButtonHeight) {
        return EDIT_MODE_PANEL_Y + (EDIT_SLOT_SIZE - navButtonHeight) / 2;
    }

    /** When false, machine/filter/upgrade/output slots are inactive (no hover/click) in SubViews. */
    private boolean machineSlotsActive = true;
    /** When false, only the 27 machine input inventory slots are inactive (variable editor). */
    private boolean machineInventoryActive = true;

    public void setMachineSlotsActive(boolean active) {
        this.machineSlotsActive = active;
    }

    public boolean areMachineSlotsActive() {
        return machineSlotsActive;
    }

    public void setMachineInventoryActive(boolean active) {
        this.machineInventoryActive = active;
    }

    public boolean isMachineInventoryActive() {
        return machineInventoryActive;
    }

    // Slot index ranges (INPUT_FILTER_END is dynamic)
    public static final int INPUT_FILTER_START = 0;
    /** Config capability (max possible key inputs), used for pagination layout. */
    private final int maxKeyInputs;
    /** Effective key inputs currently available (logic modules). */
    private final int inputFilterSlotCount;
    /** When maxKeyInputs > 18 we show 18 at a time (paginated); this is how many slots are in the menu. */
    private final int inputFilterMenuSlotCount;
    /** When paginated, view over BE handler so menu slots 0–17 show BE slots [offset..offset+17]. */
    private final InputFilterViewHandler inputFilterViewHandler;
    private final OutputPageViewHandler outputPageViewHandler;

    public int getMaxKeyInputs() {
        return maxKeyInputs;
    }

    /** Effective key-input count (same as {@link #getInputFilterSlotCount()}). */
    public int getEffectiveKeyInputCount() {
        return inputFilterSlotCount;
    }

    public int getInputFilterSlotCount() {
        return inputFilterSlotCount;
    }

    public int getInputFilterMenuSlotCount() {
        return inputFilterMenuSlotCount;
    }

    /** True when the machine is capable of more than 18 filter slots (pagination layout). */
    public boolean hasFilterPaginationCapability() {
        return maxKeyInputs > 18;
    }

    public InputFilterViewHandler getInputFilterViewHandler() {
        return inputFilterViewHandler;
    }

    /** Call from screen when page changes (paginated only). */
    public void setInputFilterViewOffset(int offset) {
        if (inputFilterViewHandler != null) inputFilterViewHandler.setOffset(offset);
    }

    public void setOutputViewOffset(int offset) {
        outputPageViewHandler.setOffset(offset);
    }

    public int getOutputSlotCount() {
        return blockEntity != null ? blockEntity.getOutputSlotCount() : OUTPUT_SLOTS;
    }

    public int getInputFilterEnd() {
        return INPUT_FILTER_START + inputFilterMenuSlotCount;
    }

    public int getOutputFilterStart() { return getInputFilterEnd(); }
    public int getOutputFilterEnd() { return getOutputFilterStart() + OUTPUT_FILTER_SLOTS; }
    public int getUpgradeStart() { return getOutputFilterEnd(); }
    public int getUpgradeEnd() { return getUpgradeStart() + UPGRADE_SLOTS; }
    public int getOutputStart() { return getUpgradeEnd(); }
    public int getOutputEnd() { return getOutputStart() + OUTPUT_SLOTS; }
    public int getInputStart() { return getOutputEnd(); }
    public int getInputEnd() { return getInputStart() + INPUT_SLOTS; }
    public int getPlayerInvStart() { return getInputEnd(); }
    public int getPlayerInvEnd() { return getPlayerInvStart() + PLAYER_INV_SLOTS; }
    public int getPlayerHotbarStart() { return getPlayerInvEnd(); }
    public int getPlayerHotbarEnd() { return getPlayerHotbarStart() + PLAYER_HOTBAR_SLOTS; }

    // ContainerData indices for synced data (energy etc. depend on inputFilterSlotCount)
    private static final int DATA_CURRENT_PATTERN = 0;
    private static final int DATA_TOTAL_PATTERNS = 1;
    private static final int DATA_GRID_START = 2;
    private static final int DATA_FILTER_LETTERS_START = 2 + PatternData.GRID_SIZE; // 11

    private int getDataEnergyStoredIndex() {
        return DATA_FILTER_LETTERS_START + inputFilterSlotCount;
    }

    private int getContainerDataSize() {
        return getDataAutoclearVariablesIndex() + 1;
    }

    private int getDataMaxEnergyIndex() { return getDataEnergyStoredIndex() + 1; }
    private int getDataCraftingModeIndex() { return getDataEnergyStoredIndex() + 2; }
    private int getDataRedstoneModeIndex() { return getDataEnergyStoredIndex() + 3; }
    private int getDataCraftingTimerIndex() { return getDataEnergyStoredIndex() + 4; }
    private int getDataCraftingIntervalIndex() { return getDataEnergyStoredIndex() + 5; }
    private int getDataFilterPageIndex() { return getDataCraftingIntervalIndex() + 1; }
    private int getDataRecursiveOutputModeIndex() { return getDataFilterPageIndex() + 1; }
    private int getDataRemainderRoutingModeIndex() { return getDataFilterPageIndex() + 2; }
    private int getDataOutputPageIndex() { return getDataFilterPageIndex() + 3; }
    private int getDataToolSafeguardIndex() { return getDataFilterPageIndex() + 4; }
    private int getDataAutoclearVariablesIndex() { return getDataToolSafeguardIndex() + 1; }

    private final ImprovedPatternCrafterBlockEntity blockEntity;
    private final ContainerData patternContainerData;

    /**
     * Client-side constructor - reads BlockPos from network buffer.
     * Uses SimpleContainerData so synced values from server are stored correctly.
     */
    public ImprovedPatternCrafterMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(extraData.readBlockPos()),
                true);
    }

    /**
     * Server-side constructor.
     * Uses BlockEntity-backed ContainerData for live pattern data reads.
     */
    public ImprovedPatternCrafterMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity) {
        this(containerId, playerInventory, blockEntity, false);
    }

    /**
     * Unified constructor.
     * @param isClientSide true when called from the client (FriendlyByteBuf) constructor
     */
    private ImprovedPatternCrafterMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity, boolean isClientSide) {
        super(ModMenuTypes.IMPROVED_PATTERN_CRAFTER_MENU.get(), containerId);

        if (blockEntity instanceof ImprovedPatternCrafterBlockEntity pcbe) {
            this.blockEntity = pcbe;
            this.maxKeyInputs = pcbe.getMaxKeyInputs();
            this.inputFilterSlotCount = pcbe.getEffectiveKeyInputCount();
            // Paginate by capability (maxKeyInputs), not only by current effective count.
            if (this.maxKeyInputs > 18) {
                this.inputFilterMenuSlotCount = 18;
                this.inputFilterViewHandler = new InputFilterViewHandler(pcbe.getInputFilterHandler());
                this.inputFilterViewHandler.setOffset(pcbe.getGuiFilterPage() * 18);
                addInputFilterSlots(this.inputFilterViewHandler, this.inputFilterMenuSlotCount);
            } else {
                this.inputFilterMenuSlotCount = this.inputFilterSlotCount;
                this.inputFilterViewHandler = null;
                addInputFilterSlots(pcbe.getInputFilterHandler(), this.inputFilterMenuSlotCount);
            }
            addUpgradeSlots(pcbe.getUpgradeHandler());
            this.outputPageViewHandler = new OutputPageViewHandler(pcbe.getOutputHandler());
            this.outputPageViewHandler.setOffset(pcbe.getGuiOutputPage() * OUTPUT_SLOTS);
            addOutputSlots(this.outputPageViewHandler);
            addInputSlots(pcbe.getInputHandler());

            if (isClientSide) {
                this.patternContainerData = new SimpleContainerData(getContainerDataSize());
            } else {
                final int dataEnergy = getDataEnergyStoredIndex();
                final int dataMaxEnergy = getDataMaxEnergyIndex();
                final int dataMode = getDataCraftingModeIndex();
                final int dataRedstone = getDataRedstoneModeIndex();
                final int dataTimer = getDataCraftingTimerIndex();
                final int dataInterval = getDataCraftingIntervalIndex();
                final int dataFilterPage = getDataFilterPageIndex();
                final int dataRecursiveOutput = getDataRecursiveOutputModeIndex();
                final int dataRemainderRouting = getDataRemainderRoutingModeIndex();
                final int dataOutputPage = getDataOutputPageIndex();
                final int dataToolSafeguard = getDataToolSafeguardIndex();
                final int dataAutoclearVariables = getDataAutoclearVariablesIndex();
                this.patternContainerData = new ContainerData() {
                    @Override
                    public int get(int index) {
                        if (index == DATA_CURRENT_PATTERN) return pcbe.getCurrentPatternIndex();
                        if (index == DATA_TOTAL_PATTERNS) return pcbe.getPatternCount();
                        if (index == dataEnergy) return pcbe.getEnergyStorage().getEnergyStored();
                        if (index == dataMaxEnergy) return pcbe.getEnergyStorage().getMaxEnergyStored();
                        if (index == dataMode) return pcbe.getCraftingMode();
                        if (index == dataRedstone) return pcbe.getRedstoneMode();
                        if (index == dataTimer) return pcbe.getCraftingTimer();
                        if (index == dataInterval) return pcbe.getEffectiveCraftingInterval();
                        if (index == dataFilterPage) return pcbe.getGuiFilterPage();
                        if (index == dataRecursiveOutput) return pcbe.getRecursiveOutputMode();
                        if (index == dataRemainderRouting) return pcbe.getRemainderRoutingMode();
                        if (index == dataOutputPage) return pcbe.getGuiOutputPage();
                        if (index == dataToolSafeguard) return pcbe.isToolSafeguardEnabled() ? 1 : 0;
                        if (index == dataAutoclearVariables) return pcbe.isAutoclearVariables() ? 1 : 0;
                        if (index >= DATA_FILTER_LETTERS_START && index < dataEnergy) {
                            return pcbe.getFilterLetter(index - DATA_FILTER_LETTERS_START);
                        }
                        PatternData pattern = pcbe.getCurrentPattern();
                        return pattern != null ? pattern.getCell(index - DATA_GRID_START) : 0;
                    }

                    @Override
                    public void set(int index, int value) {}

                    @Override
                    public int getCount() {
                        return getContainerDataSize();
                    }
                };
            }
        } else {
            this.blockEntity = null;
            this.maxKeyInputs = 18;
            this.inputFilterSlotCount = 18;
            this.inputFilterMenuSlotCount = 18;
            this.inputFilterViewHandler = null;
            addInputFilterSlots(new ItemStackHandler(18), 18);
            addUpgradeSlots(new ItemStackHandler(UPGRADE_SLOTS));
            this.outputPageViewHandler = new OutputPageViewHandler(new ItemStackHandler(OUTPUT_SLOTS));
            addOutputSlots(this.outputPageViewHandler);
            addInputSlots(new ItemStackHandler(INPUT_SLOTS));
            this.patternContainerData = new SimpleContainerData(getContainerDataSize());
        }

        addDataSlots(patternContainerData);
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    // ===== Pattern Data Accessors (read from synced ContainerData) =====

    public int getCurrentPatternIndex() {
        return patternContainerData.get(DATA_CURRENT_PATTERN);
    }

    public int getTotalPatterns() {
        return patternContainerData.get(DATA_TOTAL_PATTERNS);
    }

    public int getGridCell(int index) {
        if (index < 0 || index >= PatternData.GRID_SIZE) return 0;
        return patternContainerData.get(DATA_GRID_START + index);
    }

    /**
     * Returns the letter assigned to an input filter slot (0=disabled, 1..N = letters).
     */
    public int getFilterLetter(int index) {
        if (index < 0 || index >= inputFilterSlotCount) return 0;
        return patternContainerData.get(DATA_FILTER_LETTERS_START + index);
    }

    // ===== Energy Data Accessors =====

    public int getEnergyStored() {
        return patternContainerData.get(getDataEnergyStoredIndex());
    }

    public int getMaxEnergyStored() {
        return patternContainerData.get(getDataMaxEnergyIndex());
    }

    /** 0 = Shaped+Shapeless, 1 = Only Shaped, 2 = Only Shapeless */
    public int getCraftingMode() {
        return patternContainerData.get(getDataCraftingModeIndex());
    }

    public int getRedstoneMode() {
        return patternContainerData.get(getDataRedstoneModeIndex());
    }

    public int getCraftingTimer() {
        return patternContainerData.get(getDataCraftingTimerIndex());
    }

    public int getCraftingInterval() {
        return patternContainerData.get(getDataCraftingIntervalIndex());
    }

    /** Current filter page (0-based) synced from server; only meaningful when inputFilterSlotCount > 18. */
    public int getSyncedFilterPage() {
        return patternContainerData.get(getDataFilterPageIndex());
    }

    /** 1–3: recursive output routing mode (synced). */
    public int getSyncedRecursiveOutputMode() {
        return patternContainerData.get(getDataRecursiveOutputModeIndex());
    }

    /** 1–2: remainder routing mode (synced). */
    public int getSyncedRemainderRoutingMode() {
        return patternContainerData.get(getDataRemainderRoutingModeIndex());
    }

    public int getSyncedOutputPage() {
        return patternContainerData.get(getDataOutputPageIndex());
    }

    public boolean isToolSafeguardEnabled() {
        return patternContainerData.get(getDataToolSafeguardIndex()) != 0;
    }

    public boolean isAutoclearVariablesEnabled() {
        return patternContainerData.get(getDataAutoclearVariablesIndex()) != 0;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide() && blockEntity != null && blockEntity.isAutoclearVariables()) {
            blockEntity.clearUnusedVariables();
        }
    }

    // ===== Ghost Slot Click Handling =====

    /**
     * Ghost slot click handling. Input filter slots are visual-only buttons on the Screen;
     * no quick-edit with carried items on the main view.
     */
    @Override
    public void clicked(int slotId, int button, ContainerInput containerInput, Player player) {
        if (slotId >= INPUT_FILTER_START && slotId < getInputFilterEnd()) {
            return;
        }
        super.clicked(slotId, button, containerInput, player);
    }

    // ===== Slot Setup Methods =====

    // Input filter: 9 columns x N rows at (80, 47), N = handler.getSlots()
    private void addInputFilterSlots(net.neoforged.neoforge.items.IItemHandler handler, int slots) {
        for (int i = 0; i < slots; i++) {
            int row = i / 9;
            int col = i % 9;
            this.addSlot(new GhostSlot(handler, i, 80 + col * 18, 47 + row * 18) {
                @Override
                public boolean isActive() {
                    // Replaced by letter labels + 18x18 filter buttons on the screen — never hover/click.
                    return false;
                }
            });
        }
    }

    // Output filter: 3 columns x 7 rows at (13, 65)
    private void addOutputFilterSlots(ItemStackHandler handler) {
        for (int row = 0; row < 7; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new GhostSlot(handler, row * 3 + col,
                        13 + col * 18, 65 + row * 18) {
                    @Override
                    public boolean isActive() {
                        return ImprovedPatternCrafterMenu.this.machineSlotsActive;
                    }
                });
            }
        }
    }

    // Upgrade slots: stacked left column (logic / speed / production).
    private void addUpgradeSlots(ItemStackHandler handler) {
        this.addSlot(new UpgradeSlot(handler, 0, UPGRADE_SLOT_X, UPGRADE_SLOT_Y0) {
            @Override
            public boolean isActive() {
                return ImprovedPatternCrafterMenu.this.machineSlotsActive;
            }
        });
        this.addSlot(new UpgradeSlot(handler, 1, UPGRADE_SLOT_X, UPGRADE_SLOT_Y1) {
            @Override
            public boolean isActive() {
                return ImprovedPatternCrafterMenu.this.machineSlotsActive;
            }
        });
        this.addSlot(new UpgradeSlot(handler, 2, UPGRADE_SLOT_X, UPGRADE_SLOT_Y2) {
            @Override
            public boolean isActive() {
                return ImprovedPatternCrafterMenu.this.machineSlotsActive;
            }
        });
    }

    // Output: 3 columns x 3 rows at (259, 157) - extract only, no insertion
    private void addOutputSlots(net.neoforged.neoforge.items.IItemHandler handler) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new OutputSlot(handler, row * 3 + col,
                        OUTPUT_SLOT_X + col * 18, OUTPUT_SLOT_Y + row * 18) {
                    @Override
                    public boolean isActive() {
                        return ImprovedPatternCrafterMenu.this.machineSlotsActive;
                    }
                });
            }
        }
    }

    // Machine input inventory: 9 columns x 3 rows at (80, 105)
    private void addInputSlots(ItemStackHandler handler) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new SlotItemHandler(handler, row * 9 + col,
                        MACHINE_INPUT_X + col * 18, MACHINE_INPUT_Y + row * 18) {
                    @Override
                    public boolean isActive() {
                        return ImprovedPatternCrafterMenu.this.machineSlotsActive
                                && ImprovedPatternCrafterMenu.this.machineInventoryActive;
                    }
                });
            }
        }
    }

    // Player inventory: 9 columns x 3 rows at (80, 171)
    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        80 + col * 18, 171 + row * 18));
            }
        }
    }

    // Player hotbar: 9 columns x 1 row at (80, 229)
    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col,
                    80 + col * 18, 229));
        }
    }

    // ===== Quick Move (Shift-Click) =====

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot == null || !slot.hasItem()) {
            return result;
        }

        // Ghost slots: no shift-click behavior
        if (index < getOutputFilterEnd()) {
            return result;
        }

        ItemStack stackInSlot = slot.getItem();
        result = stackInSlot.copy();

        if (index < getPlayerInvStart()) {
            // From machine slots (upgrade/output/input) -> to player inventory/hotbar
            if (!moveItemStackTo(stackInSlot, getPlayerInvStart(), getPlayerHotbarEnd(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (index < getPlayerHotbarStart()) {
            // From player inventory -> try machine input first, then upgrade, then hotbar
            if (!moveItemStackTo(stackInSlot, getInputStart(), getInputEnd(), false)) {
                if (!moveItemStackTo(stackInSlot, getUpgradeStart(), getUpgradeEnd(), false)) {
                    if (!moveItemStackTo(stackInSlot, getPlayerHotbarStart(), getPlayerHotbarEnd(), false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        } else {
            // From hotbar -> try machine input first, then upgrade, then player inventory
            if (!moveItemStackTo(stackInSlot, getInputStart(), getInputEnd(), false)) {
                if (!moveItemStackTo(stackInSlot, getUpgradeStart(), getUpgradeEnd(), false)) {
                    if (!moveItemStackTo(stackInSlot, getPlayerInvStart(), getPlayerInvEnd(), false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        if (stackInSlot.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null || blockEntity.getLevel() == null) {
            return false;
        }
        return player.distanceToSqr(
                blockEntity.getBlockPos().getX() + 0.5,
                blockEntity.getBlockPos().getY() + 0.5,
                blockEntity.getBlockPos().getZ() + 0.5) <= 64.0;
    }

    public ImprovedPatternCrafterBlockEntity getBlockEntity() {
        return blockEntity;
    }

    /** Slot-dedication (mark input) filter for the given input slot index (0..26). For GUI ghost display. */
    public ItemStack getMarkInputFilter(int slot) {
        return blockEntity != null ? blockEntity.getMarkInputFilter(slot) : ItemStack.EMPTY;
    }

    /** True if the given input slot has a mark-input filter. */
    public boolean hasMarkInputFilter(int slot) {
        return blockEntity != null && blockEntity.hasMarkInputFilter(slot);
    }

    public ItemStack getMarkOutputFilter(int slot) {
        return blockEntity != null ? blockEntity.getMarkOutputFilter(slot) : ItemStack.EMPTY;
    }

    public boolean hasMarkOutputFilter(int slot) {
        return blockEntity != null && blockEntity.hasMarkOutputFilter(slot);
    }
}
