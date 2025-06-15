package net.unfamily.iskautils.block;

import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

public class NetheriteBarsBlock extends IronBarsBlock {
    public NetheriteBarsBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
    
	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 0;
	}
} 