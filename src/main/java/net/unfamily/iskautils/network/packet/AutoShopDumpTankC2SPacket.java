package net.unfamily.iskautils.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.AutoShopBlockEntity;
import net.unfamily.iskautils.client.gui.AutoShopMenu;

public record AutoShopDumpTankC2SPacket(BlockPos pos, boolean gas) implements CustomPacketPayload {
    public static final Type<AutoShopDumpTankC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "auto_shop_dump_tank"));

    public static final StreamCodec<FriendlyByteBuf, AutoShopDumpTankC2SPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, AutoShopDumpTankC2SPacket::pos,
                    ByteBufCodecs.BOOL, AutoShopDumpTankC2SPacket::gas,
                    AutoShopDumpTankC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AutoShopDumpTankC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (!(player.level().getBlockEntity(packet.pos()) instanceof AutoShopBlockEntity autoShop)
                    || !autoShop.canPlayerUse(player)) {
                return;
            }
            if (packet.gas()) {
                autoShop.dumpGasTankContents();
            } else {
                autoShop.dumpFluidTankContents();
            }
            if (player.containerMenu instanceof AutoShopMenu menu
                    && menu.getBlockPos().equals(packet.pos())) {
                menu.broadcastFullState();
            }
        });
    }
}
