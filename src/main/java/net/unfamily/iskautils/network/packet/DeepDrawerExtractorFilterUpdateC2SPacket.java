package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.block.entity.DeepDrawerExtractorBlockEntity;

/**
 * Packet to update filter configuration from client to server
 * Note: Currently not used - ModMessages handles this directly, but kept for potential future use
 */
public class DeepDrawerExtractorFilterUpdateC2SPacket {
    
    private final BlockPos pos;
    private final java.util.List<String> filterFields;
    private final boolean isWhitelistMode;
    
    public DeepDrawerExtractorFilterUpdateC2SPacket(BlockPos pos, java.util.List<String> filterFields, boolean isWhitelistMode) {
        this.pos = pos;
        this.filterFields = filterFields != null ? new java.util.ArrayList<>(filterFields) : new java.util.ArrayList<>();
        this.isWhitelistMode = isWhitelistMode;
    }
    
    /**
     * Handles the packet on server
     */
    public void handle(ServerPlayer player) {
        if (player == null || player.level() == null) return;
        
        // Get the BlockEntity
        var blockEntity = player.level().getBlockEntity(pos);
        if (!(blockEntity instanceof DeepDrawerExtractorBlockEntity extractor)) return;
        
        // Update filter fields
        if (filterFields != null) {
            extractor.setFilterFields(filterFields);
        }
        
        // Update mode
        extractor.setWhitelistMode(isWhitelistMode);
        
        // Mark BlockEntity as changed
        extractor.setChanged();
        
        // Update client
        player.level().sendBlockUpdated(pos, blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
    }
}
