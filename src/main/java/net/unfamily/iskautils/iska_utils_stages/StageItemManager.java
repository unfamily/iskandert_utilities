package net.unfamily.iskautils.iska_utils_stages;

/**
 * Thin stub: stage-item events and bootstrap are owned by Library {@code StageItemEvents}/{@code StageBootstrap}.
 */
public final class StageItemManager {
    private StageItemManager() {}

    /** No-op; Library {@code StageBootstrap} initializes stage items. */
    public static void initialize(Object ignored) {}

    public static void reloadItemRestrictions() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        var rm = server != null ? server.getResourceManager() : null;
        StageItemHandler.loadAll(rm);
    }
}
