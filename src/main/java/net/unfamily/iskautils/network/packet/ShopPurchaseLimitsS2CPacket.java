package net.unfamily.iskautils.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.gui.ShopClientPurchaseLimits;
import net.unfamily.iskautils.shop.ShopEntry;
import net.unfamily.iskautils.shop.ShopLoader;
import net.unfamily.iskautils.shop.ShopPurchaseLimitsData;
import net.unfamily.iskautils.shop.ShopRepeatableRule;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Server-evaluated buy/sell limit state for the current shop player. */
public record ShopPurchaseLimitsS2CPacket(Map<String, Long> blocked) implements CustomPacketPayload {
    public static final Type<ShopPurchaseLimitsS2CPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "shop_purchase_limits"));

    public static final StreamCodec<FriendlyByteBuf, ShopPurchaseLimitsS2CPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.blocked.size());
                packet.blocked.forEach((key, reset) -> {
                    buf.writeUtf(key, 512);
                    buf.writeLong(reset);
                });
            },
            buf -> {
                int size = buf.readVarInt();
                Map<String, Long> blocked = new HashMap<>();
                for (int i = 0; i < size; i++) {
                    blocked.put(buf.readUtf(512), buf.readLong());
                }
                return new ShopPurchaseLimitsS2CPacket(blocked);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void sendTo(ServerPlayer player) {
        ShopPurchaseLimitsData data = ShopPurchaseLimitsData.get(((net.minecraft.server.level.ServerLevel) player.level()));
        Map<String, Long> blocked = new HashMap<>();
        for (ShopEntry entry : ShopLoader.getEntries().values()) {
            for (ShopPurchaseLimitsData.TradeSide side : ShopPurchaseLimitsData.TradeSide.values()) {
                if (!data.canTrade(player, entry, side, 1)) {
                    ShopRepeatableRule rule = ShopPurchaseLimitsData.effective(entry, side);
                    Instant reset = ShopPurchaseLimitsData.nextResetInstant(rule);
                    blocked.put(ShopClientPurchaseLimits.key(entry.id, side),
                            reset != null ? reset.toEpochMilli() : -1L);
                }
            }
        }
        PacketDistributor.sendToPlayer(player, new ShopPurchaseLimitsS2CPacket(blocked));
    }

    public static void handle(ShopPurchaseLimitsS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ShopClientPurchaseLimits.replace(packet.blocked));
    }
}
