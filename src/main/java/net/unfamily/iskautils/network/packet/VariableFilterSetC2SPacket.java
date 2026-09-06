package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.ImprovedPatternCrafterBlockEntity;
import net.unfamily.iskautils.client.gui.ImprovedPatternCrafterMenu;

/** Sets one Pattern Crafter variable filter string (and optional letter). */
public record VariableFilterSetC2SPacket(BlockPos pos, int slotIndex, String filter, int letter)
        implements CustomPacketPayload {
    private static final int MAX_LEN = 256;

    public static final Type<VariableFilterSetC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "pc_variable_filter_set"));

    public static final StreamCodec<FriendlyByteBuf, VariableFilterSetC2SPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                BlockPos.STREAM_CODEC.encode(buf, packet.pos());
                buf.writeVarInt(packet.slotIndex());
                buf.writeUtf(packet.filter() == null ? "" : packet.filter(), MAX_LEN);
                buf.writeVarInt(packet.letter());
            },
            buf -> new VariableFilterSetC2SPacket(
                    BlockPos.STREAM_CODEC.decode(buf),
                    buf.readVarInt(),
                    buf.readUtf(MAX_LEN),
                    buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(VariableFilterSetC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (!(player.containerMenu instanceof ImprovedPatternCrafterMenu menu)
                    || menu.getBlockEntity() == null
                    || !menu.getBlockEntity().getBlockPos().equals(packet.pos())) {
                return;
            }
            if (player.serverLevel().getBlockEntity(packet.pos()) instanceof ImprovedPatternCrafterBlockEntity crafter) {
                crafter.setInputFilterString(packet.slotIndex(), packet.filter());
                crafter.setFilterLetter(packet.slotIndex(), packet.letter());
                menu.broadcastFullState();
            }
        });
    }
}
