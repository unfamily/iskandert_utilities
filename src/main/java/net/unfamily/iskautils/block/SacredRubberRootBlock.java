package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Sacred rubber root block placed at the base of the sacred rubber tree.
 * Replaces the sapling when growth completes.
 */
public class SacredRubberRootBlock extends Block {
    public static final MapCodec<SacredRubberRootBlock> CODEC = simpleCodec(SacredRubberRootBlock::new);
    
    public SacredRubberRootBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    public @NotNull MapCodec<? extends Block> codec() {
        return CODEC;
    }
    
    @Override
    public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
        return 15; // Same as rubber logs
    }
    
    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
        return 10; // Same as rubber logs
    }
}
