package net.unfamily.iskautils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.unfamily.iskautils.block.FanBlock;
import net.unfamily.iskautils.block.entity.FanBlockEntity;
import net.unfamily.iskautils.block.entity.StructurePlacerMachineBlockEntity;
import net.unfamily.iskautils.util.preview.MachinePreviewMarkerLogic;
import net.unfamily.iskalib.client.marker.AreaBorderRenderer;
import net.unfamily.iskalib.client.marker.MarkRenderer;
import net.unfamily.iskalib.structure.StructureDefinition;
import net.unfamily.iskalib.structure.StructureLoader;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client footprint preview for Fan and Structure Placer Machine (owned markers via iskalib).
 */
public final class MachinePreviewTracker {

    public static final int MACHINE_PREVIEW_DURATION_TICKS = 7_200_000;

    private static final int WORLD_POLL_INTERVAL_TICKS = 5;
    private static final int REFRESH_COOLDOWN_TICKS = 20;
    private static final int PERIODIC_RECONCILE_INTERVAL_TICKS = 40;

    private static final int COLOR_OCCUPIED_FAN = MachinePreviewMarkerLogic.COLOR_OCCUPIED_FAN;
    private static final int COLOR_OCCUPIED_STRUCTURE = MachinePreviewMarkerLogic.COLOR_OCCUPIED_STRUCTURE;
    private static final int COLOR_FRAME_EDGE = MachinePreviewMarkerLogic.COLOR_FRAME_EDGE;
    private static final int COLOR_VALID = MachinePreviewMarkerLogic.COLOR_VALID;

    private static final int[] BASELINE_COLOR_PRIORITY = {
            COLOR_FRAME_EDGE,
            COLOR_VALID,
    };

    private static final Set<BlockPos> activePreviews = ConcurrentHashMap.newKeySet();
    private static final Map<BlockPos, Integer> lastWorldStateHashByOwner = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Integer> lastFootprintGeometryHashByOwner = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Integer> refreshCooldownByOwner = new ConcurrentHashMap<>();
    private static final Set<BlockPos> pendingWorldRefreshByOwner = ConcurrentHashMap.newKeySet();
    private static final Map<BlockPos, Integer> activeFootprintGeneration = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Map<BlockPos, Set<Integer>>> markerLayersByOwner = new ConcurrentHashMap<>();
    private static final Set<BlockPos> pendingLayerResetByOwner = ConcurrentHashMap.newKeySet();

    private static final int INVALID_FOOTPRINT_GENERATION = -1;

    private static long lastPollGameTime = Long.MIN_VALUE;
    private static long lastPeriodicReconcileGameTime = Long.MIN_VALUE;

    private MachinePreviewTracker() {}

    public static boolean isPreviewActive(BlockPos owner) {
        return owner != null && activePreviews.contains(owner.immutable());
    }

    public static void setPreviewActive(BlockPos owner, boolean active) {
        if (owner == null) {
            return;
        }
        BlockPos key = owner.immutable();
        if (active) {
            activePreviews.add(key);
            // Show the outer shell immediately; occupied cells arrive via server packets / reconcile.
            refreshFanAreaBorder(key);
            Level level = Minecraft.getInstance().level;
            if (level != null) {
                seedWorldHash(level, key);
            }
        } else {
            activePreviews.remove(key);
            clearTrackingForOwner(key);
            clearBillboardMarkers(key);
            clearAreaBorder(key);
        }
    }

    public static void clearMarkersForOwner(BlockPos owner) {
        if (owner == null) {
            return;
        }
        BlockPos key = owner.immutable();
        activeFootprintGeneration.put(key, INVALID_FOOTPRINT_GENERATION);
        clearBillboardMarkers(key);
        // Keep / refresh the fan outer shell — marker rebuild must not hide the area border.
        refreshFanAreaBorder(key);
    }

