package net.unfamily.iskautils.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;

/**
 * Adds Pattern Crafter config-based tooltips to shared upgrade modules.
 * Uses reflection so no compile dependency on Pattern Crafter is required.
 * Only called when ModList.get().isLoaded("pattern_crafter") is true.
 */
public final class PatternCrafterTooltipHelper {

    private static final String CONFIG_CLASS = "net.unfamily.pattern_crafter.Config";
    private static final String PATTERN_CRAFTER_MOD_ID = "pattern_crafter";
    private static final int[] MIN_PRODUCTION_MODULE_VERSION = {1, 2, 0, 0, 0};

    private PatternCrafterTooltipHelper() {}

    /**
     * Production module tooltips require Pattern Crafter 1.2.0.0.0 or newer.
     */
    public static boolean supportsProductionModule() {
        if (!ModList.get().isLoaded(PATTERN_CRAFTER_MOD_ID)) {
            return false;
        }
        return ModList.get().getModContainerById(PATTERN_CRAFTER_MOD_ID)
                .map(container -> isAtLeastVersion(container.getModInfo().getVersion().toString(), MIN_PRODUCTION_MODULE_VERSION))
                .orElse(false);
    }

    public static boolean isPatternCrafterLoaded() {
        return ModList.get().isLoaded(PATTERN_CRAFTER_MOD_ID);
    }

    public static void appendSpeedModuleMaxInstall(Consumer<Component> tooltip) {
        if (!isPatternCrafterLoaded()) {
            return;
        }
        appendPatternCrafterMaxInstall(tooltip, getConfigInt("MAX_SPEED_MODULES", 1));
    }

    public static void appendLogicModuleMaxInstall(Consumer<Component> tooltip) {
        if (!isPatternCrafterLoaded()) {
            return;
        }
        appendPatternCrafterMaxInstall(tooltip, getConfigInt("MAX_LOGIC_MODULES", 3));
    }

    public static void appendProductionModuleMaxInstall(Consumer<Component> tooltip) {
        if (!supportsProductionModule()) {
            return;
        }
        appendPatternCrafterMaxInstall(tooltip, getConfigInt("MAX_PRODUCTION_MODULES", 1));
    }

    private static void appendPatternCrafterMaxInstall(Consumer<Component> tooltip, int max) {
        tooltip.accept(Component.translatable("tooltip.iska_utils.module_compat.pattern_crafter.max", max)
                .withStyle(ChatFormatting.GRAY));
    }

    /** Speed module types: slow, moderate, fast, extreme, ultra. */
    public static final String[] SPEED_TYPES = { "slow", "moderate", "fast", "extreme", "ultra" };

    /**
     * Appends Pattern Crafter tooltip line for a speed module (max count + crafting time).
     */
    public static void addSpeedModuleTooltip(List<Component> tooltip, String speedType) {
        int maxSpeed = getConfigInt("MAX_SPEED_MODULES", 1);
        double multiplier = getSpeedMultiplier(speedType);
        int percentInterval = (int) Math.round(multiplier * 100);
        tooltip.add(Component.translatable("tooltip.iska_utils.pattern_crafter.speed_module",
                maxSpeed, percentInterval)
                .withStyle(ChatFormatting.GRAY));
    }

    /**
     * Appends Pattern Crafter tooltip line for the logic module (max installable count).
     */
    public static void addLogicModuleTooltip(List<Component> tooltip) {
        int maxLogic = getConfigInt("MAX_LOGIC_MODULES", 3);
        tooltip.add(Component.translatable("tooltip.iska_utils.pattern_crafter.logic_module", maxLogic)
                .withStyle(ChatFormatting.GRAY));
    }

    /**
     * Appends Pattern Crafter tooltip for the production module (max 1, one stack per identical recipe).
     */
    public static void addProductionModuleTooltip(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.iska_utils.pattern_crafter.production_module")
                .withStyle(ChatFormatting.GRAY));
    }

    private static boolean isAtLeastVersion(String version, int[] minimum) {
        String[] parts = version.split("\\.");
        for (int i = 0; i < minimum.length; i++) {
            int current = 0;
            if (i < parts.length) {
                String digits = parts[i].replaceAll("[^0-9].*", "");
                if (!digits.isEmpty()) {
                    current = Integer.parseInt(digits);
                }
            }
            if (current > minimum[i]) {
                return true;
            }
            if (current < minimum[i]) {
                return false;
            }
        }
        return true;
    }

    private static double getSpeedMultiplier(String speedType) {
        String fieldName = switch (speedType) {
            case "slow" -> "SPEED_MULTIPLIER_SLOW";
            case "moderate" -> "SPEED_MULTIPLIER_MODERATE";
            case "fast" -> "SPEED_MULTIPLIER_FAST";
            case "extreme" -> "SPEED_MULTIPLIER_EXTREME";
            case "ultra" -> "SPEED_MULTIPLIER_ULTRA";
            default -> null;
        };
        if (fieldName == null) return 1.0;
        return getConfigDouble(fieldName, 1.0);
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

    private static double getConfigDouble(String fieldName, double defaultValue) {
        try {
            Class<?> configClass = Class.forName(CONFIG_CLASS);
            Field field = configClass.getField(fieldName);
            Object spec = field.get(null);
            Method get = spec.getClass().getMethod("get");
            return (Double) get.invoke(spec);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
