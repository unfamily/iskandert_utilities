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

import java.util.HashMap;
import java.util.Map;

/**
 * Server → client: used/max/resetEpochMs for each entry|side that has a non-always rule.
 * A9: send for ALL such entries, not only blocked ones, so tooltips can show Y/X.
 */
public record ShopPurchaseLimitsS2CPacket(Map<String, long[]> limits) implements CustomPacketPayload {
    public static final Type<ShopPurchaseLimitsS2CPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "shop_purchase_limits"));

    public static final StreamCodec<FriendlyByteBuf, ShopPurchaseLimitsS2CPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.limits.size());
                packet.limits.forEach((key, arr) -> {
                    buf.writeUtf(key, 512);
                    buf.writeVarInt((int) arr[0]); // used
                    buf.writeVarInt((int) arr[1]); // max
                    buf.writeLong(arr[2]);          // resetEpochMs
                });
            },
            buf -> {
                int size = buf.readVarInt();
                Map<String, long[]> limits = new HashMap<>();
                for (int i = 0; i < size; i++) {
                    String key = buf.readUtf(512);
                    int used = buf.readVarInt();
                    int max = buf.readVarInt();
                    long reset = buf.readLong();
                    limits.put(key, new long[]{used, max, reset});
                }
                return new ShopPurchaseLimitsS2CPacket(limits);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void sendTo(ServerPlayer player) {
        net.minecraft.server.level.ServerLevel sl = (net.minecraft.server.level.ServerLevel) player.level();
        ShopPurchaseLimitsData data = ShopPurchaseLimitsData.get(sl);
        net.unfamily.iskalib.team.ShopTeamManager tm =
                net.unfamily.iskalib.team.ShopTeamManager.getInstance(sl);
        Map<String, long[]> limits = new HashMap<>();
        for (ShopEntry entry : ShopLoader.getEntries().values()) {
            for (ShopPurchaseLimitsData.TradeSide side : ShopPurchaseLimitsData.TradeSide.values()) {
                ShopRepeatableRule rule = ShopPurchaseLimitsData.effective(entry, side);
                if (ShopRepeatableRule.WHEN_ALWAYS.equalsIgnoreCase(rule.when)) {
                    continue; // always-allowed rules don't need tracking
                }
                ShopPurchaseLimitsData.LimitInfo info = data.getLimitInfo(
                        sl, player.getUUID(),
                        tm.getPlayerTeam(player), entry, side);
                if (info == null) continue;
                String key = ShopClientPurchaseLimits.key(entry.id, side);
                limits.put(key, new long[]{info.used(), info.max(), info.resetEpochMs()});
            }
        }
        PacketDistributor.sendToPlayer(player, new ShopPurchaseLimitsS2CPacket(limits));
    }

    public static void handle(ShopPurchaseLimitsS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ShopClientPurchaseLimits.replace(packet.limits()));
    }
}