    public static void applyFootprintClear(BlockPos owner, int footprintGeneration) {
        if (owner == null) {
            return;
        }
        BlockPos key = owner.immutable();
        activeFootprintGeneration.put(key, footprintGeneration);
        pendingLayerResetByOwner.add(key);
        clearBillboardMarkers(key);
        refreshFanAreaBorder(key);
    }

    public static void deactivateOwner(BlockPos owner) {
        setPreviewActive(owner, false);
    }

    public static void clearAll() {
        MarkRenderer renderer = MarkRenderer.getInstance();
        for (BlockPos key : activePreviews) {
            renderer.clearBillboardMarkersForOwner(key);
            clearAreaBorder(key);
        }
        activePreviews.clear();
        lastWorldStateHashByOwner.clear();
        lastFootprintGeometryHashByOwner.clear();
        refreshCooldownByOwner.clear();
        pendingWorldRefreshByOwner.clear();
        activeFootprintGeneration.clear();
        markerLayersByOwner.clear();
        pendingLayerResetByOwner.clear();
        lastPollGameTime = Long.MIN_VALUE;
        lastPeriodicReconcileGameTime = Long.MIN_VALUE;
    }

    public static void onFootprintRefreshRequested(Level level, BlockPos owner) {
        if (level == null || owner == null) {
            return;
        }
        BlockPos key = owner.immutable();
        clearMarkersForOwner(key);
        reconcileMarkersForOwner(key);
        seedWorldHash(level, key);
        noteRefreshRequested(key);
    }

    public static void noteRefreshRequested(BlockPos owner) {
        if (owner != null) {
            refreshCooldownByOwner.put(owner.immutable(), REFRESH_COOLDOWN_TICKS);
        }
    }

    public static void seedWorldHash(Level level, BlockPos owner) {
        if (level == null || owner == null) {
            return;
        }
        AABB volume = resolveFootprintVolume(level, owner);
        if (volume != null) {
            BlockPos key = owner.immutable();
            lastWorldStateHashByOwner.put(key, computeOccupiedBlocksHash(level, volume));
            lastFootprintGeometryHashByOwner.put(key, computeFootprintGeometryHash(volume));
        }
    }

    public static void onBlockInPreviewChanged(Level level, BlockPos worldPos) {
        if (level == null || worldPos == null || activePreviews.isEmpty()) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(worldPos);
        for (BlockPos owner : List.copyOf(activePreviews)) {
            AABB volume = resolveFootprintVolume(level, owner);
            if (volume == null || !volume.contains(center)) {
                continue;
            }
            reconcileMarkersForOwner(owner);
            lastWorldStateHashByOwner.put(owner, computeOccupiedBlocksHash(level, volume));
        }
    }

