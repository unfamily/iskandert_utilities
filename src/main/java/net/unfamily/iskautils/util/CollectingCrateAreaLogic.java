package net.unfamily.iskautils.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

/**
 * Collection volume behind the crate facing (same axis layout as Colossal Reactor Builder).
 */
public final class CollectingCrateAreaLogic {

    private CollectingCrateAreaLogic() {}

    public static int blockWidth(int sizeLeft, int sizeRight) {
        return sizeLeft + sizeRight + 1;
    }

    public static int blockHeight(int sizeHeight) {
        return sizeHeight + 1;
    }

    public static int blockDepth(int sizeDepth) {
        return sizeDepth + 1;
    }

    /**
     * Returns the collection AABB in world coordinates (block-aligned).
     * Volume starts one block behind the crate (opposite of facing).
     */
    public static AABB getCollectionVolumeAABB(
            BlockPos cratePos,
            Direction facing,
            int sizeLeft,
            int sizeRight,
            int sizeHeight,
            int sizeDepth) {
        Direction back = facing.getOpposite();
        Direction left = facing.getCounterClockWise(Direction.Axis.Y);
        Direction right = facing.getClockWise(Direction.Axis.Y);
        BlockPos base = cratePos.relative(back, 1);
        int minX = base.getX();
        int maxX = base.getX();
        int minY = base.getY();
        int maxY = base.getY();
        int minZ = base.getZ();
        int maxZ = base.getZ();
        for (int dl = 0; dl <= sizeLeft; dl++) {
            for (int dr = 0; dr <= sizeRight; dr++) {
                for (int dy = 0; dy <= sizeHeight; dy++) {
                    for (int dd = 0; dd <= sizeDepth; dd++) {
                        int x = base.getX() + left.getStepX() * dl + right.getStepX() * dr + back.getStepX() * dd;
                        int y = base.getY() + Direction.UP.getStepY() * dy + left.getStepY() * dl + right.getStepY() * dr + back.getStepY() * dd;
                        int z = base.getZ() + left.getStepZ() * dl + right.getStepZ() * dr + back.getStepZ() * dd;
                        minX = Math.min(minX, x);
                        maxX = Math.max(maxX, x);
                        minY = Math.min(minY, y);
                        maxY = Math.max(maxY, y);
                        minZ = Math.min(minZ, z);
                        maxZ = Math.max(maxZ, z);
                    }
                }
            }
        }
        return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }
}
