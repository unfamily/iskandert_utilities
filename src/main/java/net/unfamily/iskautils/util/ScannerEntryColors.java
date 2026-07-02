package net.unfamily.iskautils.util;

import net.unfamily.iskautils.Config;

import java.util.List;

/**
 * Resolves scanner marker colors from config entry lists (same rules as ore/mob entries).
 */
public final class ScannerEntryColors {
    private ScannerEntryColors() {}

    public static int resolveColor(List<String> entries, String lookupKey, int defaultColor) {
        if (lookupKey == null || entries == null) {
            return defaultColor;
        }
        String lowerKey = lookupKey.toLowerCase();
        for (String entry : entries) {
            String[] parts = entry.split(";");
            if (parts.length != 2) {
                continue;
            }
            String pattern = parts[0];
            try {
                int color = Integer.parseInt(parts[1], 16);
                if (pattern.equals(lookupKey)) {
                    return color;
                }
                if (pattern.startsWith("$")) {
                    String searchTerm = pattern.substring(1).toLowerCase();
                    if (lowerKey.contains(searchTerm)) {
                        return color;
                    }
                } else if (lowerKey.contains(pattern.toLowerCase())) {
                    return color;
                }
            } catch (NumberFormatException ignored) {
                // Use next entry or default
            }
        }
        return defaultColor;
    }

    public static int applyAlpha(int rgb) {
        return (Config.scannerDefaultAlpha << 24) | (rgb & 0xFFFFFF);
    }

    public static int resolveLootColor(String colorKey, boolean lootr) {
        int color = resolveColor(Config.scannerLootEntries, colorKey,
                lootr ? Config.scannerDefaultLootrColor : Config.scannerDefaultLootColor);
        return applyAlpha(color);
    }

    public static int resolveLootEntityColor(String entityTypeId, boolean lootr) {
        int color = resolveColor(Config.scannerLootEntityEntries, entityTypeId,
                lootr ? Config.scannerDefaultLootrColor : Config.scannerDefaultLootColor);
        return applyAlpha(color);
    }

    public static int resolveFluidColor(String fluidId) {
        int color = resolveColor(Config.scannerFluidEntries, fluidId, Config.scannerDefaultLiquidColor);
        return applyAlpha(color);
    }

    public static int resolveSpawnerColor(String blockId) {
        int color = resolveColor(Config.scannerSpawnerEntries, blockId, Config.scannerDefaultSpawnerColor);
        return applyAlpha(color);
    }
}
