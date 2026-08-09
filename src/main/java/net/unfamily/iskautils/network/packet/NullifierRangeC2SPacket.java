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

/**
 * C2S: change the range of a nullifier by a delta (+1 or -1).
 */
public record NullifierRangeC2SPacket(BlockPos pos, int delta) implements CustomPacketPayload {

    public static final Type<NullifierRangeC2SPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "nullifier_range"));

    public static final StreamCodec<FriendlyByteBuf, NullifierRangeC2SPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, NullifierRangeC2SPacket::pos,
                    ByteBufCodecs.INT,     NullifierRangeC2SPacket::delta,
                    NullifierRangeC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(NullifierRangeC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = (ServerLevel) player.level();
            var be = level.getBlockEntity(packet.pos());
            if (!(be instanceof INullifierBE nullifier)) return;
            int current = nullifier.getRange();
            int newRange = Math.max(1, Math.min(current + packet.delta(), nullifier.getMaxRange()));
            level.playSound(null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.0f);
            if (newRange != current) {
                nullifier.setRange(newRange);
                ((net.minecraft.world.level.block.entity.BlockEntity) nullifier).setChanged();
            }
        });
    }
}
