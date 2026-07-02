package net.unfamily.iskautils.block;

import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ObscureGlassBlock extends TransparentBlock {
    public ObscureGlassBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return explosion.getDirectSourceEntity() instanceof WitherBoss ? false : super.dropFromExplosion(explosion);
    }
}
