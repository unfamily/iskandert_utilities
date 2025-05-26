package net.unfamily.iskautils.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
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
 * Scans datapack files during mod initialization to discover potion plates
 * that need to be registered before the RegisterEvent fires
 */
public class DynamicPotionPlateScanner {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    // Map of discovered plate configurations
    private static final Map<String, PotionPlateConfig> DISCOVERED_CONFIGS = new HashMap<>();
    
    /**
     * Scans for potion plate configurations in the mod's resources
     * This is called during mod initialization, before RegisterEvent
     */
    public static void scanForPotionPlates() {
        LOGGER.info("Scanning for dynamic potion plate configurations...");
        
        try {
            // First, scan for iska_utils namespace configurations in mod resources
            scanModResources();
            
            // In development environment, also scan the source directory
            scanDevelopmentResources();
            
            LOGGER.info("Found {} potion plate configurations to register dynamically", DISCOVERED_CONFIGS.size());
            
        } catch (Exception e) {
            LOGGER.error("Error scanning for potion plate configurations: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Scans external scripts directory for potion plate configurations
     * This is called during mod initialization, before RegisterEvent
     */
    public static void scanConfigDirectory() {
        LOGGER.info("Scanning external scripts directory for potion plate configurations...");
        
        try {
            // Get the configured external scripts path
            String externalScriptsBasePath = net.unfamily.iskautils.Config.externalScriptsPath;
            if (externalScriptsBasePath == null || externalScriptsBasePath.trim().isEmpty()) {
                externalScriptsBasePath = "kubejs/external_scripts"; // fallback default
            }
            
            // Create external scripts directory if it doesn't exist
            Path configPath = Paths.get(externalScriptsBasePath, "potion_plates");
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath);
                LOGGER.info("Created external scripts directory: {}", configPath.toAbsolutePath());
                
                // Create a README file to explain the directory
                createConfigReadme(configPath, externalScriptsBasePath);
                return;
            }
            
            if (!Files.isDirectory(configPath)) {
                LOGGER.warn("External scripts path exists but is not a directory: {}", configPath);
                return;
            }
            
            LOGGER.info("Scanning external scripts directory: {}", configPath.toAbsolutePath());
            
            // Scan all JSON files in the external scripts directory recursively
            try (Stream<Path> files = Files.walk(configPath)) {
                files.filter(Files::isRegularFile)
                     .filter(path -> path.toString().endsWith(".json"))
                     .filter(path -> !path.getFileName().toString().startsWith("."))
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
     * Creates a README file in the external scripts directory to explain its purpose
     */
    private static void createConfigReadme(Path configPath, String externalScriptsBasePath) {
        try {
            Path readmePath = configPath.resolve("README.md");
            String readmeContent = "# Iska Utils - Dynamic Potion Plates Configuration\n" +
                "\n" +
                "This directory allows you to create custom potion plates that will be registered as real blocks in the game.\n" +
                "\n" +
                "## Location: " + externalScriptsBasePath + "/potion_plates/\n" +
                "\n" +
                "This directory is organized under KubeJS external scripts to keep all custom configurations in one place,\n" +
                "avoiding cluttering the config directory with hundreds of mod-specific folders.\n" +
                "\n" +
                "You can configure the base path in the mod's config file (iska_utils-common.toml):\n" +
                "external_scripts_path = \"" + externalScriptsBasePath + "\"\n" +
                "\n" +
                "## How to use:\n" +
                "\n" +
                "1. Create JSON files in this directory (or subdirectories)\n" +
                "2. Each JSON file represents one potion plate configuration\n" +
                "3. The filename (without .json) will be used as the plate ID\n" +
                "4. Restart the game to load new configurations\n" +
                "\n" +
                "## JSON Format:\n" +
                "\n" +
                "```json\n" +
                "{\n" +
                "  \"type\": \"iska_utils:potion_plate\",\n" +
                "  \"effect\": \"minecraft:speed\",\n" +
                "  \"amplifier\": 1,\n" +
                "  \"duration\": 300,\n" +
                "  \"affects_players\": true,\n" +
                "  \"affects_mobs\": false,\n" +
                "  \"hide_particles\": false\n" +
                "}\n" +
                "```\n" +
                "\n" +
                "## Fields:\n" +
                "\n" +
                "- `type`: Must be \"iska_utils:potion_plate\"\n" +
                "- `effect`: The potion effect ID (e.g., \"minecraft:speed\", \"minecraft:slowness\")\n" +
                "- `amplifier`: Effect amplifier (0 = level I, 1 = level II, etc.) [optional, default: 0]\n" +
                "- `duration`: Effect duration in ticks (20 ticks = 1 second) [optional, default: 200]\n" +
                "- `affects_players`: Whether the plate affects players [optional, default: true]\n" +
                "- `affects_mobs`: Whether the plate affects mobs [optional, default: true]\n" +
                "- `hide_particles`: Whether to hide potion particles [optional, default: false]\n" +
                "\n" +
                "## Examples:\n" +
                "\n" +
                "### Speed Plate (speed_boost.json):\n" +
                "```json\n" +
                "{\n" +
                "  \"type\": \"iska_utils:potion_plate\",\n" +
                "  \"effect\": \"minecraft:speed\",\n" +
                "  \"amplifier\": 2,\n" +
                "  \"duration\": 600,\n" +
                "  \"affects_players\": true,\n" +
                "  \"affects_mobs\": false\n" +
                "}\n" +
                "```\n" +
                "\n" +
                "### Healing Plate (healing_pad.json):\n" +
                "```json\n" +
                "{\n" +
                "  \"type\": \"iska_utils:potion_plate\",\n" +
                "  \"effect\": \"minecraft:instant_health\",\n" +
                "  \"amplifier\": 1,\n" +
                "  \"duration\": 1,\n" +
                "  \"affects_players\": true,\n" +
                "  \"affects_mobs\": true,\n" +
                "  \"hide_particles\": true\n" +
                "}\n" +
                "```\n" +
                "\n" +
                "## Organization Tips:\n" +
                "\n" +
                "- You can create subdirectories to organize plates by category\n" +
                "- Example: `combat/`, `utility/`, `movement/`, etc.\n" +
                "- All JSON files will be scanned recursively\n" +
                "\n" +
                "## Notes:\n" +
                "\n" +
                "- Plate IDs must be unique across all files\n" +
                "- Invalid configurations will be logged and skipped\n" +
                "- Changes require a game restart\n" +
                "- Plates will appear in the Iska Utils creative tab\n" +
                "- Use `/give @p iska_utils:<plate_id>` to get plates in-game\n" +
                "- This system works independently of KubeJS scripts\n";
            
            Files.writeString(readmePath, readmeContent);
            LOGGER.info("Created README file: {}", readmePath);
            
        } catch (Exception e) {
            LOGGER.warn("Could not create README file: {}", e.getMessage());
        }
    }
    
    /**
     * Scans mod resources for iska_utils potion plate configurations
     */
    private static void scanModResources() {
        try {
            ClassLoader classLoader = DynamicPotionPlateScanner.class.getClassLoader();
            
            // Try to load known iska_utils configurations first
            String[] knownConfigs = {
                "slowness_trap.json",
                "poison_trap.json", 
                "weakness_trap.json"
            };
            
            for (String configFile : knownConfigs) {
                String resourcePath = "data/iska_utils/potion_plates/" + configFile;
                try (InputStream inputStream = classLoader.getResourceAsStream(resourcePath)) {
                    if (inputStream != null) {
                        String plateId = configFile.replace(".json", "");
                        PotionPlateConfig config = parseConfigFromStream(plateId, inputStream);
                        if (config != null && config.isValid()) {
                            DISCOVERED_CONFIGS.put(plateId, config);
                            LOGGER.debug("Loaded iska_utils potion plate config: {} -> {}", configFile, plateId);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Error loading iska_utils config {}: {}", configFile, e.getMessage());
                }
            }
            
            LOGGER.debug("Mod resources scanning completed. Found {} iska_utils configurations", DISCOVERED_CONFIGS.size());
            
        } catch (Exception e) {
            LOGGER.warn("Error scanning mod resources: {}", e.getMessage());
        }
    }
    
    /**
     * Scans development resources (only works in development environment)
     */
    private static void scanDevelopmentResources() {
        try {
            // Try to scan the development resources directory
            Path resourcesPath = Paths.get("src/main/resources/data/iska_utils/potion_plates");
            
            if (Files.exists(resourcesPath) && Files.isDirectory(resourcesPath)) {
                LOGGER.info("Scanning development resources directory: {}", resourcesPath);
                
                try (Stream<Path> files = Files.walk(resourcesPath)) {
                    files.filter(Files::isRegularFile)
                         .filter(path -> path.toString().endsWith(".json"))
                         .filter(path -> !path.getFileName().toString().equals("README.md"))
                         .filter(path -> !path.getFileName().toString().startsWith("."))
                         .forEach(DynamicPotionPlateScanner::scanConfigFile);
                }
                
                LOGGER.info("Development scanning completed. Found {} configurations", DISCOVERED_CONFIGS.size());
            } else {
                LOGGER.debug("Development resources directory not found: {}", resourcesPath);
            }
            
        } catch (Exception e) {
            LOGGER.debug("Development resources not available (normal in production): {}", e.getMessage());
        }
    }
    
    /**
     * Scans a single configuration file
     */
    private static void scanConfigFile(Path configFile) {
        try {
            String plateId = extractPlateIdFromPath(configFile.toString());
            
            try (InputStream inputStream = Files.newInputStream(configFile)) {
                PotionPlateConfig config = parseConfigFromStream(plateId, inputStream);
                if (config != null && config.isValid()) {
                    DISCOVERED_CONFIGS.put(plateId, config);
                    LOGGER.debug("Discovered potion plate config from file: {} -> {}", configFile, plateId);
                }
            }
            
        } catch (Exception e) {
            LOGGER.warn("Error scanning config file {}: {}", configFile, e.getMessage());
        }
    }
    
    /**
     * Parses a configuration from an input stream
     */
    private static PotionPlateConfig parseConfigFromStream(String defaultPlateId, InputStream inputStream) {
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            JsonElement jsonElement = GSON.fromJson(reader, JsonElement.class);
            if (jsonElement != null && jsonElement.isJsonObject()) {
                return parseConfig(defaultPlateId, jsonElement.getAsJsonObject());
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing config for {}: {}", defaultPlateId, e.getMessage());
        }
        return null;
    }
    
    /**
     * Parses a JSON object into a PotionPlateConfig (simplified version of PotionPlateLoader logic)
     */
    private static PotionPlateConfig parseConfig(String defaultPlateId, JsonObject json) {
        try {
            // Required type field
            String type = getRequiredString(json, "type");
            if (!"iska_utils:potion_plate".equals(type)) {
                LOGGER.error("Invalid type '{}' in config {}. Expected 'iska_utils:potion_plate'", type, defaultPlateId);
                return null;
            }
            
            // Required fields
            String effectId = getRequiredString(json, "effect");
            
            // Optional plate ID - if not specified, use the filename
            String plateId = json.has("id") ? getRequiredString(json, "id") : defaultPlateId;
            
            // Validate plate ID format
            if (!isValidResourceLocationPath(plateId)) {
                LOGGER.error("Invalid plate ID '{}' in config {}. IDs must contain only lowercase letters, numbers, underscore, hyphen, and dots.", plateId, defaultPlateId);
                return null;
            }
            
            // Optional fields with defaults
            int amplifier = json.has("amplifier") ? json.get("amplifier").getAsInt() : 0;
            int duration = json.has("duration") ? json.get("duration").getAsInt() : 200;
            boolean affectsPlayers = json.has("affects_players") ? json.get("affects_players").getAsBoolean() : true;
            boolean affectsMobs = json.has("affects_mobs") ? json.get("affects_mobs").getAsBoolean() : true;
            boolean hideParticles = json.has("hide_particles") ? json.get("hide_particles").getAsBoolean() : false;
            
            // Validate values
            if (amplifier < 0) {
                amplifier = 0;
            }
            if (duration < 60) {
                duration = 60;
            }
            if (!affectsPlayers && !affectsMobs) {
                LOGGER.error("Config {} affects neither players nor mobs, this is invalid", plateId);
                return null;
            }
            
            return new PotionPlateConfig(plateId, effectId, amplifier, duration, affectsPlayers, affectsMobs, hideParticles);
            
        } catch (Exception e) {
            LOGGER.error("Error parsing config for plate {}: {}", defaultPlateId, e.getMessage());
            return null;
        }
    }
    
    /**
     * Extracts plate ID from file path
     */
    private static String extractPlateIdFromPath(String path) {
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        if (fileName.endsWith(".json")) {
            fileName = fileName.substring(0, fileName.length() - 5);
        }
        return fileName;
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
} 