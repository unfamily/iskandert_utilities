package net.unfamily.iskautils.data.load;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Hooks vanilla {@code /reload}; actual IskaUtils reload runs next server tick once the new
 * {@link net.minecraft.server.packs.resources.ResourceManager} is installed on the server.
 */
public final class IskaUtilsLoadReloadListener extends SimplePreparableReloadListener<Object> {

    @Override
    protected Object prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return null;
    }

    @Override
    protected void apply(Object prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        IskaUtilsLoadReloadScheduler.scheduleAfterVanillaReload();
    }
}
