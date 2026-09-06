package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.ImprovedPatternCrafterBlockEntity;

/**
 * Client-to-server: cycles remainder routing mode (1–2).
 */
public record RemainderRoutingModeC2SPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<RemainderRoutingModeC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "pc_remainder_routing_mode")
    );

    public static final StreamCodec<FriendlyByteBuf, RemainderRoutingModeC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RemainderRoutingModeC2SPacket::pos,
            RemainderRoutingModeC2SPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RemainderRoutingModeC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            BlockEntity be = ((net.minecraft.server.level.ServerLevel) player.level()).getBlockEntity(packet.pos());
            if (be instanceof ImprovedPatternCrafterBlockEntity pcbe) {
                pcbe.cycleRemainderRoutingMode();
            }
        });
    }
}
