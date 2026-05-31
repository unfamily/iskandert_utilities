package net.unfamily.iskautils.block.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletCraftLogic;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRecipeEntry;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRecipeLoader;
import net.unfamily.iskautils.item.component.AncientTabletContents;
import net.unfamily.iskautils.util.AncientTableFuel;
import net.unfamily.iskautils.util.BlockCraftOwner;
import org.jetbrains.annotations.Nullable;

public class AncientTableBlockEntity extends BlockEntity implements WorldlyContainer, BlockCraftOwner.BlockCraftOwnerHolder {
    public static final int VISIBLE_GRID_SLOTS = 9;

    private final int inputCount;
    private final int outputCount;
    public final int fuelSlotIndex;
    public final int outputStartIndex;
    private final int totalSlots;

    private final net.minecraft.world.SimpleContainer items;
    private final IItemHandler itemHandler = new AncientTableItemHandler();

    private int inputScrollOffset;
    private int outputScrollOffset;
    private int redstoneMode;
    private int craftProgress;
    private int storedFuel;
    private boolean pulsePreviousRedstone;
    private boolean pulseEdgeAllowsCraft;
    @Nullable
    private UUID ownerPlayerUuid;

    public AncientTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANCIENT_TABLE_BE.get(), pos, state);
        this.inputCount = Math.max(9, Config.ancientTableIoSlots);
        this.outputCount = this.inputCount;
        this.fuelSlotIndex = this.inputCount;
        this.outputStartIndex = this.inputCount + 1;
        this.totalSlots = this.inputCount + 1 + this.outputCount;
        this.items = new net.minecraft.world.SimpleContainer(this.totalSlots);
    }

    public net.minecraft.world.SimpleContainer getItems() {
        return items;
    }

    /** Drops all slot contents and recoverable internal fuel when the block is broken. */
    public void drops() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockCraftOwner.clear(this);
        net.unfamily.iskautils.util.MachineBreakDrops.dropContainerContents(level, worldPosition, items);
        int fuel = storedFuel;
        storedFuel = 0;
        net.unfamily.iskautils.util.MachineBreakDrops.dropStoredEntropyCharge(level, worldPosition, fuel);
        setChanged();
    }

    @Override
    @Nullable
    public UUID getOwnerPlayerUuid() {
        return ownerPlayerUuid;
    }

    @Override
    public void setOwnerPlayerUuid(@Nullable UUID uuid) {
        this.ownerPlayerUuid = uuid;
    }

    public void claimOwner(ServerPlayer player) {
        BlockCraftOwner.claimOnFirstOpen(this, player, ownerPlayerUuid);
    }

    @Nullable
    public ServerPlayer resolveOwnerPlayer() {
        return BlockCraftOwner.resolve(level, ownerPlayerUuid);
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public int getInputCount() {
        return inputCount;
    }

    public int getInputScrollOffset() {
        return inputScrollOffset;
    }

    public void setInputScrollOffset(int offset) {
        int max = Math.max(0, inputCount - VISIBLE_GRID_SLOTS);
        this.inputScrollOffset = Mth.clamp(offset, 0, max);
        setChangedAndSync();
    }

    public int getOutputScrollOffset() {
        return outputScrollOffset;
    }

    public void setOutputScrollOffset(int offset) {
        int max = Math.max(0, outputCount - VISIBLE_GRID_SLOTS);
        this.outputScrollOffset = Mth.clamp(offset, 0, max);
        setChangedAndSync();
    }

    public int getRedstoneMode() {
        return redstoneMode;
    }

    public void setRedstoneMode(int value) {
        applyRedstoneMode(Mth.clamp(value, 0, 4));
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

    public int getCraftProgress() {
        return craftProgress;
    }

    public int getCraftProgressMax() {
        return Config.ancientTableCraftTicks;
    }

    public int getStoredFuel() {
        return storedFuel;
    }

    public int getFuelCapacity() {
        return AncientTableFuel.maxStored();
    }

    /** Synced to client for fuel % label (internal buffer, not the fuel slot stack). */
    public int getFuelCharges() {
        return storedFuel;
    }

    public int getFuelChargesMax() {
        return getFuelCapacity();
    }

    /** Comparator reads only the physical fuel slot stack, not internal stored fuel. */
    public int getAnalogOutputSignal() {
        return AncientTableFuel.comparatorFromFuelSlot(items.getItem(fuelSlotIndex));
    }

    private void tryAbsorbFuelSlot() {
        boolean changed = false;
        while (true) {
            ItemStack fuel = items.getItem(fuelSlotIndex);
            if (!AncientTableFuel.isEntropyFuel(fuel)) {
                break;
            }
            if (!AncientTableFuel.canAbsorbOneMore(storedFuel)) {
                break;
            }
            fuel.shrink(1);
            storedFuel += AncientTableFuel.fuelPerDrop();
            items.setItem(fuelSlotIndex, fuel.isEmpty() ? ItemStack.EMPTY : fuel);
            changed = true;
        }
        if (changed) {
            setChangedAndSync();
        }
    }

    private void tickPulseRedstone(Level level) {
        pulseEdgeAllowsCraft = false;
        if (redstoneMode != 3 || level.isClientSide) {
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

    public static void tickServer(Level level, BlockPos pos, BlockState state, AncientTableBlockEntity be) {
        if (level.isClientSide) {
            return;
        }
        be.tickPulseRedstone(level);
        be.tryAbsorbFuelSlot();
        if (!be.redstoneAllowsCraft(level)) {
            if (be.craftProgress > 0) {
                be.craftProgress = 0;
                be.setChangedAndSync();
            }
            return;
        }

        if (be.craftProgress > 0) {
            be.craftProgress++;
            if (be.craftProgress >= Config.ancientTableCraftTicks) {
                be.finishCraft();
                be.craftProgress = 0;
            }
            be.setChangedAndSync();
            return;
        }

        Optional<AncientTabletCraftLogic.CraftSuccess> match = be.findCraftMatch();
        if (match.isEmpty()) {
            return;
        }
        AncientTabletRecipeEntry.ResolvedCraft resolved = match.get().resolved();
        if (be.storedFuel < resolved.fuelCost()) {
            return;
        }
        if (!be.canFitOutputs(resolved)) {
            return;
        }
        be.craftProgress = 1;
        be.setChangedAndSync();
    }

    private Optional<AncientTabletCraftLogic.CraftSuccess> findCraftMatch() {
        // Inputs + fuel slot (fuel is also a recipe ingredient in some entries).
        List<ItemStack> inputList = new ArrayList<>(inputCount + 1);
        for (int i = 0; i < inputCount; i++) {
            inputList.add(items.getItem(i).copy());
        }
        inputList.add(items.getItem(fuelSlotIndex).copy());
        List<AncientTabletContents.SlotView> views = AncientTabletCraftLogic.expandContainerSlots(inputList);
        if (views.isEmpty()) {
            return Optional.empty();
        }
        return AncientTabletCraftLogic.tryCraftUnordered(
                views, AncientTabletRecipeLoader.getEntries(), resolveOwnerPlayer());
    }

    private boolean canFitOutputs(AncientTabletRecipeEntry.ResolvedCraft resolved) {
        List<ItemStack> outputs = AncientTabletCraftLogic.outputStacks(resolved);
        for (ItemStack out : outputs) {
            if (out.isEmpty()) {
                continue;
            }
            if (remainingSpaceFor(out) < out.getCount()) {
                return false;
            }
        }
        return true;
    }

    private int remainingSpaceFor(ItemStack template) {
        int space = 0;
        for (int i = outputStartIndex; i < totalSlots; i++) {
            ItemStack existing = items.getItem(i);
            if (existing.isEmpty()) {
                space += template.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(existing, template)) {
                space += template.getMaxStackSize() - existing.getCount();
            }
        }
        return space;
    }

    private void finishCraft() {
        Optional<AncientTabletCraftLogic.CraftSuccess> match = findCraftMatch();
        if (match.isEmpty()) {
            return;
        }
        AncientTabletRecipeEntry.ResolvedCraft resolved = match.get().resolved();
        if (storedFuel < resolved.fuelCost()) {
            return;
        }

        List<ItemStack> inputList = new ArrayList<>(inputCount + 1);
        for (int i = 0; i < inputCount; i++) {
            inputList.add(items.getItem(i).copy());
        }
        inputList.add(items.getItem(fuelSlotIndex).copy());
        AncientTabletCraftLogic.consumeContainerAtIndices(inputList, match.get().consumedSlotIndices());
        for (int i = 0; i < inputCount; i++) {
            items.setItem(i, inputList.get(i));
        }
        items.setItem(fuelSlotIndex, inputList.get(inputCount));

        storedFuel -= resolved.fuelCost();

        for (ItemStack out : AncientTabletCraftLogic.outputStacks(resolved)) {
            insertOutput(out);
        }
        setChangedAndSync();
    }

    private void insertOutput(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack remaining = stack.copy();
        for (int i = outputStartIndex; i < totalSlots && !remaining.isEmpty(); i++) {
            ItemStack existing = items.getItem(i);
            if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, remaining)) {
                continue;
            }
            int limit = Math.min(remaining.getMaxStackSize(), getMaxStackSize());
            if (existing.isEmpty()) {
                int move = Math.min(limit, remaining.getCount());
                ItemStack placed = remaining.copy();
                placed.setCount(move);
                items.setItem(i, placed);
                remaining.shrink(move);
            } else {
                int space = limit - existing.getCount();
                if (space > 0) {
                    int move = Math.min(space, remaining.getCount());
                    existing.grow(move);
                    items.setItem(i, existing);
                    remaining.shrink(move);
                }
            }
        }
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    // --- WorldlyContainer ---

    @Override
    public int getContainerSize() {
        return totalSlots;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < totalSlots; i++) {
            if (!items.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return items.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return items.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.setItem(slot, stack);
        if (slot == fuelSlotIndex) {
            tryAbsorbFuelSlot();
        } else {
            setChangedAndSync();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5)
                <= 64.0;
    }

    @Override
    public void clearContent() {
        items.clearContent();
        setChangedAndSync();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        int[] arr = new int[totalSlots];
        for (int i = 0; i < totalSlots; i++) {
            arr[i] = i;
        }
        return arr;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == fuelSlotIndex) {
            return AncientTableFuel.isEntropyFuel(stack);
        }
        if (slot >= 0 && slot < inputCount) {
            return !AncientTableFuel.isEntropyFuel(stack);
        }
        return false;
    }

    private boolean canAcceptEntropyInFuelSlot() {
        if (AncientTableFuel.canAbsorbOneMore(storedFuel)) {
            return true;
        }
        ItemStack fuel = items.getItem(fuelSlotIndex);
        if (fuel.isEmpty()) {
            return false;
        }
        return AncientTableFuel.isEntropyFuel(fuel) && fuel.getCount() < fuel.getMaxStackSize();
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return slot >= 0 && slot <= fuelSlotIndex && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot >= outputStartIndex && slot < totalSlots;
    }

    // --- Insert routing ---

    public ItemStack insertWithFuelPriority(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return stack;
        }
        ItemStack remaining = stack.copy();
        if (AncientTableFuel.isEntropyFuel(remaining)) {
            if (canAcceptEntropyInFuelSlot()) {
                remaining = insertIntoSlot(fuelSlotIndex, remaining, simulate);
                if (!simulate) {
                    tryAbsorbFuelSlot();
                }
                if (remaining.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }
        for (int i = 0; i < inputCount && !remaining.isEmpty(); i++) {
            if (AncientTableFuel.isEntropyFuel(remaining)) {
                continue;
            }
            remaining = insertIntoSlot(i, remaining, simulate);
        }
        return remaining;
    }

    private ItemStack insertIntoSlot(int slot, ItemStack stack, boolean simulate) {
        if (slot == fuelSlotIndex && AncientTableFuel.isEntropyFuel(stack) && !canAcceptEntropyInFuelSlot()) {
            return stack;
        }
        if (!canPlaceItem(slot, stack)) {
            return stack;
        }
        ItemStack existing = items.getItem(slot);
        int limit = Math.min(stack.getMaxStackSize(), getMaxStackSize());
        if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack)) {
            return stack;
        }
        int free = limit - (existing.isEmpty() ? 0 : existing.getCount());
        if (free <= 0) {
            return stack;
        }
        int toInsert = Math.min(free, stack.getCount());
        if (!simulate) {
            if (existing.isEmpty()) {
                ItemStack copy = stack.copy();
                copy.setCount(toInsert);
                items.setItem(slot, copy);
            } else {
                existing.grow(toInsert);
                items.setItem(slot, existing);
            }
            if (slot == fuelSlotIndex) {
                tryAbsorbFuelSlot();
            } else {
                setChangedAndSync();
            }
        }
        ItemStack rem = stack.copy();
        rem.shrink(toInsert);
        return rem;
    }

    private final class AncientTableItemHandler implements IItemHandlerModifiable {
        @Override
        public int getSlots() {
            return totalSlots;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getItem(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) {
                return stack;
            }
            if (slot >= 0 && slot <= fuelSlotIndex) {
                ItemStack rem = insertWithFuelPriority(stack, simulate);
                return rem.isEmpty() ? ItemStack.EMPTY : rem;
            }
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0 || slot < 0 || slot >= totalSlots) {
                return ItemStack.EMPTY;
            }
            if (slot < outputStartIndex) {
                return ItemStack.EMPTY;
            }
            ItemStack existing = items.getItem(slot);
            if (existing.isEmpty()) {
                return ItemStack.EMPTY;
            }
            int toExtract = Math.min(amount, existing.getCount());
            ItemStack extracted = existing.copy();
            extracted.setCount(toExtract);
            if (!simulate) {
                existing.shrink(toExtract);
                items.setItem(slot, existing.isEmpty() ? ItemStack.EMPTY : existing);
                setChangedAndSync();
            }
            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            return getMaxStackSize();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return canPlaceItem(slot, stack);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            items.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            if (slot == fuelSlotIndex) {
                tryAbsorbFuelSlot();
            } else {
                setChangedAndSync();
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("InputScroll", inputScrollOffset);
        tag.putInt("OutputScroll", outputScrollOffset);
        tag.putInt("RedstoneMode", redstoneMode);
        tag.putInt("CraftProgress", craftProgress);
        tag.putInt("StoredFuel", storedFuel);
        tag.putBoolean("PulsePreviousRedstone", pulsePreviousRedstone);
        BlockCraftOwner.write(tag, ownerPlayerUuid);
        // Save storage using ListTag format (like ItemStackHandler does internally).
        ListTag inv = new ListTag();
        for (int i = 0; i < totalSlots; i++) {
            ItemStack stack = items.getItem(i);
            if (!stack.isEmpty()) {
                net.minecraft.nbt.Tag saved = stack.saveOptional(registries);
                // saveOptional should yield a CompoundTag for real stacks. Skip anything empty/unexpected.
                if (!(saved instanceof CompoundTag savedTag) || savedTag.isEmpty()) {
                    continue;
                }
                CompoundTag entry = new CompoundTag();
                entry.putInt("Slot", i);
                entry.put("Item", savedTag);
                inv.add(entry);
            }
        }
        if (!inv.isEmpty()) {
            tag.put("Items", inv);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inputScrollOffset = Math.max(0, tag.getInt("InputScroll"));
        outputScrollOffset = Math.max(0, tag.getInt("OutputScroll"));
        redstoneMode = tag.contains("RedstoneMode") ? tag.getInt("RedstoneMode") : 0;
        craftProgress = tag.getInt("CraftProgress");
        storedFuel = Mth.clamp(tag.getInt("StoredFuel"), 0, AncientTableFuel.maxStored());
        pulsePreviousRedstone = tag.getBoolean("PulsePreviousRedstone");
        ownerPlayerUuid = BlockCraftOwner.read(tag);
        for (int i = 0; i < totalSlots; i++) {
            items.setItem(i, ItemStack.EMPTY);
        }
        if (tag.contains("Items", net.minecraft.nbt.Tag.TAG_LIST)) {
            ListTag inv = tag.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int idx = 0; idx < inv.size(); idx++) {
                CompoundTag entry = inv.getCompound(idx);
                int slot = entry.getInt("Slot");
                if (slot < 0 || slot >= totalSlots) {
                    continue;
                }
                if (entry.contains("Item", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                    ItemStack parsed = ItemStack.parseOptional(registries, entry.getCompound("Item"));
                    if (!parsed.isEmpty()) {
                        items.setItem(slot, parsed);
                    }
                } else if (entry.contains("Stack", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                    // Transitional support for the earlier list format (Slot + Stack).
                    items.setItem(slot, ItemStack.parse(registries, entry.getCompound("Stack")).orElse(ItemStack.EMPTY));
                }
            }
        } else if (tag.contains("Items", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            // Backward compatible with the older "SlotX" compound format.
            CompoundTag inv = tag.getCompound("Items");
            for (int i = 0; i < totalSlots; i++) {
                String k = "Slot" + i;
                if (inv.contains(k)) {
                    items.setItem(i, ItemStack.parse(registries, inv.getCompound(k)).orElse(ItemStack.EMPTY));
                }
            }
        }
    }
}
