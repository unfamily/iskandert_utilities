package net.unfamily.iskautils.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskalib.stage.StageRegistry;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.gui.ShopClientStages;
import net.unfamily.iskautils.shop.ShopCategory;
import net.unfamily.iskautils.shop.ShopEntry;
import net.unfamily.iskautils.shop.ShopLoader;
import net.unfamily.iskautils.shop.ShopStage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Server → client: map of stage key ("type:id") → hasStage boolean for all stages referenced
 * by shop entries and categories. Sent when shop opens / on request (A2/A6).
 */
public record ShopStagesS2CPacket(Map<String, Boolean> stageCache) implements CustomPacketPayload {

    public static final Type<ShopStagesS2CPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "shop_stages"));

    public static final StreamCodec<FriendlyByteBuf, ShopStagesS2CPacket> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeVarInt(pkt.stageCache.size());
                pkt.stageCache.forEach((key, val) -> {
                    buf.writeUtf(key, 512);
                    buf.writeBoolean(val);
                });
            },
            buf -> {
                int size = buf.readVarInt();
                Map<String, Boolean> map = new HashMap<>();
                for (int i = 0; i < size; i++) {
                    map.put(buf.readUtf(512), buf.readBoolean());
                }
                return new ShopStagesS2CPacket(map);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Collect all unique stage keys referenced by entries and categories in the shop. */
    private static Set<ShopStage> collectAllStages() {
        Set<ShopStage> all = new HashSet<>();
        for (ShopEntry entry : ShopLoader.getEntries().values()) {
            if (entry.stages != null) {
                for (ShopStage s : entry.stages) {
                    if (s != null) all.add(s);
                }
            }
        }
        for (ShopCategory cat : ShopLoader.getCategories().values()) {
            if (cat.stages != null) {
                for (ShopStage s : cat.stages) {
                    if (s != null) all.add(s);
                }
            }
        }
        return all;
    }

    public static void sendTo(ServerPlayer player) {
        StageRegistry registry = StageRegistry.getInstance(player.getServer());
        Map<String, Boolean> stageCache = new HashMap<>();
        for (ShopStage stage : collectAllStages()) {
            if (stage.stageType == null) continue;
            boolean has = switch (stage.stageType.toLowerCase()) {
                case "player" -> registry.hasPlayerStage(player, stage.stage);
                case "world" -> registry.hasWorldStage(stage.stage);
                case "team" -> registry.hasPlayerTeamStage(player, stage.stage);
                default -> false;
            };
            stageCache.put(ShopClientStages.stageKey(stage.stageType, stage.stage), has);
        }
        PacketDistributor.sendToPlayer(player, new ShopStagesS2CPacket(stageCache));
    }

    public static void handle(ShopStagesS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ShopClientStages.replaceCache(packet.stageCache()));
    }
}
