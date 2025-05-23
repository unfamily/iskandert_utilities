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
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // Save energy only if the block has a capacity
        if (canReceiveEnergy()) {
            tag.putInt("Energy", energyStorage.getEnergyStored());
        }
    }

    // Tick method called every frame
    public static void tick(Level level, BlockPos pos, BlockState state, HellfireIgniterBlockEntity blockEntity) {
        // No tick functionality needed for now
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