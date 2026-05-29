package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.ClientEvents;

/** S2C: clear footprint preview markers for one owner block only. */
public record ClearPreviewForOwnerS2CPayload(BlockPos owner) implements CustomPacketPayload {

    public static final Type<ClearPreviewForOwnerS2CPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "clear_preview_for_owner"));

    public static final StreamCodec<FriendlyByteBuf, ClearPreviewForOwnerS2CPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ClearPreviewForOwnerS2CPayload::owner,
            ClearPreviewForOwnerS2CPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClearPreviewForOwnerS2CPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientEvents.handleClearPreviewForOwner(payload.owner()));
    }
}
