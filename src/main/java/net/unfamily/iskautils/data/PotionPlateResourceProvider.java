package net.unfamily.iskautils.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Provides dynamic models and blockstates for potion plates at runtime
 * This works in JAR files by generating JSON in memory
 */
public class PotionPlateResourceProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Cache for generated models
    private static final Map<ResourceLocation, JsonObject> GENERATED_MODELS = new HashMap<>();
    private static final Map<ResourceLocation, JsonObject> GENERATED_BLOCKSTATES = new HashMap<>();
    
    // Template cache
    private static JsonObject blockModelTemplate;
    private static JsonObject blockstateTemplate;
    private static JsonObject itemModelTemplate;
    
    /**
     * Initializes the resource provider by loading templates
     */
    public static void initialize(ResourceManager resourceManager) {
        try {
            blockModelTemplate = loadTemplate(resourceManager, "iska_utils:models/block/potion_plate_template.json");
            blockstateTemplate = loadTemplate(resourceManager, "iska_utils:blockstates/potion_plate_template.json");
            itemModelTemplate = loadTemplate(resourceManager, "iska_utils:models/item/potion_plate_template.json");
            
            LOGGER.info("Potion plate resource provider initialized");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize potion plate resource provider: {}", e.getMessage());
        }
    }
    
    /**
     * Generates and caches models for all loaded configurations
     */
    public static void generateResources() {
        Map<String, PotionPlateConfig> configs = PotionPlateLoader.getLoadedConfigs();
        
        if (configs.isEmpty()) {
            LOGGER.info("No potion plate configurations found, skipping resource generation");
            return;
        }
        
        LOGGER.info("Generating resources for {} potion plates", configs.size());
        
        for (Map.Entry<String, PotionPlateConfig> entry : configs.entrySet()) {
            String plateId = entry.getKey();
            PotionPlateConfig config = entry.getValue();
            
            try {
                generateResourcesForConfig(plateId, config);
            } catch (Exception e) {
                LOGGER.error("Failed to generate resources for potion plate {}: {}", plateId, e.getMessage());
            }
        }
        
        LOGGER.info("Resource generation completed. Generated {} models and {} blockstates", 
                   GENERATED_MODELS.size(), GENERATED_BLOCKSTATES.size());
    }
    
    /**
     * Generates resources for a single configuration
     */
    private static void generateResourcesForConfig(String plateId, PotionPlateConfig config) {
        String blockName = config.getRegistryBlockName();
        String textureName = config.getTextureNameWithoutExtension();
        
        // Generate block model
        JsonObject blockModel = blockModelTemplate.deepCopy();
        replaceInJson(blockModel, "TEXTURE_PLACEHOLDER", textureName);
        GENERATED_MODELS.put(ResourceLocation.fromNamespaceAndPath("iska_utils", "models/block/" + blockName + ".json"), blockModel);
        
        // Generate blockstate
        JsonObject blockstate = blockstateTemplate.deepCopy();
        replaceInJson(blockstate, "MODEL_PLACEHOLDER", blockName);
        GENERATED_BLOCKSTATES.put(ResourceLocation.fromNamespaceAndPath("iska_utils", "blockstates/" + blockName + ".json"), blockstate);
        
        // Generate item model (references the block model)
        JsonObject itemModel = itemModelTemplate.deepCopy();
        replaceInJson(itemModel, "MODEL_PLACEHOLDER", blockName);
        GENERATED_MODELS.put(ResourceLocation.fromNamespaceAndPath("iska_utils", "models/item/" + blockName + ".json"), itemModel);
        
        LOGGER.debug("Generated resources for potion plate: {} (block: {}, item: {}, texture: {})", 
                    plateId, blockName, blockName, textureName);
    }
    
    /**
     * Gets a generated model by resource location
     */
    public static Optional<JsonObject> getGeneratedModel(ResourceLocation location) {
        return Optional.ofNullable(GENERATED_MODELS.get(location));
    }
    
    /**
     * Gets a generated blockstate by resource location
     */
    public static Optional<JsonObject> getGeneratedBlockstate(ResourceLocation location) {
        return Optional.ofNullable(GENERATED_BLOCKSTATES.get(location));
    }
    
    /**
     * Checks if a resource is a generated potion plate resource
     */
    public static boolean isGeneratedResource(ResourceLocation location) {
        return GENERATED_MODELS.containsKey(location) || GENERATED_BLOCKSTATES.containsKey(location);
    }
    
    /**
     * Clears all generated resources (useful for reload)
     */
    public static void clearGenerated() {
        GENERATED_MODELS.clear();
        GENERATED_BLOCKSTATES.clear();
        LOGGER.debug("Cleared all generated potion plate resources");
    }
    
    /**
     * Loads a template JSON from resources
     */
    private static JsonObject loadTemplate(ResourceManager resourceManager, String path) throws IOException {
        ResourceLocation location = ResourceLocation.parse(path);
        Optional<Resource> resource = resourceManager.getResource(location);
        
        if (resource.isEmpty()) {
            throw new IOException("Template not found: " + path);
        }
        
        try (InputStream inputStream = resource.get().open();
             InputStreamReader reader = new InputStreamReader(inputStream)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
    
    /**
     * Recursively replaces placeholders in JSON
     */
    private static void replaceInJson(JsonObject json, String placeholder, String replacement) {
        json.entrySet().forEach(entry -> {
            if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsString().contains(placeholder)) {
                String newValue = entry.getValue().getAsString().replace(placeholder, replacement);
                entry.setValue(JsonParser.parseString("\"" + newValue + "\""));
            } else if (entry.getValue().isJsonObject()) {
                replaceInJson(entry.getValue().getAsJsonObject(), placeholder, replacement);
            } else if (entry.getValue().isJsonArray()) {
                entry.getValue().getAsJsonArray().forEach(element -> {
                    if (element.isJsonObject()) {
                        replaceInJson(element.getAsJsonObject(), placeholder, replacement);
                    }
                });
            }
        });
    }
} 