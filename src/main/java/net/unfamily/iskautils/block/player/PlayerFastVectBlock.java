package net.unfamily.iskautils.block.player;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.VectorBlock;

public class PlayerFastVectBlock extends VectorBlock {
    public static final MapCodec<PlayerFastVectBlock> CODEC = simpleCodec(PlayerFastVectBlock::new);

    public PlayerFastVectBlock(BlockBehaviour.Properties properties) {
        super(properties, () -> Config.fastVectorSpeed, true);
    }

    @Override
    protected MapCodec<? extends PlayerFastVectBlock> codec() {
        return CODEC;
    }
} 