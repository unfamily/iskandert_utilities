package net.unfamily.iskautils.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;

/**
 * Mob Grinding Utils dreadful/delightful dirt parity for redstone-accelerated soil spawning.
 */
public final class SoilAcceleratedSpawn {
    /** Same inflate as MGU dreadful/delightful dirt ({@code new AABB(pos).inflate(5, 2, 5)}). */
    public static final int AREA_INFLATE_HORIZONTAL = 5;
    public static final int AREA_INFLATE_VERTICAL = 2;
    /** Max mobs counted directly above a single soil block during normal (slow) spawn tries. */
    public static final int PER_BLOCK_MOB_CAP = 1;

    private SoilAcceleratedSpawn() {
    }

    public static AABB spawnCheckArea(BlockPos soilPos) {
        return new AABB(soilPos).inflate(AREA_INFLATE_HORIZONTAL, AREA_INFLATE_VERTICAL, AREA_INFLATE_HORIZONTAL);
    }

    /** Column from soil up to two blocks above (one mob per tile during slow spawn). */
    public static AABB perBlockSpawnCheckArea(BlockPos soilPos) {
        return new AABB(
                soilPos.getX(), soilPos.getY(), soilPos.getZ(),
                soilPos.getX() + 1, soilPos.getY() + 3, soilPos.getZ() + 1);
    }

    public static boolean isUnderHostileCap(ServerLevel level, BlockPos soilPos, int cap, boolean accelerated) {
        if (cap <= 0) {
            return true;
        }
        AABB area = accelerated ? spawnCheckArea(soilPos) : perBlockSpawnCheckArea(soilPos);
        int count = level.getEntitiesOfClass(Mob.class, area, mob -> mob != null && mob instanceof Enemy).size();
        return count < cap;
    }

    public static boolean isUnderCreatureCap(ServerLevel level, BlockPos soilPos, int cap, boolean accelerated) {
        if (cap <= 0) {
            return true;
        }
        AABB area = accelerated ? spawnCheckArea(soilPos) : perBlockSpawnCheckArea(soilPos);
        int count = level.getEntitiesOfClass(Mob.class, area,
                mob -> mob != null && mob.getType().getCategory() == MobCategory.CREATURE).size();
        return count < cap;
    }
}
