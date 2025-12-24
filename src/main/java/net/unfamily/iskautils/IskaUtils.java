package net.unfamily.iskautils;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.fml.ModList;
import guideme.Guide;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.ModBlockEntities;
import net.unfamily.iskautils.client.ClientEvents;
import net.unfamily.iskautils.command.CommandItemLoader;
import net.unfamily.iskautils.data.VectorCharmData;
import net.unfamily.iskautils.events.LootEvents;
import net.unfamily.iskautils.item.CommandItemRegistry;
import net.unfamily.iskautils.item.ModCreativeModeTabs;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.item.custom.CuriosIntegration;
import net.unfamily.iskautils.network.ModMessages;
import net.unfamily.iskautils.shop.ShopTeamManager;
import net.unfamily.iskautils.data.BurningBrazierData;
import net.unfamily.iskautils.util.ModUtils;
import net.unfamily.iskautils.util.ModWoodTypes;
import net.unfamily.iskautils.data.PotionPlateRegistry;
import net.unfamily.iskautils.block.PotionPlateBlock;
import net.unfamily.iskautils.data.DynamicPotionPlateScanner;
import net.unfamily.iskautils.data.DynamicPotionPlateModelLoader;
import net.unfamily.iskautils.command.MacroLoader;
import net.unfamily.iskautils.command.MacroCommand;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.iskautils.structure.StructureLoader;
import net.unfamily.iskautils.shop.ShopLoader;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(IskaUtils.MOD_ID)
public class IskaUtils {
    public static final String MOD_ID = "iska_utils";
    private static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public IskaUtils(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        
        // If we are on the client, also register client setup
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::clientSetup);

