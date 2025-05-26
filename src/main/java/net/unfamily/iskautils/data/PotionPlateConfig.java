package net.unfamily.iskautils.data;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Configuration class for potion effect plates loaded from external configurations
 * Now supports overwritable parameters for configuration merging and multiple plate types
 */
public class PotionPlateConfig {
    private final String plateId;
    private final PotionPlateType plateType;
    private final boolean overwritable;
    private final boolean affectsPlayers;
    private final boolean affectsMobs;
    
    // Effect-specific fields
    private final String effectId;
    private final int amplifier;
    private final int duration;
    private final int delay; // Delay between applications in ticks (default 40 = 2 seconds)
    private final boolean hideParticles;
    
    // Damage-specific fields
    private final String damageType;
    private final float damageAmount;
    
    // Fire-specific fields
    private final int fireDuration;
    
    // Freeze-specific fields
    private final int freezeDuration;
    
    // Cobweb-specific fields
    private boolean cobwebEffect;
    
    // Cached effect holder for performance
    private Holder<MobEffect> cachedEffect;
    
    /**
     * Creates a new PotionPlateConfig for an effect-type plate
     */
    public PotionPlateConfig(String plateId, String effectId, int amplifier, int duration, int delay,
                           boolean affectsPlayers, boolean affectsMobs, boolean hideParticles, boolean overwritable) {
        this.plateId = plateId;
        this.plateType = PotionPlateType.EFFECT;
        this.overwritable = overwritable;
        this.affectsPlayers = affectsPlayers;
        this.affectsMobs = affectsMobs;
        
        // Effect fields
        this.effectId = effectId;
        this.amplifier = Math.max(0, amplifier);
        this.duration = Math.max(60, duration);
        this.delay = Math.max(40, delay); // Minimum 2 seconds delay for EFFECT plates
        this.hideParticles = hideParticles;
        
        // Default values for other types
        this.damageType = "";
        this.damageAmount = 0.0f;
        this.fireDuration = 0;
        this.freezeDuration = 0;
        this.cobwebEffect = false;
    }
    
    /**
     * Creates a new PotionPlateConfig for a damage-type plate
     */
    public PotionPlateConfig(String plateId, String damageType, float damageAmount, int delay,
                           boolean affectsPlayers, boolean affectsMobs, boolean overwritable) {
        this.plateId = plateId;
        this.plateType = PotionPlateType.DAMAGE;
        this.overwritable = overwritable;
        this.affectsPlayers = affectsPlayers;
        this.affectsMobs = affectsMobs;
        
        // Damage fields
        this.damageType = damageType;
        this.damageAmount = Math.max(0.0f, damageAmount);
        this.delay = Math.max(1, delay); // Minimum 1 tick delay for DAMAGE plates (can be very fast)
        
        // Default values for other types
        this.effectId = "";
        this.amplifier = 0;
        this.duration = 0;
        this.hideParticles = false;
        this.fireDuration = 0;
        this.freezeDuration = 0;
        this.cobwebEffect = false;
    }
    
    /**
     * Creates a new PotionPlateConfig for a fire-type plate (special)
     */
    public PotionPlateConfig(String plateId, int fireDuration, int delay,
                           boolean affectsPlayers, boolean affectsMobs, boolean overwritable) {
        this.plateId = plateId;
        this.plateType = PotionPlateType.SPECIAL;
        this.overwritable = overwritable;
        this.affectsPlayers = affectsPlayers;
        this.affectsMobs = affectsMobs;
        
        // Fire fields
        this.fireDuration = Math.max(0, fireDuration);
        this.delay = Math.max(40, delay); // Minimum 2 seconds delay for SPECIAL plates
        
        // Default values for other types
        this.effectId = "";
        this.amplifier = 0;
        this.duration = 0;
        this.hideParticles = false;
        this.damageType = "";
        this.damageAmount = 0.0f;
        this.freezeDuration = 0;
        this.cobwebEffect = false;
    }
    
    /**
     * Creates a new PotionPlateConfig for a freeze-type plate (special)
     */
    public static PotionPlateConfig createFreezePlate(String plateId, int freezeDuration, int delay,
                                                     boolean affectsPlayers, boolean affectsMobs, boolean overwritable) {
        return new PotionPlateConfig(plateId, PotionPlateType.SPECIAL, freezeDuration, delay, affectsPlayers, affectsMobs, overwritable);
    }
    
    /**
     * Creates a new PotionPlateConfig for a cobweb-type plate (special)
     */
    public static PotionPlateConfig createCobwebPlate(String plateId, int delay,
                                                    boolean affectsPlayers, boolean affectsMobs, boolean overwritable) {
        PotionPlateConfig config = new PotionPlateConfig(plateId, PotionPlateType.SPECIAL, 0, delay, affectsPlayers, affectsMobs, overwritable);
        config.cobwebEffect = true;
        return config;
    }
    
