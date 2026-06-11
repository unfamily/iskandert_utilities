package net.unfamily.iskautils.client;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.ModBlocks;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.unfamily.iskautils.client.particle.EntropicFlameParticle;
import net.unfamily.iskautils.particle.ModParticles;
import net.unfamily.iskautils.client.entropic.EntropicAnimatedArmorTextures;
import net.unfamily.iskautils.client.fluid.ModFluidClient;
import net.unfamily.iskautils.client.gui.*;
import net.unfamily.iskautils.block.entity.ModBlockEntities;
import net.unfamily.iskautils.client.renderer.DeceptionSeatRenderer;
import net.unfamily.iskautils.client.renderer.EntropicSpawnerRenderer;
import net.unfamily.iskautils.data.DynamicPotionPlateModelLoader;
import net.unfamily.iskautils.entity.ModEntities;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.item.custom.artifact.ChosenCheeseItem;
import net.unfamily.iskautils.item.custom.artifact.TheRootsItem;

@EventBusSubscriber(modid = IskaUtils.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class IskaUtilsClientModEvents {
    private IskaUtilsClientModEvents() {}

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
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SLOW_VECT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.RUBBER_SAPLING.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SACRED_RUBBER_SAPLING.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DYE_BUSH_EMPTY.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DYE_BUSH_FILLED.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.NETHERITE_BARS.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.REDSTONE_ACTIVATOR_SIGNAL.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ENTROPIC_SPAWNER.get(), RenderType.cutout());

            EntropicAnimatedArmorTextures.register();

            ItemProperties.register(
                    ModItems.DOLLY.get(),
                    ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "filled"),
                    (stack, level, entity, seed) -> {
                        CustomData customData = stack.getOrDefault(
                                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                                CustomData.EMPTY);
                        CompoundTag nbt = customData.copyTag();
                        return nbt.getBoolean("HasBlock") ? 1.0F : 0.0F;
                    }
            );

            ItemProperties.register(
                    ModItems.DOLLY_HARD.get(),
                    ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "filled"),
                    (stack, level, entity, seed) -> {
                        CustomData customData = stack.getOrDefault(
                                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                                CustomData.EMPTY);
                        CompoundTag nbt = customData.copyTag();
                        return nbt.getBoolean("HasBlock") ? 1.0F : 0.0F;
                    }
            );

            ItemProperties.register(
                    ModItems.DOLLY_CREATIVE.get(),
                    ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "filled"),
                    (stack, level, entity, seed) -> {
                        CustomData customData = stack.getOrDefault(
                                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                                CustomData.EMPTY);
                        CompoundTag nbt = customData.copyTag();
                        return nbt.getBoolean("HasBlock") ? 1.0F : 0.0F;
                    }
            );

            ItemProperties.register(
                    ModItems.CHOSEN_CHEESE.get(),
                    ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "chosen_cheese_full"),
                    (stack, level, entity, seed) -> {
                        int y = ChosenCheeseItem.getLevel(stack);
                        int x = Config.chosenCheeseMax;
                        return (y >= x) ? 1.0F : 0.0F;
                    }
            );

            ItemProperties.register(
                    ModItems.THE_ROOTS.get(),
                    ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "unix_root"),
                    (stack, level, entity, seed) -> TheRootsItem.isUnixLike() ? 1.0F : 0.0F
            );
        });
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(
                (stack, tintIndex) -> {
                    if (tintIndex == 0) {
                        return 0xFFFFFFFF;
                    }
                    return 0xFF000000 | net.unfamily.iskautils.item.component.UnstableEntropyCatalystDecay.calcTintRgb(stack);
                },
                ModItems.UNSTABLE_ENTROPY_CATALYST.get()
        );
    }

    @SubscribeEvent
    public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(DynamicPotionPlateModelLoader.ID, DynamicPotionPlateModelLoader.INSTANCE);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        ModFluidClient.registerClientExtensions(event);
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
