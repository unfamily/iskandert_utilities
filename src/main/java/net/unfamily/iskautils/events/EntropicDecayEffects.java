package net.unfamily.iskautils.events;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.unfamily.iskautils.effect.ModMobEffects;

@EventBusSubscriber
public final class EntropicDecayEffects {
    private EntropicDecayEffects() {}

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }

        MobEffectInstance expired = event.getEffectInstance();
        if (expired.getEffect().is(ModMobEffects.ENTROPIC_DECAY)) {
            return;
        }

        MobEffectInstance decay = entity.getEffect(ModMobEffects.ENTROPIC_DECAY);
        if (decay == null) {
            return;
        }

        boolean eligible = expired.getEffect().value().getCategory() == MobEffectCategory.HARMFUL
                || expired.getEffect().is(ModMobEffects.CURSE_OF_PAIN);
        if (!eligible) {
            return;
        }

        if (entity.getRandom().nextFloat() >= 0.30F) {
            return;
        }

        int amp = decay.getAmplifier();
        int xSec = 10 + 10 * amp;
        int ySec = 30 + 10 * amp;
        int seconds = xSec + entity.getRandom().nextInt(ySec - xSec + 1);
        entity.addEffect(new MobEffectInstance(
                expired.getEffect(),
                seconds * 20,
                expired.getAmplifier()));
    }
}
