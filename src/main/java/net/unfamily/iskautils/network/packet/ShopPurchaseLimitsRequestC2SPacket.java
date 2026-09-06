package net.unfamily.iskautils.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;

/** Requests the current player's buy/sell limit state. */
public record ShopPurchaseLimitsRequestC2SPacket() implements CustomPacketPayload {
    public static final Type<ShopPurchaseLimitsRequestC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "shop_purchase_limits_request"));
    public static final StreamCodec<FriendlyByteBuf, ShopPurchaseLimitsRequestC2SPacket> STREAM_CODEC =
            StreamCodec.unit(new ShopPurchaseLimitsRequestC2SPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ShopPurchaseLimitsRequestC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ShopPurchaseLimitsS2CPacket.sendTo(player);
            }
        });
    }
}
