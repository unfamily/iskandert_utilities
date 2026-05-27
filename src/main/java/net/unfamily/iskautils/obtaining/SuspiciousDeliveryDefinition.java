package net.unfamily.iskautils.obtaining;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public final class SuspiciousDeliveryDefinition {

    public record Entry(int weight, int luck, List<Action> actions) {}

    public sealed interface Action permits MessageAction, DropAction {
        void run(CommandSourceStack source, ServerPlayer opener, Vec3 origin, Logger logger, String contextId);
    }

    public record MessageAction(MessageSpec spec) implements Action {
        @Override
        public void run(CommandSourceStack source, ServerPlayer opener, Vec3 origin, Logger logger, String contextId) {
            spec.send(source, origin, logger, contextId);
        }
    }

    public record DropAction(Identifier itemId) implements Action {
        @Override
        public void run(CommandSourceStack source, ServerPlayer opener, Vec3 origin, Logger logger, String contextId) {
            Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
            if (item == null) {
                logger.warn("Unknown drop item '{}' in {}", itemId, contextId);
                return;
            }
            Level level = opener.level();
            if (level.isClientSide()) return;
            ItemStack stack = new ItemStack(item);
            ItemEntity ent = new ItemEntity(level, origin.x, origin.y, origin.z, stack);
            level.addFreshEntity(ent);
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
            if (!e.isJsonObject()) continue;
            JsonObject obj = e.getAsJsonObject();
            int weight = obj.has("weight") ? obj.get("weight").getAsInt() : 0;
            int luck = obj.has("luck") ? obj.get("luck").getAsInt() : 0;
            List<Action> actions = parseActions(obj, logger, contextId);
            out.add(new Entry(Math.max(0, weight), luck, actions));
        }
        return new SuspiciousDeliveryDefinition(out);
    }

    private static List<Action> parseActions(JsonObject entryObj, Logger logger, String contextId) {
        List<Action> out = new ArrayList<>();
        if (!entryObj.has("do") || !entryObj.get("do").isJsonArray()) {
            return out;
        }
        for (JsonElement e : entryObj.getAsJsonArray("do")) {
            if (!e.isJsonObject()) continue;
            JsonObject obj = e.getAsJsonObject();
            if (obj.has("message") && obj.get("message").isJsonObject()) {
                out.add(new MessageAction(MessageSpec.fromJson(obj.getAsJsonObject("message"), logger, contextId)));
            } else if (obj.has("drop")) {
                String id = obj.get("drop").getAsString();
                Identifier rl = Identifier.tryParse(id);
                if (rl == null) {
                    logger.warn("Invalid drop id '{}' in {}", id, contextId);
                } else {
                    out.add(new DropAction(rl));
                }
            }
        }
        return out;
    }
}

