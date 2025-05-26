package net.unfamily.iskautils.data;

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
 * Scans external configuration files for potion plates that need to be registered
 * Now supports array format with overwritable parameters
 */
public class DynamicPotionPlateScanner {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    // Map of discovered plate configurations with merge support
    private static final Map<String, PotionPlateConfig> DISCOVERED_CONFIGS = new HashMap<>();
    
    /**
     * Scans external scripts directory for potion plate configurations
     * This is called during mod initialization, before RegisterEvent
     */
    public static void scanConfigDirectory() {
        LOGGER.info("Scanning external scripts directory for iska utils plate configurations...");
        
        try {
            // Get the configured external scripts path
            String externalScriptsBasePath = net.unfamily.iskautils.Config.externalScriptsPath;
            if (externalScriptsBasePath == null || externalScriptsBasePath.trim().isEmpty()) {
                externalScriptsBasePath = "kubejs/external_scripts"; // fallback default
            }
            
            // Create external scripts directory if it doesn't exist
            Path configPath = Paths.get(externalScriptsBasePath, "iska_utils_plates");
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath);
                LOGGER.info("Created external scripts directory: {}", configPath.toAbsolutePath());
                
                // Create a README file to explain the directory
                createConfigReadme(configPath, externalScriptsBasePath);
                
                // Generate default configuration files
                generateDefaultConfigurations(configPath);
                return;
            }
            
            if (!Files.isDirectory(configPath)) {
                LOGGER.warn("External scripts path exists but is not a directory: {}", configPath);
                return;
            }
            
            LOGGER.info("Scanning external scripts directory: {}", configPath.toAbsolutePath());
            
            // Always check and regenerate README if missing or outdated
            Path readmePath = configPath.resolve("README.md");
            if (!Files.exists(readmePath) || !isReadmeUpToDate(readmePath)) {
                LOGGER.info("README.md missing or outdated, regenerating...");
                createConfigReadme(configPath, externalScriptsBasePath);
            }
            
            // Check if iska_utils.json exists and if it's overwritable
            Path iskaUtilsFile = configPath.resolve("iska_utils.json");
            if (Files.exists(iskaUtilsFile)) {
                if (shouldRegenerateIskaUtils(iskaUtilsFile)) {
                    LOGGER.info("iska_utils.json has overwritable=true, regenerating with defaults...");
                    generateIskaUtilsPlates(configPath);
                }
            } else {
                // If iska_utils.json doesn't exist, generate it
                LOGGER.info("iska_utils.json not found, generating default configuration...");
                generateIskaUtilsPlates(configPath);
            }
            
            // Clear previous configurations
            DISCOVERED_CONFIGS.clear();
            
            // Scan all JSON files in the external scripts directory recursively
            // Files are processed in alphabetical order for consistent override behavior
            try (Stream<Path> files = Files.walk(configPath)) {
                files.filter(Files::isRegularFile)
                     .filter(path -> path.toString().endsWith(".json"))
                     .filter(path -> !path.getFileName().toString().startsWith("."))
                     .sorted() // Ensure consistent processing order
                     .forEach(DynamicPotionPlateScanner::scanConfigFile);
            }
            
