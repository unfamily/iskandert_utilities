package net.unfamily.iskautils.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.BlazingAltarBlockEntity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Removes blazing-altar flames one chunk per server tick (nearest to farthest) to avoid stalls
 * when placement and removal compete on large radii.
 */
public final class BlazingAltarExtinguishJobs {
    private static final Map<ServerLevel, List<Job>> JOBS_BY_LEVEL = new ConcurrentHashMap<>();

    public enum FinishMode {
        FLAMES_ONLY,
        FINISH_ALTAR
    }

    private BlazingAltarExtinguishJobs() {}

    public static void enqueueFromAltar(ServerLevel level, BlazingAltarBlockEntity altar, FinishMode finishMode) {
        enqueue(level, altar.getBlockPos(), altar.getChunkRadius(), altar.isGroundOnly(), finishMode);
    }

    public static boolean hasJob(ServerLevel level, BlockPos altarPos) {
        List<Job> jobs = JOBS_BY_LEVEL.get(level);
        if (jobs == null) {
            return false;
        }
        synchronized (jobs) {
            for (Job job : jobs) {
                if (job.altarPos.equals(altarPos)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void cancelForAltar(ServerLevel level, BlockPos altarPos) {
        List<Job> jobs = JOBS_BY_LEVEL.get(level);
        if (jobs == null) {
            return;
        }
        synchronized (jobs) {
            jobs.removeIf(job -> job.altarPos.equals(altarPos));
            if (jobs.isEmpty()) {
                JOBS_BY_LEVEL.remove(level, jobs);
            }
        }
    }

    public static void enqueue(ServerLevel level, BlockPos altarPos, int chunkRadius, boolean groundOnly, FinishMode finishMode) {
        List<ChunkPos> chunks = BlazingAltarChunks.collectOrdered(altarPos, chunkRadius);
        if (chunks.isEmpty()) {
            if (finishMode == FinishMode.FINISH_ALTAR) {
                applyAltarFinish(level, altarPos, 0);
            }
            return;
        }
        BlockEntity be = level.getBlockEntity(altarPos);
        if (be instanceof BlazingAltarBlockEntity altar) {
            altar.setExtinguishProgress(0, chunks.size());
        }
        List<Job> jobs = JOBS_BY_LEVEL.computeIfAbsent(level, ignored -> new ArrayList<>());
        synchronized (jobs) {
            jobs.removeIf(job -> job.altarPos.equals(altarPos));
            jobs.add(new Job(altarPos, chunks, groundOnly, finishMode));
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

    private static void applyAltarFinish(ServerLevel level, BlockPos altarPos, int normalFlamesRemoved) {
        BlockEntity be = level.getBlockEntity(altarPos);
        if (be instanceof BlazingAltarBlockEntity altar) {
            altar.completeExtinguishJob(normalFlamesRemoved);
        }
    }

    private static final class Job {
        private final BlockPos altarPos;
        private final List<ChunkPos> chunks;
        private final boolean groundOnly;
        private final FinishMode finishMode;

        private int chunkIndex;
        private int normalFlamesRemoved;

        private Job(BlockPos altarPos, List<ChunkPos> chunks, boolean groundOnly, FinishMode finishMode) {
            this.altarPos = altarPos;
            this.chunks = chunks;
            this.groundOnly = groundOnly;
            this.finishMode = finishMode;
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
            BlockEntity be = level.getBlockEntity(altarPos);
            if (be instanceof BlazingAltarBlockEntity altar) {
                altar.setExtinguishProgress(chunkIndex, chunks.size());
            }
        }

        private void scanColumn(ServerLevel level, int x, int z, Block burningFlame, Block cursedFlame) {
            int minY = level.getMinBuildHeight();
            int maxY = groundOnly
                    ? level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 2
                    : level.getMaxBuildHeight() - 1;
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
            if (finishMode == FinishMode.FINISH_ALTAR) {
                applyAltarFinish(level, altarPos, normalFlamesRemoved);
            }
        }
    }
}
