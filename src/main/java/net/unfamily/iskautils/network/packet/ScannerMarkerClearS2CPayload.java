package net.unfamily.iskautils.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.ClientEvents;

/** S2C: clear all scanner highlight/billboard markers for the receiving player. */
public record ScannerMarkerClearS2CPayload() implements CustomPacketPayload {

    public static final Type<ScannerMarkerClearS2CPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "scanner_marker_clear"));

    public static final StreamCodec<FriendlyByteBuf, ScannerMarkerClearS2CPayload> STREAM_CODEC =
            StreamCodec.unit(new ScannerMarkerClearS2CPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ScannerMarkerClearS2CPayload payload, IPayloadContext context) {
        context.enqueueWork(ClientEvents::handleClearHighlights);
    }
}
