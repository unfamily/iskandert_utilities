package net.unfamily.iskautils.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.integration.ColossalReactorsTooltipHelper;
import net.unfamily.iskautils.integration.PatternCrafterTooltipHelper;
import net.unfamily.iskautils.util.ScreenAccess;

import java.util.List;
import java.util.function.Consumer;

/**
 * Shared upgrade-module tooltips: gray install lines on Shift, generic aqua effect last.
 */
public final class UpgradeModuleTooltipHelper {

    private static final ChatFormatting GRAY = ChatFormatting.GRAY;
    private static final ChatFormatting AQUA = ChatFormatting.AQUA;

    private static final String MODULAR_FAN_MAX = "tooltip.iska_utils.module_compat.modular_fan.max";
    private static final String BLAZING_ALTAR_MAX = "tooltip.iska_utils.module_compat.blazing_altar.max";
    private static final String COLLECTING_CRATE_MAX = "tooltip.iska_utils.module_compat.collecting_crate.max";
    private static final String MOB_REAPER_MAX = "tooltip.iska_utils.module_compat.mob_reaper.max";
    private static final String TEMPORAL_OVERCLOCKER_MAX = "tooltip.iska_utils.module_compat.temporal_overclocker.max";
    private static final String ENTROPIC_SPAWNER_MAX = "tooltip.iska_utils.module_compat.entropic_spawner.max";

    private UpgradeModuleTooltipHelper() {}

    public static void appendShiftHint(List<Component> tooltip) {
        appendShiftHint(tooltip::add);
    }

    public static void appendShiftHint(Consumer<Component> tooltip) {
        tooltip.accept(Component.translatable("tooltip.iska_utils.fan_module.press_shift").withStyle(GRAY));
    }

    public static void appendRangeModuleTooltip(List<Component> tooltip, TooltipFlag flag) {
        finish(tooltip::add, flag, () -> appendRangeModuleInstalls(tooltip::add), "tooltip.iska_utils.module.range.effect");
    }

    public static void appendGhostModuleTooltip(List<Component> tooltip, TooltipFlag flag) {
        finish(tooltip::add, flag, () -> appendMachineMax(tooltip::add, MODULAR_FAN_MAX, 1), "tooltip.iska_utils.module.ghost.effect");
    }

    public static void appendSpeedModuleTooltip(List<Component> tooltip, TooltipFlag flag) {
        finish(tooltip::add, flag, () -> appendSpeedModuleInstalls(tooltip::add), "tooltip.iska_utils.module.speed.effect");
    }

    public static void appendLogicModuleTooltip(List<Component> tooltip, TooltipFlag flag) {
        finish(tooltip::add, flag, () -> PatternCrafterTooltipHelper.appendLogicModuleMaxInstall(tooltip::add), "tooltip.iska_utils.module.logic.effect");
    }

    public static void appendProductionModuleTooltip(List<Component> tooltip, TooltipFlag flag) {
        finish(tooltip::add, flag, () -> appendProductionModuleInstalls(tooltip::add), "tooltip.iska_utils.module.production.effect");
    }

    public static void appendEntropicClockTooltip(List<Component> tooltip, TooltipFlag flag) {
        finish(tooltip::add, flag, () -> appendEntropicClockInstalls(tooltip::add), "tooltip.iska_utils.module.entropic_clock.effect");
    }

    public static void appendMobReaperModuleTooltip(List<Component> tooltip, TooltipFlag flag, int max, String effectKey) {
        finish(tooltip::add, flag, () -> appendMachineMax(tooltip::add, MOB_REAPER_MAX, max), effectKey);
    }

    private static void finish(Consumer<Component> tooltip, TooltipFlag flag, Runnable installLines, String effectKey) {
        if (ScreenAccess.hasShiftDown()) {
            installLines.run();
            appendGenericEffect(tooltip, effectKey);
        } else {
            appendShiftHint(tooltip);
        }
    }

    private static void appendGenericEffect(Consumer<Component> tooltip, String effectKey) {
        tooltip.accept(Component.translatable(effectKey).withStyle(AQUA));
    }

    private static void appendMachineMax(Consumer<Component> tooltip, String key, int max) {
        tooltip.accept(Component.translatable(key, max).withStyle(GRAY));
    }

    private static void appendRangeModuleInstalls(Consumer<Component> tooltip) {
        appendMachineMax(tooltip, MODULAR_FAN_MAX, Config.fanRangeUpgradeMax);
        appendMachineMax(tooltip, BLAZING_ALTAR_MAX, Config.blazingAltarRangeUpgradeMax);
        appendMachineMax(tooltip, COLLECTING_CRATE_MAX, Config.collectingCrateRangeUpgradeMax);
    }

    private static void appendSpeedModuleInstalls(Consumer<Component> tooltip) {
        appendMachineMax(tooltip, MODULAR_FAN_MAX, Config.fanAccelerationUpgradeMax);
        PatternCrafterTooltipHelper.appendSpeedModuleMaxInstall(tooltip);
    }

    private static void appendProductionModuleInstalls(Consumer<Component> tooltip) {
        PatternCrafterTooltipHelper.appendProductionModuleMaxInstall(tooltip);
        ColossalReactorsTooltipHelper.appendProductionModuleMaxInstall(tooltip);
        appendMachineMax(tooltip, ENTROPIC_SPAWNER_MAX, Config.entropicSpawnerMaxProductionModules);
    }

    private static void appendEntropicClockInstalls(Consumer<Component> tooltip) {
        appendMachineMax(tooltip, TEMPORAL_OVERCLOCKER_MAX, 1);
        appendMachineMax(tooltip, ENTROPIC_SPAWNER_MAX, Config.entropicSpawnerMaxEntropicClocks);
    }
}