            LOGGER.info("External scripts directory scanning completed. Total configurations found: {}", DISCOVERED_CONFIGS.size());
            
        } catch (Exception e) {
            LOGGER.error("Error scanning external scripts directory: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Checks if the README file is up to date by looking for a version marker
     */
    private static boolean isReadmeUpToDate(Path readmePath) {
        try {
            String content = Files.readString(readmePath);
            // Check for version marker - if it contains the current format markers, it's up to date
            return content.contains("## Array Format:") && 
                   content.contains("- `delay`: Delay between applications in ticks") &&
                   content.contains("## Overwritable System:");
        } catch (Exception e) {
            LOGGER.debug("Error reading README file for version check: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks if iska_utils.json should be regenerated based on its overwritable flag
     */
    private static boolean shouldRegenerateIskaUtils(Path iskaUtilsFile) {
        try {
            try (InputStream inputStream = Files.newInputStream(iskaUtilsFile);
                 InputStreamReader reader = new InputStreamReader(inputStream)) {
                
                JsonElement jsonElement = GSON.fromJson(reader, JsonElement.class);
                if (jsonElement != null && jsonElement.isJsonObject()) {
                    JsonObject json = jsonElement.getAsJsonObject();
                    
                    // Check if overwritable field exists and is true
                    if (json.has("overwritable")) {
                        return json.get("overwritable").getAsBoolean();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error reading iska_utils.json for overwritable check: {}", e.getMessage());
        }
        
        // Default to false if we can't read the file or field doesn't exist
        return false;
    }
    
    /**
     * Creates a README file in the external scripts directory to explain its purpose
     */
    private static void createConfigReadme(Path configPath, String externalScriptsBasePath) {
        try {
            Path readmePath = configPath.resolve("README.md");
            String readmeContent = "# Iska Utils - Dynamic Plates Configuration\n" +
                "\n" +
                "This directory allows you to create custom plates that will be registered as real blocks in the game.\n" +
                "\n" +
                "## Location: " + externalScriptsBasePath + "/iska_utils_plates/\n" +
                "\n" +
                "## Supported Plate Types:\n" +
                "\n" +
                "### 1. Effect Plates (Potion Effects)\n" +
                "```json\n" +
                "{\n" +
                "  \"plate_type\": \"effect\",\n" +
                "  \"id\": \"iska_utils-slowness\",\n" +
                "  \"effect\": \"minecraft:slowness\",\n" +
                "  \"amplifier\": 0,\n" +
                "  \"duration\": 100,\n" +
                "  \"delay\": 40,\n" +
                "  \"affects_players\": true,\n" +
                "  \"affects_mobs\": true,\n" +
                "  \"hide_particles\": true\n" +
                "}\n" +
                "```\n" +
                "\n" +
                "### 2. Damage Plates\n" +
                "```json\n" +
                "{\n" +
                "  \"plate_type\": \"damage\",\n" +
                "  \"id\": \"iska_utils-damage\",\n" +
                "  \"damage_type\": \"minecraft:cactus\",\n" +
                "  \"damage\": 2.0,\n" +
                "  \"delay\": 20,\n" +
                "  \"affects_players\": true,\n" +
                "  \"affects_mobs\": true\n" +
                "}\n" +
                "```\n" +
                "\n" +
                "### 3. Special Plates (Fire, Freeze, etc.)\n" +
                "```json\n" +
                "{\n" +
                "  \"plate_type\": \"special\",\n" +
                "  \"id\": \"iska_utils-fire\",\n" +
                "  \"apply\": \"fire\",\n" +
                "  \"duration\": 100,\n" +
                "  \"delay\": 40,\n" +
                "  \"affects_players\": true,\n" +
                "  \"affects_mobs\": true\n" +
                "}\n" +
                "```\n" +
                "\n" +
                "```json\n" +
                "{\n" +
                "  \"plate_type\": \"special\",\n" +
                "  \"id\": \"iska_utils-freeze\",\n" +
                "  \"apply\": \"freeze\",\n" +
                "  \"duration\": 100,\n" +
                "  \"delay\": 40,\n" +
                "  \"affects_players\": true,\n" +
                "  \"affects_mobs\": true\n" +
                "}\n" +
                "```\n" +
                "\n" +
                "## Array Format:\n" +
                "\n" +
                "```json\n" +
                "{\n" +
                "  \"type\": \"iska_utils:plates\",\n" +
                "  \"overwritable\": true,\n" +
                "  \"plates\": [\n" +
                "    {\n" +
                "      \"plate_type\": \"effect\",\n" +
                "      \"id\": \"iska_utils-slowness\",\n" +
                "      \"effect\": \"minecraft:slowness\",\n" +
                "      \"amplifier\": 0,\n" +
                "      \"duration\": 100,\n" +
                "      \"delay\": 40,\n" +
                "      \"affects_players\": true,\n" +
                "      \"affects_mobs\": true,\n" +
                "      \"hide_particles\": true\n" +
                "    },\n" +
                "    {\n" +
                "      \"plate_type\": \"damage\",\n" +
                "      \"id\": \"iska_utils-damage\",\n" +
                "      \"damage_type\": \"minecraft:cactus\",\n" +
                "      \"damage\": 2.0,\n" +
                "      \"delay\": 20,\n" +
                "      \"affects_players\": true,\n" +
                "      \"affects_mobs\": true\n" +
                "    },\n" +
                "    {\n" +
                "      \"plate_type\": \"special\",\n" +
                "      \"id\": \"iska_utils-fire\",\n" +
                "      \"apply\": \"fire\",\n" +
                "      \"duration\": 100,\n" +
                "      \"delay\": 40,\n" +
                "      \"affects_players\": true,\n" +
                "      \"affects_mobs\": true\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "```\n" +
                "\n" +
                "## Common Fields:\n" +
                "\n" +
                "- `type`: Must be **\"iska_utils:plates\"**\n" +
                "- `overwritable`: Whether ALL plates in this file can be overwritten [**required**]\n" +
                "- `plates`: Array of plate configurations\n" +
                "- `plate_type`: Type of plate (\"effect\", \"damage\", \"special\")\n" +
                "- `id`: Unique identifier for the plate [**required**]\n" +
                "- `delay`: Delay between applications in ticks [optional]\n" +
                "  - Effect/Special plates: minimum 40 ticks (2 seconds), default 40\n" +
                "  - Damage plates: minimum 1 tick, default 20 (1 second)\n" +
                "- `affects_players`: Whether affects players [optional, default: true]\n" +
                "- `affects_mobs`: Whether affects mobs [optional, default: true]\n" +
                "\n" +
                "## Effect Plate Fields:\n" +
                "- `effect`: The potion effect ID [**required**]\n" +
                "- `amplifier`: Effect amplifier (0 = level I) [optional, default: 0]\n" +
                "- `duration`: Effect duration in ticks [optional, default: 100]\n" +
                "- `hide_particles`: Whether to hide particles [optional, default: false]\n" +
                "\n" +
                "## Damage Plate Fields:\n" +
                "- `damage_type`: Damage source type [**required**]\n" +
                "  - Examples: \"minecraft:cactus\", \"minecraft:magic\", \"minecraft:generic\", \"minecraft:player_attack\"\n" +
                "- `damage`: Damage amount (hearts) [**required**]\n" +
                "\n" +
                "## Special Plate Fields:\n" +
                "- `apply`: Special effect type [**required**]\n" +
                "  - \"fire\": Sets entity on fire\n" +
                "  - \"freeze\": Freezes entity (powder snow effect)\n" +
                "- `duration`: Effect duration in ticks [**required**]\n" +
                "\n" +
                "## Overwritable System:\n" +
                "\n" +
                "- Files are processed in alphabetical order\n" +
                "- If `overwritable: true`, ALL plates in this file CAN BE overwritten by later files\n" +
                "- If `overwritable: false`, ALL plates in this file CANNOT BE overwritten by later files\n" +
                "- When a plate with the same ID is found in multiple files:\n" +
                "  - If the existing plate has `overwritable: true`, it gets replaced by the new plate\n" +
                "  - If the existing plate has `overwritable: false`, the new plate gets skipped and existing remains\n" +
                "\n" +
                "## Notes:\n" +
                "\n" +
                "- **Plate IDs must be unique** across all files\n" +
                "- **Changes require a game restart**\n" +
                "- Use `/give @p iska_utils:<plate_id>` to get plates in-game\n";
            
            Files.writeString(readmePath, readmeContent);
            LOGGER.info("Created README file: {}", readmePath);
            
        } catch (Exception e) {
            LOGGER.warn("Failed to create README file: {}", e.getMessage());
        }
    }
    
    /**
     * Scans a single configuration file for potion plates
     */
    private static void scanConfigFile(Path configFile) {
        try {
            LOGGER.debug("Scanning config file: {}", configFile);
            
            try (InputStream inputStream = Files.newInputStream(configFile)) {
                parseConfigFromStream(configFile.toString(), inputStream);
            }
            
        } catch (Exception e) {
            LOGGER.warn("Error scanning config file {}: {}", configFile, e.getMessage());
        }
    }
    
    /**
     * Parses a configuration from an input stream
     */
    private static void parseConfigFromStream(String filePath, InputStream inputStream) {
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            JsonElement jsonElement = GSON.fromJson(reader, JsonElement.class);
            if (jsonElement != null && jsonElement.isJsonObject()) {
                parseConfigFile(filePath, jsonElement.getAsJsonObject());
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing config file {}: {}", filePath, e.getMessage());
        }
    }
    
    /**
     * Parses a configuration file JSON object
     */
    private static void parseConfigFile(String filePath, JsonObject json) {
        try {
            // Check for required type field
            String type = getRequiredString(json, "type");
            
            // Support both old and new formats for backward compatibility
            boolean isNewFormat = "iska_utils:plates".equals(type);
            boolean isOldFormat = "iska_utils:potion_plates".equals(type);
            
            if (!isNewFormat && !isOldFormat) {
                LOGGER.error("Invalid type '{}' in config file {}. Expected 'iska_utils:plates' or 'iska_utils:potion_plates'", type, filePath);
                return;
            }
            
            // Get the overwritable flag at array level (new format)
            boolean arrayOverwritable = json.has("overwritable") ? json.get("overwritable").getAsBoolean() : true;
            
            // Get the plates array (new format) or potion_plates array (old format)
            String arrayFieldName = isNewFormat ? "plates" : "potion_plates";
            if (!json.has(arrayFieldName) || !json.get(arrayFieldName).isJsonArray()) {
                LOGGER.error("Missing or invalid '{}' array in config file {}", arrayFieldName, filePath);
                return;
            }
            
            JsonArray platesArray = json.getAsJsonArray(arrayFieldName);
            int processedCount = 0;
            int errorCount = 0;
            
            for (JsonElement element : platesArray) {
                if (!element.isJsonObject()) {
                    LOGGER.warn("Skipping non-object element in {} array in file {}", arrayFieldName, filePath);
                    errorCount++;
                    continue;
                }
                
                try {
                    PotionPlateConfig config = parseConfig(element.getAsJsonObject(), isNewFormat, arrayOverwritable);
                    if (config != null && config.isValid()) {
                        processConfig(config);
                        processedCount++;
                    } else {
                        LOGGER.error("Invalid plate configuration in file {}", filePath);
                        errorCount++;
                    }
                } catch (Exception e) {
                    LOGGER.error("Error parsing plate in file {}: {}", filePath, e.getMessage());
                    errorCount++;
                }
            }
            
            LOGGER.info("Processed file {}: {} configurations loaded, {} errors", filePath, processedCount, errorCount);
            
        } catch (Exception e) {
            LOGGER.error("Error parsing config file {}: {}", filePath, e.getMessage());
        }
    }
    
    /**
     * Processes a single configuration, handling merging with existing configs
     */
    private static void processConfig(PotionPlateConfig config) {
        String plateId = config.getPlateId();
        
        if (DISCOVERED_CONFIGS.containsKey(plateId)) {
            PotionPlateConfig existingConfig = DISCOVERED_CONFIGS.get(plateId);
            
            // If existing config is overwritable, it can be replaced by the new one
            if (existingConfig.isOverwritable()) {
                DISCOVERED_CONFIGS.put(plateId, config);
                LOGGER.debug("Replaced configuration for plate {}: existing config was overwritable, replaced with new config", plateId);
            } else {
                // Keep existing configuration - existing config is not overwritable
                LOGGER.info("Skipping configuration for plate {}: existing config is not overwritable, keeping existing", plateId);
            }
        } else {
            // New configuration - always add it regardless of overwritable flag
            DISCOVERED_CONFIGS.put(plateId, config);
            LOGGER.debug("Added new configuration for plate {}", plateId);
        }
    }
    
    /**
     * Parses a JSON object into a PotionPlateConfig
     */
    private static PotionPlateConfig parseConfig(JsonObject json, boolean isNewFormat, boolean arrayOverwritable) {
        try {
            // Get plate type (required in new format)
            String plateTypeStr = json.has("plate_type") ? getRequiredString(json, "plate_type") : "effect";
            PotionPlateType plateType = PotionPlateType.fromString(plateTypeStr);
            
            // Required plate ID
            String plateId = json.has("id") ? getRequiredString(json, "id") : null;
            
            // Common optional fields with defaults
            boolean affectsPlayers = json.has("affects_players") ? json.get("affects_players").getAsBoolean() : true;
            boolean affectsMobs = json.has("affects_mobs") ? json.get("affects_mobs").getAsBoolean() : true;
            
            // Validate common fields
            if (!affectsPlayers && !affectsMobs) {
                LOGGER.error("Config {} affects neither players nor mobs, this is invalid", plateId);
                return null;
            }
            
            // Parse based on plate type
            switch (plateType) {
                case EFFECT:
                    return parseEffectPlate(json, plateId, arrayOverwritable, affectsPlayers, affectsMobs);
                case DAMAGE:
                    return parseDamagePlate(json, plateId, arrayOverwritable, affectsPlayers, affectsMobs);
                case SPECIAL:
                    return parseSpecialPlate(json, plateId, arrayOverwritable, affectsPlayers, affectsMobs);
                default:
                    LOGGER.error("Unknown plate type: {}", plateTypeStr);
                    return null;
            }
            
        } catch (Exception e) {
            LOGGER.error("Error parsing config: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Parses an effect plate configuration
     */
    private static PotionPlateConfig parseEffectPlate(JsonObject json, String plateId, boolean arrayOverwritable, 
                                                     boolean affectsPlayers, boolean affectsMobs) {
        try {
            // Required effect field
            String effectId = getRequiredString(json, "effect");
            
            // Generate plate ID if not specified
            if (plateId == null) {
                plateId = generatePlateId(effectId);
            }
            
            // Validate plate ID format
            if (!isValidResourceLocationPath(plateId)) {
                LOGGER.error("Invalid plate ID '{}'. IDs must contain only lowercase letters, numbers, underscore, hyphen, and dots.", plateId);
                return null;
            }
            
            // Optional fields with defaults
            int amplifier = json.has("amplifier") ? json.get("amplifier").getAsInt() : 0;
            int duration = json.has("duration") ? json.get("duration").getAsInt() : 100;
            int delay = json.has("delay") ? json.get("delay").getAsInt() : 40; // Default 2 seconds
            boolean hideParticles = json.has("hide_particles") ? json.get("hide_particles").getAsBoolean() : false;
            
            // Validate values
            if (amplifier < 0) {
                amplifier = 0;
            }
            if (duration < 60) {
                duration = 60;
            }
            if (delay < 40) {
                LOGGER.warn("Delay {} ticks for effect plate {} is below minimum of 40 ticks (2 seconds). Setting to 40.", delay, plateId);
                delay = 40;
            }
            
            return new PotionPlateConfig(plateId, effectId, amplifier, duration, delay, affectsPlayers, affectsMobs, hideParticles, arrayOverwritable);
            
        } catch (Exception e) {
            LOGGER.error("Error parsing effect plate: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Parses a damage plate configuration
     */
    private static PotionPlateConfig parseDamagePlate(JsonObject json, String plateId, boolean arrayOverwritable,
                                                     boolean affectsPlayers, boolean affectsMobs) {
        try {
            // Required damage fields
            String damageType = getRequiredString(json, "damage_type");
            float damageAmount = json.get("damage").getAsFloat();
            
            // Optional delay field with default
            int delay = json.has("delay") ? json.get("delay").getAsInt() : 20; // Default 1 second for damage plates
            
            // Generate plate ID if not specified
            if (plateId == null) {
                plateId = "iska_utils-damage";
            }
            
            // Validate plate ID format
            if (!isValidResourceLocationPath(plateId)) {
                LOGGER.error("Invalid plate ID '{}'. IDs must contain only lowercase letters, numbers, underscore, hyphen, and dots.", plateId);
                return null;
            }
            
            // Validate values
            if (damageAmount <= 0) {
                LOGGER.error("Damage amount must be positive, got: {}", damageAmount);
                return null;
            }
            if (delay < 1) {
                LOGGER.warn("Delay {} ticks for damage plate {} is below minimum of 1 tick. Setting to 1.", delay, plateId);
                delay = 1;
            }
            
            return new PotionPlateConfig(plateId, damageType, damageAmount, delay, affectsPlayers, affectsMobs, arrayOverwritable);
            
        } catch (Exception e) {
            LOGGER.error("Error parsing damage plate: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Parses a special plate configuration (fire, freeze, etc.)
     */
    private static PotionPlateConfig parseSpecialPlate(JsonObject json, String plateId, boolean arrayOverwritable,
                                                      boolean affectsPlayers, boolean affectsMobs) {
        try {
            // Required special fields
            String applyType = getRequiredString(json, "apply");
            
            // Duration is required for all special plates
            if (!json.has("duration")) {
                throw new RuntimeException("Missing required field: duration");
            }
            int duration = json.get("duration").getAsInt();
            
            // Optional delay field with default
            int delay = json.has("delay") ? json.get("delay").getAsInt() : 40; // Default 2 seconds for special plates
            
            // Ensure delay is at least 40 ticks (2 seconds) for all special plates
            if (delay < 40) {
                LOGGER.warn("Delay {} ticks for special plate {} is below minimum of 40 ticks (2 seconds). Setting to 40.", delay, plateId);
                delay = 40;
            }
            
            // Generate plate ID if not specified
            if (plateId == null) {
                plateId = "iska_utils-" + applyType;
            }
            
            // Validate plate ID format
            if (!isValidResourceLocationPath(plateId)) {
                LOGGER.error("Invalid plate ID '{}'. IDs must contain only lowercase letters, numbers, underscore, hyphen, and dots.", plateId);
                return null;
            }
            
            // Validate duration for all special types
            if (duration <= 0) {
                LOGGER.error("Duration must be positive for special plates, got: {}", duration);
                return null;
            }
            
            // Create appropriate special plate based on apply type
            switch (applyType.toLowerCase()) {
                case "fire":
                    return new PotionPlateConfig(plateId, duration, delay, affectsPlayers, affectsMobs, arrayOverwritable);
                case "freeze":
                    return PotionPlateConfig.createFreezePlate(plateId, duration, delay, affectsPlayers, affectsMobs, arrayOverwritable);
                default:
                    LOGGER.error("Unknown special apply type: {}", applyType);
                    return null;
            }
            
        } catch (Exception e) {
            LOGGER.error("Error parsing special plate: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Generates a plate ID from an effect ID
     */
    private static String generatePlateId(String effectId) {
        // Convert "minecraft:slowness" to "iska_utils-slowness"
        String effectName = effectId.contains(":") ? effectId.split(":", 2)[1] : effectId;
        return "iska_utils-" + effectName;
    }
    
    /**
     * Validates if a string is a valid ResourceLocation path
     */
    private static boolean isValidResourceLocationPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (!isValidResourceLocationChar(c)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Checks if a character is valid for ResourceLocation paths
     */
    private static boolean isValidResourceLocationChar(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= '0' && c <= '9') ||
               c == '_' ||
               c == '-' ||
               c == '.';
    }
    
    /**
     * Gets a required string field from JSON
     */
    private static String getRequiredString(JsonObject json, String fieldName) {
        if (!json.has(fieldName)) {
            throw new RuntimeException("Missing required field: " + fieldName);
        }
        
        JsonElement element = json.get(fieldName);
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new RuntimeException("Field " + fieldName + " must be a string");
        }
        
        String value = element.getAsString();
        if (value.isEmpty()) {
            throw new RuntimeException("Field " + fieldName + " cannot be empty");
        }
        
        return value;
    }
    
    /**
     * Gets all discovered configurations
     */
    public static Map<String, PotionPlateConfig> getDiscoveredConfigs() {
        return new HashMap<>(DISCOVERED_CONFIGS);
    }
    
    /**
     * Checks if any configurations were discovered
     */
    public static boolean hasDiscoveredConfigs() {
        return !DISCOVERED_CONFIGS.isEmpty();
    }
    
    /**
     * Gets the number of discovered configurations
     */
    public static int getDiscoveredCount() {
        return DISCOVERED_CONFIGS.size();
    }
    
    /**
     * Clears all discovered configurations (for testing)
     */
    public static void clearDiscovered() {
        DISCOVERED_CONFIGS.clear();
    }
    
    /**
     * Generates default potion plate configurations based on the original internal configs
     */
    private static void generateDefaultConfigurations(Path configPath) {
        try {
            LOGGER.info("Generating default potion plate configurations...");
            
            // Generate single iska_utils configuration file
            generateIskaUtilsPlates(configPath);
            
            LOGGER.info("Default configuration files generated successfully");
            
        } catch (Exception e) {
            LOGGER.error("Failed to generate default configurations: {}", e.getMessage());
        }
    }
    
    /**
     * Generates the main iska_utils plates configuration file
     */
    private static void generateIskaUtilsPlates(Path configPath) throws IOException {
        String content = "{\n" +
            "  \"type\": \"iska_utils:plates\",\n" +
            "  \"overwritable\": true,\n" +
            "  \"plates\": [\n" +
            "    {\n" +
            "      \"plate_type\": \"effect\",\n" +
            "      \"id\": \"iska_utils-slowness\",\n" +
            "      \"effect\": \"minecraft:slowness\",\n" +
            "      \"amplifier\": 0,\n" +
            "      \"duration\": 100,\n" +
            "      \"delay\": 40,\n" +
            "      \"affects_players\": true,\n" +
            "      \"affects_mobs\": true,\n" +
            "      \"hide_particles\": true\n" +
            "    },\n" +
            "    {\n" +
            "      \"plate_type\": \"effect\",\n" +
            "      \"id\": \"iska_utils-weakness\",\n" +
            "      \"effect\": \"minecraft:weakness\",\n" +
            "      \"amplifier\": 0,\n" +
            "      \"duration\": 100,\n" +
            "      \"delay\": 40,\n" +
            "      \"affects_players\": true,\n" +
            "      \"affects_mobs\": true,\n" +
            "      \"hide_particles\": true\n" +
            "    },\n" +
            "    {\n" +
            "      \"plate_type\": \"effect\",\n" +
            "      \"id\": \"iska_utils-poison\",\n" +
            "      \"effect\": \"minecraft:poison\",\n" +
            "      \"amplifier\": 0,\n" +
            "      \"duration\": 100,\n" +
            "      \"delay\": 40,\n" +
            "      \"affects_players\": true,\n" +
            "      \"affects_mobs\": true,\n" +
            "      \"hide_particles\": false\n" +
            "    },\n" +
            "    {\n" +
            "      \"plate_type\": \"damage\",\n" +
            "      \"id\": \"iska_utils-damage\",\n" +
            "      \"damage_type\": \"minecraft:cactus\",\n" +
            "      \"damage\": 1.0,\n" +
            "      \"delay\": 10,\n" +
            "      \"affects_players\": true,\n" +
            "      \"affects_mobs\": true\n" +
            "    },\n" +
            "    {\n" +
            "      \"plate_type\": \"damage\",\n" +
            "      \"id\": \"iska_utils-improved_damage\",\n" +
            "      \"damage_type\": \"minecraft:player_attack\",\n" +
            "      \"damage\": 2.0,\n" +
            "      \"delay\": 10,\n" +
            "      \"affects_players\": true,\n" +
            "      \"affects_mobs\": true\n" +
            "    },\n" +
            "    {\n" +
            "      \"plate_type\": \"special\",\n" +
            "      \"id\": \"iska_utils-fire\",\n" +
            "      \"apply\": \"fire\",\n" +
            "      \"duration\": 100,\n" +
            "      \"delay\": 40,\n" +
            "      \"affects_players\": true,\n" +
            "      \"affects_mobs\": true\n" +
            "    },\n" +
            "    {\n" +
            "      \"plate_type\": \"special\",\n" +
            "      \"id\": \"iska_utils-freeze\",\n" +
            "      \"apply\": \"freeze\",\n" +
            "      \"duration\": 100,\n" +
            "      \"delay\": 40,\n" +
            "      \"affects_players\": true,\n" +
            "      \"affects_mobs\": true\n" +
            "    }\n" +
            "  ]\n" +
            "}";
        
        Path filePath = configPath.resolve("iska_utils.json");
        Files.writeString(filePath, content);
        LOGGER.info("Generated default iska_utils plates configuration: {}", filePath);
    }
} 