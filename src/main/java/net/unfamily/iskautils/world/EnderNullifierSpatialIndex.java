package net.unfamily.iskautils.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.unfamily.iskautils.block.entity.NullifierTargetMode;

import java.util.Set;

/** @deprecated Use {@link NullifierSpatialIndex}; kept for call-site compatibility during migration. */
@Deprecated
public final class EnderNullifierSpatialIndex {
    private EnderNullifierSpatialIndex() {}

    public static void update(ResourceKey<Level> dimension, BlockPos pos, boolean active, int radius) {
        NullifierSpatialIndex.update(
                dimension, pos, NullifierSpatialIndex.Kind.ENDER, active, radius, NullifierTargetMode.ONLY_MOBS);
    }

    public static void update(
            ResourceKey<Level> dimension,
            BlockPos pos,
            boolean active,
            int radius,
            NullifierTargetMode targetMode) {
        NullifierSpatialIndex.update(dimension, pos, NullifierSpatialIndex.Kind.ENDER, active, radius, targetMode);
    }

    public static void remove(ResourceKey<Level> dimension, BlockPos pos) {
        NullifierSpatialIndex.remove(dimension, pos);
    }

    public static Set<BlockPos> getNullifiersInChunk(ResourceKey<Level> dimension, ChunkPos chunkPos) {
        return NullifierSpatialIndex.getNullifiersInChunk(dimension, chunkPos);
    }

    public static boolean isTeleportBlocked(ResourceKey<Level> dimension, Vec3 position) {
        return NullifierSpatialIndex.isTeleportBlocked(dimension, position);
    }
}
