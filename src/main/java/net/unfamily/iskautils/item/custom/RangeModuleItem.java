package net.unfamily.iskautils.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.unfamily.iskautils.Config;

import java.util.function.Consumer;

/**
 * Custom Item for Range Module with tooltip showing max installable count
 */
public class RangeModuleItem extends Item {
    
    public RangeModuleItem(Properties properties) {
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
    }
}
