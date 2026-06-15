package net.unfamily.iskautils.data.load;

import net.unfamily.iskautils.util.ModLogger;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.unfamily.iskautils.command.ShopCommand;

/**
 * Shared side effects for IskaUtils {@code load/**} reload.
 * Vanilla {@code /reload} uses {@link #applyReloadFromDatapacks()} (no chat notice).
 * Mod commands use {@link #applyReloadFromDatapacks(CommandSourceStack)} to show a notice to the executor.
 */
public final class IskaUtilsLoadReloadEffects {

    private static final ModLogger LOGGER = ModLogger.of(IskaUtilsLoadReloadEffects.class);

    private IskaUtilsLoadReloadEffects() {}

    /** Full load/** reload after vanilla datapack reload has finished. */
    public static void applyReloadFromDatapacks() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            LOGGER.warn("applyReloadFromDatapacks with no server; running synchronous reload");
            IskaUtilsDataReload.reloadAllFromServer();
            notifyShopClientsAfterReload();
            return;
        }
        IskaUtilsPhasedReloadScheduler.schedule(server, IskaUtilsLoadReloadEffects::notifyShopClientsAfterReload);
    }

    /** Same as {@link #applyReloadFromDatapacks()} plus a notice to the command executor. */
    public static void applyReloadFromDatapacks(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        if (server == null) {
            applyReloadFromDatapacks();
            sendReloadNotice(source);
            return;
        }
        IskaUtilsPhasedReloadScheduler.schedule(server, () -> {
            notifyShopClientsAfterReload();
            sendReloadNotice(source);
        });
    }

    static void notifyShopClientsAfterReload() {
        try {
            ShopCommand.notifyClientGUIReload();
        } catch (Exception e) {
            LOGGER.error("Error notifying shop GUI reload: {}", e.getMessage());
        }
    }

    /** Notice for mod-initiated reload commands (debug/shop/structure, etc.). */
    public static void sendReloadNotice(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.iska_utils.reload.notice"), false);
    }
}