    public static void addMarker(
            BlockPos owner, BlockPos worldPos, int color, int durationTicks, int footprintGeneration) {
        if (worldPos == null || owner == null || owner.equals(BlockPos.ZERO)) {
            return;
        }
        BlockPos ownerKey = owner.immutable();
        if (footprintGeneration != activeFootprintGeneration.getOrDefault(ownerKey, INVALID_FOOTPRINT_GENERATION)) {
            return;
        }
        if (pendingLayerResetByOwner.remove(ownerKey)) {
            clearLayerCacheForOwner(ownerKey);
        }
        BlockPos cell = worldPos.immutable();
        Level level = Minecraft.getInstance().level;
        if (level != null && isOccupiedColor(color)) {
            BlockState state = level.getBlockState(cell);
            if (state.isAir() || state.canBeReplaced()) {
                return;
            }
        }
        int duration = durationTicks > 0 ? durationTicks : MACHINE_PREVIEW_DURATION_TICKS;
        markerLayersByOwner
                .computeIfAbsent(ownerKey, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(cell, k -> ConcurrentHashMap.newKeySet())
                .add(color);
        syncResolvedMarker(ownerKey, cell, duration);
    }

    public static List<BlockPos> pollOwnersNeedingWorldRefresh(Level level) {
        if (level == null || activePreviews.isEmpty()) {
            return List.of();
        }
        long gameTime = level.getGameTime();
        if (gameTime - lastPollGameTime < WORLD_POLL_INTERVAL_TICKS) {
            return List.of();
        }
        lastPollGameTime = gameTime;

        for (BlockPos key : List.copyOf(refreshCooldownByOwner.keySet())) {
            int remaining = refreshCooldownByOwner.getOrDefault(key, 0) - WORLD_POLL_INTERVAL_TICKS;
            if (remaining <= 0) {
                refreshCooldownByOwner.remove(key);
            } else {
                refreshCooldownByOwner.put(key, remaining);
            }
        }

        List<BlockPos> needsRefresh = new ArrayList<>();
        for (BlockPos owner : activePreviews) {
            AABB volume = resolveFootprintVolume(level, owner);
            if (volume == null) {
                continue;
            }
            int hash = computeOccupiedBlocksHash(level, volume);
            int geomHash = computeFootprintGeometryHash(volume);
            Integer last = lastWorldStateHashByOwner.get(owner);
            Integer lastGeom = lastFootprintGeometryHashByOwner.get(owner);
            boolean geometryChanged = lastGeom != null && lastGeom != geomHash;
            if (last != null && last != hash) {
                reconcileMarkersForOwner(owner);
                lastWorldStateHashByOwner.put(owner, hash);
                if (refreshCooldownByOwner.containsKey(owner)) {
                    pendingWorldRefreshByOwner.add(owner);
                } else {
                    scheduleWorldRefresh(owner, needsRefresh);
                }
            } else if (last == null) {
                lastWorldStateHashByOwner.put(owner, hash);
            }
            if (geometryChanged) {
                reconcileMarkersForOwner(owner);
                lastFootprintGeometryHashByOwner.put(owner, geomHash);
                if (refreshCooldownByOwner.containsKey(owner)) {
                    pendingWorldRefreshByOwner.add(owner);
                } else {
                    scheduleWorldRefresh(owner, needsRefresh);
                }
            } else if (lastGeom == null) {
                lastFootprintGeometryHashByOwner.put(owner, geomHash);
            }
        }

        for (BlockPos owner : List.copyOf(pendingWorldRefreshByOwner)) {
            if (!refreshCooldownByOwner.containsKey(owner)) {
                pendingWorldRefreshByOwner.remove(owner);
                scheduleWorldRefresh(owner, needsRefresh);
            }
        }
        return needsRefresh;
    }

    public static void tickPeriodicReconcile(Level level) {
        if (level == null || activePreviews.isEmpty()) {
            return;
        }
        long gameTime = level.getGameTime();
        if (gameTime - lastPeriodicReconcileGameTime < PERIODIC_RECONCILE_INTERVAL_TICKS) {
            return;
        }
        lastPeriodicReconcileGameTime = gameTime;
        for (BlockPos owner : List.copyOf(activePreviews)) {
            reconcileMarkersForOwner(owner);
        }
    }

    private static void scheduleWorldRefresh(BlockPos owner, List<BlockPos> needsRefresh) {
        refreshCooldownByOwner.put(owner, REFRESH_COOLDOWN_TICKS);
        needsRefresh.add(owner);
    }

    private static void clearTrackingForOwner(BlockPos ownerKey) {
        lastWorldStateHashByOwner.remove(ownerKey);
        lastFootprintGeometryHashByOwner.remove(ownerKey);
        refreshCooldownByOwner.remove(ownerKey);
        pendingWorldRefreshByOwner.remove(ownerKey);
        activeFootprintGeneration.remove(ownerKey);
        pendingLayerResetByOwner.remove(ownerKey);
        clearLayerCacheForOwner(ownerKey);
    }

    private static void clearLayerCacheForOwner(BlockPos owner) {
        markerLayersByOwner.remove(owner);
    }

    private static void clearBillboardMarkers(BlockPos owner) {
        MarkRenderer.getInstance().clearBillboardMarkersForOwner(owner);
    }

    private static void clearAreaBorder(BlockPos owner) {
        AreaBorderRenderer.getInstance().clearArea(areaBorderKey(owner));
    }

    private static Object areaBorderKey(BlockPos owner) {
        return "machine_area_" + owner.toShortString();
    }

    private static void showAreaBorder(BlockPos owner, AABB aabb) {
        if (aabb == null) {
            return;
        }
        int minX = (int) Math.floor(aabb.minX);
        int minY = (int) Math.floor(aabb.minY);
        int minZ = (int) Math.floor(aabb.minZ);
        int maxX = (int) Math.floor(aabb.maxX) - 1;
        int maxY = (int) Math.floor(aabb.maxY) - 1;
        int maxZ = (int) Math.floor(aabb.maxZ) - 1;
        AreaBorderRenderer.getInstance().showArea(
                areaBorderKey(owner),
                new BlockPos(minX, minY, minZ),
                new BlockPos(maxX, maxY, maxZ),
                AreaBorderRenderer.DEFAULT_MACHINE_COLOR,
                0);
    }

    /** Recomputes the fan outer shell from the client block entity (call after range sync). */
    public static void refreshActiveAreaBorder(BlockPos owner) {
        if (owner != null) {
            refreshFanAreaBorder(owner.immutable());
        }
    }

    /** Outer push-area shell for fans; safe to call whenever the footprint may have changed. */
    private static void refreshFanAreaBorder(BlockPos owner) {
        if (owner == null || !activePreviews.contains(owner)) {
            return;
        }
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        AABB volume = resolveFootprintVolume(level, owner);
        if (volume != null && level.getBlockEntity(owner) instanceof FanBlockEntity) {
            showAreaBorder(owner, volume);
        }
    }

    private static void reconcileMarkersForOwner(BlockPos owner) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        refreshLayerCacheFromWorld(level, owner);
        refreshAllMarkersForOwner(owner, MACHINE_PREVIEW_DURATION_TICKS);
        refreshFanAreaBorder(owner);
    }

