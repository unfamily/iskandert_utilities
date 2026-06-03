package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.ClientEvents;

/** S2C: add one scanner highlight or billboard marker for the receiving player. */
public record ScannerMarkerAddS2CPayload(BlockPos pos, int color, int durationTicks, String name, boolean billboard)
        implements CustomPacketPayload {

    public static final Type<ScannerMarkerAddS2CPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "scanner_marker_add"));

    public static final StreamCodec<FriendlyByteBuf, ScannerMarkerAddS2CPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ScannerMarkerAddS2CPayload::pos,
            ByteBufCodecs.INT,
            ScannerMarkerAddS2CPayload::color,
            ByteBufCodecs.INT,
            ScannerMarkerAddS2CPayload::durationTicks,
            ByteBufCodecs.STRING_UTF8,
            ScannerMarkerAddS2CPayload::name,
            ByteBufCodecs.BOOL,
            ScannerMarkerAddS2CPayload::billboard,
            ScannerMarkerAddS2CPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ScannerMarkerAddS2CPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            String name = payload.name();
            if (payload.billboard()) {
                if (name == null || name.isEmpty()) {
                    ClientEvents.handleAddBillboard(payload.pos(), payload.color(), payload.durationTicks());
                } else {
                    ClientEvents.handleAddBillboardWithName(payload.pos(), payload.color(), payload.durationTicks(), name);
                }
            } else if (name == null || name.isEmpty()) {
                ClientEvents.handleAddHighlight(payload.pos(), payload.color(), payload.durationTicks());
            } else {
                ClientEvents.handleAddHighlightWithName(payload.pos(), payload.color(), payload.durationTicks(), name);
            }
        });
    }
}
