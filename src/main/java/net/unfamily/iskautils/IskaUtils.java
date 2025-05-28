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
import net.unfamily.iskautils.data.VectorCharmData;
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
        
        // Register blocks and items
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        
        // Register dynamic potion plate blocks and items
        PotionPlateRegistry.POTION_PLATES.register(modEventBus);
        PotionPlateRegistry.POTION_PLATE_ITEMS.register(modEventBus);
        
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
        
        // Scansiona e carica le macro di comandi
        MacroLoader.scanConfigDirectory();
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
            
            // La registrazione dei comandi avviene ora tramite RegisterCommandsEvent
            // quindi non è più necessario farlo qui
            // ExecuteMacroCommand.register(event.getServer().getCommands().getDispatcher());
        }

        @SubscribeEvent
        public static void onServerStopping(ServerStoppingEvent event) {
            // For now, data is only stored in memory
            
            // Arresta l'executor service delle macro
            MacroCommand.shutdown();
        }
        
        @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
        public static void onServerStarted(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
            // Server is fully started, all external datapacks should be loaded by now
            LOGGER.info("Server fully started, all external datapacks should be available");
            
            // Ricarica le macro per assicurarsi che tutti i comandi siano registrati
            // Questo garantisce che i comandi siano disponibili anche se non viene eseguito /reload
            try {
                MacroLoader.reloadAllMacros();
            } catch (Exception e) {
                LOGGER.error("Errore nel caricamento delle macro all'avvio del server: {}", e.getMessage());
            }
        }
        
        @SubscribeEvent
        public static void onAddReloadListener(net.neoforged.neoforge.event.AddReloadListenerEvent event) {
            LOGGER.info("Registrazione del listener per il ricaricamento delle macro...");
            
            // Aggiungi un ReloadListener che ricarica le macro quando viene chiamato il comando /reload
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
                        LOGGER.info("Server reload rilevato, ricarico le macro dei comandi...");
                        // Ricarica le macro dei comandi
                        MacroLoader.reloadAllMacros();
                    }, gameExecutor).thenCompose(preparationBarrier::wait);
                }
                
                @Override
                public String getName() {
                    return "IskaUtils Macro Commands";
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
