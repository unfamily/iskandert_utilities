package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.FanBlockEntity;
import net.unfamily.iskautils.network.ModMessages;
import net.unfamily.iskautils.util.FanPreview;

/**
 * C2S: enable or disable fan push-area visualization for the opening player.
 */
public record FanShowAreaC2SPacket(BlockPos pos, boolean enable) implements CustomPacketPayload {

    public static final Type<FanShowAreaC2SPacket> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "fan_show_area")
    );

    public static final StreamCodec<FriendlyByteBuf, FanShowAreaC2SPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        FanShowAreaC2SPacket::pos,
        ByteBufCodecs.BOOL,
        FanShowAreaC2SPacket::enable,
        FanShowAreaC2SPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FanShowAreaC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = (ServerLevel) player.level();

            BlockEntity blockEntity = level.getBlockEntity(packet.pos());
            if (!(blockEntity instanceof FanBlockEntity fan)) {
                return;
            }
            if (packet.enable()) {
                fan.setShowAreaEnabled(true);
                fan.setChanged();
                FanPreview.sendFootprint(player, level, packet.pos(), fan);
            } else {
                fan.setShowAreaEnabled(false);
                fan.setChanged();
                ModMessages.clearPreviewForBuilder(player, packet.pos());
            }
        });
    }
}
