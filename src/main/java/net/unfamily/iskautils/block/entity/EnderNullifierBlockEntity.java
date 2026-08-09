package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.EnderNullifierBlock;
import net.unfamily.iskautils.client.gui.NullifierMenu;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.world.EnderNullifierSpatialIndex;

public class EnderNullifierBlockEntity extends BlockEntity implements MenuProvider, INullifierBE {
    private EnderNullifierRedstoneMode redstoneMode = EnderNullifierRedstoneMode.MANUAL;
    private boolean manualEnabled = true;
    private boolean previousRedstoneState = false;
    private int range = -1;
    private boolean showAreaEnabled = false;

    private final ItemStackHandler moduleHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            clampRangeToMax();
            setChanged();
        }
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ModItems.RANGE_MODULE.get());
        }
        @Override
        public int getSlotLimit(int slot) { return Config.nullifierRangeUpgradeMax; }
    };

    public EnderNullifierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENDER_NULLIFIER_BE.get(), pos, state);
    }

    // --- INullifierBE ---
    @Override public NullifierType getNullifierType() { return NullifierType.ENDER; }
    @Override public ItemStackHandler getModuleHandler() { return moduleHandler; }
    @Override public boolean isShowAreaEnabled() { return showAreaEnabled; }
    @Override public void setShowAreaEnabled(boolean v) { showAreaEnabled = v; setChanged(); }

    @Override
    public int getRange() {
        if (range < 0) range = Config.enderNullifierRadius;
        return range;
    }
    @Override
    public void setRange(int r) {
        this.range = Math.max(1, Math.min(r, getMaxRange()));
        setChanged();
        syncSpatialIndex(computeEffectiveActive(getBlockState().getValue(EnderNullifierBlock.POWERED)));
    }

    /** Shrinks current range when modules are removed and max drops below it. */
    private void clampRangeToMax() {
        if (range < 0) {
            return;
        }
        int max = getMaxRange();
        if (range > max) {
            setRange(max);
        }
    }

    @Override
    public int getMaxRange() {
        int modules = moduleHandler.getStackInSlot(0).getCount();
        return Config.enderNullifierMaxRange + modules * Config.enderNullifierRangeModuleBonus;
    }
    @Override
    public int getRedstoneModeGui() {
        if (!manualEnabled) return 1;
        return switch (redstoneMode) { case MANUAL -> 0; case LOW -> 2; case HIGH -> 3; };
    }
    @Override
    public void setRedstoneModeGui(int guiMode) {
        switch (guiMode) {
            case 0 -> { manualEnabled = true;  redstoneMode = EnderNullifierRedstoneMode.MANUAL; }
            case 1 -> manualEnabled = false;
            case 2 -> { manualEnabled = true;  redstoneMode = EnderNullifierRedstoneMode.LOW; }
            case 3 -> { manualEnabled = true;  redstoneMode = EnderNullifierRedstoneMode.HIGH; }
        }
        if (level != null && !level.isClientSide()) {
            applyEffectiveState(level, worldPosition, getBlockState());
        }
    }

    // --- MenuProvider ---
    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.iska_utils.ender_nullifier.title");
    }
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new NullifierMenu(id, inv, this);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, EnderNullifierBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        boolean effectiveBefore = state.getValue(EnderNullifierBlock.ON);
        boolean effectiveAfter = blockEntity.computeEffectiveActive(state.getValue(EnderNullifierBlock.POWERED));
        if (effectiveBefore != effectiveAfter) {
            level.setBlock(pos, state.setValue(EnderNullifierBlock.ON, effectiveAfter), 3);
        }
        blockEntity.syncSpatialIndex(effectiveAfter);
    }

    public void onRedstoneChanged(Level level, BlockPos pos, BlockState state, boolean powered) {
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
            EnderNullifierSpatialIndex.update(serverLevel.dimension(), worldPosition, active, getRange());
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
        if (!level.isClientSide()) {
            reconcileEffectiveState();
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int modeValue = input.getInt("RedstoneMode").orElse(0);
        if (modeValue == 3) modeValue = 0;
        redstoneMode = EnderNullifierRedstoneMode.fromValue(modeValue);
        manualEnabled = input.getBooleanOr("ManualEnabled", true);
        previousRedstoneState = input.getBooleanOr("PreviousRedstoneState", false);
        range = input.getInt("Range").orElse(-1);
        showAreaEnabled = input.getBooleanOr("ShowArea", false);
        for (net.minecraft.world.ItemStackWithSlot item : input.listOrEmpty("Modules", net.minecraft.world.ItemStackWithSlot.CODEC)) {
            int slot = item.slot();
            if (slot >= 0 && slot < moduleHandler.getSlots()) {
                moduleHandler.setStackInSlot(slot, item.stack());
            }
        }
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
        output.putInt("Range", getRange());
        output.putBoolean("ShowArea", showAreaEnabled);
        net.minecraft.world.level.storage.ValueOutput.TypedOutputList<net.minecraft.world.ItemStackWithSlot> modules =
                output.list("Modules", net.minecraft.world.ItemStackWithSlot.CODEC);
        for (int slot = 0; slot < moduleHandler.getSlots(); slot++) {
            net.minecraft.world.item.ItemStack stack = moduleHandler.getStackInSlot(slot);
            if (!stack.isEmpty()) modules.add(new net.minecraft.world.ItemStackWithSlot(slot, stack));
        }
        if (modules.isEmpty()) output.discard("Modules");
    }
}
