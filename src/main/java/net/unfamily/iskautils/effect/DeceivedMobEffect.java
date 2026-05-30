package net.unfamily.iskautils.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.unfamily.iskautils.IskaUtils;

/**
 * The Deception: half of vanilla Absorption at level I (1 golden heart = 2 HP).
 * Duration comes from the MobEffectInstance timer, not from remaining absorption.
 */
public class DeceivedMobEffect extends MobEffect {
    /** One golden heart at amplifier 0. */
    public static final float ABSORPTION_HP_PER_LEVEL = 2.0F;

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

    @Override
    public void onEffectStarted(LivingEntity mob, int amplifier) {
        super.onEffectStarted(mob, amplifier);
        float grant = ABSORPTION_HP_PER_LEVEL * (amplifier + 1);
        mob.setAbsorptionAmount(Math.max(mob.getAbsorptionAmount(), grant));
    }
}
