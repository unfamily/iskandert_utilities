package net.unfamily.iskautils.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.Map;

/**
 * Shared checks before placing scripted structures (item, machine, monouse).
 */
public final class StructurePlacementMobChecks {

    private StructurePlacementMobChecks() {}

    public static AABB boundsAroundPositions(Iterable<BlockPos> positions) {
        boolean first = true;
        int minX = 0;
        int minY = 0;
        int minZ = 0;
        int maxX = 0;
        int maxY = 0;
        int maxZ = 0;
        for (BlockPos pos : positions) {
            if (first) {
                minX = maxX = pos.getX();
                minY = maxY = pos.getY();
                minZ = maxZ = pos.getZ();
                first = false;
            } else {
                minX = Math.min(minX, pos.getX());
                minY = Math.min(minY, pos.getY());
                minZ = Math.min(minZ, pos.getZ());
                maxX = Math.max(maxX, pos.getX());
                maxY = Math.max(maxY, pos.getY());
                maxZ = Math.max(maxZ, pos.getZ());
            }
        }
        if (first) {
            return new AABB(0, 0, 0, 0, 0, 0);
        }
        return new AABB(minX, minY, minZ, maxX + 1.0, maxY + 1.0, maxZ + 1.0);
    }

    /**
     * @param blockPositions keys are world positions to be filled
     */
    public static boolean hasNonPlayerLivingMobIn(ServerLevel level, Map<BlockPos, ?> blockPositions) {
        if (blockPositions.isEmpty()) {
            return false;
        }
        AABB box = boundsAroundPositions(blockPositions.keySet());
        return !level.getEntitiesOfClass(LivingEntity.class, box, e -> !(e instanceof Player) && e.isAlive()).isEmpty();
    }
}
