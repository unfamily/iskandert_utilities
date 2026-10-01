package net.unfamily.iskautils.potion;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.effect.ModMobEffects;

public final class ModPotions {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(Registries.POTION, IskaUtils.MOD_ID);

    /** Strength-like base duration (3:00); decay is always 2× empowerment. */
    private static final int EMPOWER_T = 3600;
    private static final int EMPOWER_T_LONG = 9600;
    private static final int EMPOWER_T_STRONG = 1800;

    public static final DeferredHolder<Potion, Potion> ENTROPIC = POTIONS.register("entropic",
            () -> new Potion(
                    "entropic",
                    new MobEffectInstance(ModMobEffects.ENTROPIC_EMPOWERMENT_PLAYER, EMPOWER_T, 0),
                    new MobEffectInstance(ModMobEffects.ENTROPIC_DECAY, EMPOWER_T * 2, 0)));

    public static final DeferredHolder<Potion, Potion> LONG_ENTROPIC = POTIONS.register("long_entropic",
            () -> new Potion(
                    "entropic",
                    new MobEffectInstance(ModMobEffects.ENTROPIC_EMPOWERMENT_PLAYER, EMPOWER_T_LONG, 0),
                    new MobEffectInstance(ModMobEffects.ENTROPIC_DECAY, EMPOWER_T_LONG * 2, 0)));

    public static final DeferredHolder<Potion, Potion> STRONG_ENTROPIC = POTIONS.register("strong_entropic",
            () -> new Potion(
                    "entropic",
                    new MobEffectInstance(ModMobEffects.ENTROPIC_EMPOWERMENT_PLAYER, EMPOWER_T_STRONG, 1),
                    new MobEffectInstance(ModMobEffects.ENTROPIC_DECAY, EMPOWER_T_STRONG * 2, 1)));

    private ModPotions() {}

    public static void register(IEventBus bus) {
        POTIONS.register(bus);
    }
}
