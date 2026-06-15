package net.unfamily.iskautils.obtaining;

import net.unfamily.iskautils.util.ModLogger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.unfamily.iskautils.command.CommandItemAction;
import net.unfamily.iskautils.command.CommandItemDefinition;
import net.unfamily.iskautils.command.PlayerCommandSources;
import net.unfamily.iskautils.script.LoadActionParser;
import net.unfamily.iskautils.script.LoadModCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Runs suspicious delivery {@code do[]} action lists (including delays and commands).
 * Pending runs are anchored to world + position + actor UUID so delays continue without an online player.
 */
public final class SuspiciousDeliveryScriptRunner {
    private static final ModLogger LOGGER = ModLogger.of(SuspiciousDeliveryScriptRunner.class);
    private static final String CONTEXT = "suspicious_delivery";
    private static final Gson GSON = new Gson();

    private static final CopyOnWriteArrayList<PendingRun> PENDING = new CopyOnWriteArrayList<>();

    record PendingRun(
            ResourceKey<Level> dimension,
            Vec3 anchor,
            UUID actorId,
            List<CommandItemAction> actions,
            int startIndex,
            SuspiciousDeliveryStageHost stageHost,
            long executeAtTick) {}

    public record StoredPendingRun(
            String dimension,
            double x,
            double y,
            double z,
            UUID actorId,
            String actionsJson,
            String stageHostJson,
            int startIndex,
            long executeAtTick) {}

    private SuspiciousDeliveryScriptRunner() {}

    public static void start(ServerPlayer player, SuspiciousDeliveryDefinition.Entry entry) {
        if (entry == null || entry.actions().isEmpty()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Vec3 anchor = player.position();
        runImmediate(level, anchor, player.getUUID(), player, entry.actions(), 0, entry.stageHost());
    }

    public static void tickAll(MinecraftServer server) {
        if (PENDING.isEmpty()) {
            return;
        }
        List<PendingRun> due = new ArrayList<>();
        for (PendingRun run : PENDING) {
            ServerLevel level = server.getLevel(run.dimension());
            if (level == null) {
                continue;
            }
            if (run.executeAtTick() <= level.getGameTime()) {
                due.add(run);
            }
        }
        for (PendingRun run : due) {
            PENDING.remove(run);
            ServerLevel level = server.getLevel(run.dimension());
            if (level == null) {
                continue;
            }
            ServerPlayer actor = resolveActor(server, level, run);
            if (actor == null) {
                continue;
            }
            runImmediate(level, run.anchor(), run.actorId(), actor, run.actions(), run.startIndex(), run.stageHost());
        }
    }

    public static List<StoredPendingRun> exportPending() {
        List<StoredPendingRun> out = new ArrayList<>(PENDING.size());
        for (PendingRun run : PENDING) {
            out.add(new StoredPendingRun(
                    run.dimension().location().toString(),
                    run.anchor().x,
                    run.anchor().y,
                    run.anchor().z,
                    run.actorId(),
                    actionsToJson(run.actions()),
                    stageHostToJson(run.stageHost()),
                    run.startIndex(),
                    run.executeAtTick()));
        }
        return out;
    }

    public static void importPending(MinecraftServer server, List<StoredPendingRun> stored) {
        PENDING.clear();
        if (stored == null || stored.isEmpty()) {
            return;
        }
        for (StoredPendingRun storedRun : stored) {
            ResourceLocation dimId = ResourceLocation.tryParse(storedRun.dimension());
            if (dimId == null) {
                continue;
            }
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimId);
            List<CommandItemAction> actions = actionsFromJson(storedRun.actionsJson());
            SuspiciousDeliveryStageHost stageHost = stageHostFromJson(storedRun.stageHostJson());
            if (actions.isEmpty()) {
                continue;
            }
            PENDING.add(new PendingRun(
                    dimension,
                    new Vec3(storedRun.x(), storedRun.y(), storedRun.z()),
                    storedRun.actorId(),
                    actions,
                    storedRun.startIndex(),
                    stageHost,
                    storedRun.executeAtTick()));
        }
        LOGGER.debug("Restored {} suspicious delivery pending runs", PENDING.size());
    }

