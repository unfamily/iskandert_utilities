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
 * Client-to-Server packet: switches to the next/previous pattern,
 * or resets the current pattern when direction == 0.
 */
public record PatternSwitchC2SPacket(BlockPos pos, int direction) implements CustomPacketPayload {

    public static final Type<PatternSwitchC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "pc_pattern_switch")
    );

    public static final StreamCodec<FriendlyByteBuf, PatternSwitchC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PatternSwitchC2SPacket::pos,
            ByteBufCodecs.INT, PatternSwitchC2SPacket::direction,
            PatternSwitchC2SPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PatternSwitchC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            BlockEntity be = ((net.minecraft.server.level.ServerLevel) player.level()).getBlockEntity(packet.pos());
            if (be instanceof ImprovedPatternCrafterBlockEntity pcbe) {
                if (packet.direction() == 0) {
                    // Reset current pattern: clear all 9 grid cells
                    PatternData pattern = pcbe.getCurrentPattern();
                    if (pattern != null) {
                        for (int i = 0; i < PatternData.GRID_SIZE; i++) {
                            pattern.setCell(i, PatternData.EMPTY);
                        }
                        pcbe.setChanged();
                    }
                } else {
                    // Switch to next/previous pattern
                    int current = pcbe.getCurrentPatternIndex();
                    int total = pcbe.getPatternCount();
                    int newIndex = ((current + packet.direction()) % total + total) % total;
                    pcbe.setCurrentPatternIndex(newIndex);
                }
            }
        });
    }
}
