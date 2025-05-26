package net.unfamily.iskautils.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.unfamily.iskautils.IskaUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Loads potion plate configurations from datapack JSON files
 */
public class PotionPlateLoader implements PreparableReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    // Map of plate ID to configuration
    private static final Map<String, PotionPlateConfig> LOADED_CONFIGS = new HashMap<>();
    
    @Override
    public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, 
                                         ProfilerFiller preparationProfiler, ProfilerFiller reloadProfiler, 
                                         Executor backgroundExecutor, Executor gameExecutor) {
        
        return CompletableFuture.supplyAsync(() -> {
            Map<ResourceLocation, JsonElement> foundConfigs = new HashMap<>();
            
            // Search for potion_plates in ALL namespaces
            for (String namespace : resourceManager.getNamespaces()) {
                Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                    "potion_plates", 
                    location -> location.getPath().endsWith(".json")
                );
                
                for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                    ResourceLocation location = entry.getKey();
                    
                    // Only process if it's in the current namespace
                    if (!location.getNamespace().equals(namespace)) {
                        continue;
                    }
                    
                    try (InputStreamReader reader = new InputStreamReader(entry.getValue().open())) {
                        JsonElement jsonElement = GSON.fromJson(reader, JsonElement.class);
                        if (jsonElement != null) {
                            foundConfigs.put(location, jsonElement);
                            LOGGER.debug("Found potion plate config: {}", location);
                        }
                    } catch (IOException e) {
                        LOGGER.error("Error reading potion plate config {}: {}", location, e.getMessage());
                    }
                }
            }
            
            LOGGER.info("Found {} potion plate configurations across all namespaces", foundConfigs.size());
            return foundConfigs;
            
        }, backgroundExecutor).thenCompose(preparationBarrier::wait).thenAcceptAsync(foundConfigs -> {
            // Clear previous virtual configurations
            LOADED_CONFIGS.clear();
            int loadedCount = 0;
            int errorCount = 0;
            
            LOGGER.info("Loading potion plate configurations...");
            
            for (Map.Entry<ResourceLocation, JsonElement> entry : foundConfigs.entrySet()) {
                ResourceLocation resourceLocation = entry.getKey();
                JsonElement jsonElement = entry.getValue();
                
                try {
                    // Extract plate ID from resource location path
                    String plateId = resourceLocation.getPath().replace("potion_plates/", "").replace(".json", "");
                    
                    // Parse the JSON configuration
                    PotionPlateConfig config = parseConfig(plateId, jsonElement.getAsJsonObject());
                    
                    if (config != null && config.isValid()) {
                        LOADED_CONFIGS.put(plateId, config);
                        
                        // Register as virtual block (for external datapacks like KubeJS)
                        PotionPlateRegistry.registerPotionPlateDynamic(plateId, config);
                        LOGGER.info("Registered virtual potion plate: {}", plateId);
                        
                        loadedCount++;
                        LOGGER.debug("Loaded potion plate config from {}: {}", resourceLocation, config);
                    } else {
                        LOGGER.error("Invalid potion plate configuration in file: {}", resourceLocation);
                        errorCount++;
                    }
                    
                } catch (Exception e) {
                    LOGGER.error("Error loading potion plate configuration from {}: {}", resourceLocation, e.getMessage());
                    if (LOGGER.isDebugEnabled()) {
                        e.printStackTrace();
                    }
                    errorCount++;
                }
            }
            
            LOGGER.info("Loaded {} potion plate configurations successfully, {} errors", loadedCount, errorCount);
            
            // Initialize resource provider after loading configs (skip if templates missing)
            try {
                PotionPlateResourceProvider.initialize(resourceManager);
                PotionPlateResourceProvider.generateResources();
            } catch (Exception e) {
                LOGGER.warn("Resource provider initialization failed (templates missing): {}", e.getMessage());
            }
            
        }, gameExecutor);
    }
    
    /**
     * Parses a JSON object into a PotionPlateConfig
     */
    private PotionPlateConfig parseConfig(String defaultPlateId, JsonObject json) {
        try {
            // Required type field for datapack compatibility
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
            int duration = json.has("duration") ? json.get("duration").getAsInt() : 200; // 10 seconds default
            boolean affectsPlayers = json.has("affects_players") ? json.get("affects_players").getAsBoolean() : true;
            boolean affectsMobs = json.has("affects_mobs") ? json.get("affects_mobs").getAsBoolean() : true;
            boolean hideParticles = json.has("hide_particles") ? json.get("hide_particles").getAsBoolean() : false;
            
            // Validate values
            if (amplifier < 0) {
                LOGGER.warn("Negative amplifier {} in config {}, setting to 0", amplifier, plateId);
                amplifier = 0;
            }
            
            if (duration < 60) {
                LOGGER.warn("Duration {} ticks ({} seconds) in config {} is below minimum of 3 seconds. Setting to 60 ticks for proper effect functionality.", 
                    duration, duration / 20.0f, plateId);
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
     * Validates if a string is a valid ResourceLocation path
     * Valid characters: a-z, 0-9, _, -, .
     */
    private boolean isValidResourceLocationPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        
        // Check each character
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
    private boolean isValidResourceLocationChar(char c) {
        return (c >= 'a' && c <= 'z') ||  // lowercase letters
               (c >= '0' && c <= '9') ||  // numbers
               c == '_' ||                // underscore
               c == '-' ||                // hyphen
               c == '.';                  // dot
    }
    
    /**
     * Gets a required string field from JSON
     */
    private String getRequiredString(JsonObject json, String fieldName) {
        if (!json.has(fieldName)) {
            throw new JsonParseException("Missing required field: " + fieldName);
        }
        
        JsonElement element = json.get(fieldName);
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new JsonParseException("Field " + fieldName + " must be a string");
        }
        
        String value = element.getAsString();
        if (value.isEmpty()) {
            throw new JsonParseException("Field " + fieldName + " cannot be empty");
        }
        
        return value;
    }
    
    /**
     * Gets all loaded configurations
     */
    public static Map<String, PotionPlateConfig> getLoadedConfigs() {
        return new HashMap<>(LOADED_CONFIGS);
    }
    
    /**
     * Gets a specific configuration by plate ID
     */
    public static PotionPlateConfig getConfig(String plateId) {
        return LOADED_CONFIGS.get(plateId);
    }
    
    /**
     * Checks if a configuration exists for the given plate ID
     */
    public static boolean hasConfig(String plateId) {
        return LOADED_CONFIGS.containsKey(plateId);
    }
    
    /**
     * Gets the number of loaded configurations
     */
    public static int getLoadedCount() {
        return LOADED_CONFIGS.size();
    }
} 