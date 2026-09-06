package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.ImprovedPatternCrafterBlockEntity;
import net.unfamily.iskautils.client.gui.ImprovedPatternCrafterMenu;

/**
 * Client-to-Server: sync current input filter page so server menu view offset matches.
 * When the client is on page 1, server must set view offset to 18 so slot writes go to BE slots 18-35.
 */
public record FilterPageC2SPacket(BlockPos pos, int page) implements CustomPacketPayload {

    public static final Type<FilterPageC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "pc_filter_page")
    );

    public static final StreamCodec<FriendlyByteBuf, FilterPageC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FilterPageC2SPacket::pos,
            ByteBufCodecs.INT, FilterPageC2SPacket::page,
            FilterPageC2SPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FilterPageC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player.containerMenu instanceof ImprovedPatternCrafterMenu menu
                    && menu.getBlockEntity() != null
                    && menu.getBlockEntity().getBlockPos().equals(packet.pos())) {
                menu.setInputFilterViewOffset(packet.page() * 18);
                if (menu.getBlockEntity() instanceof ImprovedPatternCrafterBlockEntity be) {
                    be.setGuiFilterPage(packet.page());
                }
            }
        });
    }
}
