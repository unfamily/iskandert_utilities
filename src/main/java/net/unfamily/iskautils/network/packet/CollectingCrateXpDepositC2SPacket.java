package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
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

public record CollectingCrateXpDepositC2SPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<CollectingCrateXpDepositC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "collecting_crate_xp_deposit")
    );

    public static final StreamCodec<FriendlyByteBuf, CollectingCrateXpDepositC2SPacket> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, CollectingCrateXpDepositC2SPacket::pos, CollectingCrateXpDepositC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CollectingCrateXpDepositC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            BlockEntity blockEntity = player.serverLevel().getBlockEntity(packet.pos());
            if (blockEntity instanceof CollectingCrateBlockEntity crate) {
                crate.depositAllXpFromPlayer(player);
                player.serverLevel().playSound(null, packet.pos(), SoundEvents.BOTTLE_EMPTY,
                        SoundSource.PLAYERS, 0.3f, 1.0f);
                crate.setChanged();
                if (player.containerMenu instanceof net.unfamily.iskautils.client.gui.CollectingCrateMenu) {
                    player.containerMenu.broadcastChanges();
                }
            }
        });
    }
}
