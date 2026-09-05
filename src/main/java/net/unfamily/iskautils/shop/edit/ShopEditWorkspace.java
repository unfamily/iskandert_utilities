package net.unfamily.iskautils.shop.edit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.shop.ShopCategory;
import net.unfamily.iskautils.shop.ShopCurrency;
import net.unfamily.iskautils.shop.ShopEntry;
import net.unfamily.iskautils.shop.ShopStage;
import net.unfamily.iskautils.util.ModLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads/writes the three shop editor workspace JSON files under {@link Config#shopEditWorkPath}.
 */
public final class ShopEditWorkspace {

    private static final ModLogger LOGGER = ModLogger.of(ShopEditWorkspace.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static final String CURRENCIES_FILE = "default_currencies.json";
    public static final String CATEGORIES_FILE = "default_categories.json";
    public static final String ENTRIES_FILE = "default_entries.json";

    /** Classpath location of jar shop defaults (categories/entries). Currencies come from Library at the same path. */
    private static final String JAR_DEFAULT_DIR = "/data/iska_utils/load/iska_utils_shop/";

    private ShopEditWorkspace() {}

    public static Path resolveWorkDir(MinecraftServer server) {
        String configured = Config.shopEditWorkPath;
        if (configured == null || configured.isBlank()) {
            configured = "kubejs/data/iska_utils/load/iska_utils_shop";
        }
        Path path = Path.of(configured.trim());
        if (path.isAbsolute()) {
            return path.normalize();
        }
        // Match KubeJS datapack layout: relative to game directory, not the world folder.
        return net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().resolve(path).normalize();
    }

    public static void ensureBootstrap(MinecraftServer server) throws IOException {
        Path dir = resolveWorkDir(server);
        Files.createDirectories(dir);
        ensureFileFromJarOrEmpty(dir.resolve(CURRENCIES_FILE), CURRENCIES_FILE, ShopEditWorkspace::writeEmptyCurrencies);
        ensureFileFromJarOrEmpty(dir.resolve(CATEGORIES_FILE), CATEGORIES_FILE, ShopEditWorkspace::writeEmptyCategories);
        ensureFileFromJarOrEmpty(dir.resolve(ENTRIES_FILE), ENTRIES_FILE, ShopEditWorkspace::writeEmptyEntries);
    }

    @FunctionalInterface
    private interface EmptyWriter {
        void write(Path file) throws IOException;
    }

    private static void ensureFileFromJarOrEmpty(Path target, String fileName, EmptyWriter emptyFallback) throws IOException {
        if (Files.exists(target)) {
            return;
        }
        if (copyJarDefault(target, fileName)) {
            LOGGER.info("Bootstrapped shop edit workspace from jar default: {}", target);
            return;
        }
        LOGGER.warn("Jar default {} missing; writing empty shop file at {}", fileName, target);
        emptyFallback.write(target);
    }

    /**
     * Copies the mod's bundled default JSON into the workspace when the file is absent.
     *
     * @return true if the jar resource was copied
     */
    private static boolean copyJarDefault(Path target, String fileName) throws IOException {
        String resourcePath = JAR_DEFAULT_DIR + fileName;
        InputStream stream = ShopEditWorkspace.class.getResourceAsStream(resourcePath);
        if (stream == null && CURRENCIES_FILE.equals(fileName)) {
            // Built-in currencies live in Library jar at the same data/iska_utils/… path.
            stream = net.unfamily.iskalib.IskaLib.class.getResourceAsStream(resourcePath);
        }
        if (stream == null) {
            return false;
        }
        try (InputStream in = stream) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target);
            return true;
        }
    }

    public static ShopEditData load(MinecraftServer server) throws IOException {
        ensureBootstrap(server);
        Path dir = resolveWorkDir(server);
        ShopEditData data = new ShopEditData();
        data.currencies.putAll(readCurrencies(dir.resolve(CURRENCIES_FILE)));
        data.categories.putAll(readCategories(dir.resolve(CATEGORIES_FILE)));
        data.entries.putAll(readEntries(dir.resolve(ENTRIES_FILE)));
        return data;
    }

    public static void saveCurrencies(MinecraftServer server, Map<String, ShopCurrency> currencies) throws IOException {
        ensureBootstrap(server);
        writeCurrencies(resolveWorkDir(server).resolve(CURRENCIES_FILE), currencies);
    }

    public static void saveCategories(MinecraftServer server, Map<String, ShopCategory> categories) throws IOException {
        ensureBootstrap(server);
        writeCategories(resolveWorkDir(server).resolve(CATEGORIES_FILE), categories);
    }

    public static void saveEntries(MinecraftServer server, Map<String, ShopEntry> entries) throws IOException {
        ensureBootstrap(server);
        writeEntries(resolveWorkDir(server).resolve(ENTRIES_FILE), entries);
    }

    public static void writeEmptyCurrencies(Path file) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("type", "iska_lib:shop_currency");
        root.add("currencies", new JsonArray());
        writeJson(file, root);
    }

    public static void writeEmptyCategories(Path file) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("type", "iska_utils:shop_category");
        root.add("categories", new JsonArray());
        writeJson(file, root);
    }

    public static void writeEmptyEntries(Path file) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("type", "iska_utils:shop_entry");
        root.add("entries", new JsonArray());
        writeJson(file, root);
    }

    private static Map<String, ShopCurrency> readCurrencies(Path file) throws IOException {
        Map<String, ShopCurrency> map = new LinkedHashMap<>();
        JsonObject root = readJsonObject(file);
        if (root == null || !root.has("currencies") || !root.get("currencies").isJsonArray()) {
            return map;
        }
        for (JsonElement el : root.getAsJsonArray("currencies")) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject o = el.getAsJsonObject();
            String id = stringOrNull(o, "id");
            if (id == null || id.isBlank()) {
                continue;
            }
            ShopCurrency c = new ShopCurrency();
            c.id = id;
            c.name = stringOr(o, "name", id);
            c.charSymbol = stringOr(o, "char_symbol", ShopCurrency.DEFAULT_SYMBOL);
            c.priority = o.has("priority") ? o.get("priority").getAsInt() : 0;
            map.put(id, c);
        }
        return map;
    }

    private static Map<String, ShopCategory> readCategories(Path file) throws IOException {
        Map<String, ShopCategory> map = new LinkedHashMap<>();
        JsonObject root = readJsonObject(file);
        if (root == null || !root.has("categories") || !root.get("categories").isJsonArray()) {
            return map;
        }
        for (JsonElement el : root.getAsJsonArray("categories")) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject o = el.getAsJsonObject();
            String id = stringOrNull(o, "id");
            if (id == null || id.isBlank()) {
                continue;
            }
            ShopCategory c = new ShopCategory();
            c.id = id;
            c.name = stringOr(o, "name", id);
            c.description = stringOr(o, "description", "");
            c.item = stringOr(o, "item", "minecraft:stone");
            c.priority = o.has("priority") ? o.get("priority").getAsInt() : 0;
            map.put(id, c);
        }
        return map;
    }

    private static Map<String, ShopEntry> readEntries(Path file) throws IOException {
        Map<String, ShopEntry> map = new LinkedHashMap<>();
        JsonObject root = readJsonObject(file);
        if (root == null || !root.has("entries") || !root.get("entries").isJsonArray()) {
            return map;
        }
        for (JsonElement el : root.getAsJsonArray("entries")) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject o = el.getAsJsonObject();
            String id = stringOrNull(o, "id");
            if (id == null || id.isBlank()) {
                continue;
            }
            ShopEntry e = new ShopEntry();
            e.id = id;
            e.inCategory = stringOr(o, "in_category", "000_default");
            e.type = parseType(stringOr(o, "type", "item"));
            e.item = stringOrNull(o, "item");
            e.fluid = stringOrNull(o, "fluid");
            e.gas = stringOrNull(o, "gas");
            e.other = stringOrNull(o, "other");
            if (o.has("amount")) {
                e.amount = o.get("amount").getAsInt();
            } else if (o.has("item_count")) {
                e.amount = o.get("item_count").getAsInt();
            } else {
                e.amount = 1;
            }
            e.itemCount = e.amount;
            e.currency = o.has("currency") ? o.get("currency").getAsString()
                    : (o.has("valute") ? o.get("valute").getAsString() : "null_coin");
            e.valute = e.currency;
            e.buy = o.has("buy") ? o.get("buy").getAsDouble() : 0;
            e.sell = o.has("sell") ? o.get("sell").getAsDouble() : 0;
            e.priority = o.has("priority") ? o.get("priority").getAsInt() : 0;
            e.free = o.has("free") && o.get("free").getAsBoolean();
            if (o.has("stages") && o.get("stages").isJsonArray()) {
                List<ShopStage> stages = new ArrayList<>();
                for (JsonElement se : o.getAsJsonArray("stages")) {
                    if (!se.isJsonObject()) {
                        continue;
                    }
                    JsonObject so = se.getAsJsonObject();
                    ShopStage st = new ShopStage();
                    st.stage = stringOr(so, "stage", "");
                    st.stageType = stringOr(so, "stage_type", "world");
                    st.is = !so.has("is") || so.get("is").getAsBoolean();
                    stages.add(st);
                }
                e.stages = stages.toArray(new ShopStage[0]);
            }
            map.put(id, e);
        }
        return map;
    }

    private static void writeCurrencies(Path file, Map<String, ShopCurrency> currencies) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("type", "iska_lib:shop_currency");
        JsonArray arr = new JsonArray();
        for (ShopCurrency c : currencies.values()) {
            JsonObject o = new JsonObject();
            o.addProperty("id", c.id);
            o.addProperty("name", c.name != null ? c.name : c.id);
            o.addProperty("char_symbol", c.charSymbol != null ? c.charSymbol : ShopCurrency.DEFAULT_SYMBOL);
            o.addProperty("priority", c.priority);
            arr.add(o);
        }
        root.add("currencies", arr);
        writeJson(file, root);
    }

    private static void writeCategories(Path file, Map<String, ShopCategory> categories) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("type", "iska_utils:shop_category");
        JsonArray arr = new JsonArray();
        for (ShopCategory c : categories.values()) {
            JsonObject o = new JsonObject();
            o.addProperty("id", c.id);
            o.addProperty("name", c.name != null ? c.name : c.id);
            o.addProperty("description", c.description != null ? c.description : "");
            o.addProperty("item", c.item != null ? c.item : "minecraft:stone");
            o.addProperty("priority", c.priority);
            arr.add(o);
        }
        root.add("categories", arr);
        writeJson(file, root);
    }

    private static void writeEntries(Path file, Map<String, ShopEntry> entries) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("type", "iska_utils:shop_entry");
        JsonArray arr = new JsonArray();
        for (ShopEntry e : entries.values()) {
            JsonObject o = new JsonObject();
            o.addProperty("id", e.id);
            o.addProperty("in_category", e.inCategory != null ? e.inCategory : "000_default");
            ShopEntry.EntryType type = e.type != null ? e.type : ShopEntry.EntryType.ITEM;
            o.addProperty("type", type.name().toLowerCase(Locale.ROOT));
            switch (type) {
                case FLUID -> {
                    if (e.fluid != null) {
                        o.addProperty("fluid", e.fluid);
                    }
                }
                case GAS -> {
                    if (e.gas != null) {
                        o.addProperty("gas", e.gas);
                    }
                }
                case OTHER -> {
                    if (e.other != null) {
                        o.addProperty("other", e.other);
                    }
                }
                default -> {
                    if (e.item != null) {
                        o.addProperty("item", e.item);
                    }
                }
            }
            o.addProperty("amount", Math.max(1, e.amount));
            o.addProperty("currency", e.currency != null ? e.currency : "null_coin");
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
                    so.addProperty("stage", st.stage != null ? st.stage : "");
                    so.addProperty("stage_type", st.stageType != null ? st.stageType : "world");
                    so.addProperty("is", st.is);
                    stages.add(so);
                }
                o.add("stages", stages);
            }
            arr.add(o);
        }
        root.add("entries", arr);
        writeJson(file, root);
    }

    private static ShopEntry.EntryType parseType(String raw) {
        if (raw == null) {
            return ShopEntry.EntryType.ITEM;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "fluid" -> ShopEntry.EntryType.FLUID;
            case "gas" -> ShopEntry.EntryType.GAS;
            case "other" -> ShopEntry.EntryType.OTHER;
            default -> ShopEntry.EntryType.ITEM;
        };
    }

    private static JsonObject readJsonObject(Path file) throws IOException {
        if (!Files.exists(file)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement el = JsonParser.parseReader(reader);
            return el != null && el.isJsonObject() ? el.getAsJsonObject() : null;
        } catch (Exception e) {
            LOGGER.warn("Failed to read {}: {}", file, e.toString());
            return null;
        }
    }

    private static void writeJson(Path file, JsonObject root) throws IOException {
        Files.createDirectories(file.getParent());
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
    }

    private static String stringOrNull(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : null;
    }

    private static String stringOr(JsonObject o, String key, String def) {
        String v = stringOrNull(o, key);
        return v != null ? v : def;
    }

    /** In-memory workspace snapshot. */
    public static final class ShopEditData {
        public final Map<String, ShopCurrency> currencies = new LinkedHashMap<>();
        public final Map<String, ShopCategory> categories = new LinkedHashMap<>();
        public final Map<String, ShopEntry> entries = new LinkedHashMap<>();
    }
}
