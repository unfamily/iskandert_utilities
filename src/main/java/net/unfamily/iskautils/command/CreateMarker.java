package net.unfamily.iskautils.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.MarkRenderer;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Command handler for the markers
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public class CreateMarker {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Default values for debug
    private static final String DEFAULT_COLOR = "33FF0000"; // Rosso semi-trasparente
    private static final int DEFAULT_DURATION = 1200; // 60 seconds (20 tick = 1 second)
    
    // Command usage messages map
    private static final Map<String, String> COMMAND_USAGE = new HashMap<>();
    
    // Initialize usage messages
    static {
        COMMAND_USAGE.put("create_marker", "/iska_utils_marker create <x> <y> <z> [color] [duration] [text]");
        COMMAND_USAGE.put("create_marker_looking", "/iska_utils_marker create_looking [color] [duration] [text]");
        COMMAND_USAGE.put("create_billboard", "/iska_utils_marker billboard <x> <y> <z> [color] [duration] [text]");
        COMMAND_USAGE.put("create_billboard_looking", "/iska_utils_marker billboard_looking [color] [duration] [text]");
        COMMAND_USAGE.put("clear_markers", "/iska_utils_marker clear");
    }
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registering marker commands");
        register(event.getDispatcher());
    }
    
    /**
     * Register the commands for the markers
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("iska_utils_marker")
                .requires(source -> source.hasPermission(0)) //No permission required
                .then(Commands.literal("create")
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(context -> createMarker(context, DEFAULT_COLOR, DEFAULT_DURATION, null))
                        .then(Commands.argument("color", StringArgumentType.word())
                            .executes(context -> createMarker(context, 
                                StringArgumentType.getString(context, "color"), 
                                DEFAULT_DURATION, null))
                            .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                                .executes(context -> createMarker(context, 
                                    StringArgumentType.getString(context, "color"), 
                                    IntegerArgumentType.getInteger(context, "duration"), null))
                                .then(Commands.argument("text", StringArgumentType.greedyString())
                                    .executes(context -> createMarker(context, 
                                        StringArgumentType.getString(context, "color"), 
                                        IntegerArgumentType.getInteger(context, "duration"),
                                        StringArgumentType.getString(context, "text"))))))))
                .then(Commands.literal("create_looking")
                    .executes(context -> createMarkerLooking(context, DEFAULT_COLOR, DEFAULT_DURATION, null))
                    .then(Commands.argument("color", StringArgumentType.word())
                        .executes(context -> createMarkerLooking(context, 
                            StringArgumentType.getString(context, "color"), 
                            DEFAULT_DURATION, null))
                        .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                            .executes(context -> createMarkerLooking(context, 
                                StringArgumentType.getString(context, "color"), 
                                IntegerArgumentType.getInteger(context, "duration"), null))
                            .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(context -> createMarkerLooking(context, 
                                    StringArgumentType.getString(context, "color"), 
                                    IntegerArgumentType.getInteger(context, "duration"),
                                    StringArgumentType.getString(context, "text")))))))
                .then(Commands.literal("billboard")
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(context -> createBillboard(context, DEFAULT_COLOR, DEFAULT_DURATION, null))
                        .then(Commands.argument("color", StringArgumentType.word())
                            .executes(context -> createBillboard(context, 
                                StringArgumentType.getString(context, "color"), 
                                DEFAULT_DURATION, null))
                            .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                                .executes(context -> createBillboard(context, 
                                    StringArgumentType.getString(context, "color"), 
                                    IntegerArgumentType.getInteger(context, "duration"), null))
                                .then(Commands.argument("text", StringArgumentType.greedyString())
                                    .executes(context -> createBillboard(context, 
                                        StringArgumentType.getString(context, "color"), 
                                        IntegerArgumentType.getInteger(context, "duration"),
                                        StringArgumentType.getString(context, "text"))))))))
                .then(Commands.literal("billboard_looking")
                    .executes(context -> createBillboardLooking(context, DEFAULT_COLOR, DEFAULT_DURATION, null))
                    .then(Commands.argument("color", StringArgumentType.word())
                        .executes(context -> createBillboardLooking(context, 
                            StringArgumentType.getString(context, "color"), 
                            DEFAULT_DURATION, null))
                        .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                            .executes(context -> createBillboardLooking(context, 
                                StringArgumentType.getString(context, "color"), 
                                IntegerArgumentType.getInteger(context, "duration"), null))
                            .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(context -> createBillboardLooking(context, 
                                    StringArgumentType.getString(context, "color"), 
                                    IntegerArgumentType.getInteger(context, "duration"),
                                    StringArgumentType.getString(context, "text")))))))
                .then(Commands.literal("clear")
                    .executes(CreateMarker::clearMarkers))
        );
    }
    
    /**
     * Displays command usage to a player
     */
    public static void sendUsage(CommandSourceStack source, String commandKey) {
        String usage = COMMAND_USAGE.getOrDefault(commandKey, "/iska_utils_marker " + commandKey.replace('_', ' '));
        source.sendFailure(Component.literal("§cUsage: " + usage));
    }
    
    /**
     * Gets the command usage string for JSON definitions
     */
    public static String getCommandUsage(String commandKey) {
        return COMMAND_USAGE.getOrDefault(commandKey, "/iska_utils_marker " + commandKey.replace('_', ' '));
    }
    
    /**
     * Converte una stringa esadecimale in un valore intero
     * Supporta formati come "RRGGBB" o "AARRGGBB"
     */
    private static int parseHexColor(String hexColor) {
        // Rimuovi eventuali prefissi "0x" o "#"
        if (hexColor.startsWith("0x") || hexColor.startsWith("0X")) {
            hexColor = hexColor.substring(2);
        } else if (hexColor.startsWith("#")) {
            hexColor = hexColor.substring(1);
        }
        
        // Se il colore è in formato RGB (6 caratteri), aggiungi l'alfa
        if (hexColor.length() == 6) {
            hexColor = "33" + hexColor; // Aggiungi alfa semi-trasparente (33 = ~20%)
        }
        
        // Assicurati che la stringa abbia 8 caratteri (AARRGGBB)
        if (hexColor.length() != 8) {
            LOGGER.warn("Invalid hex color format: {}. Using default color.", hexColor);
            return 0x33FF0000; // Rosso semi-trasparente come default
        }
        
        try {
            return (int) Long.parseLong(hexColor, 16);
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid hex color format: {}. Using default color.", hexColor);
            return 0x33FF0000; // Rosso semi-trasparente come default
        }
    }
    
    /**
     * Create a marker for a block with specified coordinates
     */
    private static int createMarker(CommandContext<CommandSourceStack> context, String colorHex, int duration, String text) throws CommandSyntaxException {
        // Get the command source
        CommandSourceStack source = context.getSource();
        
        // Extract the parameters
        BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
        
        // Parse the color
        int color = parseHexColor(colorHex);
        
        // Add the highlighted block
        if (text != null) {
            MarkRenderer.getInstance().addHighlightedBlock(pos, color, duration, text);
        } else {
            MarkRenderer.getInstance().addHighlightedBlock(pos, color, duration);
        }
     
        // Send a confirmation message
        String textInfo = text != null ? " with text \"" + text + "\"" : "";
        source.sendSuccess(() -> Component.literal(
            String.format("§aMarker Block created at %s with color 0x%08X for %d tick%s", 
            pos.toShortString(), color, duration, textInfo)
        ), true);
        
        return 1;
    }
    
    /**
     * Create a marker for the block the player is looking at
     */
    private static int createMarkerLooking(CommandContext<CommandSourceStack> context, String colorHex, int duration, String text) throws CommandSyntaxException {
        // Get the command source
        CommandSourceStack source = context.getSource();
        
        // Check if the command was executed by a player
        Player player = source.getPlayerOrException();
        
        // Get the block the player is looking at
        HitResult hitResult = player.pick(20.0, 0.0F, false);
        
        if (!(hitResult instanceof BlockHitResult blockHitResult) || hitResult.getType() == HitResult.Type.MISS) {
            source.sendFailure(Component.literal("§cNo block found. You must look at a block."));
            return 0;
        }
        
        BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();
        
        // Parse the color
        int color = parseHexColor(colorHex);
        
        // Add the highlighted block
        if (text != null) {
            MarkRenderer.getInstance().addHighlightedBlock(pos, color, duration, text);
        } else {
            MarkRenderer.getInstance().addHighlightedBlock(pos, color, duration);
        }
     
        // Send a confirmation message
        String textInfo = text != null ? " with text \"" + text + "\"" : "";
        source.sendSuccess(() -> Component.literal(
            String.format("§aMarker Block created at %s with color 0x%08X for %d tick%s", 
            pos.toShortString(), color, duration, textInfo)
        ), true);
        
        return 1;
    }
    
    /**
     * Create a billboard marker at specified coordinates
     */
    private static int createBillboard(CommandContext<CommandSourceStack> context, String colorHex, int duration, String text) throws CommandSyntaxException {
        // Get the command source
        CommandSourceStack source = context.getSource();
        
        // Extract the parameters
        BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
        
        // Parse the color
        int color = parseHexColor(colorHex);
        
        // Add the billboard marker
        if (text != null) {
            MarkRenderer.getInstance().addBillboardMarker(pos, color, duration, text);
        } else {
            MarkRenderer.getInstance().addBillboardMarker(pos, color, duration);
        }
     
        // Send a confirmation message
        String textInfo = text != null ? " with text \"" + text + "\"" : "";
        source.sendSuccess(() -> Component.literal(
            String.format("§aBillboard Marker created at %s with color 0x%08X for %d tick%s", 
            pos.toShortString(), color, duration, textInfo)
        ), true);
        
        return 1;
    }
    
    /**
     * Create a billboard marker for the block the player is looking at
     */
    private static int createBillboardLooking(CommandContext<CommandSourceStack> context, String colorHex, int duration, String text) throws CommandSyntaxException {
        // Get the command source
        CommandSourceStack source = context.getSource();
        
        // Check if the command was executed by a player
        Player player = source.getPlayerOrException();
        
        // Get the block the player is looking at
        HitResult hitResult = player.pick(20.0, 0.0F, false);
        
        if (!(hitResult instanceof BlockHitResult blockHitResult) || hitResult.getType() == HitResult.Type.MISS) {
            source.sendFailure(Component.literal("§cNo block found. You must look at a block."));
            return 0;
        }
        
        BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();
        
        // Parse the color
        int color = parseHexColor(colorHex);
        
        // Add the billboard marker
        if (text != null) {
            MarkRenderer.getInstance().addBillboardMarker(pos, color, duration, text);
        } else {
            MarkRenderer.getInstance().addBillboardMarker(pos, color, duration);
        }
     
        // Send a confirmation message
        String textInfo = text != null ? " with text \"" + text + "\"" : "";
        source.sendSuccess(() -> Component.literal(
            String.format("§aBillboard Marker created at %s with color 0x%08X for %d tick%s", 
            pos.toShortString(), color, duration, textInfo)
        ), true);
        
        return 1;
    }
    
    /**
     * Remove all markers
     */
    private static int clearMarkers(CommandContext<CommandSourceStack> context) {
        // Get the command source
        CommandSourceStack source = context.getSource();
        
        // Remove all highlighted blocks
        MarkRenderer.getInstance().clearHighlightedBlocks();
     
        // Send a confirmation message
        source.sendSuccess(() -> Component.literal("§aAll markers have been removed"), true);
        
        return 1;
    }
}