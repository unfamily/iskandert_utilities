package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.FanBlock;
import net.unfamily.iskautils.block.entity.FanBlockEntity;
import net.unfamily.iskautils.network.ModMessages;

/**
 * Packet for showing fan area visualization
 */
public record FanShowAreaC2SPacket(BlockPos pos) implements CustomPacketPayload {
    
    public static final Type<FanShowAreaC2SPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "fan_show_area")
    );
    
    public static final StreamCodec<FriendlyByteBuf, FanShowAreaC2SPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        FanShowAreaC2SPacket::pos,
        FanShowAreaC2SPacket::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(FanShowAreaC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = player.serverLevel();
            
            BlockEntity blockEntity = level.getBlockEntity(packet.pos());
            if (blockEntity instanceof FanBlockEntity fan) {
                // Get fan facing direction
                var state = level.getBlockState(packet.pos());
                if (!(state.getBlock() instanceof FanBlock)) return;
                var facing = state.getValue(FanBlock.FACING);
                
                // Calculate the push area AABB
                var aabb = FanBlockEntity.calculatePushArea(packet.pos(), facing, fan);
                
                // Get bounds
                int minX = (int) Math.floor(aabb.minX);
                int minY = (int) Math.floor(aabb.minY);
                int minZ = (int) Math.floor(aabb.minZ);
                int maxX = (int) Math.floor(aabb.maxX);
                int maxY = (int) Math.floor(aabb.maxY);
                int maxZ = (int) Math.floor(aabb.maxZ);
                
                // Purple color for border markers when air (ARGB: 0x80FF00FF = 50% transparent purple)
                int purpleColor = 0x80FF00FF;
                // Red color for border markers when block (ARGB: 0x80FF0000 = 50% transparent red, same transparency)
                int redColor = 0x80FF0000;
                int durationTicks = 60; // 3 seconds
                
                // Add billboard markers only at the edges of the area (not inside faces)
                // Purple if air, red if block (same transparency)
                
                // Top and bottom faces - only edges (x or z at boundary)
                for (int x = minX; x < maxX; x++) {
                    for (int z = minZ; z < maxZ; z++) {
                        // Only place marker if on edge (x or z at boundary)
                        boolean isOnEdge = (x == minX || x == maxX - 1 || z == minZ || z == maxZ - 1);
                        if (isOnEdge) {
                            // Top face
                            BlockPos topPos = new BlockPos(x, maxY - 1, z);
                            int topColor = level.getBlockState(topPos).isAir() ? purpleColor : redColor;
                            ModMessages.sendAddBillboardPacket(player, topPos, topColor, durationTicks);
                            
                            // Bottom face
                            BlockPos bottomPos = new BlockPos(x, minY, z);
                            int bottomColor = level.getBlockState(bottomPos).isAir() ? purpleColor : redColor;
                            ModMessages.sendAddBillboardPacket(player, bottomPos, bottomColor, durationTicks);
                        }
                    }
                }
                
                // Front and back faces (Z faces) - only edges (x or y at boundary)
                for (int x = minX; x < maxX; x++) {
                    for (int y = minY; y < maxY; y++) {
                        // Only place marker if on edge (x or y at boundary)
                        boolean isOnEdge = (x == minX || x == maxX - 1 || y == minY || y == maxY - 1);
                        if (isOnEdge) {
                            // Min Z face
                            BlockPos minZPos = new BlockPos(x, y, minZ);
                            int minZColor = level.getBlockState(minZPos).isAir() ? purpleColor : redColor;
                            ModMessages.sendAddBillboardPacket(player, minZPos, minZColor, durationTicks);
                            
                            // Max Z face
                            BlockPos maxZPos = new BlockPos(x, y, maxZ - 1);
                            int maxZColor = level.getBlockState(maxZPos).isAir() ? purpleColor : redColor;
                            ModMessages.sendAddBillboardPacket(player, maxZPos, maxZColor, durationTicks);
                        }
                    }
                }
                
                // Left and right faces (X faces) - only edges (z or y at boundary)
                for (int z = minZ; z < maxZ; z++) {
                    for (int y = minY; y < maxY; y++) {
                        // Only place marker if on edge (z or y at boundary)
                        boolean isOnEdge = (z == minZ || z == maxZ - 1 || y == minY || y == maxY - 1);
                        if (isOnEdge) {
                            // Min X face
                            BlockPos minXPos = new BlockPos(minX, y, z);
                            int minXColor = level.getBlockState(minXPos).isAir() ? purpleColor : redColor;
                            ModMessages.sendAddBillboardPacket(player, minXPos, minXColor, durationTicks);
                            
                            // Max X face
                            BlockPos maxXPos = new BlockPos(maxX - 1, y, z);
                            int maxXColor = level.getBlockState(maxXPos).isAir() ? purpleColor : redColor;
                            ModMessages.sendAddBillboardPacket(player, maxXPos, maxXColor, durationTicks);
                        }
                    }
                }
                
                // Add red markers inside the area for blocks (obstacles)
                for (int x = minX; x < maxX; x++) {
                    for (int y = minY; y < maxY; y++) {
                        for (int z = minZ; z < maxZ; z++) {
                            BlockPos blockPos = new BlockPos(x, y, z);
                            // Only place marker if there's a block (not air)
                            if (!level.getBlockState(blockPos).isAir()) {
                                ModMessages.sendAddBillboardPacket(player, blockPos, redColor, durationTicks);
                            }
                        }
                    }
                }
            }
        });
    }
}
