package net.unfamily.iskautils.network;

import net.unfamily.iskautils.util.ModLogger;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
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
import net.unfamily.iskautils.network.packet.VectorCharmC2SPacket;
import net.unfamily.iskautils.network.packet.PortableDislocatorC2SPacket;
import net.unfamily.iskautils.network.packet.ClearPreviewForOwnerS2CPayload;
import net.unfamily.iskautils.network.packet.PreviewMarkerS2CPayload;
import net.unfamily.iskautils.network.packet.StructurePlacerMachineTogglePreviewC2SPacket;
import net.unfamily.iskautils.network.packet.TemporalOverclockerHighlightBlockC2SPacket;
import net.neoforged.neoforge.network.PacketDistributor;
import net.unfamily.iskalib.structure.StructureDefinition;
import net.unfamily.iskalib.structure.StructureLoader;
import net.unfamily.iskautils.util.ClientEventsAccess;
import net.unfamily.iskautils.util.ClientGuiAccess;
import net.unfamily.iskautils.util.ClientRuntimeAccess;
import net.unfamily.iskautils.util.ModUtils;
import net.unfamily.iskautils.block.entity.StructurePlacerMachineBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import com.mojang.math.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.unfamily.iskautils.network.packet.AutoShopSetEncapsulatedC2SPacket;
import net.unfamily.iskautils.network.packet.FactoryScrollC2SPacket;
import net.unfamily.iskautils.network.packet.FactorySelectColorC2SPacket;
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
    private static final ModLogger LOGGER = ModLogger.of(ModMessages.class);
    
    // Simplified version to avoid NeoForge networking compatibility issues
    
    /**
     * Registers network messages for the mod
     */
    public static void register() {
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

        registrar.playToServer(
            net.unfamily.iskautils.network.packet.FanPushPullSetC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.FanPushPullSetC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.FanPushPullSetC2SPacket::handle
        );

        registrar.playToServer(
            net.unfamily.iskautils.network.packet.FanTargetTypeSetC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.FanTargetTypeSetC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.FanTargetTypeSetC2SPacket::handle
        );

        registrar.playToServer(
            net.unfamily.iskautils.network.packet.MobReaperTargetTypeC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.MobReaperTargetTypeC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.MobReaperTargetTypeC2SPacket::handle
        );

        registrar.playToServer(
            net.unfamily.iskautils.network.packet.MobReaperRedstoneModeC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.MobReaperRedstoneModeC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.MobReaperRedstoneModeC2SPacket::handle
        );

        registrar.playToServer(
            net.unfamily.iskautils.network.packet.MobReaperAgeFilterC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.MobReaperAgeFilterC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.MobReaperAgeFilterC2SPacket::handle
        );

        registrar.playToServer(
            net.unfamily.iskautils.network.packet.CollectingCrateModeC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.CollectingCrateModeC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.CollectingCrateModeC2SPacket::handle
        );
        registrar.playToServer(
            net.unfamily.iskautils.network.packet.CollectingCrateRedstoneModeC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.CollectingCrateRedstoneModeC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.CollectingCrateRedstoneModeC2SPacket::handle
        );
        registrar.playToServer(
            net.unfamily.iskautils.network.packet.CollectingCrateXpCollectC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.CollectingCrateXpCollectC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.CollectingCrateXpCollectC2SPacket::handle
        );
        registrar.playToServer(
            net.unfamily.iskautils.network.packet.CollectingCrateXpDepositC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.CollectingCrateXpDepositC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.CollectingCrateXpDepositC2SPacket::handle
        );
        registrar.playToServer(
            net.unfamily.iskautils.network.packet.CollectingCrateSizeC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.CollectingCrateSizeC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.CollectingCrateSizeC2SPacket::handle
        );
        registrar.playToServer(
            net.unfamily.iskautils.network.packet.CollectingCratePreviewToggleC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.CollectingCratePreviewToggleC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.CollectingCratePreviewToggleC2SPacket::handle
        );
        
        // Register Smart Timer Redstone Mode C2S Packet (Client to Server)
        registrar.playToServer(
            net.unfamily.iskautils.network.packet.SmartTimerRedstoneModeC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.SmartTimerRedstoneModeC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.SmartTimerRedstoneModeC2SPacket::handle
        );

        registrar.playToServer(
            net.unfamily.iskautils.network.packet.StructurePlacerMachineRedstoneModeC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.StructurePlacerMachineRedstoneModeC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.StructurePlacerMachineRedstoneModeC2SPacket::handle
        );

        registrar.playToServer(
            net.unfamily.iskautils.network.packet.DeepDrawerExtractorRedstoneModeC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.DeepDrawerExtractorRedstoneModeC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.DeepDrawerExtractorRedstoneModeC2SPacket::handle
        );
        registrar.playToServer(
            net.unfamily.iskautils.network.packet.DeepDrawerExtractorListLogicToggleC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.DeepDrawerExtractorListLogicToggleC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.DeepDrawerExtractorListLogicToggleC2SPacket::handle
        );
        registrar.playToServer(
            net.unfamily.iskautils.network.packet.DeepDrawerExtractorFilterPanelC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.DeepDrawerExtractorFilterPanelC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.DeepDrawerExtractorFilterPanelC2SPacket::handle
        );
        registrar.playToServer(
            net.unfamily.iskautils.network.packet.DeepDrawerExtractorFilterUpdateC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.DeepDrawerExtractorFilterUpdateC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.DeepDrawerExtractorFilterUpdateC2SPacket::handle
        );
        registrar.playToServer(
            net.unfamily.iskautils.network.packet.DeepDrawerExtractorInvertedFiltersC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.DeepDrawerExtractorInvertedFiltersC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.DeepDrawerExtractorInvertedFiltersC2SPacket::handle
        );
        registrar.playToServer(
            net.unfamily.iskautils.network.packet.DeepDrawerExtractorSettingsCopierC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.DeepDrawerExtractorSettingsCopierC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.DeepDrawerExtractorSettingsCopierC2SPacket::handle
        );

        // Register Sound Muffler Volume C2S Packet (Client to Server)
        registrar.playToServer(
            net.unfamily.iskautils.network.packet.SoundMufflerVolumeC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.SoundMufflerVolumeC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.SoundMufflerVolumeC2SPacket::handle
        );
        // Register Sound Muffler Mode Toggle C2S Packet (Client to Server)
        registrar.playToServer(
            net.unfamily.iskautils.network.packet.SoundMufflerModeToggleC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.SoundMufflerModeToggleC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.SoundMufflerModeToggleC2SPacket::handle
        );
        // Register Sound Muffler Filter Update C2S Packet (Client to Server)
        registrar.playToServer(
            net.unfamily.iskautils.network.packet.SoundMufflerFilterUpdateC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.SoundMufflerFilterUpdateC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.SoundMufflerFilterUpdateC2SPacket::handle
        );
        // Register Sound Muffler Range C2S Packet (Client to Server)
        registrar.playToServer(
            net.unfamily.iskautils.network.packet.SoundMufflerRangeC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.SoundMufflerRangeC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.SoundMufflerRangeC2SPacket::handle
        );
        
        // Register Fan Show Area C2S Packet (Client to Server)
        registrar.playToServer(
            net.unfamily.iskautils.network.packet.FanShowAreaC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.FanShowAreaC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.FanShowAreaC2SPacket::handle
        );

        registrar.playToServer(
            FactorySelectColorC2SPacket.TYPE,
            FactorySelectColorC2SPacket.STREAM_CODEC,
            FactorySelectColorC2SPacket::handle
        );

        registrar.playToServer(
            FactoryScrollC2SPacket.TYPE,
            FactoryScrollC2SPacket.STREAM_CODEC,
            FactoryScrollC2SPacket::handle
        );

        registrar.playToServer(
            net.unfamily.iskautils.network.packet.FactoryRedstoneModeC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.FactoryRedstoneModeC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.FactoryRedstoneModeC2SPacket::handle
        );

        registrar.playToServer(
            net.unfamily.iskautils.network.packet.EntropicSpawnerRedstoneModeC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.EntropicSpawnerRedstoneModeC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.EntropicSpawnerRedstoneModeC2SPacket::handle
        );

        registrar.playToServer(
            net.unfamily.iskautils.network.packet.AncientTabletCraftC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.AncientTabletCraftC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.AncientTabletCraftC2SPacket::handle
        );

        registrar.playToServer(
            net.unfamily.iskautils.network.packet.SwissWrenchCycleModeC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.SwissWrenchCycleModeC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.SwissWrenchCycleModeC2SPacket::handle
        );

        registrar.playToServer(
            net.unfamily.iskautils.network.packet.AncientTableScrollC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.AncientTableScrollC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.AncientTableScrollC2SPacket::handle
        );

        registrar.playToServer(
            net.unfamily.iskautils.network.packet.AncientTableRedstoneModeC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.AncientTableRedstoneModeC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.AncientTableRedstoneModeC2SPacket::handle
        );

        registrar.playToServer(
            StructurePlacerMachineTogglePreviewC2SPacket.TYPE,
            StructurePlacerMachineTogglePreviewC2SPacket.STREAM_CODEC,
            StructurePlacerMachineTogglePreviewC2SPacket::handle
        );

        registrar.playToClient(
            PreviewMarkerS2CPayload.TYPE,
            PreviewMarkerS2CPayload.STREAM_CODEC,
            PreviewMarkerS2CPayload::handle
        );

        registrar.playToClient(
            ClearPreviewForOwnerS2CPayload.TYPE,
            ClearPreviewForOwnerS2CPayload.STREAM_CODEC,
            ClearPreviewForOwnerS2CPayload::handle
        );

        registrar.playToServer(
            net.unfamily.iskautils.network.packet.FlameVisionToggleC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.FlameVisionToggleC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.FlameVisionToggleC2SPacket::handle
        );

        registrar.playToClient(
            net.unfamily.iskautils.network.packet.FlameVisionSyncS2CPacket.TYPE,
            net.unfamily.iskautils.network.packet.FlameVisionSyncS2CPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.FlameVisionSyncS2CPacket::handle
        );

        registrar.playToServer(
            net.unfamily.iskautils.network.packet.BlazingAltarConfigC2SPacket.TYPE,
            net.unfamily.iskautils.network.packet.BlazingAltarConfigC2SPacket.STREAM_CODEC,
            net.unfamily.iskautils.network.packet.BlazingAltarConfigC2SPacket::handle
        );

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
        // Simplified implementation for single player compatibility
    }

    @net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    public static void sendFactorySelectColor(net.minecraft.core.BlockPos pos, int index) {
        var packet = new net.unfamily.iskautils.network.packet.FactorySelectColorC2SPacket(pos, index);
        try {
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
            if (server != null) {
                server.execute(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().isEmpty()
                            ? null : server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        net.minecraft.world.level.block.entity.BlockEntity be = player.level().getBlockEntity(pos);
                        if (be instanceof net.unfamily.iskautils.block.entity.FactoryBlockEntity factory) {
                            factory.setSelectedColorIndex(index);
                        }
                    }
                });
                return;
            }
        } catch (Exception ignored) {
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
    }

    @net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    public static void sendFactoryScroll(net.minecraft.core.BlockPos pos, int offset) {
        var packet = new net.unfamily.iskautils.network.packet.FactoryScrollC2SPacket(pos, offset);
        try {
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
            if (server != null) {
                server.execute(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().isEmpty()
                            ? null : server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        net.minecraft.world.level.block.entity.BlockEntity be = player.level().getBlockEntity(pos);
                        if (be instanceof net.unfamily.iskautils.block.entity.FactoryBlockEntity factory) {
                            factory.setScrollOffset(offset);
                        }
                    }
                });
                return;
            }
        } catch (Exception ignored) {
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendFactoryRedstoneMode(net.minecraft.core.BlockPos pos, boolean backward) {
        var packet = new net.unfamily.iskautils.network.packet.FactoryRedstoneModeC2SPacket(pos, backward);
        try {
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
            if (server != null) {
                server.execute(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().isEmpty()
                            ? null : server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        net.minecraft.world.level.block.entity.BlockEntity be = player.level().getBlockEntity(pos);
                        if (be instanceof net.unfamily.iskautils.block.entity.FactoryBlockEntity factory) {
                            if (backward) {
                                factory.cycleRedstoneModeBackward();
                            } else {
                                factory.cycleRedstoneMode();
                            }
                        }
                    }
                });
                return;
            }
        } catch (Exception ignored) {
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendEntropicSpawnerRedstoneMode(net.minecraft.core.BlockPos pos, boolean backward) {
        var packet = new net.unfamily.iskautils.network.packet.EntropicSpawnerRedstoneModeC2SPacket(pos, backward);
        try {
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
            if (server != null) {
                server.execute(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().isEmpty()
                            ? null : server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        net.minecraft.world.level.block.entity.BlockEntity be = player.level().getBlockEntity(pos);
                        if (be instanceof net.unfamily.iskautils.block.entity.EntropicSpawnerBlockEntity spawner) {
                            if (backward) {
                                spawner.cycleRedstoneModeBackward();
                            } else {
                                spawner.cycleRedstoneMode();
                            }
                        }
                    }
                });
                return;
            }
        } catch (Exception ignored) {
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendAncientTableScroll(net.minecraft.core.BlockPos pos, int side, int offset) {
        var packet = new net.unfamily.iskautils.network.packet.AncientTableScrollC2SPacket(pos, side, offset);
        try {
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
            if (server != null) {
                server.execute(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().isEmpty()
                            ? null : server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        net.minecraft.world.level.block.entity.BlockEntity be = player.level().getBlockEntity(pos);
                        if (be instanceof net.unfamily.iskautils.block.entity.AncientTableBlockEntity table) {
                            if (side == net.unfamily.iskautils.network.packet.AncientTableScrollC2SPacket.SIDE_OUTPUT) {
                                table.setOutputScrollOffset(offset);
                            } else {
                                table.setInputScrollOffset(offset);
                            }
                        }
                    }
                });
                return;
            }
        } catch (Exception ignored) {
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendAncientTableRedstoneMode(net.minecraft.core.BlockPos pos, boolean backward) {
        var packet = new net.unfamily.iskautils.network.packet.AncientTableRedstoneModeC2SPacket(pos, backward);
        try {
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
            if (server != null) {
                server.execute(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().isEmpty()
                            ? null : server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        net.minecraft.world.level.block.entity.BlockEntity be = player.level().getBlockEntity(pos);
                        if (be instanceof net.unfamily.iskautils.block.entity.AncientTableBlockEntity table) {
                            if (backward) {
                                table.cycleRedstoneModeBackward();
                            } else {
                                table.cycleRedstoneMode();
                            }
                        }
                    }
                });
                return;
            }
        } catch (Exception ignored) {
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
    }
    
    /**
     * Sends a Structure Undo packet to the server
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendStructureUndoPacket() {
        // Simplified implementation for single player compatibility
        try {
            // Get the server from single player or dedicated server
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
            if (server == null) return;
            
            // Create and handle the packet on server thread
            server.execute(() -> {
                try {
                    net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        // Directly call the undo functionality
                        boolean success = net.unfamily.iskalib.structure.StructurePlacementHistory.undoLastPlacement(player);
                        
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
        try {
            ClientRuntimeAccess.runOnClientThread(() -> {
                ClientEventsAccess.handleAddHighlight(pos, color, durationTicks);
            });
        } catch (Exception e) {
            // Ignore errors when running on dedicated server
        }
    }

    public static void sendAddHighlightWithNamePacket(ServerPlayer player, BlockPos pos, int color, int durationTicks, String name) {
        try {
            ClientRuntimeAccess.runOnClientThread(() -> {
                ClientEventsAccess.handleAddHighlightWithName(pos, color, durationTicks, name);
            });
        } catch (Exception e) {
            // Ignore errors when running on dedicated server
        }
    }

    /** S2C: footprint preview marker owned by a machine block (toggle only). */
    public static void sendPreviewMarker(ServerPlayer player, BlockPos builderOrigin, BlockPos pos, int color, int durationTicks) {
        PacketDistributor.sendToPlayer(player, new PreviewMarkerS2CPayload(builderOrigin, pos, color, durationTicks));
    }

    /** S2C: clear footprint preview markers for one builder (toggle off only). */
    public static void clearPreviewForBuilder(ServerPlayer player, BlockPos builderOrigin) {
        PacketDistributor.sendToPlayer(player, new ClearPreviewForOwnerS2CPayload(builderOrigin));
    }

    public static void sendAddBillboardPacket(ServerPlayer player, BlockPos pos, int color, int durationTicks) {
        try {
            ClientRuntimeAccess.runOnClientThread(() -> {
                ClientEventsAccess.handleAddBillboard(pos, color, durationTicks);
            });
        } catch (Exception e) {
            // Ignore errors when running on dedicated server
        }
    }

    public static void sendAddBillboardWithNamePacket(ServerPlayer player, BlockPos pos, int color, int durationTicks, String name) {
        try {
            ClientRuntimeAccess.runOnClientThread(() -> {
                ClientEventsAccess.handleAddBillboardWithName(pos, color, durationTicks, name);
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
            ClientRuntimeAccess.runOnClientThread(() -> {
                ClientGuiAccess.handleShopTeamDataUpdate(teamName, teamBalances);
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
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
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
        try {
            ClientRuntimeAccess.runOnClientThread(() -> {
                ClientEventsAccess.handleRemoveHighlight(pos);
            });
        } catch (Exception e) {
            // Ignore errors when running on dedicated server
        }
    }

    /**
     * Sends a packet to clear all highlighted blocks
     */
    public static void sendClearHighlightsPacket(ServerPlayer player) {
        try {
            ClientRuntimeAccess.runOnClientThread(() -> {
                ClientEventsAccess.handleClearHighlights();
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
            ServerPlayer player = ClientRuntimeAccess.getFirstSingleplayerPlayer();
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
            ClientRuntimeAccess.runOnClientThread(() -> {
                // Handle packet on client side
                try {
                    var level = ClientRuntimeAccess.getClientLevel();
                    if (level != null) {
                        var blockEntity = level.getBlockEntity(machinePos);
                        if (blockEntity instanceof net.unfamily.iskautils.block.entity.StructureSaverMachineBlockEntity structureSaver) {
                            structureSaver.setBlueprintDataClientSide(vertex1, vertex2, center);
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
        }
    }
    
    
    /**
     * Sends a Structure Placer Machine Rotate packet to the server
     * This simulates a client-to-server packet for rotating the structure
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendStructurePlacerMachineRotatePacket(BlockPos machinePos) {
        try {
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
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
                                if (machine.isShowPreview()) {
                                    sendStructurePlacerMachineFootprint(player, player.serverLevel(), machinePos, machine);
                                }
                                
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
     * Sends an Auto Shop Redstone Mode packet to the server (cycle mode on button click)
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendAutoShopRedstoneModePacket(BlockPos machinePos, boolean backward) {
        try {
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
            if (server == null) return;
            server.execute(() -> {
                try {
                    net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        net.minecraft.server.level.ServerLevel level = player.serverLevel();
                        net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(machinePos);
                        if (blockEntity instanceof net.unfamily.iskautils.block.entity.AutoShopBlockEntity autoShop) {
                            net.unfamily.iskautils.block.entity.AutoShopBlockEntity.RedstoneMode current =
                                    net.unfamily.iskautils.block.entity.AutoShopBlockEntity.RedstoneMode.fromValue(autoShop.getRedstoneMode());
                            net.unfamily.iskautils.block.entity.AutoShopBlockEntity.RedstoneMode next =
                                    backward ? current.previous() : current.next();
                            autoShop.setRedstoneMode(next.getValue());
                            float pitch = backward ? 0.82f : 1.0f;
                            level.playSound(null, machinePos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
                                    net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, pitch);
                            autoShop.setChanged();
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error handling Auto Shop redstone mode packet: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            LOGGER.error("Could not send Auto Shop redstone mode packet: {}", e.getMessage(), e);
        }
    }

    /**
     * Sends a Structure Placer Machine Redstone Mode packet to the server
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendStructurePlacerMachineRedstoneModePacket(BlockPos machinePos, boolean backward) {
        try {
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
            if (server != null) {
                server.execute(() -> {
                    try {
                        net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                        if (player != null) {
                            net.minecraft.server.level.ServerLevel level = player.serverLevel();
                            net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(machinePos);
                            if (blockEntity instanceof StructurePlacerMachineBlockEntity machine) {
                                net.unfamily.iskautils.block.entity.StructurePlacerRedstoneMode currentMode =
                                        net.unfamily.iskautils.block.entity.StructurePlacerRedstoneMode.fromValue(machine.getRedstoneMode());
                                net.unfamily.iskautils.block.entity.StructurePlacerRedstoneMode newMode =
                                        backward ? currentMode.previous() : currentMode.next();
                                machine.setRedstoneMode(newMode.getValue());
                                float pitch = backward ? 0.82f : 1.0f;
                                level.playSound(null, machinePos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
                                        net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, pitch);
                                machine.setChanged();
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error handling Structure Placer Machine redstone mode packet: {}", e.getMessage());
                    }
                });
                return;
            }
        } catch (Exception e) {
            LOGGER.error("Structure Placer redstone SP path failed: {}", e.getMessage(), e);
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new net.unfamily.iskautils.network.packet.StructurePlacerMachineRedstoneModeC2SPacket(machinePos, backward));
    }
    
    /**
     * Sends a Deep Drawer Extractor Redstone Mode packet to the server
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendDeepDrawerExtractorRedstoneModePacket(BlockPos machinePos, boolean backward) {
        try {
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
            if (server != null) {
                server.execute(() -> {
                    try {
                        net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                        if (player != null) {
                            net.minecraft.server.level.ServerLevel level = player.serverLevel();
                            net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(machinePos);
                            if (blockEntity instanceof net.unfamily.iskautils.block.entity.DeepDrawerExtractorBlockEntity extractor) {
                                int oldMode = extractor.getRedstoneMode();
                                net.unfamily.iskautils.block.entity.DeepDrawerExtractorBlockEntity.RedstoneMode currentMode =
                                        net.unfamily.iskautils.block.entity.DeepDrawerExtractorBlockEntity.RedstoneMode.fromValue(oldMode);
                                net.unfamily.iskautils.block.entity.DeepDrawerExtractorBlockEntity.RedstoneMode newMode =
                                        backward ? currentMode.previous() : currentMode.next();
                                extractor.setRedstoneMode(newMode.getValue());
                                float pitch = backward ? 0.82f : 1.0f;
                                level.playSound(null, machinePos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
                                        net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, pitch);
                                extractor.setChanged();
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error handling Deep Drawer Extractor redstone mode packet: {}", e.getMessage());
                    }
                });
                return;
            }
        } catch (Exception e) {
            LOGGER.error("Deep Drawer Extractor redstone SP path failed: {}", e.getMessage(), e);
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new net.unfamily.iskautils.network.packet.DeepDrawerExtractorRedstoneModeC2SPacket(machinePos, backward));
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
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
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
     * Sends structure footprint preview markers to the opening player (persistent until toggled off).
     */
    public static void sendStructurePlacerMachineFootprint(
            ServerPlayer player,
            ServerLevel world,
            BlockPos machinePos,
            StructurePlacerMachineBlockEntity machine) {
        String selectedStructure = machine.getSelectedStructure();
        if (selectedStructure == null || selectedStructure.isEmpty()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§cNo structure selected!"), true);
            return;
        }
        StructureDefinition structure = StructureLoader.getStructure(selectedStructure);
        if (structure == null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§cStructure not found: " + selectedStructure), true);
            return;
        }
        showStructurePreview(world, machinePos, player, structure, machine.getRotation());
        String structureName = structure.getName() != null ? structure.getName() : structure.getId();
        player.displayClientMessage(net.minecraft.network.chat.Component.literal("§bShowing preview: §f" + structureName), true);
    }

    /**
     * Shows structure preview using billboard markers owned by the machine block.
     */
    private static void showStructurePreview(ServerLevel world, BlockPos machinePos, ServerPlayer player, StructureDefinition structure, int rotation) {
        if (structure == null) {
            return;
        }

        String[][][][] pattern = structure.getPattern();
        if (pattern == null || pattern.length == 0) {
            return;
        }

        // Find structure center
        BlockPos center = structure.findCenter();
        if (center == null) center = new BlockPos(0, 0, 0);

        int duration = 0; // no expiry until preview is toggled off

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
                            
                            sendPreviewMarker(player, machinePos, finalPos, markerColor, duration);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Calculates positions of all structure blocks with rotation around center @
     */
    private static java.util.Map<net.minecraft.core.BlockPos, String> calculateStructurePositions(net.minecraft.core.BlockPos centerPos, net.unfamily.iskalib.structure.StructureDefinition structure, int rotation) {
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
                                java.util.Map<String, java.util.List<net.unfamily.iskalib.structure.StructureDefinition.BlockDefinition>> key = structure.getKey();
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
    private static boolean canReplaceBlock(net.minecraft.world.level.block.state.BlockState state, net.unfamily.iskalib.structure.StructureDefinition structure) {
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
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
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
                return; // In singleplayer, the client already has its local structures
            }
            
            // Get ONLY structures to synchronize (NEVER server client structures)
            Map<String, StructureDefinition> serverStructures = StructureLoader.getStructuresForSync();
            
            if (serverStructures.isEmpty()) {
                return;
            }
            
            
            // Create synchronization packet with server flag
            net.unfamily.iskautils.network.packet.StructureSyncS2CPacket packet = 
                net.unfamily.iskautils.network.packet.StructureSyncS2CPacket.create(serverStructures, net.unfamily.iskautils.Config.acceptClientStructure);
            
            // Send the real packet to client using NeoForge networking system
            try {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, packet);
            } catch (Exception networkError) {
                LOGGER.error("Error sending structure sync packet to {}: {}", 
                           player.getName().getString(), networkError.getMessage());
                // Fallback: try simplified method as backup
                try {
                    ClientRuntimeAccess.runOnClientThread(() -> {
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
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
            if (server != null) {
                // Find the machine BlockEntity and request recalculation
                var level = server.getAllLevels().iterator().next(); // Get the first world
                var blockEntity = level.getBlockEntity(machinePos);
                if (blockEntity instanceof net.unfamily.iskautils.block.entity.StructureSaverMachineBlockEntity machine) {
                    machine.requestAreaRecalculation();
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
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
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
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
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
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
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
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
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
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
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
    public static void sendDeepDrawersSearchStatePacket(BlockPos pos, String query, int filterScrollOffset) {
        try {
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
            if (server == null) {
                return;
            }
            server.execute(() -> {
                try {
                    net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        new net.unfamily.iskautils.network.packet.DeepDrawersSearchStateC2SPacket(
                                pos, query, filterScrollOffset).handle(player);
                    }
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendDeepDrawersScrollPacket(BlockPos pos, int scrollOffset) {
        // Simplified implementation for single player compatibility
        try {
            // Get the server from single player or dedicated server
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
            if (server == null) {
                LOGGER.warn("Client: Cannot send DeepDrawersScrollPacket: server is null");
                return;
            }

            // Create and handle the packet on server thread
            server.execute(() -> {
                try {
                    net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
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
            ClientRuntimeAccess.runOnClientThread(() -> packet.handle());
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
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
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
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.iska_utils.burning_flames.auto_placement." +
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
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
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
     * Sends a Gauntlet of Climbing toggle packet to the server
     * This toggles the climbing ability on/off
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendGauntletClimbingTogglePacket() {
        // Simplified implementation for single player compatibility
        try {
            // Get the server from single player or dedicated server
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
            if (server == null) return;

            // Create and handle the packet on server thread
            server.execute(() -> {
                try {
                    net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    if (player == null) {
                        return;
                    }

                    boolean newState = net.unfamily.iskautils.item.custom.GauntletOfClimbingItem.toggleClimbing(player);
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable(
                                    "message.iska_utils.gauntlet_climbing_toggle." + (newState ? "enabled" : "disabled")),
                            true);
                } catch (Exception e) {
                    LOGGER.warn("Failed to toggle Gauntlet of Climbing: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            LOGGER.warn("Failed to send Gauntlet of Climbing toggle packet: {}", e.getMessage());
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
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
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
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
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
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
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
    public static void sendDeepDrawerExtractorFilterUpdatePacket(
            BlockPos pos, java.util.Map<Integer, String> filterMap,
            java.util.Map<Integer, Integer> concatMap, boolean isWhitelistMode) {
        try {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new net.unfamily.iskautils.network.packet.DeepDrawerExtractorFilterUpdateC2SPacket(
                            pos, filterMap, concatMap, isWhitelistMode));
        } catch (Exception e) {
            LOGGER.error("Could not send Deep Drawer Extractor filter update packet: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Sends inverted filter update packet to server
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendDeepDrawerExtractorInvertedFilterUpdatePacket(
            BlockPos pos, java.util.Map<Integer, String> invertedFilterMap,
            java.util.Map<Integer, Integer> concatMap) {
        try {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new net.unfamily.iskautils.network.packet.DeepDrawerExtractorInvertedFiltersC2SPacket(
                            pos, invertedFilterMap, concatMap));
        } catch (Exception e) {
            LOGGER.error("Could not send Deep Drawer Extractor inverted filter update packet: {}", e.getMessage(), e);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendDeepDrawerExtractorSettingsCopierPacket(BlockPos pos, int action, int allowDeny) {
        if (pos == null || pos.equals(BlockPos.ZERO)) {
            return;
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new net.unfamily.iskautils.network.packet.DeepDrawerExtractorSettingsCopierC2SPacket(pos, action, allowDeny));
    }
    
    /**
     * Sends a Deep Drawer Extractor Mode Toggle packet to the server
     * Toggles between whitelist and blacklist mode (like rotation in Structure Placer)
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendDeepDrawerExtractorModeTogglePacket(BlockPos machinePos) {
        if (machinePos == null || machinePos.equals(BlockPos.ZERO)) {
            return;
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new net.unfamily.iskautils.network.packet.DeepDrawerExtractorListLogicToggleC2SPacket(machinePos));
    }
    
    @OnlyIn(Dist.CLIENT)
    public static void sendDeepDrawerExtractorFilterPanelPacket(BlockPos machinePos, int panel) {
        if (machinePos == null || machinePos.equals(BlockPos.ZERO)) {
            return;
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new net.unfamily.iskautils.network.packet.DeepDrawerExtractorFilterPanelC2SPacket(machinePos, panel));
    }
    
    /**
     * Invia il packet per ciclare il tipo I/O di una faccia del timer
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendSmartTimerIoConfigCyclePacket(BlockPos machinePos, net.minecraft.core.Direction direction) {
        try {
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
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
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
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
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
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
    public static void sendTemporalOverclockerRedstoneModePacket(BlockPos machinePos, boolean backward) {
        try {
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
            if (server == null) return;

            server.execute(() -> {
                try {
                    net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        net.minecraft.server.level.ServerLevel level = player.serverLevel();
                        net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(machinePos);
                        if (blockEntity instanceof net.unfamily.iskautils.block.entity.TemporalOverclockerBlockEntity overclocker) {
                            int currentMode = overclocker.getRedstoneMode();
                            if (currentMode == 3) {
                                currentMode = 4;
                            }
                            int nextMode;
                            if (backward) {
                                nextMode = switch (currentMode) {
                                    case 0 -> 4;
                                    case 1 -> 0;
                                    case 2 -> 1;
                                    case 4 -> 2;
                                    default -> currentMode;
                                };
                            } else {
                                nextMode = Math.floorMod(currentMode + 1, 5);
                                if (nextMode == 3) {
                                    nextMode = 4;
                                }
                            }
                            overclocker.setRedstoneMode(nextMode);
                            float pitch = backward ? 0.82f : 1.0f;
                            level.playSound(null, machinePos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
                                    net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, pitch);
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
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
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
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
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
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
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
    
    @OnlyIn(Dist.CLIENT)
    public static void sendFanRedstoneModePacket(BlockPos pos, boolean backward) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new net.unfamily.iskautils.network.packet.FanRedstoneModeC2SPacket(pos, backward));
    }
    
    /**
     * Sends Smart Timer Redstone Mode packet to the server
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendSmartTimerRedstoneModePacket(BlockPos pos, boolean backward) {
        try {
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
            if (server != null) {
                server.execute(() -> {
                    try {
                        net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                        if (player != null) {
                            net.minecraft.server.level.ServerLevel level = player.serverLevel();
                            net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(pos);
                            if (blockEntity instanceof net.unfamily.iskautils.block.entity.SmartTimerBlockEntity timer) {
                                if (backward) {
                                    timer.cycleRedstoneModeBackward();
                                } else {
                                    timer.cycleRedstoneMode();
                                }
                                float pitch = backward ? 0.82f : 1.0f;
                                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
                                        net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, pitch);
                                timer.setChanged();
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error handling Smart Timer redstone mode packet: {}", e.getMessage());
                    }
                });
                return;
            }
        } catch (Exception e) {
            LOGGER.error("Smart Timer redstone SP path failed: {}", e.getMessage(), e);
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new net.unfamily.iskautils.network.packet.SmartTimerRedstoneModeC2SPacket(pos, backward));
    }

    /**
     * Sends Sound Muffler volume change to the server (client-only).
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendSoundMufflerVolumePacket(BlockPos pos, int categoryIndex, int delta) {
        var packet = new net.unfamily.iskautils.network.packet.SoundMufflerVolumeC2SPacket(pos, categoryIndex, delta);
        try {
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
            if (server != null) {
                server.execute(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().isEmpty() ? null : server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        net.minecraft.world.level.block.entity.BlockEntity be = player.level().getBlockEntity(packet.pos());
                        if (be instanceof net.unfamily.iskautils.block.entity.SoundMufflerBlockEntity muffler) {
                            muffler.addVolume(packet.categoryIndex(), packet.delta());
                            player.level().playSound(null, packet.pos(), net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, 1.0f);
                        }
                    }
                });
                return;
            }
        } catch (Exception ignored) {}
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendSoundMufflerRangePacket(BlockPos pos, int range) {
        var packet = new net.unfamily.iskautils.network.packet.SoundMufflerRangeC2SPacket(pos, range);
        try {
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
            if (server != null) {
                server.execute(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().isEmpty() ? null : server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        net.minecraft.world.level.block.entity.BlockEntity be = player.level().getBlockEntity(pos);
                        if (be instanceof net.unfamily.iskautils.block.entity.SoundMufflerBlockEntity muffler) {
                            muffler.setRange(range);
                            player.level().playSound(null, pos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, 1.0f);
                        }
                    }
                });
                return;
            }
        } catch (Exception ignored) {}
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendSoundMufflerModeTogglePacket(BlockPos pos) {
        var packet = new net.unfamily.iskautils.network.packet.SoundMufflerModeToggleC2SPacket(pos);
        try {
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
            if (server != null) {
                server.execute(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().isEmpty() ? null : server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        net.minecraft.world.level.block.entity.BlockEntity be = player.level().getBlockEntity(pos);
                        if (be instanceof net.unfamily.iskautils.block.entity.SoundMufflerBlockEntity muffler) {
                            muffler.toggleAllowList();
                            player.level().playSound(null, pos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, 1.0f);
                        }
                    }
                });
                return;
            }
        } catch (Exception ignored) {}
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendSoundMufflerFilterUpdatePacket(BlockPos pos, java.util.List<String> filterSoundIds) {
        var packet = new net.unfamily.iskautils.network.packet.SoundMufflerFilterUpdateC2SPacket(pos, filterSoundIds != null ? new java.util.ArrayList<>(filterSoundIds) : new java.util.ArrayList<>());
        try {
            MinecraftServer server = ClientRuntimeAccess.getSingleplayerServer();
            if (server != null) {
                server.execute(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().isEmpty() ? null : server.getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        net.minecraft.world.level.block.entity.BlockEntity be = player.level().getBlockEntity(pos);
                        if (be instanceof net.unfamily.iskautils.block.entity.SoundMufflerBlockEntity muffler) {
                            muffler.setFilterSoundIds(packet.filterSoundIds());
                            player.level().playSound(null, pos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, 1.0f);
                        }
                    }
                });
                return;
            }
        } catch (Exception ignored) {}
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendFanPushPullSetPacket(BlockPos pos, boolean pull) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new net.unfamily.iskautils.network.packet.FanPushPullSetC2SPacket(pos, pull));
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendFanTargetTypeSetPacket(BlockPos pos, int typeId) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new net.unfamily.iskautils.network.packet.FanTargetTypeSetC2SPacket(pos, typeId));
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendMobReaperTargetTypePacket(BlockPos pos, boolean backward) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new net.unfamily.iskautils.network.packet.MobReaperTargetTypeC2SPacket(pos, backward));
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendMobReaperRedstoneModePacket(BlockPos pos, boolean backward) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new net.unfamily.iskautils.network.packet.MobReaperRedstoneModeC2SPacket(pos, backward));
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendMobReaperAgeFilterPacket(BlockPos pos, boolean backward) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new net.unfamily.iskautils.network.packet.MobReaperAgeFilterC2SPacket(pos, backward));
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendCollectingCrateModePacket(BlockPos pos, boolean backward) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new net.unfamily.iskautils.network.packet.CollectingCrateModeC2SPacket(pos, backward));
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendCollectingCrateRedstoneModePacket(BlockPos pos, boolean backward) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new net.unfamily.iskautils.network.packet.CollectingCrateRedstoneModeC2SPacket(pos, backward));
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendCollectingCrateXpCollectPacket(BlockPos pos) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new net.unfamily.iskautils.network.packet.CollectingCrateXpCollectC2SPacket(pos));
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendCollectingCrateXpDepositPacket(BlockPos pos) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new net.unfamily.iskautils.network.packet.CollectingCrateXpDepositC2SPacket(pos));
    }

    public static void sendCollectingCrateSizePacket(BlockPos pos, int direction, boolean increment, int amount) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new net.unfamily.iskautils.network.packet.CollectingCrateSizeC2SPacket(pos, direction, increment, amount));
    }

    public static void sendCollectingCratePreviewTogglePacket(BlockPos pos, boolean enable) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new net.unfamily.iskautils.network.packet.CollectingCratePreviewToggleC2SPacket(pos, enable));
    }

    /**
     * Sends Fan Push/Pull packet to the server
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendFanPushPullPacket(BlockPos pos, boolean backward) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new net.unfamily.iskautils.network.packet.FanPushPullC2SPacket(pos, backward));
    }

    /**
     * Sends Fan Push Type packet to the server
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendFanPushTypePacket(BlockPos pos, boolean backward) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new net.unfamily.iskautils.network.packet.FanPushTypeC2SPacket(pos, backward));
    }

} 