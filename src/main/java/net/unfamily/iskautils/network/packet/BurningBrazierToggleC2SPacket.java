package net.unfamily.iskautils.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.data.BurningBrazierData;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.util.CurioEquipUtil;

public record BurningBrazierToggleC2SPacket() implements CustomPacketPayload {
    public static final Type<BurningBrazierToggleC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "burning_brazier_toggle"));

    public static final StreamCodec<FriendlyByteBuf, BurningBrazierToggleC2SPacket> STREAM_CODEC =
            StreamCodec.unit(new BurningBrazierToggleC2SPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BurningBrazierToggleC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (CurioEquipUtil.findActiveStack(player, ModItems.BURNING_BRAZIER.get()).isEmpty()) {
                return;
            }
            boolean newState = !BurningBrazierData.getAutoPlacementEnabledFromPlayer(player);
            BurningBrazierData.setAutoPlacementEnabledToPlayer(player, newState);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.iska_utils.burning_flames.auto_placement." + (newState ? "enabled" : "disabled")), true);
        });
    }
}
