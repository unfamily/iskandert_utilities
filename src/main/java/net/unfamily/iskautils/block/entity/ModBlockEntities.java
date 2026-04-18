package net.unfamily.iskautils.block.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.ModBlocks;

/**
 * Registro delle entità blocco
 */
public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IskaUtils.MOD_ID);
            
    // Register the block entity for Hellfire Igniter
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HellfireIgniterBlockEntity>> HELLFIRE_IGNITER_BE =
            BLOCK_ENTITIES.register("hellfire_igniter", () ->
                    new BlockEntityType<>(HellfireIgniterBlockEntity::new, ModBlocks.HELLFIRE_IGNITER.get()));
                            
    // BlockEntity per il nuovo blocco di legno di gomma vuoto
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RubberLogEmptyBlockEntity>> RUBBER_LOG_EMPTY =
            BLOCK_ENTITIES.register("rubber_log_empty", 
                    () -> new BlockEntityType<>(RubberLogEmptyBlockEntity::new, ModBlocks.RUBBER_LOG_EMPTY.get()));

    // BlockEntity for empty dye bush (refill timer)
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DyeBushEmptyBlockEntity>> DYE_BUSH_EMPTY =
            BLOCK_ENTITIES.register("dye_bush_empty", 
                    () -> new BlockEntityType<>(DyeBushEmptyBlockEntity::new, ModBlocks.DYE_BUSH_EMPTY.get()));

    // Passive BlockEntity for filled blocks (no logic); keeps block position "active" for tick accelerators
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PassiveFilledBlockEntity>> PASSIVE_FILLED =
            BLOCK_ENTITIES.register("passive_filled", 
                    () -> new BlockEntityType<>(
                        PassiveFilledBlockEntity::new,
                        ModBlocks.RUBBER_LOG_FILLED.get(),
                        ModBlocks.DYE_BUSH_FILLED.get()
                    ));

    // BlockEntity per il RubberSapExtractor
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RubberSapExtractorBlockEntity>> RUBBER_SAP_EXTRACTOR =
            BLOCK_ENTITIES.register("rubber_sap_extractor", () ->
                    new BlockEntityType<>(RubberSapExtractorBlockEntity::new, ModBlocks.RUBBER_SAP_EXTRACTOR.get()));

    // Registra il Weather Alterer Block Entity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WeatherAltererBlockEntity>> WEATHER_ALTERER_BE =
            BLOCK_ENTITIES.register("weather_alterer_block_entity", () ->
                    new BlockEntityType<>(WeatherAltererBlockEntity::new, ModBlocks.WEATHER_ALTERER.get()));

    // Registra il Time Alterer Block Entity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TimeAltererBlockEntity>> TIME_ALTERER_BE =
            BLOCK_ENTITIES.register("time_alterer_block_entity", () ->
                    new BlockEntityType<>(TimeAltererBlockEntity::new, ModBlocks.TIME_ALTERER.get()));
    
    // Registra il Temporal Overclocker Block Entity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TemporalOverclockerBlockEntity>> TEMPORAL_OVERCLOCKER_BE =
            BLOCK_ENTITIES.register("temporal_overclocker_block_entity", () ->
                    new BlockEntityType<>(TemporalOverclockerBlockEntity::new, ModBlocks.TEMPORAL_OVERCLOCKER.get()));

    // Angel Block Entity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AngelBlockEntity>> ANGEL_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("angel_block_entity",
                    () -> new BlockEntityType<>(AngelBlockEntity::new, ModBlocks.ANGEL_BLOCK.get()));

    // Structure Placer Machine Block Entity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StructurePlacerMachineBlockEntity>> STRUCTURE_PLACER_MACHINE_BE =
            BLOCK_ENTITIES.register("structure_placer_machine",
                    () -> new BlockEntityType<>(StructurePlacerMachineBlockEntity::new, ModBlocks.STRUCTURE_PLACER_MACHINE.get()));

    // Structure Saver Machine Block Entity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StructureSaverMachineBlockEntity>> STRUCTURE_SAVER_MACHINE_BE =
            BLOCK_ENTITIES.register("structure_saver_machine",
                    () -> new BlockEntityType<>(StructureSaverMachineBlockEntity::new, ModBlocks.STRUCTURE_SAVER_MACHINE.get()));

    // Shop Block Entity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShopBlockEntity>> SHOP_BE =
            BLOCK_ENTITIES.register("shop",
                    () -> new BlockEntityType<>(ShopBlockEntity::new, ModBlocks.SHOP.get()));

    // Auto Shop Block Entity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AutoShopBlockEntity>> AUTO_SHOP_BE =
            BLOCK_ENTITIES.register("auto_shop",
                    () -> new BlockEntityType<>(AutoShopBlockEntity::new, ModBlocks.AUTO_SHOP.get()));

    // Deep Drawers Block Entity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DeepDrawersBlockEntity>> DEEP_DRAWERS_BE =
            BLOCK_ENTITIES.register("deep_drawer",
                    () -> new BlockEntityType<>(DeepDrawersBlockEntity::new, ModBlocks.DEEP_DRAWERS.get()));

    // Deep Drawer Extractor Block Entity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DeepDrawerExtractorBlockEntity>> DEEP_DRAWER_EXTRACTOR =
            BLOCK_ENTITIES.register("deep_drawer_extractor",
                    () -> new BlockEntityType<>(DeepDrawerExtractorBlockEntity::new, ModBlocks.DEEP_DRAWER_EXTRACTOR.get()));
    
    // Deep Drawer Interface Block Entity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DeepDrawerInterfaceBlockEntity>> DEEP_DRAWER_INTERFACE =
            BLOCK_ENTITIES.register("deep_drawer_interface",
                    () -> new BlockEntityType<>(DeepDrawerInterfaceBlockEntity::new, ModBlocks.DEEP_DRAWER_INTERFACE.get()));
    
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DeepDrawerExtenderBlockEntity>> DEEP_DRAWER_EXTENDER =
            BLOCK_ENTITIES.register("deep_drawer_extender",
                    () -> new BlockEntityType<>(DeepDrawerExtenderBlockEntity::new, ModBlocks.DEEP_DRAWER_EXTENDER.get()));
    
    // Smart Timer Block Entity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmartTimerBlockEntity>> SMART_TIMER_BE =
            BLOCK_ENTITIES.register("smart_timer",
                    () -> new BlockEntityType<>(SmartTimerBlockEntity::new, ModBlocks.SMART_TIMER.get()));

    // Sound Muffler Block Entity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SoundMufflerBlockEntity>> SOUND_MUFFLER_BE =
            BLOCK_ENTITIES.register("sound_muffler",
                    () -> new BlockEntityType<>(SoundMufflerBlockEntity::new, ModBlocks.SOUND_MUFFLER.get()));

    // Fan Block Entity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FanBlockEntity>> FAN_BE =
            BLOCK_ENTITIES.register("fan",
                    () -> new BlockEntityType<>(FanBlockEntity::new, ModBlocks.FAN.get()));
    
    // Sacred Rubber Sapling Block Entity
    // BlockEntity for RubberLogSacredBlock (stores root coordinates)
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RubberLogSacredBlockEntity>> RUBBER_LOG_SACRED_BE =
            BLOCK_ENTITIES.register("rubber_log_sacred", () ->
                    new BlockEntityType<>(RubberLogSacredBlockEntity::new, ModBlocks.RUBBER_LOG_SACRED.get()));
    
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SacredRubberSaplingBlockEntity>> SACRED_RUBBER_SAPLING_BE =
            BLOCK_ENTITIES.register("sacred_rubber_sapling",
                    () -> new BlockEntityType<>(SacredRubberSaplingBlockEntity::new, ModBlocks.SACRED_RUBBER_SAPLING.get()));

    /**
     * Registra tutte le entità blocco
     */
    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }

    public static class ModBlockEntityEvents {
        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            // Register energy capability for Hellfire Igniter
            event.registerBlockEntity(
                    Capabilities.Energy.BLOCK,
                    HELLFIRE_IGNITER_BE.get(),
                    (blockEntity, context) -> {
                        if (blockEntity instanceof HellfireIgniterBlockEntity hellfireEntity) {
                            return hellfireEntity.getEnergyHandler();
                        }
                        return null;
                    }
            );
            
            // Register energy capability for RubberSapExtractor
            event.registerBlockEntity(
                    Capabilities.Energy.BLOCK,
                    RUBBER_SAP_EXTRACTOR.get(),
                    (blockEntity, context) -> {
                        if (blockEntity instanceof RubberSapExtractorBlockEntity extractorEntity) {
                            return extractorEntity.getEnergyHandler();
                        }
                        return null;
                    }
            );
            
            // Register energy capability for WeatherAlterer
            event.registerBlockEntity(
                    Capabilities.Energy.BLOCK,
                    WEATHER_ALTERER_BE.get(),
                    (blockEntity, context) -> {
                        if (blockEntity instanceof WeatherAltererBlockEntity weatherAltererEntity) {
                            return weatherAltererEntity.getEnergyHandler();
                        }
                        return null;
                    }
            );
            
            // Register energy capability for TimeAlterer
            event.registerBlockEntity(
                    Capabilities.Energy.BLOCK,
                    TIME_ALTERER_BE.get(),
                    (blockEntity, context) -> {
                        if (blockEntity instanceof TimeAltererBlockEntity timeAltererEntity) {
                            return timeAltererEntity.getEnergyHandler();
                        }
                        return null;
                    }
            );
            
            // Register energy capability for TemporalOverclocker
            event.registerBlockEntity(
                    Capabilities.Energy.BLOCK,
                    TEMPORAL_OVERCLOCKER_BE.get(),
                    (blockEntity, context) -> {
                        if (blockEntity instanceof TemporalOverclockerBlockEntity overclockerEntity) {
                            return overclockerEntity.getEnergyHandler();
                        }
                        return null;
                    }
            );
            // Register energy capability for Structure Placer Machine
            event.registerBlockEntity(
                    Capabilities.Energy.BLOCK,
                    STRUCTURE_PLACER_MACHINE_BE.get(),
                    (blockEntity, context) -> {
                        if (blockEntity instanceof StructurePlacerMachineBlockEntity machineEntity) {
                            return machineEntity.getEnergyHandler();
                        }
                        return null;
                    }
            );

            // Item transfer (NeoForge 26 ResourceHandler; wraps legacy IItemHandler for hoppers / pipes)
            event.registerBlockEntity(
                    Capabilities.Item.BLOCK,
                    DEEP_DRAWERS_BE.get(),
                    (blockEntity, ctx) -> blockEntity instanceof DeepDrawersBlockEntity be ? be.getItemTransferHandler() : null
            );
            event.registerBlockEntity(
                    Capabilities.Item.BLOCK,
                    DEEP_DRAWER_EXTRACTOR.get(),
                    (blockEntity, ctx) -> blockEntity instanceof DeepDrawerExtractorBlockEntity be ? be.getItemTransferHandler() : null
            );
            event.registerBlockEntity(
                    Capabilities.Item.BLOCK,
                    DEEP_DRAWER_INTERFACE.get(),
                    (blockEntity, ctx) -> blockEntity instanceof DeepDrawerInterfaceBlockEntity be ? be.getItemTransferHandler() : null
            );
            event.registerBlockEntity(
                    Capabilities.Item.BLOCK,
                    DEEP_DRAWER_EXTENDER.get(),
                    (blockEntity, ctx) -> blockEntity instanceof DeepDrawerExtenderBlockEntity be ? be.getItemTransferHandler() : null
            );
            event.registerBlockEntity(
                    Capabilities.Item.BLOCK,
                    SHOP_BE.get(),
                    (blockEntity, ctx) -> blockEntity instanceof ShopBlockEntity be ? be.getItemTransferHandler() : null
            );
            event.registerBlockEntity(
                    Capabilities.Item.BLOCK,
                    AUTO_SHOP_BE.get(),
                    (blockEntity, ctx) -> blockEntity instanceof AutoShopBlockEntity be ? be.getItemTransferHandler() : null
            );
            event.registerBlockEntity(
                    Capabilities.Item.BLOCK,
                    STRUCTURE_PLACER_MACHINE_BE.get(),
                    (blockEntity, ctx) -> blockEntity instanceof StructurePlacerMachineBlockEntity be ? be.getItemTransferHandler() : null
            );
            event.registerBlockEntity(
                    Capabilities.Item.BLOCK,
                    STRUCTURE_SAVER_MACHINE_BE.get(),
                    (blockEntity, ctx) -> blockEntity instanceof StructureSaverMachineBlockEntity be ? be.getItemTransferHandler() : null
            );
            event.registerBlockEntity(
                    Capabilities.Item.BLOCK,
                    FAN_BE.get(),
                    (blockEntity, ctx) -> blockEntity instanceof FanBlockEntity be ? be.getItemTransferHandler() : null
            );
        }
    }
} 