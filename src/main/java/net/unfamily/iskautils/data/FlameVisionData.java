package net.unfamily.iskautils.data;

import net.unfamily.iskautils.util.ModLogger;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.unfamily.iskautils.IskaUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-player global flame vision toggle (Blazing Altar GUI). */
public class FlameVisionData extends SavedData {
    private static final ModLogger LOGGER = ModLogger.of(FlameVisionData.class);
    private static final SavedDataType<FlameVisionData> TYPE = new SavedDataType<>(
            Identifier.tryBuild(IskaUtils.MOD_ID, "flame_vision_data"),
            FlameVisionData::new,
            CompoundTag.CODEC.xmap(FlameVisionData::fromTag, FlameVisionData::toTag)
    );

    private final Map<UUID, Boolean> flameVisionEnabled = new HashMap<>();

    public FlameVisionData() {}

    public void setFlameVisionEnabled(Player player, boolean enabled) {
        flameVisionEnabled.put(player.getUUID(), enabled);
        setDirty();
    }

    public boolean getFlameVisionEnabled(Player player) {
        return flameVisionEnabled.getOrDefault(player.getUUID(), false);
    }

    public static FlameVisionData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public static void setFlameVisionEnabledForPlayer(Player player, boolean enabled) {
        if (player.level() instanceof ServerLevel level) {
            get(level).setFlameVisionEnabled(player, enabled);
        }
    }

    public static boolean getFlameVisionEnabledForPlayer(Player player) {
        if (player.level() instanceof ServerLevel level) {
            return get(level).getFlameVisionEnabled(player);
        }
        return false;
    }

    private static FlameVisionData fromTag(CompoundTag tag) {
        FlameVisionData data = new FlameVisionData();
        CompoundTag visionTag = tag.getCompound("flameVisionEnabled").orElse(new CompoundTag());
        for (String key : visionTag.keySet()) {
            try {
                data.flameVisionEnabled.put(UUID.fromString(key), visionTag.getBoolean(key).orElse(false));
            } catch (Exception e) {
                LOGGER.warn("Failed to parse player UUID: {}", key, e);
            }
        }
        return data;
    }

    private static CompoundTag toTag(FlameVisionData data) {
        CompoundTag tag = new CompoundTag();
        CompoundTag visionTag = new CompoundTag();
        for (Map.Entry<UUID, Boolean> entry : data.flameVisionEnabled.entrySet()) {
            visionTag.putBoolean(entry.getKey().toString(), entry.getValue());
        }
        tag.put("flameVisionEnabled", visionTag);
        return tag;
    }
}
