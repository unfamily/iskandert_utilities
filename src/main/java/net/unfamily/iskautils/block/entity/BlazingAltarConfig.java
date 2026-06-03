package net.unfamily.iskautils.block.entity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.unfamily.iskautils.block.BlazingAltarSpawnMode;

import java.util.UUID;

/** Persistent blazing-altar settings stored on the block entity in the world. */
public final class BlazingAltarConfig {
    private static final String TAG_INSTANCE_ID = "InstanceId";
    private static final String TAG_CHUNK_RADIUS = "ChunkRadius";
    private static final String TAG_GROUND_ONLY = "GroundOnly";
    private static final String TAG_SPAWN_MODE = "SpawnMode";
    private static final String TAG_REDSTONE_MODE = "RedstoneMode";
    private static final String TAG_TICK_COUNTER = "TickCounter";
    private static final String TAG_PLACEMENT_CHUNK_INDEX = "PlacementChunkIndex";

    private long instanceId;
    private int chunkRadius = 1;
    private boolean groundOnly = true;
    private BlazingAltarSpawnMode spawnMode = BlazingAltarSpawnMode.HOSTILE;
    private int redstoneMode;
    private int tickCounter;
    private int placementChunkIndex;

    public BlazingAltarConfig() {
        assignNewInstanceId();
    }

    public long getInstanceId() {
        return instanceId;
    }

    public void assignNewInstanceId() {
        instanceId = UUID.randomUUID().getLeastSignificantBits();
    }

    public int getChunkRadius() {
        return chunkRadius;
    }

    public void setChunkRadius(int chunkRadius) {
        this.chunkRadius = chunkRadius;
    }

    public boolean isGroundOnly() {
        return groundOnly;
    }

    public void setGroundOnly(boolean groundOnly) {
        this.groundOnly = groundOnly;
    }

    public BlazingAltarSpawnMode getSpawnMode() {
        return spawnMode;
    }

    public void setSpawnMode(BlazingAltarSpawnMode spawnMode) {
        this.spawnMode = spawnMode;
    }

    public int getRedstoneMode() {
        return redstoneMode;
    }

    public void setRedstoneMode(int redstoneMode) {
        this.redstoneMode = redstoneMode;
    }

    public int getTickCounter() {
        return tickCounter;
    }

    public void setTickCounter(int tickCounter) {
        this.tickCounter = tickCounter;
    }

    public int getPlacementChunkIndex() {
        return placementChunkIndex;
    }

    public void setPlacementChunkIndex(int placementChunkIndex) {
        this.placementChunkIndex = placementChunkIndex;
    }

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong(TAG_INSTANCE_ID, instanceId);
        tag.putInt(TAG_CHUNK_RADIUS, chunkRadius);
        tag.putBoolean(TAG_GROUND_ONLY, groundOnly);
        tag.putInt(TAG_SPAWN_MODE, spawnMode.getId());
        tag.putInt(TAG_REDSTONE_MODE, redstoneMode);
        tag.putInt(TAG_TICK_COUNTER, tickCounter);
        tag.putInt(TAG_PLACEMENT_CHUNK_INDEX, placementChunkIndex);
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains(TAG_INSTANCE_ID)) {
            instanceId = tag.getLong(TAG_INSTANCE_ID);
        } else {
            assignNewInstanceId();
        }
        chunkRadius = tag.getInt(TAG_CHUNK_RADIUS);
        if (chunkRadius < 1) {
            chunkRadius = 1;
        }
        groundOnly = !tag.contains(TAG_GROUND_ONLY) || tag.getBoolean(TAG_GROUND_ONLY);
        spawnMode = BlazingAltarSpawnMode.fromLegacyId(tag.getInt(TAG_SPAWN_MODE));
        redstoneMode = tag.contains(TAG_REDSTONE_MODE) ? tag.getInt(TAG_REDSTONE_MODE) : 0;
        if (redstoneMode == 3) {
            redstoneMode = 4;
        }
        tickCounter = tag.getInt(TAG_TICK_COUNTER);
        placementChunkIndex = Math.max(0, tag.getInt(TAG_PLACEMENT_CHUNK_INDEX));
    }
}