    private static void refreshLayerCacheFromWorld(Level level, BlockPos owner) {
        BlockEntity be = level.getBlockEntity(owner);
        if (be == null) {
            return;
        }
        Map<BlockPos, Set<Integer>> fresh = new ConcurrentHashMap<>();
        MachinePreviewMarkerLogic.MarkerSink sink = (worldPos, color) -> fresh
                .computeIfAbsent(worldPos.immutable(), k -> ConcurrentHashMap.newKeySet())
                .add(color);
        if (be instanceof FanBlockEntity fan && level.getBlockState(owner).getBlock() instanceof FanBlock) {
            MachinePreviewMarkerLogic.forEachFanMarker(level, fan, owner, sink);
        } else if (be instanceof StructurePlacerMachineBlockEntity machine) {
            String structureId = machine.getSelectedStructure();
            if (structureId == null || structureId.isEmpty()) {
                return;
            }
            StructureDefinition structure = StructureLoader.getStructure(structureId);
            if (structure == null) {
                return;
            }
            MachinePreviewMarkerLogic.forEachStructurePlacerMarker(
                    level, owner, structure, machine.getRotation(), sink);
        } else {
            return;
        }
        markerLayersByOwner.put(owner, fresh);
    }

    private static void syncResolvedMarker(BlockPos owner, BlockPos worldPos, int durationTicks) {
        Map<BlockPos, Set<Integer>> layers = markerLayersByOwner.get(owner);
        Set<Integer> colors = layers != null ? layers.get(worldPos) : null;
        if (colors == null || colors.isEmpty()) {
            refreshAllMarkersForOwner(owner, durationTicks);
            return;
        }
        Level level = Minecraft.getInstance().level;
        Integer resolved = level != null ? resolveDisplayColor(level, worldPos, colors) : null;
        if (resolved == null) {
            refreshAllMarkersForOwner(owner, durationTicks);
        } else {
            MarkRenderer.getInstance().addBillboardMarker(owner, worldPos, resolved, durationTicks);
        }
    }

