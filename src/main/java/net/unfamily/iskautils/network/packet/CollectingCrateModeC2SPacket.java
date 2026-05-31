package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.CollectingCrateBlockEntity;

public record CollectingCrateModeC2SPacket(BlockPos pos, boolean backward) implements CustomPacketPayload {

    public static final Type<CollectingCrateModeC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "collecting_crate_mode")
    );

    public static final StreamCodec<FriendlyByteBuf, CollectingCrateModeC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            CollectingCrateModeC2SPacket::pos,
            ByteBufCodecs.BOOL,
            CollectingCrateModeC2SPacket::backward,
            CollectingCrateModeC2SPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CollectingCrateModeC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            BlockEntity blockEntity = player.serverLevel().getBlockEntity(packet.pos());
            if (blockEntity instanceof CollectingCrateBlockEntity crate) {
                crate.cycleCollectMode(packet.backward());
                float pitch = packet.backward() ? 0.82f : 1.0f;
                player.serverLevel().playSound(null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(),
                        SoundSource.BLOCKS, 0.3f, pitch);
                crate.setChanged();
                if (player.containerMenu instanceof net.unfamily.iskautils.client.gui.CollectingCrateMenu) {
                    player.containerMenu.broadcastChanges();
                }
            }
        });
    }
}
