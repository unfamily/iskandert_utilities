package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
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
import net.unfamily.iskautils.block.entity.SmartTimerBlockEntity;

/**
 * Packet for handling Smart Timer Redstone Mode button clicks
 */
public record SmartTimerRedstoneModeC2SPacket(BlockPos pos) implements CustomPacketPayload {
    
    public static final Type<SmartTimerRedstoneModeC2SPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "smart_timer_redstone_mode")
    );
    
    public static final StreamCodec<FriendlyByteBuf, SmartTimerRedstoneModeC2SPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        SmartTimerRedstoneModeC2SPacket::pos,
        SmartTimerRedstoneModeC2SPacket::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(SmartTimerRedstoneModeC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = player.serverLevel();
            
            BlockEntity blockEntity = level.getBlockEntity(packet.pos());
            if (blockEntity instanceof SmartTimerBlockEntity timer) {
                // Cycle to next redstone mode (skip PULSE mode 3): 0->1->2->4->0
                timer.cycleRedstoneMode();
                
                // Play click sound
                level.playSound(null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.0f);
                
                // Mark the block entity as changed
                timer.setChanged();
            }
        });
    }
}
