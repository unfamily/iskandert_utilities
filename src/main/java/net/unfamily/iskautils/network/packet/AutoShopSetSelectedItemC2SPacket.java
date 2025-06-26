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
     * Gestisce il packet sul server
     */
    public void handle(ServerPlayer player) {
        if (player == null || player.level() == null) return;
        
        // Ottieni la BlockEntity
        var blockEntity = player.level().getBlockEntity(pos);
        if (!(blockEntity instanceof AutoShopBlockEntity autoShop)) return;
        
        // Se non abbiamo uno stack specifico, usa l'item dalla mano del player
        ItemStack itemToSet = this.stack;
        if (itemToSet.isEmpty()) {
            itemToSet = player.getMainHandItem();
            if (itemToSet.isEmpty()) {
                // If hand is empty, clear the slot
                autoShop.setSelectedItem(ItemStack.EMPTY);
            } else {
                // Altrimenti, copia l'item nella slot (1 item alla volta)
                ItemStack copyStack = itemToSet.copy();
                copyStack.setCount(1);
                autoShop.setSelectedItem(copyStack);
            }
        } else {
            // Usa lo stack specificato nel packet
            autoShop.setSelectedItem(itemToSet.copy());
        }
        
        // Marca la BlockEntity come modificata
        autoShop.setChanged();
        
        // Aggiorna il client
        player.level().sendBlockUpdated(pos, blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
    }
} 