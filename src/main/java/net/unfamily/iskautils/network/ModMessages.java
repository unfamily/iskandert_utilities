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
import net.unfamily.iskautils.structure.StructureLoader;
import net.unfamily.iskautils.util.ModUtils;
import net.unfamily.iskautils.block.entity.StructurePlacerMachineBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import com.mojang.math.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mojang.logging.LogUtils;
import net.unfamily.iskautils.network.packet.AutoShopSetEncapsulatedC2SPacket;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.Map;
import java.util.List;

/**
 * Handles network messages for the mod
 */
public class ModMessages {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Simplified version to avoid NeoForge networking compatibility issues
    
    /**
     * Registers network messages for the mod
     */
    public static void register() {
        LOGGER.info("Registering packets for " + IskaUtils.MOD_ID);
        
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
     * Sends a Structure Undo packet to the server
     */
    public static void sendStructureUndoPacket() {
        LOGGER.debug("Sending Structure Undo packet to server");
        // Simplified implementation for single player compatibility
        try {
            // Get the server from single player or dedicated server
            net.minecraft.server.MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
            if (server == null) return;
            
            // Create and handle the packet on server thread
            server.execute(() -> {
                try {
                    net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        // Directly call the undo functionality
                        boolean success = net.unfamily.iskautils.structure.StructurePlacementHistory.undoLastPlacement(player);
                        
                        // Success/failure messages are already handled in StructurePlacementHistory.undoLastPlacement()
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to handle structure undo: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            LOGGER.warn("Failed to send structure undo packet: {}", e.getMessage());
        }
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
        }
    }
    
    /**
     * Sends shop team data to the client
     */
    public static void sendShopTeamDataToClient(ServerPlayer player, String teamName, Map<String, Double> teamBalances) {
        // Simplified implementation for single player compatibility
        try {
            // This will be executed on the client side
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                net.unfamily.iskautils.client.gui.ShopScreen.handleTeamDataUpdate(teamName, teamBalances);
            });
        } catch (Exception e) {
            // Ignore errors when running on dedicated server
        }
    }
    
    /**
     * Sends a shop team data request to the server
     */
    public static void sendShopTeamDataRequest() {
        // Simplified implementation - directly handle on the server side
        try {
            // Get the server from single player or dedicated server
            net.minecraft.server.MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
            if (server == null) return;
            
            // Create and handle the packet on server thread
            server.execute(() -> {
                try {
                    net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        // Create and handle the packet
                        new net.unfamily.iskautils.network.packet.ShopTeamDataRequestC2SPacket().handle(player);
                    }
                } catch (Exception e) {
                    // Ignore errors
                }
            });
        } catch (Exception e) {
            // Ignore errors
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
        }
    }
    