    private static void runImmediate(
            ServerLevel level,
            Vec3 anchor,
            UUID actorId,
            ServerPlayer actor,
            List<CommandItemAction> actions,
            int fromIndex,
            SuspiciousDeliveryStageHost stageHost) {
        positionActor(actor, anchor);
        CommandItemDefinition stageAdapter = stageHost.asDefinitionAdapter();
        for (int i = fromIndex; i < actions.size(); i++) {
            CommandItemAction action = actions.get(i);
            if ((stageHost.getStagesLogic() == CommandItemDefinition.StagesLogic.DEF_AND
                    || stageHost.getStagesLogic() == CommandItemDefinition.StagesLogic.DEF_OR)
                    && !action.getStages().isEmpty()
                    && !action.checkActionStages(actor, stageAdapter)) {
                continue;
            }
            if (action.getType() == CommandItemAction.ActionType.DELAY) {
                int delay = Math.max(1, action.getDelay());
                schedule(level, anchor, actorId, actions, i + 1, stageHost, level.getGameTime() + delay);
                return;
            }
            if (action.getType() == CommandItemAction.ActionType.IF) {
                if (action.checkIfConditions(actor, stageAdapter)) {
                    runImmediate(level, anchor, actorId, actor, action.getSubActions(), 0, stageHost);
                }
                continue;
            }
            executeOne(level, anchor, actor, action);
        }
    }

    private static void schedule(
            ServerLevel level,
            Vec3 anchor,
            UUID actorId,
            List<CommandItemAction> actions,
            int startIndex,
            SuspiciousDeliveryStageHost stageHost,
            long executeAtTick) {
        PENDING.add(new PendingRun(level.dimension(), anchor, actorId, actions, startIndex, stageHost, executeAtTick));
    }

    private static void executeOne(ServerLevel level, Vec3 anchor, ServerPlayer actor, CommandItemAction action) {
        positionActor(actor, anchor);
        CommandSourceStack source = PlayerCommandSources.atSilent(actor).withPosition(anchor);
        switch (action.getType()) {
            case EXECUTE -> {
                try {
                    var server = level.getServer();
                    if (server != null) {
                        server.getCommands().performPrefixedCommand(source, action.getCommand());
                    }
                } catch (Exception e) {
                    LOGGER.error("Suspicious Delivery command failed: {}", e.getMessage());
                }
            }
            case MESSAGE -> {
                if (action.getMessage() != null) {
                    action.getMessage().send(source, anchor, LOGGER.unwrap(), CONTEXT);
                }
            }
            case DROP -> spawnDrop(level, action.getDropItemId(), anchor);
            case ITEM -> LOGGER.warn("Suspicious Delivery ignored item action: {}", action.getItemAction());
            case IF, DELAY -> LOGGER.warn("Suspicious Delivery unexpected action type {}", action.getType());
        }
    }

    private static void spawnDrop(ServerLevel level, ResourceLocation itemId, Vec3 origin) {
        if (itemId == null) {
            return;
        }
        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
        if (item == null) {
            LOGGER.warn("Unknown drop '{}' in suspicious delivery", itemId);
            return;
        }
        ItemEntity ent = new ItemEntity(level, origin.x, origin.y, origin.z, new ItemStack(item));
        level.addFreshEntity(ent);
    }

    private static ServerPlayer resolveActor(MinecraftServer server, ServerLevel level, PendingRun run) {
        ServerPlayer online = server.getPlayerList().getPlayer(run.actorId());
        if (online != null) {
            return online;
        }
        GameProfile profile = new GameProfile(run.actorId(), "[SuspiciousDelivery]");
        FakePlayer fake = FakePlayerFactory.get(level, profile);
        positionActor(fake, run.anchor());
        return fake;
    }

    private static void positionActor(ServerPlayer actor, Vec3 anchor) {
        actor.setPos(anchor.x, anchor.y, anchor.z);
    }

    public static void clearPlayer(UUID playerId) {
        PENDING.removeIf(run -> run.actorId().equals(playerId));
    }

    static String actionsToJson(List<CommandItemAction> actions) {
        JsonArray array = new JsonArray();
        for (CommandItemAction action : actions) {
            array.add(actionToJson(action));
        }
        return GSON.toJson(array);
    }

