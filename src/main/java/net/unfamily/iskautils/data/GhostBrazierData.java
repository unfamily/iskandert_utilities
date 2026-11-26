package net.unfamily.iskautils.data;

import net.unfamily.iskautils.util.ModLogger;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.unfamily.iskautils.IskaUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent Ghost Brazier state: item presence, previous game mode, cast, ethereal pool.
 */
public class GhostBrazierData extends SavedData {
    private static final ModLogger LOGGER = ModLogger.of(GhostBrazierData.class);
    private static final SavedDataType<GhostBrazierData> TYPE = new SavedDataType<>(
            Identifier.tryBuild(IskaUtils.MOD_ID, "ghost_brazier_data"),
            GhostBrazierData::new,
            CompoundTag.CODEC.xmap(GhostBrazierData::fromTag, GhostBrazierData::toTag)
    );

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
    
    // Anti-stuck: track safe position and block collision count
    final Map<UUID, BlockPos> safePosition = new HashMap<>();
    final Map<UUID, Integer> consecutiveBlockTicks = new HashMap<>();
    // Cooldown tracking
    final Map<UUID, Long> lastGlobalEtherealEndTick = new HashMap<>();

    public GhostBrazierData() {}

    public static GhostBrazierData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public static void setHasGhostBrazier(Player player, boolean hasItem) {
        if (!(player.level() instanceof ServerLevel level)) {
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
        if (player.level() instanceof ServerLevel level) {
            return get(level).hasGhostBrazier.getOrDefault(player.getUUID(), false);
        }
        return false;
    }

    public static void setPreviousGameMode(Player player, GameType gameMode) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        GhostBrazierData data = get(level);
        data.previousGameMode.put(player.getUUID(), gameMode);
        data.setDirty();
    }

    public static GameType getPreviousGameMode(Player player) {
        if (player.level() instanceof ServerLevel level) {
            return get(level).previousGameMode.getOrDefault(player.getUUID(), GameType.SURVIVAL);
        }
        return GameType.SURVIVAL;
    }

    public static void clearPreviousGameMode(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        GhostBrazierData data = get(level);
        data.previousGameMode.remove(player.getUUID());
        data.setDirty();
    }

    public static CastState getCastState(Player player) {
        if (player.level() instanceof ServerLevel level) {
            return get(level).castState.getOrDefault(player.getUUID(), CastState.IDLE);
        }
        return CastState.IDLE;
    }

    public static void setCastState(Player player, CastState state) {
        if (!(player.level() instanceof ServerLevel level)) {
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
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        GhostBrazierData data = get(level);
        data.castStartTick.put(player.getUUID(), tick);
        data.setDirty();
    }

    public static long getCastStartTick(Player player) {
        if (player.level() instanceof ServerLevel level) {
            return get(level).castStartTick.getOrDefault(player.getUUID(), 0L);
        }
        return 0L;
    }

    public static void setRemainingPoolTicks(Player player, double ticks) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        GhostBrazierData data = get(level);
        data.remainingPoolTicks.put(player.getUUID(), ticks);
        data.setDirty();
    }

    public static double getRemainingPoolTicks(Player player) {
        if (player.level() instanceof ServerLevel level) {
            return get(level).remainingPoolTicks.getOrDefault(player.getUUID(), 0.0D);
        }
        return 0.0D;
    }

    public static void clearEtherealSession(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        GhostBrazierData data = get(level);
        UUID id = player.getUUID();
        data.castState.remove(id);
        data.castStartTick.remove(id);
        data.remainingPoolTicks.remove(id);
        data.previousGameMode.remove(id);
        data.safePosition.remove(id);
        data.consecutiveBlockTicks.remove(id);
        data.setDirty();
    }
    
    public static void setSafePosition(Player player, BlockPos pos) {
        if (!(player.level() instanceof ServerLevel level) || pos == null) {
            return;
        }
        GhostBrazierData data = get(level);
        BlockPos previous = data.safePosition.put(player.getUUID(), pos.immutable());
        if (previous == null || !previous.equals(pos)) {
            data.setDirty();
        }
    }
    
    public static BlockPos getSafePosition(Player player) {
        if (player.level() instanceof ServerLevel level) {
            return get(level).safePosition.getOrDefault(player.getUUID(), null);
        }
        return null;
    }
    
