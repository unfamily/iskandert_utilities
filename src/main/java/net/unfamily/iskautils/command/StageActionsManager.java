package net.unfamily.iskautils.command;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages execution of stage actions when stages are added or removed.
 */
public class StageActionsManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(2);

    private static boolean executingActions = false;

    /**
     * Called when a player stage is added or removed.
     */
    public static void onPlayerStageChanged(ServerPlayer player, String stageId, boolean wasAdded) {
        if (player == null || player.getServer() == null) return;
        onStageChanged("player", stageId, wasAdded, player, null);
    }

    /**
     * Called when a world stage is added or removed.
     * Note: World stages have no player context; actions using @p may not work as expected.
     */
    public static void onWorldStageChanged(MinecraftServer server, String stageId, boolean wasAdded) {
        if (server == null) return;
        onStageChanged("world", stageId, wasAdded, null, null);
    }

    /**
     * Called when a team stage is added or removed.
     * Note: For team stages we would need to run for each player in the team - not yet implemented.
     */
    public static void onTeamStageChanged(MinecraftServer server, String teamName, String stageId, boolean wasAdded) {
        if (server == null) return;
        onStageChanged("team", stageId, wasAdded, null, teamName);
    }

    private static void onStageChanged(String stageType, String stageId, boolean wasAdded,
                                       ServerPlayer player, String teamName) {
        // Player stages only for now - world/team need different handling
        if (!"player".equalsIgnoreCase(stageType) || player == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Stage actions: ignoring non-player stage change (type={}, stage={})", stageType, stageId);
            }
            return;
        }

        if (executingActions) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Stage actions: skipping recursive call for stage {} on player {}", 
                    stageId, player.getName().getString());
            }
            return;
        }

        List<StageActionDefinition> actions = StageActionsLoader.getLoadedActions();
        if (actions.isEmpty()) return;

        for (StageActionDefinition def : actions) {
            try {
                if (!def.matchesTrigger(stageType, stageId, wasAdded)) continue;
                if (!def.checkStages(player)) continue;

                List<StageActionDefinition.StageAction> toExecute = null;

                if (def.hasIfBranches()) {
                    for (StageActionDefinition.IfBranch branch : def.getIfBranches()) {
                        if (def.checkConditionsByIndices(player, branch.conditions)) {
                            toExecute = branch.doActions;
                            break;
                        }
                    }
                }
                if (toExecute == null && def.hasDoActions()) {
                    toExecute = def.getDoActions();
                }

                if (toExecute != null && !toExecute.isEmpty()) {
                    executingActions = true;
                    try {
                        executeActionSequence(player, toExecute, 0);
                    } finally {
                        executingActions = false;
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error processing stage action for stage {}: {}", stageId, e.getMessage());
                if (LOGGER.isDebugEnabled()) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void executeActionSequence(ServerPlayer player, 
                                              List<StageActionDefinition.StageAction> actions, 
                                              int index) {
        if (index >= actions.size() || player.isRemoved() || !player.isAlive()) {
            return;
        }

        ServerLevel level = player.serverLevel();
        StageActionDefinition.StageAction action = actions.get(index);

        if (action.execute != null && !action.execute.isEmpty()) {
            executeCommand(player, action.execute);
            scheduleNext(player, level, actions, index + 1);
        } else if (action.delay > 0) {
            long delayMs = (long) action.delay * 50 + 5;
            MinecraftServer server = player.getServer();
            EXECUTOR.schedule(() -> {
                if (server != null && !player.isRemoved() && player.isAlive()) {
                    server.execute(() -> executeActionSequence(player, actions, index + 1));
                }
            }, delayMs, TimeUnit.MILLISECONDS);
        } else {
            scheduleNext(player, level, actions, index + 1);
        }
    }

    private static void scheduleNext(ServerPlayer player, ServerLevel level,
                                     List<StageActionDefinition.StageAction> actions, int nextIndex) {
        if (nextIndex >= actions.size()) return;
        MinecraftServer server = player.getServer();
        if (server != null) {
            server.execute(() -> {
                if (!player.isRemoved() && player.isAlive()) {
                    executeActionSequence(player, actions, nextIndex);
                }
            });
        }
    }

    /**
     * Executes an action by id for the given players.
     * @param actionId The action id
     * @param players Players to run the action for (commands use @p per player)
     * @param force If true, bypasses onCall check
     * @return Number of players the action was executed for, or -1 if action not found
     */
    public static int executeActionById(String actionId, List<ServerPlayer> players, boolean force) {
        if (actionId == null || actionId.isEmpty() || players == null || players.isEmpty()) {
            return 0;
        }
        StageActionDefinition def = StageActionsLoader.getActionById(actionId);
        if (def == null) {
            return -1;
        }
        if (!force && !def.isOnCall()) {
            return 0;
        }
        int executed = 0;
        for (ServerPlayer player : players) {
            if (player == null || player.isRemoved() || !player.isAlive()) continue;
            try {
                if (!def.checkStages(player)) continue;
                List<StageActionDefinition.StageAction> toExecute = null;
                if (def.hasIfBranches()) {
                    for (StageActionDefinition.IfBranch branch : def.getIfBranches()) {
                        if (def.checkConditionsByIndices(player, branch.conditions)) {
                            toExecute = branch.doActions;
                            break;
                        }
                    }
                }
                if (toExecute == null && def.hasDoActions()) {
                    toExecute = def.getDoActions();
                }
                if (toExecute != null && !toExecute.isEmpty()) {
                    executingActions = true;
                    try {
                        executeActionSequence(player, toExecute, 0);
                        executed++;
                    } finally {
                        executingActions = false;
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error executing stage action {} for player {}: {}", actionId, player.getName().getString(), e.getMessage());
            }
        }
        return executed;
    }

    private static void executeCommand(ServerPlayer player, String command) {
        try {
            MinecraftServer server = player.getServer();
            if (server == null) return;

            CommandSourceStack source = player.createCommandSourceStack()
                .withPosition(player.position())
                .withEntity(player)
                .withRotation(player.getRotationVector());

            server.getCommands().performPrefixedCommand(source, command);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Stage action executed command: '{}' for player {}", command, player.getName().getString());
            }
        } catch (Exception e) {
            LOGGER.error("Error executing stage action command '{}': {}", command, e.getMessage());
        }
    }
}
