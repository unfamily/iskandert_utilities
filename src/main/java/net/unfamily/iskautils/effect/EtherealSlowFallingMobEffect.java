package net.unfamily.iskautils.effect;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.unfamily.iskautils.particle.ModParticles;

/**
 * Ethereal slow-falling marker effect. Fall slowing is applied only while descending
 * (see {@link net.unfamily.iskautils.events.EtherealSlowFallingEffects}) so jumping is not boosted.
 */
public class EtherealSlowFallingMobEffect extends MobEffect {
    public EtherealSlowFallingMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x7FE4FF);
        // No GRAVITY attribute: a permanent -0.07 lets players jump higher for the full duration.
    }

    @Override
    public ParticleOptions createParticleOptions(MobEffectInstance instance) {
        return ModParticles.ETHEREAL_SCULK_SOUL.get();
    }
}
