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
import net.unfamily.iskautils.block.entity.StructurePlacerMachineBlockEntity;

/**
 * Packet for handling Set Inventory button clicks with modifiers
 */
public record StructurePlacerMachineSetInventoryC2SPacket(BlockPos pos, int mode) implements CustomPacketPayload {
    
    // Modes: 0 = normal click, 1 = shift+click, 2 = ctrl/alt+click
    public static final int MODE_NORMAL = 0;
    public static final int MODE_SHIFT = 1;
    public static final int MODE_CTRL = 2;
    
    public static final Type<StructurePlacerMachineSetInventoryC2SPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "structure_placer_machine_set_inventory")
    );
    
    public static final StreamCodec<FriendlyByteBuf, StructurePlacerMachineSetInventoryC2SPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        StructurePlacerMachineSetInventoryC2SPacket::pos,
        ByteBufCodecs.INT,
        StructurePlacerMachineSetInventoryC2SPacket::mode,
        StructurePlacerMachineSetInventoryC2SPacket::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(StructurePlacerMachineSetInventoryC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = player.serverLevel();
            
            BlockEntity blockEntity = level.getBlockEntity(packet.pos());
            if (blockEntity instanceof StructurePlacerMachineBlockEntity machine) {
                
                // Execute the appropriate action based on mode
                switch (packet.mode()) {
                    case MODE_NORMAL -> {
                        machine.setInventoryFilters();
                        // Play a soft click sound
                        level.playSound(null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.0f);
                    }
                    case MODE_SHIFT -> {
                        machine.clearAllFilters();
                        // Play a different sound for clearing
                        level.playSound(null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 0.8f);
                    }
                    case MODE_CTRL -> {
                        machine.clearEmptyFilters();
                        // Play a third sound for partial clearing
                        level.playSound(null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 0.9f);
                    }
                }
                
                // Mark the block entity as changed
                machine.setChanged();
            }
        });
    }
} 