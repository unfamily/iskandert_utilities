package net.unfamily.iskautils.guide;

import com.google.gson.stream.JsonWriter;
import guideme.Guide;
import guideme.GuidePageChange;
import guideme.Guides;
import guideme.compiler.ParsedGuidePage;
import guideme.indices.PageIndex;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

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

        var minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.execute(patch);
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
