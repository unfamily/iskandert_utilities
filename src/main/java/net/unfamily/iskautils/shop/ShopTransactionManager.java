package net.unfamily.iskautils.shop;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.unfamily.iskautils.stage.StageRegistry;
import net.unfamily.iskautils.shop.ItemConverter;
import org.slf4j.Logger;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Manages shop transactions using team valutes
 */
public class ShopTransactionManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Attempts to buy an item from the shop using team valutes
     * @param entryId L'ID univoco della ShopEntry (non l'itemId)
     */
    public static boolean buyItem(ServerPlayer player, String entryId, int quantity) {
        // Cerca l'entry tramite ID univoco
        ShopEntry entry = ShopLoader.getEntryById(entryId);
        if (entry == null) {
            // Invia errore al client invece del chat
            sendTransactionErrorToClient(player, "item_not_found", entryId, null);
            return false;
        }
        
        if (entry.buy <= 0) {
            // Invia errore al client invece del chat
            sendTransactionErrorToClient(player, "cannot_buy", entryId, null);
            return false;
        }
        
        // Check stage requirements
        if (!checkStageRequirements(player, entry)) {
            // Invia errore al client invece del chat
            sendTransactionErrorToClient(player, "stage_requirements", entryId, null);
            return false;
        }
        
        String valuteId = entry.valute != null ? entry.valute : "null_coin";
        double totalCost = entry.buy * quantity;
        
        // Check if player is in a team
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        String teamName = teamManager.getPlayerTeam(player);
        
        if (teamName == null) {
            // Invia errore al client invece del chat
            sendTransactionErrorToClient(player, "no_team", entryId, valuteId);
            return false;
        }
        
        // Check team balance
        double teamBalance = teamManager.getTeamValuteBalance(teamName, valuteId);
        if (teamBalance < totalCost) {
            // Invia errore al client invece del chat
            sendTransactionErrorToClient(player, "insufficient_funds", entryId, valuteId);
            return false;
        }
        
        // Remove valutes from team
        if (!teamManager.removeTeamValutes(teamName, valuteId, totalCost)) {
            // Invia errore al client invece del chat
            sendTransactionErrorToClient(player, "transaction_failed", entryId, valuteId);
            return false;
        }
        
        // Give item to player - ora usa l'item esatto della entry
        if (!giveItemToPlayer(player, entry, entry.itemCount * quantity)) {
            // Se non riusciamo a dare l'item, rimborsa i soldi
            teamManager.addTeamValutes(teamName, valuteId, totalCost);
            // Invia errore al client invece del chat
            sendTransactionErrorToClient(player, "give_item_failed", entryId, valuteId);
            return false;
        }
        
        // Invia successo al client invece del chat
        sendTransactionSuccessToClient(player);
        
        LOGGER.info("Player {} bought {}x {} (entry: {}) from team {} for {} {}", 
            player.getName().getString(), quantity, entry.item, entryId, teamName, totalCost, valuteId);
        
        return true;
    }
    
    /**
     * Attempts to sell an item to the shop using team valutes
     * @param entryId L'ID univoco della ShopEntry (non l'itemId)
     */
    public static boolean sellItem(ServerPlayer player, String entryId, int quantity) {
        // Cerca l'entry tramite ID univoco
        ShopEntry entry = ShopLoader.getEntryById(entryId);
        if (entry == null) {
            // Invia errore al client invece del chat
            sendTransactionErrorToClient(player, "item_not_found", entryId, null);
            return false;
        }
        
        if (entry.sell <= 0) {
            // Invia errore al client invece del chat
            sendTransactionErrorToClient(player, "cannot_sell", entryId, null);
            return false;
        }
        
        // Check stage requirements
        if (!checkStageRequirements(player, entry)) {
            // Invia errore al client invece del chat
            sendTransactionErrorToClient(player, "stage_requirements", entryId, null);
            return false;
        }
        
        String valuteId = entry.valute != null ? entry.valute : "null_coin";
        double totalReward = entry.sell * quantity;
        
        // Check if player is in a team
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        String teamName = teamManager.getPlayerTeam(player);
        
        if (teamName == null) {
            // Invia errore al client invece del chat
            sendTransactionErrorToClient(player, "no_team", entryId, valuteId);
            return false;
        }
        
        // Check if player has the item and remove it  
        if (!removeItemFromPlayer(player, entry, entry.itemCount * quantity)) {
            // Invia errore al client invece del chat
            sendTransactionErrorToClient(player, "insufficient_items", entryId, valuteId);
            return false;
        }
        
        // Add valutes to team
        if (!teamManager.addTeamValutes(teamName, valuteId, totalReward)) {
            // Invia errore al client invece del chat
            sendTransactionErrorToClient(player, "transaction_failed", entryId, valuteId);
            return false;
        }
        
        // Invia successo al client invece del chat
        sendTransactionSuccessToClient(player);
        
        LOGGER.info("Player {} sold {}x {} (entry: {}) to team {} for {} {}", 
            player.getName().getString(), quantity, entry.item, entryId, teamName, totalReward, valuteId);
        
        return true;
    }
    
    /**
     * Checks if a player meets all stage requirements for a shop entry
     */
    private static boolean checkStageRequirements(ServerPlayer player, ShopEntry entry) {
        if (entry.stages == null || entry.stages.length == 0) {
            return true; // No stage requirements
        }
        
        StageRegistry registry = StageRegistry.getInstance(player.getServer());
        if (registry == null) {
            LOGGER.warn("StageRegistry not available for player {}", player.getName().getString());
            return false;
        }
        
        for (ShopStage stage : entry.stages) {
            boolean stageMet = false;
            
            switch (stage.stageType.toLowerCase()) {
                case "player":
                    boolean hasPlayerStage = registry.hasPlayerStage(player, stage.stage);
                    stageMet = (hasPlayerStage == stage.is);
                    break;
                    
                case "world":
                    boolean hasWorldStage = registry.hasWorldStage(stage.stage);
                    stageMet = (hasWorldStage == stage.is);
                    break;
                    
                case "team":
                    boolean hasTeamStage = registry.hasPlayerTeamStage(player, stage.stage);
                    stageMet = (hasTeamStage == stage.is);
                    break;
                    
                default:
                    LOGGER.warn("Unknown stage type: {}", stage.stageType);
                    stageMet = false;
                    break;
            }
            
            if (!stageMet) {
                LOGGER.debug("Player {} failed stage requirement: {} {} {}", 
                    player.getName().getString(), stage.stageType, stage.stage, stage.is);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Gestisce l'acquisto di item con ripetizioni multiple
     * Come specificato: x4 o x16 è semplicemente ripetere l'operazione controllando ogni volta i denari
     * @param entryId L'ID univoco della ShopEntry
     */
    public static void handleBuyItem(ServerPlayer player, String entryId, int multiplier) {
        System.out.println("DEBUG: handleBuyItem chiamato - entryId: " + entryId + ", multiplier: " + multiplier);
        
        int successfulPurchases = 0;
        
        for (int i = 0; i < multiplier; i++) {
            boolean success = buyItem(player, entryId, 1);
            if (success) {
                successfulPurchases++;
                System.out.println("DEBUG: Acquisto " + (i + 1) + "/" + multiplier + " riuscito");
            } else {
                System.out.println("DEBUG: Acquisto " + (i + 1) + "/" + multiplier + " fallito - interrompo");
                break; // Fermiamo qui se non ci sono più soldi
            }
        }
        
        // Rimuovo i messaggi in chat - il feedback è gestito dal client
        
        System.out.println("DEBUG: handleBuyItem completato - acquisti riusciti: " + successfulPurchases + "/" + multiplier);
        
        // Aggiorna i dati del team nel client dopo la transazione
        updatePlayerTeamDataInClient(player);
    }
    
    /**
     * Gestisce la vendita di item con ripetizioni multiple
     * Come specificato: x4 o x16 è semplicemente ripetere l'operazione
     * @param entryId L'ID univoco della ShopEntry
     */
    public static void handleSellItem(ServerPlayer player, String entryId, int multiplier) {
        System.out.println("DEBUG: handleSellItem chiamato - entryId: " + entryId + ", multiplier: " + multiplier);
        
        int successfulSales = 0;
        
        for (int i = 0; i < multiplier; i++) {
            boolean success = sellItem(player, entryId, 1);
            if (success) {
                successfulSales++;
                System.out.println("DEBUG: Vendita " + (i + 1) + "/" + multiplier + " riuscita");
            } else {
                System.out.println("DEBUG: Vendita " + (i + 1) + "/" + multiplier + " fallita - interrompo");
                break; // Fermiamo qui se non ci sono più item da vendere
            }
        }
        
        // Rimuovo i messaggi in chat - il feedback è gestito dal client
        
        System.out.println("DEBUG: handleSellItem completato - vendite riuscite: " + successfulSales + "/" + multiplier);
        
        // Aggiorna i dati del team nel client dopo la transazione
        updatePlayerTeamDataInClient(player);
    }
    
    /**
     * Gets the current team balance for a player
     */
    public static double getPlayerTeamBalance(ServerPlayer player, String valuteId) {
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        String teamName = teamManager.getPlayerTeam(player);
        
        if (teamName == null) {
            return 0.0;
        }
        
        return teamManager.getTeamValuteBalance(teamName, valuteId);
    }
    
    /**
     * Gets the current team balance for a player (default null_coin)
     */
    public static double getPlayerTeamBalance(ServerPlayer player) {
        return getPlayerTeamBalance(player, "null_coin");
    }
    
    /**
     * Shows the current team balance to a player
     */
    public static void showTeamBalance(ServerPlayer player) {
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        String teamName = teamManager.getPlayerTeam(player);
        
        if (teamName == null) {
            player.sendSystemMessage(Component.literal("§cYou are not in a team"));
            return;
        }
        
        player.sendSystemMessage(Component.literal("§6=== Team Balance ==="));
        player.sendSystemMessage(Component.literal("§eTeam: " + teamName));
        
        // Show null_coin balance
        double nullCoinBalance = teamManager.getTeamValuteBalance(teamName, "null_coin");
        player.sendSystemMessage(Component.literal("§aNull Coins: " + nullCoinBalance));
        
        // Could show other valutes here if needed
    }
    
    /**
     * Dà un item al giocatore utilizzando il nuovo sistema di parsing 1.21.1
     */
    private static boolean giveItemToPlayer(ServerPlayer player, ShopEntry entry, int count) {
        try {
            // Usa il nuovo sistema di parsing che supporta data components
            ItemStack itemStack = ItemConverter.parseItemString(entry.item, count);
            
            if (itemStack.isEmpty()) {
                LOGGER.warn("Impossibile parsare l'item: {}", entry.item);
                return false;
            }
            
            // Aggiungi l'item all'inventario del giocatore
            Inventory inventory = player.getInventory();
            boolean added = inventory.add(itemStack);
            if (!added) {
                player.drop(itemStack, false);
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("Errore nel dare item {} al giocatore {}: {}", entry.item, player.getName().getString(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Rimuove un item dall'inventario del giocatore
     * Nota: Per la vendita, confronta solo il tipo base dell'item (senza data components)
     * per permettere di vendere qualsiasi versione dell'item
     */
    private static boolean removeItemFromPlayer(ServerPlayer player, ShopEntry entry, int count) {
        try {
            // Per la vendita, usa solo l'ID base dell'item senza components
            String baseItemId = extractBaseItemId(entry.item);
            ResourceLocation itemResource = ResourceLocation.parse(baseItemId);
            var item = BuiltInRegistries.ITEM.get(itemResource);
            
            if (item == Items.AIR) {
                LOGGER.warn("Item non trovato: {}", baseItemId);
                return false;
            }
            
            Inventory inventory = player.getInventory();
            
            // Conta quanti item ha il giocatore
            int totalCount = 0;
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack.getItem() == item) {
                    totalCount += stack.getCount();
                }
            }
            
            if (totalCount < count) {
                return false; // Non ha abbastanza item
            }
            
            // Rimuovi gli item
            int remainingToRemove = count;
            for (int i = 0; i < inventory.getContainerSize() && remainingToRemove > 0; i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack.getItem() == item) {
                    int toRemove = Math.min(remainingToRemove, stack.getCount());
                    stack.shrink(toRemove);
                    remainingToRemove -= toRemove;
                    
                    if (stack.isEmpty()) {
                        inventory.setItem(i, ItemStack.EMPTY);
                    }
                }
            }
            
            return true;
        } catch (Exception e) {
            LOGGER.error("Errore nel rimuovere item {} dal giocatore {}: {}", entry.item, player.getName().getString(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Aggiorna i dati del team nel client del giocatore
     */
    private static void updatePlayerTeamDataInClient(ServerPlayer player) {
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        String teamName = teamManager.getPlayerTeam(player);
        
        if (teamName != null) {
            // Ottieni tutti i balance del team
            Map<String, Double> teamBalances = new HashMap<>();
            
            // Ottieni tutte le valute disponibili e i loro balance
            Map<String, ShopValute> allValutes = ShopLoader.getValutes();
            for (String valuteId : allValutes.keySet()) {
                double balance = teamManager.getTeamValuteBalance(teamName, valuteId);
                teamBalances.put(valuteId, balance);
            }
            
            // Invia i dati aggiornati al client
            net.unfamily.iskautils.network.ModMessages.sendShopTeamDataToClient(player, teamName, teamBalances);
        }
    }
    
    /**
     * Trova una ShopEntry per itemId, indipendentemente dalla categoria
     * Confronta usando l'ID base dell'item (senza data components)
     */
    private static ShopEntry findEntryByItemId(String itemId) {
        Map<String, ShopEntry> allEntries = ShopLoader.getEntries();
        String baseItemId = extractBaseItemId(itemId);
        
        for (ShopEntry entry : allEntries.values()) {
            String entryBaseItemId = extractBaseItemId(entry.item);
            if (baseItemId.equals(entryBaseItemId)) {
                return entry;
            }
        }
        
        return null; // Item non trovato
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
    
    /**
     * Invia un errore di transazione al client
     */
    private static void sendTransactionErrorToClient(ServerPlayer player, String errorType, String itemId, String valuteId) {
        try {
            // Simplified implementation for single player compatibility
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                net.unfamily.iskautils.client.gui.ShopScreen.handleTransactionError(errorType, itemId, valuteId);
            });
        } catch (Exception e) {
            // Ignore errors when running on dedicated server or if client is not available
        }
    }
    
    /**
     * Invia un successo di transazione al client
     */
    private static void sendTransactionSuccessToClient(ServerPlayer player) {
        try {
            // Simplified implementation for single player compatibility
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                net.unfamily.iskautils.client.gui.ShopScreen.handleTransactionSuccess();
            });
        } catch (Exception e) {
            // Ignore errors when running on dedicated server or if client is not available
        }
    }
} 