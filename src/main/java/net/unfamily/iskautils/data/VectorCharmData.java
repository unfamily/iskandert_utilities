package net.unfamily.iskautils.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.unfamily.iskautils.IskaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Class to manage persistent Vector Charm data.
 * Currently only uses memory, save support will be
 * implemented later.
 */
public class VectorCharmData {
    private static final Logger LOGGER = LoggerFactory.getLogger(VectorCharmData.class);
    private static final String DATA_NAME = IskaUtils.MOD_ID + "_vector_charm_data";
    
    // Singleton instance
    private static VectorCharmData instance;
    
    // Maps to store factors for each player
    private final Map<UUID, Byte> verticalFactors = new HashMap<>();
    private final Map<UUID, Byte> horizontalFactors = new HashMap<>();
    
    // Map to store previous vertical factor value before hover
    private final Map<UUID, Byte> previousVerticalFactors = new HashMap<>();
    
    // Special value for hover mode
    public static final byte HOVER_MODE_VALUE = 6;
    
    // Special value for parachute mode (initial activation)
    public static final byte PARACHUTE_MODE_VALUE = 7;
    
    // Special value for parachute active mode (after slow falling is applied)
    public static final byte PARACHUTE_ACTIVE_VALUE = 8;
    
    // Duration of parachute mode in ticks (5 seconds)
    public static final int PARACHUTE_DURATION = 100;
    
    // Map to track remaining parachute time
    private final Map<UUID, Integer> parachuteTicksLeft = new HashMap<>();
    
    // Flag to track changes
    private boolean isDirty = false;

    /**
     * Private constructor for singleton
     */
    private VectorCharmData() {
        // Private constructor
    }
    
    /**
     * Gets the singleton instance of VectorCharmData
     */
    public static VectorCharmData getInstance() {
        if (instance == null) {
            instance = new VectorCharmData();
        }
        return instance;
    }

    /**
     * Sets the vertical factor for a player
     * @param player the player
     * @param factor the factor (0-5, or 6 for hover)
     */
    public void setVerticalFactor(Player player, byte factor) {
        UUID playerId = player.getUUID();
        
        // If we're activating hover mode, save the previous value
        byte currentFactor = verticalFactors.getOrDefault(playerId, (byte) 0);
        if (factor == HOVER_MODE_VALUE && currentFactor != HOVER_MODE_VALUE) {
            previousVerticalFactors.put(playerId, currentFactor);
        }
        
        verticalFactors.put(playerId, factor);
        isDirty = true;
    }

    /**
     * Gets the vertical factor for a player
     * @param player the player
     * @return the factor (0-5, or 6 for hover)
     */
    public byte getVerticalFactor(Player player) {
        UUID playerId = player.getUUID();
        return verticalFactors.getOrDefault(playerId, (byte) 0);
    }
    
    /**
     * Disables hover mode and restores the previous value
     * @param player the player
     * @return the restored factor
     */
    public byte disableHoverMode(Player player) {
        UUID playerId = player.getUUID();
        
        // Get the previous value, or use 0 as fallback
        byte previousFactor = previousVerticalFactors.getOrDefault(playerId, (byte) 0);
        
        // Set the vertical factor to the previous value
        verticalFactors.put(playerId, previousFactor);
        isDirty = true;
        
        return previousFactor;
    }

    /**
     * Sets the horizontal factor for a player
     * @param player the player
     * @param factor the factor (0-5)
     */
    public void setHorizontalFactor(Player player, byte factor) {
        UUID playerId = player.getUUID();
        horizontalFactors.put(playerId, factor);
        isDirty = true;
    }

    /**
     * Gets the horizontal factor for a player
     * @param player the player
     * @return the factor (0-5)
     */
    public byte getHorizontalFactor(Player player) {
        UUID playerId = player.getUUID();
        return horizontalFactors.getOrDefault(playerId, (byte) 0);
    }

    /**
     * Checks if data has been modified
     */
    public boolean isDirty() {
        return isDirty;
    }

    /**
     * Resets the dirty flag
     */
    public void resetDirty() {
        isDirty = false;
    }

