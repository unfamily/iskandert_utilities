package net.unfamily.iskautils.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Harmful partner of player entropic empowerment; drives debuff/curse re-roll on expire. */
public class EntropicDecayMobEffect extends MobEffect {
    public EntropicDecayMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x9424A4);
    }
}
