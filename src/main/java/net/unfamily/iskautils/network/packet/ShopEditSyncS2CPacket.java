package net.unfamily.iskautils.network.packet;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.gui.ShopEditMenu;
import net.unfamily.iskautils.client.gui.ShopEditScreen;
import net.unfamily.iskautils.shop.ShopCategory;
import net.unfamily.iskautils.shop.ShopCurrency;
import net.unfamily.iskautils.shop.ShopEntry;
import net.unfamily.iskautils.shop.ShopStage;
import net.unfamily.iskautils.shop.edit.ShopEditWorkspace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Server → client full workspace snapshot for the shop editor. */
public record ShopEditSyncS2CPacket(String json) implements CustomPacketPayload {

    public static final Type<ShopEditSyncS2CPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "shop_edit_sync"));

    public static final StreamCodec<FriendlyByteBuf, ShopEditSyncS2CPacket> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> buf.writeUtf(pkt.json() != null ? pkt.json() : "{}", 2_000_000),
            buf -> new ShopEditSyncS2CPacket(buf.readUtf(2_000_000))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void sendTo(ServerPlayer player, ShopEditWorkspace.ShopEditData data) {
        PacketDistributor.sendToPlayer(player, new ShopEditSyncS2CPacket(encode(data)));
    }

    public static String encode(ShopEditWorkspace.ShopEditData data) {
        JsonObject root = new JsonObject();
        JsonArray currencies = new JsonArray();
        for (ShopCurrency c : data.currencies.values()) {
            JsonObject o = new JsonObject();
            o.addProperty("id", c.id);
            o.addProperty("name", c.name);
            o.addProperty("char_symbol", c.charSymbol);
            currencies.add(o);
        }
        root.add("currencies", currencies);

        JsonArray categories = new JsonArray();
        for (ShopCategory c : data.categories.values()) {
            JsonObject o = new JsonObject();
            o.addProperty("id", c.id);
            o.addProperty("name", c.name);
            o.addProperty("description", c.description);
            o.addProperty("item", c.item);
            o.addProperty("priority", c.priority);
            categories.add(o);
        }
        root.add("categories", categories);

        JsonArray entries = new JsonArray();
        for (ShopEntry e : data.entries.values()) {
            JsonObject o = new JsonObject();
            o.addProperty("id", e.id);
            o.addProperty("in_category", e.inCategory);
            o.addProperty("type", (e.type != null ? e.type : ShopEntry.EntryType.ITEM).name().toLowerCase(Locale.ROOT));
            if (e.item != null) {
                o.addProperty("item", e.item);
            }
            if (e.fluid != null) {
                o.addProperty("fluid", e.fluid);
            }
            if (e.gas != null) {
                o.addProperty("gas", e.gas);
            }
            o.addProperty("amount", e.amount);
            o.addProperty("currency", e.currency);
            o.addProperty("buy", e.buy);
            o.addProperty("sell", e.sell);
            o.addProperty("priority", e.priority);
            o.addProperty("free", e.free);
            if (e.stages != null && e.stages.length > 0) {
                JsonArray stages = new JsonArray();
                for (ShopStage st : e.stages) {
                    if (st == null) {
                        continue;
                    }
                    JsonObject so = new JsonObject();
                    so.addProperty("stage", st.stage);
                    so.addProperty("stage_type", st.stageType);
                    so.addProperty("is", st.is);
                    stages.add(so);
                }
                o.add("stages", stages);
            }
            entries.add(o);
        }
        root.add("entries", entries);
        return root.toString();
    }

    public static ShopEditWorkspace.ShopEditData decode(String json) {
        ShopEditWorkspace.ShopEditData data = new ShopEditWorkspace.ShopEditData();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (root.has("currencies")) {
            for (var el : root.getAsJsonArray("currencies")) {
                JsonObject o = el.getAsJsonObject();
                ShopCurrency c = new ShopCurrency();
                c.id = o.get("id").getAsString();
                c.name = o.has("name") ? o.get("name").getAsString() : c.id;
                c.charSymbol = o.has("char_symbol") ? o.get("char_symbol").getAsString() : "§";
                data.currencies.put(c.id, c);
            }
        }
        if (root.has("categories")) {
            for (var el : root.getAsJsonArray("categories")) {
                JsonObject o = el.getAsJsonObject();
                ShopCategory c = new ShopCategory();
                c.id = o.get("id").getAsString();
                c.name = o.has("name") ? o.get("name").getAsString() : c.id;
                c.description = o.has("description") ? o.get("description").getAsString() : "";
                c.item = o.has("item") ? o.get("item").getAsString() : "minecraft:stone";
                c.priority = o.has("priority") ? o.get("priority").getAsInt() : 0;
                data.categories.put(c.id, c);
            }
        }
        if (root.has("entries")) {
            for (var el : root.getAsJsonArray("entries")) {
                JsonObject o = el.getAsJsonObject();
                ShopEntry e = new ShopEntry();
                e.id = o.get("id").getAsString();
                e.inCategory = o.has("in_category") ? o.get("in_category").getAsString() : "000_default";
                String type = o.has("type") ? o.get("type").getAsString() : "item";
                e.type = switch (type.toLowerCase(Locale.ROOT)) {
                    case "fluid" -> ShopEntry.EntryType.FLUID;
                    case "gas" -> ShopEntry.EntryType.GAS;
                    default -> ShopEntry.EntryType.ITEM;
                };
                e.item = o.has("item") ? o.get("item").getAsString() : null;
                e.fluid = o.has("fluid") ? o.get("fluid").getAsString() : null;
                e.gas = o.has("gas") ? o.get("gas").getAsString() : null;
                e.amount = o.has("amount") ? o.get("amount").getAsInt() : 1;
                e.itemCount = e.amount;
                e.currency = o.has("currency") ? o.get("currency").getAsString() : "null_coin";
                e.valute = e.currency;
                e.buy = o.has("buy") ? o.get("buy").getAsDouble() : 0;
                e.sell = o.has("sell") ? o.get("sell").getAsDouble() : 0;
                e.priority = o.has("priority") ? o.get("priority").getAsInt() : 0;
                e.free = o.has("free") && o.get("free").getAsBoolean();
                if (o.has("stages") && o.get("stages").isJsonArray()) {
                    List<ShopStage> stages = new ArrayList<>();
                    for (var se : o.getAsJsonArray("stages")) {
                        JsonObject so = se.getAsJsonObject();
                        ShopStage st = new ShopStage();
                        st.stage = so.has("stage") ? so.get("stage").getAsString() : "";
                        st.stageType = so.has("stage_type") ? so.get("stage_type").getAsString() : "world";
                        st.is = !so.has("is") || so.get("is").getAsBoolean();
                        stages.add(st);
                    }
                    e.stages = stages.toArray(new ShopStage[0]);
                }
                data.entries.put(e.id, e);
            }
        }
        return data;
    }

    public static void handle(ShopEditSyncS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ShopEditWorkspace.ShopEditData data = decode(packet.json());
            if (Minecraft.getInstance().screen instanceof ShopEditScreen screen) {
                screen.applySync(data);
            } else if (Minecraft.getInstance().player != null
                    && Minecraft.getInstance().player.containerMenu instanceof ShopEditMenu menu) {
                menu.applySync(data);
            }
        });
    }
}
