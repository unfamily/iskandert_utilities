package net.unfamily.iskautils.network.packet;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.gui.ShopEditMenu;
import net.unfamily.iskautils.shop.ShopCategory;
import net.unfamily.iskautils.shop.ShopCurrency;
import net.unfamily.iskautils.shop.ShopEntry;
import net.unfamily.iskautils.shop.ShopStage;
import net.unfamily.iskautils.shop.edit.ShopEditSession;
import net.unfamily.iskautils.shop.edit.ShopEditWorkspace;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Client → server shop editor mutation. Server autosaves and replies with {@link ShopEditSyncS2CPacket}.
 */
public record ShopEditActionC2SPacket(String action, String payloadJson) implements CustomPacketPayload {

    public static final Type<ShopEditActionC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "shop_edit_action"));

    public static final StreamCodec<FriendlyByteBuf, ShopEditActionC2SPacket> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeUtf(pkt.action(), 64);
                buf.writeUtf(pkt.payloadJson() != null ? pkt.payloadJson() : "{}", 1_000_000);
            },
            buf -> new ShopEditActionC2SPacket(buf.readUtf(64), buf.readUtf(1_000_000))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ShopEditActionC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!ShopEditSession.isHolder(player) || !(player.containerMenu instanceof ShopEditMenu)) {
                return;
            }
            ShopEditWorkspace.ShopEditData data = ShopEditSession.getData();
            if (data == null) {
                return;
            }
            try {
                apply(player, data, packet.action(), packet.payloadJson());
            } catch (Exception ignored) {
            }
            ShopEditSyncS2CPacket.sendTo(player, data);
        });
    }

    private static void apply(ServerPlayer player, ShopEditWorkspace.ShopEditData data, String action, String payloadJson) {
        JsonObject payload = JsonParser.parseString(payloadJson).getAsJsonObject();
        switch (action) {
            case "upsert_currency" -> {
                ShopCurrency c = readCurrency(payload);
                if (c.id == null || c.id.isBlank()) {
                    return;
                }
                String oldId = payload.has("old_id") ? payload.get("old_id").getAsString() : c.id;
                if (!oldId.equals(c.id) && data.currencies.containsKey(oldId)) {
                    data.currencies.remove(oldId);
                }
                data.currencies.put(c.id, c);
                ShopEditSession.autosaveCurrencies(player.server);
                handleRename(data, "currency", oldId, c.id, payload, player);
            }
            case "delete_currency" -> {
                String id = payload.get("id").getAsString();
                data.currencies.remove(id);
                ShopEditSession.autosaveCurrencies(player.server);
            }
            case "upsert_category" -> {
                ShopCategory c = readCategory(payload);
                if (c.id == null || c.id.isBlank()) {
                    return;
                }
                String oldId = payload.has("old_id") ? payload.get("old_id").getAsString() : c.id;
                if (!oldId.equals(c.id) && data.categories.containsKey(oldId)) {
                    data.categories.remove(oldId);
                }
                data.categories.put(c.id, c);
                ShopEditSession.autosaveCategories(player.server);
                handleRename(data, "category", oldId, c.id, payload, player);
            }
            case "delete_category" -> {
                String id = payload.get("id").getAsString();
                data.categories.remove(id);
                ShopEditSession.autosaveCategories(player.server);
            }
            case "upsert_entry" -> {
                ShopEntry e = readEntry(payload);
                if (e.id == null || e.id.isBlank()) {
                    return;
                }
                String oldId = payload.has("old_id") ? payload.get("old_id").getAsString() : e.id;
                if (!oldId.equals(e.id) && data.entries.containsKey(oldId)) {
                    data.entries.remove(oldId);
                }
                data.entries.put(e.id, e);
                ShopEditSession.autosaveEntries(player.server);
            }
            case "delete_entry" -> {
                String id = payload.get("id").getAsString();
                data.entries.remove(id);
                ShopEditSession.autosaveEntries(player.server);
            }
            case "rename_resolve" -> {
                String kind = payload.get("kind").getAsString();
                String oldId = payload.get("old_id").getAsString();
                String newId = payload.get("new_id").getAsString();
                String mode = payload.get("mode").getAsString();
                applyRenameResolve(data, kind, oldId, newId, mode, player);
            }
            default -> {
            }
        }
    }

    private static void handleRename(
            ShopEditWorkspace.ShopEditData data,
            String kind,
            String oldId,
            String newId,
            JsonObject payload,
            ServerPlayer player) {
        if (oldId == null || newId == null || oldId.equals(newId)) {
            return;
        }
        if (!payload.has("rename_mode")) {
            return;
        }
        applyRenameResolve(data, kind, oldId, newId, payload.get("rename_mode").getAsString(), player);
    }

    private static void applyRenameResolve(
            ShopEditWorkspace.ShopEditData data,
            String kind,
            String oldId,
            String newId,
            String mode,
            ServerPlayer player) {
        if (oldId.equals(newId)) {
            return;
        }
        if ("category".equals(kind)) {
            if ("propagate".equals(mode)) {
                for (ShopEntry e : data.entries.values()) {
                    if (oldId.equals(e.inCategory)) {
                        e.inCategory = newId;
                    }
                }
                ShopEditSession.autosaveEntries(player.server);
            } else if ("delete".equals(mode)) {
                Iterator<Map.Entry<String, ShopEntry>> it = data.entries.entrySet().iterator();
                while (it.hasNext()) {
                    if (oldId.equals(it.next().getValue().inCategory)) {
                        it.remove();
                    }
                }
                ShopEditSession.autosaveEntries(player.server);
            }
        } else if ("currency".equals(kind)) {
            if ("propagate".equals(mode)) {
                for (ShopEntry e : data.entries.values()) {
                    if (oldId.equals(e.currency) || oldId.equals(e.valute)) {
                        e.currency = newId;
                        e.valute = newId;
                    }
                }
                ShopEditSession.autosaveEntries(player.server);
            } else if ("delete".equals(mode)) {
                Iterator<Map.Entry<String, ShopEntry>> it = data.entries.entrySet().iterator();
                while (it.hasNext()) {
                    ShopEntry e = it.next().getValue();
                    if (oldId.equals(e.currency) || oldId.equals(e.valute)) {
                        it.remove();
                    }
                }
                ShopEditSession.autosaveEntries(player.server);
            }
        }
    }

    private static ShopCurrency readCurrency(JsonObject o) {
        ShopCurrency c = new ShopCurrency();
        c.id = o.has("id") ? o.get("id").getAsString() : "";
        c.name = o.has("name") ? o.get("name").getAsString() : c.id;
        c.charSymbol = o.has("char_symbol") ? o.get("char_symbol").getAsString() : ShopCurrency.DEFAULT_SYMBOL;
        c.priority = o.has("priority") ? o.get("priority").getAsInt() : 0;
        return c;
    }

    private static ShopCategory readCategory(JsonObject o) {
        ShopCategory c = new ShopCategory();
        c.id = o.has("id") ? o.get("id").getAsString() : "";
        c.name = o.has("name") ? o.get("name").getAsString() : c.id;
        c.description = o.has("description") ? o.get("description").getAsString() : "";
        c.item = o.has("item") ? o.get("item").getAsString() : "minecraft:stone";
        c.priority = o.has("priority") ? o.get("priority").getAsInt() : 0;
        return c;
    }

    private static ShopEntry readEntry(JsonObject o) {
        ShopEntry e = new ShopEntry();
        e.id = o.has("id") ? o.get("id").getAsString() : "";
        e.inCategory = o.has("in_category") ? o.get("in_category").getAsString() : "000_default";
        String type = o.has("type") ? o.get("type").getAsString() : "item";
        e.type = switch (type.toLowerCase(Locale.ROOT)) {
            case "fluid" -> ShopEntry.EntryType.FLUID;
            case "gas" -> ShopEntry.EntryType.GAS;
            case "other" -> ShopEntry.EntryType.OTHER;
            default -> ShopEntry.EntryType.ITEM;
        };
        e.item = o.has("item") ? o.get("item").getAsString() : null;
        e.fluid = o.has("fluid") ? o.get("fluid").getAsString() : null;
        e.gas = o.has("gas") ? o.get("gas").getAsString() : null;
        e.other = o.has("other") ? o.get("other").getAsString() : null;
        e.amount = o.has("amount") ? Math.max(1, o.get("amount").getAsInt()) : 1;
        e.itemCount = e.amount;
        e.currency = o.has("currency") ? o.get("currency").getAsString() : "null_coin";
        e.valute = e.currency;
        e.buy = o.has("buy") ? o.get("buy").getAsDouble() : 0;
        e.sell = o.has("sell") ? o.get("sell").getAsDouble() : 0;
        e.priority = o.has("priority") ? o.get("priority").getAsInt() : 0;
        e.free = o.has("free") && o.get("free").getAsBoolean();
        if (o.has("stages") && o.get("stages").isJsonArray()) {
            List<ShopStage> stages = new ArrayList<>();
            for (var el : o.getAsJsonArray("stages")) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonObject so = el.getAsJsonObject();
                ShopStage st = new ShopStage();
                st.stage = so.has("stage") ? so.get("stage").getAsString() : "";
                st.stageType = so.has("stage_type") ? so.get("stage_type").getAsString() : "world";
                st.is = !so.has("is") || so.get("is").getAsBoolean();
                stages.add(st);
            }
            e.stages = stages.toArray(new ShopStage[0]);
        }
        return e;
    }
}
