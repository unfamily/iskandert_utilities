package net.unfamily.iskautils.client;

import net.minecraft.client.Minecraft;
import net.unfamily.iskautils.data.load.FactoryLoader;

/** Loads Factory mappings from the local {@link net.minecraft.world.item.crafting.RecipeManager} on client. */
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
            FactoryLoader.loadFromRecipeManager(server.getRecipeManager());
        }
    }
}
