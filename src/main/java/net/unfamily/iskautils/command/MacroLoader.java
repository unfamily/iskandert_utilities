package net.unfamily.iskautils.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.unfamily.iskautils.IskaUtils;
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
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
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
                
                // Generate index file
                IndexGenerator.generateIndex();
                
                return;
            }
            
            if (!Files.isDirectory(configPath)) {
                LOGGER.warn("The path for macros exists but is not a directory: {}", configPath);
                return;
            }
            
            LOGGER.info("Scanning directory for macros: {}", configPath.toAbsolutePath());
            
            // Always regenerate README
            createReadme(configPath);
            LOGGER.info("Updated README.md");
            
            // Generate index file
            IndexGenerator.generateIndex();
            
            // Clear previous protections
            PROTECTED_MACROS.clear();
            
            // Always check and regenerate default_commands_macro.json if needed
            Path defaultCommandsFile = configPath.resolve("default_commands_macro.json");
            if (!Files.exists(defaultCommandsFile)) {
                LOGGER.info("Generating default_commands_macro.json file");
                generateDefaultCommandsMacro(configPath);
            } else {
                // Check if the file has overwritable: true
                if (shouldRegenerateDefaultCommandsMacro(defaultCommandsFile)) {
                    LOGGER.info("Regenerating default_commands_macro.json file (overwritable: true)");
                    generateDefaultCommandsMacro(configPath);
                }
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
                    
                    // Check if the overwritable field exists
                    if (json.has("overwritable")) {
                        // If overwritable is true, regenerate the file
                        boolean overwritable = json.get("overwritable").getAsBoolean();
                        if (overwritable) {
                            LOGGER.debug("Found default_commands_macro.json with overwritable: true, will regenerate");
                            return true;
                        } else {
                            LOGGER.debug("Found default_commands_macro.json with overwritable: false, will not regenerate");
                            return false;
                        }
                    }
                    
                    // If no overwritable field, default to true (regenerate)
                    LOGGER.debug("Found default_commands_macro.json without overwritable field, assuming true");
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error reading default_commands_macro.json file: {}", e.getMessage());
        }
        
        // If the file can't be read or isn't valid JSON, regenerate it
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
                "\n"+
                "All commands are reloadable by /reload or better verison provided by this mod\n/reloader, so you can modify them at any time, if you change a default generated file set overwritable to false in the file.\n" +
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
                "        {\"execute\": \"summon minecraft:#0 ~ ~ ~\"}\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"command\": \"staged_example\",\n" +
                "      \"level\": 0,\n" +
                "      \"stages_logic\": \"AND\",\n" +
                "      \"stages\": [\n" +
                "        {\"stage\": \"example_stage_0\", \"stage_type\": \"player\", \"is\": true}\n" +
                "      ],\n" +
                "      \"do\": [\n" +
                "        {\"execute\": \"say example_stage_0\"}\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"command\": \"staged_def\",\n" +
                "      \"level\": 0,\n" +
                "      \"stages_logic\": \"DEF_AND\",\n" +
                "      \"stages\": [\n" +
                "        {\"stage\": \"example_stage_0\", \"stage_type\": \"player\", \"is\": true},\n" +
                "        {\"stage\": \"example_stage_1\", \"stage_type\": \"player\", \"is\": true},\n" +
                "        {\"stage\": \"example_stage_2\", \"stage_type\": \"player\", \"is\": true}\n" +
                "      ],\n" +
                "      \"do\": [\n" +
                "        {\"if\": [\n" +
                "            {\"conditions\": [0, 2]},\n" +
                "            {\"execute\": \"say example_stage_0_and_2\"}\n" +
                "        ]},\n" +
                "        {\"if\": [\n" +
                "            {\"conditions\": [1, 2]},\n" +
                "            {\"execute\": \"say example_stage_1_and_2\"}\n" +
                "        ]}\n" +
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
                "- `stages_logic`: Logic for stage requirements evaluation [optional, default: AND]\n" +
                "  - `AND`: All stages must be satisfied\n" +
                "  - `OR`: At least one stage must be satisfied\n" +
                "  - `DEF_AND`: Deferred evaluation where all per-action stages must be satisfied (AND logic)\n" +
                "  - `DEF_OR`: Deferred evaluation where at least one per-action stage must be satisfied (OR logic)\n" +
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
                "  - `{\"if\": [ {\"conditions\": [0, 1]}, {\"execute\": \"command\"} ]}`: Conditional execution based on stage indices\n" +
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
                "You can escape the # character by using ##, which will be converted to a single # in the command.\n" +
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
                "### Sub-Command Implementation\n" +
                "You can implement sub-commands using the static parameter type and conditional execution with stages. Here's a complete example of how to create a powerful recursive sub-command system:\n" +
                "\n" +
                "```json\n" +
                "{\n" +
                "  \"type\": \"iska_utils:commands_macro\",\n" +
                "  \"overwritable\": false,\n" +
                "  \"commands\": [\n" +
                "    {\n" +
                "      \"command\": \"sub_command\",\n" +
                "      \"level\": 0,\n" +
                "      \"stages_logic\": \"DEF_AND\",\n" +
                "      \"stages\": [\n" +
                "        {\"stage\": \"sub_command_condition_0\", \"stage_type\": \"player\", \"is\": true},\n" +
                "        {\"stage\": \"sub_command_condition_1\", \"stage_type\": \"player\", \"is\": true},\n" +
                "        {\"stage\": \"sub_command_condition_2\", \"stage_type\": \"player\", \"is\": true},\n" +
                "\n" +
                "        {\"stage\": \"sub_command_condition_0\", \"stage_type\": \"player\", \"is\": false},\n" +
                "        {\"stage\": \"sub_command_condition_1\", \"stage_type\": \"player\", \"is\": false},\n" +
                "        {\"stage\": \"sub_command_condition_2\", \"stage_type\": \"player\", \"is\": false}\n" +
                "      ],\n" +
                "      \"parameters\": [\n" +
                "          {\n" +
                "            \"type\": \"static\",\n" +
                "            \"list\": [\n" +
                "              {\"declare\": \"condition_0\"},\n" +
                "              {\"declare\": \"condition_1\"},\n" +
                "              {\"declare\": \"condition_2\"}\n" +
                "            ]\n" +
                "          }\n" +
                "        ],\n" +
                "\n" +
                "      \"do\": [\n" +
                "      {\"if\": [\n" +
                "        {\"conditions\":[3,4,5]},\n" +
                "        {\"execute\": \"iska_utils_stage add player @s sub_command_#0 true\"},\n" +
                "        {\"delay\": 5},\n" +
                "        {\"execute\": \"sub_commad #0\"}\n" +
                "      ]},\n" +
                "        {\"if\": [\n" +
                "            {\"conditions\": [0]},\n" +
                "            {\"execute\": \"tellraw @s \\\"condition_0\\\"\"},\n" +
                "            {\"execute\": \"iska_utils_stage remove player @s sub_command_#0 true\"}\n" +
                "        ]},\n" +
                "        {\"if\": [\n" +
                "            {\"conditions\": [1]},\n" +
                "            {\"execute\": \"tellraw @s \\\"condition_1\\\"\"},\n" +
                "            {\"execute\": \"iska_utils_stage remove player @s sub_command_#0 true\"}\n" +
                "        ]},\n" +
                "        {\"if\": [\n" +
                "            {\"conditions\": [2]},\n" +
                "            {\"execute\": \"tellraw @s \\\"condition_1\\\"\"},\n" +
                "            {\"execute\": \"iska_utils_stage remove player @s sub_command_#0 true\"}\n" +
                "        ]}\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "```\n" +
                "\n" +
                "This advanced pattern works by:\n" +
                "1. Defining a command with a static parameter that offers multiple sub-command options (`condition_0`, `condition_1`, `condition_2`)\n" +
                "2. Using the `DEF_AND` stages logic for deferred evaluation of conditions\n" +
                "3. Leveraging both positive and negative stage conditions:\n" +
                "   - Indexes 0-2: Positive conditions checking if stages are present\n" +
                "   - Indexes 3-5: Negative conditions checking if stages are absent\n" +
                "4. Using recursion for efficient handling:\n" +
                "   - The first `if` block checks if no stages are active (negative conditions)\n" +
                "   - It then adds the appropriate stage and recursively calls itself\n" +
                "   - On the second call, one of the positive condition blocks will match\n" +
                "   - The matching block executes its command and cleans up by removing the stage\n" +
                "\n" +
                "The key advantages of this approach:\n" +
                "- Clean recursive implementation that separates stage setting from execution\n" +
                "- Only one stage is active at a time, preventing conflicts\n" +
                "- Tab completion for sub-commands comes built-in\n" +
                "- Easy to add new sub-commands by extending the static list and adding corresponding conditions\n" +
                "- Each sub-command has its own dedicated code block for maximum flexibility\n" +
                "\n" +
                "Additionally, you can use the `is` field to create negative requirements:\n" +
                "- `\"is\": true` (default): The player/world/dimension MUST have the specified stage\n" +
                "- `\"is\": false`: The player/world/dimension MUST NOT have the specified stage\n" +
                "\n" +
                "This allows you to create conditions like \"player must have stage A but must not have stage B\".\n" +
                "\n" +
                "### Conditional Execution with IF\n" +
                "The IF action type allows you to conditionally execute commands based on specific stage conditions.\n" +
                "When using `DEF_AND` or `DEF_OR` stage logic, you can use the `if` action type in your `do` array:\n" +
                "\n" +
                "```json\n" +
                "{\"if\": [\n" +
                "    {\"conditions\": [0, 2]},\n" +
                "    {\"execute\": \"say condition met\"}\n" +
                "]}\n" +
                "```\n" +
                "\n" +
                "The `conditions` array contains indices that refer to the stages defined in the command's `stages` array.\n" +
                "In the example above, the command will execute only if stages at index 0 and 2 are satisfied.\n" +
                "\n" +
                "With `DEF_AND` logic, ALL specified conditions must be met.\n" +
                "With `DEF_OR` logic, ANY ONE of the specified conditions must be met.\n" +
                "\n" +
                "## Notes\n" +
                "\n" +
                "- Commands are executed in the context of the player who triggered the macro\n" +
                "- If executed by a command block, the position of the command block is used\n" +
                "- Changes require a game restart to apply\n" +
                "- For security reasons, macros are limited to players with the appropriate permission level\n" +
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
                "### Per-Action Stage Requirements with DEF_AND and DEF_OR\n" +
                "\n" +
                "#### DEF_AND Example\n" +
                "Using `DEF_AND` means each action requires ALL of its stage conditions to be met:\n" +
                "\n" +
                "```json\n" +
                "{\n" +
                "  \"command\": \"dungeon_rewards\",\n" +
                "  \"level\": 0,\n" +
                "  \"stages_logic\": \"DEF_AND\",\n" +
                "  \"stages\": [\n" +
                "    {\"stage\": \"dungeon_boss_defeated\", \"stage_type\": \"player\", \"is\": true},\n" +
                "    {\"stage\": \"dungeon_curse\", \"stage_type\": \"player\", \"is\": false},\n" +
                "    {\"stage\": \"dungeon_secret_found\", \"stage_type\": \"player\", \"is\": true}\n" +
                "  ],\n" +
                "  \"do\": [\n" +
                "    {\n" +
                "      \"execute\": \"say Welcome to the dungeon rewards system!\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"if\": [\n" +
                "        {\"conditions\": [0, 1]},\n" +
                "        {\"execute\": \"give @s minecraft:diamond 5\"}\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"if\": [\n" +
                "        {\"conditions\": [0, 2]},\n" +
                "        {\"execute\": \"give @s minecraft:netherite_ingot 1\"}\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "```\n" +
                "\n" +
                "In this example:\n" +
                "- The first action always executes (no conditions)\n" +
                "- The second action only executes if conditions 0 and 1 are met (player has beaten the boss AND does NOT have the curse)\n" +
                "- The third action only executes if conditions 0 and 2 are met (player has beaten the boss AND found the secret)\n" +
                "\n" +
                "#### DEF_OR Example\n" +
                "Using `DEF_OR` means each action requires ANY of its stage conditions to be met:\n" +
                "\n" +
                "```json\n" +
                "{\n" +
                "  \"command\": \"guild_benefits\",\n" +
                "  \"level\": 0,\n" +
                "  \"stages_logic\": \"DEF_OR\",\n" +
                "  \"stages\": [\n" +
                "    {\"stage\": \"guild_healer\", \"stage_type\": \"player\", \"is\": true},\n" +
                "    {\"stage\": \"guild_tank\", \"stage_type\": \"player\", \"is\": true},\n" +
                "    {\"stage\": \"guild_warrior\", \"stage_type\": \"player\", \"is\": true}\n" +
                "  ],\n" +
                "  \"do\": [\n" +
                "    {\n" +
                "      \"execute\": \"say Guild Benefits System Activated!\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"if\": [\n" +
                "        {\"conditions\": [0]},\n" +
                "        {\"execute\": \"effect give @s minecraft:regeneration 60 1\"}\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"if\": [\n" +
                "        {\"conditions\": [1]},\n" +
                "        {\"execute\": \"effect give @s minecraft:resistance 60 1\"}\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"if\": [\n" +
                "        {\"conditions\": [2]},\n" +
                "        {\"execute\": \"effect give @s minecraft:strength 60 1\"}\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "```\n" +
                "\n" +
                "In this example:\n" +
                "- The first action always executes (no conditions)\n" +
                "- The second action executes if the player has the \"guild_healer\" stage\n" +
                "- The third action executes if the player has the \"guild_tank\" stage\n" +
                "- The fourth action executes if the player has the \"guild_warrior\" stage\n" +
                "\n" +
                "The key difference between these examples is how the stage conditions are evaluated for each action:\n" +
                "- With `DEF_AND`, each action requires ALL of its conditions to be met\n" +
                "- With `DEF_OR`, each action requires ANY ONE of its conditions to be met\n" +
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
            "        {\"execute\": \"custommachinery reload\"}\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";
        
        Path filePath = configPath.resolve("default_commands_macro.json");
        Files.writeString(filePath, content);
        LOGGER.info("Generated default_commands_macro.json file: {}", filePath);
    }

    /**
     * Reloads all macros from configuration files and registers commands
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
            
            // Registra i comandi se il server è attivo
            registerCommandsIfServerAvailable();
            
        } catch (Exception e) {
            LOGGER.error("Error reloading macros: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Registra i comandi per tutti i macro nel CommandDispatcher se il server è disponibile
     */
    private static void registerCommandsIfServerAvailable() {
        try {
            // Ottiene il server
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                LOGGER.debug("Server not available, commands will be registered when the server starts");
                return;
            }
            
            // Ottiene il CommandDispatcher
            CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
            if (dispatcher == null) {
                LOGGER.error("CommandDispatcher not available");
                return;
            }
            
            // Registra i comandi per tutti i macro
            for (String macroId : MacroCommand.getAvailableMacros()) {
                MacroCommand.MacroDefinition macro = MacroCommand.getMacro(macroId);
                if (macro != null) {
                    MacroCommand.registerCommand(dispatcher, macroId, macro);
                    LOGGER.debug("Registered command for macro: {}", macroId);
                }
            }
            
            LOGGER.info("Registered all macro commands in the dispatcher");
        } catch (Exception e) {
            LOGGER.error("Error registering commands: {}", e.getMessage());
        }
    }

    /**
     * Registra i comandi quando viene caricato il server
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registering commands for all macros...");
        
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        // Registra i comandi per tutti i macro
        for (String macroId : MacroCommand.getAvailableMacros()) {
            MacroCommand.MacroDefinition macro = MacroCommand.getMacro(macroId);
            if (macro != null) {
                MacroCommand.registerCommand(dispatcher, macroId, macro);
                LOGGER.debug("Registered command for macro: {}", macroId);
            }
        }
    }
} 