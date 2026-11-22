package net.unfamily.iskautils.events;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.effect.ModMobEffects;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Fall-damage immunity and 3s continuous ground-clear for ethereal_slow_falling.
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class EtherealSlowFallingEffects {
    private static final int GROUND_CLEAR_TICKS = 60;
    private static final Map<UUID, Integer> GROUND_TICKS = new HashMap<>();

    private EtherealSlowFallingEffects() {}

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity().hasEffect(ModMobEffects.ETHEREAL_SLOW_FALLING)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide()) {
            return;
        }
        if (!living.hasEffect(ModMobEffects.ETHEREAL_SLOW_FALLING)) {
            GROUND_TICKS.remove(living.getUUID());
            return;
        }
        UUID id = living.getUUID();
        if (living.onGround()) {
            int ticks = GROUND_TICKS.getOrDefault(id, 0) + 1;
            if (ticks >= GROUND_CLEAR_TICKS) {
                living.removeEffect(ModMobEffects.ETHEREAL_SLOW_FALLING);
                GROUND_TICKS.remove(id);
            } else {
                GROUND_TICKS.put(id, ticks);
            }
        } else {
            GROUND_TICKS.remove(id);
        }
    }
}