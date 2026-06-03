package net.unfamily.iskautils.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Chunk ring ordering for Blazing Altar placement and extinguish (nearest altar chunk first). */
public final class BlazingAltarChunks {
    private BlazingAltarChunks() {}

    public static int countInRadius(int chunkRadius) {
        int count = 0;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) <= chunkRadius) {
                    count++;
                }
            }
        }
        return count;
    }

    public static List<ChunkPos> collectOrdered(BlockPos center, int chunkRadius) {
        ChunkPos origin = new ChunkPos(center);
        List<ChunkPos> result = new ArrayList<>();
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) > chunkRadius) {
                    continue;
                }
                result.add(new ChunkPos(origin.x + dx, origin.z + dz));
            }
        }
        sortByDistance(origin, result);
        return result;
    }

    public static List<ChunkPos> collectLoadedOrdered(ServerLevel level, BlockPos center, int chunkRadius) {
        ChunkPos origin = new ChunkPos(center);
        List<ChunkPos> result = new ArrayList<>();
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) > chunkRadius) {
                    continue;
                }
                ChunkPos chunkPos = new ChunkPos(origin.x + dx, origin.z + dz);
                BlockPos probe = new BlockPos(chunkPos.getMinBlockX(), center.getY(), chunkPos.getMinBlockZ());
                if (level.isLoaded(probe)) {
                    result.add(chunkPos);
                }
            }
        }
        sortByDistance(origin, result);
        return result;
    }

    private static void sortByDistance(ChunkPos origin, List<ChunkPos> chunks) {
        chunks.sort(Comparator
                .comparingInt((ChunkPos cp) -> chebyshevDistance(origin, cp))
                .thenComparingInt(cp -> cp.x)
                .thenComparingInt(cp -> cp.z));
    }

    public static int chebyshevDistance(ChunkPos center, ChunkPos other) {
        return Math.max(Math.abs(other.x - center.x), Math.abs(other.z - center.z));
    }
}
