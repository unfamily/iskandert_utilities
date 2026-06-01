package net.unfamily.iskautils.fluid;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.item.ModItems;

public class ModFluids {
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(BuiltInRegistries.FLUID, IskaUtils.MOD_ID);
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, IskaUtils.MOD_ID);

    /** Thin liquid style: vanilla water sprites + tint (Colossal gelid breezium convention). */
    public static final ResourceLocation STILL_TEXTURE =
            ResourceLocation.withDefaultNamespace("block/water_still");
    public static final ResourceLocation FLOWING_TEXTURE =
            ResourceLocation.withDefaultNamespace("block/water_flow");
    public static final ResourceLocation OVERLAY_TEXTURE =
            ResourceLocation.withDefaultNamespace("block/water_overlay");

    public static final DeferredHolder<FluidType, FluidType> CONDENSED_KNOWLEDGE_TYPE =
            FLUID_TYPES.register("condensed_knowledge", () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.iska_utils.condensed_knowledge")
                    .lightLevel(10)
                    .density(1000)
                    .viscosity(1000)
                    .temperature(300)
                    .canConvertToSource(false)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

    private static final BaseFlowingFluid.Properties[] CONDENSED_KNOWLEDGE_PROPERTIES_HOLDER = new BaseFlowingFluid.Properties[1];

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> CONDENSED_KNOWLEDGE_SOURCE =
            FLUIDS.register("condensed_knowledge",
                    () -> new BaseFlowingFluid.Source(CONDENSED_KNOWLEDGE_PROPERTIES_HOLDER[0]));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> CONDENSED_KNOWLEDGE_FLOWING =
            FLUIDS.register("condensed_knowledge_flowing",
                    () -> new BaseFlowingFluid.Flowing(CONDENSED_KNOWLEDGE_PROPERTIES_HOLDER[0]));

    public static final BaseFlowingFluid.Properties CONDENSED_KNOWLEDGE_PROPERTIES;

    static {
        CONDENSED_KNOWLEDGE_PROPERTIES = new BaseFlowingFluid.Properties(
                CONDENSED_KNOWLEDGE_TYPE,
                CONDENSED_KNOWLEDGE_SOURCE,
                CONDENSED_KNOWLEDGE_FLOWING)
                .block(ModBlocks.CONDENSED_KNOWLEDGE_BLOCK)
                .bucket(ModItems.CONDENSED_KNOWLEDGE_BUCKET);
        CONDENSED_KNOWLEDGE_PROPERTIES_HOLDER[0] = CONDENSED_KNOWLEDGE_PROPERTIES;
    }

    private ModFluids() {}

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }
}
