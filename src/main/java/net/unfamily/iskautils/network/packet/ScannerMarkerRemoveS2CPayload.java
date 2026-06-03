package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.ClientEvents;

/** S2C: remove one scanner marker at a position. */
public record ScannerMarkerRemoveS2CPayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<ScannerMarkerRemoveS2CPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "scanner_marker_remove"));

    public static final StreamCodec<FriendlyByteBuf, ScannerMarkerRemoveS2CPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ScannerMarkerRemoveS2CPayload::pos,
            ScannerMarkerRemoveS2CPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ScannerMarkerRemoveS2CPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientEvents.handleRemoveHighlight(payload.pos()));
    }
}
