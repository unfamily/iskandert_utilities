package net.unfamily.iskautils.obtaining;

import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.command.CommandItemDefinition;
import net.unfamily.iskautils.script.LoadModCondition;
import net.unfamily.iskautils.script.LoadModGate;

import java.util.List;

/**
 * Stage and mod evaluation for a suspicious delivery entry (same rules as command items).
 */
public final class SuspiciousDeliveryStageHost {
    private final List<CommandItemDefinition.StageCondition> stages;
    private final CommandItemDefinition.StagesLogic stagesLogic;
    private final List<LoadModCondition> mods;
    private final CommandItemDefinition.StagesLogic modsLogic;

    public SuspiciousDeliveryStageHost(
            List<CommandItemDefinition.StageCondition> stages,
            CommandItemDefinition.StagesLogic stagesLogic,
            List<LoadModCondition> mods,
            CommandItemDefinition.StagesLogic modsLogic) {
        this.stages = List.copyOf(stages);
        this.stagesLogic = stagesLogic;
        this.mods = mods != null ? List.copyOf(mods) : List.of();
        this.modsLogic = modsLogic != null ? modsLogic : CommandItemDefinition.StagesLogic.AND;
    }

    public List<CommandItemDefinition.StageCondition> getStages() {
        return stages;
    }

    public CommandItemDefinition.StagesLogic getStagesLogic() {
        return stagesLogic;
    }

    public List<LoadModCondition> getMods() {
        return mods;
    }

    public CommandItemDefinition.StagesLogic getModsLogic() {
        return modsLogic;
    }

    public boolean isEligible(ServerPlayer player) {
        return checkAllStages(player);
    }

    public boolean isFullyEligible(ServerPlayer player) {
        return checkAllMods() && checkAllStages(player);
    }

    public boolean isPoolEligible(ServerPlayer player) {
        return isFullyEligible(player);
    }

    public boolean isEmpty() {
        return stages.isEmpty() && mods.isEmpty();
    }

    public boolean checkAllMods() {
        if (LoadModGate.isDeferredLogic(modsLogic)) {
            return true;
        }
        if (mods.isEmpty()) {
            return true;
        }
        if (modsLogic == CommandItemDefinition.StagesLogic.AND) {
            for (LoadModCondition condition : mods) {
                if (!condition.matches()) {
                    return false;
                }
            }
            return true;
        }
        for (LoadModCondition condition : mods) {
            if (condition.matches()) {
                return true;
            }
        }
        return false;
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
                net.unfamily.iskalib.stage.StageRegistry.getInstance(player.getServer());
        return switch (condition.getStageType().toLowerCase()) {
            case "player" -> registry.hasPlayerStage(player, stageId) == shouldBeSet;
            case "world" -> registry.hasWorldStage(stageId) == shouldBeSet;
            case "team" -> registry.hasPlayerTeamStage(player, stageId) == shouldBeSet;
            default -> false;
        };
    }

    /**
     * Offline evaluation: world stages via server; player/team stages via UUID APIs
     * ({@code StageRegistry.hasPlayerStage}/{@code hasPlayerTeamStage}), including offline playerdata.
     */
    public boolean isFullyEligibleOffline(
            @org.jetbrains.annotations.Nullable java.util.UUID ownerUuid,
            @org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level) {
        if (!checkAllMods()) {
            return false;
        }
        return checkAllStagesOffline(ownerUuid, level);
    }

    public boolean checkAllStagesOffline(
            @org.jetbrains.annotations.Nullable java.util.UUID ownerUuid,
            @org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level) {
        if (stagesLogic == CommandItemDefinition.StagesLogic.DEF_AND
                || stagesLogic == CommandItemDefinition.StagesLogic.DEF_OR) {
            return true;
        }
        if (stages.isEmpty()) {
            return true;
        }
        if (stagesLogic == CommandItemDefinition.StagesLogic.AND) {
            for (CommandItemDefinition.StageCondition stage : stages) {
                if (!checkSingleStageOffline(ownerUuid, level, stage)) {
                    return false;
                }
            }
            return true;
        }
        for (CommandItemDefinition.StageCondition stage : stages) {
            if (checkSingleStageOffline(ownerUuid, level, stage)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkSingleStageOffline(
            @org.jetbrains.annotations.Nullable java.util.UUID ownerUuid,
            @org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level,
            CommandItemDefinition.StageCondition condition) {
        if (level == null || level.getServer() == null) {
            return false;
        }
        String stageId = condition.getStage();
        boolean shouldBeSet = condition.shouldBeSet();
        net.unfamily.iskalib.stage.StageRegistry registry =
                net.unfamily.iskalib.stage.StageRegistry.getInstance(level.getServer());
        return switch (condition.getStageType().toLowerCase()) {
            case "world" -> registry.hasWorldStage(stageId) == shouldBeSet;
            case "player" ->
                    ownerUuid != null && registry.hasPlayerStage(ownerUuid, stageId) == shouldBeSet;
            case "team" ->
                    ownerUuid != null && registry.hasPlayerTeamStage(ownerUuid, stageId) == shouldBeSet;
            default -> false;
        };
    }

    /** Adapter so {@link net.unfamily.iskautils.command.CommandItemAction} IF blocks can resolve stage/mod indices. */
    public CommandItemDefinition asDefinitionAdapter() {
        CommandItemDefinition def = new CommandItemDefinition("suspicious_delivery_stage_host");
        def.setStagesLogic(stagesLogic);
        for (CommandItemDefinition.StageCondition stage : stages) {
            def.addStage(stage);
        }
        def.setMods(mods);
        def.setModsLogic(modsLogic);
        return def;
    }
}