    private PotionPlateConfig(String plateId, PotionPlateType plateType, int freezeDuration, int delay,
                            boolean affectsPlayers, boolean affectsMobs, boolean overwritable) {
        this.plateId = plateId;
        this.plateType = plateType;
        this.overwritable = overwritable;
        this.affectsPlayers = affectsPlayers;
        this.affectsMobs = affectsMobs;
        
        // Freeze fields
        this.freezeDuration = Math.max(0, freezeDuration);
        this.delay = Math.max(40, delay); // Minimum 2 seconds delay for SPECIAL plates
        
        // Default values for other types
        this.effectId = null;
        this.amplifier = 0;
        this.duration = 0;
        this.hideParticles = false;
        this.damageType = null;
        this.damageAmount = 0.0f;
        this.fireDuration = 0;
    }
    
    // Backward compatibility constructors
    public PotionPlateConfig(String plateId, String effectId, int amplifier, int duration, 
                           boolean affectsPlayers, boolean affectsMobs, boolean hideParticles, boolean overwritable) {
        this(plateId, effectId, amplifier, duration, 40, affectsPlayers, affectsMobs, hideParticles, overwritable);
    }
    
    public PotionPlateConfig(String plateId, String damageType, float damageAmount,
                           boolean affectsPlayers, boolean affectsMobs, boolean overwritable) {
        this(plateId, damageType, damageAmount, 40, affectsPlayers, affectsMobs, overwritable);
    }
    
    public PotionPlateConfig(String plateId, int fireDuration,
                           boolean affectsPlayers, boolean affectsMobs, boolean overwritable) {
        this(plateId, fireDuration, 40, affectsPlayers, affectsMobs, overwritable);
    }
    
    /**
     * Gets the plate ID (e.g., "iska_utils-slowness")
     */
    public String getPlateId() {
        return plateId;
    }
    
