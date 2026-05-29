package net.unfamily.iskautils.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.explosion.ExplosionInfo;
import net.unfamily.iskautils.explosion.ExplosionSystem;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Command interface for custom lag-free explosions with progressive elliptical expansion
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public class ExplosionCommand {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registering custom explosion commands");
        register(event.getDispatcher());
    }
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("iska_utils_explosion")
                .requires(source -> source.hasPermission(2))
                
                .then(Commands.literal("start")
                    .then(Commands.argument("horizontal_radius", IntegerArgumentType.integer(1))
                        .then(Commands.argument("vertical_radius", IntegerArgumentType.integer(1, 1000))
                            .then(Commands.argument("tick_interval", IntegerArgumentType.integer(0))
                                .then(Commands.argument("damage", FloatArgumentType.floatArg(0.0f))
                                    .then(Commands.argument("break_unbreakable", BoolArgumentType.bool())
                                        .executes(ExplosionCommand::startExplosionAtPlayer)
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                            .executes(ExplosionCommand::startExplosionAtCoords))))))))
                
                .then(Commands.literal("stop")
                    .executes(ExplosionCommand::stopAllExplosions)
                    .then(Commands.argument("target", StringArgumentType.word())
                        .suggests(ExplosionCommand::suggestStopTargets)
                        .executes(ExplosionCommand::stopExplosionTarget)))

                .then(Commands.literal("info")
                    .executes(ExplosionCommand::showExplosionInfo))
                
                .executes(ExplosionCommand::showUsage)
        );
    }

    private static CompletableFuture<Suggestions> suggestStopTargets(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {
        SharedSuggestionProvider.suggest(new String[] {"all"}, builder);
        for (int number : ExplosionSystem.getActiveExplosionNumbers()) {
            builder.suggest(String.valueOf(number));
        }
        return builder.buildFuture();
    }
    
    private static int showUsage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.translatable("commands.iska_utils.explosion.usage.title"), false);
        source.sendSuccess(() -> Component.literal("§a/iska_utils_explosion start <horizontal_radius> <vertical_radius> <tick_interval> <damage> <break_unbreakable> [x y z]"), false);
        source.sendSuccess(() -> Component.literal("§a/iska_utils_explosion stop [all|<id>]"), false);
        source.sendSuccess(() -> Component.literal("§a/iska_utils_explosion info"), false);
        source.sendSuccess(() -> Component.translatable("commands.iska_utils.explosion.usage.horizontal"), false);
        source.sendSuccess(() -> Component.translatable("commands.iska_utils.explosion.usage.vertical"), false);
        source.sendSuccess(() -> Component.translatable("commands.iska_utils.explosion.usage.interval"), false);
        source.sendSuccess(() -> Component.translatable("commands.iska_utils.explosion.usage.damage"), false);
        source.sendSuccess(() -> Component.translatable("commands.iska_utils.explosion.usage.break_unbreakable"), false);
        return 1;
    }
    
    private static int startExplosionAtPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        
        return startExplosion(
                source,
                source.getLevel(),
                player.blockPosition(),
                IntegerArgumentType.getInteger(context, "horizontal_radius"),
                IntegerArgumentType.getInteger(context, "vertical_radius"),
                IntegerArgumentType.getInteger(context, "tick_interval"),
                FloatArgumentType.getFloat(context, "damage"),
                BoolArgumentType.getBool(context, "break_unbreakable"));
    }
    
    private static int startExplosionAtCoords(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        return startExplosion(
                source,
                source.getLevel(),
                BlockPosArgument.getBlockPos(context, "pos"),
                IntegerArgumentType.getInteger(context, "horizontal_radius"),
                IntegerArgumentType.getInteger(context, "vertical_radius"),
                IntegerArgumentType.getInteger(context, "tick_interval"),
                FloatArgumentType.getFloat(context, "damage"),
                BoolArgumentType.getBool(context, "break_unbreakable"));
    }
    
    private static int startExplosion(CommandSourceStack source, ServerLevel level, BlockPos center,
                                    int horizontalRadius, int verticalRadius, int tickInterval, float damage, boolean breakUnbreakable) {
        
        int number = ExplosionSystem.createExplosion(level, center, horizontalRadius, verticalRadius, tickInterval, damage, breakUnbreakable);
        
        source.sendSuccess(() -> Component.translatable("commands.iska_utils.explosion.created",
            number,
            center.getX(), center.getY(), center.getZ(), 
            horizontalRadius, verticalRadius, tickInterval), true);
        
        LOGGER.info("Created explosion #{} at center {} with radii {}x{}, interval {} ticks, damage {}, break unbreakable: {}", 
            number, center, horizontalRadius, verticalRadius, tickInterval, damage, breakUnbreakable);
        
        return number;
    }
    
    private static int stopAllExplosions(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int stoppedCount = ExplosionSystem.stopAllExplosions();
        
        if (stoppedCount > 0) {
            source.sendSuccess(() -> Component.translatable("commands.iska_utils.explosion.stopped", stoppedCount), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.iska_utils.explosion.none_active"), false);
        }
        
        return stoppedCount;
    }

    private static int stopExplosionTarget(CommandContext<CommandSourceStack> context) {
        String target = StringArgumentType.getString(context, "target");
        if ("all".equalsIgnoreCase(target)) {
            return stopAllExplosions(context);
        }

        int number;
        try {
            number = Integer.parseInt(target);
        } catch (NumberFormatException e) {
            context.getSource().sendFailure(Component.translatable("commands.iska_utils.explosion.stop.invalid_target", target));
            return 0;
        }

        CommandSourceStack source = context.getSource();
        if (ExplosionSystem.stopExplosion(number)) {
            source.sendSuccess(() -> Component.translatable("commands.iska_utils.explosion.stopped_one", number), true);
            return 1;
        }

        source.sendFailure(Component.translatable("commands.iska_utils.explosion.stop.not_found", number));
        return 0;
    }

    private static int showExplosionInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        List<ExplosionInfo> active = ExplosionSystem.getActiveExplosionInfo();

        if (active.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.iska_utils.explosion.info.none"), false);
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("commands.iska_utils.explosion.info.header", active.size()), false);

        for (ExplosionInfo info : active) {
            source.sendSuccess(() -> Component.translatable(
                    "commands.iska_utils.explosion.info.entry",
                    info.number(),
                    info.dimensionId(),
                    info.center().getX(),
                    info.center().getY(),
                    info.center().getZ(),
                    info.horizontalRadius(),
                    info.verticalRadius(),
                    String.format("%.1f", info.completionPercent()),
                    info.waveRing(),
                    info.maxWaveRing(),
                    info.completedSubRegions(),
                    info.scheduledSubRegions(),
                    info.blocksDestroyed()), false);

            if (info.activeSubRegionOrigin() != null) {
                BlockPos front = info.activeSubRegionOrigin();
                source.sendSuccess(() -> Component.translatable(
                        "commands.iska_utils.explosion.info.active_front",
                        front.getX(),
                        front.getY(),
                        front.getZ(),
                        String.format("%.0f", info.activeSubRegionPartial() * 100f)), false);
            }
        }

        return active.size();
    }
}
