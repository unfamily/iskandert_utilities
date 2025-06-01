package net.unfamily.iskautils.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.ModBlocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModConfiguredFeatures {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModConfiguredFeatures.class);
    
    // Chiave per l'albero di gomma
    public static final ResourceKey<ConfiguredFeature<?, ?>> RUBBER_KEY = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            ResourceLocation.tryParse(IskaUtils.MOD_ID + ":" + "rubber"));
    
    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // Registrazione dell'albero di gomma
        register(context, RUBBER_KEY, Feature.TREE, createRubberTree().build());
        
        LOGGER.info("Registered rubber tree configured feature: {}", RUBBER_KEY.location());
    }
    
    /**
     * Crea la configurazione per un albero di gomma.
     * 
     * @return Un TreeConfiguration.TreeConfigurationBuilder per un albero di gomma
     */
    private static TreeConfiguration.TreeConfigurationBuilder createRubberTree() {
        return new TreeConfiguration.TreeConfigurationBuilder(
                // Tronco
                BlockStateProvider.simple(ModBlocks.RUBBER_LOG.get()),
                // Tronco dritto, altezza base 5, altezza randomizzata 2, altezza del fogliame sopra 0
                new StraightTrunkPlacer(5, 2, 0),
                // Foglie
                BlockStateProvider.simple(ModBlocks.RUBBER_LEAVES.get()),
                // Foglie a blob, raggio 2, offset 0, altezza 3
                new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 3),
                // Dimensione a due strati, limite 1, limite superiore 2
                new TwoLayersFeatureSize(1, 0, 1)
        )
        // Ignora le vines
        .ignoreVines();
    }
    
    /**
     * Registra una ConfiguredFeature nel contesto di bootstrap.
     * 
     * @param context Il contesto di bootstrap
     * @param key La chiave di registro per la feature
     * @param feature La feature da configurare
     * @param config La configurazione della feature
     * @param <FC> Il tipo di configurazione della feature
     * @param <F> Il tipo di feature
     */
    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(
            BootstrapContext<ConfiguredFeature<?, ?>> context,
            ResourceKey<ConfiguredFeature<?, ?>> key,
            F feature,
            FC config) {
        context.register(key, new ConfiguredFeature<>(feature, config));
    }
} 