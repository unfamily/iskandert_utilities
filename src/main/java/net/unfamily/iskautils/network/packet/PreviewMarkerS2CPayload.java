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
import net.unfamily.iskautils.client.MachinePreviewTracker;

/** S2C: add one footprint preview marker owned by a machine block (toggle preview only). */
public record PreviewMarkerS2CPayload(
        BlockPos builderOrigin, BlockPos pos, int color, int durationTicks, int footprintGeneration)
        implements CustomPacketPayload {

    /** Ephemeral structure-item preview; not tied to a machine owner block. */
    public static final int EPHEMERAL_FOOTPRINT_GENERATION = -1;

    public static boolean isEphemeral(int footprintGeneration) {
        return footprintGeneration < 0;
    }

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
            ByteBufCodecs.VAR_INT,
            PreviewMarkerS2CPayload::footprintGeneration,
            PreviewMarkerS2CPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PreviewMarkerS2CPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (isEphemeral(payload.footprintGeneration())) {
                ClientEvents.handleAddBillboard(payload.pos(), payload.color(), payload.durationTicks());
                return;
            }
            if (MachinePreviewTracker.isPreviewActive(payload.builderOrigin())) {
                MachinePreviewTracker.addMarker(
                        payload.builderOrigin(),
                        payload.pos(),
                        payload.color(),
                        payload.durationTicks(),
                        payload.footprintGeneration());
            } else {
                ClientEvents.handleAddOwnedBillboard(
                        payload.builderOrigin(), payload.pos(), payload.color(), payload.durationTicks());
            }
        });
    }
}
