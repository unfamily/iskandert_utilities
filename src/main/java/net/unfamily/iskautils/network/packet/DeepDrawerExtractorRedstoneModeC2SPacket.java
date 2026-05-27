package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
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

/**
 * Packet for handling Redstone Mode button clicks
 */
public record DeepDrawerExtractorRedstoneModeC2SPacket(BlockPos pos, boolean backward) implements CustomPacketPayload {

    public static final Type<DeepDrawerExtractorRedstoneModeC2SPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "deep_drawer_extractor_redstone_mode")
    );

    public static final StreamCodec<FriendlyByteBuf, DeepDrawerExtractorRedstoneModeC2SPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        DeepDrawerExtractorRedstoneModeC2SPacket::pos,
        ByteBufCodecs.BOOL,
        DeepDrawerExtractorRedstoneModeC2SPacket::backward,
        DeepDrawerExtractorRedstoneModeC2SPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeepDrawerExtractorRedstoneModeC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = player.serverLevel();

            BlockEntity blockEntity = level.getBlockEntity(packet.pos());
            if (blockEntity instanceof DeepDrawerExtractorBlockEntity extractor) {
                DeepDrawerExtractorBlockEntity.RedstoneMode currentMode =
                    DeepDrawerExtractorBlockEntity.RedstoneMode.fromValue(extractor.getRedstoneMode());
                DeepDrawerExtractorBlockEntity.RedstoneMode newMode =
                    packet.backward() ? currentMode.previous() : currentMode.next();
                extractor.setRedstoneMode(newMode.getValue());
                extractor.setChanged();
            }
        });
    }
}
