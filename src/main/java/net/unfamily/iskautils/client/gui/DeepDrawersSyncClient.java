package net.unfamily.iskautils.client.gui;

import net.minecraft.client.Minecraft;
import net.unfamily.iskautils.network.packet.DeepDrawersSyncSlotsS2CPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client-side handler for deep drawer slot sync packets. */
public final class DeepDrawersSyncClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeepDrawersSyncClient.class);

    private DeepDrawersSyncClient() {}

    public static void handle(DeepDrawersSyncSlotsS2CPacket packet) {
        if (packet.isFullSync()) {
            LOGGER.debug("Client: Received DeepDrawersSyncSlotsS2CPacket (full sync). Slots: {}", packet.allSlots().size());
        } else {
            LOGGER.debug("Client: Received DeepDrawersSyncSlotsS2CPacket. Offset: {}, Stacks: {}",
                    packet.scrollOffset(), packet.visibleStacks().size());
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
