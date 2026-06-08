package net.unfamily.iskautils.item.custom;

import net.unfamily.iskautils.util.ScreenAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

public class RangeModuleItem extends Item {

    public RangeModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (ScreenAccess.hasShiftDown()) {
            FanModuleTooltipHelper.appendRangeModuleLines(tooltip);
            FanModuleTooltipHelper.appendBlazingAltarRangeModuleLines(tooltip);
        } else {
            FanModuleTooltipHelper.appendShiftHint(tooltip);
        }
    }
}
