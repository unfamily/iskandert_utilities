package net.unfamily.iskautils;

import net.unfamily.iskautils.util.ModLogger;



import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.unfamily.iskautils.data.load.IskaUtilsLoadReloadScheduler;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.resource.VanillaServerListeners;
import net.neoforged.neoforge.common.NeoForge;
import net.unfamily.iskautils.guide.IskaUtilsGuide;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.ModBlockEntities;
import net.unfamily.iskautils.client.ClientEvents;
import net.unfamily.iskautils.fluid.ModFluids;
import net.unfamily.iskautils.command.CommandItemLoader;
import net.unfamily.iskautils.data.VectorCharmData;
import net.unfamily.iskautils.events.LootEvents;
import net.unfamily.iskautils.item.CommandItemRegistry;
import net.unfamily.iskautils.item.ModCreativeModeTabs;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.item.custom.CuriosIntegration;
import net.unfamily.iskautils.effect.ModMobEffects;
import net.unfamily.iskautils.network.ModMessages;
import net.unfamily.iskalib.client.marker.VanillaWorldMarkerClientHooks;
import net.unfamily.iskalib.team.ShopTeamManager;
import net.unfamily.iskalib.stage.StageHooks;
import net.unfamily.iskautils.util.ModUtils;
import net.unfamily.iskautils.util.ModWoodTypes;
import net.unfamily.iskautils.data.PotionPlateRegistry;
import net.unfamily.iskautils.block.PotionPlateBlock;
import net.unfamily.iskautils.data.DynamicPotionPlateScanner;
import net.unfamily.iskautils.command.MacroLoader;
import net.unfamily.iskautils.command.MacroCommand;
import net.unfamily.iskautils.crafting.FactorySourcesReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.Identifier;
import net.unfamily.iskalib.structure.StructureIOHooks;
import net.unfamily.iskalib.structure.StructureLoader;
import net.unfamily.iskautils.shop.ShopLoader;
import net.unfamily.iskautils.structure.StructureMonouseLoader;
import net.unfamily.iskautils.data.load.IskaUtilsLoadJson;
import net.unfamily.iskautils.data.load.IskaUtilsLoadPaths;
import net.unfamily.iskautils.data.load.IskaUtilsDataReload;
import net.unfamily.iskautils.data.load.IskaUtilsLoadReloadListener;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(IskaUtils.MOD_ID)
public class IskaUtils {
    public static final String MOD_ID = "iska_utils";
    private static final ModLogger LOGGER = ModLogger.of(IskaUtils.class);

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public IskaUtils(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register capability providers (MOD bus)
        modEventBus.addListener(net.unfamily.iskautils.item.ModItemCapabilities::registerCapabilities);
        modEventBus.addListener(net.unfamily.iskautils.block.entity.ModBlockEntities.ModBlockEntityEvents::registerCapabilities);
        
        // Client-only setup/registrations (keep server-safe via reflection gate)
        if (isClientEnvironment()) {
            modEventBus.addListener(this::clientSetup);
            VanillaWorldMarkerClientHooks.registerIfNeeded(NeoForge.EVENT_BUS);

            // Register GUI MenuTypes (client-only)
            net.unfamily.iskautils.client.gui.ModMenuTypes.MENUS.register(modEventBus);

            // Register key mappings (client-only, MOD bus)
            modEventBus.register(net.unfamily.iskautils.client.KeyBindings.class);

            modEventBus.addListener(net.unfamily.iskautils.client.IskaUtilsClientModEvents::registerMenuScreens);
            modEventBus.addListener(net.unfamily.iskautils.client.IskaUtilsClientModEvents::registerEntityRenderers);
            modEventBus.addListener(net.unfamily.iskautils.client.IskaUtilsClientModEvents::registerParticleProviders);
            modEventBus.addListener(net.neoforged.neoforge.client.event.RegisterFluidModelsEvent.class,
                    net.unfamily.iskautils.client.fluid.ModFluidClient::registerFluidModels);

        }

        // Register game events on the NeoForge bus
        NeoForge.EVENT_BUS.register(GameEventBusEvents.class);
        NeoForge.EVENT_BUS.register(net.unfamily.iskautils.command.CommandEvents.class);
        NeoForge.EVENT_BUS.register(net.unfamily.iskautils.events.FlightHandler.class);
        NeoForge.EVENT_BUS.register(net.unfamily.iskautils.events.StructurePlacerEvents.class);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        
        
        // ===== DYNAMIC POTION PLATE SYSTEM =====
        // IMPORTANT: This must happen BEFORE registering the DeferredRegisters
        // to ensure blocks are available when registries are built
        LOGGER.info("=== STARTING DYNAMIC POTION PLATE BOOTSTRAP ===");
        DynamicPotionPlateScanner.loadAll(null);
        LOGGER.info("=== COMPLETED DYNAMIC POTION PLATE BOOTSTRAP ===");
        
        // Register discovered dynamic potion plates from external configurations
        PotionPlateRegistry.registerDiscoveredBlocks();
        
        // ===== COMMAND ITEM SYSTEM =====
        // Carica e inizializza gli item di comando prima della registrazione
        // per evitare errori "Cannot register new entries to DeferredRegister after RegisterEvent has been fired"
        LOGGER.info("=== STARTING COMMAND ITEMS BOOTSTRAP ===");
        CommandItemLoader.loadAll(null);
        CommandItemRegistry.initializeItems();
        LOGGER.info("=== COMPLETED COMMAND ITEMS BOOTSTRAP ===");
        
        LOGGER.info("=== STARTING STRUCTURE MONOUSE BOOTSTRAP ===");
        StructureMonouseLoader.loadAll(null);
        net.unfamily.iskautils.structure.StructureMonouseRegistry.initializeItems();
        LOGGER.info("=== COMPLETED STRUCTURE MONOUSE BOOTSTRAP ===");
        
        // ===== STRUCTURE SYSTEM =====
        // Carica solo le definizioni delle strutture server/globali all'avvio
        // Le strutture client saranno caricate quando il giocatore è disponibile
        StructureLoader.scanConfigDirectoryServerOnly();
        
        // ===== SHOP SYSTEM =====
        // Carica e inizializza il sistema shop custom
        ShopLoader.loadAll(null);
        
        net.unfamily.iskautils.worldgen.ModBiomeModifierSerializers.register(modEventBus);

        // Register blocks and items
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModFluids.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        net.unfamily.iskautils.crafting.ModFactoryRecipes.register(modEventBus);
        ModMobEffects.register(modEventBus);
        net.unfamily.iskautils.particle.ModParticles.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        net.unfamily.iskautils.entity.ModEntities.register(modEventBus);
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
        
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("IskaUtils common setup");

        net.unfamily.iskautils.arcane.ArcaneDictionaryEffectsInit.registerBuiltins();
        net.unfamily.iskautils.arcane.ArcaneDictionaryLoader.loadAllBootstrap();
        
        // Register network messages
        ModMessages.register();

        // Wire stage side-effects (actions) into the library stage system
        StageHooks.setListener(new StageHooks.Listener() {
            @Override
            public void onPlayerStageChanged(ServerPlayer player, String stage, boolean value) {
                net.unfamily.iskautils.command.StageActionsManager.onPlayerStageChanged(player, stage, value);
            }

            @Override
            public void onWorldStageChanged(MinecraftServer server, String stage, boolean value) {
                net.unfamily.iskautils.command.StageActionsManager.onWorldStageChanged(server, stage, value);
            }

            @Override
            public void onTeamStageChanged(MinecraftServer server, String teamName, String stage, boolean value) {
                net.unfamily.iskautils.command.StageActionsManager.onTeamStageChanged(server, teamName, stage, value);
            }
        });

        // Wire stage "action" command callbacks into mod implementation
        net.unfamily.iskalib.stage.StageActionHooks.setListener(new net.unfamily.iskalib.stage.StageActionHooks.Listener() {
            @Override
            public java.util.List<String> listActionIds() {
                return net.unfamily.iskautils.command.StageActionsManager.listActionIds();
            }

            @Override
            public int executeActionById(String actionId, java.util.List<ServerPlayer> players, boolean force) {
                return net.unfamily.iskautils.command.StageActionsManager.executeActionById(actionId, players, force);
            }
        });

        StructureIOHooks.setListener(new StructureIOHooks.Listener() {
            @Override
            public void loadServerStructureDefinitions(ResourceManager resourceManagerOrNull, StructureIOHooks.StructureDefinitionSink sink) {
                Map<Identifier, JsonElement> merged = resourceManagerOrNull != null
                        ? IskaUtilsLoadJson.collectMergedJsonForSubdir(resourceManagerOrNull, IskaUtilsLoadPaths.STRUCTURE_DEFINITIONS)
                        : IskaUtilsLoadJson.collectFromModJarOnly(IskaUtilsLoadPaths.STRUCTURE_DEFINITIONS);
                for (var e : IskaUtilsLoadJson.orderedEntries(merged)) {
                    if (!e.getValue().isJsonObject()) {
                        continue;
                    }
                    String defId = IskaUtilsLoadJson.definitionIdFromLocation(e.getKey());
                    sink.accept(defId, e.getKey().toString(), e.getValue().getAsJsonObject());
                }
            }

            @Override
            public boolean acceptClientStructures() {
                return Config.acceptClientStructure;
            }

            @Override
            public String clientStructurePath() {
                return Config.clientStructurePath;
            }

            @Override
            public boolean allowClientStructurePlaceLikePlayer() {
                return Config.allowClientStructurePlayerLike;
            }
        });
        
        // Register wood types
        ModWoodTypes.register();
        
        // Scan and load command macros
        MacroLoader.scanConfigDirectory();

        // Scan and load stage actions (run when stages are added/removed)
        net.unfamily.iskautils.command.StageActionsLoader.scanConfigDirectory();

        // Gli item di comando sono già stati inizializzati nel costruttore
        // Non chiamare più CommandItemRegistry.initializeItems() qui
        
        // Inizializza il sistema di stage degli item
        net.unfamily.iskautils.iska_utils_stages.StageItemManager.initialize(event);
        
        // Registro l'handler degli eventi dei loot table
        LOGGER.info("Registrando l'handler degli eventi dei loot table...");
        NeoForge.EVENT_BUS.register(LootEvents.class);
    }

