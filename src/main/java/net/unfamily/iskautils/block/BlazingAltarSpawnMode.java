package net.unfamily.iskautils.block;

/** Natural spawn filter for the Blazing Altar area (always active when cycled — no off state). */
public enum BlazingAltarSpawnMode {
    HOSTILE(0),
    BOTH(1),
    PASSIVE(2);

    private final int id;

    BlazingAltarSpawnMode(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static BlazingAltarSpawnMode fromId(int id) {
        for (BlazingAltarSpawnMode mode : values()) {
            if (mode.id == id) {
                return mode;
            }
        }
        return HOSTILE;
    }

    /** Migrates legacy save ids: 0=OFF→HOSTILE, 1=ALL→BOTH, 2=HOSTILE, 3=PASSIVE. */
    public static BlazingAltarSpawnMode fromLegacyId(int legacyId) {
        return switch (legacyId) {
            case 1 -> BOTH;
            case 3 -> PASSIVE;
            default -> HOSTILE;
        };
    }

    public BlazingAltarSpawnMode next() {
        return switch (this) {
            case HOSTILE -> BOTH;
            case BOTH -> PASSIVE;
            case PASSIVE -> HOSTILE;
        };
    }

    public BlazingAltarSpawnMode previous() {
        return switch (this) {
            case HOSTILE -> PASSIVE;
            case BOTH -> HOSTILE;
            case PASSIVE -> BOTH;
        };
    }
}
