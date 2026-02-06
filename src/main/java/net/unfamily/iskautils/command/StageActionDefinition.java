package net.unfamily.iskautils.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.stage.StageRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Definition of a stage action that runs commands when a stage is added or removed.
 * Uses the same syntax as command macros and stage items: stages, stages_logic, do, if.
 */
public class StageActionDefinition {

    private final String id;
    private final boolean onCall;
    private final boolean onAdd;
    private final boolean onRemove;
    private final String stagesLogic;
    private final List<StageCondition> stages = new ArrayList<>();
    private final List<StageAction> doActions = new ArrayList<>();
    private final List<IfBranch> ifBranches = new ArrayList<>();

    public StageActionDefinition(String id, boolean onCall, boolean onAdd, boolean onRemove, String stagesLogic,
                                 List<StageCondition> stages,
                                 List<StageAction> doActions,
                                 List<IfBranch> ifBranches) {
        this.id = id != null ? id : "";
        this.onCall = onCall;
        this.onAdd = onAdd;
        this.onRemove = onRemove;
        this.stagesLogic = stagesLogic != null ? stagesLogic : "AND";
        if (stages != null) {
            this.stages.addAll(stages);
        }
        if (doActions != null) {
            this.doActions.addAll(doActions);
        }
        if (ifBranches != null) {
            this.ifBranches.addAll(ifBranches);
        }
    }

    public String getId() {
        return id;
    }

    public boolean isOnCall() {
        return onCall;
    }

    public boolean isOnAdd() {
        return onAdd;
    }

    public boolean isOnRemove() {
        return onRemove;
    }

    public String getStagesLogic() {
        return stagesLogic;
    }

    public List<StageCondition> getStages() {
        return stages;
    }

    public List<StageAction> getDoActions() {
        return doActions;
    }

    public List<IfBranch> getIfBranches() {
        return ifBranches;
    }

    public boolean hasDoActions() {
        return !doActions.isEmpty();
    }

    public boolean hasIfBranches() {
        return !ifBranches.isEmpty();
    }

    /**
     * Checks if this action should trigger for the given stage change.
     */
    public boolean matchesTrigger(String stageType, String stageId, boolean wasAdded) {
        if (wasAdded && !onAdd) return false;
        if (!wasAdded && !onRemove) return false;

        // Check if the changed stage is in our stages list
        boolean stageInList = false;
        for (StageCondition cond : stages) {
            if (cond.stage.equals(stageId) && cond.stageType.equalsIgnoreCase(stageType)) {
                stageInList = true;
                break;
            }
        }
        return stageInList;
    }

    /**
     * Checks if stage conditions are satisfied for the given player (player stage type only).
     */
    public boolean checkStages(ServerPlayer player) {
        if (stages.isEmpty()) return true;

        StageRegistry registry = StageRegistry.getInstance(player.getServer());
        boolean defLogic = "DEF_AND".equalsIgnoreCase(stagesLogic) || "DEF_OR".equalsIgnoreCase(stagesLogic);

        if ("AND".equalsIgnoreCase(stagesLogic)) {
            for (StageCondition cond : stages) {
                if (!checkSingleStage(player, registry, cond)) return false;
            }
            return true;
        } else if ("OR".equalsIgnoreCase(stagesLogic)) {
            for (StageCondition cond : stages) {
                if (checkSingleStage(player, registry, cond)) return true;
            }
            return stages.isEmpty();
        } else if (defLogic) {
            // DEF logic is evaluated per if-branch, not here
            return true;
        }
        return true;
    }

    private static boolean checkSingleStage(ServerPlayer player, StageRegistry registry, StageCondition cond) {
        boolean hasStage = false;
        switch (cond.stageType.toLowerCase()) {
            case "player" -> hasStage = registry.hasPlayerStage(player, cond.stage);
            case "world" -> hasStage = registry.hasWorldStage(cond.stage);
            case "team" -> hasStage = registry.hasPlayerTeamStage(player, cond.stage);
            default -> {}
        }
        return hasStage == cond.is;
    }

    /**
     * Checks if conditions by indices are met (for DEF_AND/DEF_OR if branches).
     */
    public boolean checkConditionsByIndices(ServerPlayer player, List<Integer> conditionIndices) {
        if (conditionIndices == null || conditionIndices.isEmpty()) return true;

        StageRegistry registry = StageRegistry.getInstance(player.getServer());
        boolean defAnd = "DEF_AND".equalsIgnoreCase(stagesLogic);

        if (defAnd) {
            for (Integer idx : conditionIndices) {
                if (idx < 0 || idx >= stages.size()) return false;
                StageCondition cond = stages.get(idx);
                if (!checkSingleStage(player, registry, cond)) return false;
            }
            return true;
        } else {
            for (Integer idx : conditionIndices) {
                if (idx >= 0 && idx < stages.size()) {
                    StageCondition cond = stages.get(idx);
                    if (checkSingleStage(player, registry, cond)) return true;
                }
            }
            return false;
        }
    }

