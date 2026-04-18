package net.unfamily.iskautils.block;

import java.util.function.UnaryOperator;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.player.*;
import net.unfamily.iskautils.block.standard.*;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IskaUtils.MOD_ID);

    /**
     * NeoForge 26+: {@link net.neoforged.neoforge.registries.RegisterEvent} invokes block suppliers immediately;
     * {@link net.neoforged.neoforge.registries.DeferredHolder#get()} for another block in this mod can still be unbound during that pass.
     * Resolve the base from {@link BuiltInRegistries#BLOCK} by id instead.
     */
    private static BlockState baseBlockStateForStairs(String path) {
        Identifier id = Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, path);
        return BuiltInRegistries.BLOCK.getOptional(id)
                .orElseThrow(() -> new IllegalStateException("Missing base block for dependent stair (register base first in ModBlocks): " + id))
                .defaultBlockState();
    }

    /**
     * Minecraft 26+: {@link BlockBehaviour.Properties} must have {@link BlockBehaviour.Properties#setId} before the block constructor runs.
     */
    public static BlockBehaviour.Properties assignBlockId(Identifier key, UnaryOperator<BlockBehaviour.Properties> configure) {
        return configure.apply(BlockBehaviour.Properties.of())
                .setId(ResourceKey.create(Registries.BLOCK, key));
    }

    // Common properties for all vector blocks
    private static final UnaryOperator<BlockBehaviour.Properties> VECTOR_PROPERTIES = p -> p
            .mapColor(MapColor.COLOR_BLACK)
            .strength(0.3f, 1.0f)
            .sound(SoundType.DEEPSLATE)
            .noOcclusion()
            .noCollision()
            .isRedstoneConductor((state, level, pos) -> false)
            .pushReaction(PushReaction.DESTROY)
            .isViewBlocking((state, level, pos) -> false)
            .lightLevel((state) -> 0);

    // Properties for the Hellfire Igniter
    private static final UnaryOperator<BlockBehaviour.Properties> HELLFIRE_PROPERTIES = p -> p
            .mapColor(MapColor.STONE)
            .strength(1.5f, 6.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
            .lightLevel((state) -> state.getValue(HellfireIgniterBlock.POWERED) ? 7 : 0);

    // Properties for the Fan
    private static final UnaryOperator<BlockBehaviour.Properties> FAN_PROPERTIES = p -> p
            .mapColor(MapColor.STONE)
            .strength(1.5f, 6.0f)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops();

    // Properties for Rubber Sap Extractor
    private static final UnaryOperator<BlockBehaviour.Properties> RUBBER_SAP_EXTRACTOR_PROPERTIES = p -> p
            .mapColor(MapColor.STONE)
            .strength(3.5f, 6.0f)
            .sound(SoundType.COPPER)
            .requiresCorrectToolForDrops()
            .lightLevel((state) -> state.getValue(HellfireIgniterBlock.POWERED) ? 7 : 3)
            .noOcclusion();

    // ===== WITHER PROOF BLOCKS =====

    // Properties for Wither Proof Block
    private static final UnaryOperator<BlockBehaviour.Properties> WITHER_PROOF_PROPERTIES = p -> p
            .strength(50.0f, 1200.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops();

    // Properties for Netherite Bars
    private static final UnaryOperator<BlockBehaviour.Properties> NETHERITE_BARS_PROPERTIES = p -> p
            .mapColor(MapColor.COLOR_BLACK)
            .strength(6.0f, 1200.0f)
            .sound(SoundType.NETHERITE_BLOCK)
            .requiresCorrectToolForDrops()
            .noOcclusion();

    private static final UnaryOperator<BlockBehaviour.Properties> STRUCTURE_PLACER_MACHINE_PROPERTIES = p -> p
            .mapColor(MapColor.METAL)
            .strength(3.0f, 6.0f)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops();

    // Wither Proof Block
    public static final DeferredBlock<WitherProofBlock> WITHER_PROOF_BLOCK = BLOCKS.register("wither_proof_block",
            key -> new WitherProofBlock(assignBlockId(key, WITHER_PROOF_PROPERTIES)));

    // Wither Proof Stairs
    public static final DeferredBlock<StairBlock> WITHER_PROOF_STAIRS = BLOCKS.register("wither_proof_stairs",
            key -> new StairBlock(baseBlockStateForStairs("wither_proof_block"), assignBlockId(key, WITHER_PROOF_PROPERTIES)));

    // Wither Proof Slab
    public static final DeferredBlock<SlabBlock> WITHER_PROOF_SLAB = BLOCKS.register("wither_proof_slab",
            key -> new SlabBlock(assignBlockId(key, WITHER_PROOF_PROPERTIES)));

    // Wither Proof Wall
    public static final DeferredBlock<WallBlock> WITHER_PROOF_WALL = BLOCKS.register("wither_proof_wall",
            key -> new WallBlock(assignBlockId(key, p -> WITHER_PROOF_PROPERTIES.apply(p).forceSolidOn())));

    // Netherite Bars
    public static final DeferredBlock<NetheriteBarsBlock> NETHERITE_BARS = BLOCKS.register("netherite_bars",
            key -> new NetheriteBarsBlock(assignBlockId(key, NETHERITE_BARS_PROPERTIES)));

    // ===== STANDARD VECTOR PLATES (DON'T AFFECT PLAYERS) =====

    public static final DeferredBlock<SlowVectBlock> SLOW_VECT = BLOCKS.register("slow_vect",
            key -> new SlowVectBlock(assignBlockId(key, VECTOR_PROPERTIES)));

    public static final DeferredBlock<ModerateVectBlock> MODERATE_VECT = BLOCKS.register("moderate_vect",
            key -> new ModerateVectBlock(assignBlockId(key, VECTOR_PROPERTIES)));

    public static final DeferredBlock<FastVectBlock> FAST_VECT = BLOCKS.register("fast_vect",
            key -> new FastVectBlock(assignBlockId(key, VECTOR_PROPERTIES)));

    public static final DeferredBlock<ExtremeVectBlock> EXTREME_VECT = BLOCKS.register("extreme_vect",
            key -> new ExtremeVectBlock(assignBlockId(key, VECTOR_PROPERTIES)));

    public static final DeferredBlock<UltraVectBlock> ULTRA_VECT = BLOCKS.register("ultra_vect",
            key -> new UltraVectBlock(assignBlockId(key, VECTOR_PROPERTIES)));

    // ===== PLAYER VECTOR PLATES (AFFECT PLAYERS) =====

    public static final DeferredBlock<PlayerSlowVectBlock> PLAYER_SLOW_VECT = BLOCKS.register("player_slow_vect",
            key -> new PlayerSlowVectBlock(assignBlockId(key, VECTOR_PROPERTIES)));

    public static final DeferredBlock<PlayerModerateVectBlock> PLAYER_MODERATE_VECT = BLOCKS.register("player_moderate_vect",
            key -> new PlayerModerateVectBlock(assignBlockId(key, VECTOR_PROPERTIES)));

    public static final DeferredBlock<PlayerFastVectBlock> PLAYER_FAST_VECT = BLOCKS.register("player_fast_vect",
            key -> new PlayerFastVectBlock(assignBlockId(key, VECTOR_PROPERTIES)));

    public static final DeferredBlock<PlayerExtremeVectBlock> PLAYER_EXTREME_VECT = BLOCKS.register("player_extreme_vect",
            key -> new PlayerExtremeVectBlock(assignBlockId(key, VECTOR_PROPERTIES)));

    public static final DeferredBlock<PlayerUltraVectBlock> PLAYER_ULTRA_VECT = BLOCKS.register("player_ultra_vect",
            key -> new PlayerUltraVectBlock(assignBlockId(key, VECTOR_PROPERTIES)));

    // ===== UTILITY BLOCKS =====

    public static final DeferredBlock<StructurePlacerMachineBlock> STRUCTURE_PLACER_MACHINE = BLOCKS.register("structure_placer_machine",
            key -> new StructurePlacerMachineBlock(assignBlockId(key, STRUCTURE_PLACER_MACHINE_PROPERTIES)));

    public static final DeferredBlock<StructureSaverMachineBlock> STRUCTURE_SAVER_MACHINE = BLOCKS.register("structure_saver_machine",
            key -> new StructureSaverMachineBlock(assignBlockId(key, STRUCTURE_PLACER_MACHINE_PROPERTIES)));

    public static final DeferredBlock<ShopBlock> SHOP = BLOCKS.register("shop",
            key -> new ShopBlock(assignBlockId(key, STRUCTURE_PLACER_MACHINE_PROPERTIES)));

    public static final DeferredBlock<AutoShopBlock> AUTO_SHOP = BLOCKS.register("auto_shop",
            key -> new AutoShopBlock(assignBlockId(key, STRUCTURE_PLACER_MACHINE_PROPERTIES)));

    public static final DeferredBlock<HellfireIgniterBlock> HELLFIRE_IGNITER = BLOCKS.register("hellfire_igniter",
            key -> new HellfireIgniterBlock(assignBlockId(key, HELLFIRE_PROPERTIES)));

    public static final DeferredBlock<FanBlock> FAN = BLOCKS.register("fan",
            key -> new FanBlock(assignBlockId(key, FAN_PROPERTIES)));

    public static final DeferredBlock<RubberSapExtractorBlock> RUBBER_SAP_EXTRACTOR = BLOCKS.register("rubber_sap_extractor",
            key -> new RubberSapExtractorBlock(assignBlockId(key, RUBBER_SAP_EXTRACTOR_PROPERTIES)));

    private static final UnaryOperator<BlockBehaviour.Properties> SMART_TIMER_PROPERTIES = p -> p
            .mapColor(MapColor.STONE)
            .strength(1.5f, 6.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
            .isRedstoneConductor((state, level, pos) -> false);

    public static final DeferredBlock<SmartTimerBlock> SMART_TIMER = BLOCKS.register("smart_timer",
            key -> new SmartTimerBlock(assignBlockId(key, SMART_TIMER_PROPERTIES)));

    // ===== SMOOTH BLACKSTONE =====
    private static final UnaryOperator<BlockBehaviour.Properties> SMOOTH_BLACKSTONE_PROPERTIES = p -> p
            .mapColor(MapColor.COLOR_BLACK)
            .strength(1.5f, 6.0f)
            .sound(SoundType.DEEPSLATE)
            .requiresCorrectToolForDrops();

    public static final DeferredBlock<Block> SMOOTH_BLACKSTONE = BLOCKS.register("smooth_blackstone",
            key -> new Block(assignBlockId(key, SMOOTH_BLACKSTONE_PROPERTIES)));
    public static final DeferredBlock<SlabBlock> SMOOTH_BLACKSTONE_SLAB = BLOCKS.register("smooth_blackstone_slab",
            key -> new SlabBlock(assignBlockId(key, SMOOTH_BLACKSTONE_PROPERTIES)));
    public static final DeferredBlock<StairBlock> SMOOTH_BLACKSTONE_STAIRS = BLOCKS.register("smooth_blackstone_stairs",
            key -> new StairBlock(baseBlockStateForStairs("smooth_blackstone"), assignBlockId(key, SMOOTH_BLACKSTONE_PROPERTIES)));
    public static final DeferredBlock<Block> SMOOTH_BLACKSTONE_WALL = BLOCKS.register("smooth_blackstone_wall",
            key -> new SmoothBlackstoneWallBlock(assignBlockId(key, SMOOTH_BLACKSTONE_PROPERTIES)));

    // ===== RUBBER TREE BLOCKS =====
    private static final UnaryOperator<BlockBehaviour.Properties> RUBBER_LOG_PROPERTIES = p -> p
            .mapColor(MapColor.WOOD)
            .strength(2.0f)
            .sound(SoundType.WOOD);

    private static final UnaryOperator<BlockBehaviour.Properties> SACRED_RUBBER_ROOT_PROPERTIES = p -> p
            .mapColor(MapColor.WOOD)
            .strength(5.0f, 600.0f)
            .sound(SoundType.WOOD);

    private static final UnaryOperator<BlockBehaviour.Properties> RUBBER_LOG_SAP_PROPERTIES = p -> p
            .mapColor(MapColor.WOOD)
            .strength(2.0f)
            .sound(SoundType.WOOD);

    private static final UnaryOperator<BlockBehaviour.Properties> RUBBER_LEAVES_PROPERTIES = p -> p
            .mapColor(MapColor.PLANT)
            .strength(0.2f)
            .sound(SoundType.GRASS)
            .noOcclusion()
            .isValidSpawn((state, level, pos, type) -> false)
            .isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos) -> false);

    private static final UnaryOperator<BlockBehaviour.Properties> RUBBER_SAPLING_PROPERTIES = p -> p
            .mapColor(MapColor.PLANT)
            .noCollision()
            .randomTicks()
            .instabreak()
            .sound(SoundType.GRASS)
            .pushReaction(PushReaction.DESTROY);

    private static final UnaryOperator<BlockBehaviour.Properties> SACRED_RUBBER_SAPLING_PROPERTIES = p -> p
            .mapColor(MapColor.PLANT)
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .pushReaction(PushReaction.DESTROY);

    private static final UnaryOperator<BlockBehaviour.Properties> RUBBER_WOOD_PROPERTIES = p -> p
            .mapColor(MapColor.WOOD)
            .strength(2.0f)
            .sound(SoundType.WOOD);

    private static final UnaryOperator<BlockBehaviour.Properties> RUBBER_PLANKS_PROPERTIES = p -> p
            .mapColor(MapColor.WOOD)
            .strength(2.0f, 3.0f)
            .sound(SoundType.WOOD);

    public static final DeferredBlock<RubberLogBlock> RUBBER_LOG = BLOCKS.register("rubber_log",
            key -> new RubberLogBlock(assignBlockId(key, RUBBER_LOG_PROPERTIES)));

    public static final DeferredBlock<StrippedRubberLogBlock> STRIPPED_RUBBER_LOG = BLOCKS.register("stripped_rubber_log",
            key -> new StrippedRubberLogBlock(assignBlockId(key, RUBBER_LOG_PROPERTIES)));

    public static final DeferredBlock<RubberWoodBlock> RUBBER_WOOD = BLOCKS.register("rubber_wood",
            key -> new RubberWoodBlock(assignBlockId(key, RUBBER_WOOD_PROPERTIES)));

    public static final DeferredBlock<StrippedRubberWoodBlock> STRIPPED_RUBBER_WOOD = BLOCKS.register("stripped_rubber_wood",
            key -> new StrippedRubberWoodBlock(assignBlockId(key, RUBBER_WOOD_PROPERTIES)));

    public static final DeferredBlock<RubberPlanksBlock> RUBBER_PLANKS = BLOCKS.register("rubber_planks",
            key -> new RubberPlanksBlock(assignBlockId(key, RUBBER_PLANKS_PROPERTIES)));

    public static final DeferredBlock<RubberLogFilledBlock> RUBBER_LOG_FILLED = BLOCKS.register("rubber_log_filled",
            key -> new RubberLogFilledBlock(assignBlockId(key, RUBBER_LOG_SAP_PROPERTIES)));

    public static final DeferredBlock<RubberLogEmptyBlock> RUBBER_LOG_EMPTY = BLOCKS.register("rubber_log_empty",
            key -> new RubberLogEmptyBlock(assignBlockId(key, RUBBER_LOG_PROPERTIES)));

    public static final DeferredBlock<LeavesBlock> RUBBER_LEAVES = BLOCKS.register("rubber_leaves",
            key -> new RubberLeavesBlock(assignBlockId(key, RUBBER_LEAVES_PROPERTIES)));

    public static final DeferredBlock<DyeBushEmptyBlock> DYE_BUSH_EMPTY = BLOCKS.register("dye_bush_empty",
            key -> new DyeBushEmptyBlock(assignBlockId(key, RUBBER_LEAVES_PROPERTIES)));
    public static final DeferredBlock<DyeBushFilledBlock> DYE_BUSH_FILLED = BLOCKS.register("dye_bush_filled",
            key -> new DyeBushFilledBlock(assignBlockId(key, RUBBER_LEAVES_PROPERTIES)));

    public static final DeferredBlock<RubberSaplingBlock> RUBBER_SAPLING = BLOCKS.register("rubber_sapling",
            key -> new RubberSaplingBlock(assignBlockId(key, RUBBER_SAPLING_PROPERTIES)));

    public static final DeferredBlock<SacredRubberSaplingBlock> SACRED_RUBBER_SAPLING = BLOCKS.register("sacred_rubber_sapling",
            key -> new SacredRubberSaplingBlock(assignBlockId(key, SACRED_RUBBER_SAPLING_PROPERTIES)));

    public static final DeferredBlock<SacredRubberRootBlock> SACRED_RUBBER_ROOT = BLOCKS.register("sacred_rubber_root",
            key -> new SacredRubberRootBlock(assignBlockId(key, SACRED_RUBBER_ROOT_PROPERTIES)));

    public static final DeferredBlock<RubberLogSacredBlock> RUBBER_LOG_SACRED = BLOCKS.register("rubber_log_sacred",
            key -> new RubberLogSacredBlock(assignBlockId(key, RUBBER_LOG_PROPERTIES)));

    public static final DeferredBlock<RubberBlock> RUBBER_BLOCK = BLOCKS.register("rubber_block",
            key -> new RubberBlock(assignBlockId(key, p -> p
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(2.0f)
                    .sound(SoundType.TUFF))));

    public static final DeferredBlock<RubberStairsBlock> RUBBER_STAIRS = BLOCKS.register("rubber_stairs",
            key -> new RubberStairsBlock(baseBlockStateForStairs("rubber_planks"), assignBlockId(key, RUBBER_PLANKS_PROPERTIES)));

    public static final DeferredBlock<RubberSlabBlock> RUBBER_SLAB = BLOCKS.register("rubber_slab",
            key -> new RubberSlabBlock(assignBlockId(key, RUBBER_PLANKS_PROPERTIES)));

    public static final DeferredBlock<RubberFenceBlock> RUBBER_FENCE = BLOCKS.register("rubber_fence",
            key -> new RubberFenceBlock(assignBlockId(key, RUBBER_PLANKS_PROPERTIES)));

    public static final DeferredBlock<RubberFenceGateBlock> RUBBER_FENCE_GATE = BLOCKS.register("rubber_fence_gate",
            key -> new RubberFenceGateBlock(assignBlockId(key, RUBBER_PLANKS_PROPERTIES)));

    public static final DeferredBlock<RubberButtonBlock> RUBBER_BUTTON = BLOCKS.register("rubber_button",
            key -> new RubberButtonBlock(assignBlockId(key, p -> RUBBER_PLANKS_PROPERTIES.apply(p).noCollision())));

    public static final DeferredBlock<RubberPressurePlateBlock> RUBBER_PRESSURE_PLATE = BLOCKS.register("rubber_pressure_plate",
            key -> new RubberPressurePlateBlock(assignBlockId(key, p -> RUBBER_PLANKS_PROPERTIES.apply(p).noCollision())));

    public static final DeferredBlock<RubberDoorBlock> RUBBER_DOOR = BLOCKS.register("rubber_door",
            key -> new RubberDoorBlock(assignBlockId(key, p -> RUBBER_PLANKS_PROPERTIES.apply(p).noOcclusion())));

    public static final DeferredBlock<RubberTrapDoorBlock> RUBBER_TRAPDOOR = BLOCKS.register("rubber_trapdoor",
            key -> new RubberTrapDoorBlock(assignBlockId(key, p -> RUBBER_PLANKS_PROPERTIES.apply(p).noOcclusion())));

    public static final DeferredBlock<PlateBaseBlock> PLATE_BASE_BLOCK = BLOCKS.register("plate_base_block",
            key -> new PlateBaseBlock(assignBlockId(key, VECTOR_PROPERTIES)));

    private static final UnaryOperator<BlockBehaviour.Properties> RAFT_PROPERTIES = p -> p
            .mapColor(MapColor.WOOD)
            .strength(0.5f, 1.0f)
            .sound(SoundType.WOOD)
            .dynamicShape()
            .isSuffocating((state, level, pos) -> false);

    public static final DeferredBlock<RaftBlock> RAFT = BLOCKS.register("raft",
            key -> new RaftBlock(assignBlockId(key, RAFT_PROPERTIES)));

    public static final DeferredBlock<RaftNoDropBlock> RAFT_NO_DROP = BLOCKS.register("raft_no_drop",
            key -> new RaftNoDropBlock(assignBlockId(key, RAFT_PROPERTIES)));

    public static final DeferredBlock<TarSlimeBlock> TAR_SLIME_BLOCK = BLOCKS.register("tar_slime_block",
            key -> new TarSlimeBlock(assignBlockId(key, p -> p
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(0.5f, 1.0f)
                    .sound(SoundType.SLIME_BLOCK)
                    .friction(0.8f)
                    .jumpFactor(0.5f)
                    .noOcclusion())));

    public static final DeferredBlock<SapBlock> SAP_BLOCK = BLOCKS.register("sap_block",
            key -> new SapBlock(assignBlockId(key, p -> p
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(0.5f, 1.0f)
                    .sound(SoundType.SLIME_BLOCK)
                    .friction(0.8f)
                    .jumpFactor(0.5f)
                    .noOcclusion())));

    public static final DeferredBlock<WeatherDetectorBlock> WEATHER_DETECTOR = BLOCKS.register("weather_detector",
            key -> new WeatherDetectorBlock(assignBlockId(key, p -> p
                    .strength(0.5f, 1.0f)
                    .sound(SoundType.COPPER)
                    .noOcclusion())));

    public static final DeferredBlock<SoundMufflerBlock> SOUND_MUFFLER = BLOCKS.register("sound_muffler",
            key -> new SoundMufflerBlock(assignBlockId(key, p -> p
                    .mapColor(MapColor.WOOL)
                    .strength(0.8f, 1.0f)
                    .sound(SoundType.WOOL))));

    public static final DeferredBlock<WeatherAltererBlock> WEATHER_ALTERER = BLOCKS.register("weather_alterer",
            key -> new WeatherAltererBlock(assignBlockId(key, p -> p
                    .strength(1.5F)
                    .sound(SoundType.COPPER)
                    .lightLevel((state) -> 3)
                    .noOcclusion())));

    public static final DeferredBlock<TimeAltererBlock> TIME_ALTERER = BLOCKS.register("time_alterer",
            key -> new TimeAltererBlock(assignBlockId(key, p -> p
                    .strength(1.5F)
                    .sound(SoundType.TRIAL_SPAWNER)
                    .lightLevel((state) -> 3)
                    .noOcclusion())));

    public static final DeferredBlock<TemporalOverclockerBlock> TEMPORAL_OVERCLOCKER = BLOCKS.register("temporal_overclocker",
            key -> new TemporalOverclockerBlock(assignBlockId(key, p -> p
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()
                    .lightLevel((state) -> state.getValue(TemporalOverclockerBlock.POWERED) ? 7 : 0))));

    public static final DeferredBlock<AngelBlock> ANGEL_BLOCK = BLOCKS.register("angel_block",
            key -> new AngelBlock(assignBlockId(key, p -> p
                    .mapColor(MapColor.STONE)
                    .strength(0.0F)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false)
                    .pushReaction(PushReaction.DESTROY)
                    .isViewBlocking((state, level, pos) -> false))));

    public static final DeferredBlock<ChaoticTntBlock> CHAOTIC_TNT = BLOCKS.register("chaotic_tnt",
            key -> new ChaoticTntBlock(assignBlockId(key, p -> p.strength(0.0F).sound(SoundType.GRASS).ignitedByLava())));

    private static final UnaryOperator<BlockBehaviour.Properties> BURNING_FLAME_PROPERTIES = p -> p
            .mapColor(MapColor.COLOR_ORANGE)
            .noCollision()
            .instabreak()
            .sound(SoundType.WOOL)
            .lightLevel((state) -> 15)
            .noOcclusion()
            .replaceable()
            .isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos) -> false);

    public static final DeferredBlock<BurningFlameBlock> BURNING_FLAME = BLOCKS.register("burning_flame",
            key -> new BurningFlameBlock(assignBlockId(key, BURNING_FLAME_PROPERTIES)));

    private static final UnaryOperator<BlockBehaviour.Properties> REDSTONE_SIGNAL_PROPERTIES = p -> p
            .mapColor(MapColor.NONE)
            .noCollision()
            .instabreak()
            .sound(SoundType.STONE)
            .noOcclusion()
            .replaceable()
            .isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos) -> false);

    public static final DeferredBlock<RedstoneSignalBlock> REDSTONE_ACTIVATOR_SIGNAL = BLOCKS.register("redstone_activator_signal",
            key -> new RedstoneSignalBlock(assignBlockId(key, REDSTONE_SIGNAL_PROPERTIES)));

    private static final UnaryOperator<BlockBehaviour.Properties> DEEP_DRAWERS_PROPERTIES = p -> p
            .mapColor(MapColor.METAL)
            .strength(3.5f, 6.0f)
            .sound(SoundType.COPPER)
            .requiresCorrectToolForDrops()
            .isRedstoneConductor((state, level, pos) -> false);

    public static final DeferredBlock<DeepDrawersBlock> DEEP_DRAWERS = BLOCKS.register("deep_drawer",
            key -> new DeepDrawersBlock(assignBlockId(key, DEEP_DRAWERS_PROPERTIES)));

    public static final DeferredBlock<DeepDrawerExtractorBlock> DEEP_DRAWER_EXTRACTOR = BLOCKS.register("deep_drawer_extractor",
            key -> new DeepDrawerExtractorBlock(assignBlockId(key, DEEP_DRAWERS_PROPERTIES)));

    public static final DeferredBlock<DeepDrawerInterfaceBlock> DEEP_DRAWER_INTERFACE = BLOCKS.register("deep_drawer_interface",
            key -> new DeepDrawerInterfaceBlock(assignBlockId(key, DEEP_DRAWERS_PROPERTIES)));

    public static final DeferredBlock<DeepDrawerExtenderBlock> DEEP_DRAWER_EXTENDER = BLOCKS.register("deep_drawer_extender",
            key -> new DeepDrawerExtenderBlock(assignBlockId(key, DEEP_DRAWERS_PROPERTIES)));

    private static final UnaryOperator<BlockBehaviour.Properties> GIFT_PROPERTIES = p -> p
            .noCollision()
            .instabreak()
            .sound(SoundType.WOOL)
            .pushReaction(PushReaction.DESTROY)
            .noOcclusion()
            .isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos) -> false);

    public static final DeferredBlock<Block> GIFT = BLOCKS.register("gift",
            key -> new Block(assignBlockId(key, GIFT_PROPERTIES)));

    private static final UnaryOperator<BlockBehaviour.Properties> HARD_ICE_PROPERTIES = p -> p
            .strength(-1.0f, 3600000.0f)
            .sound(SoundType.GLASS)
            .friction(0.98f);

    public static final DeferredBlock<HardIceBlock> HARD_ICE = BLOCKS.register("hard_ice",
            key -> new HardIceBlock(assignBlockId(key, HARD_ICE_PROPERTIES)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
