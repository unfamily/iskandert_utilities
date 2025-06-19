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
import net.unfamily.iskautils.block.entity.StructurePlacerMachineBlockEntity;

/**
 * Packet for handling Redstone Mode button clicks
 */
public record StructurePlacerMachineRedstoneModeC2SPacket(BlockPos pos) implements CustomPacketPayload {
    
    public static final Type<StructurePlacerMachineRedstoneModeC2SPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "structure_placer_machine_redstone_mode")
    );
    
    public static final StreamCodec<FriendlyByteBuf, StructurePlacerMachineRedstoneModeC2SPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        StructurePlacerMachineRedstoneModeC2SPacket::pos,
        StructurePlacerMachineRedstoneModeC2SPacket::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(StructurePlacerMachineRedstoneModeC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = player.serverLevel();
            
            BlockEntity blockEntity = level.getBlockEntity(packet.pos());
            if (blockEntity instanceof StructurePlacerMachineBlockEntity machine) {
                
                // Cycle to next redstone mode
                StructurePlacerMachineBlockEntity.RedstoneMode currentMode = StructurePlacerMachineBlockEntity.RedstoneMode.fromValue(machine.getRedstoneMode());
                StructurePlacerMachineBlockEntity.RedstoneMode nextMode = currentMode.next();
                machine.setRedstoneMode(nextMode.getValue());
                
                // Play click sound
                level.playSound(null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.0f);
                
                // Mark the block entity as changed
                machine.setChanged();
            }
        });
    }
} 