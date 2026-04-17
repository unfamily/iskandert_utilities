package net.unfamily.iskautils.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

/**
 * Stable façade re-export for integrations that reflect or reference {@code net.unfamily.iskautils.explosion.ExplosionSystem}.
 * Implementation: {@link net.unfamily.iskalib.explosion.ExplosionSystem}.
 */
public final class ExplosionSystem {

    private ExplosionSystem() {}

    public static UUID createExplosion(
            ServerLevel level,
            BlockPos center,
            int horizontalRadius,
            int verticalRadius,
            int tickInterval) {
        return net.unfamily.iskalib.explosion.ExplosionSystem.createExplosion(
                level, center, horizontalRadius, verticalRadius, tickInterval);
    }

    public static UUID createExplosion(
            ServerLevel level,
            BlockPos center,
            int horizontalRadius,
            int verticalRadius,
            int tickInterval,
            float explosionDamage,
            boolean breakUnbreakable) {
        return net.unfamily.iskalib.explosion.ExplosionSystem.createExplosion(
                level, center, horizontalRadius, verticalRadius, tickInterval, explosionDamage, breakUnbreakable);
    }

    public static int stopAllExplosions() {
        return net.unfamily.iskalib.explosion.ExplosionSystem.stopAllExplosions();
    }

    public static int getActiveExplosionCount() {
        return net.unfamily.iskalib.explosion.ExplosionSystem.getActiveExplosionCount();
    }
}
