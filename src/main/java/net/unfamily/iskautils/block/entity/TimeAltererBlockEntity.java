package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.TimeAltererBlock;

import java.util.List;

public class TimeAltererBlockEntity extends BlockEntity {
    private static final String ENERGY_TAG = "Energy";
    private static final String ACTIVE_MODE_TAG = "ActiveMode";
    
    private int activeMode = -1; // Active mode (-1 = inactive)
    
    // Energy storage for NeoForge Energy API
    private final EnergyStorageImpl energyStorage;
    
    public TimeAltererBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TIME_ALTERER_BE.get(), pos, state);
        this.energyStorage = new EnergyStorageImpl(Config.timeAltererEnergyBuffer);
        // Start with 0 energy
        this.energyStorage.setEnergy(0);
    }

    /**
     * Executes time change
     * @return true if time was actually changed
     */
    private boolean executeTimeChange(int mode) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        
        // Set time based on mode
        // Minecraft day is 24000 ticks
        // 0 = day (1000)
        // 1 = night (13000)
        // 2 = noon (6000)
        // 3 = midnight (18000)
        long newTime;
        switch (mode) {
            case 0 -> newTime = 1000;     // Day
            case 1 -> newTime = 13000;    // Night
            case 2 -> newTime = 6000;     // Noon
            case 3 -> newTime = 18000;    // Midnight
            default -> newTime = 0;       // Should never happen
        }
        
        // Set the time
        serverLevel.setDayTime(newTime);
        
        return true; // Time was changed
    }
    
    /**
     * Plays the appropriate sound for time change
     */
    private void playTimeChangeSound(int mode) {
        switch (mode) {
            case 0 -> level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);       // Day - bright sound
            case 1 -> level.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 0.8F);     // Night - darker sound
            case 2 -> level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.2F);  // Noon - high pitch
            case 3 -> level.playSound(null, worldPosition, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.BLOCKS, 1.0F, 0.5F);  // Midnight - creepy sound
        }
    }
    
    /**
     * Notifies nearby players with a message
     */
    private void notifyNearbyPlayers(Component message) {
        if (level == null || level.isClientSide) {
            return;
        }
        
        // Get nearby players (within 64 blocks)
        List<ServerPlayer> nearbyPlayers = ((ServerLevel) level).getPlayers(
            player -> player.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) <= 64 * 64
        );
        
        // Send message to all nearby players
        for (ServerPlayer player : nearbyPlayers) {
            player.displayClientMessage(message, true);
        }
    }
    
    /**
     * Called when the block receives a redstone signal
     */
    public void activateTimeChange() {
        if (level == null || level.isClientSide) {
            return;
        }
        
        // Get the current mode from blockstate
        int mode = this.getBlockState().getValue(TimeAltererBlock.MODE);
        
        // Check if there's enough energy
        int energyRequired = Config.timeAltererEnergyConsume;
        if (energyRequired > 0 && this.energyStorage.getEnergyStored() < energyRequired) {
            // Not enough energy, simply return without any feedback
            return;
        }
        
        // Change time
        boolean timeChanged = executeTimeChange(mode);
        
        // Only consume energy if time was actually changed
        if (timeChanged) {
            // Consume energy
            this.energyStorage.extractEnergy(energyRequired, false);
            
            // Set active mode
            this.activeMode = mode;
            
            // Play time change sound
            playTimeChangeSound(mode);
            
            this.setChanged();
        }
    }
    
    /**
     * Recharges the block's energy
     * @param amount Amount of energy to add
     * @return Amount of energy actually added
     */
    public int rechargeEnergy(int amount) {
        int maxBuffer = Config.timeAltererEnergyBuffer;
        if (maxBuffer <= 0 || amount <= 0) {
            return 0;
        }
        
        int currentEnergy = this.energyStorage.getEnergyStored();
        int energyToAdd = Math.min(amount, maxBuffer - currentEnergy);
        
        if (energyToAdd > 0) {
            // Use the built-in method to receive energy (simulating = false for real operation)
            this.energyStorage.receiveEnergy(energyToAdd, false);
            setChanged();
        }
        
        return energyToAdd;
    }
    
    /**
     * Gets the current energy stored
     */
    public int getEnergyStored() {
        return this.energyStorage.getEnergyStored();
    }
    
    /**
     * Gets the maximum energy capacity
     */
    public int getMaxEnergyStored() {
        return Config.timeAltererEnergyBuffer;
    }
    
    /**
     * Checks if the block has enough energy for time change
     */
    public boolean hasEnoughEnergy() {
        return this.energyStorage.getEnergyStored() >= Config.timeAltererEnergyConsume;
    }
    
    /**
     * Save data to NBT
     */
    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        // Save energy directly from the energy storage
        tag.putInt(ENERGY_TAG, this.energyStorage.getEnergyStored());
        tag.putInt(ACTIVE_MODE_TAG, activeMode);
    }
    
    /**
     * Load data from NBT
     */
    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains(ENERGY_TAG)) {
            // Load energy directly into the energy storage
            this.energyStorage.setEnergy(tag.getInt(ENERGY_TAG));
        }
        if (tag.contains(ACTIVE_MODE_TAG)) {
            activeMode = tag.getInt(ACTIVE_MODE_TAG);
        }
    }
    
    /**
     * Ticker method that handles block entity updates
     */
    public static void tick(Level level, BlockPos blockPos, BlockState blockState, TimeAltererBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }
        
        // If the block is inactive but was active before, reset the active mode
        if (!blockState.getValue(TimeAltererBlock.POWERED) && blockEntity.activeMode != -1) {
            // Play deactivation sound
            level.playSound(null, blockPos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
            
            // Reset active mode to -1 (inactive)
            blockEntity.activeMode = -1;
            blockEntity.setChanged();
        }
    }
    
    /**
     * Returns the energy storage for the NeoForge Energy API
     */
    public IEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }
    
    /**
     * Custom EnergyStorage implementation for NeoForge Energy API
     */
    public static class EnergyStorageImpl extends EnergyStorage {
        public EnergyStorageImpl(int capacity) {
            super(capacity);
        }
        
        public void setEnergy(int energy) {
            this.energy = Math.max(0, Math.min(energy, capacity));
        }
        
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int energyReceived = super.receiveEnergy(maxReceive, simulate);
            return energyReceived;
        }
        
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (simulate) {
                return Math.min(this.energy, maxExtract);
            } else {
                int energyExtracted = Math.min(this.energy, maxExtract);
                this.energy -= energyExtracted;
                return energyExtracted;
            }
        }
        
        @Override
        public boolean canExtract() {
            return true; // Now allow energy extraction for our own use
        }
        
        @Override
        public boolean canReceive() {
            return true; // Can receive energy
        }
    }
} 