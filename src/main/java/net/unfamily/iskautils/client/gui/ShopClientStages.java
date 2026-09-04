package net.unfamily.iskautils.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskalib.stage.StageRegistry;
import net.unfamily.iskautils.shop.ShopEntry;
import net.unfamily.iskautils.shop.ShopStage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side stage checks for shop browse UI (singleplayer integrated server only).
 */
public final class ShopClientStages {

    public record StageFailure(String stageType, String stageId, boolean required) {}

    private ShopClientStages() {}

    public static boolean isEntryBlocked(@Nullable ShopEntry item) {
        return !getFailures(item).isEmpty();
    }

    public static List<StageFailure> getFailures(@Nullable ShopEntry item) {
        List<StageFailure> failures = new ArrayList<>();
        if (item == null || item.stages == null || item.stages.length == 0) {
            return failures;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return failures;
        }
        try {
            MinecraftServer server = mc.getSingleplayerServer();
            if (server == null) {
                return failures;
            }
            ServerPlayer serverPlayer = null;
            String localName = mc.player.getName().getString();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.getName().getString().equals(localName)) {
                    serverPlayer = player;
                    break;
                }
            }
            if (serverPlayer == null) {
                return failures;
            }
            StageRegistry registry = StageRegistry.getInstance(server);
            for (ShopStage stage : item.stages) {
                if (stage == null || stage.stageType == null) {
                    continue;
                }
                boolean hasStage = switch (stage.stageType.toLowerCase()) {
                    case "player" -> registry.hasPlayerStage(serverPlayer, stage.stage);
                    case "world" -> registry.hasWorldStage(stage.stage);
                    case "team" -> registry.hasPlayerTeamStage(serverPlayer, stage.stage);
                    default -> false;
                };
                if (hasStage != stage.is) {
                    failures.add(new StageFailure(stage.stageType, stage.stage, stage.is));
                }
            }
        } catch (Exception ignored) {
            // ignore client lookup failures
        }
        return failures;
    }
}
