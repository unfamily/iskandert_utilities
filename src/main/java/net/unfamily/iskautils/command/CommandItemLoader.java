package net.unfamily.iskautils.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.unfamily.iskautils.data.load.IskaUtilsLoadJson;
import net.unfamily.iskautils.data.load.IskaUtilsLoadPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads command item definitions from datapack JSON under {@code data/<namespace>/load/iska_utils_command_items/}.
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
        Map<ResourceLocation, JsonElement> merged = resourceManagerOrNull != null
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

    /** @deprecated kept for compatibility; uses built-in defaults only. */
    public static void scanConfigDirectory() {
        loadAll(null);
    }
    
    /**
     * Registers internal default command item definitions.
     * These are always available as fallback; external scripts can override them.
     */
    private static void registerInternalDefaults() {
        // Default: iska_utils-world_init
        CommandItemDefinition worldInit = new CommandItemDefinition("iska_utils-world_init");
        worldInit.setCreativeTabVisible(false);
        worldInit.setMaxStackSize(1);
        worldInit.setGlowing(false);
        worldInit.setStagesLogic(CommandItemDefinition.StagesLogic.DEF_AND);
        worldInit.setCooldown(10);
        worldInit.addStage("world", "initializing", false);  // index 0
        worldInit.addStage("world", "initialized", false);    // index 1
        worldInit.addStage("world", "initialized", true);     // index 2
        
        // onTick: if conditions [0,1] -> initialization sequence
        CommandItemAction ifAction1 = new CommandItemAction();
        ifAction1.setType(CommandItemAction.ActionType.IF);
        List<Integer> cond1 = new ArrayList<>();
        cond1.add(0);
        cond1.add(1);
        ifAction1.setConditionIndices(cond1);
        
        String[] initCommands = {
            "iska_utils_stage add world initializing true",
            "title @a times 20 100 40",
            "title @a subtitle {\"text\":\"please stand still and do nothing.\",\"color\":\"dark_red\"}",
            "title @a title {\"text\":\"World Initialization:\",\"color\":\"dark_red\"}"
        };
        for (String cmd : initCommands) {
            CommandItemAction exec = new CommandItemAction();
            exec.setType(CommandItemAction.ActionType.EXECUTE);
            exec.setCommand(cmd);
            ifAction1.addSubAction(exec);
        }
        
        CommandItemAction delay1 = new CommandItemAction();
        delay1.setType(CommandItemAction.ActionType.DELAY);
        delay1.setDelay(160);
        ifAction1.addSubAction(delay1);
        
        String[] reloadCommands = {
            "kubejs reload server-scripts",
            "reload"
        };
        for (String cmd : reloadCommands) {
            CommandItemAction exec = new CommandItemAction();
            exec.setType(CommandItemAction.ActionType.EXECUTE);
            exec.setCommand(cmd);
            ifAction1.addSubAction(exec);
        }
        
        CommandItemAction delay2 = new CommandItemAction();
        delay2.setType(CommandItemAction.ActionType.DELAY);
        delay2.setDelay(20);
        ifAction1.addSubAction(delay2);
        
        String[] finishCommands = {
            "custommachinery reload",
            "title @a times 20 100 40",
            "title @a subtitle {\"text\":\"completed, apologies for the wait\",\"color\":\"dark_green\"}",
            "title @a title {\"text\":\"World Initialization:\",\"color\":\"dark_green\"}",
            "title @a times 20 100 20",
            "iska_utils_stage add world initialized true",
            "iska_utils_stage remove world initializing true"
        };
        for (String cmd : finishCommands) {
            CommandItemAction exec = new CommandItemAction();
            exec.setType(CommandItemAction.ActionType.EXECUTE);
            exec.setCommand(cmd);
            ifAction1.addSubAction(exec);
        }
        
        CommandItemAction deleteAll1 = new CommandItemAction();
        deleteAll1.setType(CommandItemAction.ActionType.ITEM);
        deleteAll1.setItemAction(CommandItemAction.ItemActionType.DELETE_ALL);
        ifAction1.addSubAction(deleteAll1);
        
        worldInit.addTickAction(ifAction1);
        
        // onTick: if conditions [0,2] -> already initialized, just delete
        CommandItemAction ifAction2 = new CommandItemAction();
        ifAction2.setType(CommandItemAction.ActionType.IF);
        List<Integer> cond2 = new ArrayList<>();
        cond2.add(0);
        cond2.add(2);
        ifAction2.setConditionIndices(cond2);
        
        CommandItemAction deleteAll2 = new CommandItemAction();
        deleteAll2.setType(CommandItemAction.ActionType.ITEM);
        deleteAll2.setItemAction(CommandItemAction.ItemActionType.DELETE_ALL);
        ifAction2.addSubAction(deleteAll2);
        
        worldInit.addTickAction(ifAction2);
        
        COMMAND_ITEMS.put("iska_utils-world_init", worldInit);
        LOGGER.debug("Registered internal default command item: iska_utils-world_init");
    }
    
    
    /**
     * Creates a README file in the configuration directory
     */
    private static void createReadme(Path configPath) {
        try {
            Path readmePath = configPath.resolve("README.md");
            
            String readmeContent = "# Command Items - External Configuration\n\n" +
                "This directory contains configuration files for command items that perform automated actions.\n\n" +
                 "## Format\n\n" +
                "The format is JSON with the following structure:\n\n" +
                "```json\n" +
                "{\n" +
                "  \"id\": \"unique-item-id\",\n" +
                "  \"creative_tab\": true,\n" +
                "  \"stack_size\": 64,\n" +
                "  \"is_foil\": false,\n" +
                "  \"cooldown\": 20,\n" +
                "  \"stages_logic\": \"AND\",\n" +
                "  \"stages\": [\n" +
                "    {\n" +
                "      \"stage_type\": \"player\",\n" +
                "      \"stage\": \"some_stage\",\n" +
                "      \"is\": true\n" +
                "    }\n" +
                "  ],\n" +
                "  \"do\": [\n" +
                "    {\n" +
                "      \"onUse\": [\n" +
                "        {\"execute\": \"say Hello World!\"}\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"onTick\": [\n" +
                "        {\"delay\": 20},\n" +
                "        {\"execute\": \"say Tick!\"}\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "```\n\n" +
                "## Fields\n\n" +
                "### General Fields\n" +
                "- `id`: Unique identifier for the command item (required)\n" +
                "- `creative_tab`: Whether the item should appear in the creative tab (optional, default: true)\n" +
                "- `stack_size`: Maximum stack size for this item (optional, default: 64, range: 1-64)\n" +
                "- `is_foil`: Whether the item should have an enchantment glint (optional, default: false)\n" +
                "- `cooldown`: Cooldown in ticks (20 ticks = 1 second) between actions (optional, default: 0)\n" +
                "- `stages_logic`: Logic for evaluating stages (optional, values: \"AND\", \"OR\", \"DEF_AND\", \"DEF_OR\", default: \"AND\")\n" +
                "  - \"AND\": All stages must be satisfied\n" +
                "  - \"OR\": At least one stage must be satisfied\n" +
                "  - \"DEF_AND\": Stages are defined per action with AND logic\n" +
                "  - \"DEF_OR\": Stages are defined per action with OR logic\n" +
                "- `stages`: List of stage conditions that must be met for the item to function\n\n" +
                
                "### Special Stage System for DEF Logic\n" +
                "When using `stages_logic: \"DEF_AND\"` or `stages_logic: \"DEF_OR\"`, you can create a special initialization system:\n\n" +
                "1. Include a stage called `initialized` in your item definition:\n" +
                "```json\n" +
                "\"stages\": [\n" +
                "  {\n" +
                "    \"stage_type\": \"player\",\n" +
                "    \"stage\": \"initialized\",\n" +
                "    \"is\": true\n" +
                "  }\n" +
                "]\n" +
                "```\n\n" +
                
                "2. In your tick actions, set this stage:\n" +
                "```json\n" +
                "{\n" +
                "  \"onTick\": [\n" +
                "    {\"execute\": \"iska_utils_stage add player initialized true\"}\n" +
                "  ]\n" +
                "}\n" +
                "```\n\n" +
                
                "3. The system will automatically check if the item has been initialized.\n" +
                "   If not, it will be removed from inventory after the first tick.\n" +
                "   This is useful for one-time use items that should be removed if their\n" +
                "   initialization fails for any reason.\n\n" +
                
                "### Item Properties\n" +
                "- `stack_size`: The number of items in the stack (optional, default: 64)\n" +
                "- `is_foil`: Whether the item is a foil (optional, default: false)\n" +
                "- `cooldown`: The cooldown in ticks (20 ticks = 1 second) between actions (optional, default: 0)\n" +
                "\n" +

                "### Stage Condition Fields\n" +
                "- `stage_type`: Type of stage, e.g., \"player\", \"world\", or \"team\" (optional, default: \"player\")\n" +
                "- `stage`: Name of the stage (required)\n" +
                "- `is`: Whether the stage should be set (true) or not set (false) (optional, default: true)\n\n" +
                
                "### Action Blocks\n" +
                "- `onTick`: Actions to perform every tick\n" +
                "- `onUse`: Actions to perform when the item is used\n" +
                "- `onFinishUse`: Actions to perform when the item use is finished\n" +
                "- `onUseOn`: Actions to perform when the item is used on a block\n" +
                "- `onHitEntity`: Actions to perform when the item hits an entity\n" +
                "- `onSwing`: Actions to perform when the item is swung\n" +
                "- `onDrop`: Actions to perform when the item is dropped\n" +
                "- `onReleaseUsing`: Actions to perform when the use is released\n\n" +
                
                "### Action Types\n" +
                "- `execute`: Execute a command (e.g., `{\"execute\": \"say Hello World!\"}`)\n" +
                "- `delay`: Delay next actions by ticks (e.g., `{\"delay\": 20}`)\n" +
                "- `item`: Perform an item action (e.g., `{\"item\": \"consume\"}`)\n" +
                "- `if`: Conditional execution based on stage conditions (see example below)\n\n" +
                
                "### Item Action Values\n" +
                "- `consume`: Consume one item from the stack\n" +
                "- `delete`: Delete the item in hand\n" +
                "- `delete_all`: Delete all items of this type in inventory\n" +
                "- `drop`: Drop the item in hand\n" +
                "- `drop_all`: Drop all items of this type in inventory\n" +
                "- `damage`: Damage the item\n\n" +
                
                "## Important Notes\n\n" +
                "- **Final Actions**: The actions like `drop`, `drop_all`, `delete` and `delete_all` must always be the last in a command block to ensure the correct execution of subsequent commands.\n\n" +
                "- **Logical Spinlocks**: Stages can be used to create logical spinlocks, as demonstrated in the World Initializer example.\n\n" +
               
                "## Example: World Initialization Item\n\n" +
                "```json\n" +
                "{\n" +
                "  \"id\": \"world_init\",\n" +
                "  \"creative_tab\": true,\n" +
                "  \"stack_size\": 1,\n" +
                "  \"stages_logic\": \"DEF_AND\",\n" +
                "  \"stages\": [\n" +
                "    {\n" +
                "      \"stage_type\": \"world\",\n" +
                "      \"stage\": \"initialized\",\n" +
                "      \"is\": true\n" +
                "    }\n" +
                "  ],\n" +
                "  \"do\": [\n" +
                "    {\n" +
                "      \"onTick\": [\n" +
                "        {\"execute\": \"iska_utils_stage add world initialized true\"},\n" +
                "        {\"execute\": \"say World initialized!\"},\n" +
                "        {\"item\": \"consume\"}\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "```\n\n" +
                
                "## Conditional Example with IF\n\n" +
                "```json\n" +
                "{\n" +
                "  \"onTick\": [\n" +
                "    {\"if\": [\n" +
                "        {\"conditions\":[0,1]},\n" +
                "        {\"execute\": \"say Condition met!\"},\n" +
                "        {\"item\": \"delete_all\"}\n" +
                "    ]}\n" +
                "  ]\n" +
                "}\n" +
                "```\n\n" +
                
                "## Notes\n\n" +
                "- Command items are loaded during game startup from JSON files in this directory.\n" +
                "- Default items are registered internally and always available even without configuration files.\n" +
                "- To see the internal defaults as JSON files, run: `/iska_utils_debug dump_default`\n" +
                "- Configuration files in this directory override the internal defaults.\n" +
                "- Reload: `/iska_utils_debug reload` (quick) or `/reload` (full) to apply changes without restart.\n" +
                "- You can create as many command item configurations as needed.\n";
            
            Files.write(readmePath, readmeContent.getBytes());
            LOGGER.info("Created README.md file at {}", readmePath);
            
        } catch (IOException e) {
            LOGGER.error("Failed to create README.md file: {}", e.getMessage());
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
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        var rm = server != null ? server.getResourceManager() : null;
        loadAll(rm);
    }
} 