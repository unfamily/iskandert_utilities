package net.unfamily.iskautils.block;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.standard.*;
import net.unfamily.iskautils.block.player.*;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SaplingBlock;
import net.unfamily.iskautils.block.PlateBaseBlock;
import net.unfamily.iskautils.block.SmoothBlackstoneWallBlock;
import net.unfamily.iskautils.block.PotionPlateBlock;
import net.unfamily.iskautils.block.RaftBlock;
import net.unfamily.iskautils.block.RubberSaplingBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IskaUtils.MOD_ID);

    // Common properties for all vector blocks
    private static final BlockBehaviour.Properties VECTOR_PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .strength(0.3f, 1.0f)
            .sound(SoundType.DEEPSLATE)
            .noOcclusion()
            .noCollission()
            .isRedstoneConductor((state, level, pos) -> false)
            .pushReaction(PushReaction.DESTROY)
            .isViewBlocking((state, level, pos) -> false)
            .lightLevel((state) -> 0);
    
    // Properties for the Hellfire Igniter
    private static final BlockBehaviour.Properties HELLFIRE_PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .strength(0.3f, 1.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
            .lightLevel((state) -> state.getValue(HellfireIgniterBlock.POWERED) ? 7 : 0);
    
    // Properties for Rubber Sap Extractor
    private static final BlockBehaviour.Properties RUBBER_SAP_EXTRACTOR_PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .strength(3.5f, 6.0f)
            .sound(SoundType.COPPER)
            .requiresCorrectToolForDrops()
            .lightLevel((state) -> state.getValue(HellfireIgniterBlock.POWERED) ? 7 : 3)
            .noOcclusion();
    
    // ===== STANDARD VECTOR PLATES (DON'T AFFECT PLAYERS) =====
    
    // Slow Vector Plate (slowest)
    public static final DeferredBlock<SlowVectBlock> SLOW_VECT = BLOCKS.register("slow_vect",
            () -> new SlowVectBlock(VECTOR_PROPERTIES));
    
    // Moderate Vector Plate
    public static final DeferredBlock<ModerateVectBlock> MODERATE_VECT = BLOCKS.register("moderate_vect",
            () -> new ModerateVectBlock(VECTOR_PROPERTIES));
    
    // Fast Vector Plate
    public static final DeferredBlock<FastVectBlock> FAST_VECT = BLOCKS.register("fast_vect",
            () -> new FastVectBlock(VECTOR_PROPERTIES));
    
    // Extreme Vector Plate
    public static final DeferredBlock<ExtremeVectBlock> EXTREME_VECT = BLOCKS.register("extreme_vect",
            () -> new ExtremeVectBlock(VECTOR_PROPERTIES));
    
    // Ultra Vector Plate (fastest)
    public static final DeferredBlock<UltraVectBlock> ULTRA_VECT = BLOCKS.register("ultra_vect",
            () -> new UltraVectBlock(VECTOR_PROPERTIES));
    
    // ===== PLAYER VECTOR PLATES (AFFECT PLAYERS) =====
    
    // Player Slow Vector Plate (slowest)
    public static final DeferredBlock<PlayerSlowVectBlock> PLAYER_SLOW_VECT = BLOCKS.register("player_slow_vect",
            () -> new PlayerSlowVectBlock(VECTOR_PROPERTIES));
    
    // Player Moderate Vector Plate
    public static final DeferredBlock<PlayerModerateVectBlock> PLAYER_MODERATE_VECT = BLOCKS.register("player_moderate_vect",
            () -> new PlayerModerateVectBlock(VECTOR_PROPERTIES));
    
    // Player Fast Vector Plate
    public static final DeferredBlock<PlayerFastVectBlock> PLAYER_FAST_VECT = BLOCKS.register("player_fast_vect",
            () -> new PlayerFastVectBlock(VECTOR_PROPERTIES));
    
    // Player Extreme Vector Plate
    public static final DeferredBlock<PlayerExtremeVectBlock> PLAYER_EXTREME_VECT = BLOCKS.register("player_extreme_vect",
            () -> new PlayerExtremeVectBlock(VECTOR_PROPERTIES));
    
    // Player Ultra Vector Plate (fastest)
    public static final DeferredBlock<PlayerUltraVectBlock> PLAYER_ULTRA_VECT = BLOCKS.register("player_ultra_vect",
            () -> new PlayerUltraVectBlock(VECTOR_PROPERTIES));
    
    // ===== UTILITY BLOCKS =====
    
    // Hellfire Igniter (creates fire when activated by redstone)
    public static final DeferredBlock<HellfireIgniterBlock> HELLFIRE_IGNITER = BLOCKS.register("hellfire_igniter",
            () -> new HellfireIgniterBlock(HELLFIRE_PROPERTIES));
            
    // Rubber Sap Extractor (automatically extracts sap from rubber logs)
    public static final DeferredBlock<RubberSapExtractorBlock> RUBBER_SAP_EXTRACTOR = BLOCKS.register("rubber_sap_extractor",
            () -> new RubberSapExtractorBlock(RUBBER_SAP_EXTRACTOR_PROPERTIES));


    // ===== SMOOTH BLACKSTONE =====
    private static final BlockBehaviour.Properties SMOOTH_BLACKSTONE_PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .strength(1.5f, 6.0f)
            .sound(SoundType.DEEPSLATE)
            .requiresCorrectToolForDrops();

    public static final DeferredBlock<Block> SMOOTH_BLACKSTONE = BLOCKS.register("smooth_blackstone",
            () -> new Block(SMOOTH_BLACKSTONE_PROPERTIES));
    public static final DeferredBlock<SlabBlock> SMOOTH_BLACKSTONE_SLAB = BLOCKS.register("smooth_blackstone_slab",
            () -> new SlabBlock(SMOOTH_BLACKSTONE_PROPERTIES));
    public static final DeferredBlock<StairBlock> SMOOTH_BLACKSTONE_STAIRS = BLOCKS.register("smooth_blackstone_stairs",
            () -> new StairBlock(SMOOTH_BLACKSTONE.get().defaultBlockState(), SMOOTH_BLACKSTONE_PROPERTIES));
    public static final DeferredBlock<Block> SMOOTH_BLACKSTONE_WALL = BLOCKS.register("smooth_blackstone_wall",
            () -> new SmoothBlackstoneWallBlock(SMOOTH_BLACKSTONE_PROPERTIES));

    // ===== RUBBER TREE BLOCKS =====
    private static final BlockBehaviour.Properties RUBBER_LOG_PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(2.0f)
            .sound(SoundType.WOOD);
            
            
    private static final BlockBehaviour.Properties RUBBER_LOG_SAP_PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(2.0f)
            .sound(SoundType.WOOD);
            
    private static final BlockBehaviour.Properties RUBBER_LEAVES_PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .strength(0.2f)
            .sound(SoundType.GRASS)
            .noOcclusion()
            .isValidSpawn((state, level, pos, type) -> false)
            .isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos) -> false);
    
    private static final BlockBehaviour.Properties RUBBER_SAPLING_PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollission()
            .randomTicks()
            .instabreak()
            .sound(SoundType.GRASS)
            .pushReaction(PushReaction.DESTROY);
            
    // properties for rubber wood blocks, for reuse
    private static final BlockBehaviour.Properties RUBBER_WOOD_PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(2.0f)
            .sound(SoundType.WOOD);
            
    private static final BlockBehaviour.Properties RUBBER_PLANKS_PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(2.0f, 3.0f)
            .sound(SoundType.WOOD);
            
    // Standard placeable block (visible in the creative tab)
    public static final DeferredBlock<RubberLogBlock> RUBBER_LOG = BLOCKS.register("rubber_log",
            () -> new RubberLogBlock(RUBBER_LOG_PROPERTIES));
            
    // Stripped Rubber Log
    public static final DeferredBlock<StrippedRubberLogBlock> STRIPPED_RUBBER_LOG = BLOCKS.register("stripped_rubber_log",
            () -> new StrippedRubberLogBlock(RUBBER_LOG_PROPERTIES));
            
    // Rubber Wood (6 faces with bark)
    public static final DeferredBlock<RubberWoodBlock> RUBBER_WOOD = BLOCKS.register("rubber_wood",
            () -> new RubberWoodBlock(RUBBER_WOOD_PROPERTIES));

    // Stripped Rubber Wood (6 faces without bark)
    public static final DeferredBlock<StrippedRubberWoodBlock> STRIPPED_RUBBER_WOOD = BLOCKS.register("stripped_rubber_wood",
            () -> new StrippedRubberWoodBlock(RUBBER_WOOD_PROPERTIES));

    // Rubber Planks
    public static final DeferredBlock<RubberPlanksBlock> RUBBER_PLANKS = BLOCKS.register("rubber_planks",
            () -> new RubberPlanksBlock(RUBBER_PLANKS_PROPERTIES));
            
    // Filled block with sap
    public static final DeferredBlock<RubberLogFilledBlock> RUBBER_LOG_FILLED = BLOCKS.register("rubber_log_filled",
            () -> new RubberLogFilledBlock(RUBBER_LOG_SAP_PROPERTIES));
            
    // Empty block with sap
    public static final DeferredBlock<RubberLogEmptyBlock> RUBBER_LOG_EMPTY = BLOCKS.register("rubber_log_empty",
            () -> new RubberLogEmptyBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)));
            
    public static final DeferredBlock<LeavesBlock> RUBBER_LEAVES = BLOCKS.register("rubber_leaves",
            () -> new RubberLeavesBlock(RUBBER_LEAVES_PROPERTIES));
            
    public static final DeferredBlock<RubberSaplingBlock> RUBBER_SAPLING = BLOCKS.register("rubber_sapling",
            () -> new RubberSaplingBlock(RUBBER_SAPLING_PROPERTIES));

    // Rubber Block
    public static final DeferredBlock<RubberBlock> RUBBER_BLOCK = BLOCKS.register("rubber_block",
            () -> new RubberBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(2.0f)
                    .sound(SoundType.TUFF)));
                    
    // ===== RUBBER WOOD VARIANTS =====
    // Stairs, slabs, fences, fence gates, buttons, pressure plates, doors, trapdoors
    
    // Rubber Stairs
    public static final DeferredBlock<RubberStairsBlock> RUBBER_STAIRS = BLOCKS.register("rubber_stairs",
            () -> new RubberStairsBlock(RUBBER_PLANKS.get().defaultBlockState(), RUBBER_PLANKS_PROPERTIES));
            
    // Rubber Slab
    public static final DeferredBlock<RubberSlabBlock> RUBBER_SLAB = BLOCKS.register("rubber_slab",
            () -> new RubberSlabBlock(RUBBER_PLANKS_PROPERTIES));
            
    // Rubber Fence
    public static final DeferredBlock<RubberFenceBlock> RUBBER_FENCE = BLOCKS.register("rubber_fence",
            () -> new RubberFenceBlock(RUBBER_PLANKS_PROPERTIES));
            
    // Rubber Fence Gate
    public static final DeferredBlock<RubberFenceGateBlock> RUBBER_FENCE_GATE = BLOCKS.register("rubber_fence_gate",
            () -> new RubberFenceGateBlock(RUBBER_PLANKS_PROPERTIES));
            
    // Rubber Button
    public static final DeferredBlock<RubberButtonBlock> RUBBER_BUTTON = BLOCKS.register("rubber_button",
            () -> new RubberButtonBlock(RUBBER_PLANKS_PROPERTIES.noCollission()));
            
    // Rubber Pressure Plate
    public static final DeferredBlock<RubberPressurePlateBlock> RUBBER_PRESSURE_PLATE = BLOCKS.register("rubber_pressure_plate",
            () -> new RubberPressurePlateBlock(RUBBER_PLANKS_PROPERTIES.noCollission()));
            
    // Rubber Door
    public static final DeferredBlock<RubberDoorBlock> RUBBER_DOOR = BLOCKS.register("rubber_door",
            () -> new RubberDoorBlock(RUBBER_PLANKS_PROPERTIES.noOcclusion()));
            
    // Rubber Trapdoor
    public static final DeferredBlock<RubberTrapDoorBlock> RUBBER_TRAPDOOR = BLOCKS.register("rubber_trapdoor",
            () -> new RubberTrapDoorBlock(RUBBER_PLANKS_PROPERTIES.noOcclusion()));

    // ===== PLATE BASE BLOCK (vector type, texture above and below plate_base) =====
    public static final DeferredBlock<PlateBaseBlock> PLATE_BASE_BLOCK = BLOCKS.register("plate_base_block",
            () -> new PlateBaseBlock(VECTOR_PROPERTIES));
            
    // ===== RAFT BLOCK (floating wood block) =====
    private static final BlockBehaviour.Properties RAFT_PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(0.5f, 1.0f)
            .sound(SoundType.WOOD)
            .dynamicShape()
            .isSuffocating((state, level, pos) -> false);
            
    public static final DeferredBlock<RaftBlock> RAFT = BLOCKS.register("raft",
            () -> new RaftBlock(RAFT_PROPERTIES));

    // ===== TAR BLOCK =====
    public static final DeferredBlock<TarSlimeBlock> TAR_SLIME_BLOCK = BLOCKS.register("tar_slime_block",
            () -> new TarSlimeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(0.5f, 1.0f)
                    .sound(SoundType.SLIME_BLOCK)
                    .friction(0.8f)
                    .jumpFactor(0.5f)
                    .noOcclusion()));

    // ===== WEATHER DETECTOR =====
    public static final DeferredBlock<WeatherDetectorBlock> WEATHER_DETECTOR = BLOCKS.register("weather_detector",
            () -> new WeatherDetectorBlock(BlockBehaviour.Properties.of()
                    .strength(0.5f, 1.0f)
                    .sound(SoundType.COPPER)
                    .noOcclusion()));

    // ===== DEV BLOCKS =====
    // SCAN BLOCK
    public static final DeferredBlock<Block> SCAN_BLOCK = BLOCKS.register("scan_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(0.5f, 1.0f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()));

    // Weather Alterer Block
    public static final DeferredBlock<WeatherAltererBlock> WEATHER_ALTERER = BLOCKS.register("weather_alterer",
        () -> new WeatherAltererBlock(BlockBehaviour.Properties.of()
            .strength(3.5F)
            .requiresCorrectToolForDrops()
            .sound(SoundType.COPPER)
            .lightLevel((state) -> 3)
            .noOcclusion()));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
} 