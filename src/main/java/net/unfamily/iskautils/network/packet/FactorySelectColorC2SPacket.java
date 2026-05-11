package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.FactoryBlockEntity;

public record FactorySelectColorC2SPacket(BlockPos pos, int index) implements CustomPacketPayload {
    public static final Type<FactorySelectColorC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "factory_select_color")
    );

    public static final StreamCodec<FriendlyByteBuf, FactorySelectColorC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            FactorySelectColorC2SPacket::pos,
            net.minecraft.network.codec.ByteBufCodecs.INT,
            FactorySelectColorC2SPacket::index,
            FactorySelectColorC2SPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FactorySelectColorC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = player.serverLevel();
            BlockEntity be = level.getBlockEntity(packet.pos());
            if (be instanceof FactoryBlockEntity factory) {
                factory.setSelectedColorIndex(packet.index());
            }
        });
    }
}
