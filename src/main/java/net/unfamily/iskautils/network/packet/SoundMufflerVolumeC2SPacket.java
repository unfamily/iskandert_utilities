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
import net.unfamily.iskautils.block.entity.SoundMufflerBlockEntity;

public record SoundMufflerVolumeC2SPacket(BlockPos pos, int categoryIndex, int delta) implements CustomPacketPayload {

    public static final Type<SoundMufflerVolumeC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "sound_muffler_volume"));

    public static final StreamCodec<FriendlyByteBuf, SoundMufflerVolumeC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SoundMufflerVolumeC2SPacket::pos,
            ByteBufCodecs.INT,
            SoundMufflerVolumeC2SPacket::categoryIndex,
            ByteBufCodecs.INT,
            SoundMufflerVolumeC2SPacket::delta,
            SoundMufflerVolumeC2SPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SoundMufflerVolumeC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            BlockEntity be = player.level().getBlockEntity(packet.pos());
            if (be instanceof SoundMufflerBlockEntity muffler) {
                muffler.addVolume(packet.categoryIndex(), packet.delta());
                player.level().playSound(null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.0f);
            }
        });
    }
}
