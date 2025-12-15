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
        if (player == null || player.level() == null) return;
        
        // Get the BlockEntity
        var blockEntity = player.level().getBlockEntity(pos);
        if (!(blockEntity instanceof DeepDrawersBlockEntity deepDrawers)) return;
        
        // Verify the player has the correct menu open
        if (!(player.containerMenu instanceof DeepDrawersMenu menu)) return;
        
        // Update the scroll offset in the menu
        // This will update the viewHandler on the server
        menu.setScrollOffset(scrollOffset);
        
        // Get the visible slots content from the item handler
        IItemHandler itemHandler = deepDrawers.getItemHandler();
        List<ItemStack> visibleStacks = new ArrayList<>();
        
        for (int i = 0; i < DeepDrawersMenu.VISIBLE_SLOTS; i++) {
            int actualSlot = scrollOffset + i;
            ItemStack stack = itemHandler.getStackInSlot(actualSlot);
            visibleStacks.add(stack.copy());
        }
        
        // Send the updated slots back to the client
        ModMessages.sendToPlayer(new DeepDrawersSyncSlotsS2CPacket(scrollOffset, visibleStacks), player);
        
        LOGGER.debug("Scroll packet handled: offset={}, sent {} stacks to client", scrollOffset, visibleStacks.size());
    }
}

