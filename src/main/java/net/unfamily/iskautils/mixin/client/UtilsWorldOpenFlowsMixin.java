package net.unfamily.iskautils.mixin.client;

import com.mojang.serialization.Dynamic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.unfamily.iskalib.client.migration.WorldBackupGateClient;
import net.unfamily.iskalib.migration.worldbackup.WorldBackupGate;
import net.unfamily.iskalib.migration.worldbackup.WorldBackupGateConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks world selection before load (vanilla {@code BackupConfirmScreen} flow). Lives in utils because the
 * host mod has full client compile classpath; calls into iska_lib.
 */
@Mixin(WorldOpenFlows.class)
public abstract class UtilsWorldOpenFlowsMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(
            method = "openWorldCheckVersionCompatibility",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    remap = false,
                    target = "Lnet/minecraft/client/gui/screens/worldselection/WorldOpenFlows;openWorldLoadLevelStem(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;Lcom/mojang/serialization/Dynamic;ZLjava/lang/Runnable;)V"),
            cancellable = true)
    private void iskaUtils$promptModBackupBeforeLoad(
            LevelStorageSource.LevelStorageAccess access,
            LevelSummary summary,
            Dynamic<?> dynamic,
            Runnable onFail,
            CallbackInfo ci) {
        if (!WorldBackupGate.isEnabled()) {
            return;
        }
        WorldBackupGateConfig config = WorldBackupGate.findPendingConfig(access);
        if (config == null) {
            return;
        }

        ci.cancel();
        Runnable resume = () -> ((UtilsWorldOpenFlowsInvoker) this)
                .invokeOpenWorldLoadLevelStem(access, dynamic, false, onFail);

        WorldBackupGateClient.showPreWorldLoadBackupScreen(
                minecraft,
                access,
                config,
                resume,
                () -> {
                    access.safeClose();
                    onFail.run();
                });
    }
}
