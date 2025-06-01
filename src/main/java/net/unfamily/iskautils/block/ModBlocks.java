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
            .sound(SoundType.WOOD)
            .randomTicks(); // Per il rifornimento della linfa
            
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
            
    public static final DeferredBlock<RubberLogBlock> RUBBER_LOG = BLOCKS.register("rubber_log",
            () -> new RubberLogBlock(RUBBER_LOG_PROPERTIES));
            
    public static final DeferredBlock<LeavesBlock> RUBBER_LEAVES = BLOCKS.register("rubber_leaves",
            () -> new LeavesBlock(RUBBER_LEAVES_PROPERTIES));
            
    public static final DeferredBlock<RubberSaplingBlock> RUBBER_SAPLING = BLOCKS.register("rubber_sapling",
            () -> new RubberSaplingBlock(RUBBER_SAPLING_PROPERTIES));

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

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
} 