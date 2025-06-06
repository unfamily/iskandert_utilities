package net.unfamily.iskautils.block;

import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.unfamily.iskautils.util.ModWoodTypes;

public class RubberFenceGateBlock extends FenceGateBlock {
    
    public RubberFenceGateBlock(Properties properties) {
        super(ModWoodTypes.RUBBER, properties);
    }
    
    @Override
    public int getLightBlock(BlockState state, BlockGetter world, BlockPos pos) {
        return 0;
    }
    
    @Override
    public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
        return 20;
    }
} 