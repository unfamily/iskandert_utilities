package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.AutoShopBlockEntity;
import net.unfamily.iskautils.client.gui.AutoShopMenu;

public record AutoShopSetSelectedItemC2SPacket(BlockPos pos, ItemStack item) implements CustomPacketPayload {

    public static final Type<AutoShopSetSelectedItemC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "auto_shop_set_selected_item"));

    private static final StreamCodec<FriendlyByteBuf, ItemStack> FILTER_ITEM_CODEC = new StreamCodec<>() {
        @Override
        public ItemStack decode(FriendlyByteBuf buf) {
            return ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf);
        }

        @Override
        public void encode(FriendlyByteBuf buf, ItemStack stack) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, stack);
        }
    };

    public static final StreamCodec<FriendlyByteBuf, AutoShopSetSelectedItemC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AutoShopSetSelectedItemC2SPacket::pos,
            FILTER_ITEM_CODEC, AutoShopSetSelectedItemC2SPacket::item,
            AutoShopSetSelectedItemC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AutoShopSetSelectedItemC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            var blockEntity = player.serverLevel().getBlockEntity(packet.pos());
            if (!(blockEntity instanceof AutoShopBlockEntity autoShop)) {
                return;
            }
            if (!autoShop.canPlayerUse(player)) {
                return;
            }
            ItemStack item = packet.item();
            if (item.isEmpty()) {
                autoShop.setSelectedItem(ItemStack.EMPTY);
            } else {
                ItemStack copy = item.copy();
                copy.setCount(1);
                autoShop.setSelectedItem(copy);
            }
            autoShop.setChanged();
            player.serverLevel().sendBlockUpdated(packet.pos(), blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
            if (player.containerMenu instanceof AutoShopMenu autoMenu && autoMenu.getBlockPos().equals(packet.pos())) {
                autoMenu.broadcastFullState();
            }
        });
    }
}
