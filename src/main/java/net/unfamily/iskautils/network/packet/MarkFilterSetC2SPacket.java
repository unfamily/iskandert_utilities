package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.gui.ImprovedPatternCrafterMenu;

/** Sets one input/output dedication filter from a JEI ghost drop. */
public record MarkFilterSetC2SPacket(BlockPos pos, boolean outputMark, int slotIndex, ItemStack stack)
        implements CustomPacketPayload {
    public static final Type<MarkFilterSetC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "pc_mark_filter_set"));
    private static final StreamCodec<FriendlyByteBuf, ItemStack> ITEM_CODEC = new StreamCodec<>() {
        @Override public ItemStack decode(FriendlyByteBuf buffer) {
            return ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer);
        }
        @Override public void encode(FriendlyByteBuf buffer, ItemStack stack) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, stack);
        }
    };
    public static final StreamCodec<FriendlyByteBuf, MarkFilterSetC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, MarkFilterSetC2SPacket::pos,
            ByteBufCodecs.BOOL, MarkFilterSetC2SPacket::outputMark,
            ByteBufCodecs.INT, MarkFilterSetC2SPacket::slotIndex,
            ITEM_CODEC, MarkFilterSetC2SPacket::stack,
            MarkFilterSetC2SPacket::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(MarkFilterSetC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (!(player.containerMenu instanceof ImprovedPatternCrafterMenu menu)
                    || menu.getBlockEntity() == null
                    || !menu.getBlockEntity().getBlockPos().equals(packet.pos())) return;
            menu.getBlockEntity().setMarkFilter(packet.outputMark(), packet.slotIndex(), packet.stack());
            menu.broadcastFullState();
        });
    }
}
