package net.unfamily.iskautils.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.iskautils.IskaUtils;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, IskaUtils.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<DeceptionSeatEntity>> DECEPTION_SEAT =
            ENTITY_TYPES.register("deception_seat", () ->
                    EntityType.Builder.<DeceptionSeatEntity>of(DeceptionSeatEntity::new, MobCategory.MISC)
                            .sized(0.0F, 0.0F)
                            .clientTrackingRange(256)
                            .updateInterval(Integer.MAX_VALUE)
                            .build("deception_seat"));

    private ModEntities() {}

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
