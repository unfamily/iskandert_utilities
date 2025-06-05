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
                            
    // BlockEntity per il nuovo blocco di legno di gomma vuoto
    public static final Supplier<BlockEntityType<RubberLogEmptyBlockEntity>> RUBBER_LOG_EMPTY = 
            BLOCK_ENTITIES.register("rubber_log_empty", 
                    () -> BlockEntityType.Builder.of(RubberLogEmptyBlockEntity::new, 
                            ModBlocks.RUBBER_LOG_EMPTY.get()).build(null));

    // BlockEntity per il RubberSapExtractor
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RubberSapExtractorBlockEntity>> RUBBER_SAP_EXTRACTOR =
            BLOCK_ENTITIES.register("rubber_sap_extractor", () ->
                    BlockEntityType.Builder.of(RubberSapExtractorBlockEntity::new,
                            ModBlocks.RUBBER_SAP_EXTRACTOR.get()).build(null));

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
            
            // Register energy capability for RubberSapExtractor
            event.registerBlockEntity(
                    Capabilities.EnergyStorage.BLOCK,
                    RUBBER_SAP_EXTRACTOR.get(),
                    (blockEntity, context) -> {
                        if (blockEntity instanceof RubberSapExtractorBlockEntity extractorEntity) {
                            return extractorEntity.getEnergyStorage();
                        }
                        return null;
                    }
            );
        }
    }
} 