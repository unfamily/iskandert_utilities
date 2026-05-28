package net.unfamily.iskautils.block.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.data.load.FactoryLoader;
import org.jetbrains.annotations.Nullable;

public class FactoryBlockEntity extends BlockEntity implements WorldlyContainer {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_COUNT = 2;

    private final SimpleContainer items = new SimpleContainer(SLOT_COUNT);
    private final IItemHandler itemHandler = new FactoryItemHandler();

    private int selectedColorIndex = -1;
    private int scrollOffset = 0;
    /** Detects input item / mapping changes; selection is restored from {@link #rememberedOutputBySource}. */
    private int inputRecipeStamp = 0;
    /** Last selected output row index per {@link FactoryLoader#sourcePreferenceKey}. */
    private final Map<Integer, Integer> rememberedOutputBySource = new HashMap<>();

    private final int effectiveEnergyCapacity;
    private final EnergyStorageImpl energyStorage;

    /** 0 NONE, 1 LOW, 2 HIGH, 3 PULSE (rising edge, single op), 4 DISABLED. */
    private int redstoneMode = 0;

    /** Last neighbor redstone level for PULSE edge detection (high = powered). */
    private boolean pulsePreviousRedstone = false;
    /** Set at tick start when mode==3: craft allowed only if true (rising edge). */
    private boolean pulseEdgeAllowsCraft = false;

    public FactoryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FACTORY_BE.get(), pos, state);
        this.effectiveEnergyCapacity = Math.max(0, Config.factoryEnergyBuffer);
        this.energyStorage =
                new EnergyStorageImpl(this.effectiveEnergyCapacity, this.effectiveEnergyCapacity, this.effectiveEnergyCapacity);
    }

    public SimpleContainer getItems() {
        return items;
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
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
        if (mode == 3 && level != null && !level.isClientSide) {
            pulsePreviousRedstone = level.getBestNeighborSignal(worldPosition) > 0;
        } else if (mode != 3) {
            pulsePreviousRedstone = false;
        }
    }

    /** Runs every server tick so PULSE tracks edges even when the machine has no input. */
    private void tickPulseRedstone(Level level) {
        pulseEdgeAllowsCraft = false;
        if (redstoneMode != 3 || level == null || level.isClientSide) {
            return;
        }
        boolean sig = level.getBestNeighborSignal(worldPosition) > 0;
        pulseEdgeAllowsCraft = sig && !pulsePreviousRedstone;
        pulsePreviousRedstone = sig;
    }

    private boolean redstoneAllowsCraft(Level level) {
        if (level == null || level.isClientSide) {
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
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public static void tickServer(Level level, BlockPos pos, BlockState state, FactoryBlockEntity be) {
        if (level.isClientSide) return;

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

    // --- WorldlyContainer Implementation (hoppers) ---

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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("SelectedColorIndex", selectedColorIndex);
        tag.putInt("ScrollOffset", scrollOffset);
        tag.putInt("RedstoneMode", redstoneMode);
        tag.putBoolean("PulsePreviousRedstone", pulsePreviousRedstone);
        if (effectiveEnergyCapacity > 0) {
            tag.putInt("Energy", energyStorage.getEnergyStored());
        }

        if (!rememberedOutputBySource.isEmpty()) {
            ListTag list = new ListTag();
            for (Map.Entry<Integer, Integer> e : rememberedOutputBySource.entrySet()) {
                CompoundTag row = new CompoundTag();
                row.putInt("K", e.getKey());
                row.putInt("V", e.getValue());
                list.add(row);
            }
            tag.put("FactoryOutputPrefs", list);
        } else {
            tag.remove("FactoryOutputPrefs");
        }
        tag.remove("DyeOutputPrefs");

        CompoundTag inv = new CompoundTag();
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = items.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag slotTag = new CompoundTag();
                stack.save(registries, slotTag);
                inv.put("Slot" + i, slotTag);
            }
        }
        tag.put("Items", inv);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        selectedColorIndex = tag.contains("SelectedColorIndex") ? tag.getInt("SelectedColorIndex") : -1;
        scrollOffset = Math.max(0, tag.getInt("ScrollOffset"));
        redstoneMode = tag.contains("RedstoneMode") ? tag.getInt("RedstoneMode") : 0;
        pulsePreviousRedstone = tag.contains("PulsePreviousRedstone") && tag.getBoolean("PulsePreviousRedstone");
        if (effectiveEnergyCapacity > 0) {
            energyStorage.setEnergy(tag.contains("Energy") ? tag.getInt("Energy") : 0);
        }

        rememberedOutputBySource.clear();
        loadPrefsList(tag, "FactoryOutputPrefs");
        if (rememberedOutputBySource.isEmpty()) {
            loadPrefsList(tag, "DyeOutputPrefs");
        }

        if (tag.contains("Items")) {
            CompoundTag inv = tag.getCompound("Items");
            for (int i = 0; i < SLOT_COUNT; i++) {
                String k = "Slot" + i;
                if (inv.contains(k)) {
                    items.setItem(i, ItemStack.parse(registries, inv.getCompound(k)).orElse(ItemStack.EMPTY));
                } else {
                    items.setItem(i, ItemStack.EMPTY);
                }
            }
        }
        if (level != null) {
            inputRecipeStamp = computeInputRecipeStamp(level, items.getItem(SLOT_INPUT));
        }
    }

    private void loadPrefsList(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_LIST)) {
            return;
        }
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag row = list.getCompound(i);
            int k = row.getInt("K");
            int v = row.getInt("V");
            if (v >= 0) {
                rememberedOutputBySource.put(k, v);
            }
        }
    }
}

