package net.unfamily.iskautils.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.unfamily.iskalib.explosion.ExplosionInfo;

import java.util.List;

/**
 * Stable façade re-export for integrations that reflect or reference {@code net.unfamily.iskautils.explosion.ExplosionSystem}.
 * Implementation: {@link net.unfamily.iskalib.explosion.ExplosionSystem}.
 */
public final class ExplosionSystem {

    private ExplosionSystem() {}

    public static int createExplosion(
            ServerLevel level,
            BlockPos center,
            int horizontalRadius,
            int verticalRadius,
            int tickInterval) {
        return net.unfamily.iskalib.explosion.ExplosionSystem.createExplosion(
                level, center, horizontalRadius, verticalRadius, tickInterval);
    }

    public static int createExplosion(
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

    public static boolean stopExplosion(int number) {
        return net.unfamily.iskalib.explosion.ExplosionSystem.stopExplosion(number);
    }

    public static int getActiveExplosionCount() {
        return net.unfamily.iskalib.explosion.ExplosionSystem.getActiveExplosionCount();
    }

    public static List<Integer> getActiveExplosionNumbers() {
        return net.unfamily.iskalib.explosion.ExplosionSystem.getActiveExplosionNumbers();
    }

    public static List<ExplosionInfo> getActiveExplosionInfo() {
        return net.unfamily.iskalib.explosion.ExplosionSystem.getActiveExplosionInfo();
    }
}
