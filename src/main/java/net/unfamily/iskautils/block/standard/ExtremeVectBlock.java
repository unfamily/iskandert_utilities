package net.unfamily.iskautils.block.standard;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.VectorBlock;

public class ExtremeVectBlock extends VectorBlock {
    public static final MapCodec<ExtremeVectBlock> CODEC = simpleCodec(ExtremeVectBlock::new);

    public ExtremeVectBlock(BlockBehaviour.Properties properties) {
        super(properties, () -> Config.extremeVectorSpeed);
    }

    @Override
    protected MapCodec<? extends ExtremeVectBlock> codec() {
        return CODEC;
    }
} 