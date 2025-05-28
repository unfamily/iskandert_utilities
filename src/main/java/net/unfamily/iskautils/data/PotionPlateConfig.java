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
    
    // Visibility and behavior configuration
    private boolean creativeTabVisible;
    private boolean playerShiftDisable;
    
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
        
        // Default values for visibility and behavior
        this.creativeTabVisible = true;
        this.playerShiftDisable = true;
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
        
        // Default values for visibility and behavior
        this.creativeTabVisible = true;
        this.playerShiftDisable = true;
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
        
        // Default values for visibility and behavior
        this.creativeTabVisible = true;
        this.playerShiftDisable = true;
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
        
        // Default values for visibility and behavior
        this.creativeTabVisible = true;
        this.playerShiftDisable = true;
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
     * Whether this plate affects mobs
     */
    public boolean affectsMobs() {
        return affectsMobs;
    }
    
    /**
     * Whether to hide particles for this plate's effect
     */
    public boolean hideParticles() {
        return hideParticles;
    }
    
    /**
     * Whether this configuration can be overwritten by other configurations
     */
    public boolean isOverwritable() {
        return overwritable;
    }
    
    /**
     * Gets the damage type ID for damage plates
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
     * Gets the fire duration in ticks for fire plates
     */
    public int getFireDuration() {
        return fireDuration;
    }
    
    /**
     * Gets the freeze duration in ticks for freeze plates
     */
    public int getFreezeDuration() {
        return freezeDuration;
    }
    
    /**
     * Whether this plate has cobweb effect (for special plates)
     */
    public boolean hasCobwebEffect() {
        return cobwebEffect;
    }
    
    /**
     * Whether this plate should be visible in the creative tab
     */
    public boolean isCreativeTabVisible() {
        return creativeTabVisible;
    }
    
    /**
     * Sets whether this plate should be visible in the creative tab
     */
    public void setCreativeTabVisible(boolean visible) {
        this.creativeTabVisible = visible;
    }
    
    /**
     * Whether shift (sneaking) should disable this plate's effect for players
     */
    public boolean isPlayerShiftDisable() {
        return playerShiftDisable;
    }
    
    /**
     * Sets whether shift (sneaking) should disable this plate's effect for players
     */
    public void setPlayerShiftDisable(boolean disable) {
        this.playerShiftDisable = disable;
    }
    
    /**
     * Gets the effect for this configuration
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
     * Checks if a player shifting should prevent the effect
     * 
     * @param player The player entity
     * @return true if the effect should be prevented due to shifting, false otherwise
     */
    public boolean shouldShiftPreventEffect(Player player) {
        return playerShiftDisable && player.isShiftKeyDown();
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
        switch (plateType) {
            case EFFECT:
                return effectId != null && !effectId.isEmpty() && duration > 0;
                
            case DAMAGE:
                return damageType != null && !damageType.isEmpty() && damageAmount > 0.0f;
                
            case SPECIAL:
                return fireDuration > 0 || freezeDuration > 0 || cobwebEffect;
                
            default:
                return false;
        }
    }
    
    /**
     * Merges this configuration with another one, preferring the other's values where available
     */
    public PotionPlateConfig mergeWith(PotionPlateConfig other) {
        if (other == null) {
            return this;
        }
        
        if (!plateId.equals(other.plateId)) {
            throw new IllegalArgumentException("Cannot merge configurations with different plate IDs");
        }
        
        if (plateType != other.plateType) {
            throw new IllegalArgumentException("Cannot merge configurations with different plate types");
        }
        
        switch (plateType) {
            case EFFECT:
                PotionPlateConfig mergedEffectConfig = new PotionPlateConfig(
                    plateId,
                    other.effectId != null ? other.effectId : effectId,
                    other.amplifier,
                    other.duration,
                    other.delay,
                    other.affectsPlayers,
                    other.affectsMobs,
                    other.hideParticles,
                    other.overwritable
                );
                
                // Copy visibility and behavior settings
                mergedEffectConfig.setCreativeTabVisible(other.isCreativeTabVisible());
                mergedEffectConfig.setPlayerShiftDisable(other.isPlayerShiftDisable());
                
                return mergedEffectConfig;
                
            case DAMAGE:
                PotionPlateConfig mergedDamageConfig = new PotionPlateConfig(
                    plateId,
                    other.damageType != null ? other.damageType : damageType,
                    other.damageAmount,
                    other.delay,
                    other.affectsPlayers,
                    other.affectsMobs,
                    other.overwritable
                );
                
                // Copy visibility and behavior settings
                mergedDamageConfig.setCreativeTabVisible(other.isCreativeTabVisible());
                mergedDamageConfig.setPlayerShiftDisable(other.isPlayerShiftDisable());
                
                return mergedDamageConfig;
                
            case SPECIAL:
                PotionPlateConfig mergedSpecialConfig = new PotionPlateConfig(
                    plateId,
                    Math.max(other.fireDuration, fireDuration),
                    other.delay,
                    other.affectsPlayers,
                    other.affectsMobs,
                    other.overwritable
                );
                
                // Special handling for cobweb effect
                if (other.hasCobwebEffect() || hasCobwebEffect()) {
                    mergedSpecialConfig.cobwebEffect = true;
                }
                
                // Copy visibility and behavior settings
                mergedSpecialConfig.setCreativeTabVisible(other.isCreativeTabVisible());
                mergedSpecialConfig.setPlayerShiftDisable(other.isPlayerShiftDisable());
                
                return mergedSpecialConfig;
                
            default:
                return this;
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PotionPlateConfig{");
        sb.append("id='").append(plateId).append('\'');
        sb.append(", type=").append(plateType);
        
        switch (plateType) {
            case EFFECT:
                sb.append(", effect='").append(effectId).append('\'');
                sb.append(", amplifier=").append(amplifier);
                sb.append(", duration=").append(duration).append(" ticks");
                break;
            case DAMAGE:
                sb.append(", damageType='").append(damageType).append('\'');
                sb.append(", amount=").append(damageAmount);
                break;
            case SPECIAL:
                if (fireDuration > 0) {
                    sb.append(", fire=").append(fireDuration).append(" ticks");
                }
                if (freezeDuration > 0) {
                    sb.append(", freeze=").append(freezeDuration).append(" ticks");
                }
                if (cobwebEffect) {
                    sb.append(", cobweb=true");
                }
                break;
        }
        
        sb.append(", delay=").append(delay).append(" ticks");
        sb.append(", affectsPlayers=").append(affectsPlayers);
        sb.append(", affectsMobs=").append(affectsMobs);
        sb.append(", creativeTabVisible=").append(creativeTabVisible);
        sb.append(", playerShiftDisable=").append(playerShiftDisable);
        sb.append('}');
        return sb.toString();
    }
} 
