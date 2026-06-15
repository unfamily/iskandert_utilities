package net.unfamily.iskautils.client.gui;

import net.unfamily.iskautils.util.ModLogger;

import net.minecraft.client.Minecraft;
import net.unfamily.iskautils.network.packet.DeepDrawersSyncSlotsS2CPacket;

/** Client-side handler for deep drawer slot sync packets. */
public final class DeepDrawersSyncClient {
    private static final ModLogger LOGGER = ModLogger.of(DeepDrawersSyncClient.class);

    private DeepDrawersSyncClient() {}

    public static void handle(DeepDrawersSyncSlotsS2CPacket packet) {
        if (packet.isFullSync()) {
        } else {
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            LOGGER.warn("Client: Minecraft instance is null, cannot handle packet");
            return;
        }

        minecraft.execute(() -> {
            if (minecraft.player != null && minecraft.player.containerMenu instanceof DeepDrawersMenu menu) {
                if (packet.isFullSync()) {
                    menu.updateAllSlotsFromServer(packet.allSlots());
                } else {
                    menu.updateViewHandlerFromServer(packet.scrollOffset(), packet.visibleStacks());
                }
            } else {
                LOGGER.warn("Client: Player or DeepDrawersMenu unavailable for sync packet");
            }
        });
    }
}
