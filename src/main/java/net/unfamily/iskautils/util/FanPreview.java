package net.unfamily.iskautils.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.block.entity.FanBlockEntity;
import net.unfamily.iskautils.network.MachinePreviewNetworking;
import net.unfamily.iskautils.network.ModMessages;
import net.unfamily.iskautils.util.preview.MachinePreviewMarkerLogic;
import net.unfamily.iskautils.util.preview.MachinePreviewServerTracker;

/**
 * Server-side fan push-area footprint preview (owned markers, no expiry until toggled off).
 */
public final class FanPreview {

    private FanPreview() {}

    public static void sendFootprint(ServerPlayer player, ServerLevel level, BlockPos fanPos, FanBlockEntity fan) {
        int generation = MachinePreviewServerTracker.nextFootprintGeneration(fanPos);
        MachinePreviewNetworking.clearClientPreview(player, fanPos, generation);
        int durationTicks = 0;
        MachinePreviewMarkerLogic.forEachFanMarker(level, fan, fanPos,
                (worldPos, color) -> ModMessages.sendPreviewMarker(
                        player, fanPos, worldPos, color, durationTicks, generation));
    }
}
