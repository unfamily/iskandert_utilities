package net.unfamily.iskautils.util;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * Particle and sound FX for Ghost Brazier cast / ethereal / exit.
 */
public final class GhostBrazierFx {
    private GhostBrazierFx() {}

    public static void playCastStart(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(),
                SoundSource.PLAYERS, 0.7F, 0.8F);
        spawnRing(level, player, 12, 0.6D, 0.2D);
    }

    public static void playCastTick(ServerPlayer player, float progress) {
        ServerLevel level = player.serverLevel();
        if (player.tickCount % 12 == 0) {
            float pitch = 0.7F + progress * 0.6F;
            level.playSound(null, player.blockPosition(), SoundEvents.SCULK_CLICKING,
                    SoundSource.PLAYERS, 0.15F, pitch);
        }
        if (player.tickCount % 3 == 0) {
            spawnRisingSpiral(level, player, progress);
        }
    }

    public static void playCastInterrupt(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                SoundSource.PLAYERS, 0.6F, 1.2F);
        spawnBurst(level, player, 8, 0.3D);
    }

    public static void playEnter(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.8F, 0.9F);
        level.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(),
                SoundSource.PLAYERS, 0.9F, 0.7F);
        spawnBurst(level, player, 40, 0.9D);
    }

    public static void playLoop(ServerPlayer player) {
        if (player.tickCount % 4 != 0) {
            return;
        }
        spawnRisingSpiral(player.serverLevel(), player, 0.5F);
    }

    public static void playExit(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.8F, 0.6F);
        level.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(),
                SoundSource.PLAYERS, 0.7F, 1.1F);
        spawnBurst(level, player, 36, 0.7D);
    }

    private static void spawnRisingSpiral(ServerLevel level, Player player, float progress) {
        double radius = 0.85D - progress * 0.35D;
        double angle = (player.tickCount * 0.35D) % (Math.PI * 2.0D);
        double y = player.getY() + 0.2D + (player.tickCount % 20) * 0.05D;
        double x = player.getX() + Math.cos(angle) * radius;
        double z = player.getZ() + Math.sin(angle) * radius;
        sendMixed(level, x, y, z, 1);
    }

    private static void spawnRing(ServerLevel level, Player player, int count, double radius, double yOff) {
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0D * i) / count;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            sendMixed(level, x, player.getY() + yOff, z, 1);
        }
    }

    private static void spawnBurst(ServerLevel level, Player player, int count, double radius) {
        for (int i = 0; i < count; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double r = level.random.nextDouble() * radius;
            double x = player.getX() + Math.cos(angle) * r;
            double y = player.getY() + level.random.nextDouble() * player.getBbHeight();
            double z = player.getZ() + Math.sin(angle) * r;
            sendMixed(level, x, y, z, 1);
        }
    }

    private static void sendMixed(ServerLevel level, double x, double y, double z, int count) {
        var type = switch (level.random.nextInt(3)) {
            case 0 -> ParticleTypes.SOUL_FIRE_FLAME;
            case 1 -> ParticleTypes.SOUL;
            default -> ParticleTypes.SCULK_SOUL;
        };
        level.sendParticles(type, x, y, z, count, 0.02D, 0.05D, 0.02D, 0.01D);
    }
}