package net.unfamily.iskautils.command;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Central command registration for the mod.
 *
 * <p>Library commands are registered here to avoid duplicate "twin" classes.
 */
public final class CommandEvents {
    private CommandEvents() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // Library-provided commands
        net.unfamily.iskalib.command.StageCommand.register(event.getDispatcher());
        net.unfamily.iskalib.command.MarkerCommand.register(event.getDispatcher());
        net.unfamily.iskalib.command.IskaLibDebugCommand.register(event.getDispatcher());

        // Mod-owned commands (depend on mod configuration/assets)
        ShopCommand.register(event.getDispatcher());
        ShopTeamCommand.register(event.getDispatcher());
    }
}

