package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.DeepDrawerExtractorBlockEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-to-server: apply normal filter map, concat map, and whitelist mode on the Deep Drawer Extractor.
 */
public record DeepDrawerExtractorFilterUpdateC2SPacket(
        BlockPos pos, Map<Integer, String> filterMap, Map<Integer, Integer> concatMap, boolean whitelistMode)
        implements CustomPacketPayload {

    private static final int MAX_ENTRIES = 512;
    private static final int MAX_STRING_LEN = 512;

    public static final Type<DeepDrawerExtractorFilterUpdateC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "deep_drawer_extractor_filter_update"));

    public static final StreamCodec<FriendlyByteBuf, DeepDrawerExtractorFilterUpdateC2SPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                BlockPos.STREAM_CODEC.encode(buf, p.pos());
                buf.writeBoolean(p.whitelistMode());
                writeStringMap(buf, p.filterMap());
                writeIntMap(buf, p.concatMap());
            },
            buf -> {
                BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
                boolean whitelist = buf.readBoolean();
                Map<Integer, String> map = readStringMap(buf);
                Map<Integer, Integer> concat = readIntMap(buf);
                return new DeepDrawerExtractorFilterUpdateC2SPacket(pos, map, concat, whitelist);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeepDrawerExtractorFilterUpdateC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            BlockEntity be = player.level().getBlockEntity(packet.pos());
            if (!(be instanceof DeepDrawerExtractorBlockEntity extractor)) {
                return;
            }
            if (!extractor.stillValid(player)) {
                return;
            }
            extractor.setFilterFieldsFromMap(packet.filterMap());
            extractor.setAllowConcatFromMap(packet.concatMap());
            extractor.setWhitelistMode(packet.whitelistMode());
        });
    }

    static void writeStringMap(FriendlyByteBuf buf, Map<Integer, String> map) {
        Map<Integer, String> safe = map != null ? map : Map.of();
        buf.writeVarInt(safe.size());
        for (Map.Entry<Integer, String> e : safe.entrySet()) {
            buf.writeVarInt(e.getKey());
            buf.writeUtf(e.getValue() != null ? e.getValue() : "", MAX_STRING_LEN);
        }
    }

    static Map<Integer, String> readStringMap(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        if (n < 0 || n > MAX_ENTRIES) {
            n = 0;
        }
        Map<Integer, String> map = new HashMap<>(Math.min(n, MAX_ENTRIES));
        for (int i = 0; i < n; i++) {
            map.put(buf.readVarInt(), buf.readUtf(MAX_STRING_LEN));
        }
        return map;
    }

    static void writeIntMap(FriendlyByteBuf buf, Map<Integer, Integer> map) {
        Map<Integer, Integer> safe = map != null ? map : Map.of();
        buf.writeVarInt(safe.size());
        for (Map.Entry<Integer, Integer> e : safe.entrySet()) {
            buf.writeVarInt(e.getKey());
            buf.writeVarInt(e.getValue() != null ? e.getValue() : 0);
        }
    }

    static Map<Integer, Integer> readIntMap(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        if (n < 0 || n > MAX_ENTRIES) {
            n = 0;
        }
        Map<Integer, Integer> map = new HashMap<>(Math.min(n, MAX_ENTRIES));
        for (int i = 0; i < n; i++) {
            map.put(buf.readVarInt(), buf.readVarInt());
        }
        return map;
    }
}
