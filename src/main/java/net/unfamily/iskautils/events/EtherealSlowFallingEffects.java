package net.unfamily.iskautils.events;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
 * Fall-damage immunity, descent-only slow-fall (vanilla gravity feel), ground-clear.
 * Gravity modifier is applied only while descending so jumps stay normal.
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class EtherealSlowFallingEffects {
    private static final int GROUND_CLEAR_TICKS = 20;
    private static final int AIR_GRACE_TICKS = 10;
    /** Same as vanilla Slow Falling gravity bonus. */
    private static final double SLOW_FALL_GRAVITY = -0.07D;
    private static final ResourceLocation GRAVITY_MOD_ID =
            ResourceLocation.fromNamespaceAndPath("iska_utils", "ethereal_slow_falling_descent");

    private static final Map<UUID, Integer> GROUND_TICKS = new HashMap<>();
    private static final Map<UUID, Integer> AIR_TICKS = new HashMap<>();

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
        UUID id = living.getUUID();
        if (!living.hasEffect(ModMobEffects.ETHEREAL_SLOW_FALLING)) {
            clearDescentGravity(living);
            GROUND_TICKS.remove(id);
            AIR_TICKS.remove(id);
            return;
        }

        // Apply vanilla-like gravity only while going down; remove while jumping / ascending.
        boolean descending = living.getDeltaMovement().y < -0.01D
                && !living.onGround()
                && !living.verticalCollisionBelow;
        if (descending) {
            applyDescentGravity(living);
        } else {
            clearDescentGravity(living);
        }

        if (living.onGround() || living.verticalCollisionBelow) {
            AIR_TICKS.remove(id);
            clearDescentGravity(living);
            int ticks = GROUND_TICKS.getOrDefault(id, 0) + 1;
            if (ticks >= GROUND_CLEAR_TICKS) {
                living.removeEffect(ModMobEffects.ETHEREAL_SLOW_FALLING);
                GROUND_TICKS.remove(id);
            } else {
                GROUND_TICKS.put(id, ticks);
            }
            return;
        }

        int air = AIR_TICKS.getOrDefault(id, 0) + 1;
        if (air >= AIR_GRACE_TICKS) {
            GROUND_TICKS.remove(id);
            AIR_TICKS.remove(id);
        } else {
            AIR_TICKS.put(id, air);
        }
    }

    private static void applyDescentGravity(LivingEntity living) {
        AttributeInstance gravity = living.getAttribute(Attributes.GRAVITY);
        if (gravity == null) {
            return;
        }
        if (!gravity.hasModifier(GRAVITY_MOD_ID)) {
            gravity.addOrUpdateTransientModifier(new AttributeModifier(
                    GRAVITY_MOD_ID,
                    SLOW_FALL_GRAVITY,
                    AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void clearDescentGravity(LivingEntity living) {
        AttributeInstance gravity = living.getAttribute(Attributes.GRAVITY);
        if (gravity != null) {
            gravity.removeModifier(GRAVITY_MOD_ID);
        }
    }
}
