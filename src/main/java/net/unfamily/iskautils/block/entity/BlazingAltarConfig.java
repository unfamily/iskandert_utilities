package net.unfamily.iskautils.block.entity;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.unfamily.iskautils.block.BlazingAltarSpawnMode;

import java.util.UUID;

/** Persistent blazing-altar settings stored on the block entity in the world. */
public final class BlazingAltarConfig {
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

    public void save(ValueOutput output) {
        output.putLong("InstanceId", instanceId);
        output.putInt("ChunkRadius", chunkRadius);
        output.putBoolean("GroundOnly", groundOnly);
        output.putInt("SpawnMode", spawnMode.getId());
        output.putInt("RedstoneMode", redstoneMode);
        output.putInt("TickCounter", tickCounter);
        output.putInt("PlacementChunkIndex", placementChunkIndex);
    }

    public void load(ValueInput input) {
        long loadedId = input.getLongOr("InstanceId", Long.MIN_VALUE);
        if (loadedId != Long.MIN_VALUE) {
            instanceId = loadedId;
        } else {
            assignNewInstanceId();
        }
        chunkRadius = input.getIntOr("ChunkRadius", 1);
        if (chunkRadius < 1) {
            chunkRadius = 1;
        }
        groundOnly = input.getBooleanOr("GroundOnly", true);
        spawnMode = BlazingAltarSpawnMode.fromLegacyId(input.getIntOr("SpawnMode", BlazingAltarSpawnMode.HOSTILE.getId()));
        redstoneMode = input.getIntOr("RedstoneMode", 0);
        if (redstoneMode == 3) {
            redstoneMode = 4;
        }
        tickCounter = input.getIntOr("TickCounter", 0);
        placementChunkIndex = Math.max(0, input.getIntOr("PlacementChunkIndex", 0));
    }
}
