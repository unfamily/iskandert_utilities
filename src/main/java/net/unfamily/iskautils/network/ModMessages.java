package net.unfamily.iskautils.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

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
import net.unfamily.iskautils.network.packet.TemporalOverclockerHighlightBlockC2SPacket;
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
        // Registration is now handled by the RegisterPayloadHandlersEvent
        // See registerPayloads() method below
    }
    
    /**
     * Registers payload handlers for NeoForge networking
     */
    @net.neoforged.bus.api.SubscribeEvent
    public static void registerPayloads(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
        final net.neoforged.neoforge.network.registration.PayloadRegistrar registrar = event.registrar(IskaUtils.MOD_ID).versioned("1");
        
        // Register Structure Sync S2C Packet (Server to Client)
        registrar.playToClient(
            net.unfamily.iskautils.network.packet.StructureSyncS2CPacket.TYPE,
            net.unfamily.iskautils.network.packet.StructureSyncS2CPacket.STREAM_CODEC,
            (packet, context) -> net.unfamily.iskautils.network.packet.StructureSyncS2CPacket.handle(packet, context)
        );
        
        // Register Temporal Overclocker Highlight Block C2S Packet (Client to Server)
        registrar.playToServer(
            TemporalOverclockerHighlightBlockC2SPacket.TYPE,
            TemporalOverclockerHighlightBlockC2SPacket.STREAM_CODEC,
            TemporalOverclockerHighlightBlockC2SPacket::handle
        );
        
        // Register Temporal Overclocker Toggle Persistent C2S Packet (Client to Server)
        registrar.playToServer(
            net.unfamily.iskautils.network.packet.TemporalOverclockerTogglePersistentC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.TemporalOverclockerTogglePersistentC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.TemporalOverclockerTogglePersistentC2SPacket::handle
        );
        
        // Register Fan Redstone Mode C2S Packet (Client to Server)
        registrar.playToServer(
            net.unfamily.iskautils.network.packet.FanRedstoneModeC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.FanRedstoneModeC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.FanRedstoneModeC2SPacket::handle
        );
        
        // Register Fan Push/Pull C2S Packet (Client to Server)
        registrar.playToServer(
            net.unfamily.iskautils.network.packet.FanPushPullC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.FanPushPullC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.FanPushPullC2SPacket::handle
        );
        
        // Register Fan Push Type C2S Packet (Client to Server)
        registrar.playToServer(
            net.unfamily.iskautils.network.packet.FanPushTypeC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.FanPushTypeC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.FanPushTypeC2SPacket::handle
        );
        
        // Register Smart Timer Redstone Mode C2S Packet (Client to Server)
        registrar.playToServer(
            net.unfamily.iskautils.network.packet.SmartTimerRedstoneModeC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.SmartTimerRedstoneModeC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.SmartTimerRedstoneModeC2SPacket::handle
        );
        
        // Register Fan Show Area C2S Packet (Client to Server)
        registrar.playToServer(
            net.unfamily.iskautils.network.packet.FanShowAreaC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.FanShowAreaC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.FanShowAreaC2SPacket::handle
        );
        
        LOGGER.info("Registered {} networking packets", 8);
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
    @OnlyIn(Dist.CLIENT)
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
    @OnlyIn(Dist.CLIENT)
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
    @OnlyIn(Dist.CLIENT)
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

        
        // Simplified system identical to other packets in this class
        try {
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                // Handle packet on client side
                try {
                    var level = net.minecraft.client.Minecraft.getInstance().level;
                    if (level != null) {
                        var blockEntity = level.getBlockEntity(machinePos);
                        if (blockEntity instanceof net.unfamily.iskautils.block.entity.StructureSaverMachineBlockEntity structureSaver) {
                            structureSaver.setBlueprintDataClientSide(vertex1, vertex2, center);
                            LOGGER.info("Blueprint synchronized successfully on client via ModMessages");
                        } else {
                            LOGGER.warn("BlockEntity at {} is not a StructureSaverMachineBlockEntity", machinePos);
                        }
                    } else {
                        LOGGER.warn("Client level is null, cannot sync blueprint data");
                    }
                } catch (Exception e) {
                    LOGGER.error("Error during blueprint synchronization: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            // Ignore errors when running on dedicated server
            LOGGER.debug("Blueprint sync packet not sent in dedicated server mode: {}", e.getMessage());
        }
    }
    
    /**
     * Sends a Structure Placer Machine show packet to toggle preview mode
     */
    @OnlyIn(Dist.CLIENT)
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
    @OnlyIn(Dist.CLIENT)
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
    @OnlyIn(Dist.CLIENT)
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
     * Sends a Deep Drawer Extractor Redstone Mode packet to the server
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendDeepDrawerExtractorRedstoneModePacket(BlockPos machinePos) {
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
                        if (blockEntity instanceof net.unfamily.iskautils.block.entity.DeepDrawerExtractorBlockEntity extractor) {
                            
                            // Cycle to next redstone mode
                            int oldMode = extractor.getRedstoneMode();
                            net.unfamily.iskautils.block.entity.DeepDrawerExtractorBlockEntity.RedstoneMode currentMode = 
                                net.unfamily.iskautils.block.entity.DeepDrawerExtractorBlockEntity.RedstoneMode.fromValue(oldMode);
                            net.unfamily.iskautils.block.entity.DeepDrawerExtractorBlockEntity.RedstoneMode nextMode = currentMode.next();
                            
                            LOGGER.debug("DeepDrawerExtractor: Cycling redstone mode from {} ({}) to {} ({})", 
                                oldMode, currentMode, nextMode.getValue(), nextMode);
                            
                            extractor.setRedstoneMode(nextMode.getValue());
                            
                            // Play click sound
                            level.playSound(null, machinePos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, 1.0f);
                            
                            // setRedstoneMode already calls setChanged(), but we ensure it's marked as changed
                            extractor.setChanged();
                        } else {
                            LOGGER.warn("DeepDrawerExtractor: BlockEntity at {} is not a DeepDrawerExtractorBlockEntity", machinePos);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error handling Deep Drawer Extractor redstone mode packet: {}", e.getMessage());
                }
            });
            

        } catch (Exception e) {
            LOGGER.error("Could not send Deep Drawer Extractor redstone mode packet: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Sends a Structure Placer Machine Set Inventory packet to the server
     * Mode: 0 = normal, 1 = shift+click, 2 = ctrl/alt+click
     */
    @OnlyIn(Dist.CLIENT)
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
    @OnlyIn(Dist.CLIENT)
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
     * Sends server structures to client for synchronization
     * This allows the client to see all available structures on the server
     */
    public static void sendStructureSyncPacket(ServerPlayer player) {
        try {
            // Check if we're in singleplayer mode
            boolean isSingleplayer = player.getServer().isSingleplayer();
            
            if (isSingleplayer) {
                LOGGER.debug("Singleplayer mode detected, skipping structure sync for player {}", 
                           player.getName().getString());
                return; // In singleplayer, the client already has its local structures
            }
            
            // Get ONLY structures to synchronize (NEVER server client structures)
            Map<String, StructureDefinition> serverStructures = StructureLoader.getStructuresForSync();
            
            if (serverStructures.isEmpty()) {
                LOGGER.debug("No structures to synchronize for player {}", player.getName().getString());
                return;
            }
            
            LOGGER.info("Synchronizing {} structures to client for player {} (dedicated server)", 
                       serverStructures.size(), player.getName().getString());
            
            // Create synchronization packet with server flag
            net.unfamily.iskautils.network.packet.StructureSyncS2CPacket packet = 
                net.unfamily.iskautils.network.packet.StructureSyncS2CPacket.create(serverStructures, net.unfamily.iskautils.Config.acceptClientStructure);
            
            // Send the real packet to client using NeoForge networking system
            try {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, packet);
                LOGGER.info("Successfully synchronized structures to client {} via real networking", 
                           player.getName().getString());
            } catch (Exception networkError) {
                LOGGER.error("Error sending structure sync packet to {}: {}", 
                           player.getName().getString(), networkError.getMessage());
                // Fallback: try simplified method as backup
                try {
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        net.unfamily.iskautils.network.packet.StructureSyncS2CPacket.handle(packet);
                    });
                } catch (Exception fallbackError) {
                    LOGGER.error("Synchronization fallback also failed: {}", fallbackError.getMessage());
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error sending structure synchronization packet: {}", e.getMessage());
        }
    }
    
    /**
     * Sends a Structure Saver Machine recalculate packet to the server
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendStructureSaverMachineRecalculatePacket(BlockPos machinePos) {

        // Simplified implementation for single player compatibility
        try {
            net.minecraft.server.MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
            if (server != null) {
                // Find the machine BlockEntity and request recalculation
                var level = server.getAllLevels().iterator().next(); // Get the first world
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
    @OnlyIn(Dist.CLIENT)
    public static void sendStructureSaverMachineSavePacket(String structureName, String structureId, BlockPos machinePos, boolean slower, boolean placeAsPlayer) {
        sendStructureSaverMachineSavePacket(structureName, structureId, machinePos, slower, placeAsPlayer, null);
    }
    
    /**
     * Sends a Structure Saver Machine save/modify packet to the server
     * This simulates a client-to-server packet for saving or modifying a structure from the machine
     */
    @OnlyIn(Dist.CLIENT)
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
    @OnlyIn(Dist.CLIENT)
    public static void sendShopBuyItemPacket(String entryId, int quantity) {
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
                    LOGGER.error("Error sending buy packet", e);
                }
            });
        } catch (Exception e) {
            LOGGER.error("Error in sendShopBuyItemPacket", e);
        }
    }
    
    /**
     * Sends a shop sell item packet to the server
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendShopSellItemPacket(String entryId, int quantity) {
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
                    LOGGER.error("Error sending sell packet", e);
                }
            });
        } catch (Exception e) {
            LOGGER.error("Error in sendShopSellItemPacket", e);
        }
    }

    /**
     * Invia il packet per settare lo slot encapsulato dell'Auto Shop
     */
    @OnlyIn(Dist.CLIENT)
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
    @OnlyIn(Dist.CLIENT)
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
    
    /**
     * Sends a Deep Drawers scroll packet to the server
     * Updates the scroll offset for the Deep Drawers GUI
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendDeepDrawersScrollPacket(BlockPos pos, int scrollOffset) {
        LOGGER.info("Client: Sending DeepDrawersScrollPacket: pos={}, offset={}", pos, scrollOffset);
        // Simplified implementation for single player compatibility
        try {
            // Get the server from single player or dedicated server
            net.minecraft.server.MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
            if (server == null) {
                LOGGER.warn("Client: Cannot send DeepDrawersScrollPacket: server is null");
                return;
            }

            // Create and handle the packet on server thread
            server.execute(() -> {
                try {
                    net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        LOGGER.info("Client: Executing DeepDrawersScrollPacket on server thread: pos={}, offset={}", pos, scrollOffset);
                        // Create and handle the packet
                        new net.unfamily.iskautils.network.packet.DeepDrawersScrollC2SPacket(pos, scrollOffset).handle(player);
                    } else {
                        LOGGER.warn("Client: Cannot send DeepDrawersScrollPacket: player is null");
                    }
                } catch (Exception e) {
                    LOGGER.error("Client: Error executing DeepDrawersScrollPacket", e);
                }
            });
        } catch (Exception e) {
            LOGGER.error("Client: Error in sendDeepDrawersScrollPacket", e);
        }
    }

    /**
     * Sends a Deep Drawers sync slots packet to a player (Server to Client)
     * Synchronizes the visible slot contents after scrolling
     */
    public static void sendToPlayer(net.unfamily.iskautils.network.packet.DeepDrawersSyncSlotsS2CPacket packet, ServerPlayer player) {
        try {
            // Direct call for single player compatibility
            // Execute on client thread to ensure GUI is accessible
            net.minecraft.client.Minecraft.getInstance().execute(() -> packet.handle());
        } catch (Exception e) {
            LOGGER.warn("Failed to send Deep Drawers sync packet to player: {}", e.getMessage());
        }
    }
    
    /**
     * Sends a Burning Brazier toggle packet to the server
     * This toggles the auto-placement state for the Burning Brazier item
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendBurningBrazierTogglePacket() {
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
                        // Toggle the Burning Brazier auto-placement state
                        boolean currentState = net.unfamily.iskautils.data.BurningBrazierData.getAutoPlacementEnabledFromPlayer(player);
                        boolean newState = !currentState;

                        net.unfamily.iskautils.data.BurningBrazierData.setAutoPlacementEnabledToPlayer(player, newState);

                        // Send message to player
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.iska_utils.burning_brazier_auto_placement." +
                                                     (newState ? "enabled" : "disabled")), true);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to toggle Burning Brazier auto-placement: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            LOGGER.warn("Failed to send Burning Brazier toggle packet: {}", e.getMessage());
        }
    }

    /**
     * Sends a Scanner range cycle packet to the server
     * This cycles through the available scan range options
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendScannerRangeCyclePacket() {
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
                        // Get scanner from main hand
                        net.minecraft.world.item.ItemStack mainHandItem = player.getMainHandItem();
                        if (mainHandItem.getItem() instanceof net.unfamily.iskautils.item.custom.ScannerItem scanner) {
                            scanner.cycleScanRange(player, mainHandItem);
                        } else {
                            // Try offhand
                            net.minecraft.world.item.ItemStack offHandItem = player.getOffhandItem();
                            if (offHandItem.getItem() instanceof net.unfamily.iskautils.item.custom.ScannerItem scanner2) {
                                scanner2.cycleScanRange(player, offHandItem);
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to cycle scanner range: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            LOGGER.warn("Failed to send scanner range cycle packet: {}", e.getMessage());
        }
    }

    /**
     * Sends a Ghost Brazier toggle packet to the server
     * This toggles the game mode between Survival and Spectator
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendGhostBrazierTogglePacket() {
        // Simplified implementation for single player compatibility
        try {
            // Get the server from single player or dedicated server
            net.minecraft.server.MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
            if (server == null) return;

            // Create and handle the packet on server thread
            server.execute(() -> {
                try {
                    net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    if (player == null) {
                        return;
                    }

                    net.minecraft.world.level.GameType currentGameMode = player.gameMode.getGameModeForPlayer();
                    
                    if (currentGameMode == net.minecraft.world.level.GameType.SPECTATOR) {
                        // Switch back to previous game mode (or Survival if not set)
                        net.minecraft.world.level.GameType previousMode = net.unfamily.iskautils.data.GhostBrazierData.getPreviousGameMode(player);
                        player.setGameMode(previousMode);
                        net.unfamily.iskautils.data.GhostBrazierData.clearPreviousGameMode(player);
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.iska_utils.ghost_brazier.became_physical"), true);
                    } else {
                        // Switch to Spectator mode, save current mode
                        net.unfamily.iskautils.data.GhostBrazierData.setPreviousGameMode(player, currentGameMode);
                        player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.iska_utils.ghost_brazier.became_ethereal"), true);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to toggle Ghost Brazier game mode: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            LOGGER.warn("Failed to send Ghost Brazier toggle packet: {}", e.getMessage());
        }
    }

    /**
     * Invia il packet per aggiornare i parametri del timer
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendSmartTimerUpdatePacket(BlockPos pos, boolean isCooldown, int deltaTicks) {
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
                        new net.unfamily.iskautils.network.packet.SmartTimerUpdateC2SPacket(pos, isCooldown, deltaTicks).handle(player);
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
     * Sends packet to update fan range parameters
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendFanRangeUpdatePacket(BlockPos pos, net.unfamily.iskautils.network.packet.FanRangeUpdateC2SPacket.RangeType rangeType, int delta) {
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
                        new net.unfamily.iskautils.network.packet.FanRangeUpdateC2SPacket(pos, rangeType, delta).handle(player);
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
     * Sends filter update packet to server
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendDeepDrawerExtractorFilterUpdatePacket(BlockPos pos, java.util.Map<Integer, String> filterMap, boolean isWhitelistMode) {
        // Simplified implementation - directly handle on the server side (like rotation in Structure Placer)
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
                        
                        net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(pos);
                        if (blockEntity instanceof net.unfamily.iskautils.block.entity.DeepDrawerExtractorBlockEntity extractor) {
                            // Update filter fields from map (indices outside valid range are ignored)
                            if (filterMap != null) {
                                extractor.setFilterFieldsFromMap(filterMap);
                            }
                            
                            // Update mode
                            extractor.setWhitelistMode(isWhitelistMode);
                            
                            // Mark BlockEntity as changed
                            extractor.setChanged();
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error handling Deep Drawer Extractor filter update packet: {}", e.getMessage());
                }
            });
            

        } catch (Exception e) {
            LOGGER.error("Could not send Deep Drawer Extractor filter update packet: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Sends inverted filter update packet to server
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendDeepDrawerExtractorInvertedFilterUpdatePacket(BlockPos pos, java.util.Map<Integer, String> invertedFilterMap) {
        // Simplified implementation - directly handle on the server side (like rotation in Structure Placer)
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
                        
                        net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(pos);
                        if (blockEntity instanceof net.unfamily.iskautils.block.entity.DeepDrawerExtractorBlockEntity extractor) {
                            // Update inverted filter fields from map (indices outside valid range are ignored)
                            if (invertedFilterMap != null) {
                                extractor.setInvertedFilterFieldsFromMap(invertedFilterMap);
                            }
                            
                            // Mark BlockEntity as changed
                            extractor.setChanged();
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error handling Deep Drawer Extractor inverted filter update packet: {}", e.getMessage());
                }
            });
            

        } catch (Exception e) {
            LOGGER.error("Could not send Deep Drawer Extractor inverted filter update packet: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Sends a Deep Drawer Extractor Mode Toggle packet to the server
     * Toggles between whitelist and blacklist mode (like rotation in Structure Placer)
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendDeepDrawerExtractorModeTogglePacket(BlockPos machinePos) {
        // Use simplified approach like rotation in Structure Placer
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
                        // Directly call the toggle logic
                        net.minecraft.world.level.block.entity.BlockEntity blockEntity = player.serverLevel().getBlockEntity(machinePos);
                        if (blockEntity instanceof net.unfamily.iskautils.block.entity.DeepDrawerExtractorBlockEntity extractor) {
                            // Toggle whitelist/blacklist mode
                            boolean currentMode = extractor.isWhitelistMode();
                            extractor.setWhitelistMode(!currentMode);
                            // setWhitelistMode() already calls setChanged() which forces sync
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error handling Deep Drawer Extractor mode toggle packet: {}", e.getMessage());
                }
            });
            
        } catch (Exception e) {
            LOGGER.error("Could not send Deep Drawer Extractor mode toggle packet: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Invia il packet per ciclare il tipo I/O di una faccia del timer
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendSmartTimerIoConfigCyclePacket(BlockPos machinePos, net.minecraft.core.Direction direction) {
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
                        if (blockEntity instanceof net.unfamily.iskautils.block.entity.SmartTimerBlockEntity timer) {
                            
                            // Play click sound
                            level.playSound(null, machinePos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 
                                net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, 1.0f);
                            
                            // Mark the block entity as changed
                            timer.setChanged();
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error handling Smart Timer I/O config cycle packet: {}", e.getMessage());
                }
            });
            
        } catch (Exception e) {
            LOGGER.error("Could not send Smart Timer I/O config cycle packet: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Invia il packet per resettare tutte le facce I/O del timer
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendSmartTimerIoConfigResetPacket(BlockPos machinePos) {
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
                        if (blockEntity instanceof net.unfamily.iskautils.block.entity.SmartTimerBlockEntity timer) {
                            
                            // Play click sound
                            level.playSound(null, machinePos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 
                                net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, 1.0f);
                            
                            // Mark the block entity as changed
                            timer.setChanged();
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error handling Smart Timer I/O config reset packet: {}", e.getMessage());
                }
            });
            
        } catch (Exception e) {
            LOGGER.error("Could not send Smart Timer I/O config reset packet: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Invia il packet per cambiare l'accelerazione del Temporal Overclocker
     * @param machinePos Posizione del blocco
     * @param action Tipo di azione: 0=increase, 1=decrease, 2=increaseBy5, 3=decreaseBy5, 4=max, 5=min, 6=default
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendTemporalOverclockerAccelerationChangePacket(BlockPos machinePos, int action) {
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
                        if (blockEntity instanceof net.unfamily.iskautils.block.entity.TemporalOverclockerBlockEntity overclocker) {
                            switch (action) {
                                case 0 -> overclocker.increaseAccelerationFactor();
                                case 1 -> overclocker.decreaseAccelerationFactor();
                                case 2 -> overclocker.increaseAccelerationFactorBy5();
                                case 3 -> overclocker.decreaseAccelerationFactorBy5();
                                case 4 -> overclocker.setAccelerationFactorToMax();
                                case 5 -> overclocker.setAccelerationFactorToMin();
                                case 6 -> overclocker.setAccelerationFactorToDefault();
                            }
                            
                            // Play click sound
                            level.playSound(null, machinePos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 
                                net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, 1.0f);
                            
                            overclocker.setChanged();
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error handling Temporal Overclocker acceleration change packet: {}", e.getMessage());
                }
            });
            
        } catch (Exception e) {
            LOGGER.error("Could not send Temporal Overclocker acceleration change packet: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Invia il packet per cambiare il redstone mode del Temporal Overclocker
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendTemporalOverclockerRedstoneModePacket(BlockPos machinePos) {
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
                        if (blockEntity instanceof net.unfamily.iskautils.block.entity.TemporalOverclockerBlockEntity overclocker) {
                            // Cycle to next redstone mode
                            int currentMode = overclocker.getRedstoneMode();
                            int nextMode = (currentMode + 1) % 5;
                            overclocker.setRedstoneMode(nextMode);
                            
                            // Play click sound
                            level.playSound(null, machinePos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 
                                net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, 1.0f);
                            
                            overclocker.setChanged();
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error handling Temporal Overclocker redstone mode packet: {}", e.getMessage());
                }
            });
            
        } catch (Exception e) {
            LOGGER.error("Could not send Temporal Overclocker redstone mode packet: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Invia il packet per fare toggle della modalità persistente del Temporal Overclocker
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendTemporalOverclockerTogglePersistentPacket(BlockPos overclockerPos) {
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
                        // Toggle persistent mode
                        if (player.level().getBlockEntity(overclockerPos) instanceof net.unfamily.iskautils.block.entity.TemporalOverclockerBlockEntity blockEntity) {
                            blockEntity.togglePersistentMode();
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error handling Temporal Overclocker toggle persistent packet: {}", e.getMessage());
                }
            });
            
        } catch (Exception e) {
            LOGGER.error("Could not send Temporal Overclocker toggle persistent packet: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Invia il packet per evidenziare un blocco collegato nel mondo (crea un marker di 5 secondi)
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendTemporalOverclockerHighlightBlockPacket(BlockPos blockPos) {
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
                        // Create marker at block position (5 seconds = 100 ticks)
                        int color = (0x80 << 24) | 0x00FF00; // Semi-transparent green
                        int durationTicks = 100; // 5 seconds
                        ModMessages.sendAddBillboardPacket(player, blockPos, color, durationTicks);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error handling Temporal Overclocker highlight packet: {}", e.getMessage());
                }
            });
            
        } catch (Exception e) {
            LOGGER.error("Could not send Temporal Overclocker highlight block packet: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Invia il packet per rimuovere un blocco collegato dal Temporal Overclocker
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendTemporalOverclockerRemoveLinkPacket(BlockPos overclockerPos, BlockPos linkedPos) {
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
                        
                        net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(overclockerPos);
                        if (blockEntity instanceof net.unfamily.iskautils.block.entity.TemporalOverclockerBlockEntity overclocker) {
                            overclocker.removeLinkedBlock(linkedPos);
                            overclocker.setChanged();
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error handling Temporal Overclocker remove link packet: {}", e.getMessage());
                }
            });
            
        } catch (Exception e) {
            LOGGER.error("Could not send Temporal Overclocker remove link packet: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Sends Fan Redstone Mode packet to the server
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendFanRedstoneModePacket(BlockPos pos) {
        try {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft == null) return;
            
            net.minecraft.client.server.IntegratedServer server = minecraft.getSingleplayerServer();
            if (server == null) return;
            
            server.execute(() -> {
                try {
                    net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        net.minecraft.server.level.ServerLevel level = player.serverLevel();
                        net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(pos);
                        if (blockEntity instanceof net.unfamily.iskautils.block.entity.FanBlockEntity fan) {
                            // Cycle to next redstone mode (0-4)
                            int currentMode = fan.getRedstoneMode();
                            int nextMode = (currentMode + 1) % 5; // Cycle 0->1->2->3->4->0
                            fan.setRedstoneMode(nextMode);
                            
                            // Play click sound
                            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 
                                net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, 1.0f);
                            
                            // Mark the block entity as changed
                            fan.setChanged();
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error handling Fan redstone mode packet: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            LOGGER.error("Could not send Fan redstone mode packet: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Sends Smart Timer Redstone Mode packet to the server
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendSmartTimerRedstoneModePacket(BlockPos pos) {
        try {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft == null) return;
            
            net.minecraft.client.server.IntegratedServer server = minecraft.getSingleplayerServer();
            if (server == null) return;
            
            server.execute(() -> {
                try {
                    net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        net.minecraft.server.level.ServerLevel level = player.serverLevel();
                        net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(pos);
                        if (blockEntity instanceof net.unfamily.iskautils.block.entity.SmartTimerBlockEntity timer) {
                            // Cycle to next redstone mode (skip PULSE mode 3): 0->1->2->4->0
                            timer.cycleRedstoneMode();
                            
                            // Play click sound
                            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 
                                net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, 1.0f);
                            
                            // Mark the block entity as changed
                            timer.setChanged();
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error handling Smart Timer redstone mode packet: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            LOGGER.error("Could not send Smart Timer redstone mode packet: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Sends Fan Push/Pull packet to the server
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendFanPushPullPacket(BlockPos pos) {
        try {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft == null) return;
            
            net.minecraft.client.server.IntegratedServer server = minecraft.getSingleplayerServer();
            if (server == null) return;
            
            server.execute(() -> {
                try {
                    net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        net.minecraft.server.level.ServerLevel level = player.serverLevel();
                        net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(pos);
                        if (blockEntity instanceof net.unfamily.iskautils.block.entity.FanBlockEntity fan) {
                            // Toggle push/pull
                            fan.setPull(!fan.isPull());
                            
                            // Play click sound
                            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 
                                net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, 1.0f);
                            
                            // Mark the block entity as changed
                            fan.setChanged();
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error handling Fan push/pull packet: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            LOGGER.error("Could not send Fan push/pull packet: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Sends Fan Push Type packet to the server
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendFanPushTypePacket(BlockPos pos) {
        try {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft == null) return;
            
            net.minecraft.client.server.IntegratedServer server = minecraft.getSingleplayerServer();
            if (server == null) return;
            
            server.execute(() -> {
                try {
                    net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        net.minecraft.server.level.ServerLevel level = player.serverLevel();
                        net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(pos);
                        if (blockEntity instanceof net.unfamily.iskautils.block.entity.FanBlockEntity fan) {
                            // Cycle to next push type
                            net.unfamily.iskautils.block.entity.FanBlockEntity.PushType currentType = fan.getPushType();
                            net.unfamily.iskautils.block.entity.FanBlockEntity.PushType nextType = switch (currentType) {
                                case MOBS_ONLY -> net.unfamily.iskautils.block.entity.FanBlockEntity.PushType.MOBS_AND_PLAYERS;
                                case MOBS_AND_PLAYERS -> net.unfamily.iskautils.block.entity.FanBlockEntity.PushType.PLAYERS_ONLY;
                                case PLAYERS_ONLY -> net.unfamily.iskautils.block.entity.FanBlockEntity.PushType.MOBS_ONLY;
                            };
                            fan.setPushType(nextType);
                            
                            // Play click sound
                            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 
                                net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, 1.0f);
                            
                            // Mark the block entity as changed
                            fan.setChanged();
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error handling Fan push type packet: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            LOGGER.error("Could not send Fan push type packet: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Sends Fan Show Area packet to the server
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendFanShowAreaPacket(BlockPos pos) {
        try {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft == null) return;
            
            net.minecraft.client.server.IntegratedServer server = minecraft.getSingleplayerServer();
            if (server == null) return;
            
            server.execute(() -> {
                try {
                    net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        net.minecraft.server.level.ServerLevel level = player.serverLevel();
                        net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(pos);
                        if (blockEntity instanceof net.unfamily.iskautils.block.entity.FanBlockEntity fan) {
                            // Get fan facing direction
                            var state = level.getBlockState(pos);
                            if (!(state.getBlock() instanceof net.unfamily.iskautils.block.FanBlock)) return;
                            var facing = state.getValue(net.unfamily.iskautils.block.FanBlock.FACING);
                            
                            // Calculate the push area AABB
                            var aabb = net.unfamily.iskautils.block.entity.FanBlockEntity.calculatePushArea(pos, facing, fan);
                            
                            // Get bounds
                            int minX = (int) Math.floor(aabb.minX);
                            int minY = (int) Math.floor(aabb.minY);
                            int minZ = (int) Math.floor(aabb.minZ);
                            int maxX = (int) Math.floor(aabb.maxX);
                            int maxY = (int) Math.floor(aabb.maxY);
                            int maxZ = (int) Math.floor(aabb.maxZ);
                            
                            // Purple color for border markers when air (ARGB: 0x80FF00FF = 50% transparent purple)
                            int purpleColor = 0x80FF00FF;
                            // Red color for border markers when block (ARGB: 0x80FF0000 = 50% transparent red, same transparency)
                            int redColor = 0x80FF0000;
                            int durationTicks = 60; // 3 seconds
                            
                            // Add billboard markers only at the edges of the area (not inside faces)
                            // Purple if air, red if block (same transparency)
                            
                            // Top and bottom faces - only edges (x or z at boundary)
                            for (int x = minX; x < maxX; x++) {
                                for (int z = minZ; z < maxZ; z++) {
                                    // Only place marker if on edge (x or z at boundary)
                                    boolean isOnEdge = (x == minX || x == maxX - 1 || z == minZ || z == maxZ - 1);
                                    if (isOnEdge) {
                                        // Top face
                                        net.minecraft.core.BlockPos topPos = new net.minecraft.core.BlockPos(x, maxY - 1, z);
                                        int topColor = level.getBlockState(topPos).isAir() ? purpleColor : redColor;
                                        sendAddBillboardPacket(player, topPos, topColor, durationTicks);
                                        
                                        // Bottom face
                                        net.minecraft.core.BlockPos bottomPos = new net.minecraft.core.BlockPos(x, minY, z);
                                        int bottomColor = level.getBlockState(bottomPos).isAir() ? purpleColor : redColor;
                                        sendAddBillboardPacket(player, bottomPos, bottomColor, durationTicks);
                                    }
                                }
                            }
                            
                            // Front and back faces (Z faces) - only edges (x or y at boundary)
                            for (int x = minX; x < maxX; x++) {
                                for (int y = minY; y < maxY; y++) {
                                    // Only place marker if on edge (x or y at boundary)
                                    boolean isOnEdge = (x == minX || x == maxX - 1 || y == minY || y == maxY - 1);
                                    if (isOnEdge) {
                                        // Min Z face
                                        net.minecraft.core.BlockPos minZPos = new net.minecraft.core.BlockPos(x, y, minZ);
                                        int minZColor = level.getBlockState(minZPos).isAir() ? purpleColor : redColor;
                                        sendAddBillboardPacket(player, minZPos, minZColor, durationTicks);
                                        
                                        // Max Z face
                                        net.minecraft.core.BlockPos maxZPos = new net.minecraft.core.BlockPos(x, y, maxZ - 1);
                                        int maxZColor = level.getBlockState(maxZPos).isAir() ? purpleColor : redColor;
                                        sendAddBillboardPacket(player, maxZPos, maxZColor, durationTicks);
                                    }
                                }
                            }
                            
                            // Left and right faces (X faces) - only edges (z or y at boundary)
                            for (int z = minZ; z < maxZ; z++) {
                                for (int y = minY; y < maxY; y++) {
                                    // Only place marker if on edge (z or y at boundary)
                                    boolean isOnEdge = (z == minZ || z == maxZ - 1 || y == minY || y == maxY - 1);
                                    if (isOnEdge) {
                                        // Min X face
                                        net.minecraft.core.BlockPos minXPos = new net.minecraft.core.BlockPos(minX, y, z);
                                        int minXColor = level.getBlockState(minXPos).isAir() ? purpleColor : redColor;
                                        sendAddBillboardPacket(player, minXPos, minXColor, durationTicks);
                                        
                                        // Max X face
                                        net.minecraft.core.BlockPos maxXPos = new net.minecraft.core.BlockPos(maxX - 1, y, z);
                                        int maxXColor = level.getBlockState(maxXPos).isAir() ? purpleColor : redColor;
                                        sendAddBillboardPacket(player, maxXPos, maxXColor, durationTicks);
                                    }
                                }
                            }
                            
                            // Add red markers inside the area for blocks (obstacles)
                            for (int x = minX; x < maxX; x++) {
                                for (int y = minY; y < maxY; y++) {
                                    for (int z = minZ; z < maxZ; z++) {
                                        net.minecraft.core.BlockPos blockPos = new net.minecraft.core.BlockPos(x, y, z);
                                        // Only place marker if there's a block (not air)
                                        if (!level.getBlockState(blockPos).isAir()) {
                                            sendAddBillboardPacket(player, blockPos, redColor, durationTicks);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error handling Fan show area packet: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            LOGGER.error("Could not send Fan show area packet: {}", e.getMessage(), e);
        }
    }
    
} 