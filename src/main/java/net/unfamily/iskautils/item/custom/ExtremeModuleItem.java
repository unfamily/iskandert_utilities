package net.unfamily.iskautils.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.fml.ModList;
import net.unfamily.iskautils.integration.PatternCrafterTooltipHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ExtremeModuleItem extends Item {

    public ExtremeModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        if (flag.hasShiftDown()) {
            FanModuleTooltipHelper.appendSpeedModuleLines(tooltip, FanModuleTooltipHelper.POWER_EXTREME);
            appendPatternCrafterLines(tooltip, "extreme");
        } else {
            FanModuleTooltipHelper.appendShiftHint(tooltip);
        }
    }

    private static void appendPatternCrafterLines(Consumer<Component> tooltip, String tier) {
        if (ModList.get().isLoaded("pattern_crafter")) {
            List<Component> tmp = new ArrayList<>();
            PatternCrafterTooltipHelper.addSpeedModuleTooltip(tmp, tier);
            tmp.forEach(tooltip);
        }
    }
}
