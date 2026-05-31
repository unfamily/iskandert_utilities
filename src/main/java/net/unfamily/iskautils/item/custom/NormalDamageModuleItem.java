package net.unfamily.iskautils.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.unfamily.iskautils.Config;

import java.util.function.Consumer;

public class NormalDamageModuleItem extends Item {

    public NormalDamageModuleItem(Properties properties) {
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
            tooltip.accept(Component.translatable("tooltip.iska_utils.mob_reaper_module.max", Config.reaperNormalUpgradeMax)
                    .withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("tooltip.iska_utils.mob_reaper_module.normal_bonus", Config.reaperNormalBonusPerModule)
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(Component.translatable("tooltip.iska_utils.fan_module.press_shift")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
