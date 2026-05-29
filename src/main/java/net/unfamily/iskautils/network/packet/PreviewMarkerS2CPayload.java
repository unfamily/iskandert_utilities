package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.ClientEvents;

/** S2C: add one footprint preview marker owned by a machine block (toggle preview only). */
public record PreviewMarkerS2CPayload(BlockPos builderOrigin, BlockPos pos, int color, int durationTicks)
        implements CustomPacketPayload {

    public static final Type<PreviewMarkerS2CPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "preview_marker"));

    public static final StreamCodec<FriendlyByteBuf, PreviewMarkerS2CPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            PreviewMarkerS2CPayload::builderOrigin,
            BlockPos.STREAM_CODEC,
            PreviewMarkerS2CPayload::pos,
            ByteBufCodecs.INT,
            PreviewMarkerS2CPayload::color,
            ByteBufCodecs.INT,
            PreviewMarkerS2CPayload::durationTicks,
            PreviewMarkerS2CPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PreviewMarkerS2CPayload payload, IPayloadContext context) {
        context.enqueueWork(() ->
                ClientEvents.handleAddOwnedBillboard(
                        payload.builderOrigin(), payload.pos(), payload.color(), payload.durationTicks()));
    }
}
