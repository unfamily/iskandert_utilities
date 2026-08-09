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
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.TemporalOverclockerBlockEntity;

/**
 * C2S: enable or disable Temporal Overclocker link-range area border preview.
 */
public record TemporalOverclockerShowAreaC2SPacket(BlockPos pos, boolean enable) implements CustomPacketPayload {

    public static final Type<TemporalOverclockerShowAreaC2SPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "temporal_overclocker_show_area"));

    public static final StreamCodec<FriendlyByteBuf, TemporalOverclockerShowAreaC2SPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, TemporalOverclockerShowAreaC2SPacket::pos,
                    ByteBufCodecs.BOOL, TemporalOverclockerShowAreaC2SPacket::enable,
                    TemporalOverclockerShowAreaC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TemporalOverclockerShowAreaC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = (ServerLevel) player.level();
            if (!(level.getBlockEntity(packet.pos()) instanceof TemporalOverclockerBlockEntity overclocker)) {
                return;
            }
            overclocker.setShowAreaEnabled(packet.enable());
            level.playSound(null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.0f);
        });
    }
}
