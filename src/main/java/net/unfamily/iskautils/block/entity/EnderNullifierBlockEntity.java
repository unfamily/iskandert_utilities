package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.block.EnderNullifierBlock;
import net.unfamily.iskautils.world.EnderNullifierSpatialIndex;

public class EnderNullifierBlockEntity extends BlockEntity {
    private static final int PULSE_DURATION_TICKS = 20;

    private EnderNullifierRedstoneMode redstoneMode = EnderNullifierRedstoneMode.MANUAL;
    private boolean manualEnabled = true;
    private boolean previousRedstoneState = false;
    private int pulseTicksRemaining = 0;

    public EnderNullifierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENDER_NULLIFIER_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, EnderNullifierBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }

        if (blockEntity.pulseTicksRemaining > 0) {
            blockEntity.pulseTicksRemaining--;
        }

        boolean effectiveBefore = state.getValue(EnderNullifierBlock.ON);
        boolean effectiveAfter = blockEntity.computeEffectiveActive(state.getValue(EnderNullifierBlock.POWERED));
        if (effectiveBefore != effectiveAfter) {
            level.setBlock(pos, state.setValue(EnderNullifierBlock.ON, effectiveAfter), 3);
        }
        blockEntity.syncSpatialIndex(effectiveAfter);
    }

    public void onRedstoneChanged(Level level, BlockPos pos, BlockState state, boolean powered) {
        if (redstoneMode == EnderNullifierRedstoneMode.PULSE && powered && !previousRedstoneState) {
            pulseTicksRemaining = PULSE_DURATION_TICKS;
        }
        previousRedstoneState = powered;

        boolean effective = computeEffectiveActive(powered);
        if (state.getValue(EnderNullifierBlock.ON) != effective) {
            level.setBlock(pos, state.setValue(EnderNullifierBlock.ON, effective), 3);
        }
        syncSpatialIndex(effective);
        setChanged();
    }

    public void toggleManualEnabled(Level level, BlockPos pos, BlockState state) {
        manualEnabled = !manualEnabled;
        applyEffectiveState(level, pos, state);
    }

    public void cycleRedstoneMode(Level level, BlockPos pos, BlockState state) {
        redstoneMode = redstoneMode.next();
        pulseTicksRemaining = 0;
        applyEffectiveState(level, pos, state);
    }

    public EnderNullifierRedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    public boolean isManualEnabled() {
        return manualEnabled;
    }

    public boolean computeEffectiveActive(boolean powered) {
        if (!manualEnabled) {
            return false;
        }
        return switch (redstoneMode) {
            case MANUAL -> true;
            case LOW -> !powered;
            case HIGH -> powered;
            case PULSE -> pulseTicksRemaining > 0;
        };
    }

    public void reconcileEffectiveState() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof EnderNullifierBlock)) {
            return;
        }
        boolean powered = state.getValue(EnderNullifierBlock.POWERED);
        boolean effective = computeEffectiveActive(powered);
        if (state.getValue(EnderNullifierBlock.ON) != effective) {
            level.setBlock(worldPosition, state.setValue(EnderNullifierBlock.ON, effective), 3);
        }
        syncSpatialIndex(effective);
    }

    private void applyEffectiveState(Level level, BlockPos pos, BlockState state) {
        boolean powered = state.getValue(EnderNullifierBlock.POWERED);
        boolean effective = computeEffectiveActive(powered);
        if (state.getValue(EnderNullifierBlock.ON) != effective) {
            level.setBlock(pos, state.setValue(EnderNullifierBlock.ON, effective), 3);
        }
        syncSpatialIndex(effective);
        setChanged();
    }

    private void syncSpatialIndex(boolean active) {
        if (level instanceof ServerLevel serverLevel) {
            EnderNullifierSpatialIndex.update(serverLevel.dimension(), worldPosition, active);
        }
    }

    public void clearSpatialIndex() {
        if (level instanceof ServerLevel serverLevel) {
            EnderNullifierSpatialIndex.remove(serverLevel.dimension(), worldPosition);
        }
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (!level.isClientSide) {
            reconcileEffectiveState();
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        redstoneMode = EnderNullifierRedstoneMode.fromValue(tag.contains("RedstoneMode") ? tag.getInt("RedstoneMode") : 0);
        manualEnabled = !tag.contains("ManualEnabled") || tag.getBoolean("ManualEnabled");
        previousRedstoneState = tag.getBoolean("PreviousRedstoneState");
        pulseTicksRemaining = tag.contains("PulseTicksRemaining") ? tag.getInt("PulseTicksRemaining") : 0;
        if (level != null && !level.isClientSide) {
            reconcileEffectiveState();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("RedstoneMode", redstoneMode.getValue());
        tag.putBoolean("ManualEnabled", manualEnabled);
        tag.putBoolean("PreviousRedstoneState", previousRedstoneState);
        tag.putInt("PulseTicksRemaining", pulseTicksRemaining);
    }
}
