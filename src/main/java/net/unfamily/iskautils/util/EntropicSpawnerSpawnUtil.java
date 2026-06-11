package net.unfamily.iskautils.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.unfamily.iskautils.Config;

/**
 * Spawn placement helpers for the Entropic Spawner (above-block spawn, mod-friendly collision).
 */
public final class EntropicSpawnerSpawnUtil {
    private EntropicSpawnerSpawnUtil() {}

    private static final int MOB_COUNT_HORIZONTAL_RANGE = 4;
    private static final int MOB_COUNT_VERTICAL_HEIGHT = 16;

    /**
     * Living entities in the column above the spawner (excluding players).
     */
    public static int countLivingEntitiesAbove(ServerLevel level, BlockPos spawnerPos) {
        BlockPos above = spawnerPos.above();
        AABB box = new AABB(
                above.getX() - MOB_COUNT_HORIZONTAL_RANGE,
                above.getY(),
                above.getZ() - MOB_COUNT_HORIZONTAL_RANGE,
                above.getX() + MOB_COUNT_HORIZONTAL_RANGE + 1.0D,
                above.getY() + MOB_COUNT_VERTICAL_HEIGHT,
                above.getZ() + MOB_COUNT_HORIZONTAL_RANGE + 1.0D);
        return level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive).size();
    }

    public static boolean isMobCapReachedAbove(ServerLevel level, BlockPos spawnerPos) {
        int cap = Config.entropicSpawnerMaxMobsAbove;
        if (cap <= 0) {
            return false;
        }
        return countLivingEntitiesAbove(level, spawnerPos) >= cap;
    }

    /**
     * Spawns one mob centered on the block above the spawner.
     * Ignores existing entities and vanilla spawn rules; only solid block collision is checked.
     */
    public static boolean trySpawnOneRelaxed(ServerLevel level, BlockPos spawnPos, EntityType<?> type) {
        Entity entity = type.create(level);
        if (!(entity instanceof Mob mob)) {
            if (entity != null) {
                entity.discard();
            }
            return false;
        }
        double x = spawnPos.getX() + 0.5D;
        double y = spawnPos.getY();
        double z = spawnPos.getZ() + 0.5D;
        mob.moveTo(x, y, z, level.random.nextFloat() * 360.0F, 0.0F);
        if (hasBlockingCollision(level, mob.getBoundingBox())) {
            mob.discard();
            return false;
        }
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.SPAWNER, null);
        if (!level.addFreshEntity(mob)) {
            mob.discard();
            return false;
        }
        return true;
    }

    private static boolean hasBlockingCollision(ServerLevel level, AABB entityBox) {
        int minX = (int) Math.floor(entityBox.minX);
        int minY = (int) Math.floor(entityBox.minY);
        int minZ = (int) Math.floor(entityBox.minZ);
        int maxX = (int) Math.floor(entityBox.maxX);
        int maxY = (int) Math.floor(entityBox.maxY);
        int maxZ = (int) Math.floor(entityBox.maxZ);

        VoxelShape entityShape = Shapes.create(entityBox);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }
                    VoxelShape blockShape = state.getCollisionShape(level, pos);
                    if (blockShape.isEmpty()) {
                        continue;
                    }
                    VoxelShape moved = blockShape.move(pos.getX(), pos.getY(), pos.getZ());
                    if (Shapes.joinIsNotEmpty(moved, entityShape, BooleanOp.AND)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
