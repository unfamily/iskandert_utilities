package net.unfamily.iskautils.network.packet;

import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.shop.ShopTransactionManager;

/**
 * Packet per l'acquisto di item dal shop
 */
public class ShopBuyItemC2SPacket {
    private final String entryId;
    private final int quantity;
    
    public ShopBuyItemC2SPacket(String entryId, int quantity) {
        this.entryId = entryId;
        this.quantity = quantity;
    }
    
    /**
     * Gestisce il packet sul server
     */
    public void handle(ServerPlayer player) {
        System.out.println("DEBUG: ShopBuyItemC2SPacket.handle chiamato - player: " + player.getName().getString() + 
                          ", entryId: " + entryId + ", quantity: " + quantity);
        
        // Gestisci l'acquisto tramite il ShopTransactionManager
        ShopTransactionManager.handleBuyItem(player, entryId, quantity);
    }
} 