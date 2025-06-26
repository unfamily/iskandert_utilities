package net.unfamily.iskautils.shop;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

/**
 * Uses Minecraft 1.21.1 parsing system to convert item strings using Minecraft 1.21.1 parsing system to support data components
 */
public class ItemConverter {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Converts an item string in Minecraft 1.21.1 format to ItemStack
     * Supports both simple format "minecraft:diamond_sword" and with components
     * "minecraft:diamond_sword[damage=500,enchantments={sharpness:3}]"
     * 
     * @param itemString The string representing the item
     * @param count The number of items in the stack
     * @return Corresponding ItemStack or ItemStack.EMPTY if not valid
     */
    public static ItemStack parseItemString(String itemString, int count) {
        if (itemString == null || itemString.trim().isEmpty()) {
            return ItemStack.EMPTY;
        }
        
        try {
            // Get server for registry lookup
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                LOGGER.warn("Server not available for item parsing: {}", itemString);
                return fallbackParsing(itemString, count);
            }
            
            try {
                // Use ItemParser directly like the /give command does
                HolderLookup.Provider registryAccess = server.registryAccess();
                ItemParser itemParser = new ItemParser(registryAccess);
                StringReader reader = new StringReader(itemString);
                
                // Parse string to get ItemResult
                ItemParser.ItemResult itemResult = itemParser.parse(reader);
                
                // Create ItemInput and then the final ItemStack
                ItemInput itemInput = new ItemInput(itemResult.item(), itemResult.components());
                ItemStack result = itemInput.createItemStack(count, false);
                
                return result;
                
            } catch (CommandSyntaxException e) {
                LOGGER.warn("Error parsing item '{}': {}. Attempting fallback.", itemString, e.getMessage());
                return fallbackParsing(itemString, count);
            }
            
        } catch (Exception e) {
            LOGGER.error("Unexpected error during item parsing '{}': {}", itemString, e.getMessage());
            return fallbackParsing(itemString, count);
        }
    }
    
    /**
     * Fallback method that uses simple parsing for compatibility
     * with strings that contain only the item ID
     */
    private static ItemStack fallbackParsing(String itemString, int count) {
        try {
            // Remove any components to get only the item ID
            String itemId = extractItemId(itemString);
            
            ResourceLocation itemResource = ResourceLocation.parse(itemId);
            var item = BuiltInRegistries.ITEM.get(itemResource);
            
            if (item != Items.AIR) {
                ItemStack stack = new ItemStack(item, count);
                return stack;
            }
        } catch (Exception e) {
            LOGGER.warn("Fallback parsing failed for '{}': {}", itemString, e.getMessage());
        }
        
        // Last fallback: return stone
        LOGGER.warn("Unable to parse item '{}', using stone as fallback", itemString);
        return new ItemStack(Items.STONE, count);
    }
    
    /**
     * Extracts the item ID from a string that might contain components
     * Ex: "minecraft:diamond_sword[damage=500]" -> "minecraft:diamond_sword"
     */
    private static String extractItemId(String itemString) {
        int bracketIndex = itemString.indexOf('[');
        if (bracketIndex != -1) {
            return itemString.substring(0, bracketIndex);
        }
        return itemString;
    }
    
    /**
     * Converts an item string with a single item (count = 1)
     */
    public static ItemStack parseItemString(String itemString) {
        return parseItemString(itemString, 1);
    }
    
    /**
     * Verifies if a string can be parsed as a valid item
     */
    public static boolean isValidItemString(String itemString) {
        if (itemString == null || itemString.trim().isEmpty()) {
            return false;
        }
        
        try {
            ItemStack result = parseItemString(itemString, 1);
            return !result.isEmpty() && result.getItem() != Items.STONE;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Gets the display name of an item from its string
     */
    public static String getItemDisplayName(String itemString) {
        ItemStack stack = parseItemString(itemString, 1);
        if (!stack.isEmpty()) {
            return stack.getHoverName().getString();
        }
        return itemString; // Fallback to original name
    }
} 