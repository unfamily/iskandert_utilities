package net.unfamily.iskautils.crafting;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;

/**
 * Placeholder reload listener so Factory runs after {@link net.minecraft.world.item.crafting.RecipeManager}
 * is updated. Actual work is deferred to {@link net.unfamily.iskautils.data.load.IskaUtilsLoadReloadListener}
 * / {@link net.unfamily.iskautils.data.load.IskaUtilsPhasedReloadScheduler} to avoid blocking world join.
 */
public final class FactorySourcesReloadListener extends SimplePreparableReloadListener<Object> {
    @Override
    protected Object prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return null;
    }

    @Override
    protected void apply(Object prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        // Intentionally empty — see class javadoc.
    }
}
