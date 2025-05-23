package net.unfamily.iskautils.network;

import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.network.packet.VectorCharmC2SPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simplified version to avoid compatibility issues with NeoForge
 */
public class ModMessages {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModMessages.class);
    
    public static void register() {
        // LOGGER.info("Registering network messages for {}", IskaUtils.MOD_ID);
        
        // To implement later when networking is needed
    }
    
    /**
     * Sends a packet to the server (stub for when it will be implemented)
     */
    public static <MSG> void sendToServer(MSG message) {
        // LOGGER.debug("Sending packet to server: {}", message);
        // Stub implementation - will be completed when networking is needed
    }
} 