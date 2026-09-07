package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.pattern.PatternData;
import net.unfamily.iskautils.util.DeepDrawerItemFilter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * BlockEntity for the Improved Pattern Crafter.
 * Stores ghost filter items, machine input/output inventories, upgrade items,
 * and crafting patterns.
 */
public class ImprovedPatternCrafterBlockEntity extends BlockEntity {

    // Input filter ghost slots: count = getMaxKeyInputs() (config: improved 90, normal 18)
    private ItemStackHandler inputFilterHandler;
    /** Extractor-style filters, one per variable slot. Empty means wildcard. */
    private String[] inputFilterStrings;

    // Legacy-only handlers retained to decode old saves; new saves use string lists.
    private final ItemStackHandler outputFilterHandler = new ItemStackHandler(21) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final List<String> forbiddenFilters = new ArrayList<>();

    // Three real upgrade slots: logic, speed, and production.
    private final ItemStackHandler upgradeHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            clearCraftIdle();
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            return switch (slot) {
                case 0 -> getMaxLogicModules();
                case 1 -> getMaxSpeedModules();
                case 2 -> getMaxProductionModules();
                default -> super.getSlotLimit(slot);
            };
        }
    };

    // Output storage is larger than the nine-slot paginated GUI view.
    private final ItemStackHandler outputHandler;

    /**
     * Slot dedication filters for the 27 input slots (like ghostFilters in Structure Placer).
     * "Mark Input" saves here only; used for isItemValid and GUI ghost display. Not used for crafting.
     */
    private final List<ItemStack> markInputFilters = new ArrayList<>();
    private final List<ItemStack> markOutputFilters = new ArrayList<>();

    // 9 columns x 3 rows = 27 input slots (machine internal inventory, hopper can insert)
    private final ItemStackHandler inputHandler = new ItemStackHandler(27) {
        @Override
        protected void onContentsChanged(int slot) {
            clearCraftIdle();
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot >= 0 && slot < markInputFilters.size()) {
                ItemStack filter = markInputFilters.get(slot);
                if (!filter.isEmpty()) {
                    return sameItemType(stack, filter);
                }
            }
            return true;
        }
    };

    // Pattern system
    private final List<PatternData> patterns = new ArrayList<>();
    private int currentPatternIndex = 0;

    // Filter letter assignments: 0=empty(disabled), 1..N = letters (N = getMaxKeyInputs())
    private int[] filterLetters;

    // Crafting system
    private int craftingTimer = 0;
    private int craftingPatternIndex = 0; // Round-robin index, separate from GUI currentPatternIndex
    private boolean craftIdleUntilChange = false;

    private enum CraftBlockReason {
        SUCCESS,
        NO_MATCH,
        STORAGE_FULL
    }

    // Note: crafting always respects crafting interval; no same-tick recursion.

    // Energy storage (RF/FE)
    private final EnergyStorageImpl energyStorage = new EnergyStorageImpl(getEnergyCapacity());
    private final net.neoforged.neoforge.transfer.energy.EnergyHandler energyHandler = new EnergyHandlerImpl();

    // Redstone mode: 0 = ignore, 1 = low, 2 = high, 3 = pulse (once per rising edge), 4 = disabled
    private int redstoneMode = 0;
    private boolean previousRedstoneState = false;
    private int pulseIgnoreTimer = 0;
    private static final int PULSE_IGNORE_INTERVAL = 10;
    /** In PULSE mode: one craft is scheduled and will run after the normal crafting interval (not instant). */
    private boolean pulseCraftPending = false;

    /**
     * When true (default), unused variable slots are cleared when a player closes the GUI:
     * letters not referenced by any effective pattern lose their letter and filter string.
     */
    private boolean autoclearVariables = true;

    /** Current input filter page when GUI is paginated (0-based). Synced to client via ContainerData; not persisted. */
    private int guiFilterPage = 0;
    private int guiOutputPage = 0;
    private ItemStack lastCraftResult = ItemStack.EMPTY;

    public int getGuiFilterPage() {
        return guiFilterPage;
    }

    public void setGuiFilterPage(int page) {
        this.guiFilterPage = Math.max(0, page);
    }

    public int getGuiOutputPage() {
        return guiOutputPage;
    }

    public void setGuiOutputPage(int page) {
        int maxPage = Math.max(0, (getOutputSlotCount() - 1) / 9);
        this.guiOutputPage = Math.max(0, Math.min(maxPage, page));
    }

    /** Result mode for current pattern: 1 = Eject, 2 = Keep, 3 = Smart. */
    public int getRecursiveOutputMode() {
        PatternData p = getCurrentPattern();
        return p != null ? p.getResultMode() : 1;
    }

    public void cycleRecursiveOutputMode() {
        PatternData p = getCurrentPattern();
        if (p == null) return;
        p.cycleResultMode();
        setChanged();
    }

    /** Ingredient mode for current pattern: 1 = Keep, 2 = Eject. */
    public int getRemainderRoutingMode() {
        PatternData p = getCurrentPattern();
        return p != null ? p.getIngredientMode() : 1;
    }

    public void cycleRemainderRoutingMode() {
        PatternData p = getCurrentPattern();
        if (p == null) return;
        p.cycleIngredientMode();
        setChanged();
    }

    /**
     * Combined handler for automation (hoppers, tubes, etc.).
     * Slots 0-26:  input  (insert only via automation)
     * Slots 27-35: output (extract only via automation)
     * Upgrades and ghost filters are NOT exposed.
     */
    private final IItemHandler automationHandler = new IItemHandler() {
        private static final int INPUT_SIZE = 27;
        @Override
        public int getSlots() {
            return INPUT_SIZE + getOutputSlotCount();
        }

        @Override
        @NotNull
        public ItemStack getStackInSlot(int slot) {
            if (slot < INPUT_SIZE) return inputHandler.getStackInSlot(slot);
            return outputHandler.getStackInSlot(slot - INPUT_SIZE);
        }

        @Override
        @NotNull
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot < INPUT_SIZE) return inputHandler.insertItem(slot, stack, simulate);
            return stack; // Output: no insertion
        }

        @Override
        @NotNull
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < INPUT_SIZE) return ItemStack.EMPTY; // Input: no extraction via automation
            return outputHandler.extractItem(slot - INPUT_SIZE, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot < INPUT_SIZE) return inputHandler.getSlotLimit(slot);
            return outputHandler.getSlotLimit(slot - INPUT_SIZE);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot < INPUT_SIZE) return inputHandler.isItemValid(slot, stack);
            return false; // Output: no insertion
        }
    };
    private final net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource>
            itemTransferHandler =
            net.unfamily.iskalib.transfer.LegacyItemHandlerResourceHandler.wrap(automationHandler);

    public ImprovedPatternCrafterBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.IMPROVED_PATTERN_CRAFTER_BE.get(), pos, state);
    }

    /** For subclasses (e.g. PatternCrafterBlockEntity) that use a different BlockEntityType. */
    protected ImprovedPatternCrafterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        int maxKeys = getMaxKeyInputs();
        this.inputFilterHandler = new ItemStackHandler(maxKeys) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
        this.filterLetters = new int[maxKeys];
        this.inputFilterStrings = new String[maxKeys];
        java.util.Arrays.fill(this.inputFilterStrings, "");
        this.outputHandler = new ItemStackHandler(getOutputSlotCount()) {
            @Override
            protected void onContentsChanged(int slot) {
                clearCraftIdle();
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (slot >= 0 && slot < markOutputFilters.size()) {
                    ItemStack filter = markOutputFilters.get(slot);
                    if (!filter.isEmpty()) return sameItemType(stack, filter);
                }
                return true;
            }
        };
        for (int i = 0; i < 27; i++) {
            markInputFilters.add(ItemStack.EMPTY);
        }
        for (int i = 0; i < getOutputSlotCount(); i++) {
            markOutputFilters.add(ItemStack.EMPTY);
        }
        initPatterns();
    }

    /** Total machine output slots. The GUI exposes them in pages of nine. */
    public int getOutputSlotCount() {
        return 45;
    }

    /** Max key inputs (filter slots / letters) for this machine. Improved: config 90, Normal: config 18. */
    public int getMaxKeyInputs() {
        try {
            return Math.max(1, Math.min(256, Config.IMPROVED_MAX_KEY_INPUTS.get()));
        } catch (Exception e) {
            return 90;
        }
    }

    private void initPatterns() {
        int maxPatterns = getMaxPatterns();
        patterns.clear();
        for (int i = 0; i < maxPatterns; i++) {
            patterns.add(new PatternData());
        }
    }

    @Override
    public void setChanged() {
        clearCraftIdle();
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    private void clearCraftIdle() {
        craftIdleUntilChange = false;
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    // Packet data is loaded through the current ValueInput serialization callback.

    // ===== Item Handler Getters =====

    public ItemStackHandler getInputFilterHandler() {
        return inputFilterHandler;
    }

    public void setFilterItem(int slot, boolean outputFilter, ItemStack stack) {
        if (outputFilter) {
            setForbiddenFilter(slot, stack.isEmpty() ? "" : "-" + BuiltInRegistries.ITEM.getKey(stack.getItem()));
        } else {
            setInputFilterString(slot, stack.isEmpty() ? "" : "-" + BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }
    }

    public ItemStackHandler getOutputFilterHandler() {
        return outputFilterHandler;
    }

    public String getInputFilterString(int slot) {
        return slot >= 0 && slot < inputFilterStrings.length ? inputFilterStrings[slot] : "";
    }

    public void setInputFilterString(int slot, String value) {
        if (slot < 0 || slot >= getEffectiveKeyInputCount()) return;
        inputFilterStrings[slot] = value == null ? "" : value.trim();
        setChanged();
    }

    public List<String> getForbiddenFilters() {
        return List.copyOf(forbiddenFilters);
    }

    public void setForbiddenFilters(List<String> values) {
        forbiddenFilters.clear();
        if (values != null) {
            values.stream().limit(64).map(value -> value == null ? "" : value.trim())
                    .forEach(forbiddenFilters::add);
        }
        setChanged();
    }

    public void setForbiddenFilter(int index, String value) {
        if (index < 0 || index >= 64) return;
        while (forbiddenFilters.size() <= index) forbiddenFilters.add("");
        forbiddenFilters.set(index, value == null ? "" : value.trim());
        setChanged();
    }

    public boolean isToolSafeguardEnabled() {
        PatternData pattern = getCurrentPattern();
        return pattern == null || pattern.isToolSafeguard();
    }

    public void toggleToolSafeguard() {
        PatternData pattern = getCurrentPattern();
        if (pattern != null) {
            pattern.toggleToolSafeguard();
            setChanged();
        }
    }

    public ItemStackHandler getUpgradeHandler() {
        return upgradeHandler;
    }

    public ItemStackHandler getOutputHandler() {
        return outputHandler;
    }

    public ItemStackHandler getInputHandler() {
        return inputHandler;
    }

    public IItemHandler getAutomationHandler() {
        return automationHandler;
    }

    public net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource>
    getItemTransferHandler() {
        return itemTransferHandler;
    }

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public net.neoforged.neoforge.transfer.energy.EnergyHandler getEnergyHandler() {
        return energyHandler;
    }

    /** Overridable for Normal (0 when no energy). */
    protected int getEnergyCapacity() {
        try {
            return Config.ENERGY_CAPACITY.get();
        } catch (Exception ignored) {
            return 1000;
        }
    }

    /** Overridable for Normal (0 when no energy). */
    protected int getEnergyPerCraft() {
        try {
            return Config.ENERGY_PER_CRAFT.get();
        } catch (Exception ignored) {
            return 5;
        }
    }

    /** Overridable for Normal. */
    protected int getMaxLogicModules() {
        try {
            return Config.MAX_LOGIC_MODULES.get();
        } catch (Exception ignored) {
            return 4;
        }
    }

    /** Overridable for Normal. */
    protected int getMaxSpeedModules() {
        try {
            return Config.MAX_SPEED_MODULES.get();
        } catch (Exception ignored) {
            return 1;
        }
    }

    protected int getBaseKeyInputs() {
        try {
            return Config.BASE_KEY_INPUTS.get();
        } catch (Exception ignored) {
            return 18;
        }
    }

    protected int getMaxProductionModules() {
        try {
            return Config.MAX_PRODUCTION_MODULES.get();
        } catch (Exception ignored) {
            return 1;
        }
    }

    protected int getCraftsPerProductionModule() {
        try {
            return Config.CRAFTS_PER_PRODUCTION_MODULE.get();
        } catch (Exception ignored) {
            return 1;
        }
    }

    /** Whether a batch should target full result stacks. */
    protected boolean processAsStack() {
        try {
            return Config.PROCESS_AS_STACK.get();
        } catch (Exception ignored) {
            return true;
        }
    }

    /** True if any upgrade slot is allowed (used to show/dim upgrade area in GUI). */
    public boolean hasUpgrades() {
        return getMaxLogicModules() > 0 || getMaxSpeedModules() > 0 || getMaxProductionModules() > 0;
    }

    // ===== Upgrade effects (logic = extra patterns, speed = interval multiplier) =====

    private static final String ISKA_UTILS = "iska_utils";
    private static final Identifier LOGIC_MODULE_ID = Identifier.fromNamespaceAndPath(ISKA_UTILS, "logic_module");
    private static final Identifier PRODUCTION_MODULE_ID = Identifier.fromNamespaceAndPath(ISKA_UTILS, "production_module");
    private static final Identifier[] SPEED_MODULE_IDS = new Identifier[]{
            Identifier.fromNamespaceAndPath(ISKA_UTILS, "slow_module"),
            Identifier.fromNamespaceAndPath(ISKA_UTILS, "moderate_module"),
            Identifier.fromNamespaceAndPath(ISKA_UTILS, "fast_module"),
            Identifier.fromNamespaceAndPath(ISKA_UTILS, "extreme_module"),
            Identifier.fromNamespaceAndPath(ISKA_UTILS, "ultra_module")
    };

    /** Number of logic modules in the top upgrade slot (each adds +1 pattern and +1 variable page). */
    private int getLogicModuleCount() {
        ItemStack stack = upgradeHandler.getStackInSlot(0);
        if (stack.isEmpty()) return 0;
        Item logic = BuiltInRegistries.ITEM.getValue(LOGIC_MODULE_ID);
        if (logic == null || logic == net.minecraft.world.item.Items.AIR) return 0;
        if (!stack.is(logic)) return 0;
        return stack.getCount();
    }

    private int getProductionModuleCount() {
        ItemStack stack = upgradeHandler.getStackInSlot(2);
        if (stack.isEmpty()) return 0;
        Item production = BuiltInRegistries.ITEM.getValue(PRODUCTION_MODULE_ID);
        if (production == null || production == net.minecraft.world.item.Items.AIR || !stack.is(production)) return 0;
        return stack.getCount();
    }

    public int getEffectiveKeyInputCount() {
        int effective = getBaseKeyInputs() + 18 * getLogicModuleCount();
        return Math.max(1, Math.min(getMaxKeyInputs(), effective));
    }

    public int getProductionBatch(ItemStack result) {
        int modules = Math.min(getProductionModuleCount(), getMaxProductionModules());
        // Stack-mode: 1 module = 1 full output stack; N modules = N stacks.
        if (processAsStack() && modules > 0 && !result.isEmpty()) {
            int stackCrafts = Math.max(1, result.getMaxStackSize() / Math.max(1, result.getCount()));
            return Math.min(4096, stackCrafts * modules);
        }
        return 1 + modules * getCraftsPerProductionModule();
    }

    /** Speed multiplier from the speed upgrade slot (one module only). 1.0 = no change. */
    private double getSpeedMultiplier() {
        ItemStack stack = upgradeHandler.getStackInSlot(1);
        if (stack.isEmpty()) return 1.0;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        try {
            if (id.equals(SPEED_MODULE_IDS[0])) return Config.SPEED_MULTIPLIER_SLOW.get();
            if (id.equals(SPEED_MODULE_IDS[1])) return Config.SPEED_MULTIPLIER_MODERATE.get();
            if (id.equals(SPEED_MODULE_IDS[2])) return Config.SPEED_MULTIPLIER_FAST.get();
            if (id.equals(SPEED_MODULE_IDS[3])) return Config.SPEED_MULTIPLIER_EXTREME.get();
            if (id.equals(SPEED_MODULE_IDS[4])) return Config.SPEED_MULTIPLIER_ULTRA.get();
        } catch (Exception ignored) {}
        return 1.0;
    }

    /** Overridable for Normal. */
    protected int getBasePatterns() {
        try {
            return Config.BASE_PATTERNS.get();
        } catch (Exception ignored) {
            return 6;
        }
    }

    /** Overridable for Normal. */
    protected int getMaxPatterns() {
        try {
            return Config.MAX_PATTERNS.get();
        } catch (Exception ignored) {
            return 16;
        }
    }

    /** Effective number of patterns (base + logic modules), capped by max. */
    public int getEffectivePatternCount() {
        int base = getBasePatterns();
        int max = getMaxPatterns();
        int count = base + getLogicModuleCount();
        return Math.min(max, Math.max(1, count));
    }

    /** Overridable for Normal. */
    protected int getCraftingInterval() {
        try {
            return Config.CRAFTING_INTERVAL.get();
        } catch (Exception ignored) {
            return 20;
        }
    }

    /** Effective crafting interval in ticks (base interval * speed multiplier). */
    public int getEffectiveCraftingInterval() {
        int base = getCraftingInterval();
        double mult = getSpeedMultiplier();
        int interval = (int) Math.round(base * mult);
        return Math.max(1, interval);
    }

    public int getCraftingTimer() {
        return craftingTimer;
    }

    // ===== Crafting Mode (per pattern) =====

    /** Returns the crafting mode for the currently selected pattern (GUI). 0 = both, 1 = only shaped, 2 = only shapeless */
    public int getCraftingMode() {
        if (currentPatternIndex < 0 || currentPatternIndex >= patterns.size()) return 0;
        return patterns.get(currentPatternIndex).getCraftingMode();
    }

    /** Cycles the crafting mode for the currently selected pattern */
    public void cycleCraftingMode() {
        if (currentPatternIndex < 0 || currentPatternIndex >= patterns.size()) return;
        patterns.get(currentPatternIndex).cycleCraftingMode();
        setChanged();
    }

    public int getRedstoneMode() {
        return redstoneMode;
    }

    public void cycleRedstoneMode() {
        redstoneMode = (redstoneMode + 1) % 5; // 0 -> 1 -> 2 -> 3 (pulse) -> 4 (disabled) -> 0
        setChanged();
        if (redstoneMode != 3) {
            pulseIgnoreTimer = 0;
        }
    }

    // ===== Filter Letter System =====

    public int getFilterLetter(int index) {
        if (index < 0 || index >= filterLetters.length) return 0;
        return filterLetters[index];
    }

    public void setFilterLetter(int index, int value) {
        if (index < 0 || index >= filterLetters.length) return;
        // A-Z only (1-26); ignore slot-count limit
        if (value < 0 || value > PatternData.MAX_LETTER) value = 0;
        filterLetters[index] = value;
        setChanged();
    }

    public boolean isAutoclearVariables() {
        return autoclearVariables;
    }

    public void setAutoclearVariables(boolean enabled) {
        if (this.autoclearVariables == enabled) return;
        this.autoclearVariables = enabled;
        setChanged();
    }

    public void toggleAutoclearVariables() {
        setAutoclearVariables(!autoclearVariables);
    }

    /**
     * Clears variable slots whose letter is not used in any effective pattern grid.
     * Locked slots ({@link PatternData#EMPTY}) are left untouched.
     */
    public void clearUnusedVariables() {
        boolean[] used = new boolean[PatternData.MAX_LETTER + 1];
        int patternCount = getEffectivePatternCount();
        for (int p = 0; p < patternCount; p++) {
            PatternData pattern = patterns.get(p);
            for (int c = 0; c < PatternData.GRID_SIZE; c++) {
                int letter = pattern.getCell(c);
                if (letter > PatternData.EMPTY && letter <= PatternData.MAX_LETTER) {
                    used[letter] = true;
                }
            }
        }
        boolean changed = false;
        int keyCount = getEffectiveKeyInputCount();
        for (int i = 0; i < keyCount; i++) {
            int letter = filterLetters[i];
            if (letter > PatternData.EMPTY && !used[letter]) {
                filterLetters[i] = PatternData.EMPTY;
                inputFilterStrings[i] = "";
                changed = true;
            }
        }
        if (changed) {
            setChanged();
        }
    }

    /** True if at least one input filter slot has an active letter (enabled). No crafting when all are disabled. */
    public boolean hasAnyInputFilterActive() {
        for (int i = 0; i < getEffectiveKeyInputCount(); i++) {
            if (filterLetters[i] > 0) return true;
        }
        return false;
    }

    /**
     * Saves slot-dedication filters from current machine input slots (1:1, 27 slots).
     * Identical to Structure Placer setInventoryFilters: only slots with an item get their
     * markInputFilters entry set; empty input slots are left unchanged. Does not touch
     * inputFilterHandler or filterLetters (those are for crafting only).
     */
    public void setInputFilters() {
        for (int slot = 0; slot < inputHandler.getSlots(); slot++) {
            ItemStack currentStack = inputHandler.getStackInSlot(slot);
            if (!currentStack.isEmpty()) {
                ItemStack filter = currentStack.copyWithCount(1);
                markInputFilters.set(slot, filter);
            }
            // Empty slots remain unchanged (same as iskandert_utilities)
        }
        setChanged();
    }

    /** Clear all slot-dedication filters (Shift+Click on Mark Input). Does not touch inputFilterHandler. */
    public void clearAllInputFilters() {
        for (int i = 0; i < markInputFilters.size(); i++) {
            markInputFilters.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    /** Clear slot-dedication filters where the input slot no longer has the matching item (Ctrl/Alt+Click). */
    public void clearEmptyInputFilters() {
        for (int slot = 0; slot < markInputFilters.size(); slot++) {
            ItemStack filter = markInputFilters.get(slot);
            if (!filter.isEmpty()) {
                ItemStack currentStack = inputHandler.getStackInSlot(slot);
                if (currentStack.isEmpty() || !sameItemType(currentStack, filter)) {
                    markInputFilters.set(slot, ItemStack.EMPTY);
                }
            }
        }
        setChanged();
    }

    /** Returns the slot-dedication filter for the given input slot (for GUI ghost display). */
    public ItemStack getMarkInputFilter(int slot) {
        if (slot >= 0 && slot < markInputFilters.size()) {
            return markInputFilters.get(slot);
        }
        return ItemStack.EMPTY;
    }

    /** Returns true if the given input slot has a mark-input (slot dedication) filter. */
    public boolean hasMarkInputFilter(int slot) {
        return getMarkInputFilter(slot).isEmpty() == false;
    }

    public void setOutputFilters() {
        for (int slot = 0; slot < outputHandler.getSlots(); slot++) {
            ItemStack current = outputHandler.getStackInSlot(slot);
            if (!current.isEmpty()) markOutputFilters.set(slot, current.copyWithCount(1));
        }
        setChanged();
    }

    public void clearAllOutputFilters() {
        for (int i = 0; i < markOutputFilters.size(); i++) markOutputFilters.set(i, ItemStack.EMPTY);
        setChanged();
    }

    public void clearEmptyOutputFilters() {
        for (int slot = 0; slot < markOutputFilters.size(); slot++) {
            ItemStack filter = markOutputFilters.get(slot);
            ItemStack current = outputHandler.getStackInSlot(slot);
            if (!filter.isEmpty() && (current.isEmpty() || !sameItemType(current, filter))) {
                markOutputFilters.set(slot, ItemStack.EMPTY);
            }
        }
        setChanged();
    }

    public ItemStack getMarkOutputFilter(int slot) {
        return slot >= 0 && slot < markOutputFilters.size() ? markOutputFilters.get(slot) : ItemStack.EMPTY;
    }

    public boolean hasMarkOutputFilter(int slot) {
        return !getMarkOutputFilter(slot).isEmpty();
    }

    public void setMarkFilter(boolean output, int slot, ItemStack stack) {
        List<ItemStack> filters = output ? markOutputFilters : markInputFilters;
        if (slot < 0 || slot >= filters.size()) return;
        filters.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        setChanged();
    }

    // ===== Pattern System =====

    public int getCurrentPatternIndex() {
        return currentPatternIndex;
    }

    public int getPatternCount() {
        return getEffectivePatternCount();
    }

    public PatternData getCurrentPattern() {
        int n = getEffectivePatternCount();
        if (currentPatternIndex >= 0 && currentPatternIndex < n) {
            return patterns.get(currentPatternIndex);
        }
        return null;
    }

    public PatternData getPattern(int index) {
        int n = getEffectivePatternCount();
        if (index >= 0 && index < n) {
            return patterns.get(index);
        }
        return null;
    }

    public void setCurrentPatternIndex(int index) {
        int n = getEffectivePatternCount();
        if (index >= 0 && index < n) {
            this.currentPatternIndex = index;
            setChanged();
        }
    }

    /**
     * Assigns a JEI crafting grid to the current pattern, reusing matching variables and
     * allocating empty filter slots for new item types.
     */
    public boolean applyPatternItemAssignment(int cell, ItemStack stack) {
        PatternData pattern = getCurrentPattern();
        if (pattern == null || cell < 0 || cell >= PatternData.GRID_SIZE || stack.isEmpty()) return false;

        int letter = findLetterForExactItem(stack);
        if (letter == PatternData.EMPTY) {
            int freeSlot = findFreeFilterSlot();
            int freeLetter = findFreeLetter();
            if (freeSlot < 0 || freeLetter == PatternData.EMPTY) return false;
            inputFilterStrings[freeSlot] = "-" + BuiltInRegistries.ITEM.getKey(stack.getItem());
            filterLetters[freeSlot] = freeLetter;
            letter = freeLetter;
        }
        pattern.setCell(cell, letter);
        setChanged();
        return true;
    }

    /**
     * Predicts the letter {@link #applyPatternItemAssignment} would use (exact {@code -id} only).
     * Does not mutate state; for newly allocated letters returns the next free letter.
     */
    public int previewExactAssignLetter(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return PatternData.EMPTY;
        int existing = findLetterForExactItem(stack);
        if (existing != PatternData.EMPTY) return existing;
        if (findFreeFilterSlot() < 0) return PatternData.EMPTY;
        return findFreeLetter();
    }

    /** Reuse letter only for simple exact item filters ({@code -id} or bare id), never tag/mod/macro/nbt. */
    private int findLetterForExactItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return PatternData.EMPTY;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return PatternData.EMPTY;
        String itemId = id.toString();
        for (int i = 0; i < getEffectiveKeyInputCount(); i++) {
            if (filterLetters[i] <= 0) continue;
            if (isExactSimpleItemFilter(inputFilterStrings[i], itemId)) {
                return filterLetters[i];
            }
        }
        return PatternData.EMPTY;
    }

    private int findLetterForFilterSpec(String filterSpec) {
        if (filterSpec == null || filterSpec.isEmpty()) return PatternData.EMPTY;
        for (int i = 0; i < getEffectiveKeyInputCount(); i++) {
            if (filterLetters[i] > 0 && filterSpec.equals(inputFilterStrings[i])) {
                return filterLetters[i];
            }
        }
        return PatternData.EMPTY;
    }

    public static boolean isExactSimpleItemFilter(String filter, String itemId) {
        if (filter == null || filter.isEmpty() || itemId == null || itemId.isEmpty()) return false;
        if (filter.startsWith("-")) return itemId.equals(filter.substring(1));
        if (filter.startsWith("#") || filter.startsWith("@") || filter.startsWith("&") || filter.startsWith("?")) {
            return false;
        }
        return itemId.equals(filter);
    }

    private static String itemFilterSpec(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        return "-" + BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    private static String resolveFilterSpec(int cell, List<ItemStack> ingredients, @org.jetbrains.annotations.Nullable List<String> filterSpecs) {
        if (filterSpecs != null && cell < filterSpecs.size()) {
            String spec = filterSpecs.get(cell);
            if (spec != null && !spec.isEmpty()) return spec;
        }
        ItemStack stack = cell < ingredients.size() ? ingredients.get(cell) : ItemStack.EMPTY;
        return itemFilterSpec(stack);
    }

    private int findFreeFilterSlot() {
        for (int i = 0; i < getEffectiveKeyInputCount(); i++) {
            if (inputFilterStrings[i].isEmpty() && filterLetters[i] == PatternData.EMPTY) return i;
        }
        // Allow reclaiming letter-unlocked slots that still have an empty filter.
        for (int i = 0; i < getEffectiveKeyInputCount(); i++) {
            if (inputFilterStrings[i].isEmpty()) return i;
        }
        return -1;
    }

    private int findFreeLetter() {
        boolean[] used = new boolean[PatternData.MAX_LETTER + 1];
        for (int letter : filterLetters) {
            if (letter > 0 && letter <= PatternData.MAX_LETTER) used[letter] = true;
        }
        for (int letter = 1; letter <= PatternData.MAX_LETTER; letter++) {
            if (!used[letter]) return letter;
        }
        return PatternData.EMPTY;
    }

    /** Non-mutating JEI transfer preview (grid letters + variable filters/letters). */
    public record JeiTransferPreview(int[] cellLetters, String[] variableFilters, int[] variableLetters) {}

    /**
     * Commits JEI variable filters/letters immediately (not the pattern grid).
     * Pattern cells stay client-pending until Save.
     *
     * @return letter for each of the 9 grid cells, or {@code null} if the transfer is not possible
     */
    @org.jetbrains.annotations.Nullable
    public int[] applyJeiVariablesOnly(List<ItemStack> ingredients) {
        return applyJeiVariablesOnly(ingredients, null);
    }

    @org.jetbrains.annotations.Nullable
    public int[] applyJeiVariablesOnly(List<ItemStack> ingredients, @org.jetbrains.annotations.Nullable List<String> filterSpecs) {
        JeiTransferPreview preview = previewJeiTransfer(ingredients, filterSpecs);
        if (preview == null) return null;

        int keyCount = Math.min(preview.variableFilters().length, getEffectiveKeyInputCount());
        for (int i = 0; i < keyCount; i++) {
            inputFilterStrings[i] = preview.variableFilters()[i] == null ? "" : preview.variableFilters()[i];
            filterLetters[i] = preview.variableLetters()[i];
        }
        setChanged();
        return preview.cellLetters();
    }

    /**
     * Client-side mirror of {@link #applyJeiVariablesOnly}: resolves grid + variables without mutating this BE.
     */
    @org.jetbrains.annotations.Nullable
    public int[] previewJeiGridLetters(List<ItemStack> ingredients) {
        return previewJeiGridLetters(ingredients, null);
    }

    @org.jetbrains.annotations.Nullable
    public int[] previewJeiGridLetters(List<ItemStack> ingredients, @org.jetbrains.annotations.Nullable List<String> filterSpecs) {
        JeiTransferPreview preview = previewJeiTransfer(ingredients, filterSpecs);
        return preview == null ? null : preview.cellLetters();
    }

    @org.jetbrains.annotations.Nullable
    public JeiTransferPreview previewJeiTransfer(List<ItemStack> ingredients, @org.jetbrains.annotations.Nullable List<String> filterSpecs) {
        if (ingredients.size() < PatternData.GRID_SIZE) return null;
        int keyCount = getEffectiveKeyInputCount();
        String[] filters = java.util.Arrays.copyOf(inputFilterStrings, keyCount);
        int[] letters = java.util.Arrays.copyOf(filterLetters, keyCount);

        List<String> newSpecs = new ArrayList<>();
        for (int cell = 0; cell < PatternData.GRID_SIZE; cell++) {
            String spec = resolveFilterSpec(cell, ingredients, filterSpecs);
            if (spec.isEmpty()) continue;
            if (previewFindLetterBySpec(spec, filters, letters) == PatternData.EMPTY) {
                if (!newSpecs.contains(spec)) newSpecs.add(spec);
            }
        }

        int freeSlots = 0;
        for (int i = 0; i < keyCount; i++) {
            if (filters[i].isEmpty()) freeSlots++;
        }
        boolean[] usedLetters = new boolean[PatternData.MAX_LETTER + 1];
        for (int letter : letters) {
            if (letter > 0 && letter <= PatternData.MAX_LETTER) usedLetters[letter] = true;
        }
        int freeLetters = 0;
        for (int letter = 1; letter <= PatternData.MAX_LETTER; letter++) {
            if (!usedLetters[letter]) freeLetters++;
        }
        if (newSpecs.size() > freeSlots || newSpecs.size() > freeLetters) return null;

        int[] cellLetters = new int[PatternData.GRID_SIZE];
        for (int cell = 0; cell < PatternData.GRID_SIZE; cell++) {
            String spec = resolveFilterSpec(cell, ingredients, filterSpecs);
            if (spec.isEmpty()) {
                cellLetters[cell] = PatternData.EMPTY;
                continue;
            }
            int letter = previewFindLetterBySpec(spec, filters, letters);
            if (letter == PatternData.EMPTY) {
                int freeSlot = -1;
                for (int i = 0; i < keyCount; i++) {
                    if (filters[i].isEmpty() && letters[i] == PatternData.EMPTY) {
                        freeSlot = i;
                        break;
                    }
                }
                if (freeSlot < 0) {
                    for (int i = 0; i < keyCount; i++) {
                        if (filters[i].isEmpty()) {
                            freeSlot = i;
                            break;
                        }
                    }
                }
                int freeLetter = PatternData.EMPTY;
                if (freeSlot >= 0 && letters[freeSlot] > PatternData.EMPTY) {
                    freeLetter = letters[freeSlot];
                } else {
                    for (int l = 1; l <= PatternData.MAX_LETTER; l++) {
                        if (!usedLetters[l]) {
                            freeLetter = l;
                            break;
                        }
                    }
                }
                if (freeSlot < 0 || freeLetter == PatternData.EMPTY) return null;
                filters[freeSlot] = spec;
                letters[freeSlot] = freeLetter;
                usedLetters[freeLetter] = true;
                letter = freeLetter;
            }
            cellLetters[cell] = letter;
        }
        return new JeiTransferPreview(cellLetters, filters, letters);
    }

    private static int previewFindLetterBySpec(String filterSpec, String[] filters, int[] letters) {
        if (filterSpec == null || filterSpec.isEmpty()) return PatternData.EMPTY;
        for (int i = 0; i < filters.length; i++) {
            if (letters[i] > 0 && filterSpec.equals(filters[i])) {
                return letters[i];
            }
        }
        return PatternData.EMPTY;
    }

    public boolean applyJeiPattern(List<ItemStack> ingredients) {
        int[] cellLetters = applyJeiVariablesOnly(ingredients);
        if (cellLetters == null) return false;
        PatternData pattern = getCurrentPattern();
        if (pattern == null) return false;
        for (int cell = 0; cell < PatternData.GRID_SIZE; cell++) {
            if (cellLetters[cell] != PatternData.EMPTY) {
                pattern.setCell(cell, cellLetters[cell]);
            }
        }
        setChanged();
        return true;
    }

    public boolean applyJeiPattern(List<ItemStack> ingredients, int craftingMode) {
        boolean applied = applyJeiPattern(ingredients);
        if (applied) {
            PatternData pattern = getCurrentPattern();
            if (pattern != null) pattern.setCraftingMode(craftingMode);
        }
        return applied;
    }

    public void setCraftingMode(int mode) {
        PatternData pattern = getCurrentPattern();
        if (pattern != null) {
            pattern.setCraftingMode(mode);
            setChanged();
        }
    }

    // ===== Crafting Logic =====

    /**
     * Called every tick on the server side by the BlockEntityTicker.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, ImprovedPatternCrafterBlockEntity entity) {
        // When logic modules are removed, effective pattern count can drop: reset to first pattern (1) if current is out of range
        int effectiveCount = entity.getEffectivePatternCount();
        if (entity.currentPatternIndex >= effectiveCount) {
            entity.currentPatternIndex = 0;
            entity.setChanged();
        }

        // Stay entirely idle until inventory/filter state changes.
        if (entity.craftIdleUntilChange || !entity.hasAnyInputFilterActive() || !entity.hasAnyMachineInput()) {
            return;
        }

        int signal = entity.level != null ? entity.level.getBestNeighborSignal(entity.worldPosition) : 0;
        boolean hasSignal = signal > 0;

        if (entity.redstoneMode == 3) {
            // PULSE: schedule one craft on rising edge; it runs after the normal crafting interval (not instant)
            if (entity.pulseIgnoreTimer > 0) {
                entity.pulseIgnoreTimer--;
            }
            if (entity.pulseIgnoreTimer == 0 && hasSignal && !entity.previousRedstoneState) {
                entity.pulseIgnoreTimer = PULSE_IGNORE_INTERVAL;
                entity.pulseCraftPending = true;
                entity.craftingTimer = 0;
            }
            entity.previousRedstoneState = hasSignal;

            if (entity.pulseCraftPending) {
                entity.craftingTimer++;
                int interval = entity.getEffectiveCraftingInterval();
                if (entity.craftingTimer >= interval) {
                    entity.craftingTimer = 0;
                    entity.pulseCraftPending = false;
                    entity.attemptCraft();
                }
            }
            return;
        }

        if (!entity.isRedstoneAllowed()) {
            return;
        }

        entity.craftingTimer++;
        int interval = entity.getEffectiveCraftingInterval();

        if (entity.craftingTimer >= interval) {
            entity.craftingTimer = 0;
            entity.attemptCraft();
        }
    }

    private boolean hasAnyMachineInput() {
        for (int i = 0; i < inputHandler.getSlots(); i++) {
            if (!inputHandler.getStackInSlot(i).isEmpty()) return true;
        }
        return false;
    }

    /** True when redstone mode allows crafting this tick (not used for PULSE). */
    private boolean isRedstoneAllowed() {
        if (level == null) return true;
        int signal = level.getBestNeighborSignal(worldPosition);
        return switch (redstoneMode) {
            case 0 -> true;   // Ignore: always
            case 1 -> signal == 0;  // Low: craft when no signal
            case 2 -> signal > 0;   // High: craft when signal
            case 4 -> false;  // Disabled: never
            default -> true;
        };
    }

    /**
     * Attempts to craft: for each input slot (0, 1, 2, ...) try all patterns in sequence.
     * Only when all patterns have been tried for the current slot do we move to the next slot.
     * Ensures a valid key in filters (non-empty pattern) before trying.
     */
    private void attemptCraft() {
        if (level == null || level.isClientSide()) return;

        int totalPatterns = getEffectivePatternCount();
        if (totalPatterns == 0) return;

        int totalSlots = inputHandler.getSlots();
        craftingPatternIndex = craftingPatternIndex % totalPatterns;

        // Outer loop: input slots (0, 1, 2, ...)
        for (int prioritySlot = 0; prioritySlot < totalSlots; prioritySlot++) {
            // Inner loop: all patterns for this slot
            for (int patternAttempt = 0; patternAttempt < totalPatterns; patternAttempt++) {
                int patternIndex = (craftingPatternIndex + patternAttempt) % totalPatterns;
                PatternData pattern = patterns.get(patternIndex);
                if (isPatternEmpty(pattern)) continue;

                if (pattern.isToolSafeguard()) {
                    CraftBlockReason toolSafety = ejectToolsAtBreakingPoint();
                    if (toolSafety == CraftBlockReason.STORAGE_FULL) {
                        craftIdleUntilChange = true;
                        return;
                    }
                }

                lastCraftResult = ItemStack.EMPTY;
                CraftBlockReason result = tryCraftPattern(pattern, prioritySlot);
                if (result == CraftBlockReason.STORAGE_FULL) {
                    craftIdleUntilChange = true;
                    return;
                }
                if (result == CraftBlockReason.SUCCESS) {
                    int batch = getProductionBatch(lastCraftResult);
                    for (int extra = 1; extra < batch; extra++) {
                        CraftBlockReason extraResult = tryCraftPattern(pattern, prioritySlot);
                        if (extraResult == CraftBlockReason.STORAGE_FULL) {
                            craftIdleUntilChange = true;
                            break;
                        }
                        if (extraResult != CraftBlockReason.SUCCESS) break;
                    }
                    craftingPatternIndex = (patternIndex + 1) % totalPatterns;
                    return;
                }
            }
        }
    }

    /**
     * Tries to craft using a specific pattern, preferring the given input slot when resolving items.
     * If a result is blacklisted or no valid recipe is found, excludes the item types
     * that led to the failure and retries with different items from input.
     * @param pattern the pattern to use
     * @param prioritySlot input slot to try first when resolving the grid (then slot+1, slot+2, ...)
     * @return why crafting succeeded or was blocked
     */
    private CraftBlockReason tryCraftPattern(PatternData pattern, int prioritySlot) {
        // Base exclusion: specific filter items (for wildcard matching)
        List<String> specificItems = collectSpecificFilterItems();
        // Additional exclusions from blacklisted results (grows on each retry)
        List<ItemStack> craftExclusions = new ArrayList<>();
        // Type-only exclusions for retrying different candidates (forces trying next assigned type)
        List<ItemStack> craftTypeExclusions = new ArrayList<>();

        // Retry loop: when a result is blacklisted, exclude those items and try again
        int maxRetries = inputHandler.getSlots(); // can't have more distinct items than slots
        for (int retry = 0; retry <= maxRetries; retry++) {

            // Track available item counts per input slot
            int[] availableCounts = new int[inputHandler.getSlots()];
            for (int i = 0; i < availableCounts.length; i++) {
                availableCounts[i] = inputHandler.getStackInSlot(i).getCount();
            }

            // Resolve each grid cell to an actual item from inputHandler.
            // Key rule: all cells with the SAME letter must use the SAME item type.
            // This prevents mixed grids (e.g., 1 iron + 8 redstone) that don't form valid recipes.
            Map<Integer, ItemStack> letterItemDecision = new HashMap<>();
            List<ItemStack> craftingGrid = new ArrayList<>(9);
            int[] reservedSlotForCell = new int[9];
            for (int i = 0; i < 9; i++) reservedSlotForCell[i] = -1;
            boolean resolutionFailed = false;

            for (int cell = 0; cell < PatternData.GRID_SIZE; cell++) {
                int letterValue = pattern.getCell(cell);
                if (letterValue == PatternData.EMPTY) {
                    craftingGrid.add(ItemStack.EMPTY);
                    continue;
                }

                int foundSlot;

                if (letterItemDecision.containsKey(letterValue)) {
                    // Already decided which item type for this letter - find more of the same
                    ItemStack decidedItem = letterItemDecision.get(letterValue);
                    foundSlot = findExactItem(decidedItem, availableCounts, prioritySlot);
                } else {
                    // First cell with this letter - pick from this letter's filter, or wildcard if unassigned
                    List<String> acceptedItems = getAcceptedItemsForLetter(letterValue);

                    if (acceptedItems.isEmpty()) {
                        // Letter has no filter assigned: accept any item NOT assigned by other filter slots
                        foundSlot = findWildcardItem(specificItems, craftExclusions, craftTypeExclusions, availableCounts, prioritySlot);
                    } else {
                        foundSlot = findSpecificItem(acceptedItems, availableCounts, craftExclusions, craftTypeExclusions, prioritySlot);
                    }

                    // Commit this item type for all cells with this letter
                    if (foundSlot >= 0) {
                        letterItemDecision.put(letterValue,
                                inputHandler.getStackInSlot(foundSlot).copyWithCount(1));
                    }
                }

                if (foundSlot == -1) {
                    resolutionFailed = true;
                    break;
                }

                availableCounts[foundSlot]--;
                reservedSlotForCell[cell] = foundSlot;
                craftingGrid.add(inputHandler.getStackInSlot(foundSlot).copyWithCount(1));
            }

            if (resolutionFailed) {
                // No items found for this pattern (e.g. first cell has no valid slot): skip retries and try next pattern
                if (letterItemDecision.isEmpty()) {
                    return CraftBlockReason.NO_MATCH;
                }
                // Exclude decided items and retry with different item choices
                for (ItemStack decided : letterItemDecision.values()) {
                    addDistinctItems(craftExclusions, List.of(decided));
                }
                continue;
            }

            // Create CraftingInput and check vanilla recipe
            CraftingInput craftingInput = CraftingInput.of(3, 3, craftingGrid);
            Optional<RecipeHolder<CraftingRecipe>> recipe = findBestCraftingRecipe(craftingInput, pattern.getCraftingMode());

            if (recipe.isEmpty()) {
                // No recipe found - exclude these items and retry with different ones
                addDistinctItems(craftExclusions, craftingGrid);
                excludeDistinctItemTypes(craftTypeExclusions, letterItemDecision.values());
                continue;
            }

            ItemStack result = recipe.get().value().assemble(craftingInput);
            if (result.isEmpty()) {
                addDistinctItems(craftExclusions, craftingGrid);
                excludeDistinctItemTypes(craftTypeExclusions, letterItemDecision.values());
                continue;
            }

            // Check output filter blacklist
            if (isInOutputFilter(result)) {
                // Blacklisted! Exclude the items that produced this result and retry
                addDistinctItems(craftExclusions, craftingGrid);
                excludeDistinctItemTypes(craftTypeExclusions, letterItemDecision.values());
                continue;
            }

            // Space check: must fit result + remainders; never drop anything.
            NonNullList<ItemStack> remainingItemsSim = recipe.get().value().getRemainingItems(craftingInput);
            int resultMode = pattern.getResultMode();
            int ingredientMode = pattern.getIngredientMode();
            if (!canAcceptAfterVirtualCraft(result, reservedSlotForCell, remainingItemsSim, resultMode, ingredientMode)) {
                return CraftBlockReason.STORAGE_FULL;
            }
            // Result/ingredient modes are per-pattern; recursion decision is set only on success.

            // Check if there's enough energy (when capacity is 0, never require or consume - e.g. normal Pattern Crafter)
            int energyCost = getEnergyPerCraft();
            if (getEnergyCapacity() == 0) energyCost = 0;
            if (energyCost > 0 && energyStorage.getEnergyStored() < energyCost) return CraftBlockReason.NO_MATCH;

            // === All checks passed - execute the craft (never consume energy on failure) ===

            // Consume energy only on successful craft when machine has energy (config; 0 for normal)
            if (energyCost > 0) {
                energyStorage.consumeEnergy(energyCost);
            }

            // Consume one item from each reserved input slot
            for (int cell = 0; cell < PatternData.GRID_SIZE; cell++) {
                if (reservedSlotForCell[cell] >= 0) {
                    inputHandler.extractItem(reservedSlotForCell[cell], 1, false);
                }
            }

            routeCraftResult(result, resultMode);
            lastCraftResult = result.copy();

            // Handle remainder items (e.g., empty buckets from water bucket recipes)
            NonNullList<ItemStack> remainingItems = remainingItemsSim;
            for (int cell = 0; cell < remainingItems.size(); cell++) {
                ItemStack remainder = remainingItems.get(cell);
                if (!remainder.isEmpty()) {
                    insertRemainderItem(remainder, ingredientMode);
                }
            }

            setChanged();
            return CraftBlockReason.SUCCESS;
        }
        return CraftBlockReason.NO_MATCH;
    }

    /**
     * Adds all distinct non-empty item types from the crafting grid to the exclusion list.
     */
    private void addDistinctItems(List<ItemStack> exclusions, List<ItemStack> craftingGrid) {
        for (ItemStack gridItem : craftingGrid) {
            if (gridItem.isEmpty()) continue;
            boolean alreadyPresent = false;
            for (ItemStack existing : exclusions) {
                if (ItemStack.isSameItemSameComponents(existing, gridItem)) {
                    alreadyPresent = true;
                    break;
                }
            }
            if (!alreadyPresent) {
                exclusions.add(gridItem.copy());
            }
        }
    }

    // ===== Crafting Helpers =====

    private Optional<RecipeHolder<CraftingRecipe>> findBestCraftingRecipe(CraftingInput input, int mode) {
        if (level == null) return Optional.empty();
        List<RecipeHolder<CraftingRecipe>> matches = ((net.minecraft.server.level.ServerLevel) level)
                .recipeAccess().getRecipes().stream()
                .filter(holder -> holder.value().getType() == RecipeType.CRAFTING)
                .map(holder -> (RecipeHolder<CraftingRecipe>) (RecipeHolder<?>) holder)
                .filter(holder -> holder.value().matches(input, level))
                .toList();
        if (mode == 1) {
            return matches.stream().filter(holder -> holder.value() instanceof ShapedRecipe).findFirst();
        }
        if (mode == 2) {
            return matches.stream().filter(holder -> !(holder.value() instanceof ShapedRecipe)).findFirst();
        }
        return matches.stream().filter(holder -> holder.value() instanceof ShapedRecipe).findFirst()
                .or(() -> matches.stream().findFirst());
    }

    private CraftBlockReason ejectToolsAtBreakingPoint() {
        for (int slot = 0; slot < inputHandler.getSlots(); slot++) {
            ItemStack stack = inputHandler.getStackInSlot(slot);
            if (!stack.isDamageableItem() || stack.getDamageValue() < stack.getMaxDamage() - 1) continue;
            if (!canInsertIntoOutput(stack)) return CraftBlockReason.STORAGE_FULL;
            ItemStack extracted = inputHandler.extractItem(slot, stack.getCount(), false);
            insertIntoOutput(extracted);
        }
        return CraftBlockReason.SUCCESS;
    }

    /**
     * Checks if a pattern has all grid cells empty.
     */
    private boolean isPatternEmpty(PatternData pattern) {
        for (int i = 0; i < PatternData.GRID_SIZE; i++) {
            if (pattern.getCell(i) != PatternData.EMPTY) return false;
        }
        return true;
    }

    /**
     * Collects all specific (non-wildcard) items from the input filter.
     * These are items in ghost slots that have a letter assigned.
     * Used to build the exclusion set for wildcard matching.
     */
    private List<String> collectSpecificFilterItems() {
        List<String> specific = new ArrayList<>();
        for (int i = 0; i < getEffectiveKeyInputCount(); i++) {
            if (filterLetters[i] > 0) {
                String filter = inputFilterStrings[i];
                if (!filter.isEmpty() && !specific.contains(filter)) specific.add(filter);
            }
        }
        return specific;
    }

    /**
     * Gets the list of accepted items for a specific letter.
     * Scans all filter slots with the matching letter that have a ghost item.
     * If the list is empty, it means this letter is a wildcard.
     */
    private List<String> getAcceptedItemsForLetter(int letterValue) {
        List<String> accepted = new ArrayList<>();
        for (int i = 0; i < getEffectiveKeyInputCount(); i++) {
            if (filterLetters[i] == letterValue) {
                String filter = inputFilterStrings[i];
                if (!filter.isEmpty()) accepted.add(filter);
            }
        }
        return accepted;
    }

    /**
     * Finds an exact item match in the inputHandler, starting from the given slot (then wrapping).
     * Used when a letter's item type has already been decided.
     * @return slot index, or -1 if not found
     */
    private int findExactItem(ItemStack target, int[] availableCounts, int startSlot) {
        int n = inputHandler.getSlots();
        for (int i = 0; i < n; i++) {
            int slot = (startSlot + i) % n;
            if (availableCounts[slot] <= 0) continue;
            ItemStack slotItem = inputHandler.getStackInSlot(slot);
            if (!slotItem.isEmpty() && sameItemType(slotItem, target)) {
                return slot;
            }
        }
        return -1;
    }

    /**
     * Finds an item in the inputHandler that matches any of the accepted items, starting from startSlot.
     * Skips slots with no available items left, and items in the craft exclusion list.
     * @return slot index, or -1 if not found
     */
    private int findSpecificItem(List<String> acceptedItems, int[] availableCounts,
                                 List<ItemStack> craftExclusions, List<ItemStack> craftTypeExclusions, int startSlot) {
        int n = inputHandler.getSlots();
        for (int i = 0; i < n; i++) {
            int slot = (startSlot + i) % n;
            if (availableCounts[slot] <= 0) continue;
            ItemStack slotItem = inputHandler.getStackInSlot(slot);
            if (slotItem.isEmpty()) continue;
            // Skip items excluded due to blacklisted results
            if (isItemInList(slotItem, craftExclusions)) continue;
            if (isItemTypeInList(slotItem, craftTypeExclusions)) continue;
            for (String accepted : acceptedItems) {
                if (DeepDrawerItemFilter.matchesFilterEntry(
                        slotItem, accepted, level != null ? level.registryAccess() : null)) {
                    return slot;
                }
            }
        }
        return -1;
    }

    /**
     * Finds any item in the inputHandler that is NOT excluded for wildcard matching.
     * Ghost-filter exclusions compare by item type only; crafting retry exclusions use full components.
     */
    private int findWildcardItem(List<String> ghostSpecificExclusions, List<ItemStack> craftExclusions,
                                 List<ItemStack> craftTypeExclusions,
                                 int[] availableCounts, int startSlot) {
        int n = inputHandler.getSlots();
        for (int i = 0; i < n; i++) {
            int slot = (startSlot + i) % n;
            if (availableCounts[slot] <= 0) continue;
            ItemStack slotItem = inputHandler.getStackInSlot(slot);
            if (slotItem.isEmpty()) continue;
            if (!isWildcardExcluded(slotItem, ghostSpecificExclusions, craftExclusions, craftTypeExclusions)) return slot;
        }
        return -1;
    }

    /** Item type only (ignore data components) — mark input and ghost key matching. */
    private static boolean sameItemType(ItemStack a, ItemStack b) {
        return ItemStack.isSameItem(a, b);
    }

    private boolean isWildcardExcluded(ItemStack slotItem, List<String> ghostSpecificExclusions,
                                              List<ItemStack> craftExclusions, List<ItemStack> craftTypeExclusions) {
        for (String ghost : ghostSpecificExclusions) {
            if (DeepDrawerItemFilter.matchesFilterEntry(
                    slotItem, ghost, level != null ? level.registryAccess() : null)) return true;
        }
        for (ItemStack craft : craftExclusions) {
            if (ItemStack.isSameItemSameComponents(slotItem, craft)) return true;
        }
        for (ItemStack craftType : craftTypeExclusions) {
            if (sameItemType(slotItem, craftType)) return true;
        }
        return false;
    }

    private static boolean isItemTypeInList(ItemStack item, List<ItemStack> list) {
        for (ItemStack entry : list) {
            if (sameItemType(item, entry)) return true;
        }
        return false;
    }

    private static void excludeDistinctItemTypes(List<ItemStack> exclusions, Iterable<ItemStack> items) {
        for (ItemStack item : items) {
            if (item.isEmpty()) continue;
            if (!isItemTypeInList(item, exclusions)) {
                exclusions.add(item.copyWithCount(1));
            }
        }
    }

    /**
     * Checks if an item matches any item in the given list.
     */
    private boolean isItemInList(ItemStack item, List<ItemStack> list) {
        for (ItemStack entry : list) {
            if (ItemStack.isSameItemSameComponents(item, entry)) return true;
        }
        return false;
    }

    /**
     * Checks if the crafted result matches any item in the output filter (blacklist).
     */
    private boolean isInOutputFilter(ItemStack result) {
        for (String filter : forbiddenFilters) {
            if (!filter.isEmpty() && DeepDrawerItemFilter.matchesFilterEntry(
                    result, filter, level != null ? level.registryAccess() : null)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the result item can be inserted into the output handler (simulate).
     */
    private boolean canInsertIntoOutput(ItemStack result) {
        ItemStack toInsert = result.copy();
        for (int i = 0; i < outputHandler.getSlots(); i++) {
            toInsert = outputHandler.insertItem(i, toInsert, true);
            if (toInsert.isEmpty()) return true;
        }
        return false;
    }

    /**
     * Actually inserts the result item into the output handler.
     */
    private void insertIntoOutput(ItemStack result) {
        ItemStack toInsert = result.copy();
        for (int i = 0; i < outputHandler.getSlots(); i++) {
            toInsert = outputHandler.insertItem(i, toInsert, false);
            if (toInsert.isEmpty()) break;
        }
    }

    /**
     * After ingredients are consumed (simulated): merge craft result into input; return what could not fit (simulate=true).
     */
    private ItemStack simulateOverflowAfterVirtualCraft(ItemStack result, int[] reservedSlotForCell) {
        ItemStackHandler sim = new ItemStackHandler(inputHandler.getSlots());
        for (int i = 0; i < inputHandler.getSlots(); i++) {
            sim.setStackInSlot(i, inputHandler.getStackInSlot(i).copy());
        }
        for (int cell = 0; cell < PatternData.GRID_SIZE; cell++) {
            int s = reservedSlotForCell[cell];
            if (s >= 0) {
                sim.extractItem(s, 1, false);
            }
        }
        ItemStack rest = result.copy();
        for (int i = 0; i < sim.getSlots(); i++) {
            rest = sim.insertItem(i, rest, true);
            if (rest.isEmpty()) break;
        }
        return rest;
    }

    /**
     * Simulate consuming ingredients, routing primary result, and inserting all remainders.
     * If anything would overflow, crafting must not happen (no drop fallback).
     */
    private boolean canAcceptAfterVirtualCraft(ItemStack result, int[] reservedSlotForCell, List<ItemStack> remainders,
                                               int resultMode, int ingredientMode) {
        ItemStackHandler inputSim = new ItemStackHandler(inputHandler.getSlots());
        ItemStackHandler outputSim = new ItemStackHandler(outputHandler.getSlots());
        for (int i = 0; i < inputHandler.getSlots(); i++) {
            inputSim.setStackInSlot(i, inputHandler.getStackInSlot(i).copy());
        }
        for (int i = 0; i < outputHandler.getSlots(); i++) {
            outputSim.setStackInSlot(i, outputHandler.getStackInSlot(i).copy());
        }

        // Consume ingredients
        for (int cell = 0; cell < PatternData.GRID_SIZE; cell++) {
            int s = reservedSlotForCell[cell];
            if (s >= 0) {
                inputSim.extractItem(s, 1, false);
            }
        }

        // Route primary result
        ItemStack rest = result.copy();
        if (resultMode == 2 || (resultMode == 3 && isExplicitActiveVariableInput(result))) {
            for (int i = 0; i < inputSim.getSlots(); i++) {
                rest = inputSim.insertItem(i, rest, true);
                if (rest.isEmpty()) break;
            }
        }
        if (!rest.isEmpty()) {
            for (int i = 0; i < outputSim.getSlots(); i++) {
                rest = outputSim.insertItem(i, rest, true);
                if (rest.isEmpty()) break;
            }
        }
        if (!rest.isEmpty()) return false;

        // Route remainders
        for (ItemStack remainderStack : remainders) {
            if (remainderStack.isEmpty()) continue;
            ItemStack rem = remainderStack.copy();
            if (ingredientMode == 2) {
                for (int i = 0; i < outputSim.getSlots(); i++) {
                    rem = outputSim.insertItem(i, rem, true);
                    if (rem.isEmpty()) break;
                }
            } else {
                for (int i = 0; i < inputSim.getSlots(); i++) {
                    rem = inputSim.insertItem(i, rem, true);
                    if (rem.isEmpty()) break;
                }
                if (!rem.isEmpty()) {
                    for (int i = 0; i < outputSim.getSlots(); i++) {
                        rem = outputSim.insertItem(i, rem, true);
                        if (rem.isEmpty()) break;
                    }
                }
            }
            if (!rem.isEmpty()) return false;
        }
        return true;
    }

    /**
     * True when an active variable (letter assigned + non-empty filter) matches the stack.
     * Empty / letter-0 variables do not count.
     */
    private boolean isExplicitActiveVariableInput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var registries = level != null ? level.registryAccess() : null;
        for (int i = 0; i < getEffectiveKeyInputCount(); i++) {
            if (filterLetters[i] <= PatternData.EMPTY) continue;
            String filter = inputFilterStrings[i];
            if (filter == null || filter.isEmpty()) continue;
            if (DeepDrawerItemFilter.matchesFilterEntry(stack, filter, registries)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Routes primary craft output:
     * mode 1 = output only;
     * mode 2 Keep = input first then overflow to output;
     * mode 3 Smart = input first only if an active variable matches, otherwise output only.
     */
    private void routeCraftResult(ItemStack result, int resultMode) {
        if (resultMode == 1 || (resultMode == 3 && !isExplicitActiveVariableInput(result))) {
            insertIntoOutput(result);
            return;
        }
        ItemStack rest = result.copy();
        for (int i = 0; i < inputHandler.getSlots(); i++) {
            rest = inputHandler.insertItem(i, rest, false);
            if (rest.isEmpty()) return;
        }
        insertIntoOutput(rest);
    }

    /**
     * Inserts a remainder item (e.g., empty bucket) back into the machine.
     * Mode 1: input first, then output, then drop. Mode 2: output first, then drop.
     */
    private void insertRemainderItem(ItemStack remainder, int ingredientMode) {
        if (ingredientMode == 2) {
            for (int i = 0; i < outputHandler.getSlots(); i++) {
                remainder = outputHandler.insertItem(i, remainder, false);
                if (remainder.isEmpty()) return;
            }
        } else {
            for (int i = 0; i < inputHandler.getSlots(); i++) {
                remainder = inputHandler.insertItem(i, remainder, false);
                if (remainder.isEmpty()) return;
            }
            for (int i = 0; i < outputHandler.getSlots(); i++) {
                remainder = outputHandler.insertItem(i, remainder, false);
                if (remainder.isEmpty()) return;
            }
        }
        // No drop fallback: crafting should have been blocked by canAcceptAfterVirtualCraft.
    }

    // ===== Persistent state =====

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        // New format: string filters + letters (migrate from ItemStacks on load)
        net.minecraft.world.level.storage.ValueOutput.ValueOutputList inputFilterSlots =
                output.childrenList("inputFilterSlots");
        int filterSlots = Math.min(inputFilterHandler.getSlots(), inputFilterStrings.length);
        for (int i = 0; i < filterSlots; i++) {
            var slotOut = inputFilterSlots.addChild();
            String filter = inputFilterStrings[i];
            if (filter != null && !filter.isEmpty()) slotOut.putString("filter", filter);
            slotOut.putInt("letter", filterLetters[i]);
        }

        net.minecraft.world.level.storage.ValueOutput.ValueOutputList forbiddenOut =
                output.childrenList("forbiddenFilters");
        for (String filter : forbiddenFilters) {
            if (filter == null || filter.isEmpty()) continue;
            var entry = forbiddenOut.addChild();
            entry.putString("filter", filter);
        }
        if (forbiddenOut.isEmpty()) output.discard("forbiddenFilters");

        saveHandler(output, "upgrades", upgradeHandler);
        saveHandler(output, "output", outputHandler);
        saveHandler(output, "input", inputHandler);

        net.minecraft.world.level.storage.ValueOutput.TypedOutputList<net.minecraft.world.ItemStackWithSlot> marks =
                output.list("markInputFilters", net.minecraft.world.ItemStackWithSlot.CODEC);
        for (int i = 0; i < markInputFilters.size(); i++) {
            ItemStack stack = markInputFilters.get(i);
            if (!stack.isEmpty()) marks.add(new net.minecraft.world.ItemStackWithSlot(i, stack));
        }
        if (marks.isEmpty()) output.discard("markInputFilters");
        net.minecraft.world.level.storage.ValueOutput.TypedOutputList<net.minecraft.world.ItemStackWithSlot> outputMarks =
                output.list("markOutputFilters", net.minecraft.world.ItemStackWithSlot.CODEC);
        for (int i = 0; i < markOutputFilters.size(); i++) {
            ItemStack stack = markOutputFilters.get(i);
            if (!stack.isEmpty()) outputMarks.add(new net.minecraft.world.ItemStackWithSlot(i, stack));
        }
        if (outputMarks.isEmpty()) output.discard("markOutputFilters");

        output.putInt("Energy", energyStorage.getEnergyStored());
        output.putInt("craftingTimer", craftingTimer);
        output.putInt("craftingPatternIndex", craftingPatternIndex);
        output.putInt("redstoneMode", redstoneMode);
        output.putBoolean("previousRedstoneState", previousRedstoneState);
        output.putInt("pulseIgnoreTimer", pulseIgnoreTimer);
        output.putBoolean("pulseCraftPending", pulseCraftPending);
        output.putBoolean("autoclearVariables", autoclearVariables);
        output.putInt("currentPattern", currentPatternIndex);

        net.minecraft.world.level.storage.ValueOutput.TypedOutputList<CompoundTag> savedPatterns =
                output.list("patterns", CompoundTag.CODEC);
        for (PatternData pattern : patterns) savedPatterns.add(pattern.save());
    }

    private static void saveHandler(net.minecraft.world.level.storage.ValueOutput output, String key, ItemStackHandler handler) {
        net.minecraft.world.level.storage.ValueOutput.TypedOutputList<net.minecraft.world.ItemStackWithSlot> items =
                output.list(key, net.minecraft.world.ItemStackWithSlot.CODEC);
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) items.add(new net.minecraft.world.ItemStackWithSlot(i, stack));
        }
        if (items.isEmpty()) output.discard(key);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        java.util.Arrays.fill(inputFilterStrings, "");
        java.util.Arrays.fill(filterLetters, 0);
        boolean loadedNewFilters = false;
        int slotIdx = 0;
        for (var slotIn : input.childrenListOrEmpty("inputFilterSlots")) {
            loadedNewFilters = true;
            if (slotIdx >= Math.min(inputFilterHandler.getSlots(), filterLetters.length)) break;
            String filter = slotIn.getString("filter").orElse("");
            if (!filter.isEmpty()) inputFilterStrings[slotIdx] = filter;
            filterLetters[slotIdx] = slotIn.getIntOr("letter", 0);
            inputFilterHandler.setStackInSlot(slotIdx, ItemStack.EMPTY);
            slotIdx++;
        }
        if (!loadedNewFilters) {
            loadHandler(input, "inputFilter", inputFilterHandler);
            for (int i = 0; i < inputFilterHandler.getSlots() && i < inputFilterStrings.length; i++) {
                ItemStack stack = inputFilterHandler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    inputFilterStrings[i] = "-" + BuiltInRegistries.ITEM.getKey(stack.getItem());
                }
                inputFilterHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
            int[] savedLetters = input.getIntArray("filterLetters").orElse(new int[0]);
            System.arraycopy(savedLetters, 0, filterLetters, 0, Math.min(savedLetters.length, filterLetters.length));
        }

        forbiddenFilters.clear();
        boolean loadedForbidden = false;
        for (var entry : input.childrenListOrEmpty("forbiddenFilters")) {
            loadedForbidden = true;
            String filter = entry.getString("filter").orElse("");
            if (!filter.isEmpty() && forbiddenFilters.size() < 64) forbiddenFilters.add(filter);
        }
        if (!loadedForbidden) {
            loadHandler(input, "outputFilter", outputFilterHandler);
            for (int i = 0; i < outputFilterHandler.getSlots() && forbiddenFilters.size() < 64; i++) {
                ItemStack stack = outputFilterHandler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    forbiddenFilters.add("-" + BuiltInRegistries.ITEM.getKey(stack.getItem()));
                }
                outputFilterHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }

        loadHandler(input, "upgrades", upgradeHandler);
        loadHandler(input, "output", outputHandler);
        loadHandler(input, "input", inputHandler);


        for (int i = 0; i < markInputFilters.size(); i++) markInputFilters.set(i, ItemStack.EMPTY);
        for (net.minecraft.world.ItemStackWithSlot item : input.listOrEmpty("markInputFilters", net.minecraft.world.ItemStackWithSlot.CODEC)) {
            if (item.slot() >= 0 && item.slot() < markInputFilters.size()) markInputFilters.set(item.slot(), item.stack());
        }
        for (int i = 0; i < markOutputFilters.size(); i++) markOutputFilters.set(i, ItemStack.EMPTY);
        for (net.minecraft.world.ItemStackWithSlot item : input.listOrEmpty("markOutputFilters", net.minecraft.world.ItemStackWithSlot.CODEC)) {
            if (item.slot() >= 0 && item.slot() < markOutputFilters.size()) markOutputFilters.set(item.slot(), item.stack());
        }

        energyStorage.setEnergy(input.getIntOr("Energy", 0));
        craftingTimer = input.getIntOr("craftingTimer", 0);
        craftingPatternIndex = input.getIntOr("craftingPatternIndex", 0);
        int savedMode = input.getIntOr("redstoneMode", 0);
        redstoneMode = Math.max(0, Math.min(4, savedMode));
        previousRedstoneState = input.getBooleanOr("previousRedstoneState", false);
        pulseIgnoreTimer = input.getIntOr("pulseIgnoreTimer", 0);
        pulseCraftPending = input.getBooleanOr("pulseCraftPending", false);
        autoclearVariables = input.getBooleanOr("autoclearVariables", true);
        currentPatternIndex = input.getIntOr("currentPattern", 0);

        patterns.clear();
        for (CompoundTag patternTag : input.listOrEmpty("patterns", CompoundTag.CODEC)) {
            patterns.add(PatternData.load(patternTag));
        }
        int maxPatterns = getMaxPatterns();
        while (patterns.size() < maxPatterns) patterns.add(new PatternData());
        if (currentPatternIndex < 0 || currentPatternIndex >= patterns.size()) currentPatternIndex = 0;
        if (craftingPatternIndex < 0 || craftingPatternIndex >= patterns.size()) craftingPatternIndex = 0;
    }

    private static void loadHandler(net.minecraft.world.level.storage.ValueInput input, String key, ItemStackHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) handler.setStackInSlot(i, ItemStack.EMPTY);
        for (net.minecraft.world.ItemStackWithSlot item : input.listOrEmpty(key, net.minecraft.world.ItemStackWithSlot.CODEC)) {
            if (item.slot() >= 0 && item.slot() < handler.getSlots()) handler.setStackInSlot(item.slot(), item.stack());
        }
    }

    // ===== Energy Storage Implementation =====

    /**
     * Custom EnergyStorage that exposes setEnergy() for NBT loading.
     */
    public static class EnergyStorageImpl extends EnergyStorage {
        public EnergyStorageImpl(int capacity) {
            super(capacity, capacity, capacity); // maxReceive = maxExtract = capacity
        }

        public void setEnergy(int energy) {
            this.energy = Math.max(0, Math.min(energy, capacity));
        }

        /** Consumes exactly the given amount of RF (used when crafting). */
        public void consumeEnergy(int amount) {
            if (amount <= 0) return;
            this.energy = Math.max(0, this.energy - amount);
        }
    }

    private final class EnergyHandlerImpl
            extends net.neoforged.neoforge.transfer.transaction.SnapshotJournal<Integer>
            implements net.neoforged.neoforge.transfer.energy.EnergyHandler {
        @Override
        protected Integer createSnapshot() {
            return energyStorage.getEnergyStored();
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            energyStorage.setEnergy(snapshot);
        }

        @Override
        public long getAmountAsLong() {
            return energyStorage.getEnergyStored();
        }

        @Override
        public long getCapacityAsLong() {
            return energyStorage.getMaxEnergyStored();
        }

        @Override
        public int insert(int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
            net.neoforged.neoforge.transfer.TransferPreconditions.checkNonNegative(amount);
            if (amount == 0) return 0;
            updateSnapshots(transaction);
            return energyStorage.receiveEnergy(amount, false);
        }

        @Override
        public int extract(int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
            net.neoforged.neoforge.transfer.TransferPreconditions.checkNonNegative(amount);
            if (amount == 0) return 0;
            updateSnapshots(transaction);
            return energyStorage.extractEnergy(amount, false);
        }
    }
}
