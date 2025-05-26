package net.unfamily.iskautils.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.unfamily.iskautils.data.PotionPlateConfig;
import net.unfamily.iskautils.data.PotionPlateType;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A plate block that applies potion effects to entities stepping on it
 */
public class PotionPlateBlock extends VectorBlock {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<PotionPlateBlock> CODEC = simpleCodec(PotionPlateBlock::new);
    
    // Cooldown system: entity UUID -> last application time
    private static final Map<UUID, Long> EFFECT_COOLDOWNS = new HashMap<>();
    private static final int MIN_DURATION_TICKS = 100; // Minimum 3 seconds duration
    
    private final PotionPlateConfig config;
    
    public PotionPlateBlock(BlockBehaviour.Properties properties, PotionPlateConfig config) {
        super(properties);
        this.config = config;
    }
    
    // Constructor for codec compatibility
    public PotionPlateBlock(BlockBehaviour.Properties properties) {
        this(properties, null);
    }
    
    @Override
    protected MapCodec<? extends PotionPlateBlock> codec() {
        return CODEC;
    }
    
    /**
     * Gets the configuration for this potion plate
     */
    public PotionPlateConfig getConfig() {
        return config;
    }
    
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && config != null && config.isValid()) {
            // Only affect living entities
            if (!(entity instanceof LivingEntity livingEntity)) {
                return;
            }
            
            // Check if this plate should affect this type of entity
            if (!config.shouldAffectEntity(livingEntity)) {
                return;
            }
            
            // If player is sneaking (shift), don't apply effect
            if (entity instanceof Player player && player.isShiftKeyDown()) {
                return;
            }
            
            // Check cooldown - only apply effect every 3 seconds per entity
            UUID entityId = entity.getUUID();
            long currentTime = level.getGameTime();
            Long lastApplication = EFFECT_COOLDOWNS.get(entityId);
            
            // Special handling for freezing effect and cobweb effect - don't use regular cooldown
            // to better simulate continuous behavior
            boolean isFreezePlate = config.getPlateType() == PotionPlateType.SPECIAL && config.getFreezeDuration() > 0;
            boolean isCobwebPlate = config.getPlateType() == PotionPlateType.SPECIAL && config.hasCobwebEffect();
            boolean isSpecialContinuousEffect = isFreezePlate || isCobwebPlate;
            
            if (!isSpecialContinuousEffect && lastApplication != null && (currentTime - lastApplication) < config.getDelay()) {
                // Still in cooldown, don't apply effect
                return;
            }
            
            // Apply effect based on plate type
            boolean effectApplied = false;
            
            switch (config.getPlateType()) {
                case EFFECT:
                    effectApplied = applyPotionEffect(livingEntity, currentTime);
                    break;
                case DAMAGE:
                    effectApplied = applyDamage(livingEntity, level, currentTime);
                    break;
                case SPECIAL:
                    effectApplied = applySpecialEffect(livingEntity, currentTime);
                    break;
            }
            
            if (effectApplied) {
                // Update cooldown
                EFFECT_COOLDOWNS.put(entityId, currentTime);
            }
        }
        
        // Don't call super.entityInside() as we don't want vector movement for potion plates
    }
    
    /**
     * Applies potion effect for EFFECT plates
     */
    private boolean applyPotionEffect(LivingEntity livingEntity, long currentTime) {
        MobEffectInstance effectInstance = config.createEffectInstance();
        if (effectInstance == null) {
            return false;
        }
        
        // Ensure minimum duration of 3 seconds
        int duration = Math.max(effectInstance.getDuration(), MIN_DURATION_TICKS);
        MobEffectInstance adjustedEffect = new MobEffectInstance(
            effectInstance.getEffect(),
            duration,
            effectInstance.getAmplifier(),
            effectInstance.isAmbient(),
            effectInstance.isVisible(),
            effectInstance.showIcon()
        );
        
        // Check if the entity already has this effect
        MobEffectInstance existingEffect = livingEntity.getEffect(adjustedEffect.getEffect());
        if (existingEffect != null) {
            // If the existing effect has a higher amplifier, don't override it
            if (existingEffect.getAmplifier() > adjustedEffect.getAmplifier()) {
                return true; // Still count as applied for cooldown purposes
            }
            
            // If the existing effect has the same or lower amplifier, we can refresh it
            // This allows the effect to be reapplied every delay ticks as intended
            // The effect duration will be reset to the full duration
        }
        
        // Apply the effect (this will override any existing effect with same or lower amplifier)
        boolean success = livingEntity.addEffect(adjustedEffect);
        
        if (success && LOGGER.isDebugEnabled()) {
            LOGGER.debug("Applied effect {} (duration: {}s) to entity {} at position {}", 
                config.getEffectId(), duration / 20.0f, livingEntity.getName().getString(), livingEntity.blockPosition());
        }
        
        return success;
    }
    
    /**
     * Applies damage for DAMAGE plates
     */
    private boolean applyDamage(LivingEntity livingEntity, Level level, long currentTime) {
        try {
            // Create damage source
            var damageSource = level.damageSources().generic(); // Default damage source
            
            // Try to get specific damage source if available
            String damageTypeId = config.getDamageType();
            if (damageTypeId != null) {
                // For now, use generic damage - could be expanded to support specific damage types
                switch (damageTypeId) {
                    case "minecraft:cactus":
                        damageSource = level.damageSources().cactus();
                        break;
                    case "minecraft:magic":
                        damageSource = level.damageSources().magic();
                        break;
                    case "minecraft:player_attack":
                        damageSource = level.damageSources().generic();
                        break;
                    default:
                        damageSource = level.damageSources().generic();
                        break;
                }
            }
            
            // Apply damage
            boolean success = livingEntity.hurt(damageSource, config.getDamageAmount());
            
            if (success && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Applied {} damage (type: {}) to entity {} at position {}", 
                    config.getDamageAmount(), config.getDamageType(), 
                    livingEntity.getName().getString(), livingEntity.blockPosition());
            }
            
            return success;
        } catch (Exception e) {
            LOGGER.error("Failed to apply damage from plate {}: {}", config.getPlateId(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Applies special effects for SPECIAL plates (fire, freeze, etc.)
     */
    private boolean applySpecialEffect(LivingEntity livingEntity, long currentTime) {
        try {
            if (config.getFireDuration() > 0) {
                // Apply fire effect, always refresh the duration
                int fireTicks = config.getFireDuration();
                
                // Always set to the full duration for consistent behavior
                livingEntity.setRemainingFireTicks(fireTicks);
                
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Applied fire effect ({}s) to entity {} at position {}", 
                        fireTicks / 20.0f, livingEntity.getName().getString(), livingEntity.blockPosition());
                }
                return true;
            }
            
            if (config.getFreezeDuration() > 0) {
                // Increase freeze ticks gradually, similar to how powder snow works
                int currentFreezeTicks = livingEntity.getTicksFrozen();
                int maxFreezeTicks = config.getFreezeDuration();
                
                // In Minecraft, freeze damage starts at 140 ticks
                // Set directly to 200 to ensure immediate damage effect
                // This simulates continuous exposure to powder snow
                
                // Instead of incrementing, set directly to a high value (200)
                // This ensures immediate freezing effect like vanilla powder snow
                int newFreezeTicks = 200; // Ensuring we're well above the 140 threshold
                livingEntity.setTicksFrozen(newFreezeTicks);
                
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Set freeze effect to {} for entity {} at position {} (max: {})", 
                        newFreezeTicks / 20.0f, 
                        livingEntity.getName().getString(), livingEntity.blockPosition(), 
                        maxFreezeTicks / 20.0f);
                }
                
                // Always return true to track cooldown, even if we're at max freeze
                return true;
            }
            
            return false;
        } catch (Exception e) {
            LOGGER.error("Failed to apply special effect from plate {}: {}", config.getPlateId(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Clean up old cooldown entries to prevent memory leaks
     * Called periodically by the game
     */
    public static void cleanupCooldowns(Level level) {
        if (level.isClientSide) return;
        
        long currentTime = level.getGameTime();
        // Remove entries older than 10 minutes (12000 ticks)
        EFFECT_COOLDOWNS.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > 12000);
    }
    
    @Override
    public String toString() {
        return String.format("PotionPlateBlock{config=%s}", config);
    }
} 