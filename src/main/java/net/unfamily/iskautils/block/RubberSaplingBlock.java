package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.worldgen.tree.ModTreeGrowers;
import org.jetbrains.annotations.NotNull;

/**
 * Class for the rubber tree sapling.
 * Extends SaplingBlock to use Minecraft's standard tree growth system.
 */
public class RubberSaplingBlock extends SaplingBlock {
    public static final MapCodec<RubberSaplingBlock> CODEC = simpleCodec(RubberSaplingBlock::new);
    
    public RubberSaplingBlock(Properties properties) {
        super(ModTreeGrowers.RUBBER, properties);
    }
    
    @Override
    public @NotNull MapCodec<? extends SaplingBlock> codec() {
        return CODEC;
    }
    
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        // Check that the ground below is valid (not water or lava)
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || 
               state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL) || 
               state.is(Blocks.FARMLAND);
    }
} 