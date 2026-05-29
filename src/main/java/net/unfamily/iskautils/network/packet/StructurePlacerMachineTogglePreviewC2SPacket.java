package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.StructurePlacerMachineBlockEntity;
import net.unfamily.iskautils.network.ModMessages;

/** C2S: enable or disable structure footprint preview for one Structure Placer Machine. */
public record StructurePlacerMachineTogglePreviewC2SPacket(BlockPos pos, boolean enable) implements CustomPacketPayload {

    public static final Type<StructurePlacerMachineTogglePreviewC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "structure_placer_machine_toggle_preview"));

    public static final StreamCodec<FriendlyByteBuf, StructurePlacerMachineTogglePreviewC2SPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    StructurePlacerMachineTogglePreviewC2SPacket::pos,
                    ByteBufCodecs.BOOL,
                    StructurePlacerMachineTogglePreviewC2SPacket::enable,
                    StructurePlacerMachineTogglePreviewC2SPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StructurePlacerMachineTogglePreviewC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = (ServerLevel) player.level();
            BlockEntity blockEntity = level.getBlockEntity(packet.pos());
            if (!(blockEntity instanceof StructurePlacerMachineBlockEntity machine)) {
                return;
            }
            if (packet.enable()) {
                machine.setShowPreview(true);
                machine.setChanged();
                ModMessages.sendStructurePlacerMachineFootprint(player, level, packet.pos(), machine);
            } else {
                machine.setShowPreview(false);
                machine.setChanged();
                ModMessages.clearPreviewForBuilder(player, packet.pos());
            }
        });
    }
}
