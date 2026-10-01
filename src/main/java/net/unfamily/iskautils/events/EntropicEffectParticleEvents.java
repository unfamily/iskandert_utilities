package net.unfamily.iskautils.events;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.EffectParticleModificationEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.unfamily.iskautils.effect.ModMobEffects;
import net.unfamily.iskautils.particle.ModParticles;

/**
 * Replaces vanilla status swirls with entropic flames; decay spawns fewer particles than empowerment.
 * Particles spawn around the torso/legs, not at eye height.
 */
@EventBusSubscriber
public final class EntropicEffectParticleEvents {
    private static final int EMPOWERMENT_CHANCE = 4;
    private static final int DECAY_CHANCE = 14;

    private EntropicEffectParticleEvents() {}

    @SubscribeEvent
    public static void onEffectParticle(EffectParticleModificationEvent event) {
        Holder<MobEffect> effect = event.getEffect().getEffect();
        if (effect.is(ModMobEffects.ENTROPIC_EMPOWERMENT)
                || effect.is(ModMobEffects.ENTROPIC_EMPOWERMENT_PLAYER)
                || effect.is(ModMobEffects.ENTROPIC_DECAY)) {
            // Suppress vanilla swirl; ambient flames are spawned in onEntityTick.
            event.setVisible(false);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        if (!living.level().isClientSide()) {
            return;
        }

        boolean empowered = living.hasEffect(ModMobEffects.ENTROPIC_EMPOWERMENT)
                || living.hasEffect(ModMobEffects.ENTROPIC_EMPOWERMENT_PLAYER);
        if (empowered && living.getRandom().nextInt(EMPOWERMENT_CHANCE) == 0) {
            spawnFlame(living);
        }

        if (living.hasEffect(ModMobEffects.ENTROPIC_DECAY)
                && living.getRandom().nextInt(DECAY_CHANCE) == 0) {
            spawnFlame(living);
        }
    }

    private static void spawnFlame(LivingEntity living) {
        living.level().addParticle(
                ModParticles.ENTROPIC_FLAME.get(),
                living.getRandomX(0.5D),
                bodyParticleY(living),
                living.getRandomZ(0.5D),
                0.0D,
                0.0D,
                0.0D);
    }

    /** Random Y between feet and mid-torso, always below eye height. */
    private static double bodyParticleY(LivingEntity living) {
        double minY = living.getY() + 0.1D;
        double maxY = Math.min(
                living.getEyeY() - 0.4D,
                living.getY() + living.getBbHeight() * 0.55D);
        if (maxY <= minY) {
            return minY;
        }
        return minY + living.getRandom().nextDouble() * (maxY - minY);
    }
}
