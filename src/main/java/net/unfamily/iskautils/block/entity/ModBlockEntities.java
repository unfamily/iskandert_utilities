package net.unfamily.iskautils.block.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.ModBlocks;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IskaUtils.MOD_ID);

    // Register the block entity for Hellfire Igniter
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HellfireIgniterBlockEntity>> HELLFIRE_IGNITER_BE =
            BLOCK_ENTITIES.register("hellfire_igniter", () ->
                    BlockEntityType.Builder.of(HellfireIgniterBlockEntity::new,
                            ModBlocks.HELLFIRE_IGNITER.get()).build(null));

    public static final Supplier<BlockEntityType<RubberLogBlockEntity>> RUBBER_LOG = 
            BLOCK_ENTITIES.register("rubber_log", 
                    () -> BlockEntityType.Builder.of(RubberLogBlockEntity::new, 
                            ModBlocks.RUBBER_LOG.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }

    @EventBusSubscriber(modid = IskaUtils.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModBlockEntityEvents {
        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            // Register energy capability for Hellfire Igniter
            event.registerBlockEntity(
                    Capabilities.EnergyStorage.BLOCK,
                    HELLFIRE_IGNITER_BE.get(),
                    (blockEntity, context) -> {
                        if (blockEntity instanceof HellfireIgniterBlockEntity hellfireEntity) {
                            return hellfireEntity.getEnergyStorage();
                        }
                        return null;
                    }
            );
        }
    }
} 