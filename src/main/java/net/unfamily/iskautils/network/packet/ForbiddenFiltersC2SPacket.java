package net.unfamily.iskautils.network.packet;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.ImprovedPatternCrafterBlockEntity;
import net.unfamily.iskautils.client.gui.ImprovedPatternCrafterMenu;

/** Replaces the Pattern Crafter forbidden-output string list. */
public record ForbiddenFiltersC2SPacket(BlockPos pos, List<String> filters) implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 64;
    private static final int MAX_LEN = 256;

    public static final Type<ForbiddenFiltersC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "pc_forbidden_filters"));

    public static final StreamCodec<FriendlyByteBuf, ForbiddenFiltersC2SPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                BlockPos.STREAM_CODEC.encode(buf, packet.pos());
                List<String> list = packet.filters() != null ? packet.filters() : List.of();
                buf.writeVarInt(Math.min(MAX_ENTRIES, list.size()));
                int written = 0;
                for (String filter : list) {
                    if (written >= MAX_ENTRIES) break;
                    buf.writeUtf(filter == null ? "" : filter, MAX_LEN);
                    written++;
                }
            },
            buf -> {
                BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
                int n = buf.readVarInt();
                List<String> filters = new ArrayList<>();
                for (int i = 0; i < n && i < MAX_ENTRIES; i++) {
                    filters.add(buf.readUtf(MAX_LEN));
                }
                return new ForbiddenFiltersC2SPacket(pos, filters);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ForbiddenFiltersC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (!(player.containerMenu instanceof ImprovedPatternCrafterMenu menu)
                    || menu.getBlockEntity() == null
                    || !menu.getBlockEntity().getBlockPos().equals(packet.pos())) {
                return;
            }
            if (((net.minecraft.server.level.ServerLevel) player.level()).getBlockEntity(packet.pos()) instanceof ImprovedPatternCrafterBlockEntity crafter) {
                crafter.setForbiddenFilters(packet.filters());
                menu.broadcastFullState();
            }
        });
    }
}
