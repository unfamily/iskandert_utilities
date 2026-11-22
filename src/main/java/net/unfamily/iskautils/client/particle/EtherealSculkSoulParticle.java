package net.unfamily.iskautils.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SoulParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * Wraps vanilla sculk-soul visuals; clamps status-effect fling so particles stay on the body (anti-face).
 */
public final class EtherealSculkSoulParticle {
    private EtherealSculkSoulParticle() {}

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SoulParticle.EmissiveProvider delegate;

        public Provider(SpriteSet sprites) {
            this.delegate = new SoulParticle.EmissiveProvider(sprites);
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed, RandomSource random) {
            double xd = xSpeed;
            double yd = ySpeed;
            double zd = zSpeed;
            if (Math.abs(xSpeed) > 0.1D || Math.abs(ySpeed) > 0.1D || Math.abs(zSpeed) > 0.1D) {
                xd = 0.0D;
                yd = 0.0D;
                zd = 0.0D;
            }
            return delegate.createParticle(type, level, x, y, z, xd, yd, zd, random);
        }
    }
}
