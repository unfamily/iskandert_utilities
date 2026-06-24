package net.unfamily.iskautils.data.load;

import net.unfamily.iskautils.util.ModLogger;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.unfamily.iskautils.command.MacroLoader;
import net.unfamily.iskautils.command.StageActionsLoader;
import net.unfamily.iskautils.data.DynamicPotionPlateScanner;
import net.unfamily.iskautils.iska_utils_stages.StageItemHandler;
import net.unfamily.iskautils.item.CommandItemRegistry;
import net.unfamily.iskautils.shop.ShopLoader;
import net.unfamily.iskalib.structure.StructureLoader;
import net.unfamily.iskautils.structure.StructureMonouseLoader;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRecipeLoader;
import net.unfamily.iskautils.arcane.ArcaneDictionaryLoader;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryLoader;

/**
 * Reloads IskaUtils JSON from the {@code load} tree in all datapacks using the current server {@link ResourceManager}.
 */
public final class IskaUtilsDataReload {
    private static final ModLogger LOGGER = ModLogger.of(IskaUtilsDataReload.class);

    private IskaUtilsDataReload() {}

    public static void reloadAllFromServer() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            LOGGER.warn("reloadAllFromServer called with no server; skipping datapack merge");
            return;
        }
        long totalStart = System.nanoTime();
        LOGGER.info("IskaUtils load/** reload starting...");
        ResourceManager rm = server.getResourceManager();

        // Must be safe to call multiple times and during /reload.
        runPhase("command items", () -> {
            net.unfamily.iskautils.command.CommandItemLoader.loadAll(rm);
            CommandItemRegistry.reloadDefinitions();
        });
        runPhase("potion plates", () -> DynamicPotionPlateScanner.loadAll(rm));
        runPhase("structure monouse", () -> StructureMonouseLoader.loadAll(rm));
        runPhase("shop", () -> ShopLoader.loadAll(rm));
        runPhase("macros", MacroLoader::reloadAllMacros);
        runPhase("stage actions", () -> StageActionsLoader.loadAll(rm));
        runPhase("stage items", () -> StageItemHandler.loadAll(rm));
        runPhase("structures", () -> StructureLoader.reloadAllDefinitions(true));
        runPhase("factory sources", () -> FactoryLoader.loadFromRecipeManager(server.getRecipeManager(), rm));
        runPhase("suspicious delivery", () -> SuspiciousDeliveryLoader.loadAll(rm));
        runPhase("ancient tablet", () -> AncientTabletRecipeLoader.loadAll(rm));
        runPhase("arcane dictionary", () -> ArcaneDictionaryLoader.loadAll(rm));

        LOGGER.info("IskaUtils load/** reload finished in {} ms", elapsedMs(totalStart));
    }

    private static void runPhase(String name, Runnable action) {
        long start = System.nanoTime();
        action.run();
        LOGGER.info("  load/** phase '{}' took {} ms", name, elapsedMs(start));
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }
}

