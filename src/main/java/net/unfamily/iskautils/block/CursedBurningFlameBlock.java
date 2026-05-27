package net.unfamily.iskautils.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
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
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier effectApplier,
            boolean isPrecise) {
        if (!level.isClientSide() && entity instanceof LivingEntity livingEntity) {
            livingEntity.setRemainingFireTicks(5 * 20);
        }
    }
}
