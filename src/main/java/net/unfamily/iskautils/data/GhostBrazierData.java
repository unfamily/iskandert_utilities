package net.unfamily.iskautils.data;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.unfamily.iskautils.IskaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Class to manage persistent Ghost Brazier data.
 * Manages whether player has Ghost Brazier in inventory and previous game mode.
 */
public class GhostBrazierData extends SavedData {
    private static final Logger LOGGER = LoggerFactory.getLogger(GhostBrazierData.class);
    private static final String DATA_NAME = IskaUtils.MOD_ID + "_ghost_brazier_data";
    private static final SavedDataType<GhostBrazierData> TYPE = new SavedDataType<>(
            Identifier.tryBuild(IskaUtils.MOD_ID, "ghost_brazier_data"),
            GhostBrazierData::new,
            CompoundTag.CODEC.xmap(GhostBrazierData::fromTag, GhostBrazierData::toTag)
    );

    // Map to store whether player has Ghost Brazier in inventory
    final Map<UUID, Boolean> hasGhostBrazier = new HashMap<>();
    
    // Map to store previous game mode before switching to spectator
    final Map<UUID, GameType> previousGameMode = new HashMap<>();

    /**
     * Private constructor for SavedData
     */
    public GhostBrazierData() {
        // Private constructor
    }


    /**
     * Gets the Ghost Brazier data from world storage
     * @param level the server level
     * @return the Ghost Brazier data
     */
    public static GhostBrazierData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /**
     * Sets whether player has Ghost Brazier using static method
     * @param player the player
     * @param hasItem true if player has the item, false otherwise
     */
    public static void setHasGhostBrazier(Player player, boolean hasItem) {
        if (player.level() instanceof ServerLevel level) {
            GhostBrazierData data = get(level);
            UUID playerId = player.getUUID();
            data.hasGhostBrazier.put(playerId, hasItem);
            
            // If player no longer has the item, clear previous game mode
            if (!hasItem) {
                data.previousGameMode.remove(playerId);
            }
            
            data.setDirty();
        }
    }

    /**
     * Gets whether player has Ghost Brazier using static method
     * @param player the player
     * @return true if player has the item (default: false)
     */
    public static boolean getHasGhostBrazier(Player player) {
        if (player.level() instanceof ServerLevel level) {
            UUID playerId = player.getUUID();
            return get(level).hasGhostBrazier.getOrDefault(playerId, false);
        }
        return false; // Default if server is not available
    }

    /**
     * Sets the previous game mode for a player using static method
     * @param player the player
     * @param gameMode the previous game mode
     */
    public static void setPreviousGameMode(Player player, GameType gameMode) {
        if (player.level() instanceof ServerLevel level) {
            GhostBrazierData data = get(level);
            UUID playerId = player.getUUID();
            data.previousGameMode.put(playerId, gameMode);
            data.setDirty();
        }
    }

    /**
     * Gets the previous game mode for a player using static method
     * @param player the player
     * @return the previous game mode, or SURVIVAL if not set
     */
    public static GameType getPreviousGameMode(Player player) {
        if (player.level() instanceof ServerLevel level) {
            UUID playerId = player.getUUID();
            return get(level).previousGameMode.getOrDefault(playerId, GameType.SURVIVAL);
        }
        return GameType.SURVIVAL; // Default if server is not available
    }

    /**
     * Clears the previous game mode for a player using static method
     * @param player the player
     */
    public static void clearPreviousGameMode(Player player) {
        if (player.level() instanceof ServerLevel level) {
            GhostBrazierData data = get(level);
            UUID playerId = player.getUUID();
            data.previousGameMode.remove(playerId);
            data.setDirty();
        }
    }

    /**
     * Loads the data from NBT
     * @param tag the compound tag containing the data
     * @param provider the holder lookup provider
     * @return the loaded GhostBrazierData
     */
    public static GhostBrazierData load(CompoundTag tag, HolderLookup.Provider provider) {
        return fromTag(tag);
    }

    private static GhostBrazierData fromTag(CompoundTag tag) {
        GhostBrazierData data = new GhostBrazierData();

        // Load hasGhostBrazier states
        CompoundTag hasItemTag = tag.getCompound("hasGhostBrazier").orElse(new CompoundTag());
        for (String key : hasItemTag.keySet()) {
            try {
                UUID playerId = UUID.fromString(key);
                boolean hasItem = hasItemTag.getBoolean(key).orElse(false);
                data.hasGhostBrazier.put(playerId, hasItem);
            } catch (Exception e) {
                LOGGER.warn("Failed to parse player UUID: {}", key, e);
            }
        }

        // Load previous game modes
        CompoundTag gameModeTag = tag.getCompound("previousGameMode").orElse(new CompoundTag());
        for (String key : gameModeTag.keySet()) {
            try {
                UUID playerId = UUID.fromString(key);
                int gameModeId = gameModeTag.getInt(key).orElse(0);
                GameType gameMode = GameType.byId(gameModeId);
                data.previousGameMode.put(playerId, gameMode);
            } catch (Exception e) {
                LOGGER.warn("Failed to parse game mode for player UUID: {}", key, e);
            }
        }

        LOGGER.debug("Loaded Ghost Brazier data for {} players", data.hasGhostBrazier.size());
        return data;
    }

    private static CompoundTag toTag(GhostBrazierData data) {
        CompoundTag tag = new CompoundTag();

        // Save hasGhostBrazier states
        CompoundTag hasItemTag = new CompoundTag();
        for (Map.Entry<UUID, Boolean> entry : data.hasGhostBrazier.entrySet()) {
            hasItemTag.putBoolean(entry.getKey().toString(), entry.getValue());
        }
        tag.put("hasGhostBrazier", hasItemTag);

        // Save previous game modes
        CompoundTag gameModeTag = new CompoundTag();
        for (Map.Entry<UUID, GameType> entry : data.previousGameMode.entrySet()) {
            gameModeTag.putInt(entry.getKey().toString(), entry.getValue().getId());
        }
        tag.put("previousGameMode", gameModeTag);

        return tag;
    }

    /**
     * Saves the data to NBT
     * @param tag the compound tag to save to
     * @param provider the holder lookup provider
     * @return the compound tag with the data
     */
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        return toTag(this);
    }

    /**
     * Gets the number of players with stored data
     * @return the number of players
     */
    public int getStoredPlayerCount() {
        return hasGhostBrazier.size();
    }
}
