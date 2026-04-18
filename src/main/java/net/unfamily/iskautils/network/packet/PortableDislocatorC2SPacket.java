package net.unfamily.iskautils.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.item.custom.PortableDislocatorItem;

/**
 * Client-to-server: player requested Portable Dislocator teleport to target X/Z (from compass).
 */
public record PortableDislocatorC2SPacket(int targetX, int targetZ) implements CustomPacketPayload {

    public static final Type<PortableDislocatorC2SPacket> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "portable_dislocator"));

    public static final StreamCodec<FriendlyByteBuf, PortableDislocatorC2SPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        PortableDislocatorC2SPacket::targetX,
        ByteBufCodecs.VAR_INT,
        PortableDislocatorC2SPacket::targetZ,
        PortableDislocatorC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PortableDislocatorC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                PortableDislocatorItem.startTeleportationFromNetwork(serverPlayer, packet.targetX(), packet.targetZ());
            }
        });
    }
}
