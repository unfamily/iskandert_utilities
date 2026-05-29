package net.unfamily.iskautils.explosion;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Read-only status of an active progressive explosion (for commands / debug).
 */
public record ExplosionInfo(
        int number,
        String dimensionId,
        BlockPos center,
        int horizontalRadius,
        int verticalRadius,
        int tickInterval,
        float explosionDamage,
        boolean breakUnbreakable,
        int waveRing,
        int maxWaveRing,
        int completedSubRegions,
        int scheduledSubRegions,
        float completionPercent,
        int blocksDestroyed,
        @Nullable BlockPos activeSubRegionOrigin,
        float activeSubRegionPartial) {
}
