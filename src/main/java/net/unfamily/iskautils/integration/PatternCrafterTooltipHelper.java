package net.unfamily.iskautils.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Adds Pattern Crafter config-based tooltips to shared upgrade modules.
 * Uses reflection so no compile dependency on Pattern Crafter is required.
 * Only called when ModList.get().isLoaded("pattern_crafter") is true.
 */
public final class PatternCrafterTooltipHelper {

    private static final String CONFIG_CLASS = "net.unfamily.pattern_crafter.Config";

    private PatternCrafterTooltipHelper() {}

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
