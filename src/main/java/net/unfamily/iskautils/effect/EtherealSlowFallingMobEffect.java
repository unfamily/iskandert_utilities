package net.unfamily.iskautils.effect;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.unfamily.iskautils.particle.ModParticles;

/**
 * Slow-falling like vanilla, with custom sparse sculk_soul particles (anti-face via custom particle).
 * Ground-clear after 3s is handled in {@link net.unfamily.iskautils.events.EtherealSlowFallingEffects}.
 */
public class EtherealSlowFallingMobEffect extends MobEffect {
    private static final ResourceLocation GRAVITY_ID =
            ResourceLocation.fromNamespaceAndPath("iska_utils", "effect.ethereal_slow_falling");

    public EtherealSlowFallingMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x7FE4FF);
        this.addAttributeModifier(
                Attributes.GRAVITY,
                GRAVITY_ID,
                -0.07D,
                AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public ParticleOptions createParticleOptions(MobEffectInstance instance) {
        return ModParticles.ETHEREAL_SCULK_SOUL.get();
    }
}