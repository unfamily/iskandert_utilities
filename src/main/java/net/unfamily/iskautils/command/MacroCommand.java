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
import net.minecraft.server.MinecraftServer;
import net.unfamily.iskautils.stage.StageRegistry;
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
 * System for executing command macros with delays
 */
public class MacroCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Thread pool for scheduled command execution
    private static final ScheduledExecutorService COMMAND_EXECUTOR = Executors.newScheduledThreadPool(2);
    
    // Active commands: Player UUID -> future for possible cancellation
    private static final Map<UUID, ScheduledFuture<?>> ACTIVE_COMMANDS = new ConcurrentHashMap<>();
    
    // Registry of available macros
    private static final Map<String, MacroDefinition> MACRO_REGISTRY = new HashMap<>();
    
    // Pattern to find parameters in commands (e.g. #0, #1, etc.)
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("#(\\d+)");
    
    // Exception for missing permissions
    public static final SimpleCommandExceptionType ERROR_PERMISSION = new SimpleCommandExceptionType(
            Component.translatable("commands.iska_utils.macro.no_permission"));
    
    /**
     * Registers a new command macro
     */
    public static void registerMacro(String id, MacroDefinition macro) {
        MACRO_REGISTRY.put(id, macro);
        LOGGER.info("Registered command macro: {} with {} actions", id, macro.actions.size());
    }
    
    /**
     * Removes a macro from the registry
     */
    public static void unregisterMacro(String id) {
        MACRO_REGISTRY.remove(id);
    }
    
    /**
     * Checks if a macro with the specified ID exists
     */
    public static boolean hasMacro(String id) {
        return MACRO_REGISTRY.containsKey(id);
    }
    
    /**
     * Gets a macro from the registry
     */
    public static MacroDefinition getMacro(String id) {
        return MACRO_REGISTRY.get(id);
    }
    
    /**
     * Gets the list of all available macros
     */
    public static Iterable<String> getAvailableMacros() {
        return MACRO_REGISTRY.keySet();
    }
    
    /**
     * Registers a command in the dispatcher for a macro
     */
    public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher, String macroId, MacroDefinition macro) {
        if (macro.hasParameters()) {
            // Macro with parameters
            registerParameterizedCommand(dispatcher, macroId, macro);
        } else {
            // Macro without parameters (simple)
            dispatcher.register(Commands.literal(macroId)
                .requires(source -> source.hasPermission(0))
                .executes(context -> executeMacro(context, macroId, new Object[0])));
            
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Registered command /{}", macroId);
            }
        }
    }
    
    /**
     * Registers a parameterized command
     */
    private static void registerParameterizedCommand(CommandDispatcher<CommandSourceStack> dispatcher, String macroId, MacroDefinition macro) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(macroId)
            .requires(source -> source.hasPermission(0));
        
        // Build the argument structure
        ArgumentBuilder<CommandSourceStack, ?> argumentBuilder = buildArgumentTree(command, macro.parameters, 0, args -> {
            return executeMacro(args, macroId, extractParameterValues(args, macro.parameters));
        });
        
        // Register the command
        dispatcher.register((LiteralArgumentBuilder<CommandSourceStack>) argumentBuilder);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Registered parameterized command /{} with {} parameters", macroId, macro.parameters.size());
        }
    }
    
    /**
     * Recursively builds the argument tree for the command
     */
    private static ArgumentBuilder<CommandSourceStack, ?> buildArgumentTree(
            ArgumentBuilder<CommandSourceStack, ?> builder, 
            List<ParameterDefinition> parameters, 
            int index, 
            Command<CommandSourceStack> executor) {
        
        if (index >= parameters.size()) {
            // End of parameters, add the executor
            return builder.executes(executor);
        }
        
        ParameterDefinition param = parameters.get(index);
        String paramName = "param" + index;
        
        // Create the argument based on type
        RequiredArgumentBuilder<CommandSourceStack, ?> argBuilder;
        if ("static".equals(param.type.toLowerCase()) && param.staticValues != null && !param.staticValues.isEmpty()) {
            argBuilder = createArgumentForType(paramName, param.type, param.staticValues);
        } else {
            argBuilder = createArgumentForType(paramName, param.type);
        }
        
        // Add the remaining parameters
        ArgumentBuilder<CommandSourceStack, ?> nextBuilder = buildArgumentTree(argBuilder, parameters, index + 1, executor);
        
        if (!param.required) {
            // If the parameter is optional, also add an executor at the current level
            builder.executes(ctx -> executeMacro(ctx, 
                    getCommandName(builder), 
                    extractParameterValues(ctx, parameters.subList(0, index))));
        }
        
        // Connect the current parameter to the builder
        return builder.then(nextBuilder);
    }
    
    /**
     * Extracts the command name from a builder
     */
    private static String getCommandName(ArgumentBuilder<CommandSourceStack, ?> builder) {
        if (builder instanceof LiteralArgumentBuilder) {
            return ((LiteralArgumentBuilder<CommandSourceStack>)builder).getLiteral();
        } else {
            return "unknown";
        }
    }
    
    /**
     * Creates a Brigadier argument based on the specified type
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
                return Commands.argument(name, EntityArgument.player());
            case "static":
                // For static values, we use a word argument with suggestions
                RequiredArgumentBuilder<CommandSourceStack, String> arg = Commands.argument(name, StringArgumentType.word());
                
                // Add suggestions
                if (staticValues != null && !staticValues.isEmpty()) {
                    arg.suggests((context, builder) -> {
                        for (String value : staticValues) {
                            if (value.startsWith(builder.getRemaining().toLowerCase())) {
                                builder.suggest(value);
                            }
                        }
                        return builder.buildFuture();
                    });
                }
                
                return arg;
            default:
                // Default to string for unknown types
                return Commands.argument(name, StringArgumentType.string());
        }
    }
    
    /**
     * Overload for createArgumentForType without static values
     */
    private static RequiredArgumentBuilder<CommandSourceStack, ?> createArgumentForType(String name, String type) {
        return createArgumentForType(name, type, null);
    }
    
    /**
     * Extracts parameter values from the command context
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
                    case "static": // Treat static parameters as normal strings
                        values[i] = StringArgumentType.getString(context, paramName);
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Extracted parameter {}: '{}' (type: {})", paramName, values[i], param.type);
                        }
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
                        values[i] = EntityArgument.getPlayer(context, paramName);
                        break;
                    default:
                        values[i] = StringArgumentType.getString(context, paramName);
                        break;
                }
            } catch (Exception e) {
                // Parameter not provided or invalid
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Could not extract parameter {}: {}", paramName, e.getMessage());
                }
                values[i] = null;
            }
        }
        
        return values;
    }
    
    /**
     * Executes a macro of commands for a command source (player or command block)
     */
    public static int executeMacro(CommandContext<CommandSourceStack> context, String macroId, Object[] paramValues) throws CommandSyntaxException {
        if (!MACRO_REGISTRY.containsKey(macroId)) {
            LOGGER.warn("Attempt to execute non-existent macro: {}", macroId);
            return 0;
        }
        
        MacroDefinition macro = MACRO_REGISTRY.get(macroId);
        
        // Check if context.getSource() is null
        CommandSourceStack source = context.getSource();
        if (source == null) {
            LOGGER.error("CommandSourceStack is null during execution of macro {}", macroId);
            return 0;
        }
        
        // Verifies if the source is a player
        ServerPlayer player = null;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            // The source is not a player, it could be a command block or the server
            // We continue execution without a player
            LOGGER.debug("Macro {} executed by a non-player source", macroId);
        }
        
        // Check permission level
        if (!source.hasPermission(macro.getLevel())) {
            if (player != null) {
                source.sendFailure(Component.literal("§cYou don't have permission to use this command"));
            } else {
                LOGGER.warn("Command source doesn't have permission to execute macro {}", macroId);
            }
            return 0;
        }
        
        // Check required stages only for players
        if (player != null && macro.hasStageRequirements() && !macro.checkStages(player)) {
            source.sendFailure(Component.literal("§cYou don't have the required stages to use this command"));
            LOGGER.debug("Player {} does not have the required stages to execute macro {}", 
                player.getName().getString(), macroId);
            return 0;
        }
        
        // Execute the macro actions
        return executeActions(source, player, macro, paramValues);
    }
    
    /**
     * Executes a macro's actions for a command source
     */
    private static int executeActions(CommandSourceStack source, ServerPlayer player, MacroDefinition macro, Object[] paramValues) {
        int successCount = 0;
        MinecraftServer server = source.getServer();
        
        if (server == null) {
            LOGGER.error("Server is null during execution of macro");
            return 0;
        }
        
        for (MacroAction action : macro.getActions()) {
            // For IF actions, check conditions first
            if (action.getType() == MacroActionType.IF) {
                // Check the conditions - skip for non-player sources when stage conditions are present
                if (player == null && macro.hasStageRequirements()) {
                    LOGGER.debug("Skipping IF block with stage conditions for non-player source");
                    continue;
                }
                
                if (player != null && !checkIfConditions(player, action, macro)) {
                    continue; // Skip this action if conditions aren't met
                }
                
                // Execute sub-actions if conditions are met
                for (MacroAction subAction : action.getSubActions()) {
                    String command = subAction.getCommand();
                    
                    // Replace parameters if any
                    if (paramValues != null && paramValues.length > 0) {
                        command = replaceParameters(command, paramValues);
                    }
                    
                    // Execute the command
                    try {
                        if (server != null && subAction.getType() == MacroActionType.EXECUTE) {
                            // Prepare the CommandSourceStack with appropriate position
                            executeCommandWithSource(source, player, server, command);
                            successCount++;
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error executing command: {}", command, e);
                    }
                }
                continue;
            }
            
            // For regular execute actions
            if (action.getType() == MacroActionType.EXECUTE) {
                String command = action.getCommand();
                
                // Replace parameters if any
                if (paramValues != null && paramValues.length > 0) {
                    command = replaceParameters(command, paramValues);
                }
                
                // Execute the command
                try {
                    if (server != null) {
                        // Prepare the CommandSourceStack with appropriate position
                        executeCommandWithSource(source, player, server, command);
                        successCount++;
                    }
                } catch (Exception e) {
                    LOGGER.error("Error executing command: {}", command, e);
                }
            }
        }
        
        return successCount;
    }
    
    /**
     * Executes a command with the appropriate source stack
     */
    private static void executeCommandWithSource(CommandSourceStack originalSource, ServerPlayer player, MinecraftServer server, String command) {
        try {
            // Special handling for critical commands
            if (command.trim().equals("reload")) {
                LOGGER.info("Detected 'reload' command in a macro, executing safely");
                
                try {
                    server.getCommands().performPrefixedCommand(originalSource, command);
                } catch (Exception e) {
                    LOGGER.error("Error executing 'reload' command: {}", e.getMessage());
                }
                return;
            }
            
            // Prepare the CommandSourceStack with appropriate position
            CommandSourceStack source;
            
            if (player != null) {
                // If player source, use player position
                source = player.createCommandSourceStack()
                    .withPosition(player.position())       // Set player position
                    .withEntity(player)                    // Set player entity
                    .withRotation(player.getRotationVector()); // Set player rotation
                
                server.getCommands().performPrefixedCommand(source, command);
                
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Executed command: '{}' as player: {} at position: {}", 
                        command, player.getName().getString(), player.position());
                }
            } else {
                // Not a player source, use original position if available or (0,0,0)
                net.minecraft.world.phys.Vec3 position = originalSource.getPosition();
                
                // If source is a command block, use that position
                if (position.x() == 0 && position.y() == 0 && position.z() == 0) {
                    // Seems to be default position, use (0,0,0) as fallback
                    LOGGER.debug("Using default position (0,0,0) for command: {}", command);
                } else {
                    LOGGER.debug("Using command block/entity position: {} for command: {}", position, command);
                }
                
                // Use original source
                server.getCommands().performPrefixedCommand(originalSource, command);
                
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Executed command: '{}' as non-player source at position: {}", 
                        command, originalSource.getPosition());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error executing command '{}': {}", command, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Executes a command macro for a player
     */
    public static boolean executeMacro(String macroId, ServerPlayer player) {
        if (!MACRO_REGISTRY.containsKey(macroId)) {
            LOGGER.warn("Attempt to execute non-existent macro: {}", macroId);
            return false;
        }
        
        MacroDefinition macro = MACRO_REGISTRY.get(macroId);
        
        // Check if player has required permission level
        if (macro.level > 0 && player.getServer() != null && !player.hasPermissions(macro.level)) {
            LOGGER.debug("Player {} does not have required permission level {} for macro {}", 
                player.getName().getString(), macro.level, macroId);
            return false;
        }
        
        // Check required stages
        if (macro.hasStageRequirements() && !macro.checkStages(player)) {
            LOGGER.debug("Player {} does not have the required stages to execute macro {}", 
                player.getName().getString(), macroId);
            return false;
        }
        
        // Execute macro without parameters
        return executeMacroWithParams(macroId, player, new Object[0]);
    }
    
    /**
     * Executes a command macro with parameters
     */
    private static boolean executeMacroWithParams(String macroId, ServerPlayer player, Object[] paramValues) {
        MacroDefinition macro = MACRO_REGISTRY.get(macroId);
        
        // Cancel any active command sequence for this player
        UUID playerUUID = player.getUUID();
        ScheduledFuture<?> activeFuture = ACTIVE_COMMANDS.get(playerUUID);
        if (activeFuture != null && !activeFuture.isDone()) {
            activeFuture.cancel(true);
            ACTIVE_COMMANDS.remove(playerUUID);
        }
        
        // Start the command sequence
        if (player.level() instanceof ServerLevel serverLevel) {
            executeActionSequence(player, serverLevel, macro.actions, paramValues, 0);
            return true;
        }
        
        return false;
    }
    
    /**
     * Executes a sequence of actions with delays
     */
    private static void executeActionSequence(ServerPlayer player, ServerLevel level, List<MacroAction> actions, 
                                             Object[] paramValues, int index) {
        if (index >= actions.size() || player.isRemoved() || !player.isAlive()) {
            // End of sequence or player no longer valid
            ACTIVE_COMMANDS.remove(player.getUUID());
            return;
        }
        
        MacroAction action = actions.get(index);
        
        // Check action-specific stage requirements if any
        if (action.hasStageRequirements() && !checkActionStages(player, action.getStageRequirements())) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Action at index {} skipped due to stage requirements not met", index);
            }
            // Skip this action and move to the next
            scheduleNextAction(player, level, actions, paramValues, index + 1);
            return;
        }
        
        if (action.type == MacroActionType.EXECUTE) {
            // Replace parameters in the command
            String commandWithParams = replaceParameters(action.command, paramValues);
            
            // Execute the command immediately
            executeCommand(player, level, commandWithParams);
            
            // Schedule the next action
            scheduleNextAction(player, level, actions, paramValues, index + 1);
        } else if (action.type == MacroActionType.DELAY) {
            // Schedule the next action with delay
            int delayTicks = action.delayTicks;
            
            // We use exactly 50ms per tick, but add a small margin 
            // to ensure the minimum delay is respected
            long delayMillis = (long) delayTicks * 50 + 5; 
            
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Scheduling delay of {} ticks ({} ms) for player {}", 
                    delayTicks, delayMillis, player.getName().getString());
            }
            
            UUID playerUUID = player.getUUID();
            ScheduledFuture<?> future = COMMAND_EXECUTOR.schedule(() -> {
                // After the delay, execute the next action
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Executing action after delay of {} ticks for player {}", 
                        delayTicks, player.getName().getString());
                }
                executeActionSequence(player, level, actions, paramValues, index + 1);
            }, delayMillis, TimeUnit.MILLISECONDS);
            
            // Remember the future for potential cancellation
            ACTIVE_COMMANDS.put(playerUUID, future);
        } else if (action.type == MacroActionType.IF) {
            // Handle IF blocks by checking conditions and executing sub-actions if conditions are met
            List<Integer> indices = action.getConditionIndices();
            List<MacroAction> subActions = action.getSubActions();
            
            // Get current macro for this player
            UUID playerUUID = player.getUUID();
            String currentMacroId = "unknown"; // Default
            
            // Try to find the current macro being executed
            for (String macroId : MACRO_REGISTRY.keySet()) {
                MacroDefinition macro = MACRO_REGISTRY.get(macroId);
                if (macro != null && macro.getActions().contains(action)) {
                    currentMacroId = macroId;
                    
                    // Check if conditions are met
                    if (checkConditionsByIndices(player, indices, macro.getRequiredStages(), macro.getStagesLogic())) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("IF conditions met in macro {}, executing sub-actions", currentMacroId);
                        }
                        
                        // Verify if there are delays in sub-actions
                        if (containsDelayAction(subActions)) {
                            // If there are delays, execute sub-actions in a special way
                            // using a new version that handles delays correctly
                            ScheduledFuture<?> future = COMMAND_EXECUTOR.schedule(() -> {
                                // Execute sub-actions with special delay handling
                                executeActionSequenceWithDelayHandling(player, level, subActions, paramValues, 0, () -> {
                                    // When all sub-actions (including delays) are completed, 
                                    // continue with the next action in the main sequence
                                    scheduleNextAction(player, level, actions, paramValues, index + 1);
                                });
                            }, 0, TimeUnit.MILLISECONDS);
                            
                            ACTIVE_COMMANDS.put(playerUUID, future);
                        } else {
                            // No delays in sub-actions, we can use the normal method
                            ScheduledFuture<?> future = COMMAND_EXECUTOR.schedule(() -> {
                                // Execute sub-actions as a separate sequence
                                executeActionSequence(player, level, subActions, paramValues, 0);
                                
                                // After sub-actions, continue with the next action in the main sequence
                                scheduleNextAction(player, level, actions, paramValues, index + 1);
                            }, 0, TimeUnit.MILLISECONDS);
                            
                            ACTIVE_COMMANDS.put(playerUUID, future);
                        }
                        
                        return; // Exit early as we've scheduled the next actions
                    }
                    
                    break; // Found the macro, no need to continue searching
                }
            }
            
            // If conditions are not met or macro not found, continue with next action
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("IF conditions not met in macro {}, skipping sub-actions", currentMacroId);
            }
            scheduleNextAction(player, level, actions, paramValues, index + 1);
        }
    }
    
    /**
     * Checks stage conditions for an action
     */
    private static boolean checkActionStages(ServerPlayer player, List<StageRequirement> requirements) {
        if (requirements == null || requirements.isEmpty()) {
            return true; // No requirements to check
        }
        
        // Since we can't use MacroDefinition's method directly from a static context,
        // we implement the same logic here
        for (StageRequirement requirement : requirements) {
            boolean hasStage = checkPlayerHasStage(player, requirement);
            if (!hasStage) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Static version of the stage checking logic
     */
    private static boolean checkPlayerHasStage(ServerPlayer player, StageRequirement requirement) {
        // Get game stages for the player
        boolean hasStage = false;
        
        // Use StageRegistry to check if player has the stage
        if (requirement.getType() == StageType.PLAYER) {
            try {
                // Try to use actual stage registry
                hasStage = StageRegistry.playerHasStage(player, requirement.getStageId());
                
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Checking stage '{}' for player {}: {}", 
                        requirement.getStageId(), player.getName().getString(), hasStage);
                }
            } catch (Exception e) {
                // Fallback to debug mode or if StageRegistry is not available
                LOGGER.warn("Error checking stage status: {}", e.getMessage());
                // In development/testing mode, always return false for easier debugging
                hasStage = false;
            }
        } else if (requirement.getType() == StageType.WORLD) {
            try {
                // Try to use actual stage registry for world stages
                hasStage = StageRegistry.worldHasStage(player.level(), requirement.getStageId());
                
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Checking world stage '{}': {}", requirement.getStageId(), hasStage);
                }
            } catch (Exception e) {
                // Fallback to debug mode or if StageRegistry is not available
                LOGGER.warn("Error checking world stage status: {}", e.getMessage());
                hasStage = false;
            }
        }
        
        // Check if the player has the stage or not, based on whether it's required or not
        return hasStage == requirement.isRequired();
    }
    
    /**
     * Replaces parameter references in a command.
     * Supports parameter substitution using #0, #1, etc.
     * Also supports escaping the # character using ##, which is converted to a single #.
     *
     * @param command The command string to process
     * @param paramValues The parameter values to substitute
     * @return The command string with parameters substituted
     */
    private static String replaceParameters(String command, Object[] paramValues) {
        if (command == null || paramValues == null || paramValues.length == 0) {
            return command;
        }
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Replacing parameters in command: '{}' with values: {}", command, paramValues);
        }
        
        // Replace simple patterns (#0, #1, etc.) first
        String workingCommand = command;
        for (int i = 0; i < paramValues.length; i++) {
            if (paramValues[i] != null) {
                String pattern = "#" + i;
                String value = formatParameterValue(paramValues[i]);
                workingCommand = workingCommand.replace(pattern, value);
            }
        }
        
        // Replace ## with # (escape character handling)
        workingCommand = workingCommand.replace("##", "#");
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Command after parameter replacement: '{}'", workingCommand);
        }
        
        return workingCommand;
    }
    
    /**
     * Formats a parameter value for inclusion in a command
     */
    private static String formatParameterValue(Object value) {
        if (value == null) {
            return "";
        } else if (value instanceof EntitySelector) {
            return "@s"; // For entity selectors, use the current player
        } else {
            return value.toString();
        }
    }
    
    /**
     * Schedules the next action in a sequence
     */
    private static void scheduleNextAction(ServerPlayer player, ServerLevel level, List<MacroAction> actions, 
                                          Object[] paramValues, int nextIndex) {
        if (nextIndex >= actions.size()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Macro execution completed for player {}", player.getName().getString());
            }
            ACTIVE_COMMANDS.remove(player.getUUID());
            return;
        }
        
        MacroAction action = actions.get(nextIndex);
        
        if (action.getType() == MacroActionType.DELAY) {
            int delayTicks = action.getDelayTicks();
            
            // Add a small margin to ensure the minimum delay is respected
            long delayMillis = (long) delayTicks * 50L + 5L;
            
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Scheduling delay of {} ticks ({} ms) for player {}", 
                    delayTicks, delayMillis, player.getName().getString());
            }
            
            // Schedule next action after the delay
            ScheduledFuture<?> future = COMMAND_EXECUTOR.schedule(() -> {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Executing action after delay of {} ticks for player {}", 
                        delayTicks, player.getName().getString());
                }
                executeActionSequence(player, level, actions, paramValues, nextIndex + 1);
            }, delayMillis, TimeUnit.MILLISECONDS);
            
            ACTIVE_COMMANDS.put(player.getUUID(), future);
        } else {
            // Execute the next action immediately
            executeActionSequence(player, level, actions, paramValues, nextIndex);
        }
    }
    
    /**
     * Executes a single command
     */
    private static void executeCommand(ServerPlayer player, ServerLevel level, String command) {
        try {
            if (player.getServer() != null) {
                // Create the CommandSourceStack from the player
                CommandSourceStack source = player.createCommandSourceStack();
                
                // Use the new method to execute the command
                executeCommandWithSource(source, player, player.getServer(), command);
            }
        } catch (Exception e) {
            LOGGER.error("Error executing command '{}': {}", command, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Stops the executor service
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
     * Checks if a list of actions contains at least one action of type DELAY
     */
    private static boolean containsDelayAction(List<MacroAction> actions) {
        for (MacroAction action : actions) {
            if (action.getType() == MacroActionType.DELAY) {
                return true;
            }
            // Check recursively also in sub-actions of IF blocks
            if (action.getType() == MacroActionType.IF && action.hasSubActions()) {
                if (containsDelayAction(action.getSubActions())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Special version of executeActionSequence that handles delays correctly
     * 
     * @param callback A function to call when all actions are completed
     */
    private static void executeActionSequenceWithDelayHandling(ServerPlayer player, ServerLevel level, 
                                                 List<MacroAction> actions, Object[] paramValues, 
                                                 int index, Runnable callback) {
        if (index >= actions.size() || player.isRemoved() || !player.isAlive()) {
            // End of sequence or player no longer valid
            if (callback != null) {
                callback.run();
            }
            return;
        }
        
        MacroAction action = actions.get(index);
        
        // Check action-specific stage requirements if any
        if (action.hasStageRequirements() && !checkActionStages(player, action.getStageRequirements())) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Action at index {} skipped due to stage requirements not met", index);
            }
            // Skip this action and move to the next
            executeActionSequenceWithDelayHandling(player, level, actions, paramValues, index + 1, callback);
            return;
        }
        
        if (action.type == MacroActionType.EXECUTE) {
            // Replace parameters in the command
            String commandWithParams = replaceParameters(action.command, paramValues);
            
            // Execute the command immediately
            executeCommand(player, level, commandWithParams);
            
            // Pass to the next action
            executeActionSequenceWithDelayHandling(player, level, actions, paramValues, index + 1, callback);
        } else if (action.type == MacroActionType.DELAY) {
            // Special handling for delay - wait before executing the next action
            int delayTicks = action.delayTicks;
            
            // We use exactly 50ms per tick, but add a small margin 
            // to ensure the minimum delay is respected
            long delayMillis = (long) delayTicks * 50 + 5; 
            
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Special handling: Scheduling delay of {} ticks ({} ms) for player {}", 
                    delayTicks, delayMillis, player.getName().getString());
            }
            
            UUID playerUUID = player.getUUID();
            ScheduledFuture<?> future = COMMAND_EXECUTOR.schedule(() -> {
                // After the delay, execute the next action
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Special handling: Executing action after delay of {} ticks for player {}", 
                        delayTicks, player.getName().getString());
                }
                executeActionSequenceWithDelayHandling(player, level, actions, paramValues, index + 1, callback);
            }, delayMillis, TimeUnit.MILLISECONDS);
            
            // Remember the future for potential cancellation
            ACTIVE_COMMANDS.put(playerUUID, future);
        } else if (action.type == MacroActionType.IF) {
            // Special handling for IF blocks
            List<Integer> indices = action.getConditionIndices();
            List<MacroAction> subActions = action.getSubActions();
            UUID playerUUID = player.getUUID();
            
            // Try to find the current macro
            MacroDefinition currentMacro = null;
            for (String macroId : MACRO_REGISTRY.keySet()) {
                MacroDefinition macro = MACRO_REGISTRY.get(macroId);
                if (macro != null && macro.getActions().contains(action)) {
                    currentMacro = macro;
                    break;
                }
            }
            
            if (currentMacro != null && checkConditionsByIndices(player, indices, 
                    currentMacro.getRequiredStages(), currentMacro.getStagesLogic())) {
                // Conditions met, execute sub-actions
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Special handling: IF conditions met, executing sub-actions");
                }
                
                // Execute sub-actions and then pass to the next main action
                executeActionSequenceWithDelayHandling(player, level, subActions, paramValues, 0, () -> {
                    // After all sub-actions are completed, continue with the next action
                    executeActionSequenceWithDelayHandling(player, level, actions, paramValues, index + 1, callback);
                });
            } else {
                // Conditions not met or macro not found, skip sub-actions
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Special handling: IF conditions not met, skipping sub-actions");
                }
                // Pass to the next action
                executeActionSequenceWithDelayHandling(player, level, actions, paramValues, index + 1, callback);
            }
        } else {
            // Unknown action type, skip and pass to the next
            LOGGER.warn("Unknown action type: {}, skipping", action.type);
            executeActionSequenceWithDelayHandling(player, level, actions, paramValues, index + 1, callback);
        }
    }
    
    /**
     * Checks conditions based on indices for IF statements
     */
    private static boolean checkConditionsByIndices(ServerPlayer player, List<Integer> indices, List<StageRequirement> allStages, StagesLogic logic) {
        if (indices == null || indices.isEmpty()) {
            return true;
        }
        
        // For DEF_AND logic, all conditions must be met
        if (logic == StagesLogic.DEF_AND) {
            for (Integer index : indices) {
                if (index < 0 || index >= allStages.size()) {
                    // Invalid index
                    LOGGER.warn("Invalid stage condition index: {} (max: {})", index, allStages.size() - 1);
                    return false;
                }
                
                StageRequirement condition = allStages.get(index);
                boolean hasStage = checkPlayerHasStage(player, condition);
                
                if (!hasStage) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("DEF_AND condition not met for stage index {}: {}", 
                            index, condition.getStageId());
                    }
                    return false;
                }
            }
            
            // All conditions met
            return true;
        } 
        // For DEF_OR logic, at least one condition must be met
        else if (logic == StagesLogic.DEF_OR) {
            for (Integer index : indices) {
                if (index < 0 || index >= allStages.size()) {
                    // Skip invalid index
                    continue;
                }
                
                StageRequirement condition = allStages.get(index);
                if (checkPlayerHasStage(player, condition)) {
                    return true;
                }
            }
            return indices.isEmpty(); // Return true only if no conditions
        }
        
        // Default to AND logic for any other case
        for (Integer index : indices) {
            if (index < 0 || index >= allStages.size()) {
                // Invalid index
                return false;
            }
            
            StageRequirement condition = allStages.get(index);
            if (!checkPlayerHasStage(player, condition)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Checks if all required conditions are met for an if action
     */
    private static boolean checkDeferredStageConditions(ServerPlayer player, List<Integer> conditionIndices, MacroDefinition macro) {
        if (!macro.hasStageRequirements() || macro.getRequiredStages().isEmpty()) {
            return true; // No requirements to check
        }
        
        List<StageRequirement> allStages = macro.getRequiredStages();
        
        if (macro.getStagesLogic() == StagesLogic.DEF_AND) {
            // All specified conditions must be met
            for (Integer index : conditionIndices) {
                if (index < 0 || index >= allStages.size()) {
                    // Invalid index
                    LOGGER.warn("Invalid stage condition index: {} (max: {})", index, allStages.size() - 1);
                    return false;
                }
                
                StageRequirement requirement = allStages.get(index);
                boolean hasStage = checkPlayerHasStage(player, requirement);
                
                if (!hasStage) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("DEF_AND condition not met for stage index {}: {}", 
                            index, requirement.getStageId());
                    }
                    return false;
                }
            }
            
            // All conditions met
            return true;
        } else if (macro.getStagesLogic() == StagesLogic.DEF_OR) {
            // At least one condition must be met
            for (Integer index : conditionIndices) {
                if (index < 0 || index >= allStages.size()) {
                    // Invalid index
                    LOGGER.warn("Invalid stage condition index: {} (max: {})", index, allStages.size() - 1);
                    continue; // Skip this invalid index
                }
                
                StageRequirement requirement = allStages.get(index);
                boolean hasStage = checkPlayerHasStage(player, requirement);
                
                if (hasStage) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("DEF_OR condition met for stage index {}: {}", 
                            index, requirement.getStageId());
                    }
                    return true;
                }
            }
            
            // No conditions met
            return false;
        }
        
        // Unsupported logic type
        LOGGER.warn("Unsupported stages logic for deferred conditions: {}", macro.getStagesLogic());
        return false;
    }
    
    /**
     * Checks if the conditions for an if action are met
     */
    private static boolean checkIfConditions(ServerPlayer player, MacroAction action, MacroDefinition macro) {
        List<Integer> conditionIndices = action.getConditionIndices();
        
        if (conditionIndices == null || conditionIndices.isEmpty()) {
            // No conditions specified, so they're "met" by default
            return true;
        }
        
        return checkDeferredStageConditions(player, conditionIndices, macro);
    }
    
    /**
     * Process a single JSON 'if' condition with stage indices
     */
    private static List<Integer> parseConditionIndices(JsonObject conditionObj) {
        List<Integer> indices = new ArrayList<>();
        
        if (conditionObj.has("conditions") && conditionObj.get("conditions").isJsonArray()) {
            JsonArray indexArray = conditionObj.get("conditions").getAsJsonArray();
            for (JsonElement indexElem : indexArray) {
                if (indexElem.isJsonPrimitive() && indexElem.getAsJsonPrimitive().isNumber()) {
                    indices.add(indexElem.getAsInt());
                }
            }
        }
        
        return indices;
    }
    
    /**
     * Definition of a command macro
     */
    public static class MacroDefinition {
        private final String id;
        private final List<MacroAction> actions;
        private final List<ParameterDefinition> parameters;
        private final int level;
        private final List<StageRequirement> requiredStages;
        private final StagesLogic stagesLogic;
        
        /**
         * Complete constructor with all fields
         */
        public MacroDefinition(String id, List<MacroAction> actions, List<ParameterDefinition> parameters, 
                              int level, List<StageRequirement> requiredStages, StagesLogic stagesLogic) {
            this.id = id;
            this.actions = actions;
            this.parameters = parameters != null ? parameters : new ArrayList<>();
            this.level = level;
            this.requiredStages = requiredStages != null ? requiredStages : new ArrayList<>();
            this.stagesLogic = stagesLogic != null ? stagesLogic : StagesLogic.AND;
        }
        
        /**
         * Simplified constructor without stages and usage
         */
        public MacroDefinition(String id, List<MacroAction> actions, List<ParameterDefinition> parameters, int level) {
            this(id, actions, parameters, level, null, StagesLogic.AND);
        }
        
        /**
         * Creates a macro definition from a JSON object
         */
        public static MacroDefinition fromJson(JsonObject json) {
            String id = json.get("command").getAsString();
            
            // Extract command level (permission)
            int level = 0;
            if (json.has("level")) {
                level = json.get("level").getAsInt();
            }
            
            // Extract required stages
            List<StageRequirement> requiredStages = new ArrayList<>();
            StagesLogic stagesLogic = StagesLogic.AND; // Default AND
            
            if (json.has("stages") && json.get("stages").isJsonArray()) {
                JsonArray stagesArray = json.get("stages").getAsJsonArray();
                for (JsonElement stageElem : stagesArray) {
                    if (stageElem.isJsonObject()) {
                        JsonObject stageObj = stageElem.getAsJsonObject();
                        if (stageObj.has("stage")) {
                            String stageId = stageObj.get("stage").getAsString();
                            String stageType = "player"; // Default to player if not specified
                            boolean isRequired = true;   // Default to true (stage must be present)
                            
                            if (stageObj.has("stage_type")) {
                                stageType = stageObj.get("stage_type").getAsString();
                            }
                            
                            if (stageObj.has("is")) {
                                isRequired = stageObj.get("is").getAsBoolean();
                            }
                            
                            requiredStages.add(new StageRequirement(stageId, StageType.fromString(stageType), isRequired));
                        }
                    }
                }
            }
            
            // Extract stages logic
            if (json.has("stages_logic")) {
                String logicStr = json.get("stages_logic").getAsString().toUpperCase();
                try {
                    if (logicStr.equals("DEF")) {
                        // Retrocompatibility: treat DEF as DEF_AND
                        stagesLogic = StagesLogic.DEF_AND;
                        LOGGER.warn("Stage logic 'DEF' is deprecated, please use 'DEF_AND' instead for macro {}", id);
                    } else {
                        stagesLogic = StagesLogic.valueOf(logicStr);
                    }
                } catch (IllegalArgumentException e) {
                    // If invalid, default to AND
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Invalid stages_logic value: {}, using AND", logicStr);
                    }
                }
            }
            
            // Extract parameters
            List<ParameterDefinition> parameters = new ArrayList<>();
            if (json.has("parameters") && json.get("parameters").isJsonArray()) {
                JsonArray paramsArray = json.get("parameters").getAsJsonArray();
                for (int i = 0; i < paramsArray.size(); i++) {
                    JsonElement paramElem = paramsArray.get(i);
                    if (paramElem.isJsonObject()) {
                        JsonObject paramObj = paramElem.getAsJsonObject();
                        
                        String type = paramObj.has("type") ? paramObj.get("type").getAsString() : "string";
                        boolean required = !paramObj.has("required") || paramObj.get("required").getAsBoolean();
                        
                        // For static parameters, extract the list of allowed values
                        List<String> staticValues = null;
                        if ("static".equals(type) && paramObj.has("list") && paramObj.get("list").isJsonArray()) {
                            staticValues = new ArrayList<>();
                            JsonArray listArray = paramObj.get("list").getAsJsonArray();
                            for (JsonElement valueElem : listArray) {
                                if (valueElem.isJsonObject() && ((JsonObject)valueElem).has("declare")) {
                                    staticValues.add(((JsonObject)valueElem).get("declare").getAsString());
                                }
                            }
                        }
                        
                        parameters.add(new ParameterDefinition(type, required, staticValues));
                    }
                }
            }
            
            // Extract actions
            List<MacroAction> actions = new ArrayList<>();
            if (json.has("do") && json.get("do").isJsonArray()) {
                JsonArray doArray = json.get("do").getAsJsonArray();
                for (JsonElement actionElem : doArray) {
                    if (actionElem.isJsonObject()) {
                        JsonObject actionObj = actionElem.getAsJsonObject();
                        
                        // Extract stage requirements for this action if available
                        List<StageRequirement> actionStages = null;
                        if ((stagesLogic == StagesLogic.DEF_AND || stagesLogic == StagesLogic.DEF_OR) && 
                            actionObj.has("stages") && actionObj.get("stages").isJsonArray()) {
                            actionStages = new ArrayList<>();
                            JsonArray stagesArray = actionObj.get("stages").getAsJsonArray();
                            
                            for (JsonElement stageElem : stagesArray) {
                                if (stageElem.isJsonObject()) {
                                    JsonObject stageObj = stageElem.getAsJsonObject();
                                    if (stageObj.has("stage")) {
                                        String stageId = stageObj.get("stage").getAsString();
                                        String stageType = "player"; // Default
                                        boolean isRequired = true;   // Default
                                        
                                        if (stageObj.has("stage_type")) {
                                            stageType = stageObj.get("stage_type").getAsString();
                                        }
                                        
                                        if (stageObj.has("is")) {
                                            isRequired = stageObj.get("is").getAsBoolean();
                                        }
                                        
                                        actionStages.add(new StageRequirement(stageId, StageType.fromString(stageType), isRequired));
                                    }
                                }
                            }
                        }
                        
                        if (actionObj.has("execute")) {
                            // Command execution action
                            String command = actionObj.get("execute").getAsString();
                            actions.add(new MacroAction(MacroActionType.EXECUTE, command, 0, actionStages));
                        } else if (actionObj.has("delay")) {
                            // Delay action
                            int ticks = actionObj.get("delay").getAsInt();
                            actions.add(new MacroAction(MacroActionType.DELAY, null, ticks, actionStages));
                        } else if (actionObj.has("if") && actionObj.get("if").isJsonArray()) {
                            // If condition block
                            JsonArray ifArray = actionObj.get("if").getAsJsonArray();
                            if (ifArray.size() >= 2 && ifArray.get(0).isJsonObject()) {
                                JsonObject conditionObj = ifArray.get(0).getAsJsonObject();
                                
                                // Extract condition indices
                                List<Integer> conditionIndices = new ArrayList<>();
                                if (conditionObj.has("conditions") && conditionObj.get("conditions").isJsonArray()) {
                                    JsonArray conditionsArray = conditionObj.get("conditions").getAsJsonArray();
                                    
                                    for (JsonElement indexElement : conditionsArray) {
                                        if (indexElement.isJsonPrimitive()) {
                                            conditionIndices.add(indexElement.getAsInt());
                                        }
                                    }
                                }
                                
                                // Extract sub-actions
                                List<MacroAction> subActions = new ArrayList<>();
                                for (int i = 1; i < ifArray.size(); i++) {
                                    if (ifArray.get(i).isJsonObject()) {
                                        JsonObject subActionObj = ifArray.get(i).getAsJsonObject();
                                        
                                        if (subActionObj.has("execute")) {
                                            // Sub-action: execute command
                                            String command = subActionObj.get("execute").getAsString();
                                            subActions.add(new MacroAction(MacroActionType.EXECUTE, command, 0));
                                        } else if (subActionObj.has("delay")) {
                                            // Sub-action: delay
                                            int ticks = subActionObj.get("delay").getAsInt();
                                            subActions.add(new MacroAction(MacroActionType.DELAY, null, ticks));
                                        }
                                        // Potentially add other action types here
                                    }
                                }
                                
                                if (!subActions.isEmpty()) {
                                    // Create and add the IF action
                                    MacroAction ifAction = new MacroAction(
                                        MacroActionType.IF,
                                        null,
                                        0,
                                        actionStages,
                                        conditionIndices,
                                        subActions
                                    );
                                    actions.add(ifAction);
                                } else {
                                    LOGGER.warn("No valid sub-actions found in 'if' block");
                                }
                            }
                        }
                    }
                }
            }
            
            return new MacroDefinition(id, actions, parameters, level, requiredStages, stagesLogic);
        }
        
        public String getId() {
            return id;
        }
        
        public List<MacroAction> getActions() {
            return actions;
        }
        
        public List<ParameterDefinition> getParameters() {
            return parameters;
        }
        
        public int getLevel() {
            return level;
        }
        
        public List<StageRequirement> getRequiredStages() {
            return requiredStages;
        }
        
        public StagesLogic getStagesLogic() {
            return stagesLogic;
        }
        
        public boolean hasStageRequirements() {
            return requiredStages != null && !requiredStages.isEmpty();
        }
        
        public boolean hasParameters() {
            return parameters != null && !parameters.isEmpty();
        }
        
        /**
         * Checks if the player has the required stages to use this macro
         */
        public boolean checkStages(ServerPlayer player) {
            if (!hasStageRequirements()) {
                return true; // No stage requirements
            }
            
            // If using DEF_AND or DEF_OR logic, stage checks are deferred to individual actions
            if (stagesLogic == StagesLogic.DEF_AND || stagesLogic == StagesLogic.DEF_OR) {
                return true;
            }
            
            if (stagesLogic == StagesLogic.AND) {
                // All stages must be satisfied
                for (StageRequirement requirement : requiredStages) {
                    boolean hasStage = playerHasStage(player, requirement);
                    if (!hasStage) {
                        return false;
                    }
                }
                return true;
            } else { // OR logic
                // At least one stage must be satisfied (OR logic)
                for (StageRequirement requirement : requiredStages) {
                    boolean hasStage = playerHasStage(player, requirement);
                    if (hasStage) {
                        return true;
                    }
                }
                return requiredStages.isEmpty(); // Empty list means no requirements
            }
        }
        
        /**
         * Utility method to check if a player has a specific stage
         * This prevents the infinite recursion issue by directly checking stages here
         */
        public boolean playerHasStage(ServerPlayer player, StageRequirement requirement) {
            // Get game stages for the player
            boolean hasStage = false;
            
            // Use StageRegistry to check if player has the stage
            if (requirement.getType() == StageType.PLAYER) {
                try {
                    // Try to use actual stage registry
                    hasStage = StageRegistry.playerHasStage(player, requirement.getStageId());
                    
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Checking stage '{}' for player {}: {}", 
                            requirement.getStageId(), player.getName().getString(), hasStage);
                    }
                } catch (Exception e) {
                    // Fallback to debug mode or if StageRegistry is not available
                    LOGGER.warn("Error checking stage status: {}", e.getMessage());
                    // In development/testing mode, always return false for easier debugging
                    hasStage = false;
                }
            } else if (requirement.getType() == StageType.WORLD) {
                try {
                    // Try to use actual stage registry for world stages
                    hasStage = StageRegistry.worldHasStage(player.level(), requirement.getStageId());
                    
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Checking world stage '{}': {}", requirement.getStageId(), hasStage);
                    }
                } catch (Exception e) {
                    // Fallback to debug mode or if StageRegistry is not available
                    LOGGER.warn("Error checking world stage status: {}", e.getMessage());
                    hasStage = false;
                }
            }
            
            // Check if the player has the stage or not, based on whether it's required or not
            return hasStage == requirement.isRequired();
        }
    }
    
    /**
     * Type of stage requirement
     */
    public enum StageType {
        PLAYER,    // Stage related to player progression
        WORLD;     // Stage related to world state
        
        public static StageType fromString(String type) {
            try {
                return valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid stage type: {}, using PLAYER as default", type);
                return PLAYER;
            }
        }
    }
    
    /**
     * Represents a stage requirement for a macro
     */
    public static class StageRequirement {
        private final String stageId;
        private final StageType type;
        private final boolean required; // true = stage must be present, false = stage must be absent
        
        public StageRequirement(String stageId, StageType type, boolean required) {
            this.stageId = stageId;
            this.type = type != null ? type : StageType.PLAYER;
            this.required = required;
        }
        
        public StageRequirement(String stageId, StageType type) {
            this(stageId, type, true); // Default is that the stage is required
        }
        
        public String getStageId() {
            return stageId;
        }
        
        public StageType getType() {
            return type;
        }
        
        public boolean isRequired() {
            return required;
        }
    }
    
    /**
     * Logic for evaluating stage requirements
     */
    public enum StagesLogic {
        AND,  // All stages are required
        OR,   // At least one stage is required
        DEF_AND, // Deferred evaluation with array indices referring to stages list (AND logic)
        DEF_OR  // Deferred evaluation with array indices referring to stages list (OR logic)
    }
    
    /**
     * Parameter definition for command macros
     */
    public static class ParameterDefinition {
        private final String type;
        private final boolean required;
        private final List<String> staticValues;
        
        public ParameterDefinition(String type, boolean required) {
            this(type, required, null);
        }
        
        public ParameterDefinition(String type, boolean required, List<String> staticValues) {
            this.type = type.toLowerCase();
            this.required = required;
            this.staticValues = staticValues;
        }
        
        public String getType() {
            return type;
        }
        
        public boolean isRequired() {
            return required;
        }
        
        public List<String> getStaticValues() {
            return staticValues;
        }
    }
    
    /**
     * Action to execute in a macro
     */
    public static class MacroAction {
        private final MacroActionType type;
        private final String command;
        private final int delayTicks;
        private final List<StageRequirement> stageRequirements;
        
        // For IF actions
        private final List<Integer> conditionIndices;
        private final List<MacroAction> subActions;
        
        public MacroAction(MacroActionType type, String command, int delayTicks, List<StageRequirement> stageRequirements) {
            this(type, command, delayTicks, stageRequirements, null, null);
        }
        
        public MacroAction(MacroActionType type, String command, int delayTicks) {
            this(type, command, delayTicks, null, null, null);
        }
        
        public MacroAction(MacroActionType type, String command, int delayTicks, 
                          List<StageRequirement> stageRequirements,
                          List<Integer> conditionIndices, List<MacroAction> subActions) {
            this.type = type;
            this.command = command;
            this.delayTicks = delayTicks;
            this.stageRequirements = stageRequirements != null ? stageRequirements : new ArrayList<>();
            this.conditionIndices = conditionIndices != null ? conditionIndices : new ArrayList<>();
            this.subActions = subActions != null ? subActions : new ArrayList<>();
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
        
        public List<StageRequirement> getStageRequirements() {
            return stageRequirements;
        }
        
        public boolean hasStageRequirements() {
            return stageRequirements != null && !stageRequirements.isEmpty();
        }
        
        public List<Integer> getConditionIndices() {
            return conditionIndices;
        }
        
        public List<MacroAction> getSubActions() {
            return subActions;
        }
        
        public boolean hasSubActions() {
            return subActions != null && !subActions.isEmpty();
        }
        
        /**
         * Checks if the conditions specified by indices are met
         * 
         * @param player The player to check conditions for
         * @param macro The macro definition
         * @return true if conditions are met based on the logic
         */
        public boolean checkConditionsByIndices(ServerPlayer player, MacroDefinition macro) {
            if (conditionIndices.isEmpty()) {
                return true;
            }
            
            List<StageRequirement> allStages = macro.getRequiredStages();
            
            // For DEF_AND logic, all conditions must be met
            if (macro.getStagesLogic() == StagesLogic.DEF_AND) {
                for (Integer index : conditionIndices) {
                    if (index < 0 || index >= allStages.size()) {
                        // Invalid index
                        LOGGER.warn("Invalid stage condition index: {} (max: {})", index, allStages.size() - 1);
                        return false;
                    }
                    
                    StageRequirement requirement = allStages.get(index);
                    boolean hasStage = checkPlayerHasStage(player, requirement);
                    
                    if (!hasStage) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("DEF_AND condition not met for stage index {}: {}", 
                                index, requirement.getStageId());
                        }
                        return false;
                    }
                }
                
                // All conditions met
                return true;
            } 
            // For DEF_OR logic, at least one condition must be met
            else if (macro.getStagesLogic() == StagesLogic.DEF_OR) {
                for (Integer index : conditionIndices) {
                    if (index < 0 || index >= allStages.size()) {
                        // Invalid index
                        LOGGER.warn("Invalid stage condition index: {} (max: {})", index, allStages.size() - 1);
                        continue; // Skip this invalid index
                    }
                    
                    StageRequirement requirement = allStages.get(index);
                    boolean hasStage = checkPlayerHasStage(player, requirement);
                    
                    if (hasStage) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("DEF_OR condition met for stage index {}: {}", 
                                index, requirement.getStageId());
                        }
                        return true;
                    }
                }
                
                // No conditions met
                return false;
            }
            
            // Unsupported logic type
            LOGGER.warn("Unsupported stages logic for deferred conditions: {}", macro.getStagesLogic());
            return false;
        }
    }
    
    /**
     * Type of action in a macro
     */
    public enum MacroActionType {
        EXECUTE,
        DELAY,
        IF      // Conditional action block
    }
} 