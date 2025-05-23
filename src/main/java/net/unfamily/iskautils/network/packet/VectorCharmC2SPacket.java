package net.unfamily.iskautils.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.data.VectorCharmData;
import net.unfamily.iskautils.data.VectorFactorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Packet for Vector Charm configuration from client to server
 * Currently simplified to avoid networking complexity
 */
public class VectorCharmC2SPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(VectorCharmC2SPacket.class);
    
    private final byte newFactor;
    private final boolean isVertical;
    
    /**
     * Creates a new Vector Charm packet
     * @param newFactor The new factor value (0-5)
     * @param isVertical true if it's the vertical factor, false for horizontal
     */
    public VectorCharmC2SPacket(byte newFactor, boolean isVertical) {
        this.newFactor = newFactor;
        this.isVertical = isVertical;
    }
    
    /**
     * Handles the packet on the server side
     * @param player The server player who sent the packet
     */
    public void handle(ServerPlayer player) {
        if (player == null) {
            LOGGER.error("Server player is null while handling VectorCharmC2SPacket");
            return;
        }
        
        ServerLevel level = player.serverLevel();

        // Get the Vector Charm data instance
        VectorCharmData data = VectorCharmData.get(level);
        
        // Update the appropriate factor
        if (isVertical) {
            data.setVerticalFactor(player, newFactor);
            
            // Send confirmation message to player
            VectorFactorType newFactorType = VectorFactorType.fromByte(newFactor);
            player.sendSystemMessage(Component.translatable("message.iska_utils.vector_vertical_factor", 
                                   Component.translatable("vectorcharm.factor." + newFactorType.getName())));
            
            // Log the change
            // LOGGER.info("Player {} set vertical factor to {}", player.getScoreboardName(), newFactorType.getName());
            
        } else {
            data.setHorizontalFactor(player, newFactor);
            
            // Send confirmation message to player
            VectorFactorType newFactorType = VectorFactorType.fromByte(newFactor);
            player.sendSystemMessage(Component.translatable("message.iska_utils.vector_horizontal_factor", 
                                   Component.translatable("vectorcharm.factor." + newFactorType.getName())));
            
            // Log the change
            // LOGGER.info("Player {} set horizontal factor to {}", player.getScoreboardName(), newFactorType.getName());
        }
    }
    
    /**
     * Compatibility method for when complete network packets will be implemented
     * This is a stub method that allows maintaining a signature similar to what will be used
     * in the future when full networking is implemented
     */
    public static void handlePacket(VectorCharmC2SPacket packet, ServerPlayer player) {
        // LOGGER.debug("Handling VectorCharmC2SPacket (stub method)");
        packet.handle(player);
    }
} 