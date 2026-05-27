package net.unfamily.iskautils.block;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cursed variant of burning flame: always ignites entities inside, never drops.
 */
public class CursedBurningFlameBlock extends BurningFlameBlock {

    public CursedBurningFlameBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void entityInside(BlockState state, Level level, net.minecraft.core.BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity livingEntity) {
            livingEntity.setRemainingFireTicks(5 * 20);
        }
    }
}
