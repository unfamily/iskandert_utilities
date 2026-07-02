package net.unfamily.iskautils.block;

import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ObscureGlassBlock extends TransparentBlock {
    public ObscureGlassBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return false;
    }

    @Override
    protected int getLightDampening(BlockState state) {
        return 15;
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return explosion.getDirectSourceEntity() instanceof WitherBoss ? false : super.dropFromExplosion(explosion);
    }
}
