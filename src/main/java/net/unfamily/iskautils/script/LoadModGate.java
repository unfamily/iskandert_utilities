package net.unfamily.iskautils.script;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.unfamily.iskautils.command.CommandItemDefinition;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Mod presence gates for load JSON: load-time inclusion and deferred {@code if} checks.
 */
public final class LoadModGate {
    private LoadModGate() {}

    public static List<LoadModCondition> parseMods(JsonArray modsArray) {
        List<LoadModCondition> out = new ArrayList<>();
        if (modsArray == null) {
            return List.copyOf(out);
        }
        for (JsonElement el : modsArray) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject obj = el.getAsJsonObject();
            if (!obj.has("mod") || !obj.get("mod").isJsonPrimitive()) {
                continue;
            }
            String modId = obj.get("mod").getAsString();
            boolean required = !obj.has("is") || obj.get("is").getAsBoolean();
            out.add(new LoadModCondition(modId, required));
        }
        return List.copyOf(out);
    }

    public static List<LoadModCondition> parseMods(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || !obj.get(key).isJsonArray()) {
            return List.of();
        }
        return parseMods(obj.getAsJsonArray(key));
    }

    public static CommandItemDefinition.StagesLogic parseModsLogic(String raw) {
        return LoadActionParser.parseStagesLogic(raw);
    }

    public static boolean isDeferredLogic(CommandItemDefinition.StagesLogic logic) {
        return logic == CommandItemDefinition.StagesLogic.DEF_AND
                || logic == CommandItemDefinition.StagesLogic.DEF_OR;
    }

    /**
     * When {@code mods} is present without DEF logic, all conditions must match or the entry is skipped at load.
     */
    public static boolean shouldIncludeAtLoad(JsonObject obj, Logger logger, String contextId) {
        if (obj == null || !obj.has("mods") || !obj.get("mods").isJsonArray()) {
            return true;
        }
        List<LoadModCondition> mods = parseMods(obj.getAsJsonArray("mods"));
        if (mods.isEmpty()) {
            return true;
        }
        CommandItemDefinition.StagesLogic logic = obj.has("mods_logic")
                ? parseModsLogic(obj.get("mods_logic").getAsString())
                : CommandItemDefinition.StagesLogic.AND;
        if (isDeferredLogic(logic)) {
            return true;
        }
        for (LoadModCondition condition : mods) {
            if (!condition.matches()) {
                logger.debug("Skipping {} — mod gate failed for mod '{}' (required loaded={})",
                        contextId, condition.modId(), condition.requiredLoaded());
                return false;
            }
        }
        return true;
    }

    public static boolean checkModIndices(
            List<LoadModCondition> mods,
            CommandItemDefinition.StagesLogic modsLogic,
            List<Integer> indices) {
        if (indices == null || indices.isEmpty()) {
            return true;
        }
        if (mods == null || mods.isEmpty()) {
            return false;
        }
        if (modsLogic == CommandItemDefinition.StagesLogic.DEF_OR) {
            for (Integer index : indices) {
                if (index >= 0 && index < mods.size() && mods.get(index).matches()) {
                    return true;
                }
            }
            return false;
        }
        for (Integer index : indices) {
            if (index < 0 || index >= mods.size() || !mods.get(index).matches()) {
                return false;
            }
        }
        return true;
    }
}
