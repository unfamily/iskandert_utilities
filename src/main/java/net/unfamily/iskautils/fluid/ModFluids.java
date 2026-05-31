package net.unfamily.iskautils.fluid;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.unfamily.iskalib.liquid.IskaLibLiquids;
import net.unfamily.iskalib.liquid.LiquidRegistrationRegisters;
import net.unfamily.iskalib.liquid.RegisteredLiquid;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.item.ModItems;

/**
 * Tinted fluid registration (Colossal Reactors {@code ModFluids} shape).
 * Internals delegate to {@link IskaLibLiquids} so the backing registrar can be swapped later.
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

    /** Registered via {@link IskaLibLiquids} on this mod's deferred registers. */
    public static TintedFluid CONDENSED_KNOWLEDGE;

    private ModFluids() {}

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);

        CONDENSED_KNOWLEDGE = registerTintedFluid(
                modEventBus,
                "condensed_knowledge",
                FluidColors.CONDENSED_KNOWLEDGE,
                "fluid.iska_utils.condensed_knowledge",
                10);
    }

    /**
     * Colossal-style entry point; replace this body to register in-mod without changing call sites.
     */
    private static TintedFluid registerTintedFluid(
            IEventBus modEventBus,
            String name,
            int tintArgb,
            String descriptionId,
            int blockLightLevel
    ) {
        RegisteredLiquid registered = IskaLibLiquids.registerLiquid(
                modEventBus,
                new LiquidRegistrationRegisters(FLUID_TYPES, FLUIDS, ModBlocks.BLOCKS, ModItems.ITEMS),
                IskaUtils.MOD_ID,
                name,
                tintArgb,
                descriptionId,
                blockLightLevel);
        return TintedFluid.from(registered);
    }

    public record TintedFluid(
            DeferredHolder<Fluid, ? extends Fluid> source,
            DeferredHolder<Fluid, ? extends Fluid> flowing,
            DeferredBlock<? extends Block> block,
            DeferredHolder<Item, ? extends Item> bucket
    ) {
        public static TintedFluid from(RegisteredLiquid registered) {
            return new TintedFluid(
                    registered.sourceHolder(),
                    registered.flowingHolder(),
                    registered.blockHolder(),
                    registered.bucketHolder());
        }

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
