package net.unfamily.iskautils.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.unfamily.iskautils.network.packet.ClearPreviewForOwnerS2CPayload;

/** Server helpers for machine footprint preview S2C batches. */
public final class MachinePreviewNetworking {

    private MachinePreviewNetworking() {}

    public static void clearClientPreview(ServerPlayer player, BlockPos owner, int footprintGeneration) {
        PacketDistributor.sendToPlayer(player, new ClearPreviewForOwnerS2CPayload(owner, false, footprintGeneration));
    }
}
