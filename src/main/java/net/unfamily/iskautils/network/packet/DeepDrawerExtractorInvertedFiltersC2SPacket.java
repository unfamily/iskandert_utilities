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

import java.util.Map;

/**
 * Client-to-server: apply inverted filter map and concat map on the Deep Drawer Extractor.
 */
public record DeepDrawerExtractorInvertedFiltersC2SPacket(
        BlockPos pos, Map<Integer, String> invertedFilterMap, Map<Integer, Integer> concatMap)
        implements CustomPacketPayload {

    public static final Type<DeepDrawerExtractorInvertedFiltersC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "deep_drawer_extractor_inverted_filters"));

    public static final StreamCodec<FriendlyByteBuf, DeepDrawerExtractorInvertedFiltersC2SPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                BlockPos.STREAM_CODEC.encode(buf, p.pos());
                DeepDrawerExtractorFilterUpdateC2SPacket.writeStringMap(buf, p.invertedFilterMap());
                DeepDrawerExtractorFilterUpdateC2SPacket.writeIntMap(buf, p.concatMap());
            },
            buf -> new DeepDrawerExtractorInvertedFiltersC2SPacket(
                    BlockPos.STREAM_CODEC.decode(buf),
                    DeepDrawerExtractorFilterUpdateC2SPacket.readStringMap(buf),
                    DeepDrawerExtractorFilterUpdateC2SPacket.readIntMap(buf))
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
            extractor.setDenyConcatFromMap(packet.concatMap());
        });
    }
}
