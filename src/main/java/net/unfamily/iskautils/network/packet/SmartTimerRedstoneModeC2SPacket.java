package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.SmartTimerBlockEntity;

/**
 * Packet for handling Smart Timer Redstone Mode button clicks
 */
public record SmartTimerRedstoneModeC2SPacket(BlockPos pos, boolean backward) implements CustomPacketPayload {

    public static final Type<SmartTimerRedstoneModeC2SPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "smart_timer_redstone_mode")
    );

    public static final StreamCodec<FriendlyByteBuf, SmartTimerRedstoneModeC2SPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        SmartTimerRedstoneModeC2SPacket::pos,
        ByteBufCodecs.BOOL,
        SmartTimerRedstoneModeC2SPacket::backward,
        SmartTimerRedstoneModeC2SPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SmartTimerRedstoneModeC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = player.serverLevel();

            BlockEntity blockEntity = level.getBlockEntity(packet.pos());
            if (blockEntity instanceof SmartTimerBlockEntity timer) {
                if (packet.backward()) {
                    timer.cycleRedstoneModeBackward();
                } else {
                    timer.cycleRedstoneMode();
                }
                float pitch = packet.backward() ? 0.82f : 1.0f;
                level.playSound(null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, pitch);
                timer.setChanged();
            }
        });
    }
}
