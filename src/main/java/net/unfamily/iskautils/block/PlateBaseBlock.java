package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class PlateBaseBlock extends VectorBlock {
    public static final MapCodec<PlateBaseBlock> CODEC = simpleCodec(PlateBaseBlock::new);

    public PlateBaseBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends PlateBaseBlock> codec() {
        return CODEC;
    }
} 