package net.unfamily.iskautils.fluid;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.unfamily.iskalib.liquid.IskaLibLiquids;
import net.unfamily.iskalib.liquid.LiquidBlockProperties;
import net.unfamily.iskalib.liquid.LiquidRegistrationRegisters;
import net.unfamily.iskalib.liquid.LiquidSpec;
import net.unfamily.iskalib.liquid.RegisteredLiquid;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.item.ModItems;

/**
 * Consumer liquid registration via {@link IskaLibLiquids} (NeoForge 26+).
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

    /** Set in {@link #register(IEventBus)}; not available before mod construction. */
    public static RegisteredLiquid CONDENSED_KNOWLEDGE;

    private ModFluids() {}

    public static void register(IEventBus modEventBus) {
        LiquidRegistrationRegisters registers = new LiquidRegistrationRegisters(
                FLUID_TYPES, FLUIDS, ModBlocks.BLOCKS, ModItems.ITEMS);

        CONDENSED_KNOWLEDGE = IskaLibLiquids.registerLiquid(
                modEventBus,
                registers,
                LiquidSpec.withThinVanillaWaterSprites(
                                IskaUtils.MOD_ID,
                                "condensed_knowledge",
                                FluidColors.CONDENSED_KNOWLEDGE,
                                "fluid.iska_utils.condensed_knowledge",
                                10,
                                true)
                        .withBlockProperties(new LiquidBlockProperties(
                                MapColor.COLOR_LIGHT_GREEN,
                                100.0F,
                                PushReaction.DESTROY,
                                -1,
                                null)));

        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
    }
}
