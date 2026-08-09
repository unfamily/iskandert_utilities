package net.unfamily.iskautils.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.block.CollectingCrateBlock;
import net.unfamily.iskautils.block.entity.CollectingCrateBlockEntity;
import net.unfamily.iskautils.network.ModMessages;

/**
 * Server-side collection-area footprint preview.
 * Outer shell is drawn client-side via {@code AreaBorderRenderer}; this only clears prior markers.
 */
public final class CollectingCratePreview {

    private CollectingCratePreview() {}

    public static void sendFootprint(ServerPlayer player, ServerLevel level, BlockPos cratePos, CollectingCrateBlockEntity crate) {
        var state = level.getBlockState(cratePos);
        if (!(state.getBlock() instanceof CollectingCrateBlock)) {
            return;
        }
        // Clear any legacy billboard shell; border is rendered on the client from AABB.
        ModMessages.clearPreviewForBuilder(player, cratePos);
    }
}
