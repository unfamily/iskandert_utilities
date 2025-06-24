package net.unfamily.iskautils.stage;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.unfamily.iskautils.IskaUtils;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        
        COMMAND_USAGE.put("add_player", "/iska_utils_stage add player [target player] <stage> [silent=false]");
        COMMAND_USAGE.put("add_world", "/iska_utils_stage add world <stage> [silent=false]");
        COMMAND_USAGE.put("add_team", "/iska_utils_stage add team <team_name> <stage> [silent=false]");
        COMMAND_USAGE.put("add_team_player", "/iska_utils_stage add team_player [target player] <stage> [silent=false]");
        
        COMMAND_USAGE.put("remove_player", "/iska_utils_stage remove player [target player] <stage> [silent=false]");
        COMMAND_USAGE.put("remove_world", "/iska_utils_stage remove world <stage> [silent=false]");
        COMMAND_USAGE.put("remove_team", "/iska_utils_stage remove team <team_name> <stage> [silent=false]");
        COMMAND_USAGE.put("remove_team_player", "/iska_utils_stage remove team_player [target player] <stage> [silent=false]");
        
        COMMAND_USAGE.put("clear_player", "/iska_utils_stage clear player [target player] [silent=false]");
        COMMAND_USAGE.put("clear_world", "/iska_utils_stage clear world [silent=false]");
        COMMAND_USAGE.put("clear_team", "/iska_utils_stage clear team <team_name> [silent=false]");
        COMMAND_USAGE.put("clear_team_player", "/iska_utils_stage clear team_player [target player] [silent=false]");
        COMMAND_USAGE.put("clear_all", "/iska_utils_stage clear all [target player] [silent=false]");
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
        dispatcher.register(
            Commands.literal("iska_utils_stage")
                .requires(source -> source.hasPermission(2))
                
                // LIST commands
                .then(Commands.literal("list")
                    .then(Commands.literal("all")
                        .executes(StageCommand::listAllStages)
                        .then(Commands.argument("target", EntityArgument.player())
                            .executes(StageCommand::listAllStagesForTarget)))
                    .then(Commands.literal("player")
                        .executes(StageCommand::listPlayerStages)
                        .then(Commands.argument("target", EntityArgument.player())
                            .executes(StageCommand::listPlayerStagesForTarget)))
                    .then(Commands.literal("world")
                        .executes(StageCommand::listWorldStages))
                    .then(Commands.literal("team")
                        .then(Commands.argument("team_name", StringArgumentType.string())
                            .executes(StageCommand::listTeamStages)))
                    .then(Commands.literal("team_player")
                        .executes(StageCommand::listTeamPlayerStages)
                        .then(Commands.argument("target", EntityArgument.player())
                            .executes(StageCommand::listTeamPlayerStagesForTarget))))
                
                // SET commands
                .then(Commands.literal("set")
                    .then(Commands.literal("player")
                        .then(Commands.argument("target", EntityArgument.player())
                            .then(Commands.argument("stage", StringArgumentType.string())
                                .executes(ctx -> setPlayerStage(ctx, true, false))
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                    .executes(ctx -> setPlayerStage(ctx, null, false))
                                    .then(Commands.argument("silent", BoolArgumentType.bool())
                                        .executes(ctx -> setPlayerStage(ctx, null, null))))))
                        .then(Commands.argument("stage", StringArgumentType.string())
                            .executes(ctx -> setPlayerStageForSelf(ctx, true, false))
                            .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> setPlayerStageForSelf(ctx, null, false))
                                .then(Commands.argument("silent", BoolArgumentType.bool())
                                    .executes(ctx -> setPlayerStageForSelf(ctx, null, null))))))
                    .then(Commands.literal("world")
                        .then(Commands.argument("stage", StringArgumentType.string())
                            .executes(ctx -> setWorldStage(ctx, true, false))
                            .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> setWorldStage(ctx, null, false))
                                .then(Commands.argument("silent", BoolArgumentType.bool())
                                    .executes(ctx -> setWorldStage(ctx, null, null))))))
                    .then(Commands.literal("team")
                        .then(Commands.argument("team_name", StringArgumentType.string())
                            .then(Commands.argument("stage", StringArgumentType.string())
                                .executes(ctx -> setTeamStage(ctx, true, false))
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                    .executes(ctx -> setTeamStage(ctx, null, false))
                                    .then(Commands.argument("silent", BoolArgumentType.bool())
                                        .executes(ctx -> setTeamStage(ctx, null, null)))))))
                    .then(Commands.literal("team_player")
                        .then(Commands.argument("target", EntityArgument.player())
                            .then(Commands.argument("stage", StringArgumentType.string())
                                .executes(ctx -> setTeamPlayerStage(ctx, true, false))
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                    .executes(ctx -> setTeamPlayerStage(ctx, null, false))
                                    .then(Commands.argument("silent", BoolArgumentType.bool())
                                        .executes(ctx -> setTeamPlayerStage(ctx, null, null))))))
                        .then(Commands.argument("stage", StringArgumentType.string())
                            .executes(ctx -> setTeamPlayerStageForSelf(ctx, true, false))
                            .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> setTeamPlayerStageForSelf(ctx, null, false))
                                .then(Commands.argument("silent", BoolArgumentType.bool())
                                    .executes(ctx -> setTeamPlayerStageForSelf(ctx, null, null)))))))
                
                // ADD commands
                .then(Commands.literal("add")
                    .then(Commands.literal("player")
                        .then(Commands.argument("target", EntityArgument.player())
                            .then(Commands.argument("stage", StringArgumentType.string())
                                .executes(ctx -> setPlayerStage(ctx, true, false))
                                .then(Commands.argument("silent", BoolArgumentType.bool())
                                    .executes(ctx -> setPlayerStage(ctx, true, null)))))
                        .then(Commands.argument("stage", StringArgumentType.string())
                            .executes(ctx -> setPlayerStageForSelf(ctx, true, false))
                            .then(Commands.argument("silent", BoolArgumentType.bool())
                                .executes(ctx -> setPlayerStageForSelf(ctx, true, null)))))
                    .then(Commands.literal("world")
                        .then(Commands.argument("stage", StringArgumentType.string())
                            .executes(ctx -> setWorldStage(ctx, true, false))
                            .then(Commands.argument("silent", BoolArgumentType.bool())
                                .executes(ctx -> setWorldStage(ctx, true, null)))))
                    .then(Commands.literal("team")
                        .then(Commands.argument("team_name", StringArgumentType.string())
                            .then(Commands.argument("stage", StringArgumentType.string())
                                .executes(ctx -> setTeamStage(ctx, true, false))
                                .then(Commands.argument("silent", BoolArgumentType.bool())
                                    .executes(ctx -> setTeamStage(ctx, true, null))))))
                    .then(Commands.literal("team_player")
                        .then(Commands.argument("target", EntityArgument.player())
                            .then(Commands.argument("stage", StringArgumentType.string())
                                .executes(ctx -> setTeamPlayerStage(ctx, true, false))
                                .then(Commands.argument("silent", BoolArgumentType.bool())
                                    .executes(ctx -> setTeamPlayerStage(ctx, true, null)))))
                        .then(Commands.argument("stage", StringArgumentType.string())
                            .executes(ctx -> setTeamPlayerStageForSelf(ctx, true, false))
                            .then(Commands.argument("silent", BoolArgumentType.bool())
                                .executes(ctx -> setTeamPlayerStageForSelf(ctx, true, null))))))
                
                // REMOVE commands
                .then(Commands.literal("remove")
                    .then(Commands.literal("player")
                        .then(Commands.argument("target", EntityArgument.player())
                            .then(Commands.argument("stage", StringArgumentType.string())
                                .executes(ctx -> setPlayerStage(ctx, false, false))
                                .then(Commands.argument("silent", BoolArgumentType.bool())
                                    .executes(ctx -> setPlayerStage(ctx, false, null)))))
                        .then(Commands.argument("stage", StringArgumentType.string())
                            .executes(ctx -> setPlayerStageForSelf(ctx, false, false))
                            .then(Commands.argument("silent", BoolArgumentType.bool())
                                .executes(ctx -> setPlayerStageForSelf(ctx, false, null)))))
                    .then(Commands.literal("world")
                        .then(Commands.argument("stage", StringArgumentType.string())
                            .executes(ctx -> setWorldStage(ctx, false, false))
                            .then(Commands.argument("silent", BoolArgumentType.bool())
                                .executes(ctx -> setWorldStage(ctx, false, null)))))
                    .then(Commands.literal("team")
                        .then(Commands.argument("team_name", StringArgumentType.string())
                            .then(Commands.argument("stage", StringArgumentType.string())
                                .executes(ctx -> setTeamStage(ctx, false, false))
                                .then(Commands.argument("silent", BoolArgumentType.bool())
                                    .executes(ctx -> setTeamStage(ctx, false, null))))))
                    .then(Commands.literal("team_player")
                        .then(Commands.argument("target", EntityArgument.player())
                            .then(Commands.argument("stage", StringArgumentType.string())
                                .executes(ctx -> setTeamPlayerStage(ctx, false, false))
                                .then(Commands.argument("silent", BoolArgumentType.bool())
                                    .executes(ctx -> setTeamPlayerStage(ctx, false, null)))))
                        .then(Commands.argument("stage", StringArgumentType.string())
                            .executes(ctx -> setTeamPlayerStageForSelf(ctx, false, false))
                            .then(Commands.argument("silent", BoolArgumentType.bool())
                                .executes(ctx -> setTeamPlayerStageForSelf(ctx, false, null))))))
                
                // CLEAR commands
                .then(Commands.literal("clear")
                    .then(Commands.literal("player")
                        .then(Commands.argument("target", EntityArgument.player())
                            .executes(ctx -> clearPlayerStages(ctx, false))
                            .then(Commands.argument("silent", BoolArgumentType.bool())
                                .executes(ctx -> clearPlayerStages(ctx, null))))
                        .executes(ctx -> clearPlayerStagesForSelf(ctx, false))
                        .then(Commands.argument("silent", BoolArgumentType.bool())
                            .executes(ctx -> clearPlayerStagesForSelf(ctx, null))))
                    .then(Commands.literal("world")
                        .executes(ctx -> clearWorldStages(ctx, false))
                        .then(Commands.argument("silent", BoolArgumentType.bool())
                            .executes(ctx -> clearWorldStages(ctx, null))))
                    .then(Commands.literal("team")
                        .then(Commands.argument("team_name", StringArgumentType.string())
                            .executes(ctx -> clearTeamStages(ctx, false))
                            .then(Commands.argument("silent", BoolArgumentType.bool())
                                .executes(ctx -> clearTeamStages(ctx, null)))))
                    .then(Commands.literal("team_player")
                        .then(Commands.argument("target", EntityArgument.player())
                            .executes(ctx -> clearTeamPlayerStages(ctx, false))
                            .then(Commands.argument("silent", BoolArgumentType.bool())
                                .executes(ctx -> clearTeamPlayerStages(ctx, null))))
                        .executes(ctx -> clearTeamPlayerStagesForSelf(ctx, false))
                        .then(Commands.argument("silent", BoolArgumentType.bool())
                            .executes(ctx -> clearTeamPlayerStagesForSelf(ctx, null))))
                    .then(Commands.literal("all")
                        .then(Commands.argument("target", EntityArgument.player())
                            .executes(ctx -> clearAllStages(ctx, false))
                            .then(Commands.argument("silent", BoolArgumentType.bool())
                                .executes(ctx -> clearAllStages(ctx, null))))
                        .executes(ctx -> clearAllStagesForSelf(ctx, false))
                        .then(Commands.argument("silent", BoolArgumentType.bool())
                            .executes(ctx -> clearAllStagesForSelf(ctx, null)))))
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
     * Lists all stages for a specific target player
     */
    private static int listAllStagesForTarget(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            
            // Get the target player
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "target");
            String playerName = targetPlayer.getName().getString();
            
            source.sendSuccess(() -> Component.literal("§6===== All Stages for " + playerName + " ====="), false);
            
            // World stages
            List<String> worldStages = registry.getWorldStages();
            String worldStagesText = worldStages.isEmpty() ? "§7(none)" : "§a" + String.join(", ", worldStages);
            source.sendSuccess(() -> Component.literal("§2World Stages: " + worldStagesText), false);
            
            // Player stages for target player
            List<String> playerStages = registry.getPlayerStages(targetPlayer);
            String playerStagesText = playerStages.isEmpty() ? "§7(none)" : "§a" + String.join(", ", playerStages);
            source.sendSuccess(() -> Component.literal("§dPlayer Stages for " + playerName + ": " + playerStagesText), false);
            
            return 1;
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
     * Lists player stages for a specific target player
     */
    private static int listPlayerStagesForTarget(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            
            // Get the target player
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "target");
            String playerName = targetPlayer.getName().getString();
            
            source.sendSuccess(() -> Component.literal("§6===== Player Stages for " + playerName + " ====="), false);
            
            List<String> playerStages = registry.getPlayerStages(targetPlayer);
            String playerStagesText = playerStages.isEmpty() ? "§7(none)" : "§a" + String.join(", ", playerStages);
            source.sendSuccess(() -> Component.literal("§dPlayer Stages: " + playerStagesText), false);
            
            return 1;
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
     * Lists team stages for a specific target player
     */
    private static int listTeamPlayerStagesForTarget(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            
            // Get the target player
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "target");
            String playerName = targetPlayer.getName().getString();
            
            source.sendSuccess(() -> Component.literal("§6===== Team Player Stages for " + playerName + " ====="), false);
            
            List<String> teamPlayerStages = registry.getPlayerTeamStages(targetPlayer);
            String teamPlayerStagesText = teamPlayerStages.isEmpty() ? "§7(none)" : "§b" + String.join(", ", teamPlayerStages);
            source.sendSuccess(() -> Component.literal("§bTeam Player Stages: " + teamPlayerStagesText), false);
            
            return 1;
        } catch (CommandSyntaxException e) {
            LOGGER.error("Error in listTeamPlayerStagesForTarget command", e);
            sendUsage(context.getSource(), "list_team_player");
            return 0;
        }
    }
    
    /**
     * Sets a player stage
     */
    private static int setPlayerStage(CommandContext<CommandSourceStack> context, Boolean valueOverride, Boolean silentOverride) {
        try {
            CommandSourceStack source = context.getSource();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            
            // Get command arguments
            ServerPlayer player = EntityArgument.getPlayer(context, "target");
            String stage = StringArgumentType.getString(context, "stage");
            
            // Get optional arguments or use defaults
            boolean value = valueOverride != null 
                ? valueOverride 
                : BoolArgumentType.getBool(context, "value");
                
            boolean silent = silentOverride != null
                ? silentOverride
                : context.getArgument("silent", Boolean.class);
            
            // Set the stage
            boolean success = registry.setPlayerStage(player, stage, value);
            
            if (success && !silent) {
                Component message = value 
                    ? Component.literal("Added stage §a" + stage + "§r to player §e" + player.getName().getString())
                    : Component.literal("Removed stage §c" + stage + "§r from player §e" + player.getName().getString());
                    
                source.sendSuccess(() -> message, true);
                
                // Also notify the target player
                Component playerMessage = value 
                    ? Component.literal("§aYou gained the stage: §e" + stage)
                    : Component.literal("§cYou lost the stage: §e" + stage);
                    
                player.sendSystemMessage(playerMessage);
            }
            
            return success ? 1 : 0;
        } catch (CommandSyntaxException e) {
            LOGGER.error("Error in setPlayerStage command", e);
            sendUsage(context.getSource(), valueOverride == null ? "set_player" : (valueOverride ? "add_player" : "remove_player"));
            return 0;
        }
    }
    
    /**
     * Sets a world stage
     */
    private static int setWorldStage(CommandContext<CommandSourceStack> context, Boolean valueOverride, Boolean silentOverride) {
        try {
            CommandSourceStack source = context.getSource();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            
            // Get command arguments
            String stage = StringArgumentType.getString(context, "stage");
            
            // Get optional arguments or use defaults
            boolean value = valueOverride != null 
                ? valueOverride 
                : BoolArgumentType.getBool(context, "value");
                
            boolean silent = silentOverride != null
                ? silentOverride
                : context.getArgument("silent", Boolean.class);
            
            // Set the stage
            boolean success = registry.setWorldStage(stage, value);
            
            if (success && !silent) {
                Component message = value 
                    ? Component.literal("Added stage §a" + stage + "§r to world")
                    : Component.literal("Removed stage §c" + stage + "§r from world");
                    
                source.sendSuccess(() -> message, true);
            }
            
            return success ? 1 : 0;
        } catch (Exception e) {
            LOGGER.error("Error in setWorldStage command", e);
            sendUsage(context.getSource(), valueOverride == null ? "set_world" : (valueOverride ? "add_world" : "remove_world"));
            return 0;
        }
    }
    
    /**
     * Clears all player stages for a specific target player
     */
    private static int clearPlayerStages(CommandContext<CommandSourceStack> context, Boolean silentOverride) {
        try {
            CommandSourceStack source = context.getSource();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            
            // Get the target player
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "target");
            String playerName = targetPlayer.getName().getString();
            
            // Get all player stages
            List<String> playerStages = registry.getPlayerStages(targetPlayer);
            int stageCount = playerStages.size();
            
            // Remove each stage
            for (String stage : playerStages) {
                registry.setPlayerStage(targetPlayer, stage, false);
            }
            
            // Determine if this is a silent operation
            boolean silent = silentOverride != null ? silentOverride : false;
            
            // Send feedback
            if (!silent) {
                String feedback = "§aCleared " + stageCount + " player stages for " + playerName;
                source.sendSuccess(() -> Component.literal(feedback), true);
            }
            
            return stageCount;
        } catch (CommandSyntaxException e) {
            LOGGER.error("Error in clearPlayerStages command", e);
            sendUsage(context.getSource(), "clear_player");
            return 0;
        }
    }
    
    /**
     * Clears all world stages
     */
    private static int clearWorldStages(CommandContext<CommandSourceStack> context, Boolean silentOverride) {
        try {
            CommandSourceStack source = context.getSource();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            
            // Get all world stages
            List<String> worldStages = registry.getWorldStages();
            int stageCount = worldStages.size();
            
            // Remove each stage
            for (String stage : worldStages) {
                registry.setWorldStage(stage, false);
            }
            
            // Determine if this is a silent operation
            boolean silent = silentOverride != null ? silentOverride : false;
            
            // Send feedback
            if (!silent) {
                String feedback = "§aCleared " + stageCount + " world stages";
                source.sendSuccess(() -> Component.literal(feedback), true);
            }
            
            return stageCount;
        } catch (Exception e) {
            LOGGER.error("Error in clearWorldStages command", e);
            sendUsage(context.getSource(), "clear_world");
            return 0;
        }
    }
    
    /**
     * Clears all stages (player and world) for a specific target player
     */
    private static int clearAllStages(CommandContext<CommandSourceStack> context, Boolean silentOverride) {
        try {
            CommandSourceStack source = context.getSource();
            
            // First clear player stages
            int playerStagesCleared = clearPlayerStages(context, true);
            
            // Then clear world stages
            int worldStagesCleared = clearWorldStages(context, true);
            
            // Get the target player name for the feedback
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "target");
            String playerName = targetPlayer.getName().getString();
            
            // Determine if this is a silent operation
            boolean silent = silentOverride != null ? silentOverride : false;
            
            // Send feedback
            if (!silent) {
                String feedback = "§aCleared " + playerStagesCleared + " player stages and " + 
                    worldStagesCleared + " world stages for " + playerName;
                source.sendSuccess(() -> Component.literal(feedback), true);
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
     */
    private static int setPlayerStageForSelf(CommandContext<CommandSourceStack> context, Boolean valueOverride, Boolean silentOverride) {
        try {
            CommandSourceStack source = context.getSource();
            
            // Ottieni il giocatore che esegue il comando
            ServerPlayer player = source.getPlayerOrException();
            
            // Get command arguments
            String stage = StringArgumentType.getString(context, "stage");
            
            // Get optional arguments or use defaults
            boolean value = valueOverride != null 
                ? valueOverride 
                : BoolArgumentType.getBool(context, "value");
                
            boolean silent = silentOverride != null
                ? silentOverride
                : context.getArgument("silent", Boolean.class);
            
            // Imposta lo stage usando il registry
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            boolean success = registry.setPlayerStage(player, stage, value);
            
            if (success && !silent) {
                Component message = value 
                    ? Component.literal("Added stage §a" + stage + "§r to you")
                    : Component.literal("Removed stage §c" + stage + "§r from you");
                    
                source.sendSuccess(() -> message, true);
                
                // Also notify the player
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
     */
    private static int clearPlayerStagesForSelf(CommandContext<CommandSourceStack> context, Boolean silentOverride) {
        try {
            CommandSourceStack source = context.getSource();
            
            // Ottieni il giocatore che esegue il comando
            ServerPlayer player = source.getPlayerOrException();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            
            // Get all player stages
            List<String> playerStages = registry.getPlayerStages(player);
            int stageCount = playerStages.size();
            
            // Remove each stage
            for (String stage : playerStages) {
                registry.setPlayerStage(player, stage, false);
            }
            
            // Determine if this is a silent operation
            boolean silent = silentOverride != null ? silentOverride : false;
            
            // Send feedback
            if (!silent) {
                String feedback = "§aCleared " + stageCount + " player stages for you";
                source.sendSuccess(() -> Component.literal(feedback), true);
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
     */
    private static int clearAllStagesForSelf(CommandContext<CommandSourceStack> context, Boolean silentOverride) {
        try {
            CommandSourceStack source = context.getSource();
            
            // Make sure we have a player
            ServerPlayer player = source.getPlayerOrException();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            
            // Get all player stages
            List<String> playerStages = registry.getPlayerStages(player);
            int playerStagesCount = playerStages.size();
            
            // Remove each player stage
            for (String stage : playerStages) {
                registry.setPlayerStage(player, stage, false);
            }
            
            // Get all world stages
            List<String> worldStages = registry.getWorldStages();
            int worldStagesCount = worldStages.size();
            
            // Remove each world stage
            for (String stage : worldStages) {
                registry.setWorldStage(stage, false);
            }
            
            // Determine if this is a silent operation
            boolean silent = silentOverride != null ? silentOverride : false;
            
            // Send feedback
            if (!silent) {
                String feedback = "§aCleared " + playerStagesCount + " player stages and " + 
                    worldStagesCount + " world stages for you";
                source.sendSuccess(() -> Component.literal(feedback), true);
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
     */
    private static int setTeamStage(CommandContext<CommandSourceStack> context, Boolean valueOverride, Boolean silentOverride) {
        try {
            CommandSourceStack source = context.getSource();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            
            // Get command arguments
            String teamName = StringArgumentType.getString(context, "team_name");
            String stage = StringArgumentType.getString(context, "stage");
            
            // Get optional arguments or use defaults
            boolean value = valueOverride != null 
                ? valueOverride 
                : BoolArgumentType.getBool(context, "value");
                
            boolean silent = silentOverride != null
                ? silentOverride
                : context.getArgument("silent", Boolean.class);
            
            // Set the stage
            boolean success = registry.setTeamStage(teamName, stage, value);
            
            if (success && !silent) {
                Component message = value 
                    ? Component.literal("Added stage §a" + stage + "§r to team §b" + teamName)
                    : Component.literal("Removed stage §c" + stage + "§r from team §b" + teamName);
                    
                source.sendSuccess(() -> message, true);
            }
            
            return success ? 1 : 0;
        } catch (Exception e) {
            LOGGER.error("Error in setTeamStage command", e);
            sendUsage(context.getSource(), valueOverride == null ? "set_team" : (valueOverride ? "add_team" : "remove_team"));
            return 0;
        }
    }
    
    /**
     * Sets a team player stage for a specific target player
     */
    private static int setTeamPlayerStage(CommandContext<CommandSourceStack> context, Boolean valueOverride, Boolean silentOverride) {
        try {
            CommandSourceStack source = context.getSource();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            
            // Get command arguments
            ServerPlayer player = EntityArgument.getPlayer(context, "target");
            String stage = StringArgumentType.getString(context, "stage");
            
            // Get optional arguments or use defaults
            boolean value = valueOverride != null 
                ? valueOverride 
                : BoolArgumentType.getBool(context, "value");
                
            boolean silent = silentOverride != null
                ? silentOverride
                : context.getArgument("silent", Boolean.class);
            
            // Set the stage
            boolean success = registry.setPlayerTeamStage(player, stage, value);
            
            if (success && !silent) {
                Component message = value 
                    ? Component.literal("Added team stage §a" + stage + "§r to player §e" + player.getName().getString())
                    : Component.literal("Removed team stage §c" + stage + "§r from player §e" + player.getName().getString());
                    
                source.sendSuccess(() -> message, true);
                
                // Also notify the target player
                Component playerMessage = value 
                    ? Component.literal("§aYour team gained the stage: §e" + stage)
                    : Component.literal("§cYour team lost the stage: §e" + stage);
                    
                player.sendSystemMessage(playerMessage);
            }
            
            return success ? 1 : 0;
        } catch (CommandSyntaxException e) {
            LOGGER.error("Error in setTeamPlayerStage command", e);
            sendUsage(context.getSource(), valueOverride == null ? "set_team_player" : (valueOverride ? "add_team_player" : "remove_team_player"));
            return 0;
        }
    }
    
    /**
     * Sets a team player stage for the player executing the command
     */
    private static int setTeamPlayerStageForSelf(CommandContext<CommandSourceStack> context, Boolean valueOverride, Boolean silentOverride) {
        try {
            CommandSourceStack source = context.getSource();
            
            // Ottieni il giocatore che esegue il comando
            ServerPlayer player = source.getPlayerOrException();
            
            // Get command arguments
            String stage = StringArgumentType.getString(context, "stage");
            
            // Get optional arguments or use defaults
            boolean value = valueOverride != null 
                ? valueOverride 
                : BoolArgumentType.getBool(context, "value");
                
            boolean silent = silentOverride != null
                ? silentOverride
                : context.getArgument("silent", Boolean.class);
            
            // Imposta lo stage usando il registry
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            boolean success = registry.setPlayerTeamStage(player, stage, value);
            
            if (success && !silent) {
                Component message = value 
                    ? Component.literal("Added team stage §a" + stage + "§r to you")
                    : Component.literal("Removed team stage §c" + stage + "§r from you");
                    
                source.sendSuccess(() -> message, true);
                
                // Also notify the player
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
     * Cancella tutti gli stage di un team specifico
     */
    private static int clearTeamStages(CommandContext<CommandSourceStack> context, Boolean silentOverride) {
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
            if (!silent) {
                String feedback = "§aCleared " + stageCount + " team stages for " + teamName;
                source.sendSuccess(() -> Component.literal(feedback), true);
            }
            return stageCount;
        } catch (Exception e) {
            LOGGER.error("Error in clearTeamStages command", e);
            sendUsage(context.getSource(), "clear_team");
            return 0;
        }
    }

    /**
     * Cancella tutti gli stage del team di un player specifico
     */
    private static int clearTeamPlayerStages(CommandContext<CommandSourceStack> context, Boolean silentOverride) {
        try {
            CommandSourceStack source = context.getSource();
            StageRegistry registry = StageRegistry.getInstance(source.getServer());
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "target");
            List<String> teamStages = registry.getPlayerTeamStages(targetPlayer);
            int stageCount = teamStages.size();
            for (String stage : teamStages) {
                registry.setPlayerTeamStage(targetPlayer, stage, false);
            }
            boolean silent = silentOverride != null ? silentOverride : false;
            if (!silent) {
                String feedback = "§aCleared " + stageCount + " team stages for player " + targetPlayer.getName().getString();
                source.sendSuccess(() -> Component.literal(feedback), true);
            }
            return stageCount;
        } catch (CommandSyntaxException e) {
            LOGGER.error("Error in clearTeamPlayerStages command", e);
            sendUsage(context.getSource(), "clear_team_player");
            return 0;
        }
    }

    /**
     * Cancella tutti gli stage del team del player che esegue il comando
     */
    private static int clearTeamPlayerStagesForSelf(CommandContext<CommandSourceStack> context, Boolean silentOverride) {
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
            if (!silent) {
                String feedback = "§aCleared " + stageCount + " team stages for you";
                source.sendSuccess(() -> Component.literal(feedback), true);
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