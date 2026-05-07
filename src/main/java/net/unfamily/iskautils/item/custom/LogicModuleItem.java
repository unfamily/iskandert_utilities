package net.unfamily.iskautils.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.fml.ModList;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.integration.PatternCrafterTooltipHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Logic Module: used by Pattern Crafter (adds pattern slots).
 * Tooltip shows Pattern Crafter max when mod is present and shift is held.
 */
public class LogicModuleItem extends Item {

    public LogicModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);

        tooltip.accept(Component.translatable("tooltip.iska_utils.fan_module.modular_fan_max", Config.fanAccelerationUpgradeMax)
                .withStyle(ChatFormatting.GRAY));

        if (ModList.get().isLoaded("pattern_crafter")) {
            List<Component> tmp = new ArrayList<>();
            PatternCrafterTooltipHelper.addLogicModuleTooltip(tmp);
            tmp.forEach(tooltip);
        }
    }
}
