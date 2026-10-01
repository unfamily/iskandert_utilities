package net.unfamily.iskautils.events;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.EffectParticleModificationEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.effect.ModMobEffects;
import net.unfamily.iskautils.particle.ModParticles;

/**
 * Ensures entropic effects sync {@link ModParticles#ENTROPIC_FLAME} via entity effect-particle data.
 * Remote mobs do not receive full effect packets; particles must stay visible in DATA_EFFECT_PARTICLES.
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class EntropicEffectParticleEvents {
    private EntropicEffectParticleEvents() {}

    @SubscribeEvent
    public static void onEffectParticle(EffectParticleModificationEvent event) {
        Holder<MobEffect> effect = event.getEffect().getEffect();
        if (effect.is(ModMobEffects.ENTROPIC_EMPOWERMENT)
                || effect.is(ModMobEffects.ENTROPIC_EMPOWERMENT_PLAYER)
                || effect.is(ModMobEffects.ENTROPIC_DECAY)) {
            event.setVisible(true);
            event.setParticleOptions(ModParticles.ENTROPIC_FLAME.get());
        }
    }
}
