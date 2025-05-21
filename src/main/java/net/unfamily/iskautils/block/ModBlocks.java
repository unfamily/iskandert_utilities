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

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IskaUtils.MOD_ID);

    // Common properties for all vector blocks
    private static final BlockBehaviour.Properties VECTOR_PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(0.5f, 1.0f)
            .sound(SoundType.METAL)
            .noOcclusion()
            .noCollission()
            .isRedstoneConductor((state, level, pos) -> false)
            .pushReaction(PushReaction.DESTROY)
            .isViewBlocking((state, level, pos) -> false)
            .lightLevel((state) -> 0);
    
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

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
} 