            // Register GUI MenuTypes (client-only)
            net.unfamily.iskautils.client.gui.ModMenuTypes.MENUS.register(modEventBus);
        }

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        
        
        // ===== DYNAMIC POTION PLATE SYSTEM =====
        // IMPORTANT: This must happen BEFORE registering the DeferredRegisters
        // to ensure blocks are available when registries are built
        
        // Scan for external configurations in config directory (before registry freeze)
        DynamicPotionPlateScanner.scanConfigDirectory();
        
        // Register discovered dynamic potion plates from external configurations
        PotionPlateRegistry.registerDiscoveredBlocks();
        
        // ===== COMMAND ITEM SYSTEM =====
        // Carica e inizializza gli item di comando prima della registrazione
        // per evitare errori "Cannot register new entries to DeferredRegister after RegisterEvent has been fired"
        CommandItemLoader.scanConfigDirectory();
        CommandItemRegistry.initializeItems();
        
        // ===== STRUCTURE MONOUSE ITEM SYSTEM =====
        // Carica e inizializza gli item Structure Monouse dinamici
        net.unfamily.iskautils.structure.StructureMonouseRegistry.initializeItems();
        
        // ===== STRUCTURE SYSTEM =====
        // Carica solo le definizioni delle strutture server/globali all'avvio
        // Le strutture client saranno caricate quando il giocatore è disponibile
        StructureLoader.scanConfigDirectoryServerOnly();
        
        // ===== SHOP SYSTEM =====
        // Carica e inizializza il sistema shop custom
        ShopLoader.scanConfigDirectory();
        
        // Register blocks and items
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        
        // Register dynamic potion plate blocks and items
        PotionPlateRegistry.POTION_PLATES.register(modEventBus);
        PotionPlateRegistry.POTION_PLATE_ITEMS.register(modEventBus);
        
        // Register Command Items system
        CommandItemRegistry.register(modEventBus);
        
        // Register Structure Monouse Items system
        net.unfamily.iskautils.structure.StructureMonouseRegistry.register(modEventBus);

        // Register ModMessages as event subscriber for payload registration
        modEventBus.register(ModMessages.class);
        
        // Register Curios integration if it's installed
        if (ModUtils.isCuriosLoaded()) {
            CuriosIntegration.register(modEventBus);
        }

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);
        
        // Add shutdown hook to clean up client threads
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ClientEvents.shutdown();
        }));
        
        // Initialize GuideME guide if available
        if (ModList.get().isLoaded("guideme")) {
            try {
                Guide.builder(ResourceLocation.fromNamespaceAndPath(MOD_ID, "guide")).build();
                LOGGER.info("GuideME guide registered");
            } catch (Exception e) {
                LOGGER.warn("Failed to register GuideME guide: {}", e.getMessage());
            }
        }

    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("IskaUtils common setup");
        
        // Register network messages
        ModMessages.register();
        
        // Register wood types
        ModWoodTypes.register();
        
        // Scan and load command macros
        MacroLoader.scanConfigDirectory();
        
        // Gli item di comando sono già stati inizializzati nel costruttore
        // Non chiamare più CommandItemRegistry.initializeItems() qui
        
        // Inizializza il sistema di stage degli item
        net.unfamily.iskautils.iska_utils_stages.StageItemManager.initialize(event);

        // Registra eventi per chunk unload che rimuovono i marker dello scanner
        NeoForge.EVENT_BUS.addListener(this::onChunkUnload);
        
        // Registro l'handler degli eventi dei loot table
        LOGGER.info("Registrando l'handler degli eventi dei loot table...");
        NeoForge.EVENT_BUS.register(LootEvents.class);
    }
    
    /**
     * Client-side initialization
     */
    private void clientSetup(final FMLClientSetupEvent event) {
        // Initialize client events
        ClientEvents.init();
        
        // Register custom GUI screens - will be done in ClientModEvents
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // No need to add anything here as we already defined the content of our tab
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModEventBusEvents {
        @SubscribeEvent
        public static void commonSetup(FMLCommonSetupEvent event) {
            // Register network messages
            ModMessages.register();
        }
    }

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Register CUTOUT rendering for the slow_vect block
            event.enqueueWork(() -> {
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.SLOW_VECT.get(), RenderType.cutout());
                // Aggiungi RenderType cutout per il rubber_sapling
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.RUBBER_SAPLING.get(), RenderType.cutout());
                // Aggiungi RenderType cutout_mipped per le netherite_bars
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.NETHERITE_BARS.get(), RenderType.cutoutMipped());
                
                // Register item property functions for dolly filled state
                ItemProperties.register(
                    ModItems.DOLLY.get(),
                    ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "filled"),
                    (stack, level, entity, seed) -> {
                        net.minecraft.world.item.component.CustomData customData = stack.getOrDefault(
                            net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                            net.minecraft.world.item.component.CustomData.EMPTY);
                        net.minecraft.nbt.CompoundTag nbt = customData.copyTag();
                        return nbt.getBoolean("HasBlock") ? 1.0F : 0.0F;
                    }
                );
                
                ItemProperties.register(
                    ModItems.DOLLY_HARD.get(),
                    ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "filled"),
                    (stack, level, entity, seed) -> {
                        net.minecraft.world.item.component.CustomData customData = stack.getOrDefault(
                            net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                            net.minecraft.world.item.component.CustomData.EMPTY);
                        net.minecraft.nbt.CompoundTag nbt = customData.copyTag();
                        return nbt.getBoolean("HasBlock") ? 1.0F : 0.0F;
                    }
                );
                
                ItemProperties.register(
                    ModItems.DOLLY_CREATIVE.get(),
                    ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "filled"),
                    (stack, level, entity, seed) -> {
                        net.minecraft.world.item.component.CustomData customData = stack.getOrDefault(
                            net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                            net.minecraft.world.item.component.CustomData.EMPTY);
                        net.minecraft.nbt.CompoundTag nbt = customData.copyTag();
                        return nbt.getBoolean("HasBlock") ? 1.0F : 0.0F;
                    }
                );
            });
        }
        
        @SubscribeEvent
        public static void registerGeometryLoaders(net.neoforged.neoforge.client.event.ModelEvent.RegisterGeometryLoaders event) {
            event.register(DynamicPotionPlateModelLoader.ID, DynamicPotionPlateModelLoader.INSTANCE);
        }
        
        
        @SubscribeEvent
        public static void registerMenuScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
            event.register(net.unfamily.iskautils.client.gui.ModMenuTypes.STRUCTURE_PLACER_MENU.get(),
                          net.unfamily.iskautils.client.gui.StructurePlacerScreen::new);
            event.register(net.unfamily.iskautils.client.gui.ModMenuTypes.STRUCTURE_PLACER_MACHINE_MENU.get(),
                          net.unfamily.iskautils.client.gui.StructurePlacerMachineScreen::new);
            event.register(net.unfamily.iskautils.client.gui.ModMenuTypes.STRUCTURE_SELECTION_MENU.get(),
                          net.unfamily.iskautils.client.gui.StructureSelectionScreen::new);
            event.register(net.unfamily.iskautils.client.gui.ModMenuTypes.STRUCTURE_SAVER_MACHINE_MENU.get(),
                          net.unfamily.iskautils.client.gui.StructureSaverMachineScreen::new);
            event.register(net.unfamily.iskautils.client.gui.ModMenuTypes.SHOP_MENU.get(),
                          net.unfamily.iskautils.client.gui.ShopScreen::new);
            event.register(net.unfamily.iskautils.client.gui.ModMenuTypes.AUTO_SHOP_MENU.get(),
                          net.unfamily.iskautils.client.gui.AutoShopScreen::new);
            event.register(net.unfamily.iskautils.client.gui.ModMenuTypes.DEEP_DRAWERS_MENU.get(),
                          net.unfamily.iskautils.client.gui.DeepDrawersScreen::new);
            event.register(net.unfamily.iskautils.client.gui.ModMenuTypes.SMART_TIMER_MENU.get(),
                          net.unfamily.iskautils.client.gui.SmartTimerScreen::new);
            event.register(net.unfamily.iskautils.client.gui.ModMenuTypes.DEEP_DRAWER_EXTRACTOR_MENU.get(),
                net.unfamily.iskautils.client.gui.DeepDrawerExtractorScreen::new);
            event.register(net.unfamily.iskautils.client.gui.ModMenuTypes.TEMPORAL_OVERCLOCKER_MENU.get(),
                net.unfamily.iskautils.client.gui.TemporalOverclockerScreen::new);
        }
    }

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.GAME)
    public static class GameEventBusEvents {
        
        @SubscribeEvent
        public static void onContainerClose(net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Close event) {
            if (event.getContainer() instanceof net.unfamily.iskautils.client.gui.DeepDrawersMenu menu) {
                if (menu.getBlockEntity() != null) {
                    menu.getBlockEntity().onGuiClosed();
                }
            }
        }
        private static int cooldownCleanupTimer = 0;
        
        @SubscribeEvent
        public static void onServerStarting(ServerStartingEvent event) {
            VectorCharmData.getInstance();

            // Initialize Burning Brazier data (will be loaded automatically when needed)
            // BurningBrazierData.get(event.getServer().overworld());

            // Commands registration happens via RegisterCommandsEvent
        }
        
        @SubscribeEvent
        public static void onPlayerLoggedIn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                // Ricarica le strutture includendo le client structures ora che il giocatore è disponibile
                try {
                    LOGGER.info("Player {} connected, reloading structures with client support...", serverPlayer.getName().getString());
                    
                    // Ricarica le strutture con il contesto del giocatore per il nickname corretto
                    StructureLoader.reloadAllDefinitions(true, serverPlayer);
                    
                    // Aggiungi un piccolo delay per assicurarsi che il client sia pronto per la sincronizzazione
                    serverPlayer.getServer().execute(() -> {
                        try {
                            net.unfamily.iskautils.network.ModMessages.sendStructureSyncPacket(serverPlayer);
                        } catch (Exception e) {
                            LOGGER.error("Error synchronizing structures to player {}: {}", 
                                       serverPlayer.getName().getString(), e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    LOGGER.error("Error in player login event: {}", e.getMessage());
                }
            }
        }

        @SubscribeEvent
        public static void onServerStopping(ServerStoppingEvent event) {
            // For now, data is only stored in memory
            
            // Shutdown the macro executor service
            MacroCommand.shutdown();
            
            // Ritorna alle strutture locali quando il server si ferma
            try {
                StructureLoader.resetToLocalStructures();
            } catch (Exception e) {
                LOGGER.error("Error resetting to local structures: {}", e.getMessage());
            }
        }
        
        @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
        public static void onServerStarted(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
            // Server is fully started, all external datapacks should be loaded by now
            LOGGER.info("Server fully started, all external datapacks should be available");
            
            // Initialize a new session ID for markers
            net.unfamily.iskautils.util.SessionVariables.resetScannerSessionId();
            LOGGER.info("Initialized new scanner session ID: {}", net.unfamily.iskautils.util.SessionVariables.getScannerSessionId());
            
            // Clean up any scanner markers that might be leftover from previous session
            try {
                if (event.getServer().overworld() != null) {
                    // La funzionalità cleanupAllMarkers è stata rimossa
                    // net.unfamily.iskautils.item.custom.ScannerItem.cleanupAllMarkers(event.getServer().overworld());
                    LOGGER.info("Cleaned up all scanner markers");
                }
            } catch (Exception e) {
                LOGGER.error("Error cleaning up scanner markers: {}", e.getMessage());
            }
            
            // Reload macros to ensure all commands are registered
            try {
                MacroLoader.reloadAllMacros();
            } catch (Exception e) {
                LOGGER.error("Error loading macros at server startup: {}", e.getMessage());
            }
            
            // Reload command item definitions
            try {
                CommandItemRegistry.reloadDefinitions();
            } catch (Exception e) {
                LOGGER.error("Error loading command item definitions at server startup: {}", e.getMessage());
            }
            
            // Sincronizza le strutture con tutti i client connessi
            try {
                for (net.minecraft.server.level.ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                    net.unfamily.iskautils.network.ModMessages.sendStructureSyncPacket(player);
                }
            } catch (Exception e) {
                LOGGER.error("Error synchronizing structures to clients: {}", e.getMessage());
            }
        }
        
        @SubscribeEvent
        public static void onAddReloadListener(net.neoforged.neoforge.event.AddReloadListenerEvent event) {
            LOGGER.info("Registering reload listeners for macros and command items...");
            
            // Add a ReloadListener that reloads macros and command item definitions when /reload command is executed
            event.addListener(new PreparableReloadListener() {
                @Override
                public CompletableFuture<Void> reload(
                        PreparableReloadListener.PreparationBarrier preparationBarrier,
                        ResourceManager resourceManager,
                        ProfilerFiller preparationsProfiler,
                        ProfilerFiller reloadProfiler,
                        Executor backgroundExecutor,
                        Executor gameExecutor) {
                    
                    return CompletableFuture.runAsync(() -> {
                        LOGGER.info("Server reload detected, reloading macros and command item definitions...");
                        
                        // Clean up any scanner markers
                        try {
                            MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                            if (server != null && server.overworld() != null) {
                                // La funzionalità cleanupAllMarkers è stata rimossa
                                // net.unfamily.iskautils.item.custom.ScannerItem.cleanupAllMarkers(server.overworld());
                                LOGGER.info("Cleaned up all scanner markers during reload");
                            }
                        } catch (Exception e) {
                            LOGGER.error("Error cleaning up scanner markers during reload: {}", e.getMessage());
                        }
                        
                        // Reload command macros
                        MacroLoader.reloadAllMacros();
                        // Reload command item definitions
                        CommandItemRegistry.reloadDefinitions();
                        // Reload stage item restrictions
                        net.unfamily.iskautils.iska_utils_stages.StageItemManager.reloadItemRestrictions();
                        // Reload shop system
                        ShopLoader.reloadAllConfigurations();
                    }, gameExecutor).thenCompose(preparationBarrier::wait);
                }
                
                @Override
                public String getName() {
                    return "IskaUtils Commands and Items";
                }
            });
        }
        
        @SubscribeEvent
        public static void onServerTick(ServerTickEvent.Post event) {
            // Clean up potion plate cooldowns every 5 minutes (6000 ticks)
            cooldownCleanupTimer++;
            if (cooldownCleanupTimer >= 6000) {
                cooldownCleanupTimer = 0;
                if (event.getServer().overworld() != null) {
                    PotionPlateBlock.cleanupCooldowns(event.getServer().overworld());
                }
                // Clean up expired team invitations
                ShopTeamManager.getInstance(event.getServer().overworld()).getTeamDataInstance().cleanupExpiredInvitations();
                // Note: Burning Brazier data is saved automatically when modified
            }
        }
    }
    
    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class ClientGameEvents {
        @SubscribeEvent
        public static void onClientPlayerLoggedIn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
            // Solo per il client locale (singleplayer) o quando il client si connette a un server
            if (event.getEntity() instanceof net.minecraft.client.player.LocalPlayer) {
                try {
                    LOGGER.info("Local player joined world, reloading client structures...");
                    
                    // Ricarica le strutture includendo le client structures ora che il giocatore è disponibile
                    StructureLoader.reloadAllDefinitions(true);
                    
                } catch (Exception e) {
                    LOGGER.error("Error reloading client structures on player join: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Gestisce l'evento di unload di un chunk per rimuovere i marker dello scanner
     */
    private void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk levelChunk && !levelChunk.getLevel().isClientSide()) {
            net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) levelChunk.getLevel();
            net.minecraft.world.level.ChunkPos chunkPos = levelChunk.getPos();
            
            // La funzionalità removeMarkersInChunk è stata rimossa
            // ScannerItem.removeMarkersInChunk(level, chunkPos);
        }
    }

    /**
     * Creates a text component from a string
     */
    public static Component component(String text) {
        return Component.literal(text);
    }
}
