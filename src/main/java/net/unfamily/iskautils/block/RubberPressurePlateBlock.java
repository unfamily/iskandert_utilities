package net.unfamily.iskautils.block;

import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.unfamily.iskautils.util.ModWoodTypes;

public class RubberPressurePlateBlock extends PressurePlateBlock {
    
    public RubberPressurePlateBlock(Properties properties) {
        super(ModWoodTypes.RUBBER_SET_TYPE, properties);
    }
    
    @Override
    public int getLightBlock(BlockState state, BlockGetter world, BlockPos pos) {
        return 0;
    }
} 