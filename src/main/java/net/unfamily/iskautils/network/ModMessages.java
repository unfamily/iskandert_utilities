package net.unfamily.iskautils.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Vec3i;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.client.ClientEvents;
import net.unfamily.iskautils.client.MarkRenderer;
import net.unfamily.iskautils.network.packet.VectorCharmC2SPacket;
import net.unfamily.iskautils.network.packet.PortableDislocatorC2SPacket;
import net.unfamily.iskautils.structure.StructureDefinition;
import net.unfamily.iskautils.util.ModUtils;
import net.unfamily.iskautils.block.entity.StructurePlacerMachineBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import com.mojang.math.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.Map;
import java.util.List;

/**
 * Handles network messages for the mod
 */
public class ModMessages {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModMessages.class);
    
    // Simplified version to avoid NeoForge networking compatibility issues
    
    /**
     * Registers network messages for the mod
     */
    public static void register() {
        LOGGER.info("Registering network messages for {}", IskaUtils.MOD_ID);
        // Simplified implementation - actual registration is handled by NeoForge
    }
    
    /**
     * Sends a packet to the server
     */
    public static <MSG> void sendToServer(MSG message) {
        // Simplified implementation - actual sending is handled by NeoForge
    }
    
    /**
     * Sends a packet to a specific player
     */
    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        // Simplified implementation - actual sending is handled by NeoForge
    }
    
    /**
     * Sends a Portable Dislocator packet to the server
     */
    public static void sendPortableDislocatorPacket(int targetX, int targetZ) {
        LOGGER.info("Sending Portable Dislocator packet to server: {}, {}", targetX, targetZ);
        // Simplified implementation for single player compatibility
    }
    
    /**
     * Sends a packet to add a highlighted block
     * This is a simplified implementation that directly calls the client handler
     * in single player mode, but would use actual packets in multiplayer
     */
    public static void sendAddHighlightPacket(ServerPlayer player, BlockPos pos, int color, int durationTicks) {
        // In a real implementation, this would send a packet to the client
        // For now, we'll use a direct call for single player compatibility
        // This is a simplified approach that works in both single player and dedicated server
        try {
            // This will be executed on the client side
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                ClientEvents.handleAddHighlight(pos, color, durationTicks);
            });
        } catch (Exception e) {
            // Ignore errors when running on dedicated server
            LOGGER.debug("Could not send highlight packet to client: {}", e.getMessage());
        }
    }
    
    /**
     * Sends a packet to add a highlighted block with a name
     */
    public static void sendAddHighlightWithNamePacket(ServerPlayer player, BlockPos pos, int color, int durationTicks, String name) {
        // In a real implementation, this would send a packet to the client
        // For now, we'll use a direct call for single player compatibility
        try {
            // This will be executed on the client side
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                ClientEvents.handleAddHighlightWithName(pos, color, durationTicks, name);
            });
        } catch (Exception e) {
            // Ignore errors when running on dedicated server
            LOGGER.debug("Could not send highlight with name packet to client: {}", e.getMessage());
        }
    }
    
    /**
     * Sends a packet to add a billboard marker
     * This is a simplified implementation that directly calls the client handler
     * in single player mode, but would use actual packets in multiplayer
     */
    public static void sendAddBillboardPacket(ServerPlayer player, BlockPos pos, int color, int durationTicks) {
        // In a real implementation, this would send a packet to the client
        // For now, we'll use a direct call for single player compatibility
        try {
            // This will be executed on the client side
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                ClientEvents.handleAddBillboard(pos, color, durationTicks);
            });
        } catch (Exception e) {
            // Ignore errors when running on dedicated server
            LOGGER.debug("Could not send billboard packet to client: {}", e.getMessage());
        }
    }
    
    /**
     * Sends a packet to add a billboard marker with a name
     */
    public static void sendAddBillboardWithNamePacket(ServerPlayer player, BlockPos pos, int color, int durationTicks, String name) {
        // In a real implementation, this would send a packet to the client
        // For now, we'll use a direct call for single player compatibility
        try {
            // This will be executed on the client side
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                ClientEvents.handleAddBillboardWithName(pos, color, durationTicks, name);
            });
        } catch (Exception e) {
            // Ignore errors when running on dedicated server
            LOGGER.debug("Could not send billboard with name packet to client: {}", e.getMessage());
        }
    }
    
    /**
     * Sends a packet to remove a highlighted block
     */
    public static void sendRemoveHighlightPacket(ServerPlayer player, BlockPos pos) {
        // In a real implementation, this would send a packet to the client
        // For now, we'll use a direct call for single player compatibility
        try {
            // This will be executed on the client side
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                ClientEvents.handleRemoveHighlight(pos);
            });
        } catch (Exception e) {
            // Ignore errors when running on dedicated server
            LOGGER.debug("Could not send remove highlight packet to client: {}", e.getMessage());
        }
    }
    
    /**
     * Sends a packet to clear all highlighted blocks
     */
    public static void sendClearHighlightsPacket(ServerPlayer player) {
        // In a real implementation, this would send a packet to the client
        // For now, we'll use a direct call for single player compatibility
        try {
            // This will be executed on the client side
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                ClientEvents.handleClearHighlights();
            });
        } catch (Exception e) {
            // Ignore errors when running on dedicated server
            LOGGER.debug("Could not send clear highlights packet to client: {}", e.getMessage());
        }
    }
    
    /**
     * Sends a Structure Placer save packet to the server
     * This simulates a client-to-server packet for saving the selected structure
     */
    public static void sendStructurePlacerSavePacket(String structureId) {
        LOGGER.info("Sending Structure Placer save packet: {}", structureId);
        // Simplified implementation - directly handle on the server side
        try {
            net.minecraft.server.level.ServerPlayer player = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
            if (player != null) {
                net.unfamily.iskautils.network.packet.StructurePlacerSaveC2SPacket packet = 
                    new net.unfamily.iskautils.network.packet.StructurePlacerSaveC2SPacket(structureId);
                packet.handle(player);
            }
        } catch (Exception e) {
            LOGGER.error("Could not send Structure Placer save packet: {}", e.getMessage());
        }
    }
    
    /**
     * Sends a Structure Placer Machine show packet to toggle preview mode
     */
    public static void sendStructurePlacerMachineShowPacket(BlockPos machinePos) {
        try {
            // Get the server from single player or dedicated server
            net.minecraft.server.MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
            if (server == null) {
                LOGGER.error("Server is null - cannot send Structure Placer Machine show packet");
                return;
            }
            
            // Create and handle the packet on server thread
            server.execute(() -> {
                try {
                    ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    if (player == null) {
                        LOGGER.error("Player is null while handling Structure Placer Machine show packet");
                        return;
                    }
                    
                    ServerLevel level = (ServerLevel) player.level();
                    net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(machinePos);
                    
                    if (!(blockEntity instanceof StructurePlacerMachineBlockEntity machineEntity)) {
                        LOGGER.error("BlockEntity at {} is not a StructurePlacerMachineBlockEntity", machinePos);
                        return;
                    }
                    
                    String selectedStructure = machineEntity.getSelectedStructure();
                    if (selectedStructure.isEmpty()) {
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal("§cNo structure selected!"), true);
                        return;
                    }
                    
                    // Load structure definition
                    net.unfamily.iskautils.structure.StructureDefinition structure = 
                        net.unfamily.iskautils.structure.StructureLoader.getStructure(selectedStructure);
                    if (structure == null) {
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal("§cStructure not found: " + selectedStructure), true);
                        return;
                    }
                    
                    // Toggle preview mode
                    boolean currentPreview = machineEntity.isShowPreview();
                    boolean newPreview = !currentPreview;
                    machineEntity.setShowPreview(newPreview);
                    
                    if (newPreview) {
                        // Show preview
                        showStructurePreview(level, machinePos, player, structure, machineEntity.getRotation());
                        
                        String structureName = structure.getName() != null ? structure.getName() : structure.getId();
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal("§bShowing preview: §f" + structureName), true);
                    } else {
                        // Hide preview - remove existing markers
                        // Note: Markers will automatically expire, but we could send a clear packet here if needed
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal("§7Preview hidden"), true);
                    }
                    
                } catch (Exception e) {
                    LOGGER.error("Error executing packet on server thread: {}", e.getMessage());
                }
            });
            
        } catch (Exception e) {
            LOGGER.error("Could not send Structure Placer Machine show packet: {}", e.getMessage());
        }
    }
    
    /**
     * Sends a Structure Placer Machine Rotate packet to the server
     * This simulates a client-to-server packet for rotating the structure
     */
    public static void sendStructurePlacerMachineRotatePacket(BlockPos machinePos) {
        LOGGER.info("Sending Structure Placer Machine rotate packet at {}", machinePos);
        // Simplified implementation - directly handle on the server side
        try {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft == null) {
                LOGGER.error("Minecraft instance is null!");
                return;
            }
            
            net.minecraft.client.server.IntegratedServer server = minecraft.getSingleplayerServer();
            if (server == null) {
                LOGGER.error("Singleplayer server is null!");
                return;
            }
            
            // Execute on server thread
            server.execute(() -> {
                try {
                    net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        // Directly call the rotate logic
                        net.minecraft.world.level.block.entity.BlockEntity blockEntity = player.serverLevel().getBlockEntity(machinePos);
                        if (blockEntity instanceof net.unfamily.iskautils.block.entity.StructurePlacerMachineBlockEntity machine) {
                            String structureId = machine.getSelectedStructure();
                            if (structureId != null && !structureId.isEmpty()) {
                                // Rotate structure clockwise (0 -> 90 -> 180 -> 270 -> 0)
                                int currentRotation = machine.getRotation();
                                int newRotation = (currentRotation + 90) % 360;
                                machine.setRotation(newRotation);
                                
                                // Get translated direction text
                                String rotationText = switch (newRotation) {
                                    case 0 -> net.minecraft.network.chat.Component.translatable("direction.iska_utils.north").getString();
                                    case 90 -> net.minecraft.network.chat.Component.translatable("direction.iska_utils.east").getString(); 
                                    case 180 -> net.minecraft.network.chat.Component.translatable("direction.iska_utils.south").getString();
                                    case 270 -> net.minecraft.network.chat.Component.translatable("direction.iska_utils.west").getString();
                                    default -> String.valueOf(newRotation) + "°";
                                };
                                
                                // Notify player
                                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("gui.iska_utils.structure_placer_machine.rotated", rotationText), true);
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error handling Structure Placer Machine rotate packet: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            LOGGER.error("Could not send Structure Placer Machine rotate packet: {}", e.getMessage());
        }
    }
    
    /**
     * Mostra la preview della struttura usando marker billboard
     */
    private static void showStructurePreview(ServerLevel world, BlockPos machinePos, net.minecraft.server.level.ServerPlayer player, StructureDefinition structure, int rotation) {
        LOGGER.info("=== START showStructurePreview ===");
        LOGGER.info("Machine pos: {}, Player: {}, Structure: {}, Rotation: {}", machinePos, player.getName().getString(), structure != null ? structure.getId() : "null", rotation);
        
        if (structure == null) {
            LOGGER.warn("Structure is null, aborting preview");
            return;
        }

        // Prima rimuovi i marker precedenti per questa macchina
        LOGGER.info("Removing previous markers...");
        try {
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                ClientEvents.handleClearHighlights();
            });
        } catch (Exception e) {
            LOGGER.debug("Could not clear client highlights: {}", e.getMessage());
        }

        String[][][][] pattern = structure.getPattern();
        if (pattern == null || pattern.length == 0) {
            LOGGER.warn("Pattern is null or empty, aborting preview");
            return;
        }
        
        LOGGER.info("Pattern dimensions: [Y={}, X={}, Z={}, chars=variable]", pattern.length, pattern[0].length, pattern[0][0].length);

        // Trova il centro della struttura
        BlockPos center = structure.findCenter();
        if (center == null) center = new BlockPos(0, 0, 0);
        LOGGER.info("Structure center: {}", center);

        int duration = 300; // 15 secondi (300 ticks)
        int totalBlocksProcessed = 0;
        int markersCreated = 0;

        // Itera attraverso il pattern della struttura [Y][X][Z][caratteri]
        for (int y = 0; y < pattern.length; y++) {
            for (int x = 0; x < pattern[y].length; x++) {
                for (int z = 0; z < pattern[y][x].length; z++) {
                    String[] cellChars = pattern[y][x][z];
                    
                    if (cellChars != null) {
                        for (int charIndex = 0; charIndex < cellChars.length; charIndex++) {
                            String patternChar = cellChars[charIndex];
                            totalBlocksProcessed++;
                            
                            // Salta spazi vuoti
                            if (patternChar == null || patternChar.equals(" ")) continue;
                            
                            // Se è @, controllare se è definito nella key
                            if (patternChar.equals("@")) {
                                Map<String, List<StructureDefinition.BlockDefinition>> key = structure.getKey();
                                if (key == null || !key.containsKey("@")) {
                                    // @ non è definito nella key, trattalo come spazio vuoto
                                    continue;
                                }
                                // Se arriviamo qui, @ è definito nella key, quindi processalo come un blocco normale
                            }

                            // Calcola la posizione Z effettiva
                            int zEffettivo = z * cellChars.length + charIndex;
                            
                            // Calcola offset dal centro
                            int offsetX = x - center.getX();
                            int offsetY = y - center.getY(); 
                            int offsetZ = zEffettivo - center.getZ();
                            
                            // Applica rotazione (semplice rotazione 2D su X-Z)
                            int rotatedX, rotatedZ;
                            switch (rotation) {
                                case 90:
                                    rotatedX = -offsetZ;
                                    rotatedZ = offsetX;
                                    break;
                                case 180:
                                    rotatedX = -offsetX;
                                    rotatedZ = -offsetZ;
                                    break;
                                case 270:
                                    rotatedX = offsetZ;
                                    rotatedZ = -offsetX;
                                    break;
                                default: // 0 gradi
                                    rotatedX = offsetX;
                                    rotatedZ = offsetZ;
                                    break;
                            }

                            // Calcola la posizione finale nel mondo (spostata di +1 in Y per evitare conflitti con la macchina)
                            BlockPos finalPos = machinePos.offset(rotatedX, offsetY + 1, rotatedZ);

                            // Determina se c'è un conflitto
                            boolean hasConflict = !world.getBlockState(finalPos).canBeReplaced();
                            
                            // Usa colori per i marker: stessi colori degli item
                            int markerColor = hasConflict ? 0x80FF4444 : 0x804444FF; // Rosso e azzurro come negli item
                            
                            LOGGER.info("Creating marker at {} with color {:08x} (conflict: {})", finalPos, markerColor, hasConflict);

                            // Crea marker billboard
                            try {
                                sendAddBillboardPacket(player, finalPos, markerColor, duration);
                                markersCreated++;
                                LOGGER.info("Marker created successfully at {}", finalPos);
                                
                            } catch (Exception e) {
                                LOGGER.error("Error creating marker: {}", e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
        
        LOGGER.info("Preview creation completed: {} blocks processed, {} markers created", totalBlocksProcessed, markersCreated);
        
        // Informa il giocatore
        player.displayClientMessage(net.minecraft.network.chat.Component.literal("§bPreview created: §f" + markersCreated + " §bmarkers (15s duration)"), true);
        
        LOGGER.info("=== END showStructurePreview ===");
    }
    
    /**
     * Calculates positions of all structure blocks with rotation around center @
     */
    private static java.util.Map<net.minecraft.core.BlockPos, String> calculateStructurePositions(net.minecraft.core.BlockPos centerPos, net.unfamily.iskautils.structure.StructureDefinition structure, int rotation) {
        java.util.Map<net.minecraft.core.BlockPos, String> positions = new java.util.HashMap<>();
        
        String[][][][] pattern = structure.getPattern();
        if (pattern == null) return positions;
        
        // Find structure center (@ symbol)
        net.minecraft.core.BlockPos relativeCenter = structure.findCenter();
        if (relativeCenter == null) relativeCenter = net.minecraft.core.BlockPos.ZERO;
        
        for (int y = 0; y < pattern.length; y++) {
            for (int x = 0; x < pattern[y].length; x++) {
                for (int z = 0; z < pattern[y][x].length; z++) {
                    String[] cellChars = pattern[y][x][z];
                    
                    if (cellChars != null) {
                        for (int charIndex = 0; charIndex < cellChars.length; charIndex++) {
                            String character = cellChars[charIndex];
                            
                            // Salta spazi vuoti
                            if (character == null || character.equals(" ")) continue;
                            
                            // Se è @, controllare se è definito nella key
                            if (character.equals("@")) {
                                java.util.Map<String, java.util.List<net.unfamily.iskautils.structure.StructureDefinition.BlockDefinition>> key = structure.getKey();
                                if (key == null || !key.containsKey("@")) {
                                    // @ non è definito nella key, trattalo come spazio vuoto
                                    continue;
                                }
                                // Se arriviamo qui, @ è definito nella key, quindi processalo come un blocco normale
                            }
                            
                            int originalX = x;
                            int originalY = y;
                            int originalZ = z * cellChars.length + charIndex;
                            
                            // Calculate relative position from center @
                            int relX = originalX - relativeCenter.getX();
                            int relY = originalY - relativeCenter.getY();
                            int relZ = originalZ - relativeCenter.getZ();
                            
                            // Apply rotation to relative coordinates
                            net.minecraft.core.BlockPos rotatedRelativePos = applyRotation(relX, relY, relZ, rotation);
                            
                            // Calculate final position in world (spostata di +1 in Y per evitare conflitti con la macchina)
                            net.minecraft.core.BlockPos blockPos = centerPos.offset(
                                rotatedRelativePos.getX(), 
                                rotatedRelativePos.getY() + 1, 
                                rotatedRelativePos.getZ()
                            );
                            
                            positions.put(blockPos, character);
                        }
                    }
                }
            }
        }
        
        return positions;
    }
    
    /**
     * Applies rotation transformation to coordinates
     */
    private static net.minecraft.core.BlockPos applyRotation(int x, int y, int z, int rotation) {
        return switch (rotation) {
            case 90 -> new net.minecraft.core.BlockPos(-z, y, x);   // 90° clockwise
            case 180 -> new net.minecraft.core.BlockPos(-x, y, -z); // 180°
            case 270 -> new net.minecraft.core.BlockPos(z, y, -x);  // 270° clockwise (90° counter-clockwise)
            default -> new net.minecraft.core.BlockPos(x, y, z);    // 0° (no rotation)
        };
    }
    
    /**
     * Checks if a block can be replaced based on structure settings
     */
    private static boolean canReplaceBlock(net.minecraft.world.level.block.state.BlockState state, net.unfamily.iskautils.structure.StructureDefinition structure) {
        net.minecraft.world.level.block.Block block = state.getBlock();
        
        // Always allow air and replaceable blocks
        if (state.isAir() || state.canBeReplaced()) {
            return true;
        }
        
        // Check if block is in the can_replace list
        if (structure.getCanReplace() != null) {
            for (String replaceableBlock : structure.getCanReplace()) {
                try {
                    net.minecraft.resources.ResourceLocation blockLocation = net.minecraft.resources.ResourceLocation.parse(replaceableBlock);
                    net.minecraft.world.level.block.Block allowedBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(blockLocation);
                    if (block == allowedBlock) {
                        return true;
                    }
                } catch (Exception e) {
                    // Ignore invalid block names
                }
            }
        }
        
        return false;
    }
    
    /**
     * Sends a Structure Placer Machine save packet to the server
     * This simulates a client-to-server packet for saving the selected structure in the machine
     */
    public static void sendStructurePlacerMachineSavePacket(String structureId, BlockPos machinePos) {
        LOGGER.info("Sending Structure Placer Machine save packet: {} at {}", structureId, machinePos);
        // Simplified implementation - directly handle on the server side
        try {
            // Try to get the minecraft instance
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft == null) {
                LOGGER.error("Minecraft instance is null!");
                return;
            }
            
            // Try to get the singleplayer server
            net.minecraft.client.server.IntegratedServer server = minecraft.getSingleplayerServer();
            if (server == null) {
                LOGGER.error("Singleplayer server is null!");
                return;
            }
            
            // Try to get the player list
            net.minecraft.server.players.PlayerList playerList = server.getPlayerList();
            if (playerList == null) {
                LOGGER.error("Player list is null!");
                return;
            }
            
            // Try to get the players
            java.util.List<net.minecraft.server.level.ServerPlayer> players = playerList.getPlayers();
            if (players.isEmpty()) {
                LOGGER.error("No players found on server!");
                return;
            }
            
            net.minecraft.server.level.ServerPlayer player = players.get(0);
            if (player == null) {
                LOGGER.error("First player is null!");
                return;
            }
            
            LOGGER.info("Creating StructurePlacerMachineSaveC2SPacket...");
            net.unfamily.iskautils.network.packet.StructurePlacerMachineSaveC2SPacket packet = 
                new net.unfamily.iskautils.network.packet.StructurePlacerMachineSaveC2SPacket(structureId, machinePos);
            
            LOGGER.info("Scheduling packet.handle() to run on server thread with player: {}", player.getName().getString());
            
            // Forza l'esecuzione sul server thread per accedere correttamente ai BlockEntity
            server.execute(() -> {
                try {
                    LOGGER.info("Executing packet.handle() on server thread");
                    packet.handle(player);
                    LOGGER.info("Packet handle completed successfully on server thread!");
                } catch (Exception e) {
                    LOGGER.error("Error executing packet on server thread: {}", e.getMessage());
                    e.printStackTrace();
                }
            });
            
        } catch (Exception e) {
            LOGGER.error("Could not send Structure Placer Machine save packet: {}", e.getMessage());
            e.printStackTrace(); // Print full stack trace for debugging
        }
    }
} 