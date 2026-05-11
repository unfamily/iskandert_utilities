package net.unfamily.iskautils.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.unfamily.iskautils.IskaUtils;

public final class ModBiomeModifierSerializers {
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, IskaUtils.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<RubberTreesIfEnabledBiomeModifier>> RUBBER_TREES_IF_ENABLED =
            SERIALIZERS.register("rubber_trees_if_enabled", () -> RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(RubberTreesIfEnabledBiomeModifier::biomes),
                    PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(RubberTreesIfEnabledBiomeModifier::features),
                    GenerationStep.Decoration.CODEC.fieldOf("step").forGetter(RubberTreesIfEnabledBiomeModifier::step)
            ).apply(instance, RubberTreesIfEnabledBiomeModifier::new)));

    private ModBiomeModifierSerializers() {}

    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
    }
}
