package net.unfamily.iskautils.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.client.gui.DeepDrawersMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Packet to synchronize visible slot contents from server to client
 * Sent when the player scrolls or opens the GUI
 */
public class DeepDrawersSyncSlotsS2CPacket {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DeepDrawersSyncSlotsS2CPacket.class);
    
    private final int scrollOffset;
    private final List<ItemStack> visibleStacks;
    
    public DeepDrawersSyncSlotsS2CPacket(int scrollOffset, List<ItemStack> visibleStacks) {
        this.scrollOffset = scrollOffset;
        this.visibleStacks = visibleStacks;
    }
    
    /**
     * Handles the packet on the client
     */
    public void handle() {
        LOGGER.debug("Client: Received DeepDrawersSyncSlotsS2CPacket. Offset: {}, Stacks: {}", scrollOffset, visibleStacks.size());
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            LOGGER.warn("Client: Minecraft instance is null, cannot handle packet");
            return;
        }
        
        minecraft.execute(() -> {
            if (minecraft.player != null && minecraft.player.containerMenu instanceof DeepDrawersMenu menu) {
                LOGGER.debug("Client: Calling updateViewHandlerFromServer on menu");
                menu.updateViewHandlerFromServer(scrollOffset, visibleStacks);
            } else {
                LOGGER.warn("Client: Player or menu is null. Player: {}, Menu: {}", 
                           minecraft.player != null, 
                           minecraft.player != null && minecraft.player.containerMenu instanceof DeepDrawersMenu);
            }
        });
    }
}

