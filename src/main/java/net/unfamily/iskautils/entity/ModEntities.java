package net.unfamily.iskautils.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.iskautils.IskaUtils;

@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, IskaUtils.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<DeceptionSeatEntity>> DECEPTION_SEAT =
            ENTITY_TYPES.register("deception_seat", () ->
                    EntityType.Builder.<DeceptionSeatEntity>of(DeceptionSeatEntity::new, MobCategory.MISC)
                            .sized(0.0F, 0.0F)
                            .clientTrackingRange(256)
                            .updateInterval(Integer.MAX_VALUE)
                            .build(ResourceKey.create(
                                    Registries.ENTITY_TYPE,
                                    Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "deception_seat"))));

    public static final DeferredHolder<EntityType<?>, EntityType<EntropicCreeper>> ENTROPIC_CREEPER =
            ENTITY_TYPES.register("entropic_creeper", () ->
                    EntityType.Builder.<EntropicCreeper>of(EntropicCreeper::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.7F)
                            .clientTrackingRange(8)
                            .build(ResourceKey.create(
                                    Registries.ENTITY_TYPE,
                                    Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "entropic_creeper"))));

    public static final DeferredHolder<EntityType<?>, EntityType<PrimedEntropyTnt>> PRIMED_ENTROPY_TNT =
            ENTITY_TYPES.register("primed_entropy_tnt", () ->
                    EntityType.Builder.<PrimedEntropyTnt>of(PrimedEntropyTnt::new, MobCategory.MISC)
                            .fireImmune()
                            .sized(0.98F, 0.98F)
                            .eyeHeight(0.15F)
                            .clientTrackingRange(10)
                            .updateInterval(10)
                            .build(ResourceKey.create(
                                    Registries.ENTITY_TYPE,
                                    Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "primed_entropy_tnt"))));

    private ModEntities() {}

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }

    @SubscribeEvent
    public static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ENTROPIC_CREEPER.get(), EntropicCreeper.createAttributes().build());
    }
}
