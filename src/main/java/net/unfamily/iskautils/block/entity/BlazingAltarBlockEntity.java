package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.unfamily.iskautils.Config;
import net.unfamily.iskalib.transfer.LegacyItemHandlerResourceHandler;
import net.unfamily.iskautils.block.BlazingAltarSpawnMode;
import net.unfamily.iskautils.client.gui.BlazingAltarMenu;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.item.custom.BurningBrazierItem;
import net.unfamily.iskautils.util.BlazingAltarChunks;
import net.unfamily.iskautils.util.BlazingAltarExtinguishJobs;
import net.unfamily.iskautils.util.BlazingAltarFlamePlacement;
import net.unfamily.iskautils.world.BlazingAltarSpatialIndex;

import javax.annotation.Nullable;
import java.util.List;

public class BlazingAltarBlockEntity extends BlockEntity implements MenuProvider {
    public static final int PLACER_SLOT = 0;
    public static final int MODULE_SLOT = 0;

    private final BlazingAltarConfig config = new BlazingAltarConfig();

    private final ItemStackHandler placerHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            BlazingAltarBlockSync.sync(BlazingAltarBlockEntity.this);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ModItems.BURNING_BRAZIER.get()) || stack.is(ModItems.CURSED_CANDLE.get());
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    private final ItemStackHandler moduleHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            clampChunkRadius();
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ModItems.RANGE_MODULE.get());
        }

        @Override
        public int getSlotLimit(int slot) {
            return Config.blazingAltarRangeUpgradeMax;
        }
    };

    private final ResourceHandler<ItemResource> itemTransferHandler = LegacyItemHandlerResourceHandler.wrap(placerHandler);

    private boolean extinguishing;
    private int extinguishChunkProgress;
    private int extinguishChunkTotal;
    private int redstoneModeBeforeExtinguish = -1;
    private boolean preventChunkSave;

    public BlazingAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLAZING_ALTAR_BE.get(), pos, state);
    }

    public long getInstanceId() {
        return config.getInstanceId();
    }

    public ItemStackHandler getPlacerHandler() {
        return placerHandler;
    }

    public ItemStackHandler getModuleHandler() {
        return moduleHandler;
    }

    public ResourceHandler<ItemResource> getItemTransferHandler() {
        return itemTransferHandler;
    }

    public int countRangeModules() {
        ItemStack stack = moduleHandler.getStackInSlot(MODULE_SLOT);
        return stack.is(ModItems.RANGE_MODULE.get()) ? stack.getCount() : 0;
    }

    public int getMaxChunkRadius() {
        int modules = Math.min(countRangeModules(), Config.blazingAltarRangeUpgradeMax);
        return Config.blazingAltarMaxChunkRadius + modules * Config.blazingAltarRangeModuleChunkBonus;
    }

    public int getChunkRadius() {
        return config.getChunkRadius();
    }

    public void setChunkRadius(int radius) {
        config.setChunkRadius(Math.max(1, Math.min(radius, getMaxChunkRadius())));
        resetPlacementProgress();
        setChanged();
    }

    private void clampChunkRadius() {
        if (config.getChunkRadius() > getMaxChunkRadius()) {
            setChunkRadius(getMaxChunkRadius());
        }
    }

    public void adjustChunkRadius(int delta) {
        setChunkRadius(config.getChunkRadius() + delta);
    }

    public boolean isGroundOnly() {
        return config.isGroundOnly();
    }

    public void setGroundOnly(boolean groundOnly) {
        config.setGroundOnly(groundOnly);
        resetPlacementProgress();
        setChanged();
    }

    public void toggleGroundOnly() {
        setGroundOnly(!config.isGroundOnly());
    }

    public BlazingAltarSpawnMode getSpawnMode() {
        return config.getSpawnMode();
    }

    public void cycleSpawnMode() {
        config.setSpawnMode(config.getSpawnMode().next());
        setChanged();
    }

    public void cycleSpawnModeBackward() {
        config.setSpawnMode(config.getSpawnMode().previous());
        setChanged();
    }

    public int getRedstoneMode() {
        return config.getRedstoneMode();
    }

    public void cycleRedstoneMode() {
        int next = (config.getRedstoneMode() + 1) % 5;
        if (next == 3) {
            next = 4;
        }
        config.setRedstoneMode(next);
        setChanged();
        BlazingAltarBlockSync.sync(this);
    }

    public void cycleRedstoneModeBackward() {
        int mode = switch (config.getRedstoneMode()) {
            case 0 -> 4;
            case 1 -> 0;
            case 2 -> 1;
            case 4 -> 2;
            default -> 4;
        };
        config.setRedstoneMode(mode);
        setChanged();
        BlazingAltarBlockSync.sync(this);
    }

    public void extinguishFlamesInRange(ServerLevel level) {
        if (extinguishing || BlazingAltarExtinguishJobs.hasJob(level, worldPosition, getInstanceId())) {
            cancelExtinguishInRange(level);
            return;
        }
        redstoneModeBeforeExtinguish = config.getRedstoneMode();
        extinguishing = true;
        config.setRedstoneMode(4);
        resetPlacementProgress();
        extinguishChunkProgress = 0;
        extinguishChunkTotal = BlazingAltarChunks.countInRadius(config.getChunkRadius());
        setChanged();
        BlazingAltarBlockSync.sync(this);
        BlazingAltarExtinguishJobs.enqueueFromAltar(level, this, BlazingAltarExtinguishJobs.FinishMode.FINISH_ALTAR);
    }

    public void cancelExtinguishInRange(ServerLevel level) {
        BlazingAltarExtinguishJobs.cancelForAltar(level, worldPosition, getInstanceId());
        extinguishing = false;
        extinguishChunkProgress = 0;
        extinguishChunkTotal = 0;
        if (redstoneModeBeforeExtinguish >= 0) {
            config.setRedstoneMode(redstoneModeBeforeExtinguish == 3 ? 4 : redstoneModeBeforeExtinguish);
            redstoneModeBeforeExtinguish = -1;
        }
        setChanged();
        BlazingAltarBlockSync.sync(this);
    }

    public void enqueueFlameCleanupOnBreak(ServerLevel level) {
        BlazingAltarExtinguishJobs.enqueueBreakCleanup(
                level, worldPosition, config.getChunkRadius(), config.isGroundOnly());
    }

    public void completeExtinguishJob(int normalFlamesRemoved) {
        if (normalFlamesRemoved > 0) {
            ItemStack placer = placerHandler.getStackInSlot(PLACER_SLOT);
            if (placer.is(ModItems.BURNING_BRAZIER.get())) {
                placer.setDamageValue(Math.max(0, placer.getDamageValue() - normalFlamesRemoved));
            }
        }
        config.setRedstoneMode(4);
        clearTransientState();
        resetPlacementProgress();
        setChanged();
        BlazingAltarBlockSync.sync(this);
    }

    public void setExtinguishProgress(int chunksDone, int chunksTotal) {
        extinguishChunkProgress = chunksDone;
        extinguishChunkTotal = chunksTotal;
        setChanged();
    }

    public void onPlaced() {
        clearTransientState();
        BlazingAltarBlockSync.sync(this);
    }

    public void attachToActiveJobIfAny(ServerLevel level) {
        if (!BlazingAltarExtinguishJobs.hasJob(level, worldPosition, getInstanceId())) {
            clearTransientState();
            return;
        }
        int[] progress = BlazingAltarExtinguishJobs.getProgress(level, worldPosition, getInstanceId());
        if (progress != null) {
            setExtinguishProgress(progress[0], progress[1]);
        } else if (extinguishChunkTotal <= 0) {
            extinguishChunkTotal = BlazingAltarChunks.countInRadius(config.getChunkRadius());
        }
        extinguishing = true;
        if (redstoneModeBeforeExtinguish < 0 && config.getRedstoneMode() != 4) {
            redstoneModeBeforeExtinguish = config.getRedstoneMode();
        }
        config.setRedstoneMode(4);
        setChanged();
    }

    public void clearTransientState() {
        extinguishing = false;
        extinguishChunkProgress = 0;
        extinguishChunkTotal = 0;
        redstoneModeBeforeExtinguish = -1;
    }

    public int getPlacementChunkProgress() {
        return config.getPlacementChunkIndex();
    }

    public int getPlacementChunkTotal() {
        return BlazingAltarChunks.countInRadius(config.getChunkRadius());
    }

    public int getExtinguishChunkProgress() {
        return extinguishChunkProgress;
    }

    public int getExtinguishChunkTotal() {
        return extinguishChunkTotal;
    }

    public boolean isExtinguishing() {
        return extinguishing;
    }

    private void resetPlacementProgress() {
        config.setPlacementChunkIndex(0);
    }

    public void prepareForRemoval() {
        if (preventChunkSave) {
            return;
        }
        preventChunkSave = true;
        if (level instanceof ServerLevel serverLevel) {
            BlazingAltarExtinguishJobs.cancelForAltar(serverLevel, worldPosition, getInstanceId());
        }
    }

    @Override
    public void setChanged() {
        if (preventChunkSave) {
            return;
        }
        super.setChanged();
    }

    public boolean isOperational() {
        if (level == null || level.isClientSide()) {
            return false;
        }
        if (extinguishing) {
            return false;
        }
        int mode = Math.max(0, Math.min(config.getRedstoneMode(), 4));
        if (mode == 3) {
            mode = 4;
        }
        int power = level.getBestNeighborSignal(worldPosition);
        boolean sig = power > 0;
        return switch (mode) {
            case 0 -> true;
            case 1 -> !sig;
            case 2 -> sig;
            case 4 -> false;
            default -> true;
        };
    }

    public boolean blocksNaturalSpawn(net.minecraft.world.entity.MobCategory category) {
        if (!isOperational()) {
            return false;
        }
        return switch (config.getSpawnMode()) {
            case BOTH -> true;
            case HOSTILE -> category == net.minecraft.world.entity.MobCategory.MONSTER;
            case PASSIVE -> category != net.minecraft.world.entity.MobCategory.MONSTER;
        };
    }

    public static void tickServer(Level level, BlockPos pos, BlockState state, BlazingAltarBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }
        be.config.setTickCounter(be.config.getTickCounter() + 1);
        int interval = Config.blazingAltarTickInterval;
        if (be.config.getTickCounter() < interval) {
            return;
        }
        be.config.setTickCounter(0);
        if (!be.isOperational()) {
            be.resetPlacementProgress();
            BlazingAltarBlockSync.sync(be);
            return;
        }
        be.tryPlaceFlames((ServerLevel) level);
    }

    private void tryPlaceFlames(ServerLevel level) {
        ItemStack placer = placerHandler.getStackInSlot(PLACER_SLOT);
        List<ChunkPos> chunksByDistance = BlazingAltarChunks.collectLoadedOrdered(level, worldPosition, config.getChunkRadius());
        if (chunksByDistance.isEmpty()) {
            return;
        }
        if (placer.isEmpty()) {
            advancePlacementChunkIndex(chunksByDistance.size());
            return;
        }
        Block flameBlock = BlazingAltarFlamePlacement.flameBlockForPlacer(placer.getItem());
        boolean isBrazier = placer.is(ModItems.BURNING_BRAZIER.get());
        if (isBrazier && placer.getDamageValue() >= BurningBrazierItem.MAX_DURABILITY - 1) {
            return;
        }

        int placementChunkIndex = config.getPlacementChunkIndex();
        if (placementChunkIndex >= chunksByDistance.size()) {
            placementChunkIndex = chunksByDistance.size() - 1;
        }
        ChunkPos currentChunk = chunksByDistance.get(placementChunkIndex);

        RandomSource random = level.getRandom();
        int budget = Config.blazingAltarPlacementsPerTick;
        BlockState flameState = flameBlock.defaultBlockState();
        for (int attempt = 0; attempt < budget; attempt++) {
            BlockPos candidate = randomCandidateInChunk(level, currentChunk, random);
            if (candidate == null) {
                continue;
            }
            if (!BlazingAltarFlamePlacement.canPlaceFlameAt(level, candidate, config.isGroundOnly(), flameState)) {
                continue;
            }
            level.setBlock(candidate, flameState, 3);
            if (isBrazier) {
                int nextDamage = placer.getDamageValue() + 1;
                if (nextDamage >= BurningBrazierItem.MAX_DURABILITY) {
                    placer.setDamageValue(BurningBrazierItem.MAX_DURABILITY - 1);
                } else {
                    placer.setDamageValue(nextDamage);
                }
                setChanged();
            }
        }

        advancePlacementChunkIndex(chunksByDistance.size());
    }

    private void advancePlacementChunkIndex(int loadedChunkCount) {
        if (loadedChunkCount <= 0) {
            return;
        }
        int index = config.getPlacementChunkIndex();
        if (index >= loadedChunkCount) {
            index = loadedChunkCount - 1;
        }
        if (index < loadedChunkCount - 1) {
            config.setPlacementChunkIndex(index + 1);
            setChanged();
        }
    }

    private static final int AIR_CANDIDATE_ATTEMPTS = 16;

    @Nullable
    private BlockPos randomCandidateInChunk(ServerLevel level, ChunkPos chunk, RandomSource random) {
        int baseX = chunk.getMinBlockX();
        int baseZ = chunk.getMinBlockZ();
        int x = baseX + random.nextInt(16);
        int z = baseZ + random.nextInt(16);
        if (config.isGroundOnly()) {
            int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            for (int y = topY; y >= level.getMinY(); y--) {
                BlockPos ground = new BlockPos(x, y, z);
                BlockPos above = ground.above();
                if (BlazingAltarFlamePlacement.isGroundPlacement(level, above)
                        && level.getBlockState(above).isAir()) {
                    return above;
                }
            }
            return null;
        }
        int height = level.getMaxY() - level.getMinY() + 1;
        for (int attempt = 0; attempt < AIR_CANDIDATE_ATTEMPTS; attempt++) {
            int y = level.getMinY() + random.nextInt(height);
            BlockPos candidate = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(candidate);
            if (state.isAir() || state.canBeReplaced()) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide()) {
            return;
        }
        BlazingAltarSpatialIndex.update(level.dimension(), worldPosition, isOperational(), getChunkRadius());
        if (level instanceof ServerLevel serverLevel) {
            attachToActiveJobIfAny(serverLevel);
            BlazingAltarBlockSync.sync(this);
            if (!isOperational()) {
                BlazingAltarFlamePlacement.refreshBrazierFlameLightInRadius(
                        serverLevel, worldPosition, config.getChunkRadius(), config.isGroundOnly());
            }
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide()) {
            prepareForRemoval();
            BlazingAltarSpatialIndex.remove(level.dimension(), worldPosition);
        }
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        config.save(output);
        ItemStack placer = placerHandler.getStackInSlot(PLACER_SLOT);
        if (!placer.isEmpty()) {
            ValueOutput.TypedOutputList<ItemStackWithSlot> placerList = output.list("Placer", ItemStackWithSlot.CODEC);
            placerList.add(new ItemStackWithSlot(PLACER_SLOT, placer));
        } else {
            output.discard("Placer");
        }
        ItemStack module = moduleHandler.getStackInSlot(MODULE_SLOT);
        if (!module.isEmpty()) {
            ValueOutput.TypedOutputList<ItemStackWithSlot> moduleList = output.list("Module", ItemStackWithSlot.CODEC);
            moduleList.add(new ItemStackWithSlot(MODULE_SLOT, module));
        } else {
            output.discard("Module");
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        config.load(input);
        for (ItemStackWithSlot entry : input.listOrEmpty("Placer", ItemStackWithSlot.CODEC)) {
            if (entry.slot() == PLACER_SLOT) {
                placerHandler.setStackInSlot(PLACER_SLOT, entry.stack());
            }
        }
        for (ItemStackWithSlot entry : input.listOrEmpty("Module", ItemStackWithSlot.CODEC)) {
            if (entry.slot() == MODULE_SLOT) {
                moduleHandler.setStackInSlot(MODULE_SLOT, entry.stack());
            }
        }
        clearTransientState();
        clampChunkRadius();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.iska_utils.blazing_altar");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new BlazingAltarMenu(id, playerInventory, this);
    }
}
