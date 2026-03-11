package net.unfamily.iskautils.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

public class ModFoodProperties {
    public static final FoodProperties LAPIS_ICE_CREAM = new FoodProperties.Builder()
            .nutrition(10)
            .saturationModifier(2.0f)
            .build();

    /** Edible even when full; gives no hunger/saturation; applies Weakness II for 5 seconds. */
    public static final FoodProperties DYE_BERRY = new FoodProperties.Builder()
            .nutrition(0)
            .saturationModifier(0.0f)
            .alwaysEdible()
            .effect(() -> new MobEffectInstance(MobEffects.WEAKNESS, 100, 2), 1.0f)
            .build();
}
