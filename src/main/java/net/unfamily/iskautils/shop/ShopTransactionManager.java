package net.unfamily.iskautils.shop;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.unfamily.iskautils.stage.StageRegistry;
import org.slf4j.Logger;

/**
 * Manages shop transactions using team valutes
 */
public class ShopTransactionManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Attempts to buy an item from the shop using team valutes
     */
    public static boolean buyItem(ServerPlayer player, String itemId, int quantity) {
        ShopEntry entry = ShopLoader.getEntry(null, itemId);
        if (entry == null) {
            player.sendSystemMessage(Component.literal("§cItem not found in shop: " + itemId));
            return false;
        }
        
        if (entry.buy <= 0) {
            player.sendSystemMessage(Component.literal("§cThis item cannot be bought"));
            return false;
        }
        
        // Check stage requirements
        if (!checkStageRequirements(player, entry)) {
            player.sendSystemMessage(Component.literal("§cYou don't meet the stage requirements for this item"));
            return false;
        }
        
        String valuteId = entry.valute != null ? entry.valute : "null_coin";
        double totalCost = entry.buy * quantity;
        
        // Check if player is in a team
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        String teamName = teamManager.getPlayerTeam(player);
        
        if (teamName == null) {
            player.sendSystemMessage(Component.literal("§cYou must be in a team to use the shop"));
            return false;
        }
        
        // Check team balance
        double teamBalance = teamManager.getTeamValuteBalance(teamName, valuteId);
        if (teamBalance < totalCost) {
            player.sendSystemMessage(Component.literal(String.format("§cInsufficient team balance. Need %.1f %s, have %.1f", 
                totalCost, valuteId, teamBalance)));
            return false;
        }
        
        // Remove valutes from team
        if (!teamManager.removeTeamValutes(teamName, valuteId, totalCost)) {
            player.sendSystemMessage(Component.literal("§cFailed to process transaction"));
            return false;
        }
        
        // Give item to player (this would need to be implemented based on your item system)
        // For now, just send a success message
        player.sendSystemMessage(Component.literal(String.format("§aSuccessfully bought %d %s for %.1f %s", 
            quantity, itemId, totalCost, valuteId)));
        
        LOGGER.info("Player {} bought {}x {} from team {} for {} {}", 
            player.getName().getString(), quantity, itemId, teamName, totalCost, valuteId);
        
        return true;
    }
    
    /**
     * Attempts to sell an item to the shop using team valutes
     */
    public static boolean sellItem(ServerPlayer player, String itemId, int quantity) {
        ShopEntry entry = ShopLoader.getEntry(null, itemId);
        if (entry == null) {
            player.sendSystemMessage(Component.literal("§cItem not found in shop: " + itemId));
            return false;
        }
        
        if (entry.sell <= 0) {
            player.sendSystemMessage(Component.literal("§cThis item cannot be sold"));
            return false;
        }
        
        // Check stage requirements
        if (!checkStageRequirements(player, entry)) {
            player.sendSystemMessage(Component.literal("§cYou don't meet the stage requirements for this item"));
            return false;
        }
        
        String valuteId = entry.valute != null ? entry.valute : "null_coin";
        double totalReward = entry.sell * quantity;
        
        // Check if player is in a team
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        String teamName = teamManager.getPlayerTeam(player);
        
        if (teamName == null) {
            player.sendSystemMessage(Component.literal("§cYou must be in a team to use the shop"));
            return false;
        }
        
        // Check if player has the item (this would need to be implemented based on your item system)
        // For now, we'll assume they have it
        
        // Add valutes to team
        if (!teamManager.addTeamValutes(teamName, valuteId, totalReward)) {
            player.sendSystemMessage(Component.literal("§cFailed to process transaction"));
            return false;
        }
        
        // Remove item from player (this would need to be implemented based on your item system)
        // For now, just send a success message
        player.sendSystemMessage(Component.literal(String.format("§aSuccessfully sold %d %s for %.1f %s", 
            quantity, itemId, totalReward, valuteId)));
        
        LOGGER.info("Player {} sold {}x {} to team {} for {} {}", 
            player.getName().getString(), quantity, itemId, teamName, totalReward, valuteId);
        
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
                    
                default:
                    LOGGER.warn("Unknown stage type: {} for stage: {}", stage.stageType, stage.stage);
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
} 