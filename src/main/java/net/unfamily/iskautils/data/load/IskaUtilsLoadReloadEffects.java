package net.unfamily.iskautils.data.load;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.unfamily.iskautils.command.ShopCommand;
import net.unfamily.iskautils.network.ModMessages;
import org.slf4j.Logger;

/**
 * Shared side effects for IskaUtils {@code load/**} reload.
 * Vanilla {@code /reload} uses {@link #applyReloadFromDatapacks()} (no chat notice).
 * Mod commands use {@link #applyReloadFromDatapacks(CommandSourceStack)} to show a notice to the executor.
 */
public final class IskaUtilsLoadReloadEffects {

    private static final Logger LOGGER = LogUtils.getLogger();

    private IskaUtilsLoadReloadEffects() {}

    /** Full load/** reload after vanilla datapack reload has finished. */
    public static void applyReloadFromDatapacks() {
        LOGGER.info("Applying IskaUtils load/** reload from server ResourceManager");
        IskaUtilsDataReload.reloadAllFromServer();
        try {
            ShopCommand.notifyClientGUIReload();
        } catch (Exception e) {
            LOGGER.error("Error notifying shop GUI reload: {}", e.getMessage());
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            try {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    ModMessages.sendStructureSyncPacket(player);
                }
            } catch (Exception e) {
                LOGGER.error("Error syncing structures after reload: {}", e.getMessage());
            }
        }
    }

    /** Same as {@link #applyReloadFromDatapacks()} plus a notice to the command executor. */
    public static void applyReloadFromDatapacks(CommandSourceStack source) {
        applyReloadFromDatapacks();
        sendReloadNotice(source);
    }

    /** Notice for mod-initiated reload commands (debug/shop/structure, etc.). */
    public static void sendReloadNotice(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.iska_utils.reload.notice"), false);
    }
}
