package net.unfamily.iskautils.block.standard;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.VectorBlock;

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