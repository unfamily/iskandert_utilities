package net.unfamily.iskautils.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Comando per eseguire macro di comandi
 */
public class ExecuteMacroCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final SimpleCommandExceptionType ERROR_PERMISSION = new SimpleCommandExceptionType(
            Component.translatable("commands.iska_utils.macro.no_permission"));
    
    /**
     * Registra i comandi nel server
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Registra un comando separato per ogni macro
        for (String macroId : MacroCommand.getAvailableMacros()) {
            MacroCommand.MacroDefinition macro = MacroCommand.getMacro(macroId);
            MacroCommand.registerCommand(dispatcher, macroId, macro);
            
            LOGGER.debug("Registrato comando /{}", macroId);
        }
        
        LOGGER.info("Registrati {} comandi macro", MacroCommand.getAvailableMacros().spliterator().getExactSizeIfKnown());
    }
    
    /**
     * Esegue una macro di comandi
     */
    private static int executeMacro(CommandContext<CommandSourceStack> context, String macroId) throws CommandSyntaxException {
        // Ottieni il giocatore che ha eseguito il comando
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // Ottieni la definizione della macro
        MacroCommand.MacroDefinition macro = MacroCommand.getMacro(macroId);
        
        // Verifica se il giocatore ha il livello di permesso richiesto
        int requiredLevel = macro.getLevel();
        if (requiredLevel > 0 && !player.hasPermissions(requiredLevel)) {
            throw ERROR_PERMISSION.create();
        }
        
        // Esegui la macro
        boolean success = MacroCommand.executeMacro(macroId, player);
        
        if (success) {
            context.getSource().sendSuccess(() -> 
                Component.translatable("commands.iska_utils.macro.success"), true);
            return 1;
        } else {
            context.getSource().sendFailure(
                Component.translatable("commands.iska_utils.macro.failed"));
            return 0;
        }
    }
} 