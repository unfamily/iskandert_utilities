package net.unfamily.iskautils.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.unfamily.iskautils.Config;

import java.util.List;

public class BeheadingModuleItem extends Item {

    public BeheadingModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        UpgradeModuleTooltipHelper.appendMobReaperModuleTooltip(
                tooltip, flag, Config.reaperBeheadingUpgradeMax, "tooltip.iska_utils.module.beheading.effect");
    }
}
