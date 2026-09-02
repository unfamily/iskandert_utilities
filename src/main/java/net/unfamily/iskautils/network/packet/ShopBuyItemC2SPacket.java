package net.unfamily.iskautils.network.packet;

import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.shop.ShopTransactionManager;

/**
 * Client-to-server packet to buy a shop item
 */
public class ShopBuyItemC2SPacket {
    private final String entryId;
    private final int quantity;
    
    public ShopBuyItemC2SPacket(String entryId, int quantity) {
        this.entryId = entryId;
        this.quantity = quantity;
    }
    
    /**
     * Handle packet on the server
     */
    public void handle(ServerPlayer player) {
        System.out.println("DEBUG: ShopBuyItemC2SPacket.handle chiamato - player: " + player.getName().getString() + 
                          ", entryId: " + entryId + ", quantity: " + quantity);
        
        // Process buy via ShopTransactionManager
        ShopTransactionManager.handleBuyItem(player, entryId, quantity);
    }
} 