package net.unfamily.iskautils.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Neutral effect applied by Totem of Pain.
 * The actual damage amplification is handled in event code.
 */
public class CurseOfPainMobEffect extends MobEffect {
    public CurseOfPainMobEffect() {
        super(MobEffectCategory.NEUTRAL, 0x7D2AE8);
    }
}

