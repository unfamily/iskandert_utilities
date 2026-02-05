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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    
    private static final Map<String, ShopCurrency> CURRENCIES = new HashMap<>();
    private static final Map<String, ShopCategory> CATEGORIES = new HashMap<>();
    private static final Map<String, ShopEntry> ENTRIES = new HashMap<>();
    
    private static final Map<String, Boolean> PROTECTED_CURRENCIES = new HashMap<>();
    private static final Map<String, Boolean> PROTECTED_CATEGORIES = new HashMap<>();
    private static final Map<String, Boolean> PROTECTED_ENTRIES = new HashMap<>();
    
    public static void scanConfigDirectory() {

        
        try {
            String externalScriptsBasePath = Config.externalScriptsPath;
            if (externalScriptsBasePath == null || externalScriptsBasePath.trim().isEmpty()) {
                externalScriptsBasePath = "kubejs/external_scripts";
            }
            
            Path configPath = Paths.get(externalScriptsBasePath, "iska_utils_shop");
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath);

                createReadme(configPath);
                generateDefaultConfigurations(configPath);
                return;
            }
            
            if (!Files.isDirectory(configPath)) {
                LOGGER.warn("Shop path exists but is not a directory: {}", configPath);
                return;
            }
            

            
            // Always regenerate README
            createReadme(configPath);
            LOGGER.info("Updated README.md");
            
            // Check and regenerate default files if needed
            checkAndRegenerateDefaultFiles(configPath);
            
            CURRENCIES.clear();
            CATEGORIES.clear();
            ENTRIES.clear();
            PROTECTED_CURRENCIES.clear();
            PROTECTED_CATEGORIES.clear();
            PROTECTED_ENTRIES.clear();
            
            try (Stream<Path> files = Files.walk(configPath)) {
                files.filter(Files::isRegularFile)
                     .filter(path -> path.toString().endsWith(".json"))
                     .filter(path -> !path.getFileName().toString().startsWith("."))
                     .sorted()
                     .forEach(ShopLoader::scanConfigFile);
            }
            

            
        } catch (Exception e) {
            LOGGER.error("Error during shop directory scan: {}", e.getMessage());
        }
    }
    
    private static void scanConfigFile(Path filePath) {
        try {
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
                    case "shop_currency":
                    case "shop_valute": // backward compatibility
                        processCurrenciesFile(json, overwritable, filePath.getFileName().toString());
                        break;
                    case "shop_category":
                        processCategoriesFile(json, overwritable, filePath.getFileName().toString());
                        break;
                    case "shop_entry":
                        processEntriesFile(json, overwritable, filePath.getFileName().toString());
                        break;
                    default:
                        break;
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error scanning file {}: {}", filePath.getFileName(), e.getMessage());
        }
    }
    
    private static void processCurrenciesFile(JsonObject json, boolean overwritable, String fileName) {
        if (!json.has("currencies") || !json.get("currencies").isJsonArray()) {
            LOGGER.warn("File {} does not contain valid 'currencies' array", fileName);
            return;
        }
        
        JsonArray currenciesArray = json.get("currencies").getAsJsonArray();
        for (JsonElement element : currenciesArray) {
            if (!element.isJsonObject()) continue;
            
            JsonObject currencyObj = element.getAsJsonObject();
            String id = currencyObj.has("id") ? currencyObj.get("id").getAsString() : null;
            
            if (id == null || id.trim().isEmpty()) {
                LOGGER.warn("Currency without valid ID found in {}", fileName);
                continue;
            }
            
            if (PROTECTED_CURRENCIES.containsKey(id) && PROTECTED_CURRENCIES.get(id)) {
                continue;
            }
            
            ShopCurrency currency = new ShopCurrency();
            currency.id = id;
            currency.name = currencyObj.has("name") ? currencyObj.get("name").getAsString() : id;
            currency.charSymbol = currencyObj.has("char_symbol") ? currencyObj.get("char_symbol").getAsString() : "§";
            
            CURRENCIES.put(id, currency);
            PROTECTED_CURRENCIES.put(id, !overwritable);
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
                continue;
            }
            
            ShopCategory category = new ShopCategory();
            category.id = id;
            category.name = categoryObj.has("name") ? categoryObj.get("name").getAsString() : id;
            category.description = categoryObj.has("description") ? categoryObj.get("description").getAsString() : "";
            category.item = categoryObj.has("item") ? categoryObj.get("item").getAsString() : "minecraft:stone";
            category.priority = categoryObj.has("priority") ? categoryObj.get("priority").getAsInt() : 0;
            
            CATEGORIES.put(id, category);
            PROTECTED_CATEGORIES.put(id, !overwritable);
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
            String id = entryObj.has("id") ? entryObj.get("id").getAsString() : null;
            String item = entryObj.has("item") ? entryObj.get("item").getAsString() : null;
            String category = entryObj.has("in_category") ? entryObj.get("in_category").getAsString() : null;
            
            if (item == null || item.trim().isEmpty()) {
                LOGGER.warn("Entry without valid item found in {}", fileName);
                continue;
            }
            
            // If no specific ID, generate one automatically based on item and category
            String entryKey;
            if (id != null && !id.trim().isEmpty()) {
                entryKey = id.trim();
            } else {
                // Fallback: use old system for compatibility
                String baseItemId = extractBaseItemId(item);
                entryKey = category != null ? category + ":" + baseItemId : baseItemId;
                LOGGER.warn("Entry without ID found in {}, using fallback key: {}", fileName, entryKey);
            }
            
            if (PROTECTED_ENTRIES.containsKey(entryKey) && PROTECTED_ENTRIES.get(entryKey)) {
                continue;
            }
            
            ShopEntry entry = new ShopEntry();
            entry.id = entryKey;
            entry.inCategory = category;
            entry.item = item;
            entry.itemCount = entryObj.has("item_count") ? entryObj.get("item_count").getAsInt() : 1;
            // Support both currency and legacy valute fields
            if (entryObj.has("currency")) {
                entry.currency = entryObj.get("currency").getAsString();
                entry.valute = entry.currency; // For backward compatibility
            } else if (entryObj.has("valute")) {
                entry.valute = entryObj.get("valute").getAsString();
                entry.currency = entry.valute; // For forward compatibility
            } else {
                entry.currency = null;
                entry.valute = null;
            }
            entry.buy = entryObj.has("buy") ? entryObj.get("buy").getAsDouble() : 0.0;
            entry.sell = entryObj.has("sell") ? entryObj.get("sell").getAsDouble() : 0.0;
            entry.priority = entryObj.has("priority") ? entryObj.get("priority").getAsInt() : 0;
            entry.free = entryObj.has("free") && entryObj.get("free").getAsBoolean();
            
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
        }
    }
    
    private static void createReadme(Path configPath) {
        try {
            Path readmePath = configPath.resolve("README.md");
            
            String readmeContent = "# Iska Utils - Shop System\n\n" +
                "Complete shop system with team support, multiple currencies, categories and stages.\n\n" +
                "## Configuration Files\n\n" +
                "The system uses three types of JSON files:\n\n" +
                "### 1. shop_currency.json - Currencies\n" +
                "Defines available currencies in the shop system.\n\n" +
                "```json\n" +
                "{\n" +
                "  \"type\": \"shop_currency\",\n" +
                "  \"overwritable\": true,\n" +
                "  \"currencies\": [\n" +
                "    {\n" +
                "      \"id\": \"null_coin\",\n" +
                "      \"name\": \"shop.currency.null_coin\",\n" +
                "      \"char_symbol\": \"∅\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"emerald\",\n" +
                "      \"name\": \"shop.currency.emerald\",\n" +
                "      \"char_symbol\": \"💎\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "```\n\n" +
                "**Fields:**\n" +
                "- `id`: Unique identifier for the currency\n" +
                "- `name`: Translation key for the currency name\n" +
                "- `char_symbol`: Symbol displayed next to the name\n\n" +
                "### 2. shop_category.json - Categories\n" +
                "Defines product categories in the shop.\n\n" +
                "```json\n" +
                "{\n" +
                "  \"type\": \"shop_category\",\n" +
                "  \"overwritable\": true,\n" +
                "  \"categories\": [\n" +
                "    {\n" +
                "      \"id\": \"000_default\",\n" +
                "      \"name\": \"shop.category.default\",\n" +
                "      \"description\": \"shop.category.default.desc\",\n" +
                "      \"item\": \"minecraft:gold_nugget\",\n" +
                "      \"priority\": 0\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"tools\",\n" +
                "      \"name\": \"shop.category.tools\",\n" +
                "      \"description\": \"shop.category.tools.desc\",\n" +
                "      \"item\": \"minecraft:diamond_pickaxe\",\n" +
                "      \"priority\": 10\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "```\n\n" +
                "**Fields:**\n" +
                "- `id`: Unique identifier for the category\n" +
                "- `name`: Translation key for the category name\n" +
                "- `description`: Translation key for the category description\n" +
                "- `item`: Item ID used as icon for the category\n" +
                "- `priority`: Display order (optional, default 0). Higher value = category shown first.\n\n" +
                "### 3. shop_entry.json - Shop Products\n" +
                "Defines specific products available in the shop.\n\n" +
                "```json\n" +
                "{\n" +
                "  \"type\": \"shop_entry\",\n" +
                "  \"overwritable\": true,\n" +
                "  \"entries\": [\n" +
                "    {\n" +
                "      \"id\": \"bread_default\",\n" +
                "      \"in_category\": \"000_default\",\n" +
                "      \"item\": \"minecraft:bread\",\n" +
                "      \"item_count\": 1,\n" +
                "      \"currency\": \"null_coin\",\n" +
                "      \"buy\": 1.0,\n" +
                "      \"sell\": 0.5,\n" +
                "      \"priority\": 0,\n" +
                "      \"free\": false\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"diamond_pickaxe_silk\",\n" +
                "      \"in_category\": \"tools\",\n" +
                "      \"item\": \"minecraft:diamond_pickaxe[custom_name='\"Silky\"',repair_cost=1,enchantments={levels:{\"minecraft:silk_touch\":1}}]\",\n" +
                "      \"item_count\": 1,\n" +
                "      \"currency\": \"emerald\",\n" +
                "      \"buy\": 50.0,\n" +
                "      \"sell\": 25.0,\n" +
                "      \"priority\": 10,\n" +
                "      \"stages\": [\n" +
                "        {\n" +
                "          \"stage\": \"advanced_tools\",\n" +
                "          \"stage_type\": \"world\",\n" +
                "          \"is\": true\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "```\n\n" +
                "**Fields:**\n" +
                "- `id`: Unique identifier for the product\n" +
                "- `in_category`: Category ID this product belongs to\n" +
                "- `item`: Item ID (supports compound tags)\n" +
                "- `item_count`: Number of items in the stack\n" +
                "- `currency`: Currency ID for this product\n" +
                "- `buy`: Price to buy the item\n" +
                "- `sell`: Price to sell the item\n" +
                "- `priority`: Display order within category (optional, default 0). Higher value = entry shown first.\n" +
                "- `free`: If true, item can be bought at no cost even when buy is 0; Buy button is shown (optional, default false).\n" +
                "- `stages`: Array of required stages (optional)\n\n" +
                "## 🎯 Stage System\n\n" +
                "Stages allow unlocking products based on player/world progress.\n\n" +
                "```json\n" +
                "\"stages\": [\n" +
                "  {\n" +
                "    \"stage\": \"advanced_tools\",\n" +
                "    \"stage_type\": \"world\",\n" +
                "    \"is\": true\n" +
                "  },\n" +
                "  {\n" +
                "    \"stage\": \"vip_player\",\n" +
                "    \"stage_type\": \"player\",\n" +
                "    \"is\": true\n" +
                "  },\n" +
                "  {\n" +
                "    \"stage\": \"team_premium\",\n" +
                "    \"stage_type\": \"team\",\n" +
                "    \"is\": true\n" +
                "  }\n" +
                "]\n" +
                "```\n\n" +
                "**Stage Types:**\n" +
                "- `world`: World stage (all players)\n" +
                "- `player`: Player stage (single player)\n" +
                "- `team`: Team stage (all team members)\n\n" +
                "**Fields:**\n" +
                "- `stage`: Stage name\n" +
                "- `stage_type`: Stage type (world/player/team)\n" +
                "- `is`: true = stage must be active, false = stage must be inactive\n\n" +
                "## 🏷️ Compound Tag Support\n\n" +
                "The `item` field supports compound tags to specify items with custom properties.\n\n" +
                "### Compound Tag Examples:\n\n" +
                "**1. Enchanted Item:**\n" +
                "```json\n" +
                "\"item\": \"minecraft:diamond_pickaxe[enchantments={levels:{\"minecraft:silk_touch\":1,\"minecraft:efficiency\":5}}]\"\n" +
                "```\n\n" +
                "**2. Custom Named Item:**\n" +
                "```json\n" +
                "\"item\": \"minecraft:diamond_sword[custom_name='\\\"Excalibur\\\"',repair_cost=0]\"\n" +
                "```\n\n" +
                "**3. Damaged Item:**\n" +
                "```json\n" +
                "\"item\": \"minecraft:diamond_pickaxe[damage=100]\"\n" +
                "```\n\n" +
                "**4. Item with Lore:**\n" +
                "```json\n" +
                "\"item\": \"minecraft:diamond_helmet[custom_name='\\\"Magic Helmet\\\"',display={lore:['\\\"Special helmet\\\"','\\\"With magic powers\\\"']}]\"\n" +
                "```\n\n" +
                "**5. Colored Item:**\n" +
                "```json\n" +
                "\"item\": \"minecraft:leather_chestplate[display={color:16711680}]\"\n" +
                "```\n\n" +
                "**6. Complex NBT Item:**\n" +
                "```json\n" +
                "\"item\": \"minecraft:book[pages:['\\\"{\\\\\\\"text\\\\\\\":\\\\\\\"Magical spell\\\\\\\"}\\\"'],title:\\\"Spellbook\\\",author:\\\"Wizard\\\"]\"\n" +
                "```\n\n" +
                "## Overwritable System\n\n" +
                "- `overwritable: false` = File cannot be overwritten by other files\n" +
                "- `overwritable: true` = File can be overwritten by files loaded later\n" +
                "- Files are processed in alphabetical order\n\n" +
                "## Subdirectory Search\n\n" +
                "The system automatically searches for files in all subdirectories of the shop directory.\n\n" +
                "---\n" +
                "Generated by Iska Utils";
            
            // Force overwrite the README file always
            Files.write(readmePath, readmeContent.getBytes(), 
                       java.nio.file.StandardOpenOption.CREATE, 
                       java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
            
            LOGGER.info("Updated README.md in {}", readmePath);
            
        } catch (IOException e) {
            LOGGER.error("Error creating README.md: {}", e.getMessage());
        }
    }
    
    private static void generateDefaultConfigurations(Path configPath) {
        try {
            ShopDefaultGenerator.generateDefaultCurrencies(configPath);
            ShopDefaultGenerator.generateDefaultCategories(configPath);
            ShopDefaultGenerator.generateDefaultEntries(configPath);

        } catch (Exception e) {
            LOGGER.error("Error generating default configurations: {}", e.getMessage());
        }
    }
    
    /**
     * Checks if default files should be regenerated and regenerates them if needed
     */
    private static void checkAndRegenerateDefaultFiles(Path configPath) {
        try {
            // Check default currencies file
            Path defaultCurrenciesFile = configPath.resolve("default_currencies.json");
            if (!Files.exists(defaultCurrenciesFile)) {
                LOGGER.info("Generating default_currencies.json file");
                ShopDefaultGenerator.generateDefaultCurrencies(configPath);
            } else if (shouldRegenerateDefaultFile(defaultCurrenciesFile)) {
                LOGGER.info("Regenerating default_currencies.json file (overwritable: true)");
                ShopDefaultGenerator.generateDefaultCurrencies(configPath);
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
                            return true;
                        } else {
                            return false;
                        }
                    }
                    
                    // If no overwritable field, default to true (regenerate)
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

        
        // Simply reload everything from scratch - no protection of entries in memory
        // The 'overwritable' flag is only for automatic regeneration of physical files
        scanConfigDirectory();
        

    }
    
    // Getter methods
    public static Map<String, ShopCurrency> getCurrencies() {
        return new HashMap<>(CURRENCIES);
    }
    
    // Legacy method name for backward compatibility
    public static Map<String, ShopCurrency> getValutes() {
        return getCurrencies();
    }
    
    public static Map<String, ShopCategory> getCategories() {
        return new HashMap<>(CATEGORIES);
    }
    
    public static Map<String, ShopEntry> getEntries() {
        return new HashMap<>(ENTRIES);
    }
    
    public static ShopCurrency getCurrency(String id) {
        return CURRENCIES.get(id);
    }
    
    // Legacy method name for backward compatibility
    public static ShopCurrency getValute(String id) {
        return getCurrency(id);
    }
    
    public static ShopCategory getCategory(String id) {
        return CATEGORIES.get(id);
    }
    
    public static ShopEntry getEntry(String category, String item) {
        // First try with direct ID if available
        String baseItemId = extractBaseItemId(item);
        String key = category != null ? category + ":" + baseItemId : baseItemId;
        ShopEntry entry = ENTRIES.get(key);
        
        // If not found with direct key, search in entire map
        if (entry == null) {
            for (ShopEntry e : ENTRIES.values()) {
                if (category != null && !category.equals(e.inCategory)) {
                    continue;
                }
                String entryBaseItemId = extractBaseItemId(e.item);
                if (baseItemId.equals(entryBaseItemId)) {
                    return e;
                }
            }
        }
        
        return entry;
    }
    
    /**
     * Gets a ShopEntry by its unique ID
     */
    public static ShopEntry getEntryById(String id) {
        return ENTRIES.get(id);
    }
    
    /**
     * Gets all available currency IDs for autocompletion
     */
    public static List<String> getAllCurrencyIds() {
        return new ArrayList<>(CURRENCIES.keySet());
    }
    
    // Legacy method name for backward compatibility
    public static List<String> getAllValuteIds() {
        return getAllCurrencyIds();
    }
    
    /**
     * Extracts the base ID of an item by removing data components
     * Ex: "minecraft:diamond_sword[enchantments={...}]" -> "minecraft:diamond_sword"
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