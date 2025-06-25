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
 * Utilizza il sistema di parsing di Minecraft 1.21.1 per convertire le stringhe item usando il sistema di parsing di Minecraft 1.21.1 per supportare i data components
 */
public class ItemConverter {
    private static final Logger LOGGER = LogUtils.getLogger();
    
        /**
     * Converte una stringa item nel formato di Minecraft 1.21.1 in ItemStack
     * Supporta sia il formato semplice "minecraft:diamond_sword" che quello con components
     * "minecraft:diamond_sword[damage=500,enchantments={sharpness:3}]"
     * 
     * @param itemString La stringa che rappresenta l'item
     * @param count Il numero di item nello stack
     * @return ItemStack corrispondente o ItemStack.EMPTY se non valido
     */
    public static ItemStack parseItemString(String itemString, int count) {
        if (itemString == null || itemString.trim().isEmpty()) {
            return ItemStack.EMPTY;
        }
        
        try {
            // Ottieni il server per il registry lookup
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                LOGGER.warn("Server non disponibile per il parsing dell'item: {}", itemString);
                return fallbackParsing(itemString, count);
            }
            
            try {
                // Usa direttamente ItemParser come fa il comando /give
                HolderLookup.Provider registryAccess = server.registryAccess();
                ItemParser itemParser = new ItemParser(registryAccess);
                StringReader reader = new StringReader(itemString);
                
                // Parsa la stringa per ottenere ItemResult
                ItemParser.ItemResult itemResult = itemParser.parse(reader);
                
                // Crea ItemInput e poi l'ItemStack finale
                ItemInput itemInput = new ItemInput(itemResult.item(), itemResult.components());
                ItemStack result = itemInput.createItemStack(count, false);
                
                LOGGER.debug("Item parsato con successo: {} -> {}", itemString, result);
                return result;
                
            } catch (CommandSyntaxException e) {
                LOGGER.warn("Errore nel parsing dell'item '{}': {}. Tentativo fallback.", itemString, e.getMessage());
                return fallbackParsing(itemString, count);
            }
            
        } catch (Exception e) {
            LOGGER.error("Errore imprevisto durante il parsing dell'item '{}': {}", itemString, e.getMessage());
            return fallbackParsing(itemString, count);
        }
    }
    
    /**
     * Metodo di fallback che usa il parsing semplice per compatibilità
     * con le stringhe che contengono solo l'ID dell'item
     */
    private static ItemStack fallbackParsing(String itemString, int count) {
        try {
            // Rimuovi eventuali components per ottenere solo l'ID dell'item
            String itemId = extractItemId(itemString);
            
            ResourceLocation itemResource = ResourceLocation.parse(itemId);
            var item = BuiltInRegistries.ITEM.get(itemResource);
            
            if (item != Items.AIR) {
                ItemStack stack = new ItemStack(item, count);
                LOGGER.debug("Item parsato con fallback: {} -> {}", itemString, stack);
                return stack;
            }
        } catch (Exception e) {
            LOGGER.warn("Fallback parsing fallito per '{}': {}", itemString, e.getMessage());
        }
        
        // Ultimo fallback: restituisci stone
        LOGGER.warn("Impossibile parsare l'item '{}', usando stone come fallback", itemString);
        return new ItemStack(Items.STONE, count);
    }
    
    /**
     * Estrae l'ID dell'item da una stringa che potrebbe contenere components
     * Es: "minecraft:diamond_sword[damage=500]" -> "minecraft:diamond_sword"
     */
    private static String extractItemId(String itemString) {
        int bracketIndex = itemString.indexOf('[');
        if (bracketIndex != -1) {
            return itemString.substring(0, bracketIndex);
        }
        return itemString;
    }
    
    /**
     * Converte una stringa item con un singolo item (count = 1)
     */
    public static ItemStack parseItemString(String itemString) {
        return parseItemString(itemString, 1);
    }
    
    /**
     * Verifica se una stringa può essere parsata come item valido
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
     * Ottiene il nome display di un item dalla sua stringa
     */
    public static String getItemDisplayName(String itemString) {
        ItemStack stack = parseItemString(itemString, 1);
        if (!stack.isEmpty()) {
            return stack.getHoverName().getString();
        }
        return itemString; // Fallback al nome originale
    }
} 