package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.unfamily.iskautils.Config;

public class FastVectBlock extends VectorBlock {
    public static final MapCodec<FastVectBlock> CODEC = simpleCodec(FastVectBlock::new);

    public FastVectBlock(BlockBehaviour.Properties properties) {
        super(properties, () -> Config.fastVectorSpeed);
    }

    @Override
    protected MapCodec<? extends FastVectBlock> codec() {
        return CODEC;
    }
} 