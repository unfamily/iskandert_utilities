package net.unfamily.iskautils.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.unfamily.iskautils.Config;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class FanModuleTooltipHelper {

    public static final int POWER_SLOW = 0;
    public static final int POWER_MODERATE = 1;
    public static final int POWER_FAST = 2;
    public static final int POWER_EXTREME = 3;
    public static final int POWER_ULTRA = 4;

    private static final ChatFormatting GRAY = ChatFormatting.GRAY;

    private FanModuleTooltipHelper() {}

    public static void appendShiftHint(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.iska_utils.fan_module.press_shift").withStyle(GRAY));
    }

    public static void appendShiftHint(Consumer<Component> tooltip) {
        tooltip.accept(Component.translatable("tooltip.iska_utils.fan_module.press_shift").withStyle(GRAY));
    }

    public static void appendRangeModuleLines(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.iska_utils.fan_module.modular_fan_max", Config.fanRangeUpgradeMax)
                .withStyle(GRAY));
        tooltip.add(Component.translatable("tooltip.iska_utils.fan_module.range_bonus").withStyle(GRAY));
    }

    public static void appendRangeModuleLines(Consumer<Component> tooltip) {
        tooltip.accept(Component.translatable("tooltip.iska_utils.fan_module.modular_fan_max", Config.fanRangeUpgradeMax)
                .withStyle(GRAY));
        tooltip.accept(Component.translatable("tooltip.iska_utils.fan_module.range_bonus").withStyle(GRAY));
    }

    public static void appendGhostModuleLines(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.iska_utils.fan_module.modular_fan_max", 1).withStyle(GRAY));
        tooltip.add(Component.translatable(ghostEffectKey()).withStyle(GRAY));
    }

    public static void appendGhostModuleLines(Consumer<Component> tooltip) {
        tooltip.accept(Component.translatable("tooltip.iska_utils.fan_module.modular_fan_max", 1).withStyle(GRAY));
        tooltip.accept(Component.translatable(ghostEffectKey()).withStyle(GRAY));
    }

    public static void appendSpeedModuleLines(List<Component> tooltip, int powerIndex) {
        tooltip.add(Component.translatable("tooltip.iska_utils.fan_module.modular_fan_max", Config.fanAccelerationUpgradeMax)
                .withStyle(GRAY));
        tooltip.add(Component.translatable("tooltip.iska_utils.fan_module.power_bonus", formatPower(getModulePower(powerIndex)))
                .withStyle(GRAY));
    }

    public static void appendSpeedModuleLines(Consumer<Component> tooltip, int powerIndex) {
        tooltip.accept(Component.translatable("tooltip.iska_utils.fan_module.modular_fan_max", Config.fanAccelerationUpgradeMax)
                .withStyle(GRAY));
        tooltip.accept(Component.translatable("tooltip.iska_utils.fan_module.power_bonus", formatPower(getModulePower(powerIndex)))
                .withStyle(GRAY));
    }

    private static String ghostEffectKey() {
        return Config.fanGhostModuleBypassUnbreakable
                ? "tooltip.iska_utils.fan_module.ghost_all_solids"
                : "tooltip.iska_utils.fan_module.ghost_breakable";
    }

    static double getModulePower(int powerIndex) {
        var powers = Config.fanAccelerationModulePowers;
        if (powerIndex >= 0 && powerIndex < powers.size()) {
            return powers.get(powerIndex);
        }
        return switch (powerIndex) {
            case POWER_SLOW -> 0.1D;
            case POWER_MODERATE -> 0.5D;
            case POWER_FAST -> 1.0D;
            case POWER_EXTREME -> 5.0D;
            case POWER_ULTRA -> 15.0D;
            default -> 0.0D;
        };
    }

    static String formatPower(double value) {
        if (Math.abs(value - Math.rint(value)) < 1.0e-9) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
