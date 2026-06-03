package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.unfamily.iskalib.transfer.LegacyItemHandlerResourceHandler;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.util.AncientTableFuel;
import net.unfamily.iskautils.util.EntropyCharges;

import java.util.ArrayList;
import java.util.List;

/**
 * BlockEntity for the Temporal Overclocker
 * Accelerates ticks of linked blocks via chipset
 */
public class TemporalOverclockerBlockEntity extends BlockEntity {
    private static final String ENERGY_TAG = "Energy";
    private static final String LINKED_BLOCKS_TAG = "LinkedBlocks";
    private static final String UPGRADE_STACK_TAG = "UpgradeStack";
    private static final String FUEL_STACK_TAG = "FuelStack";
    private static final String STORED_ENTROPY_TAG = "StoredEntropy";

    public static final int UPGRADE_SLOT_INDEX = 0;
    public static final int FUEL_SLOT_INDEX = 1;
    private static final int MACHINE_SLOT_COUNT = 2;
    
    // List of linked block positions
    private final List<BlockPos> linkedBlocks = new ArrayList<>();
    
    private final SimpleContainer machineItems = new SimpleContainer(MACHINE_SLOT_COUNT);
    private int storedEntropy;
    
    // Energy storage
    private final EnergyStorageImpl energyStorage;
    private final net.neoforged.neoforge.transfer.energy.EnergyHandler energyHandler26;
    private final OverclockerItemHandler itemHandler = new OverclockerItemHandler();
    private final ResourceHandler<ItemResource> itemTransferHandler =
            LegacyItemHandlerResourceHandler.wrap(itemHandler);
    
    // Counter to manage acceleration (accelerates every N ticks)
    private int tickCounter = 0;
    private static final int ACCELERATION_INTERVAL = 1; // Accelerate every tick
    
    // Redstone mode (0=NONE, 1=LOW, 2=HIGH, 4=DISABLED). Mode 3 (PULSE) is not used; migrated to 4 on load.
    private int redstoneMode = 0;
    
    // Acceleration factor (modifiable from GUI, default from config)
    private int accelerationFactor = Config.temporalOverclockerAccelerationFactor;
    
    // Persistent mode (if true, blocks are not removed when broken, only when out of range)
    private boolean persistentMode = false;
    
