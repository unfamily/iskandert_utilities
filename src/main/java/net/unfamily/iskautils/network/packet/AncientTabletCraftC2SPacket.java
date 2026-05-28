package net.unfamily.iskautils.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.events.AncientTabletCraftHandler;

public record AncientTabletCraftC2SPacket() implements CustomPacketPayload {

    public static final Type<AncientTabletCraftC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "ancient_tablet_craft"));

    public static final StreamCodec<FriendlyByteBuf, AncientTabletCraftC2SPacket> STREAM_CODEC =
            StreamCodec.unit(new AncientTabletCraftC2SPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AncientTabletCraftC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                AncientTabletCraftHandler.tryCraftFromServer(sp);
            }
        });
    }
}
