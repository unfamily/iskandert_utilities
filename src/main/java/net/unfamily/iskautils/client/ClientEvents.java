package net.unfamily.iskautils.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Class that manages client-specific events
 */
public class ClientEvents {

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
     * Render the marks during the world rendering (NeoForge 26+ requires a concrete {@link RenderLevelStageEvent} subclass).
     */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent.AfterLevel event) {
        PoseStack poseStack = event.getPoseStack();
        float partialTick = 0.0f;
        MarkRenderer.getInstance().render(poseStack, partialTick);
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