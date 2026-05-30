package net.unfamily.iskautils.obtaining;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.unfamily.iskautils.command.CommandItemAction;
import net.unfamily.iskautils.command.CommandItemDefinition;
import net.unfamily.iskautils.command.PlayerCommandSources;
import org.slf4j.Logger;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Runs suspicious delivery {@code do[]} action lists (including delays and commands).
 */
public final class SuspiciousDeliveryScriptRunner {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CONTEXT = "suspicious_delivery";

    private static final ConcurrentHashMap<UUID, CopyOnWriteArrayList<PendingRun>> PENDING =
            new ConcurrentHashMap<>();

    private record PendingRun(
            ServerPlayer player,
            List<CommandItemAction> actions,
            int startIndex,
            SuspiciousDeliveryStageHost stageHost,
            long executeAtTick) {}

    private SuspiciousDeliveryScriptRunner() {}

    public static void start(ServerPlayer player, SuspiciousDeliveryDefinition.Entry entry) {
        if (entry == null || entry.actions().isEmpty()) {
            return;
        }
        runImmediate(player, entry.actions(), 0, entry.stageHost());
    }

    public static void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        CopyOnWriteArrayList<PendingRun> list = PENDING.get(id);
        if (list == null || list.isEmpty()) {
            return;
        }
        long now = player.level().getGameTime();
        for (PendingRun run : list) {
            if (run.executeAtTick() <= now && run.player().isAlive()) {
                list.remove(run);
                runImmediate(run.player(), run.actions(), run.startIndex(), run.stageHost());
            }
        }
        if (list.isEmpty()) {
            PENDING.remove(id);
        }
    }

    private static void runImmediate(
            ServerPlayer player,
            List<CommandItemAction> actions,
            int fromIndex,
            SuspiciousDeliveryStageHost stageHost) {
        CommandItemDefinition stageAdapter = stageHost.asDefinitionAdapter();
        for (int i = fromIndex; i < actions.size(); i++) {
            CommandItemAction action = actions.get(i);
            if ((stageHost.getStagesLogic() == CommandItemDefinition.StagesLogic.DEF_AND
                    || stageHost.getStagesLogic() == CommandItemDefinition.StagesLogic.DEF_OR)
                    && !action.getStages().isEmpty()
                    && !action.checkActionStages(player, stageAdapter)) {
                continue;
            }
            if (action.getType() == CommandItemAction.ActionType.DELAY) {
                int delay = Math.max(1, action.getDelay());
                schedule(player, actions, i + 1, stageHost, player.level().getGameTime() + delay);
                return;
            }
            if (action.getType() == CommandItemAction.ActionType.IF) {
                if (action.checkIfConditions(player, stageAdapter)) {
                    runImmediate(player, action.getSubActions(), 0, stageHost);
                }
                continue;
            }
            executeOne(player, action);
        }
    }

    private static void schedule(
            ServerPlayer player,
            List<CommandItemAction> actions,
            int startIndex,
            SuspiciousDeliveryStageHost stageHost,
            long executeAtTick) {
        PENDING.computeIfAbsent(player.getUUID(), k -> new CopyOnWriteArrayList<>())
                .add(new PendingRun(player, actions, startIndex, stageHost, executeAtTick));
    }

    private static void executeOne(ServerPlayer player, CommandItemAction action) {
        Vec3 origin = player.position();
        CommandSourceStack source = PlayerCommandSources.atSilent(player);
        switch (action.getType()) {
            case EXECUTE -> {
                try {
                    var server = player.getServer();
                    if (server != null) {
                        server.getCommands().performPrefixedCommand(source, action.getCommand());
                    }
                } catch (Exception e) {
                    LOGGER.error("Suspicious Delivery command failed: {}", e.getMessage());
                }
            }
            case MESSAGE -> {
                if (action.getMessage() != null) {
                    action.getMessage().send(source, origin, LOGGER, CONTEXT);
                }
            }
            case DROP -> spawnDrop(player, action.getDropItemId(), origin);
            case ITEM -> LOGGER.warn("Suspicious Delivery ignored item action: {}", action.getItemAction());
            case IF, DELAY -> LOGGER.warn("Suspicious Delivery unexpected action type {}", action.getType());
        }
    }

    private static void spawnDrop(ServerPlayer player, net.minecraft.resources.ResourceLocation itemId, Vec3 origin) {
        if (itemId == null) {
            return;
        }
        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
        if (item == null) {
            LOGGER.warn("Unknown drop '{}' in suspicious delivery", itemId);
            return;
        }
        Level level = player.level();
        if (level.isClientSide) {
            return;
        }
        ItemEntity ent = new ItemEntity(level, origin.x, origin.y, origin.z, new ItemStack(item));
        level.addFreshEntity(ent);
    }

    public static void clearPlayer(UUID playerId) {
        PENDING.remove(playerId);
    }
}
