package net.unfamily.iskautils.events;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.effect.ModMobEffects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Entropic Decay: when a harmful effect/curse expires, chance to re-apply after 1 tick
 * (same-tick re-add during {@link MobEffectEvent.Expired} is wiped by vanilla's iterator.remove).
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class EntropicDecayEffects {
    private static final float REAPPLY_CHANCE = 0.30F;
    private static final long APPLY_DELAY_TICKS = 1L;
    private static final List<PendingReapply> PENDING = new ArrayList<>();

    private EntropicDecayEffects() {}

    private record PendingReapply(
            UUID entityId,
            Holder<MobEffect> effect,
            int amplifier,
            boolean ambient,
            boolean visible,
            boolean showIcon,
            int durationTicks,
            long applyAtGameTime) {}

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }

        MobEffectInstance expired = event.getEffectInstance();
        if (expired == null) {
            return;
        }
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

        if (entity.getRandom().nextFloat() >= REAPPLY_CHANCE) {
            return;
        }

        int amp = decay.getAmplifier();
        int xSec = 10 + 10 * amp;
        int ySec = 30 + 10 * amp;
        int seconds = xSec + entity.getRandom().nextInt(ySec - xSec + 1);

        PENDING.add(new PendingReapply(
                entity.getUUID(),
                expired.getEffect(),
                expired.getAmplifier(),
                expired.isAmbient(),
                expired.isVisible(),
                expired.showIcon(),
                seconds * 20,
                entity.level().getGameTime() + APPLY_DELAY_TICKS));
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        if (living.level().isClientSide() || PENDING.isEmpty()) {
            return;
        }

        long now = living.level().getGameTime();
        UUID id = living.getUUID();
        Iterator<PendingReapply> it = PENDING.iterator();
        while (it.hasNext()) {
            PendingReapply pending = it.next();
            if (!pending.entityId().equals(id)) {
                continue;
            }
            if (now < pending.applyAtGameTime()) {
                continue;
            }
            it.remove();
            if (!living.isAlive()) {
                continue;
            }
            // Decay must still be active when the delayed re-apply lands.
            if (!living.hasEffect(ModMobEffects.ENTROPIC_DECAY)) {
                continue;
            }
            living.addEffect(new MobEffectInstance(
                    pending.effect(),
                    pending.durationTicks(),
                    pending.amplifier(),
                    pending.ambient(),
                    pending.visible(),
                    pending.showIcon()));
        }
    }
}
