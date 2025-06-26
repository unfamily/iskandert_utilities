package net.unfamily.iskautils.shop;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.CompletableFuture;

import java.util.List;
import java.util.UUID;
import java.util.Map;

/**
 * Command for managing shop teams
 */
public class ShopTeamCommand {
    
    /**
     * Registers the team command
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("iska_utils_team")
            .requires(source -> source.hasPermission(0)) // All players can use
            .then(Commands.literal("create")
                .then(Commands.argument("teamName", StringArgumentType.word())
                    .executes(ShopTeamCommand::createTeam)))
            .then(Commands.literal("delete")
                .executes(ShopTeamCommand::deleteOwnTeam)
                .then(Commands.argument("teamName", StringArgumentType.word())
                    .requires(source -> source.hasPermission(2)) // Admin only for other teams
                    .executes(ShopTeamCommand::deleteTeam)))
            .then(Commands.literal("rename")
                .then(Commands.argument("newName", StringArgumentType.word())
                    .executes(ShopTeamCommand::renameOwnTeam)
                    .then(Commands.argument("teamName", StringArgumentType.word())
                        .requires(source -> source.hasPermission(2)) // Admin only for other teams
                        .executes(ShopTeamCommand::renameTeam))))
            .then(Commands.literal("transfer")
                .then(Commands.argument("newLeader", EntityArgument.player())
                    .executes(ShopTeamCommand::transferOwnTeamLeadership)
                    .then(Commands.argument("teamName", StringArgumentType.word())
                        .requires(source -> source.hasPermission(2)) // Admin only for other teams
                        .executes(ShopTeamCommand::transferTeamLeadership))))
            .then(Commands.literal("assistant")
                .then(Commands.literal("add")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ShopTeamCommand::addAssistantToOwnTeam)
                        .then(Commands.argument("teamName", StringArgumentType.word())
                            .requires(source -> source.hasPermission(2)) // Admin only for other teams
                            .executes(ShopTeamCommand::addTeamAssistant))))
                .then(Commands.literal("remove")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ShopTeamCommand::removeAssistantFromOwnTeam)
                        .then(Commands.argument("teamName", StringArgumentType.word())
                            .requires(source -> source.hasPermission(2)) // Admin only for other teams
                            .executes(ShopTeamCommand::removeTeamAssistant))))
                .then(Commands.literal("list")
                    .executes(ShopTeamCommand::listOwnTeamAssistants)
                    .then(Commands.argument("teamName", StringArgumentType.word())
                        .executes(ShopTeamCommand::listTeamAssistants))))
            .then(Commands.literal("invite")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ShopTeamCommand::inviteToOwnTeam)
                    .then(Commands.argument("teamName", StringArgumentType.word())
                        .requires(source -> source.hasPermission(2)) // Admin only for other teams
                        .executes(ShopTeamCommand::inviteToTeam))))
            .then(Commands.literal("accept")
                .then(Commands.argument("teamName", StringArgumentType.word())
                    .executes(ShopTeamCommand::acceptInvitation)))
            .then(Commands.literal("leave")
                .executes(ShopTeamCommand::leaveTeam))
            .then(Commands.literal("add")
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(source -> source.hasPermission(2)) // Admin only - add is special
                    .executes(ShopTeamCommand::addToOwnTeam)
                    .then(Commands.argument("teamName", StringArgumentType.word())
                        .requires(source -> source.hasPermission(2)) // Admin only for other teams
                        .executes(ShopTeamCommand::addPlayer))))
            .then(Commands.literal("remove")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ShopTeamCommand::removeFromOwnTeam)
                    .then(Commands.argument("teamName", StringArgumentType.word())
                        .requires(source -> source.hasPermission(2)) // Admin only for other teams
                        .executes(ShopTeamCommand::removePlayer))))
            .then(Commands.literal("info")
                .executes(ShopTeamCommand::ownTeamInfo)
                .then(Commands.argument("teamName", StringArgumentType.word())
                    .executes(ShopTeamCommand::teamInfo)))
            .then(Commands.literal("list")
                .executes(ShopTeamCommand::listTeams))
            .then(Commands.literal("balance")
                .executes(ShopTeamCommand::ownTeamBalance)
                .then(Commands.argument("teamName", StringArgumentType.word())
                    .executes(ShopTeamCommand::getBalance)
                    .then(Commands.argument("currencyId", StringArgumentType.word())
                        .executes(ShopTeamCommand::getValuteBalance))))
            .then(Commands.literal("addValute")
                .requires(source -> source.hasPermission(2)) // Admin only
                .then(Commands.argument("currencyId", StringArgumentType.word())
                    .suggests(ShopTeamCommand::suggestValutes)
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                        .executes(ShopTeamCommand::addValuteToOwnTeam)
                        .then(Commands.literal("team")
                            .then(Commands.argument("teamName", StringArgumentType.word())
                                .suggests(ShopTeamCommand::suggestTeams)
                                .executes(ShopTeamCommand::addValuteToTeam)))
                        .then(Commands.literal("player")
                            .then(Commands.argument("player", EntityArgument.player())
                                .executes(ShopTeamCommand::addValuteToPlayerTeam))))))
            .then(Commands.literal("removeValute")
                .requires(source -> source.hasPermission(2)) // Admin only
                .then(Commands.argument("currencyId", StringArgumentType.word())
                    .suggests(ShopTeamCommand::suggestValutes)
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                        .executes(ShopTeamCommand::removeValuteFromOwnTeam)
                        .then(Commands.literal("team")
                            .then(Commands.argument("teamName", StringArgumentType.word())
                                .suggests(ShopTeamCommand::suggestTeams)
                                .executes(ShopTeamCommand::removeValuteFromTeam)))
                        .then(Commands.literal("player")
                            .then(Commands.argument("player", EntityArgument.player())
                                .executes(ShopTeamCommand::removeValuteFromPlayerTeam))))))
            .then(Commands.literal("setCurrency")
                .requires(source -> source.hasPermission(2)) // Admin only
                .then(Commands.argument("currencyId", StringArgumentType.word())
                    .suggests(ShopTeamCommand::suggestValutes)
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                        .executes(ShopTeamCommand::setValuteForOwnTeam)
                        .then(Commands.literal("team")
                            .then(Commands.argument("teamName", StringArgumentType.word())
                                .suggests(ShopTeamCommand::suggestTeams)
                                .executes(ShopTeamCommand::setValuteForTeam)))
                        .then(Commands.literal("player")
                            .then(Commands.argument("player", EntityArgument.player())
                                .executes(ShopTeamCommand::setValuteForPlayerTeam))))))
            .then(Commands.literal("invitations")
                .executes(ShopTeamCommand::listInvitations)));
    }
    
    private static int createTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        String teamName = StringArgumentType.getString(context, "teamName");
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        
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
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        String teamName = teamManager.getPlayerTeam(player);
        
        if (teamName == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        
        if (teamManager.deleteTeam(teamName, player)) {
            source.sendSuccess(() -> Component.literal("Team '" + teamName + "' deleted successfully!"), false);
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
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        
        if (teamManager.deleteTeam(teamName, player)) {
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
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        String teamName = teamManager.getPlayerTeam(player);
        
        if (teamName == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        
        if (teamManager.renameTeam(teamName, newName, player)) {
            source.sendSuccess(() -> Component.literal("Team '" + teamName + "' renamed to '" + newName + "' successfully!"), false);
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
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        
        if (teamManager.renameTeam(teamName, newName, player)) {
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
        
        ServerPlayer newLeader = EntityArgument.getPlayer(context, "newLeader");
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        String teamName = teamManager.getPlayerTeam(player);
        
        if (teamName == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        
        if (teamManager.transferLeadership(teamName, player, newLeader)) {
            source.sendSuccess(() -> Component.literal("Leadership transferred to " + newLeader.getName().getString() + "!"), false);
            newLeader.sendSystemMessage(Component.literal("You are now the leader of team '" + teamName + "'!"));
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
        ServerPlayer newLeader = EntityArgument.getPlayer(context, "newLeader");
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        
        if (teamManager.transferLeadership(teamName, player, newLeader)) {
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
        
        ServerPlayer assistant = EntityArgument.getPlayer(context, "player");
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        String teamName = teamManager.getPlayerTeam(player);
        
        if (teamName == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        
        if (teamManager.addTeamAssistant(teamName, player, assistant)) {
            source.sendSuccess(() -> Component.literal("Added " + assistant.getName().getString() + " as assistant to your team!"), false);
            assistant.sendSystemMessage(Component.literal("You are now an assistant of team '" + teamName + "'!"));
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to add assistant. They might not be in your team or you're not the leader."));
            return 0;
        }
    }
    
    private static int addTeamAssistant(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        String teamName = StringArgumentType.getString(context, "teamName");
        ServerPlayer assistant = EntityArgument.getPlayer(context, "player");
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        
        if (teamManager.addTeamAssistant(teamName, player, assistant)) {
            source.sendSuccess(() -> Component.literal("Added " + assistant.getName().getString() + " as assistant to team '" + teamName + "'!"), false);
            assistant.sendSystemMessage(Component.literal("You are now an assistant of team '" + teamName + "'!"));
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to add assistant to team."));
            return 0;
        }
    }
    
    private static int removeAssistantFromOwnTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ServerPlayer assistant = EntityArgument.getPlayer(context, "player");
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        String teamName = teamManager.getPlayerTeam(player);
        
        if (teamName == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        
        if (teamManager.removeTeamAssistant(teamName, player, assistant)) {
            source.sendSuccess(() -> Component.literal("Removed " + assistant.getName().getString() + " as assistant from your team!"), false);
            assistant.sendSystemMessage(Component.literal("You are no longer an assistant of team '" + teamName + "'!"));
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to remove assistant. You might not be the leader."));
            return 0;
        }
    }
    
    private static int removeTeamAssistant(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        String teamName = StringArgumentType.getString(context, "teamName");
        ServerPlayer assistant = EntityArgument.getPlayer(context, "player");
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        
        if (teamManager.removeTeamAssistant(teamName, player, assistant)) {
            source.sendSuccess(() -> Component.literal("Removed " + assistant.getName().getString() + " as assistant from team '" + teamName + "'!"), false);
            assistant.sendSystemMessage(Component.literal("You are no longer an assistant of team '" + teamName + "'!"));
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to remove assistant from team."));
            return 0;
        }
    }
    
    private static int listOwnTeamAssistants(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance(source.getPlayer().serverLevel());
        String teamName = teamManager.getPlayerTeam(source.getPlayer());
        
        if (teamName == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        
        return showTeamAssistants(source, teamName);
    }
    
    private static int listTeamAssistants(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String teamName = StringArgumentType.getString(context, "teamName");
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        return showTeamAssistants(source, teamName);
    }
    
    private static int showTeamAssistants(CommandSourceStack source, String teamName) {
        ShopTeamManager teamManager = ShopTeamManager.getInstance(source.getPlayer().serverLevel());
        List<UUID> assistants = teamManager.getTeamAssistants(teamName);
        
        if (assistants.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Team '" + teamName + "' has no assistants"), false);
            return 1;
        }
        
        source.sendSuccess(() -> Component.literal("=== Team '" + teamName + "' Assistants ==="), false);
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
        
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        String teamName = teamManager.getPlayerTeam(player);
        
        if (teamName == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        
        if (teamManager.invitePlayerToTeam(teamName, player, targetPlayer)) {
            source.sendSuccess(() -> Component.literal("Invited " + targetPlayer.getName().getString() + " to your team!"), false);
            targetPlayer.sendSystemMessage(Component.literal("You have been invited to join team '" + teamName + "'! Use /iska_utils_team accept " + teamName + " to join."));
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to invite player. They might already be in a team."));
            return 0;
        }
    }
    
    private static int inviteToTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        String teamName = StringArgumentType.getString(context, "teamName");
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        
        if (teamManager.invitePlayerToTeam(teamName, player, targetPlayer)) {
            source.sendSuccess(() -> Component.literal("Invited " + targetPlayer.getName().getString() + " to team '" + teamName + "'!"), false);
            targetPlayer.sendSystemMessage(Component.literal("You have been invited to join team '" + teamName + "'! Use /iska_utils_team accept " + teamName + " to join."));
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to invite player to team."));
            return 0;
        }
    }
    
    private static int acceptInvitation(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        String teamName = StringArgumentType.getString(context, "teamName");
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        
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
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        
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
        
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        String teamName = teamManager.getPlayerTeam(player);
        
        if (teamName == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        
        if (teamManager.addPlayerToTeam(teamName, targetPlayer)) {
            source.sendSuccess(() -> Component.literal("Added " + targetPlayer.getName().getString() + " to your team!"), false);
            targetPlayer.sendSystemMessage(Component.literal("You have been added to team '" + teamName + "'!"));
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to add player to team. They might already be in a team."));
            return 0;
        }
    }
    
    private static int addPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        String teamName = StringArgumentType.getString(context, "teamName");
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        
        if (teamManager.addPlayerToTeam(teamName, targetPlayer)) {
            source.sendSuccess(() -> Component.literal("Added " + targetPlayer.getName().getString() + " to team '" + teamName + "'!"), false);
            targetPlayer.sendSystemMessage(Component.literal("You have been added to team '" + teamName + "'!"));
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to add player to team. Team might not exist or player is already in a team."));
            return 0;
        }
    }
    
    private static int removeFromOwnTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        String teamName = teamManager.getPlayerTeam(player);
        
        if (teamName == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        
        if (teamManager.removePlayerFromTeam(teamName, targetPlayer)) {
            source.sendSuccess(() -> Component.literal("Removed " + targetPlayer.getName().getString() + " from your team!"), false);
            targetPlayer.sendSystemMessage(Component.literal("You have been removed from team '" + teamName + "'!"));
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to remove player from team. You might not be the leader or the player is not in the team."));
            return 0;
        }
    }
    
    private static int removePlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        String teamName = StringArgumentType.getString(context, "teamName");
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        ShopTeamManager teamManager = ShopTeamManager.getInstance(player.serverLevel());
        
        if (teamManager.removePlayerFromTeam(teamName, targetPlayer)) {
            source.sendSuccess(() -> Component.literal("Removed " + targetPlayer.getName().getString() + " from team '" + teamName + "'!"), false);
            targetPlayer.sendSystemMessage(Component.literal("You have been removed from team '" + teamName + "'!"));
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to remove player from team. You might not be the leader or the player is not in the team."));
            return 0;
        }
    }
    
    private static int ownTeamInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance(source.getPlayer().serverLevel());
        String teamName = teamManager.getPlayerTeam(source.getPlayer());
        
        if (teamName == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        
        return showTeamInfo(source, teamName);
    }
    
    private static int teamInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String teamName = StringArgumentType.getString(context, "teamName");
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        return showTeamInfo(source, teamName);
    }
    
    private static int showTeamInfo(CommandSourceStack source, String teamName) {
        ShopTeamManager teamManager = ShopTeamManager.getInstance(source.getPlayer().serverLevel());
        List<UUID> members = teamManager.getTeamMembers(teamName);
        List<UUID> assistants = teamManager.getTeamAssistants(teamName);
        UUID leader = teamManager.getTeamLeader(teamName);
        
        if (leader == null) {
            source.sendFailure(Component.literal("Team '" + teamName + "' does not exist"));
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("=== Team: " + teamName + " ==="), false);
        source.sendSuccess(() -> Component.literal("Leader: " + getPlayerName(leader, source.getServer())), false);
        
        if (!assistants.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Assistants (" + assistants.size() + "):"), false);
            for (UUID assistantId : assistants) {
                String assistantName = getPlayerName(assistantId, source.getServer());
                source.sendSuccess(() -> Component.literal("  - " + assistantName), false);
            }
        }
        
        source.sendSuccess(() -> Component.literal("Members (" + members.size() + "):"), false);
        for (UUID memberId : members) {
            String memberName = getPlayerName(memberId, source.getServer());
            source.sendSuccess(() -> Component.literal("  - " + memberName), false);
        }
        
        // Show all currency balances with localized names and symbols
        Map<String, ShopCurrency> allCurrencies = ShopLoader.getCurrencies();
        for (String currencyId : allCurrencies.keySet()) {
            ShopCurrency currency = allCurrencies.get(currencyId);
            double balance = teamManager.getTeamValuteBalance(teamName, currencyId);
            String localizedName = Component.translatable(currency.name).getString();
            String formattedName = localizedName + " " + currency.charSymbol;
            source.sendSuccess(() -> Component.literal("Team '" + teamName + "' has " + balance + " " + formattedName), false);
        }
        
        return 1;
    }
    
    private static int listTeams(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance(source.getPlayer().serverLevel());
        var teams = teamManager.getAllTeams();
        
        if (teams.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No teams exist"), false);
            return 1;
        }
        
        source.sendSuccess(() -> Component.literal("=== All Teams ==="), false);
        for (String teamName : teams) {
            List<UUID> members = teamManager.getTeamMembers(teamName);
            source.sendSuccess(() -> Component.literal(teamName + " (" + members.size() + " members)"), false);
        }
        
        return 1;
    }
    
    private static int ownTeamBalance(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance(source.getPlayer().serverLevel());
        String teamName = teamManager.getPlayerTeam(source.getPlayer());
        
        if (teamName == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        
        double nullCoinBalance = teamManager.getTeamValuteBalance(teamName, "null_coin");
        ShopCurrency nullCoin = ShopLoader.getValutes().get("null_coin");
        if (nullCoin != null) {
            String localizedName = Component.translatable(nullCoin.name).getString();
            String formattedName = localizedName + " " + nullCoin.charSymbol;
            source.sendSuccess(() -> Component.literal("Your team has " + nullCoinBalance + " " + formattedName), false);
        } else {
            source.sendSuccess(() -> Component.literal("Your team has " + nullCoinBalance + " null_coin"), false);
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
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance(source.getPlayer().serverLevel());
        double balance = teamManager.getTeamValuteBalance(teamName, "null_coin");
        
        ShopCurrency nullCoin = ShopLoader.getValutes().get("null_coin");
        if (nullCoin != null) {
            String localizedName = Component.translatable(nullCoin.name).getString();
            String formattedName = localizedName + " " + nullCoin.charSymbol;
            source.sendSuccess(() -> Component.literal("Team '" + teamName + "' has " + balance + " " + formattedName), false);
        } else {
            source.sendSuccess(() -> Component.literal("Team '" + teamName + "' has " + balance + " null_coin"), false);
        }
        return 1;
    }
    
    private static int getValuteBalance(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String teamName = StringArgumentType.getString(context, "teamName");
        String currencyId = StringArgumentType.getString(context, "currencyId");
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance(source.getPlayer().serverLevel());
        double balance = teamManager.getTeamValuteBalance(teamName, currencyId);
        
        ShopCurrency currency = ShopLoader.getValutes().get(currencyId);
        String currencyDisplay;
        if (currency != null) {
            String localizedName = Component.translatable(currency.name).getString();
            currencyDisplay = localizedName + " " + currency.charSymbol;
        } else {
            currencyDisplay = currencyId;
        }
        
        source.sendSuccess(() -> Component.literal("Team '" + teamName + "' has " + balance + " " + currencyDisplay), false);
        return 1;
    }
    
    private static int addValuteToOwnTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String currencyId = StringArgumentType.getString(context, "currencyId");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance(source.getPlayer().serverLevel());
        String teamName = teamManager.getPlayerTeam(source.getPlayer());
        
        if (teamName == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        
        if (teamManager.addTeamValutes(teamName, currencyId, amount)) {
            ShopCurrency currency = ShopLoader.getValutes().get(currencyId);
            String currencyDisplay;
            if (currency != null) {
                String localizedName = Component.translatable(currency.name).getString();
                currencyDisplay = localizedName + " " + currency.charSymbol;
            } else {
                currencyDisplay = currencyId;
            }
            source.sendSuccess(() -> Component.literal("Added " + amount + " " + currencyDisplay + " to your team '" + teamName + "'!"), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to add currencys to team."));
            return 0;
        }
    }
    
    private static int addValuteToTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String currencyId = StringArgumentType.getString(context, "currencyId");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        String teamName = StringArgumentType.getString(context, "teamName");
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance(source.getPlayer().serverLevel());
        
        if (teamManager.addTeamValutes(teamName, currencyId, amount)) {
            ShopCurrency currency = ShopLoader.getValutes().get(currencyId);
            String currencyDisplay;
            if (currency != null) {
                String localizedName = Component.translatable(currency.name).getString();
                currencyDisplay = localizedName + " " + currency.charSymbol;
            } else {
                currencyDisplay = currencyId;
            }
            source.sendSuccess(() -> Component.literal("Added " + amount + " " + currencyDisplay + " to team '" + teamName + "'!"), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to add currencys to team. Team might not exist."));
            return 0;
        }
    }
    
    private static int addValuteToPlayerTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String currencyId = StringArgumentType.getString(context, "currencyId");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            ShopTeamManager teamManager = ShopTeamManager.getInstance(targetPlayer.serverLevel());
            String teamName = teamManager.getPlayerTeam(targetPlayer);
            
            if (teamName == null) {
                source.sendFailure(Component.literal("Player " + targetPlayer.getName().getString() + " is not in a team"));
                return 0;
            }
            
            if (teamManager.addTeamValutes(teamName, currencyId, amount)) {
                ShopCurrency currency = ShopLoader.getValutes().get(currencyId);
                String currencyDisplay;
                if (currency != null) {
                    String localizedName = Component.translatable(currency.name).getString();
                    currencyDisplay = localizedName + " " + currency.charSymbol;
                } else {
                    currencyDisplay = currencyId;
                }
                source.sendSuccess(() -> Component.literal("Added " + amount + " " + currencyDisplay + " to " + targetPlayer.getName().getString() + "'s team '" + teamName + "'!"), false);
                return 1;
            } else {
                source.sendFailure(Component.literal("Failed to add currencys to team."));
                return 0;
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("Player not found or error occurred"));
            return 0;
        }
    }
    
    private static int removeValuteFromOwnTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String currencyId = StringArgumentType.getString(context, "currencyId");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance(source.getPlayer().serverLevel());
        String teamName = teamManager.getPlayerTeam(source.getPlayer());
        
        if (teamName == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        
        if (teamManager.removeTeamValutes(teamName, currencyId, amount)) {
            ShopCurrency currency = ShopLoader.getValutes().get(currencyId);
            String currencyDisplay;
            if (currency != null) {
                String localizedName = Component.translatable(currency.name).getString();
                currencyDisplay = localizedName + " " + currency.charSymbol;
            } else {
                currencyDisplay = currencyId;
            }
            source.sendSuccess(() -> Component.literal("Removed " + amount + " " + currencyDisplay + " from your team '" + teamName + "'!"), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to remove currencys from team. Insufficient balance or team doesn't exist."));
            return 0;
        }
    }
    
    private static int removeValuteFromTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String currencyId = StringArgumentType.getString(context, "currencyId");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        String teamName = StringArgumentType.getString(context, "teamName");
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance(source.getPlayer().serverLevel());
        
        if (teamManager.removeTeamValutes(teamName, currencyId, amount)) {
            ShopCurrency currency = ShopLoader.getValutes().get(currencyId);
            String currencyDisplay;
            if (currency != null) {
                String localizedName = Component.translatable(currency.name).getString();
                currencyDisplay = localizedName + " " + currency.charSymbol;
            } else {
                currencyDisplay = currencyId;
            }
            source.sendSuccess(() -> Component.literal("Removed " + amount + " " + currencyDisplay + " from team '" + teamName + "'!"), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to remove currencys from team. Insufficient balance or team doesn't exist."));
            return 0;
        }
    }
    
    private static int removeValuteFromPlayerTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String currencyId = StringArgumentType.getString(context, "currencyId");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            ShopTeamManager teamManager = ShopTeamManager.getInstance(targetPlayer.serverLevel());
            String teamName = teamManager.getPlayerTeam(targetPlayer);
            
            if (teamName == null) {
                source.sendFailure(Component.literal("Player " + targetPlayer.getName().getString() + " is not in a team"));
                return 0;
            }
            
            if (teamManager.removeTeamValutes(teamName, currencyId, amount)) {
                ShopCurrency currency = ShopLoader.getValutes().get(currencyId);
                String currencyDisplay;
                if (currency != null) {
                    String localizedName = Component.translatable(currency.name).getString();
                    currencyDisplay = localizedName + " " + currency.charSymbol;
                } else {
                    currencyDisplay = currencyId;
                }
                source.sendSuccess(() -> Component.literal("Removed " + amount + " " + currencyDisplay + " from " + targetPlayer.getName().getString() + "'s team '" + teamName + "'!"), false);
                return 1;
            } else {
                source.sendFailure(Component.literal("Failed to remove currencys from team. Insufficient balance or team doesn't exist."));
                return 0;
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("Player not found or error occurred"));
            return 0;
        }
    }
    
    private static int setValuteForOwnTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String currencyId = StringArgumentType.getString(context, "currencyId");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance(source.getPlayer().serverLevel());
        String teamName = teamManager.getPlayerTeam(source.getPlayer());
        
        if (teamName == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        
        return setTeamValute(source, teamManager, teamName, currencyId, amount);
    }
    
    private static int setValuteForTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String currencyId = StringArgumentType.getString(context, "currencyId");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        String teamName = StringArgumentType.getString(context, "teamName");
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance(source.getPlayer().serverLevel());
        return setTeamValute(source, teamManager, teamName, currencyId, amount);
    }
    
    private static int setValuteForPlayerTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String currencyId = StringArgumentType.getString(context, "currencyId");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            ShopTeamManager teamManager = ShopTeamManager.getInstance(targetPlayer.serverLevel());
            String teamName = teamManager.getPlayerTeam(targetPlayer);
            
            if (teamName == null) {
                source.sendFailure(Component.literal("Player " + targetPlayer.getName().getString() + " is not in a team"));
                return 0;
            }
            
            return setTeamValute(source, teamManager, teamName, currencyId, amount);
        } catch (Exception e) {
            source.sendFailure(Component.literal("Player not found or error occurred"));
            return 0;
        }
    }
    
    private static int setTeamValute(CommandSourceStack source, ShopTeamManager teamManager, String teamName, String currencyId, double targetAmount) {
        double currentBalance = teamManager.getTeamValuteBalance(teamName, currencyId);
        double difference = targetAmount - currentBalance;
        
        boolean success;
        if (difference > 0) {
            // We need to add currencys   
            success = teamManager.addTeamValutes(teamName, currencyId, difference);
        } else if (difference < 0) {
            // We need to remove currencys
            success = teamManager.removeTeamValutes(teamName, currencyId, Math.abs(difference));
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
            source.sendSuccess(() -> Component.literal("Set " + currencyDisplay + " balance for team '" + teamName + "' to " + targetAmount + "!"), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to set currencys for team. Team might not exist."));
            return 0;
        }
    }
    
    private static int listInvitations(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance(source.getPlayer().serverLevel());
        List<String> invitations = teamManager.getPlayerInvitations(source.getPlayer());
        
        if (invitations.isEmpty()) {
            source.sendSuccess(() -> Component.literal("You have no pending team invitations"), false);
            return 1;
        }
        
        source.sendSuccess(() -> Component.literal("=== Your Team Invitations ==="), false);
        for (String teamName : invitations) {
            source.sendSuccess(() -> Component.literal("- " + teamName + " (use /iska_utils_team accept " + teamName + " to join)"), false);
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
     * Suggests available currency IDs for autocompletion
     */
    private static CompletableFuture<Suggestions> suggestValutes(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        // Gets all available currency IDs from ShopLoader
        List<String> currencyIds = ShopLoader.getAllValuteIds();
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
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance(source.getPlayer().serverLevel());
        List<String> teamNames = teamManager.getAllTeamNames();
        return SharedSuggestionProvider.suggest(teamNames, builder);
    }
} 