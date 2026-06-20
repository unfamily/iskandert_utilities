package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.Containers;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.fluid.ModFluids;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.util.CollectingCrateAreaLogic;
import net.unfamily.iskautils.util.CollectingCrateMode;
import net.unfamily.iskautils.util.ExperienceFluidMath;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CollectingCrateBlockEntity extends BlockEntity implements MenuProvider {
    public static final TagKey<Fluid> EXPERIENCE_FLUID_TAG =
            TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", "experience"));

    private static final int MIN_HEIGHT_OR_DEPTH = 0;
    private static final int TANK_INSERTION = 0;
    private static final int TANK_ACCUMULATION = 1;
    private static final int KNOWLEDGE_COMPRESSOR_TRANSFER_MB_PER_TICK = 1000;

    private CollectingCrateMode collectMode = CollectingCrateMode.BOTH;
    private int redstoneMode = 0;
    private int tickCounter = 0;
    private int sizeLeft = 2;
    private int sizeRight = 2;
    private int sizeHeight = 4;
    private int sizeDepth = 4;
    private boolean previewEnabled = false;

    private final ItemStackHandler storageHandler;
    private final ItemStackHandler moduleHandler;
    private final InsertionBufferTank insertionBufferTank;
    private final AccumulationTank accumulationTank;
    private final CrateFluidHandler fluidHandler;
    private final IItemHandler automationItemHandler;

    public CollectingCrateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COLLECTING_CRATE.get(), pos, state);
        int storageSlots = Math.max(1, Config.collectingCrateStorageSlots);
        this.storageHandler = new ItemStackHandler(storageSlots) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
        this.moduleHandler = new ItemStackHandler(1) {
            @Override
            protected void onContentsChanged(int slot) {
                clampSizesToMax();
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.is(ModItems.RANGE_MODULE.get());
            }

            @Override
            public int getSlotLimit(int slot) {
                return Config.collectingCrateRangeUpgradeMax;
            }
        };
        this.insertionBufferTank = new InsertionBufferTank();
        this.accumulationTank = new AccumulationTank();
        this.fluidHandler = new CrateFluidHandler();
        this.automationItemHandler = storageHandler;
    }

    public ItemStackHandler getStorageHandler() {
        return storageHandler;
    }

    public ItemStackHandler getModuleHandler() {
        return moduleHandler;
    }

    public IItemHandler getAutomationItemHandler() {
        return automationItemHandler;
    }

    /** Automation / pipes: insert into buffer, extract from accumulation. */
    public IFluidHandler getFluidHandler() {
        return fluidHandler;
    }

    /** @deprecated Use {@link #getInsertionBufferTank()} — kept for any external callers. */
    @Deprecated
    public FluidTank getExperienceTank() {
        return insertionBufferTank;
    }

    public FluidTank getInsertionBufferTank() {
        return insertionBufferTank;
    }

    public FluidTank getAccumulationTank() {
        return accumulationTank;
    }

    public CollectingCrateMode getCollectMode() {
        return collectMode;
    }

    public void setCollectMode(CollectingCrateMode mode) {
        this.collectMode = mode != null ? mode : CollectingCrateMode.BOTH;
        setChanged();
    }

    public void cycleCollectMode(boolean backward) {
        setCollectMode(collectMode.cycle(backward));
    }

    public int getRedstoneMode() {
        return Math.max(0, Math.min(redstoneMode, 4));
    }

    public void setRedstoneMode(int mode) {
        int newMode = Math.max(0, Math.min(mode, 4));
        if (redstoneMode != newMode) {
            redstoneMode = newMode;
            setChanged();
        }
    }

    public void cycleRedstoneMode() {
        setRedstoneMode((getRedstoneMode() + 1) % 5);
    }

    public void cycleRedstoneModeBackward() {
        int mode = getRedstoneMode();
        setRedstoneMode(mode == 0 ? 4 : mode - 1);
    }

    public int countRangeModules() {
        return moduleHandler.getStackInSlot(0).getCount();
    }

    public int getSizeLeft() {
        return sizeLeft;
    }

    public int getSizeRight() {
        return sizeRight;
    }

    public int getSizeHeight() {
        return sizeHeight;
    }

    public int getSizeDepth() {
        return sizeDepth;
    }

    public boolean isPreviewEnabled() {
        return previewEnabled;
    }

    public void setPreviewEnabled(boolean previewEnabled) {
        if (this.previewEnabled != previewEnabled) {
            this.previewEnabled = previewEnabled;
            setChanged();
        }
    }

    public int getMaxBlockCount() {
        int modules = Math.min(countRangeModules(), Config.collectingCrateRangeUpgradeMax);
        int base = Config.collectingCrateBaseRange;
        int cap = Config.collectingCrateMaxRange;
        if (modules <= 0 || Config.collectingCrateRangeUpgradeMax <= 0) {
            return base;
        }
        return base + (modules * (cap - base)) / Config.collectingCrateRangeUpgradeMax;
    }

    public int getMaxHeight() {
        return Math.max(MIN_HEIGHT_OR_DEPTH, getMaxBlockCount() - 1);
    }

    public int getMaxDepth() {
        return Math.max(MIN_HEIGHT_OR_DEPTH, getMaxBlockCount() - 1);
    }

    public int getMaxWidth() {
        return Math.max(MIN_HEIGHT_OR_DEPTH, getMaxBlockCount() - 1);
    }

    public void adjustSize(int direction, boolean increment, int amount) {
        int delta = increment ? amount : -amount;
        switch (direction) {
            case 0 -> sizeHeight = Math.max(MIN_HEIGHT_OR_DEPTH, Math.min(getMaxHeight(), sizeHeight + delta));
            case 1 -> {
                int newR = Math.max(0, Math.min(getMaxWidth() - sizeLeft, sizeRight + delta));
                sizeRight = newR;
                if (sizeLeft + sizeRight < 0) {
                    sizeLeft = 0;
                    sizeRight = 0;
                }
            }
            case 2 -> {
                int newL = Math.max(0, Math.min(getMaxWidth() - sizeRight, sizeLeft + delta));
                sizeLeft = newL;
                if (sizeLeft + sizeRight < 0) {
                    sizeLeft = 0;
                    sizeRight = 0;
                }
            }
            case 3 -> sizeDepth = Math.max(MIN_HEIGHT_OR_DEPTH, Math.min(getMaxDepth(), sizeDepth + delta));
            default -> {}
        }
        setChanged();
    }

    public void clampSizesToMax() {
        sizeLeft = Math.max(0, Math.min(getMaxWidth() - sizeRight, sizeLeft));
        sizeRight = Math.max(0, Math.min(getMaxWidth() - sizeLeft, sizeRight));
        sizeHeight = Math.max(MIN_HEIGHT_OR_DEPTH, Math.min(getMaxHeight(), sizeHeight));
        sizeDepth = Math.max(MIN_HEIGHT_OR_DEPTH, Math.min(getMaxDepth(), sizeDepth));
    }

    public AABB getCollectionAABB() {
        if (level == null) {
            return new AABB(worldPosition);
        }
        Direction facing = level.getBlockState(worldPosition).getValue(HorizontalDirectionalBlock.FACING);
        return CollectingCrateAreaLogic.getCollectionVolumeAABB(
                worldPosition, facing, sizeLeft, sizeRight, sizeHeight, sizeDepth);
    }

    /** Mb in accumulation (player-facing storage). */
    public long getAccumulationMb() {
        return accumulationTank.getStoredMb();
    }

    /** Mb waiting in insertion buffer (any c:experience fluid). */
    public int getInsertionBufferMb() {
        return insertionBufferTank.getFluidAmount();
    }

    /** Total mb for GUI bar: accumulation + our fluid in buffer. */
    public long getStoredXpMb() {
        return getAccumulationMb() + getOurFluidMbInBuffer();
    }

    public long getStoredXpPoints() {
        return ExperienceFluidMath.xpPointsFromMb(getStoredXpMb());
    }

    private int getOurFluidMbInBuffer() {
        FluidStack buffer = insertionBufferTank.getFluid();
        if (buffer.isEmpty() || buffer.getFluid() != ModFluids.CONDENSED_KNOWLEDGE_SOURCE.get()) {
            return 0;
        }
        return buffer.getAmount();
    }

    private static long accumulationCapacityMb() {
        return ExperienceFluidMath.capacityMbFromLevels(Config.collectingCrateXpCapacityLevels);
    }

    /** Staging buffer uses int fluid amounts; cap matches accumulation config (truncated at int max per transfer). */
    private static int bufferTankCapacityMb() {
        long cap = accumulationCapacityMb();
        return cap > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) cap;
    }

    private boolean canAcceptInsertionBufferFill() {
        return accumulationTank.getSpaceMb() > 0;
    }

    public void collectAllXpToPlayer(Player player) {
        if (level == null || level.isClientSide) {
            return;
        }
        processInsertionBuffer();
        long points = ExperienceFluidMath.xpPointsFromMb(accumulationTank.getStoredMb());
        if (points <= 0) {
            return;
        }
        while (points > 0) {
            int chunk = (int) Math.min(points, Integer.MAX_VALUE);
            player.giveExperiencePoints(chunk);
            points -= chunk;
        }
        accumulationTank.setStoredMb(0);
        setChanged();
    }

    public void depositAllXpFromPlayer(Player player) {
        if (level == null || level.isClientSide) {
            return;
        }
        processInsertionBuffer();
        long totalXp = ExperienceFluidMath.levelsToXp(player.experienceLevel)
                + Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
        if (totalXp <= 0) {
            return;
        }
        long spacePoints = ExperienceFluidMath.xpPointsFromMb(accumulationTank.getSpaceMb());
        long toStore = Math.min(totalXp, spacePoints);
        if (toStore <= 0) {
            return;
        }
        long mbRemaining = ExperienceFluidMath.mbFromXpPoints(toStore);
        long filledTotal = 0;
        while (mbRemaining > 0) {
            int chunk = (int) Math.min(mbRemaining, Integer.MAX_VALUE);
            FluidStack stack = new FluidStack(ModFluids.CONDENSED_KNOWLEDGE_SOURCE.get(), chunk);
            int filled = accumulationTank.fill(stack, IFluidHandler.FluidAction.EXECUTE);
            if (filled <= 0) {
                break;
            }
            filledTotal += filled;
            mbRemaining -= filled;
        }
        long storedPoints = ExperienceFluidMath.xpPointsFromMb(filledTotal);
        if (storedPoints > 0) {
            long remaining = storedPoints;
            while (remaining > 0) {
                int chunk = (int) Math.min(remaining, Integer.MAX_VALUE);
                player.giveExperiencePoints(-chunk);
                remaining -= chunk;
            }
            setChanged();
        }
    }

    public void drops() {
        if (level == null || level.isClientSide) {
            return;
        }
        processInsertionBuffer();
        for (int i = 0; i < storageHandler.getSlots(); i++) {
            ItemStack stack = storageHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
                storageHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        for (int i = 0; i < moduleHandler.getSlots(); i++) {
            ItemStack stack = moduleHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
                moduleHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        setChanged();
    }

    public ItemStack createDropStack(BlockState state) {
        ItemStack stack = new ItemStack(state.getBlock());
        long storedMb = getTotalStoredMbForPersistence();
        if (storedMb > 0) {
            CompoundTag data = new CompoundTag();
            data.putLong("StoredXpMb", storedMb);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        }
        return stack;
    }

    public void loadStoredXpFromDropTag(CompoundTag tag) {
        long mb = readMbTag(tag, "StoredXpMb");
        if (mb > 0) {
            insertionBufferTank.setFluid(new FluidStack(ModFluids.CONDENSED_KNOWLEDGE_SOURCE.get(),
                    (int) Math.min(mb, Integer.MAX_VALUE)));
            setChanged();
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CollectingCrateBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }
        if (blockEntity.redstoneMode == 3) {
            blockEntity.redstoneMode = 4;
        }
        blockEntity.processInsertionBuffer();
        blockEntity.pushExperienceToAdjacentKnowledgeCompressors(level);
        blockEntity.tickCounter++;
        if (blockEntity.tickCounter < Config.collectingCrateCollectionIntervalTicks) {
            return;
        }
        blockEntity.tickCounter = 0;
        if (!blockEntity.isCollectionAllowed()) {
            return;
        }
        if (blockEntity.collectMode.collectsExperience()) {
            blockEntity.collectExperience(level, pos);
        }
        if (blockEntity.collectMode.collectsItems()) {
            blockEntity.collectItems(level, pos);
        }
        blockEntity.processInsertionBuffer();
    }

    private void pushExperienceToAdjacentKnowledgeCompressors(Level level) {
        if (level == null) {
            return;
        }
        if (accumulationTank.getStoredMb() <= 0) {
            return;
        }
        for (Direction dir : Direction.values()) {
            BlockPos otherPos = worldPosition.relative(dir);
            if (!(level.getBlockEntity(otherPos) instanceof KnowledgeCompressorBlockEntity compressor)) {
                continue;
            }
            IFluidHandler target = compressor.getFluidHandler();

            FluidStack canDrain = accumulationTank.drain(KNOWLEDGE_COMPRESSOR_TRANSFER_MB_PER_TICK, IFluidHandler.FluidAction.SIMULATE);
            if (canDrain.isEmpty()) {
                continue;
            }
            int accepted = target.fill(canDrain, IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0) {
                continue;
            }
            FluidStack drained = accumulationTank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
            if (drained.isEmpty()) {
                continue;
            }
            target.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            setChanged();
            if (accumulationTank.getStoredMb() <= 0) {
                return;
            }
        }
    }

    private void processInsertionBuffer() {
        if (insertionBufferTank.isEmpty() || accumulationTank.getSpaceMb() <= 0) {
            return;
        }
        FluidStack buffer = insertionBufferTank.getFluid();
        if (buffer.isEmpty()) {
            return;
        }
        int toMove = (int) Math.min(buffer.getAmount(), accumulationTank.getSpaceMb());
        if (toMove <= 0) {
            return;
        }
        FluidStack out = new FluidStack(ModFluids.CONDENSED_KNOWLEDGE_SOURCE.get(), toMove);
        int filled = accumulationTank.fill(out, IFluidHandler.FluidAction.EXECUTE);
        if (filled > 0) {
            insertionBufferTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
            setChanged();
        }
    }

    private boolean isCollectionAllowed() {
        if (level == null) {
            return false;
        }
        int mode = getRedstoneMode();
        int signal = level.getBestNeighborSignal(worldPosition);
        boolean powered = signal > 0;
        return switch (mode) {
            case 0 -> true;
            case 1 -> !powered;
            case 2 -> powered;
            case 4 -> false;
            default -> true;
        };
    }

    private void collectExperience(Level level, BlockPos pos) {
        if (!canAcceptInsertionBufferFill() || insertionBufferTank.getSpace() < ExperienceFluidMath.mbPerXpPoint()) {
            return;
        }
        AABB area = getCollectionAABB();
        List<ExperienceOrb> orbs = level.getEntitiesOfClass(ExperienceOrb.class, area);
        for (ExperienceOrb orb : orbs) {
            if (!canAcceptInsertionBufferFill() || insertionBufferTank.getSpace() < ExperienceFluidMath.mbPerXpPoint()) {
                break;
            }
            int value = orb.getValue();
            if (value <= 0) {
                continue;
            }
            int mb = (int) Math.min(ExperienceFluidMath.mbFromXpPoints(value), Integer.MAX_VALUE);
            FluidStack stack = new FluidStack(ModFluids.CONDENSED_KNOWLEDGE_SOURCE.get(), mb);
            int filled = insertionBufferTank.fill(stack, IFluidHandler.FluidAction.EXECUTE);
            if (filled >= mb) {
                orb.discard();
                setChanged();
            }
        }
    }

    private void collectItems(Level level, BlockPos pos) {
        AABB area = getCollectionAABB();
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, area);
        if (items.isEmpty()) {
            return;
        }
        int remaining = Config.collectingCrateMaxInsertionsPerTick <= 0
                ? items.size()
                : Config.collectingCrateMaxInsertionsPerTick;
        for (ItemEntity entity : items) {
            if (remaining <= 0) {
                break;
            }
            if (!entity.isAlive()) {
                continue;
            }
            if (entity.getPersistentData().contains("PreventRemoteMovement")
                    && !entity.getPersistentData().contains("AllowMachineRemoteMovement")) {
                continue;
            }
            ItemStack stack = entity.getItem();
            ItemStack remainder = insertItemStack(stack.copy());
            int inserted = stack.getCount() - remainder.getCount();
            if (inserted > 0) {
                stack.shrink(inserted);
                entity.setItem(stack);
                if (stack.isEmpty()) {
                    entity.discard();
                }
                remaining--;
                setChanged();
            }
        }
    }

    private ItemStack insertItemStack(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < storageHandler.getSlots(); i++) {
            remaining = storageHandler.insertItem(i, remaining, false);
            if (remaining.isEmpty()) {
                break;
            }
        }
        return remaining;
    }

    private long getTotalStoredMbForPersistence() {
        return getAccumulationMb() + insertionBufferTank.getFluidAmount();
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        collectMode = CollectingCrateMode.fromId(tag.getInt("CollectMode"));
        redstoneMode = tag.getInt("RedstoneMode");
        if (redstoneMode == 3) {
            redstoneMode = 4;
        }
        if (tag.contains("SizeLeft")) {
            sizeLeft = tag.getInt("SizeLeft");
        }
        if (tag.contains("SizeRight")) {
            sizeRight = tag.getInt("SizeRight");
        }
        if (tag.contains("SizeHeight")) {
            sizeHeight = tag.getInt("SizeHeight");
        }
        if (tag.contains("SizeDepth")) {
            sizeDepth = tag.getInt("SizeDepth");
        }
        if (tag.contains("PreviewEnabled")) {
            previewEnabled = tag.getBoolean("PreviewEnabled");
        }
        if (tag.contains("Storage")) {
            storageHandler.deserializeNBT(registries, tag.getCompound("Storage"));
        }
        if (tag.contains("Module")) {
            moduleHandler.deserializeNBT(registries, tag.getCompound("Module"));
        }
        clampSizesToMax();
        if (tag.contains("InsertionBufferTank")) {
            insertionBufferTank.readFromNBT(registries, tag.getCompound("InsertionBufferTank"));
        } else if (tag.contains("ExperienceTank")) {
            insertionBufferTank.readFromNBT(registries, tag.getCompound("ExperienceTank"));
        }
        loadXpMbFromTag(tag, registries);
    }

    private void loadXpMbFromTag(CompoundTag tag, HolderLookup.Provider registries) {
        long accumMb = readMbTag(tag, "AccumulationMb");
        if (accumMb >= 0) {
            accumulationTank.setStoredMb(accumMb);
        } else if (tag.contains("AccumulationTank")) {
            accumulationTank.readFromNBT(registries, tag.getCompound("AccumulationTank"));
            accumulationTank.importLegacyFluidAmount();
        } else {
            long legacy = readMbTag(tag, "StoredXpMb");
            if (legacy > 0) {
                insertionBufferTank.setFluid(new FluidStack(ModFluids.CONDENSED_KNOWLEDGE_SOURCE.get(),
                        (int) Math.min(legacy, Integer.MAX_VALUE)));
            }
        }
    }

    private static long readMbTag(CompoundTag tag, String key) {
        if (tag.contains(key, net.minecraft.nbt.Tag.TAG_LONG)) {
            return tag.getLong(key);
        }
        if (tag.contains(key, net.minecraft.nbt.Tag.TAG_INT)) {
            return tag.getInt(key);
        }
        return -1;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.iska_utils.collecting_crate");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new net.unfamily.iskautils.client.gui.CollectingCrateMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("CollectMode", collectMode.getId());
        tag.putInt("RedstoneMode", redstoneMode);
        tag.putInt("SizeLeft", sizeLeft);
        tag.putInt("SizeRight", sizeRight);
        tag.putInt("SizeHeight", sizeHeight);
        tag.putInt("SizeDepth", sizeDepth);
        tag.putBoolean("PreviewEnabled", previewEnabled);
        tag.put("Storage", storageHandler.serializeNBT(registries));
        tag.put("Module", moduleHandler.serializeNBT(registries));
        tag.put("InsertionBufferTank", insertionBufferTank.writeToNBT(registries, new CompoundTag()));
        tag.putLong("AccumulationMb", accumulationTank.getStoredMb());
        tag.putLong("StoredXpMb", getTotalStoredMbForPersistence());
    }

    private final class InsertionBufferTank extends FluidTank {
        InsertionBufferTank() {
            super(bufferTankCapacityMb());
        }

        @Override
        public boolean isFluidValid(FluidStack stack) {
            if (stack.isEmpty()) {
                return true;
            }
            if (!canAcceptInsertionBufferFill()) {
                return false;
            }
            return stack.getFluid().is(EXPERIENCE_FLUID_TAG);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!canAcceptInsertionBufferFill()) {
                return 0;
            }
            return super.fill(resource, action);
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    }

    private final class AccumulationTank extends FluidTank {
        private long storedMb;

        AccumulationTank() {
            super(Integer.MAX_VALUE);
        }

        long getStoredMb() {
            return storedMb;
        }

        long getCapacityMb() {
            return accumulationCapacityMb();
        }

        long getSpaceMb() {
            return Math.max(0, getCapacityMb() - storedMb);
        }

        void setStoredMb(long mb) {
            storedMb = Math.min(Math.max(0, mb), getCapacityMb());
            syncInternalFluid();
            setChanged();
        }

        void importLegacyFluidAmount() {
            storedMb = Math.min(getFluid().getAmount(), getCapacityMb());
            syncInternalFluid();
        }

        private void syncInternalFluid() {
            if (storedMb <= 0) {
                setFluid(FluidStack.EMPTY);
            } else {
                int amount = (int) Math.min(storedMb, Integer.MAX_VALUE);
                setFluid(new FluidStack(ModFluids.CONDENSED_KNOWLEDGE_SOURCE.get(), amount));
            }
        }

        @Override
        public int getFluidAmount() {
            return storedMb > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) storedMb;
        }

        @Override
        public int getSpace() {
            long space = getSpaceMb();
            return space > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) space;
        }

        @Override
        public int getCapacity() {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(FluidStack stack) {
            if (stack.isEmpty()) {
                return true;
            }
            return stack.getFluid() == ModFluids.CONDENSED_KNOWLEDGE_SOURCE.get();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!isFluidValid(resource) || resource.isEmpty()) {
                return 0;
            }
            long space = getSpaceMb();
            if (space <= 0) {
                return 0;
            }
            int toFill = (int) Math.min(Math.min(resource.getAmount(), space), Integer.MAX_VALUE);
            if (toFill <= 0) {
                return 0;
            }
            if (action == FluidAction.EXECUTE) {
                storedMb += toFill;
                syncInternalFluid();
                onContentsChanged();
            }
            return toFill;
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            if (storedMb <= 0) {
                return FluidStack.EMPTY;
            }
            int toDrain = (int) Math.min(Math.min(maxDrain, storedMb), Integer.MAX_VALUE);
            if (toDrain <= 0) {
                return FluidStack.EMPTY;
            }
            FluidStack drained = new FluidStack(ModFluids.CONDENSED_KNOWLEDGE_SOURCE.get(), toDrain);
            if (action == FluidAction.EXECUTE) {
                storedMb -= toDrain;
                syncInternalFluid();
                onContentsChanged();
            }
            return drained;
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || resource.getFluid() != ModFluids.CONDENSED_KNOWLEDGE_SOURCE.get()) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    }

    private final class CrateFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case TANK_INSERTION -> insertionBufferTank.getFluid();
                case TANK_ACCUMULATION -> accumulationTank.getFluid();
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tank) {
                case TANK_INSERTION -> insertionBufferTank.getCapacity();
                case TANK_ACCUMULATION -> {
                    long cap = accumulationCapacityMb();
                    yield cap > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) cap;
                }
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return switch (tank) {
                case TANK_INSERTION -> insertionBufferTank.isFluidValid(stack);
                case TANK_ACCUMULATION -> accumulationTank.isFluidValid(stack);
                default -> false;
            };
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            return insertionBufferTank.fill(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || resource.getFluid() != ModFluids.CONDENSED_KNOWLEDGE_SOURCE.get()) {
                return FluidStack.EMPTY;
            }
            return accumulationTank.drain(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            return accumulationTank.drain(maxDrain, action);
        }
    }
}
