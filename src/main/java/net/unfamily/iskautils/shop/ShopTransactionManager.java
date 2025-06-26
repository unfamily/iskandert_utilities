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
     * @param entryId The unique ID of the ShopEntry (not the itemId)
     */
    public static boolean buyItem(ServerPlayer player, String entryId, int quantity) {
        // Find entry by unique ID
        ShopEntry entry = ShopLoader.getEntryById(entryId);
        if (entry == null) {
            // Send error to client instead of chat
            sendTransactionErrorToClient(player, "item_not_found", entryId, null);
            return false;
        }
        
        if (entry.buy <= 0) {
            // Send error to client instead of chat
            sendTransactionErrorToClient(player, "cannot_buy", entryId, null);
            return false;
        }
        
        // Check stage requirements
        if (!checkStageRequirements(player, entry)) {
            // Send error to client instead of chat
            sendTransactionErrorToClient(player, "stage_requirements", entryId, null);
            return false;
        }
        
        String valuteId = entry.valute != null ? entry.valute : "null_coin";
        double totalCost = entry.buy * quantity;
        
        // Check if player is in a team
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        String teamName = teamManager.getPlayerTeam(player);
        
        if (teamName == null) {
            // Send error to client instead of chat
            sendTransactionErrorToClient(player, "no_team", entryId, valuteId);
            return false;
        }
        
        // Check team balance
        double teamBalance = teamManager.getTeamValuteBalance(teamName, valuteId);
        if (teamBalance < totalCost) {
            // Send error to client instead of chat
            sendTransactionErrorToClient(player, "insufficient_funds", entryId, valuteId);
            return false;
        }
        
        // Remove valutes from team
        if (!teamManager.removeTeamValutes(teamName, valuteId, totalCost)) {
            // Send error to client instead of chat
            sendTransactionErrorToClient(player, "transaction_failed", entryId, valuteId);
            return false;
        }
        
        // Give item to player - now uses the exact item from the entry
        if (!giveItemToPlayer(player, entry, entry.itemCount * quantity)) {
            // If we can't give the item, refund the money
            teamManager.addTeamValutes(teamName, valuteId, totalCost);
            // Send error to client instead of chat
            sendTransactionErrorToClient(player, "give_item_failed", entryId, valuteId);
            return false;
        }
        
        // Send success to client instead of chat
        sendTransactionSuccessToClient(player);
        
        LOGGER.info("Player {} bought {}x {} (entry: {}) from team {} for {} {}", 
            player.getName().getString(), quantity, entry.item, entryId, teamName, totalCost, valuteId);
        
        return true;
    }
    
    /**
     * Attempts to sell an item to the shop using team valutes
     * @param entryId The unique ID of the ShopEntry (not the itemId)
     */
    public static boolean sellItem(ServerPlayer player, String entryId, int quantity) {
        // Find entry by unique ID
        ShopEntry entry = ShopLoader.getEntryById(entryId);
        if (entry == null) {
            // Send error to client instead of chat
            sendTransactionErrorToClient(player, "item_not_found", entryId, null);
            return false;
        }
        
        if (entry.sell <= 0) {
            // Send error to client instead of chat
            sendTransactionErrorToClient(player, "cannot_sell", entryId, null);
            return false;
        }
        
        // Check stage requirements
        if (!checkStageRequirements(player, entry)) {
            // Send error to client instead of chat
            sendTransactionErrorToClient(player, "stage_requirements", entryId, null);
            return false;
        }
        
        String valuteId = entry.valute != null ? entry.valute : "null_coin";
        double totalReward = entry.sell * quantity;
        
        // Check if player is in a team
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        String teamName = teamManager.getPlayerTeam(player);
        
        if (teamName == null) {
            // Send error to client instead of chat
            sendTransactionErrorToClient(player, "no_team", entryId, valuteId);
            return false;
        }
        
        // Check if player has the item and remove it  
        if (!removeItemFromPlayer(player, entry, entry.itemCount * quantity)) {
            // Send error to client instead of chat
            sendTransactionErrorToClient(player, "insufficient_items", entryId, valuteId);
            return false;
        }
        
        // Add valutes to team
        if (!teamManager.addTeamValutes(teamName, valuteId, totalReward)) {
            // Send error to client instead of chat
            sendTransactionErrorToClient(player, "transaction_failed", entryId, valuteId);
            return false;
        }
        
        // Send success to client instead of chat
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
     * Handles item purchase with multiple repetitions
     * As specified: x4 or x16 is simply repeating the operation checking money each time
     * @param entryId The unique ID of the ShopEntry
     */
    public static void handleBuyItem(ServerPlayer player, String entryId, int multiplier) {
        int successfulPurchases = 0;
        
        for (int i = 0; i < multiplier; i++) {
            boolean success = buyItem(player, entryId, 1);
            if (success) {
                successfulPurchases++;
            } else {
                break; // Stop here if there's no more money
            }
        }
        
        // Update team data in client after transaction
        updatePlayerTeamDataInClient(player);
    }
    
    /**
     * Handles item sale with multiple repetitions
     * As specified: x4 or x16 is simply repeating the operation
     * @param entryId The unique ID of the ShopEntry
     */
    public static void handleSellItem(ServerPlayer player, String entryId, int multiplier) {
        int successfulSales = 0;
        
        for (int i = 0; i < multiplier; i++) {
            boolean success = sellItem(player, entryId, 1);
            if (success) {
                successfulSales++;
            } else {
                break; // Stop here if there are no more items to sell
            }
        }
        
        // Update team data in client after transaction
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
        
        // Show all available valutes with localized names and symbols
        Map<String, ShopValute> allValutes = ShopLoader.getValutes();
        for (String valuteId : allValutes.keySet()) {
            ShopValute valute = allValutes.get(valuteId);
            double balance = teamManager.getTeamValuteBalance(teamName, valuteId);
            String localizedName = Component.translatable(valute.name).getString();
            String formattedName = localizedName + " " + valute.charSymbol;
            player.sendSystemMessage(Component.literal("§a" + formattedName + ": " + balance));
        }
    }
    
    /**
     * Gives an item to the player using the new 1.21.1 parsing system
     */
    private static boolean giveItemToPlayer(ServerPlayer player, ShopEntry entry, int count) {
        try {
            // Use the new parsing system that supports data components
            ItemStack itemStack = ItemConverter.parseItemString(entry.item, count);
            
            if (itemStack.isEmpty()) {
                LOGGER.warn("Unable to parse item: {}", entry.item);
                return false;
            }
            
            // Add item to player inventory
            Inventory inventory = player.getInventory();
            boolean added = inventory.add(itemStack);
            if (!added) {
                player.drop(itemStack, false);
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("Error giving item {} to player {}: {}", entry.item, player.getName().getString(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Removes an item from player inventory
     * Note: For selling, only compares the base item type (without data components)
     * to allow selling any version of the item
     */
    private static boolean removeItemFromPlayer(ServerPlayer player, ShopEntry entry, int count) {
        try {
            // For selling, use only the base item ID without components
            String baseItemId = extractBaseItemId(entry.item);
            ResourceLocation itemResource = ResourceLocation.parse(baseItemId);
            var item = BuiltInRegistries.ITEM.get(itemResource);
            
            if (item == Items.AIR) {
                LOGGER.warn("Item not found: {}", baseItemId);
                return false;
            }
            
            Inventory inventory = player.getInventory();
            
            // Count how many items the player has
            int totalCount = 0;
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack.getItem() == item) {
                    totalCount += stack.getCount();
                }
            }
            
            if (totalCount < count) {
                return false; // Not enough items
            }
            
            // Remove items
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
            LOGGER.error("Error removing item {} from player {}: {}", entry.item, player.getName().getString(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Updates team data in the player's client
     */
    private static void updatePlayerTeamDataInClient(ServerPlayer player) {
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        String teamName = teamManager.getPlayerTeam(player);
        
        if (teamName != null) {
            // Get all team balances
            Map<String, Double> teamBalances = new HashMap<>();
            
            // Get all available valutes and their balances
            Map<String, ShopValute> allValutes = ShopLoader.getValutes();
            for (String valuteId : allValutes.keySet()) {
                double balance = teamManager.getTeamValuteBalance(teamName, valuteId);
                teamBalances.put(valuteId, balance);
            }
            
            // Send updated data to client
            net.unfamily.iskautils.network.ModMessages.sendShopTeamDataToClient(player, teamName, teamBalances);
        }
    }
    
    /**
     * Finds a ShopEntry by itemId, regardless of category
     * Compares using the base item ID (without data components)
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
        
        return null; // Item not found
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