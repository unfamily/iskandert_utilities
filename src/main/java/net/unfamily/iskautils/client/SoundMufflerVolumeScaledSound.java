package net.unfamily.iskautils.client;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;

/**
 * Client-only wrapper that scales the volume of a sound. Used by Sound Muffler to reduce (not mute) sounds by category.
 */
public record SoundMufflerVolumeScaledSound(SoundInstance inner, float volumeScale) implements SoundInstance {

    @Override
    public float getVolume() {
        return inner.getVolume() * volumeScale;
    }

    @Override
    public float getPitch() {
        return inner.getPitch();
    }

    @Override
    public double getX() {
        return inner.getX();
    }

    @Override
    public double getY() {
        return inner.getY();
    }

    @Override
    public double getZ() {
        return inner.getZ();
    }

    @Override
    public SoundInstance.Attenuation getAttenuation() {
        return inner.getAttenuation();
    }

    @Override
    public boolean isRelative() {
        return inner.isRelative();
    }

    @Override
    public SoundSource getSource() {
        return inner.getSource();
    }

    @Override
    public Sound getSound() {
        return inner.getSound();
    }

    @Override
    public Identifier getIdentifier() {
        return inner.getIdentifier();
    }

    @Override
    public net.minecraft.client.sounds.WeighedSoundEvents resolve(SoundManager manager) {
        return inner.resolve(manager);
    }

    @Override
    public boolean isLooping() {
        return inner.isLooping();
    }

    @Override
    public int getDelay() {
        return inner.getDelay();
    }
}
