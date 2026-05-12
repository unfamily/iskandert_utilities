package net.unfamily.iskautils.block.entity;

/**
 * Redstone behavior modes for Structure Placer Machine.
 * Top-level enum avoids inner-class loader edge cases (NoClassDefFoundError on nested RedstoneMode).
 */
public enum StructurePlacerRedstoneMode {
    NONE(0),
    LOW(1),
    HIGH(2),
    PULSE(3),
    DISABLED(4);

    private final int value;

    StructurePlacerRedstoneMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static StructurePlacerRedstoneMode fromValue(int value) {
        for (StructurePlacerRedstoneMode mode : values()) {
            if (mode.value == value) {
                return mode;
            }
        }
        return NONE;
    }

    public StructurePlacerRedstoneMode next() {
        return switch (this) {
            case NONE -> LOW;
            case LOW -> HIGH;
            case HIGH -> PULSE;
            case PULSE -> DISABLED;
            case DISABLED -> NONE;
        };
    }

    public StructurePlacerRedstoneMode previous() {
        return switch (this) {
            case NONE -> DISABLED;
            case LOW -> NONE;
            case HIGH -> LOW;
            case PULSE -> HIGH;
            case DISABLED -> PULSE;
        };
    }
}
