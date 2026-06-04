package net.unfamily.iskautils.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chunk-keyed index of blazing altar positions together with their current operational state and
 * chunk radius.
 *
 * <p>The index is kept fully in-memory and is only mutated from the server thread. This lets the
 * light engine query brazier-flame light suppression from its worker threads (via
 * {@code BurningFlameBlock#getLightEmission}) without ever touching block entities or loading
 * neighbour chunks. Doing chunk/BE access from a light worker re-enters the chunk system and can
 * deadlock with chunk-loading mods that reinstate forced chunks during world load.
 */
public final class BlazingAltarSpatialIndex {
    /** Immutable snapshot of an altar's light-relevant state. */
    public record AltarState(boolean operational, int chunkRadius) {}

    private static final Map<ResourceKey<Level>, Map<Long, Map<BlockPos, AltarState>>> BY_DIMENSION = new ConcurrentHashMap<>();

    private BlazingAltarSpatialIndex() {}

    /** Adds or updates an altar entry. Call only from the server thread. */
    public static void update(ResourceKey<Level> dimension, BlockPos pos, boolean operational, int chunkRadius) {
        long chunkKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        BY_DIMENSION
                .computeIfAbsent(dimension, d -> new ConcurrentHashMap<>())
                .computeIfAbsent(chunkKey, k -> new ConcurrentHashMap<>())
                .put(pos.immutable(), new AltarState(operational, chunkRadius));
    }

    public static void remove(ResourceKey<Level> dimension, BlockPos pos) {
        Map<Long, Map<BlockPos, AltarState>> dimMap = BY_DIMENSION.get(dimension);
        if (dimMap == null) {
            return;
        }
        long chunkKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        Map<BlockPos, AltarState> set = dimMap.get(chunkKey);
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
        Map<Long, Map<BlockPos, AltarState>> dimMap = BY_DIMENSION.get(dimension);
        if (dimMap == null) {
            return Collections.emptySet();
        }
        Map<BlockPos, AltarState> set = dimMap.get(chunkPos.toLong());
        return set == null ? Collections.emptySet() : set.keySet();
    }

    /**
     * Pure in-memory check: is the given flame position within the chunk radius of any
     * non-operational altar? Safe to call from light-engine worker threads because it never reads
     * world state.
     */
    public static boolean isFlameSuppressedBySuspendedAltar(ResourceKey<Level> dimension, BlockPos flamePos, int searchChunkRadius) {
        Map<Long, Map<BlockPos, AltarState>> dimMap = BY_DIMENSION.get(dimension);
        if (dimMap == null) {
            return false;
        }
        int flameChunkX = flamePos.getX() >> 4;
        int flameChunkZ = flamePos.getZ() >> 4;
        for (int dcx = -searchChunkRadius; dcx <= searchChunkRadius; dcx++) {
            for (int dcz = -searchChunkRadius; dcz <= searchChunkRadius; dcz++) {
                Map<BlockPos, AltarState> altars = dimMap.get(ChunkPos.asLong(flameChunkX + dcx, flameChunkZ + dcz));
                if (altars == null) {
                    continue;
                }
                for (Map.Entry<BlockPos, AltarState> entry : altars.entrySet()) {
                    AltarState state = entry.getValue();
                    if (state.operational()) {
                        continue;
                    }
                    if (isWithinChunkRadius(entry.getKey(), flamePos, state.chunkRadius())) {
                        return true;
                    }
                }
            }
        }
        return false;
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
