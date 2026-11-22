package net.unfamily.iskautils.particle;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.iskautils.IskaUtils;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, IskaUtils.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ENTROPIC_FLAME =
            PARTICLE_TYPES.register("entropic_flame", () -> new SimpleParticleType(false));

    /** Sparse ethereal effect particle; client provider clamps status-effect fling velocity. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ETHEREAL_SCULK_SOUL =
            PARTICLE_TYPES.register("ethereal_sculk_soul", () -> new SimpleParticleType(false));

    private ModParticles() {}

    public static void register(IEventBus bus) {
        PARTICLE_TYPES.register(bus);
    }
}
