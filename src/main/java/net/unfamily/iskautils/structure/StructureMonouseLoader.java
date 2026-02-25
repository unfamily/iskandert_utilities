package net.unfamily.iskautils.structure;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

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
     * Scans the configuration directory for monouse item definitions.
     * Internal defaults are registered first, then external scripts can override them.
     */
    public static void scanConfigDirectory() {
        try {
            // Clear previous definitions
            PROTECTED_DEFINITIONS.clear();
            MONOUSE_ITEMS.clear();
            
            // Register internal defaults first (scripts can override these)
            registerInternalDefaults();
            
            String configPathStr = net.unfamily.iskautils.Config.externalScriptsPath;
            if (configPathStr == null || configPathStr.trim().isEmpty()) {
                configPathStr = "kubejs/external_scripts";
            }
            
            Path configPath = Paths.get(configPathStr, "iska_utils_structures");
            
            if (!Files.exists(configPath)) {
                return;
            }
            
            // Scan all JSON files in the directory (scripts can override internal defaults)
            try (Stream<Path> files = Files.walk(configPath)) {
                files.filter(Files::isRegularFile)
                     .filter(path -> path.toString().endsWith(".json"))
                     .filter(path -> !path.getFileName().toString().startsWith("."))
                     .sorted() // Process in alphabetical order
                     .forEach(StructureMonouseLoader::scanConfigFile);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error scanning structure monouse item definitions directory: {}", e.getMessage());
        }
    }
    
    /**
     * Registers internal default monouse item definitions.
     * These are always available as fallback; external scripts can override them.
     */
    private static void registerInternalDefaults() {
        String itemId = "iska_utils_wither_grinder";
        StructureMonouseDefinition witherGrinder = new StructureMonouseDefinition(itemId);
        witherGrinder.setStructureId("iska_utils-wither_grinder");
        witherGrinder.setPlaceName("iska_utils-wither_grinder");
        witherGrinder.setAggressive(false);
        witherGrinder.addGiveItem(new StructureMonouseDefinition.GiveItem("minecraft:wither_skeleton_skull", 3));
        witherGrinder.addGiveItem(new StructureMonouseDefinition.GiveItem("minecraft:soul_sand", 4));
        
        MONOUSE_ITEMS.put(itemId, witherGrinder);
        LOGGER.debug("Registered internal default monouse item: {}", itemId);
    }
    
    /**
     * Scans a single configuration file for monouse item definitions
     */
    private static void scanConfigFile(Path configFile) {
        String definitionId = configFile.getFileName().toString().replace(".json", "");
        
        try (InputStream inputStream = Files.newInputStream(configFile)) {
            parseConfigFromStream(definitionId, configFile.toString(), inputStream);
        } catch (Exception e) {
            LOGGER.error("Error reading structure monouse item definition file {}: {}", configFile, e.getMessage());
        }
    }
    
    /**
     * Parses configuration from an input stream
     */
    private static void parseConfigFromStream(String definitionId, String filePath, InputStream inputStream) {
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            JsonElement jsonElement = GSON.fromJson(reader, JsonElement.class);
            if (jsonElement != null && jsonElement.isJsonObject()) {
                JsonObject json = jsonElement.getAsJsonObject();
                parseConfigJson(definitionId, filePath, json);
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing structure monouse item definition file {}: {}", filePath, e.getMessage());
        }
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
     * Dumps the internal default monouse item definitions to a JSON file.
     * Called by /iska_utils_debug dump_default to let users see and override defaults.
     */
    public static void dumpDefaultFile(Path configPath) throws java.io.IOException {
        Path defaultMonousePath = configPath.resolve("default_monouse.json");
        
        String content =
            "{\n" +
            "    \"type\": \"iska_utils:structure_monouse_item\",\n" +
            "    \"overwritable\": true,\n" +
            "    \"structure\": [\n" +
            "        {\n" +
            "            \"id\": \"iska_utils-wither_grinder\",\n" +
            "            \"place\": \"iska_utils-wither_grinder\",\n" +
            "            \"aggressive\": false,\n" +
            "            \"give\": [\n" +
            "                {\n" +
            "                    \"item\": \"minecraft:wither_skeleton_skull\",\n" +
            "                    \"count\": 3\n" +
            "                },\n" +
            "                {\n" +
            "                    \"item\": \"minecraft:soul_sand\",\n" +
            "                    \"count\": 4\n" +
            "                }\n" +
            "            ]\n" +
            "        }\n" +
            "    ]\n" +
            "}";
        
        Files.write(defaultMonousePath, content.getBytes());
        LOGGER.info("Dumped default monouse items to {}", defaultMonousePath);
    }
    
    /**
     * Reloads all monouse item definitions
     */
    public static void reloadAllDefinitions() {
        scanConfigDirectory();
    }
} 