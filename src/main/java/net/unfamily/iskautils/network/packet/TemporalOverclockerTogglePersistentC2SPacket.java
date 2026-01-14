package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.TemporalOverclockerBlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Packet to toggle persistent mode in Temporal Overclocker
 */
public record TemporalOverclockerTogglePersistentC2SPacket(BlockPos overclockerPos) implements CustomPacketPayload {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TemporalOverclockerTogglePersistentC2SPacket.class);
    
    public static final Type<TemporalOverclockerTogglePersistentC2SPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "temporal_overclocker_toggle_persistent"));
    
    public static final StreamCodec<FriendlyByteBuf, TemporalOverclockerTogglePersistentC2SPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        TemporalOverclockerTogglePersistentC2SPacket::overclockerPos,
        TemporalOverclockerTogglePersistentC2SPacket::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(TemporalOverclockerTogglePersistentC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            handlePacket(packet, player);
        });
    }
    
    private static void handlePacket(TemporalOverclockerTogglePersistentC2SPacket packet, ServerPlayer player) {
        if (player.level().getBlockEntity(packet.overclockerPos()) instanceof TemporalOverclockerBlockEntity blockEntity) {
            blockEntity.togglePersistentMode();
            LOGGER.debug("Toggled persistent mode for Temporal Overclocker at {} - New state: {}", 
                packet.overclockerPos(), blockEntity.isPersistentMode());
        } else {
            LOGGER.warn("Temporal Overclocker BlockEntity not found at {}", packet.overclockerPos());
        }
    }
}
