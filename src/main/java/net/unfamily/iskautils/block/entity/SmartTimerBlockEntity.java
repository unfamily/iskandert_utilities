package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.block.SmartTimerBlock;

/**
 * Smart Timer Block Entity
 * Timer that emits a redstone signal every X seconds for Y seconds duration.
 * Default: 5s cooldown, 3s signal duration.
 */
public class SmartTimerBlockEntity extends BlockEntity {
    private static final int DEFAULT_COOLDOWN_TICKS = 5 * 20;
    private static final int DEFAULT_SIGNAL_DURATION_TICKS = 3 * 20;
    
    private int cooldownTicks = DEFAULT_COOLDOWN_TICKS;
    private int signalDurationTicks = DEFAULT_SIGNAL_DURATION_TICKS;
    private int currentTick = 0;
    private boolean isSignalActive = false;
    
    // Redstone mode: 0=NONE, 1=LOW, 2=HIGH, 4=DISABLED (PULSE mode 3 is not available for timer)
    private int redstoneMode = 1; // Default: LOW (only when redstone is OFF)
    
    public SmartTimerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SMART_TIMER_BE.get(), pos, state);
    }
    
    public static void tick(Level level, BlockPos pos, BlockState state, SmartTimerBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }

        if (blockEntity.isSignalActive) {
            // Currently outputting signal: count duration as usual (ignore redstone mode during ON phase)
            blockEntity.currentTick++;
            if (blockEntity.currentTick >= blockEntity.signalDurationTicks) {
                blockEntity.isSignalActive = false;
                blockEntity.currentTick = 0;
                // Ensure block is set to unpowered
                if (state.getValue(SmartTimerBlock.POWERED)) {
                    level.setBlock(pos, state.setValue(SmartTimerBlock.POWERED, false), 3);
                    level.updateNeighborsAt(pos, state.getBlock());
                }
            }
        } else {
            // Off phase (cooldown): apply redstone mode logic here
            int redstonePower = level.getBestNeighborSignal(pos);
            boolean hasRedstoneSignal = redstonePower > 0;
            boolean shouldAdvance = false;
            
            switch (blockEntity.redstoneMode) {
                case 0 -> { // NONE: Always active, ignore redstone
                    shouldAdvance = true;
                }
                case 1 -> { // LOW: Only when redstone is OFF (low signal)
                    shouldAdvance = !hasRedstoneSignal;
                }
                case 2 -> { // HIGH: Only when redstone is ON (high signal)
                    shouldAdvance = hasRedstoneSignal;
                }
                case 4 -> { // DISABLED: Always disabled
                    shouldAdvance = false;
                }
                default -> { // Should never happen, but fallback to DISABLED
                    shouldAdvance = false;
                }
            }
            
            // If we shouldn't advance, return (timer is paused)
            if (!shouldAdvance) {
                return;
            }
            
            // Normal cooldown progression (only if shouldAdvance is true)
            blockEntity.currentTick++;
            if (blockEntity.currentTick >= blockEntity.cooldownTicks) {
                blockEntity.isSignalActive = true;
                blockEntity.currentTick = 0;
                if (!state.getValue(SmartTimerBlock.POWERED)) {
                    level.setBlock(pos, state.setValue(SmartTimerBlock.POWERED, true), 3);
                    level.updateNeighborsAt(pos, state.getBlock());
                }
            }
        }
    }
    
    public void setCooldownSeconds(int seconds) {
        this.cooldownTicks = seconds * 20;
        this.currentTick = 0;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }
    
    public void setCooldownTicks(int ticks) {
        this.cooldownTicks = Math.max(5, ticks);
        this.currentTick = 0;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }
    
    public void setSignalDurationSeconds(int seconds) {
        this.signalDurationTicks = seconds * 20;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }
    
    public void setSignalDurationTicks(int ticks) {
        this.signalDurationTicks = Math.max(5, ticks);
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }
    
    public int getCooldownSeconds() {
        return cooldownTicks / 20;
    }
    
    public int getCooldownTicks() {
        return cooldownTicks;
    }
    
    public int getSignalDurationSeconds() {
        return signalDurationTicks / 20;
    }
    
    public int getSignalDurationTicks() {
        return signalDurationTicks;
    }
    
    public int getRedstoneMode() {
        return redstoneMode;
    }
    
    public void setRedstoneMode(int value) {
        // Ensure value is in valid range and skip PULSE mode (3)
        if (value == 3) {
            value = 4; // Convert PULSE to DISABLED
        }
        this.redstoneMode = Math.max(0, Math.min(value, 4)); // 0-4 range (excluding 3)
        setChanged();
    }
    
    public void cycleRedstoneMode() {
        // Cycle through 0->1->2->4->0 (skip PULSE mode 3)
        int nextMode = (this.redstoneMode + 1) % 5;
        if (nextMode == 3) { // Skip PULSE mode
            nextMode = 4;
        }
        this.redstoneMode = nextMode;
        setChanged();
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("CooldownTicks", cooldownTicks);
        tag.putInt("SignalDurationTicks", signalDurationTicks);
        tag.putInt("CurrentTick", currentTick);
        tag.putBoolean("IsSignalActive", isSignalActive);
        tag.putInt("RedstoneMode", redstoneMode);
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("CooldownTicks")) {
            cooldownTicks = tag.getInt("CooldownTicks");
        }
        if (tag.contains("SignalDurationTicks")) {
            signalDurationTicks = tag.getInt("SignalDurationTicks");
        }
        if (tag.contains("CurrentTick")) {
            currentTick = tag.getInt("CurrentTick");
        }
        if (tag.contains("IsSignalActive")) {
            isSignalActive = tag.getBoolean("IsSignalActive");
        }
        redstoneMode = tag.contains("RedstoneMode") ? tag.getInt("RedstoneMode") : 1; // Default: LOW
        // Ensure redstoneMode is valid (skip PULSE mode 3)
        if (redstoneMode == 3) {
            redstoneMode = 4; // Convert old PULSE mode to DISABLED
        }
    }
    
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    
    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("CooldownTicks", cooldownTicks);
        tag.putInt("SignalDurationTicks", signalDurationTicks);
        return tag;
    }
    
    @Override
    public void onDataPacket(net.minecraft.network.Connection net, 
                            net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt,
                            net.minecraft.core.HolderLookup.Provider registries) {
        super.onDataPacket(net, pkt, registries);
        if (pkt.getTag() != null) {
            CompoundTag tag = pkt.getTag();
            if (tag.contains("CooldownTicks")) {
                cooldownTicks = tag.getInt("CooldownTicks");
            }
            if (tag.contains("SignalDurationTicks")) {
                signalDurationTicks = tag.getInt("SignalDurationTicks");
            }
        }
    }
}
