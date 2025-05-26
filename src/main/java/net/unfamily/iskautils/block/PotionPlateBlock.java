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
import net.unfamily.iskautils.data.PotionPlateConfig;
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
    private static final long COOLDOWN_TICKS = 60; // 3 seconds (20 ticks per second)
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
            
            if (lastApplication != null && (currentTime - lastApplication) < COOLDOWN_TICKS) {
                // Still in cooldown, don't apply effect
                return;
            }
            
            // Create and apply the effect
            MobEffectInstance effectInstance = config.createEffectInstance();
            if (effectInstance != null) {
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
                
                // Check if the entity already has this effect with same or higher amplifier
                MobEffectInstance existingEffect = livingEntity.getEffect(adjustedEffect.getEffect());
                if (existingEffect != null && existingEffect.getAmplifier() >= adjustedEffect.getAmplifier()) {
                    // Don't apply if entity already has same or stronger effect
                    // But still update cooldown to prevent spam checking
                    EFFECT_COOLDOWNS.put(entityId, currentTime);
                    return;
                }
                
                // Apply the effect
                boolean success = livingEntity.addEffect(adjustedEffect);
                
                if (success) {
                    // Update cooldown
                    EFFECT_COOLDOWNS.put(entityId, currentTime);
                    
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Applied effect {} (duration: {}s) to entity {} at position {}", 
                            config.getEffectId(), duration / 20.0f, entity.getName().getString(), pos);
                    }
                }
            }
        }
        
        // Don't call super.entityInside() as we don't want vector movement for potion plates
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