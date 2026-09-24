package net.unfamily.iskautils.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.gui.ShopClientPrefs;
import org.jetbrains.annotations.Nullable;

/**
 * A4: Server → client: send player's saved shop UI preferences on shop open.
 */
public record ShopUiPrefsS2CPacket(
        String scope,
        @Nullable String currencyFilter,
        String tradeVisibility,
        String sortMode
) implements CustomPacketPayload {

    public static final Type<ShopUiPrefsS2CPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "shop_ui_prefs_s2c"));

    public static final StreamCodec<FriendlyByteBuf, ShopUiPrefsS2CPacket> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeUtf(pkt.scope(), 64);
                buf.writeNullable(pkt.currencyFilter(), FriendlyByteBuf::writeUtf);
                buf.writeUtf(pkt.tradeVisibility(), 64);
                buf.writeUtf(pkt.sortMode(), 64);
            },
            buf -> new ShopUiPrefsS2CPacket(
                    buf.readUtf(64),
                    buf.readNullable(FriendlyByteBuf::readUtf),
                    buf.readUtf(64),
                    buf.readUtf(64)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void sendTo(ServerPlayer player) {
        ShopUiPrefsC2SPacket saved = ShopUiPrefsC2SPacket.fromPlayerNbt(player);
        if (saved == null) return; // no saved prefs, client keeps defaults
        PacketDistributor.sendToPlayer(player, new ShopUiPrefsS2CPacket(
                saved.scope(), saved.currencyFilter(), saved.tradeVisibility(), saved.sortMode()));
    }

    public static void handle(ShopUiPrefsS2CPacket pkt, IPayloadContext context) {
        context.enqueueWork(() ->
                ShopClientPrefs.applyFromServer(
                        pkt.scope(), pkt.currencyFilter(), pkt.tradeVisibility(), pkt.sortMode()));
    }
}
