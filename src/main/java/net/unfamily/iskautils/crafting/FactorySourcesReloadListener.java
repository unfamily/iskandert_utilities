package net.unfamily.iskautils.crafting;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.unfamily.iskautils.data.load.FactoryLoader;

/** Runs after vanilla reload listeners so {@link net.minecraft.world.item.crafting.RecipeManager} is current. */
public final class FactorySourcesReloadListener extends SimplePreparableReloadListener<Object> {
    @Override
    protected Object prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return null;
    }

    @Override
    protected void apply(Object prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            FactoryLoader.loadFromRecipeManager(server.getRecipeManager(), resourceManager);
        }
    }
}
