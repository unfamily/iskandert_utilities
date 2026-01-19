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
 * Packet for handling Fan Push Type button clicks
 */
public record FanPushTypeC2SPacket(BlockPos pos) implements CustomPacketPayload {
    
    public static final Type<FanPushTypeC2SPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "fan_push_type")
    );
    
    public static final StreamCodec<FriendlyByteBuf, FanPushTypeC2SPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        FanPushTypeC2SPacket::pos,
        FanPushTypeC2SPacket::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(FanPushTypeC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = player.serverLevel();
            
            BlockEntity blockEntity = level.getBlockEntity(packet.pos());
            if (blockEntity instanceof FanBlockEntity fan) {
                // Cycle to next push type
                FanBlockEntity.PushType currentType = fan.getPushType();
                FanBlockEntity.PushType nextType = switch (currentType) {
                    case MOBS_ONLY -> FanBlockEntity.PushType.MOBS_AND_PLAYERS;
                    case MOBS_AND_PLAYERS -> FanBlockEntity.PushType.PLAYERS_ONLY;
                    case PLAYERS_ONLY -> FanBlockEntity.PushType.MOBS_ONLY;
                };
                fan.setPushType(nextType);
                
                // Play click sound
                level.playSound(null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.0f);
                
                // Mark the block entity as changed
                fan.setChanged();
            }
        });
    }
}