    public static void setConsecutiveBlockTicks(Player player, int ticks) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        GhostBrazierData data = get(level);
        data.consecutiveBlockTicks.put(player.getUUID(), ticks);
        data.setDirty();
    }
    
    public static int getConsecutiveBlockTicks(Player player) {
        if (player.level() instanceof ServerLevel level) {
            return get(level).consecutiveBlockTicks.getOrDefault(player.getUUID(), 0);
        }
        return 0;
    }
    
    public static void incrementConsecutiveBlockTicks(Player player) {
        int current = getConsecutiveBlockTicks(player);
        setConsecutiveBlockTicks(player, current + 1);
    }
    
    public static void resetConsecutiveBlockTicks(Player player) {
        setConsecutiveBlockTicks(player, 0);
    }
    
    public static void setLastGlobalEtherealEndTick(Player player, long tick) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        GhostBrazierData data = get(level);
        data.lastGlobalEtherealEndTick.put(player.getUUID(), tick);
        data.setDirty();
    }
    
    public static long getLastGlobalEtherealEndTick(Player player) {
        if (player.level() instanceof ServerLevel level) {
            return get(level).lastGlobalEtherealEndTick.getOrDefault(player.getUUID(), 0L);
        }
        return 0L;
    }

    public static boolean isEthereal(Player player) {
        return getCastState(player) == CastState.ETHEREAL;
    }

    public static boolean isCasting(Player player) {
        return getCastState(player) == CastState.CAST_IN;
    }

    private static GhostBrazierData fromTag(CompoundTag tag) {
        GhostBrazierData data = new GhostBrazierData();

        CompoundTag hasItemTag = tag.getCompound("hasGhostBrazier").orElse(new CompoundTag());
        for (String key : hasItemTag.keySet()) {
            try {
                data.hasGhostBrazier.put(UUID.fromString(key), hasItemTag.getBoolean(key).orElse(false));
            } catch (Exception e) {
                LOGGER.warn("Failed to parse player UUID: {}", key, e);
            }
        }

        CompoundTag gameModeTag = tag.getCompound("previousGameMode").orElse(new CompoundTag());
        for (String key : gameModeTag.keySet()) {
            try {
                data.previousGameMode.put(UUID.fromString(key), GameType.byId(gameModeTag.getInt(key).orElse(0)));
            } catch (Exception e) {
                LOGGER.warn("Failed to parse game mode for player UUID: {}", key, e);
            }
        }

        CompoundTag castTag = tag.getCompound("castState").orElse(new CompoundTag());
        for (String key : castTag.keySet()) {
            try {
                data.castState.put(UUID.fromString(key), CastState.valueOf(castTag.getString(key).orElse("IDLE")));
            } catch (Exception e) {
                LOGGER.warn("Failed to parse cast state for player UUID: {}", key, e);
            }
        }

        CompoundTag castStartTag = tag.getCompound("castStartTick").orElse(new CompoundTag());
        for (String key : castStartTag.keySet()) {
            try {
                data.castStartTick.put(UUID.fromString(key), castStartTag.getLong(key).orElse(0L));
            } catch (Exception ignored) {
            }
        }

        CompoundTag poolTag = tag.getCompound("remainingPoolTicks").orElse(new CompoundTag());
        for (String key : poolTag.keySet()) {
            try {
                data.remainingPoolTicks.put(UUID.fromString(key), poolTag.getDouble(key).orElse(0.0D));
            } catch (Exception ignored) {
            }
        }

        CompoundTag safePosTag = tag.getCompound("safePosition").orElse(new CompoundTag());
        for (String key : safePosTag.keySet()) {
            try {
                BlockPos pos = BlockPos.of(safePosTag.getLong(key).orElse(0L));
                data.safePosition.put(UUID.fromString(key), pos);
            } catch (Exception ignored) {
            }
        }

        CompoundTag consecutiveTag = tag.getCompound("consecutiveBlockTicks").orElse(new CompoundTag());
        for (String key : consecutiveTag.keySet()) {
            try {
                data.consecutiveBlockTicks.put(UUID.fromString(key), consecutiveTag.getInt(key).orElse(0));
            } catch (Exception ignored) {
            }
        }

        CompoundTag cooldownTag = tag.getCompound("lastGlobalEtherealEndTick").orElse(new CompoundTag());
        for (String key : cooldownTag.keySet()) {
            try {
                data.lastGlobalEtherealEndTick.put(UUID.fromString(key), cooldownTag.getLong(key).orElse(0L));
            } catch (Exception ignored) {
            }
        }

        return data;
    }

    private static CompoundTag toTag(GhostBrazierData data) {
        CompoundTag tag = new CompoundTag();

        CompoundTag hasItemTag = new CompoundTag();
        for (Map.Entry<UUID, Boolean> entry : data.hasGhostBrazier.entrySet()) {
            hasItemTag.putBoolean(entry.getKey().toString(), entry.getValue());
        }
        tag.put("hasGhostBrazier", hasItemTag);

        CompoundTag gameModeTag = new CompoundTag();
        for (Map.Entry<UUID, GameType> entry : data.previousGameMode.entrySet()) {
            gameModeTag.putInt(entry.getKey().toString(), entry.getValue().getId());
        }
        tag.put("previousGameMode", gameModeTag);

        CompoundTag castTag = new CompoundTag();
        for (Map.Entry<UUID, CastState> entry : data.castState.entrySet()) {
            castTag.putString(entry.getKey().toString(), entry.getValue().name());
        }
        tag.put("castState", castTag);

        CompoundTag castStartTag = new CompoundTag();
        for (Map.Entry<UUID, Long> entry : data.castStartTick.entrySet()) {
            castStartTag.putLong(entry.getKey().toString(), entry.getValue());
        }
        tag.put("castStartTick", castStartTag);

        CompoundTag poolTag = new CompoundTag();
        for (Map.Entry<UUID, Double> entry : data.remainingPoolTicks.entrySet()) {
            poolTag.putDouble(entry.getKey().toString(), entry.getValue());
        }
        tag.put("remainingPoolTicks", poolTag);

        CompoundTag safePosTag = new CompoundTag();
        for (Map.Entry<UUID, BlockPos> entry : data.safePosition.entrySet()) {
            safePosTag.putLong(entry.getKey().toString(), entry.getValue().asLong());
        }
        tag.put("safePosition", safePosTag);

        CompoundTag consecutiveTag = new CompoundTag();
        for (Map.Entry<UUID, Integer> entry : data.consecutiveBlockTicks.entrySet()) {
            consecutiveTag.putInt(entry.getKey().toString(), entry.getValue());
        }
        tag.put("consecutiveBlockTicks", consecutiveTag);

        CompoundTag cooldownTag = new CompoundTag();
        for (Map.Entry<UUID, Long> entry : data.lastGlobalEtherealEndTick.entrySet()) {
            cooldownTag.putLong(entry.getKey().toString(), entry.getValue());
        }
        tag.put("lastGlobalEtherealEndTick", cooldownTag);

        return tag;
    }
}
