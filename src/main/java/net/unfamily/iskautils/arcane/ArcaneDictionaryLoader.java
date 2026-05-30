package net.unfamily.iskautils.arcane;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.unfamily.iskautils.data.load.IskaUtilsLoadJson;
import net.unfamily.iskautils.data.load.IskaUtilsLoadPaths;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ArcaneDictionaryLoader {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile List<ArcaneDictionaryDefinition.Entry> ENTRIES = List.of();

    private ArcaneDictionaryLoader() {}

    public static void loadAll(ResourceManager rm) {
        Map<Identifier, JsonElement> merged = IskaUtilsLoadJson.collectMergedJsonForType(
                rm, IskaUtilsLoadPaths.TYPE_ARCANE_DICTIONARY);
        List<ArcaneDictionaryDefinition.Entry> out = new ArrayList<>();
        for (var e : IskaUtilsLoadJson.orderedEntries(merged)) {
            parseFile(e.getKey(), e.getValue(), out);
        }
        ENTRIES = List.copyOf(out);
        LOGGER.info("Loaded {} Arcane Dictionary entries from datapacks", ENTRIES.size());
    }

    public static void loadAllBootstrap() {
        Map<Identifier, JsonElement> merged = IskaUtilsLoadJson.collectFromModJarOnlyForType(
                IskaUtilsLoadPaths.TYPE_ARCANE_DICTIONARY);
        List<ArcaneDictionaryDefinition.Entry> out = new ArrayList<>();
        for (var e : IskaUtilsLoadJson.orderedEntries(merged)) {
            parseFile(e.getKey(), e.getValue(), out);
        }
        ENTRIES = List.copyOf(out);
    }

    private static void parseFile(Identifier fileId, JsonElement root, List<ArcaneDictionaryDefinition.Entry> out) {
        if (!root.isJsonObject()) {
            return;
        }
        JsonObject obj = root.getAsJsonObject();
        if (!obj.has("type") || !IskaUtilsLoadPaths.TYPE_ARCANE_DICTIONARY.equals(obj.get("type").getAsString())) {
            return;
        }
        out.addAll(ArcaneDictionaryDefinition.parseEntries(obj, LOGGER, fileId.toString()));
    }

    public static List<ArcaneDictionaryDefinition.Entry> getEntries() {
        return ENTRIES;
    }

    public static ArcaneDictionaryDefinition.Entry findEntry(Identifier enchant) {
        for (ArcaneDictionaryDefinition.Entry entry : ENTRIES) {
            if (entry.enchant().equals(enchant)) {
                return entry;
            }
        }
        return null;
    }
}
