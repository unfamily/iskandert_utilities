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
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.BlazingAltarBlockEntity;
import net.unfamily.iskautils.client.gui.BlazingAltarMenu;
import net.unfamily.iskautils.data.FlameVisionData;

/**
 * C2S altar GUI actions.
 * action: 0=cycle spawn fwd, 1=chunk+, 2=chunk-, 3=toggle ground, 4=toggle flame vision, 5=redstone fwd, 6=redstone back, 7=cycle spawn back, 8=extinguish flames, 9=chunk max, 10=chunk min
 */
public record BlazingAltarConfigC2SPacket(BlockPos pos, int action, boolean flameVisionEnabled)
        implements CustomPacketPayload {

    public static final Type<BlazingAltarConfigC2SPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "blazing_altar_config"));

    public static final StreamCodec<FriendlyByteBuf, BlazingAltarConfigC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, BlazingAltarConfigC2SPacket::pos,
            ByteBufCodecs.INT, BlazingAltarConfigC2SPacket::action,
            ByteBufCodecs.BOOL, BlazingAltarConfigC2SPacket::flameVisionEnabled,
            BlazingAltarConfigC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BlazingAltarConfigC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (packet.action() == 4) {
                FlameVisionData.setFlameVisionEnabledForPlayer(player, packet.flameVisionEnabled());
                PacketDistributor.sendToPlayer(player, new FlameVisionSyncS2CPacket(packet.flameVisionEnabled()));
                return;
            }
            BlazingAltarBlockEntity altar = resolveAltar(player, packet.pos());
            if (altar == null) {
                return;
            }
            switch (packet.action()) {
                case 0 -> altar.cycleSpawnMode();
                case 7 -> altar.cycleSpawnModeBackward();
                case 1 -> altar.adjustChunkRadius(1);
                case 2 -> altar.adjustChunkRadius(-1);
                case 9 -> altar.setChunkRadius(altar.getMaxChunkRadius());
                case 10 -> altar.setChunkRadius(1);
                case 3 -> altar.toggleGroundOnly();
                case 5 -> altar.cycleRedstoneMode();
                case 6 -> altar.cycleRedstoneModeBackward();
                case 8 -> altar.extinguishFlamesInRange((ServerLevel) player.level());
                default -> {
                    return;
                }
            }
            altar.updateFlameVisual();
            ServerLevel level = (ServerLevel) player.level();
            level.playSound(
                    null,
                    altar.getBlockPos(),
                    SoundEvents.UI_BUTTON_CLICK.value(),
                    SoundSource.BLOCKS,
                    0.3f,
                    (packet.action() == 6 || packet.action() == 7) ? 0.82f : 1.0f);
            if (player.containerMenu instanceof BlazingAltarMenu) {
                player.containerMenu.broadcastChanges();
            }
        });
    }

    private static BlazingAltarBlockEntity resolveAltar(ServerPlayer player, BlockPos pos) {
        if (player.containerMenu instanceof BlazingAltarMenu menu) {
            BlazingAltarBlockEntity menuAltar = menu.getBlockEntity();
            if (menuAltar != null && menuAltar.getBlockPos().equals(pos)) {
                return menuAltar;
            }
        }
        BlockEntity be = ((ServerLevel) player.level()).getBlockEntity(pos);
        if (be instanceof BlazingAltarBlockEntity altar) {
            return altar;
        }
        return null;
    }
}
