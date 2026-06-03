package net.unfamily.iskautils.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.FlameVisibilityClient;

/** S2C: sync global flame vision flag to the local client. */
public record FlameVisionSyncS2CPacket(boolean enabled) implements CustomPacketPayload {

    public static final Type<FlameVisionSyncS2CPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "flame_vision_sync"));

    public static final StreamCodec<FriendlyByteBuf, FlameVisionSyncS2CPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, FlameVisionSyncS2CPacket::enabled, FlameVisionSyncS2CPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FlameVisionSyncS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> FlameVisibilityClient.applyGlobalFlameVision(packet.enabled()));
    }
}
