package net.unfamily.iskautils.data;

import net.unfamily.iskautils.util.ModLogger;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryScriptRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persists suspicious delivery pending script runs across server restarts.
 */
public class SuspiciousDeliveryScriptData extends SavedData {
    private static final ModLogger LOGGER = ModLogger.of(SuspiciousDeliveryScriptData.class);
    private static final String DATA_NAME = IskaUtils.MOD_ID + "_suspicious_delivery_script_data";
    private static final String KILL_FAKE_TNT = "kill @e[type=minecraft:tnt,tag=fake_tnt]";

    private final List<SuspiciousDeliveryScriptRunner.StoredPendingRun> pendingRuns = new ArrayList<>();

    public SuspiciousDeliveryScriptData() {}

    public static SuspiciousDeliveryScriptData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new Factory<>(SuspiciousDeliveryScriptData::new, SuspiciousDeliveryScriptData::load),
                DATA_NAME
        );
    }

    public static void syncFromRunner(MinecraftServer server) {
        if (!(server.overworld() instanceof ServerLevel overworld)) {
            return;
        }
        SuspiciousDeliveryScriptData data = get(overworld);
        data.pendingRuns.clear();
        data.pendingRuns.addAll(SuspiciousDeliveryScriptRunner.exportPending());
        data.setDirty();
        LOGGER.debug("Saved {} suspicious delivery pending runs", data.pendingRuns.size());
    }

    public static void restoreToRunner(MinecraftServer server) {
        if (!(server.overworld() instanceof ServerLevel overworld)) {
            return;
        }
        SuspiciousDeliveryScriptData data = get(overworld);
        SuspiciousDeliveryScriptRunner.importPending(server, List.copyOf(data.pendingRuns));
    }

    public static void killFakeTntAll(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            try {
                var source = server.createCommandSourceStack()
                        .withLevel(level)
                        .withSuppressedOutput();
                server.getCommands().performPrefixedCommand(source, KILL_FAKE_TNT);
            } catch (Exception e) {
                LOGGER.warn("Failed to kill fake_tnt in {}: {}", level.dimension().location(), e.getMessage());
            }
        }
    }

    public static SuspiciousDeliveryScriptData load(CompoundTag tag, HolderLookup.Provider provider) {
        SuspiciousDeliveryScriptData data = new SuspiciousDeliveryScriptData();
        ListTag list = tag.getList("pendingRuns", Tag.TAG_COMPOUND);
        for (Tag element : list) {
            if (!(element instanceof CompoundTag runTag)) {
                continue;
            }
            try {
                UUID actorId = UUID.fromString(runTag.getString("actorId"));
                data.pendingRuns.add(new SuspiciousDeliveryScriptRunner.StoredPendingRun(
                        runTag.getString("dimension"),
                        runTag.getDouble("x"),
                        runTag.getDouble("y"),
                        runTag.getDouble("z"),
                        actorId,
                        runTag.getString("actionsJson"),
                        runTag.getString("stageHostJson"),
                        runTag.getInt("startIndex"),
                        runTag.getLong("executeAtTick")));
            } catch (Exception e) {
                LOGGER.warn("Failed to load suspicious delivery pending run: {}", e.getMessage());
            }
        }
        LOGGER.debug("Loaded {} suspicious delivery pending runs", data.pendingRuns.size());
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (SuspiciousDeliveryScriptRunner.StoredPendingRun run : pendingRuns) {
            CompoundTag runTag = new CompoundTag();
            runTag.putString("dimension", run.dimension());
            runTag.putDouble("x", run.x());
            runTag.putDouble("y", run.y());
            runTag.putDouble("z", run.z());
            runTag.putString("actorId", run.actorId().toString());
            runTag.putString("actionsJson", run.actionsJson());
            runTag.putString("stageHostJson", run.stageHostJson());
            runTag.putInt("startIndex", run.startIndex());
            runTag.putLong("executeAtTick", run.executeAtTick());
            list.add(runTag);
        }
        tag.put("pendingRuns", list);
        return tag;
    }
}
