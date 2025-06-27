package net.unfamily.iskautils.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.structure.StructurePlacementHistory;

/**
 * Packet sent from client to server to request structure undo
 */
public class StructureUndoC2SPacket {
    
    public StructureUndoC2SPacket() {
        // Empty constructor - no data needed
    }
    
    public static StructureUndoC2SPacket decode(FriendlyByteBuf buffer) {
        return new StructureUndoC2SPacket();
    }
    
    public void encode(FriendlyByteBuf buffer) {
        // No data to encode
    }
    
    public static void handle(StructureUndoC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                // Success/failure messages are already handled in StructurePlacementHistory.undoLastPlacement()
                StructurePlacementHistory.undoLastPlacement(serverPlayer);
            }
        });
    }
} 