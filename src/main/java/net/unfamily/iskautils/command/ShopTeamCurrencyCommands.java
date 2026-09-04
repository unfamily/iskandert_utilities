package net.unfamily.iskautils.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.unfamily.iskalib.team.ShopTeamManager;
import net.unfamily.iskautils.shop.ShopCurrency;
import net.unfamily.iskautils.shop.ShopLoader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * {@code /iska_utils_shop team …} — admin currency management for shop teams.
 */
public final class ShopTeamCurrencyCommands {

    private ShopTeamCurrencyCommands() {}

    public static ArgumentBuilder<CommandSourceStack, ?> teamLiteral() {
        return Commands.literal("team")
                .then(Commands.literal("list")
                        .executes(ShopTeamCurrencyCommands::listCurrencies))
                .then(Commands.literal("add")
                        .then(Commands.argument("currencyId", StringArgumentType.word())
                                .suggests(SUGGEST_CURRENCIES)
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                                        .executes(ShopTeamCurrencyCommands::addToOwnTeam)
                                        .then(Commands.literal("team")
                                                .then(Commands.argument("teamName", StringArgumentType.word())
                                                        .suggests(SUGGEST_TEAMS)
                                                        .executes(ShopTeamCurrencyCommands::addToTeam)))
                                        .then(Commands.literal("player")
                                                .then(Commands.argument("player", EntityArgument.entities())
                                                        .executes(ShopTeamCurrencyCommands::addToPlayerTeam))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("currencyId", StringArgumentType.word())
                                .suggests(SUGGEST_CURRENCIES)
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                                        .executes(ShopTeamCurrencyCommands::removeFromOwnTeam)
                                        .then(Commands.literal("team")
                                                .then(Commands.argument("teamName", StringArgumentType.word())
                                                        .suggests(SUGGEST_TEAMS)
                                                        .executes(ShopTeamCurrencyCommands::removeFromTeam)))
                                        .then(Commands.literal("player")
                                                .then(Commands.argument("player", EntityArgument.entities())
                                                        .executes(ShopTeamCurrencyCommands::removeFromPlayerTeam))))))
                .then(Commands.literal("set")
                        .then(Commands.argument("currencyId", StringArgumentType.word())
                                .suggests(SUGGEST_CURRENCIES)
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                                        .executes(ShopTeamCurrencyCommands::setForOwnTeam)
                                        .then(Commands.literal("team")
                                                .then(Commands.argument("teamName", StringArgumentType.word())
                                                        .suggests(SUGGEST_TEAMS)
                                                        .executes(ShopTeamCurrencyCommands::setForTeam)))
                                        .then(Commands.literal("player")
                                                .then(Commands.argument("player", EntityArgument.entities())
                                                        .executes(ShopTeamCurrencyCommands::setForPlayerTeam))))))
                .then(Commands.literal("move")
                        .then(Commands.argument("currencyId", StringArgumentType.word())
                                .suggests(SUGGEST_CURRENCIES)
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                                        .then(Commands.argument("toTeam", StringArgumentType.word())
                                                .suggests(SUGGEST_TEAMS)
                                                .executes(ShopTeamCurrencyCommands::moveFromOwnTeam)
                                                .then(Commands.argument("fromTeam", StringArgumentType.word())
                                                        .suggests(SUGGEST_TEAMS)
                                                        .executes(ShopTeamCurrencyCommands::moveBetweenTeams))))));
    }

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_CURRENCIES = (context, builder) -> {
        List<String> ids = ShopLoader.getAllCurrencyIds();
        if (ids == null || ids.isEmpty()) {
            ids = new ArrayList<>(ShopLoader.getCurrencies().keySet());
        }
        return SharedSuggestionProvider.suggest(ids, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_TEAMS = (context, builder) -> {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            return builder.buildFuture();
        }
        ShopTeamManager teamManager = teamManager(player);
        List<String> names = teamManager.getAllTeamNames();
        return SharedSuggestionProvider.suggest(names, builder);
    };

    private static ShopTeamManager teamManager(ServerPlayer player) {
        return ShopTeamManager.getInstance((ServerLevel) player.level());
    }

    private static int listCurrencies(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("=== Available Currencies ==="), false);
        for (ShopCurrency currency : ShopCurrency.sorted(ShopLoader.getCurrencies().values())) {
            String localizedName = Component.translatable(currency.name).getString();
            String formattedName = localizedName + " " + currency.charSymbol;
            source.sendSuccess(() -> Component.literal(
                    String.format("- %s (%s): %s", currency.id, formattedName, currency.charSymbol)
            ), false);
        }
        return 1;
    }

    private static int addToOwnTeam(CommandContext<CommandSourceStack> context) {
        return mutateOwn(context, true);
    }

    private static int removeFromOwnTeam(CommandContext<CommandSourceStack> context) {
        return mutateOwn(context, false);
    }

    private static int mutateOwn(CommandContext<CommandSourceStack> context, boolean add) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        String currencyId = StringArgumentType.getString(context, "currencyId");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        ShopTeamManager teamManager = teamManager(player);
        String teamKey = teamManager.getPlayerTeamKey(player);
        if (teamKey == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        boolean ok = add
                ? teamManager.addTeamCurrency(teamKey, currencyId, amount)
                : teamManager.removeTeamCurrency(teamKey, currencyId, amount);
        return reportMutate(source, teamManager, teamKey, currencyId, amount, add, ok);
    }

    private static int addToTeam(CommandContext<CommandSourceStack> context) {
        return mutateNamedTeam(context, true);
    }

    private static int removeFromTeam(CommandContext<CommandSourceStack> context) {
        return mutateNamedTeam(context, false);
    }

    private static int mutateNamedTeam(CommandContext<CommandSourceStack> context, boolean add) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        String currencyId = StringArgumentType.getString(context, "currencyId");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        String teamName = StringArgumentType.getString(context, "teamName");
        ShopTeamManager teamManager = teamManager(player);
        String teamKey = teamManager.resolveTeamKeyByDisplayName(teamName);
        boolean ok = add
                ? teamManager.addTeamCurrency(teamKey, currencyId, amount)
                : teamManager.removeTeamCurrency(teamKey, currencyId, amount);
        return reportMutate(source, teamManager, teamKey, currencyId, amount, add, ok);
    }

    private static int addToPlayerTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return mutatePlayerTeams(context, true);
    }

    private static int removeFromPlayerTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return mutatePlayerTeams(context, false);
    }

    private static int mutatePlayerTeams(CommandContext<CommandSourceStack> context, boolean add) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String currencyId = StringArgumentType.getString(context, "currencyId");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        List<ServerPlayer> targets = getTargetPlayers(context, "player");
        if (targets.isEmpty()) {
            source.sendFailure(Component.literal("No player found from selector"));
            return 0;
        }
        int count = 0;
        for (ServerPlayer target : targets) {
            ShopTeamManager teamManager = teamManager(target);
            String teamKey = teamManager.getPlayerTeamKey(target);
            if (teamKey == null) {
                source.sendFailure(Component.literal("Player " + target.getName().getString() + " is not in a team"));
                continue;
            }
            boolean ok = add
                    ? teamManager.addTeamCurrency(teamKey, currencyId, amount)
                    : teamManager.removeTeamCurrency(teamKey, currencyId, amount);
            if (ok) {
                count++;
                String display = currencyDisplay(currencyId);
                String teamDisplay = displayTeamName(teamManager, teamKey);
                String verb = add ? "Added" : "Removed";
                String prep = add ? " to " : " from ";
                source.sendSuccess(() -> Component.literal(verb + " " + amount + " " + display + prep
                        + target.getName().getString() + "'s team '" + teamDisplay + "'!"), false);
            } else {
                source.sendFailure(Component.literal("Failed to " + (add ? "add" : "remove") + " currencies for team."));
            }
        }
        return count;
    }

    private static int setForOwnTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        ShopTeamManager teamManager = teamManager(player);
        String teamKey = teamManager.getPlayerTeamKey(player);
        if (teamKey == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        return setTeamCurrency(source, teamManager, teamKey,
                StringArgumentType.getString(context, "currencyId"),
                DoubleArgumentType.getDouble(context, "amount"));
    }

    private static int setForTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        ShopTeamManager teamManager = teamManager(player);
        String teamKey = teamManager.resolveTeamKeyByDisplayName(StringArgumentType.getString(context, "teamName"));
        return setTeamCurrency(source, teamManager, teamKey,
                StringArgumentType.getString(context, "currencyId"),
                DoubleArgumentType.getDouble(context, "amount"));
    }

    private static int setForPlayerTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String currencyId = StringArgumentType.getString(context, "currencyId");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        List<ServerPlayer> targets = getTargetPlayers(context, "player");
        if (targets.isEmpty()) {
            source.sendFailure(Component.literal("No player found from selector"));
            return 0;
        }
        int total = 0;
        for (ServerPlayer target : targets) {
            ShopTeamManager teamManager = teamManager(target);
            String teamKey = teamManager.getPlayerTeamKey(target);
            if (teamKey == null) {
                source.sendFailure(Component.literal("Player " + target.getName().getString() + " is not in a team"));
                continue;
            }
            total += setTeamCurrency(source, teamManager, teamKey, currencyId, amount);
        }
        return total;
    }

    private static int setTeamCurrency(CommandSourceStack source, ShopTeamManager teamManager, String teamKey,
                                       String currencyId, double targetAmount) {
        double current = teamManager.getTeamCurrencyBalance(teamKey, currencyId);
        double difference = targetAmount - current;
        boolean success;
        if (difference > 0) {
            success = teamManager.addTeamCurrency(teamKey, currencyId, difference);
        } else if (difference < 0) {
            success = teamManager.removeTeamCurrency(teamKey, currencyId, Math.abs(difference));
        } else {
            success = true;
        }
        String display = currencyDisplay(currencyId);
        if (success) {
            String teamDisplay = displayTeamName(teamManager, teamKey);
            source.sendSuccess(() -> Component.literal(
                    "Set " + display + " balance for team '" + teamDisplay + "' to " + targetAmount + "!"), false);
            return 1;
        }
        source.sendFailure(Component.literal("Failed to set currencies for team. Team might not exist."));
        return 0;
    }

    private static int moveFromOwnTeam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        String currencyId = StringArgumentType.getString(context, "currencyId");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        String toTeam = StringArgumentType.getString(context, "toTeam");
        ShopTeamManager teamManager = teamManager(player);
        String fromTeam = teamManager.getPlayerTeamKey(player);
        if (fromTeam == null) {
            source.sendFailure(Component.literal("You are not in a team"));
            return 0;
        }
        UUID leader = teamManager.getTeamLeader(fromTeam);
        List<UUID> assistants = teamManager.getTeamAssistants(fromTeam);
        UUID playerUuid = player.getUUID();
        if (!playerUuid.equals(leader) && (assistants == null || !assistants.contains(playerUuid))) {
            source.sendFailure(Component.literal("You must be the leader or an assistant to move currency from your team"));
            return 0;
        }
        String toKey = teamManager.resolveTeamKeyByDisplayName(toTeam);
        return moveCurrencyLogic(source, teamManager, fromTeam, toKey, currencyId, amount);
    }

    private static int moveBetweenTeams(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        ShopTeamManager teamManager = teamManager(player);
        String fromKey = teamManager.resolveTeamKeyByDisplayName(StringArgumentType.getString(context, "fromTeam"));
        String toKey = teamManager.resolveTeamKeyByDisplayName(StringArgumentType.getString(context, "toTeam"));
        return moveCurrencyLogic(source, teamManager, fromKey, toKey,
                StringArgumentType.getString(context, "currencyId"),
                DoubleArgumentType.getDouble(context, "amount"));
    }

    private static int moveCurrencyLogic(CommandSourceStack source, ShopTeamManager teamManager,
                                         String fromTeam, String toTeam, String currencyId, double amount) {
        String fromDisplay = displayTeamName(teamManager, fromTeam);
        String toDisplay = displayTeamName(teamManager, toTeam);
        if (teamManager.getTeamLeader(fromTeam) == null) {
            source.sendFailure(Component.literal("Source team '" + fromDisplay + "' does not exist"));
            return 0;
        }
        if (teamManager.getTeamLeader(toTeam) == null) {
            source.sendFailure(Component.literal("Destination team '" + toDisplay + "' does not exist"));
            return 0;
        }
        double currentBalance = teamManager.getTeamCurrencyBalance(fromTeam, currencyId);
        if (currentBalance < amount) {
            source.sendFailure(Component.literal("Source team '" + fromDisplay
                    + "' does not have enough currency. Current balance: " + currentBalance));
            return 0;
        }
        if (!teamManager.removeTeamCurrency(fromTeam, currencyId, amount)) {
            source.sendFailure(Component.literal("Failed to remove currency from source team"));
            return 0;
        }
        if (!teamManager.addTeamCurrency(toTeam, currencyId, amount)) {
            teamManager.addTeamCurrency(fromTeam, currencyId, amount);
            source.sendFailure(Component.literal("Failed to add currency to destination team. Transaction rolled back."));
            return 0;
        }
        String display = currencyDisplay(currencyId);
        source.sendSuccess(() -> Component.literal(
                "Moved " + amount + " " + display + " from '" + fromDisplay + "' to '" + toDisplay + "'!"), false);
        return 1;
    }

    private static int reportMutate(CommandSourceStack source, ShopTeamManager teamManager, String teamKey,
                                    String currencyId, double amount, boolean add, boolean ok) {
        String display = currencyDisplay(currencyId);
        String teamDisplay = displayTeamName(teamManager, teamKey);
        if (ok) {
            String verb = add ? "Added" : "Removed";
            String prep = add ? " to " : " from ";
            source.sendSuccess(() -> Component.literal(
                    verb + " " + amount + " " + display + prep + "team '" + teamDisplay + "'!"), false);
            return 1;
        }
        source.sendFailure(Component.literal(add
                ? "Failed to add currencies to team. Team might not exist."
                : "Failed to remove currencies from team. Insufficient balance or team doesn't exist."));
        return 0;
    }

    private static String currencyDisplay(String currencyId) {
        ShopCurrency currency = ShopLoader.getCurrencies().get(currencyId);
        if (currency != null) {
            return Component.translatable(currency.name).getString() + " " + currency.charSymbol;
        }
        return currencyId;
    }

    private static String displayTeamName(ShopTeamManager teamManager, String teamKeyOrNull) {
        if (teamKeyOrNull == null) {
            return "null";
        }
        String display = teamManager.getTeamDisplayName(teamKeyOrNull);
        return display != null ? display : teamKeyOrNull;
    }

    private static List<ServerPlayer> getTargetPlayers(CommandContext<CommandSourceStack> context, String argumentName)
            throws CommandSyntaxException {
        Collection<? extends Entity> entities = EntityArgument.getEntities(context, argumentName);
        return entities.stream()
                .filter(ServerPlayer.class::isInstance)
                .map(ServerPlayer.class::cast)
                .collect(Collectors.toList());
    }
}
