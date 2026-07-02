package net.unfamily.iskautils.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.SoulNullifierBlock;
import net.unfamily.iskautils.block.WanderNullifierBlock;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chunk coverage index for spawn-blocking nullifiers (wander / soul).
 */
public final class NullifierChunkIndex {
    public enum Kind {
        WANDER,
        SOUL
    }

    private record ChunkKey(ResourceKey<Level> dimension, int chunkX, int chunkZ) {}
    private record NullifierKey(Kind kind, ResourceKey<Level> dimension, BlockPos pos) {}

    private static final Map<NullifierKey, Set<ChunkKey>> NULLIFIER_TO_CHUNKS = new ConcurrentHashMap<>();
    private static final Map<Kind, Map<ChunkKey, Integer>> chunkCountsByKind = new ConcurrentHashMap<>();
    private static final Set<Kind> dirtyKinds = ConcurrentHashMap.newKeySet();

    static {
        chunkCountsByKind.put(Kind.WANDER, new ConcurrentHashMap<>());
        chunkCountsByKind.put(Kind.SOUL, new ConcurrentHashMap<>());
    }

    private NullifierChunkIndex() {}

    public static void markDirty(Kind kind) {
        dirtyKinds.add(kind);
    }

    public static void rebuildSnapshotsAtEndOfTick() {
        if (dirtyKinds.isEmpty()) {
            return;
        }
        for (Kind kind : Set.copyOf(dirtyKinds)) {
            dirtyKinds.remove(kind);
            Map<ChunkKey, Integer> next = new HashMap<>();
            for (Map.Entry<NullifierKey, Set<ChunkKey>> entry : NULLIFIER_TO_CHUNKS.entrySet()) {
                if (entry.getKey().kind() != kind) {
                    continue;
                }
                for (ChunkKey ck : entry.getValue()) {
                    next.merge(ck, 1, Integer::sum);
                }
            }
            chunkCountsByKind.put(kind, next.isEmpty() ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(next));
        }
    }

    public static void refresh(ServerLevel level, BlockPos pos, Kind kind) {
        NullifierKey key = new NullifierKey(kind, level.dimension(), pos.immutable());
        NULLIFIER_TO_CHUNKS.remove(key);

        BlockState state = level.getBlockState(pos);
        boolean active = isActiveBlock(state, kind);
        if (active) {
            int radius = radiusFor(kind);
            NULLIFIER_TO_CHUNKS.put(key, chunksIntersectingCube(level.dimension(), pos, radius));
            markDirty(kind);
        }
    }

    public static void remove(ServerLevel level, BlockPos pos, Kind kind) {
        NullifierKey key = new NullifierKey(kind, level.dimension(), pos.immutable());
        if (NULLIFIER_TO_CHUNKS.remove(key) != null) {
            markDirty(kind);
        }
    }

    public static void refreshAll(MinecraftServer server, Kind kind) {
        Set<NullifierKey> keys = NULLIFIER_TO_CHUNKS.keySet().stream()
                .filter(k -> k.kind() == kind)
                .collect(java.util.stream.Collectors.toSet());
        for (NullifierKey key : keys) {
            ServerLevel sl = server.getLevel(key.dimension());
            if (sl != null) {
                refresh(sl, key.pos(), kind);
            }
        }
    }

    public static int getChunkCoverageCount(ServerLevel level, int chunkX, int chunkZ, Kind kind) {
        ChunkKey ck = new ChunkKey(level.dimension(), chunkX, chunkZ);
        return chunkCountsByKind.getOrDefault(kind, Map.of()).getOrDefault(ck, 0);
    }

    public static boolean isWithinActiveCoverage(ServerLevel level, double x, double y, double z, Kind kind) {
        int radius = radiusFor(kind);
        int cx = SectionPos.posToSectionCoord(x);
        int cz = SectionPos.posToSectionCoord(z);
        int chunkRadius = (radius >> 4) + 1;
        for (int dcx = -chunkRadius; dcx <= chunkRadius; dcx++) {
            for (int dcz = -chunkRadius; dcz <= chunkRadius; dcz++) {
                if (getChunkCoverageCount(level, cx + dcx, cz + dcz, kind) <= 0) {
                    continue;
                }
                for (Map.Entry<NullifierKey, Set<ChunkKey>> entry : NULLIFIER_TO_CHUNKS.entrySet()) {
                    NullifierKey key = entry.getKey();
                    if (key.kind() != kind || !key.dimension().equals(level.dimension())) {
                        continue;
                    }
                    BlockPos center = key.pos();
                    if (isWithinCubeRadius(center, x, y, z, radius)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isActiveBlock(BlockState state, Kind kind) {
        return switch (kind) {
            case WANDER -> state.getBlock() instanceof WanderNullifierBlock wander
                    && state.getValue(WanderNullifierBlock.ON);
            case SOUL -> state.getBlock() instanceof SoulNullifierBlock soul
                    && state.getValue(SoulNullifierBlock.ON);
        };
    }

    private static int radiusFor(Kind kind) {
        return kind == Kind.WANDER ? Config.wanderNullifierRadius : Config.enderNullifierRadius;
    }

    private static boolean isWithinCubeRadius(BlockPos center, double x, double y, double z, int radius) {
        double dx = Math.abs(center.getX() + 0.5D - x);
        double dy = Math.abs(center.getY() + 0.5D - y);
        double dz = Math.abs(center.getZ() + 0.5D - z);
        return dx <= radius && dy <= radius && dz <= radius;
    }

    private static Set<ChunkKey> chunksIntersectingCube(ResourceKey<Level> dim, BlockPos center, int radiusBlocks) {
        int minCx = SectionPos.blockToSectionCoord(center.getX() - radiusBlocks);
        int maxCx = SectionPos.blockToSectionCoord(center.getX() + radiusBlocks);
        int minCz = SectionPos.blockToSectionCoord(center.getZ() - radiusBlocks);
        int maxCz = SectionPos.blockToSectionCoord(center.getZ() + radiusBlocks);
        Set<ChunkKey> out = new HashSet<>();
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                out.add(new ChunkKey(dim, cx, cz));
            }
        }
        return out;
    }
}
