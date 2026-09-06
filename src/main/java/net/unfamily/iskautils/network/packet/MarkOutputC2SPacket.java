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
import net.unfamily.iskautils.block.entity.ImprovedPatternCrafterBlockEntity;
import net.unfamily.iskautils.client.gui.ImprovedPatternCrafterMenu;

/** Applies, clears, or cleans output slot dedication filters. */
public record MarkOutputC2SPacket(BlockPos pos, int mode) implements CustomPacketPayload {
    public static final Type<MarkOutputC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "pc_mark_output"));
    public static final StreamCodec<FriendlyByteBuf, MarkOutputC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, MarkOutputC2SPacket::pos,
            ByteBufCodecs.INT, MarkOutputC2SPacket::mode,
            MarkOutputC2SPacket::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(MarkOutputC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (!(player.containerMenu instanceof ImprovedPatternCrafterMenu menu)
                    || menu.getBlockEntity() == null
                    || !menu.getBlockEntity().getBlockPos().equals(packet.pos())) return;
            ImprovedPatternCrafterBlockEntity machine = menu.getBlockEntity();
            switch (packet.mode()) {
                case MarkInputC2SPacket.MODE_NORMAL -> machine.setOutputFilters();
                case MarkInputC2SPacket.MODE_SHIFT -> machine.clearAllOutputFilters();
                case MarkInputC2SPacket.MODE_CTRL -> machine.clearEmptyOutputFilters();
                default -> { return; }
            }
            menu.broadcastFullState();
        });
    }
}
