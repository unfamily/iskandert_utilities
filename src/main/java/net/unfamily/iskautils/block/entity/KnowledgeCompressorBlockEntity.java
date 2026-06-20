package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.KnowledgeCompressorBlock;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.util.ExperienceFluidMath;
import org.jetbrains.annotations.NotNull;

public class KnowledgeCompressorBlockEntity extends BlockEntity {
    public static final TagKey<Fluid> EXPERIENCE_FLUID_TAG =
            TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", "experience"));

    private final ExperienceTank experienceTank;
    private final ItemStackHandler outputHandler;
    private final IFluidHandler fluidHandler;
    private final IItemHandler itemHandler;
    private int conversionCooldown;

    public KnowledgeCompressorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.KNOWLEDGE_COMPRESSOR.get(), pos, state);
        this.experienceTank = new ExperienceTank();
        this.outputHandler = new ItemStackHandler(1) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, net.minecraft.world.item.ItemStack stack) {
                return stack.is(ModItems.JELLY_OF_KNOWLEDGE.get());
            }
        };
        this.fluidHandler = new CompressorFluidHandler();
        this.itemHandler = outputHandler;
    }

    public IFluidHandler getFluidHandler() {
        return fluidHandler;
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, KnowledgeCompressorBlockEntity blockEntity) {
        if (state.getValue(KnowledgeCompressorBlock.POWERED)) {
            return;
        }
        if (blockEntity.conversionCooldown > 0) {
            blockEntity.conversionCooldown--;
            return;
        }
        if (!blockEntity.outputHandler.getStackInSlot(0).isEmpty()) {
            return;
        }
        int cost = ExperienceFluidMath.jellyMbCost();
        if (blockEntity.experienceTank.getFluidAmount() < cost) {
            return;
        }
        blockEntity.experienceTank.drain(cost, IFluidHandler.FluidAction.EXECUTE);
        blockEntity.outputHandler.setStackInSlot(0, ModItems.JELLY_OF_KNOWLEDGE.get().getDefaultInstance());
        blockEntity.conversionCooldown = Math.max(1, Config.knowledgeCompressorConversionIntervalTicks);
        blockEntity.setChanged();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Tank", experienceTank.writeToNBT(registries, new CompoundTag()));
        tag.put("Output", outputHandler.serializeNBT(registries));
        tag.putInt("ConversionCooldown", conversionCooldown);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        experienceTank.readFromNBT(registries, tag.getCompound("Tank"));
        outputHandler.deserializeNBT(registries, tag.getCompound("Output"));
        conversionCooldown = tag.getInt("ConversionCooldown");
    }

    private int tankCapacity() {
        return Math.max(1, Config.knowledgeCompressorTankCapacityMb);
    }

    private final class ExperienceTank extends FluidTank {
        ExperienceTank() {
            super(1);
            updateCapacity();
        }

        void updateCapacity() {
            setCapacity(tankCapacity());
        }

        @Override
        public boolean isFluidValid(FluidStack stack) {
            return !stack.isEmpty() && stack.getFluid().is(EXPERIENCE_FLUID_TAG);
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    }

    private final class CompressorFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return experienceTank.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            experienceTank.updateCapacity();
            return experienceTank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return experienceTank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            experienceTank.updateCapacity();
            return experienceTank.fill(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            return experienceTank.drain(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            return experienceTank.drain(maxDrain, action);
        }
    }
}
