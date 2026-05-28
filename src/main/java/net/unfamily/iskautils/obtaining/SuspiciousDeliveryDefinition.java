package net.unfamily.iskautils.obtaining;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.command.CommandItemAction;
import net.unfamily.iskautils.command.CommandItemDefinition;
import net.unfamily.iskautils.script.LoadActionParser;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public final class SuspiciousDeliveryDefinition {

    public record Entry(
            int weight,
            int luck,
            SuspiciousDeliveryJeiMode jeiMode,
            SuspiciousDeliveryStageHost stageHost,
            List<CommandItemAction> actions) {

        public boolean isEligible(ServerPlayer player) {
            return stageHost.checkAllStages(player);
        }
    }

    private final List<Entry> entries;

    public SuspiciousDeliveryDefinition(List<Entry> entries) {
        this.entries = List.copyOf(entries);
    }

    public List<Entry> entries() {
        return entries;
    }

    public static SuspiciousDeliveryDefinition fromJson(JsonObject root, Logger logger, String contextId) {
        JsonArray arr = null;
        if (root.has("entrities") && root.get("entrities").isJsonArray()) {
            arr = root.getAsJsonArray("entrities");
        } else if (root.has("entries") && root.get("entries").isJsonArray()) {
            arr = root.getAsJsonArray("entries");
        }
        List<Entry> out = new ArrayList<>();
        if (arr == null) {
            return new SuspiciousDeliveryDefinition(out);
        }
        for (JsonElement e : arr) {
            if (!e.isJsonObject()) {
                continue;
            }
            JsonObject obj = e.getAsJsonObject();
            int weight = obj.has("weight") ? obj.get("weight").getAsInt() : 0;
            int luck = obj.has("luck") ? obj.get("luck").getAsInt() : 0;
            SuspiciousDeliveryJeiMode jeiMode = SuspiciousDeliveryJeiMode.fromString(
                    obj.has("jeimode") ? obj.get("jeimode").getAsString() : "show");

            List<CommandItemDefinition.StageCondition> stages = new ArrayList<>();
            CommandItemDefinition.StagesLogic stagesLogic = CommandItemDefinition.StagesLogic.AND;
            if (obj.has("stages") && obj.get("stages").isJsonArray()) {
                stages.addAll(LoadActionParser.parseStages(obj.getAsJsonArray("stages")));
            }
            if (obj.has("stages_logic")) {
                stagesLogic = LoadActionParser.parseStagesLogic(obj.get("stages_logic").getAsString());
            }

            List<CommandItemAction> actions = new ArrayList<>();
            if (obj.has("do") && obj.get("do").isJsonArray()) {
                actions.addAll(LoadActionParser.parseActions(obj.getAsJsonArray("do"), contextId));
            }

            out.add(new Entry(
                    Math.max(0, weight),
                    luck,
                    jeiMode,
                    new SuspiciousDeliveryStageHost(stages, stagesLogic),
                    actions));
        }
        return new SuspiciousDeliveryDefinition(out);
    }
}
