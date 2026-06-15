package net.unfamily.iskautils.data.load;

import net.unfamily.iskautils.util.ModLogger;

import net.minecraft.server.MinecraftServer;

/**
 * Defers IskaUtils {@code load/**} reload until the server tick after vanilla {@code /reload}
 * has swapped in the new {@link net.minecraft.server.packs.resources.ResourceManager}.
 */
public final class IskaUtilsLoadReloadScheduler {

    private static final ModLogger LOGGER = ModLogger.of(IskaUtilsLoadReloadScheduler.class);
    private static volatile boolean pending;

    private IskaUtilsLoadReloadScheduler() {}

    /** Called from {@link IskaUtilsLoadReloadListener#apply} when vanilla reload listeners finish. */
    public static void scheduleAfterVanillaReload() {
        pending = true;
        LOGGER.debug("Scheduled IskaUtils load/** reload for next server tick");
    }

    /** Runs on {@link net.neoforged.neoforge.event.tick.ServerTickEvent.Post} (lowest priority). */
    public static void runPending(MinecraftServer server) {
        if (!pending || server == null) {
            return;
        }
        pending = false;
        LOGGER.info("Running deferred IskaUtils load/** reload after vanilla datapack reload");
        IskaUtilsPhasedReloadScheduler.schedule(server, IskaUtilsLoadReloadEffects::notifyShopClientsAfterReload);
    }
}
