package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.WanderNullifierBlock;
import net.unfamily.iskautils.client.gui.NullifierMenu;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.world.NullifierChunkIndex;

public class WanderNullifierBlockEntity extends BlockEntity implements MenuProvider, INullifierBE {
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

    public WanderNullifierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WANDER_NULLIFIER_BE.get(), pos, state);
    }

    // --- INullifierBE ---
    @Override public NullifierType getNullifierType() { return NullifierType.WANDER; }
    @Override public ItemStackHandler getModuleHandler() { return moduleHandler; }
    @Override public boolean isShowAreaEnabled() { return showAreaEnabled; }
    @Override public void setShowAreaEnabled(boolean v) { showAreaEnabled = v; setChanged(); }

    @Override
    public int getRange() {
        if (range < 0) range = Config.wanderNullifierRadius;
        return range;
    }
    @Override
    public void setRange(int r) {
        this.range = Math.max(1, Math.min(r, getMaxRange()));
        setChanged();
        syncIndex(computeEffectiveActive(getBlockState().getValue(WanderNullifierBlock.POWERED)));
    }

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
        return Config.wanderNullifierMaxRange + modules * Config.wanderNullifierRangeModuleBonus;
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
        if (level != null && !level.isClientSide) {
            applyEffectiveState(level, worldPosition, getBlockState());
        }
    }

    // --- MenuProvider ---
    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.iska_utils.wander_nullifier.title");
    }
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new NullifierMenu(id, inv, this);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WanderNullifierBlockEntity blockEntity) {
        if (level.isClientSide) {
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
        if (level == null || level.isClientSide) {
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
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (active) {
            NullifierChunkIndex.refresh(serverLevel, worldPosition, NullifierChunkIndex.Kind.WANDER, getRange());
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
        if (!level.isClientSide) {
            reconcileEffectiveState();
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int modeValue = tag.contains("RedstoneMode") ? tag.getInt("RedstoneMode") : 0;
        if (modeValue == 3) modeValue = 0;
        redstoneMode = EnderNullifierRedstoneMode.fromValue(modeValue);
        manualEnabled = !tag.contains("ManualEnabled") || tag.getBoolean("ManualEnabled");
        previousRedstoneState = tag.getBoolean("PreviousRedstoneState");
        range = tag.contains("Range") ? tag.getInt("Range") : -1;
        showAreaEnabled = tag.getBoolean("ShowArea");
        if (tag.contains("Modules")) {
            moduleHandler.deserializeNBT(registries, tag.getCompound("Modules"));
        }
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
        tag.putInt("Range", getRange());
        tag.putBoolean("ShowArea", showAreaEnabled);
        tag.put("Modules", moduleHandler.serializeNBT(registries));
    }
}