    private static boolean isClientEnvironment() {
        return FMLEnvironment.getDist() == Dist.CLIENT;
    }
    
    /**
     * Client-side initialization
     */
    private void clientSetup(final FMLClientSetupEvent event) {
        // Initialize client events
        ClientEvents.init();

        Runtime.getRuntime().addShutdownHook(new Thread(ClientEvents::shutdown));

        IskaUtilsGuide.registerClient();

        // Register custom GUI screens - will be done in ClientModEvents
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // No need to add anything here as we already defined the content of our tab
    }

    public static class GameEventBusEvents {
        
        @SubscribeEvent
        public static void onContainerOpen(net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Open event) {
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                    && event.getContainer() instanceof net.unfamily.iskautils.client.gui.DeepDrawersMenu menu) {
                menu.sendAllSlotsToClient(serverPlayer);
            }
        }

        @SubscribeEvent
        public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
            if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
                return;
            }
            if (!(serverPlayer.containerMenu instanceof net.unfamily.iskautils.client.gui.DeepDrawersMenu menu)) {
                return;
            }
            // Periodic sync: keep GUI refreshed even if other code paths mutate storage.
            if (serverPlayer.tickCount % 20 == 0) {
                menu.sendAllSlotsToClient(serverPlayer);
            }
        }

        @SubscribeEvent
        public static void onContainerClose(net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Close event) {
            if (event.getContainer() instanceof net.unfamily.iskautils.client.gui.DeepDrawersMenu menu) {
                if (menu.getBlockEntity() != null) {
                    menu.getBlockEntity().onGuiClosed();
                }
            }
            if (event.getContainer() instanceof net.unfamily.iskautils.client.gui.DeepDrawerExtractorMenu menu) {
                if (menu.getBlockEntity() != null) {
                    menu.getBlockEntity().onGuiClosed();
                }
            }
            if (event.getContainer() instanceof net.unfamily.iskautils.client.gui.FanMenu menu) {
                if (menu.getBlockEntity() != null) {
                    menu.getBlockEntity().resetBackMessage();
                }
            }
        }
        private static int cooldownCleanupTimer = 0;

        @SubscribeEvent
        public static void onAddServerReloadListeners(AddServerReloadListenersEvent event) {
            Identifier loadReload =
                    Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "load_data_reload");
            event.addListener(loadReload, new IskaUtilsLoadReloadListener());
            event.addDependency(VanillaServerListeners.RECIPES, loadReload);
            Identifier factoryReload =
                    Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "factory_sources_reload");
            event.addListener(factoryReload, new FactorySourcesReloadListener());
            event.addDependency(VanillaServerListeners.RECIPES, factoryReload);
        }

        @SubscribeEvent
        public static void onServerStarting(ServerStartingEvent event) {
            VectorCharmData.getInstance();

            // Commands registration happens via RegisterCommandsEvent
        }
        
        @SubscribeEvent
        public static void onPlayerLoggedIn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                try {
                    boolean flameVision = net.unfamily.iskautils.data.FlameVisionData.getFlameVisionEnabledForPlayer(serverPlayer);
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                            serverPlayer, new net.unfamily.iskautils.network.packet.FlameVisionSyncS2CPacket(flameVision));
                } catch (Exception e) {
                    LOGGER.warn("Failed to sync flame vision for {}: {}", serverPlayer.getName().getString(), e.getMessage());
                }
                // Ricarica le strutture includendo le client structures ora che il giocatore è disponibile
                try {
                    LOGGER.info("Player {} connected, reloading structures with client support...", serverPlayer.getName().getString());
                    
                    // Ricarica le strutture con il contesto del giocatore per il nickname corretto
                    StructureLoader.reloadAllDefinitions(true, serverPlayer);
                    
                    // Aggiungi un piccolo delay per assicurarsi che il client sia pronto per la sincronizzazione
                    ((net.minecraft.server.level.ServerLevel) serverPlayer.level()).getServer().execute(() -> {
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
            
            try {
                IskaUtilsDataReload.reloadAllFromServer();
            } catch (Exception e) {
                LOGGER.error("Error applying IskaUtils datapack load JSON at server startup: {}", e.getMessage());
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
        
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onServerTick(ServerTickEvent.Post event) {
            for (net.minecraft.server.level.ServerLevel level : event.getServer().getAllLevels()) {
                net.unfamily.iskautils.util.BlazingAltarExtinguishJobs.tick(level);
            }
            IskaUtilsLoadReloadScheduler.runPending(event.getServer());
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

    /**
     * Creates a text component from a string
     */
    public static Component component(String text) {
        return Component.literal(text);
    }
}
