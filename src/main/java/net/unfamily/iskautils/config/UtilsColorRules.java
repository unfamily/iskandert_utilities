package net.unfamily.iskautils.config;

import net.neoforged.fml.config.ModConfig;
import net.unfamily.anotherconfigmanager.config.ColorRuleRegistry;
import net.unfamily.iskautils.IskaUtils;

/**
 * Registers default (hidden) color bindings for Utils scanner hex string params.
 * Values stay bare RRGGBB ({@code prefix=none}); read still accepts {@code #}/{@code 0x}.
 */
public final class UtilsColorRules {
    private static final String MOD = IskaUtils.MOD_ID;
    private static final String HEX_RGB_BARE = "hex;rgb;prefix=none";

    private UtilsColorRules() {}

    public static void register() {
        hex("scanner.007_scannerDefaultOreColor");
        hex("scanner.008_scannerDefaultMobColor");
        hex("scanner.009_scannerDefaultLootColor");
        hex("scanner.010_scannerDefaultLootrColor");
        hex("scanner.011_scannerDefaultLiquidColor");
        hex("scanner.012_scannerDefaultSpawnerColor");
    }

    private static void hex(String configPath) {
        ColorRuleRegistry.register(MOD, ModConfig.Type.COMMON, configPath, HEX_RGB_BARE);
    }
}
