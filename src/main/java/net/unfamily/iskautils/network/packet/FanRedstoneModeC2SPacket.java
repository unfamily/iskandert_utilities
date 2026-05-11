package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.FanBlockEntity;

/**
 * Packet for handling Fan Redstone Mode button clicks
 */
public record FanRedstoneModeC2SPacket(BlockPos pos, boolean backward) implements CustomPacketPayload {

    public static final Type<FanRedstoneModeC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "fan_redstone_mode")
    );

    public static final StreamCodec<FriendlyByteBuf, FanRedstoneModeC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            FanRedstoneModeC2SPacket::pos,
            ByteBufCodecs.BOOL,
            FanRedstoneModeC2SPacket::backward,
            FanRedstoneModeC2SPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FanRedstoneModeC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = (ServerLevel) player.level();

            BlockEntity blockEntity = level.getBlockEntity(packet.pos());
            if (blockEntity instanceof FanBlockEntity fan) {
                if (packet.backward()) {
                    fan.cycleRedstoneModeBackward();
                } else {
                    fan.cycleRedstoneMode();
                }

                float pitch = packet.backward() ? 0.82f : 1.0f;
                level.playSound(null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, pitch);

                fan.setChanged();
            }
        });
    }
}
