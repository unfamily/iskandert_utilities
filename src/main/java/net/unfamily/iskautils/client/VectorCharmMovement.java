package net.unfamily.iskautils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.data.VectorCharmData;
import net.unfamily.iskautils.data.VectorFactorType;
import net.unfamily.iskautils.item.custom.VectorCharmItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages player movement when they have Vector Charm equipped
 */
public class VectorCharmMovement {
    private static final Logger LOGGER = LoggerFactory.getLogger(VectorCharmMovement.class);
    
    // Flag to track if the player was in hover mode in the previous tick
    private static boolean wasInHoverMode = false;
    
    // Fixed value for hover mode
    private static final double HOVER_VALUE = 0.0D;

    /**
     * Applies movement based on Vector Charm factors
     * @param player The player to apply movement to
     */
    public static void applyMovement(Player player) {
        if (player == null) return;

        // Check if charms are enabled
        if (!Config.verticalCharmEnabled && !Config.horizontalCharmEnabled) return;
        
        // Get charm factors
        byte verticalFactorValue = VectorCharmData.getInstance().getVerticalFactor(player);
        byte horizontalFactorValue = VectorCharmData.getInstance().getHorizontalFactor(player);

        // Check if hover mode is active (special value 6)
        boolean isHoverMode = verticalFactorValue == VectorCharmData.HOVER_MODE_VALUE;
        
        // Update hover state for next tick
        boolean wasHoverModePreviously = wasInHoverMode;
        wasInHoverMode = isHoverMode;

        // Check if there are active factors
        // Exclude special value 6 (hover) from normal vertical factor check
        boolean hasVerticalFactor = Config.verticalCharmEnabled && verticalFactorValue > 0 && 
                                   verticalFactorValue != VectorCharmData.HOVER_MODE_VALUE;
        boolean hasHorizontalFactor = Config.horizontalCharmEnabled && horizontalFactorValue > 0;

        // If there are no active factors, exit
        if (!hasVerticalFactor && !hasHorizontalFactor && !isHoverMode) return;

        // Handle vertical movement for normal factors or hover
        if (hasVerticalFactor || isHoverMode) {
            applyVerticalMovement(player, verticalFactorValue, isHoverMode);
            
            // Prevent fall damage for vertical movement and hover
            player.fallDistance = 0;
        }

        // Handle horizontal movement
        if (hasHorizontalFactor) {
            applyHorizontalMovement(player, horizontalFactorValue);
        }
    }

    /**
     * Applies vertical movement
     * @param player The player to apply movement to
     * @param factorValue The vertical factor value
     * @param isHoverMode True if hover mode is active
     */
    private static void applyVerticalMovement(Player player, byte factorValue, boolean isHoverMode) {
        // Get current motion
        Vec3 currentMotion = player.getDeltaMovement();

        if (isHoverMode) {
            // Hover mode: set directly to fixed value
            player.setDeltaMovement(
                currentMotion.x,
                HOVER_VALUE,
                currentMotion.z
            );
            
            // Make sure fall distance is reset
            player.fallDistance = 0;
            
        } else if (factorValue > 0) {
            // Normal mode: get value directly from config
            double speed = getVectorSpeed(factorValue);
            
            // Add player factor from config instead of multiplying
            // This emulates the behavior of vector plates for players
            double verticalBoost = speed + Config.verticalBoostFactor;
            
            // Simulate VectorBlock.applyVerticalMovement behavior
            // Use the same constants and logic for consistency
            double accelerationFactor = 0.6; // Same value as VectorBlock
            
            // Calculate new vertical speed - don't use Math.max to allow descent
            double targetY = verticalBoost;
            double newY = (currentMotion.y * (1 - accelerationFactor)) + (targetY * accelerationFactor);
            
            player.setDeltaMovement(
                currentMotion.x,
                newY,
                currentMotion.z
            );
            
            // Prevent fall damage for vertical movement
            player.fallDistance = 0;
        }
        
        // Confirm physics updates
        player.hurtMarked = true;
    }

    /**
     * Applies horizontal movement
     * @param player The player to apply movement to
     * @param factorValue The horizontal factor value
     */
    private static void applyHorizontalMovement(Player player, byte factorValue) {
        // Get current motion
        Vec3 currentMotion = player.getDeltaMovement();
        
        // Get the direction the player is facing
        Vec3 lookVec = player.getLookAngle();
        
        // Get value directly from config
        double speed = getVectorSpeed(factorValue);
        
        // Calculate new velocity components
        double targetX = lookVec.x * speed;
        double targetZ = lookVec.z * speed;
        
        // Apply gradual acceleration - value taken from VectorBlock
        double accelerationFactor = 0.6;
        double conserveFactor = 0.75; // Keep 75% of lateral velocity - value taken from VectorBlock
        
        // Apply gradual acceleration
        double newX = (currentMotion.x * (1 - accelerationFactor)) + (targetX * accelerationFactor);
        double newZ = (currentMotion.z * (1 - accelerationFactor)) + (targetZ * accelerationFactor);
        
        // Set new movement
        player.setDeltaMovement(
            newX,
            currentMotion.y,
            newZ
        );
        
        // Confirm physics updates
        player.hurtMarked = true;
    }
    
    /**
     * Gets the Vector speed corresponding to the specified factor
     * @param factorValue The factor value (0-5)
     * @return The corresponding speed
     */
    private static double getVectorSpeed(byte factorValue) {
        VectorFactorType factorType = VectorFactorType.fromByte(factorValue);
        return switch (factorType) {
            case SLOW -> Config.slowVectorSpeed;
            case MODERATE -> Config.moderateVectorSpeed;
            case FAST -> Config.fastVectorSpeed;
            case EXTREME -> Config.extremeVectorSpeed;
            case ULTRA -> Config.ultraVectorSpeed;
            default -> 0.0;
        };
    }
} 