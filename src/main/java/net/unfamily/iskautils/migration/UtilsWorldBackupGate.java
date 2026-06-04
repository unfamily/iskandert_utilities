package net.unfamily.iskautils.migration;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskalib.migration.worldbackup.WorldBackupGate;
import net.unfamily.iskalib.migration.worldbackup.WorldBackupGateConfig;
import net.unfamily.iskalib.migration.worldbackup.WorldBackupGateStorage;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * One-time 3.6 library-split migration for Iskandert's Utilities (1.21.1 only).
 *
 * <p>There is intentionally NO client-side prompt / world-open mixin: any pre-load or in-world
 * confirmation can be hidden behind FancyMenu / level loading screens and leave world loading stuck.
 * The gate is acknowledged silently on the server after start; no UI, no blocking work on the
 * world-open or first-tick critical path.
 */
public final class UtilsWorldBackupGate {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String LIB_SPLIT_VERSION = "3.6.0.0.0";
    public static final String REGISTRY_KEY = IskaUtils.MOD_ID + "_lib_split";

    private UtilsWorldBackupGate() {
    }

    public static void register(IEventBus modEventBus) {
        WorldBackupGate.install(modEventBus, WorldBackupGateConfig.builder(IskaUtils.MOD_ID)
                .gateId("lib_split")
                .migrationVersionLabel(LIB_SPLIT_VERSION)
                .legacyWorldDataFileNames(
                        "iska_utils_world_stages.dat",
                        "iska_utils_team_stages.dat",
                        "iska_utils_shop_teams.dat",
                        "iska_utils_burning_brazier_data.dat",
                        "iska_utils_ghost_brazier_data.dat",
                        "iska_utils_flame_vision_data.dat",
                        "iska_utils_vector_charm_data.dat")
                .translationPrefix("message.iska_utils.lib_split_backup")
                .legacyAckSavedData("iska_utils_mod_metadata", "lib_split_backup_ack")
                .build());
        NeoForge.EVENT_BUS.addListener(UtilsWorldBackupGate::onServerStarted);
    }

    public static void onServerStarted(ServerStartedEvent event) {
        WorldBackupGateConfig config = WorldBackupGate.getConfig(REGISTRY_KEY);
        if (config == null) {
            return;
        }
        try {
            Path dataDir = event.getServer().getWorldPath(LevelResource.ROOT).resolve("data");
            if (!WorldBackupGateStorage.hasLegacyWorldData(dataDir, config)) {
                return;
            }
            if (!WorldBackupGateStorage.isAcknowledged(dataDir, config)) {
                WorldBackupGate.acknowledgeOnDisk(dataDir, config);
                LOGGER.warn("Auto-acknowledged lib_split migration (legacy IskaUtils .dat files present under {}). "
                        + "No backup prompt is shown; back up the save manually if you have not already.", dataDir);
            }
            ServerLevel overworld = event.getServer().overworld();
            if (overworld != null) {
                WorldBackupGate.syncAckFromDisk(overworld, config);
            }
        } catch (Exception e) {
            LOGGER.error("lib_split migration ack failed: {}", e.toString());
        }
    }
}
