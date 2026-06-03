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

/** Per-player global flame vision toggle (Blazing Altar GUI). */
public class FlameVisionData extends SavedData {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlameVisionData.class);
    private static final String DATA_NAME = IskaUtils.MOD_ID + "_flame_vision_data";

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
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(FlameVisionData::new, FlameVisionData::load),
                DATA_NAME);
    }

    public static void setFlameVisionEnabledForPlayer(Player player, boolean enabled) {
        if (player.getServer() != null && player.getServer().overworld() instanceof ServerLevel level) {
            get(level).setFlameVisionEnabled(player, enabled);
        }
    }

    public static boolean getFlameVisionEnabledForPlayer(Player player) {
        if (player.getServer() != null && player.getServer().overworld() instanceof ServerLevel level) {
            return get(level).getFlameVisionEnabled(player);
        }
        return false;
    }

    public static FlameVisionData load(CompoundTag tag, HolderLookup.Provider provider) {
        FlameVisionData data = new FlameVisionData();
        CompoundTag visionTag = tag.getCompound("flameVisionEnabled");
        for (String key : visionTag.getAllKeys()) {
            try {
                data.flameVisionEnabled.put(UUID.fromString(key), visionTag.getBoolean(key));
            } catch (Exception e) {
                LOGGER.warn("Failed to parse player UUID: {}", key, e);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag visionTag = new CompoundTag();
        for (Map.Entry<UUID, Boolean> entry : flameVisionEnabled.entrySet()) {
            visionTag.putBoolean(entry.getKey().toString(), entry.getValue());
        }
        tag.put("flameVisionEnabled", visionTag);
        return tag;
    }
}
