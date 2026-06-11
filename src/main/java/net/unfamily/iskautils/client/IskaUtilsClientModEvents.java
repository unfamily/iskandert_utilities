package net.unfamily.iskautils.client;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.gui.*;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.unfamily.iskautils.client.particle.EntropicFlameParticle;
import net.unfamily.iskautils.particle.ModParticles;
import net.unfamily.iskautils.block.entity.ModBlockEntities;
import net.unfamily.iskautils.client.renderer.DeceptionSeatRenderer;
import net.unfamily.iskautils.client.renderer.EntropicSpawnerRenderer;
import net.unfamily.iskautils.entity.ModEntities;

/** Registered on the MOD bus from {@link IskaUtils} (client-only). */
public final class IskaUtilsClientModEvents {
    private IskaUtilsClientModEvents() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // No-op for now.
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.ENTROPIC_FLAME.get(), EntropicFlameParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.DECEPTION_SEAT.get(), DeceptionSeatRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ENTROPIC_SPAWNER_BE.get(), EntropicSpawnerRenderer::new);
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.STRUCTURE_PLACER_MENU.get(), StructurePlacerScreen::new);
        event.register(ModMenuTypes.STRUCTURE_PLACER_MACHINE_MENU.get(), StructurePlacerMachineScreen::new);
        event.register(ModMenuTypes.STRUCTURE_SELECTION_MENU.get(), StructureSelectionScreen::new);
        event.register(ModMenuTypes.STRUCTURE_SAVER_MACHINE_MENU.get(), StructureSaverMachineScreen::new);
        event.register(ModMenuTypes.SHOP_MENU.get(), ShopScreen::new);
        event.register(ModMenuTypes.AUTO_SHOP_MENU.get(), AutoShopScreen::new);
        event.register(ModMenuTypes.DEEP_DRAWERS_MENU.get(), DeepDrawersScreen::new);
        event.register(ModMenuTypes.SMART_TIMER_MENU.get(), SmartTimerScreen::new);
        event.register(ModMenuTypes.DEEP_DRAWER_EXTRACTOR_MENU.get(), DeepDrawerExtractorScreen::new);
        event.register(ModMenuTypes.TEMPORAL_OVERCLOCKER_MENU.get(), TemporalOverclockerScreen::new);
        event.register(ModMenuTypes.FAN_MENU.get(), FanScreen::new);
        event.register(ModMenuTypes.MOB_REAPER_MENU.get(), MobReaperScreen::new);
        event.register(ModMenuTypes.SOUND_MUFFLER_MENU.get(), SoundMufflerScreen::new);
        event.register(ModMenuTypes.FACTORY_MENU.get(), FactoryScreen::new);
        event.register(ModMenuTypes.ANCIENT_TABLE_MENU.get(), AncientTableScreen::new);
        event.register(ModMenuTypes.COLLECTING_CRATE_MENU.get(), CollectingCrateScreen::new);
        event.register(ModMenuTypes.BLAZING_ALTAR_MENU.get(), BlazingAltarScreen::new);
        event.register(ModMenuTypes.ENTROPIC_SPAWNER_MENU.get(), EntropicSpawnerScreen::new);
    }
}
