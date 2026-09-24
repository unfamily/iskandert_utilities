package net.unfamily.iskautils.network.packet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import org.jetbrains.annotations.Nullable;

/**
 * A4: Client → server packet to persist player shop UI preferences in player NBT
 * (key: {@code iska_utils_shop_ui}).
 */
public record ShopUiPrefsC2SPacket(
        String scope,
        @Nullable String currencyFilter,
        String tradeVisibility,
        String sortMode
) implements CustomPacketPayload {

    public static final Type<ShopUiPrefsC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "shop_ui_prefs"));

    public static final StreamCodec<FriendlyByteBuf, ShopUiPrefsC2SPacket> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeUtf(pkt.scope(), 64);
                buf.writeNullable(pkt.currencyFilter(), FriendlyByteBuf::writeUtf);
                buf.writeUtf(pkt.tradeVisibility(), 64);
                buf.writeUtf(pkt.sortMode(), 64);
            },
            buf -> new ShopUiPrefsC2SPacket(
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

    private static final String NBT_KEY = "iska_utils_shop_ui";

    public static void handle(ShopUiPrefsC2SPacket pkt, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            CompoundTag prefs = player.getPersistentData().getCompound(NBT_KEY);
            prefs.putString("scope", pkt.scope());
            if (pkt.currencyFilter() != null) {
                prefs.putString("currency_filter", pkt.currencyFilter());
            } else {
                prefs.remove("currency_filter");
            }
            prefs.putString("trade_visibility", pkt.tradeVisibility());
            prefs.putString("sort_mode", pkt.sortMode());
            player.getPersistentData().put(NBT_KEY, prefs);
        });
    }

    /** Read saved prefs from player NBT and return as packet, or null if none saved. */
    @Nullable
    public static ShopUiPrefsC2SPacket fromPlayerNbt(ServerPlayer player) {
        CompoundTag playerData = player.getPersistentData();
        if (!playerData.contains(NBT_KEY)) return null;
        CompoundTag prefs = playerData.getCompound(NBT_KEY);
        String scope = prefs.contains("scope") ? prefs.getString("scope") : "ALL";
        String cf = prefs.contains("currency_filter") ? prefs.getString("currency_filter") : null;
        String tv = prefs.contains("trade_visibility") ? prefs.getString("trade_visibility") : "SHOW";
        String sm = prefs.contains("sort_mode") ? prefs.getString("sort_mode") : "PRIORITY";
        return new ShopUiPrefsC2SPacket(scope, cf, tv, sm);
    }
}
