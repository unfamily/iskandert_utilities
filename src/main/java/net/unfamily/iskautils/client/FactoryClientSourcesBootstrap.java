package net.unfamily.iskautils.client;

import net.minecraft.client.Minecraft;
import net.unfamily.iskautils.data.load.FactoryLoader;

/**
 * Loads Factory datapack sources on the dedicated client (recipe holders are not synced for {@code factory_sources}).
 */
public final class FactoryClientSourcesBootstrap {

    private FactoryClientSourcesBootstrap() {}

    public static void ensureLoaded() {
        if (!FactoryLoader.getSources().isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        var server = mc.getSingleplayerServer();
        if (server != null) {
            FactoryLoader.loadFromRecipeManager(server.getRecipeManager(), server.getResourceManager());
            return;
        }
        FactoryLoader.loadFromMergedRecipeResources(mc.getResourceManager());
    }
}
