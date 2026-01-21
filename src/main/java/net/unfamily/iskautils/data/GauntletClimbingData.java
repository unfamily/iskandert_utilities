package net.unfamily.iskautils.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.unfamily.iskautils.IskaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Class to manage persistent Gauntlet of Climbing data.
 * Manages climbing enabled state for each player (default: true).
 */
public class GauntletClimbingData extends SavedData {
    private static final Logger LOGGER = LoggerFactory.getLogger(GauntletClimbingData.class);
    private static final String DATA_NAME = IskaUtils.MOD_ID + "_gauntlet_climbing_data";

    // Map to store climbing enabled state for each player (default: true)
    private final Map<UUID, Boolean> climbingEnabled = new HashMap<>();

    /**
     * Private constructor for SavedData
     */
    public GauntletClimbingData() {
        // Private constructor
    }

    /**
     * Gets the Gauntlet Climbing data from world storage
     * @param level the server level
     * @return the Gauntlet Climbing data
     */
    public static GauntletClimbingData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            new Factory<>(GauntletClimbingData::new, GauntletClimbingData::load),
            DATA_NAME
        );
    }

    /**
     * Sets the climbing enabled state for a player using static method
     * @param player the player
     * @param enabled true if climbing should be enabled, false otherwise
     */
    public static void setClimbingEnabled(Player player, boolean enabled) {
        if (player.getServer() != null && player.getServer().overworld() instanceof ServerLevel level) {
            GauntletClimbingData data = get(level);
            UUID playerId = player.getUUID();
            data.climbingEnabled.put(playerId, enabled);
            data.setDirty();
        }
    }

    /**
     * Gets the climbing enabled state for a player using static method
     * @param player the player
     * @return true if climbing is enabled (default: true)
     */
    public static boolean isClimbingEnabled(Player player) {
        if (player.getServer() != null && player.getServer().overworld() instanceof ServerLevel level) {
            UUID playerId = player.getUUID();
            return get(level).climbingEnabled.getOrDefault(playerId, true); // Default: enabled
        }
        return true; // Default: enabled if server is not available
    }

    /**
     * Toggles the climbing enabled state for a player
     * @param player the player
     * @return the new state after toggling
     */
    public static boolean toggleClimbing(Player player) {
        boolean newState = !isClimbingEnabled(player);
        setClimbingEnabled(player, newState);
        return newState;
    }

    /**
     * Loads the data from NBT
     * @param tag the compound tag containing the data
     * @param provider the holder lookup provider
     * @return the loaded GauntletClimbingData
     */
    public static GauntletClimbingData load(CompoundTag tag, HolderLookup.Provider provider) {
        GauntletClimbingData data = new GauntletClimbingData();

        // Load climbing enabled states
        CompoundTag enabledTag = tag.getCompound("climbingEnabled");
        for (String key : enabledTag.getAllKeys()) {
            try {
                UUID playerId = UUID.fromString(key);
                boolean enabled = enabledTag.getBoolean(key);
                data.climbingEnabled.put(playerId, enabled);
            } catch (Exception e) {
                LOGGER.warn("Failed to parse player UUID: {}", key, e);
            }
        }

        LOGGER.debug("Loaded Gauntlet Climbing data for {} players", data.climbingEnabled.size());
        return data;
    }

    /**
     * Saves the data to NBT
     * @param tag the compound tag to save to
     * @param provider the holder lookup provider
     * @return the compound tag with the data
     */
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        // Save climbing enabled states
        CompoundTag enabledTag = new CompoundTag();
        for (Map.Entry<UUID, Boolean> entry : climbingEnabled.entrySet()) {
            enabledTag.putBoolean(entry.getKey().toString(), entry.getValue());
        }
        tag.put("climbingEnabled", enabledTag);

        LOGGER.debug("Saved Gauntlet Climbing data");
        return tag;
    }
}
