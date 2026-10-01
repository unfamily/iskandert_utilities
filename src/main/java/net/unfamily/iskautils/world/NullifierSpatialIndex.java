package net.unfamily.iskautils.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.unfamily.iskautils.block.entity.NullifierTargetMode;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Chunk-keyed index of active spatial nullifiers (Ender / Flight / Climbing) with per-nullifier range and target mode.
 */
public final class NullifierSpatialIndex {
    public enum Kind {
        ENDER,
        FLIGHT,
        CLIMBING
    }

    public record Entry(int radius, Kind kind, NullifierTargetMode targetMode) {}

    private static final Map<ResourceKey<Level>, Map<ChunkPos, Map<BlockPos, Entry>>> BY_DIMENSION = new ConcurrentHashMap<>();

    private NullifierSpatialIndex() {}

    public static void update(
            ResourceKey<Level> dimension,
            BlockPos pos,
            Kind kind,
            boolean active,
            int radius,
            NullifierTargetMode targetMode) {
        if (active && targetMode != NullifierTargetMode.DISABLED) {
            ChunkPos chunkPos = ChunkPos.containing(pos);
            BY_DIMENSION
                    .computeIfAbsent(dimension, d -> new ConcurrentHashMap<>())
                    .computeIfAbsent(chunkPos, k -> new ConcurrentHashMap<>())
                    .put(pos.immutable(), new Entry(radius, kind, targetMode));
        } else {
            remove(dimension, pos);
        }
    }

    public static void remove(ResourceKey<Level> dimension, BlockPos pos) {
        Map<ChunkPos, Map<BlockPos, Entry>> dimMap = BY_DIMENSION.get(dimension);
        if (dimMap == null) {
            return;
        }
        ChunkPos chunkPos = ChunkPos.containing(pos);
        Map<BlockPos, Entry> chunkMap = dimMap.get(chunkPos);
        if (chunkMap != null) {
            chunkMap.remove(pos);
            if (chunkMap.isEmpty()) {
                dimMap.remove(chunkPos);
            }
        }
        if (dimMap.isEmpty()) {
            BY_DIMENSION.remove(dimension);
        }
    }

    public static Set<BlockPos> getNullifiersInChunk(ResourceKey<Level> dimension, ChunkPos chunkPos) {
        Map<ChunkPos, Map<BlockPos, Entry>> dimMap = BY_DIMENSION.get(dimension);
        if (dimMap == null) {
            return Collections.emptySet();
        }
        Map<BlockPos, Entry> chunkMap = dimMap.get(chunkPos);
        return chunkMap == null ? Collections.emptySet() : chunkMap.keySet();
    }

    public static boolean isTeleportBlocked(ResourceKey<Level> dimension, Vec3 position) {
        return isTeleportBlockedForMobs(dimension, position);
    }

    public static boolean isTeleportBlockedForMobs(ResourceKey<Level> dimension, Vec3 position) {
        return matches(dimension, position, Kind.ENDER, NullifierTargetMode::affectsMobs);
    }

    public static boolean isTeleportBlockedForPlayers(ResourceKey<Level> dimension, Vec3 position) {
        return matches(dimension, position, Kind.ENDER, NullifierTargetMode::affectsPlayers);
    }

    public static boolean shouldBlockMobClimbing(ResourceKey<Level> dimension, Vec3 position) {
        return matches(dimension, position, Kind.CLIMBING, NullifierTargetMode::affectsMobs);
    }

    public static boolean shouldBlockPlayerFlight(ResourceKey<Level> dimension, Vec3 position) {
        return matches(dimension, position, Kind.FLIGHT, NullifierTargetMode::blocksPlayerFlightInZone);
    }

    public static boolean shouldBlockMobFlight(ResourceKey<Level> dimension, Vec3 position) {
        return matches(dimension, position, Kind.FLIGHT, NullifierTargetMode::affectsMobs);
    }

    public static boolean shouldBlockPlayerGauntletClimb(ResourceKey<Level> dimension, Vec3 position) {
        return matches(dimension, position, Kind.CLIMBING, NullifierTargetMode::affectsPlayers);
    }

    private static boolean matches(
            ResourceKey<Level> dimension,
            Vec3 position,
            Kind kind,
            Predicate<NullifierTargetMode> modeTest) {
        Map<ChunkPos, Map<BlockPos, Entry>> dimMap = BY_DIMENSION.get(dimension);
        if (dimMap == null) {
            return false;
        }

        int searchChunkRadius = 16;
        int centerChunkX = ((int) Math.floor(position.x)) >> 4;
        int centerChunkZ = ((int) Math.floor(position.z)) >> 4;

        for (int dcx = -searchChunkRadius; dcx <= searchChunkRadius; dcx++) {
            for (int dcz = -searchChunkRadius; dcz <= searchChunkRadius; dcz++) {
                Map<BlockPos, Entry> nullifiers = dimMap.get(new ChunkPos(centerChunkX + dcx, centerChunkZ + dcz));
                if (nullifiers == null) {
                    continue;
                }
                for (Map.Entry<BlockPos, Entry> entry : nullifiers.entrySet()) {
                    Entry e = entry.getValue();
                    if (e.kind() != kind || !modeTest.test(e.targetMode())) {
                        continue;
                    }
                    if (isWithinRadius(entry.getKey(), position, e.radius())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isWithinRadius(BlockPos center, Vec3 position, int radius) {
        double dx = Math.abs(center.getX() + 0.5D - position.x);
        double dy = Math.abs(center.getY() + 0.5D - position.y);
        double dz = Math.abs(center.getZ() + 0.5D - position.z);
        return dx <= radius && dy <= radius && dz <= radius;
    }
}
