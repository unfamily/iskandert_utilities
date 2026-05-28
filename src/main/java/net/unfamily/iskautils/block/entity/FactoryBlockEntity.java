package net.unfamily.iskautils.block.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.unfamily.iskalib.transfer.LegacyItemHandlerResourceHandler;
import net.unfamily.iskautils.data.load.FactoryLoader;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.unfamily.iskautils.Config;
import org.jetbrains.annotations.Nullable;

public class FactoryBlockEntity extends BlockEntity implements WorldlyContainer {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_COUNT = 2;

    private final SimpleContainer items = new SimpleContainer(SLOT_COUNT);
    private final IItemHandler itemHandler = new FactoryItemHandler();
    private final ResourceHandler<ItemResource> itemTransferHandler = LegacyItemHandlerResourceHandler.wrap(itemHandler);

    private int selectedColorIndex = -1;
    private int scrollOffset = 0;
    /** Detects input item / mapping changes; selection is restored from {@link #rememberedOutputBySource}. */
    private int inputRecipeStamp = 0;
    /** Last selected output row index per {@link FactoryLoader#sourcePreferenceKey}. */
    private final Map<Integer, Integer> rememberedOutputBySource = new HashMap<>();

    private final int effectiveEnergyCapacity;
    private final EnergyStorageImpl energyStorage;
    private final net.neoforged.neoforge.transfer.energy.EnergyHandler energyHandler26 = new EnergyHandlerImpl();

    /** 0 NONE, 1 LOW, 2 HIGH, 3 PULSE (rising edge, single op), 4 DISABLED. */
    private int redstoneMode = 0;

    /** Last neighbor redstone level for PULSE edge detection (high = powered). */
    private boolean pulsePreviousRedstone = false;
    /** Set at tick start when mode==3: craft allowed only if true (rising edge). */
    private boolean pulseEdgeAllowsCraft = false;

