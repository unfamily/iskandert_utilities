package net.unfamily.iskautils.explosion;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.unfamily.iskautils.IskaUtils;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Progressive elliptical explosions using a chunk-subregion work queue.
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public class ExplosionSystem {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int SUB_REGION_SIZE = 16;
    private static final int MAX_SUB_REGIONS_PER_TICK = 3;
    private static final int MAX_BLOCKS_PER_TICK = 4096;
    private static final int CHUNK_TICKET_RADIUS_THRESHOLD = 128;

    private static final Map<UUID, ExplosionData> ACTIVE_EXPLOSIONS = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        Iterator<Map.Entry<UUID, ExplosionData>> iterator = ACTIVE_EXPLOSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ExplosionData> entry = iterator.next();
            ExplosionData explosion = entry.getValue();

            explosion.tickCount++;

            if (explosion.tickInterval == 0) {
                while (!explosion.workQueue.isEmpty()) {
                    if (processExplosionTick(explosion)) {
                        break;
                    }
                }
                explosion.finish();
                iterator.remove();
                LOGGER.debug("Instant explosion {} completed", entry.getKey());
            } else if (explosion.tickCount >= explosion.tickInterval) {
                explosion.tickCount = 0;

                if (processExplosionTick(explosion)) {
                    explosion.finish();
                    iterator.remove();
                    LOGGER.debug("Explosion {} completed", entry.getKey());
                }
            }
        }
    }

    public static UUID createExplosion(
            ServerLevel level,
            BlockPos center,
            int horizontalRadius,
            int verticalRadius,
            int tickInterval) {
        return createExplosion(level, center, horizontalRadius, verticalRadius, tickInterval, 0.0f, false);
    }

    public static UUID createExplosion(
            ServerLevel level,
            BlockPos center,
            int horizontalRadius,
            int verticalRadius,
            int tickInterval,
            float explosionDamage,
            boolean breakUnbreakable) {

        ExplosionData explosion = new ExplosionData(
                UUID.randomUUID(),
                level,
                center,
                horizontalRadius,
                verticalRadius,
                tickInterval,
                explosionDamage,
                breakUnbreakable);

        ACTIVE_EXPLOSIONS.put(explosion.id, explosion);

        LOGGER.info(
                "Created explosion {} at center {} with radii {}x{}, interval {} ticks, damage {}, break unbreakable: {}",
                explosion.id,
                center,
                horizontalRadius,
                verticalRadius,
                tickInterval,
                explosionDamage,
                breakUnbreakable);

        return explosion.id;
    }

    public static int stopAllExplosions() {
        int count = ACTIVE_EXPLOSIONS.size();
        for (ExplosionData explosion : ACTIVE_EXPLOSIONS.values()) {
            explosion.finish();
        }
        ACTIVE_EXPLOSIONS.clear();
        LOGGER.info("Stopped {} active explosions", count);
        return count;
    }

    public static int getActiveExplosionCount() {
        return ACTIVE_EXPLOSIONS.size();
    }

    private static boolean processExplosionTick(ExplosionData explosion) {
        if (explosion.explosionDamage > 0) {
            applyEntityDamage(explosion);
        }

        int blocksProcessed = 0;
        int subRegionsProcessed = 0;

        while (!explosion.workQueue.isEmpty()
                && subRegionsProcessed < MAX_SUB_REGIONS_PER_TICK
                && blocksProcessed < MAX_BLOCKS_PER_TICK) {
            BlockPos subRegionOrigin = explosion.workQueue.pop();
            blocksProcessed += processSubRegion(explosion, subRegionOrigin, MAX_BLOCKS_PER_TICK - blocksProcessed);
            subRegionsProcessed++;
        }

        if (!explosion.workQueue.isEmpty()) {
            LOGGER.debug(
                    "Explosion {} progress: {} sub-regions remaining, {} blocks this tick",
                    explosion.id,
                    explosion.workQueue.size(),
                    blocksProcessed);
            return false;
        }

        LOGGER.debug("Explosion {} destroyed {} blocks in final tick batch", explosion.id, blocksProcessed);
        return true;
    }

    private static int processSubRegion(ExplosionData explosion, BlockPos origin, int blockBudget) {
        int blocksDestroyed = 0;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int x = 0; x < SUB_REGION_SIZE && blocksDestroyed < blockBudget; x++) {
            for (int z = 0; z < SUB_REGION_SIZE && blocksDestroyed < blockBudget; z++) {
                for (int y = SUB_REGION_SIZE - 1; y >= 0 && blocksDestroyed < blockBudget; y--) {
                    mutable.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);

                    if (!explosion.level.isInWorldBounds(mutable)) {
                        continue;
                    }
                    if (!isInsideEllipse(explosion, mutable)) {
                        continue;
                    }

                    BlockState state = explosion.level.getBlockState(mutable);
                    if (!shouldDestroy(explosion, state, mutable)) {
                        continue;
                    }

                    explosion.level.setBlock(mutable, Blocks.AIR.defaultBlockState(), 3);
                    blocksDestroyed++;
                }
            }
        }

        return blocksDestroyed;
    }

    private static boolean isInsideEllipse(ExplosionData explosion, BlockPos pos) {
        int h = Math.max(1, explosion.horizontalRadius);
        int v = Math.max(1, explosion.verticalRadius);
        double dx = (pos.getX() - explosion.center.getX()) / (double) h;
        double dy = (pos.getY() - explosion.center.getY()) / (double) v;
        double dz = (pos.getZ() - explosion.center.getZ()) / (double) h;
        return dx * dx + dy * dy + dz * dz <= 1.0;
    }

    private static boolean shouldDestroy(ExplosionData explosion, BlockState state, BlockPos pos) {
        if (state.isAir()) {
            return false;
        }
        if (explosion.breakUnbreakable) {
            return true;
        }
        return !state.is(Blocks.BEDROCK)
                && !state.is(Blocks.BARRIER)
                && state.getDestroySpeed(explosion.level, pos) >= 0;
    }

    private static void applyEntityDamage(ExplosionData explosion) {
        float maxDist = explosion.maxEntityDamageDistance;
        if (maxDist <= 0) {
            return;
        }

        AABB damageBox = new AABB(explosion.center).inflate(
                explosion.horizontalRadius * 1.5,
                explosion.verticalRadius * 0.6 + 1,
                explosion.horizontalRadius * 1.5);

        Vec3 centerVec = Vec3.atCenterOf(explosion.center);
        DamageSource damageSource = explosion.level.damageSources().explosion(null, null);

        for (LivingEntity entity : explosion.level.getEntitiesOfClass(LivingEntity.class, damageBox)) {
            float dist = (float) entity.position().distanceTo(centerVec);
            if (dist > maxDist) {
                continue;
            }
            float damage = explosion.explosionDamage * (maxDist - dist) / maxDist;
            if (damage > 0) {
                entity.hurt(damageSource, damage);
            }
        }
    }

    private static Deque<BlockPos> buildWorkQueue(BlockPos center, int horizontalRadius, int verticalRadius) {
        int h = Math.max(1, horizontalRadius);
        int v = Math.max(1, verticalRadius);

        int minX = alignDown(center.getX() - h, SUB_REGION_SIZE);
        int maxX = alignDown(center.getX() + h, SUB_REGION_SIZE);
        int minY = alignDown(center.getY() - v, SUB_REGION_SIZE);
        int maxY = alignDown(center.getY() + v, SUB_REGION_SIZE);
        int minZ = alignDown(center.getZ() - h, SUB_REGION_SIZE);
        int maxZ = alignDown(center.getZ() + h, SUB_REGION_SIZE);

        List<BlockPos> origins = new ArrayList<>();
        for (int ox = minX; ox <= maxX; ox += SUB_REGION_SIZE) {
            for (int oy = minY; oy <= maxY; oy += SUB_REGION_SIZE) {
                for (int oz = minZ; oz <= maxZ; oz += SUB_REGION_SIZE) {
                    origins.add(new BlockPos(ox, oy, oz));
                }
            }
        }

        origins.sort((a, b) -> Integer.compare(
                b.distManhattan(center),
                a.distManhattan(center)));

        return new ArrayDeque<>(origins);
    }

    private static int alignDown(int coord, int size) {
        return Math.floorDiv(coord, size) * size;
    }

    private static void preloadChunks(ServerLevel level, BlockPos center, int horizontalRadius, int verticalRadius) {
        int chunkRadius = Math.max(1, Math.max(horizontalRadius, verticalRadius) / 16 + 1);
        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                level.getChunk(centerChunkX + dx, centerChunkZ + dz);
            }
        }
    }

    private static final class ExplosionData {
        final UUID id;
        final ServerLevel level;
        final BlockPos center;
        final int horizontalRadius;
        final int verticalRadius;
        final int tickInterval;
        final float explosionDamage;
        final boolean breakUnbreakable;
        final float maxEntityDamageDistance;
        final Deque<BlockPos> workQueue;

        int tickCount = 0;

        ExplosionData(
                UUID id,
                ServerLevel level,
                BlockPos center,
                int horizontalRadius,
                int verticalRadius,
                int tickInterval,
                float explosionDamage,
                boolean breakUnbreakable) {
            this.id = id;
            this.level = level;
            this.center = center;
            this.horizontalRadius = horizontalRadius;
            this.verticalRadius = verticalRadius;
            this.tickInterval = tickInterval;
            this.explosionDamage = explosionDamage;
            this.breakUnbreakable = breakUnbreakable;
            this.maxEntityDamageDistance = Math.max(horizontalRadius, verticalRadius) * 1.5f + 1;
            this.workQueue = buildWorkQueue(center, horizontalRadius, verticalRadius);

            int maxRadius = Math.max(horizontalRadius, verticalRadius);
            if (maxRadius > CHUNK_TICKET_RADIUS_THRESHOLD) {
                preloadChunks(level, center, horizontalRadius, verticalRadius);
            }
        }

        void finish() {
            // Reserved for future ticket cleanup
        }
    }
}
