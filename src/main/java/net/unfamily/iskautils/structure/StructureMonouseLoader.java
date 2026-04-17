package net.unfamily.iskautils.structure;

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

import java.util.HashMap;
import java.util.Map;

/**
 * Loads Structure Monouse item definitions from external JSON files
 */
public class StructureMonouseLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    // Map to store monouse item definitions
    private static final Map<String, StructureMonouseDefinition> MONOUSE_ITEMS = new HashMap<>();
    
    // Store files with overwritable=false to prevent overwriting
    private static final Map<String, Boolean> PROTECTED_DEFINITIONS = new HashMap<>();
    
    /**
     * Loads monouse definitions from {@code data/<namespace>/load/iska_utils_structures_monouse/}.
     */
    public static void loadAll(ResourceManager resourceManagerOrNull) {
        PROTECTED_DEFINITIONS.clear();
        MONOUSE_ITEMS.clear();
        Map<Identifier, JsonElement> merged = resourceManagerOrNull != null
                ? IskaUtilsLoadJson.collectMergedJson(resourceManagerOrNull,
                id -> IskaUtilsLoadPaths.isJsonUnderLoadSubdir(id, IskaUtilsLoadPaths.STRUCTURE_MONOUSE))
                : IskaUtilsLoadJson.collectFromModJarOnly(IskaUtilsLoadPaths.STRUCTURE_MONOUSE);
        for (var e : IskaUtilsLoadJson.orderedEntries(merged)) {
            if (!e.getValue().isJsonObject()) {
                continue;
            }
            String definitionId = IskaUtilsLoadJson.definitionIdFromLocation(e.getKey());
            parseConfigJson(definitionId, e.getKey().toString(), e.getValue().getAsJsonObject());
        }
        LOGGER.info("Structure monouse definitions loaded: {}", MONOUSE_ITEMS.size());
    }

    public static void scanConfigDirectory() {
        loadAll(null);
    }
    
    /**
     * Parses configuration from a JSON object
     */
    private static void parseConfigJson(String definitionId, String filePath, JsonObject json) {
        try {
            // Check if this is a monouse item definition file
            if (!json.has("type") || !json.get("type").getAsString().equals("iska_utils:structure_monouse_item")) {
                return;
            }
            
            // Get overwritable status
            boolean overwritable = true;
            if (json.has("overwritable")) {
                overwritable = json.get("overwritable").getAsBoolean();
            }
            
            // Check if this is a protected file
            if (PROTECTED_DEFINITIONS.containsKey(definitionId) && !PROTECTED_DEFINITIONS.get(definitionId)) {
                return;
            }
            
            // Update protection status
            PROTECTED_DEFINITIONS.put(definitionId, overwritable);
            
            // Process structure items
            processStructuresJson(json);
            
        } catch (Exception e) {
            LOGGER.error("Error processing structure monouse item definition {}: {}", definitionId, e.getMessage());
        }
    }
    
    /**
     * Processes the structures array from a monouse item definition file
     */
    private static void processStructuresJson(JsonObject json) {
        if (!json.has("structure") || !json.get("structure").isJsonArray()) {
            LOGGER.error("Structure monouse item definition file missing 'structure' array");
            return;
        }
        
        JsonArray structuresArray = json.getAsJsonArray("structure");
        for (JsonElement structureElement : structuresArray) {
            if (structureElement.isJsonObject()) {
                processStructureDefinition(structureElement.getAsJsonObject());
            }
        }
    }
    
    /**
     * Processes a single monouse structure definition
     */
    private static void processStructureDefinition(JsonObject structureJson) {
        try {
            // Get required ID
            if (!structureJson.has("id") || !structureJson.get("id").isJsonPrimitive()) {
                LOGGER.error("Structure monouse item definition missing 'id' field");
                return;
            }
            
            String structureId = structureJson.get("id").getAsString();
            
            // Convert ID from "iska_utils-wither_grinder" to "iska_utils_wither_grinder"
            String itemId = structureId.replace("-", "_");
            
            // Create item definition
            StructureMonouseDefinition definition = new StructureMonouseDefinition(itemId);
            definition.setStructureId(structureId);
            
            // Parse structure name to place
            if (structureJson.has("place")) {
                definition.setPlaceName(structureJson.get("place").getAsString());
            } else {
                definition.setPlaceName(structureId);
            }
            
            // Parse aggressive flag
            if (structureJson.has("aggressive")) {
                definition.setAggressive(structureJson.get("aggressive").getAsBoolean());
            }

            // Parse give items array
            if (structureJson.has("give") && structureJson.get("give").isJsonArray()) {
                JsonArray giveArray = structureJson.getAsJsonArray("give");
                for (JsonElement giveElement : giveArray) {
                    if (giveElement.isJsonObject()) {
                        JsonObject giveObj = giveElement.getAsJsonObject();
                        if (giveObj.has("item") && giveObj.has("count")) {
                            String itemName = giveObj.get("item").getAsString();
                            int count = giveObj.get("count").getAsInt();
                            definition.addGiveItem(new StructureMonouseDefinition.GiveItem(itemName, count));
                        }
                    }
                }
            }
            
            // Add definition to map
            MONOUSE_ITEMS.put(itemId, definition);
            
        } catch (Exception e) {
            LOGGER.error("Error processing structure definition: {}", e.getMessage());
        }
    }
    
    /**
     * Gets a monouse item definition by ID
     */
    public static StructureMonouseDefinition getMonouseItem(String id) {
        return MONOUSE_ITEMS.get(id);
    }
    
    /**
     * Gets all loaded monouse item definitions
     */
    public static Map<String, StructureMonouseDefinition> getAllMonouseItems() {
        return new HashMap<>(MONOUSE_ITEMS);
    }
    /**
     * Reloads all monouse item definitions
     */
    public static void reloadAllDefinitions() {
        var server = ServerLifecycleHooks.getCurrentServer();
        loadAll(server != null ? server.getResourceManager() : null);
    }
} 