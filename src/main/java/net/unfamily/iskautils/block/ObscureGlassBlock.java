package net.unfamily.iskautils.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ObscureGlassBlock extends TransparentBlock {
    public ObscureGlassBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return level.getMaxLightLevel();
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return explosion.getDirectSourceEntity() instanceof WitherBoss ? false : super.dropFromExplosion(explosion);
    }
}
