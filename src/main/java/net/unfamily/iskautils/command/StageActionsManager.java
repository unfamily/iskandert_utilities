package net.unfamily.iskautils.command;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Thin bridge to Library stage-action execution.
 */
public final class StageActionsManager {
    private StageActionsManager() {}

    public static void onPlayerStageChanged(ServerPlayer player, String stageId, boolean wasAdded) {
        net.unfamily.iskalib.stage.StageActionsManager.onPlayerStageChanged(player, stageId, wasAdded);
    }

    public static void onWorldStageChanged(MinecraftServer server, String stageId, boolean wasAdded) {
        net.unfamily.iskalib.stage.StageActionsManager.onWorldStageChanged(server, stageId, wasAdded);
    }

    public static void onTeamStageChanged(MinecraftServer server, String teamName, String stageId, boolean wasAdded) {
        net.unfamily.iskalib.stage.StageActionsManager.onTeamStageChanged(server, teamName, stageId, wasAdded);
    }

    public static int executeActionById(String actionId, List<ServerPlayer> players, boolean force) {
        return net.unfamily.iskalib.stage.StageActionsManager.executeActionById(actionId, players, force);
    }
}
