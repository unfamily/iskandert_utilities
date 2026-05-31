package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.Containers;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.unfamily.iskalib.transfer.LegacyIFluidHandlerResourceHandler;
import net.unfamily.iskalib.transfer.LegacyItemHandlerResourceHandler;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.fluid.ModFluids;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.util.CollectingCrateMode;
import net.unfamily.iskautils.util.ExperienceFluidMath;

import java.util.List;

public class CollectingCrateBlockEntity extends BlockEntity implements MenuProvider {
    public static final TagKey<net.minecraft.world.level.material.Fluid> EXPERIENCE_FLUID_TAG =
            TagKey.create(Registries.FLUID, Identifier.fromNamespaceAndPath("c", "experience"));

    private CollectingCrateMode collectMode = CollectingCrateMode.BOTH;
    private int redstoneMode = 0;
    private int tickCounter = 0;

    private final ItemStackHandler storageHandler;
    private final ItemStackHandler moduleHandler;
    private final FluidTank experienceTank;
    private final ResourceHandler<ItemResource> itemTransferHandler;
    private final ResourceHandler<FluidResource> fluidTransferHandler;

    public CollectingCrateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COLLECTING_CRATE_BE.get(), pos, state);
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
        this.itemTransferHandler = LegacyItemHandlerResourceHandler.wrap(storageHandler);
        this.fluidTransferHandler = LegacyIFluidHandlerResourceHandler.wrap(experienceTank);
    }

    public ItemStackHandler getStorageHandler() {
        return storageHandler;
    }

    public ItemStackHandler getModuleHandler() {
        return moduleHandler;
    }

    public ResourceHandler<ItemResource> getItemTransferHandler() {
        return itemTransferHandler;
    }

    public ResourceHandler<FluidResource> getFluidTransferHandler() {
        return fluidTransferHandler;
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

    public int getEffectiveRange() {
        int modules = Math.min(countRangeModules(), Config.collectingCrateRangeUpgradeMax);
        int base = Config.collectingCrateBaseRange;
        int max = Config.collectingCrateMaxRange;
        return Math.min(base + (modules * 3) / 2, max);
    }

    public int getStoredXpMb() {
        return experienceTank.getFluidAmount();
    }

    public int getStoredXpPoints() {
        return ExperienceFluidMath.xpPointsFromMb(getStoredXpMb());
    }

    public void collectAllXpToPlayer(Player player) {
        if (level == null || level.isClientSide() || experienceTank.isEmpty()) {
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
        if (level == null || level.isClientSide()) {
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
        FluidStack stack = new FluidStack(ModFluids.CONDENSED_KNOWLEDGE.getSource(), mb);
        experienceTank.fill(stack, IFluidHandler.FluidAction.EXECUTE);
        player.giveExperiencePoints(-toStore);
        setChanged();
    }

    public void drops() {
        if (level == null || level.isClientSide()) {
            return;
        }
        for (int i = 0; i < storageHandler.getSlots(); i++) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                    storageHandler.getStackInSlot(i));
        }
        for (int i = 0; i < moduleHandler.getSlots(); i++) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                    moduleHandler.getStackInSlot(i));
        }
        int points = getStoredXpPoints();
        if (points > 0) {
            ExperienceOrb.award((ServerLevel) level, worldPosition.getCenter(), points);
            experienceTank.setFluid(FluidStack.EMPTY);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CollectingCrateBlockEntity blockEntity) {
        if (level.isClientSide()) {
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
        int range = getEffectiveRange();
        AABB area = AABB.encapsulatingFullBlocks(
                pos.offset(-range, -range, -range),
                pos.offset(range, range, range));
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
            FluidStack stack = new FluidStack(ModFluids.CONDENSED_KNOWLEDGE.getSource(), mb);
            int filled = experienceTank.fill(stack, IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) {
                orb.discard();
                setChanged();
            }
        }
    }

    private void collectItems(Level level, BlockPos pos) {
        int range = getEffectiveRange();
        AABB area = AABB.encapsulatingFullBlocks(
                pos.offset(-range, -range, -range),
                pos.offset(range, range, range));
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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        collectMode = CollectingCrateMode.fromId(input.getIntOr("CollectMode", CollectingCrateMode.BOTH.getId()));
        redstoneMode = input.getIntOr("RedstoneMode", 0);
        if (redstoneMode == 3) {
            redstoneMode = 4;
        }
        for (ItemStackWithSlot entry : input.listOrEmpty("Storage", ItemStackWithSlot.CODEC)) {
            if (entry.slot() >= 0 && entry.slot() < storageHandler.getSlots()) {
                storageHandler.setStackInSlot(entry.slot(), entry.stack());
            }
        }
        for (ItemStackWithSlot entry : input.listOrEmpty("Module", ItemStackWithSlot.CODEC)) {
            if (entry.slot() == 0) {
                moduleHandler.setStackInSlot(0, entry.stack());
            }
        }
        int mb = input.getIntOr("StoredXpMb", 0);
        if (mb > 0 && ModFluids.CONDENSED_KNOWLEDGE != null) {
            experienceTank.setFluid(new FluidStack(ModFluids.CONDENSED_KNOWLEDGE.getSource(), mb));
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("CollectMode", collectMode.getId());
        output.putInt("RedstoneMode", redstoneMode);
        output.putInt("StoredXpMb", experienceTank.getFluidAmount());

        ValueOutput.TypedOutputList<ItemStackWithSlot> storageList = output.list("Storage", ItemStackWithSlot.CODEC);
        for (int i = 0; i < storageHandler.getSlots(); i++) {
            ItemStack stack = storageHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                storageList.add(new ItemStackWithSlot(i, stack));
            }
        }
        if (storageList.isEmpty()) {
            output.discard("Storage");
        }

        ItemStack moduleStack = moduleHandler.getStackInSlot(0);
        if (!moduleStack.isEmpty()) {
            ValueOutput.TypedOutputList<ItemStackWithSlot> moduleList = output.list("Module", ItemStackWithSlot.CODEC);
            moduleList.add(new ItemStackWithSlot(0, moduleStack));
        } else {
            output.discard("Module");
        }
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
