package net.unfamily.iskautils.script;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.unfamily.iskautils.command.CommandItemAction;
import net.unfamily.iskautils.command.CommandItemDefinition;
import net.unfamily.iskautils.obtaining.MessageSpec;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared JSON parsing for {@code do[]} action lists (command items, suspicious delivery, etc.).
 */
public final class LoadActionParser {
    private static final Logger LOGGER = LogUtils.getLogger();

    private LoadActionParser() {}

    public static List<CommandItemAction> parseActions(JsonArray array, String contextId) {
        List<CommandItemAction> out = new ArrayList<>();
        if (array == null) {
            return out;
        }
        for (JsonElement el : array) {
            if (!el.isJsonObject()) {
                continue;
            }
            CommandItemAction action = parseItemAction(el.getAsJsonObject(), contextId);
            if (action != null) {
                parseActionStages(el.getAsJsonObject(), action);
                out.add(action);
            }
        }
        return List.copyOf(out);
    }

    public static CommandItemAction parseItemAction(JsonObject actionJson, String contextId) {
        try {
            CommandItemAction action = new CommandItemAction();

            if (actionJson.has("execute")) {
                action.setType(CommandItemAction.ActionType.EXECUTE);
                action.setCommand(actionJson.get("execute").getAsString());
            } else if (actionJson.has("message") && actionJson.get("message").isJsonObject()) {
                action.setType(CommandItemAction.ActionType.MESSAGE);
                action.setMessage(MessageSpec.fromJson(actionJson.getAsJsonObject("message"), LOGGER, contextId));
            } else if (actionJson.has("delay")) {
                action.setType(CommandItemAction.ActionType.DELAY);
                action.setDelay(actionJson.get("delay").getAsInt());
            } else if (actionJson.has("drop")) {
                String id = actionJson.get("drop").getAsString();
                Identifier rl = Identifier.tryParse(id);
                if (rl == null) {
                    LOGGER.warn("Invalid drop id '{}' in {}", id, contextId);
                    return null;
                }
                action.setType(CommandItemAction.ActionType.DROP);
                action.setDropItemId(rl);
            } else if (actionJson.has("item")) {
                action.setType(CommandItemAction.ActionType.ITEM);
                String itemAction = actionJson.get("item").getAsString().toLowerCase();
                switch (itemAction) {
                    case "delete_all" -> action.setItemAction(CommandItemAction.ItemActionType.DELETE_ALL);
                    case "delete" -> action.setItemAction(CommandItemAction.ItemActionType.DELETE);
                    case "drop_all" -> action.setItemAction(CommandItemAction.ItemActionType.DROP_ALL);
                    case "drop" -> action.setItemAction(CommandItemAction.ItemActionType.DROP);
                    case "consume" -> action.setItemAction(CommandItemAction.ItemActionType.CONSUME);
                    case "damage" -> action.setItemAction(CommandItemAction.ItemActionType.DAMAGE);
                    default -> {
                        LOGGER.warn("Unknown item action: {}", itemAction);
                        return null;
                    }
                }
            } else if (actionJson.has("if") && actionJson.get("if").isJsonArray()) {
                action.setType(CommandItemAction.ActionType.IF);
                JsonArray ifArray = actionJson.getAsJsonArray("if");
                if (!ifArray.isEmpty() && ifArray.get(0).isJsonObject()) {
                    JsonObject conditionsObj = ifArray.get(0).getAsJsonObject();
                    if (conditionsObj.has("conditions") && conditionsObj.get("conditions").isJsonArray()) {
                        List<Integer> indices = new ArrayList<>();
                        for (JsonElement indexElement : conditionsObj.getAsJsonArray("conditions")) {
                            if (indexElement.isJsonPrimitive()) {
                                indices.add(indexElement.getAsInt());
                            }
                        }
                        action.setConditionIndices(indices);
                    }
                    if (conditionsObj.has("mod_conditions") && conditionsObj.get("mod_conditions").isJsonArray()) {
                        List<Integer> modIndices = new ArrayList<>();
                        for (JsonElement indexElement : conditionsObj.getAsJsonArray("mod_conditions")) {
                            if (indexElement.isJsonPrimitive()) {
                                modIndices.add(indexElement.getAsInt());
                            }
                        }
                        action.setModConditionIndices(modIndices);
                    }
                }
                for (int i = 1; i < ifArray.size(); i++) {
                    if (ifArray.get(i).isJsonObject()) {
                        CommandItemAction sub = parseItemAction(ifArray.get(i).getAsJsonObject(), contextId);
                        if (sub != null) {
                            parseActionStages(ifArray.get(i).getAsJsonObject(), sub);
                            action.addSubAction(sub);
                        }
                    }
                }
                if (action.getSubActions().isEmpty()) {
                    LOGGER.warn("No valid sub-actions in 'if' block ({})", contextId);
                    return null;
                }
            } else {
                LOGGER.warn("Unknown action type in {}: {}", contextId, actionJson);
                return null;
            }
            return action;
        } catch (Exception e) {
            LOGGER.error("Error parsing action in {}: {}", contextId, e.getMessage());
            return null;
        }
    }

    public static void parseActionStages(JsonObject actionJson, CommandItemAction action) {
        if (!actionJson.has("stages") || !actionJson.get("stages").isJsonArray()) {
            return;
        }
        for (JsonElement stageElement : actionJson.getAsJsonArray("stages")) {
            if (!stageElement.isJsonObject()) {
                continue;
            }
            JsonObject stageJson = stageElement.getAsJsonObject();
            String stageType = stageJson.has("stage_type") ? stageJson.get("stage_type").getAsString() : "player";
            String stage = stageJson.get("stage").getAsString();
            boolean is = !stageJson.has("is") || stageJson.get("is").getAsBoolean();
            action.addStage(stageType, stage, is);
        }
    }

    public static List<CommandItemDefinition.StageCondition> parseStages(JsonArray stagesArray) {
        List<CommandItemDefinition.StageCondition> out = new ArrayList<>();
        if (stagesArray == null) {
            return out;
        }
        for (JsonElement stageElement : stagesArray) {
            if (!stageElement.isJsonObject()) {
                continue;
            }
            JsonObject stageJson = stageElement.getAsJsonObject();
            String stageType = stageJson.has("stage_type") ? stageJson.get("stage_type").getAsString() : "player";
            String stage = stageJson.get("stage").getAsString();
            boolean is = !stageJson.has("is") || stageJson.get("is").getAsBoolean();
            out.add(new CommandItemDefinition.StageCondition(stageType, stage, is));
        }
        return List.copyOf(out);
    }

    public static CommandItemDefinition.StagesLogic parseStagesLogic(String raw) {
        if (raw == null || raw.isBlank()) {
            return CommandItemDefinition.StagesLogic.AND;
        }
        return switch (raw.toUpperCase()) {
            case "OR" -> CommandItemDefinition.StagesLogic.OR;
            case "DEF_AND", "DEF" -> CommandItemDefinition.StagesLogic.DEF_AND;
            case "DEF_OR" -> CommandItemDefinition.StagesLogic.DEF_OR;
            default -> CommandItemDefinition.StagesLogic.AND;
        };
    }

    public static List<LoadModCondition> parseMods(JsonObject obj) {
        return LoadModGate.parseMods(obj, "mods");
    }

    public static CommandItemDefinition.StagesLogic parseModsLogic(JsonObject obj) {
        if (obj == null || !obj.has("mods_logic")) {
            return CommandItemDefinition.StagesLogic.AND;
        }
        return LoadModGate.parseModsLogic(obj.get("mods_logic").getAsString());
    }
}
