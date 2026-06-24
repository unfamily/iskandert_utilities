package net.unfamily.iskautils.block.entity;

/**
 * Redstone behavior modes for the Ender Nullifier.
 */
public enum EnderNullifierRedstoneMode {
    MANUAL(0),
    LOW(1),
    HIGH(2),
    PULSE(3);

    private final int value;

    EnderNullifierRedstoneMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static EnderNullifierRedstoneMode fromValue(int value) {
        for (EnderNullifierRedstoneMode mode : values()) {
            if (mode.value == value) {
                return mode;
            }
        }
        return MANUAL;
    }

    public EnderNullifierRedstoneMode next() {
        return switch (this) {
            case MANUAL -> LOW;
            case LOW -> HIGH;
            case HIGH -> PULSE;
            case PULSE -> MANUAL;
        };
    }
}
