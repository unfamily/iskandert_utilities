package net.unfamily.iskautils.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Loads stage action definitions from external JSON files.
 * Files use type "iska_utils:stage_actions" and contain actions that run when stages are added/removed.
 */
public class StageActionsLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final List<StageActionDefinition> LOADED_ACTIONS = new ArrayList<>();

    /**
     * Scans the configuration directory for stage action definitions
     */
    public static void scanConfigDirectory() {
        LOGGER.info("Scanning configuration directory for stage actions...");

        try {
            String basePath = net.unfamily.iskautils.Config.externalScriptsPath;
            if (basePath == null || basePath.trim().isEmpty()) {
                basePath = "kubejs/external_scripts";
            }

            Path configPath = Paths.get(basePath, "iska_utils_stage_actions");
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath);
                LOGGER.info("Created directory for stage actions: {}", configPath.toAbsolutePath());
                createReadme(configPath);
            }

            if (!Files.isDirectory(configPath)) {
                LOGGER.warn("Stage actions path exists but is not a directory: {}", configPath);
                return;
            }

            LOADED_ACTIONS.clear();
            createReadme(configPath);

            try (Stream<Path> files = Files.walk(configPath)) {
                files.filter(Files::isRegularFile)
                     .filter(p -> p.toString().endsWith(".json"))
                     .filter(p -> !p.getFileName().toString().startsWith("."))
                     .sorted()
                     .forEach(StageActionsLoader::scanConfigFile);
            }

            LOGGER.info("Stage actions scan completed. Loaded {} actions", LOADED_ACTIONS.size());

        } catch (Exception e) {
            LOGGER.error("Error scanning stage actions directory: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    private static void scanConfigFile(Path configFile) {
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Scanning stage actions file: {}", configFile);
            }
            try (InputStream is = Files.newInputStream(configFile);
                 InputStreamReader reader = new InputStreamReader(is)) {
                JsonElement el = GSON.fromJson(reader, JsonElement.class);
                if (el != null && el.isJsonObject()) {
                    parseConfigJson(configFile.toString(), el.getAsJsonObject());
                } else {
                    LOGGER.warn("File {} does not contain valid JSON object", configFile);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error scanning file {}: {}", configFile, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    private static void parseConfigJson(String filePath, JsonObject json) {
        try {
            if (!json.has("type")) {
                LOGGER.warn("File {} has no 'type' field, ignored", filePath);
                return;
            }
            String type = json.get("type").getAsString();
            if (!"iska_utils:stage_actions".equals(type)) {
                LOGGER.warn("Unsupported type '{}' in file {}. Must be 'iska_utils:stage_actions'", type, filePath);
                return;
            }

            if (!json.has("actions") || !json.get("actions").isJsonArray()) {
                LOGGER.warn("File {} does not have valid 'actions' array", filePath);
                return;
            }

            JsonArray actionsArray = json.getAsJsonArray("actions");
            for (int i = 0; i < actionsArray.size(); i++) {
                JsonElement elem = actionsArray.get(i);
                if (elem.isJsonObject()) {
                    try {
                        StageActionDefinition def = StageActionDefinition.fromJson(elem.getAsJsonObject());
                        LOADED_ACTIONS.add(def);
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Loaded stage action {} (id={}) from file {}", i, def.getId(), filePath);
                        }
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn("Skipping action {} in {}: {}", i, filePath, e.getMessage());
                    }
                } else {
                    LOGGER.warn("Element {} in 'actions' is not a valid JSON object in {}", i, filePath);
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error parsing stage actions from {}: {}", filePath, e.getMessage());
        }
    }

    private static void createReadme(Path configPath) {
        try {
            Path readmePath = configPath.resolve("README.md");
            String content = "# Stage Actions\n\n" +
                "When a stage is added or removed, actions in this directory can run commands.\n\n" +
                "## File format\n\n" +
                "```json\n" +
                "{\n" +
                "  \"type\": \"iska_utils:stage_actions\",\n" +
                "  \"overwritable\": false,\n" +
                "  \"actions\": [\n" +
                "    {\n" +
                "      \"id\": \"gamemode_on_stage\",\n" +
                "      \"onCall\": true,\n" +
                "      \"onAdd\": true,\n" +
                "      \"onRemove\": true,\n" +
                "      \"stages_logic\": \"DEF_OR\",\n" +
                "      \"stages\": [\n" +
                "        {\"stage_type\": \"player\", \"stage\": \"player\", \"is\": true},\n" +
                "        {\"stage_type\": \"player\", \"stage\": \"admin\", \"is\": true}\n" +
                "      ],\n" +
                "      \"if\": [\n" +
                "        {\"conditions\": [0], \"do\": [{\"execute\": \"gamemode adventure @p\"}, {\"delay\": 10}, {\"execute\": \"tellraw @p \\\"Hello!\\\"\"}]},\n" +
                "        {\"conditions\": [1], \"do\": [{\"execute\": \"gamemode creative @p\"}]}\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"vip_removed\",\n" +
                "      \"onCall\": true,\n" +
                "      \"onAdd\": false,\n" +
                "      \"onRemove\": true,\n" +
                "      \"stages_logic\": \"AND\",\n" +
                "      \"stages\": [{\"stage_type\": \"player\", \"stage\": \"vip\", \"is\": false}],\n" +
                "      \"do\": [{\"execute\": \"gamemode survival @p\"}]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "```\n\n" +
                "## Fields\n\n" +
                "- `type`: Must be **iska_utils:stage_actions** [required]\n" +
                "- `overwritable`: Reserved for future use (optional)\n" +
                "- `actions`: Array of action definitions [required]\n\n" +
                "### Action fields\n\n" +
                "- `id`: Unique identifier for this action (required). Used by `/iska_utils_stage action` command.\n" +
                "- `onCall`: Can be triggered by `/iska_utils_stage action` command (default: true). Use false to only allow add/remove triggers.\n" +
                "- `onAdd`: Run when stage is added (default: true)\n" +
                "- `onRemove`: Run when stage is removed (default: true)\n" +
                "- `stages_logic`: AND, OR, DEF_AND, DEF_OR (same as command macros)\n" +
                "- `stages`: Array of stage conditions that define which stage change triggers this action\n" +
                "- `do`: Array of actions to run: `{\"execute\": \"command\"}` or `{\"delay\": ticks}`\n" +
                "- `if`: Array of branches `{\"conditions\": [indices], \"do\": [actions]}` for conditional execution\n\n" +
                "Configurations are loaded at server startup. Reload: `/iska_utils_debug reload` (quick) or `/reload` (full).\n";
            Files.writeString(readmePath, content);
        } catch (IOException e) {
            LOGGER.warn("Could not create README for stage actions: {}", e.getMessage());
        }
    }

    /**
     * Gets all loaded stage action definitions
     */
    public static List<StageActionDefinition> getLoadedActions() {
        return new ArrayList<>(LOADED_ACTIONS);
    }

    /**
     * Gets action by id, or null if not found
     */
    public static StageActionDefinition getActionById(String id) {
        if (id == null || id.isEmpty()) return null;
        for (StageActionDefinition def : LOADED_ACTIONS) {
            if (id.equals(def.getId())) return def;
        }
        return null;
    }

    /**
     * Gets all action ids for suggestions
     */
    public static List<String> getActionIds() {
        List<String> ids = new ArrayList<>();
        for (StageActionDefinition def : LOADED_ACTIONS) {
            if (!def.getId().isEmpty()) ids.add(def.getId());
        }
        return ids;
    }

    /**
     * Reloads all stage actions from configuration files
     */
    public static void reloadAllActions() {
        LOGGER.info("Reloading stage actions...");
        scanConfigDirectory();
    }
}
