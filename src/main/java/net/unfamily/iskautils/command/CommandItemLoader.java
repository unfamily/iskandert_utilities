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
import net.unfamily.iskautils.script.LoadActionParser;
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
                ? IskaUtilsLoadJson.collectMergedJsonForSubdir(resourceManagerOrNull, IskaUtilsLoadPaths.COMMAND_ITEMS)
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
        LoadActionParser.parseActionStages(actionJson, action);
    }

    private static CommandItemAction parseItemAction(JsonObject actionJson) {
        return LoadActionParser.parseItemAction(actionJson, "command_item_action");
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