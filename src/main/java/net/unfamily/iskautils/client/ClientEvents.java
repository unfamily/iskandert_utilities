package net.unfamily.iskautils.client;

import net.unfamily.iskautils.util.ModLogger;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.FanBlockEntity;
import net.unfamily.iskautils.block.entity.StructurePlacerMachineBlockEntity;
import net.unfamily.iskautils.network.packet.FanShowAreaC2SPacket;
import net.unfamily.iskautils.network.packet.StructurePlacerMachineTogglePreviewC2SPacket;
import net.minecraft.client.renderer.LevelRenderer;
import net.unfamily.iskalib.client.marker.MarkRenderer;

/**
 * Class that manages client-specific events
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {
    private static final ModLogger LOGGER = ModLogger.of(ClientEvents.class);
    
    /**
     * Flag to check if the thread is active
     */
    private static volatile boolean threadActive = false;

    /**
     * Registers the client tick event
     */
    public static void init() {
        // Avoid initializing multiple times
        if (threadActive) {
            return;
        }
        
        threadActive = true;
        
        // Create a dedicated thread for key checking
        Thread keyCheckThread = new Thread(() -> {
            while (threadActive) {
                try {
                    // Check keys every 100ms
                    Thread.sleep(100);
                    
                    // Execute key checking only in the client thread
                    if (Minecraft.getInstance() != null) {
                        Minecraft.getInstance().execute(ClientEvents::checkKeysInClientThread);
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    // Continue running despite errors
                }
            }
        }, "VectorCharmKeyChecker");
        
        // Set the thread as daemon so it stops when the game is closed
        keyCheckThread.setDaemon(true);
        keyCheckThread.start();
    }
    
    /**
     * Method to stop the key checking thread
     */
    public static void shutdown() {
        threadActive = false;
    }

    /**
     * Check keys in the client thread
     */
    private static void checkKeysInClientThread() {
        // Check keys only if there is no GUI open
        if (Minecraft.getInstance().screen == null && Minecraft.getInstance().player != null) {
            KeyBindings.checkKeys();
            
            // Check structure undo key
            if (KeyBindings.consumeStructureUndoKeyClick()) {
                net.unfamily.iskautils.network.ModMessages.sendStructureUndoPacket();
            }
        }
        
        // We no longer apply movement here, as it's done directly by the item tick methods
    }
    
    /**
     * Render the marks during the world rendering
     */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS) {
            // Area borders are drawn by iska_lib VanillaWorldMarkerClientHooks (needs full stage event).
            MarkRenderer.getInstance().render(event.getPoseStack(), 0.0f);
        }
    }
    
    /**
     * Handles adding a highlighted block from the server
     */
    public static void handleAddHighlight(BlockPos pos, int color, int durationTicks) {
        MarkRenderer.getInstance().addHighlightedBlock(pos, color, durationTicks);
    }
    
    /**
     * Handles adding a highlighted block with name from the server
     */
    public static void handleAddHighlightWithName(BlockPos pos, int color, int durationTicks, String name) {
        MarkRenderer.getInstance().addHighlightedBlock(pos, color, durationTicks, name);
    }
    
    /**
     * Handles adding a billboard marker from the server
     */
    public static void handleAddBillboard(BlockPos pos, int color, int durationTicks) {
        MarkRenderer.getInstance().addBillboardMarker(pos, color, durationTicks);
    }

    public static void handleAddOwnedBillboard(BlockPos owner, BlockPos pos, int color, int durationTicks) {
        MarkRenderer.getInstance().addBillboardMarker(owner, pos, color, durationTicks);
    }

    public static void handleClearPreviewForOwner(BlockPos owner) {
        MarkRenderer.getInstance().clearBillboardMarkersForOwner(owner);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        MachinePreviewTracker.tickPeriodicReconcile(mc.level);
        TemporalOverclockerAreaPreview.tick(mc.level);
        for (BlockPos ownerPos : MachinePreviewTracker.pollOwnersNeedingWorldRefresh(mc.level)) {
            MachinePreviewTracker.onFootprintRefreshRequested(mc.level, ownerPos);
            var be = mc.level.getBlockEntity(ownerPos);
            if (be instanceof FanBlockEntity fan && fan.isShowAreaEnabled()) {
                PacketDistributor.sendToServer(new FanShowAreaC2SPacket(ownerPos, true));
            } else if (be instanceof StructurePlacerMachineBlockEntity machine && machine.isShowPreview()) {
                PacketDistributor.sendToServer(new StructurePlacerMachineTogglePreviewC2SPacket(ownerPos, true));
            }
        }
    }

    @SubscribeEvent
    public static void onClientPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof LocalPlayer) {
            MachinePreviewTracker.clearAll();
            BlazingAltarAreaPreview.clearAll();
            TemporalOverclockerAreaPreview.clearAll();
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof net.minecraft.world.level.Level level) {
            MachinePreviewTracker.onBlockInPreviewChanged(level, event.getPos());
            BlazingAltarAreaPreview.clear(event.getPos());
            TemporalOverclockerAreaPreview.clear(event.getPos());
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof net.minecraft.world.level.Level level) {
            MachinePreviewTracker.onBlockInPreviewChanged(level, event.getPos());
        }
    }
    
    /**
     * Handles adding a billboard marker with name from the server
     */
    public static void handleAddBillboardWithName(BlockPos pos, int color, int durationTicks, String name) {
        MarkRenderer.getInstance().addBillboardMarker(pos, color, durationTicks, name);
    }
    
    /**
     * Handles removing a highlighted block from the server
     */
    public static void handleRemoveHighlight(BlockPos pos) {
        MarkRenderer.getInstance().removeHighlightedBlock(pos);
        // Also remove any billboard markers at the same position
        MarkRenderer.getInstance().removeBillboardMarker(pos);
    }
    
    /**
     * Handles clearing all highlighted blocks from the server
     */
    public static void handleClearHighlights() {
        MarkRenderer.getInstance().clearHighlightedBlocks();
    }
} 