    public TemporalOverclockerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TEMPORAL_OVERCLOCKER_BE.get(), pos, state);
        int maxEnergy = Config.temporalOverclockerEnergyPerAcceleration <= 0 ? 0 : Config.temporalOverclockerEnergyBuffer;
        this.energyStorage = new EnergyStorageImpl(maxEnergy);
        this.energyHandler26 = new EnergyHandlerImpl();
        // accelerationFactor is already initialized from the field with the value from config
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
    
    /**
     * Adds a block to the list of linked blocks
     */
    public boolean addLinkedBlock(BlockPos pos) {
        if (linkedBlocks.size() >= Config.temporalOverclockerMaxLinks) {
            return false; // Limit reached
        }
        
        // Check distance (maximum range)
        double distance = Math.sqrt(this.worldPosition.distSqr(pos));
        if (distance > Config.temporalOverclockerLinkRange) {
            return false; // Too far
        }
        
        if (!linkedBlocks.contains(pos)) {
            linkedBlocks.add(pos);
            setChanged();
            // Force sync to client
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
            return true;
        }
        return false; // Already present
    }
    
    /**
     * Removes a block from the list of linked blocks
     */
    public boolean removeLinkedBlock(BlockPos pos) {
        boolean removed = linkedBlocks.remove(pos);
        if (removed) {
            setChanged();
            // Force sync to client
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
        return removed;
    }
    
    /**
     * Gets the list of linked blocks
     */
    public List<BlockPos> getLinkedBlocks() {
        return new ArrayList<>(linkedBlocks);
    }
    
    /**
     * Clears all links
     */
    public void clearLinkedBlocks() {
        linkedBlocks.clear();
        setChanged();
    }
    
    /**
     * Checks if a block is linked
     */
    public boolean isLinked(BlockPos pos) {
        return linkedBlocks.contains(pos);
    }
    
    /**
     * Checks if a block can be linked (checks distance and limit)
     * @return null if it can be linked, otherwise an error message
     */
    public String canLinkBlock(BlockPos pos) {
        if (linkedBlocks.size() >= Config.temporalOverclockerMaxLinks) {
            return "max_links";
        }
        
        // Check distance (maximum range)
        double distance = Math.sqrt(this.worldPosition.distSqr(pos));
        if (distance > Config.temporalOverclockerLinkRange) {
            return "too_far";
        }
        
        if (linkedBlocks.contains(pos)) {
            return "already_linked";
        }
        
        return null; // Can be linked
    }

    /** Server-side: whether linked blocks may be accelerated this tick (no PULSE mode). */
    private boolean redstoneAllowsAcceleration(Level level) {
        if (level == null || level.isClientSide()) {
            return false;
        }
        int mode = redstoneMode;
        if (mode == 3) {
            mode = 4;
        }
        if (mode == 4) {
            return false;
        }
        boolean sig = level.getBestNeighborSignal(worldPosition) > 0;
        return switch (mode) {
            case 0 -> true;
            case 1 -> !sig;
            case 2 -> sig;
            default -> false;
        };
    }
    
    /**
     * Main tick method
     */
    public static void tick(Level level, BlockPos pos, BlockState state, TemporalOverclockerBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }
        
        blockEntity.tickCounter++;
        blockEntity.tryAbsorbFuelSlot();
        
        // Every ACCELERATION_INTERVAL ticks, check for broken blocks and accelerate linked blocks
        if (blockEntity.tickCounter >= ACCELERATION_INTERVAL) {
            blockEntity.tickCounter = 0;
            
            // Check and remove broken, invalid, or too distant linked blocks
            int sizeBefore = blockEntity.linkedBlocks.size();
            blockEntity.linkedBlocks.removeIf(linkedPos -> {
                // Check if chunk is loaded
                if (!level.isLoaded(linkedPos)) {
                    return false; // Keep if chunk is not loaded (might be unloaded temporarily)
                }
                
                // Check distance (if overclocker was moved, blocks might be too far)
                // ALWAYS remove if out of range, regardless of persistent mode
                double distance = Math.sqrt(blockEntity.worldPosition.distSqr(linkedPos));
                if (distance > Config.temporalOverclockerLinkRange) {
                    return true; // Remove if too far
                }
                
                // Check if block still exists and is not air
                // If persistent mode is ON, don't remove air blocks (only remove if out of range)
                BlockState linkedState = level.getBlockState(linkedPos);
                if (linkedState.isAir() && !blockEntity.persistentMode) {
                    return true; // Remove if block is air (broken) and persistent mode is OFF
                }
                
                // Keep blocks even if they don't have a BlockEntity (we can accelerate block ticks).
                // Only remove if it's air or too far. This allows temporal accelerators to remain bound to plain blocks
                // that temporarily lose their BlockEntity (e.g., when converted to a normal block).
                
                return false; // Keep this block
            });
            
            // If we removed any blocks, mark as changed and sync to client
            if (blockEntity.linkedBlocks.size() < sizeBefore) {
                blockEntity.setChanged();
                // Force sync to client
                if (level != null && !level.isClientSide()) {
                    level.sendBlockUpdated(pos, state, state, 3);
                }
            }
            
            // Accelerate each linked block
            if (!blockEntity.linkedBlocks.isEmpty() && level instanceof ServerLevel serverLevel) {
                if (!blockEntity.redstoneAllowsAcceleration(level)) {
                    return;
                }
                int accelerationFactor = blockEntity.getRuntimeAccelerationFactor();
                
                // Count only non-air blocks for energy calculation
                int validBlockCount = 0;
                for (BlockPos linkedPos : blockEntity.linkedBlocks) {
                    BlockState blockState = level.getBlockState(linkedPos);
                    if (!blockState.isAir()) {
                        validBlockCount++;
                    }
                }
                
                // Calculate energy consumption: energyPerAcceleration * accelerationFactor * numValidBlocks
                int energyPerBlock = Config.temporalOverclockerEnergyPerAcceleration * accelerationFactor;
                int totalEnergyNeeded = energyPerBlock * validBlockCount;
                
                // Check if we have enough energy
                if (totalEnergyNeeded > 0 && blockEntity.energyStorage.getEnergyStored() < totalEnergyNeeded) {
                    return; // Not enough energy for all blocks
                }
                
                // Accelerate valid blocks (skip air blocks)
                for (BlockPos linkedPos : blockEntity.linkedBlocks) {
                    // Read state first
                    BlockState linkedState = level.getBlockState(linkedPos);
                    
                    // Skip air blocks (they exist in persistent mode but shouldn't be accelerated)
                    if (linkedState.isAir()) {
                        continue;
                    }
                    
                    BlockEntity linkedBE = level.getBlockEntity(linkedPos);
                    
                    // If there's a BlockEntity, try to accelerate its ticker
                    if (linkedBE != null) {
                    BlockEntityType<?> beType = linkedBE.getType();
                    Block block = linkedState.getBlock();
                    if (block instanceof net.minecraft.world.level.block.EntityBlock entityBlock) {
                        BlockEntityTicker<?> ticker = entityBlock.getTicker(level, linkedState, beType);
                        if (ticker != null) {
                            // Call the ticker multiple times to accelerate
                            @SuppressWarnings("unchecked")
                            BlockEntityTicker<BlockEntity> safeTicker = (BlockEntityTicker<BlockEntity>) ticker;
                            for (int i = 0; i < accelerationFactor - 1; i++) {
                                try {
                                    safeTicker.tick(level, linkedPos, linkedState, linkedBE);
                                } catch (Exception e) {
                                    // If there's an error, stop acceleration for this block
                                    break;
                                }
                                }
                            }
                        }
                    }
                    
                    // Trigger random ticks directly (covers crops and other random-ticking blocks).
                    // Calling randomTick on a block that doesn't implement it is a no-op.
                    if (serverLevel != null) {
                        for (int i = 0; i < accelerationFactor - 1; i++) {
                            try {
                                linkedState.randomTick(serverLevel, linkedPos, serverLevel.getRandom());
                            } catch (Exception ignored) {
                                // ignore individual random tick failures
                            }
                        }
                    }
                    // Always attempt to accelerate block ticks as well (covers plain blocks without BE).
                    for (int i = 0; i < accelerationFactor - 1; i++) {
                        level.scheduleTick(linkedPos, linkedState.getBlock(), 0);
                    }
                }
                
                // Consume energy
                if (totalEnergyNeeded > 0) {
                    blockEntity.energyStorage.extractEnergy(totalEnergyNeeded, false);
                    blockEntity.setChanged();
                }
                if (blockEntity.hasEntropicClockUpgrade()
                        && Config.entropicClockEntropyPerTick > 0
                        && blockEntity.hasExtendedAccelerationFuel()
                        && blockEntity.accelerationFactor > Config.temporalOverclockerAccelerationFactorMax) {
                    blockEntity.storedEntropy = EntropyCharges.consume(
                            blockEntity.storedEntropy, Config.entropicClockEntropyPerTick);
                    blockEntity.setChanged();
                }
            }
        }
    }

    public SimpleContainer getMachineItems() {
        return machineItems;
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public ResourceHandler<ItemResource> getItemTransferHandler() {
        return itemTransferHandler;
    }

    /** Drops upgrade/fuel slots and recoverable internal entropy when the block is broken. */
    public void drops() {
        if (level == null || level.isClientSide()) {
            return;
        }
        net.unfamily.iskautils.util.MachineBreakDrops.dropContainerContents(level, worldPosition, machineItems);
        int entropy = storedEntropy;
        storedEntropy = 0;
        net.unfamily.iskautils.util.MachineBreakDrops.dropStoredEntropyCharge(level, worldPosition, entropy);
        setChanged();
    }

    public ItemStack getUpgradeStack() {
        return machineItems.getItem(UPGRADE_SLOT_INDEX).copy();
    }

    public void setUpgradeStack(ItemStack stack) {
        if (stack.isEmpty()) {
            machineItems.setItem(UPGRADE_SLOT_INDEX, ItemStack.EMPTY);
        } else if (stack.is(ModItems.ENTROPIC_CLOCK.get())) {
            machineItems.setItem(UPGRADE_SLOT_INDEX, stack.copyWithCount(1));
        } else {
            return;
        }
        onUpgradeSlotChanged();
    }

    public void onMachineSlotChanged(int slot) {
        if (slot == UPGRADE_SLOT_INDEX) {
            onUpgradeSlotChanged();
        } else if (slot == FUEL_SLOT_INDEX) {
            tryAbsorbFuelSlot();
        }
    }

    private void onUpgradeSlotChanged() {
        ItemStack inSlot = machineItems.getItem(UPGRADE_SLOT_INDEX);
        if (!inSlot.isEmpty()) {
            if (!inSlot.is(ModItems.ENTROPIC_CLOCK.get())) {
                machineItems.setItem(UPGRADE_SLOT_INDEX, ItemStack.EMPTY);
            } else if (inSlot.getCount() > 1) {
                machineItems.setItem(UPGRADE_SLOT_INDEX, inSlot.copyWithCount(1));
            }
        }
        clampAccelerationFactor();
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public int getStoredEntropy() {
        return storedEntropy;
    }

    public void setStoredEntropy(int value) {
        this.storedEntropy = Mth.clamp(value, 0, getMaxStoredEntropy());
        setChanged();
    }

    public boolean hasEntropicClockUpgrade() {
        ItemStack upgrade = machineItems.getItem(UPGRADE_SLOT_INDEX);
        return !upgrade.isEmpty() && upgrade.is(ModItems.ENTROPIC_CLOCK.get());
    }

    /** True when configured above normal max but entropy fuel is unavailable (runs at normal max). */
    public boolean hasExtendedAccelerationFuel() {
        if (!hasEntropicClockUpgrade()) {
            return false;
        }
        if (Config.entropicClockEntropyPerTick <= 0) {
            return true;
        }
        if (storedEntropy >= Config.entropicClockEntropyPerTick) {
            return true;
        }
        return AncientTableFuel.isEntropyFuel(machineItems.getItem(FUEL_SLOT_INDEX));
    }

    public boolean isAccelerationDepowered() {
        return accelerationFactor > Config.temporalOverclockerAccelerationFactorMax
                && !hasExtendedAccelerationFuel();
    }

    public int getRuntimeAccelerationFactor() {
        int baseMax = Config.temporalOverclockerAccelerationFactorMax;
        if (accelerationFactor <= baseMax || hasExtendedAccelerationFuel()) {
            return accelerationFactor;
        }
        return baseMax;
    }

    public int getEffectiveAccelerationFactorMax() {
        int baseMax = Config.temporalOverclockerAccelerationFactorMax;
        if (!hasEntropicClockUpgrade()) {
            return baseMax;
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.round(baseMax * Config.entropicClockMaxFactorMultiplier));
    }

    public int getMaxStoredEntropy() {
        return Math.max(1, Config.entropicClockMaxStored);
    }

    public void tryAbsorbFuelSlot() {
        if (level == null || level.isClientSide() || !hasEntropicClockUpgrade()) {
            return;
        }
        int max = getMaxStoredEntropy();
        boolean changed = false;
        while (true) {
            ItemStack fuel = machineItems.getItem(FUEL_SLOT_INDEX);
            if (!AncientTableFuel.isEntropyFuel(fuel)) {
                break;
            }
            if (!EntropyCharges.canAbsorbOneMore(storedEntropy, max)) {
                break;
            }
            fuel.shrink(1);
            storedEntropy = EntropyCharges.absorbOneDrop(storedEntropy, max);
            machineItems.setItem(FUEL_SLOT_INDEX, fuel.isEmpty() ? ItemStack.EMPTY : fuel);
            changed = true;
        }
        if (changed) {
            setChanged();
        }
    }

    private void clampAccelerationFactor() {
        int min = Config.temporalOverclockerAccelerationFactorMin;
        int max = getEffectiveAccelerationFactorMax();
        if (accelerationFactor > max) {
            accelerationFactor = max;
        } else {
            accelerationFactor = Math.max(min, accelerationFactor);
        }
    }
    
    /**
     * Gets the acceleration factor
     */
    public int getAccelerationFactor() {
        return accelerationFactor;
    }
    
    /**
     * Sets the acceleration factor
     */
    public void setAccelerationFactor(int factor) {
        int min = Config.temporalOverclockerAccelerationFactorMin;
        int max = getEffectiveAccelerationFactorMax();
        if (factor > max) {
            this.accelerationFactor = max;
        } else {
            this.accelerationFactor = Math.max(min, factor);
        }
        setChanged();
    }
    
    /**
     * Increases the acceleration factor by 1
     */
    public void increaseAccelerationFactor() {
        setAccelerationFactor(Math.min(this.accelerationFactor + 1, getEffectiveAccelerationFactorMax()));
    }
    
    /**
     * Decreases the acceleration factor by 1
     */
    public void decreaseAccelerationFactor() {
        int min = Config.temporalOverclockerAccelerationFactorMin;
        setAccelerationFactor(Math.max(this.accelerationFactor - 1, min));
    }
    
    /**
     * Increases the acceleration factor by 5
     */
    public void increaseAccelerationFactorBy5() {
        setAccelerationFactor(Math.min(this.accelerationFactor + 5, getEffectiveAccelerationFactorMax()));
    }
    
    /**
     * Decreases the acceleration factor by 5
     */
    public void decreaseAccelerationFactorBy5() {
        int min = Config.temporalOverclockerAccelerationFactorMin;
        setAccelerationFactor(Math.max(this.accelerationFactor - 5, min));
    }
    
    /**
     * Sets the acceleration factor to maximum
     */
    public void setAccelerationFactorToMax() {
        setAccelerationFactor(getEffectiveAccelerationFactorMax());
    }
    
    /**
     * Sets the acceleration factor to minimum
     */
    public void setAccelerationFactorToMin() {
        setAccelerationFactor(Config.temporalOverclockerAccelerationFactorMin);
    }
    
    /**
     * Sets the acceleration factor to default
     */
    public void setAccelerationFactorToDefault() {
        setAccelerationFactor(Config.temporalOverclockerAccelerationFactor);
    }
    
    /**
     * Gets the energy storage
     */
    public IEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }

    public net.neoforged.neoforge.transfer.energy.EnergyHandler getEnergyHandler() {
        return this.energyHandler26;
    }
    
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt(ENERGY_TAG, this.energyStorage.getEnergyStored());
        output.putInt("redstoneMode", redstoneMode);
        output.putInt("accelerationFactor", accelerationFactor);
        output.putBoolean("persistentMode", persistentMode);
        output.putInt(STORED_ENTROPY_TAG, storedEntropy);
        ItemStack upgrade = machineItems.getItem(UPGRADE_SLOT_INDEX);
        if (!upgrade.isEmpty()) {
            output.store(UPGRADE_STACK_TAG, ItemStack.CODEC, upgrade);
        }
        ItemStack fuel = machineItems.getItem(FUEL_SLOT_INDEX);
        if (!fuel.isEmpty()) {
            output.store(FUEL_STACK_TAG, ItemStack.CODEC, fuel);
        }
        ValueOutput.TypedOutputList<BlockPos> linked = output.list(LINKED_BLOCKS_TAG, BlockPos.CODEC);
        for (BlockPos linkedPos : linkedBlocks) linked.add(linkedPos);
        if (linked.isEmpty()) output.discard(LINKED_BLOCKS_TAG);
    }
    
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getInt(ENERGY_TAG).ifPresent(this.energyStorage::setEnergy);

        this.redstoneMode = input.getIntOr("redstoneMode", this.redstoneMode);
        if (this.redstoneMode == 3) {
            this.redstoneMode = 4;
        }

        if (input.getInt("accelerationFactor").isPresent()) {
            this.accelerationFactor = input.getIntOr("accelerationFactor", Config.temporalOverclockerAccelerationFactor);
            clampAccelerationFactor();
        } else {
            this.accelerationFactor = Config.temporalOverclockerAccelerationFactor;
        }
        
        this.storedEntropy = input.getIntOr(STORED_ENTROPY_TAG, 0);
        ItemStack loadedUpgrade = input.read(UPGRADE_STACK_TAG, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        if (!loadedUpgrade.isEmpty() && !loadedUpgrade.is(ModItems.ENTROPIC_CLOCK.get())) {
            loadedUpgrade = ItemStack.EMPTY;
        }
        machineItems.setItem(UPGRADE_SLOT_INDEX, loadedUpgrade.isEmpty() ? ItemStack.EMPTY : loadedUpgrade.copyWithCount(1));
        ItemStack loadedFuel = input.read(FUEL_STACK_TAG, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        if (!loadedFuel.isEmpty() && !AncientTableFuel.isEntropyFuel(loadedFuel)) {
            loadedFuel = ItemStack.EMPTY;
        }
        machineItems.setItem(FUEL_SLOT_INDEX, loadedFuel);
        this.storedEntropy = Mth.clamp(this.storedEntropy, 0, getMaxStoredEntropy());
        clampAccelerationFactor();
        
        // Load persistent mode
        this.persistentMode = input.getBooleanOr("persistentMode", this.persistentMode);
        
        // Load linked blocks
        linkedBlocks.clear();
        for (BlockPos linkedPos : input.listOrEmpty(LINKED_BLOCKS_TAG, BlockPos.CODEC)) linkedBlocks.add(linkedPos);
    }
    
    /**
     * Gets the redstone mode
     */
    public int getRedstoneMode() {
        return redstoneMode;
    }
    
    /**
     * Sets the redstone mode
     */
    public void setRedstoneMode(int redstoneMode) {
        int m = Math.floorMod(redstoneMode, 5);
        if (m == 3) {
            m = 4;
        }
        this.redstoneMode = m;
        setChanged();
    }
    
    /**
     * Gets the persistent mode
     */
    public boolean isPersistentMode() {
        return persistentMode;
    }
    
    /**
     * Sets the persistent mode
     */
    public void setPersistentMode(boolean persistentMode) {
        this.persistentMode = persistentMode;
        setChanged();
        // Force sync to client
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
    
    /**
     * Toggles the persistent mode
     */
    public void togglePersistentMode() {
        setPersistentMode(!persistentMode);
    }
    
    private final class OverclockerItemHandler implements IItemHandlerModifiable {
        @Override
        public int getSlots() {
            return MACHINE_SLOT_COUNT;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return machineItems.getItem(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || slot < 0 || slot >= MACHINE_SLOT_COUNT) {
                return stack;
            }
            if (slot == UPGRADE_SLOT_INDEX) {
                if (!stack.is(ModItems.ENTROPIC_CLOCK.get())) {
                    return stack;
                }
                if (!machineItems.getItem(UPGRADE_SLOT_INDEX).isEmpty()) {
                    return stack;
                }
                if (!simulate) {
                    machineItems.setItem(UPGRADE_SLOT_INDEX, stack.copyWithCount(1));
                    onMachineSlotChanged(UPGRADE_SLOT_INDEX);
                }
                ItemStack rem = stack.copy();
                rem.shrink(1);
                return rem.isEmpty() ? ItemStack.EMPTY : rem;
            }
            if (slot == FUEL_SLOT_INDEX) {
                if (!AncientTableFuel.isEntropyFuel(stack)) {
                    return stack;
                }
                return insertFuelStack(stack, simulate);
            }
            return stack;
        }

        private ItemStack insertFuelStack(ItemStack stack, boolean simulate) {
            ItemStack existing = machineItems.getItem(FUEL_SLOT_INDEX);
            if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack)) {
                return stack;
            }
            int limit = 64;
            int free = limit - (existing.isEmpty() ? 0 : existing.getCount());
            if (free <= 0) {
                return stack;
            }
            int toInsert = Math.min(free, stack.getCount());
            if (!simulate) {
                if (existing.isEmpty()) {
                    ItemStack copy = stack.copy();
                    copy.setCount(toInsert);
                    machineItems.setItem(FUEL_SLOT_INDEX, copy);
                } else {
                    existing.grow(toInsert);
                    machineItems.setItem(FUEL_SLOT_INDEX, existing);
                }
                tryAbsorbFuelSlot();
            }
            ItemStack rem = stack.copy();
            rem.shrink(toInsert);
            return rem.isEmpty() ? ItemStack.EMPTY : rem;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0 || slot != FUEL_SLOT_INDEX) {
                return ItemStack.EMPTY;
            }
            ItemStack existing = machineItems.getItem(FUEL_SLOT_INDEX);
            if (existing.isEmpty()) {
                return ItemStack.EMPTY;
            }
            int toExtract = Math.min(amount, existing.getCount());
            ItemStack extracted = existing.copy();
            extracted.setCount(toExtract);
            if (!simulate) {
                existing.shrink(toExtract);
                machineItems.setItem(FUEL_SLOT_INDEX, existing.isEmpty() ? ItemStack.EMPTY : existing);
                setChanged();
            }
            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == UPGRADE_SLOT_INDEX ? 1 : 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (stack.isEmpty()) {
                return true;
            }
            return switch (slot) {
                case UPGRADE_SLOT_INDEX -> stack.is(ModItems.ENTROPIC_CLOCK.get());
                case FUEL_SLOT_INDEX -> AncientTableFuel.isEntropyFuel(stack);
                default -> false;
            };
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (slot < 0 || slot >= MACHINE_SLOT_COUNT) {
                return;
            }
            machineItems.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            onMachineSlotChanged(slot);
        }
    }

    /**
     * Custom EnergyStorage implementation
     */
    public static class EnergyStorageImpl extends EnergyStorage {
        public EnergyStorageImpl(int capacity) {
            super(capacity);
        }
        
        public void setEnergy(int energy) {
            this.energy = Math.max(0, Math.min(energy, capacity));
        }
    }
    
    // ===== Network Synchronization =====
    
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        // Same shape as disk NBT (ValueInput / BlockPos.CODEC list); client applies via onDataPacket → loadAdditional
        tag.merge(this.saveCustomOnly(provider));
        return tag;
    }
    
    // onDataPacket is handled by the current BlockEntity serialization system.
}

