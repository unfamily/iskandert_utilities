package net.unfamily.iskautils.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.world.BiomeGenerationSettingsBuilder;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.unfamily.iskautils.Config;

/**
 * Same as NeoForge {@code add_features}, but skips when {@link Config#generateRubberTrees} is false.
 */
public record RubberTreesIfEnabledBiomeModifier(
        HolderSet<Biome> biomes,
        HolderSet<PlacedFeature> features,
        GenerationStep.Decoration step
) implements BiomeModifier {

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (!Config.generateRubberTrees) {
            return;
        }
        if (phase == Phase.ADD && this.biomes.contains(biome)) {
            BiomeGenerationSettingsBuilder generationSettings = builder.getGenerationSettings();
            this.features.forEach(holder -> generationSettings.addFeature(this.step, holder));
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return ModBiomeModifierSerializers.RUBBER_TREES_IF_ENABLED.get();
    }
}
