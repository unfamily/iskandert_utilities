package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.HellfireIgniterBlock;

import org.jetbrains.annotations.Nullable;

public class HellfireIgniterBlockEntity extends BlockEntity {
    // Energy storage
    private final EnergyStorageImpl energyStorage;
    private IEnergyStorage energyHandler;
    private final net.neoforged.neoforge.transfer.energy.EnergyHandler energyHandler26;
    
    // Effective consumption and capacity values
    private final int effectiveBufferCapacity;
    private final int effectiveEnergyConsume;
    
    // Redstone mode: 0=NONE, 1=LOW, 2=HIGH, 3=PULSE, 4=DISABLED
    private int redstoneMode = 3; // Default: PULSE (original behavior)
    
    // Redstone pulse mode tracking
    private boolean previousRedstoneState = false; // For PULSE mode
    private int pulseIgnoreTimer = 0; // Timer to ignore redstone after pulse
    private static final int PULSE_IGNORE_INTERVAL = 10; // Ignore pulses for 0.5 seconds after activation

    public HellfireIgniterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HELLFIRE_IGNITER_BE.get(), pos, state);
        
        // Calculate effective values based on configurations
        this.effectiveBufferCapacity = determineEffectiveCapacity();
        this.effectiveEnergyConsume = determineEffectiveConsumption();
        
        // Create storage with configured buffer
        this.energyStorage = new EnergyStorageImpl(this.effectiveBufferCapacity);
        this.energyHandler = this.energyStorage;
        this.energyHandler26 = new EnergyHandlerImpl();
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
            return effectiveBufferCapacity;
        }

        @Override
        public int insert(int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
            net.neoforged.neoforge.transfer.TransferPreconditions.checkNonNegative(amount);
            if (amount == 0 || effectiveBufferCapacity <= 0) return 0;
            updateSnapshots(transaction);
            return energyStorage.receiveEnergy(amount, false);
        }

        @Override
        public int extract(int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
            net.neoforged.neoforge.transfer.TransferPreconditions.checkNonNegative(amount);
            if (amount == 0 || effectiveBufferCapacity <= 0) return 0;
            updateSnapshots(transaction);
            return energyStorage.extractEnergy(amount, false);
        }
    }
    
    // Determine effective energy capacity based on configurations
    private int determineEffectiveCapacity() {
        // If consumption is 0, we don't need capacity
        if (Config.hellfireIgniterConsume <= 0) {
            return 0;
        }
        
        // If configured capacity is 0, we don't store energy
        if (Config.hellfireIgniterBuffer <= 0) {
            return 0;
        }
        
        // Otherwise, use the configured value
        return Config.hellfireIgniterBuffer;
    }
    
    // Determine effective energy consumption based on configurations
    private int determineEffectiveConsumption() {
        // If capacity is 0, we can't consume energy
        if (effectiveBufferCapacity <= 0) {
            return 0;
        }
        
        // If consumption is 0, we don't consume energy
        if (Config.hellfireIgniterConsume <= 0) {
            return 0;
        }
        
        // If configured consumption is greater than capacity, limit it to capacity
        if (Config.hellfireIgniterConsume > effectiveBufferCapacity) {
            return effectiveBufferCapacity;
        }
        
        // Otherwise, use the configured value
        return Config.hellfireIgniterConsume;
    }
    
    // Check if the block can receive energy
    public boolean canReceiveEnergy() {
        return effectiveBufferCapacity > 0;
    }
    
    // Check if the block needs energy to function
    public boolean requiresEnergyToFunction() {
        return effectiveEnergyConsume > 0;
    }

    // Called when the block receives a redstone signal
    public void ignite() {
        // Get the target position where fire should be created
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        
        Direction facing = this.getBlockState().getValue(HellfireIgniterBlock.FACING);
        BlockPos targetPos = this.getBlockPos().relative(facing);
        
        // Check if target position is air
        BlockState targetState = level.getBlockState(targetPos);
        if (!targetState.isAir()) {
            // Not air, do nothing and don't consume energy
            return;
        }
        
        // If energy is not required to function, create fire directly
        if (!requiresEnergyToFunction()) {
            placeFire(level, targetPos);
            return;
        }
        
        // Otherwise, check if we have enough energy
        if (energyStorage.getEnergyStored() >= effectiveEnergyConsume) {
            // Extract energy
            energyStorage.extractEnergy(effectiveEnergyConsume, false);
            // Create fire
            placeFire(level, targetPos);
        }
    }
    
    // Separate method to place fire
    private void placeFire(Level level, BlockPos targetPos) {
        // Place fire block (we've already verified it's air)
        level.setBlockAndUpdate(targetPos, Blocks.FIRE.defaultBlockState());
    }

    @Override
    public void invalidateCapabilities() {
        super.invalidateCapabilities();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        // Load energy only if the block has a capacity
        if (canReceiveEnergy()) {
            energyStorage.setEnergy(input.getInt("Energy").orElse(0));
        }
        // Load redstone mode (default: 3 = PULSE)
        redstoneMode = input.getInt("RedstoneMode").orElse(3);
        previousRedstoneState = input.getBooleanOr("PreviousRedstoneState", false);
        pulseIgnoreTimer = input.getInt("PulseIgnoreTimer").orElse(0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        // Save energy only if the block has a capacity
        if (canReceiveEnergy()) {
            output.putInt("Energy", energyStorage.getEnergyStored());
        }
        // Save redstone mode
        output.putInt("RedstoneMode", redstoneMode);
        output.putBoolean("PreviousRedstoneState", previousRedstoneState);
        output.putInt("PulseIgnoreTimer", pulseIgnoreTimer);
    }

    // Tick method called every frame
    public static void tick(Level level, BlockPos pos, BlockState state, HellfireIgniterBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }
        
        // Vanilla-like mode: always force PULSE mode (3)
        if (Config.hellfireIgniterVanillaLike) {
            if (blockEntity.redstoneMode != 3) {
                blockEntity.setRedstoneMode(3); // Force PULSE mode
            }
            // PULSE mode is handled in neighborChanged, skip tick logic
            return;
        }
        
        // Check redstone conditions based on redstone mode
        int redstonePower = level.getBestNeighborSignal(pos);
        boolean hasRedstoneSignal = redstonePower > 0;
        boolean shouldActivate = false;
        
        // PULSE mode (3) is handled in neighborChanged, skip it here
        if (blockEntity.redstoneMode == 3) {
            return; // PULSE mode handled in neighborChanged
        }
        
        switch (blockEntity.redstoneMode) {
            case 0 -> { // NONE: Always active, ignore redstone
                shouldActivate = true;
            }
            case 1 -> { // LOW: Only when redstone is OFF (low signal)
                shouldActivate = !hasRedstoneSignal;
            }
            case 2 -> { // HIGH: Only when redstone is ON (high signal)
                shouldActivate = hasRedstoneSignal;
            }
            case 4 -> { // DISABLED: Always disabled
                shouldActivate = false;
            }
        }
        
        // Handle CONTINUOUS modes (NONE, LOW, HIGH) - maintain fire state
        if (blockEntity.redstoneMode != 4) {
            Direction facing = state.getValue(HellfireIgniterBlock.FACING);
            BlockPos targetPos = pos.relative(facing);
            BlockState targetState = level.getBlockState(targetPos);
            boolean isFire = targetState.is(Blocks.FIRE);
            
            if (shouldActivate) {
                // Should be active: if fire is not present and target is air, place fire
                if (!isFire && targetState.isAir()) {
                    blockEntity.ignite();
                }
            } else {
                // Should not be active: if fire is present, extinguish it
                if (isFire) {
                    level.setBlockAndUpdate(targetPos, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }
    
    // Getter and setter for redstone mode
    public int getRedstoneMode() {
        return redstoneMode;
    }
    
    public void setRedstoneMode(int mode) {
        // Vanilla-like mode: always force PULSE mode (3)
        if (Config.hellfireIgniterVanillaLike) {
            this.redstoneMode = 3; // Force PULSE mode
        } else {
            this.redstoneMode = Math.max(0, Math.min(mode, 4)); // 0-4 range
        }
        setChanged();
    }
    
    public void cycleRedstoneMode() {
        // Vanilla-like mode: always force PULSE mode (3)
        if (Config.hellfireIgniterVanillaLike) {
            this.redstoneMode = 3; // Force PULSE mode
        } else {
            this.redstoneMode = (this.redstoneMode + 1) % 5; // Cycle through 0-4
        }
        setChanged();
    }

    // Custom EnergyStorage implementation
    public static class EnergyStorageImpl extends EnergyStorage {
        public EnergyStorageImpl(int capacity) {
            super(capacity);
        }

        public void setEnergy(int energy) {
            this.energy = Math.max(0, Math.min(energy, capacity));
        }
    }

    // Method to get the energy storage
    public IEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }

    public net.neoforged.neoforge.transfer.energy.EnergyHandler getEnergyHandler() {
        return this.energyHandler26;
    }
    
    // Getters for effective values
    public int getEffectiveBufferCapacity() {
        return effectiveBufferCapacity;
    }
    
    public int getEffectiveEnergyConsume() {
        return effectiveEnergyConsume;
    }
} 