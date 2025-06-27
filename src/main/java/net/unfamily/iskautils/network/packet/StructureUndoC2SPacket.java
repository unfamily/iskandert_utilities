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
                boolean success = StructurePlacementHistory.undoLastPlacement(serverPlayer);
                
                if (!success) {
                    int historySize = StructurePlacementHistory.getHistorySize(serverPlayer);
                    if (historySize == 0) {
                        serverPlayer.displayClientMessage(Component.literal("§cNessuna struttura da annullare!"), true);
                    } else {
                        serverPlayer.displayClientMessage(Component.literal("§cImpossibile annullare l'ultima struttura!"), true);
                    }
                }
            }
        });
    }
} 