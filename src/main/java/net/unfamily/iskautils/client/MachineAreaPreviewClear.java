package net.unfamily.iskautils.client;

import net.minecraft.core.BlockPos;
import net.unfamily.iskalib.client.marker.AreaBorderRenderer;
import net.unfamily.iskalib.client.marker.MarkRenderer;

/**
 * Clears every client-side area preview type tied to one machine block position.
 */
public final class MachineAreaPreviewClear {

    private MachineAreaPreviewClear() {}

    public static void clearAllForOwner(BlockPos owner) {
        if (owner == null) {
            return;
        }
        BlockPos key = owner.immutable();
        MachinePreviewTracker.deactivateOwner(key);
        BlazingAltarAreaPreview.clear(key);
        TemporalOverclockerAreaPreview.clear(key);
        AreaBorderRenderer.getInstance().clearArea(nullifierAreaKey(key));
        AreaBorderRenderer.getInstance().clearArea(collectingCrateAreaKey(key));
        MarkRenderer.getInstance().clearBillboardMarkersForOwner(key);
    }

    public static Object nullifierAreaKey(BlockPos pos) {
        return "nullifier_area_" + pos.toShortString();
    }

    public static Object collectingCrateAreaKey(BlockPos pos) {
        return "collecting_crate_area_" + pos.toShortString();
    }
}
