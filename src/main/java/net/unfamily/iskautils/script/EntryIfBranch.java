package net.unfamily.iskautils.script;

import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.command.CommandItemAction;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryStageHost;

import java.util.List;

/**
 * Parsed top-level {@code if[]} branch: condition indices + deferred evaluation via host logic.
 */
public record EntryIfBranch(List<Integer> conditionIndices, List<Integer> modConditionIndices) {

    public EntryIfBranch {
        conditionIndices = conditionIndices != null ? List.copyOf(conditionIndices) : List.of();
        modConditionIndices = modConditionIndices != null ? List.copyOf(modConditionIndices) : List.of();
    }

    public boolean matches(ServerPlayer player, SuspiciousDeliveryStageHost host) {
        if (player == null || host == null) {
            return false;
        }
        CommandItemAction probe = new CommandItemAction();
        probe.setType(CommandItemAction.ActionType.IF);
        probe.setConditionIndices(conditionIndices);
        probe.setModConditionIndices(modConditionIndices);
        return probe.checkIfConditions(player, host.asDefinitionAdapter());
    }
}
