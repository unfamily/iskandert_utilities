package net.unfamily.iskautils.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.FlameParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class EntropicFlameParticle extends FlameParticle {
    private static final float BASE_RED = 0.78F;
    private static final float BASE_GREEN = 0.18F;
    private static final float BASE_BLUE = 0.98F;

    public EntropicFlameParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
                                 SpriteSet sprites, RandomSource random) {
        super(level, x, y, z, xd, yd, zd, sprites.get(random));
        float tint = 0.85F + random.nextFloat() * 0.15F;
        setColor(BASE_RED * tint, BASE_GREEN * tint, BASE_BLUE * tint);
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed, RandomSource random) {
            return new EntropicFlameParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites, random);
        }
    }
}
