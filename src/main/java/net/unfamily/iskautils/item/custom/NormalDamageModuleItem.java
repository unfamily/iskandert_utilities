package net.unfamily.iskautils.item.custom;

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
            Consumer<net.minecraft.network.chat.Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        UpgradeModuleTooltipHelper.appendMobReaperModuleTooltip(
                tooltip, flag, Config.reaperNormalUpgradeMax, "tooltip.iska_utils.module.normal_damage.effect");
    }
}
