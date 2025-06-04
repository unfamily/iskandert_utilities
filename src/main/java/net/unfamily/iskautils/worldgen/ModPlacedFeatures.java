package net.unfamily.iskautils.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.ModBlocks;
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

        // Usa treePlacement che include controlli per evitare la generazione su acqua
        register(context, RUBBER_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.RUBBER_KEY),
                VegetationPlacements.treePlacement(
                    // Parametri di posizionamento
                    PlacementUtils.countExtra(1, 0.1f, 2), // 1 albero con 10% di chance per 2 extra
                    ModBlocks.RUBBER_SAPLING.get() // Riferimento al sapling per il controllo della validità del terreno
                ));
                
        //LOGGER.info("Registered rubber tree placed feature: {}", RUBBER_PLACED_KEY.location());
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
        // Verifica che il blocco sotto non sia acqua
        BlockPos groundPos = pos.below();
        BlockState groundState = level.getBlockState(groundPos);
        
        // Controllo completo per verificare che non ci sia acqua sotto o intorno
        if (groundState.is(Blocks.WATER) || groundState.is(Blocks.LAVA) || 
            groundState.is(Blocks.BUBBLE_COLUMN) || groundState.getFluidState().isSource() ||
            level.isWaterAt(pos) || level.isWaterAt(groundPos)) {
            //LOGGER.info("Cannot place rubber tree at {} because it's above water or lava", pos);
            return false;
        }
        
        // Controlla anche intorno alla base dell'albero per assicurarsi che non ci sia acqua
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                BlockPos checkPos = pos.offset(x, 0, z);
                if (level.isWaterAt(checkPos) || level.getBlockState(checkPos).getFluidState().isSource()) {
                    //LOGGER.info("Cannot place rubber tree at {} because there's water nearby at {}", pos, checkPos);
                    return false;
                }
            }
        }
        
        // Ottieni la PlacedFeature dell'albero di gomma dal registro
        Optional<Holder.Reference<PlacedFeature>> optionalFeature = level.registryAccess()
                .registryOrThrow(Registries.PLACED_FEATURE)
                .getHolder(RUBBER_PLACED_KEY);
                
        if (optionalFeature.isEmpty()) {
            //LOGGER.error("Failed to find rubber tree placed feature: {}", RUBBER_PLACED_KEY.location());
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