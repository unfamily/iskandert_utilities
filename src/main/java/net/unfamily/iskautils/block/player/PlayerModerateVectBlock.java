package net.unfamily.iskautils.block.player;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.VectorBlock;

public class PlayerModerateVectBlock extends VectorBlock {
    public static final MapCodec<PlayerModerateVectBlock> CODEC = simpleCodec(PlayerModerateVectBlock::new);

    public PlayerModerateVectBlock(BlockBehaviour.Properties properties) {
        super(properties, () -> Config.moderateVectorSpeed, true);
    }

    @Override
    protected MapCodec<? extends PlayerModerateVectBlock> codec() {
        return CODEC;
    }
} 