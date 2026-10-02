package net.unfamily.iskautils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.RecipeManager;
import net.unfamily.iskautils.data.load.FactoryLoader;

/** Ensures Factory sources are populated from the client RecipeManager (synced recipes). */
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
        RecipeManager recipes = null;
        var server = mc.getSingleplayerServer();
        if (server != null) {
            recipes = server.getRecipeManager();
        } else if (mc.level != null) {
            recipes = mc.level.getRecipeManager();
        }
        if (recipes != null) {
            FactoryLoader.loadFromRecipeManager(recipes);
        }
    }
}
