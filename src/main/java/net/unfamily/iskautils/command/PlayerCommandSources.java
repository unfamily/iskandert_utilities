package net.unfamily.iskautils.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * Command sources for player-relative commands ({@code ~ ~ ~} resolves at the player).
 */
public final class PlayerCommandSources {

    private PlayerCommandSources() {}

    public static CommandSourceStack at(ServerPlayer player) {
        return player.createCommandSourceStack()
                .withPosition(player.position())
                .withEntity(player)
                .withRotation(player.getRotationVector());
    }

    public static CommandSourceStack atSilent(ServerPlayer player) {
        return at(player).withSuppressedOutput();
    }
}
