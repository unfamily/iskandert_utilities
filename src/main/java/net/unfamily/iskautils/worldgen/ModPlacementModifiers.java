package net.unfamily.iskautils.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.iskautils.IskaUtils;

public final class ModPlacementModifiers {
    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIERS =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, IskaUtils.MOD_ID);

    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<MaxSkyLightFilter>> MAX_SKY_LIGHT =
            PLACEMENT_MODIFIERS.register("max_sky_light", () -> () -> MaxSkyLightFilter.CODEC);

    private ModPlacementModifiers() {}

    public static void register(IEventBus modEventBus) {
        PLACEMENT_MODIFIERS.register(modEventBus);
    }
}
