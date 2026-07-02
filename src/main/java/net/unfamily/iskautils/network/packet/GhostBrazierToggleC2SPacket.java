package net.unfamily.iskautils.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.data.GhostBrazierData;
import net.unfamily.iskautils.item.custom.GhostBrazierItem;

public record GhostBrazierToggleC2SPacket() implements CustomPacketPayload {
    public static final Type<GhostBrazierToggleC2SPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "ghost_brazier_toggle"));

    public static final StreamCodec<FriendlyByteBuf, GhostBrazierToggleC2SPacket> STREAM_CODEC =
            StreamCodec.unit(new GhostBrazierToggleC2SPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GhostBrazierToggleC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (!GhostBrazierItem.hasGhostBrazier(player)) {
                return;
            }
            GameType current = player.gameMode.getGameModeForPlayer();
            if (current == GameType.SPECTATOR) {
                GameType previous = GhostBrazierData.getPreviousGameMode(player);
                player.setGameMode(previous);
                GhostBrazierData.clearPreviousGameMode(player);
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                        "message.iska_utils.ghost_brazier.became_physical"));
            } else {
                GhostBrazierData.setPreviousGameMode(player, current);
                player.setGameMode(GameType.SPECTATOR);
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                        "message.iska_utils.ghost_brazier.became_ethereal"));
            }
        });
    }
}
