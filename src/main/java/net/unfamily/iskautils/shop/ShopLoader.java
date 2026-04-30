package net.unfamily.iskautils.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.unfamily.iskautils.data.load.IskaUtilsLoadJson;
import net.unfamily.iskautils.data.load.IskaUtilsLoadPaths;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

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
    
    public static void loadAll(ResourceManager resourceManagerOrNull) {
        try {
            CURRENCIES.clear();
            CATEGORIES.clear();
            ENTRIES.clear();
            PROTECTED_CURRENCIES.clear();
            PROTECTED_CATEGORIES.clear();
            PROTECTED_ENTRIES.clear();

            Map<Identifier, JsonElement> merged = resourceManagerOrNull != null
                    ? IskaUtilsLoadJson.collectMergedJson(resourceManagerOrNull,
                    id -> IskaUtilsLoadPaths.isJsonUnderLoadSubdir(id, IskaUtilsLoadPaths.SHOP))
                    : IskaUtilsLoadJson.collectFromModJarOnly(IskaUtilsLoadPaths.SHOP);
            for (var e : IskaUtilsLoadJson.orderedEntries(merged)) {
                if (!e.getValue().isJsonObject()) {
                    continue;
                }
                String fileName = IskaUtilsLoadJson.definitionIdFromLocation(e.getKey()) + ".json";
                ingestShopJson(fileName, e.getValue().getAsJsonObject());
            }
        } catch (Exception e) {
            LOGGER.error("Error during shop load: {}", e.getMessage());
        }
    }

    public static void scanConfigDirectory() {
        loadAll(null);
    }

    private static String normalizeType(String type) {
        if (type == null) {
            return "";
        }
        String t = type.trim();
        if (t.isEmpty()) {
            return "";
        }
        // Backward compatibility: accept un-namespaced shop types
        if (!t.contains(":")) {
            return "iska_utils:" + t;
        }
        return t;
    }

    private static void ingestShopJson(String fileName, JsonObject json) {
        try {
            String type = normalizeType(json.has("type") ? json.get("type").getAsString() : "");
            boolean overwritable = json.has("overwritable") && json.get("overwritable").getAsBoolean();
            switch (type) {
                case "iska_utils:shop_currency":
                case "iska_utils:shop_valute":
                    processCurrenciesFile(json, overwritable, fileName);
                    break;
                case "iska_utils:shop_category":
                    processCategoriesFile(json, overwritable, fileName);
                    break;
                case "iska_utils:shop_entry":
                    processEntriesFile(json, overwritable, fileName);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            LOGGER.error("Error scanning shop file {}: {}", fileName, e.getMessage());
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
    

    public static void reloadAllConfigurations() {
        var server = ServerLifecycleHooks.getCurrentServer();
        loadAll(server != null ? server.getResourceManager() : null);
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