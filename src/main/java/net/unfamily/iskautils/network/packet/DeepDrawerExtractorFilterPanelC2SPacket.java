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
import net.unfamily.iskautils.block.entity.DeepDrawerExtractorBlockEntity;

/**
 * Client-to-server: remember which filter list sub-panel (allow/deny) was last open on this extractor.
 */
public record DeepDrawerExtractorFilterPanelC2SPacket(BlockPos pos, int panel) implements CustomPacketPayload {

    public static final Type<DeepDrawerExtractorFilterPanelC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "deep_drawer_extractor_filter_panel")
    );

    public static final StreamCodec<FriendlyByteBuf, DeepDrawerExtractorFilterPanelC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            DeepDrawerExtractorFilterPanelC2SPacket::pos,
            ByteBufCodecs.VAR_INT,
            DeepDrawerExtractorFilterPanelC2SPacket::panel,
            DeepDrawerExtractorFilterPanelC2SPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeepDrawerExtractorFilterPanelC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = (ServerLevel) player.level();

            BlockEntity blockEntity = level.getBlockEntity(packet.pos());
            if (blockEntity instanceof DeepDrawerExtractorBlockEntity extractor) {
                extractor.setLastFilterPanel(packet.panel());
            }
        });
    }
}
