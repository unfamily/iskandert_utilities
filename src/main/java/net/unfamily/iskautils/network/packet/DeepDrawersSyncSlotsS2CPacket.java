package net.unfamily.iskautils.network.packet;

import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.util.DeepDrawersSyncClientAccess;

import java.util.List;
import java.util.Map;

/**
 * Packet to synchronize ALL slot contents from server to client.
 */
public class DeepDrawersSyncSlotsS2CPacket {

    private final int scrollOffset;
    private final List<ItemStack> visibleStacks;
    private final Map<Integer, ItemStack> allSlots;
    private final boolean isFullSync;

    public DeepDrawersSyncSlotsS2CPacket(Map<Integer, ItemStack> allSlots) {
        this.scrollOffset = 0;
        this.visibleStacks = List.of();
        this.allSlots = allSlots;
        this.isFullSync = true;
    }

    public DeepDrawersSyncSlotsS2CPacket(int scrollOffset, List<ItemStack> visibleStacks) {
        this.scrollOffset = scrollOffset;
        this.visibleStacks = visibleStacks;
        this.allSlots = Map.of();
        this.isFullSync = false;
    }

    public boolean isFullSync() {
        return isFullSync;
    }

    public int scrollOffset() {
        return scrollOffset;
    }

    public List<ItemStack> visibleStacks() {
        return visibleStacks;
    }

    public Map<Integer, ItemStack> allSlots() {
        return allSlots;
    }

    public void handle() {
        DeepDrawersSyncClientAccess.handle(this);
    }
}
