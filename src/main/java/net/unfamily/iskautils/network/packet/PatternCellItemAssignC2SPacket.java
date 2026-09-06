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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.ImprovedPatternCrafterBlockEntity;

public record PatternCellItemAssignC2SPacket(BlockPos pos, int cellIndex, ItemStack stack)
        implements CustomPacketPayload {
    public static final Type<PatternCellItemAssignC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "pc_pattern_cell_item_assign"));
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
    public static final StreamCodec<FriendlyByteBuf, PatternCellItemAssignC2SPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PatternCellItemAssignC2SPacket::pos,
                    ByteBufCodecs.INT, PatternCellItemAssignC2SPacket::cellIndex,
                    ITEM_CODEC, PatternCellItemAssignC2SPacket::stack,
                    PatternCellItemAssignC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PatternCellItemAssignC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            BlockEntity blockEntity = ((net.minecraft.server.level.ServerLevel) player.level())
                    .getBlockEntity(packet.pos());
            if (blockEntity instanceof ImprovedPatternCrafterBlockEntity crafter) {
                crafter.applyPatternItemAssignment(packet.cellIndex(), packet.stack());
            }
        });
    }
}
