package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.network.ModMessages;

/**
 * Packet to highlight a linked block in the world (creates a marker for 5 seconds)
 */
public record TemporalOverclockerHighlightBlockC2SPacket(BlockPos blockPos) implements CustomPacketPayload {
    
    public static final Type<TemporalOverclockerHighlightBlockC2SPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "temporal_overclocker_highlight_block"));
    
    public static final StreamCodec<FriendlyByteBuf, TemporalOverclockerHighlightBlockC2SPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        TemporalOverclockerHighlightBlockC2SPacket::blockPos,
        TemporalOverclockerHighlightBlockC2SPacket::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(TemporalOverclockerHighlightBlockC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            handlePacket(packet, player);
        });
    }
    
    private static void handlePacket(TemporalOverclockerHighlightBlockC2SPacket packet, ServerPlayer player) {
        // Create marker at block position (5 seconds = 100 ticks)
        int color = (0x80 << 24) | 0x00FF00; // Semi-transparent green
        int durationTicks = 100; // 5 seconds
        ModMessages.sendAddBillboardPacket(player, packet.blockPos(), color, durationTicks);
    }
}

