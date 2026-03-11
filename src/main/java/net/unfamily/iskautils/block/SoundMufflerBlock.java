package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Block that mutes sounds within a radius when placed.
 * Radius and behaviour can be evolved later (e.g. configurable range, filters).
 */
public class SoundMufflerBlock extends Block {

    public static final MapCodec<SoundMufflerBlock> CODEC = simpleCodec(SoundMufflerBlock::new);

    public SoundMufflerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
}
