package net.unfamily.iskautils.config;

import net.unfamily.anotherconfigmanager.config.CsvRuleRegistry;
import net.unfamily.iskautils.IskaUtils;

/**
 * Registers in-memory CSV editing schemas for true multi-column Utils string lists.
 * Plain string-id lists use the generic list editor; declare CSV via Library UI if needed.
 */
public final class UtilsCsvRules {
    private static final String MOD = IskaUtils.MOD_ID;

    private UtilsCsvRules() {}

    public static void register() {
        // Two-column "key;color" scanner rows (separator ';', finite 0-1).
        keyColor("scanner.100_scanner_ore_entries");
        keyColor("scanner.101_scanner_mob_entries");
        keyColor("scanner.103_scanner_loot_entries");
        keyColor("scanner.107_scanner_loot_entity_entries");
        keyColor("scanner.105_scanner_fluid_entries");
        keyColor("scanner.106_scanner_spawner_entries");
    }

    private static void keyColor(String configPath) {
        String path = "iska_utils:" + configPath.replace('.', '/');
        CsvRuleRegistry.register(
                MOD,
                configPath,
                ";" + path + ";0=iska_utils.config.csv.key;type:color_hex:order=rgb:prefix=none;1=iska_utils.config.csv.color"
        );
    }
}
