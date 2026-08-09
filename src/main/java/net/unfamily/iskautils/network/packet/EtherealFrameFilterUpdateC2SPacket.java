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
import net.unfamily.iskautils.block.entity.EtherealFrameBlockEntity;

import java.util.ArrayList;
import java.util.List;

public record EtherealFrameFilterUpdateC2SPacket(BlockPos pos, List<String> entityTypeIds) implements CustomPacketPayload {

    public static final Type<EtherealFrameFilterUpdateC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "ethereal_frame_filter_update"));

    public static final StreamCodec<FriendlyByteBuf, EtherealFrameFilterUpdateC2SPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                BlockPos.STREAM_CODEC.encode(buf, p.pos());
                buf.writeVarInt(p.entityTypeIds().size());
                for (String s : p.entityTypeIds()) buf.writeUtf(s);
            },
            buf -> {
                BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
                int n = buf.readVarInt();
                List<String> list = new ArrayList<>(n);
                for (int i = 0; i < n; i++) list.add(buf.readUtf());
                return new EtherealFrameFilterUpdateC2SPacket(pos, list);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(EtherealFrameFilterUpdateC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            BlockEntity be = player.level().getBlockEntity(packet.pos());
            if (be instanceof EtherealFrameBlockEntity frame) {
                frame.setFilterEntityTypes(packet.entityTypeIds());
                frame.propagateFilterToNetwork(player.level());
            }
        });
    }
}
