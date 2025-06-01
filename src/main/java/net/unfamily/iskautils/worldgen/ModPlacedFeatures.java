package net.unfamily.iskautils.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.unfamily.iskautils.IskaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class ModPlacedFeatures {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModPlacedFeatures.class);
    public static final ResourceKey<PlacedFeature> RUBBER_PLACED_KEY = ResourceKey.create(
            Registries.PLACED_FEATURE, 
            ResourceLocation.tryParse(IskaUtils.MOD_ID + ":" + "rubber_placed"));

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

        register(context, RUBBER_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.RUBBER_KEY),
                // Parametri di posizionamento
                List.of(
                    // Raro ma non troppo (circa 1 ogni 250 blocchi)
                    PlacementUtils.countExtra(0, 0.01f, 1), 
                    PlacementUtils.HEIGHTMAP_WORLD_SURFACE
                ));
                
        LOGGER.info("Registered rubber tree placed feature: {}", RUBBER_PLACED_KEY.location());
    }
    
    /**
     * Posiziona un albero di gomma al posto specificato.
     * 
     * @param level Il livello server
     * @param generator Il generatore di chunk
     * @param pos La posizione dove posizionare l'albero
     * @param random La sorgente casuale
     * @return true se l'albero è stato posizionato con successo, false altrimenti
     */
    public static boolean placeRubberTree(ServerLevel level, ChunkGenerator generator, BlockPos pos, RandomSource random) {
        // Ottieni la PlacedFeature dell'albero di gomma dal registro
        Optional<Holder.Reference<PlacedFeature>> optionalFeature = level.registryAccess()
                .registryOrThrow(Registries.PLACED_FEATURE)
                .getHolder(RUBBER_PLACED_KEY);
                
        if (optionalFeature.isEmpty()) {
            LOGGER.error("Failed to find rubber tree placed feature: {}", RUBBER_PLACED_KEY.location());
            return false;
        }
        
        // Genera l'albero
        return optionalFeature.get().value().place(level, generator, random, pos);
    }

    private static void register(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key,
                                Holder<ConfiguredFeature<?, ?>> configuration,
                                List<PlacementModifier> modifiers) {
        context.register(key, new PlacedFeature(configuration, modifiers));
    }
} 