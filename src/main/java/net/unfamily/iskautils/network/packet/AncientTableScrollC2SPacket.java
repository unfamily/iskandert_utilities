package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.AncientTableBlockEntity;

public record AncientTableScrollC2SPacket(BlockPos pos, int side, int offset) implements CustomPacketPayload {
    public static final int SIDE_INPUT = 0;
    public static final int SIDE_OUTPUT = 1;

    public static final Type<AncientTableScrollC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "ancient_table_scroll"));

    public static final StreamCodec<FriendlyByteBuf, AncientTableScrollC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            AncientTableScrollC2SPacket::pos,
            ByteBufCodecs.INT,
            AncientTableScrollC2SPacket::side,
            ByteBufCodecs.INT,
            AncientTableScrollC2SPacket::offset,
            AncientTableScrollC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AncientTableScrollC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = player.serverLevel();
            BlockEntity be = level.getBlockEntity(packet.pos());
            if (be instanceof AncientTableBlockEntity table) {
                if (packet.side() == SIDE_OUTPUT) {
                    table.setOutputScrollOffset(packet.offset());
                } else {
                    table.setInputScrollOffset(packet.offset());
                }
            }
        });
    }
}
