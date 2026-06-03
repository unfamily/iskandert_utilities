package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.TypedEntityData;
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
import net.unfamily.iskautils.block.BlazingAltarFlameVisual;
import net.unfamily.iskautils.block.BlazingAltarSpawnMode;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.custom.BlazingAltarBlock;
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

    private final ItemStackHandler placerHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            updateFlameVisual();
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

    private int chunkRadius = 1;
    private boolean groundOnly = true;
    private BlazingAltarSpawnMode spawnMode = BlazingAltarSpawnMode.HOSTILE;
    private int redstoneMode = 0;
    private int tickCounter;
    /** Index into distance-sorted loaded chunks; advances one chunk per placement tick (nearest first). */
    private int placementChunkIndex;
    private boolean extinguishing;
    private int extinguishChunkProgress;
    private int extinguishChunkTotal;
    /** Redstone mode before GUI extinguish started; restored when the job is cancelled. */
    private int redstoneModeBeforeExtinguish = -1;

    public BlazingAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLAZING_ALTAR_BE.get(), pos, state);
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
        return chunkRadius;
    }

    public void setChunkRadius(int radius) {
        this.chunkRadius = Math.max(1, Math.min(radius, getMaxChunkRadius()));
        resetPlacementProgress();
        setChanged();
    }

    private void clampChunkRadius() {
        if (chunkRadius > getMaxChunkRadius()) {
            setChunkRadius(getMaxChunkRadius());
        }
    }

    public void adjustChunkRadius(int delta) {
        setChunkRadius(chunkRadius + delta);
    }

    public boolean isGroundOnly() {
        return groundOnly;
    }

    public void setGroundOnly(boolean groundOnly) {
        this.groundOnly = groundOnly;
        resetPlacementProgress();
        setChanged();
    }

    public void toggleGroundOnly() {
        setGroundOnly(!groundOnly);
    }

    public BlazingAltarSpawnMode getSpawnMode() {
        return spawnMode;
    }

    public void cycleSpawnMode() {
        spawnMode = spawnMode.next();
        setChanged();
    }

    public void cycleSpawnModeBackward() {
        spawnMode = spawnMode.previous();
        setChanged();
    }

    public int getRedstoneMode() {
        return redstoneMode;
    }

    public void cycleRedstoneMode() {
        int next = (redstoneMode + 1) % 5;
        if (next == 3) {
            next = 4;
        }
        redstoneMode = next;
        setChanged();
        updateFlameVisual();
    }

    public void cycleRedstoneModeBackward() {
        redstoneMode = switch (redstoneMode) {
            case 0 -> 4;
            case 1 -> 0;
            case 2 -> 1;
            case 4 -> 2;
            default -> 4;
        };
        setChanged();
        updateFlameVisual();
    }

    /**
     * Schedules progressive removal of mod flames in range (GUI extinguish).
     * Disables the altar and repairs the brazier when the job completes.
     */
    /**
     * Starts progressive flame removal (nearest chunk first), or cancels if already running.
     */
    public void extinguishFlamesInRange(ServerLevel level) {
        if (extinguishing || BlazingAltarExtinguishJobs.hasJob(level, worldPosition)) {
            cancelExtinguishInRange(level);
            return;
        }
        redstoneModeBeforeExtinguish = redstoneMode;
        extinguishing = true;
        redstoneMode = 4;
        resetPlacementProgress();
        extinguishChunkProgress = 0;
        extinguishChunkTotal = BlazingAltarChunks.countInRadius(chunkRadius);
        setChanged();
        updateFlameVisual();
        BlazingAltarExtinguishJobs.enqueueFromAltar(level, this, BlazingAltarExtinguishJobs.FinishMode.FINISH_ALTAR);
    }

    /** Interrupts GUI extinguish; keeps flames already removed, restores pre-job redstone mode. */
    public void cancelExtinguishInRange(ServerLevel level) {
        BlazingAltarExtinguishJobs.cancelForAltar(level, worldPosition);
        extinguishing = false;
        extinguishChunkProgress = 0;
        extinguishChunkTotal = 0;
        if (redstoneModeBeforeExtinguish >= 0) {
            redstoneMode = redstoneModeBeforeExtinguish;
            if (redstoneMode == 3) {
                redstoneMode = 4;
            }
            redstoneModeBeforeExtinguish = -1;
        }
        setChanged();
        updateFlameVisual();
    }

    /** Schedules progressive flame cleanup when the altar block is broken. */
    public void enqueueFlameCleanupOnBreak(ServerLevel level) {
        BlazingAltarExtinguishJobs.enqueueFromAltar(level, this, BlazingAltarExtinguishJobs.FinishMode.FLAMES_ONLY);
    }

    /** Called when a progressive extinguish job completes (GUI action). */
    public void completeExtinguishJob(int normalFlamesRemoved) {
        if (normalFlamesRemoved > 0) {
            ItemStack placer = placerHandler.getStackInSlot(PLACER_SLOT);
            if (placer.is(ModItems.BURNING_BRAZIER.get())) {
                placer.setDamageValue(Math.max(0, placer.getDamageValue() - normalFlamesRemoved));
            }
        }
        redstoneMode = 4;
        extinguishing = false;
        extinguishChunkProgress = 0;
        extinguishChunkTotal = 0;
        redstoneModeBeforeExtinguish = -1;
        resetPlacementProgress();
        setChanged();
        updateFlameVisual();
    }

    public void setExtinguishProgress(int chunksDone, int chunksTotal) {
        this.extinguishChunkProgress = chunksDone;
        this.extinguishChunkTotal = chunksTotal;
        setChanged();
    }

    public int getPlacementChunkProgress() {
        return placementChunkIndex;
    }

    public int getPlacementChunkTotal() {
        return BlazingAltarChunks.countInRadius(chunkRadius);
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
        placementChunkIndex = 0;
    }

    /** Machine active when redstone rules allow (mode 4 = disabled, same as Fan). */
    public boolean isOperational() {
        if (level == null || level.isClientSide()) {
            return false;
        }
        if (extinguishing) {
            return false;
        }
        int mode = Math.max(0, Math.min(redstoneMode, 4));
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
        return switch (spawnMode) {
            case BOTH -> true;
            case HOSTILE -> category == net.minecraft.world.entity.MobCategory.MONSTER;
            case PASSIVE -> category != net.minecraft.world.entity.MobCategory.MONSTER;
        };
    }

    public void updatePoweredState() {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof BlazingAltarBlock)) {
            return;
        }
        boolean powered = isOperational();
        if (state.getValue(BlazingAltarBlock.POWERED) != powered) {
            level.setBlock(worldPosition, state.setValue(BlazingAltarBlock.POWERED, powered), 3);
        }
    }

    public void updateFlameVisual() {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof BlazingAltarBlock)) {
            return;
        }
        BlazingAltarFlameVisual visual = computeFlameVisual();
        if (state.getValue(BlazingAltarBlock.FLAME_VISUAL) != visual) {
            level.setBlock(worldPosition, state.setValue(BlazingAltarBlock.FLAME_VISUAL, visual), 2);
            level.sendBlockUpdated(worldPosition, state, level.getBlockState(worldPosition), 3);
        }
        updatePoweredState();
    }

    private BlazingAltarFlameVisual computeFlameVisual() {
        if (!isOperational()) {
            return BlazingAltarFlameVisual.HIDDEN;
        }
        ItemStack placer = placerHandler.getStackInSlot(PLACER_SLOT);
        if (placer.isEmpty()) {
            return BlazingAltarFlameVisual.GLOW;
        }
        if (placer.is(ModItems.BURNING_BRAZIER.get())) {
            return BlazingAltarFlameVisual.BURNING;
        }
        if (placer.is(ModItems.CURSED_CANDLE.get())) {
            return BlazingAltarFlameVisual.CURSED;
        }
        return BlazingAltarFlameVisual.GLOW;
    }

    public static void tickServer(Level level, BlockPos pos, BlockState state, BlazingAltarBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }
        be.tickCounter++;
        int interval = Config.blazingAltarTickInterval;
        if (be.tickCounter < interval) {
            return;
        }
        be.tickCounter = 0;
        if (!be.isOperational()) {
            be.resetPlacementProgress();
            be.updateFlameVisual();
            return;
        }
        be.tryPlaceFlames((ServerLevel) level);
    }

    private void tryPlaceFlames(ServerLevel level) {
        ItemStack placer = placerHandler.getStackInSlot(PLACER_SLOT);
        if (placer.isEmpty()) {
            return;
        }
        Block flameBlock = BlazingAltarFlamePlacement.flameBlockForPlacer(placer.getItem());
        boolean isBrazier = placer.is(ModItems.BURNING_BRAZIER.get());
        if (isBrazier && placer.getDamageValue() >= BurningBrazierItem.MAX_DURABILITY - 1) {
            return;
        }

        List<ChunkPos> chunksByDistance = BlazingAltarChunks.collectLoadedOrdered(level, worldPosition, chunkRadius);
        if (chunksByDistance.isEmpty()) {
            return;
        }

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
            if (!BlazingAltarFlamePlacement.canPlaceFlameAt(level, candidate, groundOnly, flameState)) {
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

        if (placementChunkIndex < chunksByDistance.size() - 1) {
            placementChunkIndex++;
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
        if (groundOnly) {
            int topY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
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
        if (level != null && !level.isClientSide()) {
            BlazingAltarSpatialIndex.add(level.dimension(), worldPosition);
            updateFlameVisual();
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide()) {
            BlazingAltarSpatialIndex.remove(level.dimension(), worldPosition);
        }
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("ChunkRadius", chunkRadius);
        output.putBoolean("GroundOnly", groundOnly);
        output.putInt("SpawnMode", spawnMode.getId());
        output.putInt("RedstoneMode", redstoneMode);
        output.putInt("TickCounter", tickCounter);
        output.putInt("PlacementChunkIndex", placementChunkIndex);
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
        chunkRadius = input.getIntOr("ChunkRadius", 1);
        if (chunkRadius < 1) {
            chunkRadius = 1;
        }
        groundOnly = input.getBooleanOr("GroundOnly", true);
        spawnMode = BlazingAltarSpawnMode.fromLegacyId(input.getIntOr("SpawnMode", BlazingAltarSpawnMode.HOSTILE.getId()));
        redstoneMode = input.getIntOr("RedstoneMode", 0);
        if (redstoneMode == 3) {
            redstoneMode = 4;
        }
        tickCounter = input.getIntOr("TickCounter", 0);
        placementChunkIndex = Math.max(0, input.getIntOr("PlacementChunkIndex", 0));
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
        clampChunkRadius();
    }

    /** Block item drop: altar settings only; slot contents are dropped in {@link net.unfamily.iskautils.block.custom.BlazingAltarBlock#playerWillDestroy}. */
    public ItemStack createDropStack(BlockState state) {
        ItemStack stack = new ItemStack(state.getBlock());
        if (!hasConfigPersistedState()) {
            return stack;
        }
        CompoundTag data = new CompoundTag();
        data.putInt("ChunkRadius", chunkRadius);
        data.putBoolean("GroundOnly", groundOnly);
        data.putInt("SpawnMode", spawnMode.getId());
        data.putInt("RedstoneMode", redstoneMode);
        data.putInt("TickCounter", tickCounter);
        data.putInt("PlacementChunkIndex", placementChunkIndex);
        stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(getType(), data));
        return stack;
    }

    private boolean hasConfigPersistedState() {
        return chunkRadius != 1
                || !groundOnly
                || spawnMode != BlazingAltarSpawnMode.HOSTILE
                || redstoneMode != 0
                || tickCounter != 0
                || placementChunkIndex != 0;
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
