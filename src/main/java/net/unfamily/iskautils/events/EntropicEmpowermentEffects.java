package net.unfamily.iskautils.events;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.effect.ModMobEffects;

@EventBusSubscriber
public final class EntropicEmpowermentEffects {
    private EntropicEmpowermentEffects() {}

    /** Mob empowerment is mob-only; players lose it on the next tick (e.g. creeper splash). */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        if (player.hasEffect(ModMobEffects.ENTROPIC_EMPOWERMENT)) {
            player.removeEffect(ModMobEffects.ENTROPIC_EMPOWERMENT);
        }

        // Player empowerment cannot exist without decay (2x remaining empowerment duration).
        MobEffectInstance empowerment = player.getEffect(ModMobEffects.ENTROPIC_EMPOWERMENT_PLAYER);
        if (empowerment != null && !player.hasEffect(ModMobEffects.ENTROPIC_DECAY)) {
            int remaining = empowerment.getDuration();
            int decayDuration = remaining == MobEffectInstance.INFINITE_DURATION
                    ? MobEffectInstance.INFINITE_DURATION
                    : Math.max(1, remaining * 2);
            player.addEffect(new MobEffectInstance(
                    ModMobEffects.ENTROPIC_DECAY,
                    decayDuration,
                    empowerment.getAmplifier()));
        }
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (isEmpowered(target) && Config.entropicEmpowermentDamageReduction > 0.0D) {
            float reduction = (float) Config.entropicEmpowermentDamageReduction;
            event.setAmount(event.getAmount() * Math.max(0.0F, 1.0F - reduction));
        }

        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        if (!isEmpowered(attacker) || Config.entropicEmpowermentDamageBonus <= 0.0D) {
            return;
        }
        float bonus = (float) Config.entropicEmpowermentDamageBonus;
        event.setAmount(event.getAmount() * (1.0F + bonus));
    }

    private static boolean isEmpowered(LivingEntity entity) {
        return entity.getEffect(ModMobEffects.ENTROPIC_EMPOWERMENT) != null
                || entity.getEffect(ModMobEffects.ENTROPIC_EMPOWERMENT_PLAYER) != null;
    }
}
