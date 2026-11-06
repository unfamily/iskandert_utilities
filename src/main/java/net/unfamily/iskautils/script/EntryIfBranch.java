package net.unfamily.iskautils.script;

import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.command.CommandItemAction;
import net.unfamily.iskautils.command.CommandItemDefinition;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryStageHost;
import org.jetbrains.annotations.Nullable;

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

    /** Offline match: mods + world stages; player/team indices require a claimed {@code ownerUuid}. */
    public boolean matchesOffline(
            @Nullable UUID ownerUuid, @Nullable Level level, SuspiciousDeliveryStageHost host) {
        if (host == null || ownerUuid == null) {
            return false;
        }
        CommandItemDefinition def = host.asDefinitionAdapter();
        if (!LoadModGate.checkModIndices(def.getMods(), def.getModsLogic(), modConditionIndices)) {
            return false;
        }
        if (conditionIndices.isEmpty()) {
            return true;
        }
        List<CommandItemDefinition.StageCondition> allStages = def.getStages();
        CommandItemDefinition.StagesLogic logic = def.getStagesLogic();
        if (logic == CommandItemDefinition.StagesLogic.DEF_AND
                || logic == CommandItemDefinition.StagesLogic.DEF_OR) {
            return true;
        }
        if (logic == CommandItemDefinition.StagesLogic.OR) {
            for (Integer index : conditionIndices) {
                if (index == null || index < 0 || index >= allStages.size()) {
                    continue;
                }
                if (host.checkSingleStageOffline(ownerUuid, level, allStages.get(index))) {
                    return true;
                }
            }
            return false;
        }
        for (Integer index : conditionIndices) {
            if (index == null || index < 0 || index >= allStages.size()) {
                return false;
            }
            if (!host.checkSingleStageOffline(ownerUuid, level, allStages.get(index))) {
                return false;
            }
        }
        return true;
    }
}
