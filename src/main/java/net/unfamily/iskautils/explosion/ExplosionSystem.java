package net.unfamily.iskautils.explosion;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.unfamily.iskautils.IskaUtils;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * System for managing lag-free progressive elliptical explosions
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public class ExplosionSystem {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Map of active explosions: ID -> ExplosionData
    private static final Map<UUID, ExplosionData> ACTIVE_EXPLOSIONS = new ConcurrentHashMap<>();
    
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        // Process all active explosions
        Iterator<Map.Entry<UUID, ExplosionData>> iterator = ACTIVE_EXPLOSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ExplosionData> entry = iterator.next();
            ExplosionData explosion = entry.getValue();
            
            explosion.tickCount++;
            
            // Check if explosion should be instant (tickInterval = 0) or if it's time to process
            if (explosion.tickInterval == 0) {
                // Instant explosion - process all layers at once
                while (explosion.currentRadius <= explosion.maxRadius) {
                    processExplosionLayer(explosion);
                }
                iterator.remove();
                LOGGER.debug("Instant explosion {} completed", entry.getKey());
            } else if (explosion.tickCount >= explosion.tickInterval) {
                explosion.tickCount = 0;
                
                if (processExplosionLayer(explosion)) {
                    // Explosion completed, remove from list
                    iterator.remove();
                    LOGGER.debug("Explosion {} completed", entry.getKey());
                }
            }
        }
    }
    
    /**
     * Creates a new explosion at the specified location (legacy method without damage/breaking)
     * @param level The server level
     * @param center The center position of the explosion
     * @param horizontalRadius The horizontal radius (X/Z) of the ellipse
     * @param verticalRadius The vertical radius (Y) of the ellipse
     * @param tickInterval How often to expand (in ticks)
     * @return The UUID of the created explosion
     */
    public static UUID createExplosion(ServerLevel level, BlockPos center, 
                                     int horizontalRadius, int verticalRadius, int tickInterval) {
        return createExplosion(level, center, horizontalRadius, verticalRadius, tickInterval, 0.0f, false);
    }
    
    /**
     * Creates a new explosion at the specified location
     * @param level The server level
     * @param center The center position of the explosion
     * @param horizontalRadius The horizontal radius (X/Z) of the ellipse
     * @param verticalRadius The vertical radius (Y) of the ellipse
     * @param tickInterval How often to expand (in ticks)
     * @param explosionDamage Damage to deal to entities (0 = no damage)
     * @param breakUnbreakable Whether to break unbreakable blocks like bedrock
     * @return The UUID of the created explosion
     */
    public static UUID createExplosion(ServerLevel level, BlockPos center, 
                                     int horizontalRadius, int verticalRadius, int tickInterval,
                                     float explosionDamage, boolean breakUnbreakable) {
        
        // Create explosion data
        ExplosionData explosion = new ExplosionData(
            UUID.randomUUID(),
            level,
            center,
            horizontalRadius,
            verticalRadius,
            tickInterval,
            explosionDamage,
            breakUnbreakable
        );
        
        // Add to active explosions list
        ACTIVE_EXPLOSIONS.put(explosion.id, explosion);
        
        LOGGER.info("Created explosion {} at center {} with radii {}x{}, interval {} ticks, damage {}, break unbreakable: {}", 
            explosion.id, center, horizontalRadius, verticalRadius, tickInterval, explosionDamage, breakUnbreakable);
        
        return explosion.id;
    }
    
    /**
     * Stops all active explosions
     * @return The number of explosions that were stopped
     */
    public static int stopAllExplosions() {
        int count = ACTIVE_EXPLOSIONS.size();
        ACTIVE_EXPLOSIONS.clear();
        LOGGER.info("Stopped {} active explosions", count);
        return count;
    }
    
    /**
     * Gets the number of active explosions
     */
    public static int getActiveExplosionCount() {
        return ACTIVE_EXPLOSIONS.size();
    }
    
    /**
     * Processes a layer of the explosion
     * @return true if the explosion is completed
     */
    private static boolean processExplosionLayer(ExplosionData explosion) {
        if (explosion.currentRadius > explosion.maxRadius) {
            return true; // Explosion completed
        }
        
        int blocksProcessed = 0;
        int entitiesKilled = 0;
        
        // Calculate blocks for current radius dynamically
        List<BlockPos> currentLayerBlocks = calculateLayerBlocks(explosion, explosion.currentRadius);
        
        // Process all blocks in the current layer
        for (BlockPos pos : currentLayerBlocks) {
            if (explosion.level.isInWorldBounds(pos)) {
                BlockState currentState = explosion.level.getBlockState(pos);
                
                // Check if we should destroy this block
                boolean shouldDestroy = false;
                
                if (explosion.breakUnbreakable) {
                    // Break everything except void air
                    shouldDestroy = !currentState.isAir();
                } else {
                    // Don't destroy bedrock, barrier or other indestructible blocks
                    shouldDestroy = !currentState.is(Blocks.BEDROCK) && 
                                   !currentState.is(Blocks.BARRIER) &&
                                   currentState.getDestroySpeed(explosion.level, pos) >= 0;
                }
                
                if (shouldDestroy) {
                    explosion.level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    blocksProcessed++;
                }
                
                // Deal damage to entities in this block position if explosion has damage
                if (explosion.explosionDamage > 0) {
                    AABB blockAABB = new AABB(pos);
                    List<Entity> entities = explosion.level.getEntitiesOfClass(Entity.class, blockAABB);
                    
                    for (Entity entity : entities) {
                        if (entity instanceof LivingEntity livingEntity) {
                            DamageSource explosionDamageSource = explosion.level.damageSources().explosion(null, null);
                            if (livingEntity.hurt(explosionDamageSource, explosion.explosionDamage)) {
                                entitiesKilled++;
                            }
                        }
                    }
                }
            }
        }
        
        explosion.currentRadius++;
        
        String logMessage = "Processed radius {}/{} of explosion {} - {} blocks destroyed";
        if (explosion.explosionDamage > 0) {
            logMessage += ", {} entities damaged";
            LOGGER.debug(logMessage, explosion.currentRadius, explosion.maxRadius, explosion.id, blocksProcessed, entitiesKilled);
        } else {
            LOGGER.debug(logMessage, explosion.currentRadius, explosion.maxRadius, explosion.id, blocksProcessed);
        }
        
        return explosion.currentRadius > explosion.maxRadius;
    }
    
    /**
     * Calculates blocks for a specific radius dynamically
     */
    private static List<BlockPos> calculateLayerBlocks(ExplosionData explosion, int radius) {
        List<BlockPos> layerBlocks = new ArrayList<>();
        
        // Calculate 3D ellipse with progressive expansion
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    // Skip if outside our ellipse bounds
                    if (Math.abs(x) > explosion.horizontalRadius || 
                        Math.abs(y) > explosion.verticalRadius || 
                        Math.abs(z) > explosion.horizontalRadius) {
                        continue;
                    }
                    
                    // Normalized 3D ellipse formula
                    double distanceX = (double) x / explosion.horizontalRadius;
                    double distanceY = (double) y / explosion.verticalRadius;
                    double distanceZ = (double) z / explosion.horizontalRadius;
                    
                    double ellipseDistance = distanceX * distanceX + 
                                           distanceY * distanceY + 
                                           distanceZ * distanceZ;
                    
                    // Block is inside ellipse if distance <= 1
                    if (ellipseDistance <= 1.0) {
                        // Calculate real euclidean distance from center
                        double euclideanDistance = Math.sqrt(x * x + y * y + z * z);
                        
                        // Only include blocks at exactly this radius (for progressive expansion)
                        if (Math.abs(euclideanDistance - radius) < 0.5) {
                            BlockPos pos = explosion.center.offset(x, y, z);
                            layerBlocks.add(pos);
                        }
                    }
                }
            }
        }
        
        return layerBlocks;
    }
    
    /**
     * Class containing data for an active explosion
     */
    private static class ExplosionData {
        public final UUID id;
        public final ServerLevel level;
        public final BlockPos center;
        public final int horizontalRadius;
        public final int verticalRadius;
        public final int tickInterval;
        public final float explosionDamage;
        public final boolean breakUnbreakable;
        
        public final int maxRadius;
        public int currentRadius = 0;
        public int tickCount = 0;
        
        public ExplosionData(UUID id, ServerLevel level, BlockPos center, 
                           int horizontalRadius, int verticalRadius, int tickInterval,
                           float explosionDamage, boolean breakUnbreakable) {
            this.id = id;
            this.level = level;
            this.center = center;
            this.horizontalRadius = horizontalRadius;
            this.verticalRadius = verticalRadius;
            this.tickInterval = tickInterval;
            this.explosionDamage = explosionDamage;
            this.breakUnbreakable = breakUnbreakable;
            
            // Calculate max radius based on the larger of horizontal and vertical radius
            this.maxRadius = Math.max(horizontalRadius, verticalRadius);
        }
    }
} 