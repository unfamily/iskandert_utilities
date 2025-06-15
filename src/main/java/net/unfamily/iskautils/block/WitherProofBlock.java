package net.unfamily.iskautils.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.Explosion;

public class WitherProofBlock extends Block {
    public WitherProofBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
    
    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return explosion.getDirectSourceEntity() instanceof WitherBoss ? false : super.dropFromExplosion(explosion);
    }
} 