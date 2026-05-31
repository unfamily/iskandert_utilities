package net.unfamily.iskautils.script;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.unfamily.iskautils.command.CommandItemDefinition;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryStageHost;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shared parsing for top-level {@code if[]} arrays (Ancient Tab, Factory, etc.).
 */
public final class LoadEntryIfParser {
    private static final Logger LOGGER = LogUtils.getLogger();

    private LoadEntryIfParser() {}

    public static SuspiciousDeliveryStageHost parseGateHost(JsonObject obj) {
        List<CommandItemDefinition.StageCondition> stages = List.of();
        CommandItemDefinition.StagesLogic stagesLogic = CommandItemDefinition.StagesLogic.AND;
        if (obj.has("stages") && obj.get("stages").isJsonArray()) {
            stages = LoadActionParser.parseStages(obj.getAsJsonArray("stages"));
        }
        if (obj.has("stages_logic")) {
            stagesLogic = LoadActionParser.parseStagesLogic(obj.get("stages_logic").getAsString());
        }
        return new SuspiciousDeliveryStageHost(
                stages,
                stagesLogic,
                LoadActionParser.parseMods(obj),
                LoadActionParser.parseModsLogic(obj));
    }

    public static List<EntryIfBranch> parseTopLevelIfBranches(JsonArray ifArray, String contextId) {
        List<EntryIfBranch> out = new ArrayList<>();
        if (ifArray == null) {
            return List.of();
        }
        for (JsonElement el : ifArray) {
            parseTopLevelIfBranch(el, contextId).ifPresent(out::add);
        }
        return List.copyOf(out);
    }

    public static Optional<EntryIfBranch> parseTopLevelIfBranch(JsonElement el, String contextId) {
        if (el == null || !el.isJsonArray()) {
            LOGGER.warn("Invalid top-level if branch in {} (expected JSON array)", contextId);
            return Optional.empty();
        }
        JsonArray branch = el.getAsJsonArray();
        if (branch.isEmpty() || !branch.get(0).isJsonObject()) {
            LOGGER.warn("Invalid top-level if branch in {} (empty or missing conditions object)", contextId);
            return Optional.empty();
        }
        JsonObject conditionsObj = branch.get(0).getAsJsonObject();
        List<Integer> stageIndices = parseIndexArray(conditionsObj, "conditions");
        List<Integer> modIndices = parseIndexArray(conditionsObj, "mod_conditions");
        return Optional.of(new EntryIfBranch(stageIndices, modIndices));
    }

    public static Optional<JsonObject> payloadObject(JsonElement branchEl, String contextId) {
        if (branchEl == null || !branchEl.isJsonArray()) {
            return Optional.empty();
        }
        JsonArray branch = branchEl.getAsJsonArray();
        if (branch.size() < 2 || !branch.get(1).isJsonObject()) {
            LOGGER.warn("Invalid top-level if branch in {} (missing payload object)", contextId);
            return Optional.empty();
        }
        return Optional.of(branch.get(1).getAsJsonObject());
    }

    private static List<Integer> parseIndexArray(JsonObject obj, String key) {
        List<Integer> out = new ArrayList<>();
        if (!obj.has(key) || !obj.get(key).isJsonArray()) {
            return List.of();
        }
        for (JsonElement indexElement : obj.getAsJsonArray(key)) {
            if (indexElement.isJsonPrimitive() && indexElement.getAsJsonPrimitive().isNumber()) {
                out.add(indexElement.getAsInt());
            }
        }
        return List.copyOf(out);
    }
}
