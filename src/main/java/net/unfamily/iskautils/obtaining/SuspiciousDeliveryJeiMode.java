package net.unfamily.iskautils.obtaining;

/**
 * How a suspicious delivery loot entry appears in JEI.
 */
public enum SuspiciousDeliveryJeiMode {
    SHOW,
    HIDDEN,
    MASK;

    public static SuspiciousDeliveryJeiMode fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return SHOW;
        }
        return switch (raw.toLowerCase()) {
            case "hidden" -> HIDDEN;
            case "mask" -> MASK;
            default -> SHOW;
        };
    }
}
