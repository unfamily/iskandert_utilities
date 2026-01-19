package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.HellfireIgniterBlock;

import javax.annotation.Nullable;
import java.util.Optional;

public class HellfireIgniterBlockEntity extends BlockEntity {
    // Energy storage
    private final EnergyStorageImpl energyStorage;
    private IEnergyStorage energyHandler;
    
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
        if (level == null || level.isClientSide) {
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
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // Load energy only if the block has a capacity
        if (canReceiveEnergy()) {
            energyStorage.setEnergy(tag.getInt("Energy"));
        }
        // Load redstone mode (default: 3 = PULSE)
        redstoneMode = tag.contains("RedstoneMode") ? tag.getInt("RedstoneMode") : 3;
        previousRedstoneState = tag.contains("PreviousRedstoneState") ? tag.getBoolean("PreviousRedstoneState") : false;
        pulseIgnoreTimer = tag.contains("PulseIgnoreTimer") ? tag.getInt("PulseIgnoreTimer") : 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // Save energy only if the block has a capacity
        if (canReceiveEnergy()) {
            tag.putInt("Energy", energyStorage.getEnergyStored());
        }
        // Save redstone mode
        tag.putInt("RedstoneMode", redstoneMode);
        tag.putBoolean("PreviousRedstoneState", previousRedstoneState);
        tag.putInt("PulseIgnoreTimer", pulseIgnoreTimer);
    }

    // Tick method called every frame
    public static void tick(Level level, BlockPos pos, BlockState state, HellfireIgniterBlockEntity blockEntity) {
        if (level.isClientSide) {
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
    
    // Getters for effective values
    public int getEffectiveBufferCapacity() {
        return effectiveBufferCapacity;
    }
    
    public int getEffectiveEnergyConsume() {
        return effectiveEnergyConsume;
    }
} 