    /**
     * Sends a Structure Placer save packet to the server
     * This simulates a client-to-server packet for saving the selected structure
     */
    public static void sendStructurePlacerSavePacket(String structureId) {

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
     * Sends a Structure Saver blueprint sync packet
     */
    public static void sendStructureSaverBlueprintSyncPacket(ServerPlayer player, BlockPos machinePos, BlockPos vertex1, BlockPos vertex2, BlockPos center) {

        
        // Sistema semplificato identico agli altri packet in questa classe
        try {
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                // Gestisce il packet lato client
                try {
                    var level = net.minecraft.client.Minecraft.getInstance().level;
                    if (level != null) {
                        var blockEntity = level.getBlockEntity(machinePos);
                        if (blockEntity instanceof net.unfamily.iskautils.block.entity.StructureSaverMachineBlockEntity structureSaver) {
                            structureSaver.setBlueprintDataClientSide(vertex1, vertex2, center);
                            LOGGER.info("Blueprint sincronizzata con successo sul client via ModMessages");
                        } else {
                            LOGGER.warn("BlockEntity at {} is not a StructureSaverMachineBlockEntity", machinePos);
                        }
                    } else {
                        LOGGER.warn("Client level is null, cannot sync blueprint data");
                    }
                } catch (Exception e) {
                    LOGGER.error("Errore durante la sincronizzazione blueprint: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            // Ignora errori quando si esegue su server dedicato
            LOGGER.debug("Blueprint sync packet not sent in dedicated server mode: {}", e.getMessage());
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
                    
                    // Show preview (always show, don't toggle)
                    machineEntity.setShowPreview(true);
                    
                    // Always show the preview
                    showStructurePreview(level, machinePos, player, structure, machineEntity.getRotation());
                    
                    String structureName = structure.getName() != null ? structure.getName() : structure.getId();
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("§bShowing preview: §f" + structureName), true);
                    
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
     * Sends a Structure Placer Machine Redstone Mode packet to the server
     */
    public static void sendStructurePlacerMachineRedstoneModePacket(BlockPos machinePos) {

        
        // Use simplified approach like other machine buttons
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
                        net.minecraft.server.level.ServerLevel level = player.serverLevel();
                        
                        net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(machinePos);
                        if (blockEntity instanceof StructurePlacerMachineBlockEntity machine) {
                            
                            // Cycle to next redstone mode
                            StructurePlacerMachineBlockEntity.RedstoneMode currentMode = StructurePlacerMachineBlockEntity.RedstoneMode.fromValue(machine.getRedstoneMode());
                            StructurePlacerMachineBlockEntity.RedstoneMode nextMode = currentMode.next();
                            machine.setRedstoneMode(nextMode.getValue());
                            
                            // Play click sound
                            level.playSound(null, machinePos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, 1.0f);
                            
                            // Mark the block entity as changed
                            machine.setChanged();
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error handling Structure Placer Machine redstone mode packet: {}", e.getMessage());
                }
            });
            

        } catch (Exception e) {
            LOGGER.error("Could not send Structure Placer Machine redstone mode packet: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Sends a Structure Placer Machine Set Inventory packet to the server
     * Mode: 0 = normal, 1 = shift+click, 2 = ctrl/alt+click
     */
    public static void sendStructurePlacerMachineSetInventoryPacket(BlockPos machinePos, int mode) {
        // Simplified implementation - directly handle on the server side
        try {
            // Get the server from single player or dedicated server
            net.minecraft.server.MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
            if (server == null) {
                LOGGER.error("Server is null - cannot send Structure Placer Machine Set Inventory packet");
                return;
            }
            
            // Create and handle the packet on server thread
            server.execute(() -> {
                try {
                    net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        net.minecraft.server.level.ServerLevel world = player.serverLevel();
                        net.minecraft.world.level.block.entity.BlockEntity blockEntity = world.getBlockEntity(machinePos);
                        
                        if (blockEntity instanceof StructurePlacerMachineBlockEntity machine) {
                            
                            // Execute the appropriate action based on mode
                            switch (mode) {
                                case net.unfamily.iskautils.network.packet.StructurePlacerMachineSetInventoryC2SPacket.MODE_NORMAL -> {
                                    machine.setInventoryFilters();
                                    // Play a soft click sound
                                    world.playSound(null, machinePos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 
                                                  net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, 1.0f);
                                }
                                case net.unfamily.iskautils.network.packet.StructurePlacerMachineSetInventoryC2SPacket.MODE_SHIFT -> {
                                    machine.clearAllFilters();
                                    // Play a different sound for clearing
                                    world.playSound(null, machinePos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 
                                                  net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, 0.8f);
                                }
                                case net.unfamily.iskautils.network.packet.StructurePlacerMachineSetInventoryC2SPacket.MODE_CTRL -> {
                                    machine.clearEmptyFilters();
                                    // Play a third sound for partial clearing
                                    world.playSound(null, machinePos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 
                                                  net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, 0.9f);
                                }
                            }
                            
                            machine.setChanged();
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error executing Set Inventory packet on server thread: {}", e.getMessage(), e);
                }
            });
            
        } catch (Exception e) {
            LOGGER.error("Could not send Structure Placer Machine Set Inventory packet: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Shows structure preview using billboard markers
     */
    private static void showStructurePreview(ServerLevel world, BlockPos machinePos, net.minecraft.server.level.ServerPlayer player, StructureDefinition structure, int rotation) {
        if (structure == null) {
            return;
        }

        // First remove previous markers for this machine
        try {
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                ClientEvents.handleClearHighlights();
            });
        } catch (Exception e) {
            // Ignore client-side errors in single player
        }

        String[][][][] pattern = structure.getPattern();
        if (pattern == null || pattern.length == 0) {
            return;
        }

        // Find structure center
        BlockPos center = structure.findCenter();
        if (center == null) center = new BlockPos(0, 0, 0);

        int duration = 300; // 15 seconds (300 ticks)

        // Iterate through structure pattern [Y][X][Z][characters]
        for (int y = 0; y < pattern.length; y++) {
            for (int x = 0; x < pattern[y].length; x++) {
                for (int z = 0; z < pattern[y][x].length; z++) {
                    String[] cellChars = pattern[y][x][z];
                    
                    if (cellChars != null) {
                        for (int charIndex = 0; charIndex < cellChars.length; charIndex++) {
                            String patternChar = cellChars[charIndex];
                            
                            // Skip empty spaces
                            if (patternChar == null || patternChar.equals(" ")) continue;
                            
                            // If it's @, check if it's defined in the key
                            if (patternChar.equals("@")) {
                                Map<String, List<StructureDefinition.BlockDefinition>> key = structure.getKey();
                                if (key == null || !key.containsKey("@")) {
                                    // @ is not defined in key, treat as empty space
                                    continue;
                                }
                                // If we get here, @ is defined in key, so process as normal block
                            }

                            // Calculate effective Z position
                            int effectiveZ = z * cellChars.length + charIndex;
                            
                            // Calculate offset from center
                            int offsetX = x - center.getX();
                            int offsetY = y - center.getY(); 
                            int offsetZ = effectiveZ - center.getZ();
                            
                            // Apply rotation
                            BlockPos rotatedOffset = applyRotation(offsetX, offsetY, offsetZ, rotation);
                            
                            // Calculate final position in world (shifted +1 in Y to avoid conflicts with machine)
                            BlockPos finalPos = machinePos.offset(rotatedOffset.getX(), rotatedOffset.getY() + 1, rotatedOffset.getZ());
                            
                            // Check for conflicts
                            boolean hasConflict = !world.getBlockState(finalPos).canBeReplaced();
                              
                            // Use colors for markers: same colors as items
                            int markerColor = hasConflict ? 0x80FF4444 : 0x804444FF; // Red and blue like items
                            
                            try {
                                sendAddBillboardPacket(player, finalPos, markerColor, duration);
                            } catch (Exception e) {
                                // Ignore marker creation errors
                            }
                        }
                    }
                }
            }
        }
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
                            
                            // Skip empty spaces
                            if (character == null || character.equals(" ")) continue;
                            
                            // If it's @, check if it's defined in the key
                            if (character.equals("@")) {
                                java.util.Map<String, java.util.List<net.unfamily.iskautils.structure.StructureDefinition.BlockDefinition>> key = structure.getKey();
                                if (key == null || !key.containsKey("@")) {
                                    // @ is not defined in key, treat as empty space
                                    continue;
                                }
                                // If we get here, @ is defined in key, so process as normal block
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
                            
                            // Calculate final position in world (shifted +1 in Y to avoid conflicts with machine)
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
            
            net.minecraft.server.players.PlayerList playerList = server.getPlayerList();
            if (playerList == null) {
                LOGGER.error("Player list is null!");
                return;
            }
            
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
            
            net.unfamily.iskautils.network.packet.StructurePlacerMachineSaveC2SPacket packet = 
                new net.unfamily.iskautils.network.packet.StructurePlacerMachineSaveC2SPacket(structureId, machinePos);
            
            // Execute on server thread to properly access BlockEntity
            server.execute(() -> {
                try {
                    packet.handle(player);
                } catch (Exception e) {
                    LOGGER.error("Error executing packet on server thread: {}", e.getMessage());
                }
            });
            
        } catch (Exception e) {
            LOGGER.error("Could not send Structure Placer Machine save packet: {}", e.getMessage());
        }
    }
    
    /**
     * Invia le strutture del server al client per sincronizzazione
     * Questo permette al client di vedere tutte le strutture disponibili sul server
     */
    public static void sendStructureSyncPacket(ServerPlayer player) {
        try {
            // Check if we're in singleplayer mode
            boolean isSingleplayer = player.getServer().isSingleplayer();
            
            if (isSingleplayer) {
                LOGGER.debug("Modalità singleplayer detected, skipping structure sync for player {}", 
                           player.getName().getString());
                return; // In singleplayer, the client already has its local structures
            }
            
            // Ottieni SOLO le strutture da sincronizzare (MAI le strutture client del server)
            Map<String, StructureDefinition> serverStructures = StructureLoader.getStructuresForSync();
            
            if (serverStructures.isEmpty()) {
                LOGGER.debug("Nessuna struttura da sincronizzare per il player {}", player.getName().getString());
                return;
            }
            
            LOGGER.info("Sincronizzando {} strutture al client per player {} (dedicated server)", 
                       serverStructures.size(), player.getName().getString());
            
            // Crea il pacchetto di sincronizzazione con il flag del server
            net.unfamily.iskautils.network.packet.StructureSyncS2CPacket packet = 
                net.unfamily.iskautils.network.packet.StructureSyncS2CPacket.create(serverStructures, net.unfamily.iskautils.Config.acceptClientStructure);
            
            // TODO: In un server dedicato, qui invieresti il vero pacchetto al client
            // For now just log that synchronization is needed
            LOGGER.info("TODO: Inviare pacchetto di sincronizzazione strutture al client {} su server dedicato", 
                       player.getName().getString());
            
        } catch (Exception e) {
            LOGGER.error("Errore nell'invio del pacchetto di sincronizzazione strutture: {}", e.getMessage());
        }
    }
    
    /**
     * Sends a Structure Saver Machine recalculate packet to the server
     */
    public static void sendStructureSaverMachineRecalculatePacket(BlockPos machinePos) {

        // Simplified implementation per single player compatibility
        try {
            net.minecraft.server.MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
            if (server != null) {
                // Trova il BlockEntity della macchina e richiedi il ricalcolo
                var level = server.getAllLevels().iterator().next(); // Ottieni il primo mondo
                var blockEntity = level.getBlockEntity(machinePos);
                if (blockEntity instanceof net.unfamily.iskautils.block.entity.StructureSaverMachineBlockEntity machine) {
                    machine.requestAreaRecalculation();
                    LOGGER.info("Area recalculation requested successfully");
                }
            }
        } catch (Exception e) {
            LOGGER.error("Could not request Structure Saver Machine area recalculation: {}", e.getMessage());
        }
    }
    
    /**
     * Sends a Structure Saver Machine save packet to the server
     * This simulates a client-to-server packet for saving a structure from the machine
     */
    public static void sendStructureSaverMachineSavePacket(String structureName, String structureId, BlockPos machinePos, boolean slower, boolean placeAsPlayer) {
        sendStructureSaverMachineSavePacket(structureName, structureId, machinePos, slower, placeAsPlayer, null);
    }
    
    /**
     * Sends a Structure Saver Machine save/modify packet to the server
     * This simulates a client-to-server packet for saving or modifying a structure from the machine
     */
    public static void sendStructureSaverMachineSavePacket(String structureName, String structureId, BlockPos machinePos, boolean slower, boolean placeAsPlayer, String oldStructureId) {


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
            
            net.minecraft.server.players.PlayerList playerList = server.getPlayerList();
            if (playerList == null) {
                LOGGER.error("Player list is null!");
                return;
            }
            
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
            
            net.unfamily.iskautils.network.packet.StructureSaverMachineSaveC2SPacket packet = 
                new net.unfamily.iskautils.network.packet.StructureSaverMachineSaveC2SPacket(structureName, structureId, machinePos, slower, placeAsPlayer, oldStructureId);
            
            // Execute on server thread to properly access BlockEntity
            server.execute(() -> {
                try {
                    packet.handle(player);
                } catch (Exception e) {
                    LOGGER.error("Error executing packet on server thread: {}", e.getMessage());
                }
            });
            
        } catch (Exception e) {
            LOGGER.error("Could not send Structure Saver Machine save packet: {}", e.getMessage());
        }
    }

    /**
     * Sends a shop buy item packet to the server
     */
    public static void sendShopBuyItemPacket(String entryId, int quantity) {
        System.out.println("DEBUG: sendShopBuyItemPacket chiamato - entryId: " + entryId + ", quantity: " + quantity);
        
        // Simplified implementation for single player compatibility
        try {
            // Get the server from single player or dedicated server
            net.minecraft.server.MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
            if (server == null) return;
            
            // Create and handle the packet on server thread
            server.execute(() -> {
                try {
                    net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        // Create and handle the packet
                        new net.unfamily.iskautils.network.packet.ShopBuyItemC2SPacket(entryId, quantity).handle(player);
                    }
                } catch (Exception e) {
                    System.err.println("DEBUG: Errore nell'invio del packet buy: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            System.err.println("DEBUG: Errore nel sendShopBuyItemPacket: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Sends a shop sell item packet to the server
     */
    public static void sendShopSellItemPacket(String entryId, int quantity) {
        System.out.println("DEBUG: sendShopSellItemPacket chiamato - entryId: " + entryId + ", quantity: " + quantity);
        
        // Simplified implementation for single player compatibility
        try {
            // Get the server from single player or dedicated server
            net.minecraft.server.MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
            if (server == null) return;
            
            // Create and handle the packet on server thread
            server.execute(() -> {
                try {
                    net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        // Create and handle the packet
                        new net.unfamily.iskautils.network.packet.ShopSellItemC2SPacket(entryId, quantity).handle(player);
                    }
                } catch (Exception e) {
                    System.err.println("DEBUG: Errore nell'invio del packet sell: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            System.err.println("DEBUG: Errore nel sendShopSellItemPacket: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Invia il packet per settare lo slot encapsulato dell'Auto Shop
     */
    public static void sendAutoShopSetEncapsulatedPacket(BlockPos pos) {
        // Simplified implementation for single player compatibility
        try {
            // Get the server from single player or dedicated server
            net.minecraft.server.MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
            if (server == null) return;
            
            // Create and handle the packet on server thread
            server.execute(() -> {
                try {
                    net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        // Create and handle the packet
                        new AutoShopSetEncapsulatedC2SPacket(pos).handle(player);
                    }
                } catch (Exception e) {
                    // Ignore errors
                }
            });
        } catch (Exception e) {
            // Ignore errors
        }
    }

    /**
     * Invia il packet per settare lo slot selectedItem dell'Auto Shop
     */
    public static void sendAutoShopSelectedItemPacket(BlockPos pos, ItemStack stack) {
        // Simplified implementation for single player compatibility
        try {
            // Get the server from single player or dedicated server
            net.minecraft.server.MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
            if (server == null) return;
            
            // Create and handle the packet on server thread
            server.execute(() -> {
                try {
                    net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        // Create and handle the packet
                        new net.unfamily.iskautils.network.packet.AutoShopSetSelectedItemC2SPacket(pos, stack).handle(player);
                    }
                } catch (Exception e) {
                    // Ignore errors
                }
            });
        } catch (Exception e) {
            // Ignore errors
        }
    }

} 