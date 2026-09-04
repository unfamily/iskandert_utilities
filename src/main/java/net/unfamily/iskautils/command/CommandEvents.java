package net.unfamily.iskautils.command;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Central command registration for the mod (Utils-owned commands only).
 * Library commands are registered by {@code IskaLibCommandBootstrap}.
 */
public final class CommandEvents {
    private CommandEvents() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ShopCommand.register(event.getDispatcher());
    }
}
