package net.unfamily.iskautils.network.packet;

import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.shop.ShopTransactionManager;

/**
 * Packet per l'acquisto di item dal shop
 */
public class ShopBuyItemC2SPacket {
    private final String itemId;
    private final int quantity;
    
    public ShopBuyItemC2SPacket(String itemId, int quantity) {
        this.itemId = itemId;
        this.quantity = quantity;
    }
    
    /**
     * Gestisce il packet sul server
     */
    public void handle(ServerPlayer player) {
        System.out.println("DEBUG: ShopBuyItemC2SPacket.handle chiamato - player: " + player.getName().getString() + 
                          ", itemId: " + itemId + ", quantity: " + quantity);
        
        // Gestisci l'acquisto tramite il ShopTransactionManager
        ShopTransactionManager.handleBuyItem(player, itemId, quantity);
    }
} 