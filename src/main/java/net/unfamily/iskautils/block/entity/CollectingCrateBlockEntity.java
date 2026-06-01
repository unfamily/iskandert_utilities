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
    public static final TagKey<net.minecraft.world.level.material.Fluid> EXPERIENCE_FLUID_TAG =
            TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", "experience"));

    private static final int MIN_HEIGHT_OR_DEPTH = 0;

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
    private final FluidTank experienceTank;
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
        this.experienceTank = new ExperienceTank();
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

    public FluidTank getExperienceTank() {
        return experienceTank;
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

    public int getStoredXpMb() {
        return experienceTank.getFluidAmount();
    }

    public int getStoredXpPoints() {
        return ExperienceFluidMath.xpPointsFromMb(getStoredXpMb());
    }

    public void collectAllXpToPlayer(Player player) {
        if (level == null || level.isClientSide || experienceTank.isEmpty()) {
            return;
        }
        int points = getStoredXpPoints();
        if (points > 0) {
            player.giveExperiencePoints(points);
            experienceTank.setFluid(FluidStack.EMPTY);
            setChanged();
        }
    }

    public void depositAllXpFromPlayer(Player player) {
        if (level == null || level.isClientSide) {
            return;
        }
        long totalXp = ExperienceFluidMath.levelsToXp(player.experienceLevel)
                + Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
        if (totalXp <= 0) {
            return;
        }
        int spacePoints = ExperienceFluidMath.xpPointsFromMb(experienceTank.getSpace());
        int toStore = (int) Math.min(totalXp, spacePoints);
        if (toStore <= 0) {
            return;
        }
        int mb = ExperienceFluidMath.mbFromXpPoints(toStore);
        FluidStack stack = new FluidStack(ModFluids.CONDENSED_KNOWLEDGE_SOURCE.get(), mb);
        experienceTank.fill(stack, IFluidHandler.FluidAction.EXECUTE);
        player.giveExperiencePoints(-toStore);
        setChanged();
    }

    public void drops() {
        if (level == null || level.isClientSide) {
            return;
        }
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

    /** Block item drop: keeps stored XP; inventory is dropped separately in {@link #drops()}. */
    public ItemStack createDropStack(BlockState state) {
        ItemStack stack = new ItemStack(state.getBlock());
        int storedMb = experienceTank.getFluidAmount();
        if (storedMb > 0) {
            CompoundTag data = new CompoundTag();
            data.putInt("StoredXpMb", storedMb);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        }
        return stack;
    }

    /** Restores XP tank from a broken-crate item tag (1.21.1 {@link net.unfamily.iskautils.item.custom.CollectingCrateBlockItem}). */
    public void loadStoredXpFromDropTag(CompoundTag tag) {
        if (tag.contains("StoredXpMb")) {
            int mb = tag.getInt("StoredXpMb");
            if (mb > 0) {
                experienceTank.setFluid(new FluidStack(ModFluids.CONDENSED_KNOWLEDGE_SOURCE.get(), mb));
                setChanged();
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CollectingCrateBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }
        if (blockEntity.redstoneMode == 3) {
            blockEntity.redstoneMode = 4;
        }
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
        if (experienceTank.getSpace() < ExperienceFluidMath.mbPerXpPoint()) {
            return;
        }
        AABB area = getCollectionAABB();
        List<ExperienceOrb> orbs = level.getEntitiesOfClass(ExperienceOrb.class, area);
        for (ExperienceOrb orb : orbs) {
            if (experienceTank.getSpace() < ExperienceFluidMath.mbPerXpPoint()) {
                break;
            }
            int value = orb.getValue();
            if (value <= 0) {
                continue;
            }
            int mb = ExperienceFluidMath.mbFromXpPoints(value);
            FluidStack stack = new FluidStack(ModFluids.CONDENSED_KNOWLEDGE_SOURCE.get(), mb);
            int filled = experienceTank.fill(stack, IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) {
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
        if (tag.contains("ExperienceTank")) {
            experienceTank.readFromNBT(registries, tag.getCompound("ExperienceTank"));
        } else if (tag.contains("StoredXpMb")) {
            int mb = tag.getInt("StoredXpMb");
            if (mb > 0) {
                experienceTank.setFluid(new FluidStack(ModFluids.CONDENSED_KNOWLEDGE_SOURCE.get(), mb));
            }
        }
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
        tag.putInt("StoredXpMb", experienceTank.getFluidAmount());
    }

    private final class ExperienceTank extends FluidTank {
        ExperienceTank() {
            super(ExperienceFluidMath.capacityMbFromLevels(Config.collectingCrateXpCapacityLevels));
        }

        @Override
        public boolean isFluidValid(FluidStack stack) {
            if (stack.isEmpty()) {
                return true;
            }
            return stack.getFluid().is(EXPERIENCE_FLUID_TAG);
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    }
}
