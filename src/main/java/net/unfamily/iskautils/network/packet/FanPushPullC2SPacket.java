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
import net.unfamily.iskautils.block.entity.FanBlockEntity;

/**
 * Packet for handling Fan Push/Pull button clicks
 */
public record FanPushPullC2SPacket(BlockPos pos) implements CustomPacketPayload {
    
    public static final Type<FanPushPullC2SPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "fan_push_pull")
    );
    
    public static final StreamCodec<FriendlyByteBuf, FanPushPullC2SPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        FanPushPullC2SPacket::pos,
        FanPushPullC2SPacket::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(FanPushPullC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = player.serverLevel();
            
            BlockEntity blockEntity = level.getBlockEntity(packet.pos());
            if (blockEntity instanceof FanBlockEntity fan) {
                // Toggle push/pull
                fan.setPull(!fan.isPull());
                
                // Play click sound
                level.playSound(null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.0f);
                
                // Mark the block entity as changed
                fan.setChanged();
            }
        });
    }
}
