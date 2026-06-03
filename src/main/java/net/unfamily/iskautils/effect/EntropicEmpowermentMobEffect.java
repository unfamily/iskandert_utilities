package net.unfamily.iskautils.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.unfamily.iskautils.util.EntropicEmpowermentUtil;

/** Marker effect; combat bonuses handled in {@link net.unfamily.iskautils.events.EntropicEmpowermentEffects}. */
public class EntropicEmpowermentMobEffect extends MobEffect {
    public EntropicEmpowermentMobEffect() {
        super(MobEffectCategory.NEUTRAL, 0x6B3FA0);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        EntropicEmpowermentUtil.normalizePlayerDuration(entity);
    }
}
