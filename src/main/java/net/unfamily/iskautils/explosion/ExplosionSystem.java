package net.unfamily.iskautils.explosion;

import net.neoforged.fml.common.EventBusSubscriber;
import net.unfamily.iskautils.IskaUtils;

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
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Progressive elliptical explosions: ring-ordered sub-regions, block budget per tick,
 * entity damage only when the destruction wave reaches each sub-region (Alex's Caves style).
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class ExplosionSystem {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int SUB_REGION_SIZE = 16;
    private static final int MAX_SUB_REGIONS_PER_TICK = 3;
    private static final int MAX_BLOCKS_PER_TICK = 4096;

    private static final Map<UUID, ExplosionData> ACTIVE_EXPLOSIONS = new ConcurrentHashMap<>();
    private static final AtomicInteger NEXT_EXPLOSION_NUMBER = new AtomicInteger(1);

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        Iterator<Map.Entry<UUID, ExplosionData>> iterator = ACTIVE_EXPLOSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ExplosionData> entry = iterator.next();
            ExplosionData explosion = entry.getValue();

            explosion.tickCount++;

            if (explosion.tickInterval == 0) {
                while (hasWorkRemaining(explosion)) {
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

    public static int createExplosion(
            ServerLevel level,
            BlockPos center,
            int horizontalRadius,
            int verticalRadius,
            int tickInterval) {
        return createExplosion(level, center, horizontalRadius, verticalRadius, tickInterval, 0.0f, false);
    }

    public static int createExplosion(
            ServerLevel level,
            BlockPos center,
            int horizontalRadius,
            int verticalRadius,
            int tickInterval,
            float explosionDamage,
            boolean breakUnbreakable) {

        int number = NEXT_EXPLOSION_NUMBER.getAndIncrement();
        ExplosionData explosion = new ExplosionData(
                number,
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
                "Created explosion #{} ({}) at center {} with radii {}x{}, interval {} ticks, damage {}, break unbreakable: {}",
                number,
                explosion.id,
                center,
                horizontalRadius,
                verticalRadius,
                tickInterval,
                explosionDamage,
                breakUnbreakable);

        return number;
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

    /**
     * Stops a single active explosion by its display number ({@link #createExplosion} return value).
     *
     * @return true if an explosion with that number was active and removed
     */
    public static boolean stopExplosion(int number) {
        Iterator<Map.Entry<UUID, ExplosionData>> iterator = ACTIVE_EXPLOSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            ExplosionData explosion = iterator.next().getValue();
            if (explosion.number == number) {
                explosion.finish();
                iterator.remove();
                LOGGER.info("Stopped explosion #{}", number);
                return true;
            }
        }
        return false;
    }

    public static int getActiveExplosionCount() {
        return ACTIVE_EXPLOSIONS.size();
    }

    public static List<Integer> getActiveExplosionNumbers() {
        List<Integer> numbers = new ArrayList<>(ACTIVE_EXPLOSIONS.size());
        for (ExplosionData explosion : ACTIVE_EXPLOSIONS.values()) {
            numbers.add(explosion.number);
        }
        numbers.sort(Integer::compareTo);
        return numbers;
    }

    public static List<ExplosionInfo> getActiveExplosionInfo() {
        List<ExplosionInfo> list = new ArrayList<>(ACTIVE_EXPLOSIONS.size());
        for (ExplosionData explosion : ACTIVE_EXPLOSIONS.values()) {
            list.add(explosion.toInfo());
        }
        list.sort((a, b) -> Integer.compare(a.number(), b.number()));
        return list;
    }

    private static boolean hasWorkRemaining(ExplosionData explosion) {
        return explosion.activeSubRegion != null || explosion.workGrid.hasPendingWork();
    }

    private static boolean processExplosionTick(ExplosionData explosion) {
        int blocksProcessed = 0;
        int subRegionsProcessed = 0;

        while (hasWorkRemaining(explosion)
                && subRegionsProcessed < MAX_SUB_REGIONS_PER_TICK
                && blocksProcessed < MAX_BLOCKS_PER_TICK) {
            if (explosion.activeSubRegion == null) {
                BlockPos next = explosion.workGrid.pollNextSubRegion();
                if (next == null) {
                    break;
                }
                explosion.activeSubRegion = next;
                explosion.subRegionX = 0;
                explosion.subRegionZ = 0;
                explosion.subRegionY = SUB_REGION_SIZE - 1;
                ensureSubRegionChunkLoaded(explosion, next);
            }

            SubRegionResult result = processSubRegion(
                    explosion,
                    explosion.activeSubRegion,
                    MAX_BLOCKS_PER_TICK - blocksProcessed,
                    explosion.subRegionX,
                    explosion.subRegionZ,
                    explosion.subRegionY);
            blocksProcessed += result.blocksDestroyed();

            if (result.completed()) {
                applyEntityDamageForSubRegion(explosion, explosion.activeSubRegion);
                explosion.workGrid.onSubRegionCompleted();
                explosion.activeSubRegion = null;
                subRegionsProcessed++;
            } else {
                explosion.subRegionX = result.nextX();
                explosion.subRegionZ = result.nextZ();
                explosion.subRegionY = result.nextY();
                break;
            }
        }

        if (hasWorkRemaining(explosion)) {
            LOGGER.debug(
                    "Explosion {} in progress, {} blocks this tick",
                    explosion.id,
                    blocksProcessed);
            return false;
        }

        LOGGER.debug("Explosion {} destroyed {} blocks in final tick batch", explosion.id, blocksProcessed);
        return true;
    }

    private static void ensureSubRegionChunkLoaded(ExplosionData explosion, BlockPos origin) {
        int chunkX = origin.getX() >> 4;
        int chunkZ = origin.getZ() >> 4;
        explosion.level.getChunk(chunkX, chunkZ);
    }

    private record SubRegionResult(int blocksDestroyed, boolean completed, int nextX, int nextZ, int nextY) {}

    private static SubRegionResult processSubRegion(
            ExplosionData explosion,
            BlockPos origin,
            int blockBudget,
            int startX,
            int startZ,
            int startY) {
        int blocksDestroyed = 0;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int x = startX; x < SUB_REGION_SIZE; x++) {
            for (int z = x == startX ? startZ : 0; z < SUB_REGION_SIZE; z++) {
                for (int y = (x == startX && z == startZ) ? startY : SUB_REGION_SIZE - 1; y >= 0; y--) {
                    if (blocksDestroyed >= blockBudget) {
                        return new SubRegionResult(blocksDestroyed, false, x, z, y);
                    }

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
                    explosion.blocksDestroyed++;
                }
            }
        }

        return new SubRegionResult(blocksDestroyed, true, 0, 0, SUB_REGION_SIZE - 1);
    }

    private static float activeSubRegionProgress(int x, int z, int y) {
        int voxelsPerSubRegion = SUB_REGION_SIZE * SUB_REGION_SIZE * SUB_REGION_SIZE;
        int processed = x * SUB_REGION_SIZE * SUB_REGION_SIZE + z * SUB_REGION_SIZE + (SUB_REGION_SIZE - 1 - y);
        return Math.min(1f, processed / (float) voxelsPerSubRegion);
    }

    /**
     * Damage entities only when the destruction wave reaches their sub-region (once per entity).
     */
    private static void applyEntityDamageForSubRegion(ExplosionData explosion, BlockPos origin) {
        if (explosion.explosionDamage <= 0) {
            return;
        }

        float maxDist = explosion.maxEntityDamageDistance;
        if (maxDist <= 0) {
            return;
        }

        AABB subRegionBox = new AABB(
                origin.getX(), origin.getY(), origin.getZ(),
                origin.getX() + SUB_REGION_SIZE,
                origin.getY() + SUB_REGION_SIZE,
                origin.getZ() + SUB_REGION_SIZE);

        Vec3 centerVec = Vec3.atCenterOf(explosion.center);
        DamageSource damageSource = explosion.level.damageSources().explosion(null, null);

        for (LivingEntity entity : explosion.level.getEntitiesOfClass(LivingEntity.class, subRegionBox)) {
            UUID entityId = entity.getUUID();
            if (!explosion.damagedEntities.add(entityId)) {
                continue;
            }

            float dist = (float) entity.position().distanceTo(centerVec);
            if (dist > maxDist) {
                explosion.damagedEntities.remove(entityId);
                continue;
            }

            float damage = explosion.explosionDamage * (maxDist - dist) / maxDist;
            if (damage > 0) {
                entity.hurt(damageSource, damage);
            }
        }
    }

    private static boolean isInsideEllipse(ExplosionData explosion, BlockPos pos) {
        int h = Math.max(1, explosion.horizontalRadius);
        int v = Math.max(1, explosion.verticalRadius);
        double dx = (pos.getX() + 0.5 - explosion.center.getX()) / (double) h;
        double dy = (pos.getY() + 0.5 - explosion.center.getY()) / (double) v;
        double dz = (pos.getZ() + 0.5 - explosion.center.getZ()) / (double) h;
        return dx * dx + dy * dy + dz * dz <= 1.0001;
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

    private static int alignDown(int coord, int size) {
        return Math.floorDiv(coord, size) * size;
    }

    /**
     * Lazily yields sub-region origins ring-by-ring (outer first) without sorting thousands of positions at creation.
     */
    private static final class SubRegionWorkGrid {
        private final int minOx;
        private final int minOy;
        private final int minOz;
        private final int maxOx;
        private final int maxOy;
        private final int maxOz;
        private final int centerIx;
        private final int centerIy;
        private final int centerIz;
        private final int countX;
        private final int countY;
        private final int countZ;
        private final int maxRing;

        private int currentRing = -1;
        private final Deque<BlockPos> ringQueue = new ArrayDeque<>();
        private int scheduledSubRegions;
        private int completedSubRegions;

        void onSubRegionCompleted() {
            completedSubRegions++;
        }

        int getCurrentRing() {
            return Math.max(0, currentRing);
        }

        int getMaxRing() {
            return maxRing;
        }

        int getScheduledSubRegions() {
            return scheduledSubRegions;
        }

        int getCompletedSubRegions() {
            return completedSubRegions;
        }

        SubRegionWorkGrid(BlockPos center, int horizontalRadius, int verticalRadius) {
            int h = Math.max(1, horizontalRadius);
            int v = Math.max(1, verticalRadius);

            minOx = alignDown(center.getX() - h, SUB_REGION_SIZE);
            maxOx = alignDown(center.getX() + h, SUB_REGION_SIZE);
            minOy = alignDown(center.getY() - v, SUB_REGION_SIZE);
            maxOy = alignDown(center.getY() + v, SUB_REGION_SIZE);
            minOz = alignDown(center.getZ() - h, SUB_REGION_SIZE);
            maxOz = alignDown(center.getZ() + h, SUB_REGION_SIZE);

            int centerOx = alignDown(center.getX(), SUB_REGION_SIZE);
            int centerOy = alignDown(center.getY(), SUB_REGION_SIZE);
            int centerOz = alignDown(center.getZ(), SUB_REGION_SIZE);

            countX = (maxOx - minOx) / SUB_REGION_SIZE + 1;
            countY = (maxOy - minOy) / SUB_REGION_SIZE + 1;
            countZ = (maxOz - minOz) / SUB_REGION_SIZE + 1;

            centerIx = (centerOx - minOx) / SUB_REGION_SIZE;
            centerIy = (centerOy - minOy) / SUB_REGION_SIZE;
            centerIz = (centerOz - minOz) / SUB_REGION_SIZE;

            int cornerA = centerIx + centerIy + centerIz;
            int cornerB = (countX - 1 - centerIx) + (countY - 1 - centerIy) + (countZ - 1 - centerIz);
            maxRing = Math.max(cornerA, cornerB);
        }

        boolean hasPendingWork() {
            return !ringQueue.isEmpty() || currentRing < maxRing;
        }

        BlockPos pollNextSubRegion() {
            if (ringQueue.isEmpty()) {
                advanceToNextRing();
            }
            return ringQueue.pollFirst();
        }

        private void advanceToNextRing() {
            ringQueue.clear();
            currentRing++;
            if (currentRing > maxRing) {
                return;
            }

            int ring = currentRing;
            for (int ix = 0; ix < countX; ix++) {
                for (int iy = 0; iy < countY; iy++) {
                    int dz = ring - Math.abs(ix - centerIx) - Math.abs(iy - centerIy);
                    if (dz < 0) {
                        continue;
                    }
                    if (dz == 0) {
                        addIfValid(ix, iy, centerIz);
                    } else {
                        addIfValid(ix, iy, centerIz + dz);
                        if (dz != 0) {
                            addIfValid(ix, iy, centerIz - dz);
                        }
                    }
                }
            }
        }

        private void addIfValid(int ix, int iy, int iz) {
            if (iz < 0 || iz >= countZ) {
                return;
            }
            ringQueue.addLast(new BlockPos(
                    minOx + ix * SUB_REGION_SIZE,
                    minOy + iy * SUB_REGION_SIZE,
                    minOz + iz * SUB_REGION_SIZE));
            scheduledSubRegions++;
        }
    }

    private static final class ExplosionData {
        final int number;
        final UUID id;
        final ServerLevel level;
        final BlockPos center;
        final int horizontalRadius;
        final int verticalRadius;
        final int tickInterval;
        final float explosionDamage;
        final boolean breakUnbreakable;
        final float maxEntityDamageDistance;
        final SubRegionWorkGrid workGrid;
        final Set<UUID> damagedEntities = new HashSet<>();

        BlockPos activeSubRegion;
        int subRegionX;
        int subRegionZ;
        int subRegionY;
        int tickCount = 0;
        int blocksDestroyed;

        ExplosionData(
                int number,
                UUID id,
                ServerLevel level,
                BlockPos center,
                int horizontalRadius,
                int verticalRadius,
                int tickInterval,
                float explosionDamage,
                boolean breakUnbreakable) {
            this.number = number;
            this.id = id;
            this.level = level;
            this.center = center;
            this.horizontalRadius = horizontalRadius;
            this.verticalRadius = verticalRadius;
            this.tickInterval = tickInterval;
            this.explosionDamage = explosionDamage;
            this.breakUnbreakable = breakUnbreakable;
            this.maxEntityDamageDistance = Math.max(horizontalRadius, verticalRadius) * 1.5f + 1;
            this.workGrid = new SubRegionWorkGrid(center, horizontalRadius, verticalRadius);
            this.activeSubRegion = null;
            this.subRegionX = 0;
            this.subRegionZ = 0;
            this.subRegionY = SUB_REGION_SIZE - 1;
        }

        void finish() {
            activeSubRegion = null;
            damagedEntities.clear();
        }

        ExplosionInfo toInfo() {
            float partial = activeSubRegion != null
                    ? activeSubRegionProgress(subRegionX, subRegionZ, subRegionY)
                    : 0f;
            int scheduled = Math.max(1, workGrid.getScheduledSubRegions());
            float progressUnits = workGrid.getCompletedSubRegions() + partial;
            float percent = Math.min(100f, progressUnits * 100f / scheduled);

            return new ExplosionInfo(
                    number,
                    level.dimension().location().toString(),
                    center,
                    horizontalRadius,
                    verticalRadius,
                    tickInterval,
                    explosionDamage,
                    breakUnbreakable,
                    workGrid.getCurrentRing(),
                    workGrid.getMaxRing(),
                    workGrid.getCompletedSubRegions(),
                    workGrid.getScheduledSubRegions(),
                    percent,
                    blocksDestroyed,
                    activeSubRegion,
                    partial);
        }
    }

    private ExplosionSystem() {}
}
