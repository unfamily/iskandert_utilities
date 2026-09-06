package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.ImprovedPatternCrafterBlockEntity;

/**
 * Packet for Mark Input button: copy input to filters (like Structure Placer Set Inventory).
 * Modes: 0 = normal (set filters from input), 1 = shift (clear all), 2 = ctrl/alt (clear empty).
 */
public record MarkInputC2SPacket(BlockPos pos, int mode) implements CustomPacketPayload {

    public static final int MODE_NORMAL = 0;
    public static final int MODE_SHIFT = 1;
    public static final int MODE_CTRL = 2;

    public static final Type<MarkInputC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "pc_mark_input")
    );

    public static final StreamCodec<FriendlyByteBuf, MarkInputC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            MarkInputC2SPacket::pos,
            ByteBufCodecs.INT,
            MarkInputC2SPacket::mode,
            MarkInputC2SPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MarkInputC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            BlockEntity be = ((net.minecraft.server.level.ServerLevel) player.level()).getBlockEntity(packet.pos());
            if (be instanceof ImprovedPatternCrafterBlockEntity machine) {
                switch (packet.mode()) {
                    case MODE_NORMAL -> {
                        machine.setInputFilters();
                        ((net.minecraft.server.level.ServerLevel) player.level()).playSound(null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.0f);
                    }
                    case MODE_SHIFT -> {
                        machine.clearAllInputFilters();
                        ((net.minecraft.server.level.ServerLevel) player.level()).playSound(null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 0.8f);
                    }
                    case MODE_CTRL -> {
                        machine.clearEmptyInputFilters();
                        ((net.minecraft.server.level.ServerLevel) player.level()).playSound(null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 0.9f);
                    }
                }
                machine.setChanged();
            }
        });
    }
}
