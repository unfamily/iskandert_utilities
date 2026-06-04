package net.unfamily.iskautils.mixin.client;

import com.mojang.serialization.Dynamic;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.unfamily.iskalib.migration.worldbackup.WorldBackupGate;
import net.unfamily.iskalib.migration.worldbackup.WorldBackupGateConfig;
import net.unfamily.iskalib.migration.worldbackup.WorldBackupGateStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * One-time lib_split migration: write backup-gate ack on disk and continue loading without
 * {@link net.minecraft.client.gui.screens.BackupConfirmScreen}. That screen is often hidden behind
 * FancyMenu / level loading UIs and leaves the game stuck at 100% spawn with no IskaUtils logs.
 */
@Mixin(WorldOpenFlows.class)
public abstract class UtilsWorldOpenFlowsMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger(UtilsWorldOpenFlowsMixin.class);

    @Inject(
            method = "openWorldCheckVersionCompatibility",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    remap = false,
                    target = "Lnet/minecraft/client/gui/screens/worldselection/WorldOpenFlows;openWorldLoadLevelStem(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;Lcom/mojang/serialization/Dynamic;ZLjava/lang/Runnable;)V"))
    private void iskaUtils$ackLibSplitMigrationWithoutBlockingUi(
            LevelStorageSource.LevelStorageAccess access,
            LevelSummary summary,
            Dynamic<?> dynamic,
            Runnable onFail,
            CallbackInfo ci) {
        if (!WorldBackupGate.isEnabled() || !summary.isCompatible()) {
            return;
        }
        WorldBackupGateConfig config = WorldBackupGate.findPendingConfig(access);
        if (config == null) {
            return;
        }
        if (!WorldBackupGateStorage.requiresBackupPrompt(WorldBackupGateStorage.worldDataDir(access), config)) {
            return;
        }

        WorldBackupGate.acknowledgeOnDisk(access, config);
        LOGGER.warn(
                "Legacy IskaUtils world data detected (pre-{}). Skipping backup dialog and continuing load — "
                        + "back up the save folder manually if you have not already.",
                config.migrationVersionLabel());
        // Do not cancel: vanilla openWorldLoadLevelStem runs on the same path (no hidden BackupConfirmScreen).
    }
}
