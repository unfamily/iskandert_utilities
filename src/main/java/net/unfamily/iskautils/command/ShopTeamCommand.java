package net.unfamily.iskautils.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.Entity;
import net.unfamily.iskalib.integration.ftbteams.FtbTeamsBridge;
import net.unfamily.iskalib.team.ShopTeamManager;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.shop.ShopLoader;
import net.unfamily.iskautils.shop.ShopCurrency;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Command for managing shop teams
 */
public class ShopTeamCommand {

    private static final SimpleCommandExceptionType ERROR_PLAYER_NOT_FOUND = new SimpleCommandExceptionType(
            Component.literal("No player found from selector"));

    private static boolean ftbSyncActive(ServerPlayer player) {
        return player != null && Config.ftbTeamsSyncEnabled && FtbTeamsBridge.isAvailable();
    }

    private static boolean runFtbTeamsAsPlayer(ServerPlayer player, String commandWithoutSlash) {
        if (player == null || player.level() == null || player.level().getServer() == null) {
            return false;
        }
        try {
            player.level().getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack(), commandWithoutSlash);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static String displayTeamName(ShopTeamManager teamManager, String teamKeyOrNull) {
        if (teamKeyOrNull == null) {
            return null;
        }
        String display = teamManager.getTeamDisplayName(teamKeyOrNull);
        return display != null ? display : teamKeyOrNull;
    }

    /**
     * Gets target players from the argument (supports @p, @a, @r, @e, @s, @n).
     */
    private static List<ServerPlayer> getTargetPlayers(CommandContext<CommandSourceStack> context, String argumentName) throws CommandSyntaxException {
        Collection<? extends Entity> entities = EntityArgument.getEntities(context, argumentName);
        return entities.stream()
                .filter(ServerPlayer.class::isInstance)
                .map(ServerPlayer.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * Gets a single target player (for commands like newLeader; if the selector returns multiple players, uses the first).
     */
    private static ServerPlayer getSingleTargetPlayer(CommandContext<CommandSourceStack> context, String argumentName) throws CommandSyntaxException {
        List<ServerPlayer> players = getTargetPlayers(context, argumentName);
        if (players.isEmpty()) {
            throw ERROR_PLAYER_NOT_FOUND.create();
        }
        return players.get(0);
    }

    /**
     * Registers the team command
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("iska_utils_team")
            .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(0)))) // All players can use
            .then(Commands.literal("create")
                .then(Commands.argument("teamName", StringArgumentType.word())
                    .executes(ShopTeamCommand::createTeam)))
            .then(Commands.literal("delete")
                .executes(ShopTeamCommand::deleteOwnTeam)
                .then(Commands.argument("teamName", StringArgumentType.word())
                    .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2)))) // Admin only for other teams
                    .executes(ShopTeamCommand::deleteTeam)))
            .then(Commands.literal("rename")
                .then(Commands.argument("newName", StringArgumentType.word())
                    .executes(ShopTeamCommand::renameOwnTeam)
                    .then(Commands.argument("teamName", StringArgumentType.word())
                        .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2)))) // Admin only for other teams
                        .executes(ShopTeamCommand::renameTeam))))
            .then(Commands.literal("leader")
                .then(Commands.argument("newLeader", EntityArgument.entities())
                    .executes(ShopTeamCommand::transferOwnTeamLeadership)
                    .then(Commands.argument("teamName", StringArgumentType.word())
                        .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2)))) // Admin only for other teams
                        .executes(ShopTeamCommand::transferTeamLeadership))))
            .then(Commands.literal("assistant")
                .then(Commands.literal("add")
                    .then(Commands.argument("player", EntityArgument.entities())
                        .executes(ShopTeamCommand::addAssistantToOwnTeam)
                        .then(Commands.argument("teamName", StringArgumentType.word())
                            .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2)))) // Admin only for other teams
                            .executes(ShopTeamCommand::addTeamAssistant))))
                .then(Commands.literal("remove")
                    .then(Commands.argument("player", EntityArgument.entities())
                        .executes(ShopTeamCommand::removeAssistantFromOwnTeam)
                        .then(Commands.argument("teamName", StringArgumentType.word())
                            .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2)))) // Admin only for other teams
                            .executes(ShopTeamCommand::removeTeamAssistant))))
                .then(Commands.literal("list")
                    .executes(ShopTeamCommand::listOwnTeamAssistants)
                    .then(Commands.argument("teamName", StringArgumentType.word())
                        .executes(ShopTeamCommand::listTeamAssistants))))
            .then(Commands.literal("invite")
                .then(Commands.argument("player", EntityArgument.entities())
                    .executes(ShopTeamCommand::inviteToOwnTeam)
                    .then(Commands.argument("teamName", StringArgumentType.word())
                        .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2)))) // Admin only for other teams
                        .executes(ShopTeamCommand::inviteToTeam))))
            .then(Commands.literal("cancelInvite")
                .then(Commands.argument("player", EntityArgument.entities())
                    .executes(ShopTeamCommand::cancelInviteFromOwnTeam)
                    .then(Commands.argument("teamName", StringArgumentType.word())
                        .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2)))) // Admin only for other teams
                        .executes(ShopTeamCommand::cancelInviteFromTeam))))
            .then(Commands.literal("accept")
                .then(Commands.argument("teamName", StringArgumentType.word())
                    .executes(ShopTeamCommand::acceptInvitation)))
            .then(Commands.literal("leave")
                .executes(ShopTeamCommand::leaveTeam))
            .then(Commands.literal("add")
                .then(Commands.argument("player", EntityArgument.entities())
                    .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2)))) // Admin only - add is special
                    .executes(ShopTeamCommand::addToOwnTeam)
                    .then(Commands.argument("teamName", StringArgumentType.word())
                        .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2)))) // Admin only for other teams
                        .executes(ShopTeamCommand::addPlayer))))
            .then(Commands.literal("remove")
                .then(Commands.argument("player", EntityArgument.entities())
                    .executes(ShopTeamCommand::removeFromOwnTeam)
                    .then(Commands.argument("teamName", StringArgumentType.word())
                        .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2)))) // Admin only for other teams
                        .executes(ShopTeamCommand::removePlayer))))
            .then(Commands.literal("info")
                .executes(ShopTeamCommand::ownTeamInfo)
                .then(Commands.argument("teamName", StringArgumentType.word())
                    .executes(ShopTeamCommand::teamInfo)))
            .then(Commands.literal("list")
                .executes(ShopTeamCommand::listTeams))
            .then(Commands.literal("members")
                .executes(ShopTeamCommand::listOwnTeamMembers)
                .then(Commands.argument("teamName", StringArgumentType.word())
                    .suggests(ShopTeamCommand::suggestTeams)
                    .executes(ShopTeamCommand::listTeamMembers)))
            .then(Commands.literal("balance")
                .executes(ShopTeamCommand::ownTeamBalance)
                .then(Commands.argument("teamName", StringArgumentType.word())
                    .executes(ShopTeamCommand::getBalance)
                    .then(Commands.argument("currencyId", StringArgumentType.word())
                        .executes(ShopTeamCommand::getCurrencyBalance))))
            .then(Commands.literal("addCurrency")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2)))) // Admin only
                .then(Commands.argument("currencyId", StringArgumentType.word())
                    .suggests(ShopTeamCommand::suggestCurrencies)
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                        .executes(ShopTeamCommand::addCurrencyToOwnTeam)
                        .then(Commands.literal("team")
                            .then(Commands.argument("teamName", StringArgumentType.word())
                                .suggests(ShopTeamCommand::suggestTeams)
                                .executes(ShopTeamCommand::addCurrencyToTeam)))
                        .then(Commands.literal("player")
                            .then(Commands.argument("player", EntityArgument.entities())
                                .executes(ShopTeamCommand::addCurrencyToPlayerTeam))))))
            .then(Commands.literal("removeCurrency")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2)))) // Admin only
                .then(Commands.argument("currencyId", StringArgumentType.word())
                    .suggests(ShopTeamCommand::suggestCurrencies)
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                        .executes(ShopTeamCommand::removeCurrencyFromOwnTeam)
                        .then(Commands.literal("team")
                            .then(Commands.argument("teamName", StringArgumentType.word())
                                .suggests(ShopTeamCommand::suggestTeams)
                                .executes(ShopTeamCommand::removeCurrencyFromTeam)))
                        .then(Commands.literal("player")
                            .then(Commands.argument("player", EntityArgument.entities())
                                .executes(ShopTeamCommand::removeCurrencyFromPlayerTeam))))))
            .then(Commands.literal("setCurrency")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2)))) // Admin only
                .then(Commands.argument("currencyId", StringArgumentType.word())
                    .suggests(ShopTeamCommand::suggestCurrencies)
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                        .executes(ShopTeamCommand::setCurrencyForOwnTeam)
                        .then(Commands.literal("team")
                            .then(Commands.argument("teamName", StringArgumentType.word())
                                .suggests(ShopTeamCommand::suggestTeams)
                                .executes(ShopTeamCommand::setCurrencyForTeam)))
                        .then(Commands.literal("player")
                            .then(Commands.argument("player", EntityArgument.entities())
                                .executes(ShopTeamCommand::setCurrencyForPlayerTeam))))))
            .then(Commands.literal("moveCurrency")
                .then(Commands.argument("currencyId", StringArgumentType.word())
                    .suggests(ShopTeamCommand::suggestCurrencies)
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                        .then(Commands.argument("toTeam", StringArgumentType.word())
                            .suggests(ShopTeamCommand::suggestTeams)
                            .executes(ShopTeamCommand::moveCurrencyFromOwnTeam)
                            .then(Commands.argument("fromTeam", StringArgumentType.word())
                                                        .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2)))) // Admin only for other teams
                                .suggests(ShopTeamCommand::suggestTeams)
                                .executes(ShopTeamCommand::moveCurrencyBetweenTeams))))))
            .then(Commands.literal("invitations")
                .executes(ShopTeamCommand::listInvitations))
            .then(Commands.literal("help")
                .executes(context -> showHelp(context, true, false)) // Default to user commands
                .then(Commands.literal("all")
                    .executes(ShopTeamCommand::showHelp))
                .then(Commands.literal("user")
                    .executes(context -> showHelp(context, true, false)))
                .then(Commands.literal("admin")
                    .executes(context -> showHelp(context, false, true)))));
    }
    
    private static int createTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        String teamName = StringArgumentType.getString(context, "teamName");
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) player.level());

        if (ftbSyncActive(player)) {
            if (runFtbTeamsAsPlayer(player, "ftbteams party create " + teamName)) {
                source.sendSuccess(() -> Component.literal("FTB party created; shop team will sync automatically."), false);
                return 1;
            }
            source.sendFailure(Component.literal("Failed to create FTB party team (check FTB Teams permissions/state)."));
            return 0;
        }
        
        if (teamManager.createTeam(teamName, player)) {
            source.sendSuccess(() -> Component.literal("Team '" + teamName + "' created successfully!"), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to create team. Team might already exist or you're already in a team."));
            return 0;
        }
    }
    
    private static int deleteOwnTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) player.level());
        String teamKey = teamManager.getPlayerTeamKey(player);
        
        if (teamKey == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        
        String teamDisplay = displayTeamName(teamManager, teamKey);
        if (teamManager.deleteTeam(teamKey, player)) {
            source.sendSuccess(() -> Component.literal("Team '" + teamDisplay + "' deleted successfully!"), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to delete team. You might not be the leader."));
            return 0;
        }
    }
    
    private static int deleteTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        String teamName = StringArgumentType.getString(context, "teamName");
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) player.level());

        if (ftbSyncActive(player)) {
            source.sendFailure(Component.literal("When FTB Teams sync is enabled, delete is not supported here. Use FTB Teams party commands."));
            return 0;
        }
        String teamKey = teamManager.resolveTeamKeyByDisplayName(teamName);
        
        if (teamManager.deleteTeam(teamKey, player)) {
            source.sendSuccess(() -> Component.literal("Team '" + teamName + "' deleted successfully!"), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to delete team. You might not be the leader or the team doesn't exist."));
            return 0;
        }
    }
    
    private static int renameOwnTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        String newName = StringArgumentType.getString(context, "newName");
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) player.level());
        String teamKey = teamManager.getPlayerTeamKey(player);
        
        if (teamKey == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        
        if (ftbSyncActive(player)) {
            if (runFtbTeamsAsPlayer(player, "ftbteams party settings display_name " + newName)) {
                source.sendSuccess(() -> Component.literal("FTB party display name updated; shop team will sync automatically."), false);
                return 1;
            }
            source.sendFailure(Component.literal("Failed to rename via FTB Teams (check permissions/state)."));
            return 0;
        }

        String teamDisplay = displayTeamName(teamManager, teamKey);
        if (teamManager.renameTeam(teamKey, newName, player)) {
            source.sendSuccess(() -> Component.literal("Team '" + teamDisplay + "' renamed to '" + newName + "' successfully!"), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to rename team. Team might already exist or you're not the leader."));
            return 0;
        }
    }
    
    private static int renameTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        String teamName = StringArgumentType.getString(context, "teamName");
        String newName = StringArgumentType.getString(context, "newName");
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) player.level());

        if (ftbSyncActive(player)) {
            source.sendFailure(Component.literal("When FTB Teams sync is enabled, rename is managed by FTB Teams party settings."));
            return 0;
        }
        String teamKey = teamManager.resolveTeamKeyByDisplayName(teamName);
        
        if (teamManager.renameTeam(teamKey, newName, player)) {
            source.sendSuccess(() -> Component.literal("Team '" + teamName + "' renamed to '" + newName + "' successfully!"), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to rename team. Team might already exist or you're not the leader."));
            return 0;
        }
    }
    
    private static int transferOwnTeamLeadership(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ServerPlayer newLeader = getSingleTargetPlayer(context, "newLeader");
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) player.level());
        String teamKey = teamManager.getPlayerTeamKey(player);
        
        if (teamKey == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        
        if (ftbSyncActive(player)) {
            if (runFtbTeamsAsPlayer(player, "ftbteams party transfer_ownership " + newLeader.getScoreboardName())) {
                source.sendSuccess(() -> Component.literal("FTB party ownership transferred; shop team will sync automatically."), false);
                return 1;
            }
            source.sendFailure(Component.literal("Failed to transfer ownership via FTB Teams (check permissions/state)."));
            return 0;
        }

        String teamDisplay = displayTeamName(teamManager, teamKey);
        if (teamManager.transferLeadership(teamKey, player, newLeader)) {
            source.sendSuccess(() -> Component.literal("Leadership transferred to " + newLeader.getName().getString() + "!"), false);
            newLeader.sendSystemMessage(Component.literal("You are now the leader of team '" + teamDisplay + "'!"));
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to transfer leadership. You might not be the leader or the new leader is not in your team."));
            return 0;
        }
    }
    
    private static int transferTeamLeadership(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        String teamName = StringArgumentType.getString(context, "teamName");
        ServerPlayer newLeader = getSingleTargetPlayer(context, "newLeader");
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) player.level());

        if (ftbSyncActive(player)) {
            source.sendFailure(Component.literal("When FTB Teams sync is enabled, leadership is managed by FTB Teams party commands."));
            return 0;
        }
        String teamKey = teamManager.resolveTeamKeyByDisplayName(teamName);
        
        if (teamManager.transferLeadership(teamKey, player, newLeader)) {
            source.sendSuccess(() -> Component.literal("Leadership of team '" + teamName + "' transferred to " + newLeader.getName().getString() + "!"), false);
            newLeader.sendSystemMessage(Component.literal("You are now the leader of team '" + teamName + "'!"));
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to transfer leadership."));
            return 0;
        }
    }
    
    private static int addAssistantToOwnTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        List<ServerPlayer> assistants = getTargetPlayers(context, "player");
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) player.level());
        String teamKey = teamManager.getPlayerTeamKey(player);
        
        if (teamKey == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        if (assistants.isEmpty()) {
            source.sendFailure(Component.literal("No player found from selector"));
            return 0;
        }
        int count = 0;
        for (ServerPlayer assistant : assistants) {
            if (ftbSyncActive(player)) {
                if (FtbTeamsBridge.setAssistant(player, assistant.getUUID(), true)) {
                    count++;
                }
                continue;
            }
            if (teamManager.addTeamAssistant(teamKey, player, assistant)) {
                count++;
                source.sendSuccess(() -> Component.literal("Added " + assistant.getName().getString() + " as assistant to your team!"), false);
                assistant.sendSystemMessage(Component.literal("You are now an assistant of team '" + displayTeamName(teamManager, teamKey) + "'!"));
            }
        }
        if (count == 0) {
            source.sendFailure(Component.literal("Failed to add assistant. They might not be in your team or you're not the leader."));
        }
        return count;
    }
    
    private static int addTeamAssistant(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        String teamName = StringArgumentType.getString(context, "teamName");
        List<ServerPlayer> assistants = getTargetPlayers(context, "player");
        if (assistants.isEmpty()) {
            source.sendFailure(Component.literal("No player found from selector"));
            return 0;
        }
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) player.level());
        if (ftbSyncActive(player)) {
            source.sendFailure(Component.literal("When FTB Teams sync is enabled, assistants are managed by FTB Teams party promote/demote."));
            return 0;
        }
        String teamKey = teamManager.resolveTeamKeyByDisplayName(teamName);
        int count = 0;
        for (ServerPlayer assistant : assistants) {
            if (teamManager.addTeamAssistant(teamKey, player, assistant)) {
                count++;
                source.sendSuccess(() -> Component.literal("Added " + assistant.getName().getString() + " as assistant to team '" + teamName + "'!"), false);
                assistant.sendSystemMessage(Component.literal("You are now an assistant of team '" + teamName + "'!"));
            }
        }
        if (count == 0) {
            source.sendFailure(Component.literal("Failed to add assistant to team."));
        }
        return count;
    }
    
    private static int removeAssistantFromOwnTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        List<ServerPlayer> assistants = getTargetPlayers(context, "player");
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) player.level());
        String teamKey = teamManager.getPlayerTeamKey(player);
        
        if (teamKey == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        if (assistants.isEmpty()) {
            source.sendFailure(Component.literal("No player found from selector"));
            return 0;
        }
        int count = 0;
        for (ServerPlayer assistant : assistants) {
            if (ftbSyncActive(player)) {
                if (FtbTeamsBridge.setAssistant(player, assistant.getUUID(), false)) {
                    count++;
                }
                continue;
            }
            if (teamManager.removeTeamAssistant(teamKey, player, assistant)) {
                count++;
                source.sendSuccess(() -> Component.literal("Removed " + assistant.getName().getString() + " as assistant from your team!"), false);
                assistant.sendSystemMessage(Component.literal("You are no longer an assistant of team '" + displayTeamName(teamManager, teamKey) + "'!"));
            }
        }
        if (count == 0) {
            source.sendFailure(Component.literal("Failed to remove assistant. You might not be the leader."));
        }
        return count;
    }
    
    private static int removeTeamAssistant(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        String teamName = StringArgumentType.getString(context, "teamName");
        List<ServerPlayer> assistants = getTargetPlayers(context, "player");
        if (assistants.isEmpty()) {
            source.sendFailure(Component.literal("No player found from selector"));
            return 0;
        }
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) player.level());
        if (ftbSyncActive(player)) {
            source.sendFailure(Component.literal("When FTB Teams sync is enabled, assistants are managed by FTB Teams party promote/demote."));
            return 0;
        }
        String teamKey = teamManager.resolveTeamKeyByDisplayName(teamName);
        int count = 0;
        for (ServerPlayer assistant : assistants) {
            if (teamManager.removeTeamAssistant(teamKey, player, assistant)) {
                count++;
                source.sendSuccess(() -> Component.literal("Removed " + assistant.getName().getString() + " as assistant from team '" + teamName + "'!"), false);
                assistant.sendSystemMessage(Component.literal("You are no longer an assistant of team '" + teamName + "'!"));
            }
        }
        if (count == 0) {
            source.sendFailure(Component.literal("Failed to remove assistant from team."));
        }
        return count;
    }
    
    private static int listOwnTeamAssistants(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) source.getPlayer().level());
        String teamKey = teamManager.getPlayerTeamKey(source.getPlayer());
        
        if (teamKey == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        
        return showTeamAssistants(source, teamKey);
    }
    
    private static int listTeamAssistants(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String teamName = StringArgumentType.getString(context, "teamName");
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) source.getPlayer().level());
        String teamKey = teamManager.resolveTeamKeyByDisplayName(teamName);
        return showTeamAssistants(source, teamKey);
    }
    
    private static int showTeamAssistants(CommandSourceStack source, String teamKey) {
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) source.getPlayer().level());
        List<UUID> assistants = teamManager.getTeamAssistants(teamKey);
        String teamDisplay = displayTeamName(teamManager, teamKey);
        
        if (assistants.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Team '" + teamDisplay + "' has no assistants"), false);
            return 1;
        }
        
        source.sendSuccess(() -> Component.literal("=== Team '" + teamDisplay + "' Assistants ==="), false);
        for (UUID assistantId : assistants) {
            String assistantName = getPlayerName(assistantId, source.getServer());
            source.sendSuccess(() -> Component.literal("- " + assistantName), false);
        }
        
        return 1;
    }
    
    private static int inviteToOwnTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        List<ServerPlayer> targets = getTargetPlayers(context, "player");
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) player.level());
        String teamKey = teamManager.getPlayerTeamKey(player);
        
        if (teamKey == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        if (targets.isEmpty()) {
            source.sendFailure(Component.literal("No player found from selector"));
            return 0;
        }
        int count = 0;
        for (ServerPlayer targetPlayer : targets) {
            if (teamManager.invitePlayerToTeam(teamKey, player, targetPlayer)) {
                count++;
                source.sendSuccess(() -> Component.literal("Invited " + targetPlayer.getName().getString() + " to your team!"), false);
                String teamDisplay = displayTeamName(teamManager, teamKey);
                targetPlayer.sendSystemMessage(Component.literal("You have been invited to join team '" + teamDisplay + "'! Use /iska_utils_team accept " + teamDisplay + " to join."));
            }
        }
        if (count == 0) {
            source.sendFailure(Component.literal("Failed to invite player. They might already be in a team."));
        }
        return count;
    }
    
    private static int inviteToTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        String teamName = StringArgumentType.getString(context, "teamName");
        List<ServerPlayer> targets = getTargetPlayers(context, "player");
        if (targets.isEmpty()) {
            source.sendFailure(Component.literal("No player found from selector"));
            return 0;
        }
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) player.level());
        int count = 0;
        for (ServerPlayer targetPlayer : targets) {
            if (teamManager.invitePlayerToTeam(teamName, player, targetPlayer)) {
                count++;
                source.sendSuccess(() -> Component.literal("Invited " + targetPlayer.getName().getString() + " to team '" + teamName + "'!"), false);
                targetPlayer.sendSystemMessage(Component.literal("You have been invited to join team '" + teamName + "'! Use /iska_utils_team accept " + teamName + " to join."));
            }
        }
        if (count == 0) {
            source.sendFailure(Component.literal("Failed to invite player to team."));
        }
        return count;
    }
    
    private static int acceptInvitation(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        String teamName = StringArgumentType.getString(context, "teamName");
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) player.level());
        
        if (teamManager.acceptTeamInvitation(player, teamName)) {
            source.sendSuccess(() -> Component.literal("Successfully joined team '" + teamName + "'!"), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to join team. You might not have an invitation or already be in a team."));
            return 0;
        }
    }
    
    private static int leaveTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) player.level());
        
        if (teamManager.leaveTeam(player)) {
            source.sendSuccess(() -> Component.literal("Successfully left your team!"), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to leave team. You might be the leader (use delete instead) or not be in a team."));
            return 0;
        }
    }
    
    private static int addToOwnTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        List<ServerPlayer> targets = getTargetPlayers(context, "player");
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) player.level());
        String teamKey = teamManager.getPlayerTeamKey(player);
        
        if (teamKey == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        if (targets.isEmpty()) {
            source.sendFailure(Component.literal("No player found from selector"));
            return 0;
        }
        int count = 0;
        for (ServerPlayer targetPlayer : targets) {
            if (teamManager.addPlayerToTeam(teamKey, targetPlayer)) {
                count++;
                source.sendSuccess(() -> Component.literal("Added " + targetPlayer.getName().getString() + " to your team!"), false);
                targetPlayer.sendSystemMessage(Component.literal("You have been added to team '" + displayTeamName(teamManager, teamKey) + "'!"));
            }
        }
        if (count == 0) {
            source.sendFailure(Component.literal("Failed to add player to team. They might already be in a team."));
        }
        return count;
    }
    
    private static int addPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        String teamName = StringArgumentType.getString(context, "teamName");
        List<ServerPlayer> targets = getTargetPlayers(context, "player");
        if (targets.isEmpty()) {
            source.sendFailure(Component.literal("No player found from selector"));
            return 0;
        }
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) player.level());
        String teamKey = teamManager.resolveTeamKeyByDisplayName(teamName);
        int count = 0;
        for (ServerPlayer targetPlayer : targets) {
            if (teamManager.addPlayerToTeam(teamKey, targetPlayer)) {
                count++;
                source.sendSuccess(() -> Component.literal("Added " + targetPlayer.getName().getString() + " to team '" + teamName + "'!"), false);
                targetPlayer.sendSystemMessage(Component.literal("You have been added to team '" + teamName + "'!"));
            }
        }
        if (count == 0) {
            source.sendFailure(Component.literal("Failed to add player to team. Team might not exist or player is already in a team."));
        }
        return count;
    }
    
    private static int removeFromOwnTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        List<ServerPlayer> targets = getTargetPlayers(context, "player");
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) player.level());
        String teamKey = teamManager.getPlayerTeamKey(player);
        
        if (teamKey == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        if (targets.isEmpty()) {
            source.sendFailure(Component.literal("No player found from selector"));
            return 0;
        }
        int count = 0;
        for (ServerPlayer targetPlayer : targets) {
            if (teamManager.removePlayerFromTeam(teamKey, targetPlayer)) {
                count++;
                source.sendSuccess(() -> Component.literal("Removed " + targetPlayer.getName().getString() + " from your team!"), false);
                targetPlayer.sendSystemMessage(Component.literal("You have been removed from team '" + displayTeamName(teamManager, teamKey) + "'!"));
            }
        }
        if (count == 0) {
            source.sendFailure(Component.literal("Failed to remove player from team. You might not be the leader or the player is not in the team."));
        }
        return count;
    }
    
    private static int removePlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        String teamName = StringArgumentType.getString(context, "teamName");
        List<ServerPlayer> targets = getTargetPlayers(context, "player");
        if (targets.isEmpty()) {
            source.sendFailure(Component.literal("No player found from selector"));
            return 0;
        }
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) player.level());
        String teamKey = teamManager.resolveTeamKeyByDisplayName(teamName);
        int count = 0;
        for (ServerPlayer targetPlayer : targets) {
            if (teamManager.removePlayerFromTeam(teamKey, targetPlayer)) {
                count++;
                source.sendSuccess(() -> Component.literal("Removed " + targetPlayer.getName().getString() + " from team '" + teamName + "'!"), false);
                targetPlayer.sendSystemMessage(Component.literal("You have been removed from team '" + teamName + "'!"));
            }
        }
        if (count == 0) {
            source.sendFailure(Component.literal("Failed to remove player from team. You might not be the leader or the player is not in the team."));
        }
        return count;
    }
    
    private static int ownTeamInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) source.getPlayer().level());
        String teamKey = teamManager.getPlayerTeamKey(source.getPlayer());
        
        if (teamKey == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        
        return showTeamInfo(source, teamKey);
    }
    
    private static int teamInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String teamName = StringArgumentType.getString(context, "teamName");
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) source.getPlayer().level());
        String teamKey = teamManager.resolveTeamKeyByDisplayName(teamName);
        return showTeamInfo(source, teamKey);
    }
    
    private static int showTeamInfo(CommandSourceStack source, String teamKey) {
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) source.getPlayer().level());
        List<UUID> members = teamManager.getTeamMembers(teamKey);
        List<UUID> assistants = teamManager.getTeamAssistants(teamKey);
        UUID leader = teamManager.getTeamLeader(teamKey);
        String teamDisplay = displayTeamName(teamManager, teamKey);
        
        if (leader == null) {
            source.sendFailure(Component.literal("Team '" + teamDisplay + "' does not exist"));
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("=== Team: " + teamDisplay + " ==="), false);
        
        // Show leader
        String leaderName = getPlayerName(leader, source.getServer());
        source.sendSuccess(() -> Component.literal("§6Leader: §f" + leaderName), false);
        
        // Show assistants
        if (!assistants.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§eAssistants (" + assistants.size() + "):"), false);
            for (UUID assistantId : assistants) {
                String assistantName = getPlayerName(assistantId, source.getServer());
                source.sendSuccess(() -> Component.literal("  §e- §f" + assistantName), false);
            }
        }
        
        // Show regular members (excluding leader and assistants)
        List<UUID> regularMembers = new java.util.ArrayList<>();
        for (UUID memberId : members) {
            if (!memberId.equals(leader) && !assistants.contains(memberId)) {
                regularMembers.add(memberId);
            }
        }
        
        if (!regularMembers.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§7Members (" + regularMembers.size() + "):"), false);
            for (UUID memberId : regularMembers) {
                String memberName = getPlayerName(memberId, source.getServer());
                source.sendSuccess(() -> Component.literal("  §7- §f" + memberName), false);
            }
        }
        
        // Show all currency balances with localized names and symbols
        Map<String, ShopCurrency> allCurrencies = ShopLoader.getCurrencies();
        for (String currencyId : allCurrencies.keySet()) {
            ShopCurrency currency = allCurrencies.get(currencyId);
            double balance = teamManager.getTeamCurrencyBalance(teamKey, currencyId);
            String localizedName = Component.translatable(currency.name).getString();
            String formattedName = localizedName + " " + currency.charSymbol;
            source.sendSuccess(() -> Component.literal("Team '" + teamDisplay + "' has " + balance + " " + formattedName), false);
        }
        
        return 1;
    }
    
    private static int listTeams(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) source.getPlayer().level());
        var teams = teamManager.getAllTeams();
        
        if (teams.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No teams exist"), false);
            return 1;
        }
        
        source.sendSuccess(() -> Component.literal("=== All Teams ==="), false);
        for (String teamKey : teams) {
            List<UUID> members = teamManager.getTeamMembers(teamKey);
            String teamDisplay = displayTeamName(teamManager, teamKey);
            source.sendSuccess(() -> Component.literal(teamDisplay + " (" + members.size() + " members)"), false);
        }
        
        return 1;
    }
    
    private static int ownTeamBalance(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) source.getPlayer().level());
        String teamKey = teamManager.getPlayerTeamKey(source.getPlayer());
        
        if (teamKey == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        
        double nullCoinBalance = teamManager.getTeamCurrencyBalance(teamKey, "null_coin");
        ShopCurrency nullCoin = ShopLoader.getCurrencies().get("null_coin");
        if (nullCoin != null) {
            String localizedName = Component.translatable(nullCoin.name).getString();
            String formattedName = localizedName + " " + nullCoin.charSymbol;
            source.sendSuccess(() -> Component.literal("Team '" + displayTeamName(teamManager, teamKey) + "' has " + nullCoinBalance + " " + formattedName), false);
        } else {
            source.sendSuccess(() -> Component.literal("Team '" + displayTeamName(teamManager, teamKey) + "' has " + nullCoinBalance + " null_coin"), false);
        }
        return 1;
    }
    
    private static int getBalance(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String teamName = StringArgumentType.getString(context, "teamName");
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) source.getPlayer().level());
        String teamKey = teamManager.resolveTeamKeyByDisplayName(teamName);
        String teamDisplay = displayTeamName(teamManager, teamKey);
        double balance = teamManager.getTeamCurrencyBalance(teamKey, "null_coin");
        
        ShopCurrency nullCoin = ShopLoader.getCurrencies().get("null_coin");
        if (nullCoin != null) {
            String localizedName = Component.translatable(nullCoin.name).getString();
            String formattedName = localizedName + " " + nullCoin.charSymbol;
            source.sendSuccess(() -> Component.literal("Team '" + teamDisplay + "' has " + balance + " " + formattedName), false);
        } else {
            source.sendSuccess(() -> Component.literal("Team '" + teamDisplay + "' has " + balance + " null_coin"), false);
        }
        return 1;
    }
    
    private static int getCurrencyBalance(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String teamName = StringArgumentType.getString(context, "teamName");
        String currencyId = StringArgumentType.getString(context, "currencyId");
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) source.getPlayer().level());
        String teamKey = teamManager.resolveTeamKeyByDisplayName(teamName);
        String teamDisplay = displayTeamName(teamManager, teamKey);
        double balance = teamManager.getTeamCurrencyBalance(teamKey, currencyId);
        
        ShopCurrency currency = ShopLoader.getCurrencies().get(currencyId);
        String currencyDisplay;
        if (currency != null) {
            String localizedName = Component.translatable(currency.name).getString();
            currencyDisplay = localizedName + " " + currency.charSymbol;
        } else {
            currencyDisplay = currencyId;
        }
        
        source.sendSuccess(() -> Component.literal("Team '" + teamDisplay + "' has " + balance + " " + currencyDisplay), false);
        return 1;
    }
    
    private static int addCurrencyToOwnTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String currencyId = StringArgumentType.getString(context, "currencyId");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) source.getPlayer().level());
        String teamKey = teamManager.getPlayerTeamKey(source.getPlayer());
        
        if (teamKey == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        
        if (teamManager.addTeamCurrency(teamKey, currencyId, amount)) {
            ShopCurrency currency = ShopLoader.getCurrencies().get(currencyId);
            String currencyDisplay;
            if (currency != null) {
                String localizedName = Component.translatable(currency.name).getString();
                currencyDisplay = localizedName + " " + currency.charSymbol;
            } else {
                currencyDisplay = currencyId;
            }
            source.sendSuccess(() -> Component.literal("Added " + amount + " " + currencyDisplay + " to team '" + displayTeamName(teamManager, teamKey) + "'!"), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to add currencies to team."));
            return 0;
        }
    }
    
    private static int addCurrencyToTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String currencyId = StringArgumentType.getString(context, "currencyId");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        String teamName = StringArgumentType.getString(context, "teamName");
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) source.getPlayer().level());
        String teamKey = teamManager.resolveTeamKeyByDisplayName(teamName);
        
        if (teamManager.addTeamCurrency(teamKey, currencyId, amount)) {
            ShopCurrency currency = ShopLoader.getCurrencies().get(currencyId);
            String currencyDisplay;
            if (currency != null) {
                String localizedName = Component.translatable(currency.name).getString();
                currencyDisplay = localizedName + " " + currency.charSymbol;
            } else {
                currencyDisplay = currencyId;
            }
            source.sendSuccess(() -> Component.literal("Added " + amount + " " + currencyDisplay + " to team '" + displayTeamName(teamManager, teamKey) + "'!"), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to add currencies to team. Team might not exist."));
            return 0;
        }
    }
    
    private static int addCurrencyToPlayerTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String currencyId = StringArgumentType.getString(context, "currencyId");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        
        try {
            List<ServerPlayer> targets = getTargetPlayers(context, "player");
            if (targets.isEmpty()) {
                source.sendFailure(Component.literal("No player found from selector"));
                return 0;
            }
            int count = 0;
            for (ServerPlayer targetPlayer : targets) {
                ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) targetPlayer.level());
                String teamKey = teamManager.getPlayerTeamKey(targetPlayer);
                if (teamKey == null) {
                    source.sendFailure(Component.literal("Player " + targetPlayer.getName().getString() + " is not in a team"));
                    continue;
                }
                if (teamManager.addTeamCurrency(teamKey, currencyId, amount)) {
                    count++;
                    ShopCurrency currency = ShopLoader.getCurrencies().get(currencyId);
                    String currencyDisplay = currency != null ? Component.translatable(currency.name).getString() + " " + currency.charSymbol : currencyId;
                    source.sendSuccess(() -> Component.literal("Added " + amount + " " + currencyDisplay + " to " + targetPlayer.getName().getString() + "'s team '" + displayTeamName(teamManager, teamKey) + "'!"), false);
                } else {
                    source.sendFailure(Component.literal("Failed to add currencies to team."));
                }
            }
            return count;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Player not found or error occurred"));
            return 0;
        }
    }
    
    private static int removeCurrencyFromOwnTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String currencyId = StringArgumentType.getString(context, "currencyId");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) source.getPlayer().level());
        String teamKey = teamManager.getPlayerTeamKey(source.getPlayer());
        
        if (teamKey == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        
        if (teamManager.removeTeamCurrency(teamKey, currencyId, amount)) {
            ShopCurrency currency = ShopLoader.getCurrencies().get(currencyId);
            String currencyDisplay;
            if (currency != null) {
                String localizedName = Component.translatable(currency.name).getString();
                currencyDisplay = localizedName + " " + currency.charSymbol;
            } else {
                currencyDisplay = currencyId;
            }
            source.sendSuccess(() -> Component.literal("Removed " + amount + " " + currencyDisplay + " from team '" + displayTeamName(teamManager, teamKey) + "'!"), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to remove currencies from team. Insufficient balance or team doesn't exist."));
            return 0;
        }
    }
    
    private static int removeCurrencyFromTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String currencyId = StringArgumentType.getString(context, "currencyId");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        String teamName = StringArgumentType.getString(context, "teamName");
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) source.getPlayer().level());
        String teamKey = teamManager.resolveTeamKeyByDisplayName(teamName);
        
        if (teamManager.removeTeamCurrency(teamKey, currencyId, amount)) {
            ShopCurrency currency = ShopLoader.getCurrencies().get(currencyId);
            String currencyDisplay;
            if (currency != null) {
                String localizedName = Component.translatable(currency.name).getString();
                currencyDisplay = localizedName + " " + currency.charSymbol;
            } else {
                currencyDisplay = currencyId;
            }
            source.sendSuccess(() -> Component.literal("Removed " + amount + " " + currencyDisplay + " from team '" + displayTeamName(teamManager, teamKey) + "'!"), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to remove currencies from team. Insufficient balance or team doesn't exist."));
            return 0;
        }
    }
    
    private static int removeCurrencyFromPlayerTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String currencyId = StringArgumentType.getString(context, "currencyId");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        
        try {
            List<ServerPlayer> targets = getTargetPlayers(context, "player");
            if (targets.isEmpty()) {
                source.sendFailure(Component.literal("No player found from selector"));
                return 0;
            }
            int count = 0;
            for (ServerPlayer targetPlayer : targets) {
                ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) targetPlayer.level());
                String teamKey = teamManager.getPlayerTeamKey(targetPlayer);
                if (teamKey == null) {
                    source.sendFailure(Component.literal("Player " + targetPlayer.getName().getString() + " is not in a team"));
                    continue;
                }
                if (teamManager.removeTeamCurrency(teamKey, currencyId, amount)) {
                    count++;
                    ShopCurrency currency = ShopLoader.getCurrencies().get(currencyId);
                    String currencyDisplay = currency != null ? Component.translatable(currency.name).getString() + " " + currency.charSymbol : currencyId;
                    source.sendSuccess(() -> Component.literal("Removed " + amount + " " + currencyDisplay + " from " + targetPlayer.getName().getString() + "'s team '" + displayTeamName(teamManager, teamKey) + "'!"), false);
                } else {
                    source.sendFailure(Component.literal("Failed to remove currencies from team. Insufficient balance or team doesn't exist."));
                }
            }
            return count;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Player not found or error occurred"));
            return 0;
        }
    }
    
    private static int setCurrencyForOwnTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String currencyId = StringArgumentType.getString(context, "currencyId");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) source.getPlayer().level());
        String teamKey = teamManager.getPlayerTeamKey(source.getPlayer());
        
        if (teamKey == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        
        return setTeamCurrency(source, teamManager, teamKey, currencyId, amount);
    }
    
    private static int setCurrencyForTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String currencyId = StringArgumentType.getString(context, "currencyId");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        String teamName = StringArgumentType.getString(context, "teamName");
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) source.getPlayer().level());
        String teamKey = teamManager.resolveTeamKeyByDisplayName(teamName);
        return setTeamCurrency(source, teamManager, teamKey, currencyId, amount);
    }
    
    private static int setCurrencyForPlayerTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String currencyId = StringArgumentType.getString(context, "currencyId");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        
        try {
            List<ServerPlayer> targets = getTargetPlayers(context, "player");
            if (targets.isEmpty()) {
                source.sendFailure(Component.literal("No player found from selector"));
                return 0;
            }
            int total = 0;
            for (ServerPlayer targetPlayer : targets) {
                ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) targetPlayer.level());
                String teamKey = teamManager.getPlayerTeamKey(targetPlayer);
                if (teamKey == null) {
                    source.sendFailure(Component.literal("Player " + targetPlayer.getName().getString() + " is not in a team"));
                    continue;
                }
                total += setTeamCurrency(source, teamManager, teamKey, currencyId, amount);
            }
            return total;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Player not found or error occurred"));
            return 0;
        }
    }
    
    private static int setTeamCurrency(CommandSourceStack source, ShopTeamManager teamManager, String teamKey, String currencyId, double targetAmount) {
        double currentBalance = teamManager.getTeamCurrencyBalance(teamKey, currencyId);
        double difference = targetAmount - currentBalance;
        
        boolean success;
        if (difference > 0) {
            // We need to add currencies   
            success = teamManager.addTeamCurrency(teamKey, currencyId, difference);
        } else if (difference < 0) {
            // We need to remove currencies
            success = teamManager.removeTeamCurrency(teamKey, currencyId, Math.abs(difference));
        } else {
            // Balance is already at desired amount
            success = true;
        }
        
        ShopCurrency currency = ShopLoader.getCurrencies().get(currencyId);
        String currencyDisplay;
        if (currency != null) {
            String localizedName = Component.translatable(currency.name).getString();
            currencyDisplay = localizedName + " " + currency.charSymbol;
        } else {
            currencyDisplay = currencyId;
        }
        
        if (success) {
            String teamDisplay = displayTeamName(teamManager, teamKey);
            source.sendSuccess(() -> Component.literal("Set " + currencyDisplay + " balance for team '" + teamDisplay + "' to " + targetAmount + "!"), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to set currencies for team. Team might not exist."));
            return 0;
        }
    }
    
    private static int listInvitations(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) source.getPlayer().level());
        List<String> invitations = teamManager.getPlayerInvitations(source.getPlayer());
        
        if (invitations.isEmpty()) {
            source.sendSuccess(() -> Component.literal("You have no pending team invitations"), false);
            return 1;
        }
        
        source.sendSuccess(() -> Component.literal("=== Your Team Invitations ==="), false);
        for (String invitationKey : invitations) {
            String display = displayTeamName(teamManager, invitationKey);
            source.sendSuccess(() -> Component.literal("- " + display + " (use /iska_utils_team accept " + display + " to join)"), false);
        }
        
        return 1;
    }
    
    private static String getPlayerName(UUID playerId, MinecraftServer server) {
        try {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                return player.getName().getString();
            }
        } catch (Exception e) {
            // Ignore errors
        }
        return playerId.toString();
    }
    
    /**
     * Lists members of own team with their roles
     */
    private static int listOwnTeamMembers(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) source.getPlayer().level());
        String teamKey = teamManager.getPlayerTeamKey(source.getPlayer());
        
        if (teamKey == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        
        return showTeamMembers(source, teamManager, teamKey);
    }
    
    /**
     * Lists members of a specific team with their roles
     */
    private static int listTeamMembers(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String teamName = StringArgumentType.getString(context, "teamName");
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) source.getPlayer().level());
        String teamKey = teamManager.resolveTeamKeyByDisplayName(teamName);
        return showTeamMembers(source, teamManager, teamKey);
    }
    
