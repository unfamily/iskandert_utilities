package net.unfamily.iskautils;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
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
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.ModBlockEntities;
import net.unfamily.iskautils.client.ClientEvents;
import net.unfamily.iskautils.command.CommandItemLoader;
import net.unfamily.iskautils.data.VectorCharmData;
import net.unfamily.iskautils.item.CommandItemRegistry;
import net.unfamily.iskautils.item.ModCreativeModeTabs;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.item.custom.CuriosIntegration;
import net.unfamily.iskautils.network.ModMessages;
import net.unfamily.iskautils.util.ModUtils;
import net.unfamily.iskautils.data.PotionPlateLoader;
import net.unfamily.iskautils.data.PotionPlateRegistry;
import net.unfamily.iskautils.block.PotionPlateBlock;
import net.unfamily.iskautils.data.DynamicPotionPlateScanner;
import net.unfamily.iskautils.data.DynamicPotionPlateModelLoader;
import net.unfamily.iskautils.command.MacroLoader;
import net.unfamily.iskautils.command.MacroCommand;
import net.unfamily.iskautils.command.ExecuteMacroCommand;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

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
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Register network messages
        ModMessages.register();
        
        // Scan and load command macros
        MacroLoader.scanConfigDirectory();
        
        // Gli item di comando sono già stati inizializzati nel costruttore
        // Non chiamare più CommandItemRegistry.initializeItems() qui
        
        // Inizializza il sistema di stage degli item
        net.unfamily.iskautils.iska_utils_stages.StageItemManager.initialize(event);
    }
    
    /**
     * Client-side initialization
     */
    private void clientSetup(final FMLClientSetupEvent event) {
        // Initialize client events
        ClientEvents.init();
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
            });
        }
        
        @SubscribeEvent
        public static void registerGeometryLoaders(net.neoforged.neoforge.client.event.ModelEvent.RegisterGeometryLoaders event) {
            event.register(DynamicPotionPlateModelLoader.ID, DynamicPotionPlateModelLoader.INSTANCE);
        }
    }

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.GAME)
    public static class GameEventBusEvents {
        private static int cooldownCleanupTimer = 0;
        
        @SubscribeEvent
        public static void onServerStarting(ServerStartingEvent event) {
            VectorCharmData.getInstance();
            
            // Commands registration happens via RegisterCommandsEvent
        }

        @SubscribeEvent
        public static void onServerStopping(ServerStoppingEvent event) {
            // For now, data is only stored in memory
            
            // Shutdown the macro executor service
            MacroCommand.shutdown();
        }
        
        @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
        public static void onServerStarted(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
            // Server is fully started, all external datapacks should be loaded by now
            LOGGER.info("Server fully started, all external datapacks should be available");
            
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
                        // Reload command macros
                        MacroLoader.reloadAllMacros();
                        // Reload command item definitions
                        CommandItemRegistry.reloadDefinitions();
                        // Reload stage item restrictions
                        net.unfamily.iskautils.iska_utils_stages.StageItemManager.reloadItemRestrictions();
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
