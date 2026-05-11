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
import net.unfamily.iskautils.block.entity.FactoryBlockEntity;

public record FactoryScrollC2SPacket(BlockPos pos, int offset) implements CustomPacketPayload {
    public static final Type<FactoryScrollC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "factory_scroll"));

    public static final StreamCodec<FriendlyByteBuf, FactoryScrollC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            FactoryScrollC2SPacket::pos,
            net.minecraft.network.codec.ByteBufCodecs.INT,
            FactoryScrollC2SPacket::offset,
            FactoryScrollC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FactoryScrollC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            BlockEntity be = player.level().getBlockEntity(packet.pos());
            if (be instanceof FactoryBlockEntity factory) {
                factory.setScrollOffset(packet.offset());
            }
        });
    }
}
