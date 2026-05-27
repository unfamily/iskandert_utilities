package net.unfamily.iskautils.effect;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.unfamily.iskautils.IskaUtils;

public final class ModMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, IskaUtils.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> CURSE_OF_PAIN =
            MOB_EFFECTS.register("curse_of_pain", CurseOfPainMobEffect::new);

    private ModMobEffects() {}

    public static void register(IEventBus bus) {
        MOB_EFFECTS.register(bus);
    }
}

