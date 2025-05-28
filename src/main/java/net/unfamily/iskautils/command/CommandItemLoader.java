package net.unfamily.iskautils.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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
     * Scans the configuration directory for command item definitions
     */
    public static void scanConfigDirectory() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Scanning configuration directory for command item definitions...");
        }
        
        try {
            // Get the configured path for external scripts
            String externalScriptsBasePath = net.unfamily.iskautils.Config.externalScriptsPath;
            if (externalScriptsBasePath == null || externalScriptsBasePath.trim().isEmpty()) {
                externalScriptsBasePath = "kubejs/external_scripts"; // default path
            }
            
            // Create directory for command items if it doesn't exist
            Path configPath = Paths.get(externalScriptsBasePath, "iska_utils_command_items");
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Created directory for command item definitions: {}", configPath.toAbsolutePath());
                }
                
                // Create a README file to explain the directory
                createReadme(configPath);
                
                // Generate default configurations
                generateDefaultConfigurations(configPath);
                return;
            }
            
            if (!Files.isDirectory(configPath)) {
                LOGGER.warn("The path for command item definitions exists but is not a directory: {}", configPath);
                return;
            }
            
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Scanning directory for command item definitions: {}", configPath.toAbsolutePath());
            }
            
            // Verify and regenerate README if missing
            Path readmePath = configPath.resolve("README.md");
            if (!Files.exists(readmePath)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("README.md missing, generating...");
                }
                createReadme(configPath);
            }
            
            // Clear previous protections and definitions
            PROTECTED_DEFINITIONS.clear();
            COMMAND_ITEMS.clear();
            
            // Check if default_command_items.json exists and check if it's overwritable
            Path defaultItemsFile = configPath.resolve("default_command_items.json");
            if (!Files.exists(defaultItemsFile) || shouldRegenerateDefaultItems(defaultItemsFile)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Generating or regenerating default_command_items.json file");
                }
                generateDefaultCommandItems(configPath);
            }
            
            // Scan all JSON files in the directory
            try (Stream<Path> files = Files.walk(configPath)) {
                files.filter(Files::isRegularFile)
                     .filter(path -> path.toString().endsWith(".json"))
                     .filter(path -> !path.getFileName().toString().startsWith("."))
                     .sorted() // Process in alphabetical order
                     .forEach(CommandItemLoader::scanConfigFile);
            }
            
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Command item definitions scan completed. Loaded {} definitions", COMMAND_ITEMS.size());
            }
            
        } catch (Exception e) {
            LOGGER.error("Error scanning command item definitions directory: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Checks if the default_command_items.json file should be regenerated
     */
    private static boolean shouldRegenerateDefaultItems(Path filePath) {
        try {
            try (InputStream inputStream = Files.newInputStream(filePath);
                 InputStreamReader reader = new InputStreamReader(inputStream)) {
                
                JsonElement jsonElement = GSON.fromJson(reader, JsonElement.class);
                if (jsonElement != null && jsonElement.isJsonObject()) {
                    JsonObject json = jsonElement.getAsJsonObject();
                    
                    // Check if the overwritable field exists and is true
                    if (json.has("overwritable")) {
                        return json.get("overwritable").getAsBoolean();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error reading default_command_items.json file: {}", e.getMessage());
        }
        
        // If the file can't be read or doesn't have the overwritable field, regenerate it
        return true;
    }
    
    /**
     * Creates a README file in the configuration directory
     */
    private static void createReadme(Path configPath) {
        try {
            Path readmePath = configPath.resolve("README.md");
            String readmeContent = "# Iska Utils - Command Items\n" +
                "\n" +
                "This directory allows you to create special command items that can perform actions automatically.\n" +
                "\n" +
                "## Format\n" +
                "\n" +
                "```json\n" +
                "{\n" +
                "  \"type\": \"iska_utils:command_item\",\n" +
                "  \"overwritable\": true,\n" +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"id\": \"iska_utils-world_init\",\n" +
                "      \"creative_tab\": false,\n" +
                "      \"stages_logic\": \"AND\",\n" +
                "      \"stages\": [\n" +
                "        {\"stage_type\": \"world\", \"stage\": \"initialized\", \"is\": false}\n" +
                "      ],\n" +
                "      \"cooldown\": 10,\n" +
                "      \"do\": [\n" +
                "        {\n" +
                "          \"onFirstTick\": [\n" +
                "            {\"execute\": \"kubejs reload server-scripts\"},\n" +
                "            {\"execute\": \"reload\"},\n" +
                "            {\"delay\": 20},\n" +
                "            {\"execute\": \"custommachinery reload\"},\n" +
                "            {\"item\": \"delete_all\"}\n" +
                "          ],\n" +
                "          \"onTick\": [],\n" +
                "          \"onUse\": []\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"iska_utils-per_action_stages\",\n" +
                "      \"creative_tab\": true,\n" +
                "      \"stages_logic\": \"DEF\",\n" +
                "      \"cooldown\": 10,\n" +
                "      \"do\": [\n" +
                "        {\n" +
                "          \"onUse\": [\n" +
                "            {\n" +
                "              \"execute\": \"say This action is available to everyone\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"execute\": \"say This action requires the player to have the 'beginner' stage\",\n" +
                "              \"stages\": [\n" +
                "                {\"stage_type\": \"player\", \"stage\": \"beginner\", \"is\": true}\n" +
                "              ]\n" +
                "            },\n" +
                "            {\n" +
                "              \"execute\": \"say This action requires the player to be in the nether\",\n" +
                "              \"stages\": [\n" +
                "                {\"stage_type\": \"dimension\", \"stage\": \"the_nether\", \"is\": true}\n" +
                "              ]\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "```\n" +
                "\n" +
                "## Fields\n" +
                "\n" +
                "### File Fields\n" +
                "- `type`: Must be **\"iska_utils:command_item\"** [**required**]\n" +
                "- `overwritable`: If true, this file can be overwritten by automatic updates [default: true]\n" +
                "- `items`: Array of command item definitions [**required**]\n" +
                "\n" +
                "### Item Fields\n" +
                "- `id`: Unique identifier for this command item [**required**]\n" +
                "- `creative_tab`: Whether to show this item in the creative tab [default: true]\n" +
                "- `stages_logic`: How to evaluate stages (`AND`, `OR`, or `DEF`) [default: `AND`]\n" +
                "  - `AND`: All stages must be satisfied\n" +
                "  - `OR`: At least one stage must be satisfied\n" +
                "  - `DEF`: Deferred evaluation - stages are defined per action, not at item level\n" +
                "- `stages`: Array of stage conditions that must be met for the item to activate\n" +
                "- `cooldown`: Cooldown in ticks between activation attempts [default: 0]\n" +
                "- `do`: Array of actions to perform when conditions are met [**required**]\n" +
                "\n" +
                "### Action Fields\n" +
                "- `onFirstTick`: Array of actions to perform on the first tick when this item enters inventory\n" +
                "- `onTick`: Array of actions to perform on every tick (subject to cooldown) while in inventory\n" +
                "- `onUse`: Array of actions to perform when the item is right-clicked to use\n" +
                "- `onFinishUse`: Array of actions to perform when the player finishes using the item\n" +
                "- `onUseOn`: Array of actions to perform when the item is used on a block\n" +
                "- `onHitEntity`: Array of actions to perform when the player hits an entity with the item\n" +
                "- `onSwing`: Array of actions to perform when the player swings the item\n" +
                "- `onDrop`: Array of actions to perform when the player drops the item\n" +
                "- `onReleaseUsing`: Array of actions to perform when the player stops using the item before use duration ends\n" +
                "\n" +
                "### Command Actions\n" +
                "- `execute`: Execute a command\n" +
                "- `delay`: Wait specified ticks before next action\n" +
                "- `item`: What to do with the item (`delete_all`, `delete`, `drop_all`, `drop`, `consume`, `damage`)\n" +
                "- `stages`: Array of stage conditions that must be met for this specific action (used with DEF logic)\n" +
                "\n" +
                "## Examples\n" +
                "\n" +
                "### World Initialization Item\n" +
                "A world initialization item that runs reload commands when first obtained and then removes itself:\n" +
                "```json\n" +
                "{\n" +
                "  \"type\": \"iska_utils:command_item\",\n" +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"id\": \"iska_utils-world_init\",\n" +
                "      \"creative_tab\": false,\n" +
                "      \"stages\": [\n" +
                "        {\"stage_type\": \"world\", \"stage\": \"initialized\", \"is\": false}\n" +
                "      ],\n" +
                "      \"do\": [\n" +
                "        {\n" +
                "          \"onFirstTick\": [\n" +
                "            {\"execute\": \"kubejs reload server-scripts\"},\n" +
                "            {\"execute\": \"reload\"},\n" +
                "            {\"delay\": 20},\n" +
                "            {\"execute\": \"custommachinery reload\"},\n" +
                "            {\"item\": \"delete_all\"}\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "```\n" +
                "\n" +
                "### Per-Action Stage Requirements\n" +
                "An item that demonstrates using different stage requirements for each action:\n" +
                "```json\n" +
                "{\n" +
                "  \"type\": \"iska_utils:command_item\",\n" +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"id\": \"iska_utils-progression_test\",\n" +
                "      \"creative_tab\": true,\n" +
                "      \"stages_logic\": \"DEF\",\n" +
                "      \"do\": [\n" +
                "        {\n" +
                "          \"onUse\": [\n" +
                "            {\n" +
                "              \"execute\": \"say This action is available to everyone\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"execute\": \"say This action requires the player to have unlocked the beginning stage\",\n" +
                "              \"stages\": [\n" +
                "                {\"stage_type\": \"player\", \"stage\": \"beginning\", \"is\": true}\n" +
                "              ]\n" +
                "            },\n" +
                "            {\n" +
                "              \"execute\": \"say This action requires the player to be in the nether\",\n" +
                "              \"stages\": [\n" +
                "                {\"stage_type\": \"dimension\", \"stage\": \"nether\", \"is\": true}\n" +
                "              ]\n" +
                "            },\n" +
                "            {\n" +
                "              \"execute\": \"say This action requires the player to NOT have the locked_stage\",\n" +
                "              \"stages\": [\n" +
                "                {\"stage_type\": \"player\", \"stage\": \"locked_stage\", \"is\": false}\n" +
                "              ]\n" +
                "            },\n" +
                "            {\n" +
                "              \"execute\": \"say This action requires multiple conditions\",\n" +
                "              \"stages\": [\n" +
                "                {\"stage_type\": \"player\", \"stage\": \"advanced\", \"is\": true},\n" +
                "                {\"stage_type\": \"player\", \"stage\": \"locked_stage\", \"is\": false},\n" +
                "                {\"stage_type\": \"world\", \"stage\": \"event_active\", \"is\": true}\n" +
                "              ]\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "```\n";
            
            Files.writeString(readmePath, readmeContent);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Created README.md file in command items directory");
            }
        } catch (IOException e) {
            LOGGER.error("Error creating README.md file: {}", e.getMessage());
        }
    }
    
    /**
     * Scans a single config file for command item definitions
     */
    private static void scanConfigFile(Path configFile) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Scanning config file: {}", configFile);
        }
        
        String definitionId = configFile.getFileName().toString().replace(".json", "");
        
        try (InputStream inputStream = Files.newInputStream(configFile)) {
            parseConfigFromStream(definitionId, configFile.toString(), inputStream);
        } catch (Exception e) {
            LOGGER.error("Error reading command item definition file {}: {}", configFile, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Parses config from an input stream
     */
    private static void parseConfigFromStream(String definitionId, String filePath, InputStream inputStream) {
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            JsonElement jsonElement = GSON.fromJson(reader, JsonElement.class);
            if (jsonElement != null && jsonElement.isJsonObject()) {
                JsonObject json = jsonElement.getAsJsonObject();
                parseConfigJson(definitionId, filePath, json);
            } else {
                LOGGER.error("Invalid JSON in command item definition file: {}", filePath);
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing command item definition file {}: {}", filePath, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
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
            
            // Parse stages logic
            if (itemJson.has("stages_logic")) {
                String logic = itemJson.get("stages_logic").getAsString().toUpperCase();
                switch (logic) {
                    case "OR":
                        definition.setStagesLogic(CommandItemDefinition.StagesLogic.OR);
                        break;
                    case "DEF":
                        definition.setStagesLogic(CommandItemDefinition.StagesLogic.DEF);
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
        // Process onFirstTick actions
        if (actionJson.has("onFirstTick") && actionJson.get("onFirstTick").isJsonArray()) {
            JsonArray actionsArray = actionJson.getAsJsonArray("onFirstTick");
            for (JsonElement actionElement : actionsArray) {
                if (actionElement.isJsonObject()) {
                    JsonObject action = actionElement.getAsJsonObject();
                    CommandItemAction itemAction = parseItemAction(action);
                    if (itemAction != null) {
                        // Parse action-specific stages if present
                        parseActionStages(action, itemAction);
                        definition.addFirstTickAction(itemAction);
                    }
                }
            }
        }
        
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
     * Generate default configurations
     */
    private static void generateDefaultConfigurations(Path configPath) {
        try {
            generateDefaultCommandItems(configPath);
        } catch (Exception e) {
            LOGGER.error("Error generating default command item configurations: {}", e.getMessage());
        }
    }
    
    /**
     * Generate default command item definition file
     */
    private static void generateDefaultCommandItems(Path configPath) throws IOException {
        Path defaultItemsPath = configPath.resolve("default_command_items.json");
        
        // Default items definition based on the example
        String defaultItemsJson = "{\n" +
            "  \"type\": \"iska_utils:command_item\",\n" +
            "  \"overwritable\": true,\n" +
            "  \"items\": [\n" +
            "    {\n" +
            "      \"id\": \"iska_utils-world_init\",\n" +
            "      \"creative_tab\": false,\n" +
            "      \"stages_logic\": \"AND\",\n" +
            "      \"stages\": [\n" +
            "        {\"stage_type\": \"world\", \"stage\": \"initialized\", \"is\": false}\n" +
            "      ],\n" +
            "      \"cooldown\": 10,\n" +
            "      \"do\": [\n" +
            "        {\n" +
            "          \"onFirstTick\": [\n" +
            "            {\"execute\": \"iska_utils_stage add world initialized\"},\n" +
            "            {\"execute\": \"kubejs reload server-scripts\"},\n" +
            "            {\"execute\": \"reload\"},\n" +
            "            {\"delay\": 20},\n" +
            "            {\"execute\": \"custommachinery reload\"},\n" +
            "            {\"item\": \"delete_all\"}\n" +
            "          ],\n" +
            "          \"onTick\": [\n" +
            "            {\"item\": \"delete_all\"}\n" +
            "          ],\n" +
            "          \"onUse\": []\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";
        
        Files.writeString(defaultItemsPath, defaultItemsJson);
        LOGGER.info("Generated default command items definition file");
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
        scanConfigDirectory();
    }
} 