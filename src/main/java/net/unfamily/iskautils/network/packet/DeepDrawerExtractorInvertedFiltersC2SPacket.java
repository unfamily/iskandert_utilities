package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.DeepDrawerExtractorBlockEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-to-server: apply inverted filter map on the Deep Drawer Extractor.
 */
public record DeepDrawerExtractorInvertedFiltersC2SPacket(BlockPos pos, Map<Integer, String> invertedFilterMap)
        implements CustomPacketPayload {

    private static final int MAX_ENTRIES = 512;
    private static final int MAX_STRING_LEN = 512;

    public static final Type<DeepDrawerExtractorInvertedFiltersC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "deep_drawer_extractor_inverted_filters"));

    public static final StreamCodec<FriendlyByteBuf, DeepDrawerExtractorInvertedFiltersC2SPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                BlockPos.STREAM_CODEC.encode(buf, p.pos());
                Map<Integer, String> map = p.invertedFilterMap() != null ? p.invertedFilterMap() : Map.of();
                buf.writeVarInt(map.size());
                for (Map.Entry<Integer, String> e : map.entrySet()) {
                    buf.writeVarInt(e.getKey());
                    buf.writeUtf(e.getValue() != null ? e.getValue() : "", MAX_STRING_LEN);
                }
            },
            buf -> {
                BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
                int n = buf.readVarInt();
                if (n < 0 || n > MAX_ENTRIES) {
                    n = 0;
                }
                Map<Integer, String> map = new HashMap<>(Math.min(n, MAX_ENTRIES));
                for (int i = 0; i < n; i++) {
                    map.put(buf.readVarInt(), buf.readUtf(MAX_STRING_LEN));
                }
                return new DeepDrawerExtractorInvertedFiltersC2SPacket(pos, map);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeepDrawerExtractorInvertedFiltersC2SPacket packet, IPayloadContext context) {
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
            extractor.setInvertedFilterFieldsFromMap(packet.invertedFilterMap());
            player.level().playSound(null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.0f);
        });
    }
}
