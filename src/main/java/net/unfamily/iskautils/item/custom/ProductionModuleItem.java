package net.unfamily.iskautils.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.unfamily.iskautils.integration.PatternCrafterTooltipHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Production Module: Pattern Crafter only (1.2.0.0.0+).
 * Tooltip is shown only when a compatible Pattern Crafter version is loaded.
 */
public class ProductionModuleItem extends Item {

    public ProductionModuleItem(Properties properties) {
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

        if (PatternCrafterTooltipHelper.supportsProductionModule()) {
            List<Component> tmp = new ArrayList<>();
            PatternCrafterTooltipHelper.addProductionModuleTooltip(tmp);
            tmp.forEach(tooltip);
        }
    }
}
