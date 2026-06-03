package net.unfamily.iskautils.migration;

import net.neoforged.bus.api.IEventBus;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskalib.migration.worldbackup.WorldBackupGate;
import net.unfamily.iskalib.migration.worldbackup.WorldBackupGateConfig;

/**
 * Registers the one-time 3.6 library-split backup gate for Iskandert's Utilities (1.21.1 only).
 */
public final class UtilsWorldBackupGate {
    public static final String LIB_SPLIT_VERSION = "3.6.0.0.0";

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
    }
}
