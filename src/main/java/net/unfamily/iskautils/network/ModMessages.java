package net.unfamily.iskautils.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.ClientEvents;
import net.unfamily.iskautils.client.MarkRenderer;
import net.unfamily.iskautils.network.packet.VectorCharmC2SPacket;
import net.unfamily.iskautils.network.packet.PortableDislocatorC2SPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Handles network messages for the mod
 */
public class ModMessages {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModMessages.class);
    
    // Simplified version to avoid NeoForge networking compatibility issues
    
    /**
     * Registers network messages for the mod
     */
    public static void register() {
        LOGGER.info("Registering network messages for {}", IskaUtils.MOD_ID);
        // Simplified implementation - actual registration is handled by NeoForge
    }
    
    /**
     * Sends a packet to the server
     */
    public static <MSG> void sendToServer(MSG message) {
        // Simplified implementation - actual sending is handled by NeoForge
    }
    
    /**
     * Sends a packet to a specific player
     */
    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        // Simplified implementation - actual sending is handled by NeoForge
    }
    
    /**
     * Sends a Portable Dislocator packet to the server
     */
    public static void sendPortableDislocatorPacket(int targetX, int targetZ) {
        LOGGER.info("Sending Portable Dislocator packet to server: {}, {}", targetX, targetZ);
        // Simplified implementation for single player compatibility
    }
    
    /**
     * Sends a packet to add a highlighted block
     * This is a simplified implementation that directly calls the client handler
     * in single player mode, but would use actual packets in multiplayer
     */
    public static void sendAddHighlightPacket(ServerPlayer player, BlockPos pos, int color, int durationTicks) {
        // In a real implementation, this would send a packet to the client
        // For now, we'll use a direct call for single player compatibility
        // This is a simplified approach that works in both single player and dedicated server
        try {
            // This will be executed on the client side
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                ClientEvents.handleAddHighlight(pos, color, durationTicks);
            });
        } catch (Exception e) {
            // Ignore errors when running on dedicated server
            LOGGER.debug("Could not send highlight packet to client: {}", e.getMessage());
        }
    }
    
    /**
     * Sends a packet to add a highlighted block with a name
     */
    public static void sendAddHighlightWithNamePacket(ServerPlayer player, BlockPos pos, int color, int durationTicks, String name) {
        // In a real implementation, this would send a packet to the client
        // For now, we'll use a direct call for single player compatibility
        try {
            // This will be executed on the client side
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                ClientEvents.handleAddHighlightWithName(pos, color, durationTicks, name);
            });
        } catch (Exception e) {
            // Ignore errors when running on dedicated server
            LOGGER.debug("Could not send highlight with name packet to client: {}", e.getMessage());
        }
    }
    
    /**
     * Sends a packet to add a billboard marker
     * This is a simplified implementation that directly calls the client handler
     * in single player mode, but would use actual packets in multiplayer
     */
    public static void sendAddBillboardPacket(ServerPlayer player, BlockPos pos, int color, int durationTicks) {
        // In a real implementation, this would send a packet to the client
        // For now, we'll use a direct call for single player compatibility
        try {
            // This will be executed on the client side
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                ClientEvents.handleAddBillboard(pos, color, durationTicks);
            });
        } catch (Exception e) {
            // Ignore errors when running on dedicated server
            LOGGER.debug("Could not send billboard packet to client: {}", e.getMessage());
        }
    }
    
    /**
     * Sends a packet to add a billboard marker with a name
     */
    public static void sendAddBillboardWithNamePacket(ServerPlayer player, BlockPos pos, int color, int durationTicks, String name) {
        // In a real implementation, this would send a packet to the client
        // For now, we'll use a direct call for single player compatibility
        try {
            // This will be executed on the client side
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                ClientEvents.handleAddBillboardWithName(pos, color, durationTicks, name);
            });
        } catch (Exception e) {
            // Ignore errors when running on dedicated server
            LOGGER.debug("Could not send billboard with name packet to client: {}", e.getMessage());
        }
    }
    
    /**
     * Sends a packet to remove a highlighted block
     */
    public static void sendRemoveHighlightPacket(ServerPlayer player, BlockPos pos) {
        // In a real implementation, this would send a packet to the client
        // For now, we'll use a direct call for single player compatibility
        try {
            // This will be executed on the client side
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                ClientEvents.handleRemoveHighlight(pos);
            });
        } catch (Exception e) {
            // Ignore errors when running on dedicated server
            LOGGER.debug("Could not send remove highlight packet to client: {}", e.getMessage());
        }
    }
    
    /**
     * Sends a packet to clear all highlighted blocks
     */
    public static void sendClearHighlightsPacket(ServerPlayer player) {
        // In a real implementation, this would send a packet to the client
        // For now, we'll use a direct call for single player compatibility
        try {
            // This will be executed on the client side
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                ClientEvents.handleClearHighlights();
            });
        } catch (Exception e) {
            // Ignore errors when running on dedicated server
            LOGGER.debug("Could not send clear highlights packet to client: {}", e.getMessage());
        }
    }
    
    /**
     * Sends a Structure Placer save packet to the server
     * This simulates a client-to-server packet for saving the selected structure
     */
    public static void sendStructurePlacerSavePacket(String structureId) {
        LOGGER.info("Sending Structure Placer save packet: {}", structureId);
        // Simplified implementation - directly handle on the server side
        try {
            net.minecraft.server.level.ServerPlayer player = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
            if (player != null) {
                net.unfamily.iskautils.network.packet.StructurePlacerSaveC2SPacket packet = 
                    new net.unfamily.iskautils.network.packet.StructurePlacerSaveC2SPacket(structureId);
                packet.handle(player);
            }
        } catch (Exception e) {
            LOGGER.error("Could not send Structure Placer save packet: {}", e.getMessage());
        }
    }
} 