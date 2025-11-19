package net.unfamily.iskautils.structure;

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
     * Scans the configuration directory for monouse item definitions
     */
    public static void scanConfigDirectory() {
        try {
            String configPathStr = net.unfamily.iskautils.Config.externalScriptsPath;
            if (configPathStr == null || configPathStr.trim().isEmpty()) {
                configPathStr = "kubejs/external_scripts";
            }
            
            Path configPath = Paths.get(configPathStr, "iska_utils_structures");
            
            if (!Files.exists(configPath)) {
                return;
            }
            
            // Clear previous definitions
            PROTECTED_DEFINITIONS.clear();
            MONOUSE_ITEMS.clear();
            
            // Generate default file if it doesn't exist
            Path defaultMonouseFile = configPath.resolve("default_monouse.json");
            if (!Files.exists(defaultMonouseFile) || shouldRegenerateDefaultMonouse(defaultMonouseFile)) {
                generateDefaultMonouse(configPath);
            }
            
            // Scan all JSON files in the directory
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
     * Checks if the default_monouse.json file should be regenerated
     */
    private static boolean shouldRegenerateDefaultMonouse(Path defaultFile) {
        try {
            if (!Files.exists(defaultFile)) return true;
            
            // Read file and check if it has overwritable field
            try (InputStream inputStream = Files.newInputStream(defaultFile)) {
                JsonElement jsonElement = GSON.fromJson(new InputStreamReader(inputStream), JsonElement.class);
                if (jsonElement != null && jsonElement.isJsonObject()) {
                    JsonObject json = jsonElement.getAsJsonObject();
                    if (json.has("overwritable")) {
                        return json.get("overwritable").getAsBoolean();
                    }
                }
            }
            return false;
        } catch (Exception e) {
            LOGGER.warn("Error reading default monouse items file, will regenerate: {}", e.getMessage());
            return true;
        }
    }
    
    /**
     * Generates the default monouse items file
     */
    private static void generateDefaultMonouse(Path configPath) {
        try {
            Path defaultMonousePath = configPath.resolve("default_monouse.json");
            
            String defaultMonouseContent =
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
            
            Files.write(defaultMonousePath, defaultMonouseContent.getBytes());
            
        } catch (IOException e) {
            LOGGER.error("Cannot create default monouse items file: {}", e.getMessage());
        }
    }
    
    /**
     * Reloads all monouse item definitions
     */
    public static void reloadAllDefinitions() {
        scanConfigDirectory();
    }
} 