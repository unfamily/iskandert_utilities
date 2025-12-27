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
    // When true the timer is blocked (keeps off) while redstone signal is present
    private boolean blockedByRedstone = false;
    
    public SmartTimerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SMART_TIMER_BE.get(), pos, state);
    }
    
    public static void tick(Level level, BlockPos pos, BlockState state, SmartTimerBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }

        // Check current redstone input (any neighbor)
        int redstonePower = level.getBestNeighborSignal(pos);
        boolean hasRedstoneSignal = redstonePower > 0;

        if (blockEntity.isSignalActive) {
            // Currently outputting signal: count duration as usual
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
            // Off phase: if we receive any redstone signal, block the timer,
            // reset the cooldown and keep it off until the redstone signal stops.
            if (hasRedstoneSignal) {
                if (!blockEntity.blockedByRedstone) {
                    blockEntity.blockedByRedstone = true;
                    blockEntity.currentTick = 0; // reset timer while blocked
                    blockEntity.setChanged();
                    // Ensure block state is unpowered while blocked
                    if (state.getValue(SmartTimerBlock.POWERED)) {
                        level.setBlock(pos, state.setValue(SmartTimerBlock.POWERED, false), 3);
                        level.updateNeighborsAt(pos, state.getBlock());
                    }
                }
                // While blocked, do not advance the cooldown
                return;
            } else {
                // No redstone input: if we were blocked, clear the block and reset timer
                if (blockEntity.blockedByRedstone) {
                    blockEntity.blockedByRedstone = false;
                    blockEntity.currentTick = 0;
                    blockEntity.setChanged();
                }

                // Normal cooldown progression
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
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("CooldownTicks", cooldownTicks);
        tag.putInt("SignalDurationTicks", signalDurationTicks);
        tag.putInt("CurrentTick", currentTick);
        tag.putBoolean("IsSignalActive", isSignalActive);
        tag.putBoolean("BlockedByRedstone", blockedByRedstone);
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
        if (tag.contains("BlockedByRedstone")) {
            blockedByRedstone = tag.getBoolean("BlockedByRedstone");
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
