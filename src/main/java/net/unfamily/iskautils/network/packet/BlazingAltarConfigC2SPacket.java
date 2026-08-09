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
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.BlazingAltarBlockEntity;
import net.unfamily.iskautils.block.entity.BlazingAltarBlockSync;
import net.unfamily.iskautils.client.gui.BlazingAltarMenu;
import net.unfamily.iskautils.data.FlameVisionData;

/**
 * C2S altar GUI actions.
 * action: 0=cycle spawn fwd, 1=chunk+, 2=chunk-, 3=toggle ground, 4=set flame vision fwd,
 * 5=redstone fwd, 6=redstone back, 7=cycle spawn back, 8=extinguish flames, 9=chunk max, 10=chunk min,
 * 11=toggle ground back, 12=set flame vision back, 13=show area on, 14=show area off
 */
public record BlazingAltarConfigC2SPacket(BlockPos pos, int action, boolean flameVisionEnabled)
        implements CustomPacketPayload {

    public static final Type<BlazingAltarConfigC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "blazing_altar_config"));

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
            if (packet.action() == 4 || packet.action() == 12) {
                FlameVisionData.setFlameVisionEnabledForPlayer(player, packet.flameVisionEnabled());
                PacketDistributor.sendToPlayer(player, new FlameVisionSyncS2CPacket(packet.flameVisionEnabled()));
                BlazingAltarBlockEntity altarForSound = resolveAltar(player, packet.pos());
                BlockPos soundPos = altarForSound != null ? altarForSound.getBlockPos() : player.blockPosition();
                player.serverLevel().playSound(
                        null,
                        soundPos,
                        SoundEvents.UI_BUTTON_CLICK.value(),
                        SoundSource.BLOCKS,
                        0.3f,
                        isBackPitch(packet.action()) ? 0.82f : 1.0f);
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
                case 3, 11 -> altar.toggleGroundOnly();
                case 5 -> altar.cycleRedstoneMode();
                case 6 -> altar.cycleRedstoneModeBackward();
                case 8 -> altar.extinguishFlamesInRange(player.serverLevel());
                case 13 -> altar.setShowAreaEnabled(true);
                case 14 -> altar.setShowAreaEnabled(false);
                default -> {
                    return;
                }
            }
            BlazingAltarBlockSync.sync(altar);
            player.serverLevel().playSound(
                    null,
                    altar.getBlockPos(),
                    SoundEvents.UI_BUTTON_CLICK.value(),
                    SoundSource.BLOCKS,
                    0.3f,
                    isBackPitch(packet.action()) ? 0.82f : 1.0f);
            if (player.containerMenu instanceof BlazingAltarMenu) {
                player.containerMenu.broadcastChanges();
            }
        });
    }

    private static boolean isBackPitch(int action) {
        return action == 6 || action == 7 || action == 11 || action == 12;
    }

    private static BlazingAltarBlockEntity resolveAltar(ServerPlayer player, BlockPos pos) {
        if (player.containerMenu instanceof BlazingAltarMenu menu) {
            BlazingAltarBlockEntity menuAltar = menu.getBlockEntity();
            if (menuAltar != null && menuAltar.getBlockPos().equals(pos)) {
                return menuAltar;
            }
        }
        BlockEntity be = player.serverLevel().getBlockEntity(pos);
        if (be instanceof BlazingAltarBlockEntity altar) {
            return altar;
        }
        return null;
    }
}
