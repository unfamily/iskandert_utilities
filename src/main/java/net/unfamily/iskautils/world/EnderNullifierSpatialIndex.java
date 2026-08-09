package net.unfamily.iskautils.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chunk-keyed index of active ender nullifier positions with per-nullifier range.
 */
public final class EnderNullifierSpatialIndex {
    // stores radius per active nullifier position
    private static final Map<ResourceKey<Level>, Map<Long, Map<BlockPos, Integer>>> BY_DIMENSION = new ConcurrentHashMap<>();

    private EnderNullifierSpatialIndex() {}

    public static void update(ResourceKey<Level> dimension, BlockPos pos, boolean active, int radius) {
        if (active) {
            long chunkKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
            BY_DIMENSION
                    .computeIfAbsent(dimension, d -> new ConcurrentHashMap<>())
                    .computeIfAbsent(chunkKey, k -> new ConcurrentHashMap<>())
                    .put(pos.immutable(), radius);
        } else {
            remove(dimension, pos);
        }
    }

    public static void remove(ResourceKey<Level> dimension, BlockPos pos) {
        Map<Long, Map<BlockPos, Integer>> dimMap = BY_DIMENSION.get(dimension);
        if (dimMap == null) return;
        long chunkKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        Map<BlockPos, Integer> chunkMap = dimMap.get(chunkKey);
        if (chunkMap != null) {
            chunkMap.remove(pos);
            if (chunkMap.isEmpty()) dimMap.remove(chunkKey);
        }
        if (dimMap.isEmpty()) BY_DIMENSION.remove(dimension);
    }

    public static Set<BlockPos> getNullifiersInChunk(ResourceKey<Level> dimension, ChunkPos chunkPos) {
        Map<Long, Map<BlockPos, Integer>> dimMap = BY_DIMENSION.get(dimension);
        if (dimMap == null) return Collections.emptySet();
        Map<BlockPos, Integer> chunkMap = dimMap.get(chunkPos.toLong());
        return chunkMap == null ? Collections.emptySet() : chunkMap.keySet();
    }

    public static boolean isTeleportBlocked(ResourceKey<Level> dimension, Vec3 position) {
        Map<Long, Map<BlockPos, Integer>> dimMap = BY_DIMENSION.get(dimension);
        if (dimMap == null) return false;

        // Use a large search window; each entry carries its own radius
        int searchChunkRadius = 16; // ~256 blocks max range / 16
        int centerChunkX = ((int) Math.floor(position.x)) >> 4;
        int centerChunkZ = ((int) Math.floor(position.z)) >> 4;

        for (int dcx = -searchChunkRadius; dcx <= searchChunkRadius; dcx++) {
            for (int dcz = -searchChunkRadius; dcz <= searchChunkRadius; dcz++) {
                Map<BlockPos, Integer> nullifiers = dimMap.get(ChunkPos.asLong(centerChunkX + dcx, centerChunkZ + dcz));
                if (nullifiers == null) continue;
                for (Map.Entry<BlockPos, Integer> entry : nullifiers.entrySet()) {
                    if (isWithinRadius(entry.getKey(), position, entry.getValue())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Axis-aligned cube centered on the nullifier block (Chebyshev / per-axis radius). */
    private static boolean isWithinRadius(BlockPos center, Vec3 position, int radius) {
        double dx = Math.abs(center.getX() + 0.5D - position.x);
        double dy = Math.abs(center.getY() + 0.5D - position.y);
        double dz = Math.abs(center.getZ() + 0.5D - position.z);
        return dx <= radius && dy <= radius && dz <= radius;
    }
}
