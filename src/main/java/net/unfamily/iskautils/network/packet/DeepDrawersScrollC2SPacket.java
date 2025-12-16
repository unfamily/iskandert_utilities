package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.unfamily.iskautils.block.entity.DeepDrawersBlockEntity;
import net.unfamily.iskautils.client.gui.DeepDrawersMenu;
import net.unfamily.iskautils.network.ModMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet to synchronize the scroll offset of the Deep Drawers GUI
 * Sent from client to server when the player scrolls
 */
public class DeepDrawersScrollC2SPacket {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DeepDrawersScrollC2SPacket.class);
    
    private final BlockPos pos;
    private final int scrollOffset;
    
    public DeepDrawersScrollC2SPacket(BlockPos pos, int scrollOffset) {
        this.pos = pos;
        this.scrollOffset = scrollOffset;
    }
    
    /**
     * Handles the packet on the server
     */
    public void handle(ServerPlayer player) {
        if (player == null || player.level() == null) {
            LOGGER.warn("DeepDrawersScrollC2SPacket.handle: player or level is null");
            return;
        }
        
        // Get the BlockEntity
        var blockEntity = player.level().getBlockEntity(pos);
        if (!(blockEntity instanceof DeepDrawersBlockEntity deepDrawers)) {
            LOGGER.warn("DeepDrawersScrollC2SPacket.handle: BlockEntity is not DeepDrawersBlockEntity at pos {}", pos);
            return;
        }
        
        // Verify the player has the correct menu open
        if (!(player.containerMenu instanceof DeepDrawersMenu menu)) {
            LOGGER.warn("DeepDrawersScrollC2SPacket.handle: Player menu is not DeepDrawersMenu. Menu type: {}", 
                       player.containerMenu != null ? player.containerMenu.getClass().getName() : "null");
            return;
        }
        
        // Update the menu's scroll offset
        // Note: We don't save to BlockEntity - scrollOffset resets to 0 when GUI opens
        int oldOffset = menu.getScrollOffset();
        LOGGER.debug("DeepDrawersScrollC2SPacket.handle: Updating scrollOffset from {} to {}", oldOffset, scrollOffset);
        menu.setScrollOffset(scrollOffset);
        
        // NOTE: We no longer send slot data when scrolling because all slots are sent
        // to the client when the menu opens. The client has all data in memory and
        // can display any slot range instantly without waiting for server response.
    }
}

