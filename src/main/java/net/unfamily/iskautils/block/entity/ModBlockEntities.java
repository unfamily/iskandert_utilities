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

/**
 * Registro delle entità blocco
 */
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

    // Registra il Weather Alterer Block Entity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WeatherAltererBlockEntity>> WEATHER_ALTERER_BE =
            BLOCK_ENTITIES.register("weather_alterer_block_entity", () ->
                    BlockEntityType.Builder.of(WeatherAltererBlockEntity::new, 
                            ModBlocks.WEATHER_ALTERER.get()).build(null));

    // Registra il Time Alterer Block Entity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TimeAltererBlockEntity>> TIME_ALTERER_BE =
            BLOCK_ENTITIES.register("time_alterer_block_entity", () ->
                    BlockEntityType.Builder.of(TimeAltererBlockEntity::new, 
                            ModBlocks.TIME_ALTERER.get()).build(null));

    // Angel Block Entity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AngelBlockEntity>> ANGEL_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("angel_block_entity",
                    () -> BlockEntityType.Builder.of(AngelBlockEntity::new, ModBlocks.ANGEL_BLOCK.get())
                            .build(null));

    // Structure Placer Machine Block Entity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StructurePlacerMachineBlockEntity>> STRUCTURE_PLACER_MACHINE_BE =
            BLOCK_ENTITIES.register("structure_placer_machine",
                    () -> BlockEntityType.Builder.of(StructurePlacerMachineBlockEntity::new, ModBlocks.STRUCTURE_PLACER_MACHINE.get())
                            .build(null));

    // Structure Saver Machine Block Entity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StructureSaverMachineBlockEntity>> STRUCTURE_SAVER_MACHINE_BE =
            BLOCK_ENTITIES.register("structure_saver_machine",
                    () -> BlockEntityType.Builder.of(StructureSaverMachineBlockEntity::new, ModBlocks.STRUCTURE_SAVER_MACHINE.get())
                            .build(null));

    // Shop Block Entity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShopBlockEntity>> SHOP_BE =
            BLOCK_ENTITIES.register("shop",
                    () -> BlockEntityType.Builder.of(ShopBlockEntity::new, ModBlocks.SHOP.get())
                            .build(null));

    // Auto Shop Block Entity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AutoShopBlockEntity>> AUTO_SHOP_BE =
            BLOCK_ENTITIES.register("auto_shop",
                    () -> BlockEntityType.Builder.of(AutoShopBlockEntity::new, ModBlocks.AUTO_SHOP.get())
                            .build(null));

    // Deep Drawers Block Entity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DeepDrawersBlockEntity>> DEEP_DRAWERS_BE =
            BLOCK_ENTITIES.register("deep_drawers",
                    () -> BlockEntityType.Builder.of(DeepDrawersBlockEntity::new, ModBlocks.DEEP_DRAWERS.get())
                            .build(null));

    /**
     * Registra tutte le entità blocco
     */
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
            
            // Register energy capability for WeatherAlterer
            event.registerBlockEntity(
                    Capabilities.EnergyStorage.BLOCK,
                    WEATHER_ALTERER_BE.get(),
                    (blockEntity, context) -> {
                        if (blockEntity instanceof WeatherAltererBlockEntity weatherAltererEntity) {
                            return weatherAltererEntity.getEnergyStorage();
                        }
                        return null;
                    }
            );
            
            // Register energy capability for TimeAlterer
            event.registerBlockEntity(
                    Capabilities.EnergyStorage.BLOCK,
                    TIME_ALTERER_BE.get(),
                    (blockEntity, context) -> {
                        if (blockEntity instanceof TimeAltererBlockEntity timeAltererEntity) {
                            return timeAltererEntity.getEnergyStorage();
                        }
                        return null;
                    }
            );
            
            // Register item handler capability for Structure Placer Machine
            event.registerBlockEntity(
                    Capabilities.ItemHandler.BLOCK,
                    STRUCTURE_PLACER_MACHINE_BE.get(),
                    (blockEntity, context) -> {
                        if (blockEntity instanceof StructurePlacerMachineBlockEntity machineEntity) {
                            return machineEntity.getItemHandler();
                        }
                        return null;
                    }
            );
            
            // Register energy capability for Structure Placer Machine
            event.registerBlockEntity(
                    Capabilities.EnergyStorage.BLOCK,
                    STRUCTURE_PLACER_MACHINE_BE.get(),
                    (blockEntity, context) -> {
                        if (blockEntity instanceof StructurePlacerMachineBlockEntity machineEntity) {
                            return machineEntity.getEnergyStorage();
                        }
                        return null;
                    }
            );
            
            // Register item handler capability for Structure Saver Machine
            event.registerBlockEntity(
                    Capabilities.ItemHandler.BLOCK,
                    STRUCTURE_SAVER_MACHINE_BE.get(),
                    (blockEntity, context) -> {
                        if (blockEntity instanceof StructureSaverMachineBlockEntity saverMachineEntity) {
                            return saverMachineEntity.getItemHandler();
                        }
                        return null;
                    }
            );
            
            // Register item handler capability for Auto Shop Block (encapsulated slot only)
            event.registerBlockEntity(
                    Capabilities.ItemHandler.BLOCK,
                    AUTO_SHOP_BE.get(),
                    (blockEntity, context) -> {
                        if (blockEntity instanceof AutoShopBlockEntity autoShopEntity) {
                            return autoShopEntity.getEncapsulatedSlot();
                        }
                        return null;
                    }
            );
            
            // Shop Block non registra capability IItemHandler per prevenire interazioni con hopper
            // Il negozio funziona solo tramite GUI diretta del giocatore
            
            // Register item handler capability for Deep Drawers
            event.registerBlockEntity(
                    Capabilities.ItemHandler.BLOCK,
                    DEEP_DRAWERS_BE.get(),
                    (blockEntity, context) -> {
                        if (blockEntity instanceof DeepDrawersBlockEntity deepDrawersEntity) {
                            return deepDrawersEntity.getItemHandler();
                        }
                        return null;
                    }
            );
        }
    }
} 