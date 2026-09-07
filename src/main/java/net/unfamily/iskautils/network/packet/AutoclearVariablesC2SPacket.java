package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.ImprovedPatternCrafterBlockEntity;

/** Client-to-Server packet: toggles unused-variable autoclear on Pattern Crafter close. */
public record AutoclearVariablesC2SPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<AutoclearVariablesC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "pc_autoclear_variables")
    );

    public static final StreamCodec<FriendlyByteBuf, AutoclearVariablesC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AutoclearVariablesC2SPacket::pos,
            AutoclearVariablesC2SPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AutoclearVariablesC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            BlockEntity be = player.serverLevel().getBlockEntity(packet.pos());
            if (be instanceof ImprovedPatternCrafterBlockEntity pcbe) {
                pcbe.toggleAutoclearVariables();
            }
        });
    }
}
