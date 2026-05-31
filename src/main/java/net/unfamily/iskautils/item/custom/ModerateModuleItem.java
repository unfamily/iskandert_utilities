package net.unfamily.iskautils.item.custom;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;
import net.unfamily.iskautils.integration.PatternCrafterTooltipHelper;

import java.util.List;

public class ModerateModuleItem extends Item {

    public ModerateModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (Screen.hasShiftDown()) {
            FanModuleTooltipHelper.appendSpeedModuleLines(tooltip, FanModuleTooltipHelper.POWER_MODERATE);
            if (ModList.get().isLoaded("pattern_crafter")) {
                PatternCrafterTooltipHelper.addSpeedModuleTooltip(tooltip, "moderate");
            }
        } else {
            FanModuleTooltipHelper.appendShiftHint(tooltip);
        }
    }
}
