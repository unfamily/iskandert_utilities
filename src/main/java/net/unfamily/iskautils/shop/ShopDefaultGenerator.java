package net.unfamily.iskautils.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Default file generator for the shop system
 */
public class ShopDefaultGenerator {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    /**
     * Generates default valutes file
     */
    public static void generateDefaultValutes(Path configPath) {
        try {
            Path valutesFile = configPath.resolve("default_valutes.json");
            
            // Check if file exists and if it's overwritable
            if (Files.exists(valutesFile)) {
                try {
                    String content = Files.readString(valutesFile);
                    JsonObject existingJson = GSON.fromJson(content, JsonObject.class);
                    
                    if (existingJson != null && existingJson.has("overwritable")) {
                        boolean overwritable = existingJson.get("overwritable").getAsBoolean();
                        if (!overwritable) {
                            LOGGER.info("Skipping default valutes file generation - file is protected");
                            return;
                        } else {
                            LOGGER.info("File exists and is overwritable, will regenerate");
                        }
                    } else {
                        LOGGER.info("File exists but no overwritable flag found, will regenerate");
                    }
                } catch (Exception e) {
                    LOGGER.warn("Error reading existing valutes file, will regenerate: {}", e.getMessage());
                }
            } else {
                LOGGER.info("File does not exist, will create new");
            }
            
            JsonObject root = new JsonObject();
            root.addProperty("type", "shop_valute");
            root.addProperty("overwritable", true);
            
            JsonArray valutes = new JsonArray();
            
            // Default valute
            JsonObject nullCoin = new JsonObject();
            nullCoin.addProperty("id", "null_coin");
            nullCoin.addProperty("name", "shop.valute.null_coin");
            nullCoin.addProperty("char_symbol", "∅");
            valutes.add(nullCoin);
            
            root.add("valutes", valutes);
            
            String jsonContent = GSON.toJson(root);
            Files.write(valutesFile, jsonContent.getBytes());
            
            LOGGER.info("Generated default valutes file: {}", valutesFile);
            
        } catch (IOException e) {
            LOGGER.error("Error generating default valutes file: {}", e.getMessage());
        }
    }
    
    /**
     * Generates default categories file
     */
    public static void generateDefaultCategories(Path configPath) {
        try {
            Path categoriesFile = configPath.resolve("default_categories.json");
            
            // Check if file exists and if it's overwritable
            if (Files.exists(categoriesFile)) {
                try {
                    String content = Files.readString(categoriesFile);
                    JsonObject existingJson = GSON.fromJson(content, JsonObject.class);
                    
                    if (existingJson != null && existingJson.has("overwritable")) {
                        boolean overwritable = existingJson.get("overwritable").getAsBoolean();
                        if (!overwritable) {
                            LOGGER.info("Skipping default categories file generation - file is protected");
                            return;
                        } else {
                            LOGGER.info("File exists and is overwritable, will regenerate");
                        }
                    } else {
                        LOGGER.info("File exists but no overwritable flag found, will regenerate");
                    }
                } catch (Exception e) {
                    LOGGER.warn("Error reading existing categories file, will regenerate: {}", e.getMessage());
                }
            } else {
                LOGGER.info("File does not exist, will create new");
            }
            
            JsonObject root = new JsonObject();
            root.addProperty("type", "shop_category");
            root.addProperty("overwritable", true);
            
            JsonArray categories = new JsonArray();
            
            // Default category
            JsonObject defaultCategory = new JsonObject();
            defaultCategory.addProperty("id", "000_default");
            defaultCategory.addProperty("name", "shop.category.default");
            defaultCategory.addProperty("description", "shop.category.default.desc");
            defaultCategory.addProperty("item", "minecraft:gold_nugget");
            categories.add(defaultCategory);
            
            root.add("categories", categories);
            
            String jsonContent = GSON.toJson(root);
            Files.write(categoriesFile, jsonContent.getBytes());
            
            LOGGER.info("Generated default categories file: {}", categoriesFile);
            
        } catch (IOException e) {
            LOGGER.error("Error generating default categories file: {}", e.getMessage());
        }
    }
    
    /**
     * Generates default entries file
     */
    public static void generateDefaultEntries(Path configPath) {
        try {
            Path entriesFile = configPath.resolve("default_entries.json");
            
            // Check if file exists and if it's overwritable
            if (Files.exists(entriesFile)) {
                try {
                    String content = Files.readString(entriesFile);
                    JsonObject existingJson = GSON.fromJson(content, JsonObject.class);
                    
                    if (existingJson != null && existingJson.has("overwritable")) {
                        boolean overwritable = existingJson.get("overwritable").getAsBoolean();
                        if (!overwritable) {
                            LOGGER.info("Skipping default entries file generation - file is protected");
                            return;
                        } else {
                            LOGGER.info("File exists and is overwritable, will regenerate");
                        }
                    } else {
                        LOGGER.info("File exists but no overwritable flag found, will regenerate");
                    }
                } catch (Exception e) {
                    LOGGER.warn("Error reading existing entries file, will regenerate: {}", e.getMessage());
                }
            } else {
                LOGGER.info("File does not exist, will create new");
            }
            
            JsonObject root = new JsonObject();
            root.addProperty("type", "shop_entry");
            root.addProperty("overwritable", true);
            
            JsonArray entries = new JsonArray();
            
            // Default bread entry
            JsonObject breadEntry = new JsonObject();
            breadEntry.addProperty("id", "bread_default");
            breadEntry.addProperty("in_category", "000_default");
            breadEntry.addProperty("item", "minecraft:bread");
            breadEntry.addProperty("item_count", 1);
            breadEntry.addProperty("valute", "null_coin");
            breadEntry.addProperty("buy", 1.0);
            breadEntry.addProperty("sell", 0.5);
            entries.add(breadEntry);
            
            root.add("entries", entries);
            
            String jsonContent = GSON.toJson(root);
            Files.write(entriesFile, jsonContent.getBytes());
            
            LOGGER.info("Generated default entries file: {}", entriesFile);
            
        } catch (IOException e) {
            LOGGER.error("Error generating default entries file: {}", e.getMessage());
        }
    }
} 