    /**
     * Shows team members with their roles (leader/assistant/member)
     */
    private static int showTeamMembers(CommandSourceStack source, ShopTeamManager teamManager, String teamKey) {
        UUID leader = teamManager.getTeamLeader(teamKey);
        List<UUID> assistants = teamManager.getTeamAssistants(teamKey);
        List<UUID> members = teamManager.getTeamMembers(teamKey);
        String teamDisplay = displayTeamName(teamManager, teamKey);
        
        if (leader == null) {
            source.sendFailure(Component.literal("Team '" + teamDisplay + "' does not exist"));
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("=== Team: " + teamDisplay + " Members ==="), false);
        
        // Show leader
        String leaderName = getPlayerName(leader, source.getServer());
        source.sendSuccess(() -> Component.literal("§6Leader: §f" + leaderName), false);
        
        // Show assistants
        if (!assistants.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§eAssistants (" + assistants.size() + "):"), false);
            for (UUID assistantId : assistants) {
                String assistantName = getPlayerName(assistantId, source.getServer());
                source.sendSuccess(() -> Component.literal("  §e- §f" + assistantName), false);
            }
        }
        
        // Show regular members (excluding leader and assistants)
        List<UUID> regularMembers = new java.util.ArrayList<>();
        for (UUID memberId : members) {
            if (!memberId.equals(leader) && !assistants.contains(memberId)) {
                regularMembers.add(memberId);
            }
        }
        
        if (!regularMembers.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§7Members (" + regularMembers.size() + "):"), false);
            for (UUID memberId : regularMembers) {
                String memberName = getPlayerName(memberId, source.getServer());
                source.sendSuccess(() -> Component.literal("  §7- §f" + memberName), false);
            }
        }
        
        return 1;
    }
    
