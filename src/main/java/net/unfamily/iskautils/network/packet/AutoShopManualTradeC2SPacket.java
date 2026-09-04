package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.AutoShopBlockEntity;
import net.unfamily.iskautils.client.gui.AutoShopMenu;

/**
 * Manual buy/sell from the emerald button. Active only while redstone mode is DISABLED;
 * quantity uses shop multipliers (1 / 4 / 16).
 */
public record AutoShopManualTradeC2SPacket(BlockPos pos, int quantity) implements CustomPacketPayload {
    public static final Type<AutoShopManualTradeC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "auto_shop_manual_trade"));

    public static final StreamCodec<FriendlyByteBuf, AutoShopManualTradeC2SPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, AutoShopManualTradeC2SPacket::pos,
                    net.minecraft.network.codec.ByteBufCodecs.VAR_INT, AutoShopManualTradeC2SPacket::quantity,
                    AutoShopManualTradeC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AutoShopManualTradeC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            var blockEntity = player.serverLevel().getBlockEntity(packet.pos());
            if (!(blockEntity instanceof AutoShopBlockEntity autoShop) || !autoShop.canPlayerUse(player)) {
                return;
            }
            if (autoShop.tryManualTrade(player, packet.quantity())) {
                player.serverLevel().sendBlockUpdated(packet.pos(), blockEntity.getBlockState(),
                        blockEntity.getBlockState(), 3);
                if (player.containerMenu instanceof AutoShopMenu autoMenu
                        && autoMenu.getBlockPos().equals(packet.pos())) {
                    autoMenu.broadcastFullState();
                }
                player.serverLevel().playSound(
                        null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.0f);
            }
        });
    }
}
