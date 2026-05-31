package net.unfamily.iskautils.arcane;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.iskautils.command.CommandItemDefinition;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryStageHost;
import net.unfamily.iskautils.script.LoadActionParser;
import net.unfamily.iskautils.script.LoadModGate;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ArcaneDictionaryDefinition {

    public record Entry(
            int weight,
            int luck,
            ResourceLocation enchant,
            int consumePerLevel,
            List<String> catalysts,
            int traitColorRgb,
            boolean onlyCurio,
            SuspiciousDeliveryStageHost gateHost) {

        public boolean isFullyEligible(net.minecraft.server.level.ServerPlayer player) {
            return gateHost.isFullyEligible(player);
        }

        public boolean checkAllMods() {
            return gateHost.checkAllMods();
        }
    }

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
            if (!obj.has("enchant")) {
                logger.warn("Arcane dictionary entry in {} missing enchant", contextId);
                continue;
            }
            ResourceLocation enchant = ResourceLocation.tryParse(obj.get("enchant").getAsString());
            if (enchant == null) {
                logger.warn("Arcane dictionary entry in {} has invalid enchant", contextId);
                continue;
            }
            int weight = obj.has("weight") ? obj.get("weight").getAsInt() : 0;
            int luck = obj.has("luck") ? obj.get("luck").getAsInt() : 0;
            int consumePerLevel = obj.has("ent_cha") ? obj.get("ent_cha").getAsInt() : -1;
            List<String> catalysts = parseStringList(obj, "cat");
            int traitColorRgb = obj.has("color")
                    ? ArcaneDictionaryTraitStyle.parseHexRgb(obj.get("color").getAsString(), logger, contextId)
                    : -1;
            boolean onlyCurio = !obj.has("only_curio") || obj.get("only_curio").getAsBoolean();
            SuspiciousDeliveryStageHost gateHost = parseGateHost(obj, logger, contextId, enchant);
            out.add(new Entry(
                    Math.max(0, weight),
                    luck,
                    enchant,
                    consumePerLevel,
                    catalysts,
                    traitColorRgb,
                    onlyCurio,
                    gateHost));
        }
        return List.copyOf(out);
    }

    private static SuspiciousDeliveryStageHost parseGateHost(
            JsonObject obj,
            Logger logger,
            String contextId,
            ResourceLocation enchant) {
        List<CommandItemDefinition.StageCondition> stages = new ArrayList<>();
        CommandItemDefinition.StagesLogic stagesLogic = CommandItemDefinition.StagesLogic.AND;
        if (obj.has("stages") && obj.get("stages").isJsonArray()) {
            stages.addAll(LoadActionParser.parseStages(obj.getAsJsonArray("stages")));
        }
        if (obj.has("stages_logic")) {
            stagesLogic = LoadActionParser.parseStagesLogic(obj.get("stages_logic").getAsString());
            if (LoadModGate.isDeferredLogic(stagesLogic)) {
                logger.warn(
                        "Arcane dictionary entry {} in {} uses unsupported stages_logic {}; using AND",
                        enchant,
                        contextId,
                        stagesLogic);
                stagesLogic = CommandItemDefinition.StagesLogic.AND;
            }
        }

        CommandItemDefinition.StagesLogic modsLogic = LoadActionParser.parseModsLogic(obj);
        if (LoadModGate.isDeferredLogic(modsLogic)) {
            logger.warn(
                    "Arcane dictionary entry {} in {} uses unsupported mods_logic {}; using AND",
                    enchant,
                    contextId,
                    modsLogic);
            modsLogic = CommandItemDefinition.StagesLogic.AND;
        }

        return new SuspiciousDeliveryStageHost(
                stages,
                stagesLogic,
                LoadActionParser.parseMods(obj),
                modsLogic);
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
