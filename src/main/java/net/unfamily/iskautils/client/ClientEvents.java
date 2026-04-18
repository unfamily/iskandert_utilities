package net.unfamily.iskautils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.unfamily.iskalib.client.marker.MarkRenderer;

/**
 * Client-side helpers (key thread). World marker rendering is registered in
 * {@link net.unfamily.iskalib.client.marker.VanillaWorldMarkerClientHooks} (iska_lib).
 */
public class ClientEvents {

    private static volatile boolean threadActive = false;

    public static void init() {
        if (threadActive) {
            return;
        }

        threadActive = true;

        Thread keyCheckThread = new Thread(() -> {
            while (threadActive) {
                try {
                    Thread.sleep(100);

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

        keyCheckThread.setDaemon(true);
        keyCheckThread.start();
    }

    public static void shutdown() {
        threadActive = false;
    }

    private static void checkKeysInClientThread() {
        if (Minecraft.getInstance().screen == null && Minecraft.getInstance().player != null) {
            KeyBindings.checkKeys();

            if (KeyBindings.consumeStructureUndoKeyClick()) {
                net.unfamily.iskautils.network.ModMessages.sendStructureUndoPacket();
            }
        }
    }

    /** @see MarkRenderer#addHighlightedBlock */
    public static void handleAddHighlight(BlockPos pos, int color, int durationTicks) {
        MarkRenderer.getInstance().addHighlightedBlock(pos, color, durationTicks);
    }

    public static void handleAddHighlightWithName(BlockPos pos, int color, int durationTicks, String name) {
        MarkRenderer.getInstance().addHighlightedBlock(pos, color, durationTicks, name);
    }

    public static void handleAddBillboard(BlockPos pos, int color, int durationTicks) {
        MarkRenderer.getInstance().addBillboardMarker(pos, color, durationTicks);
    }

    public static void handleAddBillboardWithName(BlockPos pos, int color, int durationTicks, String name) {
        MarkRenderer.getInstance().addBillboardMarker(pos, color, durationTicks, name);
    }

    public static void handleRemoveHighlight(BlockPos pos) {
        MarkRenderer.getInstance().removeHighlightedBlock(pos);
        MarkRenderer.getInstance().removeBillboardMarker(pos);
    }

    public static void handleClearHighlights() {
        MarkRenderer.getInstance().clearHighlightedBlocks();
    }
}
