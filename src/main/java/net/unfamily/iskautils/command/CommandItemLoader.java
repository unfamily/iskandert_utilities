package net.unfamily.iskautils.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.unfamily.iskautils.data.load.IskaUtilsLoadJson;
import net.unfamily.iskautils.data.load.IskaUtilsLoadPaths;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads command item definitions from external JSON files
 */
public class CommandItemLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    // Map to store command item definitions
    private static final Map<String, CommandItemDefinition> COMMAND_ITEMS = new HashMap<>();
    
    // Stores files with overwritable=false to prevent them from being overwritten
    private static final Map<String, Boolean> PROTECTED_DEFINITIONS = new HashMap<>();
    
    /**
     * Loads command item definitions from {@code data/<namespace>/load/iska_utils_command_items/} (merged datapacks),
     * or from the mod jar only when {@code resourceManagerOrNull} is null (early mod construction).
     */
    public static void loadAll(ResourceManager resourceManagerOrNull) {
        LOGGER.info("Loading command item definitions from datapack path load/{} ...", IskaUtilsLoadPaths.COMMAND_ITEMS);
        PROTECTED_DEFINITIONS.clear();
        COMMAND_ITEMS.clear();
        Map<Identifier, JsonElement> merged = resourceManagerOrNull != null
                ? IskaUtilsLoadJson.collectMergedJson(resourceManagerOrNull,
                id -> IskaUtilsLoadPaths.isJsonUnderLoadSubdir(id, IskaUtilsLoadPaths.COMMAND_ITEMS))
                : IskaUtilsLoadJson.collectFromModJarOnly(IskaUtilsLoadPaths.COMMAND_ITEMS);
        for (var e : IskaUtilsLoadJson.orderedEntries(merged)) {
            JsonElement jsonElement = e.getValue();
            if (jsonElement == null || !jsonElement.isJsonObject()) {
                continue;
            }
            String definitionId = IskaUtilsLoadJson.definitionIdFromLocation(e.getKey());
            parseConfigJson(definitionId, e.getKey().toString(), jsonElement.getAsJsonObject());
        }
        LOGGER.info("Command item definitions loaded: {}", COMMAND_ITEMS.size());
    }

    /** @deprecated use {@link #loadAll(ResourceManager)} with null at mod init */
    public static void scanConfigDirectory() {
        loadAll(null);
    }

    /**
     * Parses config from a JSON object
     */
    private static void parseConfigJson(String definitionId, String filePath, JsonObject json) {
        try {
            // Check if this is a command item definition file
            if (!json.has("type") || !json.get("type").getAsString().equals("iska_utils:command_item")) {
                LOGGER.debug("Skipping file {} - not a command item definition", filePath);
                return;
            }
            
            // Get overwritable status
            boolean overwritable = true;
            if (json.has("overwritable")) {
                overwritable = json.get("overwritable").getAsBoolean();
            }
            
            // Check if this is a protected file
            if (PROTECTED_DEFINITIONS.containsKey(definitionId) && !PROTECTED_DEFINITIONS.get(definitionId)) {
                LOGGER.debug("Skipping protected command item definition: {}", definitionId);
                return;
            }
            
            // Update protection status
            PROTECTED_DEFINITIONS.put(definitionId, overwritable);
            
            // Process the command items
            processItemsJson(json);
            
        } catch (Exception e) {
            LOGGER.error("Error processing command item definition {}: {}", definitionId, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Process the items array from a command item definition file
     */
    private static void processItemsJson(JsonObject json) {
        if (!json.has("items") || !json.get("items").isJsonArray()) {
            LOGGER.error("Command item definition file missing 'items' array");
            return;
        }
        
        JsonArray itemsArray = json.getAsJsonArray("items");
        for (JsonElement itemElement : itemsArray) {
            if (itemElement.isJsonObject()) {
                processItemDefinition(itemElement.getAsJsonObject());
            }
        }
    }
    
    /**
     * Process a single command item definition
     */
    private static void processItemDefinition(JsonObject itemJson) {
        try {
            // Get required ID
            if (!itemJson.has("id") || !itemJson.get("id").isJsonPrimitive()) {
                LOGGER.error("Command item definition missing 'id' field");
                return;
            }
            
            String itemId = itemJson.get("id").getAsString();
            
            // Create item definition
            CommandItemDefinition definition = new CommandItemDefinition(itemId);
            
            // Parse creative tab visibility
            if (itemJson.has("creative_tab")) {
                definition.setCreativeTabVisible(itemJson.get("creative_tab").getAsBoolean());
            }
            
            // Parse max stack size
            if (itemJson.has("stack_size")) {
                int stackSize = itemJson.get("stack_size").getAsInt();
                if (stackSize < 1) stackSize = 1;
                if (stackSize > 64) stackSize = 64;
                definition.setMaxStackSize(stackSize);
            }
            
            // Parse glowing/foil effect
            if (itemJson.has("is_foil")) {
                definition.setGlowing(itemJson.get("is_foil").getAsBoolean());
            }
            
            // Parse stages logic
            if (itemJson.has("stages_logic")) {
                String logic = itemJson.get("stages_logic").getAsString().toUpperCase();
                switch (logic) {
                    case "OR":
                        definition.setStagesLogic(CommandItemDefinition.StagesLogic.OR);
                        break;
                    case "DEF_AND":
                        definition.setStagesLogic(CommandItemDefinition.StagesLogic.DEF_AND);
                        break;
                    case "DEF_OR":
                        definition.setStagesLogic(CommandItemDefinition.StagesLogic.DEF_OR);
                        break;
                    case "DEF": // Retrocompatibilità: tratta DEF come DEF_AND
                        definition.setStagesLogic(CommandItemDefinition.StagesLogic.DEF_AND);
                        LOGGER.warn("Stage logic 'DEF' is deprecated, please use 'DEF_AND' instead for item {}", itemId);
                        break;
                    default:
                        definition.setStagesLogic(CommandItemDefinition.StagesLogic.AND);
                        break;
                }
            }
            
            // Parse stages
            if (itemJson.has("stages") && itemJson.get("stages").isJsonArray()) {
                JsonArray stagesArray = itemJson.getAsJsonArray("stages");
                for (JsonElement stageElement : stagesArray) {
                    if (stageElement.isJsonObject()) {
                        JsonObject stageJson = stageElement.getAsJsonObject();
                        String stageType = stageJson.has("stage_type") ? stageJson.get("stage_type").getAsString() : "player";
                        String stage = stageJson.get("stage").getAsString();
                        boolean is = stageJson.has("is") && stageJson.get("is").getAsBoolean();
                        
                        definition.addStage(stageType, stage, is);
                    }
                }
            }
            
            // Parse cooldown
            if (itemJson.has("cooldown")) {
                definition.setCooldown(itemJson.get("cooldown").getAsInt());
            }
            
            // Parse actions
            if (itemJson.has("do") && itemJson.get("do").isJsonArray()) {
                JsonArray actionsArray = itemJson.getAsJsonArray("do");
                for (JsonElement actionElement : actionsArray) {
                    if (actionElement.isJsonObject()) {
                        processActionDefinition(definition, actionElement.getAsJsonObject());
                    }
                }
            }
            
            // Register the command item definition
            COMMAND_ITEMS.put(itemId, definition);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Registered command item definition: {}", itemId);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error processing command item definition: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Process action definitions for a command item
     */
    private static void processActionDefinition(CommandItemDefinition definition, JsonObject actionJson) {
        
        // Process onTick actions
        if (actionJson.has("onTick") && actionJson.get("onTick").isJsonArray()) {
            JsonArray actionsArray = actionJson.getAsJsonArray("onTick");
            for (JsonElement actionElement : actionsArray) {
                if (actionElement.isJsonObject()) {
                    JsonObject action = actionElement.getAsJsonObject();
                    CommandItemAction itemAction = parseItemAction(action);
                    if (itemAction != null) {
                        // Parse action-specific stages if present
                        parseActionStages(action, itemAction);
                        definition.addTickAction(itemAction);
                    }
                }
            }
        }
        
        // Process onUse actions
        if (actionJson.has("onUse") && actionJson.get("onUse").isJsonArray()) {
            JsonArray actionsArray = actionJson.getAsJsonArray("onUse");
            for (JsonElement actionElement : actionsArray) {
                if (actionElement.isJsonObject()) {
                    JsonObject action = actionElement.getAsJsonObject();
                    CommandItemAction itemAction = parseItemAction(action);
                    if (itemAction != null) {
                        // Parse action-specific stages if present
                        parseActionStages(action, itemAction);
                        definition.addUseAction(itemAction);
                    }
                }
            }
        }
        
        // Process onFinishUse actions
        if (actionJson.has("onFinishUse") && actionJson.get("onFinishUse").isJsonArray()) {
            JsonArray actionsArray = actionJson.getAsJsonArray("onFinishUse");
            for (JsonElement actionElement : actionsArray) {
                if (actionElement.isJsonObject()) {
                    JsonObject action = actionElement.getAsJsonObject();
                    CommandItemAction itemAction = parseItemAction(action);
                    if (itemAction != null) {
                        // Parse action-specific stages if present
                        parseActionStages(action, itemAction);
                        definition.addFinishUseAction(itemAction);
                    }
                }
            }
        }
        
        // Process onUseOn actions
        if (actionJson.has("onUseOn") && actionJson.get("onUseOn").isJsonArray()) {
            JsonArray actionsArray = actionJson.getAsJsonArray("onUseOn");
            for (JsonElement actionElement : actionsArray) {
                if (actionElement.isJsonObject()) {
                    JsonObject action = actionElement.getAsJsonObject();
                    CommandItemAction itemAction = parseItemAction(action);
                    if (itemAction != null) {
                        // Parse action-specific stages if present
                        parseActionStages(action, itemAction);
                        definition.addUseOnAction(itemAction);
                    }
                }
            }
        }
        
        // Process onHitEntity actions
        if (actionJson.has("onHitEntity") && actionJson.get("onHitEntity").isJsonArray()) {
            JsonArray actionsArray = actionJson.getAsJsonArray("onHitEntity");
            for (JsonElement actionElement : actionsArray) {
                if (actionElement.isJsonObject()) {
                    JsonObject action = actionElement.getAsJsonObject();
                    CommandItemAction itemAction = parseItemAction(action);
                    if (itemAction != null) {
                        // Parse action-specific stages if present
                        parseActionStages(action, itemAction);
                        definition.addHitEntityAction(itemAction);
                    }
                }
            }
        }
        
        // Process onSwing actions
        if (actionJson.has("onSwing") && actionJson.get("onSwing").isJsonArray()) {
            JsonArray actionsArray = actionJson.getAsJsonArray("onSwing");
            for (JsonElement actionElement : actionsArray) {
                if (actionElement.isJsonObject()) {
                    JsonObject action = actionElement.getAsJsonObject();
                    CommandItemAction itemAction = parseItemAction(action);
                    if (itemAction != null) {
                        // Parse action-specific stages if present
                        parseActionStages(action, itemAction);
                        definition.addSwingAction(itemAction);
                    }
                }
            }
        }
        
        // Process onDrop actions
        if (actionJson.has("onDrop") && actionJson.get("onDrop").isJsonArray()) {
            JsonArray actionsArray = actionJson.getAsJsonArray("onDrop");
            for (JsonElement actionElement : actionsArray) {
                if (actionElement.isJsonObject()) {
                    JsonObject action = actionElement.getAsJsonObject();
                    CommandItemAction itemAction = parseItemAction(action);
                    if (itemAction != null) {
                        // Parse action-specific stages if present
                        parseActionStages(action, itemAction);
                        definition.addDropAction(itemAction);
                    }
                }
            }
        }
        
        // Process onReleaseUsing actions
        if (actionJson.has("onReleaseUsing") && actionJson.get("onReleaseUsing").isJsonArray()) {
            JsonArray actionsArray = actionJson.getAsJsonArray("onReleaseUsing");
            for (JsonElement actionElement : actionsArray) {
                if (actionElement.isJsonObject()) {
                    JsonObject action = actionElement.getAsJsonObject();
                    CommandItemAction itemAction = parseItemAction(action);
                    if (itemAction != null) {
                        // Parse action-specific stages if present
                        parseActionStages(action, itemAction);
                        definition.addReleaseUsingAction(itemAction);
                    }
                }
            }
        }
    }
    
    /**
     * Parse stage conditions for a specific action (used with DEF stages logic)
     */
    private static void parseActionStages(JsonObject actionJson, CommandItemAction action) {
        // Check if this action has stage requirements
        if (actionJson.has("stages") && actionJson.get("stages").isJsonArray()) {
            JsonArray stagesArray = actionJson.getAsJsonArray("stages");
            for (JsonElement stageElement : stagesArray) {
                if (stageElement.isJsonObject()) {
                    JsonObject stageJson = stageElement.getAsJsonObject();
                    String stageType = stageJson.has("stage_type") ? stageJson.get("stage_type").getAsString() : "player";
                    String stage = stageJson.get("stage").getAsString();
                    boolean is = stageJson.has("is") ? stageJson.get("is").getAsBoolean() : true;
                    
                    action.addStage(stageType, stage, is);
                }
            }
        }
    }
    
    /**
     * Parse a single action from a JSON object
     */
    private static CommandItemAction parseItemAction(JsonObject actionJson) {
        try {
            CommandItemAction action = new CommandItemAction();
            
            // Execute command
            if (actionJson.has("execute")) {
                action.setType(CommandItemAction.ActionType.EXECUTE);
                action.setCommand(actionJson.get("execute").getAsString());
            }
            // Delay
            else if (actionJson.has("delay")) {
                action.setType(CommandItemAction.ActionType.DELAY);
                action.setDelay(actionJson.get("delay").getAsInt());
            }
            // Item operation
            else if (actionJson.has("item")) {
                action.setType(CommandItemAction.ActionType.ITEM);
                String itemAction = actionJson.get("item").getAsString().toLowerCase();
                switch (itemAction) {
                    case "delete_all":
                        action.setItemAction(CommandItemAction.ItemActionType.DELETE_ALL);
                        break;
                    case "delete":
                        action.setItemAction(CommandItemAction.ItemActionType.DELETE);
                        break;
                    case "drop_all":
                        action.setItemAction(CommandItemAction.ItemActionType.DROP_ALL);
                        break;
                    case "drop":
                        action.setItemAction(CommandItemAction.ItemActionType.DROP);
                        break;
                    case "consume":
                        action.setItemAction(CommandItemAction.ItemActionType.CONSUME);
                        break;
                    case "damage":
                        action.setItemAction(CommandItemAction.ItemActionType.DAMAGE);
                        break;
                    default:
                        LOGGER.warn("Unknown item action: {}", itemAction);
                        return null;
                }
            }
            // If condition
            else if (actionJson.has("if") && actionJson.get("if").isJsonArray()) {
                action.setType(CommandItemAction.ActionType.IF);
                JsonArray ifArray = actionJson.getAsJsonArray("if");
                
                // The first element contains the conditions
                if (ifArray.size() > 0 && ifArray.get(0).isJsonObject()) {
                    JsonObject conditionsObj = ifArray.get(0).getAsJsonObject();
                    
                    // Parse conditions array which contains indices to stages
                    if (conditionsObj.has("conditions") && conditionsObj.get("conditions").isJsonArray()) {
                        JsonArray conditionsArray = conditionsObj.getAsJsonArray("conditions");
                        List<Integer> indices = new ArrayList<>();
                        
                        for (JsonElement indexElement : conditionsArray) {
                            if (indexElement.isJsonPrimitive()) {
                                indices.add(indexElement.getAsInt());
                            }
                        }
                        
                        action.setConditionIndices(indices);
                    }
                }
                
                // Process sub-actions (all elements after the first one)
                for (int i = 1; i < ifArray.size(); i++) {
                    if (ifArray.get(i).isJsonObject()) {
                        JsonObject subActionJson = ifArray.get(i).getAsJsonObject();
                        CommandItemAction subAction = parseItemAction(subActionJson);
                        if (subAction != null) {
                            action.addSubAction(subAction);
                        }
                    }
                }
                
                if (action.getSubActions().isEmpty()) {
                    LOGGER.warn("No valid sub-actions found in 'if' block");
                    return null;
                }
            }
            // Unknown action
            else {
                LOGGER.warn("Unknown command item action type: {}", actionJson);
                return null;
            }
            
            return action;
        } catch (Exception e) {
            LOGGER.error("Error parsing command item action: {}", e.getMessage());
            return null;
        }
    }
    /**
     * Get a command item definition by ID
     */
    public static CommandItemDefinition getCommandItem(String id) {
        return COMMAND_ITEMS.get(id);
    }
    
    /**
     * Get all command item definitions
     */
    public static Map<String, CommandItemDefinition> getAllCommandItems() {
        return COMMAND_ITEMS;
    }
    
    /**
     * Reload all command item definitions
     */
    public static void reloadAllDefinitions() {
        LOGGER.info("Reloading all command item definitions...");
        var server = ServerLifecycleHooks.getCurrentServer();
        loadAll(server != null ? server.getResourceManager() : null);
    }
} 