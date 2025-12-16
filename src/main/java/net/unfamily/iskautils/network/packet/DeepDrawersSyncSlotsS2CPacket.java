package net.unfamily.iskautils.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.client.gui.DeepDrawersMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Packet to synchronize ALL slot contents from server to client
 * Sent when the player opens the GUI (sends all slots)
 * Can also be used to update specific slots when scrolling (but now we send all slots on open)
 */
public class DeepDrawersSyncSlotsS2CPacket {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DeepDrawersSyncSlotsS2CPacket.class);
    
    private final int scrollOffset; // For backward compatibility, but now we send all slots
    private final List<ItemStack> visibleStacks; // For backward compatibility
    private final Map<Integer, ItemStack> allSlots; // Map of slot index -> ItemStack (only non-empty slots)
    private final boolean isFullSync; // If true, this is a full sync of all slots
    
    // Constructor for full sync (all slots)
    public DeepDrawersSyncSlotsS2CPacket(Map<Integer, ItemStack> allSlots) {
        this.scrollOffset = 0;
        this.visibleStacks = List.of();
        this.allSlots = allSlots;
        this.isFullSync = true;
    }
    
    // Constructor for backward compatibility (visible slots only)
    public DeepDrawersSyncSlotsS2CPacket(int scrollOffset, List<ItemStack> visibleStacks) {
        this.scrollOffset = scrollOffset;
        this.visibleStacks = visibleStacks;
        this.allSlots = Map.of();
        this.isFullSync = false;
    }
    
    /**
     * Handles the packet on the client
     */
    public void handle() {
        if (isFullSync) {
            LOGGER.debug("Client: Received DeepDrawersSyncSlotsS2CPacket (full sync). Slots: {}", allSlots.size());
        } else {
            LOGGER.debug("Client: Received DeepDrawersSyncSlotsS2CPacket. Offset: {}, Stacks: {}", scrollOffset, visibleStacks.size());
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            LOGGER.warn("Client: Minecraft instance is null, cannot handle packet");
            return;
        }
        
        minecraft.execute(() -> {
            if (minecraft.player != null && minecraft.player.containerMenu instanceof DeepDrawersMenu menu) {
                if (isFullSync) {
                    LOGGER.debug("Client: Calling updateAllSlotsFromServer on menu");
                    menu.updateAllSlotsFromServer(allSlots);
                } else {
                    LOGGER.debug("Client: Calling updateViewHandlerFromServer on menu (backward compatibility)");
                    menu.updateViewHandlerFromServer(scrollOffset, visibleStacks);
                }
            } else {
                LOGGER.warn("Client: Player or menu is null. Player: {}, Menu: {}", 
                           minecraft.player != null, 
                           minecraft.player != null && minecraft.player.containerMenu instanceof DeepDrawersMenu);
            }
        });
    }
}

