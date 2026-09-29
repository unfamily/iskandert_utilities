package net.unfamily.iskautils.command;

import net.minecraft.server.packs.resources.ResourceManager;
import net.unfamily.iskalib.stage.StageActionDefinition;

import java.util.List;

/**
 * Thin bridge to Library stage-action catalog loading.
 */
public final class StageActionsLoader {
    private StageActionsLoader() {}

    public static void loadAll(ResourceManager resourceManagerOrNull) {
        net.unfamily.iskalib.stage.StageActionsLoader.loadAll(resourceManagerOrNull);
    }

    public static void scanConfigDirectory() {
        net.unfamily.iskalib.stage.StageActionsLoader.scanConfigDirectory();
    }

    public static List<StageActionDefinition> getLoadedActions() {
        return net.unfamily.iskalib.stage.StageActionsLoader.getLoadedActions();
    }

    public static StageActionDefinition getActionById(String id) {
        return net.unfamily.iskalib.stage.StageActionsLoader.getActionById(id);
    }

    public static List<String> getActionIds() {
        return net.unfamily.iskalib.stage.StageActionsLoader.getActionIds();
    }

    public static void reloadAllActions() {
        net.unfamily.iskalib.stage.StageActionsLoader.reloadAllActions();
    }
}
