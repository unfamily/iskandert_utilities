package net.unfamily.iskautils.block.player;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.VectorBlock;

public class PlayerExtremeVectBlock extends VectorBlock {
    public static final MapCodec<PlayerExtremeVectBlock> CODEC = simpleCodec(PlayerExtremeVectBlock::new);

    public PlayerExtremeVectBlock(BlockBehaviour.Properties properties) {
        super(properties, () -> Config.extremeVectorSpeed, true);
    }

    @Override
    protected MapCodec<? extends PlayerExtremeVectBlock> codec() {
        return CODEC;
    }
} 