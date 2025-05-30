package net.unfamily.iskautils.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.unfamily.iskautils.IskaUtils;
import org.slf4j.Logger;

/**
 * Command handler for executing command macros
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public class ExecuteMacroCommand {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registering IskaUtils macro commands");
        register(event.getDispatcher());
    }

    /**
     * Registers the command
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register individual commands for each macro
        for (String macroId : MacroCommand.getAvailableMacros()) {
            MacroCommand.MacroDefinition macro = MacroCommand.getMacro(macroId);
            if (macro != null) {
                MacroCommand.registerCommand(dispatcher, macroId, macro);
                LOGGER.debug("Registered command /{} using MacroCommand.registerCommand", macroId);
            }
        }
        
        // Keep the admin command for administration functionality
        dispatcher.register(
            Commands.literal("iska_utils_macro")
                .requires(source -> source.hasPermission(2)) // Require permission level 2 (OP)
                .then(Commands.literal("list")
                    .executes(ExecuteMacroCommand::listMacros))
                .then(Commands.literal("test")
                    .then(Commands.argument("macro_id", MacroCommandArgument.macroId())
                        .suggests(MacroCommandArgument::suggestMacros)
                        .executes(context -> executeMacro(context, true))))
                .executes(ExecuteMacroCommand::showUsage)
        );
    }
    
    /**
     * Shows command usage
     */
    private static int showUsage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("§6===== IskaUtils Macro Commands ====="), false);
        source.sendSuccess(() -> Component.literal("§a/iska_utils_macro list §7- List available commands"), false);
        source.sendSuccess(() -> Component.literal("§a/iska_utils_macro test <macro_id> §7- Test a command without executing it"), false);
        source.sendSuccess(() -> Component.literal("§a/<macro_id> §7- Directly execute a macro command"), false);
        return 1;
    }

    /**
     * Lists all registered macros
     */
    private static int listMacros(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("§6===== Available Macros ====="), false);
        
        for (String macroId : MacroCommand.getAvailableMacros()) {
            final String currentMacroId = macroId; // Make it final for the lambda
            MacroCommand.MacroDefinition macro = MacroCommand.getMacro(currentMacroId);
            if (macro != null) {
                source.sendSuccess(() -> Component.literal("§a" + currentMacroId), false);
            }
        }
        
        return 1;
    }

    /**
     * Executes a macro directly by ID
     */
    private static int executeMacroDirectly(CommandContext<CommandSourceStack> context, String macroId, boolean testOnly) throws CommandSyntaxException {
        if (!MacroCommand.hasMacro(macroId)) {
            context.getSource().sendFailure(Component.literal("Unknown macro: " + macroId));
            return 0;
        }
        
        ServerPlayer player = context.getSource().getPlayerOrException();
        MacroCommand.MacroDefinition macro = MacroCommand.getMacro(macroId);
        
        if (testOnly) {
            context.getSource().sendSuccess(() -> Component.literal("§aTesting macro: §e" + macroId), false);
            for (MacroCommand.MacroAction action : macro.getActions()) {
                context.getSource().sendSuccess(() -> Component.literal("§7Would execute: §f" + action.getCommand()), false);
            }
            return 1;
        }
        
        return MacroCommand.executeMacro(context, macroId, new Object[0]);
    }
    
    /**
     * Executes a macro from command context
     */
    private static int executeMacro(CommandContext<CommandSourceStack> context, boolean testOnly) throws CommandSyntaxException {
        String macroId = MacroCommandArgument.getMacroId(context, "macro_id");
        return executeMacroDirectly(context, macroId, testOnly);
    }
} 