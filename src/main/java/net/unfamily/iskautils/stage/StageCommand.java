package net.unfamily.iskautils.stage;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.unfamily.iskautils.IskaUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Command handler for stage management
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public class StageCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Error messages
    private static final SimpleCommandExceptionType ERROR_NEED_PLAYER = new SimpleCommandExceptionType(
            Component.translatable("commands.iska_utils.stage.error.need_player"));
    
    // Command usage messages map
    private static final Map<String, String> COMMAND_USAGE = new HashMap<>();
    
    // Initialize usage messages
    static {
        COMMAND_USAGE.put("list_all", "/iska_utils_stage list all [target player]");
        COMMAND_USAGE.put("list_player", "/iska_utils_stage list player [target player]");
        COMMAND_USAGE.put("list_world", "/iska_utils_stage list world");
        COMMAND_USAGE.put("list_team", "/iska_utils_stage list team <team_name>");
        COMMAND_USAGE.put("list_team_player", "/iska_utils_stage list team_player [target player]");
        
        COMMAND_USAGE.put("set_player", "/iska_utils_stage set player [target player] <stage> [value=true] [silent=false]");
        COMMAND_USAGE.put("set_world", "/iska_utils_stage set world <stage> [value=true] [silent=false]");
        COMMAND_USAGE.put("set_team", "/iska_utils_stage set team <team_name> <stage> [value=true] [silent=false]");
        COMMAND_USAGE.put("set_team_player", "/iska_utils_stage set team_player [target player] <stage> [value=true] [silent=false]");
        
        COMMAND_USAGE.put("add_player", "/iska_utils_stage add player [target player] <stage> [silent=false] [hide=false]");
        COMMAND_USAGE.put("add_world", "/iska_utils_stage add world <stage> [silent=false] [hide=false]");
        COMMAND_USAGE.put("add_team", "/iska_utils_stage add team <team_name> <stage> [silent=false] [hide=false]");
        COMMAND_USAGE.put("add_team_player", "/iska_utils_stage add team_player [target player] <stage> [silent=false] [hide=false]");
        
        COMMAND_USAGE.put("remove_player", "/iska_utils_stage remove player [target player] <stage> [silent=false] [hide=false]");
        COMMAND_USAGE.put("remove_world", "/iska_utils_stage remove world <stage> [silent=false] [hide=false]");
        COMMAND_USAGE.put("remove_team", "/iska_utils_stage remove team <team_name> <stage> [silent=false] [hide=false]");
        COMMAND_USAGE.put("remove_team_player", "/iska_utils_stage remove team_player [target player] <stage> [silent=false] [hide=false]");
        
        COMMAND_USAGE.put("clear_player", "/iska_utils_stage clear player [target player] [silent=false] [hide=false]");
        COMMAND_USAGE.put("clear_world", "/iska_utils_stage clear world [silent=false] [hide=false]");
        COMMAND_USAGE.put("clear_team", "/iska_utils_stage clear team <team_name> [silent=false] [hide=false]");
        COMMAND_USAGE.put("clear_team_player", "/iska_utils_stage clear team_player [target player] [silent=false] [hide=false]");
        COMMAND_USAGE.put("clear_all", "/iska_utils_stage clear all [target player] [silent=false] [hide=false]");

        COMMAND_USAGE.put("call_action", "/iska_utils_stage call_action <target> <action_id> [force=false] [silent=false] [hide=false]");
    }
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registering stage commands");
        register(event.getDispatcher());
    }
    
    /**
     * Registers the stage command
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // CLEAR nodes (extracted to avoid messy parentheses; hide support)
        LiteralArgumentBuilder<CommandSourceStack> clearPlayerNode = Commands.literal("player")
            .then(Commands.argument("target", EntityArgument.entities())
                .executes(ctx -> clearPlayerStages(ctx, false, false))
                .then(Commands.argument("silent", BoolArgumentType.bool())
                    .suggests((ctx, builder) -> Suggestions.empty())
                    .executes(ctx -> clearPlayerStages(ctx, ctx.getArgument("silent", Boolean.class), false))
                    .then(Commands.argument("hide", BoolArgumentType.bool())
                        .suggests((ctx, builder) -> Suggestions.empty())
                        .executes(ctx -> clearPlayerStages(ctx, ctx.getArgument("silent", Boolean.class), ctx.getArgument("hide", Boolean.class))))))
            .executes(ctx -> clearPlayerStagesForSelf(ctx, false, false))
            .then(Commands.argument("silent", BoolArgumentType.bool())
                .suggests((ctx, builder) -> Suggestions.empty())
                .executes(ctx -> clearPlayerStagesForSelf(ctx, ctx.getArgument("silent", Boolean.class), false))
                .then(Commands.argument("hide", BoolArgumentType.bool())
                    .suggests((ctx, builder) -> Suggestions.empty())
                    .executes(ctx -> clearPlayerStagesForSelf(ctx, ctx.getArgument("silent", Boolean.class), ctx.getArgument("hide", Boolean.class)))));
        LiteralArgumentBuilder<CommandSourceStack> clearWorldNode = Commands.literal("world")
            .executes(ctx -> clearWorldStages(ctx, false, false))
            .then(Commands.argument("silent", BoolArgumentType.bool())
                .executes(ctx -> clearWorldStages(ctx, ctx.getArgument("silent", Boolean.class), false))
                .then(Commands.argument("hide", BoolArgumentType.bool())
                    .executes(ctx -> clearWorldStages(ctx, ctx.getArgument("silent", Boolean.class), ctx.getArgument("hide", Boolean.class)))));
        LiteralArgumentBuilder<CommandSourceStack> clearTeamNode = Commands.literal("team")
            .then(Commands.argument("team_name", StringArgumentType.string()).suggests(StageCommand::suggestTeamNames)
                .executes(ctx -> clearTeamStages(ctx, false, false))
                .then(Commands.argument("silent", BoolArgumentType.bool())
                    .executes(ctx -> clearTeamStages(ctx, ctx.getArgument("silent", Boolean.class), false))
                    .then(Commands.argument("hide", BoolArgumentType.bool())
                        .executes(ctx -> clearTeamStages(ctx, ctx.getArgument("silent", Boolean.class), ctx.getArgument("hide", Boolean.class))))));
        LiteralArgumentBuilder<CommandSourceStack> clearTeamPlayerNode = Commands.literal("team_player")
            .then(Commands.argument("target", EntityArgument.entities())
                .executes(ctx -> clearTeamPlayerStages(ctx, false, false))
                .then(Commands.argument("silent", BoolArgumentType.bool())
                    .executes(ctx -> clearTeamPlayerStages(ctx, ctx.getArgument("silent", Boolean.class), false))
                    .then(Commands.argument("hide", BoolArgumentType.bool())
                        .executes(ctx -> clearTeamPlayerStages(ctx, ctx.getArgument("silent", Boolean.class), ctx.getArgument("hide", Boolean.class)))))
            .executes(ctx -> clearTeamPlayerStagesForSelf(ctx, false, false))
            .then(Commands.argument("silent", BoolArgumentType.bool())
                .executes(ctx -> clearTeamPlayerStagesForSelf(ctx, ctx.getArgument("silent", Boolean.class), false))
                .then(Commands.argument("hide", BoolArgumentType.bool())
                    .executes(ctx -> clearTeamPlayerStagesForSelf(ctx, ctx.getArgument("silent", Boolean.class), ctx.getArgument("hide", Boolean.class))))));
        LiteralArgumentBuilder<CommandSourceStack> clearAllNode = Commands.literal("all")
            .then(Commands.argument("target", EntityArgument.entities())
                .executes(ctx -> clearAllStages(ctx, false, false))
                .then(Commands.argument("silent", BoolArgumentType.bool())
                    .executes(ctx -> clearAllStages(ctx, ctx.getArgument("silent", Boolean.class), false))
                    .then(Commands.argument("hide", BoolArgumentType.bool())
                        .executes(ctx -> clearAllStages(ctx, ctx.getArgument("silent", Boolean.class), ctx.getArgument("hide", Boolean.class)))))
            .executes(ctx -> clearAllStagesForSelf(ctx, false, false))
            .then(Commands.argument("silent", BoolArgumentType.bool())
                .executes(ctx -> clearAllStagesForSelf(ctx, ctx.getArgument("silent", Boolean.class), false))
                .then(Commands.argument("hide", BoolArgumentType.bool())
                    .executes(ctx -> clearAllStagesForSelf(ctx, ctx.getArgument("silent", Boolean.class), ctx.getArgument("hide", Boolean.class))))));
        LiteralArgumentBuilder<CommandSourceStack> clearNode = Commands.literal("clear")
            .then(clearPlayerNode)
            .then(clearWorldNode)
            .then(clearTeamNode)
            .then(clearTeamPlayerNode)
            .then(clearAllNode);

        // ADD node: player, world, team, team_player as siblings (same level)
        LiteralArgumentBuilder<CommandSourceStack> addNode = Commands.literal("add")
            .then(Commands.literal("player")
                .then(Commands.argument("target", EntityArgument.entities())
                    .then(Commands.argument("stage", StringArgumentType.string()).suggests(StageCommand::suggestStages)
                        .executes(ctx -> setPlayerStage(ctx, true, false, false))
                        .then(Commands.argument("silent", BoolArgumentType.bool())
                            .executes(ctx -> setPlayerStage(ctx, true, null, false))
                            .then(Commands.argument("hide", BoolArgumentType.bool())
                                .executes(ctx -> setPlayerStage(ctx, true, null, ctx.getArgument("hide", Boolean.class)))))))
                .then(Commands.argument("stage", StringArgumentType.string()).suggests(StageCommand::suggestStages)
                    .executes(ctx -> setPlayerStageForSelf(ctx, true, false, false))
                    .then(Commands.argument("silent", BoolArgumentType.bool())
                        .executes(ctx -> setPlayerStageForSelf(ctx, true, null, false))
                        .then(Commands.argument("hide", BoolArgumentType.bool())
                            .executes(ctx -> setPlayerStageForSelf(ctx, true, null, ctx.getArgument("hide", Boolean.class)))))))
            .then(Commands.literal("world")
                .then(Commands.argument("stage", StringArgumentType.string()).suggests(StageCommand::suggestStages)
                    .executes(ctx -> setWorldStage(ctx, true, false, false))
                    .then(Commands.argument("silent", BoolArgumentType.bool())
                        .executes(ctx -> setWorldStage(ctx, true, null, false))
                        .then(Commands.argument("hide", BoolArgumentType.bool())
                            .executes(ctx -> setWorldStage(ctx, true, null, ctx.getArgument("hide", Boolean.class)))))))
            .then(Commands.literal("team")
                .then(Commands.argument("team_name", StringArgumentType.string()).suggests(StageCommand::suggestTeamNames)
                    .then(Commands.argument("stage", StringArgumentType.string()).suggests(StageCommand::suggestStages)
                        .executes(ctx -> setTeamStage(ctx, true, false, false))
                        .then(Commands.argument("silent", BoolArgumentType.bool())
                            .executes(ctx -> setTeamStage(ctx, true, null, false))
                            .then(Commands.argument("hide", BoolArgumentType.bool())
                                .executes(ctx -> setTeamStage(ctx, true, null, ctx.getArgument("hide", Boolean.class))))))))
            .then(Commands.literal("team_player")
                .then(Commands.argument("target", EntityArgument.entities())
                    .then(Commands.argument("stage", StringArgumentType.string()).suggests(StageCommand::suggestStages)
                        .executes(ctx -> setTeamPlayerStage(ctx, true, false, false))
                        .then(Commands.argument("silent", BoolArgumentType.bool())
                            .executes(ctx -> setTeamPlayerStage(ctx, true, null, false))
                            .then(Commands.argument("hide", BoolArgumentType.bool())
                                .executes(ctx -> setTeamPlayerStage(ctx, true, null, ctx.getArgument("hide", Boolean.class)))))))
                .then(Commands.argument("stage", StringArgumentType.string()).suggests(StageCommand::suggestStages)
                    .executes(ctx -> setTeamPlayerStageForSelf(ctx, true, false, false))
                    .then(Commands.argument("silent", BoolArgumentType.bool())
                        .executes(ctx -> setTeamPlayerStageForSelf(ctx, true, null, false))
                        .then(Commands.argument("hide", BoolArgumentType.bool())
                            .executes(ctx -> setTeamPlayerStageForSelf(ctx, true, null, ctx.getArgument("hide", Boolean.class)))))));

        // REMOVE node: player, world, team, team_player as siblings (same level)
        LiteralArgumentBuilder<CommandSourceStack> removeNode = Commands.literal("remove")
            .then(Commands.literal("player")
                .then(Commands.argument("target", EntityArgument.entities())
                    .then(Commands.argument("stage", StringArgumentType.string()).suggests(StageCommand::suggestStages)
                        .executes(ctx -> setPlayerStage(ctx, false, false, false))
                        .then(Commands.argument("silent", BoolArgumentType.bool())
                            .executes(ctx -> setPlayerStage(ctx, false, null, false))
                            .then(Commands.argument("hide", BoolArgumentType.bool())
                                .executes(ctx -> setPlayerStage(ctx, false, null, ctx.getArgument("hide", Boolean.class)))))))
                .then(Commands.argument("stage", StringArgumentType.string()).suggests(StageCommand::suggestStages)
                    .executes(ctx -> setPlayerStageForSelf(ctx, false, false, false))
                    .then(Commands.argument("silent", BoolArgumentType.bool())
                        .executes(ctx -> setPlayerStageForSelf(ctx, false, null, false))
                        .then(Commands.argument("hide", BoolArgumentType.bool())
                            .executes(ctx -> setPlayerStageForSelf(ctx, false, null, ctx.getArgument("hide", Boolean.class)))))))
            .then(Commands.literal("world")
                .then(Commands.argument("stage", StringArgumentType.string()).suggests(StageCommand::suggestStages)
                    .executes(ctx -> setWorldStage(ctx, false, false, false))
                    .then(Commands.argument("silent", BoolArgumentType.bool())
                        .executes(ctx -> setWorldStage(ctx, false, null, false))
                        .then(Commands.argument("hide", BoolArgumentType.bool())
                            .executes(ctx -> setWorldStage(ctx, false, null, ctx.getArgument("hide", Boolean.class)))))))
            .then(Commands.literal("team")
                .then(Commands.argument("team_name", StringArgumentType.string()).suggests(StageCommand::suggestTeamNames)
                    .then(Commands.argument("stage", StringArgumentType.string()).suggests(StageCommand::suggestStages)
                        .executes(ctx -> setTeamStage(ctx, false, false, false))
                        .then(Commands.argument("silent", BoolArgumentType.bool())
                            .executes(ctx -> setTeamStage(ctx, false, null, false))
                            .then(Commands.argument("hide", BoolArgumentType.bool())
                                .executes(ctx -> setTeamStage(ctx, false, null, ctx.getArgument("hide", Boolean.class))))))))
            .then(Commands.literal("team_player")
                .then(Commands.argument("target", EntityArgument.entities())
                    .then(Commands.argument("stage", StringArgumentType.string()).suggests(StageCommand::suggestStages)
                        .executes(ctx -> setTeamPlayerStage(ctx, false, false, false))
                        .then(Commands.argument("silent", BoolArgumentType.bool())
                            .executes(ctx -> setTeamPlayerStage(ctx, false, null, false))
                            .then(Commands.argument("hide", BoolArgumentType.bool())
                                .executes(ctx -> setTeamPlayerStage(ctx, false, null, ctx.getArgument("hide", Boolean.class)))))))
                .then(Commands.argument("stage", StringArgumentType.string()).suggests(StageCommand::suggestStages)
                    .executes(ctx -> setTeamPlayerStageForSelf(ctx, false, false, false))
                    .then(Commands.argument("silent", BoolArgumentType.bool())
                        .executes(ctx -> setTeamPlayerStageForSelf(ctx, false, null, false))
                        .then(Commands.argument("hide", BoolArgumentType.bool())
                            .executes(ctx -> setTeamPlayerStageForSelf(ctx, false, null, ctx.getArgument("hide", Boolean.class)))))));

        dispatcher.register(
            Commands.literal("iska_utils_stage")
                .requires(source -> source.hasPermission(2))
                // ADD and REMOVE first so they appear in suggestions (add, clear, list, remove, set)
                .then(addNode)
                .then(removeNode)
                // LIST commands
                .then(Commands.literal("list")
                    .then(Commands.literal("all")
                        .executes(StageCommand::listAllStages)
                        .then(Commands.argument("target", EntityArgument.entities())
                            .executes(StageCommand::listAllStagesForTarget)))
                    .then(Commands.literal("player")
                        .executes(StageCommand::listPlayerStages)
                        .then(Commands.argument("target", EntityArgument.entities())
                            .executes(StageCommand::listPlayerStagesForTarget)))
                    .then(Commands.literal("world")
                        .executes(StageCommand::listWorldStages))
                    .then(Commands.literal("team")
                        .then(Commands.argument("team_name", StringArgumentType.string()).suggests(StageCommand::suggestTeamNames)
                            .executes(StageCommand::listTeamStages)))
                    .then(Commands.literal("team_player")
                        .executes(StageCommand::listTeamPlayerStages)
                        .then(Commands.argument("target", EntityArgument.entities())
                            .executes(StageCommand::listTeamPlayerStagesForTarget))))
                
                // SET commands
                .then(Commands.literal("set")
                    .then(Commands.literal("player")
                        .then(Commands.argument("target", EntityArgument.entities())
                            .then(Commands.argument("stage", StringArgumentType.string()).suggests(StageCommand::suggestStages)
                                .executes(ctx -> setPlayerStage(ctx, true, false, false))
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                    .executes(ctx -> setPlayerStage(ctx, null, false, false))
                                    .then(Commands.argument("silent", BoolArgumentType.bool())
                                        .executes(ctx -> setPlayerStage(ctx, null, null, false))))))
                        .then(Commands.argument("stage", StringArgumentType.string()).suggests(StageCommand::suggestStages)
                            .executes(ctx -> setPlayerStageForSelf(ctx, true, false, false))
                            .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> setPlayerStageForSelf(ctx, null, false, false))
                                .then(Commands.argument("silent", BoolArgumentType.bool())
                                    .executes(ctx -> setPlayerStageForSelf(ctx, null, null, false))))))
                    .then(Commands.literal("world")
                        .then(Commands.argument("stage", StringArgumentType.string()).suggests(StageCommand::suggestStages)
                            .executes(ctx -> setWorldStage(ctx, true, false, false))
                            .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> setWorldStage(ctx, null, false, false))
                                .then(Commands.argument("silent", BoolArgumentType.bool())
                                    .executes(ctx -> setWorldStage(ctx, null, null, false))))))
                    .then(Commands.literal("team")
                        .then(Commands.argument("team_name", StringArgumentType.string()).suggests(StageCommand::suggestTeamNames)
                            .then(Commands.argument("stage", StringArgumentType.string()).suggests(StageCommand::suggestStages)
                                .executes(ctx -> setTeamStage(ctx, true, false, false))
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                    .executes(ctx -> setTeamStage(ctx, null, false, false))
                                    .then(Commands.argument("silent", BoolArgumentType.bool())
                                        .executes(ctx -> setTeamStage(ctx, null, null, false)))))))
                    .then(Commands.literal("team_player")
                        .then(Commands.argument("target", EntityArgument.entities())
                            .then(Commands.argument("stage", StringArgumentType.string()).suggests(StageCommand::suggestStages)
                                .executes(ctx -> setTeamPlayerStage(ctx, true, false, false))
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                    .executes(ctx -> setTeamPlayerStage(ctx, null, false, false))
                                    .then(Commands.argument("silent", BoolArgumentType.bool())
                                        .executes(ctx -> setTeamPlayerStage(ctx, null, null, false))))))
                        .then(Commands.argument("stage", StringArgumentType.string()).suggests(StageCommand::suggestStages)
                            .executes(ctx -> setTeamPlayerStageForSelf(ctx, true, false, false))
                            .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> setTeamPlayerStageForSelf(ctx, null, false, false))
                                .then(Commands.argument("silent", BoolArgumentType.bool())
                                    .executes(ctx -> setTeamPlayerStageForSelf(ctx, null, null, false)))))))
                // CALL_ACTION: run stage action for target(s), stages rechecked at execution
                .then(Commands.literal("call_action")
                    .then(Commands.argument("target", EntityArgument.entities())
                        .then(Commands.argument("action_id", StringArgumentType.word()).suggests(StageCommand::suggestActionIds)
                            .executes(ctx -> callAction(ctx))
                            .then(Commands.argument("force", BoolArgumentType.bool())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"false", "true"}, b))
                                .executes(ctx -> callAction(ctx))
                                .then(Commands.argument("silent", BoolArgumentType.bool())
                                    .executes(ctx -> callAction(ctx))
                                    .then(Commands.argument("hide", BoolArgumentType.bool())
                                        .executes(ctx -> callAction(ctx))))))))
                // CLEAR commands
                .then(clearNode)
        );
    }
    
    /**
     * Displays command usage to a player
     */
    public static void sendUsage(CommandSourceStack source, String commandKey) {
        String usage = COMMAND_USAGE.getOrDefault(commandKey, "/iska_utils_stage " + commandKey.replace('_', ' '));
        source.sendFailure(Component.literal("§cUsage: " + usage));
    }
    
    /**
     * Gets the command usage string for JSON definitions
     */
    public static String getCommandUsage(String commandKey) {
        return COMMAND_USAGE.getOrDefault(commandKey, "/iska_utils_stage " + commandKey.replace('_', ' '));
    }
    
    /**
     * call_action command: runs a stage action for target(s). Stages are rechecked at execution.
     */
    private static int callAction(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String actionId = StringArgumentType.getString(context, "action_id");
        boolean force = getOptionalBool(context, "force", false);
        boolean silent = getOptionalBool(context, "silent", false);
        boolean hide = getOptionalBool(context, "hide", false);

        List<ServerPlayer> players;
        try {
            players = getTargetPlayers(context, "target");
        } catch (CommandSyntaxException e) {
            if (!silent) source.sendFailure(Component.literal("§cInvalid target: " + e.getMessage()));
            return 0;
        }

        if (players.isEmpty()) {
            if (!silent) source.sendFailure(Component.literal("§cNo valid players found"));
            return 0;
        }

        int executed = net.unfamily.iskautils.command.StageActionsManager.executeActionById(actionId, players, force);
        if (executed == -1) {
            if (!silent) source.sendFailure(Component.literal("§cUnknown action id: " + actionId));
            return 0;
        }
        if (executed == 0 && !force) {
            if (!silent) source.sendFailure(Component.literal("§cAction '" + actionId + "' has onCall=false. Use force=true to bypass."));
            return 0;
        }
        if (!silent && !hide) {
            source.sendSuccess(() -> Component.literal("§aExecuted action §e" + actionId + " §afor §f" + executed + " §aplayer(s)"), false);
        }
        return executed;
    }

    private static boolean getOptionalBool(CommandContext<CommandSourceStack> context, String name, boolean defaultValue) {
        try {
            return context.getArgument(name, Boolean.class);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    /**
     * Gets target players from the argument (supports @p, @a, @r, @e, @s, @n).
     * Filters to ServerPlayer only because stages are for players.
     */
    private static List<ServerPlayer> getTargetPlayers(CommandContext<CommandSourceStack> context, String argumentName) throws CommandSyntaxException {
        Collection<? extends Entity> entities = EntityArgument.getEntities(context, argumentName);
        return entities.stream()
                .filter(ServerPlayer.class::isInstance)
                .map(ServerPlayer.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * Suggestion provider for stage names (autocomplete).
     */
    private static CompletableFuture<Suggestions> suggestStages(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        try {
            StageRegistry registry = StageRegistry.getInstance(context.getSource().getServer());
            return SharedSuggestionProvider.suggest(new ArrayList<>(registry.getAllRegisteredStages()), builder);
        } catch (Exception e) {
            return Suggestions.empty();
        }
    }

    /**
     * Suggestion provider for stage action ids (autocomplete).
     */
    private static CompletableFuture<Suggestions> suggestActionIds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        List<String> ids = net.unfamily.iskautils.command.StageActionsLoader.getActionIds();
        return SharedSuggestionProvider.suggest(ids, builder);
    }

    /**
     * Suggestion provider for team names (autocomplete).
     */
    private static CompletableFuture<Suggestions> suggestTeamNames(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        try {
            var server = context.getSource().getServer();
            var level = server.getLevel(Level.OVERWORLD);
            if (level == null) return Suggestions.empty();
            StageRegistry registry = StageRegistry.getInstance(server);
            var teamData = registry.getTeamStageData(level);
            if (teamData == null) return Suggestions.empty();
            return SharedSuggestionProvider.suggest(new ArrayList<>(teamData.getAllTeams()), builder);
        } catch (Exception e) {
            return Suggestions.empty();
        }
    }

    /**
     * Lists all stages
     */
    private static int listAllStages(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            
            source.sendSuccess(() -> Component.literal("§6===== All Stages ====="), false);
            
            // World stages
            List<String> worldStages = registry.getWorldStages();
            String worldStagesText = worldStages.isEmpty() ? "§7(none)" : "§a" + String.join(", ", worldStages);
            source.sendSuccess(() -> Component.literal("§2World Stages: " + worldStagesText), false);
            
            // Player stages - just for the executing player if they are a player
            try {
                ServerPlayer player = source.getPlayerOrException();
                List<String> playerStages = registry.getPlayerStages(player);
                String playerStagesText = playerStages.isEmpty() ? "§7(none)" : "§a" + String.join(", ", playerStages);
                source.sendSuccess(() -> Component.literal("§dPlayer Stages (for you): " + playerStagesText), false);
            } catch (CommandSyntaxException e) {
                source.sendSuccess(() -> Component.literal("§dPlayer Stages: §7(run as player to see your stages)"), false);
            }
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error in listAllStages command", e);
            sendUsage(context.getSource(), "list_all");
            return 0;
        }
    }
    
    /**
     * Lists all stages for target player(s) (@p, @a, @r, @e, @s, @n)
     */
    private static int listAllStagesForTarget(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            List<ServerPlayer> targets = getTargetPlayers(context, "target");
            if (targets.isEmpty()) {
                source.sendFailure(Component.literal("§cNo player found from selector"));
                return 0;
            }
            // World stages (once)
            List<String> worldStages = registry.getWorldStages();
            String worldStagesText = worldStages.isEmpty() ? "§7(none)" : "§a" + String.join(", ", worldStages);
            source.sendSuccess(() -> Component.literal("§2World Stages: " + worldStagesText), false);
            for (ServerPlayer targetPlayer : targets) {
                String playerName = targetPlayer.getName().getString();
                source.sendSuccess(() -> Component.literal("§6===== All Stages for " + playerName + " ====="), false);
                List<String> playerStages = registry.getPlayerStages(targetPlayer);
                String playerStagesText = playerStages.isEmpty() ? "§7(none)" : "§a" + String.join(", ", playerStages);
                source.sendSuccess(() -> Component.literal("§dPlayer Stages for " + playerName + ": " + playerStagesText), false);
            }
            return targets.size();
        } catch (CommandSyntaxException e) {
            LOGGER.error("Error in listAllStagesForTarget command", e);
            sendUsage(context.getSource(), "list_all");
            return 0;
        }
    }
    
    /**
     * Lists player stages
     */
    private static int listPlayerStages(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            
            source.sendSuccess(() -> Component.literal("§6===== Player Stages ====="), false);
            
            try {
                ServerPlayer player = source.getPlayerOrException();
                List<String> playerStages = registry.getPlayerStages(player);
                String playerStagesText = playerStages.isEmpty() ? "§7(none)" : "§a" + String.join(", ", playerStages);
                source.sendSuccess(() -> Component.literal("§dPlayer Stages (for you): " + playerStagesText), false);
            } catch (CommandSyntaxException e) {
                source.sendSuccess(() -> Component.literal("§7(Run as player to see your stages)"), false);
            }
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error in listPlayerStages command", e);
            sendUsage(context.getSource(), "list_player");
            return 0;
        }
    }
    
    /**
     * Lists player stages for target player(s) (@p, @a, @r, @e, @s, @n)
     */
    private static int listPlayerStagesForTarget(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            List<ServerPlayer> targets = getTargetPlayers(context, "target");
            if (targets.isEmpty()) {
                source.sendFailure(Component.literal("§cNo player found from selector"));
                return 0;
            }
            for (ServerPlayer targetPlayer : targets) {
                String playerName = targetPlayer.getName().getString();
                source.sendSuccess(() -> Component.literal("§6===== Player Stages for " + playerName + " ====="), false);
                List<String> playerStages = registry.getPlayerStages(targetPlayer);
                String playerStagesText = playerStages.isEmpty() ? "§7(none)" : "§a" + String.join(", ", playerStages);
                source.sendSuccess(() -> Component.literal("§dPlayer Stages: " + playerStagesText), false);
            }
            return targets.size();
        } catch (CommandSyntaxException e) {
            LOGGER.error("Error in listPlayerStagesForTarget command", e);
            sendUsage(context.getSource(), "list_player");
            return 0;
        }
    }
    
    /**
     * Lists world stages
     */
    private static int listWorldStages(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            
            source.sendSuccess(() -> Component.literal("§6===== World Stages ====="), false);
            
            List<String> worldStages = registry.getWorldStages();
            String worldStagesText = worldStages.isEmpty() ? "§7(none)" : "§a" + String.join(", ", worldStages);
            source.sendSuccess(() -> Component.literal("§2World Stages: " + worldStagesText), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error in listWorldStages command", e);
            sendUsage(context.getSource(), "list_world");
            return 0;
        }
    }
    
    /**
     * Lists team stages for a specific team
     */
    private static int listTeamStages(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            
            // Get the team name
            String teamName = StringArgumentType.getString(context, "team_name");
            
            source.sendSuccess(() -> Component.literal("§6===== Team Stages for " + teamName + " ====="), false);
            
            List<String> teamStages = registry.getTeamStages(teamName);
            String teamStagesText = teamStages.isEmpty() ? "§7(none)" : "§b" + String.join(", ", teamStages);
            source.sendSuccess(() -> Component.literal("§bTeam Stages: " + teamStagesText), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error in listTeamStages command", e);
            sendUsage(context.getSource(), "list_team");
            return 0;
        }
    }
    
    /**
     * Lists team stages for the player executing the command
     */
    private static int listTeamPlayerStages(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            
            source.sendSuccess(() -> Component.literal("§6===== Team Player Stages ====="), false);
            
            try {
                ServerPlayer player = source.getPlayerOrException();
                List<String> teamPlayerStages = registry.getPlayerTeamStages(player);
                String teamPlayerStagesText = teamPlayerStages.isEmpty() ? "§7(none)" : "§b" + String.join(", ", teamPlayerStages);
                source.sendSuccess(() -> Component.literal("§bTeam Player Stages (for you): " + teamPlayerStagesText), false);
            } catch (CommandSyntaxException e) {
                source.sendSuccess(() -> Component.literal("§7(Run as player to see your team stages)"), false);
            }
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error in listTeamPlayerStages command", e);
            sendUsage(context.getSource(), "list_team_player");
            return 0;
        }
    }
    
    /**
     * Lists team stages for target player(s) (@p, @a, @r, @e, @s, @n)
     */
    private static int listTeamPlayerStagesForTarget(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            List<ServerPlayer> targets = getTargetPlayers(context, "target");
            if (targets.isEmpty()) {
                source.sendFailure(Component.literal("§cNo player found from selector"));
                return 0;
            }
            for (ServerPlayer targetPlayer : targets) {
                String playerName = targetPlayer.getName().getString();
                source.sendSuccess(() -> Component.literal("§6===== Team Player Stages for " + playerName + " ====="), false);
                List<String> teamPlayerStages = registry.getPlayerTeamStages(targetPlayer);
                String teamPlayerStagesText = teamPlayerStages.isEmpty() ? "§7(none)" : "§b" + String.join(", ", teamPlayerStages);
                source.sendSuccess(() -> Component.literal("§bTeam Player Stages: " + teamPlayerStagesText), false);
            }
            return targets.size();
        } catch (CommandSyntaxException e) {
            LOGGER.error("Error in listTeamPlayerStagesForTarget command", e);
            sendUsage(context.getSource(), "list_team_player");
            return 0;
        }
    }
    
    /**
     * Sets a player stage
     * @param hideInLog se true, il feedback non viene inviato agli op / non viene registrato nel log
     */
    private static int setPlayerStage(CommandContext<CommandSourceStack> context, Boolean valueOverride, Boolean silentOverride, boolean hideInLog) {
        try {
            CommandSourceStack source = context.getSource();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            List<ServerPlayer> targets = getTargetPlayers(context, "target");
            if (targets.isEmpty()) {
                source.sendFailure(Component.literal("§cNo player found from selector"));
                return 0;
            }
            String stage = StringArgumentType.getString(context, "stage");
            boolean value = valueOverride != null ? valueOverride : BoolArgumentType.getBool(context, "value");
            boolean silent = silentOverride != null ? silentOverride : context.getArgument("silent", Boolean.class);
            boolean broadcastToOps = !hideInLog;
            int count = 0;
            for (ServerPlayer player : targets) {
                boolean success = registry.setPlayerStage(player, stage, value);
                if (success) {
                    count++;
                    if (!silent) {
                        Component message = value
                            ? Component.literal("Added stage §a" + stage + "§r to player §e" + player.getName().getString())
                            : Component.literal("Removed stage §c" + stage + "§r from player §e" + player.getName().getString());
                        source.sendSuccess(() -> message, broadcastToOps);
                        Component playerMessage = value
                            ? Component.literal("§aYou gained the stage: §e" + stage)
                            : Component.literal("§cYou lost the stage: §e" + stage);
                        player.sendSystemMessage(playerMessage);
                    }
                }
            }
            return count;
        } catch (CommandSyntaxException e) {
            LOGGER.error("Error in setPlayerStage command", e);
            sendUsage(context.getSource(), valueOverride == null ? "set_player" : (valueOverride ? "add_player" : "remove_player"));
            return 0;
        }
    }
    
    /**
     * Sets a world stage
     * @param hideInLog se true, il feedback non viene inviato agli op / non viene registrato nel log
     */
    private static int setWorldStage(CommandContext<CommandSourceStack> context, Boolean valueOverride, Boolean silentOverride, boolean hideInLog) {
        try {
            CommandSourceStack source = context.getSource();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            String stage = StringArgumentType.getString(context, "stage");
            boolean value = valueOverride != null ? valueOverride : BoolArgumentType.getBool(context, "value");
            boolean silent = silentOverride != null ? silentOverride : context.getArgument("silent", Boolean.class);
            boolean broadcastToOps = !hideInLog;
            boolean success = registry.setWorldStage(stage, value);
            if (success && !silent) {
                Component message = value
                    ? Component.literal("Added stage §a" + stage + "§r to world")
                    : Component.literal("Removed stage §c" + stage + "§r from world");
                source.sendSuccess(() -> message, broadcastToOps);
            }
            return success ? 1 : 0;
        } catch (Exception e) {
            LOGGER.error("Error in setWorldStage command", e);
            sendUsage(context.getSource(), valueOverride == null ? "set_world" : (valueOverride ? "add_world" : "remove_world"));
            return 0;
        }
    }
    
    /**
     * Clears all player stages for target player(s) (@p, @a, @r, @e, @s, @n)
     * @param hideInLog se true, il feedback non viene inviato agli op / non viene registrato nel log
     */
    private static int clearPlayerStages(CommandContext<CommandSourceStack> context, Boolean silentOverride, boolean hideInLog) {
        try {
            CommandSourceStack source = context.getSource();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            List<ServerPlayer> targets = getTargetPlayers(context, "target");
            if (targets.isEmpty()) {
                source.sendFailure(Component.literal("§cNo player found from selector"));
                return 0;
            }
            boolean silent = silentOverride != null ? silentOverride : false;
            boolean broadcastToOps = !hideInLog;
            int totalCleared = 0;
            for (ServerPlayer targetPlayer : targets) {
                List<String> playerStages = registry.getPlayerStages(targetPlayer);
                for (String stage : playerStages) {
                    registry.setPlayerStage(targetPlayer, stage, false);
                    totalCleared++;
                }
                if (!silent) {
                    String playerName = targetPlayer.getName().getString();
                    String feedback = "§aCleared player stages for " + playerName;
                    source.sendSuccess(() -> Component.literal(feedback), broadcastToOps);
                }
            }
            return totalCleared;
        } catch (CommandSyntaxException e) {
            LOGGER.error("Error in clearPlayerStages command", e);
            sendUsage(context.getSource(), "clear_player");
            return 0;
        }
    }
    
    /**
     * Clears all world stages
     * @param hideInLog se true, il feedback non viene inviato agli op / non viene registrato nel log
     */
    private static int clearWorldStages(CommandContext<CommandSourceStack> context, Boolean silentOverride, boolean hideInLog) {
        try {
            CommandSourceStack source = context.getSource();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            List<String> worldStages = registry.getWorldStages();
            int stageCount = worldStages.size();
            for (String stage : worldStages) {
                registry.setWorldStage(stage, false);
            }
            boolean silent = silentOverride != null ? silentOverride : false;
            boolean broadcastToOps = !hideInLog;
            if (!silent) {
                String feedback = "§aCleared " + stageCount + " world stages";
                source.sendSuccess(() -> Component.literal(feedback), broadcastToOps);
            }
            return stageCount;
        } catch (Exception e) {
            LOGGER.error("Error in clearWorldStages command", e);
            sendUsage(context.getSource(), "clear_world");
            return 0;
        }
    }
    
    /**
     * Clears all stages (player and world) for target player(s) (@p, @a, @r, @e, @s, @n)
     * @param hideInLog se true, il feedback non viene inviato agli op / non viene registrato nel log
     */
    private static int clearAllStages(CommandContext<CommandSourceStack> context, Boolean silentOverride, boolean hideInLog) {
        try {
            CommandSourceStack source = context.getSource();
            List<ServerPlayer> targets = getTargetPlayers(context, "target");
            if (targets.isEmpty()) {
                source.sendFailure(Component.literal("§cNo player found from selector"));
                return 0;
            }
            int playerStagesCleared = clearPlayerStages(context, true, hideInLog);
            int worldStagesCleared = clearWorldStages(context, true, hideInLog);
            boolean silent = silentOverride != null ? silentOverride : false;
            boolean broadcastToOps = !hideInLog;
            if (!silent) {
                String names = targets.stream().map(p -> p.getName().getString()).reduce((a, b) -> a + ", " + b).orElse("");
                String feedback = "§aCleared " + playerStagesCleared + " player stages and " + worldStagesCleared + " world stages for " + names;
                source.sendSuccess(() -> Component.literal(feedback), broadcastToOps);
            }
            return playerStagesCleared + worldStagesCleared;
        } catch (CommandSyntaxException e) {
            LOGGER.error("Error in clearAllStages command", e);
            sendUsage(context.getSource(), "clear_all");
            return 0;
        }
    }
    
    /**
     * Sets a player stage for the player executing the command
     * @param hideInLog se true, il feedback non viene inviato agli op / non viene registrato nel log
     */
    private static int setPlayerStageForSelf(CommandContext<CommandSourceStack> context, Boolean valueOverride, Boolean silentOverride, boolean hideInLog) {
        try {
            CommandSourceStack source = context.getSource();
            ServerPlayer player = source.getPlayerOrException();
            String stage = StringArgumentType.getString(context, "stage");
            boolean value = valueOverride != null ? valueOverride : BoolArgumentType.getBool(context, "value");
            boolean silent = silentOverride != null ? silentOverride : context.getArgument("silent", Boolean.class);
            boolean broadcastToOps = !hideInLog;
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            boolean success = registry.setPlayerStage(player, stage, value);
            if (success && !silent) {
                Component message = value
                    ? Component.literal("Added stage §a" + stage + "§r to you")
                    : Component.literal("Removed stage §c" + stage + "§r from you");
                source.sendSuccess(() -> message, broadcastToOps);
                Component playerMessage = value
                    ? Component.literal("§aYou gained the stage: §e" + stage)
                    : Component.literal("§cYou lost the stage: §e" + stage);
                player.sendSystemMessage(playerMessage);
            }
            return success ? 1 : 0;
        } catch (CommandSyntaxException e) {
            LOGGER.error("Error executing command: {}", e.getMessage());
            context.getSource().sendFailure(Component.literal("§cThis command must be executed by a player"));
            return 0;
        } catch (Exception e) {
            LOGGER.error("Error in setPlayerStageForSelf command", e);
            sendUsage(context.getSource(), valueOverride == null ? "set_player" : (valueOverride ? "add_player" : "remove_player"));
            return 0;
        }
    }
    
    /**
     * Clears all player stages for the player executing the command
     * @param hideInLog se true, il feedback non viene inviato agli op / non viene registrato nel log
     */
    private static int clearPlayerStagesForSelf(CommandContext<CommandSourceStack> context, Boolean silentOverride, boolean hideInLog) {
        try {
            CommandSourceStack source = context.getSource();
            ServerPlayer player = source.getPlayerOrException();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            List<String> playerStages = registry.getPlayerStages(player);
            int stageCount = playerStages.size();
            for (String stage : playerStages) {
                registry.setPlayerStage(player, stage, false);
            }
            boolean silent = silentOverride != null ? silentOverride : false;
            boolean broadcastToOps = !hideInLog;
            if (!silent) {
                String feedback = "§aCleared " + stageCount + " player stages for you";
                source.sendSuccess(() -> Component.literal(feedback), broadcastToOps);
            }
            return stageCount;
        } catch (CommandSyntaxException e) {
            LOGGER.error("Error executing command: {}", e.getMessage());
            context.getSource().sendFailure(Component.literal("§cThis command must be executed by a player"));
            return 0;
        } catch (Exception e) {
            LOGGER.error("Error in clearPlayerStagesForSelf command", e);
            sendUsage(context.getSource(), "clear_player");
            return 0;
        }
    }
    
    /**
     * Clears all stages (player and world) for the player executing the command
     * @param hideInLog se true, il feedback non viene inviato agli op / non viene registrato nel log
     */
    private static int clearAllStagesForSelf(CommandContext<CommandSourceStack> context, Boolean silentOverride, boolean hideInLog) {
        try {
            CommandSourceStack source = context.getSource();
            ServerPlayer player = source.getPlayerOrException();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            List<String> playerStages = registry.getPlayerStages(player);
            int playerStagesCount = playerStages.size();
            for (String stage : playerStages) {
                registry.setPlayerStage(player, stage, false);
            }
            List<String> worldStages = registry.getWorldStages();
            int worldStagesCount = worldStages.size();
            for (String stage : worldStages) {
                registry.setWorldStage(stage, false);
            }
            boolean silent = silentOverride != null ? silentOverride : false;
            boolean broadcastToOps = !hideInLog;
            if (!silent) {
                String feedback = "§aCleared " + playerStagesCount + " player stages and " + worldStagesCount + " world stages for you";
                source.sendSuccess(() -> Component.literal(feedback), broadcastToOps);
            }
            return playerStagesCount + worldStagesCount;
        } catch (CommandSyntaxException e) {
            LOGGER.error("Error executing command: {}", e.getMessage());
            context.getSource().sendFailure(Component.literal("§cThis command must be executed by a player"));
            return 0;
        } catch (Exception e) {
            LOGGER.error("Error in clearAllStagesForSelf command", e);
            sendUsage(context.getSource(), "clear_all");
            return 0;
        }
    }
    
    /**
     * Sets a team stage
     * @param hideInLog se true, il feedback non viene inviato agli op / non viene registrato nel log
     */
    private static int setTeamStage(CommandContext<CommandSourceStack> context, Boolean valueOverride, Boolean silentOverride, boolean hideInLog) {
        try {
            CommandSourceStack source = context.getSource();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            String teamName = StringArgumentType.getString(context, "team_name");
            String stage = StringArgumentType.getString(context, "stage");
            boolean value = valueOverride != null ? valueOverride : BoolArgumentType.getBool(context, "value");
            boolean silent = silentOverride != null ? silentOverride : context.getArgument("silent", Boolean.class);
            boolean broadcastToOps = !hideInLog;
            boolean success = registry.setTeamStage(teamName, stage, value);
            if (success && !silent) {
                Component message = value
                    ? Component.literal("Added stage §a" + stage + "§r to team §b" + teamName)
                    : Component.literal("Removed stage §c" + stage + "§r from team §b" + teamName);
                source.sendSuccess(() -> message, broadcastToOps);
            }
            return success ? 1 : 0;
        } catch (Exception e) {
            LOGGER.error("Error in setTeamStage command", e);
            sendUsage(context.getSource(), valueOverride == null ? "set_team" : (valueOverride ? "add_team" : "remove_team"));
            return 0;
        }
    }
    
    /**
     * Sets a team player stage for target player(s) (@p, @a, @r, @e, @s, @n)
     * @param hideInLog se true, il feedback non viene inviato agli op / non viene registrato nel log
     */
    private static int setTeamPlayerStage(CommandContext<CommandSourceStack> context, Boolean valueOverride, Boolean silentOverride, boolean hideInLog) {
        try {
            CommandSourceStack source = context.getSource();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            List<ServerPlayer> targets = getTargetPlayers(context, "target");
            if (targets.isEmpty()) {
                source.sendFailure(Component.literal("§cNo player found from selector"));
                return 0;
            }
            String stage = StringArgumentType.getString(context, "stage");
            boolean value = valueOverride != null ? valueOverride : BoolArgumentType.getBool(context, "value");
            boolean silent = silentOverride != null ? silentOverride : context.getArgument("silent", Boolean.class);
            boolean broadcastToOps = !hideInLog;
            int count = 0;
            for (ServerPlayer player : targets) {
                boolean success = registry.setPlayerTeamStage(player, stage, value);
                if (success) {
                    count++;
                    if (!silent) {
                        Component message = value
                            ? Component.literal("Added team stage §a" + stage + "§r to player §e" + player.getName().getString())
                            : Component.literal("Removed team stage §c" + stage + "§r from player §e" + player.getName().getString());
                        source.sendSuccess(() -> message, broadcastToOps);
                        Component playerMessage = value
                            ? Component.literal("§aYour team gained the stage: §e" + stage)
                            : Component.literal("§cYour team lost the stage: §e" + stage);
                        player.sendSystemMessage(playerMessage);
                    }
                }
            }
            return count;
        } catch (CommandSyntaxException e) {
            LOGGER.error("Error in setTeamPlayerStage command", e);
            sendUsage(context.getSource(), valueOverride == null ? "set_team_player" : (valueOverride ? "add_team_player" : "remove_team_player"));
            return 0;
        }
    }
    
    /**
     * Sets a team player stage for the player executing the command
     * @param hideInLog se true, il feedback non viene inviato agli op / non viene registrato nel log
     */
    private static int setTeamPlayerStageForSelf(CommandContext<CommandSourceStack> context, Boolean valueOverride, Boolean silentOverride, boolean hideInLog) {
        try {
            CommandSourceStack source = context.getSource();
            ServerPlayer player = source.getPlayerOrException();
            String stage = StringArgumentType.getString(context, "stage");
            boolean value = valueOverride != null ? valueOverride : BoolArgumentType.getBool(context, "value");
            boolean silent = silentOverride != null ? silentOverride : context.getArgument("silent", Boolean.class);
            boolean broadcastToOps = !hideInLog;
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            boolean success = registry.setPlayerTeamStage(player, stage, value);
            if (success && !silent) {
                Component message = value
                    ? Component.literal("Added team stage §a" + stage + "§r to you")
                    : Component.literal("Removed team stage §c" + stage + "§r from you");
                source.sendSuccess(() -> message, broadcastToOps);
                Component playerMessage = value
                    ? Component.literal("§aYour team gained the stage: §e" + stage)
                    : Component.literal("§cYour team lost the stage: §e" + stage);
                player.sendSystemMessage(playerMessage);
            }
            return success ? 1 : 0;
        } catch (CommandSyntaxException e) {
            LOGGER.error("Error executing command: {}", e.getMessage());
            context.getSource().sendFailure(Component.literal("§cThis command must be executed by a player"));
            return 0;
        } catch (Exception e) {
            LOGGER.error("Error in setTeamPlayerStageForSelf command", e);
            sendUsage(context.getSource(), valueOverride == null ? "set_team_player" : (valueOverride ? "add_team_player" : "remove_team_player"));
            return 0;
        }
    }

    /**
     * Clears all stages of a specific team
     * @param hideInLog se true, il feedback non viene inviato agli op / non viene registrato nel log
     */
    private static int clearTeamStages(CommandContext<CommandSourceStack> context, Boolean silentOverride, boolean hideInLog) {
        try {
            CommandSourceStack source = context.getSource();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            String teamName = StringArgumentType.getString(context, "team_name");
            List<String> teamStages = registry.getTeamStages(teamName);
            int stageCount = teamStages.size();
            for (String stage : teamStages) {
                registry.setTeamStage(teamName, stage, false);
            }
            boolean silent = silentOverride != null ? silentOverride : false;
            boolean broadcastToOps = !hideInLog;
            if (!silent) {
                String feedback = "§aCleared " + stageCount + " team stages for " + teamName;
                source.sendSuccess(() -> Component.literal(feedback), broadcastToOps);
            }
            return stageCount;
        } catch (Exception e) {
            LOGGER.error("Error in clearTeamStages command", e);
            sendUsage(context.getSource(), "clear_team");
            return 0;
        }
    }

    /**
     * Clears all team stages for target player(s) (@p, @a, @r, @e, @s, @n)
     * @param hideInLog se true, il feedback non viene inviato agli op / non viene registrato nel log
     */
    private static int clearTeamPlayerStages(CommandContext<CommandSourceStack> context, Boolean silentOverride, boolean hideInLog) {
        try {
            CommandSourceStack source = context.getSource();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            List<ServerPlayer> targets = getTargetPlayers(context, "target");
            if (targets.isEmpty()) {
                source.sendFailure(Component.literal("§cNo player found from selector"));
                return 0;
            }
            boolean silent = silentOverride != null ? silentOverride : false;
            boolean broadcastToOps = !hideInLog;
            int totalCleared = 0;
            for (ServerPlayer targetPlayer : targets) {
                List<String> teamStages = registry.getPlayerTeamStages(targetPlayer);
                for (String stage : teamStages) {
                    registry.setPlayerTeamStage(targetPlayer, stage, false);
                    totalCleared++;
                }
                if (!silent) {
                    String feedback = "§aCleared team stages for player " + targetPlayer.getName().getString();
                    source.sendSuccess(() -> Component.literal(feedback), broadcastToOps);
                }
            }
            return totalCleared;
        } catch (CommandSyntaxException e) {
            LOGGER.error("Error in clearTeamPlayerStages command", e);
            sendUsage(context.getSource(), "clear_team_player");
            return 0;
        }
    }

    /**
     * Clears all stages of the team of the player executing the command
     * @param hideInLog se true, il feedback non viene inviato agli op / non viene registrato nel log
     */
    private static int clearTeamPlayerStagesForSelf(CommandContext<CommandSourceStack> context, Boolean silentOverride, boolean hideInLog) {
        try {
            CommandSourceStack source = context.getSource();
            ServerPlayer player = source.getPlayerOrException();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            List<String> teamStages = registry.getPlayerTeamStages(player);
            int stageCount = teamStages.size();
            for (String stage : teamStages) {
                registry.setPlayerTeamStage(player, stage, false);
            }
            boolean silent = silentOverride != null ? silentOverride : false;
            boolean broadcastToOps = !hideInLog;
            if (!silent) {
                String feedback = "§aCleared " + stageCount + " team stages for you";
                source.sendSuccess(() -> Component.literal(feedback), broadcastToOps);
            }
            return stageCount;
        } catch (CommandSyntaxException e) {
            LOGGER.error("Error executing command: {}", e.getMessage());
            context.getSource().sendFailure(Component.literal("§cThis command must be executed by a player"));
            return 0;
        } catch (Exception e) {
            LOGGER.error("Error in clearTeamPlayerStagesForSelf command", e);
            sendUsage(context.getSource(), "clear_team_player");
            return 0;
        }
    }
}