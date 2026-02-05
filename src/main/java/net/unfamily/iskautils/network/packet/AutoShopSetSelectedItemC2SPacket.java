package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.block.entity.AutoShopBlockEntity;

/**
 * Packet per impostare l'item nello slot selectedItem dell'Auto Shop
 * Versione semplificata per compatibilità single player
 */
public class AutoShopSetSelectedItemC2SPacket {
    
    private final BlockPos pos;
    private final ItemStack stack;
    
    public AutoShopSetSelectedItemC2SPacket(BlockPos pos, ItemStack stack) {
        this.pos = pos;
        this.stack = stack;
    }
    
    public AutoShopSetSelectedItemC2SPacket(BlockPos pos) {
        this.pos = pos;
        this.stack = ItemStack.EMPTY;
    }
    
    /**
     * Handles the packet on the server.
     * If stack is empty: clears the filter (ghost slot). Otherwise sets the filter to a copy of the stack.
     */
    public void handle(ServerPlayer player) {
        if (player == null || player.level() == null) return;
        
        var blockEntity = player.level().getBlockEntity(pos);
        if (!(blockEntity instanceof AutoShopBlockEntity autoShop)) return;
        
        if (stack.isEmpty()) {
            autoShop.setSelectedItem(ItemStack.EMPTY);
        } else {
            ItemStack copyStack = stack.copy();
            copyStack.setCount(1);
            autoShop.setSelectedItem(copyStack);
        }
        
        autoShop.setChanged();
        player.level().sendBlockUpdated(pos, blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
        
        // Refresh open menu so client sees the updated ghost slot
        if (player.containerMenu instanceof net.unfamily.iskautils.client.gui.AutoShopMenu autoMenu
                && autoMenu.getBlockPos().equals(pos)) {
            autoMenu.broadcastFullState();
        }
    }
} 