package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.unfamily.iskautils.block.SmartTimerBlock;
import net.unfamily.iskautils.client.model.SmartTimerModelData;

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
     * Relative faces of the block that can be configured for I/O.
     * BACK = facing, LEFT/RIGHT = 90° rotation, UP/DOWN = always UP/DOWN.
     * FRONT (opposite of facing) is not configurable.
     */
    public enum RelativeFace {
        BACK(0),
        LEFT(1),
        RIGHT(2),
        UP(3),
        DOWN(4);
        
        private final int index;
        
        RelativeFace(int index) {
            this.index = index;
        }
        
        public int getIndex() {
            return index;
        }
        
        public static RelativeFace fromIndex(int index) {
            for (RelativeFace face : values()) {
                if (face.index == index) return face;
            }
            return BACK;
        }
    }
    
    private byte[] ioConfig = new byte[5];
    
    /**
     * Enum for I/O types
     */
    public enum IoType {
        BLANK(0),
        INPUT(1),
        OUTPUT(2);
        
        private final byte value;
        
        IoType(int value) {
            this.value = (byte) value;
        }
        
        public byte getValue() {
            return value;
        }
        
        public static IoType fromValue(byte value) {
            for (IoType type : values()) {
                if (type.value == value) return type;
            }
            return BLANK;
        }
        
        public IoType next() {
            return switch (this) {
                case BLANK -> INPUT;
                case INPUT -> OUTPUT;
                case OUTPUT -> BLANK;
            };
        }
    }
    
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
        Direction facing = state.getValue(SmartTimerBlock.FACING);
        RedstoneMode mode = RedstoneMode.fromValue(redstoneMode);
        
        if (mode == RedstoneMode.NONE) {
            return false;
        }
        
        boolean[] currentInputStates = new boolean[5];
        boolean hasAnyInput = false;
        
        for (RelativeFace face : RelativeFace.values()) {
            Direction worldDir = toWorldDirection(face, facing);
            int faceIndex = face.getIndex();
            IoType ioType = IoType.fromValue(ioConfig[faceIndex]);
            
            if (ioType == IoType.INPUT) {
                hasAnyInput = true;
                BlockPos neighborPos = pos.relative(worldDir);
                int signal = level.getSignal(neighborPos, worldDir.getOpposite());
                currentInputStates[faceIndex] = signal > 0;
            }
        }
        
        if (!hasAnyInput) {
            return false;
        }
        
        boolean shouldBlock = false;
        
        switch (mode) {
            case LOW -> {
                for (int i = 0; i < 5; i++) {
                    if (IoType.fromValue(ioConfig[i]) == IoType.INPUT && currentInputStates[i]) {
                        shouldBlock = true;
                        break;
                    }
                }
            }
            case HIGH -> {
                for (int i = 0; i < 5; i++) {
                    if (IoType.fromValue(ioConfig[i]) == IoType.INPUT && !currentInputStates[i]) {
                        shouldBlock = true;
                        break;
                    }
                }
            }
            default -> shouldBlock = false;
        }
        
        return shouldBlock;
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
    
    /**
     * Converts a world direction to a relative face.
     * BACK = facing, LEFT/RIGHT = 90° rotation, UP/DOWN = always UP/DOWN.
     */
    public static RelativeFace toRelativeFace(Direction worldDirection, Direction facing) {
        if (worldDirection == facing) {
            return RelativeFace.BACK;
        }
        
        Direction left = calculateLeftDirection(facing);
        if (worldDirection == left) {
            return RelativeFace.LEFT;
        }
        
        if (worldDirection == left.getOpposite()) {
            return RelativeFace.RIGHT;
        }
        
        if (worldDirection == Direction.UP) {
            return RelativeFace.UP;
        }
        if (worldDirection == Direction.DOWN) {
            return RelativeFace.DOWN;
        }
        
        return RelativeFace.BACK;
    }
    
    private static Direction calculateLeftDirection(Direction facing) {
        if (facing.getAxis() == Direction.Axis.Y) {
            return Direction.WEST;
        }
        
        return switch (facing) {
            case NORTH -> Direction.WEST;
            case SOUTH -> Direction.EAST;
            case EAST -> Direction.NORTH;
            case WEST -> Direction.SOUTH;
            default -> Direction.WEST;
        };
    }
    
    /**
     * Converts a relative face to a world direction.
     */
    public static Direction toWorldDirection(RelativeFace relativeFace, Direction facing) {
        return switch (relativeFace) {
            case BACK -> facing;
            case LEFT -> calculateLeftDirection(facing);
            case RIGHT -> calculateLeftDirection(facing).getOpposite();
            case UP -> Direction.UP;
            case DOWN -> Direction.DOWN;
        };
    }
    
    public byte getIoConfig(RelativeFace relativeFace) {
        return ioConfig[relativeFace.getIndex()];
    }
    
    public byte getIoConfig(Direction worldDirection) {
        Direction facing = this.getBlockState().getValue(SmartTimerBlock.FACING);
        RelativeFace relativeFace = toRelativeFace(worldDirection, facing);
        return ioConfig[relativeFace.getIndex()];
    }
    
    public void setIoConfig(RelativeFace relativeFace, byte ioType) {
        ioConfig[relativeFace.getIndex()] = ioType;
        setChanged();
        if (this.level != null) {
            if (!this.level.isClientSide) {
                this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
            } else {
                requestModelDataUpdate();
            }
        }
    }
    
    public void setIoConfig(Direction worldDirection, byte ioType) {
        Direction facing = this.getBlockState().getValue(SmartTimerBlock.FACING);
        RelativeFace relativeFace = toRelativeFace(worldDirection, facing);
        ioConfig[relativeFace.getIndex()] = ioType;
        setChanged();
        if (this.level != null) {
            if (!this.level.isClientSide) {
                this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
            } else {
                requestModelDataUpdate();
            }
        }
    }
    
    public void cycleIoConfig(RelativeFace relativeFace) {
        IoType currentType = IoType.fromValue(ioConfig[relativeFace.getIndex()]);
        IoType nextType = currentType.next();
        ioConfig[relativeFace.getIndex()] = nextType.getValue();
        setChanged();
        if (this.level != null) {
            if (!this.level.isClientSide) {
                this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
            } else {
                requestModelDataUpdate();
            }
        }
    }
    
    public void cycleIoConfig(Direction worldDirection) {
        Direction facing = this.getBlockState().getValue(SmartTimerBlock.FACING);
        RelativeFace relativeFace = toRelativeFace(worldDirection, facing);
        cycleIoConfig(relativeFace);
    }
    
    public void resetAllIoConfig() {
        for (int i = 0; i < 5; i++) {
            ioConfig[i] = IoType.BLANK.getValue();
        }
        setChanged();
        if (this.level != null) {
            if (!this.level.isClientSide) {
                this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
            } else {
                requestModelDataUpdate();
            }
        }
    }
    
    public byte[] getIoConfigArray() {
        return ioConfig.clone();
    }
    
    public void setIoConfigArray(byte[] config) {
        if (config != null && config.length == 5) {
            System.arraycopy(config, 0, ioConfig, 0, 5);
            setChanged();
            if (this.level != null) {
                if (!this.level.isClientSide) {
                    this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
            } else {
                requestModelDataUpdate();
            }
            }
        }
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("CooldownTicks", cooldownTicks);
        tag.putInt("SignalDurationTicks", signalDurationTicks);
        tag.putInt("CurrentTick", currentTick);
        tag.putBoolean("IsSignalActive", isSignalActive);
        tag.putInt("RedstoneMode", redstoneMode);
        tag.putByteArray("IoConfig", ioConfig);
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
        if (tag.contains("IoConfig")) {
            byte[] loadedConfig = tag.getByteArray("IoConfig");
            if (loadedConfig.length == 5) {
                System.arraycopy(loadedConfig, 0, ioConfig, 0, 5);
            } else if (loadedConfig.length == 6) {
                // Legacy format: convert 6 absolute directions to 5 relative faces
                Direction facing = this.getBlockState().getValue(SmartTimerBlock.FACING);
                byte[] newConfig = new byte[5];
                for (RelativeFace face : RelativeFace.values()) {
                    Direction worldDir = toWorldDirection(face, facing);
                    int worldIndex = worldDir.ordinal();
                    newConfig[face.getIndex()] = loadedConfig[worldIndex];
                }
                System.arraycopy(newConfig, 0, ioConfig, 0, 5);
            }
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
        tag.putByteArray("IoConfig", ioConfig);
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
            if (tag.contains("IoConfig")) {
                byte[] loadedConfig = tag.getByteArray("IoConfig");
                if (loadedConfig.length == 5) {
                    System.arraycopy(loadedConfig, 0, ioConfig, 0, 5);
                    requestModelDataUpdate();
                } else if (loadedConfig.length == 6) {
                    // Legacy format: convert 6 absolute directions to 5 relative faces
                    Direction facing = this.getBlockState().getValue(SmartTimerBlock.FACING);
                    byte[] newConfig = new byte[5];
                    for (RelativeFace face : RelativeFace.values()) {
                        Direction worldDir = toWorldDirection(face, facing);
                        int worldIndex = worldDir.ordinal();
                        newConfig[face.getIndex()] = loadedConfig[worldIndex];
                    }
                    System.arraycopy(newConfig, 0, ioConfig, 0, 5);
                    requestModelDataUpdate();
                }
            }
        }
    }
    
    @Override
    public ModelData getModelData() {
        ModelData.Builder builder = ModelData.builder();
        
        BlockState state = this.getBlockState();
        Direction facing = state.getValue(SmartTimerBlock.FACING);
        byte[] ioConfigWorld = new byte[6];
        boolean[] inputStates = new boolean[6];
        
        if (this.level != null && this.getBlockPos() != null) {
            for (RelativeFace face : RelativeFace.values()) {
                Direction worldDir = toWorldDirection(face, facing);
                int worldIndex = worldDir.ordinal();
                int faceIndex = face.getIndex();
                
                ioConfigWorld[worldIndex] = ioConfig[faceIndex];
                
                if (ioConfig[faceIndex] == 1) {
                    int signal = this.level.getSignal(
                        this.getBlockPos().relative(worldDir),
                        worldDir.getOpposite()
                    );
                    inputStates[worldIndex] = signal > 0;
                }
            }
        }
        
        ioConfigWorld[facing.getOpposite().ordinal()] = IoType.BLANK.getValue();
        
        builder.with(SmartTimerModelData.IO_CONFIG_PROPERTY, ioConfigWorld);
        builder.with(SmartTimerModelData.INPUT_STATES_PROPERTY, inputStates);
        
        return builder.build();
    }
}
