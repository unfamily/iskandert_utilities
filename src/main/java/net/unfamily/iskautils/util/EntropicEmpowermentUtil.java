package net.unfamily.iskautils.util;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.effect.ModMobEffects;

/** Applies Entropic Empowerment (infinite on mobs; stripped from players on effect tick). */
public final class EntropicEmpowermentUtil {
    private EntropicEmpowermentUtil() {}

    public static MobEffectInstance createInstance() {
        return new MobEffectInstance(
                ModMobEffects.ENTROPIC_EMPOWERMENT,
                MobEffectInstance.INFINITE_DURATION,
                0,
                true,
                true,
                true);
    }

    public static void apply(LivingEntity entity) {
        apply(entity, false);
    }

    /**
     * @param requireEggConfig when true, skips if {@link Config#entropicEggApplyBuff} is false (entropic egg use).
     */
    public static void apply(LivingEntity entity, boolean requireEggConfig) {
        if (requireEggConfig && !Config.entropicEggApplyBuff) {
            return;
        }
        entity.addEffect(createInstance());
    }
}
