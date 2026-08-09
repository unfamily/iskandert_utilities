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

public record EtherealFrameModeToggleC2SPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<EtherealFrameModeToggleC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "ethereal_frame_mode_toggle"));

    public static final StreamCodec<FriendlyByteBuf, EtherealFrameModeToggleC2SPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> BlockPos.STREAM_CODEC.encode(buf, p.pos()),
            buf -> new EtherealFrameModeToggleC2SPacket(BlockPos.STREAM_CODEC.decode(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(EtherealFrameModeToggleC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            BlockEntity be = player.level().getBlockEntity(packet.pos());
            if (be instanceof EtherealFrameBlockEntity frame) {
                frame.toggleAllowMode();
                frame.propagateFilterToNetwork(player.level());
            }
        });
    }
}
