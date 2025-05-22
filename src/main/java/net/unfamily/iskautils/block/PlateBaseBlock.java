package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class PlateBaseBlock extends VectorBlock {
    public static final MapCodec<PlateBaseBlock> CODEC = simpleCodec(PlateBaseBlock::new);

    public PlateBaseBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends PlateBaseBlock> codec() {
        return CODEC;
    }
    
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // Do nothing - this plate is neutral and does not move any entities
    }
} 