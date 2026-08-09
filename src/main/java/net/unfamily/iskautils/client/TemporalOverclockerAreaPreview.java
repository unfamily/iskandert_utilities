package net.unfamily.iskautils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.entity.TemporalOverclockerBlockEntity;
import net.unfamily.iskautils.util.preview.MachinePreviewMarkerLogic;
import net.unfamily.iskalib.client.marker.AreaBorderRenderer;
import net.unfamily.iskalib.client.marker.MarkRenderer;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client preview for Temporal Overclocker: link-range cube border plus billboard markers
 * on each linked block (air = red, solid = green), refreshed while Show is active.
 */
public final class TemporalOverclockerAreaPreview {

    private static final int DURATION_TICKS = MachinePreviewTracker.MACHINE_PREVIEW_DURATION_TICKS;
    private static final int REFRESH_INTERVAL_TICKS = 5;

    private static final Set<BlockPos> activeOwners = ConcurrentHashMap.newKeySet();
    private static long lastRefreshGameTime = Long.MIN_VALUE;

    private TemporalOverclockerAreaPreview() {}

    public static boolean isActive(BlockPos owner) {
        return owner != null && activeOwners.contains(owner.immutable());
    }

    public static void setActive(BlockPos owner, boolean active) {
        if (owner == null) {
            return;
        }
        BlockPos key = owner.immutable();
        if (active) {
            activeOwners.add(key);
        } else {
            activeOwners.remove(key);
            clearVisuals(key);
        }
    }

    public static void refresh(BlockPos owner) {
        if (owner == null || !activeOwners.contains(owner.immutable())) {
            return;
        }
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        BlockPos key = owner.immutable();
        if (!(level.getBlockEntity(key) instanceof TemporalOverclockerBlockEntity overclocker)) {
            clearVisuals(key);
            return;
        }

        int r = Config.temporalOverclockerLinkRange;
        AreaBorderRenderer.getInstance().showArea(
                areaBorderKey(key),
                key.offset(-r, -r, -r),
                key.offset(r, r, r),
                AreaBorderRenderer.DEFAULT_MACHINE_COLOR,
                0);

        MarkRenderer renderer = MarkRenderer.getInstance();
        renderer.clearBillboardMarkersForOwner(key);
        for (BlockPos linked : overclocker.getLinkedBlocks()) {
            int color = MachinePreviewMarkerLogic.colorForMarkedPresence(level.getBlockState(linked));
            renderer.addBillboardMarker(key, linked, color, DURATION_TICKS);
        }
    }

    /** Periodic refresh so linked list and air/solid colors stay up to date while Show is on. */
    public static void tick(Level level) {
        if (level == null || activeOwners.isEmpty()) {
            return;
        }
        long gameTime = level.getGameTime();
        if (lastRefreshGameTime != Long.MIN_VALUE
                && gameTime - lastRefreshGameTime < REFRESH_INTERVAL_TICKS) {
            return;
        }
        lastRefreshGameTime = gameTime;
        for (BlockPos owner : activeOwners) {
            refresh(owner);
        }
    }

    public static void clear(BlockPos owner) {
        if (owner == null) {
            return;
        }
        BlockPos key = owner.immutable();
        activeOwners.remove(key);
        clearVisuals(key);
    }

    public static void clearAll() {
        for (BlockPos key : activeOwners) {
            clearVisuals(key);
        }
        activeOwners.clear();
        lastRefreshGameTime = Long.MIN_VALUE;
    }

    private static void clearVisuals(BlockPos key) {
        MarkRenderer.getInstance().clearBillboardMarkersForOwner(key);
        AreaBorderRenderer.getInstance().clearArea(areaBorderKey(key));
    }

    private static Object areaBorderKey(BlockPos owner) {
        return "temporal_overclocker_area_" + owner.toShortString();
    }
}
