package net.unfamily.iskautils.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.unfamily.iskautils.data.load.IskaUtilsLoadJson;
import net.unfamily.iskautils.data.load.IskaUtilsLoadPaths;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads potion plate definitions from datapack JSON under {@code load/iska_utils_plates/} (any namespace).
 * The older {@code potion_plates/} resource tree is not read; migrate files to {@code load/iska_utils_plates/}.
 */
public class DynamicPotionPlateScanner {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    // Map of discovered plate configurations with merge support
    private static final Map<String, PotionPlateConfig> DISCOVERED_CONFIGS = new HashMap<>();
    
    /**
     * Loads potion plate configs from {@code data/<namespace>/load/iska_utils_plates/}.
     * Called during mod initialization (with null RM) and may be refreshed when the server reloads datapacks.
     */
    public static void loadAll(ResourceManager resourceManagerOrNull) {
        DISCOVERED_CONFIGS.clear();
        Map<Identifier, JsonElement> merged = resourceManagerOrNull != null
                ? IskaUtilsLoadJson.collectMergedJson(resourceManagerOrNull,
                id -> IskaUtilsLoadPaths.isJsonUnderLoadSubdir(id, IskaUtilsLoadPaths.PLATES))
                : IskaUtilsLoadJson.collectFromModJarOnly(IskaUtilsLoadPaths.PLATES);
        for (var e : IskaUtilsLoadJson.orderedEntries(merged)) {
            if (!e.getValue().isJsonObject()) {
                continue;
            }
            parseConfigFile(e.getKey().toString(), e.getValue().getAsJsonObject());
        }
        LOGGER.info("Potion plate configurations loaded: {}", DISCOVERED_CONFIGS.size());
    }

    public static void scanConfigDirectory() {
        loadAll(null);
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
            
            // If existing config is not overwritable, it cannot be replaced
            if (!existingConfig.isOverwritable()) {
                LOGGER.info("Skipping configuration for plate {}: existing config is not overwritable, keeping existing", plateId);
                return;
            }
            
            // If new config is not overwritable, it should replace the existing (which is overwritable)
            // and be protected from future overwrites
            if (!config.isOverwritable()) {
                DISCOVERED_CONFIGS.put(plateId, config);
                LOGGER.debug("Replaced configuration for plate {}: new config is not overwritable, replacing existing overwritable config", plateId);
                return;
            }
            
            // Both are overwritable - new one replaces existing
            DISCOVERED_CONFIGS.put(plateId, config);
            LOGGER.debug("Replaced configuration for plate {}: both configs are overwritable, replaced with new config", plateId);
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
            boolean creativeTabVisible = json.has("creative_tab") ? json.get("creative_tab").getAsBoolean() : true;
            boolean playerShiftDisable = json.has("player_shift_disable") ? json.get("player_shift_disable").getAsBoolean() : true;
            
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
            
            PotionPlateConfig config = new PotionPlateConfig(plateId, effectId, amplifier, duration, delay, affectsPlayers, affectsMobs, hideParticles, arrayOverwritable);
            config.setCreativeTabVisible(creativeTabVisible);
            config.setPlayerShiftDisable(playerShiftDisable);
            // Optional tooltip_lines field (default 0)
            int tooltipLines = json.has("tooltip_lines") ? json.get("tooltip_lines").getAsInt() : 0;
            config.setTooltipLines(tooltipLines);
            return config;
            
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
            boolean creativeTabVisible = json.has("creative_tab") ? json.get("creative_tab").getAsBoolean() : true;
            boolean playerShiftDisable = json.has("player_shift_disable") ? json.get("player_shift_disable").getAsBoolean() : true;
            
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
            
            PotionPlateConfig config = new PotionPlateConfig(plateId, damageType, damageAmount, delay, affectsPlayers, affectsMobs, arrayOverwritable);
            config.setCreativeTabVisible(creativeTabVisible);
            config.setPlayerShiftDisable(playerShiftDisable);
            // Optional tooltip_lines field (default 0)
            int tooltipLines = json.has("tooltip_lines") ? json.get("tooltip_lines").getAsInt() : 0;
            config.setTooltipLines(tooltipLines);
            return config;
            
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
            boolean creativeTabVisible = json.has("creative_tab") ? json.get("creative_tab").getAsBoolean() : true;
            boolean playerShiftDisable = json.has("player_shift_disable") ? json.get("player_shift_disable").getAsBoolean() : true;
            
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
            PotionPlateConfig config;
            switch (applyType.toLowerCase()) {
                case "fire":
                    config = new PotionPlateConfig(plateId, duration, delay, affectsPlayers, affectsMobs, arrayOverwritable);
                    break;
                case "freeze":
                    config = PotionPlateConfig.createFreezePlate(plateId, duration, delay, affectsPlayers, affectsMobs, arrayOverwritable);
                    break;
                default:
                    LOGGER.error("Unknown special apply type: {}", applyType);
                    return null;
            }
            
            // Apply additional settings
            config.setCreativeTabVisible(creativeTabVisible);
            config.setPlayerShiftDisable(playerShiftDisable);
            // Optional tooltip_lines field (default 0)
            int tooltipLines = json.has("tooltip_lines") ? json.get("tooltip_lines").getAsInt() : 0;
            config.setTooltipLines(tooltipLines);
            return config;
            
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
    
} 