package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.TemporalOverclockerBlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Packet to remove a linked block from the Temporal Overclocker
 */
public record TemporalOverclockerRemoveLinkC2SPacket(BlockPos overclockerPos, BlockPos linkedPos) implements CustomPacketPayload {
    private static final Logger LOGGER = LoggerFactory.getLogger(TemporalOverclockerRemoveLinkC2SPacket.class);
    
    public static final Type<TemporalOverclockerRemoveLinkC2SPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "temporal_overclocker_remove_link"));
    
    public static final StreamCodec<FriendlyByteBuf, TemporalOverclockerRemoveLinkC2SPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        TemporalOverclockerRemoveLinkC2SPacket::overclockerPos,
        BlockPos.STREAM_CODEC,
        TemporalOverclockerRemoveLinkC2SPacket::linkedPos,
        TemporalOverclockerRemoveLinkC2SPacket::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(TemporalOverclockerRemoveLinkC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            handlePacket(packet, player);
        });
    }
    
    private static void handlePacket(TemporalOverclockerRemoveLinkC2SPacket packet, ServerPlayer player) {
        if (player.level().getBlockEntity(packet.overclockerPos()) instanceof TemporalOverclockerBlockEntity blockEntity) {
            if (blockEntity.removeLinkedBlock(packet.linkedPos())) {
                LOGGER.debug("Removed linked block {} from Temporal Overclocker at {}", packet.linkedPos(), packet.overclockerPos());
            } else {
                LOGGER.warn("Failed to remove linked block {} from Temporal Overclocker at {}", packet.linkedPos(), packet.overclockerPos());
            }
        } else {
            LOGGER.warn("Temporal Overclocker BlockEntity not found at {}", packet.overclockerPos());
        }
    }
}

