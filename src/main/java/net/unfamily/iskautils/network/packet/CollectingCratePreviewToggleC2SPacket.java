package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.CollectingCrateBlockEntity;
import net.unfamily.iskautils.network.ModMessages;
import net.unfamily.iskautils.util.CollectingCratePreview;

/**
 * C2S: enable or disable collection-area visualization for the opening player.
 */
public record CollectingCratePreviewToggleC2SPacket(BlockPos pos, boolean enable) implements CustomPacketPayload {

    public static final Type<CollectingCratePreviewToggleC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "collecting_crate_preview_toggle")
    );

    public static final StreamCodec<FriendlyByteBuf, CollectingCratePreviewToggleC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            CollectingCratePreviewToggleC2SPacket::pos,
            ByteBufCodecs.BOOL,
            CollectingCratePreviewToggleC2SPacket::enable,
            CollectingCratePreviewToggleC2SPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CollectingCratePreviewToggleC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = (ServerLevel) player.level();

            BlockEntity blockEntity = level.getBlockEntity(packet.pos());
            if (!(blockEntity instanceof CollectingCrateBlockEntity crate)) {
                return;
            }
            if (packet.enable()) {
                crate.setPreviewEnabled(true);
                crate.setChanged();
                CollectingCratePreview.sendFootprint(player, level, packet.pos(), crate);
            } else {
                crate.setPreviewEnabled(false);
                crate.setChanged();
                ModMessages.clearPreviewForBuilder(player, packet.pos());
            }
        });
    }
}
