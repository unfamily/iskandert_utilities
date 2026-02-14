package net.unfamily.iskautils.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;
import net.unfamily.iskautils.integration.PatternCrafterTooltipHelper;

import java.util.List;

/**
 * Logic Module: used by Pattern Crafter (adds pattern slots).
 * Tooltip shows Pattern Crafter max when mod is present and shift is held.
 */
public class LogicModuleItem extends Item {

    public LogicModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        if (ModList.get().isLoaded("pattern_crafter")) {
            if (Screen.hasShiftDown()) {
                PatternCrafterTooltipHelper.addLogicModuleTooltip(tooltip);
            } else {
                tooltip.add(Component.translatable("tooltip.iska_utils.fan_module.press_shift")
                        .withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
