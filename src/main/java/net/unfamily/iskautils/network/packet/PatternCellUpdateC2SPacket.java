package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.ImprovedPatternCrafterBlockEntity;
import net.unfamily.iskautils.pattern.PatternData;

/**
 * Client-to-Server packet: updates a single cell in a pattern grid.
 */
public record PatternCellUpdateC2SPacket(BlockPos pos, int patternIndex, int cellIndex, int newValue)
        implements CustomPacketPayload {

    public static final Type<PatternCellUpdateC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "pc_pattern_cell_update")
    );

    public static final StreamCodec<FriendlyByteBuf, PatternCellUpdateC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PatternCellUpdateC2SPacket::pos,
            ByteBufCodecs.INT, PatternCellUpdateC2SPacket::patternIndex,
            ByteBufCodecs.INT, PatternCellUpdateC2SPacket::cellIndex,
            ByteBufCodecs.INT, PatternCellUpdateC2SPacket::newValue,
            PatternCellUpdateC2SPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PatternCellUpdateC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            BlockEntity be = ((net.minecraft.server.level.ServerLevel) player.level()).getBlockEntity(packet.pos());
            if (be instanceof ImprovedPatternCrafterBlockEntity pcbe) {
                PatternData pattern = pcbe.getPattern(packet.patternIndex());
                if (pattern != null) {
                    pattern.setCell(packet.cellIndex(), packet.newValue());
                    pcbe.setChanged();
                }
            }
        });
    }
}
