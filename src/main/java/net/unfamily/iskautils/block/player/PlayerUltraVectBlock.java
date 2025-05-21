package net.unfamily.iskautils.block.player;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.VectorBlock;

public class PlayerUltraVectBlock extends VectorBlock {
    public static final MapCodec<PlayerUltraVectBlock> CODEC = simpleCodec(PlayerUltraVectBlock::new);

    public PlayerUltraVectBlock(BlockBehaviour.Properties properties) {
        super(properties, () -> Config.ultraVectorSpeed, true);
    }

    @Override
    protected MapCodec<? extends PlayerUltraVectBlock> codec() {
        return CODEC;
    }
} 