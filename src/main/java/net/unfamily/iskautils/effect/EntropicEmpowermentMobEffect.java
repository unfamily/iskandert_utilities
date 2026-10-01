package net.unfamily.iskautils.effect;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.unfamily.iskautils.particle.ModParticles;

/** Marker effect; combat bonuses handled in {@link net.unfamily.iskautils.events.EntropicEmpowermentEffects}. */
public class EntropicEmpowermentMobEffect extends MobEffect {
    public EntropicEmpowermentMobEffect() {
        super(MobEffectCategory.NEUTRAL, 0x9424A4);
    }

    @Override
    public ParticleOptions createParticleOptions(MobEffectInstance instance) {
        return ModParticles.ENTROPIC_FLAME.get();
    }
}
