package net.unfamily.iskautils.util.preview;

import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side footprint generation counter per machine owner block. */
public final class MachinePreviewServerTracker {

    private static final Map<BlockPos, Integer> footprintGenerationByOwner = new ConcurrentHashMap<>();

    private MachinePreviewServerTracker() {}

    public static int nextFootprintGeneration(BlockPos owner) {
        return footprintGenerationByOwner.merge(owner.immutable(), 1, Integer::sum);
    }

    public static void clearGeneration(BlockPos owner) {
        if (owner != null) {
            footprintGenerationByOwner.remove(owner.immutable());
        }
    }
}
