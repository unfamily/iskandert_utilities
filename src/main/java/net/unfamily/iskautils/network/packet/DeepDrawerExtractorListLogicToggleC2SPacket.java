package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.DeepDrawerExtractorBlockEntity;

public record DeepDrawerExtractorListLogicToggleC2SPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<DeepDrawerExtractorListLogicToggleC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "deep_drawer_extractor_list_logic_toggle")
    );

    public static final StreamCodec<FriendlyByteBuf, DeepDrawerExtractorListLogicToggleC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            DeepDrawerExtractorListLogicToggleC2SPacket::pos,
            DeepDrawerExtractorListLogicToggleC2SPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeepDrawerExtractorListLogicToggleC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = (ServerLevel) player.level();

            BlockEntity blockEntity = level.getBlockEntity(packet.pos());
            if (blockEntity instanceof DeepDrawerExtractorBlockEntity extractor) {
                extractor.setWhitelistMode(!extractor.isWhitelistMode());
                extractor.setChanged();
            }
        });
    }
}
