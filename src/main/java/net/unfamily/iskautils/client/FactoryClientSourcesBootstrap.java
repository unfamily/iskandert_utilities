package net.unfamily.iskautils.client;

import net.minecraft.client.Minecraft;
import net.unfamily.iskautils.data.load.FactoryLoader;

/**
 * Loads Factory mappings from the local {@link net.minecraft.world.item.crafting.RecipeManager} on client.
 * On 26.+, only the integrated server exposes RecipeManager; MP clients rely on server-applied sources
 * / JEI refresh after recipe sync.
 */
public final class FactoryClientSourcesBootstrap {

    private FactoryClientSourcesBootstrap() {}

    /** Always refresh when an integrated server RecipeManager is available (post-{@code /reload}). */
    public static void ensureLoaded() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        var server = mc.getSingleplayerServer();
        if (server != null) {
            FactoryLoader.loadFromRecipeManager(server.getRecipeManager());
        }
    }
}
