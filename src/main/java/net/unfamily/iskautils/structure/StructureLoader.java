package net.unfamily.iskautils.structure;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.unfamily.iskautils.Config;
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
 * Loads structure definitions from external JSON files
 */
public class StructureLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    // Map to store loaded structures
    private static final Map<String, StructureDefinition> STRUCTURES = new HashMap<>();
    
    // Files and directories from which structures are loaded
    private static final Map<String, Boolean> PROTECTED_DEFINITIONS = new HashMap<>();

    /**
     * Scans the configuration directory for structures
     */
    public static void scanConfigDirectory() {
        String configPath = Config.externalScriptsPath;
        if (configPath == null || configPath.trim().isEmpty()) {
            configPath = "kubejs/external_scripts";
        }
        
        Path structuresPath = Paths.get(configPath, "iska_utils_structures");
        
        try {
            // Create directory if it doesn't exist
            if (!Files.exists(structuresPath)) {
                Files.createDirectories(structuresPath);
                LOGGER.info("Created structures directory: {}", structuresPath);
            }
            
            // Clear previous structures
            PROTECTED_DEFINITIONS.clear();
            STRUCTURES.clear();
            
            // Generate default file if it doesn't exist
            Path defaultStructuresFile = structuresPath.resolve("default_structures.json");
            if (!Files.exists(defaultStructuresFile) || shouldRegenerateDefaultStructures(defaultStructuresFile)) {
                LOGGER.debug("Generating or regenerating default_structures.json file");
                generateDefaultStructures(structuresPath);
            }
            
            // Scan all JSON files in the directory
            try (Stream<Path> files = Files.walk(structuresPath)) {
                files.filter(Files::isRegularFile)
                     .filter(path -> path.toString().endsWith(".json"))
                     .filter(path -> !path.getFileName().toString().startsWith("."))
                     .sorted() // Process in alphabetical order
                     .forEach(StructureLoader::scanConfigFile);
            }
            
            LOGGER.info("Structure scanning completed. Loaded {} structures", STRUCTURES.size());
            
            // Generate comprehensive documentation
            StructureDocumentationGenerator.generateDocumentation();
            
        } catch (Exception e) {
            LOGGER.error("Error while scanning structures directory: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Checks if the default_structures.json file should be regenerated
     */
    private static boolean shouldRegenerateDefaultStructures(Path defaultFile) {
        try {
            if (!Files.exists(defaultFile)) return true;
            
            // Read the file and check if it has the overwritable field
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
            LOGGER.warn("Error reading default structures file, it will be regenerated: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Generates the default structures file
     */
    private static void generateDefaultStructures(Path structuresPath) {
        try {
            Path defaultStructuresPath = structuresPath.resolve("default_structures.json");
            
            // Copy the content of the internal default file
            String defaultStructuresContent = 
                "{\n" +
                "    \"type\": \"iska_utils:structure\",\n" +
                "    \"overwritable\": true,\n" +
                "    \"structure\": [\n" +
                "        {\n" +
                "            \"id\": \"iska_utils-wither_grinder\",\n" +
                "            \"name\": \"Iskandert's Wither Grinder\",\n" +
                "            \"can_force\": true,\n" +
                "            \"can_replace\": [\n" +
                "            ],\n" +
                "            \"icon\": {\n" +
                "                \"type\": \"minecraft:item\",\n" +
                "                \"item\": \"minecraft:wither_skeleton_skull\"\n" +
                "            },\n" +
                "            \"description\": [\"Easy way to kill withers\"],\n" +
                "            \"pattern\": [\n" +
                "                [[\"   \"],[\"   \"],[\"   \"],[\"   \"],[\"AAA\"], [\"AAA\"], [\"AAA\"]],\n" +
                "                [[\"L@ \"],[\"   \"],[\"   \"],[\"   \"],[\"AAA\"], [\"A A\"], [\"AAA\"]],\n" +
                "                [[\"   \"],[\"   \"],[\"   \"],[\"   \"],[\"AAA\"], [\"AAA\"], [\"AAA\"]]\n" +
                "            ],\n" +
                "            \"key\": {\n" +
                "                \"A\": {\n" +
                "                    \"display\": \"iska_utils.wither_proof_block\",\n" +
                "                    \"alternatives\": [\n" +
                "                        {\n" +
                "                            \"block\": \"iska_utils:wither_proof_block\"\n" +
                "                        },\n" +
                "                        {\n" +
                "                            \"block\": \"mob_grinding_utils:tinted_glass\"\n" +
                "                        }\n" +
                "                    ]\n" +
                "                },\n" +
                "                \"@\": {\n" +
                "                    \"display\": \"minecraft.piston\",\n" +
                "                    \"alternatives\": [\n" +
                "                        {\n" +
                "                            \"block\": \"minecraft:piston\",\n" +
                "                            \"properties\": {\n" +
                "                                \"facing\": \"up\"\n" +
                "                            }\n" +
                "                        }\n" +
                "                    ]\n" +
                "                },\n" +
                "                \"L\": {\n" +
                "                    \"display\": \"minecraft.lever\",\n" +
                "                    \"alternatives\": [\n" +
                "                        {\n" +
                "                            \"block\": \"minecraft:lever\",\n" +
                "                            \"properties\": {\n" +
                "                                \"face\": \"floor\",\n" +
                "                                \"facing\": \"east\",\n" +
                "                                \"powered\": \"false\"\n" +
                "                            }\n" +
                "                        }\n" +
                "                    ]\n" +
                "                }\n" +
                "            }\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": \"iska_utils-wither_summoning\",\n" +
                "            \"name\": \"Wither Summoning Structure\",\n" +
                "            \"can_replace\": [],\n" +
                "            \"slower\": true,\n" +
                "            \"place_like_player\": true,\n" +
                "            \"icon\": {\n" +
                "                \"type\": \"minecraft:item\",\n" +
                "                \"item\": \"minecraft:wither_skeleton_skull\"\n" +
                "            },\n" +
                "            \"description\": [\"Structure to summon the Wither boss\"],\n" +
                "            \"pattern\": [\n" +
                "                [[\" @ \"],[\" A \"],[\"AAA\"],[\"BBB\"]]\n" +
                "            ],\n" +
                "            \"key\": {\n" +
                "                \"A\": {\n" +
                "                    \"display\": \"minecraft.soul_sand\",\n" +
                "                    \"alternatives\": [\n" +
                "                        {\n" +
                "                            \"block\": \"minecraft:soul_sand\"\n" +
                "                        },\n" +
                "                        {\n" +
                "                            \"block\": \"minecraft:soul_soil\"\n" +
                "                        }\n" +
                "                    ]\n" +
                "                },\n" +
                "                \"B\": {\n" +
                "                    \"display\": \"minecraft.wither_skeleton_skull\",\n" +
                "                    \"alternatives\": [\n" +
                "                        {\n" +
                "                            \"block\": \"minecraft:wither_skeleton_skull\",\n" +
                "                            \"properties\": {\n" +
                "                                \"rotation\": \"0\"\n" +
                "                            }\n" +
                "                        }\n" +
                "                    ]\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "    ]\n" +
                "}";
            
            Files.write(defaultStructuresPath, defaultStructuresContent.getBytes());
            LOGGER.info("Created example structures file: {}", defaultStructuresPath);
            
        } catch (IOException e) {
            LOGGER.error("Unable to create default structures file: {}", e.getMessage());
        }
    }

    /**
     * Scans a single configuration file for structures
     */
    private static void scanConfigFile(Path configFile) {
        LOGGER.debug("Scanning configuration file: {}", configFile);
        
        String definitionId = configFile.getFileName().toString().replace(".json", "");
        
        try (InputStream inputStream = Files.newInputStream(configFile)) {
            parseConfigFromStream(definitionId, configFile.toString(), inputStream);
        } catch (Exception e) {
            LOGGER.error("Error reading structures file {}: {}", configFile, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
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
            } else {
                LOGGER.error("Invalid JSON in structures file: {}", filePath);
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing structures file {}: {}", filePath, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Processes configuration from JSON object
     */
    private static void parseConfigJson(String definitionId, String filePath, JsonObject json) {
        try {
            // Check if this is a structure definition file
            if (!json.has("type") || !json.get("type").getAsString().equals("iska_utils:structure")) {
                LOGGER.debug("Skipped file {} - not a structure definition", filePath);
                return;
            }
            
            // Get overwritable status
            boolean overwritable = true;
            if (json.has("overwritable")) {
                overwritable = json.get("overwritable").getAsBoolean();
            }
            
            // Check if this is a protected file
            if (PROTECTED_DEFINITIONS.containsKey(definitionId) && !PROTECTED_DEFINITIONS.get(definitionId)) {
                LOGGER.debug("Skipped protected structure definition: {}", definitionId);
                return;
            }
            
            // Update protection status
            PROTECTED_DEFINITIONS.put(definitionId, overwritable);
            
            // Process structures
            processStructuresJson(json);
            
        } catch (Exception e) {
            LOGGER.error("Error processing structure definition {}: {}", definitionId, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Processes structure array from definition file
     */
    private static void processStructuresJson(JsonObject json) {
        if (!json.has("structure") || !json.get("structure").isJsonArray()) {
            LOGGER.error("Structure definition file missing 'structure' array");
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
     * Processes a single structure definition
     */
    private static void processStructureDefinition(JsonObject structureJson) {
        try {
            // Get required ID
            if (!structureJson.has("id") || !structureJson.get("id").isJsonPrimitive()) {
                LOGGER.error("Structure definition missing 'id' field");
                return;
            }
            
            String structureId = structureJson.get("id").getAsString();
            
            // Create structure definition
            StructureDefinition definition = new StructureDefinition();
            definition.setId(structureId);
            
            // Parse name
            if (structureJson.has("name")) {
                definition.setName(structureJson.get("name").getAsString());
            } else {
                definition.setName(structureId); // Use ID as fallback
            }
            
            // Parse can_force
            if (structureJson.has("can_force")) {
                definition.setCanForce(structureJson.get("can_force").getAsBoolean());
            }
            
            // Parse slower
            if (structureJson.has("slower")) {
                definition.setSlower(structureJson.get("slower").getAsBoolean());
            }
            
            // Parse place_like_player
            if (structureJson.has("place_like_player")) {
                definition.setPlaceAsPlayer(structureJson.get("place_like_player").getAsBoolean());
            }
            
            // Parse can_replace
            if (structureJson.has("can_replace") && structureJson.get("can_replace").isJsonArray()) {
                JsonArray replaceArray = structureJson.getAsJsonArray("can_replace");
                List<String> canReplace = new ArrayList<>();
                for (JsonElement element : replaceArray) {
                    canReplace.add(element.getAsString());
                }
                definition.setCanReplace(canReplace);
            }
            
            // Parse icon
            if (structureJson.has("icon") && structureJson.get("icon").isJsonObject()) {
                JsonObject iconJson = structureJson.getAsJsonObject("icon");
                StructureDefinition.IconDefinition icon = new StructureDefinition.IconDefinition();
                
                if (iconJson.has("item")) {
                    icon.setItem(iconJson.get("item").getAsString());
                }
                if (iconJson.has("count")) {
                    icon.setCount(iconJson.get("count").getAsInt());
                }
                
                definition.setIcon(icon);
            }
            
            // Parse description
            if (structureJson.has("description")) {
                if (structureJson.get("description").isJsonArray()) {
                    JsonArray descArray = structureJson.getAsJsonArray("description");
                    // Take only first element as single string
                    if (descArray.size() > 0) {
                        definition.setDescription(descArray.get(0).getAsString());
                    }
                } else {
                    definition.setDescription(structureJson.get("description").getAsString());
                }
            }
            
            // Parse pattern
            if (structureJson.has("pattern") && structureJson.get("pattern").isJsonArray()) {
                JsonArray patternArray = structureJson.getAsJsonArray("pattern");
                String[][][][] pattern = parsePattern(patternArray);
                definition.setPattern(pattern);
            }
            
            // Parse key
            if (structureJson.has("key") && structureJson.get("key").isJsonObject()) {
                JsonObject keyJson = structureJson.getAsJsonObject("key");
                Map<String, List<StructureDefinition.BlockDefinition>> key = parseKey(keyJson);
                definition.setKey(key);
            }
            
            // Parse stages - now a list of strings
            if (structureJson.has("stages") && structureJson.get("stages").isJsonArray()) {
                JsonArray stagesArray = structureJson.getAsJsonArray("stages");
                List<String> stages = new ArrayList<>();
                for (JsonElement stageElement : stagesArray) {
                    stages.add(stageElement.getAsString());
                }
                definition.setStages(stages);
            }
            
            // Register structure definition
            STRUCTURES.put(structureId, definition);
            LOGGER.debug("Registered structure definition: {}", structureId);
            
        } catch (Exception e) {
            LOGGER.error("Error processing structure definition: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Parses structure pattern
     * JSON format: [X][Y][Z] where each Z is a string of characters
     * Output: [layer=Y][row=X][col=Z][depth=characters] for StructureDefinition compatibility
     */
    private static String[][][][] parsePattern(JsonArray patternArray) {
        // JSON is in [X][Y][Z] format, we need to convert to [Y][X][Z][characters]
        int xPositions = patternArray.size(); // Number of X positions
        if (xPositions == 0) return new String[0][0][0][0];
        
        JsonArray firstX = patternArray.get(0).getAsJsonArray();
        int yLayers = firstX.size(); // Number of Y layers
        if (yLayers == 0) return new String[0][0][0][0];
        
        JsonArray firstY = firstX.get(0).getAsJsonArray();
        int zStrings = firstY.size(); // Number of Z strings
        if (zStrings == 0) return new String[0][0][0][0];
        
        // Create array in final format [Y][X][Z][characters]
        String[][][][] pattern = new String[yLayers][xPositions][zStrings][];
        
        // Iterate through JSON [X][Y][Z] and reorganize to [Y][X][Z][characters]
        for (int x = 0; x < xPositions; x++) {
            JsonArray xArray = patternArray.get(x).getAsJsonArray();
            for (int y = 0; y < yLayers; y++) {
                JsonArray yArray = xArray.get(y).getAsJsonArray();
                for (int z = 0; z < zStrings; z++) {
                    String cellValue = yArray.get(z).getAsString();
                    // Split string into individual characters for multiple Z positions
                    pattern[y][x][z] = cellValue.split("");
                }
            }
        }
        
        return pattern;
    }

    /**
     * Parses key of block definitions
     */
    private static Map<String, List<StructureDefinition.BlockDefinition>> parseKey(JsonObject keyJson) {
        Map<String, List<StructureDefinition.BlockDefinition>> key = new HashMap<>();
        
        for (String keyChar : keyJson.keySet()) {
            JsonElement keyElement = keyJson.get(keyChar);
            List<StructureDefinition.BlockDefinition> blockDefs = new ArrayList<>();
            String groupDisplayName = null;
            
            if (keyElement.isJsonObject()) {
                // New format with display and alternatives
                JsonObject keyObject = keyElement.getAsJsonObject();
                
                if (keyObject.has("display")) {
                    groupDisplayName = keyObject.get("display").getAsString();
                }
                
                if (keyObject.has("alternatives") && keyObject.get("alternatives").isJsonArray()) {
                    JsonArray alternativesArray = keyObject.getAsJsonArray("alternatives");
                    parseAlternatives(alternativesArray, blockDefs, groupDisplayName);
                }
            } else if (keyElement.isJsonArray()) {
                // Legacy format - direct array
                JsonArray blockDefArray = keyElement.getAsJsonArray();
                parseAlternatives(blockDefArray, blockDefs, groupDisplayName);
            }
            
            key.put(keyChar, blockDefs);
        }
        
        return key;
    }
    
    /**
     * Helper to parse block alternatives
     */
    private static void parseAlternatives(JsonArray alternativesArray, List<StructureDefinition.BlockDefinition> blockDefs, String groupDisplayName) {
        boolean isFirstBlock = true;
        
        for (JsonElement blockDefElement : alternativesArray) {
            JsonObject blockDefJson = blockDefElement.getAsJsonObject();
            StructureDefinition.BlockDefinition blockDef = new StructureDefinition.BlockDefinition();
            
            // First block in group gets the group display name
            if (isFirstBlock && groupDisplayName != null) {
                blockDef.setDisplay(groupDisplayName);
                isFirstBlock = false;
            }
            
            if (blockDefJson.has("block")) {
                blockDef.setBlock(blockDefJson.get("block").getAsString());
            }
            
            // Parse properties
            if (blockDefJson.has("properties") && blockDefJson.get("properties").isJsonObject()) {
                JsonObject propsJson = blockDefJson.getAsJsonObject("properties");
                Map<String, String> properties = new HashMap<>();
                for (String propKey : propsJson.keySet()) {
                    properties.put(propKey, propsJson.get(propKey).getAsString());
                }
                blockDef.setProperties(properties);
            }
            
            blockDefs.add(blockDef);
        }
    }

    /**
     * Gets a structure definition by ID
     */
    public static StructureDefinition getStructure(String id) {
        return STRUCTURES.get(id);
    }

    /**
     * Gets all structure definitions sorted by ID
     */
    public static Map<String, StructureDefinition> getAllStructures() {
        return STRUCTURES.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        java.util.LinkedHashMap::new
                ));
    }

    /**
     * Reloads all structure definitions
     */
    public static void reloadAllDefinitions() {
        LOGGER.info("Reloading all structure definitions...");
        scanConfigDirectory();
    }

    /**
     * Gets the list of available structure IDs sorted alphabetically
     */
    public static List<String> getAvailableStructureIds() {
        return STRUCTURES.keySet().stream()
                .sorted()
                .collect(java.util.stream.Collectors.toList());
    }
} 