package net.unfamily.iskautils.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * Utilities for handling identifiers.
 */
public class ResourceUtil {
    
    /**
     * Gets the identifier of an item.
     * @param item The item
     * @return The item identifier
     */
    public static Identifier getKey(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }
    
    /**
     * Controlla se una stringa è un ID valido (nel formato namespace:path)
     * @param id La stringa da controllare
     * @return true se la stringa è un ID valido
     */
    public static boolean isValidId(String id) {
        return Identifier.tryParse(id) != null;
    }
    
    /**
     * Converts a string id into an identifier.
     * @param id The id
     * @return The identifier, or null if invalid
     */
    public static Identifier fromString(String id) {
        return Identifier.tryParse(id);
    }
} 