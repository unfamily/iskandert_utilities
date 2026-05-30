package net.unfamily.iskautils.arcane;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.unfamily.iskautils.script.LoadModGate;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ArcaneDictionaryDefinition {

    public record Entry(
            int weight,
            int luck,
            Identifier enchant,
            int upkeepPerLevel,
            List<String> catalysts,
            int traitColorRgb,
            boolean onlyCurio) {}

    private ArcaneDictionaryDefinition() {}

    public static List<Entry> parseEntries(JsonObject root, Logger logger, String contextId) {
        JsonArray arr = null;
        if (root.has("entries") && root.get("entries").isJsonArray()) {
            arr = root.getAsJsonArray("entries");
        } else if (root.has("entrities") && root.get("entrities").isJsonArray()) {
            arr = root.getAsJsonArray("entrities");
        }
        if (arr == null) {
            return List.of();
        }
        List<Entry> out = new ArrayList<>();
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject obj = el.getAsJsonObject();
            if (!LoadModGate.shouldIncludeAtLoad(obj, logger, contextId)) {
                continue;
            }
            if (!obj.has("enchant")) {
                logger.warn("Arcane dictionary entry in {} missing enchant", contextId);
                continue;
            }
            Identifier enchant = Identifier.tryParse(obj.get("enchant").getAsString());
            if (enchant == null) {
                logger.warn("Arcane dictionary entry in {} has invalid enchant", contextId);
                continue;
            }
            int weight = obj.has("weight") ? obj.get("weight").getAsInt() : 0;
            int luck = obj.has("luck") ? obj.get("luck").getAsInt() : 0;
            int upkeep = obj.has("ent_cha") ? obj.get("ent_cha").getAsInt() : -1;
            List<String> catalysts = parseStringList(obj, "cat");
            int traitColorRgb = obj.has("color")
                    ? ArcaneDictionaryTraitStyle.parseHexRgb(obj.get("color").getAsString(), logger, contextId)
                    : -1;
            boolean onlyCurio = !obj.has("only_curio") || obj.get("only_curio").getAsBoolean();
            out.add(new Entry(
                    Math.max(0, weight),
                    luck,
                    enchant,
                    upkeep,
                    catalysts,
                    traitColorRgb,
                    onlyCurio));
        }
        return List.copyOf(out);
    }

    private static List<String> parseStringList(JsonObject obj, String key) {
        if (!obj.has(key) || !obj.get(key).isJsonArray()) {
            return List.of();
        }
        List<String> list = new ArrayList<>();
        for (JsonElement el : obj.getAsJsonArray(key)) {
            if (el.isJsonPrimitive()) {
                list.add(el.getAsString());
            }
        }
        return Collections.unmodifiableList(list);
    }
}
