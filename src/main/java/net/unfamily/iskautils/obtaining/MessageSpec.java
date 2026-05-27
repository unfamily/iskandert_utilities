package net.unfamily.iskautils.obtaining;

import com.google.gson.JsonObject;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.List;

/**
 * Data-driven localized message: chat/actionbar/title.
 */
public record MessageSpec(
        String target,
        double distance,
        int rgb,
        Channel channel,
        String translationKey
) {
    public enum Channel {
        CHAT,
        ACTION_BAR,
        TITLE
    }

    public static MessageSpec fromJson(JsonObject obj, Logger logger, String contextId) {
        String target = obj.has("target") ? obj.get("target").getAsString() : "@a";
        double distance = obj.has("distance") ? obj.get("distance").getAsDouble() : 0.0;
        String in = obj.has("in") ? obj.get("in").getAsString() : "chat";
        Channel channel = switch (in.toLowerCase()) {
            case "action_bar", "actionbar" -> Channel.ACTION_BAR;
            case "title" -> Channel.TITLE;
            default -> Channel.CHAT;
        };
        String color = obj.has("color") ? obj.get("color").getAsString() : "#FFFFFF";
        int rgb = parseRgb(color, logger, contextId);
        String text = obj.has("text") ? obj.get("text").getAsString() : "";
        return new MessageSpec(target, Math.max(0.0, distance), rgb, channel, text);
    }

    private static int parseRgb(String hex, Logger logger, String contextId) {
        String s = hex == null ? "" : hex.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.length() != 6) {
            logger.warn("Invalid color '{}' in {}, defaulting to white", hex, contextId);
            return 0xFFFFFF;
        }
        try {
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (Exception e) {
            logger.warn("Invalid color '{}' in {}, defaulting to white", hex, contextId);
            return 0xFFFFFF;
        }
    }

    public void send(CommandSourceStack source, Vec3 origin, Logger logger, String contextId) {
        if (translationKey == null || translationKey.isBlank()) {
            logger.warn("MessageSpec has empty text key in {}", contextId);
            return;
        }
        MutableComponent msg = Component.translatable(translationKey);
        msg.setStyle(Style.EMPTY.withColor(rgb).withItalic(false));

        List<ServerPlayer> players = resolveTargets(source, origin, logger, contextId);
        if (players.isEmpty()) {
            return;
        }
        for (ServerPlayer p : players) {
            switch (channel) {
                case ACTION_BAR -> p.sendSystemMessage(msg, true);
                case TITLE -> p.sendSystemMessage(msg, false);
                case CHAT -> p.sendSystemMessage(msg, false);
            }
        }
    }

    private List<ServerPlayer> resolveTargets(CommandSourceStack source, Vec3 origin, Logger logger, String contextId) {
        try {
            EntitySelector selector = EntityArgument.players().parse(new com.mojang.brigadier.StringReader(target));
            List<ServerPlayer> players = selector.findPlayers(source);
            if (distance <= 0.0) {
                return players;
            }
            double maxSq = distance * distance;
            return players.stream()
                    .filter(p -> p.position().distanceToSqr(origin) <= maxSq)
                    .toList();
        } catch (Exception e) {
            logger.error("Failed resolving target '{}' in {}: {}", target, contextId, e.getMessage());
            return List.of();
        }
    }
}