    private static int cancelInviteFromOwnTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendFailure(Component.literal("This command can only be executed by a player"));
            return 0;
        }

        List<ServerPlayer> invitees = getTargetPlayers(context, "player");
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) player.level());
        String teamKey = teamManager.getPlayerTeamKey(player);

        if (teamKey == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        if (invitees.isEmpty()) {
            source.sendFailure(Component.literal("No player found from selector"));
            return 0;
        }
        int count = 0;
        for (ServerPlayer invitee : invitees) {
            if (teamManager.cancelTeamInvitation(teamKey, player, invitee)) {
                count++;
                source.sendSuccess(() -> Component.literal("Cancelled invitation for '" + invitee.getName().getString() + "' to join team '" + displayTeamName(teamManager, teamKey) + "'"), false);
            }
        }
        if (count == 0) {
            source.sendFailure(Component.literal("Failed to cancel invitation. You might not be authorized or there might be no invitation."));
        }
        return count;
    }

    private static int cancelInviteFromTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendFailure(Component.literal("This command can only be executed by a player"));
            return 0;
        }

        List<ServerPlayer> invitees = getTargetPlayers(context, "player");
        String teamName = StringArgumentType.getString(context, "teamName");
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) player.level());
        if (invitees.isEmpty()) {
            source.sendFailure(Component.literal("No player found from selector"));
            return 0;
        }
        int count = 0;
        for (ServerPlayer invitee : invitees) {
            if (teamManager.cancelTeamInvitation(teamName, player, invitee)) {
                count++;
                source.sendSuccess(() -> Component.literal("Cancelled invitation for '" + invitee.getName().getString() + "' to join team '" + teamName + "'"), false);
            }
        }
        if (count == 0) {
            source.sendFailure(Component.literal("Failed to cancel invitation. You might not be authorized or there might be no invitation."));
        }
        return count;
    }

    /**
     * Suggests available currency IDs for autocompletion
     */
    private static CompletableFuture<Suggestions> suggestCurrencies(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        // Gets all available currency IDs from ShopLoader
        List<String> currencyIds = ShopLoader.getAllCurrencyIds();
        return SharedSuggestionProvider.suggest(currencyIds, builder);
    }
    
    /**
     * Suggests existing team names for autocompletion
     */
    private static CompletableFuture<Suggestions> suggestTeams(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        CommandSourceStack source = context.getSource();
        if (source.getPlayer() == null) {
            return Suggestions.empty();
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) source.getPlayer().level());
        List<String> teamNames = teamManager.getAllTeamNames();
        return SharedSuggestionProvider.suggest(teamNames, builder);
    }
    
    /**
     * Shows help for all team commands, divided between user and admin commands
     */
    private static int showHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        boolean isAdmin = source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2)));
        return showHelp(context, true, isAdmin);
    }
    
    /**
     * Shows help for team commands, with options to show user and/or admin commands
     */
    private static int showHelp(CommandContext<CommandSourceStack> context, boolean showUser, boolean showAdmin) {
        CommandSourceStack source = context.getSource();
        boolean isAdmin = source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2)));
        
        // Check if user is trying to see admin commands without permission
        if (showAdmin && !isAdmin) {
            source.sendFailure(Component.literal("You don't have permission to view admin commands."));
            return 0;
        }
        
        // User Commands
        if (showUser) {
            source.sendSuccess(() -> Component.literal("§a=== User Commands ==="), false);
        source.sendSuccess(() -> Component.literal("§e/iska_utils_team create <teamName>"), false);
        source.sendSuccess(() -> Component.literal("  §7Create a new team"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§e/iska_utils_team delete"), false);
        source.sendSuccess(() -> Component.literal("  §7Delete your own team (leader only)"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§e/iska_utils_team rename <newName>"), false);
        source.sendSuccess(() -> Component.literal("  §7Rename your own team (leader only)"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§e/iska_utils_team leader <newLeader>"), false);
        source.sendSuccess(() -> Component.literal("  §7Transfer leadership of your team to another player"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§e/iska_utils_team assistant add <player>"), false);
        source.sendSuccess(() -> Component.literal("  §7Add an assistant to your team (leader only)"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§e/iska_utils_team assistant remove <player>"), false);
        source.sendSuccess(() -> Component.literal("  §7Remove an assistant from your team (leader only)"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§e/iska_utils_team assistant list [teamName]"), false);
        source.sendSuccess(() -> Component.literal("  §7List assistants of your team or a specific team"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§e/iska_utils_team invite <player>"), false);
        source.sendSuccess(() -> Component.literal("  §7Invite a player to your team (leader/assistant only)"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§e/iska_utils_team cancelInvite <player>"), false);
        source.sendSuccess(() -> Component.literal("  §7Cancel an invitation to your team (leader/assistant only)"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§e/iska_utils_team accept <teamName>"), false);
        source.sendSuccess(() -> Component.literal("  §7Accept a team invitation"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§e/iska_utils_team leave"), false);
        source.sendSuccess(() -> Component.literal("  §7Leave your current team"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§e/iska_utils_team remove <player>"), false);
        source.sendSuccess(() -> Component.literal("  §7Remove a player from your team (leader only)"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§e/iska_utils_team info [teamName]"), false);
        source.sendSuccess(() -> Component.literal("  §7Show information about your team or a specific team (members with roles and currency balances)"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§e/iska_utils_team list"), false);
        source.sendSuccess(() -> Component.literal("  §7List all teams"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§e/iska_utils_team members [teamName]"), false);
        source.sendSuccess(() -> Component.literal("  §7List members of your team or a specific team with their roles"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§e/iska_utils_team balance [teamName] [currencyId]"), false);
        source.sendSuccess(() -> Component.literal("  §7Show your team's balance or a specific team's balance"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§e/iska_utils_team invitations"), false);
        source.sendSuccess(() -> Component.literal("  §7List your pending team invitations"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§e/iska_utils_team moveCurrency <currencyId> <amount> <toTeam>"), false);
        source.sendSuccess(() -> Component.literal("  §7Move currency from your team to another team (leader/assistant only)"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        }
        
        // Admin Commands (only if user is admin and showAdmin is true)
        if (showAdmin && isAdmin) {
            if (showUser) {
                source.sendSuccess(() -> Component.literal(""), false);
            }
            source.sendSuccess(() -> Component.literal("§a=== Admin Commands ==="), false);
            source.sendSuccess(() -> Component.literal("§c/iska_utils_team delete <teamName>"), false);
            source.sendSuccess(() -> Component.literal("  §7Delete any team"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            
            source.sendSuccess(() -> Component.literal("§c/iska_utils_team rename <newName> <teamName>"), false);
            source.sendSuccess(() -> Component.literal("  §7Rename any team"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            
            source.sendSuccess(() -> Component.literal("§c/iska_utils_team leader <newLeader> <teamName>"), false);
            source.sendSuccess(() -> Component.literal("  §7Transfer leadership of any team"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            
            source.sendSuccess(() -> Component.literal("§c/iska_utils_team assistant add <player> <teamName>"), false);
            source.sendSuccess(() -> Component.literal("  §7Add an assistant to any team"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            
            source.sendSuccess(() -> Component.literal("§c/iska_utils_team assistant remove <player> <teamName>"), false);
            source.sendSuccess(() -> Component.literal("  §7Remove an assistant from any team"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            
            source.sendSuccess(() -> Component.literal("§c/iska_utils_team invite <player> <teamName>"), false);
            source.sendSuccess(() -> Component.literal("  §7Invite a player to any team"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            
            source.sendSuccess(() -> Component.literal("§c/iska_utils_team cancelInvite <player> <teamName>"), false);
            source.sendSuccess(() -> Component.literal("  §7Cancel an invitation to any team"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            
            source.sendSuccess(() -> Component.literal("§c/iska_utils_team add <player> [teamName]"), false);
            source.sendSuccess(() -> Component.literal("  §7Add a player to your team or a specific team"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            
            source.sendSuccess(() -> Component.literal("§c/iska_utils_team remove <player> <teamName>"), false);
            source.sendSuccess(() -> Component.literal("  §7Remove a player from any team"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            
            source.sendSuccess(() -> Component.literal("§c/iska_utils_team addCurrency <currencyId> <amount> [team <teamName> | player <player>]"), false);
            source.sendSuccess(() -> Component.literal("  §7Add currency to your team, a specific team, or a player's team"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            
            source.sendSuccess(() -> Component.literal("§c/iska_utils_team removeCurrency <currencyId> <amount> [team <teamName> | player <player>]"), false);
            source.sendSuccess(() -> Component.literal("  §7Remove currency from your team, a specific team, or a player's team"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            
            source.sendSuccess(() -> Component.literal("§c/iska_utils_team setCurrency <currencyId> <amount> [team <teamName> | player <player>]"), false);
            source.sendSuccess(() -> Component.literal("  §7Set currency balance for your team, a specific team, or a player's team"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            
            source.sendSuccess(() -> Component.literal("§c/iska_utils_team moveCurrency <currencyId> <amount> <toTeam> [fromTeam]"), false);
            source.sendSuccess(() -> Component.literal("  §7Move currency from one team to another (admin can specify fromTeam)"), false);
            source.sendSuccess(() -> Component.literal(""), false);
        }
        
        return 1;
    }
    
    /**
     * Moves currency from own team to another team (user command - requires leader/assistant)
     */
    private static int moveCurrencyFromOwnTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        String currencyId = StringArgumentType.getString(context, "currencyId");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        String toTeam = StringArgumentType.getString(context, "toTeam");
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) player.level());
        String fromTeam = teamManager.getPlayerTeamKey(player);
        
        if (fromTeam == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        
        // Check if player is leader or assistant
        UUID leader = teamManager.getTeamLeader(fromTeam);
        List<UUID> assistants = teamManager.getTeamAssistants(fromTeam);
        UUID playerUuid = player.getUUID();
        
        if (!playerUuid.equals(leader) && !assistants.contains(playerUuid)) {
            source.sendFailure(Component.literal("You must be the leader or an assistant to move currency from your team"));
            return 0;
        }
        
        // Use the common move logic
        return moveCurrencyLogic(source, teamManager, fromTeam, toTeam, currencyId, amount);
    }
    
    /**
     * Moves currency from one team to another (admin command)
     */
    private static int moveCurrencyBetweenTeams(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        String currencyId = StringArgumentType.getString(context, "currencyId");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        String toTeam = StringArgumentType.getString(context, "toTeam");
        String fromTeam = StringArgumentType.getString(context, "fromTeam");
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) source.getPlayer().level());
        
        // Use the common move logic
        String fromKey = teamManager.resolveTeamKeyByDisplayName(fromTeam);
        String toKey = teamManager.resolveTeamKeyByDisplayName(toTeam);
        return moveCurrencyLogic(source, teamManager, fromKey, toKey, currencyId, amount);
    }
    
    /**
     * Common logic for moving currency between teams
     */
    private static int moveCurrencyLogic(CommandSourceStack source, ShopTeamManager teamManager, String fromTeam, String toTeam, String currencyId, double amount) {
        String fromDisplay = displayTeamName(teamManager, fromTeam);
        String toDisplay = displayTeamName(teamManager, toTeam);
        // Check if both teams exist
        if (teamManager.getTeamLeader(fromTeam) == null) {
            source.sendFailure(Component.literal("Source team '" + fromDisplay + "' does not exist"));
            return 0;
        }
        
        if (teamManager.getTeamLeader(toTeam) == null) {
            source.sendFailure(Component.literal("Destination team '" + toDisplay + "' does not exist"));
            return 0;
        }
        
        // Check if source team has enough currency
        double currentBalance = teamManager.getTeamCurrencyBalance(fromTeam, currencyId);
        if (currentBalance < amount) {
            source.sendFailure(Component.literal("Source team '" + fromDisplay + "' does not have enough currency. Current balance: " + currentBalance));
            return 0;
        }
        
        // Remove currency from source team
        if (!teamManager.removeTeamCurrency(fromTeam, currencyId, amount)) {
            source.sendFailure(Component.literal("Failed to remove currency from source team"));
            return 0;
        }
        
        // Add currency to destination team
        if (!teamManager.addTeamCurrency(toTeam, currencyId, amount)) {
            // If adding fails, try to restore the currency to source team
            teamManager.addTeamCurrency(fromTeam, currencyId, amount);
            source.sendFailure(Component.literal("Failed to add currency to destination team. Transaction rolled back."));
            return 0;
        }
        
        // Success - show formatted message
        ShopCurrency currency = ShopLoader.getCurrencies().get(currencyId);
        String currencyDisplay;
        if (currency != null) {
            String localizedName = Component.translatable(currency.name).getString();
            currencyDisplay = localizedName + " " + currency.charSymbol;
        } else {
            currencyDisplay = currencyId;
        }
        
        source.sendSuccess(() -> Component.literal("Moved " + amount + " " + currencyDisplay + " from team '" + fromDisplay + "' to team '" + toDisplay + "'!"), false);
        return 1;
    }
} 