package net.unfamily.iskautils.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Marker effect; combat bonuses handled in {@link net.unfamily.iskautils.events.EntropicEmpowermentEffects}. */
public class EntropicEmpowermentMobEffect extends MobEffect {
    public EntropicEmpowermentMobEffect() {
        super(MobEffectCategory.NEUTRAL, 0x6B3FA0);
    }
}

