package net.unfamily.iskautils.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sistema per eseguire macro di comandi con ritardi
 */
public class MacroCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Thread pool per l'esecuzione programmata dei comandi
    private static final ScheduledExecutorService COMMAND_EXECUTOR = Executors.newScheduledThreadPool(2);
    
    // Comandi attivi: UUID del giocatore -> future per possibile cancellazione
    private static final Map<UUID, ScheduledFuture<?>> ACTIVE_COMMANDS = new ConcurrentHashMap<>();
    
    // Registro delle macro disponibili
    private static final Map<String, MacroDefinition> MACRO_REGISTRY = new HashMap<>();
    
    // Pattern per trovare i parametri nei comandi (es. #0, #1, ecc.)
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("#(\\d+)");
    
    // Eccezione per permessi mancanti
    public static final SimpleCommandExceptionType ERROR_PERMISSION = new SimpleCommandExceptionType(
            Component.translatable("commands.iska_utils.macro.no_permission"));
    
    /**
     * Registra una nuova macro di comandi
     */
    public static void registerMacro(String id, MacroDefinition macro) {
        MACRO_REGISTRY.put(id, macro);
        LOGGER.info("Registrata macro di comandi: {} con {} azioni", id, macro.actions.size());
    }
    
    /**
     * Elimina una macro dal registro
     */
    public static void unregisterMacro(String id) {
        MACRO_REGISTRY.remove(id);
    }
    
    /**
     * Verifica se esiste una macro con l'ID specificato
     */
    public static boolean hasMacro(String id) {
        return MACRO_REGISTRY.containsKey(id);
    }
    
    /**
     * Ottiene una macro dal registro
     */
    public static MacroDefinition getMacro(String id) {
        return MACRO_REGISTRY.get(id);
    }
    
    /**
     * Ottiene la lista di tutte le macro disponibili
     */
    public static Iterable<String> getAvailableMacros() {
        return MACRO_REGISTRY.keySet();
    }
    
    /**
     * Registra un comando nel dispatcher per una macro
     */
    public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher, String macroId, MacroDefinition macro) {
        if (macro.hasParameters()) {
            // Macro con parametri
            registerParameterizedCommand(dispatcher, macroId, macro);
        } else {
            // Macro senza parametri (semplice)
            dispatcher.register(Commands.literal(macroId)
                .requires(source -> source.hasPermission(0))
                .executes(context -> executeMacro(context, macroId, new Object[0])));
            
            LOGGER.debug("Registrato comando /{}", macroId);
        }
    }
    
    /**
     * Registra un comando con parametri
     */
    private static void registerParameterizedCommand(CommandDispatcher<CommandSourceStack> dispatcher, String macroId, MacroDefinition macro) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(macroId)
            .requires(source -> source.hasPermission(0));
        
        // Costruisci la struttura degli argomenti
        ArgumentBuilder<CommandSourceStack, ?> argumentBuilder = buildArgumentTree(command, macro.parameters, 0, args -> {
            return executeMacro(args, macroId, extractParameterValues(args, macro.parameters));
        });
        
        // Registra il comando
        dispatcher.register((LiteralArgumentBuilder<CommandSourceStack>) argumentBuilder);
        LOGGER.debug("Registrato comando parametrizzato /{} con {} parametri", macroId, macro.parameters.size());
    }
    
    /**
     * Costruisce ricorsivamente l'albero degli argomenti per il comando
     */
    private static ArgumentBuilder<CommandSourceStack, ?> buildArgumentTree(
            ArgumentBuilder<CommandSourceStack, ?> builder, 
            List<ParameterDefinition> parameters, 
            int index, 
            Command<CommandSourceStack> executor) {
        
        if (index >= parameters.size()) {
            // Fine dei parametri, aggiungi l'executor
            return builder.executes(executor);
        }
        
        ParameterDefinition param = parameters.get(index);
        String paramName = "param" + index;
        
        // Crea l'argomento basato sul tipo
        RequiredArgumentBuilder<CommandSourceStack, ?> argBuilder;
        if ("static".equals(param.type.toLowerCase()) && param.staticValues != null && !param.staticValues.isEmpty()) {
            argBuilder = createArgumentForType(paramName, param.type, param.staticValues);
        } else {
            argBuilder = createArgumentForType(paramName, param.type);
        }
        
        // Aggiungi i parametri rimanenti
        ArgumentBuilder<CommandSourceStack, ?> nextBuilder = buildArgumentTree(argBuilder, parameters, index + 1, executor);
        
        if (!param.required) {
            // Se il parametro è opzionale, aggiungi anche un esecutore al livello corrente
            builder.executes(ctx -> executeMacro(ctx, 
                    getCommandName(builder), 
                    extractParameterValues(ctx, parameters.subList(0, index))));
        }
        
        // Collega il parametro corrente al builder
        return builder.then(nextBuilder);
    }
    
    /**
     * Estrae il nome del comando da un builder
     */
    private static String getCommandName(ArgumentBuilder<CommandSourceStack, ?> builder) {
        if (builder instanceof LiteralArgumentBuilder) {
            return ((LiteralArgumentBuilder<CommandSourceStack>)builder).getLiteral();
        } else {
            return "unknown";
        }
    }
    
    /**
     * Crea un argomento Brigadier in base al tipo specificato
     */
    @SuppressWarnings("unchecked")
    private static RequiredArgumentBuilder<CommandSourceStack, ?> createArgumentForType(String name, String type, List<String> staticValues) {
        switch (type.toLowerCase()) {
            case "string":
                return Commands.argument(name, StringArgumentType.greedyString());
            case "word":
                return Commands.argument(name, StringArgumentType.word());
            case "int":
                return Commands.argument(name, IntegerArgumentType.integer());
            case "float":
            case "double":
                return Commands.argument(name, DoubleArgumentType.doubleArg());
            case "boolean":
                return Commands.argument(name, BoolArgumentType.bool());
            case "target":
                return Commands.argument(name, EntityArgument.players());
            case "static":
                // Utilizza StringArgumentType.word() e poi restringe i valori con suggests()
                RequiredArgumentBuilder<CommandSourceStack, String> builder = 
                    Commands.argument(name, StringArgumentType.word());
                
                if (staticValues != null && !staticValues.isEmpty()) {
                    builder.suggests((context, suggestionsBuilder) -> {
                        for (String value : staticValues) {
                            if (value.startsWith(suggestionsBuilder.getRemaining().toLowerCase())) {
                                suggestionsBuilder.suggest(value);
                            }
                        }
                        return suggestionsBuilder.buildFuture();
                    });
                }
                
                return builder;
            default:
                // Default a stringa per tipi non riconosciuti
                LOGGER.warn("Tipo di parametro non riconosciuto: {}, uso String come default", type);
                return Commands.argument(name, StringArgumentType.string());
        }
    }
    
    private static RequiredArgumentBuilder<CommandSourceStack, ?> createArgumentForType(String name, String type) {
        return createArgumentForType(name, type, null);
    }
    
    /**
     * Estrae i valori dei parametri dal contesto del comando
     */
    private static Object[] extractParameterValues(CommandContext<CommandSourceStack> context, List<ParameterDefinition> parameters) {
        Object[] values = new Object[parameters.size()];
        
        for (int i = 0; i < parameters.size(); i++) {
            String paramName = "param" + i;
            ParameterDefinition param = parameters.get(i);
            
            try {
                switch (param.type.toLowerCase()) {
                    case "string":
                    case "word":
                        values[i] = StringArgumentType.getString(context, paramName);
                        break;
                    case "int":
                        values[i] = IntegerArgumentType.getInteger(context, paramName);
                        break;
                    case "float":
                    case "double":
                        values[i] = DoubleArgumentType.getDouble(context, paramName);
                        break;
                    case "boolean":
                        values[i] = BoolArgumentType.getBool(context, paramName);
                        break;
                    case "target":
                        values[i] = EntityArgument.getPlayers(context, paramName);
                        break;
                    default:
                        // Default a null per tipi non riconosciuti
                        values[i] = null;
                }
            } catch (Exception e) {
                // Parametro non presente (probabilmente opzionale)
                values[i] = null;
            }
        }
        
        return values;
    }
    
    /**
     * Esegue una macro di comandi per un giocatore
     */
    public static int executeMacro(CommandContext<CommandSourceStack> context, String macroId, Object[] paramValues) throws CommandSyntaxException {
        if (!MACRO_REGISTRY.containsKey(macroId)) {
            LOGGER.warn("Tentativo di eseguire una macro inesistente: {}", macroId);
            return 0;
        }
        
        MacroDefinition macro = MACRO_REGISTRY.get(macroId);
        
        // Ottieni il giocatore che ha eseguito il comando
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // Verifica se il giocatore ha il livello di permesso richiesto
        if (macro.level > 0 && player.getServer() != null && !player.hasPermissions(macro.level)) {
            LOGGER.debug("Il giocatore {} non ha i permessi necessari (livello {}) per la macro {}", 
                player.getName().getString(), macro.level, macroId);
            throw ERROR_PERMISSION.create();
        }
        
        // Verifica degli stages (implementazione futura)
        if (macro.hasStageRequirements() && !macro.checkStages(player)) {
            LOGGER.debug("Il giocatore {} non ha gli stages necessari per la macro {}", 
                player.getName().getString(), macroId);
            // Qui in futuro si potrà lanciare un'eccezione appropriata o gestire diversamente
            return 0;
        }
        
        // Esegui la macro con i parametri
        return executeMacroWithParams(macroId, player, paramValues) ? 1 : 0;
    }
    
    /**
     * Esegue una macro di comandi per un giocatore
     */
    public static boolean executeMacro(String macroId, ServerPlayer player) {
        if (!MACRO_REGISTRY.containsKey(macroId)) {
            LOGGER.warn("Tentativo di eseguire una macro inesistente: {}", macroId);
            return false;
        }
        
        MacroDefinition macro = MACRO_REGISTRY.get(macroId);
        
        // Verifica se il giocatore ha il livello di permesso richiesto
        if (macro.level > 0 && player.getServer() != null && !player.hasPermissions(macro.level)) {
            LOGGER.debug("Il giocatore {} non ha i permessi necessari (livello {}) per la macro {}", 
                player.getName().getString(), macro.level, macroId);
            return false;
        }
        
        // Esegui la macro senza parametri
        return executeMacroWithParams(macroId, player, new Object[0]);
    }
    
    /**
     * Esegue una macro di comandi con parametri
     */
    private static boolean executeMacroWithParams(String macroId, ServerPlayer player, Object[] paramValues) {
        MacroDefinition macro = MACRO_REGISTRY.get(macroId);
        
        // Annulla qualsiasi sequenza di comandi attiva per questo giocatore
        UUID playerUUID = player.getUUID();
        ScheduledFuture<?> activeFuture = ACTIVE_COMMANDS.get(playerUUID);
        if (activeFuture != null && !activeFuture.isDone()) {
            activeFuture.cancel(true);
            ACTIVE_COMMANDS.remove(playerUUID);
        }
        
        // Avvia la sequenza di comandi
        if (player.level() instanceof ServerLevel serverLevel) {
            executeActionSequence(player, serverLevel, macro.actions, paramValues, 0);
            return true;
        }
        
        return false;
    }
    
    /**
     * Esegue una sequenza di azioni con ritardi
     */
    private static void executeActionSequence(ServerPlayer player, ServerLevel level, List<MacroAction> actions, 
                                             Object[] paramValues, int index) {
        if (index >= actions.size() || player.isRemoved() || !player.isAlive()) {
            // Fine della sequenza o giocatore non più valido
            ACTIVE_COMMANDS.remove(player.getUUID());
            return;
        }
        
        MacroAction action = actions.get(index);
        
        if (action.type == MacroActionType.EXECUTE) {
            // Sostituisci i parametri nel comando
            String commandWithParams = replaceParameters(action.command, paramValues);
            
            // Esegui il comando immediatamente
            executeCommand(player, level, commandWithParams);
            
            // Pianifica la prossima azione
            scheduleNextAction(player, level, actions, paramValues, index + 1);
        } else if (action.type == MacroActionType.DELAY) {
            // Pianifica la prossima azione con ritardo
            int delayTicks = action.delayTicks;
            long delayMillis = (long) delayTicks * 50; // 1 tick = ~50ms
            
            UUID playerUUID = player.getUUID();
            ScheduledFuture<?> future = COMMAND_EXECUTOR.schedule(() -> {
                // Dopo il ritardo, esegui la prossima azione
                executeActionSequence(player, level, actions, paramValues, index + 1);
            }, delayMillis, TimeUnit.MILLISECONDS);
            
            // Memorizza il future per potenziale cancellazione
            ACTIVE_COMMANDS.put(playerUUID, future);
        }
    }
    
    /**
     * Sostituisce i riferimenti ai parametri in un comando
     */
    private static String replaceParameters(String command, Object[] paramValues) {
        if (paramValues == null || paramValues.length == 0) {
            return command;
        }
        
        LOGGER.debug("Sostituzione parametri nel comando: '{}' con valori: {}", command, paramValues);
        
        // Pattern migliorato che cattura tutti i casi: #0, minecraft:#0, etc.
        // Questo pattern matcha #n sia da solo che all'interno di una stringa più complessa
        final Pattern enhancedPattern = Pattern.compile("([\\w-]+:)?#(\\d+)");
        Matcher enhancedMatcher = enhancedPattern.matcher(command);
        StringBuffer result = new StringBuffer();
        
        while (enhancedMatcher.find()) {
            String prefix = enhancedMatcher.group(1) != null ? enhancedMatcher.group(1) : "";
            int paramIndex = Integer.parseInt(enhancedMatcher.group(2));
            
            if (paramIndex < paramValues.length && paramValues[paramIndex] != null) {
                String paramValue = formatParameterValue(paramValues[paramIndex]);
                // Se c'è un prefisso (come 'minecraft:'), lo manteniamo
                String replacement = prefix + paramValue;
                enhancedMatcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
                LOGGER.debug("Sostituito parametro '{}#{}' con '{}'", prefix, paramIndex, replacement);
            }
        }
        
        enhancedMatcher.appendTail(result);
        String finalCommand = result.toString();
        
        // Verifica che la sostituzione sia avvenuta per tutti i parametri
        if (command.contains("#") && finalCommand.contains("#")) {
            // Alcuni parametri potrebbero non essere stati sostituiti
            // Tentiamo con il pattern originale come fallback
            Matcher originalMatcher = PARAMETER_PATTERN.matcher(finalCommand);
            StringBuffer secondResult = new StringBuffer();
            
            while (originalMatcher.find()) {
                int paramIndex = Integer.parseInt(originalMatcher.group(1));
                if (paramIndex < paramValues.length && paramValues[paramIndex] != null) {
                    String replacement = formatParameterValue(paramValues[paramIndex]);
                    originalMatcher.appendReplacement(secondResult, Matcher.quoteReplacement(replacement));
                    LOGGER.debug("Fallback: sostituito parametro #{} con '{}'", paramIndex, replacement);
                }
            }
            
            originalMatcher.appendTail(secondResult);
            finalCommand = secondResult.toString();
        }
        
        LOGGER.debug("Comando risultante dopo sostituzione: '{}'", finalCommand);
        return finalCommand;
    }
    
    /**
     * Formatta un valore di parametro per l'inclusione in un comando
     */
    private static String formatParameterValue(Object value) {
        if (value == null) {
            return "";
        } else if (value instanceof EntitySelector) {
            return "@s"; // Sostituisci con selettore appropriato
        } else {
            String valueStr = value.toString();
            
            // Se il valore è usato in un pattern come minecraft:#0, rimuovi 'minecraft:' per evitare problemi
            if (valueStr.length() > 0) {
                return valueStr;
            } else {
                return "";
            }
        }
    }
    
    /**
     * Pianifica la prossima azione nella sequenza
     */
    private static void scheduleNextAction(ServerPlayer player, ServerLevel level, List<MacroAction> actions, 
                                          Object[] paramValues, int nextIndex) {
        if (nextIndex >= actions.size()) {
            // Fine della sequenza
            ACTIVE_COMMANDS.remove(player.getUUID());
            return;
        }
        
        // Ritardo minimo tra i comandi per sicurezza (1 tick)
        UUID playerUUID = player.getUUID();
        ScheduledFuture<?> future = COMMAND_EXECUTOR.schedule(() -> {
            executeActionSequence(player, level, actions, paramValues, nextIndex);
        }, 50, TimeUnit.MILLISECONDS);
        
        // Memorizza il future per potenziale cancellazione
        ACTIVE_COMMANDS.put(playerUUID, future);
    }
    
    /**
     * Esegue un singolo comando
     */
    private static void executeCommand(ServerPlayer player, ServerLevel level, String command) {
        try {
            if (player.getServer() != null) {
                // Se dopo la sostituzione dei parametri ci sono ancora pattern #n,
                // lo segnaliamo ma procediamo comunque con l'esecuzione
                if (command.matches(".*#\\d+.*")) {
                    LOGGER.warn("Possibile problema di sostituzione parametri nel comando: {}", command);
                }
                
                // Gestione speciale per comandi critici
                if (command.trim().equals("reload")) {
                    LOGGER.info("Rilevato comando 'reload' in una macro, viene eseguito in modo sicuro");
                    
                    // Esegui il comando reload in modo sicuro
                    CommandSourceStack source = player.createCommandSourceStack();
                    try {
                        player.getServer().getCommands().performPrefixedCommand(source, command);
                        LOGGER.info("Comando reload eseguito con successo, ricaricamento macro in corso...");
                        
                        // Schedula la ricarica delle macro dopo un ritardo per evitare conflitti
                        // Aumentiamo il ritardo a 2 secondi per assicurarsi che tutti i processi di reload siano completati
                        COMMAND_EXECUTOR.schedule(() -> {
                            try {
                                LOGGER.info("Esecuzione ricaricamento macro programmato dopo reload...");
                                net.unfamily.iskautils.command.MacroLoader.reloadAllMacros();
                                
                                // Ottieni una reference al server e forza la sincronizzazione dei comandi
                                net.minecraft.server.MinecraftServer server = player.getServer();
                                if (server != null) {
                                    LOGGER.info("Sincronizzazione forzata dei comandi con i client dopo reload...");
                                    for (ServerPlayer serverPlayer : server.getPlayerList().getPlayers()) {
                                        server.getCommands().sendCommands(serverPlayer);
                                    }
                                }
                            } catch (Exception e) {
                                LOGGER.error("Errore nel ricaricamento delle macro dopo reload: {}", e.getMessage());
                                e.printStackTrace();
                            }
                        }, 2000, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        LOGGER.error("Errore nell'esecuzione del comando reload: {}", e.getMessage());
                    }
                } else {
                    // Esecuzione normale per altri comandi
                    CommandSourceStack source = player.createCommandSourceStack();
                    player.getServer().getCommands().performPrefixedCommand(source, command);
                    
                    LOGGER.debug("Eseguito comando '{}' per il giocatore {} nella posizione {}", 
                        command, player.getName().getString(), player.blockPosition());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Errore nell'esecuzione del comando '{}' per il giocatore {}: {}", 
                command, player.getName().getString(), e.getMessage());
        }
    }
    
    /**
     * Arresta l'executor service
     */
    public static void shutdown() {
        try {
            COMMAND_EXECUTOR.shutdown();
            if (!COMMAND_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                COMMAND_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            COMMAND_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Definizione di una macro di comandi
     */
    public static class MacroDefinition {
        private final String id;
        private final List<MacroAction> actions;
        private final List<ParameterDefinition> parameters;
        private final int level;
        private final List<String> requiredStages;
        private final StagesLogic stagesLogic;
        
        /**
         * Costruttore per una nuova definizione di macro
         * @param id Identificatore della macro
         * @param actions Lista di azioni da eseguire
         * @param parameters Lista di parametri
         * @param level Livello di permesso richiesto
         * @param requiredStages Lista di stages richiesti
         * @param stagesLogic Logica di valutazione degli stages (AND/OR)
         */
        public MacroDefinition(String id, List<MacroAction> actions, List<ParameterDefinition> parameters, 
                              int level, List<String> requiredStages, StagesLogic stagesLogic) {
            this.id = id;
            this.actions = new ArrayList<>(actions);
            this.parameters = parameters != null ? new ArrayList<>(parameters) : new ArrayList<>();
            this.level = Math.max(0, level);
            this.requiredStages = requiredStages != null ? new ArrayList<>(requiredStages) : new ArrayList<>();
            this.stagesLogic = stagesLogic != null ? stagesLogic : StagesLogic.AND;
        }
        
        /**
         * Costruttore semplificato senza stages
         */
        public MacroDefinition(String id, List<MacroAction> actions, List<ParameterDefinition> parameters, int level) {
            this(id, actions, parameters, level, null, null);
        }
        
        /**
         * Crea una macro da un oggetto JSON
         */
        public static MacroDefinition fromJson(JsonObject json) {
            String id = json.has("command") ? json.get("command").getAsString() : 
                        json.has("id") ? json.get("id").getAsString() : "unnamed_macro";
                
            int level = json.has("level") ? json.get("level").getAsInt() : 0;
            
            List<MacroAction> actions = new ArrayList<>();
            List<ParameterDefinition> parameters = new ArrayList<>();
            List<String> stages = new ArrayList<>();
            StagesLogic stagesLogic = StagesLogic.AND; // Default a AND
            
            // Parsing della logica degli stages
            if (json.has("stages_logic")) {
                String logicStr = json.get("stages_logic").getAsString();
                if ("OR".equalsIgnoreCase(logicStr)) {
                    stagesLogic = StagesLogic.OR;
                }
            }
            
            // Parsing degli stages richiesti
            if (json.has("stages") && json.get("stages").isJsonArray()) {
                JsonArray stagesArray = json.get("stages").getAsJsonArray();
                
                for (JsonElement stageElem : stagesArray) {
                    if (stageElem.isJsonObject()) {
                        JsonObject stageObj = stageElem.getAsJsonObject();
                        if (stageObj.has("stage")) {
                            stages.add(stageObj.get("stage").getAsString());
                        }
                    }
                }
            }
            
            // Parsing dei parametri
            if (json.has("parameters") && json.get("parameters").isJsonArray()) {
                JsonArray paramsArray = json.get("parameters").getAsJsonArray();
                
                for (int i = 0; i < paramsArray.size(); i++) {
                    JsonElement paramElem = paramsArray.get(i);
                    if (paramElem.isJsonObject()) {
                        JsonObject paramObj = paramElem.getAsJsonObject();
                        
                        String type = paramObj.has("type") ? paramObj.get("type").getAsString() : "string";
                        boolean required = !paramObj.has("required") || paramObj.get("required").getAsBoolean();
                        
                        // Gestione parametri di tipo static con lista di valori
                        if ("static".equals(type.toLowerCase()) && paramObj.has("list") && paramObj.get("list").isJsonArray()) {
                            List<String> staticValues = new ArrayList<>();
                            JsonArray valuesArray = paramObj.get("list").getAsJsonArray();
                            
                            for (JsonElement valueElem : valuesArray) {
                                if (valueElem.isJsonObject() && valueElem.getAsJsonObject().has("declare")) {
                                    staticValues.add(valueElem.getAsJsonObject().get("declare").getAsString());
                                }
                            }
                            
                            parameters.add(new ParameterDefinition(type, required, staticValues));
                        } else {
                            parameters.add(new ParameterDefinition(type, required));
                        }
                    }
                }
            }
            
            // Parsing delle azioni dalla proprietà "do"
            if (json.has("do") && json.get("do").isJsonArray()) {
                JsonArray actionsArray = json.get("do").getAsJsonArray();
                
                for (JsonElement actionElem : actionsArray) {
                    if (actionElem.isJsonObject()) {
                        JsonObject actionObj = actionElem.getAsJsonObject();
                        
                        // Controlla il tipo di azione
                        if (actionObj.has("execute")) {
                            // Esecuzione del comando
                            String command = actionObj.get("execute").getAsString();
                            actions.add(new MacroAction(MacroActionType.EXECUTE, command, 0));
                        } else if (actionObj.has("delay")) {
                            // Ritardo in tick
                            int delayTicks = actionObj.get("delay").getAsInt();
                            actions.add(new MacroAction(MacroActionType.DELAY, "", delayTicks));
                        }
                    }
                }
            }
            
            return new MacroDefinition(id, actions, parameters, level, stages, stagesLogic);
        }
        
        public String getId() {
            return id;
        }
        
        public List<MacroAction> getActions() {
            return new ArrayList<>(actions);
        }
        
        public List<ParameterDefinition> getParameters() {
            return new ArrayList<>(parameters);
        }
        
        public int getLevel() {
            return level;
        }
        
        public List<String> getRequiredStages() {
            return new ArrayList<>(requiredStages);
        }
        
        public StagesLogic getStagesLogic() {
            return stagesLogic;
        }
        
        public boolean hasStageRequirements() {
            return !requiredStages.isEmpty();
        }
        
        public boolean hasParameters() {
            return !parameters.isEmpty();
        }
        
        /**
         * Verifica se il giocatore ha sbloccato gli stages necessari
         * Questa è solo la struttura, l'implementazione effettiva sarà aggiunta in futuro
         */
        public boolean checkStages(ServerPlayer player) {
            // Implementazione futura: verificare se il giocatore ha gli stages richiesti
            // Per ora, restituisce sempre true
            return true;
        }
    }
    
    /**
     * Enum per la logica di valutazione degli stages
     */
    public enum StagesLogic {
        AND, // Tutti gli stages sono necessari
        OR   // Almeno uno degli stages è necessario
    }
    
    /**
     * Definizione di un parametro per una macro
     */
    public static class ParameterDefinition {
        private final String type;
        private final boolean required;
        private final List<String> staticValues;
        
        public ParameterDefinition(String type, boolean required) {
            this(type, required, null);
        }
        
        public ParameterDefinition(String type, boolean required, List<String> staticValues) {
            this.type = type;
            this.required = required;
            this.staticValues = staticValues != null ? new ArrayList<>(staticValues) : null;
        }
        
        public String getType() {
            return type;
        }
        
        public boolean isRequired() {
            return required;
        }
        
        public List<String> getStaticValues() {
            return staticValues != null ? new ArrayList<>(staticValues) : null;
        }
    }
    
    /**
     * Rappresenta un'azione all'interno di una macro
     */
    public static class MacroAction {
        private final MacroActionType type;
        private final String command;
        private final int delayTicks;
        
        public MacroAction(MacroActionType type, String command, int delayTicks) {
            this.type = type;
            this.command = command;
            this.delayTicks = delayTicks;
        }
        
        public MacroActionType getType() {
            return type;
        }
        
        public String getCommand() {
            return command;
        }
        
        public int getDelayTicks() {
            return delayTicks;
        }
    }
    
    /**
     * Enum per i tipi di azioni nelle macro
     */
    public enum MacroActionType {
        EXECUTE,
        DELAY
    }
} 