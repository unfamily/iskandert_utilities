package net.unfamily.iskautils.network.packet;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.item.custom.PortableDislocatorItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Packet for Portable Dislocator teleportation from client to server
 */
public class PortableDislocatorC2SPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(PortableDislocatorC2SPacket.class);
    
    private final int targetX;
    private final int targetZ;
    
    /**
     * Creates a new Portable Dislocator packet
     * @param targetX The target X coordinate
     * @param targetZ The target Z coordinate
     */
    public PortableDislocatorC2SPacket(int targetX, int targetZ) {
        this.targetX = targetX;
        this.targetZ = targetZ;
    }
    
    /**
     * Handles the packet on the server side
     * @param player The server player who sent the packet
     */
    public void handle(ServerPlayer player) {
        if (player == null) {
            LOGGER.error("Server player is null while handling PortableDislocatorC2SPacket");
            return;
        }
        
        LOGGER.info("Received Portable Dislocator packet from player {} for coordinates {}, {}", 
            player.getName().getString(), targetX, targetZ);
        
        // Find the dislocator in player's inventory
        ItemStack dislocatorStack = PortableDislocatorItem.findPortableDislocator(player);
        if (dislocatorStack != null) {
            // Start the teleportation process on the server with the found dislocator
            PortableDislocatorItem.startTeleportation(player, dislocatorStack, targetX, targetZ);
        } else {
            LOGGER.error("Could not find Portable Dislocator for player {} during packet handling", 
                player.getName().getString());
        }
    }
    
    /**
     * Compatibility method for packet handling
     */
    public static void handlePacket(PortableDislocatorC2SPacket packet, ServerPlayer player) {
        packet.handle(player);
    }
} 