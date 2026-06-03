package net.unfamily.iskautils.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Chunk-keyed index of blazing altar positions for O(1) chunk lookups. */
public final class BlazingAltarSpatialIndex {
    private static final Map<ResourceKey<Level>, Map<Long, Set<BlockPos>>> BY_DIMENSION = new ConcurrentHashMap<>();

    private BlazingAltarSpatialIndex() {}

    public static void add(ResourceKey<Level> dimension, BlockPos pos) {
        long chunkKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        BY_DIMENSION
                .computeIfAbsent(dimension, d -> new ConcurrentHashMap<>())
                .computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet())
                .add(pos.immutable());
    }

    public static void remove(ResourceKey<Level> dimension, BlockPos pos) {
        Map<Long, Set<BlockPos>> dimMap = BY_DIMENSION.get(dimension);
        if (dimMap == null) {
            return;
        }
        long chunkKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        Set<BlockPos> set = dimMap.get(chunkKey);
        if (set != null) {
            set.remove(pos);
            if (set.isEmpty()) {
                dimMap.remove(chunkKey);
            }
        }
        if (dimMap.isEmpty()) {
            BY_DIMENSION.remove(dimension);
        }
    }

    public static Set<BlockPos> getAltarsInChunk(ResourceKey<Level> dimension, ChunkPos chunkPos) {
        Map<Long, Set<BlockPos>> dimMap = BY_DIMENSION.get(dimension);
        if (dimMap == null) {
            return Collections.emptySet();
        }
        Set<BlockPos> set = dimMap.get(chunkPos.toLong());
        return set == null ? Collections.emptySet() : set;
    }

    /** Chebyshev distance in chunks between altar and block position. */
    public static boolean isWithinChunkRadius(BlockPos altarPos, BlockPos testPos, int chunkRadius) {
        int altarChunkX = altarPos.getX() >> 4;
        int altarChunkZ = altarPos.getZ() >> 4;
        int testChunkX = testPos.getX() >> 4;
        int testChunkZ = testPos.getZ() >> 4;
        int dx = Math.abs(altarChunkX - testChunkX);
        int dz = Math.abs(altarChunkZ - testChunkZ);
        return Math.max(dx, dz) <= chunkRadius;
    }
}