    private static void refreshAllMarkersForOwner(BlockPos owner, int durationTicks) {
        Map<BlockPos, Set<Integer>> layers = markerLayersByOwner.get(owner);
        if (layers == null || layers.isEmpty()) {
            // Occupied markers only — never drop the fan area border here.
            clearBillboardMarkers(owner);
            return;
        }
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        clearBillboardMarkers(owner);
        for (Map.Entry<BlockPos, Set<Integer>> entry : layers.entrySet()) {
            Integer resolved = resolveDisplayColor(level, entry.getKey(), entry.getValue());
            if (resolved != null) {
                MarkRenderer.getInstance().addBillboardMarker(owner, entry.getKey(), resolved, durationTicks);
            }
        }
    }

    @Nullable
    private static Integer resolveDisplayColor(Level level, BlockPos worldPos, Set<Integer> layers) {
        if (layers.contains(COLOR_OCCUPIED_FAN) || layers.contains(COLOR_OCCUPIED_STRUCTURE)) {
            BlockState state = level.getBlockState(worldPos);
            if (!state.isAir() && !state.canBeReplaced()) {
                if (layers.contains(COLOR_OCCUPIED_STRUCTURE)) {
                    return COLOR_OCCUPIED_STRUCTURE;
                }
                return COLOR_OCCUPIED_FAN;
            }
        }
        for (int priority : BASELINE_COLOR_PRIORITY) {
            if (layers.contains(priority)) {
                return priority;
            }
        }
        for (int color : layers) {
            if (!isOccupiedColor(color)) {
                return color;
            }
        }
        return null;
    }

    private static boolean isOccupiedColor(int color) {
        return color == COLOR_OCCUPIED_FAN || color == COLOR_OCCUPIED_STRUCTURE;
    }

    @Nullable
    private static AABB resolveFootprintVolume(Level level, BlockPos ownerPos) {
        BlockEntity be = level.getBlockEntity(ownerPos);
        if (be instanceof FanBlockEntity fan && level.getBlockState(ownerPos).getBlock() instanceof FanBlock) {
            return FanBlockEntity.calculatePushArea(ownerPos, level.getBlockState(ownerPos).getValue(FanBlock.FACING), fan);
        }
        if (be instanceof StructurePlacerMachineBlockEntity machine) {
            return MachinePreviewMarkerLogic.getStructurePlacerFootprintAABB(ownerPos, machine);
        }
        return null;
    }

    private static int computeOccupiedBlocksHash(Level level, AABB volume) {
        int minX = (int) Math.floor(volume.minX);
        int minY = (int) Math.floor(volume.minY);
        int minZ = (int) Math.floor(volume.minZ);
        int maxX = (int) Math.floor(volume.maxX - 1e-6);
        int maxY = (int) Math.floor(volume.maxY - 1e-6);
        int maxZ = (int) Math.floor(volume.maxZ - 1e-6);

        int hash = 1;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockState state = level.getBlockState(new BlockPos(x, y, z));
                    if (!state.isAir() && !state.canBeReplaced()) {
                        hash = 31 * hash + x;
                        hash = 31 * hash + y;
                        hash = 31 * hash + z;
                        hash = 31 * hash + Block.getId(state);
                    }
                }
            }
        }
        return hash;
    }

    private static int computeFootprintGeometryHash(AABB volume) {
        int hash = 1;
        hash = 31 * hash + (int) Math.floor(volume.minX);
        hash = 31 * hash + (int) Math.floor(volume.minY);
        hash = 31 * hash + (int) Math.floor(volume.minZ);
        hash = 31 * hash + (int) Math.ceil(volume.maxX);
        hash = 31 * hash + (int) Math.ceil(volume.maxY);
        hash = 31 * hash + (int) Math.ceil(volume.maxZ);
        return hash;
    }
}
