package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class SmoothBlackstoneWallBlock extends WallBlock {
    public static final MapCodec<SmoothBlackstoneWallBlock> CODEC = simpleCodec(SmoothBlackstoneWallBlock::new);

    public SmoothBlackstoneWallBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
} 