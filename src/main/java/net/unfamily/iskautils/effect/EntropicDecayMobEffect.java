package net.unfamily.iskautils.effect;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.unfamily.iskautils.particle.ModParticles;

/** Harmful partner of player entropic empowerment; drives debuff/curse re-roll on expire. */
public class EntropicDecayMobEffect extends MobEffect {
    public EntropicDecayMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x9424A4);
    }

    @Override
    public ParticleOptions createParticleOptions(MobEffectInstance instance) {
        return ModParticles.ENTROPIC_FLAME.get();
    }
}
