package net.unfamily.iskautils.util;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.effect.ModMobEffects;

/** Applies Entropic Empowerment with correct duration (infinite on mobs, short on players). */
public final class EntropicEmpowermentUtil {
    /** 30 seconds — players never keep infinite empowerment (e.g. creeper explosion splash). */
    public static final int PLAYER_DURATION_TICKS = 30 * 20;

    private EntropicEmpowermentUtil() {}

    public static MobEffectInstance createFor(LivingEntity entity) {
        int duration = entity instanceof Player
                ? PLAYER_DURATION_TICKS
                : MobEffectInstance.INFINITE_DURATION;
        return new MobEffectInstance(
                ModMobEffects.ENTROPIC_EMPOWERMENT,
                duration,
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
        entity.addEffect(createFor(entity));
    }

    /** Normalizes player instances that arrived with infinite or excessive duration (area clouds, mob death, etc.). */
    public static void normalizePlayerDuration(LivingEntity entity) {
        if (!(entity instanceof Player)) {
            return;
        }
        MobEffectInstance inst = entity.getEffect(ModMobEffects.ENTROPIC_EMPOWERMENT);
        if (inst == null) {
            return;
        }
        if (!inst.isInfiniteDuration() && inst.getDuration() <= PLAYER_DURATION_TICKS) {
            return;
        }
        entity.removeEffect(ModMobEffects.ENTROPIC_EMPOWERMENT);
        entity.addEffect(createFor(entity));
    }
}
