package net.unfamily.iskautils.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.unfamily.iskautils.Config;

import java.util.List;
import java.util.function.Consumer;

/**
 * Adds Pattern Crafter config-based tooltips to shared upgrade modules.
 * Reads the integrated Iska Utils Pattern Crafter configuration.
 */
public final class PatternCrafterTooltipHelper {

    private PatternCrafterTooltipHelper() {}

    /**
     * Production module tooltips require Pattern Crafter 1.2.0.0.0 or newer.
     */
    public static boolean supportsProductionModule() {
        return true;
    }

    public static boolean isPatternCrafterLoaded() {
        return true;
    }

    public static void appendSpeedModuleMaxInstall(Consumer<Component> tooltip) {
        appendPatternCrafterMaxInstall(tooltip, getConfigInt("MAX_SPEED_MODULES", 1));
    }

    public static void appendLogicModuleMaxInstall(Consumer<Component> tooltip) {
        appendPatternCrafterMaxInstall(tooltip, getConfigInt("MAX_LOGIC_MODULES", 3));
    }

    public static void appendProductionModuleMaxInstall(Consumer<Component> tooltip) {
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
            return switch (fieldName) {
                case "MAX_SPEED_MODULES" -> Config.MAX_SPEED_MODULES.get();
                case "MAX_LOGIC_MODULES" -> Config.MAX_LOGIC_MODULES.get();
                case "MAX_PRODUCTION_MODULES" -> Config.MAX_PRODUCTION_MODULES.get();
                default -> defaultValue;
            };
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static double getConfigDouble(String fieldName, double defaultValue) {
        try {
            return switch (fieldName) {
                case "SPEED_MULTIPLIER_SLOW" -> Config.SPEED_MULTIPLIER_SLOW.get();
                case "SPEED_MULTIPLIER_MODERATE" -> Config.SPEED_MULTIPLIER_MODERATE.get();
                case "SPEED_MULTIPLIER_FAST" -> Config.SPEED_MULTIPLIER_FAST.get();
                case "SPEED_MULTIPLIER_EXTREME" -> Config.SPEED_MULTIPLIER_EXTREME.get();
                case "SPEED_MULTIPLIER_ULTRA" -> Config.SPEED_MULTIPLIER_ULTRA.get();
                default -> defaultValue;
            };
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
