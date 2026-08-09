package net.unfamily.iskautils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.unfamily.iskalib.client.marker.AreaBorderRenderer;
import net.unfamily.iskalib.client.marker.MarkRenderer;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client preview for Blazing Altar chunk coverage: four full-height corner pillars
 * at the extreme Chebyshev chunk corners (classic billboard markers, not a cube shell).
 */
public final class BlazingAltarAreaPreview {
    /** Same magenta as other machine previews. */
    private static final int PILLAR_COLOR = AreaBorderRenderer.DEFAULT_MACHINE_COLOR;
    private static final int DURATION_TICKS = MachinePreviewTracker.MACHINE_PREVIEW_DURATION_TICKS;
    /** Vertical sample step — solid enough to read as a pillar without flooding the marker map. */
    private static final int Y_STEP = 2;

    private static final Set<BlockPos> activeOwners = ConcurrentHashMap.newKeySet();

    private BlazingAltarAreaPreview() {}

    public static boolean isActive(BlockPos owner) {
        return owner != null && activeOwners.contains(owner.immutable());
    }

    public static void setActive(BlockPos owner, boolean active) {
        if (owner == null) {
            return;
        }
        BlockPos key = owner.immutable();
        if (active) {
            activeOwners.add(key);
        } else {
            activeOwners.remove(key);
            MarkRenderer.getInstance().clearBillboardMarkersForOwner(key);
        }
    }

    /**
     * Rebuilds the four corner pillars for the altar's Chebyshev chunk square.
     *
     * @param owner        altar block position (marker owner key)
     * @param chunkRadius  Chebyshev chunk radius ({@code max(|dx|,|dz|) <= radius})
     */
    public static void refresh(BlockPos owner, int chunkRadius) {
        if (owner == null || !activeOwners.contains(owner.immutable())) {
            return;
        }
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        int radius = Math.max(1, chunkRadius);
        BlockPos key = owner.immutable();
        MarkRenderer.getInstance().clearBillboardMarkersForOwner(key);

        ChunkPos origin = new ChunkPos(owner);
        int minChunkX = origin.x - radius;
        int maxChunkX = origin.x + radius;
        int minChunkZ = origin.z - radius;
        int maxChunkZ = origin.z + radius;

        int minBlockX = minChunkX << 4;
        int maxBlockX = (maxChunkX << 4) + 15;
        int minBlockZ = minChunkZ << 4;
        int maxBlockZ = (maxChunkZ << 4) + 15;

        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        int[] cornerX = {minBlockX, maxBlockX};
        int[] cornerZ = {minBlockZ, maxBlockZ};
        MarkRenderer renderer = MarkRenderer.getInstance();
        for (int x : cornerX) {
            for (int z : cornerZ) {
                for (int y = minY; y < maxY; y += Y_STEP) {
                    renderer.addBillboardMarker(key, new BlockPos(x, y, z), PILLAR_COLOR, DURATION_TICKS);
                }
            }
        }
    }

    public static void clear(BlockPos owner) {
        if (owner == null) {
            return;
        }
        BlockPos key = owner.immutable();
        activeOwners.remove(key);
        MarkRenderer.getInstance().clearBillboardMarkersForOwner(key);
    }

    public static void clearAll() {
        MarkRenderer renderer = MarkRenderer.getInstance();
        for (BlockPos key : activeOwners) {
            renderer.clearBillboardMarkersForOwner(key);
        }
        activeOwners.clear();
    }
}
