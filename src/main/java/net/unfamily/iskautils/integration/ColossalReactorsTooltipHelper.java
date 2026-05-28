package net.unfamily.iskautils.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Adds Colossal Reactors config-based tooltips to shared upgrade modules.
 *
 * Uses reflection so no compile dependency on Colossal Reactors is required.
 */
public final class ColossalReactorsTooltipHelper {

    private static final String COLOSSAL_REACTORS_MOD_ID = "colossal_reactors";
    private static final String MEKANISM_MOD_ID = "mekanism";
    private static final String CONFIG_CLASS = "net.unfamily.colossal_reactors.Config";

    private static final int MAX_PRODUCTION_MODULES = 8;

    private ColossalReactorsTooltipHelper() {}

    /**
     * Radiation Scrubber accepts Production Modules only when the feature is enabled in Colossal Reactors config
     * and Mekanism is present (machine integration).
     */
    public static boolean supportsRadiationScrubberProductionModule() {
        if (!ModList.get().isLoaded(COLOSSAL_REACTORS_MOD_ID)) {
            return false;
        }
        if (!ModList.get().isLoaded(MEKANISM_MOD_ID)) {
            return false;
        }
        return getConfigBoolean("ENABLE_RADIATION_MANAGEMENT", false);
    }

    public static void addRadiationScrubberProductionModuleTooltip(List<Component> tooltip) {
        int intervalTicks = getConfigInt("RADIATION_SCRUBBER_INTERVAL_TICKS", 10);
        int energyPerTick = getConfigInt("RADIATION_SCRUBBER_ENERGY_PER_TICK", 100);
        int baseRemoval = getConfigInt("RADIATION_SCRUBBER_BASE_RADIATION_REMOVAL", 100);
        int baseGasDestroy = getConfigInt("RADIATION_SCRUBBER_BASE_GAS_DESTRUCTION", 10);

        tooltip.add(
                Component.translatable(
                                "tooltip.iska_utils.colossal_reactors.radiation_scrubber.production_module.line0",
                                MAX_PRODUCTION_MODULES)
                        .withStyle(ChatFormatting.GRAY));
        tooltip.add(
                Component.translatable(
                                "tooltip.iska_utils.colossal_reactors.radiation_scrubber.production_module.line1",
                                intervalTicks,
                                baseRemoval)
                        .withStyle(ChatFormatting.GRAY));
        tooltip.add(
                Component.translatable(
                                "tooltip.iska_utils.colossal_reactors.radiation_scrubber.production_module.line2",
                                energyPerTick)
                        .withStyle(ChatFormatting.GRAY));
        tooltip.add(
                Component.translatable(
                                "tooltip.iska_utils.colossal_reactors.radiation_scrubber.production_module.line3",
                                baseGasDestroy)
                        .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static boolean getConfigBoolean(String fieldName, boolean defaultValue) {
        try {
            Class<?> configClass = Class.forName(CONFIG_CLASS);
            Field field = configClass.getField(fieldName);
            Object spec = field.get(null);
            Method get = spec.getClass().getMethod("get");
            return (Boolean) get.invoke(spec);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static int getConfigInt(String fieldName, int defaultValue) {
        try {
            Class<?> configClass = Class.forName(CONFIG_CLASS);
            Field field = configClass.getField(fieldName);
            Object spec = field.get(null);
            Method get = spec.getClass().getMethod("get");
            return (Integer) get.invoke(spec);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}

