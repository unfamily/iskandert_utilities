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
     * Generates default currencies file
     */
    public static void generateDefaultCurrencies(Path configPath) {
        try {
            Path currenciesFile = configPath.resolve("default_currencies.json");
            
            // Check if file exists and if it's overwritable
            if (Files.exists(currenciesFile)) {
                try {
                    String content = Files.readString(currenciesFile);
                    JsonObject existingJson = GSON.fromJson(content, JsonObject.class);
                    
                    if (existingJson != null && existingJson.has("overwritable")) {
                        boolean overwritable = existingJson.get("overwritable").getAsBoolean();
                        if (!overwritable) {
                            LOGGER.info("Skipping default currencies file generation - file is protected");
                            return;
                        } else {
                            LOGGER.info("File exists and is overwritable, will regenerate");
                        }
                    } else {
                        LOGGER.info("File exists but no overwritable flag found, will regenerate");
                    }
                } catch (Exception e) {
                    LOGGER.warn("Error reading existing currencies file, will regenerate: {}", e.getMessage());
                }
            } else {
                LOGGER.info("File does not exist, will create new");
            }
            
            JsonObject root = new JsonObject();
            root.addProperty("type", "shop_currency");
            root.addProperty("overwritable", true);
            
            JsonArray currencies = new JsonArray();
            
            // Default currency
            JsonObject nullCoin = new JsonObject();
            nullCoin.addProperty("id", "null_coin");
            nullCoin.addProperty("name", "shop.currency.null_coin");
            nullCoin.addProperty("char_symbol", "∅");
            currencies.add(nullCoin);
            
            root.add("currencies", currencies);
            
            String jsonContent = GSON.toJson(root);
            Files.write(currenciesFile, jsonContent.getBytes());
            
            LOGGER.info("Generated default currencies file: {}", currenciesFile);
            
        } catch (IOException e) {
            LOGGER.error("Error generating default currencies file: {}", e.getMessage());
        }
    }
    
    /**
     * Legacy method for backward compatibility
     */
    public static void generateDefaultValutes(Path configPath) {
        generateDefaultCurrencies(configPath);
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
            breadEntry.addProperty("currency", "null_coin");
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
    
    /**
     * Generates a README file (always overwritten)
     */
    public static void generateReadme(Path configPath) {
        try {
            Path readmeFile = configPath.resolve("README.txt");
            String content = """
ISKAUTILS SHOP CONFIGURATION
===========================

This folder contains the default configuration files for the IskaUtils shop system.

FILE STRUCTURE:
==============

- default_currencies.json: Defines the currencies available in the shop
- default_categories.json: Defines the categories for shop entries  
- default_entries.json: Defines the items available in the shop

All files are in JSON format and can be edited to customize the shop system.
If a file contains the property 'overwritable: false', it will not be overwritten by default generators.

CURRENCIES (default_currencies.json):
====================================

Example:
{
  "type": "shop_currency",
  "overwritable": true,
  "currencies": [
    {
      "id": "null_coin",
      "name": "shop.currency.null_coin",
      "char_symbol": "∅"
    },
    {
      "id": "emerald",
      "name": "shop.currency.emerald", 
      "char_symbol": "💎"
    }
  ]
}

- id: Unique identifier for the currency
- name: Translation key for the currency name
- char_symbol: Symbol displayed next to the currency name

CATEGORIES (default_categories.json):
====================================

Example:
{
  "type": "shop_category",
  "overwritable": true,
  "categories": [
    {
      "id": "000_default",
      "name": "shop.category.default",
      "description": "shop.category.default.desc",
      "item": "minecraft:gold_nugget"
    },
    {
      "id": "tools",
      "name": "shop.category.tools",
      "description": "shop.category.tools.desc", 
      "item": "minecraft:diamond_pickaxe"
    }
  ]
}

- id: Unique identifier for the category
- name: Translation key for the category name
- description: Translation key for the category description
- item: Item ID used as icon for the category

ENTRIES (default_entries.json):
===============================

Example:
{
  "type": "shop_entry",
  "overwritable": true,
  "entries": [
    {
      "id": "bread_default",
      "in_category": "000_default",
      "item": "minecraft:bread",
      "item_count": 1,
      "currency": "null_coin",
      "buy": 1.0,
      "sell": 0.5
    },
    {
      "id": "diamond_pickaxe_silk",
      "in_category": "tools",
      "item": "minecraft:diamond_pickaxe[custom_name='\"Silky\"',repair_cost=1,enchantments={levels:{\"minecraft:silk_touch\":1}}]",
      "item_count": 1,
      "currency": "emerald",
      "buy": 50.0,
      "sell": 25.0
    }
  ]
}

- id: Unique identifier for the entry
- in_category: Category ID this entry belongs to
- item: Item ID (supports NBT/compound tags)
- item_count: Number of items in the stack
- currency: Currency ID for this entry
- buy: Price to buy the item
- sell: Price to sell the item

NBT/COMPOUND TAGS SUPPORT:
==========================

The 'item' field supports NBT (Named Binary Tag) data to specify items with custom properties.
This allows you to sell/buy items with specific enchantments, custom names, damage values, etc.

Examples:

1. Enchanted Diamond Pickaxe with Silk Touch:
   "item": "minecraft:diamond_pickaxe[custom_name='\"Silky\"',repair_cost=1,enchantments={levels:{\"minecraft:silk_touch\":1}}]"

2. Custom Named Sword:
   "item": "minecraft:diamond_sword[custom_name='\"Excalibur\"',repair_cost=0]"

3. Damaged Item:
   "item": "minecraft:diamond_pickaxe[damage=100]"

4. Item with Lore:
   "item": "minecraft:diamond_helmet[custom_name='\"Magic Helmet\"',display={lore:['\"Special helmet\"','\"With magic powers\"']}]"

5. Item with Multiple Enchantments:
   "item": "minecraft:diamond_sword[enchantments={levels:{\"minecraft:sharpness\":5,\"minecraft:looting\":3}}]"

NBT Syntax Rules:
- Use square brackets [] after the item ID
- Separate properties with commas
- String values must be quoted: '\"value\"'
- Numbers don't need quotes
- Nested objects use curly braces {}
- Arrays use square brackets []

Tip for NBT:
- You can use /kubejs hand to get item with NBT

This README is always overwritten when the default generator runs.
""";
            Files.write(readmeFile, content.getBytes());
            LOGGER.info("Generated README file: {}", readmeFile);
        } catch (IOException e) {
            LOGGER.error("Error generating README file: {}", e.getMessage());
        }
    }
} 

