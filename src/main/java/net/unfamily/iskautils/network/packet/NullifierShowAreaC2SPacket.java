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
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.INullifierBE;

/**
 * C2S: enable or disable nullifier area border preview.
 */
public record NullifierShowAreaC2SPacket(BlockPos pos, boolean enable) implements CustomPacketPayload {

    public static final Type<NullifierShowAreaC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "nullifier_show_area"));

    public static final StreamCodec<FriendlyByteBuf, NullifierShowAreaC2SPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, NullifierShowAreaC2SPacket::pos,
                    ByteBufCodecs.BOOL, NullifierShowAreaC2SPacket::enable,
                    NullifierShowAreaC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(NullifierShowAreaC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = player.serverLevel();
            if (!player.canInteractWithBlock(packet.pos(), 8.0)) {
                return;
            }
            var be = level.getBlockEntity(packet.pos());
            if (!(be instanceof INullifierBE nullifier)) {
                return;
            }
            nullifier.setShowAreaEnabled(packet.enable());
            ((net.minecraft.world.level.block.entity.BlockEntity) nullifier).setChanged();
            level.playSound(null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.0f);
        });
    }
}
