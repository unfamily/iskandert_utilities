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
import net.unfamily.iskautils.block.entity.INullifierBE;
import net.unfamily.iskautils.block.entity.NullifierTargetMode;

/**
 * C2S: set mob/player target mode on a nullifier ({@link NullifierTargetMode#getId()}).
 */
public record NullifierTargetModeC2SPacket(BlockPos pos, int modeId) implements CustomPacketPayload {

    public static final Type<NullifierTargetModeC2SPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "nullifier_target_mode"));

    public static final StreamCodec<FriendlyByteBuf, NullifierTargetModeC2SPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, NullifierTargetModeC2SPacket::pos,
                    ByteBufCodecs.INT, NullifierTargetModeC2SPacket::modeId,
                    NullifierTargetModeC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(NullifierTargetModeC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = (ServerLevel) player.level();
            var be = level.getBlockEntity(packet.pos());
            if (!(be instanceof INullifierBE nullifier)) {
                return;
            }
            nullifier.setTargetMode(NullifierTargetMode.fromId(packet.modeId()));
            ((net.minecraft.world.level.block.entity.BlockEntity) nullifier).setChanged();
            level.playSound(null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.0f);
        });
    }
}
