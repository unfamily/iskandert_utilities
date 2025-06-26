package net.unfamily.iskautils.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
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
import net.unfamily.iskautils.structure.StructureDefinition;
import net.unfamily.iskautils.structure.StructureLoader;
import net.unfamily.iskautils.structure.StructurePlacer;
import org.slf4j.Logger;

/**
 * Command handler for structure placement
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public class StructureCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Error messages
    private static final SimpleCommandExceptionType ERROR_STRUCTURE_NOT_FOUND = new SimpleCommandExceptionType(
            Component.translatable("commands.iska_utils.structure.error.not_found"));
    
    private static final SimpleCommandExceptionType ERROR_PLACEMENT_FAILED = new SimpleCommandExceptionType(
            Component.translatable("commands.iska_utils.structure.error.placement_failed"));

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {

        register(event.getDispatcher());
    }
    
    /**
     * Suggestion provider for structure IDs
     */
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_STRUCTURE_IDS = 
        (context, builder) -> {
            return SharedSuggestionProvider.suggest(
                StructureLoader.getAvailableStructureIds(), builder);
        };
    
    /**
     * Registers the structure command
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("iska_utils_structure")
                .requires(source -> source.hasPermission(2)) // Requires OP level 2
                
                // Command to list available structures
                .then(Commands.literal("list")
                    .executes(StructureCommand::listStructures))
                
                // Command to reload structures
                .then(Commands.literal("reload")
                    .executes(StructureCommand::reloadStructures))
                    
                // Command to get information about a specific structure
                .then(Commands.literal("info")
                    .then(Commands.argument("structure_id", StringArgumentType.string())
                        .suggests(SUGGEST_STRUCTURE_IDS)
                        .executes(StructureCommand::showStructureInfo)))
                
                // Main command to place a structure
                .then(Commands.literal("place")
                    .then(Commands.argument("structure_id", StringArgumentType.string())
                        .suggests(SUGGEST_STRUCTURE_IDS)
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                            .executes(StructureCommand::placeStructure))))
                
                // Show help if no sub-command is specified
                .executes(StructureCommand::showUsage)
        );
    }
    
    /**
     * Shows command usage
     */
    private static int showUsage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("§6===== IskaUtils Structure Commands ====="), false);
        source.sendSuccess(() -> Component.literal("§a/iska_utils_structure list §7- List all available structures"), false);
        source.sendSuccess(() -> Component.literal("§a/iska_utils_structure reload §7- Reload structure definitions"), false);
        source.sendSuccess(() -> Component.literal("§a/iska_utils_structure info <structure_id> §7- Show structure information"), false);
        source.sendSuccess(() -> Component.literal("§a/iska_utils_structure place <structure_id> <x> <y> <z> §7- Place a structure"), false);
        return 1;
    }

    /**
     * Lists all available structures
     */
    private static int listStructures(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        var allStructures = StructureLoader.getAllStructures();
        var clientStructures = StructureLoader.getClientStructures();
        
        if (allStructures.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§eNo structures available."), false);
            source.sendSuccess(() -> Component.literal("§7Use §a/iska_utils_structure reload §7to reload structures."), false);
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("§6===== Available Structures ====="), false);
        
        for (StructureDefinition structure : allStructures.values()) {
            String name = structure.getName() != null ? structure.getName() : structure.getId();
            String structureId = structure.getId();
            
            // Indicates if it's a client structure and which player
            String prefix = "§a";
            if (structureId.startsWith("client_")) {
                prefix = "§b"; // Blue for client structures
                
                // Extract player name from ID if possible
                String[] parts = structureId.split("_", 3); // ["client", "nickname", "resto"]
                if (parts.length >= 2) {
                    String playerName = parts[1];
                    name = name + " §7(Player: §b" + playerName + "§7)";
                }
            }
            
            Component message = Component.literal(prefix + structureId + " §7- §f" + name);
            
            if (structure.getDescription() != null && !structure.getDescription().isEmpty()) {
                String desc = structure.getDescription();
                message = message.copy().append(Component.literal(" §7(" + desc + ")"));
            }
            
            final Component finalMessage = message;
            source.sendSuccess(() -> finalMessage, false);
        }
        
        int regularCount = allStructures.size() - clientStructures.size();
        source.sendSuccess(() -> Component.literal("§7Total: §a" + regularCount + " §7regular + §b" + clientStructures.size() + " §7client = §f" + allStructures.size() + " §7structures"), false);
        
        if (clientStructures.size() > 0) {
            source.sendSuccess(() -> Component.literal("§7§oClient structures are shown in §bblue§7§o"), false);
        }
        
        return allStructures.size();
    }

    /**
     * Reloads structure definitions
     */
    private static int reloadStructures(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            int oldCount = StructureLoader.getAllStructures().size();
            
            // Determine if we're in singleplayer or multiplayer
            boolean isSingleplayer = source.getServer() != null && source.getServer().isSingleplayer();
            
            // Get the player if available (to load their client structures)
            ServerPlayer commandPlayer = null;
            try {
                commandPlayer = source.getPlayerOrException();
            } catch (CommandSyntaxException e) {
                // Command executed from console
                LOGGER.debug("Command executed from console, no specific player available");
            }
            
            if (isSingleplayer) {
                // Singleplayer: always force loading of client structures
    
                StructureLoader.reloadAllDefinitions(true, commandPlayer);
                source.sendSuccess(() -> Component.literal("§7Singleplayer mode: Client structures included"), false);
            } else {
                // Multiplayer: use the server configuration flag
    
                StructureLoader.reloadAllDefinitions(true, commandPlayer); // Force reload on server
                source.sendSuccess(() -> Component.literal("§7Multiplayer mode: Client structures based on server config"), false);
            }
            
            int newCount = StructureLoader.getAllStructures().size();
            var clientStructures = StructureLoader.getClientStructures();
            
            source.sendSuccess(() -> Component.literal("§aStructures reloaded successfully!"), false);
            source.sendSuccess(() -> Component.literal("§7Structures loaded: §a" + (newCount - clientStructures.size()) + " §7regular + §b" + clientStructures.size() + " §7client = §f" + newCount + " §7total (previous: §c" + oldCount + "§7)"), false);
            
            // Synchronize reloaded structures with all connected clients (multiplayer only)
            if (!isSingleplayer) {
                try {
                    if (source.getServer() != null) {
                        for (net.minecraft.server.level.ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
                            net.unfamily.iskautils.network.ModMessages.sendStructureSyncPacket(player);
                        }
                        source.sendSuccess(() -> Component.literal("§7Structures synchronized to all connected clients"), false);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error synchronizing reloaded structures to clients: {}", e.getMessage());
                    source.sendSuccess(() -> Component.literal("§cWarning: Failed to synchronize structures to clients"), false);
                }
            } else {
                source.sendSuccess(() -> Component.literal("§7Singleplayer mode: No client synchronization needed"), false);
            }
            
            return newCount;
        } catch (Exception e) {
            LOGGER.error("Error reloading structures: {}", e.getMessage());
            source.sendFailure(Component.literal("§cError reloading structures: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Shows detailed information about a specific structure
     */
    private static int showStructureInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String structureId = StringArgumentType.getString(context, "structure_id");
        
        StructureDefinition structure = StructureLoader.getStructure(structureId);
        if (structure == null) {
            throw ERROR_STRUCTURE_NOT_FOUND.create();
        }
        
        source.sendSuccess(() -> Component.literal("§6===== Structure Information ====="), false);
        source.sendSuccess(() -> Component.literal("§bID: §f" + structure.getId()), false);
        source.sendSuccess(() -> Component.literal("§bName: §f" + (structure.getName() != null ? structure.getName() : structure.getId())), false);
        
        if (structure.getDescription() != null && !structure.getDescription().isEmpty()) {
            source.sendSuccess(() -> Component.literal("§bDescription:"), false);
            source.sendSuccess(() -> Component.literal("  §7" + structure.getDescription()), false);
        }
        
        int[] dimensions = structure.getDimensions();
        source.sendSuccess(() -> Component.literal("§bDimensions: §f" + dimensions[0] + "×" + dimensions[1] + "×" + dimensions[2] + " §7(W×H×D)"), false);
        
        BlockPos center = structure.findCenter();
        if (center != null) {
            source.sendSuccess(() -> Component.literal("§bCenter: §f" + center.getX() + ", " + center.getY() + ", " + center.getZ()), false);
        }
        
        source.sendSuccess(() -> Component.literal("§bForce placement: §f" + (structure.isCanForce() ? "§aAllowed" : "§cNot allowed")), false);
        
        if (structure.getCanReplace() != null && !structure.getCanReplace().isEmpty()) {
            source.sendSuccess(() -> Component.literal("§bCan replace: §f" + String.join(", ", structure.getCanReplace())), false);
        }
        
        if (structure.getStages() != null && !structure.getStages().isEmpty()) {
            source.sendSuccess(() -> Component.literal("§bRequired stages: §f" + structure.getStages().size()), false);
        }
        
        return 1;
    }

    /**
     * Places a structure at the specified coordinates
     */
    private static int placeStructure(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String structureId = StringArgumentType.getString(context, "structure_id");
        BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
        
        // Verify that the structure exists
        StructureDefinition structure = StructureLoader.getStructure(structureId);
        if (structure == null) {
            throw ERROR_STRUCTURE_NOT_FOUND.create();
        }
        
        // Get the server level
        ServerLevel level = source.getLevel();
        
        // Get the player if available (for stage checks)
        ServerPlayer player = null;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            // Command can be executed from console too
            LOGGER.debug("Command executed from console, no player available");
        }
        
        try {
            // Attempt to place the structure
            boolean success = StructurePlacer.placeStructure(level, pos, structure, player);
            
            if (success) {
                source.sendSuccess(() -> Component.literal("§aStructure §f" + structureId + " §aplaced successfully at §f" + 
                    pos.getX() + ", " + pos.getY() + ", " + pos.getZ()), false);
                return 1;
            } else {
                source.sendFailure(Component.literal("§cFailed to place structure §f" + structureId + 
                    " §cat §f" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
                return 0;
            }
        } catch (Exception e) {
            LOGGER.error("Error placing structure {}: {}", structureId, e.getMessage());
            source.sendFailure(Component.literal("§cError during placement: " + e.getMessage()));
            throw ERROR_PLACEMENT_FAILED.create();
        }
    }
} 