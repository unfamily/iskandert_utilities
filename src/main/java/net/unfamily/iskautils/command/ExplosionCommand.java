package net.unfamily.iskautils.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.explosion.ExplosionSystem;
import org.slf4j.Logger;

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
    
    /**
     * Registers the explosion commands
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("iska_utils_explosion")
                .requires(source -> source.hasPermission(2)) // Requires OP level 2
                
                // START command with optional coordinates
                .then(Commands.literal("start")
                    .then(Commands.argument("horizontal_radius", IntegerArgumentType.integer(1))
                        .then(Commands.argument("vertical_radius", IntegerArgumentType.integer(1, 1000))
                            .then(Commands.argument("tick_interval", IntegerArgumentType.integer(0))
                                .then(Commands.argument("damage", FloatArgumentType.floatArg(0.0f))
                                    .then(Commands.argument("break_unbreakable", BoolArgumentType.bool())
                                        // Without coordinates (use player position)
                                        .executes(ExplosionCommand::startExplosionAtPlayer)
                                        // With coordinates
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                            .executes(ExplosionCommand::startExplosionAtCoords))))))))
                
                // STOP command (stops all explosions)
                .then(Commands.literal("stop")
                    .executes(ExplosionCommand::stopAllExplosions))
                
                // Show usage if no sub-command is specified
                .executes(ExplosionCommand::showUsage)
        );
    }
    
    /**
     * Shows command usage
     */
    private static int showUsage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.translatable("commands.iska_utils.explosion.usage.title"), false);
        source.sendSuccess(() -> Component.literal("§a/iska_utils_explosion start <horizontal_radius> <vertical_radius> <tick_interval> <damage> <break_unbreakable> [x y z]"), false);
        source.sendSuccess(() -> Component.literal("§a/iska_utils_explosion stop"), false);
        source.sendSuccess(() -> Component.translatable("commands.iska_utils.explosion.usage.horizontal"), false);
        source.sendSuccess(() -> Component.translatable("commands.iska_utils.explosion.usage.vertical"), false);
        source.sendSuccess(() -> Component.translatable("commands.iska_utils.explosion.usage.interval"), false);
        source.sendSuccess(() -> Component.translatable("commands.iska_utils.explosion.usage.damage"), false);
        source.sendSuccess(() -> Component.translatable("commands.iska_utils.explosion.usage.break_unbreakable"), false);
        return 1;
    }
    
    /**
     * Starts an explosion at the player's position
     */
    private static int startExplosionAtPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        
        int horizontalRadius = IntegerArgumentType.getInteger(context, "horizontal_radius");
        int verticalRadius = IntegerArgumentType.getInteger(context, "vertical_radius");
        int tickInterval = IntegerArgumentType.getInteger(context, "tick_interval");
        float damage = FloatArgumentType.getFloat(context, "damage");
        boolean breakUnbreakable = BoolArgumentType.getBool(context, "break_unbreakable");
            
        BlockPos center = player.blockPosition();
        
        return startExplosion(source, level, center, horizontalRadius, verticalRadius, tickInterval, damage, breakUnbreakable);
    }
    
    /**
     * Starts an explosion at the specified coordinates
     */
    private static int startExplosionAtCoords(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        
        int horizontalRadius = IntegerArgumentType.getInteger(context, "horizontal_radius");
        int verticalRadius = IntegerArgumentType.getInteger(context, "vertical_radius");
        int tickInterval = IntegerArgumentType.getInteger(context, "tick_interval");
        float damage = FloatArgumentType.getFloat(context, "damage");
        boolean breakUnbreakable = BoolArgumentType.getBool(context, "break_unbreakable");
        BlockPos center = BlockPosArgument.getBlockPos(context, "pos");
        
        return startExplosion(source, level, center, horizontalRadius, verticalRadius, tickInterval, damage, breakUnbreakable);
    }
    
    /**
     * Common method to start an explosion
     */
    private static int startExplosion(CommandSourceStack source, ServerLevel level, BlockPos center,
                                    int horizontalRadius, int verticalRadius, int tickInterval, float damage, boolean breakUnbreakable) {
        
        ExplosionSystem.createExplosion(level, center, horizontalRadius, verticalRadius, tickInterval, damage, breakUnbreakable);
        
        source.sendSuccess(() -> Component.translatable("commands.iska_utils.explosion.created",
            center.getX(), center.getY(), center.getZ(), 
            horizontalRadius, verticalRadius, tickInterval, 0
        ), true);
        
        LOGGER.info("Created explosion at center {} with radii {}x{}, interval {} ticks, damage {}, break unbreakable: {}", 
            center, horizontalRadius, verticalRadius, tickInterval, damage, breakUnbreakable);
        
        return 1;
    }
    
    /**
     * Stops all active explosions
     */
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
} 