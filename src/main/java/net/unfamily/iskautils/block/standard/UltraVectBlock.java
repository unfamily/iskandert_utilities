package net.unfamily.iskautils.block.standard;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.VectorBlock;

public class UltraVectBlock extends VectorBlock {
    public static final MapCodec<UltraVectBlock> CODEC = simpleCodec(UltraVectBlock::new);

    public UltraVectBlock(BlockBehaviour.Properties properties) {
        super(properties, () -> Config.ultraVectorSpeed);
    }

    @Override
    protected MapCodec<? extends UltraVectBlock> codec() {
        return CODEC;
    }
} 