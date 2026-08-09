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
 * C2S: set GUI redstone mode on a nullifier (0=Manual, 1=Disabled, 2=Low, 3=High).
 */
public record NullifierRedstoneModeC2SPacket(BlockPos pos, int guiMode) implements CustomPacketPayload {

    public static final Type<NullifierRedstoneModeC2SPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "nullifier_redstone_mode"));

    public static final StreamCodec<FriendlyByteBuf, NullifierRedstoneModeC2SPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, NullifierRedstoneModeC2SPacket::pos,
                    ByteBufCodecs.INT,     NullifierRedstoneModeC2SPacket::guiMode,
                    NullifierRedstoneModeC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(NullifierRedstoneModeC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = (ServerLevel) player.level();
            var be = level.getBlockEntity(packet.pos());
            if (!(be instanceof INullifierBE nullifier)) return;
            int mode = Math.max(0, Math.min(3, packet.guiMode()));
            nullifier.setRedstoneModeGui(mode);
            ((net.minecraft.world.level.block.entity.BlockEntity) nullifier).setChanged();
            level.playSound(null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.0f);
        });
    }
}
