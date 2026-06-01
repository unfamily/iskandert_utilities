package net.unfamily.iskautils.fluid;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.item.ModItems;

/**
 * Tinted fluid registration (Colossal Reactors {@code ModFluids} shape on NeoForge 26).
 * Static fields populate deferred registers before {@link #FLUID_TYPES} / {@link #FLUIDS} subscribe on the mod bus.
 */
public final class ModFluids {

    public static final class FluidColors {
        public static final int CONDENSED_KNOWLEDGE = 0xFF55FF88;

        private FluidColors() {}
    }

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, IskaUtils.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(BuiltInRegistries.FLUID, IskaUtils.MOD_ID);

    public static final TintedFluid CONDENSED_KNOWLEDGE = registerCondensedKnowledge();

    private ModFluids() {}

    private static TintedFluid registerCondensedKnowledge() {
        DeferredHolder<FluidType, FluidType> type = FLUID_TYPES.register("condensed_knowledge_type",
                () -> new FluidType(FluidType.Properties.create()
                        .descriptionId("fluid.iska_utils.condensed_knowledge")
                        .lightLevel(10)
                        .density(1000)
                        .viscosity(1000)
                        .temperature(300)
                        .canConvertToSource(false)
                        .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                        .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

        return registerTintedFluid("condensed_knowledge", type, 10, true);
    }

    /**
     * Shared registration for a tinted fluid (block, bucket, source, flowing).
     * Replace this body with {@code IskaLibLiquids} later without changing call sites.
     */
    private static TintedFluid registerTintedFluid(
            String name,
            DeferredHolder<FluidType, FluidType> type,
            int blockLightLevel,
            boolean sourceIdIsBaseName
    ) {
        var refs = new Object() {
            DeferredHolder<Fluid, BaseFlowingFluid.Source> source;
            DeferredHolder<Fluid, FlowingFluid> flowing;
            DeferredBlock<Block> block;
            DeferredHolder<Item, BucketItem> bucket;
        };

        BaseFlowingFluid.Properties prop = new BaseFlowingFluid.Properties(
                        type,
                        () -> refs.source.get(),
                        () -> refs.flowing.get())
                .block(() -> (LiquidBlock) refs.block.get())
                .bucket(() -> refs.bucket.get());

        String sourceId = sourceIdIsBaseName ? name : (name + "_source");
        refs.source = FLUIDS.register(sourceId, () -> new BaseFlowingFluid.Source(prop));
        refs.flowing = FLUIDS.register(name + "_flowing", () -> new BaseFlowingFluid.Flowing(prop));
        refs.block = ModBlocks.BLOCKS.registerBlock(name,
                props -> new LiquidBlock(refs.flowing.get(), props),
                p -> p.mapColor(MapColor.COLOR_LIGHT_GREEN)
                        .replaceable()
                        .strength(100.0F)
                        .pushReaction(PushReaction.DESTROY)
                        .noLootTable()
                        .liquid()
                        .lightLevel(state -> blockLightLevel));
        refs.bucket = ModItems.ITEMS.registerItem(name + "_bucket",
                props -> new BucketItem(refs.source.get(), props),
                () -> new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));

        return new TintedFluid(refs.source, refs.flowing, refs.block, refs.bucket);
    }

    public record TintedFluid(
            DeferredHolder<Fluid, BaseFlowingFluid.Source> source,
            DeferredHolder<Fluid, FlowingFluid> flowing,
            DeferredBlock<Block> block,
            DeferredHolder<Item, BucketItem> bucket
    ) {
        public Fluid getSource() {
            return source.get();
        }

        public Fluid getFlowing() {
            return flowing.get();
        }

        public Block getBlock() {
            return block.get();
        }

        public Item getBucket() {
            return bucket.get();
        }
    }
}
