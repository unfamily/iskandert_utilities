package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.AutoShopBlockEntity;

public record AutoShopSetEncapsulatedC2SPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<AutoShopSetEncapsulatedC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "auto_shop_set_encapsulated"));

    public static final StreamCodec<FriendlyByteBuf, AutoShopSetEncapsulatedC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AutoShopSetEncapsulatedC2SPacket::pos,
            AutoShopSetEncapsulatedC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AutoShopSetEncapsulatedC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            var blockEntity = player.level().getBlockEntity(packet.pos());
            if (!(blockEntity instanceof AutoShopBlockEntity autoShop)) {
                return;
            }
            ItemStack itemToSet = player.getMainHandItem();
            if (itemToSet.isEmpty()) {
                autoShop.getEncapsulatedSlot().setStackInSlot(0, ItemStack.EMPTY);
            } else {
                ItemStack copyStack = itemToSet.copy();
                copyStack.setCount(1);
                autoShop.getEncapsulatedSlot().setStackInSlot(0, copyStack);
            }
            autoShop.setChanged();
            player.level().sendBlockUpdated(packet.pos(), blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
        });
    }
}
