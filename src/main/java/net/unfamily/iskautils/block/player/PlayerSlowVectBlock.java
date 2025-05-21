package net.unfamily.iskautils.block.player;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.VectorBlock;

public class PlayerSlowVectBlock extends VectorBlock {
    public static final MapCodec<PlayerSlowVectBlock> CODEC = simpleCodec(PlayerSlowVectBlock::new);

    public PlayerSlowVectBlock(BlockBehaviour.Properties properties) {
        super(properties, () -> Config.slowVectorSpeed, true);
    }

    @Override
    protected MapCodec<? extends PlayerSlowVectBlock> codec() {
        return CODEC;
    }
} 