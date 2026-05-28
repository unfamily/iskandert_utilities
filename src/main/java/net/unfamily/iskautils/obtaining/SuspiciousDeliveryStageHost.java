package net.unfamily.iskautils.obtaining;

import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.command.CommandItemDefinition;

import java.util.List;

/**
 * Stage evaluation for a suspicious delivery entry (same rules as command items).
 */
public final class SuspiciousDeliveryStageHost {
    private final List<CommandItemDefinition.StageCondition> stages;
    private final CommandItemDefinition.StagesLogic stagesLogic;

    public SuspiciousDeliveryStageHost(
            List<CommandItemDefinition.StageCondition> stages,
            CommandItemDefinition.StagesLogic stagesLogic) {
        this.stages = List.copyOf(stages);
        this.stagesLogic = stagesLogic;
    }

    public List<CommandItemDefinition.StageCondition> getStages() {
        return stages;
    }

    public CommandItemDefinition.StagesLogic getStagesLogic() {
        return stagesLogic;
    }

    public boolean checkAllStages(ServerPlayer player) {
        if (stagesLogic == CommandItemDefinition.StagesLogic.DEF_AND
                || stagesLogic == CommandItemDefinition.StagesLogic.DEF_OR) {
            return true;
        }
        if (stages.isEmpty()) {
            return true;
        }
        if (stagesLogic == CommandItemDefinition.StagesLogic.AND) {
            for (CommandItemDefinition.StageCondition stage : stages) {
                if (!checkSingleStage(player, stage)) {
                    return false;
                }
            }
            return true;
        }
        for (CommandItemDefinition.StageCondition stage : stages) {
            if (checkSingleStage(player, stage)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkSingleStage(ServerPlayer player, CommandItemDefinition.StageCondition condition) {
        String stageId = condition.getStage();
        boolean shouldBeSet = condition.shouldBeSet();
        net.unfamily.iskalib.stage.StageRegistry registry =
                net.unfamily.iskalib.stage.StageRegistry.getInstance(
                        ((net.minecraft.server.level.ServerLevel) player.level()).getServer());
        return switch (condition.getStageType().toLowerCase()) {
            case "player" -> registry.hasPlayerStage(player, stageId) == shouldBeSet;
            case "world" -> registry.hasWorldStage(stageId) == shouldBeSet;
            case "team" -> registry.hasPlayerTeamStage(player, stageId) == shouldBeSet;
            default -> false;
        };
    }

    /** Adapter so {@link net.unfamily.iskautils.command.CommandItemAction} IF blocks can resolve stage indices. */
    public CommandItemDefinition asDefinitionAdapter() {
        CommandItemDefinition def = new CommandItemDefinition("suspicious_delivery_stage_host");
        def.setStagesLogic(stagesLogic);
        for (CommandItemDefinition.StageCondition stage : stages) {
            def.addStage(stage);
        }
        return def;
    }
}
