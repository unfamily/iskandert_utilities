package net.unfamily.iskautils.util;

import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Computes bonus damage from partially ignoring armor and/or toughness (vanilla {@link CombatRules}).
 */
public final class DamageArmorBypassUtil {
    private DamageArmorBypassUtil() {}

    /**
     * @param armorIgnoreFraction     0–1 fraction of armor rating ignored (1 = full ignore)
     * @param toughnessIgnoreFraction 0–1 fraction of toughness ignored
     * @return extra damage to add to the incoming amount
     */
    public static float computeExtraDamage(
            LivingEntity victim,
            float damage,
            DamageSource source,
            float armorIgnoreFraction,
            float toughnessIgnoreFraction) {
        if (damage <= 0.0F) {
            return 0.0F;
        }
        if (armorIgnoreFraction <= 0.0F && toughnessIgnoreFraction <= 0.0F) {
            return 0.0F;
        }

        float armor = (float) victim.getAttributeValue(Attributes.ARMOR);
        float toughness = (float) victim.getAttributeValue(Attributes.ARMOR_TOUGHNESS);

        float mitigated = CombatRules.getDamageAfterAbsorb(victim, damage, source, armor, toughness);
        float reducedArmor = armor * (1.0F - Mth.clamp(armorIgnoreFraction, 0.0F, 1.0F));
        float reducedToughness = toughness * (1.0F - Mth.clamp(toughnessIgnoreFraction, 0.0F, 1.0F));
        float bypassed = CombatRules.getDamageAfterAbsorb(victim, damage, source, reducedArmor, reducedToughness);

        return Math.max(0.0F, bypassed - mitigated);
    }
}
