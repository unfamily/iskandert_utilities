package net.unfamily.iskautils.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Provides argument types for macro commands
 */
public class MacroCommandArgument {

    private static final DynamicCommandExceptionType ERROR_UNKNOWN_MACRO = new DynamicCommandExceptionType(
            (id) -> Component.literal("Unknown macro: " + id));

    /**
     * Provides a macro ID argument type - usando StringArgumentType invece di un tipo personalizzato
     */
    public static ArgumentType<String> macroId() {
        return StringArgumentType.word();
    }

    /**
     * Gets a macro ID from the command context
     */
    public static String getMacroId(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        String macroId = context.getArgument(name, String.class);
        if (!MacroCommand.hasMacro(macroId)) {
            throw ERROR_UNKNOWN_MACRO.create(macroId);
        }
        return macroId;
    }

    /**
     * Suggerisce macro disponibili per il completamento automatico
     */
    public static CompletableFuture<Suggestions> suggestMacros(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        List<String> suggestions = new ArrayList<>();
        
        CommandSourceStack sourceStack = context.getSource();
        if (sourceStack == null) {
            return builder.buildFuture();
        }
        
        for (String macroId : MacroCommand.getAvailableMacros()) {
            MacroCommand.MacroDefinition macro = MacroCommand.getMacro(macroId);
            
            if (macro != null) {
                try {
                    boolean hasPermission = true;
                    try {
                        hasPermission = sourceStack.hasPermission(macro.getLevel());
                    } catch (Exception e) {
                        // In caso di errore, mostra comunque il comando
                        hasPermission = true;
                    }
                    
                    if (hasPermission) {
                        suggestions.add(macroId);
                    }
                } catch (Exception e) {
                    // Ignora errori e includi comunque il comando nei suggerimenti
                    suggestions.add(macroId);
                }
            }
        }
        
        return SharedSuggestionProvider.suggest(suggestions, builder);
    }
} 