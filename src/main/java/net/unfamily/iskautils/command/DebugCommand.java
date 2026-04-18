package net.unfamily.iskautils.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.data.load.IskaUtilsDataReload;
import net.unfamily.iskalib.debug.HandItemDump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod debug command: {@code iska_utils_debug reload} (level 2) and {@code iska_utils_debug hand} (delegates to library dump).
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public class DebugCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(DebugCommand.class);

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("iska_utils_debug")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(0))))
                .then(Commands.literal("reload")
                        .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2))))
                        .executes(DebugCommand::executeReload))
                .then(Commands.argument("action", StringArgumentType.word())
                        .suggests((context, builder) ->
                                SharedSuggestionProvider.suggest(new String[]{"hand"}, builder))
                        .executes(DebugCommand::executeDebug))
        );
    }

    private static int executeReload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("Reloading IskaUtils load JSON (data/*/load/)")
                .withStyle(ChatFormatting.GRAY), false);

        try {
            IskaUtilsDataReload.reloadAllFromServer();
            ShopCommand.notifyClientGUIReload();
            var server = source.getServer();
            if (server != null && !server.isSingleplayer()) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    net.unfamily.iskautils.network.ModMessages.sendStructureSyncPacket(player);
                }
                source.sendSuccess(() -> Component.literal("Reload complete (structures synced to clients)")
                        .withStyle(ChatFormatting.GREEN), false);
            } else {
                source.sendSuccess(() -> Component.literal("Reload complete").withStyle(ChatFormatting.GREEN), false);
            }
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error during IskaUtils reload: {}", e.getMessage());
            source.sendFailure(Component.literal("Reload failed: " + e.getMessage()).withStyle(ChatFormatting.RED));
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
