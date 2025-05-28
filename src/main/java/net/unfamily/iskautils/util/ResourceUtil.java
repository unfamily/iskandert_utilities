package net.unfamily.iskautils.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * Utilità per gestire le ResourceLocation
 */
public class ResourceUtil {
    
    /**
     * Ottiene la ResourceLocation di un item
     * @param item L'item di cui ottenere la ResourceLocation
     * @return La ResourceLocation dell'item
     */
    public static ResourceLocation getKey(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }
    
    /**
     * Controlla se una stringa è un ID valido (nel formato namespace:path)
     * @param id La stringa da controllare
     * @return true se la stringa è un ID valido
     */
    public static boolean isValidId(String id) {
        return ResourceLocation.tryParse(id) != null;
    }
    
    /**
     * Converte un ID nel formato namespace:path in una ResourceLocation
     * @param id L'ID da convertire
     * @return La ResourceLocation corrispondente, o null se l'ID non è valido
     */
    public static ResourceLocation fromString(String id) {
        return ResourceLocation.tryParse(id);
    }
} 