package net.unfamily.iskautils.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.unfamily.iskautils.block.CollectingCrateBlock;
import net.unfamily.iskautils.block.entity.CollectingCrateBlockEntity;
import net.unfamily.iskautils.network.ModMessages;

/**
 * Server-side collection-area footprint preview (owned markers, cleared on toggle off).
 */
public final class CollectingCratePreview {

    private CollectingCratePreview() {}

    public static void sendFootprint(ServerPlayer player, ServerLevel level, BlockPos cratePos, CollectingCrateBlockEntity crate) {
        var state = level.getBlockState(cratePos);
        if (!(state.getBlock() instanceof CollectingCrateBlock)) {
            return;
        }
        var facing = state.getValue(HorizontalDirectionalBlock.FACING);
        AABB aabb = CollectingCrateAreaLogic.getCollectionVolumeAABB(
                cratePos,
                facing,
                crate.getSizeLeft(),
                crate.getSizeRight(),
                crate.getSizeHeight(),
                crate.getSizeDepth());

        int minX = (int) Math.floor(aabb.minX);
        int minY = (int) Math.floor(aabb.minY);
        int minZ = (int) Math.floor(aabb.minZ);
        int maxX = (int) Math.floor(aabb.maxX);
        int maxY = (int) Math.floor(aabb.maxY);
        int maxZ = (int) Math.floor(aabb.maxZ);

        int color = 0x80FF00FF;
        int durationTicks = 0;

        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                if (x == minX || x == maxX - 1 || z == minZ || z == maxZ - 1) {
                    ModMessages.sendPreviewMarker(player, cratePos, new BlockPos(x, maxY - 1, z), color, durationTicks);
                    ModMessages.sendPreviewMarker(player, cratePos, new BlockPos(x, minY, z), color, durationTicks);
                }
            }
        }
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                if (x == minX || x == maxX - 1 || y == minY || y == maxY - 1) {
                    ModMessages.sendPreviewMarker(player, cratePos, new BlockPos(x, y, minZ), color, durationTicks);
                    ModMessages.sendPreviewMarker(player, cratePos, new BlockPos(x, y, maxZ - 1), color, durationTicks);
                }
            }
        }
        for (int z = minZ; z < maxZ; z++) {
            for (int y = minY; y < maxY; y++) {
                if (z == minZ || z == maxZ - 1 || y == minY || y == maxY - 1) {
                    ModMessages.sendPreviewMarker(player, cratePos, new BlockPos(minX, y, z), color, durationTicks);
                    ModMessages.sendPreviewMarker(player, cratePos, new BlockPos(maxX - 1, y, z), color, durationTicks);
                }
            }
        }
    }
}