    /**
     * Gets the plate type
     */
    public PotionPlateType getPlateType() {
        return plateType;
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
     * Gets the effect delay in ticks
     */
    public int getDelay() {
        return delay;
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
     * Whether this configuration can be overwritten by subsequent configurations
     */
    public boolean isOverwritable() {
        return overwritable;
    }
    
    /**
     * Gets the damage type for damage plates
     */
    public String getDamageType() {
        return damageType;
    }
    
    /**
     * Gets the damage amount for damage plates
     */
    public float getDamageAmount() {
        return damageAmount;
    }
    
    /**
     * Gets the fire duration for fire plates
     */
    public int getFireDuration() {
        return fireDuration;
    }
    
    /**
     * Gets the freeze duration for freeze plates
     */
    public int getFreezeDuration() {
        return freezeDuration;
    }
    
    /**
     * Checks if this plate has the cobweb effect
     */
    public boolean hasCobwebEffect() {
        return cobwebEffect;
    }
    
    /**
     * Gets the MobEffect holder, caching it for performance
     * Only valid for EFFECT plates
     */
    public Holder<MobEffect> getEffect() {
        if (plateType != PotionPlateType.EFFECT) {
            return null; // Non-effect plates don't have effects
        }
        
        if (effectId == null || effectId.isEmpty()) {
            return null; // No effect ID
        }
        
        if (cachedEffect == null) {
            try {
                ResourceLocation effectLocation = ResourceLocation.parse(effectId);
                cachedEffect = BuiltInRegistries.MOB_EFFECT.getHolder(effectLocation).orElse(null);
            } catch (Exception e) {
                // Invalid effect ID format
                return null;
            }
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
     * Only valid for EFFECT plates
     */
    public MobEffectInstance createEffectInstance() {
        if (plateType != PotionPlateType.EFFECT) {
            return null; // Only effect plates can create effect instances
        }
        
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
        // Basic validation
        if (plateId == null || plateId.isEmpty() || !affectsPlayers && !affectsMobs) {
            return false;
        }
        
        // Type-specific validation
        switch (plateType) {
            case EFFECT:
                return effectId != null && !effectId.isEmpty() && getEffect() != null;
            case DAMAGE:
                return damageType != null && !damageType.isEmpty() && damageAmount > 0;
            case SPECIAL:
                return (fireDuration > 0) || (freezeDuration > 0) || cobwebEffect;
            default:
                return false;
        }
    }
    
    /**
     * Creates a merged configuration from this one and another, respecting overwritable flags
     * If this configuration has overwritable=true for a field, it can be overwritten by the other config
     * If this configuration has overwritable=false, it keeps its values
     */
    public PotionPlateConfig mergeWith(PotionPlateConfig other) {
        if (!this.plateId.equals(other.plateId)) {
            throw new IllegalArgumentException("Cannot merge configurations with different plate IDs: " + 
                this.plateId + " vs " + other.plateId);
        }
        
        // Use other's values only if this config is overwritable for that field
        String mergedEffectId = this.overwritable ? other.effectId : this.effectId;
        int mergedAmplifier = this.overwritable ? other.amplifier : this.amplifier;
        int mergedDuration = this.overwritable ? other.duration : this.duration;
        int mergedDelay = this.overwritable ? other.delay : this.delay;
        boolean mergedAffectsPlayers = this.overwritable ? other.affectsPlayers : this.affectsPlayers;
        boolean mergedAffectsMobs = this.overwritable ? other.affectsMobs : this.affectsMobs;
        boolean mergedHideParticles = this.overwritable ? other.hideParticles : this.hideParticles;
        
        // Damage fields
        String mergedDamageType = this.overwritable ? other.damageType : this.damageType;
        float mergedDamageAmount = this.overwritable ? other.damageAmount : this.damageAmount;
        
        // Special fields
        int mergedFireDuration = this.overwritable ? other.fireDuration : this.fireDuration;
        int mergedFreezeDuration = this.overwritable ? other.freezeDuration : this.freezeDuration;
        boolean mergedCobwebEffect = this.overwritable ? other.cobwebEffect : this.cobwebEffect;
        
        // The merged config inherits the overwritable flag from the other config
        boolean mergedOverwritable = other.overwritable;
        
        // Create appropriate constructor based on plate type
        PotionPlateType mergedType = this.overwritable ? other.plateType : this.plateType;
        
        switch (mergedType) {
            case EFFECT:
                return new PotionPlateConfig(plateId, mergedEffectId, mergedAmplifier, mergedDuration, mergedDelay,
                        mergedAffectsPlayers, mergedAffectsMobs, mergedHideParticles, mergedOverwritable);
            case DAMAGE:
                return new PotionPlateConfig(plateId, mergedDamageType, mergedDamageAmount, mergedDelay,
                        mergedAffectsPlayers, mergedAffectsMobs, mergedOverwritable);
            case SPECIAL:
                if (mergedFireDuration > 0) {
                    return new PotionPlateConfig(plateId, mergedFireDuration, mergedDelay,
                            mergedAffectsPlayers, mergedAffectsMobs, mergedOverwritable);
                } else if (mergedFreezeDuration > 0) {
                    return createFreezePlate(plateId, mergedFreezeDuration, mergedDelay,
                            mergedAffectsPlayers, mergedAffectsMobs, mergedOverwritable);
                } else if (mergedCobwebEffect) {
                    return createCobwebPlate(plateId, mergedDelay,
                            mergedAffectsPlayers, mergedAffectsMobs, mergedOverwritable);
                } else {
                    // Fallback to empty special plate
                    PotionPlateConfig config = new PotionPlateConfig(plateId, PotionPlateType.SPECIAL, 0, mergedDelay,
                            mergedAffectsPlayers, mergedAffectsMobs, mergedOverwritable);
                    return config;
                }
            default:
                // Fallback to effect type
                return new PotionPlateConfig(plateId, mergedEffectId, mergedAmplifier, mergedDuration, mergedDelay,
                        mergedAffectsPlayers, mergedAffectsMobs, mergedHideParticles, mergedOverwritable);
        }
    }

    @Override
    public String toString() {
        switch (plateType) {
            case EFFECT:
                return String.format("PotionPlateConfig{plateId='%s', type=EFFECT, effect='%s', amplifier=%d, duration=%d, players=%b, mobs=%b, particles=%b, overwritable=%b}",
                        plateId, effectId, amplifier, duration, affectsPlayers, affectsMobs, hideParticles, overwritable);
            case DAMAGE:
                return String.format("PotionPlateConfig{plateId='%s', type=DAMAGE, damageType='%s', damage=%.1f, players=%b, mobs=%b, overwritable=%b}",
                        plateId, damageType, damageAmount, affectsPlayers, affectsMobs, overwritable);
            case SPECIAL:
                if (fireDuration > 0) {
                    return String.format("PotionPlateConfig{plateId='%s', type=SPECIAL, fire=%d, players=%b, mobs=%b, overwritable=%b}",
                            plateId, fireDuration, affectsPlayers, affectsMobs, overwritable);
                } else if (freezeDuration > 0) {
                    return String.format("PotionPlateConfig{plateId='%s', type=SPECIAL, freeze=%d, players=%b, mobs=%b, overwritable=%b}",
                            plateId, freezeDuration, affectsPlayers, affectsMobs, overwritable);
                } else if (cobwebEffect) {
                    return String.format("PotionPlateConfig{plateId='%s', type=SPECIAL, cobweb=true, players=%b, mobs=%b, overwritable=%b}",
                            plateId, affectsPlayers, affectsMobs, overwritable);
                } else {
                    return String.format("PotionPlateConfig{plateId='%s', type=SPECIAL, unknown, players=%b, mobs=%b, overwritable=%b}",
                            plateId, affectsPlayers, affectsMobs, overwritable);
                }
            default:
                return String.format("PotionPlateConfig{plateId='%s', type=UNKNOWN, overwritable=%b}",
                        plateId, overwritable);
        }
    }
} 
