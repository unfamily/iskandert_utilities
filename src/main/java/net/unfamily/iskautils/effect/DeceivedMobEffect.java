package net.unfamily.iskautils.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.unfamily.iskautils.IskaUtils;

/**
 * The Deception: half of vanilla Absorption at level I (1 golden heart = 2 HP).
 * Stacks with vanilla Absorption by summing both grants on the shared absorption pool.
 */
public class DeceivedMobEffect extends MobEffect {
    /** One golden heart at amplifier 0. */
    public static final float ABSORPTION_HP_PER_LEVEL = 2.0F;
    /** Matches {@link net.minecraft.world.effect.AbsorptionMobEffect#onEffectStarted}. */
    public static final float VANILLA_ABSORPTION_HP_PER_LEVEL = 4.0F;

    private static final ResourceLocation MAX_ABSORPTION_ID =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "deceived");

    public DeceivedMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFC89B3C);
        this.addAttributeModifier(
                Attributes.MAX_ABSORPTION,
                MAX_ABSORPTION_ID,
                ABSORPTION_HP_PER_LEVEL,
                AttributeModifier.Operation.ADD_VALUE);
    }

    public static float vanillaAbsorptionGrant(LivingEntity mob) {
        MobEffectInstance absorption = mob.getEffect(MobEffects.ABSORPTION);
        if (absorption == null) {
            return 0.0F;
        }
        return VANILLA_ABSORPTION_HP_PER_LEVEL * (absorption.getAmplifier() + 1);
    }

    public static float deceivedAbsorptionGrant(int amplifier) {
        return ABSORPTION_HP_PER_LEVEL * (amplifier + 1);
    }

    /**
     * Raise absorption to at least vanilla Absorption + Deceived without lowering existing hearts
     * (e.g. after damage). Does not refill on every tick.
     */
    public static void applyDeceivedAbsorptionStack(LivingEntity mob, int deceivedAmplifier) {
        float target = vanillaAbsorptionGrant(mob) + deceivedAbsorptionGrant(deceivedAmplifier);
        if (mob.getAbsorptionAmount() < target) {
            mob.setAbsorptionAmount(target);
        }
    }

    @Override
    public void onEffectStarted(LivingEntity mob, int amplifier) {
        super.onEffectStarted(mob, amplifier);
        applyDeceivedAbsorptionStack(mob, amplifier);
    }
}
