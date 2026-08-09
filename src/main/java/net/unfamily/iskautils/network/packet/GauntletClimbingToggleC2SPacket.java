package net.unfamily.iskautils.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.item.custom.GauntletOfClimbingItem;
import net.unfamily.iskautils.util.CurioEquipUtil;

/** C2S: set climbing enabled to an absolute value (client already applied the same state). */
public record GauntletClimbingToggleC2SPacket(boolean enabled) implements CustomPacketPayload {
    public static final Type<GauntletClimbingToggleC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "gauntlet_climbing_toggle"));

    public static final StreamCodec<FriendlyByteBuf, GauntletClimbingToggleC2SPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, GauntletClimbingToggleC2SPacket::enabled,
                    GauntletClimbingToggleC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GauntletClimbingToggleC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (CurioEquipUtil.findActiveStack(player, ModItems.GAUNTLET_OF_CLIMBING.get()).isEmpty()) {
                return;
            }
            GauntletOfClimbingItem.setClimbingEnabled(player.getUUID(), packet.enabled());
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.iska_utils.gauntlet_climbing_toggle." + (packet.enabled() ? "enabled" : "disabled")), true);
        });
    }
}
