package net.unfamily.iskautils.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.unfamily.iskautils.Config;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Stream;

/**
 * Main loader for the custom shop system
 */
public class ShopLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    private static final Map<String, ShopValute> VALUTES = new HashMap<>();
    private static final Map<String, ShopCategory> CATEGORIES = new HashMap<>();
    private static final Map<String, ShopEntry> ENTRIES = new HashMap<>();
    
    private static final Map<String, Boolean> PROTECTED_VALUTES = new HashMap<>();
    private static final Map<String, Boolean> PROTECTED_CATEGORIES = new HashMap<>();
    private static final Map<String, Boolean> PROTECTED_ENTRIES = new HashMap<>();
    
    public static void scanConfigDirectory() {
        LOGGER.info("Scanning configuration directory for shop system...");
        
        try {
            String externalScriptsBasePath = Config.externalScriptsPath;
            if (externalScriptsBasePath == null || externalScriptsBasePath.trim().isEmpty()) {
                externalScriptsBasePath = "kubejs/external_scripts";
            }
            
            Path configPath = Paths.get(externalScriptsBasePath, "iska_utils_shop");
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath);
                LOGGER.info("Created directory for shop system: {}", configPath.toAbsolutePath());
                createReadme(configPath);
                generateDefaultConfigurations(configPath);
                return;
            }
            
            if (!Files.isDirectory(configPath)) {
                LOGGER.warn("Shop path exists but is not a directory: {}", configPath);
                return;
            }
            
            LOGGER.info("Scanning shop directory: {}", configPath.toAbsolutePath());
            
            // Always regenerate README
            createReadme(configPath);
            LOGGER.info("Updated README.md");
            
            // Check and regenerate default files if needed
            checkAndRegenerateDefaultFiles(configPath);
            
            VALUTES.clear();
            CATEGORIES.clear();
            ENTRIES.clear();
            PROTECTED_VALUTES.clear();
            PROTECTED_CATEGORIES.clear();
            PROTECTED_ENTRIES.clear();
            
            try (Stream<Path> files = Files.walk(configPath)) {
                files.filter(Files::isRegularFile)
                     .filter(path -> path.toString().endsWith(".json"))
                     .filter(path -> !path.getFileName().toString().startsWith("."))
                     .sorted()
                     .forEach(ShopLoader::scanConfigFile);
            }
            
            LOGGER.info("Shop directory scan completed. Configurations found: {} valutes, {} categories, {} entries", 
                       VALUTES.size(), CATEGORIES.size(), ENTRIES.size());
            
        } catch (Exception e) {
            LOGGER.error("Error during shop directory scan: {}", e.getMessage());
        }
    }
    
    private static void scanConfigFile(Path filePath) {
        try {
            LOGGER.debug("Scanning file: {}", filePath.getFileName());
            
            try (InputStream inputStream = Files.newInputStream(filePath);
                 InputStreamReader reader = new InputStreamReader(inputStream)) {
                
                JsonElement jsonElement = GSON.fromJson(reader, JsonElement.class);
                if (jsonElement == null || !jsonElement.isJsonObject()) {
                    LOGGER.warn("File {} does not contain valid JSON", filePath.getFileName());
                    return;
                }
                
                JsonObject json = jsonElement.getAsJsonObject();
                String type = json.has("type") ? json.get("type").getAsString() : "";
                boolean overwritable = json.has("overwritable") ? json.get("overwritable").getAsBoolean() : false;
                
                switch (type) {
                    case "shop_valute":
                        processValutesFile(json, overwritable, filePath.getFileName().toString());
                        break;
                    case "shop_category":
                        processCategoriesFile(json, overwritable, filePath.getFileName().toString());
                        break;
                    case "shop_entry":
                        processEntriesFile(json, overwritable, filePath.getFileName().toString());
                        break;
                    default:
                        LOGGER.debug("File {} ignored - unrecognized type: {}", filePath.getFileName(), type);
                        break;
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error scanning file {}: {}", filePath.getFileName(), e.getMessage());
        }
    }
    
    private static void processValutesFile(JsonObject json, boolean overwritable, String fileName) {
        if (!json.has("valutes") || !json.get("valutes").isJsonArray()) {
            LOGGER.warn("File {} does not contain valid 'valutes' array", fileName);
            return;
        }
        
        JsonArray valutesArray = json.get("valutes").getAsJsonArray();
        for (JsonElement element : valutesArray) {
            if (!element.isJsonObject()) continue;
            
            JsonObject valuteObj = element.getAsJsonObject();
            String id = valuteObj.has("id") ? valuteObj.get("id").getAsString() : null;
            
            if (id == null || id.trim().isEmpty()) {
                LOGGER.warn("Valute without valid ID found in {}", fileName);
                continue;
            }
            
            if (PROTECTED_VALUTES.containsKey(id) && PROTECTED_VALUTES.get(id)) {
                LOGGER.debug("Valute {} protected, ignoring override from {}", id, fileName);
                continue;
            }
            
            ShopValute valute = new ShopValute();
            valute.id = id;
            valute.name = valuteObj.has("name") ? valuteObj.get("name").getAsString() : id;
            valute.charSymbol = valuteObj.has("char_symbol") ? valuteObj.get("char_symbol").getAsString() : "§";
            
            VALUTES.put(id, valute);
            PROTECTED_VALUTES.put(id, !overwritable);
            
            LOGGER.debug("Loaded valute: {} ({})", id, valute.name);
        }
    }
    
    private static void processCategoriesFile(JsonObject json, boolean overwritable, String fileName) {
        if (!json.has("categories") || !json.get("categories").isJsonArray()) {
            LOGGER.warn("File {} does not contain valid 'categories' array", fileName);
            return;
        }
        
        JsonArray categoriesArray = json.get("categories").getAsJsonArray();
        for (JsonElement element : categoriesArray) {
            if (!element.isJsonObject()) continue;
            
            JsonObject categoryObj = element.getAsJsonObject();
            String id = categoryObj.has("id") ? categoryObj.get("id").getAsString() : null;
            
            if (id == null || id.trim().isEmpty()) {
                LOGGER.warn("Category without valid ID found in {}", fileName);
                continue;
            }
            
            if (PROTECTED_CATEGORIES.containsKey(id) && PROTECTED_CATEGORIES.get(id)) {
                LOGGER.debug("Category {} protected, ignoring override from {}", id, fileName);
                continue;
            }
            
            ShopCategory category = new ShopCategory();
            category.id = id;
            category.name = categoryObj.has("name") ? categoryObj.get("name").getAsString() : id;
            category.description = categoryObj.has("description") ? categoryObj.get("description").getAsString() : "";
            category.item = categoryObj.has("item") ? categoryObj.get("item").getAsString() : "minecraft:stone";
            
            CATEGORIES.put(id, category);
            PROTECTED_CATEGORIES.put(id, !overwritable);
            
            LOGGER.debug("Loaded category: {} ({})", id, category.name);
        }
    }
    
    private static void processEntriesFile(JsonObject json, boolean overwritable, String fileName) {
        if (!json.has("entries") || !json.get("entries").isJsonArray()) {
            LOGGER.warn("File {} does not contain valid 'entries' array", fileName);
            return;
        }
        
        JsonArray entriesArray = json.get("entries").getAsJsonArray();
        for (JsonElement element : entriesArray) {
            if (!element.isJsonObject()) continue;
            
            JsonObject entryObj = element.getAsJsonObject();
            String item = entryObj.has("item") ? entryObj.get("item").getAsString() : null;
            String category = entryObj.has("in_category") ? entryObj.get("in_category").getAsString() : null;
            
            if (item == null || item.trim().isEmpty()) {
                LOGGER.warn("Entry without valid item found in {}", fileName);
                continue;
            }
            
            // Usa solo l'ID base dell'item per la chiave, senza data components
            String baseItemId = extractBaseItemId(item);
            String entryKey = category != null ? category + ":" + baseItemId : baseItemId;
            
            if (PROTECTED_ENTRIES.containsKey(entryKey) && PROTECTED_ENTRIES.get(entryKey)) {
                LOGGER.debug("Entry {} protected, ignoring override from {}", entryKey, fileName);
                continue;
            }
            
            ShopEntry entry = new ShopEntry();
            entry.inCategory = category;
            entry.item = item;
            entry.itemCount = entryObj.has("item_count") ? entryObj.get("item_count").getAsInt() : 1;
            entry.valute = entryObj.has("valute") ? entryObj.get("valute").getAsString() : null;
            entry.buy = entryObj.has("buy") ? entryObj.get("buy").getAsDouble() : 0.0;
            entry.sell = entryObj.has("sell") ? entryObj.get("sell").getAsDouble() : 0.0;
            
            if (entryObj.has("stages") && entryObj.get("stages").isJsonArray()) {
                JsonArray stagesArray = entryObj.get("stages").getAsJsonArray();
                entry.stages = new ShopStage[stagesArray.size()];
                
                for (int i = 0; i < stagesArray.size(); i++) {
                    JsonElement stageElement = stagesArray.get(i);
                    if (stageElement.isJsonObject()) {
                        JsonObject stageObj = stageElement.getAsJsonObject();
                        ShopStage stage = new ShopStage();
                        stage.stage = stageObj.has("stage") ? stageObj.get("stage").getAsString() : "";
                        stage.stageType = stageObj.has("stage_type") ? stageObj.get("stage_type").getAsString() : "world";
                        stage.is = stageObj.has("is") ? stageObj.get("is").getAsBoolean() : true;
                        entry.stages[i] = stage;
                    }
                }
            }
            
            ENTRIES.put(entryKey, entry);
            PROTECTED_ENTRIES.put(entryKey, !overwritable);
            
            LOGGER.debug("Loaded entry: {} in category {}", item, category != null ? category : "default");
        }
    }
    
    private static void createReadme(Path configPath) {
        try {
            Path readmePath = configPath.resolve("README.md");
            
            String readmeContent = "# Iska Utils - Shop System\n\n" +
                "This directory contains configuration files for the custom shop system.\n\n" +
                "## File Structure\n\n" +
                "The shop system uses three types of files:\n\n" +
                "### 1. shop_valutes.json - Valutes\n" +
                "Defines available valutes in the shop system.\n\n" +
                "### 2. shop_category.json - Categories\n" +
                "Defines product categories in the shop.\n\n" +
                "### 3. shop_entry.json - Shop Entries\n" +
                "Defines specific products in the shop.\n\n" +
                "## Overwritable System\n\n" +
                "- If a file has `overwritable: false`, it cannot be overwritten by other files\n" +
                "- If a file has `overwritable: true`, it can be overwritten by files loaded later\n" +
                "- Files are processed in alphabetical order\n\n" +
                "## Subdirectory Search\n\n" +
                "The system automatically searches for files in all subdirectories of the shop directory.\n\n" +
                "---\n" +
                "Generated by Iska Utils";
            
            Files.write(readmePath, readmeContent.getBytes());
            LOGGER.info("Created README.md in {}", readmePath);
            
        } catch (IOException e) {
            LOGGER.error("Error creating README.md: {}", e.getMessage());
        }
    }
    
    private static void generateDefaultConfigurations(Path configPath) {
        try {
            ShopDefaultGenerator.generateDefaultValutes(configPath);
            ShopDefaultGenerator.generateDefaultCategories(configPath);
            ShopDefaultGenerator.generateDefaultEntries(configPath);
            LOGGER.info("Generated default configurations for shop system");
        } catch (Exception e) {
            LOGGER.error("Error generating default configurations: {}", e.getMessage());
        }
    }
    
    /**
     * Checks if default files should be regenerated and regenerates them if needed
     */
    private static void checkAndRegenerateDefaultFiles(Path configPath) {
        try {
            // Check default valutes file
            Path defaultValutesFile = configPath.resolve("default_valutes.json");
            if (!Files.exists(defaultValutesFile)) {
                LOGGER.info("Generating default_valutes.json file");
                ShopDefaultGenerator.generateDefaultValutes(configPath);
            } else if (shouldRegenerateDefaultFile(defaultValutesFile)) {
                LOGGER.info("Regenerating default_valutes.json file (overwritable: true)");
                ShopDefaultGenerator.generateDefaultValutes(configPath);
            }
            
            // Check default categories file
            Path defaultCategoriesFile = configPath.resolve("default_categories.json");
            if (!Files.exists(defaultCategoriesFile)) {
                LOGGER.info("Generating default_categories.json file");
                ShopDefaultGenerator.generateDefaultCategories(configPath);
            } else if (shouldRegenerateDefaultFile(defaultCategoriesFile)) {
                LOGGER.info("Regenerating default_categories.json file (overwritable: true)");
                ShopDefaultGenerator.generateDefaultCategories(configPath);
            }
            
            // Check default entries file
            Path defaultEntriesFile = configPath.resolve("default_entries.json");
            if (!Files.exists(defaultEntriesFile)) {
                LOGGER.info("Generating default_entries.json file");
                ShopDefaultGenerator.generateDefaultEntries(configPath);
            } else if (shouldRegenerateDefaultFile(defaultEntriesFile)) {
                LOGGER.info("Regenerating default_entries.json file (overwritable: true)");
                ShopDefaultGenerator.generateDefaultEntries(configPath);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error checking/regenerating default files: {}", e.getMessage());
        }
    }
    
    /**
     * Checks if a default file should be regenerated based on its overwritable flag
     */
    private static boolean shouldRegenerateDefaultFile(Path filePath) {
        try {
            try (InputStream inputStream = Files.newInputStream(filePath);
                 InputStreamReader reader = new InputStreamReader(inputStream)) {
                
                JsonElement jsonElement = GSON.fromJson(reader, JsonElement.class);
                if (jsonElement != null && jsonElement.isJsonObject()) {
                    JsonObject json = jsonElement.getAsJsonObject();
                    
                    // Check if the overwritable field exists and is true
                    if (json.has("overwritable")) {
                        boolean overwritable = json.get("overwritable").getAsBoolean();
                        if (overwritable) {
                            LOGGER.debug("Found {} with overwritable: true, will regenerate", filePath.getFileName());
                            return true;
                        } else {
                            LOGGER.debug("Found {} with overwritable: false, will not regenerate", filePath.getFileName());
                            return false;
                        }
                    }
                    
                    // If no overwritable field, default to true (regenerate)
                    LOGGER.debug("Found {} without overwritable field, assuming true", filePath.getFileName());
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error reading {} file: {}", filePath.getFileName(), e.getMessage());
        }
        
        // If the file can't be read or isn't valid JSON, regenerate it
        return true;
    }
    
    public static void reloadAllConfigurations() {
        LOGGER.info("Reloading all shop configurations...");
        
        // Semplicemente ricarica tutto da zero - nessuna protezione delle entry in memoria
        // Il flag 'overwritable' serve solo per la rigenerazione automatica dei file fisici
        scanConfigDirectory();
        
        LOGGER.info("Shop configurations reload completed");
    }
    
    // Getter methods
    public static Map<String, ShopValute> getValutes() {
        return new HashMap<>(VALUTES);
    }
    
    public static Map<String, ShopCategory> getCategories() {
        return new HashMap<>(CATEGORIES);
    }
    
    public static Map<String, ShopEntry> getEntries() {
        return new HashMap<>(ENTRIES);
    }
    
    public static ShopValute getValute(String id) {
        return VALUTES.get(id);
    }
    
    public static ShopCategory getCategory(String id) {
        return CATEGORIES.get(id);
    }
    
    public static ShopEntry getEntry(String category, String item) {
        String baseItemId = extractBaseItemId(item);
        String key = category != null ? category + ":" + baseItemId : baseItemId;
        return ENTRIES.get(key);
    }
    
    /**
     * Estrae l'ID base di un item rimuovendo i data components
     * Es: "minecraft:diamond_sword[enchantments={...}]" -> "minecraft:diamond_sword"
     */
    private static String extractBaseItemId(String itemString) {
        if (itemString == null) return null;
        
        int bracketIndex = itemString.indexOf('[');
        if (bracketIndex != -1) {
            return itemString.substring(0, bracketIndex);
        }
        return itemString;
    }
} 