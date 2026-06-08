package net.unfamily.iskautils.guide;

import com.google.gson.stream.JsonWriter;
import guideme.GuidePageChange;
import guideme.Guides;
import guideme.compiler.ParsedGuidePage;
import guideme.indices.PageIndex;
import java.io.IOException;
import java.util.List;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.unfamily.iskautils.util.ClientRuntimeAccess;

/**
 * Re-applies the dynamic The Roots navigation title after GuideME rebuilds its navigation tree.
 */
public final class TheRootsNavigationIndex implements PageIndex {
    @Override
    public String getName() {
        return "iska_utils_the_roots_nav";
    }

    @Override
    public boolean supportsUpdate() {
        return true;
    }

    @Override
    public void rebuild(List<ParsedGuidePage> pages) {
        schedulePatch();
    }

    @Override
    public void update(List<ParsedGuidePage> allPages, List<GuidePageChange> changes) {
        schedulePatch();
    }

    @Override
    public void export(JsonWriter writer) throws IOException {
        // Not exported
    }

    private static void schedulePatch() {
        Runnable patch = () -> TheRootsNavigationPatcher.patchGuide(Guides.getById(TheRootsNavigationPatcher.GUIDE_ID));

        if (net.unfamily.iskautils.util.ClientPlayerAccess.isClientEnvironment()) {
            ClientRuntimeAccess.runOnClientThread(patch);
            return;
        }

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(patch);
        } else {
            patch.run();
        }
    }
}
