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
import net.unfamily.iskautils.block.entity.INullifierBE;

/** C2S: shift nullifier effect area horizontally without changing range. */
public record NullifierAreaOffsetC2SPacket(BlockPos pos, int delta) implements CustomPacketPayload {

    public static final Type<NullifierAreaOffsetC2SPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "nullifier_area_offset"));

    public static final StreamCodec<FriendlyByteBuf, NullifierAreaOffsetC2SPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, NullifierAreaOffsetC2SPacket::pos,
                    ByteBufCodecs.INT, NullifierAreaOffsetC2SPacket::delta,
                    NullifierAreaOffsetC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(NullifierAreaOffsetC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = (ServerLevel) player.level();
            BlockEntity be = level.getBlockEntity(packet.pos());
            if (!(be instanceof INullifierBE nullifier)) {
                return;
            }
            int before = nullifier.getAreaOffsetX();
            nullifier.offsetAreaX(packet.delta());
            if (nullifier.getAreaOffsetX() != before) {
                be.setChanged();
            }
            level.playSound(null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.0f);
        });
    }
}