    public FactoryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FACTORY_BE.get(), pos, state);
        this.effectiveEnergyCapacity = Math.max(0, Config.factoryEnergyBuffer);
        this.energyStorage = new EnergyStorageImpl(
                this.effectiveEnergyCapacity, this.effectiveEnergyCapacity, this.effectiveEnergyCapacity);
    }

    public SimpleContainer getItems() {
        return items;
    }

    public ResourceHandler<ItemResource> getItemTransferHandler() {
        return itemTransferHandler;
    }

    public int getSelectedColorIndex() {
        return selectedColorIndex;
    }

    public void setSelectedColorIndex(int selectedColorIndex) {
        if (selectedColorIndex < 0) {
            this.selectedColorIndex = -1;
            setChangedAndSync();
            return;
        }
        ItemStack in = items.getItem(SLOT_INPUT);
        Level level = getLevel();
        var src = level != null ? FactoryLoader.findSource(in, level) : FactoryLoader.findSource(in);
        if (src.isEmpty()) {
            this.selectedColorIndex = -1;
            setChangedAndSync();
            return;
        }
        int max = src.get().outputs().size() - 1;
        this.selectedColorIndex = Math.min(Math.max(0, selectedColorIndex), max);
        rememberedOutputBySource.put(FactoryLoader.sourcePreferenceKey(src.get()), this.selectedColorIndex);
        setChangedAndSync();
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public void setScrollOffset(int scrollOffset) {
        this.scrollOffset = Math.max(0, scrollOffset);
        setChangedAndSync();
    }

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public net.neoforged.neoforge.transfer.energy.EnergyHandler getEnergyHandler() {
        return energyHandler26;
    }

    public int getEnergyStoredDisplay() {
        return effectiveEnergyCapacity <= 0 ? 0 : energyStorage.getEnergyStored();
    }

    public int getMaxEnergyDisplay() {
        return effectiveEnergyCapacity;
    }

    public int getRedstoneMode() {
        return redstoneMode;
    }

    public void setRedstoneMode(int value) {
        applyRedstoneMode(Math.max(0, Math.min(value, 4)));
        setChangedAndSync();
    }

    public void cycleRedstoneMode() {
        applyRedstoneMode((redstoneMode + 1) % 5);
        setChangedAndSync();
    }

    public void cycleRedstoneModeBackward() {
        applyRedstoneMode((redstoneMode + 4) % 5);
        setChangedAndSync();
    }

    private void applyRedstoneMode(int mode) {
        this.redstoneMode = mode;
        if (mode == 3 && level != null && !level.isClientSide()) {
            pulsePreviousRedstone = level.getBestNeighborSignal(worldPosition) > 0;
        } else if (mode != 3) {
            pulsePreviousRedstone = false;
        }
    }

    /** Runs every server tick so PULSE tracks edges even when the machine has no input. */
    private void tickPulseRedstone(Level level) {
        pulseEdgeAllowsCraft = false;
        if (redstoneMode != 3 || level == null || level.isClientSide()) {
            return;
        }
        boolean sig = level.getBestNeighborSignal(worldPosition) > 0;
        pulseEdgeAllowsCraft = sig && !pulsePreviousRedstone;
        pulsePreviousRedstone = sig;
    }

    private boolean redstoneAllowsCraft(Level level) {
        if (level == null || level.isClientSide()) {
            return false;
        }
        if (redstoneMode == 3) {
            return pulseEdgeAllowsCraft;
        }
        int power = level.getBestNeighborSignal(worldPosition);
        boolean sig = power > 0;
        return switch (redstoneMode) {
            case 0 -> true;
            case 1 -> !sig;
            case 2 -> sig;
            case 4 -> false;
            default -> false;
        };
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public static void tickServer(Level level, BlockPos pos, BlockState state, FactoryBlockEntity be) {
        if (level.isClientSide()) return;

        be.tickPulseRedstone(level);

        ItemStack in = be.items.getItem(SLOT_INPUT);
        if (in.isEmpty()) {
            if (be.inputRecipeStamp != 0 || be.selectedColorIndex >= 0) {
                be.inputRecipeStamp = 0;
                be.selectedColorIndex = -1;
                be.setChangedAndSync();
            }
            return;
        }

        if (!be.redstoneAllowsCraft(level)) {
            return;
        }

        int stamp = computeInputRecipeStamp(level, in);
        if (stamp != be.inputRecipeStamp) {
            be.inputRecipeStamp = stamp;
            var srcOpt = FactoryLoader.findSource(in, level);
            if (srcOpt.isEmpty()) {
                be.selectedColorIndex = -1;
            } else {
                var src = srcOpt.get();
                Integer remembered = be.rememberedOutputBySource.get(FactoryLoader.sourcePreferenceKey(src));
                if (remembered != null) {
                    int max = src.outputs().size() - 1;
                    be.selectedColorIndex = Math.min(Math.max(0, remembered), max);
                } else {
                    be.selectedColorIndex = -1;
                }
            }
            be.setChangedAndSync();
        }

        var sourceOpt = FactoryLoader.findSource(in, level);
        if (sourceOpt.isEmpty()) return;
        var source = sourceOpt.get();

        if (source.outputs().isEmpty()) {
            return;
        }

        if (be.selectedColorIndex < 0) {
            return;
        }

        int selected = Math.min(Math.max(0, be.selectedColorIndex), source.outputs().size() - 1);

        var outDef = source.outputs().get(selected);
        var outStackOpt = FactoryLoader.resolveOutputStack(outDef);
        if (outStackOpt.isEmpty()) return;
        ItemStack producedPerOp = outStackOpt.get();

        int inAmt = Math.max(1, source.inputAmount());
        int outAmt = Math.max(1, producedPerOp.getCount());

        int maxOpsByIn = in.getCount() / inAmt;
        if (maxOpsByIn <= 0) return;

        ItemStack out = be.items.getItem(SLOT_OUTPUT);
        int freeOut;
        if (out.isEmpty()) {
            freeOut = Math.min(producedPerOp.getMaxStackSize(), be.items.getMaxStackSize());
        } else {
            if (!ItemStack.isSameItemSameComponents(out, producedPerOp)) return;
            freeOut = Math.min(out.getMaxStackSize(), be.items.getMaxStackSize()) - out.getCount();
        }
        if (freeOut <= 0) return;

        int maxOpsByOut = freeOut / outAmt;
        if (maxOpsByOut <= 0) return;

        int ops = Math.min(maxOpsByIn, maxOpsByOut);
        if (ops <= 0) return;

        if (be.redstoneMode == 3) {
            ops = Math.min(ops, 1);
        }

        int bufferCap = Config.factoryEnergyBuffer;
        int perOpEnergy = source.energyPerOperation();
        if (bufferCap > 0 && perOpEnergy > 0) {
            int stored = be.energyStorage.getEnergyStored();
            int affordableOps = stored / perOpEnergy;
            if (affordableOps <= 0) return;
            ops = Math.min(ops, affordableOps);
        }

        int consume = ops * inAmt;
        int produce = ops * outAmt;

        in.shrink(consume);
        if (out.isEmpty()) {
            ItemStack newOut = producedPerOp.copy();
            newOut.setCount(produce);
            be.items.setItem(SLOT_OUTPUT, newOut);
        } else {
            out.grow(produce);
        }

        if (bufferCap > 0 && perOpEnergy > 0) {
            be.energyStorage.extractEnergy(ops * perOpEnergy, false);
        }

        be.setChangedAndSync();
    }

    private static int computeInputRecipeStamp(Level level, ItemStack in) {
        if (in.isEmpty()) {
            return 0;
        }
        var itemId = BuiltInRegistries.ITEM.getId(in.getItem());
        var src = FactoryLoader.findSource(in, level);
        if (src.isEmpty()) {
            return Objects.hash(itemId, 0);
        }
        return Objects.hash(itemId, src.get().outputs().size());
    }

    private static final int[] SLOTS_FOR_UP = new int[] {SLOT_INPUT};
    private static final int[] SLOTS_FOR_DOWN = new int[] {SLOT_OUTPUT};
    private static final int[] SLOTS_FOR_SIDES = new int[] {SLOT_INPUT};

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!items.getItem(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = items.removeItem(slot, amount);
        if (!result.isEmpty()) {
            setChangedAndSync();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return items.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.setItem(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChangedAndSync();
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5)
                <= 64.0;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            items.setItem(i, ItemStack.EMPTY);
        }
        setChangedAndSync();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return SLOTS_FOR_DOWN;
        }
        return side == Direction.UP ? SLOTS_FOR_UP : SLOTS_FOR_SIDES;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_INPUT;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return slot == SLOT_INPUT;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot == SLOT_OUTPUT;
    }

    private static final class EnergyStorageImpl extends EnergyStorage {
        EnergyStorageImpl(int capacity, int maxReceive, int maxExtract) {
            super(capacity, maxReceive, maxExtract);
        }

        void setEnergy(int energy) {
            this.energy = Math.max(0, Math.min(capacity, energy));
        }
    }

    private final class EnergyHandlerImpl extends net.neoforged.neoforge.transfer.transaction.SnapshotJournal<Integer>
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
            return effectiveEnergyCapacity;
        }

        @Override
        public int insert(int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
            net.neoforged.neoforge.transfer.TransferPreconditions.checkNonNegative(amount);
            if (amount == 0 || effectiveEnergyCapacity <= 0) return 0;
            updateSnapshots(transaction);
            return energyStorage.receiveEnergy(amount, false);
        }

        @Override
        public int extract(int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
            net.neoforged.neoforge.transfer.TransferPreconditions.checkNonNegative(amount);
            if (amount == 0 || effectiveEnergyCapacity <= 0) return 0;
            updateSnapshots(transaction);
            return energyStorage.extractEnergy(amount, false);
        }
    }

    private final class FactoryItemHandler implements IItemHandlerModifiable {
        @Override
        public int getSlots() {
            return SLOT_COUNT;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getItem(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != SLOT_INPUT || stack.isEmpty()) {
                return stack;
            }

            ItemStack existing = items.getItem(SLOT_INPUT);
            int limit = Math.min(stack.getMaxStackSize(), getSlotLimit(SLOT_INPUT));

            if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack)) {
                return stack;
            }

            int free = limit - existing.getCount();
            if (free <= 0) {
                return stack;
            }

            int toInsert = Math.min(free, stack.getCount());
            if (!simulate) {
                if (existing.isEmpty()) {
                    ItemStack copy = stack.copy();
                    copy.setCount(toInsert);
                    items.setItem(SLOT_INPUT, copy);
                } else {
                    existing.grow(toInsert);
                    items.setItem(SLOT_INPUT, existing);
                }
                setChangedAndSync();
            }

            ItemStack remainder = stack.copy();
            remainder.shrink(toInsert);
            return remainder;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != SLOT_OUTPUT || amount <= 0) {
                return ItemStack.EMPTY;
            }

            ItemStack existing = items.getItem(SLOT_OUTPUT);
            if (existing.isEmpty()) {
                return ItemStack.EMPTY;
            }

            int toExtract = Math.min(amount, existing.getCount());
            ItemStack extracted = existing.copy();
            extracted.setCount(toExtract);

            if (!simulate) {
                existing.shrink(toExtract);
                items.setItem(SLOT_OUTPUT, existing.isEmpty() ? ItemStack.EMPTY : existing);
                setChangedAndSync();
            }

            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getMaxStackSize();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_INPUT;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            items.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            setChangedAndSync();
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("SelectedColorIndex", selectedColorIndex);
        output.putInt("ScrollOffset", scrollOffset);
        output.putInt("RedstoneMode", redstoneMode);
        output.putBoolean("PulsePreviousRedstone", pulsePreviousRedstone);
        if (effectiveEnergyCapacity > 0) {
            output.putInt("Energy", energyStorage.getEnergyStored());
        }

        if (!rememberedOutputBySource.isEmpty()) {
            ValueOutput prefsTag = output.child("FactoryOutputPrefs");
            ValueOutput.ValueOutputList entries = prefsTag.childrenList("entries");
            for (Map.Entry<Integer, Integer> e : rememberedOutputBySource.entrySet()) {
                ValueOutput row = entries.addChild();
                row.putInt("K", e.getKey());
                row.putInt("V", e.getValue());
            }
        } else {
            output.discard("FactoryOutputPrefs");
        }

        ValueOutput.TypedOutputList<ItemStackWithSlot> itemsList = output.list("Items", ItemStackWithSlot.CODEC);
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = items.getItem(i);
            if (!stack.isEmpty()) {
                itemsList.add(new ItemStackWithSlot(i, stack));
            }
        }
        if (itemsList.isEmpty()) output.discard("Items");
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        selectedColorIndex = input.getIntOr("SelectedColorIndex", -1);
        scrollOffset = Math.max(0, input.getIntOr("ScrollOffset", 0));
        redstoneMode = input.getIntOr("RedstoneMode", 0);
        pulsePreviousRedstone = input.getBooleanOr("PulsePreviousRedstone", false);
        if (effectiveEnergyCapacity > 0) {
            energyStorage.setEnergy(input.getIntOr("Energy", 0));
        }

        rememberedOutputBySource.clear();
        loadPrefsFrom(input.child("FactoryOutputPrefs"));
        if (rememberedOutputBySource.isEmpty()) {
            loadPrefsFrom(input.child("DyeOutputPrefs"));
        }

        for (int i = 0; i < SLOT_COUNT; i++) {
            items.setItem(i, ItemStack.EMPTY);
        }
        for (ItemStackWithSlot item : input.listOrEmpty("Items", ItemStackWithSlot.CODEC)) {
            int slot = item.slot();
            if (slot >= 0 && slot < SLOT_COUNT) {
                items.setItem(slot, item.stack());
            }
        }
        if (level != null) {
            inputRecipeStamp = computeInputRecipeStamp(level, items.getItem(SLOT_INPUT));
        }
    }

    private void loadPrefsFrom(Optional<ValueInput> prefsOpt) {
        if (prefsOpt.isEmpty()) {
            return;
        }
        for (ValueInput row : prefsOpt.get().childrenListOrEmpty("entries")) {
            int k = row.getIntOr("K", 0);
            int v = row.getIntOr("V", -1);
            if (v >= 0) {
                rememberedOutputBySource.put(k, v);
            }
        }
    }
}
