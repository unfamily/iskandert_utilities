package net.unfamily.iskautils.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.data.load.IskaUtilsLoadReloadEffects;
import net.unfamily.iskalib.debug.HandItemDump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Debug command: {@code iska_utils_debug reload} and {@code iska_utils_debug hand} (delegates to {@link HandItemDump}).
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public class DebugCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(DebugCommand.class);

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("iska_utils_debug")
                .requires(source -> source.hasPermission(0))
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(DebugCommand::executeReload))
                .then(Commands.argument("action", StringArgumentType.word())
                        .suggests((context, builder) ->
                                SharedSuggestionProvider.suggest(new String[]{"hand"}, builder))
                        .executes(DebugCommand::executeDebug))
        );
    }

    private static int executeReload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("§7Reloading IskaUtils load JSON (data/*/load/**)"), false);

        try {
            IskaUtilsLoadReloadEffects.applyReloadFromDatapacks();
            source.sendSuccess(() -> Component.literal("§aReload complete"), false);
            IskaUtilsLoadReloadEffects.sendReloadNotice(source);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error during IskaUtils reload: {}", e.getMessage());
            source.sendFailure(Component.literal("§cReload failed: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeDebug(CommandContext<CommandSourceStack> context) {
        String action = StringArgumentType.getString(context, "action");

        if ("hand".equals(action)) {
            CommandSourceStack source = context.getSource();
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendFailure(Component.translatable("command.iska_utils.debug.only_player"));
                return 0;
            }
            return HandItemDump.dumpHands(player, source);
        }

        context.getSource().sendFailure(Component.translatable("command.iska_utils.debug.unknown_action", action));
        return 0;
    }
}
