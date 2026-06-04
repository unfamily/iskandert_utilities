package net.unfamily.iskautils.data.load;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.unfamily.iskautils.command.MacroLoader;
import net.unfamily.iskautils.command.StageActionsLoader;
import net.unfamily.iskautils.data.DynamicPotionPlateScanner;
import net.unfamily.iskautils.iska_utils_stages.StageItemHandler;
import net.unfamily.iskautils.item.CommandItemRegistry;
import net.unfamily.iskautils.network.ModMessages;
import net.unfamily.iskautils.shop.ShopLoader;
import net.unfamily.iskalib.structure.StructureLoader;
import net.unfamily.iskautils.structure.StructureMonouseLoader;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRecipeLoader;
import net.unfamily.iskautils.arcane.ArcaneDictionaryLoader;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryLoader;
import org.slf4j.Logger;

/**
 * Spreads IskaUtils {@code load/**} work across server ticks so world join is not blocked
 * by one long synchronous reload (Factory recipe scan, structure file walk, etc.).
 */
public final class IskaUtilsPhasedReloadScheduler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile boolean running;
    private static int phase;
    private static Runnable onComplete = () -> {};

    private IskaUtilsPhasedReloadScheduler() {}

    public static void schedule(MinecraftServer server) {
        schedule(server, () -> {});
    }

    public static void schedule(MinecraftServer server, Runnable complete) {
        if (server == null) {
            return;
        }
        if (running) {
            LOGGER.debug("IskaUtils phased reload already running; coalescing completion callback");
            Runnable previous = onComplete;
            onComplete = () -> {
                previous.run();
                complete.run();
            };
            return;
        }
        running = true;
        phase = 0;
        onComplete = complete != null ? complete : () -> {};
        LOGGER.info("IskaUtils phased load/** reload scheduled ({} phases)", PHASE_COUNT);
    }

    public static void tick(MinecraftServer server) {
        if (!running || server == null) {
            return;
        }
        if (phase >= PHASE_COUNT) {
            finish();
            return;
        }

        long start = System.nanoTime();
        String name = phaseName(phase);
        try {
            runPhase(server, phase);
        } catch (Exception e) {
            LOGGER.error("IskaUtils phased reload failed at phase '{}': {}", name, e.getMessage(), e);
            running = false;
            phase = 0;
            onComplete = () -> {};
            return;
        }
        LOGGER.info("IskaUtils phased reload: '{}' done in {} ms", name, elapsedMs(start));
        phase++;
        if (phase >= PHASE_COUNT) {
            finish();
        }
    }

    private static void finish() {
        running = false;
        phase = 0;
        LOGGER.info("IskaUtils phased load/** reload complete");
        Runnable done = onComplete;
        onComplete = () -> {};
        done.run();
    }

    private static final int PHASE_COUNT = 5;

    private static String phaseName(int index) {
        return switch (index) {
            case 0 -> "core load json";
            case 1 -> "structures";
            case 2 -> "factory sources";
            case 3 -> "delivery/tablet/arcane";
            case 4 -> "post-sync";
            default -> "unknown";
        };
    }

    private static void runPhase(MinecraftServer server, int index) {
        ResourceManager rm = server.getResourceManager();
        switch (index) {
            case 0 -> {
                CommandItemRegistry.reloadDefinitions();
                DynamicPotionPlateScanner.loadAll(rm);
                StructureMonouseLoader.loadAll(rm);
                ShopLoader.loadAll(rm);
                MacroLoader.reloadAllMacros();
                StageActionsLoader.loadAll(rm);
                StageItemHandler.loadAll(rm);
            }
            case 1 -> StructureLoader.scanConfigDirectoryServerOnly();
            case 2 -> FactoryLoader.loadFromRecipeManager(server.getRecipeManager(), rm);
            case 3 -> {
                SuspiciousDeliveryLoader.loadAll(rm);
                AncientTabletRecipeLoader.loadAll(rm);
                ArcaneDictionaryLoader.loadAll(rm);
            }
            case 4 -> {
                if (net.neoforged.fml.ModList.get().isLoaded("ftbteams")) {
                    try {
                        net.unfamily.iskalib.integration.ftbteams.FtbTeamsEvents.init();
                    } catch (Throwable t) {
                        LOGGER.error("Error initializing FTB Teams integration: {}", t.getMessage());
                    }
                }
                for (var player : server.getPlayerList().getPlayers()) {
                    net.unfamily.iskalib.team.ShopTeamManager.getInstance(player.serverLevel()).getPlayerTeam(player);
                    ModMessages.sendStructureSyncPacket(player);
                }
            }
            default -> {}
        }
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }
}
