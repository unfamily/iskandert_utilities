package net.unfamily.iskautils.block.standard;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.VectorBlock;

public class SlowVectBlock extends VectorBlock {
    public static final MapCodec<SlowVectBlock> CODEC = simpleCodec(SlowVectBlock::new);

    public SlowVectBlock(BlockBehaviour.Properties properties) {
        super(properties, () -> Config.slowVectorSpeed);
    }

    @Override
    protected MapCodec<? extends SlowVectBlock> codec() {
        return CODEC;
    }
} 