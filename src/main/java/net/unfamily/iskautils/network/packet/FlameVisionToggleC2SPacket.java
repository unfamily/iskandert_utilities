package net.unfamily.iskautils.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.data.FlameVisionData;

/** C2S: toggle global flame vision for the sending player. */
public record FlameVisionToggleC2SPacket(boolean enabled) implements CustomPacketPayload {

    public static final Type<FlameVisionToggleC2SPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "flame_vision_toggle"));

    public static final StreamCodec<FriendlyByteBuf, FlameVisionToggleC2SPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, FlameVisionToggleC2SPacket::enabled, FlameVisionToggleC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FlameVisionToggleC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            FlameVisionData.setFlameVisionEnabledForPlayer(player, packet.enabled());
            PacketDistributor.sendToPlayer(player, new FlameVisionSyncS2CPacket(packet.enabled()));
        });
    }
}
