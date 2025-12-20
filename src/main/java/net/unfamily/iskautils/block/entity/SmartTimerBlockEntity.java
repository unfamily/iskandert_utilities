package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
    private int redstoneMode = 0;
    
    /**
     * Enum for redstone modes
     */
    public enum RedstoneMode {
        NONE(0),
        LOW(1),
        HIGH(2);
        
        private final int value;
        
        RedstoneMode(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
        
        public static RedstoneMode fromValue(int value) {
            for (RedstoneMode mode : values()) {
                if (mode.value == value) return mode;
            }
            return NONE;
        }
        
        public RedstoneMode next() {
            return switch (this) {
                case NONE -> LOW;
                case LOW -> HIGH;
                case HIGH -> NONE;
            };
        }
    }
    
    public SmartTimerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SMART_TIMER_BE.get(), pos, state);
    }
    
    public static void tick(Level level, BlockPos pos, BlockState state, SmartTimerBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }
        
        boolean shouldBlock = blockEntity.shouldBlockTimer(level, pos, state);
        
        if (shouldBlock) {
            if (blockEntity.isSignalActive) {
                blockEntity.isSignalActive = false;
                BlockState newState = state.setValue(SmartTimerBlock.POWERED, false);
                level.setBlock(pos, newState, 3);
                level.updateNeighborsAt(pos, state.getBlock());
            }
            blockEntity.currentTick = 0;
            return;
        }
        
        blockEntity.currentTick++;
        
        if (blockEntity.isSignalActive) {
            if (blockEntity.currentTick >= blockEntity.signalDurationTicks) {
                blockEntity.isSignalActive = false;
                blockEntity.currentTick = 0;
                level.setBlock(pos, state.setValue(SmartTimerBlock.POWERED, false), 3);
                level.updateNeighborsAt(pos, state.getBlock());
            }
        } else {
            if (blockEntity.currentTick >= blockEntity.cooldownTicks) {
                blockEntity.isSignalActive = true;
                blockEntity.currentTick = 0;
                level.setBlock(pos, state.setValue(SmartTimerBlock.POWERED, true), 3);
                level.updateNeighborsAt(pos, state.getBlock());
            }
        }
    }
    
    private boolean shouldBlockTimer(Level level, BlockPos pos, BlockState state) {
        RedstoneMode mode = RedstoneMode.fromValue(redstoneMode);
        
        if (mode == RedstoneMode.NONE) {
            return false;
        }
        
        // Read redstone signal only from the back (facing direction)
        Direction facing = state.getValue(SmartTimerBlock.FACING);
        BlockPos neighborPos = pos.relative(facing);
        int redstonePower = level.getSignal(neighborPos, facing.getOpposite());
        boolean hasRedstoneSignal = redstonePower > 0;
        
        return switch (mode) {
            case LOW -> hasRedstoneSignal;
            case HIGH -> !hasRedstoneSignal;
            default -> false;
        };
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
    
    public void setRedstoneMode(int redstoneMode) {
        this.redstoneMode = redstoneMode % 3;
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
        if (tag.contains("RedstoneMode")) {
            redstoneMode = tag.getInt("RedstoneMode");
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
