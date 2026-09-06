package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.gui.ImprovedPatternCrafterMenu;

/** Synchronizes the nine-slot output page. */
public record OutputPageC2SPacket(BlockPos pos, int page) implements CustomPacketPayload {
    public static final Type<OutputPageC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "pc_output_page"));
    public static final StreamCodec<FriendlyByteBuf, OutputPageC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OutputPageC2SPacket::pos,
            ByteBufCodecs.INT, OutputPageC2SPacket::page,
            OutputPageC2SPacket::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(OutputPageC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player.containerMenu instanceof ImprovedPatternCrafterMenu menu
                    && menu.getBlockEntity() != null
                    && menu.getBlockEntity().getBlockPos().equals(packet.pos())) {
                menu.getBlockEntity().setGuiOutputPage(packet.page());
                menu.setOutputViewOffset(menu.getBlockEntity().getGuiOutputPage() * 9);
                menu.broadcastFullState();
            }
        });
    }
}
