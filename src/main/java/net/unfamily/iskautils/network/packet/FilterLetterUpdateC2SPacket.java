package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.ImprovedPatternCrafterBlockEntity;

/**
 * Client-to-Server packet: updates the letter assignment of an input filter slot.
 */
public record FilterLetterUpdateC2SPacket(BlockPos pos, int filterIndex, int newLetter)
        implements CustomPacketPayload {

    public static final Type<FilterLetterUpdateC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "pc_filter_letter_update")
    );

    public static final StreamCodec<FriendlyByteBuf, FilterLetterUpdateC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FilterLetterUpdateC2SPacket::pos,
            ByteBufCodecs.INT, FilterLetterUpdateC2SPacket::filterIndex,
            ByteBufCodecs.INT, FilterLetterUpdateC2SPacket::newLetter,
            FilterLetterUpdateC2SPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FilterLetterUpdateC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            BlockEntity be = player.serverLevel().getBlockEntity(packet.pos());
            if (be instanceof ImprovedPatternCrafterBlockEntity pcbe) {
                pcbe.setFilterLetter(packet.filterIndex(), packet.newLetter());
            }
        });
    }
}
