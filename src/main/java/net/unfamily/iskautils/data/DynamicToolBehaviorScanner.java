package net.unfamily.iskautils.data;

import net.unfamily.iskautils.util.ModLogger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.unfamily.iskautils.data.load.IskaUtilsLoadJson;
import net.unfamily.iskautils.data.load.IskaUtilsLoadPaths;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads AOE tool behaviors from {@code load/iska_utils_tools/} and {@code iska_utils/tools/}.
 * JSON binds an existing item id to lumberjack, excavator, scythe, or paxel behavior.
 */
public final class DynamicToolBehaviorScanner {
    private static final ModLogger LOGGER = ModLogger.of(DynamicToolBehaviorScanner.class);
    private static final String TOOLS_DATA_DIR = "iska_utils/tools";

    private static final Map<Identifier, ToolBehaviorDefinition> BY_ITEM = new HashMap<>();

    private DynamicToolBehaviorScanner() {}

    public static void loadAll(ResourceManager resourceManagerOrNull) {
        BY_ITEM.clear();
        Map<Identifier, JsonElement> merged = new HashMap<>();
        if (resourceManagerOrNull != null) {
            merged.putAll(IskaUtilsLoadJson.collectMergedJsonForSubdir(resourceManagerOrNull, IskaUtilsLoadPaths.TOOLS));
            merged.putAll(IskaUtilsLoadJson.collectMergedJsonUnderDirectory(
                    resourceManagerOrNull, TOOLS_DATA_DIR, id -> id.getPath().endsWith(".json")));
        } else {
            merged.putAll(IskaUtilsLoadJson.collectFromModJarOnly(IskaUtilsLoadPaths.TOOLS));
            merged.putAll(IskaUtilsLoadJson.collectFromModJarOnlyUnderDataDir(
                    TOOLS_DATA_DIR, id -> id.getPath().endsWith(".json")));
        }
        for (var e : IskaUtilsLoadJson.orderedEntries(merged)) {
            parseRoot(e.getKey().toString(), e.getValue());
        }
        LOGGER.info("Tool behavior configurations loaded: {}", BY_ITEM.size());
    }

    public static void loadAllBootstrap() {
        loadAll(null);
    }

    public static ToolBehaviorDefinition getForItem(Identifier itemId) {
        return BY_ITEM.get(itemId);
    }

    public static Map<Identifier, ToolBehaviorDefinition> getAllByItem() {
        return Map.copyOf(BY_ITEM);
    }

    private static void parseRoot(String source, JsonElement root) {
        if (root == null || !root.isJsonObject()) {
            return;
        }
        JsonObject obj = root.getAsJsonObject();
        if (obj.has("tools") && obj.get("tools").isJsonArray()) {
            if (obj.has("type") && !IskaUtilsLoadPaths.TYPE_TOOLS.equals(obj.get("type").getAsString())) {
                LOGGER.warn("Skipping {}: expected type {}", source, IskaUtilsLoadPaths.TYPE_TOOLS);
                return;
            }
            for (JsonElement entry : obj.getAsJsonArray("tools")) {
                if (entry.isJsonObject()) {
                    parseEntry(source, entry.getAsJsonObject());
                }
            }
            return;
        }
        if (obj.has("item")) {
            parseEntry(source, obj);
        }
    }

    private static void parseEntry(String source, JsonObject json) {
        try {
            String itemRaw = requiredString(json, "item");
            Identifier itemId = Identifier.parse(itemRaw);
            String behaviorRaw = requiredString(json, "behavior");
            ToolBehaviorType behavior = ToolBehaviorType.fromString(behaviorRaw)
                    .orElseThrow(() -> new IllegalArgumentException("unknown behavior: " + behaviorRaw));
            int range = json.has("range") ? json.get("range").getAsInt() : 1;
            List<TagKey<Block>> tags = parseHarvestTags(json);
            BY_ITEM.put(itemId, new ToolBehaviorDefinition(itemId, behavior, range, tags));
        } catch (Exception ex) {
            LOGGER.warn("Invalid tool behavior in {}: {}", source, ex.getMessage());
        }
    }

    private static List<TagKey<Block>> parseHarvestTags(JsonObject json) {
        if (!json.has("harvest_tags") || !json.get("harvest_tags").isJsonArray()) {
            return List.of();
        }
        JsonArray array = json.getAsJsonArray("harvest_tags");
        List<TagKey<Block>> tags = new ArrayList<>();
        for (JsonElement el : array) {
            if (!el.isJsonPrimitive()) {
                continue;
            }
            String raw = el.getAsString();
            if (raw.isBlank()) {
                continue;
            }
            Identifier tagId = raw.startsWith("#") ? Identifier.parse(raw.substring(1)) : Identifier.parse(raw);
            tags.add(TagKey.create(Registries.BLOCK, tagId));
        }
        return tags;
    }

    private static String requiredString(JsonObject json, String field) {
        if (!json.has(field) || !json.get(field).isJsonPrimitive()) {
            throw new IllegalArgumentException("missing field: " + field);
        }
        String value = json.get(field).getAsString();
        if (value.isBlank()) {
            throw new IllegalArgumentException("empty field: " + field);
        }
        return value;
    }
}
