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
 * Class to manage persistent Burning Brazier data.
 * Manages auto-placement toggle state for each player.
 */
public class BurningBrazierData extends SavedData {
    private static final Logger LOGGER = LoggerFactory.getLogger(BurningBrazierData.class);
    private static final String DATA_NAME = IskaUtils.MOD_ID + "_burning_brazier_data";

    // Map to store auto-placement enabled state for each player (default: true)
    private final Map<UUID, Boolean> autoPlacementEnabled = new HashMap<>();

    /**
     * Private constructor for SavedData
     */
    public BurningBrazierData() {
        // Private constructor
    }

    /**
     * Sets the auto-placement enabled state for a player
     * @param player the player
     * @param enabled true if auto-placement should be enabled, false otherwise
     */
    public void setAutoPlacementEnabled(Player player, boolean enabled) {
        UUID playerId = player.getUUID();
        autoPlacementEnabled.put(playerId, enabled);
        setDirty();
    }

    /**
     * Gets the auto-placement enabled state for a player
     * @param player the player
     * @return true if auto-placement is enabled (default: true)
     */
    public boolean getAutoPlacementEnabled(Player player) {
        UUID playerId = player.getUUID();
        return autoPlacementEnabled.getOrDefault(playerId, true); // Default: enabled
    }

    /**
     * Gets the Burning Brazier data from world storage
     * @param level the server level
     * @return the Burning Brazier data
     */
    public static BurningBrazierData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            new Factory<>(BurningBrazierData::new, BurningBrazierData::load),
            DATA_NAME
        );
    }

    /**
     * Sets the auto-placement enabled state for a player using static method
     * @param player the player
     * @param enabled true if auto-placement should be enabled, false otherwise
     */
    public static void setAutoPlacementEnabledToPlayer(Player player, boolean enabled) {
        if (player.getServer() != null && player.getServer().overworld() instanceof ServerLevel level) {
            get(level).setAutoPlacementEnabled(player, enabled);
        }
    }

    /**
     * Gets the auto-placement enabled state for a player using static method
     * @param player the player
     * @return true if auto-placement is enabled (default: true)
     */
    public static boolean getAutoPlacementEnabledFromPlayer(Player player) {
        if (player.getServer() != null && player.getServer().overworld() instanceof ServerLevel level) {
            return get(level).getAutoPlacementEnabled(player);
        }
        return true; // Default if server is not available
    }

    /**
     * Loads the data from NBT
     * @param tag the compound tag containing the data
     * @param provider the holder lookup provider
     * @return the loaded BurningBrazierData
     */
    public static BurningBrazierData load(CompoundTag tag, HolderLookup.Provider provider) {
        BurningBrazierData data = new BurningBrazierData();

        // Load auto-placement states
        CompoundTag autoPlacementTag = tag.getCompound("autoPlacementEnabled");
        for (String key : autoPlacementTag.getAllKeys()) {
            try {
                UUID playerId = UUID.fromString(key);
                boolean enabled = autoPlacementTag.getBoolean(key);
                data.autoPlacementEnabled.put(playerId, enabled);
            } catch (Exception e) {
                LOGGER.warn("Failed to parse player UUID: {}", key, e);
            }
        }

        LOGGER.debug("Loaded Burning Brazier data for {} players", data.autoPlacementEnabled.size());
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
        // Save auto-placement states
        CompoundTag autoPlacementTag = new CompoundTag();
        for (Map.Entry<UUID, Boolean> entry : autoPlacementEnabled.entrySet()) {
            autoPlacementTag.putBoolean(entry.getKey().toString(), entry.getValue());
        }
        tag.put("autoPlacementEnabled", autoPlacementTag);

        LOGGER.debug("Saved Burning Brazier data");
        return tag;
    }

    /**
     * Gets the number of players with stored data
     * @return the number of players
     */
    public int getStoredPlayerCount() {
        return autoPlacementEnabled.size();
    }
}
