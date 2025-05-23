package net.unfamily.iskautils;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.server.level.ServerLevel;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.ModBlockEntities;
import net.unfamily.iskautils.client.ClientEvents;
import net.unfamily.iskautils.data.VectorCharmData;
import net.unfamily.iskautils.item.ModCreativeModeTabs;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.item.custom.CuriosIntegration;
import net.unfamily.iskautils.network.ModMessages;
import net.unfamily.iskautils.util.ModUtils;

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
        
        // Se siamo sul client, registriamo anche il client setup
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::clientSetup);
        }

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        
        // Register blocks and items
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        
        // Registra l'integrazione con Curios se è installato
        CuriosIntegration.register(modEventBus);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);
        
        // Log Curios integration
        if (ModUtils.isCuriosLoaded()) {
            LOGGER.info("Curios mod detected - enabling integration");
        }
        
        // Registriamo lo shutdown hook per fermare i thread quando la mod viene scaricata
        if (FMLEnvironment.dist == Dist.CLIENT) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("Shutdown hook called, stopping client threads");
                ClientEvents.shutdown();
            }));
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Inizializzazione generale
        LOGGER.info("Common setup starting");
        
        // Registra i pacchetti di rete
        event.enqueueWork(ModMessages::register);
    }
    
    /**
     * Inizializzazione lato client
     */
    private void clientSetup(final FMLClientSetupEvent event) {
        // Garantiamo che questa operazione venga eseguita in modo sicuro nel thread del client
        event.enqueueWork(() -> {
            LOGGER.info("Client setup starting");
            
            // Inizializza gli eventi client per i keybindings
            ClientEvents.init();
            LOGGER.info("Client events initialized");
        });
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // No need to add anything here as we already defined the content of our tab
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("Server starting - initializing Vector Charm data");
    }
    
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        // Salva i dati persistenti quando il server si ferma
        LOGGER.info("Server stopping - saving Vector Charm data");
        if (event.getServer().overworld() != null) {
            LOGGER.info("Vector Charm data is handled in memory only for now");
        }
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Register CUTOUT rendering for the slow_vect block
            event.enqueueWork(() -> {
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.SLOW_VECT.get(), RenderType.cutout());
            });
        }
    }
}
