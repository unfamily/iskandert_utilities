package net.unfamily.iskautils.data;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Configuration class for potion effect plates loaded from datapack
 */
public class PotionPlateConfig {
    private final String effectId;
    private final int amplifier;
    private final int duration;
    private final boolean affectsPlayers;
    private final boolean affectsMobs;
    private final String plateId;
    private final boolean hideParticles;
    
    // Cached effect holder for performance
    private Holder<MobEffect> cachedEffect;
    
    public PotionPlateConfig(String plateId, String effectId, int amplifier, int duration, 
                           boolean affectsPlayers, boolean affectsMobs, boolean hideParticles) {
        this.plateId = plateId;
        this.effectId = effectId;
        this.amplifier = Math.max(0, amplifier); // Ensure non-negative
        this.duration = Math.max(60, duration); // Ensure at least 3 seconds (60 ticks)
        this.affectsPlayers = affectsPlayers;
        this.affectsMobs = affectsMobs;
        this.hideParticles = hideParticles;
    }
    
    /**
     * Gets the plate ID (e.g., "iska_utils-slowness")
     */
    public String getPlateId() {
        return plateId;
    }
    
    /**
     * Gets the full block name (e.g., "iska_utils_slowness")
     */
    public String getBlockName() {
        return plateId.replace("-", "_");
    }
    
    /**
     * Gets the registry-safe block name (converts - to _ for ResourceLocation compatibility)
     */
    public String getRegistryBlockName() {
        return plateId.replace("-", "_");
    }
    
    /**
     * Gets the texture name (e.g., "plate_trap_slowness.png")
     */
    public String getTextureName() {
        return plateId.replace("-", "_");
    }
    
    /**
     * Gets the texture path for models (e.g., "iska_utils:block/potion_plates/iska_utils/plate_trap_slowness")
     */
    public String getTexturePath() {
        // Remove .png extension if present for model reference
        String textureNameWithoutExt = getTextureName().endsWith(".png") ? 
            getTextureName().substring(0, getTextureName().length() - 4) : getTextureName();
        return "iska_utils:block/potion_plates/iska_utils/" + textureNameWithoutExt;
    }
    
    /**
     * Gets the texture name without extension (e.g., "plate_trap_slowness")
     */
    public String getTextureNameWithoutExtension() {
        return getTextureName().endsWith(".png") ? 
            getTextureName().substring(0, getTextureName().length() - 4) : getTextureName();
    }
    
    /**
     * Gets the effect ID (e.g., "minecraft:slowness")
     */
    public String getEffectId() {
        return effectId;
    }
    
    /**
     * Gets the effect amplifier (0-based, so 0 = level I)
     */
    public int getAmplifier() {
        return amplifier;
    }
    
    /**
     * Gets the effect duration in ticks
     */
    public int getDuration() {
        return duration;
    }
    
    /**
     * Whether this plate affects players
     */
    public boolean affectsPlayers() {
        return affectsPlayers;
    }
    
    /**
     * Whether this plate affects mobs/entities
     */
    public boolean affectsMobs() {
        return affectsMobs;
    }
    
    /**
     * Whether this plate hides particles for effects
     */
    public boolean hideParticles() {
        return hideParticles;
    }
    
    /**
     * Gets the MobEffect holder, caching it for performance
     */
    public Holder<MobEffect> getEffect() {
        if (cachedEffect == null) {
            ResourceLocation effectLocation = ResourceLocation.parse(effectId);
            cachedEffect = BuiltInRegistries.MOB_EFFECT.getHolder(effectLocation).orElse(null);
        }
        return cachedEffect;
    }
    
    /**
     * Checks if this plate should affect the given entity
     */
    public boolean shouldAffectEntity(LivingEntity entity) {
        if (entity instanceof Player) {
            return affectsPlayers;
        } else {
            return affectsMobs;
        }
    }
    
    /**
     * Creates a MobEffectInstance for this configuration
     */
    public MobEffectInstance createEffectInstance() {
        Holder<MobEffect> effect = getEffect();
        if (effect == null) {
            return null;
        }
        
        return new MobEffectInstance(effect, duration, amplifier, false, hideParticles, hideParticles);
    }
    
    /**
     * Validates this configuration
     */
    public boolean isValid() {
        return effectId != null && !effectId.isEmpty() && 
               plateId != null && !plateId.isEmpty() &&
               getEffect() != null &&
               (affectsPlayers || affectsMobs); // Must affect at least one type
    }
    
    @Override
    public String toString() {
        return String.format("PotionPlateConfig{plateId='%s', effect='%s', amplifier=%d, duration=%d, players=%b, mobs=%b, particles=%b}",
                plateId, effectId, amplifier, duration, affectsPlayers, affectsMobs, hideParticles);
    }
} 