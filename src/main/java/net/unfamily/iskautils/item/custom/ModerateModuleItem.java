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
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.integration.PatternCrafterTooltipHelper;

import java.util.List;

/**
 * Custom Item for Moderate Module with tooltip showing max installable count
 * (Modular Fan and Pattern Crafter when present).
 */
public class ModerateModuleItem extends Item {

    public ModerateModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.iska_utils.fan_module.modular_fan_max",
                    Config.fanAccelerationUpgradeMax)
                    .withStyle(ChatFormatting.GRAY));
            if (ModList.get().isLoaded("pattern_crafter")) {
                PatternCrafterTooltipHelper.addSpeedModuleTooltip(tooltip, "moderate");
            }
        } else {
            tooltip.add(Component.translatable("tooltip.iska_utils.fan_module.press_shift")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
