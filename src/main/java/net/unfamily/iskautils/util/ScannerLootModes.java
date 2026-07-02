package net.unfamily.iskautils.util;

/**
 * Loot container scan modes for scanner generic target strings ({@code loot:1}, {@code loot:2}, {@code loot:3}).
 */
public final class ScannerLootModes {
    public static final String MODE_1 = "loot:1";
    public static final String MODE_2 = "loot:2";
    public static final String MODE_3 = "loot:3";

    private ScannerLootModes() {}

    public static boolean isLootScanTarget(String genericTarget) {
        return genericTarget != null && genericTarget.startsWith("loot:");
    }

    public static int normalizedMode(String genericTarget, boolean lootrLoaded) {
        int mode = parseMode(genericTarget);
        if (mode == 3 && !lootrLoaded) {
            return 2;
        }
        return mode;
    }

    public static int parseMode(String genericTarget) {
        if (genericTarget == null || !genericTarget.startsWith("loot:")) {
            return 1;
        }
        try {
            int mode = Integer.parseInt(genericTarget.substring("loot:".length()));
            if (mode >= 1 && mode <= 3) {
                return mode;
            }
        } catch (NumberFormatException ignored) {
        }
        return 1;
    }

    public static String toTarget(int mode) {
        return "loot:" + mode;
    }

    public static String cycleLootTarget(String current, boolean lootrLoaded) {
        int mode = parseMode(current);
        if (lootrLoaded) {
            int next = mode >= 3 ? 1 : mode + 1;
            return toTarget(next);
        }
        int next = mode >= 2 ? 1 : mode + 1;
        return toTarget(next);
    }
}