    static List<CommandItemAction> actionsFromJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            return LoadActionParser.parseActions(array, CONTEXT);
        } catch (Exception e) {
            LOGGER.warn("Failed to parse suspicious delivery actions: {}", e.getMessage());
            return List.of();
        }
    }

    private static JsonObject actionToJson(CommandItemAction action) {
        JsonObject obj = new JsonObject();
        switch (action.getType()) {
            case EXECUTE -> obj.addProperty("execute", action.getCommand());
            case DELAY -> obj.addProperty("delay", action.getDelay());
            case DROP -> obj.addProperty("drop", action.getDropItemId().toString());
            case MESSAGE -> obj.add("message", messageToJson(action.getMessage()));
            case ITEM -> obj.addProperty("item", action.getItemAction().name().toLowerCase());
            case IF -> {
                JsonArray ifArray = new JsonArray();
                JsonObject conditions = new JsonObject();
                JsonArray condIdx = new JsonArray();
                for (Integer index : action.getConditionIndices()) {
                    condIdx.add(index);
                }
                conditions.add("conditions", condIdx);
                if (!action.getModConditionIndices().isEmpty()) {
                    JsonArray modIdx = new JsonArray();
                    for (Integer index : action.getModConditionIndices()) {
                        modIdx.add(index);
                    }
                    conditions.add("mod_conditions", modIdx);
                }
                ifArray.add(conditions);
                for (CommandItemAction sub : action.getSubActions()) {
                    ifArray.add(actionToJson(sub));
                }
                obj.add("if", ifArray);
            }
            default -> {
            }
        }
        if (!action.getStages().isEmpty()) {
            JsonArray stages = new JsonArray();
            for (CommandItemDefinition.StageCondition stage : action.getStages()) {
                JsonObject stageObj = new JsonObject();
                stageObj.addProperty("stage_type", stage.getStageType());
                stageObj.addProperty("stage", stage.getStage());
                stageObj.addProperty("is", stage.shouldBeSet());
                stages.add(stageObj);
            }
            obj.add("stages", stages);
        }
        return obj;
    }

    private static JsonObject messageToJson(MessageSpec message) {
        JsonObject obj = new JsonObject();
        if (message == null) {
            return obj;
        }
        obj.addProperty("target", message.target());
        if (message.distance() > 0.0) {
            obj.addProperty("distance", message.distance());
        }
        String channel = switch (message.channel()) {
            case ACTION_BAR -> "action_bar";
            case TITLE -> "title";
            default -> "chat";
        };
        obj.addProperty("in", channel);
        obj.addProperty("color", String.format("#%06X", message.rgb() & 0xFFFFFF));
        obj.addProperty("text", message.translationKey());
        return obj;
    }

    static String stageHostToJson(SuspiciousDeliveryStageHost host) {
        JsonObject obj = new JsonObject();
        if (!host.getStages().isEmpty()) {
            JsonArray stages = new JsonArray();
            for (CommandItemDefinition.StageCondition stage : host.getStages()) {
                JsonObject stageObj = new JsonObject();
                stageObj.addProperty("stage_type", stage.getStageType());
                stageObj.addProperty("stage", stage.getStage());
                stageObj.addProperty("is", stage.shouldBeSet());
                stages.add(stageObj);
            }
            obj.add("stages", stages);
        }
        obj.addProperty("stages_logic", stagesLogicToString(host.getStagesLogic()));
        if (!host.getMods().isEmpty()) {
            JsonArray mods = new JsonArray();
            for (LoadModCondition mod : host.getMods()) {
                JsonObject modObj = new JsonObject();
                modObj.addProperty("mod", mod.modId());
                modObj.addProperty("is", mod.requiredLoaded());
                mods.add(modObj);
            }
            obj.add("mods", mods);
        }
        obj.addProperty("mods_logic", stagesLogicToString(host.getModsLogic()));
        return GSON.toJson(obj);
    }

    static SuspiciousDeliveryStageHost stageHostFromJson(String json) {
        if (json == null || json.isBlank()) {
            return new SuspiciousDeliveryStageHost(List.of(), CommandItemDefinition.StagesLogic.AND, List.of(), CommandItemDefinition.StagesLogic.AND);
        }
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            List<CommandItemDefinition.StageCondition> stages = obj.has("stages")
                    ? LoadActionParser.parseStages(obj.getAsJsonArray("stages"))
                    : List.of();
            CommandItemDefinition.StagesLogic stagesLogic = obj.has("stages_logic")
                    ? LoadActionParser.parseStagesLogic(obj.get("stages_logic").getAsString())
                    : CommandItemDefinition.StagesLogic.AND;
            List<LoadModCondition> mods = LoadActionParser.parseMods(obj);
            CommandItemDefinition.StagesLogic modsLogic = LoadActionParser.parseModsLogic(obj);
            return new SuspiciousDeliveryStageHost(stages, stagesLogic, mods, modsLogic);
        } catch (Exception e) {
            LOGGER.warn("Failed to parse suspicious delivery stage host: {}", e.getMessage());
            return new SuspiciousDeliveryStageHost(List.of(), CommandItemDefinition.StagesLogic.AND, List.of(), CommandItemDefinition.StagesLogic.AND);
        }
    }

    private static String stagesLogicToString(CommandItemDefinition.StagesLogic logic) {
        return switch (logic) {
            case OR -> "OR";
            case DEF_AND -> "DEF_AND";
            case DEF_OR -> "DEF_OR";
            default -> "AND";
        };
    }
}
