package net.unfamily.iskautils.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.unfamily.iskautils.Config;

import java.util.function.Consumer;

public class BeheadingModuleItem extends Item {

    public BeheadingModuleItem(Properties properties) {
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
            tooltip.accept(Component.translatable("tooltip.iska_utils.mob_reaper_module.max", Config.reaperBeheadingUpgradeMax)
                    .withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("tooltip.iska_utils.mob_reaper_module.beheading_chance",
                            (int) Math.round(Config.reaperBeheadingChancePerLevel * 100) + "%")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(Component.translatable("tooltip.iska_utils.fan_module.press_shift")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