    public static class StageCondition {
        public final String stageType;
        public final String stage;
        public final boolean is;

        public StageCondition(String stageType, String stage, boolean is) {
            this.stageType = stageType != null ? stageType : "player";
            this.stage = stage;
            this.is = is;
        }
    }

    /**
     * Single action: execute or delay
     */
    public static class StageAction {
        public final String execute;
        public final int delay;

        public StageAction(String execute, int delay) {
            this.execute = execute;
            this.delay = delay;
        }
    }

    /**
     * If branch with conditions and do actions
     */
    public static class IfBranch {
        public final List<Integer> conditions;
        public final List<StageAction> doActions;

        public IfBranch(List<Integer> conditions, List<StageAction> doActions) {
            this.conditions = conditions != null ? conditions : new ArrayList<>();
            this.doActions = doActions != null ? doActions : new ArrayList<>();
        }
    }

    /**
     * Parses a stage action from JSON
     */
    public static StageActionDefinition fromJson(JsonObject json) {
        if (!json.has("id") || !json.get("id").isJsonPrimitive()) {
            throw new IllegalArgumentException("Stage action requires 'id' field");
        }
        String id = json.get("id").getAsString();
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Stage action 'id' cannot be empty");
        }
        boolean onCall = !json.has("onCall") || json.get("onCall").getAsBoolean();
        boolean onAdd = !json.has("onAdd") || json.get("onAdd").getAsBoolean();
        boolean onRemove = !json.has("onRemove") || json.get("onRemove").getAsBoolean();
        String stagesLogic = json.has("stages_logic") ? json.get("stages_logic").getAsString() : "AND";

        List<StageCondition> stages = parseStages(json);
        List<StageAction> doActions = new ArrayList<>();
        List<IfBranch> ifBranches = new ArrayList<>();

        if (json.has("do") && json.get("do").isJsonArray()) {
            doActions = parseDoActions(json.getAsJsonArray("do"));
        }
        if (json.has("if") && json.get("if").isJsonArray()) {
            ifBranches = parseIfBranches(json.getAsJsonArray("if"));
        }

        return new StageActionDefinition(id, onCall, onAdd, onRemove, stagesLogic, stages, doActions, ifBranches);
    }

    private static List<StageCondition> parseStages(JsonObject json) {
        List<StageCondition> result = new ArrayList<>();
        if (!json.has("stages") || !json.get("stages").isJsonArray()) return result;

        JsonArray arr = json.getAsJsonArray("stages");
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();
            String stage = obj.has("stage") ? obj.get("stage").getAsString() : null;
            if (stage == null) continue;
            String stageType = obj.has("stage_type") ? obj.get("stage_type").getAsString() : "player";
            boolean is = !obj.has("is") || obj.get("is").getAsBoolean();
            result.add(new StageCondition(stageType, stage, is));
        }
        return result;
    }

    private static List<StageAction> parseDoActions(JsonArray arr) {
        List<StageAction> result = new ArrayList<>();
        for (JsonElement el : arr) {
            StageAction a = parseAction(el);
            if (a != null) result.add(a);
        }
        return result;
    }

    private static List<IfBranch> parseIfBranches(JsonArray arr) {
        List<IfBranch> result = new ArrayList<>();
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();
            List<Integer> conditions = new ArrayList<>();
            if (obj.has("conditions") && obj.get("conditions").isJsonArray()) {
                for (JsonElement c : obj.getAsJsonArray("conditions")) {
                    if (c.isJsonPrimitive() && c.getAsJsonPrimitive().isNumber()) {
                        conditions.add(c.getAsInt());
                    }
                }
            }
            List<StageAction> doActions = new ArrayList<>();
            if (obj.has("do") && obj.get("do").isJsonArray()) {
                doActions = parseDoActions(obj.getAsJsonArray("do"));
            }
            result.add(new IfBranch(conditions, doActions));
        }
        return result;
    }

    private static StageAction parseAction(JsonElement el) {
        if (!el.isJsonObject()) return null;
        JsonObject obj = el.getAsJsonObject();
        if (obj.has("execute")) {
            return new StageAction(obj.get("execute").getAsString(), 0);
        }
        if (obj.has("delay")) {
            return new StageAction(null, obj.get("delay").getAsInt());
        }
        return null;
    }
}
