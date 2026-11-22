package net.unfamily.iskautils.data;

import net.unfamily.iskautils.util.ModLogger;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.saveddata.SavedData;
import net.unfamily.iskautils.IskaUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent Ghost Brazier state: item presence, previous game mode, cast, ethereal pool.
 */
public class GhostBrazierData extends SavedData {
    private static final ModLogger LOGGER = ModLogger.of(GhostBrazierData.class);
    private static final String DATA_NAME = IskaUtils.MOD_ID + "_ghost_brazier_data";

    public enum CastState {
        IDLE,
        CAST_IN,
        CAST_OUT,
        ETHEREAL
    }

    final Map<UUID, Boolean> hasGhostBrazier = new HashMap<>();
    final Map<UUID, GameType> previousGameMode = new HashMap<>();
    final Map<UUID, CastState> castState = new HashMap<>();
    final Map<UUID, Long> castStartTick = new HashMap<>();
    final Map<UUID, Double> remainingPoolTicks = new HashMap<>();

    public GhostBrazierData() {}

    public static GhostBrazierData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            new Factory<>(GhostBrazierData::new, GhostBrazierData::load),
            DATA_NAME
        );
    }

    private static ServerLevel levelFor(Player player) {
        if (player.level() instanceof ServerLevel sl) {
            return sl;
        }
        if (player.getServer() != null && player.getServer().overworld() instanceof ServerLevel level) {
            return level;
        }
        return null;
    }

    public static void setHasGhostBrazier(Player player, boolean hasItem) {
        ServerLevel level = levelFor(player);
        if (level == null) {
            return;
        }
        GhostBrazierData data = get(level);
        UUID id = player.getUUID();
        data.hasGhostBrazier.put(id, hasItem);
        if (!hasItem) {
            data.previousGameMode.remove(id);
        }
        data.setDirty();
    }

    public static boolean getHasGhostBrazier(Player player) {
        ServerLevel level = levelFor(player);
        if (level == null) {
            return false;
        }
        return get(level).hasGhostBrazier.getOrDefault(player.getUUID(), false);
    }

    public static void setPreviousGameMode(Player player, GameType gameMode) {
        ServerLevel level = levelFor(player);
        if (level == null) {
            return;
        }
        GhostBrazierData data = get(level);
        data.previousGameMode.put(player.getUUID(), gameMode);
        data.setDirty();
    }

    public static GameType getPreviousGameMode(Player player) {
        ServerLevel level = levelFor(player);
        if (level == null) {
            return GameType.SURVIVAL;
        }
        return get(level).previousGameMode.getOrDefault(player.getUUID(), GameType.SURVIVAL);
    }

    public static void clearPreviousGameMode(Player player) {
        ServerLevel level = levelFor(player);
        if (level == null) {
            return;
        }
        GhostBrazierData data = get(level);
        data.previousGameMode.remove(player.getUUID());
        data.setDirty();
    }

    public static CastState getCastState(Player player) {
        ServerLevel level = levelFor(player);
        if (level == null) {
            return CastState.IDLE;
        }
        return get(level).castState.getOrDefault(player.getUUID(), CastState.IDLE);
    }

    public static void setCastState(Player player, CastState state) {
        ServerLevel level = levelFor(player);
        if (level == null) {
            return;
        }
        GhostBrazierData data = get(level);
        UUID id = player.getUUID();
        if (state == CastState.IDLE) {
            data.castState.remove(id);
            data.castStartTick.remove(id);
        } else {
            data.castState.put(id, state);
        }
        data.setDirty();
    }

    public static void setCastStartTick(Player player, long tick) {
        ServerLevel level = levelFor(player);
        if (level == null) {
            return;
        }
        GhostBrazierData data = get(level);
        data.castStartTick.put(player.getUUID(), tick);
        data.setDirty();
    }

    public static long getCastStartTick(Player player) {
        ServerLevel level = levelFor(player);
        if (level == null) {
            return 0L;
        }
        return get(level).castStartTick.getOrDefault(player.getUUID(), 0L);
    }

    public static void setRemainingPoolTicks(Player player, double ticks) {
        ServerLevel level = levelFor(player);
        if (level == null) {
            return;
        }
        GhostBrazierData data = get(level);
        data.remainingPoolTicks.put(player.getUUID(), ticks);
        data.setDirty();
    }

    public static double getRemainingPoolTicks(Player player) {
        ServerLevel level = levelFor(player);
        if (level == null) {
            return 0.0D;
        }
        return get(level).remainingPoolTicks.getOrDefault(player.getUUID(), 0.0D);
    }

    public static void clearEtherealSession(Player player) {
        ServerLevel level = levelFor(player);
        if (level == null) {
            return;
        }
        GhostBrazierData data = get(level);
        UUID id = player.getUUID();
        data.castState.remove(id);
        data.castStartTick.remove(id);
        data.remainingPoolTicks.remove(id);
        data.previousGameMode.remove(id);
        data.setDirty();
    }

    public static boolean isEthereal(Player player) {
        return getCastState(player) == CastState.ETHEREAL;
    }

    public static boolean isCasting(Player player) {
        return getCastState(player) == CastState.CAST_IN;
    }

    public static GhostBrazierData load(CompoundTag tag, HolderLookup.Provider provider) {
        GhostBrazierData data = new GhostBrazierData();

        CompoundTag hasItemTag = tag.getCompound("hasGhostBrazier");
        for (String key : hasItemTag.getAllKeys()) {
            try {
                data.hasGhostBrazier.put(UUID.fromString(key), hasItemTag.getBoolean(key));
            } catch (Exception e) {
                LOGGER.warn("Failed to parse player UUID: {}", key, e);
            }
        }

        CompoundTag gameModeTag = tag.getCompound("previousGameMode");
        for (String key : gameModeTag.getAllKeys()) {
            try {
                data.previousGameMode.put(UUID.fromString(key), GameType.byId(gameModeTag.getInt(key)));
            } catch (Exception e) {
                LOGGER.warn("Failed to parse game mode for player UUID: {}", key, e);
            }
        }

        CompoundTag castTag = tag.getCompound("castState");
        for (String key : castTag.getAllKeys()) {
            try {
                data.castState.put(UUID.fromString(key), CastState.valueOf(castTag.getString(key)));
            } catch (Exception e) {
                LOGGER.warn("Failed to parse cast state for player UUID: {}", key, e);
            }
        }

        CompoundTag castStartTag = tag.getCompound("castStartTick");
        for (String key : castStartTag.getAllKeys()) {
            try {
                data.castStartTick.put(UUID.fromString(key), castStartTag.getLong(key));
            } catch (Exception ignored) {
            }
        }

        CompoundTag poolTag = tag.getCompound("remainingPoolTicks");
        for (String key : poolTag.getAllKeys()) {
            try {
                data.remainingPoolTicks.put(UUID.fromString(key), poolTag.getDouble(key));
            } catch (Exception ignored) {
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag hasItemTag = new CompoundTag();
        for (Map.Entry<UUID, Boolean> entry : hasGhostBrazier.entrySet()) {
            hasItemTag.putBoolean(entry.getKey().toString(), entry.getValue());
        }
        tag.put("hasGhostBrazier", hasItemTag);

        CompoundTag gameModeTag = new CompoundTag();
        for (Map.Entry<UUID, GameType> entry : previousGameMode.entrySet()) {
            gameModeTag.putInt(entry.getKey().toString(), entry.getValue().getId());
        }
        tag.put("previousGameMode", gameModeTag);

        CompoundTag castTag = new CompoundTag();
        for (Map.Entry<UUID, CastState> entry : castState.entrySet()) {
            castTag.putString(entry.getKey().toString(), entry.getValue().name());
        }
        tag.put("castState", castTag);

        CompoundTag castStartTag = new CompoundTag();
        for (Map.Entry<UUID, Long> entry : castStartTick.entrySet()) {
            castStartTag.putLong(entry.getKey().toString(), entry.getValue());
        }
        tag.put("castStartTick", castStartTag);

        CompoundTag poolTag = new CompoundTag();
        for (Map.Entry<UUID, Double> entry : remainingPoolTicks.entrySet()) {
            poolTag.putDouble(entry.getKey().toString(), entry.getValue());
        }
        tag.put("remainingPoolTicks", poolTag);

        return tag;
    }
}