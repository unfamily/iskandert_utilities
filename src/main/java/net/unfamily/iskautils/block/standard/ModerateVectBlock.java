package net.unfamily.iskautils.block.standard;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.VectorBlock;

public class ModerateVectBlock extends VectorBlock {
    public static final MapCodec<ModerateVectBlock> CODEC = simpleCodec(ModerateVectBlock::new);

    public ModerateVectBlock(BlockBehaviour.Properties properties) {
        super(properties, () -> Config.moderateVectorSpeed);
    }

    @Override
    protected MapCodec<? extends ModerateVectBlock> codec() {
        return CODEC;
    }
} 