package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.ImprovedPatternCrafterBlockEntity;
import net.unfamily.iskautils.client.gui.ImprovedPatternCrafterMenu;

public record FilterItemSetC2SPacket(BlockPos pos, int slotIndex, boolean outputFilter, ItemStack stack)
        implements CustomPacketPayload {

    public static final Type<FilterItemSetC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "pc_filter_item_set"));

    private static final StreamCodec<FriendlyByteBuf, ItemStack> ITEM_CODEC = new StreamCodec<>() {
        @Override
        public ItemStack decode(FriendlyByteBuf buffer) {
            return ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer);
        }

        @Override
        public void encode(FriendlyByteBuf buffer, ItemStack stack) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, stack);
        }
    };

    public static final StreamCodec<FriendlyByteBuf, FilterItemSetC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FilterItemSetC2SPacket::pos,
            ByteBufCodecs.INT, FilterItemSetC2SPacket::slotIndex,
            ByteBufCodecs.BOOL, FilterItemSetC2SPacket::outputFilter,
            ITEM_CODEC, FilterItemSetC2SPacket::stack,
            FilterItemSetC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FilterItemSetC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (!(player.containerMenu instanceof ImprovedPatternCrafterMenu menu)
                    || menu.getBlockEntity() == null
                    || !menu.getBlockEntity().getBlockPos().equals(packet.pos())) {
                return;
            }
            if (player.distanceToSqr(packet.pos().getCenter()) > 64.0D) return;
            if (player.serverLevel().getBlockEntity(packet.pos()) instanceof ImprovedPatternCrafterBlockEntity crafter) {
                crafter.setFilterItem(packet.slotIndex(), packet.outputFilter(), packet.stack());
                menu.broadcastFullState();
            }
        });
    }
}
