package net.unfamily.iskautils.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
import net.unfamily.iskautils.client.XRayBlockRenderer;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Command handler per i marker XRay
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public class CreateMarker {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Valori predefiniti per il debug
    private static final int DEFAULT_DIFFICULTY = 5;
    private static final int DEFAULT_DURATION = 1200; // 60 secondi (20 tick = 1 secondo)
    
    // Command usage messages map
    private static final Map<String, String> COMMAND_USAGE = new HashMap<>();
    
    // Initialize usage messages
    static {
        COMMAND_USAGE.put("create_marker", "/iska_utils_marker create <x> <y> <z> [difficulty] [duration]");
        COMMAND_USAGE.put("create_marker_looking", "/iska_utils_marker create_looking [difficulty] [duration]");
        COMMAND_USAGE.put("clear_markers", "/iska_utils_marker clear");
    }
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registering marker commands");
        register(event.getDispatcher());
    }
    
    /**
     * Registra i comandi per i marker
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("iska_utils_marker")
                .requires(source -> source.hasPermission(2)) // Require OP level 2
                .then(Commands.literal("create")
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(context -> createMarker(context, DEFAULT_DIFFICULTY, DEFAULT_DURATION))
                        .then(Commands.argument("difficulty", IntegerArgumentType.integer(1, 10))
                            .executes(context -> createMarker(context, 
                                IntegerArgumentType.getInteger(context, "difficulty"), 
                                DEFAULT_DURATION))
                            .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                                .executes(context -> createMarker(context, 
                                    IntegerArgumentType.getInteger(context, "difficulty"), 
                                    IntegerArgumentType.getInteger(context, "duration")))))))
                .then(Commands.literal("create_looking")
                    .executes(context -> createMarkerLooking(context, DEFAULT_DIFFICULTY, DEFAULT_DURATION))
                    .then(Commands.argument("difficulty", IntegerArgumentType.integer(1, 10))
                        .executes(context -> createMarkerLooking(context, 
                            IntegerArgumentType.getInteger(context, "difficulty"), 
                            DEFAULT_DURATION))
                        .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                            .executes(context -> createMarkerLooking(context, 
                                IntegerArgumentType.getInteger(context, "difficulty"), 
                                IntegerArgumentType.getInteger(context, "duration"))))))
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
     * Crea un marker X-Ray per un blocco con coordinate specificate
     */
    private static int createMarker(CommandContext<CommandSourceStack> context, int difficulty, int duration) throws CommandSyntaxException {
        // Ottieni il source del comando
        CommandSourceStack source = context.getSource();
        
        // Estrai i parametri
        BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
        
        // Aggiungi il blocco evidenziato
        XRayBlockRenderer.getInstance().addHighlightedBlock(pos, difficulty, duration);
     
        // Invia un messaggio di conferma
        source.sendSuccess(() -> Component.literal(
            String.format("§aMarker X-Ray creato a %s con difficoltà %d per %d tick", 
            pos.toShortString(), difficulty, duration)
        ), true);
        
        return 1;
    }
    
    /**
     * Crea un marker X-Ray per il blocco che il giocatore sta guardando
     */
    private static int createMarkerLooking(CommandContext<CommandSourceStack> context, int difficulty, int duration) throws CommandSyntaxException {
        // Ottieni il source del comando
        CommandSourceStack source = context.getSource();
        
        // Verifica che il comando sia stato eseguito da un giocatore
        Player player = source.getPlayerOrException();
        
        // Ottieni il blocco che il giocatore sta guardando
        HitResult hitResult = player.pick(20.0, 0.0F, false);
        
        if (!(hitResult instanceof BlockHitResult blockHitResult) || hitResult.getType() == HitResult.Type.MISS) {
            source.sendFailure(Component.literal("§cNessun blocco trovato. Devi guardare un blocco."));
            return 0;
        }
        
        BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();
        
        // Aggiungi il blocco evidenziato
        XRayBlockRenderer.getInstance().addHighlightedBlock(pos, difficulty, duration);
     
        // Invia un messaggio di conferma
        source.sendSuccess(() -> Component.literal(
            String.format("§aMarker X-Ray creato a %s con difficoltà %d per %d tick", 
            pos.toShortString(), difficulty, duration)
        ), true);
        
        return 1;
    }
    
    /**
     * Rimuove tutti i marker X-Ray
     */
    private static int clearMarkers(CommandContext<CommandSourceStack> context) {
        // Ottieni il source del comando
        CommandSourceStack source = context.getSource();
        
        // Rimuovi tutti i blocchi evidenziati
        XRayBlockRenderer.getInstance().clearHighlightedBlocks();
     
        // Invia un messaggio di conferma
        source.sendSuccess(() -> Component.literal("§aTutti i marker X-Ray sono stati rimossi"), true);
        
        return 1;
    }
}