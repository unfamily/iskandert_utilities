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
 * Loads command macros from external JSON files
 */
public class MacroLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    // Stores files with overwritable=false to prevent them from being overwritten
    private static final Map<String, Boolean> PROTECTED_MACROS = new HashMap<>();
    
    /**
     * Scans the configuration directory for command macros
     */
    public static void scanConfigDirectory() {
        LOGGER.info("Scanning configuration directory for command macros...");
        
        try {
            // Get the configured path for external scripts
            String externalScriptsBasePath = net.unfamily.iskautils.Config.externalScriptsPath;
            if (externalScriptsBasePath == null || externalScriptsBasePath.trim().isEmpty()) {
                externalScriptsBasePath = "kubejs/external_scripts"; // default path
            }
            
            // Create directory for command macros if it doesn't exist
            Path configPath = Paths.get(externalScriptsBasePath, "iska_utils_macros");
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath);
                LOGGER.info("Created directory for command macros: {}", configPath.toAbsolutePath());
                
                // Create a README file to explain the directory
                createReadme(configPath);
                
                // Generate default configurations
                generateDefaultConfigurations(configPath);
                return;
            }
            
            if (!Files.isDirectory(configPath)) {
                LOGGER.warn("The path for macros exists but is not a directory: {}", configPath);
                return;
            }
            
            LOGGER.info("Scanning directory for macros: {}", configPath.toAbsolutePath());
            
            // Verify and regenerate README if missing
            Path readmePath = configPath.resolve("README.md");
            if (!Files.exists(readmePath)) {
                LOGGER.info("README.md missing, generating...");
                createReadme(configPath);
            }
            
            // Clear previous protections
            PROTECTED_MACROS.clear();
            
            // Check if default_commands_macro.json exists and check if it's overwritable
            Path defaultCommandsFile = configPath.resolve("default_commands_macro.json");
            if (!Files.exists(defaultCommandsFile) || shouldRegenerateDefaultCommandsMacro(defaultCommandsFile)) {
                LOGGER.info("Generating or regenerating default_commands_macro.json file");
                generateDefaultCommandsMacro(configPath);
            }
            
            // Scan all JSON files in the directory
            try (Stream<Path> files = Files.walk(configPath)) {
                files.filter(Files::isRegularFile)
                     .filter(path -> path.toString().endsWith(".json"))
                     .filter(path -> !path.getFileName().toString().startsWith("."))
                     .sorted() // Process in alphabetical order
                     .forEach(MacroLoader::scanConfigFile);
            }
            
            LOGGER.info("Macro directory scan completed");
            
        } catch (Exception e) {
            LOGGER.error("Error scanning macro directory: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Checks if the default_commands_macro.json file should be regenerated
     */
    private static boolean shouldRegenerateDefaultCommandsMacro(Path filePath) {
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
            LOGGER.warn("Error reading default_commands_macro.json file: {}", e.getMessage());
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
            String readmeContent = "# Iska Utils - Command Macros\n" +
                "\n" +
                "This directory allows you to create command macros that can be executed directly in the game.\n" +
                "\n" +
                "## Format\n" +
                "\n" +
                "```json\n" +
                "{\n" +
                "  \"type\": \"iska_utils:commands_macro\",\n" +
                "  \"overwritable\": true,\n" +
                "  \"commands\": [\n" +
                "    {\n" +
                "      \"command\": \"reloader\",\n" +
                "      \"level\": 2,\n" +
                "      \"do\": [\n" +
                "        {\"execute\": \"kubejs reload server-scripts\"},\n" +
                "        {\"execute\": \"reload\"},\n" +
                "        {\"delay\": 60},\n" +
                "        {\"execute\": \"custommachinery reload\"}\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"command\": \"echo\",\n" +
                "      \"level\": 0,\n" +
                "      \"parameters\": [\n" +
                "        {\n" +
                "          \"type\": \"string\",\n" +
                "          \"required\": true\n" +
                "        }\n" +
                "      ],\n" +
                "      \"do\": [\n" +
                "        {\"execute\": \"say #0\"}\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"command\": \"spawnmob\",\n" +
                "      \"level\": 0,\n" +
                "      \"stages_logic\": \"OR\",\n" +
                "      \"stages\": [\n" +
                "        {\"stage\": \"example_stage_0\", \"stage_type\": \"player\", \"is\": true},\n" +
                "        {\"stage\": \"nether\", \"stage_type\": \"dimension\", \"is\": true},\n" +
                "        {\"stage\": \"locked_stage\", \"stage_type\": \"player\", \"is\": false}\n" +
                "      ],\n" +
                "      \"parameters\": [\n" +
                "        {\n" +
                "          \"type\": \"static\",\n" +
                "          \"list\": [\n" +
                "            {\"declare\": \"zombie\"},\n" +
                "            {\"declare\": \"skeleton\"},\n" +
                "            {\"declare\": \"creeper\"}\n" +
                "          ]\n" +
                "        }\n" +
                "      ],\n" +
                "      \"do\": [\n" +
                "        {\"execute\": \"execute at @s run summon minecraft:#0 ~ ~ ~\"}\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "```\n" +
                "\n" +
                "## Fields\n" +
                "\n" +
                "### File Fields\n" +
                "- `type`: Must be **\"iska_utils:commands_macro\"** [**required**]\n" +
                "- `overwritable`: Whether the macros in this file can be overwritten by other files loaded later [optional, default: true]\n" +
                "- `commands`: Array of command macro definitions [**required**]\n" +
                "\n" +
                "### Macro Fields\n" +
                "- `command`: Unique identifier for the macro, which becomes the command name (e.g., `/reloader`) [**required**]\n" +
                "- `level`: Required permission level (0-4) [optional, default: 0]\n" +
                "  - 0: Any player\n" +
                "  - 1-4: OP level required\n" +
                "- `stages_logic`: Logic for stage requirements evaluation: \"AND\" (all required) or \"OR\" (any one required) [optional, default: AND]\n" +
                "  - `AND`: All stages must be satisfied\n" +
                "  - `OR`: At least one stage must be satisfied\n" +
                "  - `DEF`: Deferred evaluation - stages are defined per action, not at macro level\n" +
                "- `stages`: Array of game stages that must be unlocked to use this macro [optional]\n" +
                "  - `{\"stage\": \"stage_id\", \"stage_type\": \"player\", \"is\": true}`: ID of a required game stage and its type\n" +
                "    - `stage_type` can be: \"player\" (default), \"world\", or \"dimension\"\n" +
                "    - `is`: Whether the stage must be present (`true`, default) or absent (`false`)\n" +
                "- `parameters`: Array of parameter definitions for the command [optional]\n" +
                "  - `type`: Type of parameter (`string`, `word`, `int`, `float`, `double`, `boolean`, `target`, `static`) [**required**]\n" +
                "  - `required`: Whether the parameter is required (true) or optional (false) [optional, default: true]\n" +
                "  - `list`: For static type only, array of allowed values for autocomplete [**required for static type**]\n" +
                "    - `{\"declare\": \"value\"}`: Defines a static value for the parameter\n" +
                "- `do`: Array of actions to execute in sequence [**required**]\n" +
                "  - `{\"execute\": \"command\"}`: Execute a server command\n" +
                "  - `{\"delay\": ticks}`: Wait for specified ticks (20 ticks = 1 second)\n" +
                "\n" +
                "### Parameter Types\n" +
                "- `string`: Any text (greedily captures all remaining text)\n" +
                "- `word`: A single word without spaces\n" +
                "- `int`: An integer number\n" +
                "- `float`/`double`: A decimal number\n" +
                "- `boolean`: true or false\n" +
                "- `target`: Player selector (e.g., @p, @a, or player name)\n" +
                "- `static`: Value from a predefined list with autocomplete (like vanilla command arguments)\n" +
                "\n" +
                "### Parameter Usage in Commands\n" +
                "Parameters can be used in commands using `#index` notation where index is the parameter's position (0-based).\n" +
                "For example, `#0` refers to the first parameter, `#1` to the second, etc.\n" +
                "\n" +
                "### Game Stages\n" +
                "The game stages system allows you to lock macros behind progression milestones. There are three types of stages:\n" +
                "\n" +
                "- **Player Stages**: Related to individual player progression (default type)\n" +
                "- **World Stages**: Related to global world state or events\n" +
                "- **Dimension Stages**: Related to specific dimensions (like Nether or End)\n" +
                "\n" +
                "You can specify the stage type using the `stage_type` field in each stage requirement.\n" +
                "\n" +
                "Additionally, you can use the `is` field to create negative requirements:\n" +
                "- `\"is\": true` (default): The player/world/dimension MUST have the specified stage\n" +
                "- `\"is\": false`: The player/world/dimension MUST NOT have the specified stage\n" +
                "\n" +
                "This allows you to create conditions like \"player must have stage A but must not have stage B\".\n" +
                "\n" +
                "The system will be fully implemented in future updates. When implemented, players will need to satisfy\n" +
                "all the stage requirements to use stage-restricted macros.\n" +
                "\n" +
                "## Notes\n" +
                "\n" +
                "- Commands are executed in the context of the player who triggered the macro\n" +
                "- Changes require a game restart to apply\n" +
                "- For security reasons, macros are limited to players with the appropriate permission level\n" +
                "- The `usage` field provides better command documentation and error messages\n" +
                "- When macros with the same command name are found in multiple files:\n" +
                "  - If a file has `overwritable: false`, its macros cannot be overwritten\n" +
                "  - If a file has `overwritable: true` or no overwritable field, its macros can be overwritten by later files\n" +
                "  - Files are processed in alphabetical order\n" +
                "\n" +
                "## Default Commands Macro\n" +
                "\n" +
                "The mod automatically generates a file called `default_commands_macro.json` with some example macros.\n" +
                "- This file has `overwritable: true` by default, which means it will be regenerated on each start\n" +
                "- If you want to keep your changes to this file, set `overwritable: false`\n" +
                "- Best practice: Instead of editing the default file, create your own files with custom macros\n" +
                "\n" +
                "## Example Use Cases\n" +
                "\n" +
                "### Reload Scripts and Configurations\n" +
                "A macro that reloads various mod systems with appropriate delays between commands.\n" +
                "\n" +
                "### Parametrized Commands\n" +
                "Commands like `/echo Hello World` or `/msg player1 Hello there` that take parameters.\n" +
                "\n" +
                "### Static Parameter Commands\n" +
                "Custom commands with tab-completion like `/spawnmob zombie` or `/setbiome desert` that provide convenient shortcuts with predefined options.\n" +
                "\n" +
                "### Progression-Based Commands\n" +
                "Commands that are only available once players have reached certain milestones or completed specific quests.\n" +
                "\n" +
                "### Per-Action Stage Requirements\n" +
                "Example using the `DEF` stages logic for per-action stage requirements:\n" +
                "\n" +
                "```json\n" +
                "{\n" +
                "  \"command\": \"progression_test\",\n" +
                "  \"level\": 0,\n" +
                "  \"stages_logic\": \"DEF\",\n" +
                "  \"do\": [\n" +
                "    {\n" +
                "      \"execute\": \"say This action is available to everyone\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"execute\": \"say This action requires the player to have unlocked the beginning stage\",\n" +
                "      \"stages\": [\n" +
                "        {\"stage\": \"beginning\", \"stage_type\": \"player\", \"is\": true}\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"execute\": \"say This action requires the player to be in the nether\",\n" +
                "      \"stages\": [\n" +
                "        {\"stage\": \"nether\", \"stage_type\": \"dimension\", \"is\": true}\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"execute\": \"say This action requires the player to NOT have the locked_stage\",\n" +
                "      \"stages\": [\n" +
                "        {\"stage\": \"locked_stage\", \"stage_type\": \"player\", \"is\": false}\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"execute\": \"say This action requires multiple conditions\",\n" +
                "      \"stages\": [\n" +
                "        {\"stage\": \"advanced\", \"stage_type\": \"player\", \"is\": true},\n" +
                "        {\"stage\": \"locked_stage\", \"stage_type\": \"player\", \"is\": false},\n" +
                "        {\"stage\": \"event_active\", \"stage_type\": \"world\", \"is\": true}\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "```\n" +
                "\n" +
                "In this example, each action has its own stage requirements. If a player doesn't meet the requirements for a specific action, \n" +
                "that action is skipped, but the rest of the actions will still be evaluated and potentially executed.\n" +
                "\n" +
                "### Time Cycle Demonstration\n" +
                "A macro that cycles through different times of day with delays between changes.\n" +
                "\n" +
                "### Weather Control\n" +
                "A macro that changes weather conditions with appropriate timing.\n";
            
            Files.writeString(readmePath, readmeContent);
            LOGGER.info("Created README file: {}", readmePath);
            
        } catch (Exception e) {
            LOGGER.warn("Unable to create README file: {}", e.getMessage());
        }
    }
    
    /**
     * Scans a single configuration file
     */
    private static void scanConfigFile(Path configFile) {
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Scanning configuration file: {}", configFile);
            }
            String macroId = configFile.getFileName().toString();
            
            try (InputStream inputStream = Files.newInputStream(configFile)) {
                parseConfigFromStream(macroId, configFile.toString(), inputStream);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error scanning file {}: {}", configFile, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Parses configuration from an input stream
     */
    private static void parseConfigFromStream(String macroId, String filePath, InputStream inputStream) {
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            JsonElement jsonElement = GSON.fromJson(reader, JsonElement.class);
            if (jsonElement != null && jsonElement.isJsonObject()) {
                parseConfigJson(macroId, filePath, jsonElement.getAsJsonObject());
            } else {
                LOGGER.error("File {} does not contain a valid JSON object", filePath);
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing file {}: {}", filePath, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Parses a configuration JSON object
     */
    private static void parseConfigJson(String macroId, String filePath, JsonObject json) {
        try {
            // Check configuration type
            if (!json.has("type")) {
                LOGGER.warn("File {} has no 'type' field, ignored", filePath);
                return;
            }
            
            String type = json.get("type").getAsString();
            
            // Verify that the type is correct
            if (!"iska_utils:commands_macro".equals(type)) {
                LOGGER.warn("Unsupported type '{}' in file {}. Must be 'iska_utils:commands_macro'", type, filePath);
                return;
            }
            
            // Check overwritable flag
            boolean overwritable = true;
            if (json.has("overwritable")) {
                overwritable = json.get("overwritable").getAsBoolean();
            }
            
            // Process the commands array
            if (json.has("commands") && json.get("commands").isJsonArray()) {
                JsonArray macrosArray = json.get("commands").getAsJsonArray();
                LOGGER.info("Found {} macros in file {}", macrosArray.size(), filePath);
                
                for (int i = 0; i < macrosArray.size(); i++) {
                    JsonElement macroElem = macrosArray.get(i);
                    if (macroElem.isJsonObject()) {
                        JsonObject macroObj = macroElem.getAsJsonObject();
                        processMacroJson(macroObj, overwritable);
                    } else {
                        LOGGER.warn("Element {} in 'commands' array is not a valid JSON object in {}", i, filePath);
                    }
                }
            } else {
                LOGGER.warn("Commands macro file {} does not have a valid 'commands' array", filePath);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error parsing file {}: {}", filePath, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Processes a single macro JSON object
     */
    private static void processMacroJson(JsonObject json, boolean overwritable) {
        try {
            // Check required fields
            if (!json.has("command") || !json.has("do")) {
                LOGGER.warn("Invalid macro: missing required fields 'command' or 'do'");
                return;
            }
            
            String macroId = json.get("command").getAsString();
            
            // Check if this macro is protected (defined in a non-overwritable file)
            if (PROTECTED_MACROS.containsKey(macroId) && PROTECTED_MACROS.get(macroId)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Macro '{}' is protected, ignoring new definition", macroId);
                }
                return;
            }
            
            // Store protection status
            PROTECTED_MACROS.put(macroId, !overwritable);
            
            // Create macro definition from JSON
            MacroCommand.MacroDefinition macro = MacroCommand.MacroDefinition.fromJson(json);
            
            // Register the macro
            MacroCommand.registerMacro(macro.getId(), macro);
            
        } catch (Exception e) {
            LOGGER.error("Error processing macro: {}", e.getMessage());
        }
    }
    
    /**
     * Generates default macro configurations
     */
    private static void generateDefaultConfigurations(Path configPath) {
        try {
            // Create the default_commands_macro.json file
            generateDefaultCommandsMacro(configPath);
        } catch (Exception e) {
            LOGGER.error("Error generating default configurations: {}", e.getMessage());
        }
    }
    
    /**
     * Generates the default_commands_macro.json file
     */
    private static void generateDefaultCommandsMacro(Path configPath) throws IOException {
        // Simplify and include only the reloader command
        String content = "{\n" +
            "  \"type\": \"iska_utils:commands_macro\",\n" +
            "  \"overwritable\": true,\n" +
            "  \"commands\": [\n" +
            "    {\n" +
            "      \"command\": \"reloader\",\n" +
            "      \"level\": 2,\n" +
            "      \"do\": [\n" +
            "        {\"execute\": \"kubejs reload server-scripts\"},\n" +
            "        {\"execute\": \"reload\"},\n" +
            "        {\"delay\": 20},\n" +
            "        {\"execute\": \"iska_utils_stage list all\"},\n" +
            "        {\"execute\": \"say Reload complete!\"}\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";
        
        Path filePath = configPath.resolve("default_commands_macro.json");
        Files.writeString(filePath, content);
        LOGGER.info("Generated default_commands_macro.json file: {}", filePath);
    }

    /**
     * Reloads all macros from configuration files
     */
    public static void reloadAllMacros() {
        LOGGER.info("Reloading all command macros...");
        
        try {
            // Save protected macros (overwritable=false)
            Map<String, MacroCommand.MacroDefinition> protectedMacros = new HashMap<>();
            for (String macroId : MacroCommand.getAvailableMacros()) {
                if (PROTECTED_MACROS.getOrDefault(macroId, false)) {
                    protectedMacros.put(macroId, MacroCommand.getMacro(macroId));
                }
            }
            
            // Remove all non-protected macros (create a copy of the list to avoid ConcurrentModificationException)
            List<String> macroIds = new ArrayList<>();
            MacroCommand.getAvailableMacros().forEach(macroIds::add);
            
            for (String macroId : macroIds) {
                if (!PROTECTED_MACROS.getOrDefault(macroId, false)) {
                    MacroCommand.unregisterMacro(macroId);
                }
            }
            
            // Clear previous protections
            PROTECTED_MACROS.clear();
            
            // Reload configurations
            String externalScriptsBasePath = net.unfamily.iskautils.Config.externalScriptsPath;
            if (externalScriptsBasePath == null || externalScriptsBasePath.trim().isEmpty()) {
                externalScriptsBasePath = "kubejs/external_scripts"; // default path
            }
            
            Path configPath = Paths.get(externalScriptsBasePath, "iska_utils_macros");
            if (Files.exists(configPath) && Files.isDirectory(configPath)) {
                try (Stream<Path> files = Files.walk(configPath)) {
                    files.filter(Files::isRegularFile)
                         .filter(path -> path.toString().endsWith(".json"))
                         .filter(path -> !path.getFileName().toString().startsWith("."))
                         .sorted() // Process in alphabetical order
                         .forEach(MacroLoader::scanConfigFile);
                }
            }
            
            // Restore protected macros that might have been overwritten
            for (Map.Entry<String, MacroCommand.MacroDefinition> entry : protectedMacros.entrySet()) {
                String macroId = entry.getKey();
                PROTECTED_MACROS.put(macroId, true); // Mark as protected
                if (!MacroCommand.hasMacro(macroId)) {
                    MacroCommand.registerMacro(macroId, entry.getValue());
                }
            }
            
            // Count available macros
            int macroCount = 0;
            for (String macro : MacroCommand.getAvailableMacros()) {
                macroCount++;
            }
            
            LOGGER.info("Macro reload completed. {} macros available.", macroCount);
            
        } catch (Exception e) {
            LOGGER.error("Error reloading macros: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
} 