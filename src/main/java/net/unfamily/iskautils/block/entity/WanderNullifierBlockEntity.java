package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.unfamily.iskautils.block.WanderNullifierBlock;
import net.unfamily.iskautils.world.NullifierChunkIndex;

public class WanderNullifierBlockEntity extends BlockEntity {
    private EnderNullifierRedstoneMode redstoneMode = EnderNullifierRedstoneMode.MANUAL;
    private boolean manualEnabled = true;
    private boolean previousRedstoneState = false;

    public WanderNullifierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WANDER_NULLIFIER_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WanderNullifierBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }
        boolean effectiveBefore = state.getValue(WanderNullifierBlock.ON);
        boolean effectiveAfter = blockEntity.computeEffectiveActive(state.getValue(WanderNullifierBlock.POWERED));
        if (effectiveBefore != effectiveAfter) {
            level.setBlock(pos, state.setValue(WanderNullifierBlock.ON, effectiveAfter), 3);
        }
        blockEntity.syncIndex(effectiveAfter);
    }

    public void onRedstoneChanged(Level level, BlockPos pos, BlockState state, boolean powered) {
        previousRedstoneState = powered;
        boolean effective = computeEffectiveActive(powered);
        if (state.getValue(WanderNullifierBlock.ON) != effective) {
            level.setBlock(pos, state.setValue(WanderNullifierBlock.ON, effective), 3);
        }
        syncIndex(effective);
        setChanged();
    }

    public void toggleManualEnabled(Level level, BlockPos pos, BlockState state) {
        manualEnabled = !manualEnabled;
        applyEffectiveState(level, pos, state);
    }

    public void cycleRedstoneMode(Level level, BlockPos pos, BlockState state) {
        redstoneMode = redstoneMode.next();
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
        };
    }

    public void reconcileEffectiveState() {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof WanderNullifierBlock)) {
            return;
        }
        boolean powered = state.getValue(WanderNullifierBlock.POWERED);
        boolean effective = computeEffectiveActive(powered);
        if (state.getValue(WanderNullifierBlock.ON) != effective) {
            level.setBlock(worldPosition, state.setValue(WanderNullifierBlock.ON, effective), 3);
        }
        syncIndex(effective);
    }

    private void applyEffectiveState(Level level, BlockPos pos, BlockState state) {
        boolean powered = state.getValue(WanderNullifierBlock.POWERED);
        boolean effective = computeEffectiveActive(powered);
        if (state.getValue(WanderNullifierBlock.ON) != effective) {
            level.setBlock(pos, state.setValue(WanderNullifierBlock.ON, effective), 3);
        }
        syncIndex(effective);
        setChanged();
    }

    private void syncIndex(boolean active) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (active) {
            NullifierChunkIndex.refresh(serverLevel, worldPosition, NullifierChunkIndex.Kind.WANDER);
        } else {
            NullifierChunkIndex.remove(serverLevel, worldPosition, NullifierChunkIndex.Kind.WANDER);
        }
    }

    public void clearSpatialIndex() {
        if (level instanceof ServerLevel serverLevel) {
            NullifierChunkIndex.remove(serverLevel, worldPosition, NullifierChunkIndex.Kind.WANDER);
        }
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (!level.isClientSide()) {
            reconcileEffectiveState();
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int modeValue = input.getInt("RedstoneMode").orElse(0);
        if (modeValue == 3) {
            modeValue = 0;
        }
        redstoneMode = EnderNullifierRedstoneMode.fromValue(modeValue);
        manualEnabled = input.getBooleanOr("ManualEnabled", true);
        previousRedstoneState = input.getBooleanOr("PreviousRedstoneState", false);
        if (level != null && !level.isClientSide()) {
            reconcileEffectiveState();
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("RedstoneMode", redstoneMode.getValue());
        output.putBoolean("ManualEnabled", manualEnabled);
        output.putBoolean("PreviousRedstoneState", previousRedstoneState);
    }
}
