package net.unfamily.iskautils.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.RubberLogFilledBlock;
import net.minecraft.core.Direction;
import net.minecraft.util.random.SimpleWeightedRandomList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that contains all the configurations of the features used in the mod.  
 */
public class ModConfiguredFeatures {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModConfiguredFeatures.class);
    
    /**
     * ResourceKey for the rubber tree.
     */
    public static final ResourceKey<ConfiguredFeature<?, ?>> RUBBER_KEY = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            ResourceLocation.tryBuild(IskaUtils.MOD_ID, "rubber"));
    
    /**
     * Executes the bootstrap of the feature configurations.
     * @param context Bootstrap context
     */
    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // Set the leaves as persistent
        BlockState leavesState = ModBlocks.RUBBER_LEAVES.get().defaultBlockState()
            .setValue(LeavesBlock.PERSISTENT, true);
            
        // Create a block provider that can generate normal trunks or with sap
        BlockStateProvider logProvider = createRubberLogProvider();
            
        // Register the rubber tree
        TreeConfiguration.TreeConfigurationBuilder rubberTree = new TreeConfiguration.TreeConfigurationBuilder(
                // We use the RUBBER_LOG block with the possibility of RUBBER_LOG_FILLED
                logProvider,
                new StraightTrunkPlacer(5, 2, 0),
                BlockStateProvider.simple(leavesState),
                new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 3),
                new TwoLayersFeatureSize(1, 0, 1)
        ).ignoreVines().forceDirt();
        
        context.register(RUBBER_KEY, new ConfiguredFeature<>(Feature.TREE, rubberTree.build()));
        
        //LOGGER.info("Registered rubber tree configured feature: {}", RUBBER_KEY.location());
    }
    
    /**
     * Create a block provider that can generate normal trunks or with sap
     */
    private static BlockStateProvider createRubberLogProvider() {
        SimpleWeightedRandomList.Builder<BlockState> builder = SimpleWeightedRandomList.builder();
        
        // Normal block (75% chance) with Y axis (vertical)
        BlockState normalLog = ModBlocks.RUBBER_LOG.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.Y);
        builder.add(normalLog, 100);
        //LOGGER.info("Using RUBBER_LOG for tree generation with weight 100: {}", normalLog);
        
        return new WeightedStateProvider(builder.build());
    }
} 