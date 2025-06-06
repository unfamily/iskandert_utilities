package net.unfamily.iskautils.block;

import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.unfamily.iskautils.util.ModWoodTypes;

public class RubberTrapDoorBlock extends TrapDoorBlock {
    
    public RubberTrapDoorBlock(Properties properties) {
        super(ModWoodTypes.RUBBER_SET_TYPE, properties);
    }
    
    @Override
    public int getLightBlock(BlockState state, BlockGetter world, BlockPos pos) {
        return 0;
    }
} 