    /**
     * Saves data to NBT
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        CompoundTag verticalTag = new CompoundTag();
        CompoundTag horizontalTag = new CompoundTag();
        CompoundTag previousVerticalTag = new CompoundTag();
        CompoundTag parachuteTicksTag = new CompoundTag();

        // Save all vertical factors
        for (Map.Entry<UUID, Byte> entry : verticalFactors.entrySet()) {
            verticalTag.putByte(entry.getKey().toString(), entry.getValue());
        }
        
        // Save all horizontal factors
        for (Map.Entry<UUID, Byte> entry : horizontalFactors.entrySet()) {
            horizontalTag.putByte(entry.getKey().toString(), entry.getValue());
        }
        
        // Save all previous vertical factors
        for (Map.Entry<UUID, Byte> entry : previousVerticalFactors.entrySet()) {
            previousVerticalTag.putByte(entry.getKey().toString(), entry.getValue());
        }
        
        // Save all parachute counters
        for (Map.Entry<UUID, Integer> entry : parachuteTicksLeft.entrySet()) {
            parachuteTicksTag.putInt(entry.getKey().toString(), entry.getValue());
        }

        // Add maps to main tag
        tag.put("vertical_factors", verticalTag);
        tag.put("horizontal_factors", horizontalTag);
        tag.put("previous_vertical_factors", previousVerticalTag);
        tag.put("parachute_ticks", parachuteTicksTag);

        return tag;
    }

    /**
     * Loads data from NBT
     */
    public void load(CompoundTag tag) {
        // Load vertical factors
        if (tag.contains("vertical_factors")) {
            CompoundTag verticalTag = tag.getCompound("vertical_factors");
            for (String key : verticalTag.getAllKeys()) {
                try {
                    UUID playerId = UUID.fromString(key);
                    verticalFactors.put(playerId, verticalTag.getByte(key));
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Invalid UUID in vertical_factors: {}", key);
                }
            }
        }
        
        // Load horizontal factors
        if (tag.contains("horizontal_factors")) {
            CompoundTag horizontalTag = tag.getCompound("horizontal_factors");
            for (String key : horizontalTag.getAllKeys()) {
                try {
                    UUID playerId = UUID.fromString(key);
                    horizontalFactors.put(playerId, horizontalTag.getByte(key));
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Invalid UUID in horizontal_factors: {}", key);
                }
            }
        }
        
        // Load previous vertical factors
        if (tag.contains("previous_vertical_factors")) {
            CompoundTag previousVerticalTag = tag.getCompound("previous_vertical_factors");
            for (String key : previousVerticalTag.getAllKeys()) {
                try {
                    UUID playerId = UUID.fromString(key);
                    previousVerticalFactors.put(playerId, previousVerticalTag.getByte(key));
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Invalid UUID in previous_vertical_factors: {}", key);
                }
            }
        }
        
        // Load parachute counters
        if (tag.contains("parachute_ticks")) {
            CompoundTag parachuteTicksTag = tag.getCompound("parachute_ticks");
            for (String key : parachuteTicksTag.getAllKeys()) {
                try {
                    UUID playerId = UUID.fromString(key);
                    parachuteTicksLeft.put(playerId, parachuteTicksTag.getInt(key));
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Invalid UUID in parachute_ticks: {}", key);
                }
            }
        }
    }

    /**
     * Gets the data instance, creating it if necessary
     */
    public static VectorCharmData get(ServerLevel level) {
        return getInstance();
    }

    /**
     * Activates parachute mode for a player
     * @param player the player
     */
    public void enableParachuteMode(Player player) {
        UUID playerId = player.getUUID();
        
        // Get current factor and store it for future use
        byte currentFactor = verticalFactors.getOrDefault(playerId, (byte) 0);
        
        // Only save the value if we're not already in hover mode and not already in parachute mode
        if (currentFactor != HOVER_MODE_VALUE && currentFactor != PARACHUTE_MODE_VALUE) {
            previousVerticalFactors.put(playerId, currentFactor);
        }
        
        // Set vertical factor directly to parachute value (7)
        verticalFactors.put(playerId, PARACHUTE_MODE_VALUE);
        
        // Initialize counter for parachute duration
        parachuteTicksLeft.put(playerId, PARACHUTE_DURATION);
        
        isDirty = true;
    }
    
    /**
     * Disables parachute mode and restores the previous value
     * @param player the player
     * @return the restored factor
     */
    public byte disableParachuteMode(Player player) {
        UUID playerId = player.getUUID();
        
        // Get the previous value, or use 0 as fallback
        byte previousFactor = previousVerticalFactors.getOrDefault(playerId, (byte) 0);
        
        // Set the vertical factor to the previous value
        verticalFactors.put(playerId, previousFactor);
        
        // Remove the parachute counter
        parachuteTicksLeft.remove(playerId);
        
        // Clean up the previous factor storage
        previousVerticalFactors.remove(playerId);
        
        // No need to remove the Slow Falling effect here - it will expire naturally
        // If needed, it would be: player.removeEffect(MobEffects.SLOW_FALLING);
        
        isDirty = true;
        return previousFactor;
    }
    
    /**
     * Gets the remaining time of parachute mode for a player
     * @param player the player
     * @return the number of ticks left, or 0 if not active
     */
    public int getParachuteTicksLeft(Player player) {
        UUID playerId = player.getUUID();
        return parachuteTicksLeft.getOrDefault(playerId, 0);
    }
    
    /**
     * Decrements the remaining time of parachute mode for a player
     * @param player the player
     * @return true if parachute is still active, false if it has ended
     */
    public boolean decrementParachuteTicks(Player player) {
        UUID playerId = player.getUUID();
        if (!parachuteTicksLeft.containsKey(playerId)) {
            return false;
        }
        
        int ticksLeft = parachuteTicksLeft.get(playerId) - 1;
        if (ticksLeft <= 0) {
            // Parachute has ended, restore previous value
            disableParachuteMode(player);
            return false;
        } else {
            // Update counter
            parachuteTicksLeft.put(playerId, ticksLeft);
            return true;
        }
    }
} 