package net.unfamily.iskautils.network.packet;

import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.shop.ShopTransactionManager;

/**
 * Packet per la vendita di item al shop
 */
public class ShopSellItemC2SPacket {
    private final String entryId;
    private final int quantity;
    
    public ShopSellItemC2SPacket(String entryId, int quantity) {
        this.entryId = entryId;
        this.quantity = quantity;
    }
    
    /**
     * Gestisce il packet sul server
     */
    public void handle(ServerPlayer player) {
        System.out.println("DEBUG: ShopSellItemC2SPacket.handle chiamato - player: " + player.getName().getString() + 
                          ", entryId: " + entryId + ", quantity: " + quantity);
        
        // Gestisci la vendita tramite il ShopTransactionManager
        ShopTransactionManager.handleSellItem(player, entryId, quantity);
    }
} 