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
import net.unfamily.iskautils.block.entity.CollectingCrateBlockEntity;

public record CollectingCrateRedstoneModeC2SPacket(BlockPos pos, boolean backward) implements CustomPacketPayload {

    public static final Type<CollectingCrateRedstoneModeC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "collecting_crate_redstone_mode")
    );

    public static final StreamCodec<FriendlyByteBuf, CollectingCrateRedstoneModeC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            CollectingCrateRedstoneModeC2SPacket::pos,
            ByteBufCodecs.BOOL,
            CollectingCrateRedstoneModeC2SPacket::backward,
            CollectingCrateRedstoneModeC2SPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CollectingCrateRedstoneModeC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = (ServerLevel) player.level();
            BlockEntity blockEntity = level.getBlockEntity(packet.pos());
            if (blockEntity instanceof CollectingCrateBlockEntity crate) {
                if (packet.backward()) {
                    crate.cycleRedstoneModeBackward();
                } else {
                    crate.cycleRedstoneMode();
                }
                // Client already plays the click sound; avoid duplicate UI_BUTTON_CLICK.
                crate.setChanged();
                if (player.containerMenu instanceof net.unfamily.iskautils.client.gui.CollectingCrateMenu) {
                    player.containerMenu.broadcastChanges();
                }
            }
        });
    }
}
