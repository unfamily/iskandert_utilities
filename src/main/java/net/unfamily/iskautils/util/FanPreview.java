package net.unfamily.iskautils.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.block.FanBlock;
import net.unfamily.iskautils.block.entity.FanBlockEntity;
import net.unfamily.iskautils.network.ModMessages;

/**
 * Server-side fan push-area footprint preview (owned markers, no expiry until toggled off).
 */
public final class FanPreview {

    private FanPreview() {}

    public static void sendFootprint(ServerPlayer player, ServerLevel level, BlockPos fanPos, FanBlockEntity fan) {
        var state = level.getBlockState(fanPos);
        if (!(state.getBlock() instanceof FanBlock)) {
            return;
        }
        var facing = state.getValue(FanBlock.FACING);
        boolean hasGhostModule = fan.hasGhostModule();
        var aabb = FanBlockEntity.calculatePushArea(fanPos, facing, fan);

        int minX = (int) Math.floor(aabb.minX);
        int minY = (int) Math.floor(aabb.minY);
        int minZ = (int) Math.floor(aabb.minZ);
        int maxX = (int) Math.floor(aabb.maxX);
        int maxY = (int) Math.floor(aabb.maxY);
        int maxZ = (int) Math.floor(aabb.maxZ);

        int purpleColor = 0x80FF00FF;
        int redColor = 0x80FF0000;
        int durationTicks = 0;

        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                boolean isOnEdge = (x == minX || x == maxX - 1 || z == minZ || z == maxZ - 1);
                if (isOnEdge) {
                    BlockPos topPos = new BlockPos(x, maxY - 1, z);
                    ModMessages.sendPreviewMarker(player, fanPos, topPos,
                            FanBlockEntity.isBlockObstacle(level, topPos, hasGhostModule) ? redColor : purpleColor, durationTicks);
                    BlockPos bottomPos = new BlockPos(x, minY, z);
                    ModMessages.sendPreviewMarker(player, fanPos, bottomPos,
                            FanBlockEntity.isBlockObstacle(level, bottomPos, hasGhostModule) ? redColor : purpleColor, durationTicks);
                }
            }
        }

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                boolean isOnEdge = (x == minX || x == maxX - 1 || y == minY || y == maxY - 1);
                if (isOnEdge) {
                    BlockPos minZPos = new BlockPos(x, y, minZ);
                    ModMessages.sendPreviewMarker(player, fanPos, minZPos,
                            FanBlockEntity.isBlockObstacle(level, minZPos, hasGhostModule) ? redColor : purpleColor, durationTicks);
                    BlockPos maxZPos = new BlockPos(x, y, maxZ - 1);
                    ModMessages.sendPreviewMarker(player, fanPos, maxZPos,
                            FanBlockEntity.isBlockObstacle(level, maxZPos, hasGhostModule) ? redColor : purpleColor, durationTicks);
                }
            }
        }

        for (int z = minZ; z < maxZ; z++) {
            for (int y = minY; y < maxY; y++) {
                boolean isOnEdge = (z == minZ || z == maxZ - 1 || y == minY || y == maxY - 1);
                if (isOnEdge) {
                    BlockPos minXPos = new BlockPos(minX, y, z);
                    ModMessages.sendPreviewMarker(player, fanPos, minXPos,
                            FanBlockEntity.isBlockObstacle(level, minXPos, hasGhostModule) ? redColor : purpleColor, durationTicks);
                    BlockPos maxXPos = new BlockPos(maxX - 1, y, z);
                    ModMessages.sendPreviewMarker(player, fanPos, maxXPos,
                            FanBlockEntity.isBlockObstacle(level, maxXPos, hasGhostModule) ? redColor : purpleColor, durationTicks);
                }
            }
        }

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    BlockPos blockPos = new BlockPos(x, y, z);
                    if (FanBlockEntity.isBlockObstacle(level, blockPos, hasGhostModule)) {
                        ModMessages.sendPreviewMarker(player, fanPos, blockPos, redColor, durationTicks);
                    }
                }
            }
        }
    }
}
