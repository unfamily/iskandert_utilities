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

    /** Empowerment is for mobs only; players lose it on the next tick (e.g. creeper splash). */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        if (player.hasEffect(ModMobEffects.ENTROPIC_EMPOWERMENT)) {
            player.removeEffect(ModMobEffects.ENTROPIC_EMPOWERMENT);
        }
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.getEffect(ModMobEffects.ENTROPIC_EMPOWERMENT) != null && Config.entropicEmpowermentDamageReduction > 0.0D) {
            float reduction = (float) Config.entropicEmpowermentDamageReduction;
            event.setAmount(event.getAmount() * Math.max(0.0F, 1.0F - reduction));
        }

        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        MobEffectInstance inst = attacker.getEffect(ModMobEffects.ENTROPIC_EMPOWERMENT);
        if (inst == null || Config.entropicEmpowermentDamageBonus <= 0.0D) {
            return;
        }
        float bonus = (float) Config.entropicEmpowermentDamageBonus;
        event.setAmount(event.getAmount() * (1.0F + bonus));
    }
}
