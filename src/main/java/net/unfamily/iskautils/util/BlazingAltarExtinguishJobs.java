package net.unfamily.iskautils.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.BlazingAltarBlockEntity;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Removes blazing-altar flames one chunk per server tick (nearest to farthest).
 * Jobs are tied to altar {@link BlazingAltarBlockEntity#getInstanceId()}, not position alone.
 */
public final class BlazingAltarExtinguishJobs {
    /** Break cleanup jobs never update a block entity (avoids ghost state on replace). */
    public static final long NO_OWNER_INSTANCE = 0L;

    private static final Map<ServerLevel, List<Job>> JOBS_BY_LEVEL = new ConcurrentHashMap<>();

    public enum FinishMode {
        FLAMES_ONLY,
        FINISH_ALTAR
    }

    private BlazingAltarExtinguishJobs() {}

    public static void enqueueFromAltar(ServerLevel level, BlazingAltarBlockEntity altar, FinishMode finishMode) {
        enqueue(
                level,
                altar.getBlockPos(),
                altar.getChunkRadius(),
                altar.isGroundOnly(),
                finishMode,
                altar.getInstanceId());
    }

    public static void enqueueBreakCleanup(ServerLevel level, BlockPos altarPos, int chunkRadius, boolean groundOnly) {
        enqueue(level, altarPos, chunkRadius, groundOnly, FinishMode.FLAMES_ONLY, NO_OWNER_INSTANCE);
    }

    public static boolean hasJob(ServerLevel level, BlockPos altarPos, long ownerInstanceId) {
        if (ownerInstanceId == NO_OWNER_INSTANCE) {
            return false;
        }
        List<Job> jobs = JOBS_BY_LEVEL.get(level);
        if (jobs == null) {
            return false;
        }
        synchronized (jobs) {
            for (Job job : jobs) {
                if (job.ownerInstanceId == ownerInstanceId && job.altarPos.equals(altarPos)) {
                    return true;
                }
            }
        }
        return false;
    }

    @org.jetbrains.annotations.Nullable
    public static int[] getProgress(ServerLevel level, BlockPos altarPos, long ownerInstanceId) {
        if (ownerInstanceId == NO_OWNER_INSTANCE) {
            return null;
        }
        List<Job> jobs = JOBS_BY_LEVEL.get(level);
        if (jobs == null) {
            return null;
        }
        synchronized (jobs) {
            for (Job job : jobs) {
                if (job.ownerInstanceId == ownerInstanceId && job.altarPos.equals(altarPos)) {
                    return new int[] {job.chunkIndex, job.chunks.size()};
                }
            }
        }
        return null;
    }

    public static void cancelForAltar(ServerLevel level, BlockPos altarPos, long ownerInstanceId) {
        List<Job> jobs = JOBS_BY_LEVEL.get(level);
        if (jobs == null) {
            return;
        }
        synchronized (jobs) {
            jobs.removeIf(job -> job.altarPos.equals(altarPos)
                    && (job.ownerInstanceId == ownerInstanceId || job.ownerInstanceId == NO_OWNER_INSTANCE));
            if (jobs.isEmpty()) {
                JOBS_BY_LEVEL.remove(level, jobs);
            }
        }
    }

    public static void enqueue(
            ServerLevel level,
            BlockPos altarPos,
            int chunkRadius,
            boolean groundOnly,
            FinishMode finishMode,
            long ownerInstanceId) {
        List<ChunkPos> chunks = BlazingAltarChunks.collectOrdered(altarPos, chunkRadius);
        if (chunks.isEmpty()) {
            if (finishMode == FinishMode.FINISH_ALTAR && ownerInstanceId != NO_OWNER_INSTANCE) {
                applyAltarFinish(level, altarPos, ownerInstanceId, 0);
            }
            return;
        }
        BlazingAltarBlockEntity altar = resolveOwner(level, altarPos, ownerInstanceId);
        if (altar != null) {
            altar.setExtinguishProgress(0, chunks.size());
        }
        List<Job> jobs = JOBS_BY_LEVEL.computeIfAbsent(level, ignored -> new ArrayList<>());
        synchronized (jobs) {
            if (ownerInstanceId != NO_OWNER_INSTANCE) {
                jobs.removeIf(job -> job.altarPos.equals(altarPos) && job.ownerInstanceId == ownerInstanceId);
            } else {
                jobs.removeIf(job -> job.altarPos.equals(altarPos) && job.ownerInstanceId == NO_OWNER_INSTANCE);
            }
            jobs.add(new Job(altarPos, chunks, groundOnly, finishMode, ownerInstanceId));
        }
    }

    public static void tick(ServerLevel level) {
        List<Job> jobs = JOBS_BY_LEVEL.get(level);
        if (jobs == null || jobs.isEmpty()) {
            return;
        }
        synchronized (jobs) {
            Iterator<Job> it = jobs.iterator();
            while (it.hasNext()) {
                Job job = it.next();
                job.advanceOneChunk(level);
                if (job.isDone()) {
                    job.finish(level);
                    it.remove();
                }
            }
            if (jobs.isEmpty()) {
                JOBS_BY_LEVEL.remove(level, jobs);
            }
        }
    }

    @Nullable
    private static BlazingAltarBlockEntity resolveOwner(ServerLevel level, BlockPos altarPos, long ownerInstanceId) {
        if (ownerInstanceId == NO_OWNER_INSTANCE) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(altarPos);
        if (be instanceof BlazingAltarBlockEntity altar && altar.getInstanceId() == ownerInstanceId) {
            return altar;
        }
        return null;
    }

    private static void applyAltarFinish(ServerLevel level, BlockPos altarPos, long ownerInstanceId, int normalFlamesRemoved) {
        BlazingAltarBlockEntity altar = resolveOwner(level, altarPos, ownerInstanceId);
        if (altar != null) {
            altar.completeExtinguishJob(normalFlamesRemoved);
        }
    }

    private static final class Job {
        private final BlockPos altarPos;
        private final List<ChunkPos> chunks;
        private final boolean groundOnly;
        private final FinishMode finishMode;
        private final long ownerInstanceId;

        private int chunkIndex;
        private int normalFlamesRemoved;

        private Job(
                BlockPos altarPos,
                List<ChunkPos> chunks,
                boolean groundOnly,
                FinishMode finishMode,
                long ownerInstanceId) {
            this.altarPos = altarPos;
            this.chunks = chunks;
            this.groundOnly = groundOnly;
            this.finishMode = finishMode;
            this.ownerInstanceId = ownerInstanceId;
        }

        private boolean isDone() {
            return chunkIndex >= chunks.size();
        }

        private void advanceOneChunk(ServerLevel level) {
            if (isDone()) {
                return;
            }
            ChunkPos chunk = chunks.get(chunkIndex);
            BlockPos probe = new BlockPos(chunk.getMinBlockX(), altarPos.getY(), chunk.getMinBlockZ());
            if (level.isLoaded(probe)) {
                Block burningFlame = ModBlocks.BURNING_FLAME.get();
                Block cursedFlame = ModBlocks.CURSED_BURNING_FLAME.get();
                int baseX = chunk.getMinBlockX();
                int baseZ = chunk.getMinBlockZ();
                for (int localX = 0; localX < 16; localX++) {
                    for (int localZ = 0; localZ < 16; localZ++) {
                        scanColumn(level, baseX + localX, baseZ + localZ, burningFlame, cursedFlame);
                    }
                }
            }
            chunkIndex++;
            BlazingAltarBlockEntity altar = resolveOwner(level, altarPos, ownerInstanceId);
            if (altar != null) {
                altar.setExtinguishProgress(chunkIndex, chunks.size());
            }
        }

        private void scanColumn(ServerLevel level, int x, int z, Block burningFlame, Block cursedFlame) {
            int minY = level.getMinY();
            int maxY = groundOnly
                    ? level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 2
                    : level.getMaxY();
            for (int y = minY; y <= maxY; y++) {
                BlockPos pos = new BlockPos(x, y, z);
                Block block = level.getBlockState(pos).getBlock();
                if (block == burningFlame) {
                    level.destroyBlock(pos, false);
                    normalFlamesRemoved++;
                } else if (block == cursedFlame) {
                    level.destroyBlock(pos, false);
                }
            }
        }

        private void finish(ServerLevel level) {
            if (finishMode == FinishMode.FINISH_ALTAR && ownerInstanceId != NO_OWNER_INSTANCE) {
                applyAltarFinish(level, altarPos, ownerInstanceId, normalFlamesRemoved);
            }
        }
    }
}
