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
import java.util.List;
import java.util.Map;

/**
 * Loads stage action definitions from external JSON files.
 * Files use type "iska_utils:stage_actions" and contain actions that run when stages are added/removed.
 */
public class StageActionsLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final List<StageActionDefinition> LOADED_ACTIONS = new ArrayList<>();

    public static void loadAll(ResourceManager resourceManagerOrNull) {
        LOGGER.info("Loading stage actions from datapack...");
        try {
            LOADED_ACTIONS.clear();
            Map<Identifier, JsonElement> merged = resourceManagerOrNull != null
                    ? IskaUtilsLoadJson.collectMergedJson(resourceManagerOrNull,
                    id -> IskaUtilsLoadPaths.isJsonUnderLoadSubdir(id, IskaUtilsLoadPaths.STAGE_ACTIONS))
                    : IskaUtilsLoadJson.collectFromModJarOnly(IskaUtilsLoadPaths.STAGE_ACTIONS);
            for (var e : IskaUtilsLoadJson.orderedEntries(merged)) {
                if (!e.getValue().isJsonObject()) {
                    continue;
                }
                parseConfigJson(e.getKey().toString(), e.getValue().getAsJsonObject());
            }
            LOGGER.info("Stage actions loaded: {}", LOADED_ACTIONS.size());
        } catch (Exception e) {
            LOGGER.error("Error loading stage actions: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    public static void scanConfigDirectory() {
        loadAll(null);
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
        var server = ServerLifecycleHooks.getCurrentServer();
        loadAll(server != null ? server.getResourceManager() : null);
    }
}
